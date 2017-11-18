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

package com.eucalyptus.ws.handlers.http;

import java.lang.Exception;
import java.lang.Override;
import java.util.Date;
import java.util.Optional;

import com.eucalyptus.crypto.util.Timestamps;
import com.eucalyptus.ws.StackConfiguration;
import com.google.common.base.MoreObjects;
import org.apache.log4j.Logger;
import org.jboss.netty.channel.ChannelDownstreamHandler;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelUpstreamHandler;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.handler.codec.http.HttpChunk;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.jboss.netty.handler.stream.ChunkedInput;


public class HttpResponseHeaderHandler implements ChannelUpstreamHandler, ChannelDownstreamHandler {
  private static final Logger logger = Logger.getLogger( HttpResponseHeaderHandler.class );

  private volatile int requestCount = 0;
  private volatile boolean closeSent = false;

  @Override
  public void handleUpstream( final ChannelHandlerContext ctx, final ChannelEvent e ) throws Exception {
    if ( e instanceof MessageEvent && ((MessageEvent)e).getMessage( ) instanceof HttpRequest ) {
      requestCount++;

      if ( !closeSent ) {
        ctx.sendUpstream( e );
      } else { 
        ctx.getChannel( ).setReadable( false );
      }
    } else {
      ctx.sendUpstream( e );
    }
  }

  /**
   * This is for adding default/standard headers to *outbound* HTTP Responses.
   */
  @Override
  public void handleDownstream( final ChannelHandlerContext ctx, final ChannelEvent e ) throws Exception {
    if ( e instanceof MessageEvent ) {
      final MessageEvent messageEvent = (MessageEvent) e;

      if ( messageEvent.getMessage( ) instanceof HttpChunk ) {
        final HttpChunk chunk = (HttpChunk) messageEvent.getMessage( );
        if ( chunk.isLast( ) &&
            ctx.getChannel( ).isOpen( ) &&
            closeSent ) {
          e.getFuture( ).addListener( ChannelFutureListener.CLOSE );
        }
      } else if ( messageEvent.getMessage() instanceof HttpResponse ) {
        final HttpResponse response = (HttpResponse) messageEvent.getMessage( );

        // Persistent connection close
        if ( HttpResponseStatus.OK.compareTo( response.getStatus( ) ) <= 0 &&
            ctx.getChannel( ).isOpen( ) &&
            requestCount >= MoreObjects.firstNonNull( StackConfiguration.HTTP_MAX_REQUESTS_PER_CONNECTION, 100 ) ) {
          response.setHeader( HttpHeaders.Names.CONNECTION, HttpHeaders.Values.CLOSE );
          closeSent = true;
          // chunked responses are not always marked as such, so treat as chunked if there
          // is no content in the response
          if ( !response.isChunked( ) && response.getContent( ).readableBytes( ) != 0 ) {
            e.getFuture( ).addListener( ChannelFutureListener.CLOSE );
          }
        }

        // If previous handler has already set the Date header, leave it as is.
        if (!response.containsHeader(HttpHeaders.Names.DATE)) {
          response.setHeader(HttpHeaders.Names.DATE, Timestamps.formatRfc822Timestamp(new Date()));
        }

        // If server already set then do not overwrite
        final Optional<String> header = StackConfiguration.getServerHeader( );
        if ( !response.containsHeader(HttpHeaders.Names.SERVER) && header.isPresent( ) ) {
          response.setHeader( HttpHeaders.Names.SERVER, header.get( ) );
        }
      } else if ( messageEvent.getMessage() instanceof ChunkedInput ) {
        final ChunkedInput input = (ChunkedInput) messageEvent.getMessage( );

        if ( ctx.getChannel( ).isOpen( ) &&
            closeSent ) {
          e.getFuture( ).addListener( future -> {
            if ( input.isEndOfInput( ) ) {
              future.getChannel( ).close( );
            } else {
              logger.warn( "Write listener fired before end of input, not closing channel." );
            }
          } );
        }
      }
    }

    ctx.sendDownstream( e );
  }
}
