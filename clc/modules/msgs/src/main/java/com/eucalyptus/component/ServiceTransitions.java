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
import java.util.concurrent.Future;
import org.apache.log4j.Logger;
import com.eucalyptus.component.Component.State;
import com.eucalyptus.component.event.LifecycleEvents;
import com.eucalyptus.configurable.ConfigurableProperty;
import com.eucalyptus.configurable.MultiDatabasePropertyEntry;
import com.eucalyptus.configurable.PropertyDirectory;
import com.eucalyptus.configurable.SingletonDatabasePropertyEntry;
import com.eucalyptus.context.ServiceContextManager;
import com.eucalyptus.empyrean.Empyrean;
import com.eucalyptus.event.EventFailedException;
import com.eucalyptus.event.ListenerRegistry;
import com.eucalyptus.records.EventRecord;
import com.eucalyptus.records.EventType;
import com.eucalyptus.system.Threads;
import com.eucalyptus.util.Internets;
import com.eucalyptus.util.async.Callback;
import com.eucalyptus.util.async.Callback.Completion;
import com.eucalyptus.util.async.CheckedListenableFuture;
import com.eucalyptus.util.async.Futures;
import com.eucalyptus.util.fsm.AbstractTransitionAction;
import com.eucalyptus.util.fsm.Automata;
import com.eucalyptus.util.fsm.TransitionAction;
import com.eucalyptus.ws.util.PipelineRegistry;

public class ServiceTransitions {
  private static Logger LOG = Logger.getLogger( ServiceTransitions.class );
  
  public static CheckedListenableFuture<ServiceConfiguration> transitionChain( ServiceConfiguration configuration, State goalState ) {
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
    if ( !State.NOTREADY.equals( config.lookupState( ) ) && !State.DISABLED.equals( config.lookupState( ) ) ) {
      CheckedListenableFuture<ServiceConfiguration> transitionResult = null;
      try {
        Callable<CheckedListenableFuture<ServiceConfiguration>> transition = Automata.sequenceTransitions( config, Component.State.LOADED,
                                                                                                           Component.State.NOTREADY, Component.State.DISABLED );
        
        Future<CheckedListenableFuture<ServiceConfiguration>> result = Threads.lookup( Empyrean.class ).submit( transition );
        transitionResult = result.get( );
      } catch ( InterruptedException ex ) {
        LOG.error( ex, ex );
        transitionResult = Futures.predestinedFailedFuture( ex );
      } catch ( ExecutionException ex ) {
        LOG.error( ex.getCause( ), ex.getCause( ) );
        transitionResult = Futures.predestinedFailedFuture( ex.getCause( ) );
      }
      return transitionResult;
    } else {
      return Futures.predestinedFuture( config );
    }
  }
  
  static final CheckedListenableFuture<ServiceConfiguration> enableTransitionChain( final ServiceConfiguration config ) {
    if ( !State.ENABLED.equals( config.lookupState( ) ) ) {
      CheckedListenableFuture<ServiceConfiguration> transitionResult = null;
      try {
        Callable<CheckedListenableFuture<ServiceConfiguration>> transition = Automata.sequenceTransitions( config, Component.State.DISABLED, Component.State.ENABLED );
        Future<CheckedListenableFuture<ServiceConfiguration>> result = Threads.lookup( Empyrean.class ).submit( transition );
        transitionResult = result.get( );
      } catch ( InterruptedException ex ) {
        LOG.error( ex, ex );
        transitionResult = Futures.predestinedFailedFuture( ex );
      } catch ( ExecutionException ex ) {
        LOG.error( ex.getCause( ), ex.getCause( ) );
        transitionResult = Futures.predestinedFailedFuture( ex.getCause( ) );
      }
      return transitionResult;
    } else {
      return Futures.predestinedFuture( config );
    }
  }
  static final CheckedListenableFuture<ServiceConfiguration> disableTransitionChain( final ServiceConfiguration config ) {
    if ( !State.DISABLED.equals( config.lookupState( ) ) ) {
      CheckedListenableFuture<ServiceConfiguration> transitionResult = null;
      try {
        Callable<CheckedListenableFuture<ServiceConfiguration>> transition = Automata.sequenceTransitions( config, Component.State.ENABLED, Component.State.DISABLED );
        Future<CheckedListenableFuture<ServiceConfiguration>> result = Threads.lookup( Empyrean.class ).submit( transition );
        transitionResult = result.get( );
      } catch ( InterruptedException ex ) {
        LOG.error( ex, ex );
        transitionResult = Futures.predestinedFailedFuture( ex );
      } catch ( ExecutionException ex ) {
        LOG.error( ex.getCause( ), ex.getCause( ) );
        transitionResult = Futures.predestinedFailedFuture( ex.getCause( ) );
      }
      return transitionResult;
    } else {
      return Futures.predestinedFuture( config );
    }
  }
  static final CheckedListenableFuture<ServiceConfiguration> stopTransitionChain( final ServiceConfiguration config ) {
    if ( !State.STOPPED.equals( config.lookupState( ) ) ) {
      CheckedListenableFuture<ServiceConfiguration> transitionResult = null;
      try {
        Callable<CheckedListenableFuture<ServiceConfiguration>> transition = Automata.sequenceTransitions( config, Component.State.ENABLED, Component.State.DISABLED, Component.State.STOPPED );
        Future<CheckedListenableFuture<ServiceConfiguration>> result = Threads.lookup( Empyrean.class ).submit( transition );
        transitionResult = result.get( );
      } catch ( InterruptedException ex ) {
        LOG.error( ex, ex );
        transitionResult = Futures.predestinedFailedFuture( ex );
      } catch ( ExecutionException ex ) {
        LOG.error( ex.getCause( ), ex.getCause( ) );
        transitionResult = Futures.predestinedFailedFuture( ex.getCause( ) );
      }
      return transitionResult;
    } else {
      return Futures.predestinedFuture( config );
    }
  }
  static final CheckedListenableFuture<ServiceConfiguration> destroyTransitionChain( final ServiceConfiguration config ) {
    if ( !State.PRIMORDIAL.equals( config.lookupState( ) ) ) {
      CheckedListenableFuture<ServiceConfiguration> transitionResult = null;
      try {
        Callable<CheckedListenableFuture<ServiceConfiguration>> transition = Automata.sequenceTransitions( config, Component.State.ENABLED, Component.State.DISABLED, Component.State.STOPPED );
        Future<CheckedListenableFuture<ServiceConfiguration>> result = Threads.lookup( Empyrean.class ).submit( transition );
        transitionResult = result.get( );
      } catch ( InterruptedException ex ) {
        LOG.error( ex, ex );
        transitionResult = Futures.predestinedFailedFuture( ex );
      } catch ( ExecutionException ex ) {
        LOG.error( ex.getCause( ), ex.getCause( ) );
        transitionResult = Futures.predestinedFailedFuture( ex.getCause( ) );
      }
      return transitionResult;
    } else {
      return Futures.predestinedFuture( config );
    }
  }
  
  public static final TransitionAction<ServiceConfiguration> LOAD_TRANSITION       = new AbstractTransitionAction<ServiceConfiguration>( ) {
                                                                                     
                                                                                     @Override
                                                                                     public void leave( ServiceConfiguration parent, Completion transitionCallback ) {
                                                                                       if ( parent.isLocal( ) ) {
                                                                                         try {
                                                                                           parent.lookupComponent( ).getBootstrapper( ).load( );
                                                                                           transitionCallback.fire( );
                                                                                         } catch ( Throwable ex ) {
                                                                                           LOG.error( "Transition failed on "
                                                                                                                   + parent.lookupComponent( ).getName( )
                                                                                                                   + " due to "
                                                                                                                   + ex.toString( ), ex );
//TODO:GRZE: RESTORE THIS                                                                                            transitionCallback.fireException( ex );
                                                                                           transitionCallback.fire( );
                                                                                           parent.error( ex );
                                                                                         }
                                                                                       } else {
                                                                                         transitionCallback.fire( );
                                                                                       }
                                                                                     }
                                                                                   };
  public static final TransitionAction<ServiceConfiguration> START_TRANSITION      = new AbstractTransitionAction<ServiceConfiguration>( ) {
                                                                                     @Override
                                                                                     public void leave( final ServiceConfiguration parent, final Completion transitionCallback ) {
                                                                                       EventRecord.here( ServiceBuilder.class,
                                                                                                         EventType.COMPONENT_SERVICE_START,
                                                                                                         parent.getFullName( ).toString( ),
                                                                                                         parent.toString( ) ).info( );
                                                                                       if ( parent.isLocal( ) || Internets.testLocal( parent.getHostName( ) ) ) {
                                                                                         try {
                                                                                           parent.lookupComponent( ).getBootstrapper( ).start( );
                                                                                           if ( parent.lookupComponent( ).hasLocalService( ) ) {
                                                                                             parent.lookupComponent( ).getBuilder( ).fireStart( parent );
                                                                                             try {
                                                                                               ListenerRegistry.getInstance( ).fireEvent( parent.getComponentId( ).getClass( ),
                                                                                                                                          LifecycleEvents.start( parent ) );
                                                                                             } catch ( EventFailedException e1 ) {
                                                                                               LOG.error( e1, e1 );
                                                                                               throw new ServiceRegistrationException( e1.getMessage( ), e1 );
                                                                                             }
                                                                                           }
                                                                                           transitionCallback.fire( );
                                                                                         } catch ( Throwable ex ) {
                                                                                           LOG.error( "Transition failed on "
                                                                                                                   + parent.lookupComponent( ).getName( )
                                                                                                                   + " due to "
                                                                                                                   + ex.toString( ), ex );
                                                                                           transitionCallback.fireException( ex );
                                                                                           parent.error( ex );
                                                                                         }
                                                                                       } else {
                                                                                         try {
                                                                                           parent.lookupComponent( ).getBuilder( ).fireStart( parent );
                                                                                           transitionCallback.fire( );//TODO:GRZE: this is not complete.
                                                                                         } catch ( Throwable ex ) {
                                                                                           LOG.error( "Transition failed on "
                                                                                                      + parent.lookupComponent( ).getName( )
                                                                                                      + " due to "
                                                                                                      + ex.toString( ),
                                                                                                      ex );
                                                                                           transitionCallback.fireException( ex );
                                                                                           parent.error( ex );
                                                                                         }
                                                                                       }
                                                                                     }
                                                                                   };
  public static final TransitionAction<ServiceConfiguration> ENABLE_TRANSITION     = new AbstractTransitionAction<ServiceConfiguration>( ) {
                                                                                     @Override
                                                                                     public void leave( ServiceConfiguration parent, Completion transitionCallback ) {
                                                                                       EventRecord.here( ServiceBuilder.class,
                                                                                                         EventType.COMPONENT_SERVICE_ENABLE,
                                                                                                         parent.getFullName( ).toString( ),
                                                                                                         parent.toString( ) ).info( );
                                                                                       if ( parent.isLocal( ) || Internets.testLocal( parent.getHostName( ) ) ) {
                                                                                         try {
                                                                                           if ( State.NOTREADY.equals( parent.lookupComponent( ).getState( ) ) ) {
                                                                                             parent.lookupComponent( ).getBootstrapper( ).check( );
                                                                                             parent.lookupComponent( ).getBuilder( ).fireCheck( parent );
                                                                                           }
                                                                                           parent.lookupComponent( ).getBootstrapper( ).enable( );
                                                                                           parent.lookupComponent( ).getBuilder( ).fireCheck( parent );
                                                                                           transitionCallback.fire( );
                                                                                         } catch ( Throwable ex ) {
                                                                                           LOG.error( "Transition failed on "
                                                                                                                   + parent.lookupComponent( ).getName( )
                                                                                                                   + " due to "
                                                                                                                   + ex.toString( ), ex );
                                                                                           transitionCallback.fireException( ex );
                                                                                           parent.error( ex );
                                                                                         }
                                                                                       } else {
                                                                                         try {
                                                                                           parent.lookupComponent( ).getBuilder( ).fireEnable( parent );
                                                                                           transitionCallback.fire( );//TODO:GRZE: this is not complete.
                                                                                         } catch ( Throwable ex ) {
                                                                                           LOG.error( "Transition failed on "
                                                                                                      + parent.lookupComponent( ).getName( )
                                                                                                      + " due to "
                                                                                                      + ex.toString( ),
                                                                                                      ex );
                                                                                           transitionCallback.fireException( ex );
                                                                                           parent.error( ex );
                                                                                         }
                                                                                       }
                                                                                     }
                                                                                   };
  public static final TransitionAction<ServiceConfiguration> DISABLE_TRANSITION    = new AbstractTransitionAction<ServiceConfiguration>( ) {
                                                                                     @Override
                                                                                     public void leave( ServiceConfiguration parent, Completion transitionCallback ) {
                                                                                       EventRecord.here( ServiceBuilder.class,
                                                                                                         EventType.COMPONENT_SERVICE_DISABLE,
                                                                                                         parent.getFullName( ).toString( ),
                                                                                                         parent.toString( ) ).info( );
                                                                                       if ( parent.isLocal( ) || Internets.testLocal( parent.getHostName( ) ) ) {
                                                                                         try {
                                                                                           parent.lookupComponent( ).getBootstrapper( ).disable( );
                                                                                           parent.lookupComponent( ).getBuilder( ).fireDisable( parent );
                                                                                           transitionCallback.fire( );
                                                                                         } catch ( Throwable ex ) {
                                                                                           LOG.error( "Transition failed on "
                                                                                                                   + parent.lookupComponent( ).getName( )
                                                                                                                   + " due to "
                                                                                                                   + ex.toString( ), ex );
                                                                                           transitionCallback.fireException( ex );
                                                                                           parent.error( ex );
                                                                                         }
                                                                                       } else {
                                                                                         try {
                                                                                           parent.lookupComponent( ).getBuilder( ).fireDisable( parent );
                                                                                           transitionCallback.fire( );//TODO:GRZE: this is not complete.
                                                                                         } catch ( Throwable ex ) {
                                                                                           LOG.error( "Transition failed on "
                                                                                                      + parent.lookupComponent( ).getName( )
                                                                                                      + " due to "
                                                                                                      + ex.toString( ),
                                                                                                      ex );
                                                                                           transitionCallback.fireException( ex );
                                                                                           parent.error( ex );
                                                                                         }
                                                                                       }
                                                                                     }
                                                                                   };
  public static final TransitionAction<ServiceConfiguration> STOP_TRANSITION       = new AbstractTransitionAction<ServiceConfiguration>( ) {
                                                                                     @Override
                                                                                     public void leave( ServiceConfiguration parent, Completion transitionCallback ) {
                                                                                       EventRecord.here( ServiceBuilder.class,
                                                                                                         EventType.COMPONENT_SERVICE_STOP,
                                                                                                         parent.getFullName( ).toString( ),
                                                                                                         parent.toString( ) ).debug( );
                                                                                       if ( parent.isLocal( ) || Internets.testLocal( parent.getHostName( ) ) ) {
                                                                                         try {
                                                                                           parent.lookupComponent( ).getBootstrapper( ).stop( );
                                                                                           parent.lookupComponent( ).getBuilder( ).fireStop( parent );
                                                                                           transitionCallback.fire( );
                                                                                         } catch ( Throwable ex ) {
                                                                                           LOG.error( "Transition failed on "
                                                                                                                   + parent.lookupComponent( ).getName( )
                                                                                                                   + " due to "
                                                                                                                   + ex.toString( ), ex );
                                                                                           transitionCallback.fireException( ex );
                                                                                           parent.error( ex );
                                                                                         }
                                                                                       } else {
                                                                                         try {
                                                                                           parent.lookupComponent( ).getBuilder( ).fireStop( parent );
                                                                                           transitionCallback.fire( );//TODO:GRZE: this is not complete.
                                                                                         } catch ( Throwable ex ) {
                                                                                           LOG.error( "Transition failed on "
                                                                                                      + parent.lookupComponent( ).getName( )
                                                                                                      + " due to "
                                                                                                      + ex.toString( ),
                                                                                                      ex );
                                                                                           transitionCallback.fireException( ex );
                                                                                           parent.error( ex );
                                                                                         }
                                                                                       }
                                                                                     }
                                                                                   };
  public static final TransitionAction<ServiceConfiguration> DESTROY_TRANSITION    = new AbstractTransitionAction<ServiceConfiguration>( ) {
                                                                                     @Override
                                                                                     public void leave( ServiceConfiguration parent, Completion transitionCallback ) {
                                                                                       if ( parent.isLocal( ) || Internets.testLocal( parent.getHostName( ) ) ) {
                                                                                         try {
                                                                                           parent.lookupComponent( ).getBootstrapper( ).destroy( );
                                                                                           transitionCallback.fire( );
                                                                                         } catch ( Throwable ex ) {
                                                                                           LOG.error( "Transition failed on "
                                                                                                                   + parent.lookupComponent( ).getName( )
                                                                                                                   + " due to "
                                                                                                                   + ex.toString( ), ex );
                                                                                           transitionCallback.fireException( ex );
                                                                                           parent.error( ex );
                                                                                         }
                                                                                       } else {
                                                                                         transitionCallback.fire( );//TODO:GRZE: this is not complete.
                                                                                       }
                                                                                     }
                                                                                   };
  public static final TransitionAction<ServiceConfiguration> CHECK_TRANSITION      = new AbstractTransitionAction<ServiceConfiguration>( ) {
                                                                                     @Override
                                                                                     public void leave( ServiceConfiguration parent, Completion transitionCallback ) {
                                                                                       if ( parent.isLocal( ) || Internets.testLocal( parent.getHostName( ) ) ) {
                                                                                         try {
                                                                                           if ( State.LOADED.ordinal( ) < parent.lookupComponent( ).getState( ).ordinal( ) ) {
                                                                                             parent.lookupComponent( ).getBootstrapper( ).check( );
                                                                                             if ( parent.lookupComponent( ).hasLocalService( ) ) {
                                                                                               parent.lookupComponent( ).getBuilder( ).fireCheck( parent );
                                                                                             }
                                                                                           }
                                                                                           transitionCallback.fire( );
                                                                                         } catch ( Throwable ex ) {
                                                                                           LOG.error( "Transition failed on "
                                                                                                                   + parent.lookupComponent( ).getName( )
                                                                                                                   + " due to "
                                                                                                                   + ex.toString( ),
                                                                                                                   ex );
                                                                                           if ( State.ENABLED.isIn( parent ) ) {
                                                                                             try {
                                                                                               parent.lookupComponent( ).getBootstrapper( ).disable( );
                                                                                               if ( parent.lookupComponent( ).hasLocalService( ) ) {
                                                                                                 parent.lookupComponent( ).getBuilder( ).fireDisable( parent );
                                                                                               }
                                                                                             } catch ( Throwable ex1 ) {
                                                                                               LOG.error( "Transition failed on "
                                                                                                          + parent.lookupComponent( ).getName( )
                                                                                                          + " due to "
                                                                                                          + ex.toString( ),
                                                                                                          ex );
                                                                                             }
                                                                                           }
                                                                                           transitionCallback.fireException( ex );
                                                                                           parent.error( ex );
                                                                                         }
                                                                                       } else {
                                                                                         transitionCallback.fire( );//TODO:GRZE: this is not complete.
                                                                                       }
                                                                                     }
                                                                                   };
  
  static final Callback<ServiceConfiguration>                fireStartEvent        = new Callback<ServiceConfiguration>( ) {
                                                                                     
                                                                                     @Override
                                                                                     public void fire( ServiceConfiguration config ) {
                                                                                       EventRecord.here( ServiceBuilder.class,
                                                                                                         EventType.COMPONENT_SERVICE_START,
                                                                                                         config.getFullName( ).toString( ), config.toString( ) ).debug( );
                                                                                       try {
                                                                                         ListenerRegistry.getInstance( ).fireEvent( config.getComponentId( ).getClass( ),
                                                                                                                                    LifecycleEvents.start( config ) );
                                                                                       } catch ( EventFailedException e1 ) {
                                                                                         LOG.error( e1, e1 );
                                                                                         config.error( new ServiceRegistrationException( e1.getMessage( ), e1 ) );
                                                                                       }
                                                                                       
                                                                                     }
                                                                                   };
  static final Callback<ServiceConfiguration>                fireStopEvent         = new Callback<ServiceConfiguration>( ) {
                                                                                     
                                                                                     @Override
                                                                                     public void fire( ServiceConfiguration config ) {
                                                                                       EventRecord.here( ServiceBuilder.class,
                                                                                                         EventType.COMPONENT_SERVICE_STOP,
                                                                                                         config.getFullName( ).toString( ), config.toString( ) ).debug( );
                                                                                       try {
                                                                                         ListenerRegistry.getInstance( ).fireEvent( config.getComponentId( ).getClass( ),
                                                                                                                                    LifecycleEvents.stop( config ) );
                                                                                       } catch ( EventFailedException e1 ) {
                                                                                         LOG.error( e1, e1 );
                                                                                         config.error( new ServiceRegistrationException( e1.getMessage( ), e1 ) );
                                                                                       }
                                                                                       
                                                                                     }
                                                                                   };
  static final Callback<ServiceConfiguration>                fireEnableEvent       = new Callback<ServiceConfiguration>( ) {
                                                                                     
                                                                                     @Override
                                                                                     public void fire( ServiceConfiguration config ) {
                                                                                       EventRecord.here( ServiceBuilder.class,
                                                                                                         EventType.COMPONENT_SERVICE_ENABLE,
                                                                                                         config.getFullName( ).toString( ), config.toString( ) ).debug( );
                                                                                       try {
                                                                                         ListenerRegistry.getInstance( ).fireEvent( config.getComponentId( ).getClass( ),
                                                                                                                                    LifecycleEvents.enable( config ) );
                                                                                       } catch ( EventFailedException e1 ) {
                                                                                         LOG.error( e1, e1 );
                                                                                         config.error( new ServiceRegistrationException( e1.getMessage( ), e1 ) );
                                                                                       }
                                                                                     }
                                                                                   };
  static final Callback<ServiceConfiguration>                fireDisableEvent      = new Callback<ServiceConfiguration>( ) {
                                                                                     
                                                                                     @Override
                                                                                     public void fire( ServiceConfiguration config ) {
                                                                                       EventRecord.here( ServiceBuilder.class,
                                                                                                         EventType.COMPONENT_SERVICE_DISABLE,
                                                                                                         config.getFullName( ).toString( ), config.toString( ) ).debug( );
                                                                                       try {
                                                                                         ListenerRegistry.getInstance( ).fireEvent( config.getComponentId( ).getClass( ),
                                                                                                                                    LifecycleEvents.disable( config ) );
                                                                                       } catch ( EventFailedException e1 ) {
                                                                                         LOG.error( e1, e1 );
                                                                                         config.error( new ServiceRegistrationException( e1.getMessage( ), e1 ) );
                                                                                       }
                                                                                     }
                                                                                   };
  static final Callback<ServiceConfiguration>                startEndpoint         = new Callback<ServiceConfiguration>( ) {
                                                                                     @Override
                                                                                     public void fire( ServiceConfiguration parent ) {
                                                                                       if ( parent.getComponentId( ).hasDispatcher( ) && !parent.isLocal( ) ) {//TODO:GRZE:URGENT fix this brain-damaged corner case
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
                                                                                       if ( parent.isLocal( ) || Internets.testLocal( parent.getHostName( ) ) ) {
                                                                                         ServiceContextManager.restartSync( );
                                                                                       }
                                                                                     }
                                                                                   };
  
  static final Callback<ServiceConfiguration>                addPipelines          = new Callback<ServiceConfiguration>( ) {
                                                                                     @Override
                                                                                     public void fire( ServiceConfiguration parent ) {
                                                                                       if ( parent.isLocal( ) || Internets.testLocal( parent.getHostName( ) ) ) {
                                                                                         PipelineRegistry.getInstance( ).enable( parent.getComponentId( ) );
                                                                                       }
                                                                                     }
                                                                                   };
  
  static final Callback<ServiceConfiguration>                removePipelines       = new Callback<ServiceConfiguration>( ) {
                                                                                     @Override
                                                                                     public void fire( ServiceConfiguration parent ) {
                                                                                       if ( parent.isLocal( ) || Internets.testLocal( parent.getHostName( ) ) ) {
                                                                                         PipelineRegistry.getInstance( ).disable( parent.getComponentId( ) );
                                                                                       }
                                                                                     }
                                                                                   };
  static final Callback<ServiceConfiguration>                addProperties         = new Callback<ServiceConfiguration>( ) {
                                                                                     @Override
                                                                                     public void fire( ServiceConfiguration config ) {
                                                                                       try {
                                                                                         List<ConfigurableProperty> props = PropertyDirectory.getPendingPropertyEntrySet( config.getComponentId( ).name( ) );
                                                                                         for ( ConfigurableProperty prop : props ) {
                                                                                           ConfigurableProperty addProp = null;
                                                                                           if ( prop instanceof SingletonDatabasePropertyEntry ) {
                                                                                             addProp = prop;
                                                                                           } else if ( prop instanceof MultiDatabasePropertyEntry ) {
                                                                                             addProp = ( ( MultiDatabasePropertyEntry ) prop ).getClone( config.getName( ) );
                                                                                           }
                                                                                           PropertyDirectory.addProperty( addProp );
                                                                                         }
                                                                                       } catch ( Throwable ex ) {
                                                                                         LOG.error( ex, ex );
                                                                                       }
                                                                                     }
                                                                                   };
  static final Callback<ServiceConfiguration>                removeProperties      = new Callback<ServiceConfiguration>( ) {
                                                                                     @Override
                                                                                     public void fire( ServiceConfiguration config ) {
                                                                                       try {
                                                                                         List<ConfigurableProperty> props = PropertyDirectory.getPropertyEntrySet( config.getComponentId( ).name( ) );
                                                                                         for ( ConfigurableProperty prop : props ) {
                                                                                           if ( prop instanceof SingletonDatabasePropertyEntry ) {
                                                                                             //noop
                                                                                           } else if ( prop instanceof MultiDatabasePropertyEntry ) {
                                                                                             ( ( MultiDatabasePropertyEntry ) prop ).setIdentifierValue( config.getName( ) );
                                                                                           }
                                                                                           PropertyDirectory.removeProperty( prop );
                                                                                         }
                                                                                       } catch ( Throwable ex ) {
                                                                                         LOG.error( ex, ex );
                                                                                       }
                                                                                     }
                                                                                   };

}
