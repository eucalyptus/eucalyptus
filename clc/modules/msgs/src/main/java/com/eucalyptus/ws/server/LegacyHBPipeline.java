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

package com.eucalyptus.ws.server;

import org.apache.log4j.Logger;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineCoverage;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;
import org.jboss.netty.handler.codec.http.DefaultHttpResponse;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpMethod;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import com.eucalyptus.component.Component;
import com.eucalyptus.component.ComponentId.ComponentPart;
import com.eucalyptus.component.Components;
import com.eucalyptus.component.Topology;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.empyrean.Empyrean;
import com.eucalyptus.http.MappingHttpRequest;

@ComponentPart( Empyrean.class )
public class LegacyHBPipeline extends FilteredPipeline {
  private static Logger LOG = Logger.getLogger( LegacyHBPipeline.class );
  
  @Override
  public boolean checkAccepts( HttpRequest message ) {
    return message.getUri( ).endsWith( "/services/Heartbeat" );
  }
  
  @Override
  public String getName( ) {
    return "heartbeat";
  }
  
  @Override
  public ChannelPipeline addHandlers( ChannelPipeline pipeline ) {
    pipeline.addLast( "hb-get-handler", new SimpleHeartbeatHandler( ) );
    return pipeline;
  }
  
  @ChannelPipelineCoverage( "one" )
  public static class SimpleHeartbeatHandler extends SimpleChannelHandler {
    
    @Override
    public void messageReceived( ChannelHandlerContext ctx, MessageEvent e ) throws Exception {
      if ( e.getMessage( ) instanceof MappingHttpRequest ) {
        MappingHttpRequest request = ( MappingHttpRequest ) e.getMessage( );
        HttpMethod method = request.getMethod( );
        if ( HttpMethod.GET.equals( method ) ) {
          try {
            HttpResponse response = new DefaultHttpResponse( request.getProtocolVersion( ), HttpResponseStatus.OK );
            String resp = "";
            for ( Component c : Components.list( ) ) {
              boolean enabledLocally = Topology.isEnabledLocally( c.getComponentId( ).getClass( ) );
              resp += String.format( "name=%-20.20s enabled=%-10.10s local=%-10.10s initialized=%-10.10s\n",
                                     c.getName( ),
                                     enabledLocally,
                                     c.hasLocalService( ),
                                     c.getComponentId( ).isAvailableLocally( ) );
            }
            ChannelBuffer buf = ChannelBuffers.copiedBuffer( resp.getBytes( ) );
            response.setContent( buf );
            response.addHeader( HttpHeaders.Names.CONTENT_LENGTH, String.valueOf( buf.readableBytes( ) ) );
            response.addHeader( HttpHeaders.Names.CONTENT_TYPE, "text/plain; charset=UTF-8" );
            ChannelFuture writeFuture = ctx.getChannel( ).write( response );
            writeFuture.addListener( ChannelFutureListener.CLOSE );
          } finally {
            Contexts.clear( request.getCorrelationId( ) );
          }
        } else {
          try {
            ctx.getChannel( ).write( new DefaultHttpResponse( request.getProtocolVersion( ), HttpResponseStatus.METHOD_NOT_ALLOWED ) ).addListener( ChannelFutureListener.CLOSE );
          } finally {
            Contexts.clear( request.getCorrelationId( ) );
          }
        }
      } else {
        ctx.sendUpstream( e );
      }
    }
    
    @Override
    public void exceptionCaught( ChannelHandlerContext ctx, ExceptionEvent e ) throws Exception {
      e.getFuture( ).addListener( ChannelFutureListener.CLOSE );
      super.exceptionCaught( ctx, e );
    }
    
  }
  
}
