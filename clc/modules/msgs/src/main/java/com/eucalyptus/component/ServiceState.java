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

import java.util.NavigableSet;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentSkipListSet;
import org.apache.log4j.Logger;
import com.eucalyptus.component.Component.State;
import com.eucalyptus.component.Component.Transition;
import com.eucalyptus.context.ServiceContextManager;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.async.Callback.Completion;
import com.eucalyptus.util.async.CheckedListenableFuture;
import com.eucalyptus.util.async.Futures;
import com.eucalyptus.util.fsm.AtomicMarkedState;
import com.eucalyptus.util.fsm.ExistingTransitionException;
import com.eucalyptus.util.fsm.StateMachineBuilder;
import com.eucalyptus.util.fsm.TransitionAction;
import com.eucalyptus.util.fsm.TransitionListener;
import com.eucalyptus.util.fsm.Transitions;
import com.eucalyptus.ws.util.PipelineRegistry;
import com.google.common.base.Predicate;

public class ServiceState {
  private static Logger                                         LOG                   = Logger.getLogger( ServiceState.class );
  private final AtomicMarkedState<Component, State, Transition> stateMachine;
  private final Component                                       parent;
  private Component.State                                       goal                  = Component.State.ENABLED;                 //TODO:GRZE:OMGFIXME
  private final NavigableSet<String>                            details               = new ConcurrentSkipListSet<String>( );
  private static final TransitionAction<Component>              LOAD_TRANSITION       = new TransitionAction<Component>( ) {
                                                                                        
                                                                                        @Override
                                                                                        public void leave( Component parent, Completion transitionCallback ) {
                                                                                          try {
                                                                                            parent.getBootstrapper( ).load( );
                                                                                            transitionCallback.fire( );
                                                                                          } catch ( Throwable ex ) {
                                                                                            LOG.error( "Transition failed on " + parent.getName( ) + " due to "
                                                                                                       + ex.toString( ), ex );
//                                                                                            transitionCallback.fireException( ex );
                                                                                            transitionCallback.fire( );
                                                                                            parent.submitError( ex );
                                                                                          }
                                                                                        }
                                                                                      };
  private static final TransitionAction<Component>              START_TRANSITION      = new TransitionAction<Component>( ) {
                                                                                        @Override
                                                                                        public void leave( final Component parent, final Completion transitionCallback ) {
                                                                                          try {
                                                                                            parent.getBootstrapper( ).start( );
                                                                                            if ( parent.hasLocalService( ) ) {
                                                                                              parent.getBuilder( ).fireStart( parent.getLocalService( ).getServiceConfiguration( ) );
                                                                                            }
                                                                                            transitionCallback.fire( );
                                                                                          } catch ( Throwable ex ) {
                                                                                            LOG.error( "Transition failed on " + parent.getName( ) + " due to "
                                                                                                       + ex.toString( ), ex );
                                                                                            transitionCallback.fireException( ex );
                                                                                            parent.submitError( ex );
                                                                                          }
                                                                                        }
                                                                                      };
  private static final TransitionAction<Component>              ENABLE_TRANSITION     = new TransitionAction<Component>( ) {
                                                                                        @Override
                                                                                        public void leave( Component parent, Completion transitionCallback ) {
                                                                                          try {
                                                                                            if ( State.NOTREADY.equals( parent.getState( ) ) ) {
                                                                                              parent.getBootstrapper( ).check( );
                                                                                              if ( parent.hasLocalService( ) ) {
                                                                                                parent.getBuilder( ).fireCheck( parent.getLocalService( ).getServiceConfiguration( ) );
                                                                                              }
                                                                                            }
                                                                                            parent.getBootstrapper( ).enable( );
                                                                                            if ( parent.hasLocalService( ) ) {
                                                                                              parent.getBuilder( ).fireEnable( parent.getLocalService( ).getServiceConfiguration( ) );
                                                                                            }
                                                                                            transitionCallback.fire( );
                                                                                          } catch ( Throwable ex ) {
                                                                                            LOG.error( "Transition failed on " + parent.getName( ) + " due to "
                                                                                                       + ex.toString( ), ex );
                                                                                            transitionCallback.fireException( ex );
                                                                                            parent.submitError( ex );
                                                                                          }
                                                                                        }
                                                                                      };
  private static final TransitionAction<Component>              DISABLE_TRANSITION    = new TransitionAction<Component>( ) {
                                                                                        @Override
                                                                                        public void leave( Component parent, Completion transitionCallback ) {
                                                                                          try {
                                                                                            parent.getBootstrapper( ).disable( );
                                                                                            parent.getBuilder( ).fireDisable( parent.getLocalService( ).getServiceConfiguration( ) );
                                                                                            transitionCallback.fire( );
                                                                                          } catch ( Throwable ex ) {
                                                                                            LOG.error( "Transition failed on " + parent.getName( ) + " due to "
                                                                                                       + ex.toString( ), ex );
                                                                                            transitionCallback.fireException( ex );
                                                                                            parent.submitError( ex );
                                                                                          }
                                                                                        }
                                                                                      };
  private static final TransitionAction<Component>              STOP_TRANSITION       = new TransitionAction<Component>( ) {
                                                                                        @Override
                                                                                        public void leave( Component parent, Completion transitionCallback ) {
                                                                                          try {
                                                                                            parent.getBootstrapper( ).stop( );
                                                                                            if ( parent.getLocalService( ) != null ) {
                                                                                              parent.getBuilder( ).fireStop( parent.getLocalService( ).getServiceConfiguration( ) );
                                                                                            }
                                                                                            transitionCallback.fire( );
                                                                                          } catch ( Throwable ex ) {
                                                                                            LOG.error( "Transition failed on " + parent.getName( ) + " due to "
                                                                                                       + ex.toString( ), ex );
                                                                                            transitionCallback.fireException( ex );
                                                                                            parent.submitError( ex );
                                                                                          }
                                                                                        }
                                                                                      };
  
  private static final TransitionAction<Component>              DESTROY_TRANSITION    = new TransitionAction<Component>( ) {
                                                                                        @Override
                                                                                        public void leave( Component parent, Completion transitionCallback ) {
                                                                                          try {
                                                                                            parent.getBootstrapper( ).destroy( );
                                                                                            transitionCallback.fire( );
                                                                                          } catch ( Throwable ex ) {
                                                                                            LOG.error( "Transition failed on " + parent.getName( ) + " due to "
                                                                                                       + ex.toString( ), ex );
                                                                                            transitionCallback.fireException( ex );
                                                                                            parent.submitError( ex );
                                                                                          }
                                                                                        }
                                                                                      };
  private static final TransitionAction<Component>              CHECK_TRANSITION      = new TransitionAction<Component>( ) {
                                                                                        @Override
                                                                                        public void leave( Component parent, Completion transitionCallback ) {
                                                                                          try {
                                                                                            if ( State.LOADED.ordinal( ) < parent.getState( ).ordinal( ) ) {
                                                                                              parent.getBootstrapper( ).check( );
                                                                                              if ( parent.getLocalService( ) != null ) {
                                                                                                parent.getBuilder( ).fireCheck( parent.getLocalService( ).getServiceConfiguration( ) );
                                                                                              }
                                                                                            }
                                                                                            transitionCallback.fire( );
                                                                                          } catch ( Throwable ex ) {
                                                                                            LOG.error( "Transition failed on " + parent.getName( ) + " due to "
                                                                                                       + ex.toString( ),
                                                                                                       ex );
                                                                                            if ( State.ENABLED.equals( parent.getState( ) ) ) {
                                                                                              try {
                                                                                                parent.getBootstrapper( ).disable( );
                                                                                                if ( parent.hasLocalService( ) ) {
                                                                                                  parent.getBuilder( ).fireDisable( parent.getLocalService( ).getServiceConfiguration( ) );
                                                                                                }
                                                                                              } catch ( ServiceRegistrationException ex1 ) {
                                                                                                LOG.error( ex1, ex1 );
                                                                                              }
                                                                                            }
                                                                                            transitionCallback.fireException( ex );
                                                                                            parent.submitError( ex );
                                                                                          }
                                                                                        }
                                                                                      };
  
  private static final TransitionListener<Component>            restartServiceContext = Transitions.createListener( new Predicate<Component>( ) {
                                                                                        @Override
                                                                                        public boolean apply( Component arg0 ) {
                                                                                          ServiceContextManager.restart( );
                                                                                          return true;
                                                                                        }
                                                                                      } );
  private static final TransitionListener<Component>            addPipelines          = Transitions.createListener( new Predicate<Component>( ) {
                                                                                        @Override
                                                                                        public boolean apply( Component arg0 ) {
                                                                                          PipelineRegistry.getInstance( ).enable( arg0.getComponentId( ) );
                                                                                          return true;
                                                                                        }
                                                                                      } );
  private static final TransitionListener<Component>            removePipelines       = Transitions.createListener( new Predicate<Component>( ) {
                                                                                        @Override
                                                                                        public boolean apply( Component arg0 ) {
                                                                                          PipelineRegistry.getInstance( ).disable( arg0.getComponentId( ) );
                                                                                          return true;
                                                                                        }
                                                                                      } );
  
  public ServiceState( Component parent ) {
    this.parent = parent;
    this.stateMachine = this.buildStateMachine( );
  }
  
  public State getState( ) {
    return this.stateMachine.getState( );
  }
  
  public String getDetails( ) {
    return this.details.toString( );
  }
  
  private AtomicMarkedState<Component, State, Transition> buildStateMachine( ) {
    
    return new StateMachineBuilder<Component, State, Transition>( this.parent, State.PRIMORDIAL ) {
      {
        on( Transition.INITIALIZING ).from( State.PRIMORDIAL ).to( State.INITIALIZED ).error( State.BROKEN ).noop( );
        on( Transition.LOADING ).from( State.INITIALIZED ).to( State.LOADED ).error( State.BROKEN ).run( LOAD_TRANSITION );
        on( Transition.STARTING ).from( State.LOADED ).to( State.NOTREADY ).error( State.BROKEN ).run( START_TRANSITION );
        on( Transition.ENABLING ).from( State.DISABLED ).to( State.ENABLED ).error( State.NOTREADY ).add( addPipelines, restartServiceContext ).run( ENABLE_TRANSITION );
        on( Transition.DISABLING ).from( State.ENABLED ).to( State.DISABLED ).error( State.NOTREADY ).add( removePipelines, restartServiceContext ).run( DISABLE_TRANSITION );
        on( Transition.STOPPING ).from( State.DISABLED ).to( State.STOPPED ).error( State.NOTREADY ).run( STOP_TRANSITION );
        on( Transition.DESTROYING ).from( State.STOPPED ).to( State.LOADED ).error( State.BROKEN ).run( DESTROY_TRANSITION );
        on( Transition.READY_CHECK ).from( State.NOTREADY ).to( State.DISABLED ).error( State.NOTREADY ).run( CHECK_TRANSITION );
        on( Transition.DISABLED_CHECK ).from( State.DISABLED ).to( State.DISABLED ).error( State.NOTREADY ).run( CHECK_TRANSITION );
        on( Transition.ENABLED_CHECK ).from( State.ENABLED ).to( State.ENABLED ).error( State.NOTREADY ).run( CHECK_TRANSITION );
      }
    }.newAtomicState( );
  }
  
  public CheckedListenableFuture<Component> transition( Transition transition ) throws IllegalStateException, NoSuchElementException, ExistingTransitionException {
    if ( !this.parent.isAvailableLocally( ) ) {
      throw new IllegalStateException( "Failed to perform service transition " + transition + " for " + this.parent.getName( )
                                       + " because it is not available locally." );
    }
    try {
      return this.stateMachine.startTransition( transition );
    } catch ( IllegalStateException ex ) {
      throw Exceptions.trace( ex );
    } catch ( NoSuchElementException ex ) {
      throw Exceptions.trace( ex );
    } catch ( ExistingTransitionException ex ) {
      throw ex;
    } catch ( Throwable ex ) {
      throw Exceptions.trace( new RuntimeException( "Failed to perform service transition " + transition + " for " + this.parent.getName( ) + ".\nCAUSE: "
                                                    + ex.getMessage( ) + "\nSTATE: " + this.stateMachine.toString( ), ex ) );
    }
  }
  
  public CheckedListenableFuture<Component> transition( State state ) throws IllegalStateException, NoSuchElementException, ExistingTransitionException {
    try {
      return this.stateMachine.startTransitionTo( state );
    } catch ( IllegalStateException ex ) {
      throw Exceptions.trace( ex );
    } catch ( NoSuchElementException ex ) {
      throw Exceptions.trace( ex );
    } catch ( ExistingTransitionException ex ) {
      throw ex;
    } catch ( Throwable ex ) {
      throw Exceptions.trace( new RuntimeException( "Failed to perform transition from " + this.getState( ) + " to " + state + " for " + this.parent.getName( )
                                                    + ".\nCAUSE: " + ex.getMessage( ) + "\nSTATE: " + this.stateMachine.toString( ), ex ) );
    }
  }
  
  public CheckedListenableFuture<Component> transitionSelf( ) {
    try {
      if ( this.checkTransition( Transition.READY_CHECK ) ) {//this is a special case of a transition which does not return to itself on a successful check
        return this.transition( Transition.READY_CHECK );
      } else {
        return this.transition( this.getState( ) );
      }
    } catch ( IllegalStateException ex ) {
      LOG.error( Exceptions.filterStackTrace( ex ) );
    } catch ( NoSuchElementException ex ) {
      LOG.error( Exceptions.filterStackTrace( ex ) );
    } catch ( ExistingTransitionException ex ) {}
    return Futures.predestinedFuture( this.parent );
  }
  
  /**
   * @return the goal
   */
  public Component.State getGoal( ) {
    return this.goal;
  }
  
  void setGoal( Component.State goal ) {
    this.goal = goal;
  }
  
  public boolean isBusy( ) {
    return this.stateMachine.isBusy( );
  }
  
  public boolean checkTransition( Transition transition ) {
    return this.parent.isAvailableLocally( ) && this.stateMachine.isLegalTransition( transition );
  }
  
}
