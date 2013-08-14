/*************************************************************************
 * Copyright 2009-2012 Eucalyptus Systems, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 *
 * This file may incorporate work covered under the following copyright
 * and permission notice:
 *
 *   Software License Agreement (BSD License)
 *
 *   Copyright (c) 2008, Regents of the University of California
 *   All rights reserved.
 *
 *   Redistribution and use of this software in source and binary forms,
 *   with or without modification, are permitted provided that the
 *   following conditions are met:
 *
 *     Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *     Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer
 *     in the documentation and/or other materials provided with the
 *     distribution.
 *
 *   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *   "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *   LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 *   FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 *   COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 *   INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 *   BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *   LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 *   CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 *   LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 *   ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 *   POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 *   THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 *   COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 *   AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 *   IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 *   SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 *   WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 *   REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 *   IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 *   NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/

package com.eucalyptus.ws;

import java.net.InetSocketAddress;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.log4j.Logger;
import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelHandler;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.SimpleChannelHandler;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.group.DefaultChannelGroup;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.jboss.netty.handler.execution.OrderedMemoryAwareThreadPoolExecutor;
import org.jboss.netty.logging.InternalLoggerFactory;
import org.jboss.netty.logging.Log4JLoggerFactory;
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
import com.eucalyptus.util.LogUtil;

public class WebServices {
  
  @Provides( Empyrean.class )
  @RunDuring( Bootstrap.Stage.RemoteServicesInit )
  public static class WebServicesBootstrapper extends Bootstrapper.Simple {
    static {
      InternalLoggerFactory.setDefaultFactory( new Log4JLoggerFactory( ) );
    }

    @Override
    public boolean load( ) throws Exception {
      WebServices.restart( );
      return true;
    }
    
    @Override
    public boolean check( ) throws Exception {
      return super.check( );//TODO:GRZE: you know what...
    }

    @Override
    public boolean stop( ) throws Exception {
      Handlers.pipelineExecutionHandler( ).releaseExternalResources( );
      Handlers.serviceExecutionHandler( ).releaseExternalResources( );
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

  class RestartWebServicesListener implements PropertyChangeListener<Integer> {
    
    @Override
    public void fireChange( ConfigurableProperty t, Integer newValue ) throws ConfigurablePropertyException {
      WebServices.restart( );
    }
    
  }
  
  private static Logger   LOG = Logger.getLogger( WebServices.class );
  private static Executor clientWorkerThreadPool;
  private static NioClientSocketChannelFactory nioClientSocketChannelFactory;
  private static Runnable serverShutdown;
  
  public static ClientBootstrap clientBootstrap( final ChannelPipelineFactory factory ) {
    final ChannelFactory clientChannelFactory = clientChannelFactory( );
    final ClientBootstrap bootstrap = clientBootstrap( factory, clientChannelFactory );
    return bootstrap;
    
  }
  
  private static ClientBootstrap clientBootstrap( final ChannelPipelineFactory factory, final ChannelFactory clientChannelFactory ) {
    final ClientBootstrap bootstrap = new ClientBootstrap( clientChannelFactory );
    bootstrap.setPipelineFactory( factory );
    bootstrap.setOption( "tcpNoDelay", true );
    bootstrap.setOption( "keepAlive", true );
    bootstrap.setOption( "reuseAddress", true );
    bootstrap.setOption( "connectTimeoutMillis", 3000 );
    return bootstrap;
  }
  
  private static NioClientSocketChannelFactory clientChannelFactory( ) {
    if ( nioClientSocketChannelFactory != null ) {
      return nioClientSocketChannelFactory;
    } else synchronized ( WebServices.class ) {
      if ( nioClientSocketChannelFactory != null ) {
        return nioClientSocketChannelFactory;
      } else {
        return nioClientSocketChannelFactory =
            new NioClientSocketChannelFactory( Threads.lookup( Empyrean.class, WebServices.class ),
                WebServices.clientWorkerPool( ),
                StackConfiguration.CLIENT_POOL_MAX_THREADS );
      }
    }
  }
  
  public static Executor clientWorkerPool( ) {
    if ( clientWorkerThreadPool != null ) {
      return clientWorkerThreadPool;
    } else {
      synchronized ( WebServices.class ) {
        if ( clientWorkerThreadPool != null ) {
          return clientWorkerThreadPool;
        } else {
          LOG.trace( LogUtil.subheader( "Creating client worker thread pool." ) );
          LOG.trace( String.format( "-> Pool threads:              %8d", StackConfiguration.CLIENT_POOL_MAX_THREADS ) );
          LOG.trace( String.format( "-> Pool timeout:              %8d ms", StackConfiguration.CLIENT_POOL_TIMEOUT_MILLIS ) );
          LOG.trace( String.format( "-> Max memory per connection: %8.2f MB", StackConfiguration.CLIENT_POOL_MAX_MEM_PER_CONN / ( 1024f * 1024f ) ) );
          LOG.trace( String.format( "-> Max total memory:          %8.2f MB", StackConfiguration.CLIENT_POOL_TOTAL_MEM / ( 1024f * 1024f ) ) );
          
          return clientWorkerThreadPool = new OrderedMemoryAwareThreadPoolExecutor( StackConfiguration.CLIENT_POOL_MAX_THREADS,
                                                                                    StackConfiguration.CLIENT_POOL_MAX_MEM_PER_CONN,
                                                                                    StackConfiguration.CLIENT_POOL_TOTAL_MEM,
                                                                                    StackConfiguration.CLIENT_POOL_TIMEOUT_MILLIS,
                                                                                    TimeUnit.MILLISECONDS );
        }
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
    if ( !StackConfiguration.INTERNAL_PORT.equals( StackConfiguration.PORT ) ) {
      final Channel serverChannel = bootstrap.bind( new InetSocketAddress( StackConfiguration.PORT ) );
      serverChannelGroup.add( serverChannel );
    }
    serverChannelGroup.add( bootstrap.bind( new InetSocketAddress( 443 ) ) );//GRZE:HACKHACK: always bind 443
    try {
      final Channel serverChannel = bootstrap.bind( new InetSocketAddress( StackConfiguration.INTERNAL_PORT ) );
      serverChannelGroup.add( serverChannel );
      serverShutdown = new Runnable( ) {
        AtomicBoolean ranned = new AtomicBoolean( false );
        
        @Override
        public void run( ) {
          if ( this.ranned.compareAndSet( false, true ) ) {
            serverChannelGroup.close( ).awaitUninterruptibly( );
            serverChannelFactory.releaseExternalResources( );
          }
        }
      };
      OrderedShutdown.registerPreShutdownHook( serverShutdown );
    } catch ( Exception ex ) {
      LOG.error( ex, ex );
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
    return new NioServerSocketChannelFactory( Executors.newCachedThreadPool( ),
                                              workerPool,
                                              StackConfiguration.SERVER_POOL_MAX_THREADS );
  }
  
  private static Executor workerPool( ) {
    if ( !Logs.isExtrrreeeme( ) ) {
      LOG.info( "Creating server worker thread pool. (log level EXTREME for details)" );
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
                                                                          TimeUnit.MILLISECONDS );
    return workerPool;
  }
}
