package com.eucalyptus.ws.server;

import static org.jboss.netty.channel.Channels.pipeline;

import java.io.ByteArrayInputStream;
import java.net.InetSocketAddress;
import java.util.Properties;
import java.util.Map.Entry;
import java.util.concurrent.Executors;

import org.apache.log4j.Logger;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelDownstreamHandler;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineCoverage;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.ChannelUpstreamHandler;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.jboss.netty.handler.codec.http.DefaultHttpResponse;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpRequestDecoder;
import org.jboss.netty.handler.codec.http.HttpResponseEncoder;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.jboss.netty.handler.stream.ChunkedWriteHandler;

import com.eucalyptus.bootstrap.Bootstrapper;
import com.eucalyptus.bootstrap.Component;
import com.eucalyptus.bootstrap.Depends;
import com.eucalyptus.bootstrap.Provides;
import com.eucalyptus.bootstrap.Resource;
@Provides( resource = Resource.RemoteConfiguration ) 
@Depends( remote = Component.eucalyptus )
@ChannelPipelineCoverage("all")
public class RemoteBootstrapperServer extends Bootstrapper implements ChannelDownstreamHandler, ChannelUpstreamHandler, ChannelPipelineFactory {
  private static Logger                 LOG            = Logger.getLogger( RemoteBootstrapperServer.class );
  private int                           port;
  private ServerBootstrap               bootstrap;
  private NioServerSocketChannelFactory socketFactory;
  private int                           BOOTSTRAP_PORT = 19191;
  private Channel                       channel;

  public RemoteBootstrapperServer( ) {
    this.port = BOOTSTRAP_PORT;
    this.socketFactory = new NioServerSocketChannelFactory( Executors.newCachedThreadPool( ), Executors.newCachedThreadPool( ) );
    this.bootstrap = new ServerBootstrap( this.socketFactory );
    this.bootstrap.setPipelineFactory( this );
  }

  public boolean start( ) {
    return true;
  }

  @Override
  public boolean load( Resource current ) throws Exception {
    this.channel = this.bootstrap.bind( new InetSocketAddress( this.port ) );
    LOG.info( "Waiting for system properties before continuing bootstrap.");
    this.channel.getCloseFuture( ).awaitUninterruptibly( );
    LOG.info( "Channel closed, proceeding with bootstrap.");
    return false;
  }

  @Override
  public void handleDownstream( ChannelHandlerContext ctx, ChannelEvent e ) throws Exception {
    ctx.sendDownstream( e );
  }

  @Override
  public void handleUpstream( ChannelHandlerContext ctx, ChannelEvent e ) throws Exception {
    if ( e instanceof MessageEvent ) {
      Object message = ( ( MessageEvent ) e ).getMessage( );
      if ( message instanceof HttpRequest ) {
        HttpRequest request = ( ( HttpRequest ) message );
        ByteArrayInputStream bis = new ByteArrayInputStream( request.getContent( ).toByteBuffer( ).array( ) );
        Properties props = new Properties( );
        props.load( bis );
        for ( Entry<Object, Object> entry : props.entrySet( ) ) {
          String oldValue = System.setProperty( ( String ) entry.getKey( ), ( String ) entry.getValue( ) );
          LOG.info( "Setting property: " + entry.getKey( ) + "=" + entry.getValue( ) + " [oldvalue="+oldValue+"]" );
        }
        ChannelFuture writeFuture = ctx.getChannel( ).write( new DefaultHttpResponse( request.getProtocolVersion( ), HttpResponseStatus.OK ) );
        writeFuture.addListener( ChannelFutureListener.CLOSE );
        this.channel.close( );
      }
    }
  }

  public ChannelPipeline getPipeline( ) throws Exception {
    ChannelPipeline pipeline = pipeline( );
    // TODO: handle security in here too.
    pipeline.addLast( "decoder", new HttpRequestDecoder( ) );
    pipeline.addLast( "encoder", new HttpResponseEncoder( ) );
    pipeline.addLast( "chunkedWriter", new ChunkedWriteHandler( ) );
    pipeline.addLast( "handler", this );
    return pipeline;
  }

}
