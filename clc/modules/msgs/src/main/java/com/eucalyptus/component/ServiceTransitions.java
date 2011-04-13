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

import org.apache.log4j.Logger;
import com.eucalyptus.component.Component.State;
import com.eucalyptus.context.ServiceContextManager;
import com.eucalyptus.util.async.Callback;
import com.eucalyptus.util.async.Callback.Completion;
import com.eucalyptus.util.fsm.AbstractTransitionAction;
import com.eucalyptus.util.fsm.TransitionAction;
import com.eucalyptus.ws.util.PipelineRegistry;

public class ServiceTransitions {
  private static Logger                                      LOG                   = Logger.getLogger( ServiceTransitions.class );
  
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
  
  static final Callback<ServiceConfiguration>                stopEndpoint         = new Callback<ServiceConfiguration>( ) {
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
