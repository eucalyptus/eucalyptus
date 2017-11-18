/*************************************************************************
 * Copyright 2008 Regents of the University of California
 * Copyright 2009-2016 Ent. Services Development Corporation LP
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

package com.eucalyptus.bootstrap;

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

import javax.annotation.Nullable;
import com.eucalyptus.util.Cidr;
import com.eucalyptus.util.async.Futures;
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
  private static final Logger                    LOG                        = Logger.getLogger( Hosts.class );

  @ConfigurableField( description = "Timeout for state transfers (in msec).",
                      initialInt = 10000 )
  public static Long                             STATE_TRANSFER_TIMEOUT     = 10000L;
  @ConfigurableField( description = "Timeout for state initialization (in msec).",
                      initialInt = 120000 )
  public static Long                             STATE_INITIALIZE_TIMEOUT   = 120000L;
  public static final long                       SERVICE_INITIALIZE_TIMEOUT = 10000L;
  private static ReplicatedHashMap<String, Host> hostMap;

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
    ;
    private final long                            interval;
    private static final ScheduledExecutorService hostPruner   = Executors.newScheduledThreadPool(
                                                                     32,
                                                                     Threads.threadFactory( "host-cleanup-pool-%d" ) );
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

  enum HostMapStateListener implements ReplicatedHashMap.Notification<String, Host> {
    INSTANCE;
    private static final ExecutorService dbActivation =
        Executors.newFixedThreadPool( 32, Threads.threadFactory( "host-db-activation-pool-%d" ) );

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
          dbActivation.submit( new Runnable( ) {

            @Override
            public void run( ) {
              if ( Bootstrap.isFinished( ) ) {
                BootstrapComponent.SETUP.apply( Hosts.lookup( hostKey ) );
              }
            }
          } );
        } else if ( Bootstrap.isFinished( ) && !host.isLocalHost( ) ) {
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
      final Host preMergeCoordinator = getCoordinator( );
      LOG.info("Hosts.viewChange(): merge   : pre-merge-coordinator=" + preMergeCoordinator );
      Runnable mergeViews = new Runnable() {
        /**
         * Was this host the coordinator when the merge view arrived (i.e., before the partition has been resolved)?
         */
        private final boolean coordinator = preMergeCoordinator!=null && preMergeCoordinator.isLocalHost();
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
            for ( View v : ( ( MergeView ) mergeView ).getSubgroups( ) ) {
              LOG.info( logPrefix( v ) + " localhost-member=" + v.containsMember( Hosts.getLocalGroupAddress( ) )
                        + "coordinator=[ group=" + v.getViewId( ).getCreator( )
                        + ", system=" + this.coordinatorAddress
                        + ", localhost=" + this.coordinator + "]" );
              LOG.info( logPrefix( v ) + Joiner.on( ", " ).join( v.getMembers( ) ) );

              /**
               * If this subgroup/partiton is not one we are a member of then sync the state from the coordinator, i.e. {@code org.jgroups.View#getMembers()#firstElement()} is the coordinator.
               */
              try {
                HostManager.getMembershipChannel().getState( v.getMembers().get( 0 ), 0L );
              } catch ( Exception e ) {
                LOG.error( logPrefix( v ) + " failed to merge partition state: " + e.getMessage() );
                Logs.extreme().error( e, e );
              }
            }
          } catch ( Exception ex ) {
            LOG.error( ex , ex );
          }
        }
      };
      Threads.newThread( mergeViews, Threads.threadUniqueName( "host-merge-view" ) ).start( );
    }

  }

  enum BootstrapComponent implements Predicate<Host> {
    SETUP {
      @Override
      public boolean apply( final Host input ) {
        if ( Bootstrap.isShuttingDown( ) ) {
          return false;
        } else if ( input.hasBootstrapped( ) ) {
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
            this.removeHost( input );
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
      return Groovyness.run( "setup_membership" );
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

  private static final Predicate<Host> FILTER_BOOTED_DBS = Predicates.and( DbFilter.INSTANCE, BootedFilter.INSTANCE );

  public static List<Host> listActiveDatabases( ) {
    return Hosts.list( DbFilter.INSTANCE );
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

  /**
   * Map the given host address to an alternative address matching the given cidr
   */
  public static InetAddress maphost( final InetAddress hostAddress,
                                     final InetAddress preferredLocalAddress,
                                     final Cidr mapToCidr ) {
    InetAddress result = hostAddress;
    try{
      final Host host = Hosts.lookup( hostAddress );
      if ( host != null && host.isLocalHost( ) ) {
        result = preferredLocalAddress;
      } else if ( host != null ) {
        result = Iterables.tryFind( host.getHostAddresses( ), mapToCidr ).or( result );
      }
    } catch( final Exception ex ){
      LOG.error( "Failed to map the host address: " + ex.getMessage( ) );
    }
    return result;
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

  @Nullable
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
      if ( !BootstrapArgs.isCloudController( ) ) {
        Coordinator.loggedWait( JoinShouldWait.NON_CLOUD_CONTROLLER );
        return JoinShouldWait.NON_CLOUD_CONTROLLER.get( );
      } else {
        return Hosts.localHost( );
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
    public boolean apply( Host coordinator ) {
      if ( coordinator != null && ( coordinator.isLocalHost( ) || coordinator.hasBootstrapped( ) ) ) {
        LOG.info( "Found system view with database: " + coordinator );
        return false;
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

  static void awaitDatabases( ) throws InterruptedException {
    if ( !BootstrapArgs.isCloudController( ) ) {
      while ( list( FILTER_BOOTED_DBS ).isEmpty( ) ) {
        TimeUnit.SECONDS.sleep( 3 );//GRZE: db state check sleep time
        LOG.info( "Waiting for system view with database..." );
        LOG.info( HostMapStateListener.INSTANCE.printMap( "Hosts.awaitDatabases():" ) );
      }
      if ( Databases.shouldInitialize( ) ) {
        doInitialize( );
      }
    } else if ( BootstrapArgs.isCloudController( ) && !Hosts.isCoordinator( ) ) {
      while ( AwaitDatabase.INSTANCE.apply( Hosts.getCoordinator( ) ) );
    }
  }

}
