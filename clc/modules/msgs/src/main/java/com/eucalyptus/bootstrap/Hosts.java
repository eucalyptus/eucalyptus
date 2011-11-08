/*******************************************************************************
 * Copyright (c) 2009  Eucalyptus Systems, Inc.
 * 
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, only version 3 of the License.
 * 
 * 
 *  This file is distributed in the hope that it will be useful, but WITHOUT
 *  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 *  for more details.
 * 
 *  You should have received a copy of the GNU General Public License along
 *  with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 *  Please contact Eucalyptus Systems, Inc., 130 Castilian
 *  Dr., Goleta, CA 93101 USA or visit <http://www.eucalyptus.com/licenses/>
 *  if you need additional information or have any questions.
 * 
 *  This file may incorporate work covered under the following copyright and
 *  permission notice:
 * 
 *    Software License Agreement (BSD License)
 * 
 *    Copyright (c) 2008, Regents of the University of California
 *    All rights reserved.
 * 
 *    Redistribution and use of this software in source and binary forms, with
 *    or without modification, are permitted provided that the following
 *    conditions are met:
 * 
 *      Redistributions of source code must retain the above copyright notice,
 *      this list of conditions and the following disclaimer.
 * 
 *      Redistributions in binary form must reproduce the above copyright
 *      notice, this list of conditions and the following disclaimer in the
 *      documentation and/or other materials provided with the distribution.
 * 
 *    THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 *    IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 *    TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 *    PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
 *    OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 *    EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 *    PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 *    PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 *    LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 *    NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 *    SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. USERS OF
 *    THIS SOFTWARE ACKNOWLEDGE THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE
 *    LICENSED MATERIAL, COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS
 *    SOFTWARE, AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 *    IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA, SANTA
 *    BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY, WHICH IN
 *    THE REGENTS DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
 *    OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
 *    WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
 *    ANY SUCH LICENSES OR RIGHTS.
 *******************************************************************************
 * @author chris grzegorczyk <grze@eucalyptus.com>
 */

package com.eucalyptus.bootstrap;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Vector;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import net.sf.hajdbc.InactiveDatabaseMBean;
import net.sf.hajdbc.sql.DriverDatabaseClusterMBean;
import org.apache.log4j.Logger;
import org.jgroups.Address;
import org.jgroups.ChannelException;
import org.jgroups.Global;
import org.jgroups.Header;
import org.jgroups.JChannel;
import org.jgroups.View;
import org.jgroups.blocks.ReplicatedHashMap;
import org.jgroups.conf.ClassConfigurator;
import org.jgroups.stack.Protocol;
import org.jgroups.stack.ProtocolStack;
import com.eucalyptus.component.Component.State;
import com.eucalyptus.component.ComponentId;
import com.eucalyptus.component.ComponentIds;
import com.eucalyptus.component.Components;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.ServiceConfigurations;
import com.eucalyptus.component.ServiceTransitions;
import com.eucalyptus.component.ServiceUris;
import com.eucalyptus.component.Topology;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.component.id.Eucalyptus.Database;
import com.eucalyptus.configurable.ConfigurableClass;
import com.eucalyptus.configurable.ConfigurableField;
import com.eucalyptus.empyrean.Empyrean;
import com.eucalyptus.entities.PersistenceContexts;
import com.eucalyptus.event.EventListener;
import com.eucalyptus.event.Hertz;
import com.eucalyptus.event.Listeners;
import com.eucalyptus.records.Logs;
import com.eucalyptus.scripting.Groovyness;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.Internets;
import com.eucalyptus.util.Mbeans;
import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.primitives.Longs;

@ConfigurableClass( root = "bootstrap.hosts",
                    description = "Properties controlling the handling of remote host bootstrapping" )
public class Hosts {
  
  @ConfigurableField( description = "Timeout for state transfers (in msec).",
                      readonly = true )
  public static final Long                       STATE_TRANSFER_TIMEOUT   = 10000L;
  @ConfigurableField( description = "Timeout for state initialization (in msec).",
                      readonly = true )
  public static final Long                       STATE_INITIALIZE_TIMEOUT = 30000L;
  private static final Logger                    LOG                      = Logger.getLogger( Hosts.class );
  private static ReplicatedHashMap<String, Host> hostMap;
  
  public static Predicate<ServiceConfiguration> nonLocalAddressMatch( final InetAddress addr ) {
    return new Predicate<ServiceConfiguration>( ) {
      
      @Override
      public boolean apply( final ServiceConfiguration input ) {
        return input.getInetAddress( ).equals( addr )
               || input.getInetAddress( ).getCanonicalHostName( ).equals( addr.getCanonicalHostName( ) )
               || input.getHostName( ).equals( addr.getCanonicalHostName( ) );
      }
      
    };
  }
  
  public static Predicate<ComponentId> nonLocalAddressFilter( final InetAddress addr ) {
    return new Predicate<ComponentId>( ) {
      
      @Override
      public boolean apply( final ComponentId input ) {
        return !Internets.testLocal( addr );
      }
    };
  }
  
  private static <T extends ComponentId> Function<T, ServiceConfiguration> initRemoteSetupConfigurations( final InetAddress addr ) {
    return new Function<T, ServiceConfiguration>( ) {
      
      @Override
      public ServiceConfiguration apply( final T input ) {
        final ServiceConfiguration config = Components.lookup( input ).initRemoteService( addr );
        LOG.info( "Initialized remote service: " + config.getFullName( ) );
        return config;
      }
    };
  }
  
  enum SetupRemoteServiceConfigurations implements Function<ServiceConfiguration, ServiceConfiguration> {
    INSTANCE;
    @Override
    public ServiceConfiguration apply( final ServiceConfiguration input ) {
      try {
        final ServiceConfiguration conf = ServiceTransitions.pathTo( input, State.DISABLED ).get( );
        Topology.enable( conf );
        LOG.info( "Initialized service: " + conf.getFullName( ) );
        return conf;
      } catch ( final ExecutionException ex ) {
        Exceptions.trace( ex.getCause( ) );
      } catch ( final InterruptedException ex ) {
        Thread.currentThread( ).interrupt( );
        Exceptions.trace( ex.getCause( ) );
      }
      return input;
    }
  }
  
  enum ShouldLoadRemote implements Predicate<ComponentId> {
    EMPYREAN( Empyrean.class ),
    EUCALYPTUS( Eucalyptus.class );
    Predicate<ComponentId>       delegate;
    Class<? extends ComponentId> compId;
    
    private ShouldLoadRemote( final Class<? extends ComponentId> compId ) {
      this.delegate = shouldLoadRemote( compId );
      this.compId = compId;
    }
    
    @Override
    public boolean apply( final ComponentId input ) {
      return this.delegate.apply( input );
    }
    
    private static <T extends ComponentId> Predicate<T> shouldLoadRemote( final Class<? extends ComponentId> compId ) {
      return new Predicate<T>( ) {
        @Override
        public boolean apply( final T input ) {
          return input.isAncestor( compId ) && !input.isRegisterable( );
        }
      };
    }
    
    public static Predicate<ComponentId> getInitFilter( final Class<? extends ComponentId> comp, final InetAddress addr ) {
      return Predicates.and( EMPYREAN.compId.equals( comp )
        ? EMPYREAN
        : EUCALYPTUS, nonLocalAddressFilter( addr ) );
    }
    
    public static Function<ComponentId, ServiceConfiguration> getInitFunction( final InetAddress addr ) {
      return Functions.compose( SetupRemoteServiceConfigurations.INSTANCE,
                                initRemoteSetupConfigurations( addr ) );
    }
  }
  
  enum HostBootstrapEventListener implements EventListener<Hertz> {
    INSTANCE;
    
    @Override
    public void fireEvent( final Hertz event ) {
      final Host currentHost = Hosts.localHost( );
      if ( event.isAsserted( 15L ) ) {
        UpdateEntry.INSTANCE.apply( currentHost );
      }
    }
  }
  
  enum HostMapStateListener implements ReplicatedHashMap.Notification<String, Host> {
    INSTANCE;
    
    private String printMap( ) {
      return "\n" + Joiner.on( "\n=> " ).join( hostMap.values( ) );
    }
    
    @Override
    public void contentsCleared( ) {
      LOG.info( "Hosts.contentsCleared(): " + this.printMap( ) );
    }
    
    @Override
    public void contentsSet( final Map<String, Host> arg0 ) {
      LOG.info( "Hosts.contentsSet(): " + this.printMap( ) );
    }
    
    @Override
    public void entryRemoved( final String arg0 ) {
      LOG.info( "Hosts.entryRemoved(): " + arg0 );
      LOG.info( "Hosts.entryRemoved(): " + hostMap.keySet( ) );
    }
    
    @Override
    public void entrySet( final String arg0, final Host arg1 ) {
      LOG.info( "Hosts.entryAdded(): " + arg0 + " => " + arg1 );
      if ( BootstrapRemoteComponent.INSTANCE.apply( arg1 ) ) {
        LOG.info( "Hosts.entryAdded(): Bootstrapping host  => " + arg1 );
      } else if ( arg1.hasBootstrapped( ) ) {
        LOG.info( "Hosts.entryAdded(): Host is operational => " + arg1 );
        Hosts.syncDatabase( arg1 );
      } else if ( InitializeAsCloudController.INSTANCE.apply( arg1 ) ) {
        LOG.info( "Hosts.entryAdded(): Initialized as clc  => " + arg1 );
      } else {
        LOG.info( "Hosts.entryAdded(): Wait for bootstrap  => " + arg1 );
      }
    }
    
    @Override
    public void viewChange( final View currentView, final Vector<Address> joinMembers, final Vector<Address> partMembers ) {
      LOG.trace( "Hosts.viewChange(): new view => " + Joiner.on( ", " ).join( currentView.getMembers( ) ) );
      if ( !joinMembers.isEmpty( ) ) LOG.info( "Hosts.viewChange(): joined   => " + Joiner.on( ", " ).join( joinMembers ) );
      if ( !partMembers.isEmpty( ) ) LOG.info( "Hosts.viewChange(): parted   => " + Joiner.on( ", " ).join( partMembers ) );
      for ( final Host h : Hosts.list( ) ) {
        if ( Iterables.contains( partMembers, h.getGroupsId( ) ) ) {
          try {
            teardown( Empyrean.class, h.getBindAddress( ) );
            if ( h.hasDatabase( ) ) {
              Hosts.stopDbPool( h );
              teardown( Eucalyptus.class, h.getBindAddress( ) );
            }
          } catch ( Exception ex ) {
            LOG.error( ex, ex );
          }
          if ( Hosts.Coordinator.INSTANCE.isLocalhost( ) ) {
            hostMap.remove( h.getDisplayName( ) );
            LOG.info( "Hosts.viewChange(): -> removed  => " + h );
          } else if ( h.hasDatabase( ) && BootstrapArgs.isCloudController( ) ) {
            hostMap.remove( h.getDisplayName( ) );
            LOG.info( "Hosts.viewChange(): -> removed  => " + h );
          }
        }
      }
      if ( BootstrapArgs.isCloudController( ) ) {
        Hosts.doBootstrap( Empyrean.class, Hosts.localHost( ).getBindAddress( ) );
        if ( Hosts.localHost( ).hasDatabase( ) ) {
          Hosts.doBootstrap( Eucalyptus.class, Hosts.localHost( ).getBindAddress( ) );
        }
      }
    }
    
  }
  
  enum BootstrapRemoteComponent implements Predicate<Host> {
    INSTANCE;
    private static final AtomicBoolean initialized = new AtomicBoolean( false );
    
    @Override
    public boolean apply( final Host arg1 ) {
      if ( !arg1.isLocalHost( ) && !Bootstrap.isFinished( ) ) {
        Hosts.doBootstrap( Empyrean.class, arg1.getBindAddress( ) );
        if ( arg1.hasDatabase( ) && initialized.compareAndSet( false, true ) ) {
          Hosts.doBootstrap( Eucalyptus.class, arg1.getBindAddress( ) );
        }
        return true;
      } else {
        return false;
      }
    }
    
  }
  
  private static <T extends ComponentId> void doBootstrap( final Class<T> compId, final InetAddress addr ) {
    try {
      final Collection<ComponentId> deps = Collections2.filter( ComponentIds.list( ), ShouldLoadRemote.getInitFilter( compId, addr ) );
      final Function<ComponentId, ServiceConfiguration> initFunc = ShouldLoadRemote.getInitFunction( addr );
      initFunc.apply( ComponentIds.lookup( compId ) );
      Iterables.transform( deps, initFunc );
    } catch ( final Exception ex ) {
      LOG.error( ex, ex );
    }
  }
  
  enum CheckStale implements Predicate<Host> {
    INSTANCE;
    
    @Override
    public boolean apply( final Host input ) {
      final Host that = new Host( input.getStartedTime( ) );
      if ( !input.isLocalHost( ) ) {
        return false;
      } else if ( that.hasBootstrapped( ) && !input.hasBootstrapped( ) ) {
        return true;
      } else if ( that.hasDatabase( ) && !input.hasDatabase( ) ) {
        return true;
      } else if ( that.getEpoch( ) > input.getEpoch( ) ) {
        return true;
      } else if ( !that.getHostAddresses( ).equals( input.getHostAddresses( ) ) ) {
        return true;
      } else {
        return false;
      }
    }
  }
  
  public enum UpdateEntry implements Predicate<Host> {
    INSTANCE;
    
    @Override
    public boolean apply( final Host input ) {
      if ( input == null ) {
        final Host newHost = Coordinator.INSTANCE.createLocalHost( );
        final Host oldHost = hostMap.putIfAbsent( newHost.getDisplayName( ), newHost );
        if ( oldHost != null ) {
          LOG.info( "Inserted local host information:   " + localHost( ) );
          return true;
        } else {
          return false;
        }
      } else if ( input.isLocalHost( ) ) {
        if ( CheckStale.INSTANCE.apply( input ) ) {
          final Host newHost = new Host( input.getStartedTime( ) );
          final Host oldHost = hostMap.replace( newHost.getDisplayName( ), newHost );
          if ( oldHost != null ) {
            LOG.info( "Updated local host information:   " + localHost( ) );
            return true;
          } else {
            return false;
          }
        } else {
          if ( !hostMap.containsKey( input.getDisplayName( ) ) ) {
            final Host newHost = Coordinator.INSTANCE.createLocalHost( );
            final Host oldHost = hostMap.putIfAbsent( newHost.getDisplayName( ), newHost );
            if ( oldHost == null ) {
              LOG.info( "Updated local host information:   " + localHost( ) );
              return true;
            } else {
              return false;
            }
          }
        }
      }
      return true;
    }
  }
  
  public enum InitializeAsCloudController implements Predicate<Host> {
    INSTANCE;
    
    @Override
    public boolean apply( final Host input ) {
      if ( BootstrapArgs.isCloudController( ) ) {
        return false;
      } else if ( !input.isLocalHost( ) ) {
        return false;
      } else if ( !input.hasDatabase( ) ) {
        return false;
      } else if ( !BootstrapArgs.isCloudController( ) && input.isLocalHost( ) && input.hasDatabase( ) ) {
        try {
          hostMap.stop( );
        } catch ( final Exception ex1 ) {
          LOG.error( ex1, ex1 );
        }
        try {
          Bootstrap.initializeSystem( );
          System.exit( 123 );
        } catch ( final Exception ex ) {
          LOG.error( ex, ex );
          System.exit( 123 );
        }
        return true;
      } else {
        return false;
      }
    }
    
  }
  
  public static Address getLocalGroupAddress( ) {
    return HostManager.getMembershipChannel( ).getAddress( );
  }
  
  private static boolean teardown( final Class<? extends ComponentId> compClass, final InetAddress addr ) {
    if ( Internets.testLocal( addr ) ) {
      return false;
    } else {
      try {
        for ( final ComponentId c : Iterables.filter( ComponentIds.list( ), ShouldLoadRemote.getInitFilter( compClass, addr ) ) ) {
          try {
            final ServiceConfiguration dependsConfig = ServiceConfigurations.lookupByName( c.getClass( ), addr.getHostAddress( ) );
            ServiceTransitions.pathTo( dependsConfig, State.STOPPED ).get( );
          } catch ( final Exception ex ) {
            LOG.error( ex );
            Logs.extreme( ).error( ex, ex );
          }
        }
      } catch ( final Exception ex ) {
        LOG.error( ex, ex );
        return false;
      }
      return true;
    }
  }
  
  static class HostManager {
    private final JChannel     membershipChannel;
    private static HostManager singleton;
    public static short        PROTOCOL_ID = 513;
    public static short        HEADER_ID   = 1025;
    
    private HostManager( ) {
      this.membershipChannel = HostManager.buildChannel( );
      //TODO:GRZE:set socket factory for crypto
      try {
        LOG.info( "Starting membership channel... " );
        this.membershipChannel.connect( SystemIds.membershipGroupName( ) );
        HostManager.registerHeader( EpochHeader.class );
        this.membershipChannel.downcall( new org.jgroups.Event( org.jgroups.Event.GET_PHYSICAL_ADDRESS, this.membershipChannel.getAddress( ) ) );
        LOG.info( "Started membership channel: " + SystemIds.membershipGroupName( ) );
      } catch ( final ChannelException ex ) {
        LOG.fatal( ex, ex );
        throw BootstrapException.throwFatal( "Failed to connect membership channel because of " + ex.getMessage( ), ex );
      }
    }
    
    public static short lookupRegisteredId( final Class c ) {
      return ClassConfigurator.getMagicNumber( c );
    }
    
    private static synchronized <T extends Header> String registerHeader( final Class<T> h ) {
      if ( ClassConfigurator.getMagicNumber( h ) == -1 ) {
        ClassConfigurator.add( ++HEADER_ID, h );
      }
      return "euca-" + ( h.isAnonymousClass( )
        ? h.getSuperclass( ).getSimpleName( ).toLowerCase( )
        : h.getSimpleName( ).toLowerCase( ) ) + "-header";
    }
    
    private static List<Protocol> getMembershipProtocolStack( ) {
      return Groovyness.run( "setup_membership.groovy" );
    }
    
    private static HostManager getInstance( ) {
      if ( singleton != null ) {
        return singleton;
      } else {
        synchronized ( HostManager.class ) {
          if ( singleton != null ) {
            return singleton;
          } else {
            singleton = new HostManager( );
            return singleton;
          }
        }
      }
    }
    
    private static JChannel buildChannel( ) {
      try {
        final JChannel channel = new JChannel( false );
        channel.setName( Internets.localHostIdentifier( ) );
        final ProtocolStack stack = new ProtocolStack( );
        channel.setProtocolStack( stack );
        stack.addProtocols( HostManager.getMembershipProtocolStack( ) );
        stack.init( );
        return channel;
      } catch ( final Exception ex ) {
        LOG.fatal( ex, ex );
        throw new RuntimeException( ex );
      }
    }
    
    public static JChannel getMembershipChannel( ) {
      return getInstance( ).membershipChannel;
    }
    
    public static class EpochHeader extends Header {
      private Integer value;
      
      public EpochHeader( ) {
        super( );
      }
      
      public EpochHeader( final Integer value ) {
        super( );
        this.value = value;
      }
      
      @Override
      public void writeTo( final DataOutputStream out ) throws IOException {
        out.writeInt( this.value );
      }
      
      @Override
      public void readFrom( final DataInputStream in ) throws IOException, IllegalAccessException, InstantiationException {
        this.value = in.readInt( );
      }
      
      @Override
      public int size( ) {
        return Global.INT_SIZE;
      }
      
      public Integer getValue( ) {
        return this.value;
      }
    }
    
  }
  
  @Provides( Empyrean.class )
  @RunDuring( Bootstrap.Stage.RemoteConfiguration )
  public static class HostMembershipBootstrapper extends Bootstrapper.Simple {
    
    @Override
    public boolean load( ) throws Exception {
      try {
        HostManager.getInstance( );
        LOG.info( "Started membership channel " + SystemIds.membershipGroupName( ) );
        hostMap = new ReplicatedHashMap<String, Host>( HostManager.getMembershipChannel( ) );
        hostMap.setDeadlockDetection( true );
        hostMap.setBlockingUpdates( true );
        hostMap.addNotifier( HostMapStateListener.INSTANCE );
        hostMap.start( STATE_TRANSFER_TIMEOUT );
        OrderedShutdown.register( Empyrean.class, new Runnable( ) {
          
          @Override
          public void run( ) {
            try {
              try {
                hostMap.remove( Internets.localHostIdentifier( ) );
              } catch ( Exception ex ) {
                LOG.error( ex , ex );
              }
              hostMap.stop( );
            } catch ( Exception ex ) {
              LOG.error( ex, ex );
            }
          }
        } );
        LOG.info( "Added localhost to system state: " + localHost( ) );
        Coordinator.INSTANCE.initialize( hostMap.values( ) );
        final Host local = Coordinator.INSTANCE.createLocalHost( );
        hostMap.putIfAbsent( local.getDisplayName( ), local );
        Listeners.register( HostBootstrapEventListener.INSTANCE );
        LOG.info( "System view:\n" + HostMapStateListener.INSTANCE.printMap( ) );
        if ( !BootstrapArgs.isCloudController( ) ) {
          while ( Hosts.listDatabases( ).isEmpty( ) ) {
            TimeUnit.SECONDS.sleep( 5 );
            LOG.info( "Waiting for system view with database..." );
          }
          if ( shouldInitialize( ) ) {
            doInitialize( );
          }
        } else {
          //TODO:GRZE:handle check and merge of db here!!!!
        }
        LOG.info( "Membership address for localhost: " + Hosts.localHost( ) );
        return true;
      } catch ( final Exception ex ) {
        LOG.fatal( ex, ex );
        BootstrapException.throwFatal( "Failed to connect membership channel because of " + ex.getMessage( ), ex );
        return false;
      }
    }
    
  }
  
  private static void doInitialize( ) {
    try {
      hostMap.stop( );
    } catch ( final Exception ex1 ) {
      LOG.error( ex1, ex1 );
    }
    try {
      Bootstrap.initializeSystem( );
      System.exit( 123 );
    } catch ( final Exception ex ) {
      LOG.error( ex, ex );
      System.exit( 123 );
    }
  }
  
  public static int maxEpoch( ) {
    try {
      return Collections.max( Collections2.transform( hostMap.values( ), EpochTransform.INSTANCE ) );
    } catch ( final Exception ex ) {
      return 0;
    }
  }
  
  private static boolean shouldInitialize( ) {//GRZE:WARNING:HACKHACKHACK do not duplicate pls thanks.
    for ( Host h : Hosts.listDatabases( ) ) {
      final String url = String.format( "jdbc:%s", ServiceUris.remote( Database.class, h.getBindAddress( ), "eucalyptus_config" ) );
      try {
        Connection conn = DriverManager.getConnection( url, Databases.getUserName( ), Databases.getPassword( ) );
        try {
          PreparedStatement statement = conn.prepareStatement( "select config_component_hostname from eucalyptus_config.config_component_base where config_component_partition='eucalyptus';" );
          ResultSet result = statement.executeQuery( );
          while ( result.next( ) ) {
            Object columnValue = result.getObject( 1 );
            if ( Internets.testLocal( columnValue.toString( ) ) ) {
              return true;
            }
          }
        } finally {
          conn.close( );
        }
      } catch ( Exception ex ) {
        LOG.error( ex, ex );
      }
    }
    return false;
  }
  
  public static List<Host> list( ) {
    return Lists.newArrayList( hostMap.values( ) );
  }
  
  public static List<Host> list( final Predicate<Host> filter ) {
    return Lists.newArrayList( Iterables.filter( hostMap.values( ), filter ) );
  }
  
  public static List<Host> listDatabases( ) {
    return Hosts.list( DbFilter.INSTANCE );
  }
  
  public static Host localHost( ) {
    if ( ( hostMap == null ) || !hostMap.containsKey( Internets.localHostIdentifier( ) ) ) {
      return Coordinator.INSTANCE.createLocalHost( );
    } else {
      return hostMap.get( Internets.localHostIdentifier( ) );
    }
  }
  
  enum ModifiedTimeTransform implements Function<Host, Long> {
    INSTANCE;
    @Override
    public Long apply( final Host arg0 ) {
      return arg0.getTimestamp( ).getTime( );
    }
    
  }
  
  enum StartTimeTransform implements Function<Host, Long> {
    INSTANCE;
    @Override
    public Long apply( final Host arg0 ) {
      long startTime = arg0.isLocalHost( ) ? 0L : arg0.getStartedTime( );
      return startTime == Long.MAX_VALUE ? 0L : startTime;
    }
    
  }
  
  enum EpochTransform implements Function<Host, Integer> {
    INSTANCE;
    @Override
    public Integer apply( final Host arg0 ) {
      return arg0.getEpoch( );
    }
    
  }
  
  enum DbFilter implements Predicate<Host> {
    INSTANCE;
    @Override
    public boolean apply( final Host arg0 ) {
      return arg0.hasDatabase( );
    }
    
  }
  
  enum NonLocalFilter implements Predicate<Host> {
    INSTANCE;
    @Override
    public boolean apply( final Host arg0 ) {
      return !arg0.isLocalHost( );
    }
    
  }
  
  public enum Coordinator {
    INSTANCE;
    private final AtomicLong currentStartTime = new AtomicLong( Long.MAX_VALUE );
    
    /**
     * @param values
     */
    public void initialize( final Collection<Host> values ) {
      final long currentTime = System.currentTimeMillis( );
      long startTime = values.isEmpty( ) ? currentTime : Longs.max( Longs.toArray( Collections2.transform( values, StartTimeTransform.INSTANCE ) ) );
      startTime = startTime > currentTime ? startTime : currentTime;
      startTime += 30000;
      this.currentStartTime.compareAndSet( Long.MAX_VALUE, startTime );
    }
    
    /**
     * @return
     */
    public Host createLocalHost( ) {
      return new Host( Coordinator.INSTANCE.currentStartTime.get( ) );
    }
    
    public Boolean isLocalhost( ) {
      try {
        Host minHost = null;
        for ( Host h : Hosts.listDatabases( ) ) {
          minHost = ( minHost == null ? h : ( minHost.getStartedTime( ) > h.getStartedTime( ) ? h : minHost ) );
        }
        return minHost != null ? minHost.isLocalHost( ) : false;
      } catch ( final NoSuchElementException ex ) {
        return false;
      }
    }
    
    public long getCurrentStartTime( ) {
      return this.currentStartTime.get( );
    }
    
  }
  
  private static DriverDatabaseClusterMBean findDbClusterMBean( final String ctx ) throws NoSuchElementException {
    final DriverDatabaseClusterMBean cluster = Mbeans.lookup( jdbcJmxDomain,
                                                              ImmutableMap.builder( ).put( "cluster", ctx ).build( ),
                                                              DriverDatabaseClusterMBean.class );
    return cluster;
  }
  
  private static final String jdbcJmxDomain = "net.sf.hajdbc";
  
  private static void stopDbPool( final Host host ) {
    final String hostName = host.getBindAddress( ).getHostAddress( );
    
    for ( final String ctx : PersistenceContexts.list( ) ) {
      final String contextName = ctx.startsWith( "eucalyptus_" )
        ? ctx
        : "eucalyptus_" + ctx;
      
      try {
        final DriverDatabaseClusterMBean cluster = findDbClusterMBean( contextName );
        
        try {
          if ( cluster.getActiveDatabases( ).contains( hostName ) ) {
            cluster.deactivate( hostName );
          }
          if ( cluster.getInactiveDatabases( ).contains( hostName ) ) {
            cluster.remove( hostName );
          }
        } catch ( final Exception ex ) {
          LOG.error( ex, ex );
        }
      } catch ( final NoSuchElementException ex1 ) {
        LOG.error( ex1, ex1 );
      } catch ( final Exception ex1 ) {
        LOG.error( ex1, ex1 );
      }
    }
  }
  
  private static void syncDatabase( final Host host ) {
    if ( !host.hasBootstrapped( ) || !host.hasDatabase( ) || host.isLocalHost( ) ) {
      return;
    }
    for ( final String ctx : PersistenceContexts.list( ) ) {
      final String contextName = ctx.startsWith( "eucalyptus_" )
        ? ctx
        : "eucalyptus_" + ctx;
      
      final String dbUrl = "jdbc:" + ServiceUris.remote( Database.class, host.getBindAddress( ), contextName );
      
      try {
        final DriverDatabaseClusterMBean cluster = findDbClusterMBean( contextName );
        final String dbPass = SystemIds.databasePassword( );
        final String hostName = host.getBindAddress( ).getHostAddress( );
        final String realJdbcDriver = Databases.getDriverName( );
        if ( !cluster.getActiveDatabases( ).contains( hostName ) && !cluster.getInactiveDatabases( ).contains( hostName ) ) {
          cluster.add( hostName, realJdbcDriver, dbUrl );
        } else if ( cluster.getActiveDatabases( ).contains( hostName ) ) {
          cluster.deactivate( hostName );
        }
        final InactiveDatabaseMBean database = Mbeans.lookup( jdbcJmxDomain,
                                                              ImmutableMap.builder( )
                                                                          .put( "cluster", contextName )
                                                                          .put( "database", hostName )
                                                                          .build( ),
                                                              InactiveDatabaseMBean.class );
        database.setUser( "eucalyptus" );
        database.setPassword( dbPass );
        if ( Hosts.Coordinator.INSTANCE.isLocalhost( ) ) {
          cluster.activate( hostName, "full" );
        } else {
          cluster.activate( hostName, "passive" );
        }
      } catch ( final NoSuchElementException ex1 ) {
        LOG.error( ex1, ex1 );
      } catch ( final Exception ex1 ) {
        LOG.error( ex1, ex1 );
      }
    }
  }
}
