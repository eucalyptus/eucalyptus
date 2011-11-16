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
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.log4j.Logger;
import org.jgroups.Address;
import org.jgroups.ChannelClosedException;
import org.jgroups.ChannelException;
import org.jgroups.ChannelNotConnectedException;
import org.jgroups.Global;
import org.jgroups.Header;
import org.jgroups.JChannel;
import org.jgroups.View;
import org.jgroups.blocks.ReplicatedHashMap;
import org.jgroups.conf.ClassConfigurator;
import org.jgroups.stack.Protocol;
import org.jgroups.stack.ProtocolStack;
import com.eucalyptus.component.Component;
import com.eucalyptus.component.Component.State;
import com.eucalyptus.component.ComponentId;
import com.eucalyptus.component.ComponentIds;
import com.eucalyptus.component.Components;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.ServiceConfigurations;
import com.eucalyptus.component.ServiceTransitions;
import com.eucalyptus.component.Topology;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.configurable.ConfigurableClass;
import com.eucalyptus.configurable.ConfigurableField;
import com.eucalyptus.empyrean.Empyrean;
import com.eucalyptus.event.EventListener;
import com.eucalyptus.event.Hertz;
import com.eucalyptus.event.Listeners;
import com.eucalyptus.records.Logs;
import com.eucalyptus.scripting.Groovyness;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.Internets;
import com.eucalyptus.util.Timers;
import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Collections2;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.primitives.Longs;

@ConfigurableClass( root = "bootstrap.hosts",
                    description = "Properties controlling the handling of remote host bootstrapping" )
public class Hosts {
  
  @ConfigurableField( description = "Timeout for state transfers (in msec).",
                      readonly = true )
  public static final Long                       STATE_TRANSFER_TIMEOUT     = 10000L;
  @ConfigurableField( description = "Timeout for state initialization (in msec).",
                      readonly = true )
  public static final Long                       STATE_INITIALIZE_TIMEOUT   = 120000L;
  static final Logger                            LOG                        = Logger.getLogger( Hosts.class );
  public static final long                       SERVICE_INITIALIZE_TIMEOUT = 10000L;
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
        Component component = Components.lookup( input );
        final ServiceConfiguration config = Internets.testLocal( addr ) ? component.initRemoteService( addr ) : component.initService( );
        LOG.info( "Initialized service: " + config.getFullName( ) );
        return config;
      }
    };
  }
  
  enum SetupRemoteServiceConfigurations implements Function<ServiceConfiguration, ServiceConfiguration> {
    INSTANCE;
    @Override
    public ServiceConfiguration apply( final ServiceConfiguration input ) {
      boolean inputIsLocal = Internets.testLocal( input.getHostName( ) );
      State goalState;
      if ( input.getComponentId( ).isAlwaysLocal( ) ) {
        goalState = State.ENABLED;
      } else if ( BootstrapArgs.isCloudController( ) ) {
        if ( inputIsLocal && Hosts.isCoordinator( ) ) {
          goalState = State.ENABLED;
        } else if ( !inputIsLocal && !Hosts.isCoordinator( ) ) {
          goalState = State.ENABLED;
        } else {
          goalState = State.DISABLED;
        }
      } else if ( Hosts.isCoordinator( input.getInetAddress( ) ) ) {
        goalState = State.ENABLED;
      } else {
        goalState = State.DISABLED;
      }
      LOG.info( "SetupRemoteServiceConfigurations: "
                + ( State.ENABLED.equals( goalState ) ? "Enabling" : "Disabling" )
                + " "
                + ( inputIsLocal ? "local" : "remote" )
                + " "
                + ( input.getComponentId( ).isAlwaysLocal( ) ? "bootstrap" : "cloud" )
                + " services" + ( Hosts.isCoordinator( input.getInetAddress( ) ) ? " (coordinator)" : "" )
                + ": " + input.getFullName( ) );
      try {
        return ServiceTransitions.pathTo( input, goalState ).get( SERVICE_INITIALIZE_TIMEOUT, TimeUnit.MILLISECONDS );
      } catch ( final ExecutionException ex ) {
        LOG.error( ex );
        Logs.extreme( ).error( ex, ex );
      } catch ( final InterruptedException ex ) {
        Thread.currentThread( ).interrupt( );
        Exceptions.trace( ex.getCause( ) );
      } catch ( TimeoutException ex ) {
        LOG.error( ex );
        Logs.extreme( ).error( ex, ex );
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
      this.delegate = new Predicate<ComponentId>( ) {
        @Override
        public boolean apply( final ComponentId input ) {
          return input.isAncestor( compId ) && !input.isRegisterable( );
        }
      };
      this.compId = compId;
    }
    
    @Override
    public boolean apply( final ComponentId input ) {
      return this.delegate.apply( input );
    }
    
    public static Collection<ComponentId> findDependentComponents( final Class<? extends ComponentId> comp, final InetAddress addr ) {
      return Collections2.filter( ComponentIds.list( ), Predicates.and( EMPYREAN.compId.equals( comp )
        ? EMPYREAN
        : EUCALYPTUS, nonLocalAddressFilter( addr ) ) );
    }
    
  }
  
  enum HostBootstrapEventListener implements EventListener<Hertz> {
    INSTANCE;
    
    @Override
    public void fireEvent( final Hertz event ) {
      final Host currentHost = Hosts.localHost( );
      if ( !BootstrapArgs.isCloudController( ) && currentHost.hasBootstrapped( ) && Databases.shouldInitialize( ) ) {
        System.exit( 123 );
      }
      if ( event.isAsserted( 15L ) ) {
        UpdateEntry.INSTANCE.apply( currentHost );
      }
//      Set<Address> currentMembers = Sets.newHashSet( hostMap.getChannel( ).getView( ).getMembers( ) );
//      Map<String, Host> hostCopy = Maps.newHashMap( hostMap );
//      Set<Address> currentHosts = Sets.newHashSet( Collections2.transform( hostCopy.values( ), GroupAddressTransform.INSTANCE ) );
//      Set<Address> strayHosts = Sets.difference( currentHosts, currentMembers );
//      for ( Address strayHost : strayHosts ) {
//        Host h = hostCopy.get( strayHost );
//        BootstrapComponent.TEARDOWN.apply( h );
//        hostMap.remove( strayHost );
//      }
    }
  }
  
  enum HostMapStateListener implements ReplicatedHashMap.Notification<String, Host> {
    INSTANCE;
    
    private String printMap( ) {
      return "Current System View\n" + Joiner.on( "\nhostMap.values(): " ).join( hostMap.values( ) );
    }
    
    @Override
    public void contentsCleared( ) {
      LOG.info( "Hosts.contentsCleared(): " + printMap( ) );
    }
    
    @Override
    public void contentsSet( final Map<String, Host> input ) {
      LOG.info( "Hosts.contentsSet(): " + this.printMap( ) );
      try {
        for ( final Host h : input.values( ) ) {
          BootstrapComponent.REMOTESETUP.apply( h );
        }
      } catch ( Exception ex ) {
        LOG.error( ex, ex );
      }
    }
    
    @Override
    public void entryRemoved( final String input ) {
      LOG.info( "Hosts.entryRemoved(): " + input );
      LOG.info( "Hosts.entryRemoved(): " + printMap( ) );
    }
    
    @Override
    public void entrySet( final String hostKey, final Host host ) {
      LOG.info( "Hosts.entrySet(): " + hostKey + " => " + host );
      try {
        if ( host.isLocalHost( ) && Bootstrap.isFinished( ) ) {
          LOG.info( "Hosts.entrySet(): BOOTSTRAPPED HOST => " + host );
          BootstrapComponent.SETUP.apply( host );
          if ( SyncDatabases.INSTANCE.apply( host ) ) {
            LOG.info( "Hosts.entrySet(): SYNCING HOST => " + host );
          }
        } else if ( BootstrapComponent.REMOTESETUP.apply( host ) ) {
          LOG.info( "Hosts.entrySet(): BOOTSTRAPPED HOST => " + host );
          if ( SyncDatabases.INSTANCE.apply( host ) ) {
            LOG.info( "Hosts.entrySet(): SYNCING HOST => " + host );
          }
        } else if ( InitializeAsCloudController.INSTANCE.apply( host ) ) {
          LOG.info( "Hosts.entrySet(): INITIALIZED CLC => " + host );
        } else {
          LOG.debug( "Hosts.entrySet(): UPDATED HOST => " + host );
        }
      } catch ( Exception ex ) {
        LOG.error( ex, ex );
      }
      LOG.info( "Hosts.entrySet(): " + printMap( ) );
    }
    
    @Override
    public void viewChange( final View currentView, final Vector<Address> joinMembers, final Vector<Address> partMembers ) {
      LOG.info( "Hosts.viewChange(): new view => " + Joiner.on( ", " ).join( currentView.getMembers( ) ) );
      if ( !joinMembers.isEmpty( ) ) LOG.info( "Hosts.viewChange(): joined   => " + Joiner.on( ", " ).join( joinMembers ) );
      if ( !partMembers.isEmpty( ) ) LOG.info( "Hosts.viewChange(): parted   => " + Joiner.on( ", " ).join( partMembers ) );
      for ( final Host h : Hosts.list( ) ) {
        if ( Iterables.contains( partMembers, h.getGroupsId( ) ) ) {
          BootstrapComponent.TEARDOWN.apply( h );
          LOG.info( "Hosts.viewChange(): -> removed  => " + h );
        }
      }
      LOG.info( "Hosts.viewChange(): " + printMap( ) );
    }
    
  }
  
  enum SyncDatabases implements Predicate<Host> {
    INSTANCE;
    
    @Override
    public boolean apply( final Host input ) {
      if ( input.hasBootstrapped( ) && input.hasDatabase( ) ) {
        return Databases.enable( input );
      } else {
        return false;
      }
    }
    
  }
  
  enum BootstrapComponent implements Predicate<Host> {
    SETUP {
      @Override
      public boolean apply( final Host input ) {
        try {
          setup( Empyrean.class, input.getBindAddress( ) );
          if ( input.hasDatabase( ) ) {
            setup( Eucalyptus.class, input.getBindAddress( ) );
          }
          return true;
        } catch ( Exception ex ) {
          LOG.error( ex, ex );
          return false;
        }
      }
    },
    TEARDOWN {
      @Override
      public boolean apply( final Host input ) {
        try {
          teardown( Empyrean.class, input.getBindAddress( ) );
          if ( input.hasDatabase( ) ) {
            teardown( Eucalyptus.class, input.getBindAddress( ) );
          }
          hostMap.remove( input.getDisplayName( ) );
          if ( !input.isLocalHost( ) && input.hasDatabase( ) && BootstrapArgs.isCloudController( ) ) {
            BootstrapComponent.SETUP.apply( Hosts.localHost( ) );
            UpdateEntry.INSTANCE.apply( Hosts.localHost( ) );
          }
          if ( input.hasDatabase( ) ) {
            Databases.disable( input );
          }
          return true;
        } catch ( Exception ex ) {
          LOG.error( ex, ex );
          return false;
        }
      }
    },
    REMOTESETUP {
      
      @Override
      public boolean apply( Host input ) {
        if ( !input.isLocalHost( ) && input.hasBootstrapped( ) ) {
          return BootstrapComponent.SETUP.apply( input );
        } else {
          return false;
        }
      }
      
    };
    
    private static <T extends ComponentId> boolean teardown( final Class<T> compClass, final InetAddress addr ) {
      if ( Internets.testLocal( addr ) ) {
        return false;
      } else {
        try {
          for ( final ComponentId c : ShouldLoadRemote.findDependentComponents( compClass, addr ) ) {
            try {
              final ServiceConfiguration dependsConfig = ServiceConfigurations.lookupByName( c.getClass( ), addr.getHostAddress( ) );
//              ServiceTransitions.pathTo( dependsConfig, State.PRIMORDIAL ).get( );
              Topology.destroy( dependsConfig ).get( );
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
    
    private static <T extends ComponentId> void setup( final Class<T> compId, final InetAddress addr ) {
      try {
        final Function<ComponentId, ServiceConfiguration> initFunc = Functions.compose( SetupRemoteServiceConfigurations.INSTANCE,
                                                                                        initRemoteSetupConfigurations( addr ) );
        initFunc.apply( ComponentIds.lookup( compId ) );
        final Collection<ComponentId> deps = ShouldLoadRemote.findDependentComponents( compId, addr );
        Iterables.transform( deps, initFunc );
      } catch ( final Exception ex ) {
        LOG.error( ex, ex );
      }
    }
  }
  
  enum CheckStale implements Predicate<Host> {
    INSTANCE;
    
    @Override
    public boolean apply( final Host input ) {
      if ( !input.isLocalHost( ) ) {
        return false;
      } else {
        final Host that = Host.create( );
        if ( that.hasBootstrapped( ) && !input.hasBootstrapped( ) ) {
          return true;
        } else if ( that.hasDatabase( ) && !input.hasDatabase( ) ) {
          return true;
        } else if ( that.getEpoch( ) > input.getEpoch( ) ) {
          return true;
        } else if ( that.hasSynced( ) && !input.hasSynced( ) ) {
          return true;
        } else if ( !that.getHostAddresses( ).equals( input.getHostAddresses( ) ) ) {
          return true;
        } else {
          return false;
        }
      }
    }
  }
  
  enum UpdateEntry implements Predicate<Host> {
    INSTANCE;
    
    @Override
    public boolean apply( final Host input ) {
      if ( input == null ) {
        final Host newHost = Host.create( );
        final Host oldHost = hostMap.putIfAbsent( newHost.getDisplayName( ), newHost );
        if ( oldHost != null ) {
          LOG.info( "Inserted local host information:   " + localHost( ) );
          return true;
        } else {
          return false;
        }
      } else if ( input.isLocalHost( ) ) {
        if ( CheckStale.INSTANCE.apply( input ) ) {
          final Host newHost = Host.create( );
          final Host oldHost = hostMap.replace( newHost.getDisplayName( ), newHost );
          if ( oldHost != null ) {
            LOG.info( "Updated local host information:   " + localHost( ) );
            return true;
          } else {
            return false;
          }
        } else {
          if ( !hostMap.containsKey( input.getDisplayName( ) ) ) {
            final Host newHost = Host.create( );
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
  
  enum InitializeAsCloudController implements Predicate<Host> {
    INSTANCE;
    
    @Override
    public boolean apply( final Host input ) {
      if ( !BootstrapArgs.isCloudController( ) && input.isLocalHost( ) && input.hasDatabase( ) ) {
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
        Runnable runMap = new Runnable( ) {
          public void run( ) {
            try {
              hostMap.start( STATE_INITIALIZE_TIMEOUT );
              OrderedShutdown.register( Eucalyptus.class, new Runnable( ) {
                
                @Override
                public void run( ) {
                  try {
                    try {
                      hostMap.remove( Internets.localHostIdentifier( ) );
                    } catch ( final Exception ex ) {
                      LOG.error( ex, ex );
                    }
                    hostMap.stop( );
                  } catch ( final Exception ex ) {
                    LOG.error( ex, ex );
                  }
                }
              } );
            } catch ( Exception ex ) {
              LOG.error( ex, ex );
              Exceptions.maybeInterrupted( ex );
              System.exit( 123 );
            }
          }
        };
        Timers.loggingWrapper( runMap, hostMap ).call( );
        LOG.info( "Initial view:\n" + HostMapStateListener.INSTANCE.printMap( ) );
        LOG.info( "Initial coordinator:\n" + Hosts.getCoordinator( ) );
        Coordinator.INSTANCE.initialize( hostMap.values( ) );
        final Host local = Host.create( );
        LOG.info( "Created local host entry: " + local );
        hostMap.put( local.getDisplayName( ), local );
        Listeners.register( HostBootstrapEventListener.INSTANCE );
        LOG.info( "System view:\n" + HostMapStateListener.INSTANCE.printMap( ) );
        LOG.info( "System coordinator:\n" + Hosts.getCoordinator( ) );
        if ( !BootstrapArgs.isCloudController( ) ) {
          while ( Hosts.listActiveDatabases( ).isEmpty( ) ) {
            TimeUnit.SECONDS.sleep( 5 );
            LOG.info( "Waiting for system view with database..." );
          }
          if ( Databases.shouldInitialize( ) ) {
            doInitialize( );
          }
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
  
  public static List<Host> list( ) {
    List<Host> hosts = Lists.newArrayList( );
    if ( hostMap != null ) {
      hosts.addAll( hostMap.values( ) );
    }
    return hosts;
  }
  
  public static List<Host> list( final Predicate<Host> filter ) {
    return Lists.newArrayList( Iterables.filter( list( ), filter ) );
  }
  
  public static List<Host> listDatabases( ) {
    return Hosts.list( DbFilter.INSTANCE );
  }
  
  private static final Predicate<Host> filterSyncedDbs = Predicates.and( DbFilter.INSTANCE, SyncedDbFilter.INSTANCE );
  
  public static List<Host> listActiveDatabases( ) {
    return Hosts.list( filterSyncedDbs );
  }
  
  public static Host localHost( ) {
    if ( ( hostMap == null ) || !hostMap.containsKey( Internets.localHostIdentifier( ) ) ) {
      return Host.create( );
    } else {
      return hostMap.get( Internets.localHostIdentifier( ) );
    }
  }
  
  enum ModifiedTimeTransform implements Function<Host, Long> {
    INSTANCE;
    @Override
    public Long apply( final Host input ) {
      return input.getTimestamp( ).getTime( );
    }
    
  }
  
  enum StartTimeTransform implements Function<Host, Long> {
    INSTANCE;
    @Override
    public Long apply( final Host input ) {
      final long startTime = input.isLocalHost( ) ? 0L : input.getStartedTime( );
      return startTime == Long.MAX_VALUE ? 0L : startTime;
    }
    
  }
  
  enum GroupAddressTransform implements Function<Host, Address> {
    INSTANCE;
    @Override
    public Address apply( final Host input ) {
      return input.getGroupsId( );
    }
    
  }
  
  enum EpochTransform implements Function<Host, Integer> {
    INSTANCE;
    @Override
    public Integer apply( final Host input ) {
      return input.getEpoch( );
    }
    
  }
  
  enum DbFilter implements Predicate<Host> {
    INSTANCE;
    @Override
    public boolean apply( final Host input ) {
      return input.hasDatabase( );
    }
    
  }
  
  enum SyncedDbFilter implements Predicate<Host> {
    INSTANCE;
    @Override
    public boolean apply( final Host input ) {
      return input.hasSynced( );
    }
    
  }
  
  enum NonLocalFilter implements Predicate<Host> {
    INSTANCE;
    @Override
    public boolean apply( final Host input ) {
      return !input.isLocalHost( );
    }
    
  }
  
  public static Long getStartTime( ) {
    return Coordinator.INSTANCE.getCurrentStartTime( );
  }
  
  public static boolean isCoordinator( InetAddress addr ) {
    Host coordinator = Hosts.getCoordinator( );
    return coordinator != null && coordinator.getBindAddress( ).equals( addr );
  }
  
  public static boolean isCoordinator( ) {
    return Coordinator.INSTANCE.isLocalhost( );
  }
  
  public static Host getCoordinator( ) {
    return Coordinator.INSTANCE.get( );
  }
  
  private enum Coordinator {
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
    
    public Boolean isLocalhost( ) {
      Host minHost = get( );
      if ( minHost == null && BootstrapArgs.isCloudController( ) ) {
        return true;
      } else if ( minHost != null ) {
        return minHost.isLocalHost( );
      } else {
        return false;
      }
    }
    
    public Host get( ) {
      Host minHost = null;
      List<Host> dbHosts = Hosts.listDatabases( );
      for ( final Host h : dbHosts ) {
        minHost = ( minHost == null ? h : ( minHost.getStartedTime( ) > h.getStartedTime( ) ? h : minHost ) );
      }
      return minHost;
    }
    
    public long getCurrentStartTime( ) {
      return this.currentStartTime.get( );
    }
    
  }
}
