package com.eucalyptus.ws.client;

import org.apache.log4j.Logger;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.handler.codec.http.HttpMethod;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpVersion;

import com.eucalyptus.ws.MappingHttpRequest;
import com.eucalyptus.ws.client.pipeline.NioClientPipeline;
import com.eucalyptus.ws.handlers.MessageStackHandler;

import edu.ucsb.eucalyptus.msgs.EucalyptusMessage;

import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

public class NioClient implements Client {
  private static Logger LOG = Logger.getLogger( NioClient.class );

  private NioBootstrap clientBootstrap;

  private ChannelFactory channelFactory;
  private ChannelFuture channelOpenFuture;
  private ChannelFuture channelWriteFuture;
  private Channel channel;
  private NioClientPipeline clientPipeline;


  String hostname;
  int port;
  String servicePath;
  InetSocketAddress remoteAddr;

  public NioClient( String hostname, int port, String servicePath, NioClientPipeline clientPipeline ) {
    this.channelFactory = new NioClientSocketChannelFactory( Executors.newCachedThreadPool(), Executors.newCachedThreadPool() );
    this.clientBootstrap = new NioBootstrap( channelFactory );
    this.clientBootstrap.setPipelineFactory( clientPipeline );
    this.clientPipeline = clientPipeline;
    this.remoteAddr = new InetSocketAddress( hostname, port );
    this.hostname = hostname;
    this.port = port;
    this.servicePath = servicePath;
  }

  public void write( HttpRequest httpRequest ) throws Exception {
    if ( this.channel == null || !this.channel.isOpen() || !this.channel.isConnected() ) {
      this.channelOpenFuture = this.clientBootstrap.connect( this.remoteAddr );
      this.channelOpenFuture.addListener( new DeferedWriter( httpRequest, this.clientPipeline.getHandler() ) );
    }
  }

  class DeferedWriter implements ChannelFutureListener {
    private HttpRequest httpRequest;
    private MessageStackHandler handler;

    DeferedWriter( final HttpRequest httpRequest, final MessageStackHandler handler ) {
      this.httpRequest = httpRequest;
      this.handler = handler;
    }

    @Override
    public void operationComplete( final ChannelFuture channelFuture ) throws Exception {
      if( channelFuture.isSuccess() ) {
        channel = channelFuture.getChannel();
        channelWriteFuture = channelFuture.getChannel().write( this.httpRequest );
      } else {
        this.handler.exceptionCaught( channelFuture.getCause() );
      }
    }
  }

  public void close() {
    if ( this.channelWriteFuture != null && !this.channelWriteFuture.isDone() ) {
      this.channelWriteFuture.awaitUninterruptibly();
    }
    this.channel.close();
    LOG.debug( "Forcing the channel to close." );
  }

  public void cleanup() {
    if ( this.channel != null ) { this.close(); }
    this.channelFactory.releaseExternalResources();
  }

  @Override
  public EucalyptusMessage send( final EucalyptusMessage msg ) throws Exception {
    HttpRequest request = new MappingHttpRequest( HttpVersion.HTTP_1_1, HttpMethod.POST, this.hostname, this.port, this.servicePath, msg );
    this.write( request );
    EucalyptusMessage response = this.clientPipeline.getHandler().getResponse();
    this.cleanup();
    return response;
  }

  @Override
  public void dispatch( final EucalyptusMessage msg ) throws Exception {
    HttpRequest request = new MappingHttpRequest( HttpVersion.HTTP_1_1, HttpMethod.POST, this.hostname, this.port, this.servicePath, msg );
    this.write( request );
    this.cleanup();
  }

  @Override
  public String getUri() {
    return "http://"+this.hostname+":"+this.port+(servicePath.startsWith( "/" )?"":"?")+servicePath;
  }
}
