/*************************************************************************
 * Copyright 2008 Regents of the University of California
 * Copyright 2009-2012 Ent. Services Development Corporation LP
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
 * POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 * THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 * COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 * AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 * IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 * SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 * WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 * REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 * IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 * NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/

package com.eucalyptus.ws.handlers;

import java.util.concurrent.Callable;

import com.eucalyptus.ws.server.MessageStatistics;
import org.jboss.netty.channel.ChannelDownstreamHandler;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelUpstreamHandler;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import com.eucalyptus.ws.WebServicesException;

public abstract class MessageStackHandler implements ChannelDownstreamHandler, ChannelUpstreamHandler {
  
  @Override
  public void handleDownstream( final ChannelHandlerContext ctx, final ChannelEvent channelEvent ) throws Exception {
    try {
      if ( channelEvent instanceof MessageEvent ) {
        final MessageEvent msgEvent = ( MessageEvent ) channelEvent;
        if ( msgEvent.getMessage( ) != null ) {
          Callable<Long> stat = MessageStatistics.startDownstream(ctx.getChannel(), this);
          this.outgoingMessage( ctx, msgEvent );
          stat.call( );
        }
      }
      ctx.sendDownstream( channelEvent );
    } catch ( Exception e ) {
      throw new WebServicesException( e.getMessage( ), HttpResponseStatus.BAD_REQUEST );//TODO:GRZE: this is not right; needs to propagate in the right direction wrt server vs. client
    }
  }

  /**
   * Perform processing for an outgoing message.
   *
   * @param ctx The context for the message event
   * @param event The message event
   * @throws Exception If an error occurs
   */
  public void outgoingMessage(ChannelHandlerContext ctx, MessageEvent event) throws Exception {
  }
  
  public void incomingMessage( final ChannelHandlerContext ctx, MessageEvent event ) throws Exception {
    this.incomingMessage( event );
  }
  public void incomingMessage( MessageEvent event ) throws Exception {}
  
  @Override
  public void handleUpstream( final ChannelHandlerContext ctx, final ChannelEvent channelEvent ) throws Exception {
    if ( channelEvent instanceof MessageEvent ) {
      final MessageEvent msgEvent = ( MessageEvent ) channelEvent;
      Callable<Long> stat = MessageStatistics.startUpstream(ctx.getChannel(), this);
      this.incomingMessage( ctx, msgEvent );
      stat.call( );
      ctx.sendUpstream( channelEvent );
    } else {
      ctx.sendUpstream( channelEvent );
    }
  }
}
