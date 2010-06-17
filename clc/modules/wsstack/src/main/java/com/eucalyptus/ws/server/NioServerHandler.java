/*******************************************************************************
 *Copyright (c) 2009 Eucalyptus Systems, Inc.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, only version 3 of the License.
 * 
 * 
 * This file is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 * 
 * You should have received a copy of the GNU General Public License along
 * with this program. If not, see <http://www.gnu.org/licenses/>.
 * 
 * Please contact Eucalyptus Systems, Inc., 130 Castilian
 * Dr., Goleta, CA 93101 USA or visit <http://www.eucalyptus.com/licenses/>
 * if you need additional information or have any questions.
 * 
 * This file may incorporate work covered under the following copyright and
 * permission notice:
 * 
 * Software License Agreement (BSD License)
 * 
 * Copyright (c) 2008, Regents of the University of California
 * All rights reserved.
 * 
 * Redistribution and use of this software in source and binary forms, with
 * or without modification, are permitted provided that the following
 * conditions are met:
 * 
 * Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 * 
 * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
 * OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. USERS OF
 * THIS SOFTWARE ACKNOWLEDGE THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE
 * LICENSED MATERIAL, COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS
 * SOFTWARE, AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 * IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA, SANTA
 * BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY, WHICH IN
 * THE REGENTS’ DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
 * OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
 * WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
 * ANY SUCH LICENSES OR RIGHTS.
 *******************************************************************************/
/*
 * Author: chris grzegorczyk <grze@eucalyptus.com>
 */
package com.eucalyptus.ws.server;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import javax.security.auth.login.LoginException;
import org.apache.log4j.Logger;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandler;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
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
import org.jboss.netty.util.DebugUtil;
import com.eucalyptus.bootstrap.Bootstrap;
import com.eucalyptus.http.MappingHttpRequest;
import com.eucalyptus.system.LogLevels;
import com.eucalyptus.ws.ServiceNotReadyException;
import com.eucalyptus.ws.WebServicesException;
import com.eucalyptus.ws.util.PipelineRegistry;

@ChannelPipelineCoverage( "one" )
public class NioServerHandler extends SimpleChannelUpstreamHandler {
  private static Logger LOG   = Logger.getLogger( NioServerHandler.class );
  private boolean       first = true;
  
  @Override
  public void messageReceived( final ChannelHandlerContext ctx, final MessageEvent e ) throws Exception {
    if ( !Bootstrap.isFinished( ) ) {
      throw new ServiceNotReadyException( "System has not yet completed booting." );
    } else if ( this.first ) {
      lookupPipeline( ctx, e );
    } else if ( e.getMessage( ) instanceof MappingHttpRequest ) {
      MappingHttpRequest httpRequest = ( MappingHttpRequest ) e.getMessage( );
      if ( httpRequest.getProtocolVersion( ).equals( HttpVersion.HTTP_1_1 )
           || ( httpRequest.getProtocolVersion( ).equals( HttpVersion.HTTP_1_0 ) && HttpHeaders.Values.KEEP_ALIVE
                                                                                                                 .equalsIgnoreCase( httpRequest
                                                                                                                                               .getHeader( HttpHeaders.Names.CONNECTION ) ) ) ) {
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
    ctx.sendUpstream( e );
  }
  
  private void lookupPipeline( final ChannelHandlerContext ctx, final MessageEvent e ) throws DuplicatePipelineException, NoAcceptingPipelineException {
    try {
      final HttpRequest request = ( HttpRequest ) e.getMessage( );
      if ( LogLevels.TRACE ) {
        LOG.trace( request.getContent( ).toString( "UTF-8" ) );
      }
      final ChannelPipeline pipeline = ctx.getPipeline( );
      FilteredPipeline filteredPipeline = PipelineRegistry.getInstance( ).find( request );
      filteredPipeline.unroll( pipeline );
      this.first = false;
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
    if ( cause instanceof ReadTimeoutException ) {
      LOG.debug( cause, cause );
      this.sendError( ctx, HttpResponseStatus.REQUEST_TIMEOUT, cause );
    } else if ( cause instanceof WriteTimeoutException ) {
      LOG.debug( cause, cause );
      Channels.fireExceptionCaught( ctx, cause );
      ctx.getChannel( ).close( );
    } else if ( cause instanceof TooLongFrameException ) {
      this.sendError( ctx, HttpResponseStatus.BAD_REQUEST, cause );
    } else if ( cause instanceof IllegalArgumentException ) {
      this.sendError( ctx, HttpResponseStatus.BAD_REQUEST, cause );
    } else if ( cause instanceof LoginException ) {
      this.sendError( ctx, HttpResponseStatus.FORBIDDEN, cause );
    } else {
      LOG.error( "Internal Error.", cause );
      if ( ch.isConnected( ) ) {
        if ( e.getCause( ) instanceof WebServicesException ) {
          this.sendError( ctx, ( ( WebServicesException ) e.getCause( ) ).getStatus( ), cause );
        } else {
          this.sendError( ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR, cause );
        }
      }
    }
  }
  
  private void sendError( final ChannelHandlerContext ctx, final HttpResponseStatus status, Throwable t ) {
    final HttpResponse response = new DefaultHttpResponse( HttpVersion.HTTP_1_1, status );
    response.setHeader( HttpHeaders.Names.CONTENT_TYPE, "text/plain; charset=UTF-8" );
    if ( LogLevels.DEBUG ) {
      ByteArrayOutputStream os = new ByteArrayOutputStream( );
      PrintWriter out = new PrintWriter( os );
      t.printStackTrace( out );
      response.setContent( ChannelBuffers.copiedBuffer( "Failure: " + status.toString( ) + "\r\n" + t.getMessage( ) + "\r\n" + os.toString( ) + "\r\n", "UTF-8" ) );      
    } else {
      response.setContent( ChannelBuffers.copiedBuffer( "Failure: " + status.toString( ) + "\r\n" + t.getMessage( ), "UTF-8" ) );
    }
    ChannelFuture writeFuture = Channels.future( ctx.getChannel( ) );
    writeFuture.addListener( ChannelFutureListener.CLOSE );
    Channels.write( ctx, writeFuture, response );
    //    ctx.getChannel( ).write( response ).addListener( ChannelFutureListener.CLOSE );
  }
}
