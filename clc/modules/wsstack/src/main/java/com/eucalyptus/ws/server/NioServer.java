package com.eucalyptus.ws.server;

import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

import org.apache.log4j.Logger;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;

import com.eucalyptus.ws.util.PipelineRegistry;

public class NioServer {
  private static Logger                 LOG = Logger.getLogger( NioServer.class );
  private int                           port;
  private ServerBootstrap               bootstrap;
  private NioServerSocketChannelFactory socketFactory;

  public NioServer( int port ) {
    super( );
    this.port = port;
    this.socketFactory = new NioServerSocketChannelFactory( Executors.newCachedThreadPool( ), Executors.newCachedThreadPool( ) );
    this.bootstrap = new ServerBootstrap( this.socketFactory );
    LOG.info( "Server bootstrap options:" );
    this.bootstrap.setOption( "child.tcpNoDelay", true );
    this.bootstrap.setOption( "child.reuseAddress", false );
    this.bootstrap.setOption( "child.keepAlive", true );
    for ( String key : this.bootstrap.getOptions( ).keySet( ) ) {
      Object value = this.bootstrap.getOption( key );
      LOG.info( String.format( "== %20s %s", key, value ) );
    }
    this.bootstrap.setPipelineFactory( new NioServerPipelineFactory( ) );
    PipelineRegistry.getInstance( ).register( new EucalyptusSoapPipeline( ) );
    PipelineRegistry.getInstance( ).register( new ElasticFoxPipeline( ) );
    PipelineRegistry.getInstance( ).register( new EucalyptusQueryPipeline( ) );
    PipelineRegistry.getInstance( ).register( new WalrusRESTPipeline( ) );
  }

  public void start( ) {
    this.bootstrap.bind( new InetSocketAddress( this.port ) );
  }

}
