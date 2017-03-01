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
 * Please contact Eucalyptus Systems, Inc., 6750 Navigator Way, Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 ************************************************************************/

package com.eucalyptus.ws.handlers.http;

import java.lang.Exception;
import java.lang.Override;
import java.util.Date;
import java.util.Optional;

import com.eucalyptus.crypto.util.Timestamps;
import com.eucalyptus.ws.StackConfiguration;
import com.google.common.base.MoreObjects;
import org.jboss.netty.channel.ChannelDownstreamHandler;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelUpstreamHandler;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpHeaders;


public class HttpResponseHeaderHandler implements ChannelUpstreamHandler, ChannelDownstreamHandler {
  private volatile int requestCount = 0;

  @Override
  public void handleUpstream( final ChannelHandlerContext ctx, final ChannelEvent e ) throws Exception {
    if ( e instanceof MessageEvent && ((MessageEvent)e).getMessage( ) instanceof HttpRequest ) {
      requestCount++;
    }
    ctx.sendUpstream( e );
  }

  /**
   * This is for adding default/standard headers to *outbound* HTTP Responses.
   */
  @Override
  public void handleDownstream( final ChannelHandlerContext ctx, final ChannelEvent e ) throws Exception {
    if ( e instanceof MessageEvent && ((MessageEvent)e).getMessage() instanceof HttpResponse ) {
      final HttpResponse response = (HttpResponse) ( (MessageEvent) e ).getMessage( );

      // Persistent connection close
      if ( requestCount >= MoreObjects.firstNonNull( StackConfiguration.HTTP_MAX_REQUESTS_PER_CONNECTION, 100 ) ) {
        response.setHeader( HttpHeaders.Names.CONNECTION, HttpHeaders.Values.CLOSE );
        if ( ctx.getChannel( ).isOpen( ) ) {
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
    }
    ctx.sendDownstream( e );
  }
}
