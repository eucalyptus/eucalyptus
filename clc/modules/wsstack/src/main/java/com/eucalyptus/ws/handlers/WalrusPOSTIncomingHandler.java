/*************************************************************************
 * Copyright 2009-2012 Eucalyptus Systems, Inc.
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

package com.eucalyptus.ws.handlers;

import java.io.IOException;
import org.apache.log4j.Logger;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipelineCoverage;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.UpstreamMessageEvent;
import org.jboss.netty.handler.codec.http.DefaultHttpChunk;
import com.eucalyptus.http.MappingHttpRequest;

@ChannelPipelineCoverage("one")
public class WalrusPOSTIncomingHandler extends MessageStackHandler {
	private static Logger LOG = Logger.getLogger( WalrusPOSTIncomingHandler.class );
	private final static long EXPIRATION_LIMIT = 900000;
	private boolean waitForNext;
	private boolean processedFirstChunk;
	private MappingHttpRequest httpRequest;

	@Override
	public void handleUpstream( final ChannelHandlerContext channelHandlerContext, final ChannelEvent channelEvent ) throws Exception {
		LOG.debug( this.getClass( ).getSimpleName( ) + "[incoming]: " + channelEvent );
		if ( channelEvent instanceof MessageEvent ) {
			final MessageEvent msgEvent = ( MessageEvent ) channelEvent;
			this.incomingMessage( channelHandlerContext, msgEvent );
		} else if ( channelEvent instanceof ExceptionEvent ) {
			this.exceptionCaught( channelHandlerContext, ( ExceptionEvent ) channelEvent );
		}
		if(!waitForNext)
			channelHandlerContext.sendUpstream( channelEvent );
		if(processedFirstChunk)
			waitForNext = false;
	}
	
  public void exceptionCaught( final ChannelHandlerContext ctx, final ExceptionEvent exceptionEvent ) throws Exception {
    Throwable t = exceptionEvent.getCause( );
    if ( t != null && IOException.class.isAssignableFrom( t.getClass( ) ) ) {
      LOG.debug( t, t );
    } else {
      LOG.debug( t, t );
    }
    ctx.sendUpstream( exceptionEvent );
  }

	@Override
	public void incomingMessage( ChannelHandlerContext ctx, MessageEvent event ) throws Exception {
		if(event.getMessage() instanceof MappingHttpRequest) {
			MappingHttpRequest httpRequest = (MappingHttpRequest) event.getMessage();
			if(httpRequest.getContent().readableBytes() == 0) {
				waitForNext = true;
				processedFirstChunk = false;
				this.httpRequest = httpRequest;
			}
		} else if(event.getMessage() instanceof DefaultHttpChunk) {
			if(!processedFirstChunk) {
				DefaultHttpChunk httpChunk = (DefaultHttpChunk) event.getMessage();
				httpRequest.setContent(httpChunk.getContent());
				processedFirstChunk = true;
		        UpstreamMessageEvent newEvent = new UpstreamMessageEvent( ctx.getChannel( ), httpRequest, null);
		        ctx.sendUpstream(newEvent);
			}
		}
	}

	@Override
	public void outgoingMessage(ChannelHandlerContext ctx, MessageEvent event)
			throws Exception {		
	}
}
