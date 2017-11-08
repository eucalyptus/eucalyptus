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
