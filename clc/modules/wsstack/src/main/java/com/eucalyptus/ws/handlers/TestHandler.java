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
package com.eucalyptus.ws.handlers;

import java.io.FileNotFoundException;
import java.io.RandomAccessFile;

import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineCoverage;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;
import org.jboss.netty.handler.codec.http.DefaultHttpResponse;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpMethod;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.jboss.netty.handler.codec.http.HttpVersion;
import org.jboss.netty.handler.stream.ChunkedFile;

@ChannelPipelineCoverage("one")
public class TestHandler extends SimpleChannelHandler {

  public void unrollStage( ChannelPipeline pipeline ) {
    pipeline.addLast( "test", new TestHandler( ) );
  }

private static int count = 0;
  @Override
  public void messageReceived( ChannelHandlerContext ctx, MessageEvent e ) throws Exception {
    HttpRequest request = ( HttpRequest ) e.getMessage( );
    System.out.println( count++ + " -- " + request );
//    if ( request.getMethod( ) != HttpMethod.GET ) {
//      sendError( ctx, HttpResponseStatus.METHOD_NOT_ALLOWED );
//      return;
//    }

//    if ( request.isChunked( ) ) {
//      sendError( ctx, HttpResponseStatus.BAD_REQUEST );
//      return;
//    }

    String path = request.getUri( );
//    if ( path == null ) {
//      sendError( ctx, HttpResponseStatus.FORBIDDEN );
//      return;
//    }

    
//    RandomAccessFile raf;
//    try {
//      raf = new RandomAccessFile( file, "r" );
//    } catch ( FileNotFoundException fnfe ) {
//      sendError( ctx, HttpResponseStatus.NOT_FOUND );
//      return;
//    }
//    long fileLength = raf.length( );

    HttpResponse response = new DefaultHttpResponse( HttpVersion.HTTP_1_1, HttpResponseStatus.OK );
    String hi = "hello there:\n" + response.toString();
    long fileLength = hi.getBytes( ).length;
    response.setHeader( HttpHeaders.Names.CONTENT_LENGTH, String.valueOf( fileLength ) );

    Channel ch = e.getChannel( );

    // Write the initial line and the header.
    ch.write( response );

    ChannelFuture writeFuture = ch.write( "hello there:\n" + response );
    
    // Write the content.
//    ChannelFuture writeFuture = ch.write( new ChunkedFile( raf, 0, fileLength, 8192 ) );

    // Decide whether to close the connection or not.
    boolean close = HttpHeaders.Values.CLOSE.equalsIgnoreCase( request.getHeader( HttpHeaders.Names.CONNECTION ) )
        || request.getProtocolVersion( ).equals( HttpVersion.HTTP_1_0 )
        && !HttpHeaders.Values.KEEP_ALIVE.equalsIgnoreCase( request.getHeader( HttpHeaders.Names.CONNECTION ) );

    if ( close ) {
      // Close the connection when the whole content is written out.
      writeFuture.addListener( ChannelFutureListener.CLOSE );
    }
  }

  private void sendError( ChannelHandlerContext ctx, HttpResponseStatus status ) {
    HttpResponse response = new DefaultHttpResponse( HttpVersion.HTTP_1_1, status );
    response.setHeader( HttpHeaders.Names.CONTENT_TYPE, "text/plain; charset=UTF-8" );
    response.setContent( ChannelBuffers.copiedBuffer( "Failure: " + status.toString( ) + "\r\n", "UTF-8" ) );

    // Close the connection as soon as the error message is sent.
    ctx.getChannel( ).write( response ).addListener( ChannelFutureListener.CLOSE );
  }

}
