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
 *
 * This file may incorporate work covered under the following copyright
 * and permission notice:
 *
 *   Software License Agreement (BSD License)
 *
 *   Copyright (c) 2008, Regents of the University of California
 *   All rights reserved.
 *
 *   Redistribution and use of this software in source and binary forms,
 *   with or without modification, are permitted provided that the
 *   following conditions are met:
 *
 *     Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *     Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer
 *     in the documentation and/or other materials provided with the
 *     distribution.
 *
 *   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *   "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *   LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 *   FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 *   COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 *   INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 *   BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *   LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 *   CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 *   LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 *   ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 *   POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 *   THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 *   COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 *   AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 *   IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 *   SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 *   WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 *   REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 *   IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 *   NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/

package com.eucalyptus.ws.server;

import java.io.ByteArrayOutputStream;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;
import javax.security.auth.login.LoginException;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.log4j.Logger;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandler;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipelineCoverage;
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
import com.eucalyptus.binding.HoldMe;
import com.eucalyptus.component.annotation.ComponentPart;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.http.MappingHttpMessage;
import com.eucalyptus.http.MappingHttpRequest;
import com.eucalyptus.records.Logs;
import com.eucalyptus.system.Ats;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.ws.Handlers;
import com.eucalyptus.ws.WebServicesException;
import com.eucalyptus.ws.server.FilteredPipeline.InternalPipeline;
import com.eucalyptus.ws.handlers.SoapMarshallingHandler;
import com.google.common.base.Optional;

@ChannelPipelineCoverage( "one" )
public class NioServerHandler extends SimpleChannelUpstreamHandler {//TODO:GRZE: this needs to move up dependency tree.
  private static Logger                     LOG      = Logger.getLogger( NioServerHandler.class );
  private AtomicReference<FilteredPipeline> pipeline = new AtomicReference<FilteredPipeline>( );
  
  @Override
  public void messageReceived( final ChannelHandlerContext ctx, final MessageEvent e ) throws Exception {
    Callable<Long> stat = Statistics.startUpstream( ctx.getChannel( ), this );
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
      this.sendError( ctx, HttpResponseStatus.NOT_FOUND, ex );
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
      if ( ch != null ) {
        Contexts.clear( Contexts.lookup( ch ) );
      }
    } catch ( Exception ex ) {
//      LOG.error( ex, ex );
    }
    if ( cause instanceof ReadTimeoutException ) {//TODO:ASAP:GRZE: wth are all these exception types?!?! ONLY WebServicesException caught; else wrap.
      this.sendError( ctx, HttpResponseStatus.REQUEST_TIMEOUT, cause );
    } else if ( cause instanceof WriteTimeoutException ) {
      ctx.sendUpstream( e );
      ctx.getChannel( ).close( );
    } else if ( cause instanceof TooLongFrameException ) {
      this.sendError( ctx, HttpResponseStatus.BAD_REQUEST, cause );
    } else if ( cause instanceof IllegalArgumentException ) {
      this.sendError( ctx, HttpResponseStatus.BAD_REQUEST, cause );
    } else if ( cause instanceof LoginException ) {
      this.sendError( ctx, HttpResponseStatus.FORBIDDEN, cause );
    } else if ( e.getCause( ) instanceof WebServicesException ) {
      LOG.error( "Internal Error: " + cause.getMessage( ) );
      Logs.extreme( ).error( cause, cause );
      this.sendError( ctx, ( ( WebServicesException ) e.getCause( ) ).getStatus( ), cause );
    } else {
      this.sendError( ctx, HttpResponseStatus.BAD_REQUEST, cause );
    }
  }
  
  private void sendError( final ChannelHandlerContext ctx, final HttpResponseStatus restStatus, Throwable t ) {
    Logs.exhaust( ).error( t, t );

    HttpResponseStatus status = restStatus;
    ChannelBuffer buffer = null;
    Optional<String> contentType = Optional.absent();
    if ( ctx.getPipeline().get( SoapMarshallingHandler.class ) != null ) {
      final SOAPEnvelope soapEnvelope = Binding.createFault(
          status.getCode()<500 ? "soapenv:Client" : "soapenv:Server",
          t.getMessage(),
          Logs.isExtrrreeeme() ?
            Exceptions.string( t ) :
            t.getMessage() );
      try {
        final ByteArrayOutputStream byteOut = new ByteArrayOutputStream( );
        HoldMe.canHas.lock( );
        try {
          soapEnvelope.serialize( byteOut );
        } finally {
          HoldMe.canHas.unlock( );
        }
        status = HttpResponseStatus.INTERNAL_SERVER_ERROR;
        buffer = ChannelBuffers.wrappedBuffer( byteOut.toByteArray( ) );
        contentType = Optional.of( "text/xml; charset=UTF-8" );
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
    response.addHeader( HttpHeaders.Names.CONTENT_TYPE, contentType.or( "text/plain; charset=UTF-8" ) );
    response.addHeader( HttpHeaders.Names.CONTENT_LENGTH, String.valueOf( buffer.readableBytes( ) ) );
    response.setContent( buffer );

    ChannelFuture writeFuture = Channels.future( ctx.getChannel( ) );
    writeFuture.addListener( ChannelFutureListener.CLOSE );
    response.addHeader( HttpHeaders.Names.CONNECTION, HttpHeaders.Values.CLOSE );
    if ( ctx.getChannel( ).isConnected( ) ) {
      Channels.write( ctx, writeFuture, response );
    }
  }
}
