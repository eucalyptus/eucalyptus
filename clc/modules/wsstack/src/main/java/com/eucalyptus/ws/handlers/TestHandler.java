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
