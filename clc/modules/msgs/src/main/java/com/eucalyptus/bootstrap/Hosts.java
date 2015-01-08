/*************************************************************************
 * Copyright 2009-2014 Eucalyptus Systems, Inc.
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

package com.eucalyptus.bootstrap;

import static com.eucalyptus.bootstrap.Host.DBStatus;
import java.io.DataInput;
import java.io.DataOutput;
import java.net.InetAddress;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.eucalyptus.component.ServiceConfigurations;
import com.eucalyptus.event.EventListener;
import com.eucalyptus.event.Hertz;
import com.eucalyptus.event.Listeners;
import com.eucalyptus.util.CollectionUtils;
import com.eucalyptus.util.async.Futures;
import com.google.common.base.Optional;
import com.google.common.collect.*;
import org.apache.log4j.Logger;
import org.jgroups.*;
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
import com.eucalyptus.component.Topology;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.configurable.ConfigurableClass;
import com.eucalyptus.configurable.ConfigurableField;
import com.eucalyptus.empyrean.Empyrean;
import com.eucalyptus.records.Logs;
import com.eucalyptus.scripting.Groovyness;
import com.eucalyptus.system.Threads;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.Internets;
import com.eucalyptus.util.Timers;
import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.base.Supplier;
import com.google.common.primitives.Longs;

/**
 * egrep 'contentsSet|entrySet|entryRemoved|viewChange|Hosts.values' /disk1/storage/hi.log | sed
 * 's/INFO .*\(Hosts.*\)(): /\1(): /g' | less
 */
@ConfigurableClass( root = "bootstrap.hosts",
                    description = "Properties controlling the handling of remote host bootstrapping" )
public class Hosts {

  private static int                             REJOIN_BACKOFF_SECS        = Integer.parseInt( System.getProperty( "euca.bootstrap.rejoin.backoff", "30" ) );//GRZE: do not want to mess w/ this unless necessary -- its here just in case.
  @ConfigurableField( description = "Timeout for state transfers (in msec).",
                      readonly = true )
  public static final Long                       STATE_TRANSFER_TIMEOUT     = 10000L;
  @ConfigurableField( description = "Timeout for state initialization (in msec).",
                      readonly = true )
  public static final Long                       STATE_INITIALIZE_TIMEOUT   = 120000L;
  private static final Logger                            LOG                        = Logger.getLogger( Hosts.class );
  public static final long                       SERVICE_INITIALIZE_TIMEOUT = 10000L;
  private static ReplicatedHashMap<String, Host> hostMap;
  private static final ReentrantReadWriteLock    canHas                     = new ReentrantReadWriteLock( );

  public static Predicate<ServiceConfiguration> nonLocalAddressMatch( final InetAddress addr ) {
    return new Predicate<ServiceConfiguration>( ) {

      @Override
      public boolean apply( final ServiceConfiguration input ) {
        return input.getInetAddress( ).equals( addr ) || input.getInetAddress( ).getCanonicalHostName( ).equals( addr.getCanonicalHostName( ) )
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
        final ServiceConfiguration config = !Internets.testLocal( addr.getHostAddress( ) ) ? component.initRemoteService( addr ) : component.initService( );
        Logs.extreme( ).info( "Initialized service: " + config.getFullName( ) );
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
      if ( !Bootstrap.isFinished( ) || State.STOPPED.apply( input )  ) {
        return input;
      } else if ( input.getComponentId( ).isAlwaysLocal( ) ) {
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
      if ( State.ENABLED.apply( input ) && State.ENABLED.equals( goalState ) ) {
        return input;
      } else if ( State.DISABLED.apply( input ) && State.DISABLED.equals( goalState ) ) {
        return input;
      } else {
        LOG.info( "SetupRemoteServiceConfigurations: " + goalState + " "
                  + ( inputIsLocal ? "local" : "remote" ) + " "
                  + ( input.getComponentId( ).isAlwaysLocal( ) ? "bootstrap" : "cloud" ) + " services"
                  + ( Hosts.isCoordinator( input.getInetAddress( ) ) ? " (coordinator)" : "" ) + ": " + input.getFullName( ) );
        try {
          return Topology.transition( goalState ).apply( input ).get( );
        } catch ( final ExecutionException ex ) {
          LOG.error( ex );
          Logs.extreme( ).error( ex, ex );
        } catch ( final InterruptedException ex ) {
          Thread.currentThread( ).interrupt( );
          Exceptions.trace( ex.getCause( ) );
        }
        return input;
      }
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
      return Collections2.filter( ComponentIds.list( ), Predicates.and( EMPYREAN.compId.equals( comp ) ? EMPYREAN : EUCALYPTUS, nonLocalAddressFilter( addr ) ) );
    }

  }

  enum PeriodicMembershipChecks implements Runnable {
    ENTRYUPDATE( 2 ) {
      private volatile int counter = 0;

      @Override
      public void run( ) {
        final Host currentHost = Hosts.localHost( );
        ++this.counter;
        try {
          if ( !Hosts.list( Predicates.not( BootedFilter.INSTANCE ) ).isEmpty( ) && currentHost.hasDatabase( ) ) {
            if ( UpdateEntry.INSTANCE.apply( currentHost ) ) {
              LOG.info( "Updated local host entry while booting: " + currentHost );
            }
          } else if ( this.counter % 5 == 0 ) {
            if ( UpdateEntry.INSTANCE.apply( currentHost ) ) {
              LOG.info( "Updated changed local host entry: " + currentHost );
            } else {
              Logs.extreme( ).info( "Updated local host entry periodically: " + currentHost );
              Hosts.put( Host.create( ) );
            }
          }
        } catch ( Exception ex ) {
          LOG.debug( ex );
          Logs.extreme( ).debug( ex, ex );
        }
      }

    },
    PRUNING( 10 ) {

      @Override
      public void run( ) {
        if ( Hosts.pruneHosts( ) ) {
          Hosts.updateServices( );
        }
      }

    },
    INITIALIZE( 10 ) {

      @Override
      public void run( ) {
        final Host currentHost = Hosts.localHost( );
        if ( !BootstrapArgs.isCloudController( ) && currentHost.hasBootstrapped( ) && Databases.shouldInitialize( ) ) {
          System.exit( 123 );
        }
      }
      
    },
    SWITCH_COORDINATOR( 10 ) {
      private volatile boolean wasStoppedLocally = false;

      @Override
      public void run( ) {
        final boolean wasStoppedPreviously = wasStoppedLocally;
        final boolean stoppedLocally = wasStoppedLocally = eucalyptusStoppedLocally( );
        if ( wasStoppedPreviously &&
            stoppedLocally &&
            Hosts.isCoordinator( ) && 
            Hosts.listDatabases( ).size( ) > 1 ) {
          LOG.info( "Relinquishing coordinator role: " + Hosts.getCoordinator() );
          Coordinator.INSTANCE.reset( );
        }
      }

    }
    ;
    private final long                            interval;
    private static final ScheduledExecutorService hostPruner   = Executors.newScheduledThreadPool( 32 );
    private static final Lock                     canHasChecks = new ReentrantLock( );

    private PeriodicMembershipChecks( long interval ) {
      this.interval = interval;
    }

    public static void setup( ) {
      for ( final PeriodicMembershipChecks runner : PeriodicMembershipChecks.values( ) ) {
        Runnable safeRunner = new Runnable( ) {

          @Override
          public void run( ) {
            if ( !Bootstrap.isLoaded( ) || Bootstrap.isShuttingDown( ) ) {
              return;
            } else {
              try {
                if ( PeriodicMembershipChecks.canHasChecks.tryLock( 1, TimeUnit.SECONDS ) ) {
                  try {
                    Logs.extreme( ).debug( runner.toString( ) + ": RUNNING" );
                    try {
                      runner.run( );
                    } catch ( Exception ex ) {
                      LOG.error( runner.toString( ) + ": FAILED because of: " + ex.getMessage( ) );
                      Logs.extreme( ).error( runner.toString( ) + ": FAILED because of: " + ex.getMessage( ), ex );
                    }
                  } finally {
                    PeriodicMembershipChecks.canHasChecks.unlock( );
                  }
                }
              } catch ( Exception ex ) {
                Exceptions.maybeInterrupted( ex );
                LOG.debug( runner.toString( ) + ": SKIPPED: " + ex.getMessage( ) );
                Logs.extreme( ).debug( ex, ex );
              }
            }
          }
        };
        LOG.info( "Registering " + runner + " for execution every " + runner.getInterval( ) + " seconds" );
        hostPruner.scheduleAtFixedRate( safeRunner, 0L, runner.getInterval( ), TimeUnit.SECONDS );
      }
    }

    private long getInterval( ) {
      return this.interval;
    }

    @Override
    public String toString( ) {
      return "Hosts.PeriodicMembershipChecks." + this.name( );
    }

    public static List<Runnable> shutdownNow( ) {
      return hostPruner.shutdownNow( );
    }

    public void submit( ) {
      hostPruner.execute( this );
    }

  }

  private static boolean pruneHosts( ) {
    try {
      Set<Address> currentMembers = Sets.newHashSet( hostMap.getChannel( ).getView( ).getMembers( ) );
      Map<String, Host> hostCopy = Maps.newHashMap( hostMap );
      Set<Address> currentHosts = Sets.newHashSet( Collections2.transform( hostCopy.values( ), GroupAddressTransform.INSTANCE ) );
      Set<Address> strayHosts = Sets.difference( currentHosts, currentMembers );
      if ( !strayHosts.isEmpty( ) ) {
        LOG.info( "Pruning orphan host entries: " + strayHosts );
        for ( Address strayHost : strayHosts ) {
          Host h = hostCopy.get( strayHost.toString( ) );
          if ( h == null ) {
            LOG.debug( "Pruning failed to find host copy for orphan host: " + h );
            h = Hosts.lookup( strayHost.toString( ) );
            LOG.debug( "Pruning fell back to underlying host map for orphan host: " + h );
          }
          if ( h != null ) {
            LOG.info( "Pruning orphan host: " + h );
            BootstrapComponent.TEARDOWN.apply( h );
          } else {
            LOG.info( "Pruning failed for orphan host: " + strayHost
                      + " with local-copy value: "
                      + hostCopy.get( strayHost.toString( ) )
                      + " and underlying host map value: "
                      + Hosts.lookup( strayHost ) );
          }
        }
      } else {
        return false;
      }
    } catch ( Exception ex ) {
      LOG.debug( ex );
      Logs.extreme( ).debug( ex, ex );
    }
    return true;
  }

  private static void updateServices( ) {
    try {
      if ( !Topology.isEnabled( Eucalyptus.class ) && Hosts.getCoordinator( ) != null ) {
        LOG.info( "Setting up new coordinator: " + Hosts.getCoordinator( ) );
        BootstrapComponent.SETUP.apply( Hosts.getCoordinator( ) );
      } else if ( !Hosts.isCoordinator( ) && Bootstrap.isFinished( ) ) {
        BootstrapComponent.SETUP.apply( Hosts.localHost( ) );
        UpdateEntry.INSTANCE.apply( Hosts.localHost( ) );
      }
    } catch ( Exception ex ) {
      LOG.debug( ex );
      Logs.extreme( ).debug( ex, ex );
    }
  }

  private static boolean eucalyptusStoppedLocally( ) {
    return !Iterables.isEmpty( ServiceConfigurations.filter(
        Eucalyptus.class,
        Predicates.and( ServiceConfigurations.filterHostLocal(), State.STOPPED ) ) );
  }

  enum HostMapStateListener implements ReplicatedHashMap.Notification<String, Host> {
    INSTANCE;
    private static final ExecutorService dbActivation = Executors.newFixedThreadPool( 32 );

    private String printMap( String prefix ) {
      String currentView = HostManager.getMembershipChannel( ).getViewAsString( );
      return "\n" + prefix + " " + currentView + "\n" + prefix + " " + Joiner.on( "\n" + prefix + " " ).join( hostMap.values( ) );
    }

    @Override
    public void contentsCleared( ) {
      LOG.info( this.printMap( "Hosts.contentsCleared():" ) );
    }

    @Override
    public void contentsSet( final Map<String, Host> input ) {
      LOG.info( this.printMap( "Hosts.contentsSet():" ) );
      if ( Bootstrap.isShuttingDown( ) ) {
        return;
      } else {
        for ( final Host host : input.values( ) ) {
          HostMapStateListener.updateHostEntry( host );
        }
      }
    }

    @Override
    public void entryRemoved( final String input ) {
      LOG.info( "Hosts.entryRemoved(): " + input );
      dbActivation.submit( new Runnable( ) {

        @Override
        public void run( ) {
          try {
            Databases.disable( input );
          } catch ( Exception ex ) {
            LOG.error( ex , ex );
          }
        }
      } );
    }

    @Override
    public void entrySet( final String hostKey, final Host host ) {
      if ( Bootstrap.isShuttingDown( ) ) {
        return;
      } else {
        LOG.debug( "Hosts.entrySet(): " + hostKey + " => " + host );
        HostMapStateListener.updateHostEntry( host );
        LOG.debug( "Hosts.entrySet(): " + hostKey + " finished." );
      }
    }

    private static void updateHostEntry( final Host host ) {
      try {
        final String hostKey = host.getDisplayName( );
        if ( host.isLocalHost( ) && host.hasDatabase( ) && Bootstrap.isLoaded( ) ) {
          final boolean wasSynched = Databases.isSynchronized( );
          final boolean wasVolatile = Databases.isVolatile( );
          dbActivation.submit( new Runnable( ) {

            @Override
            public void run( ) {
              if ( !wasSynched && !Databases.SyncState.SYNCING.isCurrent( ) && !Databases.isSynchronized( ) ) {
                if ( Databases.enable( host ) && Databases.isSynchronized( ) ) {
                  UpdateEntry.INSTANCE.apply( Hosts.lookup( hostKey ) );
                }
              } else if ( wasVolatile && Bootstrap.isLoaded( ) && !Databases.ActiveHostSet.ACTIVATED.get( ).contains( hostKey ) ) {
                if ( Databases.enable( host ) && !Databases.isVolatile( ) ) {
                  UpdateEntry.INSTANCE.apply( Hosts.lookup( hostKey ) );
                }
              } else if ( wasVolatile && Bootstrap.isLoaded( ) && !Databases.isVolatile( ) ) {
                UpdateEntry.INSTANCE.apply( Hosts.lookup( hostKey ) );
              } else if ( Bootstrap.isFinished( ) && !Databases.isVolatile( ) ) {
                BootstrapComponent.SETUP.apply( Hosts.lookup( hostKey ) );
              }
            }
          } );
        } else if ( Bootstrap.isFinished( ) && !host.isLocalHost( ) && host.hasSynced( ) ) {
          BootstrapComponent.REMOTESETUP.apply( host );
        } else if ( InitializeAsCloudController.INSTANCE.apply( host ) ) {
          LOG.info( "Hosts.entrySet(): INITIALIZED CLC => " + host );
        } else {
          Logs.extreme( ).debug( "Hosts.updateHostEntry(): UPDATED HOST => " + host );
        }
      } catch ( Exception ex ) {
        LOG.error( ex, ex );
      }
    }

    @Override
    public void viewChange( final View currentView, final List<Address> joinMembers, final List<Address> partMembers ) {
      LOG.info( "Hosts.viewChange(): new view [" + currentView.getViewId().getId() + ":" + currentView.getViewId().getCreator() + "]=> "
                + Joiner.on( ", " ).join( currentView.getMembers( ) ) );
      LOG.info(  printMap( "Hosts.viewChange(before):" ) );
      if ( !joinMembers.isEmpty( ) ) LOG.info( "Hosts.viewChange(): joined   [" + currentView.getViewId().getId() + ":" + currentView.getViewId().getCreator() + "]=> "
                                               + Joiner.on( ", " ).join( joinMembers ) );
      if ( !partMembers.isEmpty( ) ) LOG.info( "Hosts.viewChange(): parted   [" + currentView.getViewId().getId() + ":" + currentView.getViewId().getCreator() + "]=> "
                                               + Joiner.on( ", " ).join( partMembers ) );
      List<Address> allHostAddresses = Lists.transform( Hosts.list( ), GroupAddressTransform.INSTANCE );
      Collection<Address> partedHosts = Collections2.filter( allHostAddresses, Predicates.in( partMembers ) );
      for ( final Address hostAddress : partedHosts ) {
        LOG.info( "Hosts.viewChange(): -> removed  => " + hostAddress );
      }
      if ( !partMembers.isEmpty( ) ) {
        Threads.lookup( Empyrean.class, Hosts.class, "viewChange" ).submit( PeriodicMembershipChecks.PRUNING );
      }
      Collection<Address> joinedHosts = Collections2.filter( allHostAddresses, Predicates.in( joinMembers ) );
      for ( final Address hostAddress : joinedHosts ) {
        LOG.info( "Hosts.viewChange(): -> added    => " + hostAddress );
      }
      if ( currentView instanceof MergeView ) {
        this.handleMergeView( ( MergeView ) currentView );
      } 
      LOG.info(  printMap( "Hosts.viewChange(after):" ) );
      LOG.info( "Hosts.viewChange(): new view finished." );
    }

    /**
     * When we get a MergeView all hosts need to:
     * <ol>
     * <li>Update their map copies from non-member partition's coordinator with {@link ReplicatedHashMap#setState(java.io.InputStream)}.</li>
     * <li>Check to see if the current view state is compatible with their previous view state.</li>
     * <li>Fail-stop if an inconsistency exists.</li>
     * </ol>
     */
    private void handleMergeView( final MergeView mergeView ) {
      /**
       * Which host was the coordinator for the partition this host belongs to.
       */
      final Host preMergeCoordinator = Coordinator.INSTANCE.get();
      LOG.info("Hosts.viewChange(): merge   : pre-merge-coordinator=" + preMergeCoordinator );
      Runnable mergeViews = new Runnable() {
        /**
         * Was this host the coordinator when the merge view arrived (i.e., before the partiton has been resolved)?
         */
        private final boolean coordinator = preMergeCoordinator.isLocalHost();
        /**
         * Which host was the coordinator for the partition this host belongs to.
         */
        private final String coordinatorAddress = preMergeCoordinator != null ? preMergeCoordinator.getDisplayName() : "NONE";

        private String logPrefix( final View v ) {
          final ViewId viewId = v == null ? null : v.getViewId( );
          final String id = viewId == null ? "?" : Objects.toString( viewId.getId() );
          final String creator = viewId == null ? "?" : Objects.toString( viewId.getCreator( ), "?" );
          return "Hosts.viewChange(): merge   [" +id + ":" + creator + "]=> ";
        }

        @Override
        public void run( ) {
          try {
            Map<String, View> partitions = Maps.newHashMap( );
            View localView = null;
            for ( View v : ( ( MergeView ) mergeView ).getSubgroups( ) ) {
              LOG.info( logPrefix( v ) + " localhost-member=" + v.containsMember( Hosts.getLocalGroupAddress( ) )
                        + "coordinator=[ group=" + v.getViewId( ).getCreator( )
                        + ", system=" + this.coordinatorAddress
                        + ", localhost=" + this.coordinator + "]" );
              LOG.info( logPrefix( v ) + Joiner.on( ", " ).join( v.getMembers( ) ) );

              /**
               * If this subgroup/partiton is not one we are a member of then sync the state from the coordinator, i.e. {@code org.jgroups.View#getMembers()#firstElement()} is the coordinator.
               */
              Address viewCoordinator = v.getMembers().get( 0 );
              if ( viewCoordinator.equals( Hosts.getLocalGroupAddress( ) ) ) {
                localView = v;
              }
              try {
                HostManager.getMembershipChannel().getState( v.getMembers().get( 0 ), 0L );
              } catch ( Exception e ) {
                LOG.error( logPrefix( v ) + " failed to merge partition state: " + e.getMessage() );
                Logs.extreme().error( e, e );
              }

              /**
               * Make a map of all group members and their views
               */
              for ( Address addr : v.getMembers() ) {
                partitions.put( addr.toString(), v );
              }
            }
            /**
             * At this point local state is up-to-date and only CLCs need to proceed
             */
            if ( BootstrapArgs.isCloudController( ) ) {
              boolean partitioned = false;
              /**
               * Check if any DB was partitioned from the local view.
               */
              Set<View> dbViews = Sets.newHashSet( );//used only for logging
              for ( Host db : Hosts.listDatabases() ) {
                View dbView = partitions.get( db.getDisplayName( ) );
                if ( !dbView.equals( localView ) ) {
                  partitioned = true;
                }
                dbViews.add( dbView );
              }
              /**
               * Check to ensure that if this host was not the original coordinator it isn't restarted as the coordinator.
               */
              Host newCoordinator = Coordinator.INSTANCE.get( );
              if ( !coordinatorAddress.equals( newCoordinator.getDisplayName() ) ) {
                partitioned = true;
              }
              
              if ( !partitioned ) {//no partition, keep going ==> happy time.
                return;
              } else if ( !newCoordinator.isLocalHost( ) && this.coordinator ) {//was coordinator, am not now ==> failstop
                LOG.error( "PARTITION FAIL-STOP:  Possibility for inconsistency detected for Host: " + Hosts.localHost() );
                LOG.error( "PARTITION FAIL-STOP: " + printMap( "Hosts.handleMergeView():" ) );
                Databases.Locks.PARTITIONED.create( logPrefix( localView ) + " found partitioned database in subgroup views: " + Joiner.on( ", " ).join( dbViews ) );
                Databases.Locks.PARTITIONED.failStop( );
              } else if ( newCoordinator.isLocalHost( ) && this.coordinator ) {//was coordinator and continue to be ==> backup and keep going
                LOG.error( "PARTITION CONTINUE:  Possibility for inconsistency detected for hosts in the following views: " + Joiner.on( ", " ).join( dbViews ) );
              } else if ( !newCoordinator.isLocalHost( ) && !this.coordinator ) {//wasn't coordinator, still am not AND someone else is ==> restart to resync data
                LOG.error( "PARTITION RESTART:  Possibility for stale data copy detected for Host: " + Hosts.localHost() );
                LOG.error( "PARTITION RESTART: " + printMap( "Hosts.handleMergeView():" ) );
                Databases.Locks.PARTITIONED.create( logPrefix( localView ) + " found different coordinator " + newCoordinator );
                Databases.Locks.PARTITIONED.failStop( );
//              SystemBootstrapper.restart( );
              } else if ( newCoordinator.isLocalHost( ) && !this.coordinator ) {//wasn't coordinator, but somehow am now (wtf?) ==> pretty sure this is badness ==> failstop
                LOG.error( "PARTITION FAIL-STOP:  Possibility for inconsistency detected for Host: " + Hosts.localHost() );
                LOG.error( "PARTITION FAIL-STOP: " + printMap( "Hosts.handleMergeView():" ) );
                Databases.Locks.PARTITIONED.create( logPrefix( localView ) + " found partitioned database in subgroup views: " + Joiner.on( ", " ).join( dbViews ) );
                Databases.Locks.PARTITIONED.failStop( );
              }
            }
          } catch ( Exception ex ) {
            LOG.error( ex , ex );
          }
        }
      };
      Threads.newThread( mergeViews ).start( );
    }

  }

  enum SyncDatabases implements Predicate<Host> {
    INSTANCE;

    @Override
    public boolean apply( final Host input ) {
      if ( !Hosts.contains( input.getGroupsId() ) ) {
        return false;
      } else if ( Hosts.isCoordinator( input ) && input.hasBootstrapped( ) && !input.isLocalHost( ) ) {
        return Databases.enable( input );
      } else if ( input.hasDatabase( ) && input.hasSynced( ) && !input.isLocalHost( ) ) {
        return Databases.enable( input );
      } else if ( input.isLocalHost( ) && !Databases.isSynchronized( ) ) {
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
        if ( Bootstrap.isShuttingDown( ) ) {
          return false;
        } else if ( input.hasBootstrapped( ) ) {
          SyncDatabases.INSTANCE.apply( input );
          setup( Empyrean.class, input.getBindAddress( ) );
          if ( input.hasDatabase( ) ) {
            return setup( Eucalyptus.class, input.getBindAddress( ) );
          } else {
            return true;
          }
        } else {
          return false;
        }
      }
    },
    TEARDOWN {
      @Override
      public boolean apply( final Host input ) {
        if ( Bootstrap.isShuttingDown( ) || input.isLocalHost( ) ) {
          return false;
        } else {
          try {
            if ( input.hasDatabase( ) ) {
              Databases.disable( input.getDisplayName( ) );
              this.removeHost( input );
            } else {
              this.removeHost( input );
            }
          } catch ( Exception ex ) {
            LOG.error( ex, ex );
            return false;
          }
          try {
            this.tryPromoteSelf( input );
            return true;
          } catch ( Exception ex ) {
            LOG.error( ex, ex );
            return false;
          }
        }
      }

      private void tryPromoteSelf( final Host input ) {
        if ( input.hasDatabase( ) && BootstrapArgs.isCloudController( ) ) {
          BootstrapComponent.SETUP.apply( Hosts.localHost( ) );
          UpdateEntry.INSTANCE.apply( Hosts.localHost( ) );
        }
      }

      private void removeHost( final Host input ) {
        if ( Hosts.isCoordinator( ) ) {
          Hosts.remove( input.getDisplayName( ) );
        } else if ( !Hosts.hasCoordinator( ) || Hosts.isCoordinator( input ) ) {
          Hosts.remove( input.getDisplayName( ) );
        } else {
          //GRZE:NOTE: this case is a remote non-coordinator in a system which has a coordinator.
        }
        teardown( Empyrean.class, input.getBindAddress( ) );
        if ( input.hasDatabase( ) ) {
          teardown( Eucalyptus.class, input.getBindAddress( ) );
        }
      }
    },
    REMOTESETUP {

      @Override
      public boolean apply( Host input ) {
        if ( !input.isLocalHost( ) ) {
          return BootstrapComponent.SETUP.apply( input );
        } else {
          return false;
        }
      }
    };

    public abstract boolean apply( Host input );

    private static <T extends ComponentId> boolean teardown( final Class<T> compClass, final InetAddress addr ) {
      if ( Internets.testLocal( addr ) || !Bootstrap.isOperational() ) {
        return false;
      } else {
        try {
          Map<ServiceConfiguration,Future<ServiceConfiguration>> disabled = Maps.newHashMap();
          for ( final ComponentId c : ShouldLoadRemote.findDependentComponents( compClass, addr ) ) {
            try {
              for ( final ServiceConfiguration s : Components.lookup( compClass ).services( ) ) {
                try {
                  if ( s.getHostName( ).equals( addr.getHostAddress( ) ) ) {
                    Future<ServiceConfiguration> disable = Topology.disable( s );
                    disabled.put( s, disable );
                  }
                } catch ( final Exception ex ) {
                  LOG.error( ex );
                  Logs.extreme( ).error( ex, ex );
                }
              }
            } catch ( Exception ex ) {
              Logs.extreme( ).error( ex, ex );
            }
          }
//GRZE: should be no reason we need to wait for the torn down futures to complete, but better safe than sorry.
          Futures.waitAll( disabled );
        } catch ( final Exception ex ) {
          LOG.error( ex );
          Logs.extreme( ).error( ex, ex );
          return false;
        }
        return true;
      }
    }

    private static <T extends ComponentId> boolean setup( final Class<T> compId, final InetAddress addr ) {
      try {
        final Function<ComponentId, ServiceConfiguration> initFunc = Functions.compose( SetupRemoteServiceConfigurations.INSTANCE,
                                                                                        initRemoteSetupConfigurations( addr ) );
        initFunc.apply( ComponentIds.lookup( compId ) );
        final Collection<ComponentId> deps = ShouldLoadRemote.findDependentComponents( compId, addr );
        Iterables.transform( deps, initFunc );
        return true;
      } catch ( final Exception ex ) {
        LOG.error( ex );
        Logs.extreme( ).error( ex, ex );
        return false;
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
        } else if ( !that.getDatabaseStatus().equals( input.getDatabaseStatus() ) ) {
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
        final Host oldHost = Hosts.putIfAbsent( newHost );
        if ( oldHost != null ) {
          LOG.info( "Inserted local host information:   " + localHost( ) );
          return true;
        } else {
          return false;
        }
      } else if ( input.isLocalHost( ) ) {
        if ( CheckStale.INSTANCE.apply( input ) ) {
          final Host newHost = Host.create( );
          final Host oldHost = Hosts.put( newHost );
          if ( oldHost != null ) {
            LOG.info( "Updated local host information:   " + localHost( ) );
            return true;
          } else {
            return false;
          }
        } else {
          final Host newHost = Host.create( );
          final Host oldHost = Hosts.putIfAbsent( newHost );
          if ( oldHost == null ) {
            LOG.info( "Inserted local host information:   " + localHost( ) );
            return true;
          } else {
            return false;
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

  public static ImmutableList<DBStatus> getDBStatus( ) {
    final List<DBStatus> status = Lists.newArrayList( );
    for ( final String cluster : Databases.listRegisteredDatabases( ) ) {
      status.add( new DBStatus(
          cluster,
          Databases.listPrimaryActiveDatabases( cluster ),
          Databases.listSecondaryActiveDatabases( cluster ),
          Databases.listInactiveDatabases( cluster )
      ) );
    }
    return ImmutableList.copyOf( status );
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
      this.membershipChannel = buildChannel();
      //TODO:GRZE:set socket factory for crypto
      try {
        LOG.info( "Starting membership channel... " );
        this.membershipChannel.connect( SystemIds.membershipGroupName( ) );
        HostManager.registerHeader( EpochHeader.class );
        this.membershipChannel.down( new org.jgroups.Event( org.jgroups.Event.GET_PHYSICAL_ADDRESS, this.membershipChannel.getAddress( ) ) );
        LOG.info( "Started membership channel: " + SystemIds.membershipGroupName( ) );
      } catch ( final Exception ex ) {
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
      return "euca-" + ( h.isAnonymousClass( ) ? h.getSuperclass( ).getSimpleName( ).toLowerCase( ) : h.getSimpleName( ).toLowerCase( ) ) + "-header";
    }

    private static List<Protocol> getMembershipProtocolStack( ) {
      return Groovyness.run( "setup_membership.groovy" );
    }

    private static synchronized void start( ) {
      if ( singleton == null ) {
        singleton = new HostManager( );
      }
    }

    private static JChannel singletonChannel;
    private static synchronized JChannel buildChannel( ) {
      if ( singletonChannel == null ) {
        try {
          final JChannel channel = new JChannel( false );
          channel.setName( Internets.localHostIdentifier( ) );
          final ProtocolStack stack = new ProtocolStack( );
          channel.setProtocolStack( stack );
          stack.addProtocols( HostManager.getMembershipProtocolStack( ) );
          stack.init( );
          singletonChannel = channel;
          return channel;
        } catch ( final Exception ex ) {
          LOG.fatal( ex, ex );
          throw new RuntimeException( ex );
        }
      } else {
        return singletonChannel;
      }
    }

    public static JChannel getMembershipChannel( ) {
      return singletonChannel != null ? singletonChannel : buildChannel();
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
      public void writeTo( final DataOutput out ) throws Exception {
        out.writeInt( this.value );
      }

      @Override
      public void readFrom( final DataInput in ) throws Exception {
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
        //GRZE: 1. we must build the channel
        JChannel jchannel = HostManager.buildChannel( );
        LOG.info( "Started membership channel " + SystemIds.membershipGroupName( ) );
        //GRZE: 2. then start the map
        hostMap = new ReplicatedHashMap<String, Host>( jchannel );
        hostMap.setBlockingUpdates( true );
        //GRZE: 3. the connect the group
        HostManager.start( );
        Runnable runMap = new Runnable( ) {
          public void run( ) {
            try {
              hostMap.start( STATE_INITIALIZE_TIMEOUT );
              OrderedShutdown.registerPreShutdownHook( new Runnable( ) {

                @Override
                public void run( ) {
                  try {
                    for ( Runnable r : PeriodicMembershipChecks.shutdownNow( ) ) {
                      LOG.info( "SHUTDOWN: Pending host pruning task: " + r );
                    }
                  } catch ( Exception ex1 ) {
                    LOG.error( ex1, ex1 );
                  }
                  try {
                    hostMap.removeNotifier( HostMapStateListener.INSTANCE );
                    try {
                      if ( Hosts.contains( Internets.localHostIdentifier( ) ) ) {
                        Hosts.remove( Internets.localHostIdentifier( ) );
                      }
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

        /** initialize distributed system host state **/
        LOG.info( "Initial view: " + HostMapStateListener.INSTANCE.printMap( "Hosts.load():" ) );
        LOG.info( "Searching for potential coordinator: " + Hosts.getCoordinator( ) );
        Hosts.Coordinator.INSTANCE.await( );
        Coordinator.INSTANCE.initialize( hostMap.values( ) );


        /** create host entry for localhost **/
        LOG.info( "Created local host entry: " + Hosts.localHost( ) );
        hostMap.addNotifier( HostMapStateListener.INSTANCE );
        LOG.info( "System view: " + HostMapStateListener.INSTANCE.printMap( "Hosts.load():" ) );
        UpdateEntry.INSTANCE.apply( Hosts.localHost( ) );
        LOG.info( "System coordinator: " + Hosts.getCoordinator( ) );
        //TODO:GRZE:enable this
        //        Hosts.checkHostVersions( );

        /** wait for db if needed **/
        Hosts.awaitDatabases( );
        LOG.info( "Membership address for localhost: " + Hosts.localHost( ) );

        /** setup remote host states **/
        for ( final Host h : hostMap.values( ) ) {
          BootstrapComponent.REMOTESETUP.apply( h );
        }

        /** setup host map handling **/
        PeriodicMembershipChecks.setup( );

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

  private static final Predicate<Host> FILTER_SYNCED_DBS        = Predicates.and( DbFilter.INSTANCE, SyncedDbFilter.INSTANCE );
  private static final Predicate<Host> FILTER_BOOTED_SYNCED_DBS = Predicates.and( FILTER_SYNCED_DBS, BootedFilter.INSTANCE );

  public static List<Host> listActiveDatabases( ) {
    return Hosts.list( FILTER_SYNCED_DBS );
  }

  private static Host put( final Host newHost ) {
    return hostMap.put( newHost.getDisplayName( ), newHost );
  }

  private static Host putIfAbsent( final Host host ) {
    return hostMap.putIfAbsent( host.getDisplayName( ), host );
  }

  public static Host lookup( final Address hostGroupAddress ) {
    return lookup( hostGroupAddress.toString( ) );
  }

  public static Host lookup( final String hostDisplayName ) {
    if ( hostMap.containsKey( hostDisplayName ) ) {
      return hostMap.get( hostDisplayName );
    } else {
      final InetAddress addr = Internets.toAddress( hostDisplayName );
      Hosts.list( new Predicate<Host>( ) {

        @Override
        public boolean apply( Host input ) {
          if ( input.getBindAddress( ).equals( addr ) ) {
            return true;
          } else if ( input.getHostAddresses( ).contains( addr ) ) {
            return true;
          } else {
            return false;
          }
        }
      } );
    }
    return null;
  }

  public static Host lookup( final InetAddress address ) {
    if ( hostMap.containsKey( address.getHostAddress( ) ) ) {
      return hostMap.get( address.getHostAddress( ) );
    } else {
      return Iterables.tryFind( Hosts.list( ), new Predicate<Host>( ) {
        @Override
        public boolean apply( Host input ) {
          return input.getHostAddresses( ).contains( address );
        }
      } ).orNull( );
    }
  }

  public static boolean contains( final String hostDisplayName ) {
    return hostMap.containsKey( hostDisplayName );
  }

  static boolean contains( final Address hostGroupAddress ) {
    if ( !HostManager.getMembershipChannel( ).getView( ).containsMember( hostGroupAddress ) ) {
      return false;
    } else {
      return contains( hostGroupAddress.toString( ) );
    }
  }

  static Host remove( final Address hostGroupAddress ) {
    return remove( hostGroupAddress.toString( ) );
  }

  static Host remove( String hostDisplayName ) {
    Host ret = null;
    try {
      ret = hostMap.remove( hostDisplayName );
      LOG.info( "Removing host map entry for: " + hostDisplayName + " => " + ret );
    } catch ( RuntimeException e ) {
      LOG.info( "Removing host map entry for: " + hostDisplayName + " => " + e.getMessage( ) );
    }
    return ret;
  }

  public static Host localHost( ) {
    if ( ( hostMap == null ) || !hostMap.containsKey( Internets.localHostIdentifier( ) ) ) {
      return Host.create( );
    } else {
      return lookup( Internets.localHostIdentifier( ) );
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

  enum NameTransform implements Function<Host, String> {
    INSTANCE;
    @Override
    public String apply( final Host input ) {
      return input.getDisplayName( );
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

  enum BootedFilter implements Predicate<Host> {
    INSTANCE;
    @Override
    public boolean apply( final Host input ) {
      return input.hasBootstrapped( );
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

  enum DbStatusErrorFilter implements Predicate<Host> {
    INSTANCE;
    @Override
    public boolean apply( final Host input ) {
      return !Iterables.isEmpty( Optional.presentInstances(
          Iterables.transform( input.getDatabaseStatus( ), DbStatusErrorTransform.INSTANCE ) ) );
    }
  }

  enum DbStatusErrorTransform implements Function<DBStatus,Optional<String>> {
    INSTANCE;
    @Override
    public Optional<String> apply( final DBStatus status ) {
      return status.getError( );
    }
  }

  public static Long getStartTime( ) {
    return Coordinator.INSTANCE.getCurrentStartTime( );
  }

  public static boolean isCoordinator( Host host ) {
    return isCoordinator( host.getBindAddress( ) );
  }

  public static boolean isCoordinator( InetAddress addr ) {
    Host coordinator = Hosts.getCoordinator( );
    return coordinator != null && coordinator.getBindAddress( ).equals( addr );
  }

  public static void failstop( ) {
    Coordinator.INSTANCE.reset( );
  }

  public static boolean hasCoordinator( ) {
    return Coordinator.INSTANCE.get( ) != null;
  }

  public static boolean isCoordinator( ) {
    return Coordinator.INSTANCE.isLocalhost( );
  }

  public static Host getCoordinator( ) {
    return Coordinator.INSTANCE.get( );
  }

  enum JoinShouldWait implements Predicate<Host>, Supplier<Host> {
    CLOUD_CONTROLLER {
      @Override
      public boolean apply( Host input ) {
        if ( input == null ) {
          return false;
        } else if ( !input.hasBootstrapped( ) ) {
          return true;
        } else if ( !input.hasSynced( ) ) {
          return true;
        } else {
          return false;
        }
      }

      @Override
      public Host get( ) {
        return Coordinator.find( Hosts.listDatabases( ) );
      }
    },
    NON_CLOUD_CONTROLLER {
      @Override
      public boolean apply( Host input ) {
        if ( input == null ) {
          return true;
        } else if ( !input.hasBootstrapped( ) ) {
          return true;
        } else if ( !input.hasSynced( ) ) {
          return true;
        } else {
          return false;
        }
      }

      @Override
      public Host get( ) {
        return Coordinator.find( Hosts.listActiveDatabases( ) );
      }
    };

    public abstract boolean apply( Host input );

    public abstract Host get( );
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
      startTime = startTime > currentTime ? startTime + 30000 : currentTime;
      if ( this.currentStartTime.compareAndSet( Long.MAX_VALUE, startTime ) ) {
        Hosts.put( Hosts.localHost( ) );
      }
    }

    public void reset( ) {
      currentStartTime.set( Long.MAX_VALUE );
      initialize( hostMap.values( ) );
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

    public Host get( ) {//GRZE: this needs to use active DBs to avoid db-sync race.
      List<Host> dbHosts = Hosts.listActiveDatabases( );
      return find( dbHosts );
    }

    public Host await( ) {//GRZE: this needs to use all DBs to ensure waiting for booting coordinator
      while ( !Hosts.isCoordinator( ) && AwaitDatabase.INSTANCE.apply( Hosts.getCoordinator( ) ) );
      Host coord = get( );
      if ( !BootstrapArgs.isCloudController( ) ) {
        Coordinator.loggedWait( JoinShouldWait.NON_CLOUD_CONTROLLER );
        return JoinShouldWait.NON_CLOUD_CONTROLLER.get( );
      } else {//if ( BootstrapArgs.isCloudController( ) ) {
        Coordinator.loggedWait( JoinShouldWait.CLOUD_CONTROLLER );
        if ( coord == null ) {
          return Hosts.localHost( );
        } else {
          return coord;
        }
      }
    }

    private static void loggedWait( JoinShouldWait waitFunction ) {
      for ( Host h = waitFunction.get( ); waitFunction.apply( h ); h = waitFunction.get( ) ) {
        try {
          LOG.info( "Waiting for cloud coordinator to become ready: " + h );
          TimeUnit.MILLISECONDS.sleep( 1000 );
        } catch ( InterruptedException ex ) {
          Exceptions.maybeInterrupted( ex );
        }
      }
    }

    private static Host find( List<Host> dbHosts ) {
      Host minHost = null;
      for ( final Host h : dbHosts ) {
        if ( minHost == null ) {
          minHost = h;
        } else if ( minHost.getStartedTime( ) > h.getStartedTime( ) ) {
          minHost = h;
        } else if ( minHost.getStartedTime( ).equals( h.getStartedTime( ) ) && !minHost.getDisplayName( ).equals( h.getDisplayName( ) ) ) {
          minHost = ( minHost.getDisplayName( ).compareTo( h.getDisplayName( ) ) == -1 ? minHost : h );
        }
      }
      return minHost;
    }

    public long getCurrentStartTime( ) {
      return this.currentStartTime.get( );
    }

  }

  public static boolean isServiceLocal( final ServiceConfiguration parent ) {
    return parent.isVmLocal( ) || ( parent.isHostLocal( ) && isCoordinator( ) );
  }

  enum AwaitDatabase implements Predicate<Host> {
    INSTANCE;

    @Override
    public boolean apply( Host c ) {
      if ( c != null && ( c.isLocalHost( ) || c.hasBootstrapped( ) ) ) {
        LOG.info( "Found system view with database: " + c );
        /**
         * Ensure that cloud controller is not too aggressive about rejoining
         * membership group. A host which leaves the system and returns to the
         * group too quickly will race with fault detection time outs.
         *
         * Here a CLC which is joining a group which has a coordinator already (so, an ENABLED CLC)
         * will wait at least {@code 10*REJOIN_BACKOFF_SECS} (150 seconds, by default) after the
         * declared {@code coordinator.getStartedTime()} to ensure that everyone's timeout has
         * popped and
         * service state has settled.
         *
         * In case of emergency adjust value using -Deuca.bootstrap.rejoin.backoff
         */
        long now = System.currentTimeMillis( );
        long coordTime = c.getStartedTime( );
        long earliestJoin = coordTime + ( REJOIN_BACKOFF_SECS * 5L * 1000L );
        if ( now > earliestJoin ) {
          return false;
        } else {
          try {
            LOG.info( "Waiting for system view to settle till "
                      + earliestJoin
                      + " for coordinator "
                      + coordTime
                      + " ("
                      + ( earliestJoin - now )
                      / 1000l
                      + " secs)." );
            TimeUnit.SECONDS.sleep( REJOIN_BACKOFF_SECS );
          } catch ( InterruptedException ex ) {
            Exceptions.maybeInterrupted( ex );
          }
          return true;
        }
      } else {
        try {
          TimeUnit.SECONDS.sleep( 3 );//GRZE: db state check sleep time
          LOG.info( "Waiting for system view with database..." );
        } catch ( InterruptedException ex ) {
          Exceptions.maybeInterrupted( ex );
        }
        return true;
      }
    }

  }

  private static Predicate<DBStatus> consistentWith( final DBStatus status1 ) {
    return new Predicate<DBStatus>() {
      @Override
      public boolean apply( final DBStatus status2 ) {
        return status2.consistentWith( status1 );
      }
    };
  }

  private static Function<List<DBStatus>,Function<DBStatus,List<DBStatus>>> distinctStatus( ) {
    return new Function<List<DBStatus>,Function<DBStatus,List<DBStatus>>>(){
      @Override
      public Function<DBStatus, List<DBStatus>> apply( final List<DBStatus> statusList ) {
        return new Function<DBStatus, List<DBStatus>>(){
          @Override
          public List<DBStatus> apply( final DBStatus input ) {
            if ( !Iterables.any( statusList, consistentWith( input ) ) ) {
              statusList.add( input );
            }
            return statusList;
          }
        };
      }
    };
  }

  static void awaitDatabases( ) throws InterruptedException {
    if ( !BootstrapArgs.isCloudController( ) ) {
      while ( list( FILTER_BOOTED_SYNCED_DBS ).isEmpty( ) ) {
        TimeUnit.SECONDS.sleep( 3 );//GRZE: db state check sleep time
        LOG.info( "Waiting for system view with database..." );
        LOG.info( HostMapStateListener.INSTANCE.printMap( "Hosts.awaitDatabases():" ) );
      }
      if ( Databases.shouldInitialize( ) ) {
        doInitialize( );
      }
    } else if ( BootstrapArgs.isCloudController( ) && !Hosts.isCoordinator( ) ) {
      while ( AwaitDatabase.INSTANCE.apply( Hosts.getCoordinator( ) ) );
      TimeUnit.SECONDS.sleep( REJOIN_BACKOFF_SECS );
    }
  }

  public static List<String> getHostDatabaseErrors( ) {
    List<String> errors = Collections.emptyList( );
    if ( Bootstrap.isOperational( ) && !Databases.isVolatile( ) ) {
      errors = getHostDatabaseErrors( Hosts.getCoordinator( ), Hosts.list( ) );
    }
    return errors;
  }

  static List<String> getHostDatabaseErrors( final Host coordinator,
                                             final List<Host> hosts ) {
    final List<String> errors = Lists.newArrayList( );
    if ( coordinator != null ) {
      final List<DBStatus> distinctStatus = CollectionUtils.reduce(
          coordinator.getDatabaseStatus( ),
          Lists.<DBStatus>newArrayList( ),
          distinctStatus( ) );
      final Iterable<String> coordinatorErrors = Optional.presentInstances(
          Iterables.transform( coordinator.getDatabaseStatus(), DbStatusErrorTransform.INSTANCE ) );
      if ( distinctStatus.size( ) > 1 || !Iterables.isEmpty( coordinatorErrors ) ) {
        if ( distinctStatus.size( ) > 1 ) {
          errors.add( String.format( "Host %s database error: Inconsistent primary/secondary databases %s",
              coordinator.getDisplayName( ), distinctStatus ) );
        }

        // report per-host errors since we can't check for consistency against invalid primary
        for ( final Host host : Iterables.filter( hosts, DbStatusErrorFilter.INSTANCE ) ) {
          for ( final String error : Optional.presentInstances(
              Iterables.transform( host.getDatabaseStatus(), DbStatusErrorTransform.INSTANCE ) ) ) {
            errors.add( String.format( "Host %s database error: %s", host.getDisplayName( ), error ) );
          }
        }
      } else if ( distinctStatus.size( ) == 1 ) for ( final Host host : hosts ) {
        final List<DBStatus> inconsistentStatus = Lists.newArrayList( Iterables.filter(
            host.getDatabaseStatus( ),
            Predicates.not( consistentWith( distinctStatus.get( 0 ) ) ) ) );
        if ( !inconsistentStatus.isEmpty( ) ) {
          errors.add( String.format( "Host %s database error: Inconsistent primary/secondary databases %s",
              host.getDisplayName( ), inconsistentStatus ) );
        }
      }
    }
    return errors;
  }

  public static class HostDatabaseCheckListener implements EventListener<Hertz> {
    public static void register( ) {
      Listeners.register( Hertz.class, new HostDatabaseCheckListener() );
    }

    @Override
    public void fireEvent( final Hertz event ) {
      if ( BootstrapArgs.isCloudController() && event.isAsserted( 60 ) ) {
        for ( final String error : getHostDatabaseErrors( ) ) {
          LOG.error( error );
        }
      }
    }
  }
}
