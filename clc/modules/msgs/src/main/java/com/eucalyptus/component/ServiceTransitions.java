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

package com.eucalyptus.component;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import org.apache.log4j.Logger;
import com.eucalyptus.bootstrap.Hosts;
import com.eucalyptus.component.Component.State;
import com.eucalyptus.component.ServiceChecks.CheckException;
import com.eucalyptus.configurable.ConfigurableProperty;
import com.eucalyptus.configurable.MultiDatabasePropertyEntry;
import com.eucalyptus.configurable.PropertyDirectory;
import com.eucalyptus.configurable.SingletonDatabasePropertyEntry;
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
import com.eucalyptus.util.fsm.TransitionAction;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import com.google.common.collect.MapMaker;
import com.google.common.collect.ObjectArrays;

public class ServiceTransitions {
  static Logger                                     LOG   = Logger.getLogger( ServiceTransitions.class );
  private static final Component.State[]            EMPTY = {};
  Map<TransitionActions, ServiceTransitionCallback> hi    = new MapMaker( ).makeComputingMap( new Function<TransitionActions, ServiceTransitionCallback>( ) {
                                                            
                                                            @Override
                                                            public ServiceTransitionCallback apply( TransitionActions input ) {
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
    if ( goalState.equals( configuration.lookupState( ) ) ) {
      return Futures.predestinedFuture( configuration );
    } else {
      try {
        CheckedListenableFuture<ServiceConfiguration> result = null;
        switch ( goalState ) {
          case LOADED:
            result = executeTransition( configuration, Automata.sequenceTransitions( configuration, pathToLoaded( configuration.lookupState( ) ) ) );
            break;
          case DISABLED:
            result = executeTransition( configuration, Automata.sequenceTransitions( configuration, pathToDisabled( configuration.lookupState( ) ) ) );
            break;
          case ENABLED:
            result = executeTransition( configuration, Automata.sequenceTransitions( configuration, pathToEnabled( configuration.lookupState( ) ) ) );
            break;
          case STOPPED:
            result = executeTransition( configuration, Automata.sequenceTransitions( configuration, pathToStopped( configuration.lookupState( ) ) ) );
            break;
          case NOTREADY:
            result = executeTransition( configuration, Automata.sequenceTransitions( configuration, pathToStarted( configuration.lookupState( ) ) ) );
            break;
          case PRIMORDIAL:
            result = executeTransition( configuration, Automata.sequenceTransitions( configuration, pathToPrimordial( configuration.lookupState( ) ) ) );
            break;
          case BROKEN:
            result = executeTransition( configuration, Automata.sequenceTransitions( configuration, pathToBroken( configuration.lookupState( ) ) ) );
            break;
          case INITIALIZED:
            result = executeTransition( configuration, Automata.sequenceTransitions( configuration, pathToInitialized( configuration.lookupState( ) ) ) );
            break;
        }
        return result;
      } catch ( RuntimeException ex ) {
        Logs.extreme( ).error( ex, ex );
        LOG.error( configuration.getFullName( ) + " failed to transition to "
                   + goalState
                   + " because of: "
                   + Exceptions.causeString( ex ) );
        throw ex;
      }
    }
  }
  
  private static State[] pathToBroken( Component.State fromState ) {
    if ( Component.State.BROKEN.equals( fromState ) ) {
      return new State[] {};
    } else {
      return ObjectArrays.concat( ServiceTransitions.pathToPrimordial( fromState ), Component.State.BROKEN );
    }
  }
  
  private static State[] pathToPrimordial( Component.State fromState ) {
    if ( Component.State.PRIMORDIAL.equals( fromState ) ) {
      return new State[] {};
    } else {
      return ObjectArrays.concat( ServiceTransitions.pathToStopped( fromState ), Component.State.PRIMORDIAL );
    }
  }
  
  private static State[] pathToLoaded( Component.State fromState ) {
    if ( Component.State.LOADED.equals( fromState ) ) {
      return new State[] {};
    } else {
      return ObjectArrays.concat( ServiceTransitions.pathToInitialized( fromState ), Component.State.LOADED );
    }
  }
  
  private static final State[] pathToInitialized( final Component.State fromState ) {
    State[] transition = new State[] { fromState };
    switch ( fromState ) {
      case LOADED:
        transition = ObjectArrays.concat( fromState, pathToInitialized( Component.State.NOTREADY ) );
        //$FALL-THROUGH$
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
        //$FALL-THROUGH$
      case INITIALIZED:
        break;
    }
    return transition;
  }
  
  private static State[] pathToStarted( Component.State fromState ) {
    if ( Component.State.NOTREADY.equals( fromState ) ) {
      return new State[] {};
    } else {
      return ObjectArrays.concat( ServiceTransitions.pathToLoaded( fromState ), Component.State.NOTREADY );
    }
  }
  
  private static final State[] pathToDisabled( final Component.State fromState ) {
    State[] transition = new State[] { fromState };
    switch ( fromState ) {
      case DISABLED:
      case NOTREADY:
        transition = ObjectArrays.concat( transition, Component.State.DISABLED );
        break;
      default:
        transition = ObjectArrays.concat( pathToStarted( fromState ), Component.State.DISABLED );
    }
    return ObjectArrays.concat( transition, Component.State.DISABLED );
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
    return ObjectArrays.concat( transition, Component.State.ENABLED );
  }
  
  private static final State[] pathToStopped( final Component.State fromState ) {
    State[] transition = new State[] { fromState };
    switch ( fromState ) {
      case ENABLED:
        transition = ObjectArrays.concat( transition, Component.State.DISABLED );
        //$FALL-THROUGH$
      case DISABLED:
      case NOTREADY:
        transition = ObjectArrays.concat( transition, Component.State.STOPPED );
        break;
      case STOPPED:
        transition = new State[] {};
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
    LOG.debug( "Sending request " + msg.getClass( ).getSimpleName( )
               + " to "
               + parent.getFullName( ) );
    try {
      if ( System.getProperty( "euca.noha.cloud" ) == null ) {
        T reply = ( T ) AsyncRequests.sendSync( config, msg );
        return reply;
      } else {
        return msg.getReply( );
      }
      
    } catch ( Exception ex ) {
      LOG.error( parent + " failed request because of: "
                 + ex.getMessage( ) );
      Logs.extreme( ).error( ex, ex );
      throw ex;
    }
  }
  
  private static void processTransition( final ServiceConfiguration parent, final Completion transitionCallback, final TransitionActions transitionAction ) {
    ServiceTransitionCallback trans = null;
    try {
      if ( parent.isVmLocal( ) || ( parent.isHostLocal( ) && Hosts.Coordinator.INSTANCE.isLocalhost( ) ) ) {
        try {
          trans = ServiceLocalTransitionCallbacks.valueOf( transitionAction.name( ) );
        } catch ( Exception ex ) {
          LOG.error( ex, ex );
          throw ex;
        }
      } else if ( !Hosts.Coordinator.INSTANCE.isLocalhost( ) ) {
        try {
          trans = ServiceRemoteTransitionNotification.valueOf( transitionAction.name( ) );
        } catch ( Exception ex ) {
          LOG.error( ex, ex );
          throw ex;
        }
      } else if ( Hosts.Coordinator.INSTANCE.isLocalhost( ) ) {
        try {
          trans = CloudRemoteTransitionCallbacks.valueOf( transitionAction.name( ) );
        } catch ( Exception ex ) {
          LOG.error( ex, ex );
          throw ex;
        }
      } else {
        LOG.debug( "Silentlty accepting remotely inferred state transition for " + parent );
      }
      if ( trans != null ) {
        Logs.exhaust( ).debug( "Executing transition: " + trans.getClass( )
                               + "."
                               + transitionAction.name( )
                               + " for "
                               + parent );
        trans.fire( parent );
      }
      transitionCallback.fire( );
    } catch ( Exception ex ) {
      if ( Faults.filter( parent, ex ) ) {
        transitionCallback.fireException( ex );
        throw Exceptions.toUndeclared( ex );
      } else {
        transitionCallback.fire( );
      }
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
      EventRecord.here( ServiceBuilder.class,
                        EventType.SERVICE_TRANSITION,
                        this.name( ),
                        parent.lookupState( ).toString( ),
                        parent.getFullName( ).toString( ),
                        parent.toString( ) ).exhaust( );
      ServiceTransitions.processTransition( parent, transitionCallback, this );
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
        DescribeServicesResponseType response = ServiceTransitions.sendEmpyreanRequest( parent, new DescribeServicesType( ) );
        ServiceStatusType status = Iterables.find( response.getServiceStatuses( ), new Predicate<ServiceStatusType>( ) {
          
          @Override
          public boolean apply( final ServiceStatusType arg0 ) {
            return parent.getName( ).equals( arg0.getServiceId( ).getName( ) );
          }
        } );
        String corrId = response.getCorrelationId( );
        List<CheckException> errors = ServiceChecks.Functions.statusToCheckExceptions( corrId ).apply( status );
        if ( !errors.isEmpty( ) ) {
          if ( Component.State.ENABLED.equals( parent.lookupState( ) ) ) {
            try {
              DISABLE.fire( parent );
            } catch ( Exception ex ) {
              LOG.error( ex, ex );
            }
          }
          throw Faults.failure( parent, errors );
        }
      }
      
    },
    START {
      
      @Override
      public void fire( final ServiceConfiguration parent ) throws Exception {
        StartServiceResponseType msg = ServiceTransitions.sendEmpyreanRequest( parent, new StartServiceType( ) {
          {
            this.getServices( ).add( TypeMappers.transform( parent, ServiceId.class ) );
          }
        } );
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
        EnableServiceResponseType msg = ServiceTransitions.sendEmpyreanRequest( parent, new EnableServiceType( ) {
          {
            this.getServices( ).add( TypeMappers.transform( parent, ServiceId.class ) );
          }
        } );
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
        DisableServiceResponseType msg = ServiceTransitions.sendEmpyreanRequest( parent, new DisableServiceType( ) {
          {
            this.getServices( ).add( TypeMappers.transform( parent, ServiceId.class ) );
          }
        } );
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
        StopServiceResponseType msg = ServiceTransitions.sendEmpyreanRequest( parent, new StopServiceType( ) {
          {
            this.getServices( ).add( TypeMappers.transform( parent, ServiceId.class ) );
          }
        } );
        try {
          ServiceBuilders.lookup( parent.getComponentId( ) ).fireStop( parent );
        } catch ( Exception ex ) {
          LOG.error( ex, ex );
        }
      }
    };
    private static Function<TransitionActions, ServiceTransitionCallback> mapper = new Function<TransitionActions, ServiceTransitionCallback>( ) {
                                                                                   
                                                                                   @Override
                                                                                   public ServiceTransitionCallback apply( TransitionActions input ) {
                                                                                     return valueOf( input.name( ) );
                                                                                   }
                                                                                 };
    private static Map<TransitionActions, ServiceTransitionCallback>      map    = new MapMaker( ).makeComputingMap( mapper );
    
    public static ServiceTransitionCallback map( TransitionActions transition ) {
      return map.get( transition );
    }
    
  }
  
  enum ServiceLocalTransitionCallbacks implements ServiceTransitionCallback {
    LOAD {
      
      @Override
      public void fire( final ServiceConfiguration parent ) throws Exception {
        parent.lookupBootstrapper( ).load( );
      }
      
    },
    DESTROY {
      
      @Override
      public void fire( final ServiceConfiguration parent ) throws Exception {
        parent.lookupBootstrapper( ).destroy( );
      }
    },
    CHECK {
      
      @Override
      public void fire( final ServiceConfiguration parent ) throws Exception {
        parent.lookupBootstrapper( ).check( );
        ServiceBuilders.lookup( parent.getComponentId( ) ).fireCheck( parent );
      }
    },
    START {
      
      @Override
      public void fire( final ServiceConfiguration parent ) throws Exception {
        parent.lookupBootstrapper( ).start( );
        ServiceBuilders.lookup( parent.getComponentId( ) ).fireStart( parent );
      }
    },
    ENABLE {
      
      @Override
      public void fire( final ServiceConfiguration parent ) throws Exception {
        CHECK.fire( parent );
        parent.lookupBootstrapper( ).enable( );
        ServiceBuilders.lookup( parent.getComponentId( ) ).fireEnable( parent );
      }
    },
    DISABLE {
      
      @Override
      public void fire( final ServiceConfiguration parent ) throws Exception {
//        if ( State.NOTREADY.equals( parent.lookupComponent( ).getState( ) ) ) {
//          parent.lookupComponent( ).check( );
//          ServiceBuilders.lookup( parent.getComponentId( ) ).fireCheck( parent );
//        }
        parent.lookupBootstrapper( ).disable( );
        ServiceBuilders.lookup( parent.getComponentId( ) ).fireDisable( parent );
      }
    },
    STOP {
      
      @Override
      public void fire( final ServiceConfiguration parent ) throws Exception {
        parent.lookupBootstrapper( ).stop( );
        ServiceBuilders.lookup( parent.getComponentId( ) ).fireStop( parent );
      }
    };
    private static Function<TransitionActions, ServiceTransitionCallback> mapper = new Function<TransitionActions, ServiceTransitionCallback>( ) {
                                                                                   
                                                                                   @Override
                                                                                   public ServiceTransitionCallback apply( TransitionActions input ) {
                                                                                     return ServiceLocalTransitionCallbacks.valueOf( input.name( ) );
                                                                                   }
                                                                                 };
    private static Map<TransitionActions, ServiceTransitionCallback>      map    = new MapMaker( ).makeComputingMap( mapper );
    
    public static ServiceTransitionCallback map( TransitionActions transition ) {
      return map.get( transition );
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
          LOG.error( ex, ex );
        }
      }
      
    },
    START {
      
      @Override
      public void fire( final ServiceConfiguration parent ) throws Exception {
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
          LOG.error( ex, ex );
        }
      }
    },
    STOP {
      
      @Override
      public void fire( final ServiceConfiguration parent ) throws Exception {
        try {
          ServiceBuilders.lookup( parent.getComponentId( ) ).fireStop( parent );
        } catch ( Exception ex ) {
          LOG.error( ex, ex );
        }
      }
    };
    
    private static Function<TransitionActions, ServiceTransitionCallback> mapper = new Function<TransitionActions, ServiceTransitionCallback>( ) {
                                                                                   
                                                                                   @Override
                                                                                   public ServiceTransitionCallback apply( TransitionActions input ) {
                                                                                     return valueOf( input.name( ) );
                                                                                   }
                                                                                 };
    private static Map<TransitionActions, ServiceTransitionCallback>      map    = new MapMaker( ).makeComputingMap( mapper );
    
    public static ServiceTransitionCallback map( TransitionActions transition ) {
      return map.get( transition );
    }
  }
  
  public enum StateCallbacks implements Callback<ServiceConfiguration> {//TODO:GRZE: make these discoverable
    FIRE_STATE_EVENT {
      
      @Override
      public void fire( final ServiceConfiguration config ) {
        ServiceEvents.fire( config, config.getStateMachine( ).getState( ) );
      }
    },
    PROPERTIES_ADD {
      @Override
      public void fire( final ServiceConfiguration config ) {
        try {
          List<ConfigurableProperty> props = PropertyDirectory.getPendingPropertyEntrySet( config.getComponentId( ).name( ) );
          for ( ConfigurableProperty prop : props ) {
            ConfigurableProperty addProp = null;
            if ( prop instanceof SingletonDatabasePropertyEntry ) {
              addProp = prop;
            } else if ( prop instanceof MultiDatabasePropertyEntry ) {
              addProp = ( ( MultiDatabasePropertyEntry ) prop ).getClone( config.getPartition( ) );
            }
            PropertyDirectory.addProperty( addProp );
          }
        } catch ( Exception ex ) {
          LOG.error( ex, ex );
        }
      }
    },
    STATIC_PROPERTIES_ADD {
      @Override
      public void fire( final ServiceConfiguration config ) {
        for ( Entry<String, ConfigurableProperty> entry : Iterables.filter( PropertyDirectory.getPendingPropertyEntries( ), Predicates.instanceOf( StaticPropertyEntry.class ) ) ) {
          try {
            ConfigurableProperty prop = entry.getValue( );
            PropertyDirectory.addProperty( prop );
            try {
              prop.getValue( );
            } catch ( Exception ex ) {
              Logs.extreme( ).error( ex );
            }
          } catch ( Exception ex ) {
            Logs.extreme( ).error( ex, ex );
          }
        }
      }
    },
    PROPERTIES_REMOVE {
      @Override
      public void fire( final ServiceConfiguration config ) {
        try {
          List<ConfigurableProperty> props = PropertyDirectory.getPropertyEntrySet( config.getComponentId( ).name( ) );
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
      
    };
    
  }
  
}
