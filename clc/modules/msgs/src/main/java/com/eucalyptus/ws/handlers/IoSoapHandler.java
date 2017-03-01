/*************************************************************************
 * (c) Copyright 2016 Hewlett Packard Enterprise Development Company LP
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 ************************************************************************/
package com.eucalyptus.ws.handlers;

import java.util.Iterator;
import java.util.List;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import javax.xml.soap.SOAPConstants;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAPFactory;
import org.apache.axiom.soap.SOAPFault;
import org.apache.axiom.soap.SOAPHeader;
import org.apache.axiom.soap.SOAPHeaderBlock;
import com.eucalyptus.binding.Binding;
import com.eucalyptus.binding.HoldMe;
import com.eucalyptus.records.Logs;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.Pair;
import com.eucalyptus.ws.EucalyptusRemoteFault;
import com.eucalyptus.ws.IoMessage;
import com.google.common.collect.Lists;
import edu.ucsb.eucalyptus.msgs.EucalyptusErrorMessageType;
import edu.ucsb.eucalyptus.msgs.ExceptionResponseType;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.HttpMessage;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import javaslang.control.Option;

/**
 * SOAP object model handler
 */
@SuppressWarnings( "ThrowableResultOfMethodCallIgnored" )
@ChannelHandler.Sharable
public class IoSoapHandler extends ChannelDuplexHandler {


  /**
   * Handle incoming soap model
   */
  @Override
  public void channelRead( final ChannelHandlerContext ctx, final Object msg ) throws Exception {
    if ( msg instanceof IoMessage ) {
      final IoMessage ioMessage = IoMessage.class.cast( msg );
      final SOAPEnvelope env = ioMessage.getSoapEnvelope( );
      if ( env != null && !env.hasFault( ) ) {
        ioMessage.setOmMessage( env.getBody( ).getFirstElement( ) );
      } else {
        final Supplier<Integer> statusCodeSupplier = () -> getStatus( ioMessage );
        perhapsFault( env, statusCodeSupplier );
      }
    }
    super.channelRead( ctx, msg );
  }

  /**
   * Create outbound soap model
   */
  @Override
  public void write( final ChannelHandlerContext ctx, final Object msg, final ChannelPromise promise ) throws Exception {
    if ( msg instanceof IoMessage ) {
      final IoMessage ioMessage = IoMessage.class.cast( msg );
      final HttpMessage httpMessage = ioMessage.getHttpMessage( );
      final Option<Pair<SOAPEnvelope,Integer>> soapEnvelopeOption =
          IoSoapHandler.perhapsBuildFault( ioMessage.getMessage( ) );


      if ( soapEnvelopeOption.isDefined( ) ) {
        ioMessage.setSoapEnvelope( soapEnvelopeOption.get( ).getLeft( ) );
        if ( httpMessage instanceof HttpResponse ) {
          ( ( HttpResponse ) httpMessage ).setStatus( HttpResponseStatus.valueOf( soapEnvelopeOption.get( ).getRight( ) ) );
        }
      } else {
        ioMessage.setSoapEnvelope( buildSoapEnvelope( ioMessage.getOmMessage() ) );
      }
    }
    super.write( ctx, msg, promise );
  }

  @Nullable
  private static Integer getStatus( final IoMessage message ) {
    return message.getHttpMessage( ) instanceof HttpResponse ?
        ((HttpResponse) message.getHttpMessage( )).getStatus( ).code( ) :
        null;
  }

  static SOAPEnvelope buildSoapEnvelope( final OMElement body ) {
    final SOAPFactory factory = HoldMe.getOMSOAP11Factory( );
    final SOAPEnvelope soapEnvelope = factory.createSOAPEnvelope( factory.createOMNamespace( SOAPConstants.URI_NS_SOAP_1_1_ENVELOPE, "" ) );
    factory.createSOAPHeader( soapEnvelope );
    factory.createSOAPBody( soapEnvelope ).addChild( body );
    return soapEnvelope;
  }

  static Option<Pair<SOAPEnvelope,Integer>> perhapsBuildFault( final Object msg ) {
    Option<Pair<SOAPEnvelope,Integer>> soapEnvelopeOption = Option.none( );
    if ( msg instanceof EucalyptusErrorMessageType ) {
      EucalyptusErrorMessageType errMsg = ( EucalyptusErrorMessageType ) msg;
      soapEnvelopeOption = Option.some( Pair.of(
          Binding.createFault( errMsg.getSource( ), errMsg.getMessage( ), errMsg.getStatusMessage( ) ),
          org.jboss.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST.getCode( )
      ) );
    } else if ( msg instanceof ExceptionResponseType ) {
      ExceptionResponseType errMsg = ( ExceptionResponseType ) msg;
      String createFaultDetails = Logs.isExtrrreeeme( )
          ? Exceptions.string( errMsg.getException( ) )
          : errMsg.getException( ).getMessage( );
      soapEnvelopeOption = Option.some( Pair.of(
          Binding.createFault( errMsg.getRequestType( ), errMsg.getMessage( ), createFaultDetails ),
          errMsg.getHttpStatus( ).getCode( )
      ) );
    }
    return soapEnvelopeOption;
  }

  static void perhapsFault( final SOAPEnvelope env, final Supplier<Integer> statusCodeSupplier ) throws EucalyptusRemoteFault {
    final SOAPHeader header = env.getHeader( );
    String action = "ProblemAction";
    String relatesTo = "RelatesTo";
    if ( header != null ) {
      final List<SOAPHeaderBlock> headers = Lists.newArrayList( header.examineAllHeaderBlocks( ) );
      // :: try to get the fault info from the soap header -- hello there? :://
      for ( final SOAPHeaderBlock headerBlock : headers ) {
        if ( action.equals( headerBlock.getLocalName( ) ) ) {
          action = headerBlock.getText( );
        } else if ( relatesTo.equals( headerBlock.getLocalName( ) ) ) {
          relatesTo = headerBlock.getText( );
        }
      }
    }
    //faults don't need to have a header.
    // :: process the real fault :://
    final SOAPFault fault = env.getBody( ).getFault( );
    if ( fault != null ) {
      String faultReason = "";
      final Iterator children = fault.getChildElements( );
      while ( children.hasNext( ) ) {
        final OMElement child = ( OMElement ) children.next( );
        faultReason += child.getText( );
      }
      final String faultCode = fault.getCode( ).getText( );
      faultReason = faultReason.replaceAll( faultCode, "" );
      final String faultDetail = fault.getDetail( ).getText( );
      throw new EucalyptusRemoteFault( action, relatesTo, faultCode, faultReason, faultDetail, statusCodeSupplier.get( ) );
    }
  }
}
