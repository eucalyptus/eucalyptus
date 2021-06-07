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

import java.util.Date;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;
import javax.security.auth.login.LoginException;

import org.apache.log4j.Logger;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandler;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.handler.codec.frame.TooLongFrameException;
import org.jboss.netty.handler.codec.http.DefaultHttpResponse;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.jboss.netty.handler.codec.http.HttpVersion;
import org.jboss.netty.handler.timeout.ReadTimeoutException;
import org.jboss.netty.handler.timeout.WriteTimeoutException;
import com.eucalyptus.binding.Binding;
import com.eucalyptus.component.annotation.ComponentPart;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.crypto.util.Timestamps;
import com.eucalyptus.http.MappingHttpMessage;
import com.eucalyptus.http.MappingHttpRequest;
import com.eucalyptus.records.Logs;
import com.eucalyptus.system.Ats;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.ws.Handlers;
import com.eucalyptus.ws.StackConfiguration;
import com.eucalyptus.ws.WebServicesException;
import com.eucalyptus.ws.handlers.ExceptionMarshallerHandler;
import com.eucalyptus.ws.server.FilteredPipeline.InternalPipeline;

public class NioServerHandler extends SimpleChannelUpstreamHandler {//TODO:GRZE: this needs to move up dependency tree.
  private static Logger                     LOG      = Logger.getLogger( NioServerHandler.class );
  private AtomicReference<FilteredPipeline> pipeline = new AtomicReference<FilteredPipeline>( );
  
  @Override
  public void messageReceived( final ChannelHandlerContext ctx, final MessageEvent e ) throws Exception {
    Callable<Long> stat = MessageStatistics.startUpstream(ctx.getChannel(), this);
    try {
      if ( this.pipeline.get( ) == null ) {
        lookupPipeline( ctx, e );
      } else if ( e.getMessage( ) instanceof MappingHttpRequest ) {
        MappingHttpRequest httpRequest = ( MappingHttpRequest ) e.getMessage( );
        if ( isPersistentConnection( httpRequest ) ) {
          this.pipeline.set( null );
          ChannelHandler p;
          while ( ( p = ctx.getPipeline( ).getLast( ) ) != this ) {
            ctx.getPipeline( ).remove( p );
          }
          lookupPipeline( ctx, e );
        } else {
          LOG.warn( "Hard close the socket on an attempt to do a second request." );
          ctx.getChannel( ).close( );
          return;
        }
      }
      stat.call( );
      ctx.sendUpstream( e );
    } catch ( Exception ex ) {
      LOG.trace( ex );
      Logs.extreme( ).error( ex, ex );
      stat.call( );
      this.sendError( ctx, e, HttpResponseStatus.NOT_FOUND, ex );
    }
  }
  
  public static boolean isPersistentConnection( final MappingHttpRequest httpRequest ) {
    return
        ( httpRequest.getProtocolVersion( ).equals( HttpVersion.HTTP_1_1 )
            && !HttpHeaders.Values.CLOSE.equalsIgnoreCase( httpRequest.getHeader( HttpHeaders.Names.CONNECTION ) ) ) ||
        ( httpRequest.getProtocolVersion( ).equals( HttpVersion.HTTP_1_0 )
             && HttpHeaders.Values.KEEP_ALIVE.equalsIgnoreCase( httpRequest.getHeader( HttpHeaders.Names.CONNECTION ) ) );
  }
  
  private void lookupPipeline( final ChannelHandlerContext ctx, final MessageEvent e ) throws DuplicatePipelineException, NoAcceptingPipelineException {
    try {
      final HttpRequest request = ( HttpRequest ) e.getMessage( );
      if ( Logs.isExtrrreeeme( ) && request instanceof MappingHttpMessage ) {
        Logs.extreme( ).trace( ( ( MappingHttpMessage ) request ).logMessage( ) );
      }
      final FilteredPipeline filteredPipeline = Pipelines.find( request );
      if ( this.pipeline.compareAndSet( null, filteredPipeline ) ) {
        this.pipeline.get( ).unroll( ctx.getPipeline( ) );
        if ( filteredPipeline instanceof InternalPipeline ) {
          Handlers.addInternalSystemHandlers( ctx.getPipeline( ) );
        }
        final Ats ats = Ats.inClassHierarchy( filteredPipeline );
        Handlers.addComponentHandlers(
            ats.has(ComponentPart.class) ? ats.get(ComponentPart.class).value() : null,
            ctx.getPipeline() );
        Handlers.addSystemHandlers( ctx.getPipeline( ) );
      }
    } catch ( DuplicatePipelineException e1 ) {
      LOG.error( "This is a BUG: " + e1, e1 );
      throw e1;
    } catch ( NoAcceptingPipelineException e2 ) {
      throw e2;
    }
  }
  
  @Override
  public void exceptionCaught( final ChannelHandlerContext ctx, final ExceptionEvent e ) throws Exception {
    final Channel ch = e.getChannel( );
    final Throwable cause = e.getCause( );
    Logs.extreme( ).error( cause, cause );
    try {
      if ( cause instanceof ReadTimeoutException ) {//TODO:ASAP:GRZE: wth are all these exception types?!?! ONLY WebServicesException caught; else wrap.
        this.sendError( ctx, e, HttpResponseStatus.REQUEST_TIMEOUT, cause );
      } else if ( cause instanceof WriteTimeoutException ) {
        ctx.sendUpstream( e );
        ctx.getChannel().close();
      } else if ( cause instanceof TooLongFrameException ) {
        this.sendError( ctx, e, HttpResponseStatus.BAD_REQUEST, cause );
      } else if ( cause instanceof IllegalArgumentException ) {
        this.sendError( ctx, e, HttpResponseStatus.BAD_REQUEST, cause );
      } else if ( cause instanceof LoginException ) {
        this.sendError( ctx, e, HttpResponseStatus.FORBIDDEN, cause );
      } else if ( e.getCause() instanceof WebServicesException ) {
        final WebServicesException webEx = (WebServicesException) e.getCause();
        if ( webEx.getStatus().getCode() != 403 ) LOG.error( "Internal Error: " + cause.getMessage() );
        Logs.extreme().error( cause, cause );
        this.sendError( ctx, e, webEx.getStatus(), cause );
      } else {
        this.sendError( ctx, e, HttpResponseStatus.BAD_REQUEST, cause );
      }
    } finally {
      try {
        if ( ch != null ) {
          Contexts.clear( Contexts.lookup( ch ) );
        }
      } catch ( Exception ex ) {
//      LOG.error( ex, ex );
      }
    }
  }
  
  private void sendError( final ChannelHandlerContext ctx,
                          final ChannelEvent event,
                          final HttpResponseStatus restStatus,
                          Throwable t ) {
    Logs.exhaust( ).error( t, t );

    HttpResponseStatus status = restStatus;
    ChannelBuffer buffer = null;
    Map<String,String> headers = Collections.emptyMap( );
    final ExceptionMarshallerHandler exceptionMarshallerHandler =
        ctx.getPipeline().get( ExceptionMarshallerHandler.class );
    if ( exceptionMarshallerHandler != null ) {
      try {
        final ExceptionMarshallerHandler.ExceptionResponse exceptionResponse =
            exceptionMarshallerHandler.marshallException( event, status, t );
        status = exceptionResponse.getStatus();
        buffer = exceptionResponse.getContent();
        headers = exceptionResponse.getHeaders();
      } catch ( Exception e ) {
        Logs.exhaust().error( e, e );
      }
    }

    if ( buffer == null ) {
      buffer = ChannelBuffers.copiedBuffer( Binding.createRestFault( status.toString(), t.getMessage(), Logs.isExtrrreeeme()
          ? Exceptions.string( t )
          : t.getMessage() ), "UTF-8" );

    }

    final HttpResponse response = new DefaultHttpResponse( HttpVersion.HTTP_1_1, status );
    response.setHeader( HttpHeaders.Names.CONTENT_TYPE, "text/plain; charset=UTF-8" );
    response.setHeader( HttpHeaders.Names.CONTENT_LENGTH, String.valueOf( buffer.readableBytes() ) );
    response.setHeader( HttpHeaders.Names.DATE, Timestamps.formatRfc822Timestamp( new Date()));
    final java.util.Optional<String> header = StackConfiguration.getServerHeader( );
    if ( header.isPresent( ) ) {
      response.setHeader( HttpHeaders.Names.SERVER, header.get( ) );
    }
    for ( final Map.Entry<String,String> headerEntry : headers.entrySet( ) ) {
      response.setHeader( headerEntry.getKey( ), headerEntry.getValue( ) );
    }
    response.setContent( buffer );

    ChannelFuture writeFuture = Channels.future( ctx.getChannel( ) );
    writeFuture.addListener( ChannelFutureListener.CLOSE );
    response.setHeader( HttpHeaders.Names.CONNECTION, HttpHeaders.Values.CLOSE );
    if ( ctx.getChannel( ).isConnected( ) ) {
      Channels.write( ctx, writeFuture, response );
    }
  }
}
