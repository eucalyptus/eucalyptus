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

import java.lang.reflect.UndeclaredThrowableException;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import org.apache.log4j.Logger;
import com.eucalyptus.bootstrap.BootstrapArgs;
import com.eucalyptus.bootstrap.Hosts;
import com.eucalyptus.component.Component.State;
import com.eucalyptus.component.ServiceChecks.CheckException;
import com.eucalyptus.configurable.ConfigurableProperty;
import com.eucalyptus.configurable.MultiDatabasePropertyEntry;
import com.eucalyptus.configurable.PropertyDirectory;
import com.eucalyptus.configurable.SingletonDatabasePropertyEntry;
import com.eucalyptus.configurable.StaticPropertyEntry;
import com.eucalyptus.context.ServiceContextManager;
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
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.ObjectArrays;
import com.google.common.util.concurrent.Callables;

public class ServiceTransitions {
  static Logger                          LOG   = Logger.getLogger( ServiceTransitions.class );
  private static final Component.State[] EMPTY = {};
  
  private static Component.State[] sequence( Component.State... states ) {
    return states;
  }
  
  interface ServiceTransitionCallback {
    public void fire( ServiceConfiguration parent ) throws Exception;
  }
  
  /**
   * GRZE:FIXME: this is a shoddy static method for definining the prefered path from n_0 to n_1 for
   * n_0,n_1 \in G the state machine.
   **/
  @SuppressWarnings( "unchecked" )
  public static CheckedListenableFuture<ServiceConfiguration> pathTo( final ServiceConfiguration configuration, final Component.State goalState ) {
    Callable<CheckedListenableFuture<ServiceConfiguration>> transition;
    try {
      switch ( goalState ) {
        case LOADED:
          return executeTransition( configuration, Automata.sequenceTransitions( configuration, pathToLoaded( configuration.lookupState( ) ) ) );
        case DISABLED:
          return executeTransition( configuration, Automata.sequenceTransitions( configuration, pathToDisabled( configuration.lookupState( ) ) ) );
        case ENABLED:
          return executeTransition( configuration, Automata.sequenceTransitions( configuration, pathToEnabled( configuration.lookupState( ) ) ) );
        case STOPPED:
          return executeTransition( configuration, Automata.sequenceTransitions( configuration, pathToStopped( configuration.lookupState( ) ) ) );
        case NOTREADY:
          return executeTransition( configuration, Automata.sequenceTransitions( configuration, pathToStarted( configuration.lookupState( ) ) ) );
        case NONE:
          return executeTransition( configuration, Automata.sequenceTransitions( configuration, pathToNone( configuration.lookupState( ) ) ) );
        default:
          return Futures.predestinedFuture( configuration );
      }
    } catch ( RuntimeException ex ) {
      Logs.extreme( ).error( ex, ex );
      LOG.error( configuration.getFullName( ) + " failed to transition to " + goalState + " because of: " + Exceptions.causeString( ex ) );
      throw ex;
    }
  }
  
  /**
   * @param lookupState
   * @return
   */
  private static final State[] pathToLoaded( final Component.State fromState ) {
    switch ( fromState ) {
      case DISABLED:
      case ENABLED:
        return ServiceTransitions.sequence( Component.State.ENABLED,
                                            Component.State.DISABLED,
                                            Component.State.STOPPED,
                                            Component.State.INITIALIZED,
                                            Component.State.LOADED );
      case NOTREADY:
        return ServiceTransitions.sequence( Component.State.NOTREADY,
                                            Component.State.DISABLED,
                                            Component.State.STOPPED,
                                            Component.State.INITIALIZED,
                                            Component.State.LOADED );
      default:
        return ServiceTransitions.sequence( Component.State.PRIMORDIAL,
                                            Component.State.INITIALIZED,
                                            Component.State.LOADED );
    }
  }
  
  private static State[] pathToNone( Component.State fromState ) {
    return ObjectArrays.concat( ServiceTransitions.pathToStopped( fromState ), Component.State.NONE );
  }
  
  private static final State[] pathToStarted( final Component.State fromState ) {
    switch ( fromState ) {
      case ENABLED:
        return ServiceTransitions.sequence( Component.State.ENABLED, Component.State.DISABLED );
      case DISABLED:
      case NOTREADY:
        return ServiceTransitions.sequence( Component.State.NOTREADY, Component.State.DISABLED, Component.State.DISABLED );
      default:
        return ServiceTransitions.sequence( Component.State.PRIMORDIAL,
                                            Component.State.BROKEN,
                                            Component.State.STOPPED,
                                            Component.State.INITIALIZED,
                                            Component.State.LOADED,
                                            Component.State.NOTREADY,
                                            Component.State.DISABLED );
    }
  }
  
  private static final State[] pathToEnabled( final Component.State fromState ) {
    switch ( fromState ) {
      case ENABLED:
        return ServiceTransitions.sequence( Component.State.ENABLED,
                                            Component.State.ENABLED );
      default:
        return ObjectArrays.concat( ServiceTransitions.pathToDisabled( fromState ), Component.State.ENABLED );
    }
  }
  
  private static final State[] pathToDisabled( final Component.State fromState ) {
    switch ( fromState ) {
      case NOTREADY:
        return ServiceTransitions.sequence( Component.State.NOTREADY,
                                            Component.State.DISABLED );
      case DISABLED:
        return ServiceTransitions.sequence( Component.State.DISABLED,
                                            Component.State.DISABLED );
      case ENABLED:
        return ServiceTransitions.sequence( Component.State.ENABLED,
                                            Component.State.DISABLED );
      default:
        return ObjectArrays.concat( ServiceTransitions.pathToStarted( fromState ), Component.State.DISABLED );
    }
  }
  
  private static final State[] pathToStopped( final Component.State fromState ) {
    switch ( fromState ) {
      case ENABLED:
      case DISABLED:
        return ServiceTransitions.sequence( Component.State.ENABLED,
                                            Component.State.DISABLED,
                                            Component.State.STOPPED );
      default:
        return ServiceTransitions.sequence( Component.State.PRIMORDIAL,
                                            Component.State.INITIALIZED,
                                            Component.State.LOADED,
                                            Component.State.NOTREADY,
                                            Component.State.STOPPED );
    }
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
    LOG.debug( "Sending request " + msg.getClass( ).getSimpleName( ) + " to " + parent.getFullName( ) );
    try {
      T reply = ( T ) AsyncRequests.sendSync( config, msg );
      return reply;
    } catch ( Exception ex ) {
      LOG.error( ex, ex );
      throw ex;
    }
  }
  
  private static void processTransition( final ServiceConfiguration parent, final Completion transitionCallback, final TransitionActions transitionAction ) {
    ServiceTransitionCallback trans = null;
    try {
      if ( parent.isVmLocal( ) || ( parent.isHostLocal( ) && Hosts.isCoordinator( ) ) ) {
        try {
          trans = ServiceLocalTransitionCallbacks.valueOf( transitionAction.name( ) );
        } catch ( Exception ex ) {
          LOG.error( ex, ex );
          throw ex;
        }
      } else if ( !Hosts.isCoordinator( ) ) {
        try {
          trans = ServiceRemoteTransitionNotification.valueOf( transitionAction.name( ) );
        } catch ( Exception ex ) {
          LOG.error( ex, ex );
          throw ex;
        }
      } else if ( Hosts.isCoordinator( ) ) {
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
        Logs.exhaust( ).debug( "Executing transition: " + trans.getClass( ) + "." + transitionAction.name( ) + " for " + parent );
        trans.fire( parent );
      }
      transitionCallback.fire( );
    } catch ( Exception ex ) {
      if ( Faults.filter( parent, ex ) ) {
        transitionCallback.fireException( ex );
        throw new UndeclaredThrowableException( ex );
      } else {
        transitionCallback.fire( );
      }
    }
  }
  
  public enum TransitionActions implements TransitionAction<ServiceConfiguration> {
    ENABLE, CHECK, DISABLE, START, LOAD, STOP, DESTROY;
    
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
          throw ServiceChecks.chainCheckExceptions( errors );
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
        try {
          parent.lookupBootstrapper( ).check( );
          ServiceBuilders.lookup( parent.getComponentId( ) ).fireCheck( parent );
        } catch ( Exception ex ) {
          LOG.error( ex, ex );
          throw ex;
        }
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
    
  }
  
  public enum StateCallbacks implements Callback<ServiceConfiguration> {
    FIRE_STATE_EVENT {
      
      @Override
      public void fire( final ServiceConfiguration config ) {
        ServiceEvents.fire( config, config.getStateMachine( ).getState( ) );
      }
    },
    SERVICE_CONTEXT_RESTART {
      @Override
      public void fire( final ServiceConfiguration parent ) {
        ServiceContextManager.restartSync( parent );
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
        for ( Entry<String, ConfigurableProperty> entry : PropertyDirectory.getPendingPropertyEntries( ) ) {
          try {
            ConfigurableProperty prop = entry.getValue( );
            if ( prop instanceof StaticPropertyEntry ) {
              PropertyDirectory.addProperty( prop );
              try {
                prop.getValue( );
              } catch ( Exception ex ) {
                Logs.exhaust( ).error( ex );
              }
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
