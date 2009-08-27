/*******************************************************************************
*Copyright (c) 2009  Eucalyptus Systems, Inc.
* 
*  This program is free software: you can redistribute it and/or modify
*  it under the terms of the GNU General Public License as published by
*  the Free Software Foundation, only version 3 of the License.
* 
* 
*  This file is distributed in the hope that it will be useful, but WITHOUT
*  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
*  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
*  for more details.
* 
*  You should have received a copy of the GNU General Public License along
*  with this program.  If not, see <http://www.gnu.org/licenses/>.
* 
*  Please contact Eucalyptus Systems, Inc., 130 Castilian
*  Dr., Goleta, CA 93101 USA or visit <http://www.eucalyptus.com/licenses/>
*  if you need additional information or have any questions.
* 
*  This file may incorporate work covered under the following copyright and
*  permission notice:
* 
*    SOFTWARE, AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
*    IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA, SANTA
*    BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY, WHICH IN
*    THE REGENTS’ DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
*    OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
*    WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
*    ANY SUCH LICENSES OR RIGHTS.
 ******************************************************************************/
package com.eucalyptus.ws.server;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import org.apache.log4j.Logger;
import org.jboss.netty.buffer.ChannelBuffers;
import static org.jboss.netty.channel.Channels.*;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineCoverage;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.DownstreamMessageEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.handler.codec.frame.TooLongFrameException;
import org.jboss.netty.handler.codec.http.DefaultHttpResponse;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpMethod;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpRequestDecoder;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponseEncoder;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.jboss.netty.handler.codec.http.HttpVersion;
import org.jboss.netty.handler.stream.ChunkedFile;
import org.jboss.netty.handler.stream.ChunkedWriteHandler;

import com.eucalyptus.ws.MappingHttpRequest;
import com.eucalyptus.ws.util.PipelineRegistry;

@ChannelPipelineCoverage( "one" )
public class NioServerHandler extends SimpleChannelUpstreamHandler {
  private static Logger LOG   = Logger.getLogger( NioServerHandler.class );
  private boolean       first = true;

  @Override
  public void messageReceived( final ChannelHandlerContext ctx, final MessageEvent e ) throws Exception {
    if ( this.first ) {
      lookupPipeline( ctx, e );
    } else if( e.getMessage( ) instanceof MappingHttpRequest ) {
      LOG.warn( "Hard close the socket on an attempt to do a second request." );//TODO: fix Keep-Alive support
      ctx.getChannel( ).close( );
    }
    ctx.sendUpstream( e );
  }
  
  private void lookupPipeline( final ChannelHandlerContext ctx, final MessageEvent e ) throws DuplicatePipelineException, NoAcceptingPipelineException {
    try {
      final HttpRequest request = ( HttpRequest ) e.getMessage( );
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

  /*@Override
  public void exceptionCaught( final ChannelHandlerContext ctx, final ExceptionEvent e ) throws Exception {
    final Channel ch = e.getChannel( );
    final Throwable cause = e.getCause( );
    if ( cause instanceof TooLongFrameException ) {
      this.sendError( ctx, HttpResponseStatus.BAD_REQUEST );
      return;
    }
    cause.printStackTrace( );
    if ( ch.isConnected( ) ) {
      this.sendError( ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR );
    }
    ch.close();
  }*/

  private void sendError( final ChannelHandlerContext ctx, final HttpResponseStatus status ) {
    final HttpResponse response = new DefaultHttpResponse( HttpVersion.HTTP_1_1, status );
    response.setHeader( HttpHeaders.Names.CONTENT_TYPE, "text/plain; charset=UTF-8" );
    response.setContent( ChannelBuffers.copiedBuffer( "Failure: " + status.toString( ) + "\r\n", "UTF-8" ) );
    DownstreamMessageEvent newEvent = new DownstreamMessageEvent( ctx.getChannel( ), ctx.getChannel().getCloseFuture(), response, null );
    ctx.sendDownstream( newEvent );
    newEvent.getFuture( ).addListener( ChannelFutureListener.CLOSE );
  }
}
