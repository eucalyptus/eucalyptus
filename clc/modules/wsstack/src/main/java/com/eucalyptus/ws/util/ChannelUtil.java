package com.eucalyptus.ws.util;

import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.apache.log4j.Logger;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.jboss.netty.handler.codec.http.HttpResponseEncoder;
import org.jboss.netty.handler.execution.OrderedMemoryAwareThreadPoolExecutor;
import org.jboss.netty.handler.stream.ChunkedWriteHandler;
import org.jboss.netty.handler.timeout.IdleStateHandler;
import org.jboss.netty.handler.timeout.ReadTimeoutHandler;
import org.jboss.netty.handler.timeout.WriteTimeoutHandler;
import org.jboss.netty.util.HashedWheelTimer;
import com.eucalyptus.configurable.ConfigurableClass;
import com.eucalyptus.records.Logs;
import com.eucalyptus.system.Threads;
import com.eucalyptus.util.LogUtil;
import com.eucalyptus.ws.StackConfiguration;
import com.eucalyptus.ws.handlers.ChannelStateMonitor;
import com.eucalyptus.ws.handlers.http.NioHttpDecoder;
import com.eucalyptus.ws.handlers.http.NioSslHandler;
import com.eucalyptus.ws.server.NioServerHandler;

public class ChannelUtil {
  private static Logger        LOG                               = Logger.getLogger( ChannelUtil.class );
  static class NioServerPipelineFactory implements ChannelPipelineFactory {
    public ChannelPipeline getPipeline( ) throws Exception {
      final ChannelPipeline pipeline = Channels.pipeline( );
      pipeline.addLast( "ssl", new NioSslHandler( ) );
      //ChannelUtil.addPipelineMonitors( pipeline );
      pipeline.addLast( "decoder", new NioHttpDecoder( ) );
      pipeline.addLast( "encoder", new HttpResponseEncoder( ) );
      pipeline.addLast( "chunkedWriter", new ChunkedWriteHandler( ) );
      pipeline.addLast( "handler", new NioServerHandler( ) );
      return pipeline;
    }
  }
  
  static class SystemThreadFactory implements ThreadFactory {
    @Override
    public Thread newThread( final Runnable r ) {
      return Threads.newThread( r, "channels" );
    }
  }
  
  private static Lock                   canHas = new ReentrantLock( );
  private static ThreadFactory          systemThreadFactory;
  private static ChannelPipelineFactory serverPipelineFactory;
  /** order from here really matters. no touchy. **/
  private static ExecutorService        serverBossThreadPool;
  private static ExecutorService        serverWorkerThreadPool;
  private static ChannelFactory         serverSocketFactory;
  private static ExecutorService        clientWorkerThreadPool;
  private static ExecutorService        clientBossThreadPool;
  private static ChannelFactory         clientSocketFactory;
  private static HashedWheelTimer       timer;
  
  public static void setupServer( ) {
    canHas.lock( );
    try {
      if ( systemThreadFactory == null ) {
        systemThreadFactory = ChannelUtil.getSystemThreadFactory( );
        serverPipelineFactory = ChannelUtil.getServerPipeline( );
        serverBossThreadPool = ChannelUtil.getServerBossThreadPool( );
        serverWorkerThreadPool = ChannelUtil.getServerWorkerThreadPool( );
        serverSocketFactory = ChannelUtil.getServerSocketChannelFactory( );
        clientWorkerThreadPool = ChannelUtil.getClientWorkerThreadPool( );
        clientBossThreadPool = ChannelUtil.getClientBossThreadPool( );
        clientSocketFactory = ChannelUtil.getClientChannelFactory( );
        timer = new HashedWheelTimer( );
      }
    } finally {
      canHas.unlock( );
    }
  }
  
  public static ChannelPipeline addPipelineMonitors( final ChannelPipeline pipeline ) {
    return ChannelUtil.addPipelineMonitors( pipeline, 120 );
  }
  
  public static ChannelPipeline addPipelineMonitors( ChannelPipeline pipeline, int i ) {
    pipeline.addLast( "state-monitor", new ChannelStateMonitor( ) );
    pipeline.addLast( "idlehandler", new IdleStateHandler( ChannelUtil.timer, i, i, i ) );
    pipeline.addLast( "readTimeout", new ReadTimeoutHandler( ChannelUtil.timer, i, TimeUnit.SECONDS ) );
    pipeline.addLast( "writeTimeout", new WriteTimeoutHandler( ChannelUtil.timer, i, TimeUnit.SECONDS ) );
    return pipeline;
  }
  
  private static ExecutorService getClientWorkerThreadPool( ) {
    canHas.lock( );
    try {
      if ( clientWorkerThreadPool == null ) {
        LOG.trace( LogUtil.subheader( "Creating client worker thread pool." ) );
        LOG.trace( String.format( "-> Pool threads:              %8d", StackConfiguration.CLIENT_POOL_MAX_THREADS ) );
        LOG.trace( String.format( "-> Pool timeout:              %8d ms", StackConfiguration.CLIENT_POOL_TIMEOUT_MILLIS ) );
        LOG.trace( String.format( "-> Max memory per connection: %8.2f MB", StackConfiguration.CLIENT_POOL_MAX_MEM_PER_CONN / ( 1024f * 1024f ) ) );
        LOG.trace( String.format( "-> Max total memory:          %8.2f MB", StackConfiguration.CLIENT_POOL_TOTAL_MEM / ( 1024f * 1024f ) ) );
        clientWorkerThreadPool = new OrderedMemoryAwareThreadPoolExecutor( StackConfiguration.CLIENT_POOL_MAX_THREADS, StackConfiguration.CLIENT_POOL_MAX_MEM_PER_CONN, StackConfiguration.CLIENT_POOL_TOTAL_MEM,
                                                                           StackConfiguration.CLIENT_POOL_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS );
      }
    } finally {
      canHas.unlock( );
    }
    return clientWorkerThreadPool;
  }
  
  private static ExecutorService getClientBossThreadPool( ) {
    canHas.lock( );
    try {
      if ( clientBossThreadPool == null ) {
        LOG.trace( LogUtil.subheader( "Creating client boss thread pool." ) );
        LOG.trace( String.format( "-> Pool threads:              %8d", StackConfiguration.CLIENT_POOL_MAX_THREADS ) );
        LOG.trace( String.format( "-> Pool timeout:              %8d ms", StackConfiguration.CLIENT_POOL_TIMEOUT_MILLIS ) );
        LOG.trace( String.format( "-> Max memory per connection: %8.2f MB", StackConfiguration.CLIENT_POOL_MAX_MEM_PER_CONN / ( 1024f * 1024f ) ) );
        LOG.trace( String.format( "-> Max total memory:          %8.2f MB", StackConfiguration.CLIENT_POOL_TOTAL_MEM / ( 1024f * 1024f ) ) );
        clientBossThreadPool = new OrderedMemoryAwareThreadPoolExecutor( StackConfiguration.CLIENT_POOL_MAX_THREADS, StackConfiguration.CLIENT_POOL_MAX_MEM_PER_CONN, StackConfiguration.CLIENT_POOL_TOTAL_MEM,
                                                                         StackConfiguration.CLIENT_POOL_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS );
      }
    } finally {
      canHas.unlock( );
    }
    return clientBossThreadPool;
  }
  
  private static ExecutorService getServerBossThreadPool( ) {
    canHas.lock( );
    try {
      if ( serverBossThreadPool == null ) {
        if ( !Logs.isExtrrreeeme() ) {
          LOG.info( "Creating server boss thread pool. (log level EXTREME for details)" );
        } else {
          LOG.trace( LogUtil.subheader( "Creating server boss thread pool." ) );
          LOG.trace( String.format( "-> Pool threads:              %8d", StackConfiguration.SERVER_BOSS_POOL_MAX_THREADS ) );
          LOG.trace( String.format( "-> Pool timeout:              %8d ms", StackConfiguration.SERVER_BOSS_POOL_TIMEOUT_MILLIS ) );
          LOG.trace( String.format( "-> Max memory per connection: %8.2f MB", StackConfiguration.SERVER_BOSS_POOL_MAX_MEM_PER_CONN / ( 1024f * 1024f ) ) );
          LOG.trace( String.format( "-> Max total memory:          %8.2f MB", StackConfiguration.SERVER_BOSS_POOL_TOTAL_MEM / ( 1024f * 1024f ) ) );
        }
        serverBossThreadPool = new OrderedMemoryAwareThreadPoolExecutor( StackConfiguration.SERVER_BOSS_POOL_MAX_THREADS, StackConfiguration.SERVER_BOSS_POOL_MAX_MEM_PER_CONN,
                                                                         StackConfiguration.SERVER_BOSS_POOL_TOTAL_MEM, StackConfiguration.SERVER_BOSS_POOL_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS );
      }
    } finally {
      canHas.unlock( );
    }
    return serverBossThreadPool;
  }
  
  private static ExecutorService getServerWorkerThreadPool( ) {
    canHas.lock( );
    try {
      if ( serverWorkerThreadPool == null ) {
        if ( !Logs.isExtrrreeeme() ) {
          LOG.info( "Creating server worker thread pool. (log level EXTREME for details)" );
        } else {
          LOG.trace( LogUtil.subheader( "Creating server worker thread pool." ) );
          LOG.trace( String.format( "-> Pool threads:              %8d", StackConfiguration.SERVER_POOL_MAX_THREADS ) );
          LOG.trace( String.format( "-> Pool timeout:              %8d ms", StackConfiguration.SERVER_POOL_TIMEOUT_MILLIS ) );
          LOG.trace( String.format( "-> Max memory per connection: %8.2f MB", StackConfiguration.SERVER_POOL_MAX_MEM_PER_CONN / ( 1024f * 1024f ) ) );
          LOG.trace( String.format( "-> Max total memory:          %8.2f MB", StackConfiguration.SERVER_POOL_TOTAL_MEM / ( 1024f * 1024f ) ) );
        }
        serverWorkerThreadPool = new OrderedMemoryAwareThreadPoolExecutor( StackConfiguration.SERVER_POOL_MAX_THREADS, StackConfiguration.SERVER_POOL_MAX_MEM_PER_CONN, StackConfiguration.SERVER_POOL_TOTAL_MEM,
                                                                           StackConfiguration.SERVER_POOL_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS );
      }
    } finally {
      canHas.unlock( );
    }
    return serverWorkerThreadPool;
  }
  
  public static NioBootstrap getClientBootstrap( ChannelPipelineFactory factory ) {
    final NioBootstrap bootstrap = new NioBootstrap( ChannelUtil.getClientChannelFactory( ) );//TODO: pass port host, etc here.
    bootstrap.setPipelineFactory( factory );
    bootstrap.setOption( "tcpNoDelay", false );
    bootstrap.setOption( "keepAlive", false );
    bootstrap.setOption( "reuseAddress", false );
    bootstrap.setOption( "connectTimeoutMillis", 3000 );
    return bootstrap;
  }
  
  public static ServerBootstrap getServerBootstrap( ) {
    ChannelUtil.setupServer( );
    final ServerBootstrap bootstrap = new ServerBootstrap( ChannelUtil.getServerSocketChannelFactory( ) );
    bootstrap.setPipelineFactory( ChannelUtil.getServerPipeline( ) );
    if ( !Logs.isExtrrreeeme() ) {
      LOG.info( "Creating server bootstrap. (log level EXTREME for details)" );
    } else {
      LOG.trace( LogUtil.subheader( "Creating server boss thread pool." ) );
      LOG.trace( String.format( "-> Server option: %25.25s = %s", "child.tcpNoDelay", StackConfiguration.CHANNEL_NODELAY ) );
      LOG.trace( String.format( "-> Server option: %25.25s = %s", "child.keepAlive", StackConfiguration.CHANNEL_KEEP_ALIVE ) );
      LOG.trace( String.format( "-> Server option: %25.25s = %s", "child.reuseAddress", StackConfiguration.CHANNEL_REUSE_ADDRESS ) );
      LOG.trace( String.format( "-> Server option: %25.25s = %s", "child.connectTimeoutMillis", StackConfiguration.CHANNEL_CONNECT_TIMEOUT ) );
      LOG.trace( String.format( "-> Server option: %25.25s = %s", "tcpNoDelay", StackConfiguration.SERVER_CHANNEL_NODELAY ) );
      LOG.trace( String.format( "-> Server option: %25.25s = %s", "reuseAddress", StackConfiguration.SERVER_CHANNEL_REUSE_ADDRESS ) );
    }
    bootstrap.setOption( "child.tcpNoDelay", StackConfiguration.CHANNEL_NODELAY );
    bootstrap.setOption( "child.keepAlive", StackConfiguration.CHANNEL_KEEP_ALIVE );
    bootstrap.setOption( "child.reuseAddress", StackConfiguration.CHANNEL_REUSE_ADDRESS );
    bootstrap.setOption( "child.connectTimeoutMillis", StackConfiguration.CHANNEL_CONNECT_TIMEOUT );
    bootstrap.setOption( "tcpNoDelay", StackConfiguration.SERVER_CHANNEL_NODELAY );
    bootstrap.setOption( "reuseAddress", StackConfiguration.SERVER_CHANNEL_REUSE_ADDRESS );
    return bootstrap;
  }
  
  public static Channel getServerChannel( ) {
    return ChannelUtil.getServerBootstrap( ).bind( new InetSocketAddress( StackConfiguration.PORT ) );
  }
  
  public static ChannelPipelineFactory getServerPipeline( ) {
    canHas.lock( );
    try {
      if ( serverPipelineFactory == null ) {
        serverPipelineFactory = new NioServerPipelineFactory( );
      }
    } finally {
      canHas.unlock( );
    }
    return serverPipelineFactory;
  }
  
  private static ChannelFactory getClientChannelFactory( ) {
    canHas.lock( );
    try {
      if ( clientSocketFactory == null ) {
        clientSocketFactory = new NioClientSocketChannelFactory( ChannelUtil.getClientBossThreadPool( ), ChannelUtil.getClientWorkerThreadPool( ),
                                                                 StackConfiguration.CLIENT_POOL_MAX_THREADS );
      }
    } finally {
      canHas.unlock( );
    }
    return clientSocketFactory;
  }
  
  private static ChannelFactory getServerSocketChannelFactory( ) {
    canHas.lock( );
    try {
      if ( serverSocketFactory == null ) {
        serverSocketFactory = new NioServerSocketChannelFactory( ChannelUtil.getServerBossThreadPool( ), ChannelUtil.getServerWorkerThreadPool( ),
                                                                 StackConfiguration.SERVER_POOL_MAX_THREADS );
      }
    } finally {
      canHas.unlock( );
    }
    return serverSocketFactory;
  }
  
  private static ThreadFactory getSystemThreadFactory( ) {
    canHas.lock( );
    try {
      if ( systemThreadFactory == null ) {
        systemThreadFactory = new SystemThreadFactory( );
      }
    } finally {
      canHas.unlock( );
    }
    return systemThreadFactory;
  }
  
  public static ChannelFutureListener DISPATCH( Object o ) {
    return new DeferedWriter( o, ChannelFutureListener.CLOSE );
  }
  
  public static ChannelFutureListener WRITE_AND_CALLBACK( Object o, ChannelFutureListener callback ) {
    return new DeferedWriter( o, callback );
  }
  
  public static ChannelFutureListener WRITE( Object o ) {
    return new DeferedWriter( o, new ChannelFutureListener( ) {
      public void operationComplete( ChannelFuture future ) throws Exception {}
    } );
  }
  
  private static class DeferedWriter implements ChannelFutureListener {
    private Object                request;
    private ChannelFutureListener callback;
    
    DeferedWriter( final Object request, final ChannelFutureListener callback ) {
      this.callback = callback;
      this.request = request;
    }
    
    @Override
    public void operationComplete( ChannelFuture channelFuture ) {
      if ( channelFuture.isSuccess( ) ) {
        channelFuture.getChannel( ).write( request ).addListener( callback );
      } else {
        LOG.debug( channelFuture.getCause( ), channelFuture.getCause( ) );
        try {
          callback.operationComplete( channelFuture );
        } catch ( Exception e ) {
          LOG.debug( e, e );
        }
      }
    }
    
  }
  
}
