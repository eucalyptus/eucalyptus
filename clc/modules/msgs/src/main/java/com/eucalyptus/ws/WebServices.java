/*************************************************************************
 * Copyright 2008 Regents of the University of California
 * Copyright 2009-2012 Ent. Services Development Corporation LP
 *
 * Redistribution and use of this software in source and binary forms,
 * with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *   Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 *   Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer
 *   in the documentation and/or other materials provided with the
 *   distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 * THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 * COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 * AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 * IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 * SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 * WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 * REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 * IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 * NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/

package com.eucalyptus.ws;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.annotation.Nullable;
import com.eucalyptus.component.ComponentId;
import com.eucalyptus.component.ComponentIds;
import com.eucalyptus.component.Components;
import com.eucalyptus.event.EventListener;
import com.eucalyptus.event.Hertz;
import com.eucalyptus.event.Listeners;
import org.apache.log4j.Logger;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelException;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelHandler;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.SimpleChannelHandler;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.group.DefaultChannelGroup;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.jboss.netty.handler.execution.OrderedMemoryAwareThreadPoolExecutor;
import org.jboss.netty.logging.InternalLoggerFactory;
import org.jboss.netty.logging.Log4JLoggerFactory;
import org.jboss.netty.util.ExternalResourceReleasable;
import org.jboss.netty.util.ExternalResourceUtil;
import org.jboss.netty.util.ThreadNameDeterminer;
import org.jboss.netty.util.ThreadRenamingRunnable;
import com.eucalyptus.bootstrap.Bootstrap;
import com.eucalyptus.bootstrap.Bootstrapper;
import com.eucalyptus.bootstrap.OrderedShutdown;
import com.eucalyptus.bootstrap.Provides;
import com.eucalyptus.bootstrap.RunDuring;
import com.eucalyptus.configurable.ConfigurableProperty;
import com.eucalyptus.configurable.ConfigurablePropertyException;
import com.eucalyptus.configurable.PropertyChangeListener;
import com.eucalyptus.empyrean.Empyrean;
import com.eucalyptus.records.Logs;
import com.eucalyptus.system.Threads;
import com.eucalyptus.util.Cidr;
import com.eucalyptus.util.CollectionUtils;
import com.eucalyptus.util.Consumer;
import com.eucalyptus.util.Consumers;
import com.eucalyptus.util.Internets;
import com.eucalyptus.util.LockResource;
import com.eucalyptus.util.LogUtil;
import com.eucalyptus.util.Pair;
import com.eucalyptus.util.Strings;
import com.google.common.base.CharMatcher;
import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.concurrent.Future;

public class WebServices {

  static {
    // Allow thread factories to determine thread naming
    ThreadRenamingRunnable.setThreadNameDeterminer( ThreadNameDeterminer.CURRENT );
  }

  @Provides( Empyrean.class )
  @RunDuring( Bootstrap.Stage.RemoteServicesInit )
  public static class WebServicesBootstrapper extends Bootstrapper.Simple {
    static {
      InternalLoggerFactory.setDefaultFactory( new Log4JLoggerFactory( ) );
    }
    private static final Consumer<Void> registerShutdownConsumer = Consumers.once( new Consumer<Void>() {
      @Override
      public void accept( final Void aVoid ) {
        OrderedShutdown.registerPostShutdownHook( new Runnable( ){
          @Override
          public void run( ) {
            LOG.info( "Releasing resources on shutdown" );
            try {
              final List<ExternalResourceReleasable> resources = Lists.<ExternalResourceReleasable>newArrayList(
                  Handlers.pipelineExecutionHandler( ),
                  Handlers.serviceExecutionHandler( )
              );
              ExternalResourceUtil.release( resources.toArray( new ExternalResourceReleasable[ resources.size( ) ] ) );
            } catch ( Throwable t ) {
              LOG.error( "Error releasing resources", t );
            }
          }

          @Override
          public String toString( ) {
            return "Web services resources";
          }
        } );
      }
    } );

    @Override
    public boolean load( ) throws Exception {
      WebServices.restart( );
      registerShutdownConsumer.accept( null );
      return true;
    }
  }

  @ChannelHandler.Sharable
  private static class ChannelGroupChannelHandler extends SimpleChannelHandler {
    private final ChannelGroup serverChannelGroup;

    public ChannelGroupChannelHandler( final ChannelGroup serverChannelGroup ) {
      this.serverChannelGroup = serverChannelGroup;
    }

    @Override
    public void channelOpen( ChannelHandlerContext ctx, ChannelStateEvent e ) throws Exception {
      serverChannelGroup.add( ctx.getChannel( ) );
      super.channelOpen( ctx, e );
    }
  }

  private static Iterable<Cidr> parse( final Function<String,Optional<Cidr>> cidrTransform,
                                       final String cidrList ) {
    return Optional.presentInstances( Iterables.transform(
        Splitter.on( CharMatcher.anyOf( ", ;:" ) ).trimResults().omitEmptyStrings().split( cidrList ),
        cidrTransform ) );
  }

  public static class CheckCidrListPropertyChangeListener implements PropertyChangeListener {
    @Override
    public void fireChange( final ConfigurableProperty t, final Object newValue ) throws ConfigurablePropertyException {
      if ( newValue != null ) try {
        parse(
            Functions.compose( CollectionUtils.<Cidr>optionalUnit(), Cidr.parseUnsafe( ) ),
            Objects.toString( newValue ) );
      } catch ( IllegalArgumentException e ) {
        throw new ConfigurablePropertyException( e.getMessage( ) );
      }
    }
  }

  public static class CheckNonNegativeIntegerPropertyChangeListener implements PropertyChangeListener {
    @Override
    public void fireChange( ConfigurableProperty t, Object newValue ) throws ConfigurablePropertyException {
      int value;
      try {
        value = Integer.parseInt((String) newValue);
      } catch (Exception ex) {
        throw new ConfigurablePropertyException("Invalid value " + newValue);
      }
      if (value < 0) {
        throw new ConfigurablePropertyException("Invalid value " + newValue);
      }
    }
  }

  public static class CheckNonNegativeLongPropertyChangeListener implements PropertyChangeListener {
    @Override
    public void fireChange( ConfigurableProperty t, Object newValue ) throws ConfigurablePropertyException {
      long value;
      try {
        value = Long.parseLong((String) newValue);
      } catch (Exception ex) {
        throw new ConfigurablePropertyException("Invalid value " + newValue);
      }
      if (value < 0) {
        throw new ConfigurablePropertyException("Invalid value " + newValue);
      }
    }
  }

  public static class CheckBooleanPropertyChangeListener implements PropertyChangeListener {
    @Override
    public void fireChange( ConfigurableProperty t, Object newValue ) throws ConfigurablePropertyException {
      if ((newValue == null) || (!((String) newValue).equalsIgnoreCase("true") && !((String) newValue).equalsIgnoreCase("false"))) {
        throw new ConfigurablePropertyException("Invalid value " + newValue);
      }
    }
  }

  public static class ComponentListPropertyChangeListener implements PropertyChangeListener {
    private static final Predicate<String> validComponentName = new Predicate<String>( ){
      @Override
      public boolean apply( @Nullable final String value ) {
        try {
          ComponentIds.lookup( value );
          return true;
        } catch ( NoSuchElementException e ) {
          return false;
        }
      }
    };

    @Override
    public void fireChange( ConfigurableProperty t, Object newValue ) throws ConfigurablePropertyException {
      if ( !"*".equals( String.valueOf( newValue ) ) &&
          !Iterables.all( iterableFromList( String.valueOf( newValue ) ), validComponentName  ) ) {
        throw new ConfigurablePropertyException("Invalid value " + newValue);
      }
    }
  }

  private static Logger   LOG = Logger.getLogger( WebServices.class );
  private static Lock clientResourceLock = new ReentrantLock( );
  private static EventLoopGroup clientEventLoopGroup;
  private static Runnable serverShutdown;
  
  public static io.netty.bootstrap.Bootstrap clientBootstrap( ) {
    final EventLoopGroup clientEventLoopGroup = clientEventLoopGroup( );
    final io.netty.bootstrap.Bootstrap bootstrap = clientBootstrap( clientEventLoopGroup );
    return bootstrap;
  }
  
  private static io.netty.bootstrap.Bootstrap clientBootstrap( final EventLoopGroup group ) {
    return new io.netty.bootstrap.Bootstrap( )
        .group( group )
        .channel( NioSocketChannel.class )
        .option( ChannelOption.TCP_NODELAY, true )
        .option( ChannelOption.SO_KEEPALIVE, true )
        .option( ChannelOption.SO_REUSEADDR, true )
        .option( ChannelOption.CONNECT_TIMEOUT_MILLIS, StackConfiguration.CLIENT_INTERNAL_CONNECT_TIMEOUT_MILLIS );
  }
  
  private static EventLoopGroup clientEventLoopGroup( ) {
    if ( clientEventLoopGroup != null ) {
      return clientEventLoopGroup;
    } else try ( final LockResource resourceLock = LockResource.lock( clientResourceLock ) ) {
      if ( clientEventLoopGroup != null ) {
        return clientEventLoopGroup;
      } else {
        final NioEventLoopGroup eventLoopGroup = new NioEventLoopGroup(
            StackConfiguration.CLIENT_POOL_MAX_THREADS,
            Threads.threadFactory( "web-services-client-pool-%d" )
        );
        OrderedShutdown.registerPostShutdownHook( ( ) -> {
          LOG.info( "Client shutdown requested" );
          try {
            final Future<?> terminationFuture = clientEventLoopGroup.shutdownGracefully( 0, 5, TimeUnit.SECONDS );
            terminationFuture.await( 10, TimeUnit.SECONDS );
            if ( terminationFuture.isDone( ) ) {
              LOG.info( "Client shutdown complete" );
            } else {
              LOG.warn( "Client shutdown timed out" );
            }
          } catch ( final InterruptedException e ) {
            LOG.info( "Client shutdown interrupted" );
          }
        } );
        return clientEventLoopGroup = eventLoopGroup;
      }
    }
  }
  
  public static synchronized void restart( ) {
    if ( serverShutdown != null ) {
      serverShutdown.run( );
      serverShutdown = null;
    }
    final Executor workerPool = workerPool( );
    final ChannelFactory serverChannelFactory = channelFactory( workerPool );
    final ChannelPipelineFactory serverPipelineFactory = Handlers.serverPipelineFactory( );
    final ChannelGroup serverChannelGroup = channelGroup( );
    final ChannelHandler channelGroupHandler = new ChannelGroupChannelHandler( serverChannelGroup );
    final ChannelPipelineFactory pipelineFactory = new ChannelPipelineFactory( ) {
      @Override
      public ChannelPipeline getPipeline( ) throws Exception {
        ChannelPipeline pipeline = serverPipelineFactory.getPipeline( );
        pipeline.addLast( "channel-group-handler", channelGroupHandler );
        return pipeline;
      }
    };
    final ServerBootstrap bootstrap = serverBootstrap( serverChannelFactory, pipelineFactory );

    final List<Pair<InetAddress,Integer>>  internalAddressAndPorts = Arrays.asList(
        Pair.pair( Internets.localHostInetAddress(), StackConfiguration.INTERNAL_PORT ),
        Pair.pair( Internets.loopback(), StackConfiguration.INTERNAL_PORT )
    );
    final Set<Pair<InetAddress,Integer>> listenerAddressAndPorts = Sets.newLinkedHashSet(  );
    listenerAddressAndPorts.addAll( internalAddressAndPorts );
    if ( Bootstrap.isOperational( ) ) { // skip additional listeners until bootstrapped
      Iterables.addAll(
          listenerAddressAndPorts,
          Iterables.transform(
              Iterables.filter(
                  Iterables.concat( Collections.singleton( Internets.any() ), Internets.getAllInetAddresses( ) ),
                  Predicates.or( parse( Cidr.parse( ), StackConfiguration.LISTENER_ADDRESS_MATCH ) ) ),
              CollectionUtils.flipCurried( Pair.<InetAddress,Integer>pair( ) ).apply( StackConfiguration.PORT ) ) );
    }
    if ( listenerAddressAndPorts.contains( Pair.pair( Internets.any( ), StackConfiguration.INTERNAL_PORT  ) ) ) {
      listenerAddressAndPorts.removeAll( internalAddressAndPorts );     }
    LOG.info( "Starting web services listeners on " + Joiner.on(',').join( Iterables.transform( listenerAddressAndPorts, Pair.<InetAddress, Integer>left() ) ) );
    for ( final Pair<InetAddress,Integer> listenerAddressAndPort : listenerAddressAndPorts ) {
      final InetAddress address = listenerAddressAndPort.getLeft( );
      final int port = listenerAddressAndPort.getRight( );
      try {
        final Channel serverChannel = bootstrap.bind( new InetSocketAddress( address, port ) );
        serverChannelGroup.add( serverChannel );
      } catch ( ChannelException ex ) {
        LOG.error( "Unable to bind web services listener " + address + ":" + port + ", port may be already in use." );
        Logs.extreme( ).error( ex, ex );
      }
    }
    try {
      serverShutdown = new Runnable( ) {
        AtomicBoolean ranned = new AtomicBoolean( false );
        
        @Override
        public void run( ) {
          if ( this.ranned.compareAndSet( false, true ) ) {
            LOG.info( "Server shutdown requested" );
            serverChannelGroup.close( ).awaitUninterruptibly();
            serverChannelFactory.releaseExternalResources( );
          } else {
            LOG.info( "Server shutdown skipped" );
          }
        }

        @Override
        public String toString( ) {
          return "Web services server shutdown";
        }
      };
      OrderedShutdown.registerPostShutdownHook( serverShutdown );
    } catch ( Exception ex ) {
      LOG.error( ex, ex );
    }
    
  }

  public static class WebServicePropertiesChangedEventListener implements EventListener<Hertz> {
    // These are all the properties in StackConfiguration that have the RestartWebServicesListener.
    private Integer       CHANNEL_CONNECT_TIMEOUT           = 500;
    private Boolean       SERVER_CHANNEL_REUSE_ADDRESS      = true;
    private Boolean       SERVER_CHANNEL_NODELAY            = true;
    private boolean       CHANNEL_REUSE_ADDRESS             = true;
    private Boolean       CHANNEL_KEEP_ALIVE                = true;
    private Boolean       CHANNEL_NODELAY                   = true;
    private Integer       SERVER_POOL_MAX_THREADS           = 128;
    private Long          SERVER_POOL_MAX_MEM_PER_CONN      = 0L;
    private Long          SERVER_POOL_TOTAL_MEM             = 0L;
    private Long          SERVER_POOL_TIMEOUT_MILLIS        = 500L;
    private Integer       PORT                              = 8773;
    private String        LISTENER_ADDRESS_MATCH            = "0.0.0.0";
    private AtomicBoolean isRunning = new AtomicBoolean(false);

    public static void register( ) {
      Listeners.register(Hertz.class, new WebServicePropertiesChangedEventListener());
    }

    @Override
    public void fireEvent( final Hertz event ) {
      if (Bootstrap.isOperational() && event.isAsserted( 60 ) && isRunning.compareAndSet(false, true)) {
        LOG.trace("Checking for updates to bootstrap.webservices properties");
        boolean different = false;
        // temp vars so only look at StackConfiguration.* once (in case they change in the meantime)
        Integer NEW_CHANNEL_CONNECT_TIMEOUT = StackConfiguration.CHANNEL_CONNECT_TIMEOUT;
        Boolean NEW_SERVER_CHANNEL_REUSE_ADDRESS = StackConfiguration.SERVER_CHANNEL_REUSE_ADDRESS;
        Boolean NEW_SERVER_CHANNEL_NODELAY = StackConfiguration.SERVER_CHANNEL_NODELAY;
        boolean NEW_CHANNEL_REUSE_ADDRESS = StackConfiguration.CHANNEL_REUSE_ADDRESS;
        Boolean NEW_CHANNEL_KEEP_ALIVE = StackConfiguration.CHANNEL_KEEP_ALIVE;
        Boolean NEW_CHANNEL_NODELAY = StackConfiguration.CHANNEL_NODELAY;
        Integer NEW_SERVER_POOL_MAX_THREADS = StackConfiguration.SERVER_POOL_MAX_THREADS;
        Long NEW_SERVER_POOL_MAX_MEM_PER_CONN = StackConfiguration.SERVER_POOL_MAX_MEM_PER_CONN;
        Long NEW_SERVER_POOL_TOTAL_MEM = StackConfiguration.SERVER_POOL_TOTAL_MEM;
        Long NEW_SERVER_POOL_TIMEOUT_MILLIS = StackConfiguration.SERVER_POOL_TIMEOUT_MILLIS;
        Integer NEW_PORT = StackConfiguration.PORT;
        String NEW_LISTENER_ADDRESS_MATCH = StackConfiguration.LISTENER_ADDRESS_MATCH;
        if (!CHANNEL_CONNECT_TIMEOUT.equals(NEW_CHANNEL_CONNECT_TIMEOUT)) {
          LOG.info("bootstrap.webservices.channel_connect_timeout has changed: oldValue = " + CHANNEL_CONNECT_TIMEOUT + ", newValue = " + NEW_CHANNEL_CONNECT_TIMEOUT);
          CHANNEL_CONNECT_TIMEOUT = NEW_CHANNEL_CONNECT_TIMEOUT;
          different = true;
        }
        if (SERVER_CHANNEL_REUSE_ADDRESS != NEW_SERVER_CHANNEL_REUSE_ADDRESS) {
          LOG.info("bootstrap.webservices.server_channel_reuse_address has changed: oldValue = " + SERVER_CHANNEL_REUSE_ADDRESS + ", newValue = " + NEW_SERVER_CHANNEL_REUSE_ADDRESS);
          SERVER_CHANNEL_REUSE_ADDRESS = NEW_SERVER_CHANNEL_REUSE_ADDRESS;
          different = true;
        }
        if (SERVER_CHANNEL_NODELAY != NEW_SERVER_CHANNEL_NODELAY) {
          LOG.info("bootstrap.webservices.server_channel_nodelay has changed: oldValue = " + SERVER_CHANNEL_NODELAY + ", newValue = " + NEW_SERVER_CHANNEL_NODELAY);
          SERVER_CHANNEL_NODELAY = NEW_SERVER_CHANNEL_NODELAY;
          different = true;
        }
        if (CHANNEL_REUSE_ADDRESS != NEW_CHANNEL_REUSE_ADDRESS) {
          LOG.info("bootstrap.webservices.channel_reuse_address has changed: oldValue = " + CHANNEL_REUSE_ADDRESS + ", newValue = " + NEW_CHANNEL_REUSE_ADDRESS);
          CHANNEL_REUSE_ADDRESS = NEW_CHANNEL_REUSE_ADDRESS;
          different = true;
        }
        if (CHANNEL_KEEP_ALIVE != NEW_CHANNEL_KEEP_ALIVE) {
          LOG.info("bootstrap.webservices.channel_keep_alive has changed: oldValue = " + CHANNEL_KEEP_ALIVE + ", newValue = " + NEW_CHANNEL_KEEP_ALIVE);
          CHANNEL_KEEP_ALIVE = NEW_CHANNEL_KEEP_ALIVE;
          different = true;
        }
        if (CHANNEL_NODELAY != NEW_CHANNEL_NODELAY) {
          LOG.info("bootstrap.webservices.channel_nodelay has changed: oldValue = " + CHANNEL_NODELAY + ", newValue = " + NEW_CHANNEL_NODELAY);
          CHANNEL_NODELAY = NEW_CHANNEL_NODELAY;
          different = true;
        }
        if (!SERVER_POOL_MAX_THREADS.equals(NEW_SERVER_POOL_MAX_THREADS)) {
          LOG.info("bootstrap.webservices.server_pool_max_threads has changed: oldValue = " + SERVER_POOL_MAX_THREADS + ", newValue = " + NEW_SERVER_POOL_MAX_THREADS);
          SERVER_POOL_MAX_THREADS = NEW_SERVER_POOL_MAX_THREADS;
          different = true;
        }
        if (!SERVER_POOL_MAX_MEM_PER_CONN.equals(NEW_SERVER_POOL_MAX_MEM_PER_CONN)) {
          LOG.info("bootstrap.webservices.server_pool_max_mem_per_conn has changed: oldValue = " + SERVER_POOL_MAX_MEM_PER_CONN + ", newValue = " + NEW_SERVER_POOL_MAX_MEM_PER_CONN);
          SERVER_POOL_MAX_MEM_PER_CONN = NEW_SERVER_POOL_MAX_MEM_PER_CONN;
          different = true;
        }
        if (!SERVER_POOL_TOTAL_MEM.equals(NEW_SERVER_POOL_TOTAL_MEM)) {
          LOG.info("bootstrap.webservices.server_pool_total_mem has changed: oldValue = " + SERVER_POOL_TOTAL_MEM + ", newValue = " + NEW_SERVER_POOL_TOTAL_MEM);
          SERVER_POOL_TOTAL_MEM = NEW_SERVER_POOL_TOTAL_MEM;
          different = true;
        }
        if (!SERVER_POOL_TIMEOUT_MILLIS.equals(NEW_SERVER_POOL_TIMEOUT_MILLIS)) {
          LOG.info("bootstrap.webservices.server_pool_timeout_millis has changed: oldValue = " + SERVER_POOL_TIMEOUT_MILLIS + ", newValue = " + NEW_SERVER_POOL_TIMEOUT_MILLIS);
          SERVER_POOL_TIMEOUT_MILLIS = NEW_SERVER_POOL_TIMEOUT_MILLIS;
          different = true;
        }
        if (!PORT.equals(NEW_PORT)) {
          LOG.info("bootstrap.webservices.port has changed: oldValue = " + PORT + ", newValue = " + NEW_PORT);
          PORT = NEW_PORT;
          different = true;
        }
        if (!LISTENER_ADDRESS_MATCH.equals( NEW_LISTENER_ADDRESS_MATCH )) {
          LOG.info("bootstrap.webservices.listener_address_match has changed: oldValue = " + LISTENER_ADDRESS_MATCH + ", newValue = " + NEW_LISTENER_ADDRESS_MATCH);
          LISTENER_ADDRESS_MATCH = NEW_LISTENER_ADDRESS_MATCH;
          different = true;
        }
        if (different) {
          LOG.info("One or more bootstrap.webservices properties have changed, restarting web services listeners [May change ports]");
          new Thread( Threads.threadUniqueName( "web-services-restarter" ) ) {
            public void run() {
              try {
                restart();
                LOG.info("Web services restart complete");
              } catch (Exception ex) {
                LOG.error(ex, ex);
              } finally {
                isRunning.set(false);
              }
            }
          }.start();
        } else {
          isRunning.set(false);
          LOG.trace("No updates found to web services properties");
        }
      }
    }
  }

  private static DefaultChannelGroup channelGroup( ) {
    return new DefaultChannelGroup( Empyrean.INSTANCE.getFullName( ) + ":"
                                                                     + WebServices.class.getSimpleName( )
                                                                     + ":"
                                                                     + StackConfiguration.PORT );
  }
  
  private static ServerBootstrap serverBootstrap( final ChannelFactory channelFactory, ChannelPipelineFactory serverPipelineFactory ) {
    final ServerBootstrap bootstrap = new ServerBootstrap( channelFactory );
    bootstrap.setPipelineFactory( serverPipelineFactory );
    if ( !Logs.isExtrrreeeme( ) ) {
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
  
  private static NioServerSocketChannelFactory channelFactory( final Executor workerPool ) {
    return new NioServerSocketChannelFactory( Executors.newCachedThreadPool( Threads.threadFactory( "web-services-boss-pool-%d" ) ),
                                              workerPool,
                                              StackConfiguration.SERVER_POOL_MAX_THREADS );
  }
  
  private static Executor workerPool( ) {
    if ( !Logs.isExtrrreeeme( ) ) {
      LOG.info( String.format( "Creating server worker thread pool (%d). (log level EXTREME for details)",
              StackConfiguration.SERVER_POOL_MAX_THREADS ));
    } else {
      LOG.trace( LogUtil.subheader( "Creating server worker thread pool." ) );
      LOG.trace( String.format( "-> Pool threads:              %8d", StackConfiguration.SERVER_POOL_MAX_THREADS ) );
      LOG.trace( String.format( "-> Pool timeout:              %8d ms", StackConfiguration.SERVER_POOL_TIMEOUT_MILLIS ) );
      LOG.trace( String.format( "-> Max memory per connection: %8.2f MB", StackConfiguration.SERVER_POOL_MAX_MEM_PER_CONN / ( 1024f * 1024f ) ) );
      LOG.trace( String.format( "-> Max total memory:          %8.2f MB", StackConfiguration.SERVER_POOL_TOTAL_MEM / ( 1024f * 1024f ) ) );
    }
    final Executor workerPool = new OrderedMemoryAwareThreadPoolExecutor( StackConfiguration.SERVER_POOL_MAX_THREADS,
                                                                          StackConfiguration.SERVER_POOL_MAX_MEM_PER_CONN,
                                                                          StackConfiguration.SERVER_POOL_TOTAL_MEM,
                                                                          StackConfiguration.SERVER_POOL_TIMEOUT_MILLIS,
                                                                          TimeUnit.MILLISECONDS,
                                                                          Threads.threadFactory( "web-services-worker-pool-%d" ) );
    return workerPool;
  }

  private static Iterable<String> iterableFromList( final String list ) {
    return Splitter.on( CharMatcher.anyOf( " ,\t\n\r" ) ).omitEmptyStrings().trimResults( ).split( list );
  }

  public static boolean isSoapEnabled( final Class<? extends ComponentId> component ) {
    return
        !StackConfiguration.DISABLED_SOAP_API_COMPONENTS.equals( "*" ) &&
        !Iterables.contains(
            Iterables.transform( iterableFromList( StackConfiguration.DISABLED_SOAP_API_COMPONENTS ), Strings.lower( ) ),
            Components.lookup( component ).getName( ) );
  }
}
