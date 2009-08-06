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

import com.eucalyptus.ws.util.PipelineRegistry;

@ChannelPipelineCoverage( "one" )
public class NioServerHandler extends SimpleChannelUpstreamHandler {
  private static Logger LOG   = Logger.getLogger( NioServerHandler.class );
  private boolean       first = true;

  @Override
  public void messageReceived( final ChannelHandlerContext ctx, final MessageEvent e ) throws Exception {
    if ( this.first ) {
      lookupPipeline( ctx, e );
      ctx.sendUpstream( e );
    } else {
      LOG.warn( "Hard close the socket on an attempt to do a second request." );//TODO: Keep-Alive support
      ctx.getChannel( ).close( );
    }
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

  @Override
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
  }

  private void sendError( final ChannelHandlerContext ctx, final HttpResponseStatus status ) {
    final HttpResponse response = new DefaultHttpResponse( HttpVersion.HTTP_1_1, status );
    response.setHeader( HttpHeaders.Names.CONTENT_TYPE, "text/plain; charset=UTF-8" );
    response.setContent( ChannelBuffers.copiedBuffer( "Failure: " + status.toString( ) + "\r\n", "UTF-8" ) );
    ctx.getChannel( ).write( response ).addListener( ChannelFutureListener.CLOSE );
  }
}
