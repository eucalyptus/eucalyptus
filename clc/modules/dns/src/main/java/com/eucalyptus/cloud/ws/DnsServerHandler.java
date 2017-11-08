/*************************************************************************
 * Copyright 2009-2015 Ent. Services Development Corporation LP
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
package com.eucalyptus.cloud.ws;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Date;

import org.apache.log4j.Logger;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.xbill.DNS.Message;
import org.xbill.DNS.Rcode;

import com.eucalyptus.cloud.ws.DNSControl.TimedDns;

public class DnsServerHandler extends SimpleChannelUpstreamHandler {
  private static Logger LOG = Logger.getLogger( DnsServerHandler.class );
  private static final ConnectionHandler legacyDns = new ConnectionHandler();
  
  private Long DISCARD_REQUEST_AFTER_MS = 10000L;
  
  @Override
  public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
    byte[] inbuf = null;
    try {   
      if (e.getMessage() instanceof TimedDns) {
        final TimedDns request = (TimedDns) e.getMessage();
        inbuf = request.getRequest();
        if (request.getReceivedTime() != null 
            && (new Date()).getTime() - request.getReceivedTime() > DISCARD_REQUEST_AFTER_MS) {
          throw new Exception("Request timed out");
        }
      }else {
        final ChannelBuffer buffer = ((ChannelBuffer) e.getMessage());
        inbuf = new byte[buffer.readableBytes( )];
        buffer.getBytes( 0, inbuf );
      }
      
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
        ChannelBuffer chanOutBuf = ChannelBuffers.wrappedBuffer( outbuf );
        ctx.getChannel().write(chanOutBuf,e.getRemoteAddress( ));
        throw ex;
      } finally {
        ConnectionHandler.clearInetAddresses();
      }
    } catch ( Exception ex ) {
      LOG.debug( ex, ex);
      byte[] outbuf = legacyDns.formerrMessage(inbuf);
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
