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

import java.util.UUID;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMNamespace;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAPHeader;
import org.apache.axiom.soap.SOAPHeaderBlock;
import com.eucalyptus.binding.HoldMe;
import com.eucalyptus.ws.IoMessage;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.HttpRequest;

/**
 *
 */
@ChannelHandler.Sharable
public class IoAddressingHandler extends ChannelOutboundHandlerAdapter {

  private static final String   WSA_NAMESPACE                    = "http://www.w3.org/2005/08/addressing";
  private static final String   WSA_NAMESPACE_PREFIX             = "wsa";
  private static final String   WSA_MESSAGE_ID                   = "MessageID";
  private static final String   WSA_TO                           = "To";
  private static final String   WSA_ACTION                       = "Action";

  private final String prefix;

  public IoAddressingHandler( ) {
    this( "" );
  }

  public IoAddressingHandler( final String prefix ) {
    this.prefix = prefix;
  }

  @Override
  public void write( final ChannelHandlerContext ctx, final Object msg, final ChannelPromise promise ) throws Exception {
    if ( msg instanceof IoMessage && ((IoMessage)msg).isRequest( ) ) {
      final IoMessage ioMessage = ( IoMessage ) msg;
      final HttpRequest httpRequest = (HttpRequest) ioMessage.getHttpMessage( );
      final String action = addAddressing(
          prefix,
          ioMessage.getSoapEnvelope( ),
          ioMessage.getOmMessage( ),
          httpRequest.getUri( )
      );
      httpRequest.headers( ).add( "SOAPAction", action );
    }
    super.write( ctx, msg, promise );
  }

  static String addAddressing(
      final String prefix,
      final SOAPEnvelope soapEnvelope,
      final OMElement omMessage,
      final String uri
  ) {
    // :: set action :://
    final String action = prefix + omMessage.getLocalName( );
    final SOAPHeader header = soapEnvelope.getHeader( );

    // :: set soap addressing info :://
    final OMNamespace wsaNs = HoldMe.getOMFactory( ).createOMNamespace( WSA_NAMESPACE, WSA_NAMESPACE_PREFIX );
    if ( header != null ) {
      final SOAPHeaderBlock wsaToHeader = header.addHeaderBlock( WSA_TO, wsaNs );
      wsaToHeader.setText( uri );
      final SOAPHeaderBlock wsaActionHeader = header.addHeaderBlock( WSA_ACTION, wsaNs );
      wsaActionHeader.setText( action );
      final SOAPHeaderBlock wsaMsgId = header.addHeaderBlock( WSA_MESSAGE_ID, wsaNs );
      wsaMsgId.setText( "urn:uuid:" + UUID.randomUUID( ).toString( ).replaceAll( "-", "" ).toUpperCase( ) );
    }

    return action;
  }
}
