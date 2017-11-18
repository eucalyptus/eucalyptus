/*************************************************************************
 * Copyright 2016 Ent. Services Development Corporation LP
 *
 * Redistribution and use of this software in source and binary forms,
 * with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *   Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 *   Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer
 *   in the documentation and/or other materials provided with the
 *   distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
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
import io.vavr.control.Option;

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
