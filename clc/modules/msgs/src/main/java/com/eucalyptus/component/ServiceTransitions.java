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

import java.util.Arrays;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import org.apache.log4j.Logger;
import com.eucalyptus.component.Component.State;
import com.eucalyptus.context.ServiceContextManager;
import com.eucalyptus.empyrean.Empyrean;
import com.eucalyptus.records.EventRecord;
import com.eucalyptus.records.EventType;
import com.eucalyptus.system.Threads;
import com.eucalyptus.util.async.Callback;
import com.eucalyptus.util.async.Callback.Completion;
import com.eucalyptus.util.async.CheckedListenableFuture;
import com.eucalyptus.util.async.Futures;
import com.eucalyptus.util.fsm.AbstractTransitionAction;
import com.eucalyptus.util.fsm.TransitionAction;
import com.eucalyptus.ws.util.PipelineRegistry;

public class ServiceTransitions {
  private static Logger LOG = Logger.getLogger( ServiceTransitions.class );
  
  static final CheckedListenableFuture<ServiceConfiguration> startTransitionChain( final ServiceConfiguration config ) {
    final Service service = config.lookupService( );
    Callable<CheckedListenableFuture<ServiceConfiguration>> transition = null;
    switch ( service.getState( ) ) {
      case NOTREADY:
      case DISABLED:
      case ENABLED:
        break;
      case LOADED:
      case STOPPED:
        transition = ServiceTransitions.newServiceTransitionCallable( config, Component.State.LOADED, Component.State.NOTREADY );
        break;
      case INITIALIZED:
        transition = ServiceTransitions.newServiceTransitionCallable( config, Component.State.INITIALIZED, Component.State.LOADED, Component.State.NOTREADY );
        break;
      default:
        throw new IllegalStateException( "Failed to find transition for current component state: " + config.lookupComponent( ).toString( ) );
    }
    CheckedListenableFuture<ServiceConfiguration> transitionResult = null;
    try {
      transitionResult = Threads.lookup( Empyrean.class ).submit( transition ).get( );
    } catch ( InterruptedException ex ) {
      LOG.error( ex , ex );
      transitionResult = Futures.predestinedFailedFuture( ex );
    } catch ( ExecutionException ex ) {
      LOG.error( ex , ex );
      transitionResult = Futures.predestinedFailedFuture( ex );
    }
    return transitionResult;
  }

  static final CheckedListenableFuture<ServiceConfiguration> enableTransitionChain( final ServiceConfiguration config ) {
    final Service service = config.lookupService( );
    Callable<CheckedListenableFuture<ServiceConfiguration>> transition = null;
    switch ( service.getState( ) ) {
      case ENABLED:
        break;
      case NOTREADY:
      case DISABLED:
        transition = ServiceTransitions.newServiceTransitionCallable( config, Component.State.DISABLED, Component.State.ENABLED );
        break;
      case LOADED:
      case STOPPED:
        transition = ServiceTransitions.newServiceTransitionCallable( config, Component.State.LOADED, Component.State.NOTREADY, Component.State.DISABLED, Component.State.ENABLED );
        break;
      case INITIALIZED:
        transition = ServiceTransitions.newServiceTransitionCallable( config, Component.State.INITIALIZED, Component.State.LOADED, Component.State.NOTREADY, Component.State.DISABLED, Component.State.ENABLED );
        break;
      default:
        throw new IllegalStateException( "Failed to find transition for current component state: " + config.lookupComponent( ).toString( ) );
    }
    CheckedListenableFuture<ServiceConfiguration> transitionResult = null;
    try {
      transitionResult = Threads.lookup( Empyrean.class ).submit( transition ).get( );
    } catch ( InterruptedException ex ) {
      LOG.error( ex , ex );
      transitionResult = Futures.predestinedFailedFuture( ex );
    } catch ( ExecutionException ex ) {
      LOG.error( ex.getCause( ) , ex.getCause( ) );
      transitionResult = Futures.predestinedFailedFuture( ex.getCause( ) );
    }
    return transitionResult;
  }
  
  private static final Callable<CheckedListenableFuture<ServiceConfiguration>> newServiceTransitionCallable( final ServiceConfiguration config, final Component.State fromState, final Component.State... toStates ) {
    if ( toStates.length < 1 ) {
      throw new IllegalArgumentException( "At least one toState must be specified" );
    }
    final Component.State toState = ( toStates.length == 0 )
      ? toStates[0]
      : null;
    final Component.State nextFromState = toState;
    final Component.State[] nextStates = ( toStates.length > 1 )
      ? Arrays.copyOfRange( toStates, 1, toStates.length )
      : new Component.State[] {};
    final Callable<CheckedListenableFuture<ServiceConfiguration>> nextTransition = ( nextStates.length != 0 )
      ? newServiceTransitionCallable( config, nextFromState, nextStates )
      : null;
    return new Callable<CheckedListenableFuture<ServiceConfiguration>>( ) {
      @Override
      public CheckedListenableFuture<ServiceConfiguration> call( ) throws Exception {
        Service service = config.lookupComponent( ).lookupRegisteredService( config );
        if ( !fromState.equals( service.getState( ) ) ) {
          throw new IllegalStateException( "Attempt to transition from " + fromState + "->" + toState + " when service is currently in " + service.getState( )
                                           + " for " + config.toString( ) );
        } else {
          EventRecord.here( Component.class, EventType.CALLBACK, EventType.COMPONENT_SERVICE_TRANSITION.toString( ), fromState.toString( ), toState.toString( ), config.getFullName( ).toString( ) ).debug( );
          CheckedListenableFuture<ServiceConfiguration> future;
          try {
            future = service.transition( toState );
            if ( nextTransition != null ) {
              return future.addListener( nextTransition ).get( );
            } else {
              return future;
            }
          } catch ( Exception ex ) {
            LOG.error( ex, ex );
            throw ex;
          }
        }
      }
    };
  }
  
  public static final TransitionAction<ServiceConfiguration> LOAD_TRANSITION       = new AbstractTransitionAction<ServiceConfiguration>( ) {
                                                                                     
                                                                                     @Override
                                                                                     public void leave( ServiceConfiguration parent, Completion transitionCallback ) {
                                                                                       if ( parent.isLocal( ) ) {
                                                                                         try {
                                                                                           parent.lookupComponent( ).getBootstrapper( ).load( );
                                                                                           transitionCallback.fire( );
                                                                                         } catch ( Throwable ex ) {
                                                                                           ServiceState.LOG.error( "Transition failed on "
                                                                                                                   + parent.lookupComponent( ).getName( )
                                                                                                                   + " due to "
                                                                                                                   + ex.toString( ), ex );
//                                                                                            transitionCallback.fireException( ex );
                                                                                           transitionCallback.fire( );
                                                                                           parent.lookupComponent( ).submitError( ex );
                                                                                         }
                                                                                       } else {
                                                                                         //TODO:GRZE: do remote
                                                                                       }
                                                                                     }
                                                                                   };
  public static final TransitionAction<ServiceConfiguration> START_TRANSITION      = new AbstractTransitionAction<ServiceConfiguration>( ) {
                                                                                     @Override
                                                                                     public void leave( final ServiceConfiguration parent, final Completion transitionCallback ) {
                                                                                       if ( parent.isLocal( ) ) {
                                                                                         try {
                                                                                           parent.lookupComponent( ).getBootstrapper( ).start( );
                                                                                           if ( parent.lookupComponent( ).hasLocalService( ) ) {
                                                                                             parent.lookupComponent( ).getBuilder( ).fireStart( parent.lookupComponent( ).getLocalService( ).getServiceConfiguration( ) );
                                                                                           }
                                                                                           transitionCallback.fire( );
                                                                                         } catch ( Throwable ex ) {
                                                                                           ServiceState.LOG.error( "Transition failed on "
                                                                                                                   + parent.lookupComponent( ).getName( )
                                                                                                                   + " due to "
                                                                                                                   + ex.toString( ), ex );
                                                                                           transitionCallback.fireException( ex );
                                                                                           parent.lookupComponent( ).submitError( ex );
                                                                                         }
                                                                                       }
                                                                                     }
                                                                                   };
  public static final TransitionAction<ServiceConfiguration> ENABLE_TRANSITION     = new AbstractTransitionAction<ServiceConfiguration>( ) {
                                                                                     @Override
                                                                                     public void leave( ServiceConfiguration parent, Completion transitionCallback ) {
                                                                                       if ( parent.isLocal( ) ) {
                                                                                         try {
                                                                                           if ( State.NOTREADY.equals( parent.lookupComponent( ).getState( ) ) ) {
                                                                                             parent.lookupComponent( ).getBootstrapper( ).check( );
                                                                                             if ( parent.lookupComponent( ).hasLocalService( ) ) {
                                                                                               parent.lookupComponent( ).getBuilder( ).fireCheck( parent.lookupComponent( ).getLocalService( ).getServiceConfiguration( ) );
                                                                                             }
                                                                                           }
                                                                                           parent.lookupComponent( ).getBootstrapper( ).enable( );
                                                                                           if ( parent.lookupComponent( ).hasLocalService( ) ) {
                                                                                             parent.lookupComponent( ).getBuilder( ).fireEnable( parent.lookupComponent( ).getLocalService( ).getServiceConfiguration( ) );
                                                                                           }
                                                                                           transitionCallback.fire( );
                                                                                         } catch ( Throwable ex ) {
                                                                                           ServiceState.LOG.error( "Transition failed on "
                                                                                                                   + parent.lookupComponent( ).getName( )
                                                                                                                   + " due to "
                                                                                                                   + ex.toString( ), ex );
                                                                                           transitionCallback.fireException( ex );
                                                                                           parent.lookupComponent( ).submitError( ex );
                                                                                         }
                                                                                       }
                                                                                     }
                                                                                   };
  public static final TransitionAction<ServiceConfiguration> DISABLE_TRANSITION    = new AbstractTransitionAction<ServiceConfiguration>( ) {
                                                                                     @Override
                                                                                     public void leave( ServiceConfiguration parent, Completion transitionCallback ) {
                                                                                       if ( parent.isLocal( ) ) {
                                                                                         try {
                                                                                           parent.lookupComponent( ).getBootstrapper( ).disable( );
                                                                                           parent.lookupComponent( ).getBuilder( ).fireDisable( parent.lookupComponent( ).getLocalService( ).getServiceConfiguration( ) );
                                                                                           transitionCallback.fire( );
                                                                                         } catch ( Throwable ex ) {
                                                                                           ServiceState.LOG.error( "Transition failed on "
                                                                                                                   + parent.lookupComponent( ).getName( )
                                                                                                                   + " due to "
                                                                                                                   + ex.toString( ), ex );
                                                                                           transitionCallback.fireException( ex );
                                                                                           parent.lookupComponent( ).submitError( ex );
                                                                                         }
                                                                                       }
                                                                                     }
                                                                                   };
  public static final TransitionAction<ServiceConfiguration> STOP_TRANSITION       = new AbstractTransitionAction<ServiceConfiguration>( ) {
                                                                                     @Override
                                                                                     public void leave( ServiceConfiguration parent, Completion transitionCallback ) {
                                                                                       if ( parent.isLocal( ) ) {
                                                                                         try {
                                                                                           parent.lookupComponent( ).getBootstrapper( ).stop( );
                                                                                           if ( parent.lookupComponent( ).getLocalService( ) != null ) {
                                                                                             parent.lookupComponent( ).getBuilder( ).fireStop( parent.lookupComponent( ).getLocalService( ).getServiceConfiguration( ) );
                                                                                           }
                                                                                           transitionCallback.fire( );
                                                                                         } catch ( Throwable ex ) {
                                                                                           ServiceState.LOG.error( "Transition failed on "
                                                                                                                   + parent.lookupComponent( ).getName( )
                                                                                                                   + " due to "
                                                                                                                   + ex.toString( ), ex );
                                                                                           transitionCallback.fireException( ex );
                                                                                           parent.lookupComponent( ).submitError( ex );
                                                                                         }
                                                                                       }
                                                                                     }
                                                                                   };
  public static final TransitionAction<ServiceConfiguration> DESTROY_TRANSITION    = new AbstractTransitionAction<ServiceConfiguration>( ) {
                                                                                     @Override
                                                                                     public void leave( ServiceConfiguration parent, Completion transitionCallback ) {
                                                                                       if ( parent.isLocal( ) ) {
                                                                                         try {
                                                                                           parent.lookupComponent( ).getBootstrapper( ).destroy( );
                                                                                           transitionCallback.fire( );
                                                                                         } catch ( Throwable ex ) {
                                                                                           ServiceState.LOG.error( "Transition failed on "
                                                                                                                   + parent.lookupComponent( ).getName( )
                                                                                                                   + " due to "
                                                                                                                   + ex.toString( ), ex );
                                                                                           transitionCallback.fireException( ex );
                                                                                           parent.lookupComponent( ).submitError( ex );
                                                                                         }
                                                                                       }
                                                                                     }
                                                                                   };
  public static final TransitionAction<ServiceConfiguration> CHECK_TRANSITION      = new AbstractTransitionAction<ServiceConfiguration>( ) {
                                                                                     @Override
                                                                                     public void leave( ServiceConfiguration parent, Completion transitionCallback ) {
                                                                                       if ( parent.isLocal( ) ) {
                                                                                         try {
                                                                                           if ( State.LOADED.ordinal( ) < parent.lookupComponent( ).getState( ).ordinal( ) ) {
                                                                                             parent.lookupComponent( ).getBootstrapper( ).check( );
                                                                                             if ( parent.lookupComponent( ).getLocalService( ) != null ) {
                                                                                               parent.lookupComponent( ).getBuilder( ).fireCheck( parent.lookupComponent( ).getLocalService( ).getServiceConfiguration( ) );
                                                                                             }
                                                                                           }
                                                                                           transitionCallback.fire( );
                                                                                         } catch ( Throwable ex ) {
                                                                                           ServiceState.LOG.error( "Transition failed on "
                                                                                                                   + parent.lookupComponent( ).getName( )
                                                                                                                   + " due to "
                                                                                                                   + ex.toString( ),
                                                                                                                   ex );
                                                                                           if ( State.ENABLED.equals( parent.lookupComponent( ).getState( ) ) ) {
                                                                                             try {
                                                                                               parent.lookupComponent( ).getBootstrapper( ).disable( );
                                                                                               if ( parent.lookupComponent( ).hasLocalService( ) ) {
                                                                                                 parent.lookupComponent( ).getBuilder( ).fireDisable( parent.lookupComponent( ).getLocalService( ).getServiceConfiguration( ) );
                                                                                               }
                                                                                             } catch ( ServiceRegistrationException ex1 ) {
                                                                                               ServiceState.LOG.error( ex1, ex1 );
                                                                                             }
                                                                                           }
                                                                                           transitionCallback.fireException( ex );
                                                                                           parent.lookupComponent( ).submitError( ex );
                                                                                         }
                                                                                       }
                                                                                     }
                                                                                   };
  
  static final Callback<ServiceConfiguration>                startEndpoint         = new Callback<ServiceConfiguration>( ) {
                                                                                     @Override
                                                                                     public void fire( ServiceConfiguration parent ) {
                                                                                       if ( parent.getComponentId( ).hasDispatcher( ) && !parent.isLocal( ) ) {
                                                                                         try {
                                                                                           parent.lookupService( ).getEndpoint( ).start( );
                                                                                         } catch ( Exception ex ) {
                                                                                           LOG.error( ex, ex );
                                                                                         }
                                                                                       }
                                                                                     }
                                                                                   };
  
  static final Callback<ServiceConfiguration>                stopEndpoint          = new Callback<ServiceConfiguration>( ) {
                                                                                     @Override
                                                                                     public void fire( ServiceConfiguration parent ) {
                                                                                       if ( parent.getComponentId( ).hasDispatcher( ) && !parent.isLocal( ) ) {
                                                                                         try {
                                                                                           parent.lookupService( ).getEndpoint( ).stop( );
                                                                                         } catch ( Exception ex ) {
                                                                                           LOG.error( ex, ex );
                                                                                         }
                                                                                       }
                                                                                     }
                                                                                   };
  
  static final Callback<ServiceConfiguration>                restartServiceContext = new Callback<ServiceConfiguration>( ) {
                                                                                     @Override
                                                                                     public void fire( ServiceConfiguration parent ) {
                                                                                       if ( parent.isLocal( ) ) {
                                                                                         ServiceContextManager.restartSync( );
                                                                                       }
                                                                                     }
                                                                                   };
  
  static final Callback<ServiceConfiguration>                addPipelines          = new Callback<ServiceConfiguration>( ) {
                                                                                     @Override
                                                                                     public void fire( ServiceConfiguration parent ) {
                                                                                       if ( parent.isLocal( ) ) {
                                                                                         PipelineRegistry.getInstance( ).enable( parent.getComponentId( ) );
                                                                                       }
                                                                                     }
                                                                                   };
  
  static final Callback<ServiceConfiguration>                removePipelines       = new Callback<ServiceConfiguration>( ) {
                                                                                     @Override
                                                                                     public void fire( ServiceConfiguration parent ) {
                                                                                       if ( parent.isLocal( ) ) {
                                                                                         PipelineRegistry.getInstance( ).disable( parent.getComponentId( ) );
                                                                                       }
                                                                                     }
                                                                                   };
  
}
