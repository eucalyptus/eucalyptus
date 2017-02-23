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

import java.nio.charset.StandardCharsets;
import org.apache.axiom.soap.SOAP11Constants;
import org.apache.axiom.soap.SOAP12Constants;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAPFactory;
import org.apache.axiom.soap.impl.builder.StAXSOAPModelBuilder;
import com.eucalyptus.binding.Binding;
import com.eucalyptus.binding.BindingException;
import com.eucalyptus.binding.HoldMe;
import com.eucalyptus.util.LockResource;
import com.eucalyptus.ws.IoMessage;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.FullHttpMessage;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;

@ChannelHandler.Sharable
public class IoSoapMarshallingHandler extends ChannelDuplexHandler {

  @Override
  public void channelRead( final ChannelHandlerContext ctx, final Object msg ) throws Exception {
    if ( msg instanceof IoMessage ) {
      final IoMessage ioMessage = ( IoMessage ) msg;
      final FullHttpMessage httpMessage = ioMessage.getHttpMessage( );
      final String content = httpMessage.content( ).toString( StandardCharsets.UTF_8 );
      final boolean request;
      if ( httpMessage instanceof FullHttpRequest ) {
        request = true;
      } else {
        request = false;
        if ( content.isEmpty( ) ) {
          final FullHttpResponse httpResponse = (FullHttpResponse) httpMessage;
          ioMessage.setSoapEnvelope( Binding.createFault(
              httpResponse.getStatus( ).code( ) < 500 ? "soapenv:Client" : "soapenv:Server",
              "No content",
              httpResponse.getStatus( ).toString( ) ) );
          return;
        }
      }
      HoldMe.canHas.lock( );
      SOAPEnvelope env = null;
      try {
        StAXSOAPModelBuilder soapBuilder = null;
        try {
          SOAPFactory factory = HoldMe.getOMSOAP11Factory( );
          soapBuilder = new StAXSOAPModelBuilder( HoldMe.getXMLStreamReader( content ), factory , SOAP11Constants.SOAP_ENVELOPE_NAMESPACE_URI );
        } catch ( Exception e ) {
          SOAPFactory factory = HoldMe.getOMSOAP12Factory( );
          soapBuilder = new StAXSOAPModelBuilder( HoldMe.getXMLStreamReader( content ), factory , SOAP12Constants.SOAP_ENVELOPE_NAMESPACE_URI );
        }
        env = ( SOAPEnvelope ) soapBuilder.getDocumentElement( );
      } catch( Exception ex ) {
        if ( request ) {
          throw new BindingException( "Invalid request : " + ex.getMessage( ), ex );
        } else {
          env = Binding.createFault(
              "soapenv:Server",
              "Error parsing response",
              content );
        }
      } finally {
        HoldMe.canHas.unlock( );
      }
      ioMessage.setSoapEnvelope( env );
    }
    super.channelRead( ctx, msg );
  }

  @Override
  public void write( final ChannelHandlerContext ctx, final Object msg, final ChannelPromise promise ) throws Exception {
    if ( msg instanceof IoMessage ) {
      final IoMessage ioMessage = (IoMessage) msg;
      final FullHttpMessage httpMessage = ioMessage.getHttpMessage( );
      final ByteBuf buffer = httpMessage.content( ).clear( );
      try ( final ByteBufOutputStream out = new ByteBufOutputStream( buffer );
            final LockResource lockResource = LockResource.lock( HoldMe.canHas ) ) {
        ioMessage.getSoapEnvelope( ).serialize( out ); //HACK: does this need fixing for xml brokeness?
      }
      httpMessage.headers( ).add( HttpHeaders.Names.CONTENT_LENGTH, String.valueOf( buffer.readableBytes( ) ) );
      httpMessage.headers( ).add( HttpHeaders.Names.CONTENT_TYPE, "text/xml; charset=UTF-8" );
    }
    super.write( ctx, msg, promise );
  }
}
