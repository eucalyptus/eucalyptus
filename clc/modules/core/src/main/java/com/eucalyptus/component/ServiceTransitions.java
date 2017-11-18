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

package com.eucalyptus.component;

import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import com.eucalyptus.component.events.ServiceEvents;
import org.apache.log4j.Logger;
import com.eucalyptus.bootstrap.Bootstrap;
import com.eucalyptus.bootstrap.BootstrapArgs;
import com.eucalyptus.bootstrap.Databases;
import com.eucalyptus.bootstrap.Host;
import com.eucalyptus.bootstrap.Hosts;
import com.eucalyptus.component.Component.State;
import com.eucalyptus.component.Component.Transition;
import com.eucalyptus.component.Faults.CheckException;
import com.eucalyptus.configurable.ConfigurableProperty;
import com.eucalyptus.configurable.MultiDatabasePropertyEntry;
import com.eucalyptus.configurable.PropertyDirectory;
import com.eucalyptus.configurable.SingletonDatabasePropertyEntry;
import com.eucalyptus.configurable.StaticDatabasePropertyEntry;
import com.eucalyptus.configurable.StaticPropertyEntry;
import com.eucalyptus.empyrean.DescribeServicesResponseType;
import com.eucalyptus.empyrean.DescribeServicesType;
import com.eucalyptus.empyrean.DisableServiceResponseType;
import com.eucalyptus.empyrean.DisableServiceType;
import com.eucalyptus.empyrean.Empyrean;
import com.eucalyptus.empyrean.EmpyreanMessage;
import com.eucalyptus.empyrean.EnableServiceResponseType;
import com.eucalyptus.empyrean.EnableServiceType;
import com.eucalyptus.empyrean.ServiceId;
import com.eucalyptus.empyrean.ServiceStatusType;
import com.eucalyptus.empyrean.StartServiceResponseType;
import com.eucalyptus.empyrean.StartServiceType;
import com.eucalyptus.empyrean.StopServiceResponseType;
import com.eucalyptus.empyrean.StopServiceType;
import com.eucalyptus.entities.EntityCache;
import com.eucalyptus.records.EventRecord;
import com.eucalyptus.records.EventType;
import com.eucalyptus.records.Logs;
import com.eucalyptus.util.Callback;
import com.eucalyptus.util.Callback.Completion;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.TypeMappers;
import com.eucalyptus.util.async.AsyncRequests;
import com.eucalyptus.util.async.CheckedListenableFuture;
import com.eucalyptus.util.async.Futures;
import com.eucalyptus.util.fsm.Automata;
import com.eucalyptus.util.fsm.OrderlyTransitionException;
import com.eucalyptus.util.fsm.TransitionAction;
import com.eucalyptus.util.fsm.TransitionRecord;
import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.ObjectArrays;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.collection.Stream;

public class ServiceTransitions {
  static Logger                                     LOG   = Logger.getLogger( ServiceTransitions.class );
  private static final Component.State[]            EMPTY = {};
  private static final Supplier<Void>               STATIC_PROPERTIES_SUPPLIER = Suppliers.memoizeWithExpiration( StaticPropertiesAdd.INSTANCE, 10, TimeUnit.SECONDS );
  LoadingCache<TransitionActions, ServiceTransitionCallback> hi    = CacheBuilder.newBuilder().build( new CacheLoader<TransitionActions, ServiceTransitionCallback>( ) {
                                                            
                                                            @Override
                                                            public ServiceTransitionCallback load( TransitionActions input ) {
                                                              return null;
                                                            }
                                                          } );
  
  private static Component.State[] sequence( Component.State... states ) {
    return states;
  }
  
  interface ServiceTransitionCallback {
    public void fire( ServiceConfiguration parent ) throws Exception;
  }
  
  /**
   * GRZE:FIXME: this is a shoddy static method for definining the prefered path from n_0 to n_1 for
   * n_0,n_1 \in G the state machine; think dijkstra.
   **/
  @SuppressWarnings( "unchecked" )
  public static CheckedListenableFuture<ServiceConfiguration> pathTo( final ServiceConfiguration configuration, final Component.State goalState ) {
    try {
      State[] path = null;
      State initialState = configuration.lookupState( );
      switch ( goalState ) {
        case LOADED:
          path = pathToLoaded( initialState );
          break;
        case DISABLED:
          path = pathToDisabled( initialState );
          break;
        case ENABLED:
          path = pathToEnabled( initialState );
          break;
        case STOPPED:
          path = pathToStopped( initialState );
          break;
        case NOTREADY:
          path = pathToStarted( initialState );
          break;
        case PRIMORDIAL:
          path = pathToPrimordial( initialState );
          break;
        case BROKEN:
          path = pathToBroken( initialState );
          break;
        case INITIALIZED:
          path = pathToInitialized( initialState );
          break;
      }
      if ( !initialState.equals( goalState ) ) {
				Logs.extreme().debug(configuration.getName() + " transitioning "
														 + initialState + "->" + goalState
														 + " using path " + Joiner.on("->").join(path));
			}
      CheckedListenableFuture<ServiceConfiguration> result = executeTransition( configuration, Automata.sequenceTransitions( configuration, path ) );
      return result;
    } catch ( RuntimeException ex ) {
			Logs.extreme().error(configuration.getName() + " failed to transition to "
													 + goalState
													 + " because of: "
													 + Exceptions.causeString(ex));
			Logs.extreme( ).error( ex, ex );
      throw ex;
    }
  }
  
  private static State[] pathToBroken( Component.State fromState ) {
    State[] transition = new State[] { fromState };
    switch ( fromState ) {
      case BROKEN:
        break;
      default:
        transition = ObjectArrays.concat( ServiceTransitions.pathToPrimordial( fromState ), Component.State.BROKEN );
        break;
    }
    return transition;
  }
  
  private static State[] pathToPrimordial( Component.State fromState ) {
    State[] transition = new State[] { fromState };
    switch ( fromState ) {
      case PRIMORDIAL:
        break;
      default:
        transition = ObjectArrays.concat( ServiceTransitions.pathToStopped( fromState ), Component.State.PRIMORDIAL );
        break;
    }
    return transition;
  }
  
  private static State[] pathToLoaded( Component.State fromState ) {
    State[] transition = new State[] { fromState };
    switch ( fromState ) {
      case LOADED:
        break;
      default:
        transition = ObjectArrays.concat( ServiceTransitions.pathToInitialized( fromState ), Component.State.LOADED );
        break;
    }
    return transition;
  }
  
  private static final State[] pathToInitialized( final Component.State fromState ) {
    State[] transition = new State[] { fromState };
    switch ( fromState ) {
      case LOADED:
        transition = ObjectArrays.concat( fromState, pathToInitialized( Component.State.NOTREADY ) );
        break;
      case ENABLED:
        transition = ObjectArrays.concat( transition, Component.State.DISABLED );
        //$FALL-THROUGH$
      case DISABLED:
      case NOTREADY:
        transition = ObjectArrays.concat( transition, Component.State.STOPPED );
        //$FALL-THROUGH$
      case BROKEN:
      case PRIMORDIAL:
      case STOPPED:
        transition = ObjectArrays.concat( transition, Component.State.INITIALIZED );
        break;
      case INITIALIZED:
        break;
    }
    return transition;
  }
  
  private static State[] pathToStarted( Component.State fromState ) {
    State[] transition = new State[] { fromState };
    switch ( fromState ) {
      case NOTREADY:
        break;
      case LOADED:
        transition = ObjectArrays.concat( transition, Component.State.NOTREADY );
        break;
      default:
        transition = ObjectArrays.concat( pathToLoaded( fromState ), Component.State.NOTREADY );
    }
    return transition;
  }
  
  private static final State[] pathToDisabled( final Component.State fromState ) {
    State[] transition = new State[] { fromState };
    switch ( fromState ) {
      case ENABLED:
      case DISABLED:
      case NOTREADY:
        transition = ObjectArrays.concat( transition, Component.State.DISABLED );
        break;
      default:
        transition = ObjectArrays.concat( pathToStarted( fromState ), Component.State.DISABLED );
    }
    return transition;
  }
  
  private static final State[] pathToEnabled( final Component.State fromState ) {
    State[] transition = new State[] { fromState };
    switch ( fromState ) {
      case ENABLED:
        transition = ObjectArrays.concat( transition, Component.State.ENABLED );
        break;
      default:
        transition = ObjectArrays.concat( pathToDisabled( fromState ), Component.State.ENABLED );
    }
    return transition;
  }
  
  private static final State[] pathToStopped( final Component.State fromState ) {
    State[] transition = new State[] { fromState };
    switch ( fromState ) {
      case ENABLED:
        transition = ObjectArrays.concat( transition, Component.State.DISABLED );
        //$FALL-THROUGH$
      case DISABLED:
      case NOTREADY:
      case BROKEN:
        transition = ObjectArrays.concat( transition, Component.State.STOPPED );
        break;
      case STOPPED:
        break;
      default:
        transition = ObjectArrays.concat( pathToStarted( fromState ), Component.State.STOPPED );
    }
    return transition;
  }
  
  private static CheckedListenableFuture<ServiceConfiguration> executeTransition( final ServiceConfiguration config, Callable<CheckedListenableFuture<ServiceConfiguration>> transition ) {
    if ( transition != null ) {
      try {
        return transition.call( );
      } catch ( Exception ex ) {
        return Futures.predestinedFailedFuture( ex );
      }
    } else {
      return Futures.predestinedFuture( config );
    }
  }
  
  private static <T extends EmpyreanMessage> T sendEmpyreanRequest( final ServiceConfiguration parent, final EmpyreanMessage msg ) throws Exception {
    ServiceConfiguration config = ServiceConfigurations.createEphemeral( Empyrean.INSTANCE, parent.getInetAddress( ) );
    Logs.extreme( ).debug( "Sending request " + msg.getClass( ).getSimpleName( ) + " to " + parent.getFullName( ) );
    try {
      if ( BootstrapArgs.debugTopology( ) == null ) {
        T reply = ( T ) AsyncRequests.sendSync( config, msg );
        return reply;
      } else {
        return msg.getReply( );
      }
      
    } catch ( Exception ex ) {
      Logs.extreme( ).error( parent.getFullName( ) + " failed request because of: " + ex.getMessage( ), ex );
      throw ex;
    }
  }
  
  private static void processTransition( final ServiceConfiguration parent, final Completion transitionCallback, final TransitionActions transitionAction ) {
    TransitionRecord<ServiceConfiguration, State, Transition> transitionRecord = parent.lookupStateMachine( ).getTransitionRecord( );
    ServiceTransitionCallback trans = null;
    try {
      if ( Hosts.isServiceLocal( parent ) ) {
        trans = ServiceLocalTransitionCallbacks.valueOf( transitionAction.name( ) );
      } else if ( Hosts.isCoordinator( ) ) {
        trans = CloudRemoteTransitionCallbacks.valueOf( transitionAction.name( ) );
      } else {
        trans = ServiceRemoteTransitionNotification.valueOf( transitionAction.name( ) );
      }
      if ( trans != null ) {
        Logs.extreme( ).debug( "Executing transition: " + trans.getClass( )
                               + "."
                               + transitionAction.name( )
                               + " for "
                               + parent );
        trans.fire( parent );
      }
      transitionCallback.fire( );
      Faults.flush( parent );
    } catch ( Exception ex ) {
      Logs.extreme().info( parent.getName( ) + " failed " + transitionAction.name( ) + ": " + ex.getMessage( ) );
      if ( Faults.filter( parent, ex ) ) {
        transitionCallback.fireException( ex );
        Faults.submit( parent, transitionRecord, Faults.failure( parent, ex ) );
        throw Exceptions.toUndeclared( ex );
      } else {
        transitionCallback.fire( );
        Faults.submit( parent, transitionRecord, Faults.advisory( parent, ex ) );
      }
//    } finally {
//      transitionCallback.fire( );
    }
  }
  
  public enum TransitionActions implements TransitionAction<ServiceConfiguration> {
    ENABLE,
    CHECK,
    DISABLE,
    START,
    LOAD,
    STOP,
    DESTROY;
    
    @Override
    public boolean before( final ServiceConfiguration parent ) {
      try {
        EventRecord.here( ServiceBuilder.class,
                          EventType.SERVICE_TRANSITION_BEFORE,
                          this.name( ),
                          parent.lookupState( ).toString( ),
                          parent.getFullName( ).toString( ),
                          parent.toString( ) ).exhaust( );
      } catch ( Exception ex ) {
        LOG.error( ex, ex );
      }
      return true;
    }
    
    @Override
    public void leave( ServiceConfiguration parent, Completion transitionCallback ) {
      try {
        EventRecord.here( ServiceBuilder.class,
          EventType.SERVICE_TRANSITION,
          this.name( ),
          parent.lookupState( ).toString( ),
          parent.getFullName( ).toString( ),
          parent.toString( ) ).exhaust( );
        ServiceTransitions.processTransition( parent, transitionCallback, this );
      } catch ( Exception ex ) {
        transitionCallback.fireException( ex );
      }
    }
    
    @Override
    public void enter( final ServiceConfiguration parent ) {
      try {
        EventRecord.here( ServiceBuilder.class,
                          EventType.SERVICE_TRANSITION_ENTER_STATE,
                          this.name( ),
                          parent.lookupState( ).toString( ),
                          parent.getFullName( ).toString( ),
                          parent.toString( ) ).exhaust( );
      } catch ( Exception ex ) {
        LOG.error( ex, ex );
      }
    }
    
    @Override
    public void after( final ServiceConfiguration parent ) {
      try {
        EventRecord.here( ServiceBuilder.class,
                          EventType.SERVICE_TRANSITION_AFTER_STATE,
                          this.name( ),
                          parent.lookupState( ).toString( ),
                          parent.getFullName( ).toString( ),
                          parent.toString( ) ).exhaust( );
      } catch ( Exception ex ) {
        LOG.error( ex, ex );
      }
      
    }
    
  }
  
  enum CloudRemoteTransitionCallbacks implements ServiceTransitionCallback {
    LOAD {
      
      @Override
      public void fire( final ServiceConfiguration parent ) throws Exception {}
    },
    DESTROY {
      
      @Override
      public void fire( final ServiceConfiguration parent ) throws Exception {}
    },
    CHECK {
      
      @Override
      public void fire( final ServiceConfiguration parent ) throws Exception {
        if ( !parent.getComponentId( ).isDistributedService( ) ) {
          return;
        } else {
          CheckException errors = null;
          Host h = Hosts.lookup( parent.getHostName( ) );
          if ( h == null ) {
            UnknownHostException ex = new UnknownHostException( "Failed to lookup host " + parent.getHostName( )
                                                                + " for service "
                                                                + parent.getFullName( )
                                                                + ".  Current hosts are: "
                                                                + Hosts.list( ) );
            errors = Faults.failure( parent, ex );
          } else if ( !h.hasBootstrapped( ) ) {
            UnknownHostException ex = new UnknownHostException( "Host " + parent.getHostName( )
                                                                + " not yet bootstrapped for service "
                                                                + parent.getFullName( )
                                                                + "." );
            errors = Faults.failure( parent, ex );
          } else {
            DescribeServicesType describeServicesType = new DescribeServicesType();
            describeServicesType.getServices( ).add( TypeMappers.transform( parent, ServiceId.class ) );
            DescribeServicesResponseType response = ServiceTransitions.sendEmpyreanRequest( parent, describeServicesType );
            ServiceStatusType status = Iterables.find( response.getServiceStatuses( ), new Predicate<ServiceStatusType>( ) {
              
              @Override
              public boolean apply( final ServiceStatusType arg0 ) {
                return parent.getName( ).equals( arg0.getServiceId( ).getName( ) );
              }
            } );
            errors = Faults.transformToExceptions( ).apply( status );
          }
          if ( Faults.Severity.FATAL.equals( errors.getSeverity( ) ) ) {
//            Faults.failstop( parent, errors ); makes no sense!
            throw errors;
          } else if ( Faults.Severity.TRACE.equals( errors.getSeverity( ) ) ) {
            Logs.extreme( ).error( errors, errors );
            return;
          } else if ( errors.getSeverity( ).ordinal( ) < Faults.Severity.ERROR.ordinal( ) ) {
            Logs.extreme( ).error( errors, errors );
          } else {
            throw errors;
          }
        }
      }
      
    },
    START {
      
      @Override
      public void fire( final ServiceConfiguration parent ) throws Exception {
        StartServiceType startServiceType = new StartServiceType( );
        startServiceType.getServices( ).add( TypeMappers.transform( parent, ServiceId.class ) );
        StartServiceResponseType msg = ServiceTransitions.sendEmpyreanRequest( parent, startServiceType );
        try {
          ServiceBuilders.lookup( parent.getComponentId( ) ).fireStart( parent );
        } catch ( Exception ex ) {
          LOG.error( ex, ex );
        }
      }
    },
    ENABLE {
      
      @Override
      public void fire( final ServiceConfiguration parent ) throws Exception {
        EnableServiceType enableServiceType = new EnableServiceType( );
        enableServiceType.getServices( ).add( TypeMappers.transform( parent, ServiceId.class ) );
        EnableServiceResponseType msg = ServiceTransitions.sendEmpyreanRequest( parent, enableServiceType );
        try {
          ServiceBuilders.lookup( parent.getComponentId( ) ).fireEnable( parent );
        } catch ( Exception ex ) {
          LOG.error( ex, ex );
        }
        
      }
    },
    DISABLE {
      
      @Override
      public void fire( final ServiceConfiguration parent ) throws Exception {
        DisableServiceType disableServiceType = new DisableServiceType( );
        disableServiceType.getServices( ).add( TypeMappers.transform( parent, ServiceId.class ) );
        DisableServiceResponseType msg = ServiceTransitions.sendEmpyreanRequest( parent, disableServiceType );
        try {
          ServiceBuilders.lookup( parent.getComponentId( ) ).fireDisable( parent );
        } catch ( Exception ex ) {
          LOG.error( ex, ex );
        }
      }
    },
    STOP {
      
      @Override
      public void fire( final ServiceConfiguration parent ) throws Exception {
        StopServiceType stopServiceType = new StopServiceType( );
        stopServiceType.getServices( ).add( TypeMappers.transform( parent, ServiceId.class ) );
        StopServiceResponseType msg = ServiceTransitions.sendEmpyreanRequest( parent, stopServiceType );
        try {
          ServiceBuilders.lookup( parent.getComponentId( ) ).fireStop( parent );
        } catch ( Exception ex ) {
          LOG.error( ex, ex );
        }
      }
    };

    private static LoadingCache<TransitionActions, ServiceTransitionCallback> map = CacheBuilder.newBuilder().build(
      new CacheLoader<TransitionActions, ServiceTransitionCallback>( ) {
        @Override
        public ServiceTransitionCallback load( TransitionActions input ) {
          return valueOf( input.name( ) );
        }
      });
    
    public static ServiceTransitionCallback map( TransitionActions transition ) {
      return map.getUnchecked( transition );
    }
    
  }
  
  enum ServiceLocalTransitionCallbacks implements ServiceTransitionCallback {
    LOAD {
      @Override
      public void fire( final ServiceConfiguration parent ) throws Exception {
        try {
          parent.lookupBootstrapper( ).load( );
          ServiceBuilders.lookup( parent.getComponentId( ) ).fireLoad( parent );
        } catch ( Exception e ) {
          final ServiceOrderlyException sce = Exceptions.findCause( e, ServiceOrderlyException.class );
          final OrderlyTransitionException ote = Exceptions.findCause( e, OrderlyTransitionException.class );
          if ( ote==null && sce != null  ) {
            throw new OrderlyTransitionException( "Load failed with error: '"
                + sce.getMessage( )
                + "', terminating bootstrap for component: "
                + parent.getComponentId( ).getName( ), e );
          }
          throw e;
        }
      }
    },
    DESTROY {
      
      @Override
      public void fire( final ServiceConfiguration parent ) throws Exception {
        parent.lookupBootstrapper( ).destroy( );
        if ( Bootstrap.isFinished( ) ) {
          Components.lookup( parent.getComponentId( ) ).destroy( parent );
        }
      }
    },
    CHECK {
      
      @Override
      public void fire( final ServiceConfiguration parent ) throws Exception {
      if ( parent.isVmLocal( ) && Faults.isFailstop( ) ) {
        if ( Component.State.ENABLED.apply( parent ) ) {
          try {
            DISABLE.fire( parent );
          } catch ( Exception ex1 ) {
          }          
        }
        throw new IllegalStateException( "Failed to CHECK service " + parent.getFullName( ) + " because the host is currently fail-stopped." );
      } else if ( Component.State.ENABLED.apply( parent ) ) {
          try {
            parent.lookupBootstrapper( ).check( );
            ServiceBuilders.lookup( parent.getComponentId( ) ).fireCheck( parent );
          } catch ( Exception ex ) {
            if ( Exceptions.isCausedBy( ex, CheckException.class ) ) {
              CheckException checkEx = Exceptions.findCause( ex, CheckException.class );
              Faults.failstop( parent, checkEx );
            }
            if ( Faults.filter( parent, ex ) ) {
              try {
                DISABLE.fire( parent );
              } catch ( Exception ex1 ) {
                LOG.error( "Failed to call DISABLE on an ENABLED service after CHECK failure: " + parent.getFullName( )
                           + " due to: "
                           + ex.getMessage( )
                           + ". With current service info: "
                           + parent );
                Logs.extreme( ).error( ex1, ex1 );
              }
            }
            throw ex;
          }
        } else {
          try {
            parent.lookupBootstrapper( ).check( );
            ServiceBuilders.lookup( parent.getComponentId( ) ).fireCheck( parent );
          } catch ( Exception ex ) {
            if ( Exceptions.isCausedBy( ex, CheckException.class ) ) {
              CheckException checkEx = Exceptions.findCause( ex, CheckException.class );
              Faults.failstop( parent, checkEx );
            }
            throw ex;
          }
        }
      }
    },
    START {
      
      @Override
      public void fire( final ServiceConfiguration parent ) throws Exception {
        if ( !Bootstrap.isShuttingDown( ) ) {
          parent.lookupBootstrapper( ).start( );
          ServiceBuilders.lookup( parent.getComponentId( ) ).fireStart( parent );
        }
      }
    },
    ENABLE {
      
      @Override
      public void fire( final ServiceConfiguration parent ) throws Exception {
        if ( parent.isVmLocal( ) && Faults.isFailstop( ) ) {
          throw new IllegalStateException( "Failed to ENABLE service " + parent.getFullName( ) + " because the host is currently fail-stopped." );
        } else {
          parent.lookupBootstrapper( ).enable( );
          try {
            ServiceBuilders.lookup( parent.getComponentId( ) ).fireEnable( parent );
          } catch ( Exception ex ) {
            try {
              parent.lookupBootstrapper( ).disable( );
            } catch ( Exception ex1 ) {
            }
            throw ex;
          }
        }
      }
    },
    DISABLE {
      
      @Override
      public void fire( final ServiceConfiguration parent ) throws Exception {
        try {//GRZE: disable transition must always succeed to avoid ambigious/duplicate invocation from in( State.NOTREADY )
          parent.lookupBootstrapper( ).disable( );
          ServiceBuilders.lookup( parent.getComponentId( ) ).fireDisable( parent );
        } catch ( Exception ex ) {
          LOG.error( ex, ex );
        }
      }
    },
    STOP {
      
      @Override
      public void fire( final ServiceConfiguration parent ) throws Exception {
        parent.lookupBootstrapper( ).stop( );
        ServiceBuilders.lookup( parent.getComponentId( ) ).fireStop( parent );
      }
    };

    private static LoadingCache<TransitionActions, ServiceTransitionCallback> map = CacheBuilder.newBuilder().build(
      new CacheLoader<TransitionActions, ServiceTransitionCallback>( ) {
        @Override
        public ServiceTransitionCallback load( TransitionActions input ) {
          return ServiceLocalTransitionCallbacks.valueOf( input.name( ) );
        }
      });
    
    public static ServiceTransitionCallback map( TransitionActions transition ) {
      return map.getUnchecked( transition );
    }
  }
  
  enum ServiceRemoteTransitionNotification implements ServiceTransitionCallback {
    LOAD {
      
      @Override
      public void fire( final ServiceConfiguration parent ) throws Exception {}
    },
    DESTROY {
      
      @Override
      public void fire( final ServiceConfiguration parent ) throws Exception {}
    },
    CHECK {
      
      @Override
      public void fire( final ServiceConfiguration parent ) throws Exception {
        try {
          ServiceBuilders.lookup( parent.getComponentId( ) ).fireCheck( parent );
        } catch ( Exception ex ) {
          Logs.extreme( ).error( ex, ex );
        }
      }
      
    },
    START {
      
      @Override
      public void fire( final ServiceConfiguration parent ) throws Exception {
        try {
          ServiceBuilders.lookup( parent.getComponentId( ) ).fireStart( parent );
        } catch ( Exception ex ) {
          Logs.extreme( ).error( ex, ex );
        }
      }
    },
    ENABLE {
      
      @Override
      public void fire( final ServiceConfiguration parent ) throws Exception {
        try {
          ServiceBuilders.lookup( parent.getComponentId( ) ).fireEnable( parent );
        } catch ( Exception ex ) {
          LOG.error( ex, ex );
        }
        
      }
    },
    DISABLE {
      
      @Override
      public void fire( final ServiceConfiguration parent ) throws Exception {
        try {
          ServiceBuilders.lookup( parent.getComponentId( ) ).fireDisable( parent );
        } catch ( Exception ex ) {
          Logs.extreme( ).error( ex, ex );
        }
      }
    },
    STOP {
      
      @Override
      public void fire( final ServiceConfiguration parent ) throws Exception {
        try {
          ServiceBuilders.lookup( parent.getComponentId( ) ).fireStop( parent );
        } catch ( Exception ex ) {
          Logs.extreme( ).error( ex, ex );
        }
      }
    };
    
    private static LoadingCache<TransitionActions, ServiceTransitionCallback> map = CacheBuilder.newBuilder().build(
      new CacheLoader<TransitionActions, ServiceTransitionCallback>( ) {
        @Override
        public ServiceTransitionCallback load( TransitionActions input ) {
          return valueOf( input.name( ) );
        }
      });
    
    public static ServiceTransitionCallback map( TransitionActions transition ) {
      return map.getUnchecked( transition );
    }
  }
  
  public enum StateCallbacks implements Callback<ServiceConfiguration> {//TODO:GRZE: make these discoverable
    FIRE_STATE_EVENT {
      
      @Override
      public void fire( final ServiceConfiguration config ) {
        if ( Hosts.isCoordinator( ) && !config.isVmLocal( )
             && config.getComponentId( ).isRegisterable( )
             && !( config.getComponentId( ).isAlwaysLocal( ) || config.getComponentId( ).isCloudLocal( ) ) ) {
          ServiceEvents.fire( config, config.getStateMachine().getState() );
        }
      }
    },
    PROPERTIES_ADD {
      @Override
      public void fire( final ServiceConfiguration config ) {
        if ( Bootstrap.isFinished( ) ) {
          try {
            List<ConfigurableProperty> props = PropertyDirectory.getPendingPropertyEntrySet( config.getComponentId( ).name( ) );
            for ( ConfigurableProperty prop : props ) {
              if ( prop instanceof SingletonDatabasePropertyEntry ) {
                PropertyDirectory.addProperty( prop );
              } else if ( prop instanceof MultiDatabasePropertyEntry ) {
                MultiDatabasePropertyEntry addProp = ( ( MultiDatabasePropertyEntry ) prop ).getClone( config.getPartition( ) );
                PropertyDirectory.addProperty( addProp );
              }
            }
          } catch ( Exception ex ) {
            LOG.error( ex, ex );
          }
        }
      }
    },
    STATIC_PROPERTIES_ADD {
      @Override
      public void fire( final ServiceConfiguration config ) {
        if ( Bootstrap.isOperational( ) && !Databases.isVolatile( ) ) {
          STATIC_PROPERTIES_SUPPLIER.get( );
        }
      }
    },
    PROPERTIES_REMOVE {
      @Override
      public void fire( final ServiceConfiguration config ) {
        try {
          String prefix = config.getPartition( ) + "." + config.getComponentId( ).name( );
          List<ConfigurableProperty> props = PropertyDirectory.getPropertyEntrySet( prefix );
          for ( ConfigurableProperty prop : props ) {
            if ( prop instanceof SingletonDatabasePropertyEntry ) {
            //GRZE:REVIEW do nothing?
          } else if ( prop instanceof MultiDatabasePropertyEntry ) {
            ( ( MultiDatabasePropertyEntry ) prop ).setIdentifierValue( config.getPartition( ) );
            PropertyDirectory.removeProperty( prop );
          }
        }
      } catch ( Exception ex ) {
        LOG.error( ex, ex );
      }
    }
      
    },
    ENSURE_DISABLED {
      
      @Override
      public void fire( ServiceConfiguration input ) {
        if ( State.ENABLED.apply( input ) && Hosts.isServiceLocal( input ) ) {
          try {
            LOG.debug( "Ensuring .disable()/.fireDisable() have been called for service entering NOTREADY: " + input.getFullName( ) );
            ServiceLocalTransitionCallbacks.DISABLE.fire( input );
          } catch ( Exception ex ) {
            LOG.error( ex, ex );
          }
        }
      }
      
    };
    
  }
  
  private enum StaticPropertiesAdd implements Supplier<Void> {
    INSTANCE;

    private final EntityCache<StaticDatabasePropertyEntry,Tuple2<String,Integer>> propertyCache =
        new EntityCache<>(
            StaticDatabasePropertyEntry.example( ),
            property -> Tuple.of( property.getPropName( ), property.getVersion( ) )
        );

    private AtomicReference<Map<String,Integer>> versionInfoRef = new AtomicReference<>( ImmutableMap.of( ) );

    @Override
    public Void get() {
      final Map<String,Integer> previousVersionInfo = versionInfoRef.get( );
      final Map<String,Integer> versionInfo = Databases.isVolatile( ) ?
          previousVersionInfo :
          Stream.ofAll( propertyCache.get( ) ).toJavaMap( Function.identity( ) );
      boolean forceGets = !previousVersionInfo.equals( versionInfo );
      if ( forceGets ) {
        versionInfoRef.compareAndSet( previousVersionInfo, ImmutableMap.copyOf( versionInfo ) );
      }
      for ( ConfigurableProperty prop : Iterables.filter( PropertyDirectory.getPendingPropertyValues( ),
          Predicates.instanceOf( StaticPropertyEntry.class ) ) ) {
        try {
          if ( ( PropertyDirectory.addProperty( prop ) || forceGets ) && !Databases.isVolatile( ) ) {
            try {
              prop.getValue();
            } catch (Exception ex) {
              Logs.extreme().error(ex);
            }
          }
        } catch ( Exception ex ) {
          Logs.extreme( ).error( ex, ex );
        }
      }
      return null;
    }
  }
}
