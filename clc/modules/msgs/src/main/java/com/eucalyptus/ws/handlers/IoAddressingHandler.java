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
