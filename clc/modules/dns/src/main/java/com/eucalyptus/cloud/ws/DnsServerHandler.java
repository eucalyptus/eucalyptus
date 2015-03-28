/*************************************************************************
 * Copyright 2009-2015 Eucalyptus Systems, Inc.
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
 *
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 ************************************************************************/
package com.eucalyptus.cloud.ws;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Arrays;

import org.apache.log4j.Logger;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.xbill.DNS.Message;
import org.xbill.DNS.Rcode;

public class DnsServerHandler extends SimpleChannelUpstreamHandler {
  private static Logger LOG = Logger.getLogger( DnsServerHandler.class );
  private static final ConnectionHandler legacyDns = new ConnectionHandler();
  
  @Override
  public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
    byte[] inbuf = null;
    try {
      ChannelBuffer buffer = ((ChannelBuffer) e.getMessage());
      inbuf = new byte[buffer.readableBytes( )];
      buffer.getBytes( 0, inbuf );
      Message query = new Message(inbuf);
      final InetAddress localAddr = ((InetSocketAddress) e.getChannel( ).getLocalAddress( )).getAddress( );
      final InetAddress remoteAddr = ((InetSocketAddress) e.getRemoteAddress()).getAddress();
      ConnectionHandler.setLocalAndRemoteInetAddresses(localAddr, remoteAddr );
      try {
        byte[] outbuf = legacyDns.generateReply( query, inbuf, inbuf.length, null );
        ChannelBuffer chanOutBuf = ChannelBuffers.wrappedBuffer( outbuf );
        ctx.getChannel().write(chanOutBuf,e.getRemoteAddress( ));
        return;
      } catch ( Exception ex ) {
        LOG.debug( ex, ex );
        byte[] outbuf = legacyDns.errorMessage(query, Rcode.SERVFAIL);
        LOG.info(outbuf);
        ChannelBuffer chanOutBuf = ChannelBuffers.wrappedBuffer( outbuf );
        ctx.getChannel().write(chanOutBuf,e.getRemoteAddress( ));
        throw ex;
      } finally {
        ConnectionHandler.clearInetAddresses();
      }
    } catch ( Exception ex ) {
      LOG.debug( ex, ex);
      byte[] outbuf = legacyDns.formerrMessage(inbuf);
      LOG.info(outbuf);
      ChannelBuffer chanOutBuf = ChannelBuffers.wrappedBuffer( outbuf );
      ctx.getChannel().write(chanOutBuf,e.getRemoteAddress( ));
      throw ex;
    }
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e)
  throws Exception {
    e.getCause().printStackTrace();
    e.getChannel().close();
  }
}
