package edu.ucsb.eucalyptus.transport.client;

import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.handler.codec.http.HttpRequest;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

public class NioClient {

  private ClientBootstrap clientBootstrap;
  private NioClientPipeline clientPipeline;
  private NioResponseHandler responseHandler;

  private ChannelFactory channelFactory;
  private ChannelFuture channelOpenFuture;
  private ChannelFuture channelWriteFuture;
  private Channel channel;

  public NioClient( String hostname, int port ) {
    this.channelFactory = new NioClientSocketChannelFactory( Executors.newCachedThreadPool(), Executors.newCachedThreadPool() );
    this.clientBootstrap = new ClientBootstrap( channelFactory );
    this.responseHandler = new NioResponseHandler();
    this.clientPipeline = new NioClientPipeline( this.responseHandler );
    this.clientBootstrap.setPipelineFactory( clientPipeline );
    this.channelOpenFuture = this.clientBootstrap.connect( new InetSocketAddress( hostname, port ) );
  }

  private void open() throws Throwable {
    this.channel = this.channelOpenFuture.awaitUninterruptibly().getChannel();
    if ( !this.channelOpenFuture.isSuccess() ) {
      Throwable cause = this.channelOpenFuture.getCause();
      this.channelFactory.releaseExternalResources();
      throw cause;
    }
  }
  
  public ChannelFuture write( HttpRequest httpRequest ) throws Throwable {
    if ( this.channel == null || !this.channel.isOpen() ) {
      this.open();
    }
    if ( this.channel.isConnected() ) {
      return this.channelWriteFuture = this.channel.write( httpRequest );
    } else {
      throw new IOException( "Channel is open but not connected." );
    }
  }

  private void close() {
    if ( !this.channelWriteFuture.isDone() ) {
      this.channelWriteFuture.awaitUninterruptibly();
    }
    this.channel.getCloseFuture().awaitUninterruptibly();
  }

  public void cleanup() {
    if ( this.channel != null && this.channel.isOpen() ) { this.close(); }
    this.channelFactory.releaseExternalResources();
  }

}
