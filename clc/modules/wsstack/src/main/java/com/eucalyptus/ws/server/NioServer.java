package com.eucalyptus.ws.server;

import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;

import com.eucalyptus.ws.util.PipelineRegistry;

public class NioServer {

  private int                           port;
  private ServerBootstrap               bootstrap;
  private NioServerSocketChannelFactory socketFactory;

  public NioServer( int port ) {
    super( );
    this.port = port;
    this.socketFactory = new NioServerSocketChannelFactory( Executors.newCachedThreadPool( ), Executors.newCachedThreadPool( ) );
    this.bootstrap = new ServerBootstrap( this.socketFactory );
    this.bootstrap.setPipelineFactory( new NioServerPipelineFactory( ) );
    PipelineRegistry.getInstance( ).register( new EucalyptusSoapPipeline( ) );
    PipelineRegistry.getInstance( ).register( new EucalyptusQueryPipeline( ) );
    PipelineRegistry.getInstance( ).register( new WalrusRESTPipeline( ) );
  }

  public void start( ) {
    this.bootstrap.bind( new InetSocketAddress( this.port ) );
  }

}
