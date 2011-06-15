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
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.apache.log4j.Logger;
import com.eucalyptus.bootstrap.Bootstrap;
import com.eucalyptus.component.Component.State;
import com.eucalyptus.component.ServiceChecks.CheckException;
import com.eucalyptus.configurable.ConfigurableProperty;
import com.eucalyptus.configurable.MultiDatabasePropertyEntry;
import com.eucalyptus.configurable.PropertyDirectory;
import com.eucalyptus.configurable.SingletonDatabasePropertyEntry;
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
import com.eucalyptus.empyrean.ServiceStatusDetail;
import com.eucalyptus.empyrean.ServiceStatusType;
import com.eucalyptus.empyrean.StartServiceResponseType;
import com.eucalyptus.empyrean.StartServiceType;
import com.eucalyptus.empyrean.StopServiceResponseType;
import com.eucalyptus.empyrean.StopServiceType;
import com.eucalyptus.records.EventRecord;
import com.eucalyptus.records.EventType;
import com.eucalyptus.util.TypeMappers;
import com.eucalyptus.util.async.AsyncRequests;
import com.eucalyptus.util.async.Callback;
import com.eucalyptus.util.async.Callback.Completion;
import com.eucalyptus.util.async.CheckedListenableFuture;
import com.eucalyptus.util.async.Futures;
import com.eucalyptus.util.async.RetryableConnectionException;
import com.eucalyptus.util.fsm.Automata;
import com.eucalyptus.util.fsm.TransitionAction;
import com.eucalyptus.ws.util.PipelineRegistry;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;

public class ServiceTransitions {
  private static final int BOOTSTRAP_REMOTE_RETRY_INTERVAL = 10;                                           //TODO:GRZE:@Configurable
  private static final int BOOTSTRAP_REMOTE_RETRIES        = 10;                                           //TODO:GRZE:@Configurable
  static Logger            LOG                             = Logger.getLogger( ServiceTransitions.class );
  
  interface ServiceTransitionCallback {
    public void fire( ServiceConfiguration parent ) throws Throwable;
  }
  
  public static CheckedListenableFuture<ServiceConfiguration> transitionChain( final ServiceConfiguration configuration, final State goalState ) {
    switch ( goalState ) {
      case DISABLED:
        return disableTransitionChain( configuration );
      case ENABLED:
        return enableTransitionChain( configuration );
      case STOPPED:
        return stopTransitionChain( configuration );
      case NOTREADY:
        return startTransitionChain( configuration );
      default:
        break;
    }
    return null;
  }
  
  static final CheckedListenableFuture<ServiceConfiguration> startTransitionChain( final ServiceConfiguration config ) {
    if ( !State.NOTREADY.equals( config.lookupState( ) ) && !State.DISABLED.equals( config.lookupState( ) ) && !State.ENABLED.equals( config.lookupState( ) ) ) {
      Callable<CheckedListenableFuture<ServiceConfiguration>> transition = null;
      if ( State.STOPPED.isIn( config ) ) {
        transition = Automata.sequenceTransitions( config,
                                                   Component.State.INITIALIZED,
                                                   Component.State.LOADED,
                                                   Component.State.NOTREADY,
                                                   Component.State.DISABLED );
      } else if ( State.INITIALIZED.isIn( config ) ) {
        transition = Automata.sequenceTransitions( config,
                                                   Component.State.INITIALIZED,
                                                   Component.State.LOADED,
                                                   Component.State.NOTREADY,
                                                   Component.State.DISABLED );
      } else {
        transition = Automata.sequenceTransitions( config, config.lookupState( ), Component.State.NOTREADY, Component.State.DISABLED );
      }
      try {
        return transition.call( );
      } catch ( Exception ex ) {
        LOG.error( ex, ex );
        throw new RuntimeException( ex );
      }
    } else {
      return Futures.predestinedFuture( config );
    }
  }
  
  static final CheckedListenableFuture<ServiceConfiguration> enableTransitionChain( final ServiceConfiguration config ) {
    if ( !State.ENABLED.equals( config.lookupState( ) ) ) {
      Callable<CheckedListenableFuture<ServiceConfiguration>> transition = Automata.sequenceTransitions( config, Component.State.INITIALIZED,
                                                                                                           Component.State.LOADED,
                                                                                                           Component.State.NOTREADY, Component.State.DISABLED,
                                                                                                           Component.State.DISABLED, Component.State.ENABLED );
      try {
        return transition.call( );
      } catch ( Exception ex ) {
        LOG.error( ex, ex );
        throw new RuntimeException( ex );
      }
    } else {
      return Futures.predestinedFuture( config );
    }
  }
  
  static final CheckedListenableFuture<ServiceConfiguration> disableTransitionChain( final ServiceConfiguration config ) {
    if ( State.ENABLED.isIn( config ) ) {
      Callable<CheckedListenableFuture<ServiceConfiguration>> transition = Automata.sequenceTransitions( config, Component.State.ENABLED,
                                                                                                           Component.State.DISABLED );
      try {
        return transition.call( );
      } catch ( Exception ex ) {
        LOG.error( ex, ex );
        throw new RuntimeException( ex );
      }
    } else if ( !State.DISABLED.isIn( config ) && !State.NOTREADY.isIn( config ) ) {
      Callable<CheckedListenableFuture<ServiceConfiguration>> transition = Automata.sequenceTransitions( config, Component.State.INITIALIZED,
                                                                                                           Component.State.LOADED,
                                                                                                           Component.State.NOTREADY, Component.State.DISABLED,
                                                                                                           Component.State.DISABLED );
      try {
        return transition.call( );
      } catch ( Exception ex ) {
        LOG.error( ex, ex );
        throw new RuntimeException( ex );
      }
    } else {
      return Futures.predestinedFuture( config );
    }
  }
  
  static final CheckedListenableFuture<ServiceConfiguration> stopTransitionChain( final ServiceConfiguration config ) {
    Component.State currState = config.lookupState( );
    if ( State.ENABLED.equals( currState ) ) {
      Callable<CheckedListenableFuture<ServiceConfiguration>> transition = Automata.sequenceTransitions( config, Component.State.ENABLED,
                                                                                                         Component.State.DISABLED, Component.State.STOPPED );
      try {
        return transition.call( );
      } catch ( Exception ex ) {
        LOG.error( ex, ex );
        throw new RuntimeException( ex );
      }
    } else if ( State.DISABLED.equals( currState ) || State.NOTREADY.equals( currState ) ) {
      Callable<CheckedListenableFuture<ServiceConfiguration>> transition = Automata.sequenceTransitions( config, currState, Component.State.STOPPED );
      try {
        return transition.call( );
      } catch ( Exception ex ) {
        LOG.error( ex, ex );
        throw new RuntimeException( ex );
      }
    } else {
      return Futures.predestinedFuture( config );
    }
  }
  
  static final CheckedListenableFuture<ServiceConfiguration> destroyTransitionChain( final ServiceConfiguration config ) {
    if ( !State.INITIALIZED.isIn( config ) ) {
      Callable<CheckedListenableFuture<ServiceConfiguration>> transition = Automata.sequenceTransitions( config, Component.State.ENABLED,
                                                                                                           Component.State.DISABLED, Component.State.STOPPED );
      try {
        return transition.call( );
      } catch ( Exception ex ) {
        LOG.error( ex, ex );
        throw new RuntimeException( ex );
      }
    } else {
      return Futures.predestinedFuture( config );
    }
  }
  
  private static <T extends EmpyreanMessage> T sendEmpyreanRequest( final ServiceConfiguration parent, final EmpyreanMessage msg ) throws Throwable {
    ServiceConfiguration config = ServiceConfigurations.createEphemeral( Empyrean.INSTANCE, parent.getInetAddress( ) );
    LOG.debug( "Sending request " + msg.getClass( ).getSimpleName( ) + " to " + parent.getFullName( ) );
    for ( int i = 0; i < BOOTSTRAP_REMOTE_RETRIES; i++ ) {
      try {
        T reply = ( T ) AsyncRequests.sendSync( config, msg );
        return reply;
      } catch ( RetryableConnectionException ex ) {
        try {
          TimeUnit.SECONDS.sleep( BOOTSTRAP_REMOTE_RETRY_INTERVAL );
        } catch ( InterruptedException ex1 ) {
          Thread.currentThread( ).interrupt( );
        }
        continue;
      } catch ( ExecutionException ex ) {
        LOG.error( ex, ex );
        if ( ex.getCause( ) instanceof RetryableConnectionException ) {
          try {
            TimeUnit.SECONDS.sleep( BOOTSTRAP_REMOTE_RETRY_INTERVAL );
          } catch ( InterruptedException ex1 ) {
            Thread.currentThread( ).interrupt( );
          }
          continue;
        } else {
          throw ex;
        }
      } catch ( Exception ex ) {
        LOG.error( ex, ex );
        throw ex;
      }
    }
    throw new ServiceRegistrationException( "Failed to contact host after " + BOOTSTRAP_REMOTE_RETRIES + " retries: " + config.getUri( )
                                            + " when sending message: " + msg );
  }
  
  private static void processTransition( final ServiceConfiguration parent, final Completion transitionCallback, final TransitionActions transitionAction ) {
    ServiceTransitionCallback trans = null;
    try {
      if ( parent.isVmLocal( ) || ( parent.isHostLocal( ) && Bootstrap.isCloudController( ) ) ) {
        try {
          trans = LocalTransitionCallbacks.valueOf( transitionAction.name( ) );
        } catch ( Exception ex ) {
          LOG.error( ex, ex );
          throw ex;
        }
      } else if ( Bootstrap.isCloudController( ) ) {
        try {
          trans = RemoteTransitionCallbacks.valueOf( transitionAction.name( ) );
        } catch ( Exception ex ) {
          LOG.error( ex, ex );
          throw ex;
        }
      } else {
        LOG.debug( "Silentlty accepting remotely inferred state transition for " + parent );
      }
      if ( trans != null ) {
        LOG.debug( "Executing transition: " + trans.getClass( ) + "." + transitionAction.name( ) + " for " + parent );
        trans.fire( parent );
      }
      transitionCallback.fire( );
    } catch ( Throwable ex ) {
      if ( ServiceExceptions.filterExceptions( parent, ex ) ) {
        transitionCallback.fireException( ex );
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
                          parent.toString( ) ).debug( );
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
                        parent.toString( ) ).debug( );
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
                          parent.toString( ) ).debug( );
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
                          parent.toString( ) ).debug( );
      } catch ( Exception ex ) {
        LOG.error( ex, ex );
      }
      
    }
    
  }
  
  enum RemoteTransitionCallbacks implements ServiceTransitionCallback {
    LOAD {
      
      @Override
      public void fire( final ServiceConfiguration parent ) throws Throwable {}
    },
    DESTROY {
      
      @Override
      public void fire( final ServiceConfiguration parent ) throws Throwable {}
    },
    CHECK {
      
      @Override
      public void fire( final ServiceConfiguration parent ) throws Throwable {
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
      public void fire( final ServiceConfiguration parent ) throws Throwable {
        StartServiceResponseType msg = ServiceTransitions.sendEmpyreanRequest( parent, new StartServiceType( ) {
          {
            this.getServices( ).add( TypeMappers.transform( parent, ServiceId.class ) );
          }
        } );
        try {
          parent.lookupComponent( ).getBuilder( ).fireStart( parent );
        } catch ( Exception ex ) {
          LOG.error( ex, ex );
        }
      }
    },
    ENABLE {
      
      @Override
      public void fire( final ServiceConfiguration parent ) throws Throwable {
        EnableServiceResponseType msg = ServiceTransitions.sendEmpyreanRequest( parent, new EnableServiceType( ) {
          {
            this.getServices( ).add( TypeMappers.transform( parent, ServiceId.class ) );
          }
        } );
        try {
          parent.lookupComponent( ).getBuilder( ).fireEnable( parent );
        } catch ( Exception ex ) {
          LOG.error( ex, ex );
        }
        
      }
    },
    DISABLE {
      
      @Override
      public void fire( final ServiceConfiguration parent ) throws Throwable {
        DisableServiceResponseType msg = ServiceTransitions.sendEmpyreanRequest( parent, new DisableServiceType( ) {
          {
            this.getServices( ).add( TypeMappers.transform( parent, ServiceId.class ) );
          }
        } );
        try {
          parent.lookupComponent( ).getBuilder( ).fireDisable( parent );
        } catch ( Exception ex ) {
          LOG.error( ex, ex );
        }
      }
    },
    STOP {
      
      @Override
      public void fire( final ServiceConfiguration parent ) throws Throwable {
        StopServiceResponseType msg = ServiceTransitions.sendEmpyreanRequest( parent, new StopServiceType( ) {
          {
            this.getServices( ).add( TypeMappers.transform( parent, ServiceId.class ) );
          }
        } );
        try {
          parent.lookupComponent( ).getBuilder( ).fireStop( parent );
        } catch ( Exception ex ) {
          LOG.error( ex, ex );
        }
      }
    };
    
  }
  
  enum LocalTransitionCallbacks implements ServiceTransitionCallback {
    LOAD {
      
      @Override
      public void fire( final ServiceConfiguration parent ) throws Throwable {
        parent.lookupComponent( ).getBootstrapper( ).load( );
      }
      
    },
    DESTROY {
      
      @Override
      public void fire( final ServiceConfiguration parent ) throws Throwable {
        parent.lookupComponent( ).getBootstrapper( ).destroy( );
      }
    },
    CHECK {
      
      @Override
      public void fire( final ServiceConfiguration parent ) throws Throwable {
        if ( State.LOADED.ordinal( ) < parent.lookupComponent( ).getState( ).ordinal( ) ) {
          try {
            parent.lookupComponent( ).getBootstrapper( ).check( );
            parent.lookupComponent( ).getBuilder( ).fireCheck( parent );
          } catch ( Throwable ex ) {
            if ( State.ENABLED.equals( parent.lookupState( ) ) ) {
              try {
                DISABLE.fire( parent );
              } catch ( Exception ex1 ) {
                LOG.error( ex1, ex1 );
              }
            }
            LOG.error( ex, ex );
            throw ex;
          }
        }
      }
    },
    START {
      
      @Override
      public void fire( final ServiceConfiguration parent ) throws Throwable {
        parent.lookupComponent( ).getBootstrapper( ).start( );
        parent.lookupComponent( ).getBuilder( ).fireStart( parent );
      }
    },
    ENABLE {
      
      @Override
      public void fire( final ServiceConfiguration parent ) throws Throwable {
        if ( State.NOTREADY.equals( parent.lookupComponent( ).getState( ) ) ) {
          parent.lookupComponent( ).getBootstrapper( ).check( );
          parent.lookupComponent( ).getBuilder( ).fireCheck( parent );
        }
        parent.lookupComponent( ).getBootstrapper( ).enable( );
        parent.lookupComponent( ).getBuilder( ).fireEnable( parent );
      }
    },
    DISABLE {
      
      @Override
      public void fire( final ServiceConfiguration parent ) throws Throwable {
        parent.lookupComponent( ).getBootstrapper( ).disable( );
        parent.lookupComponent( ).getBuilder( ).fireDisable( parent );
      }
    },
    STOP {
      
      @Override
      public void fire( final ServiceConfiguration parent ) throws Throwable {
        parent.lookupComponent( ).getBootstrapper( ).stop( );
        parent.lookupComponent( ).getBuilder( ).fireStop( parent );
      }
    };
    
  }
  
  public enum StateCallbacks implements Callback<ServiceConfiguration> {
    FIRE_START_EVENT {
      
      @Override
      public void fire( final ServiceConfiguration config ) {
        EventRecord.here( ServiceBuilder.class,
                          EventType.COMPONENT_SERVICE_START,
                          config.getFullName( ).toString( ), config.toString( ) ).debug( );
        LifecycleEvents.start( config );
      }
    },
    FIRE_STOP_EVENT {
      @Override
      public void fire( final ServiceConfiguration config ) {
        EventRecord.here( ServiceBuilder.class,
                                         EventType.COMPONENT_SERVICE_STOP,
                                         config.getFullName( ).toString( ), config.toString( ) ).debug( );
        LifecycleEvents.stop( config );
      }
    },
    FIRE_ENABLE_EVENT {
      @Override
      public void fire( final ServiceConfiguration config ) {
        EventRecord.here( ServiceBuilder.class,
                                         EventType.COMPONENT_SERVICE_ENABLE,
                                         config.getFullName( ).toString( ), config.toString( ) ).debug( );
        LifecycleEvents.enable( config );
      }
    },
    FIRE_DISABLE_EVENT {
      @Override
      public void fire( final ServiceConfiguration config ) {
        EventRecord.here( ServiceBuilder.class,
                                         EventType.COMPONENT_SERVICE_DISABLE,
                                         config.getFullName( ).toString( ), config.toString( ) ).debug( );
        LifecycleEvents.disable( config );
      }
      
    },
    ENDPOINT_START {
      @Override
      public void fire( final ServiceConfiguration parent ) {
        try {
          parent.lookupService( ).start( );
        } catch ( Exception ex ) {
          LOG.error( ex, ex );
        }
      }
    },
    ENDPOINT_STOP {
      @Override
      public void fire( final ServiceConfiguration parent ) {
        try {
          parent.lookupService( ).stop( );
        } catch ( Exception ex ) {
          LOG.error( ex, ex );
        }
      }
    },
    SERVICE_CONTEXT_RESTART {
      @Override
      public void fire( final ServiceConfiguration parent ) {
        if ( parent.isVmLocal( ) || parent.isHostLocal( ) ) {
          ServiceContextManager.restartSync( );
        }
      }
    },
    PIPELINES_ADD {
      @Override
      public void fire( final ServiceConfiguration parent ) {
        if ( parent.isVmLocal( ) || parent.isHostLocal( ) ) {
          PipelineRegistry.getInstance( ).enable( parent.getComponentId( ) );
        }
      }
    },
    PIPELINES_REMOVE {
      @Override
      public void fire( final ServiceConfiguration parent ) {
        if ( parent.isVmLocal( ) || parent.isHostLocal( ) ) {
          PipelineRegistry.getInstance( ).disable( parent.getComponentId( ) );
        }
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
        } catch ( Throwable ex ) {
          LOG.error( ex, ex );
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
        } catch ( Throwable ex ) {
          LOG.error( ex, ex );
        }
      }
      
    };
    
  }
  
}
