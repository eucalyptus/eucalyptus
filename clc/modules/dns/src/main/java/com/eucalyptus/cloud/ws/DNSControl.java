/*************************************************************************
 * Copyright 2008 Regents of the University of California
 * Copyright 2009-2014 Ent. Services Development Corporation LP
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

package com.eucalyptus.cloud.ws;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.apache.log4j.Logger;
import org.jboss.netty.bootstrap.ConnectionlessBootstrap;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.ChannelUpstreamHandler;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.FixedReceiveBufferSizePredictor;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.group.DefaultChannelGroup;
import org.jboss.netty.channel.socket.DatagramChannelFactory;
import org.jboss.netty.channel.socket.ServerSocketChannelFactory;
import org.jboss.netty.channel.socket.nio.NioDatagramChannelFactory;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.jboss.netty.handler.codec.frame.LengthFieldBasedFrameDecoder;
import org.jboss.netty.handler.codec.frame.LengthFieldPrepender;
import org.jboss.netty.handler.execution.ExecutionHandler;
import org.xbill.DNS.ResolverConfig;
import com.eucalyptus.configurable.ConfigurableClass;
import com.eucalyptus.configurable.ConfigurableField;
import com.eucalyptus.configurable.ConfigurableProperty;
import com.eucalyptus.configurable.ConfigurablePropertyException;
import com.eucalyptus.configurable.PropertyChangeListener;
import com.eucalyptus.system.Threads;
import com.eucalyptus.util.Cidr;
import com.eucalyptus.util.DNSProperties;
import com.eucalyptus.util.Internets;
import com.eucalyptus.util.LockResource;
import com.eucalyptus.ws.WebServices;
import com.google.common.base.CharMatcher;
import com.google.common.base.Predicates;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

@ConfigurableClass( root = "dns", description = "Controls dns listeners." )
public class DNSControl {

  private static Logger LOG = Logger.getLogger( DNSControl.class );

  private static final AtomicReference<Collection<Cidr>> addressMatchers =
      new AtomicReference<Collection<Cidr>>( Collections.<Cidr>emptySet( ) );

  private static final boolean useLegacyTcp =
      Boolean.valueOf( System.getProperty( "com.eucalyptus.dns.legacyTcp", "false" ) );

  private static final AtomicReference<Collection<TCPListener>> tcpListenerRef =
      new AtomicReference<Collection<TCPListener>>( Collections.<TCPListener>emptySet( ) );

  private static final Lock listenerLock = new ReentrantLock( );

  @ConfigurableField( displayName = "dns_listener_address_match",
      description = "Additional address patterns to listen on for DNS requests.",
      initial = "",
      readonly = false,
      changeListener = DnsAddressChangeListener.class )
  public static volatile String dns_listener_address_match = "";

  @ConfigurableField( description = "Port number to listen on for DNS requests.",
      initial = "53",
      changeListener = WebServices.CheckNonNegativeIntegerPropertyChangeListener.class )
  public static volatile Integer dns_listener_port = 53;

  @ConfigurableField( description = "Server worker thread pool max.",
      initial = "32",
      changeListener = WebServices.CheckNonNegativeIntegerPropertyChangeListener.class )
  public static Integer SERVER_POOL_MAX_THREADS = 32;

  @ConfigurableField( displayName = "server",
      description = "Comma separated list of nameservers, OS settings used if none specified (change requires restart)",
      initial = "",
      readonly = false,
      changeListener = DnsServerChangeListener.class )
  public static volatile String server = "";

  @ConfigurableField( displayName = "search",
      description = "Comma separated list of domains to search, OS settings used if none specified (change requires restart)",
      initial = "",
      readonly = false,
      changeListener = DnsSearchChangeListener.class )
  public static volatile String search = "";

  public static class DnsServerChangeListener implements PropertyChangeListener {

    @Override
    public void fireChange( ConfigurableProperty t, Object newValue ) throws ConfigurablePropertyException {
      if ( newValue == null || ( newValue instanceof String ) ) {
        try {
          String newValueStr = (String) newValue;
          if ( Strings.isNullOrEmpty( newValueStr ) ) {
            LOG.debug( "Setting dns.server property to null (by clearing it)" );
            System.clearProperty( "dns.server" );
          } else {
            LOG.debug( "Setting dns.server property to " + newValueStr );
            System.setProperty( "dns.server", newValueStr );
          }
          ResolverConfig.refresh( );
        } catch ( final Exception e ) {
          throw new ConfigurablePropertyException( e.getMessage( ) );
        }
      }
    }
  }

  public static class DnsSearchChangeListener implements PropertyChangeListener {

    @Override
    public void fireChange( ConfigurableProperty t, Object newValue ) throws ConfigurablePropertyException {
      if ( newValue == null || ( newValue instanceof String ) ) {
        try {
          String newValueStr = (String) newValue;
          if ( Strings.isNullOrEmpty( newValueStr ) ) {
            LOG.debug( "Setting dns.search property to null (by clearing it)" );
            System.clearProperty( "dns.search" );
          } else {
            LOG.debug( "Setting dns.search property to " + newValueStr );
            System.setProperty( "dns.search", newValueStr );
          }
          ResolverConfig.refresh( );
        } catch ( final Exception e ) {
          throw new ConfigurablePropertyException( e.getMessage( ) );
        }
      }
    }
  }

  public static class DnsAddressChangeListener implements PropertyChangeListener {

    @Override
    public void fireChange( ConfigurableProperty t, Object newValue ) throws ConfigurablePropertyException {
      if ( newValue instanceof String ) {
        updateAddressMatchers( (String) newValue );
      }
    }
  }

  private static void updateAddressMatchers( final String addressCidrs ) throws ConfigurablePropertyException {
    try {
      addressMatchers.set( ImmutableList.copyOf( Iterables.transform(
          Splitter.on( CharMatcher.anyOf( ", ;:" ) ).trimResults( ).omitEmptyStrings( ).split( addressCidrs ),
          Cidr.parseUnsafe( )
      ) ) );
    } catch ( IllegalArgumentException e ) {
      throw new ConfigurablePropertyException( e.getMessage( ) );
    }
  }

  private static final ChannelGroup udpChannelGroup = new DefaultChannelGroup(
      DNSControl.class.getSimpleName( )
          + ":udp" );

  private static final ChannelGroup tcpChannelGroup = new DefaultChannelGroup(
      DNSControl.class.getSimpleName( )
          + ":tcp" );

  private static DatagramChannelFactory udpChannelFactory = null;

  private static ServerSocketChannelFactory tcpChannelFactory = null;

  private static ExecutionHandler udpExecHandler = null;

  private static Executor createWorkerPool( String name ) {
    return Executors.newFixedThreadPool(
        SERVER_POOL_MAX_THREADS,
        Threads.threadFactory( "dns-" + name + "-worker-pool-%d" ) );
  }

  public static class TimedDns {

    private Long receivedTime;

    private byte[] request;

    public Long getReceivedTime( ) {
      return receivedTime;
    }

    public byte[] getRequest( ) {
      return request;
    }
  }

  private static class DnsTimestampWrapper implements ChannelUpstreamHandler {

    @Override
    public void handleUpstream( ChannelHandlerContext ctx, ChannelEvent ce )
        throws Exception {
      if ( !( ce instanceof MessageEvent ) ) {
        return;
      }
      try {
        final MessageEvent me = (MessageEvent) ce;
        final ChannelBuffer buffer = ( (ChannelBuffer) me.getMessage( ) );
        final TimedDns wrappedRequest = new TimedDns( );
        wrappedRequest.receivedTime = ( new Date( ) ).getTime( );
        wrappedRequest.request = new byte[ buffer.readableBytes( ) ];
        buffer.getBytes( 0, wrappedRequest.request );
        Channels.fireMessageReceived( ctx, wrappedRequest,
            ( (InetSocketAddress) me.getRemoteAddress( ) ) );
      } catch ( final Exception ex ) {
        LOG.debug( ex, ex );
      }
    }
  }

  private static class UdpChannelPipelineFactory implements ChannelPipelineFactory {

    private final ExecutionHandler execHandler;

    private UdpChannelPipelineFactory( final ExecutionHandler execHandler ) {
      this.execHandler = execHandler;
    }

    @Override
    public ChannelPipeline getPipeline( ) throws Exception {
      return Channels.pipeline( new DnsTimestampWrapper( ), this.execHandler, new DnsServerHandler( ) );
    }
  }

  private static void initializeUDP( ) throws Exception {
    if ( udpChannelFactory == null ) {
      try {
        udpChannelFactory = new NioDatagramChannelFactory(
            Executors.newCachedThreadPool( Threads.threadFactory( "dns-server-udp-pool-%d" ) ) );
        final ConnectionlessBootstrap b = new ConnectionlessBootstrap( udpChannelFactory );
        udpExecHandler =
            new ExecutionHandler( createWorkerPool( "udp" ) );
        b.setPipelineFactory( new UdpChannelPipelineFactory( udpExecHandler ) );
        b.setOption( "receiveBufferSize", 4194304 );
        b.setOption( "broadcast", "false" );
        b.setOption( "receiveBufferSizePredictor", new FixedReceiveBufferSizePredictor( 1024 ) );

        b.setOption( "child.tcpNoDelay", true );
        b.setOption( "child.reuseAddress", true );
        b.setOption( "child.connectTimeoutMillis", 3000 );

        b.setOption( "tcpNoDelay", true );
        b.setOption( "reuseAddress", true );
        b.setOption( "connectTimeoutMillis", 3000 );

        final Set<InetAddress> listenAddresses = Sets.newLinkedHashSet( );
        listenAddresses.add( Internets.localHostInetAddress( ) );

        if ( addressMatchers.get( ).size( ) > 0 ) {
          Iterables.addAll(
              listenAddresses,
              Iterables.filter( Internets.getAllInetAddresses( ), Predicates.or( addressMatchers.get( ) ) ) );
        } else {
          Iterables.addAll(
              listenAddresses,
              Internets.getAllInetAddresses( ) );
        }
        for ( final InetAddress listenAddr : listenAddresses ) {
          try {
            Channel udpChannel = b.bind( new InetSocketAddress( listenAddr, dns_listener_port ) );
            udpChannelGroup.add( udpChannel );
          } catch ( final Exception ex ) {
            continue;
          }
        }
      } catch ( final Exception ex ) {
        LOG.debug( "Failed initializing DNS udp listener", ex );
        udpChannelGroup.close( ).awaitUninterruptibly( );
        if ( udpChannelFactory != null ) {
          udpChannelFactory.releaseExternalResources( );
          udpChannelFactory = null;
        }
        if ( udpExecHandler != null ) {
          udpExecHandler.releaseExternalResources( );
          udpExecHandler = null;
        }
        throw ex;
      }
    }
  }

  private static void initializeTCP( ) throws Exception {
    if ( useLegacyTcp ) {
      initializeTCPSocket( );
    } else {
      initializeTCPNetty( );
    }
  }

  private static void initializeTCPSocket( ) throws Exception {
    initializeListeners( tcpListenerRef, "TCP", new ListenerBuilder<TCPListener>( ) {
      @Override
      public TCPListener build( final InetAddress address, final int port ) throws IOException {
        return new TCPListener( address, port );
      }
    } );
  }

  private static void initializeTCPNetty( ) throws Exception {
    if ( tcpChannelFactory == null ) {
      try {
        tcpChannelFactory =
            new NioServerSocketChannelFactory(
                Executors.newCachedThreadPool( Threads.threadFactory( "dns-server-tcp-pool-%d" ) ),
                createWorkerPool( "tcp" ) );
        final ServerBootstrap b = new ServerBootstrap( tcpChannelFactory );
        b.setPipelineFactory( new ChannelPipelineFactory( ) {
          public ChannelPipeline getPipeline( ) throws Exception {
            ChannelPipeline p = Channels.pipeline( );
            p.addLast( "framer", new LengthFieldBasedFrameDecoder( 65536, 0, 2, 0, 2 ) );
            p.addLast( "prepender", new LengthFieldPrepender( 2 ) );
            p.addLast( "dns-server", new DnsServerHandler( ) );
            return p;
          }
        } );
        b.setOption( "child.tcpNoDelay", true );
        b.setOption( "child.keepAlive", false );
        b.setOption( "child.reuseAddress", true );
        b.setOption( "child.connectTimeoutMillis", 3000 );
        b.setOption( "tcpNoDelay", true );
        b.setOption( "keepAlive", false );
        b.setOption( "reuseAddress", true );
        b.setOption( "connectTimeoutMillis", 3000 );
        final Channel tcpChannel = b.bind( new InetSocketAddress( dns_listener_port ) );
        tcpChannelGroup.add( tcpChannel );
      } catch ( final Exception ex ) {
        LOG.debug( ex, ex );
        tcpChannelGroup.close( ).awaitUninterruptibly( );
        if ( tcpChannelFactory != null )
          tcpChannelFactory.releaseExternalResources( );
        tcpChannelFactory = null;
        throw ex;
      }
    }
  }

  private static <T extends Thread> void initializeListeners(
      final AtomicReference<Collection<T>> listenerRef,
      final String description,
      final ListenerBuilder<T> builder
  ) {
    try ( final LockResource lock = LockResource.lock( listenerLock ) ) {
      if ( listenerRef.get( ).isEmpty( ) ) {
        final int listenPort = DNSProperties.PORT;
        final Set<InetAddress> listenAddresses = Sets.newLinkedHashSet( );
        listenAddresses.add( Internets.localHostInetAddress( ) );

        if ( addressMatchers.get( ).size( ) > 0 ) {
          Iterables.addAll(
              listenAddresses,
              Iterables.filter( Internets.getAllInetAddresses( ), Predicates.or( addressMatchers.get( ) ) ) );
        } else {
          Iterables.addAll(
              listenAddresses,
              Internets.getAllInetAddresses( ) );
        }
        LOG.info( "Starting DNS " + description + " listeners on " + listenAddresses + ":" + listenPort );

        // Configured listeners
        final List<T> listeners = Lists.newArrayList( );
        for ( final InetAddress listenAddress : listenAddresses ) {
          try {
            final T listener = builder.build( listenAddress, listenPort );
            listener.start( );
            listeners.add( listener );
          } catch ( final Exception ex ) {
            LOG.error( "Error starting DNS " + description + " listener on " + listenAddress + ":" + listenPort, ex );
          }
        }
        listenerRef.set( ImmutableList.copyOf( listeners ) );
      }
    }
  }

  private interface ListenerBuilder<T> {

    T build( InetAddress address, int port ) throws IOException;
  }

  public static void initialize( ) throws Exception {
    try {
      initializeUDP( );
      initializeTCP( );
    } catch ( Exception ex ) {
      LOG.error( "DNS could not be initialized. Is some other service running on port 53?", ex );
      throw ex;
    }
  }

  public static void stop( ) throws Exception {
    if ( udpChannelGroup != null )
      udpChannelGroup.close( ).awaitUninterruptibly( );
    if ( udpExecHandler != null ) {
      udpExecHandler.releaseExternalResources( );
      udpExecHandler = null;
    }
    if ( udpChannelFactory != null ) {
      udpChannelFactory.releaseExternalResources( );
      udpChannelFactory = null;
    }
    if ( tcpChannelGroup != null )
      tcpChannelGroup.close( ).awaitUninterruptibly( );
    if ( tcpChannelFactory != null ) {
      tcpChannelFactory.releaseExternalResources( );
      tcpChannelFactory = null;
    }
  }

  public static void restart( ) throws Exception {
    stop( );
    initialize( );
  }
}
