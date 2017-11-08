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

package com.eucalyptus.ws.server;

import org.apache.log4j.Logger;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
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
import com.eucalyptus.component.Components;
import com.eucalyptus.component.Topology;
import com.eucalyptus.component.annotation.ComponentPart;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.empyrean.Empyrean;
import com.eucalyptus.http.MappingHttpRequest;
import com.google.common.base.Charsets;

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
            ChannelBuffer buf = ChannelBuffers.wrappedBuffer(resp.getBytes(Charsets.UTF_8));
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
