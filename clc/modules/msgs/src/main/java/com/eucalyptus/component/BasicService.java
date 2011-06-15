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

import java.net.InetSocketAddress;
import java.security.KeyPair;
import java.security.cert.X509Certificate;
import java.util.concurrent.ExecutionException;
import org.apache.log4j.Logger;
import com.eucalyptus.bootstrap.Bootstrap;
import com.eucalyptus.component.Component.State;
import com.eucalyptus.component.Component.Transition;
import com.eucalyptus.component.auth.SystemCredentialProvider;
import com.eucalyptus.empyrean.Empyrean;
import com.eucalyptus.event.ClockTick;
import com.eucalyptus.event.Event;
import com.eucalyptus.event.Hertz;
import com.eucalyptus.event.ListenerRegistry;
import com.eucalyptus.system.Threads;
import com.eucalyptus.util.FullName;
import com.eucalyptus.util.async.Request;
import com.eucalyptus.util.fsm.ExistingTransitionException;
import com.eucalyptus.util.fsm.StateMachine;

public class BasicService extends AbstractService implements Service {
  private static Logger                                                                   LOG  = Logger.getLogger( BasicService.class );
  private final ServiceConfiguration                                                      serviceConfiguration;
  private final StateMachine<ServiceConfiguration, Component.State, Component.Transition> stateMachine;
  private State                                                                           goal = Component.State.ENABLED;
  
  BasicService( ServiceConfiguration serviceConfiguration ) throws ServiceRegistrationException {
    super( );
    this.serviceConfiguration = serviceConfiguration;
    this.stateMachine = new ServiceState( this.serviceConfiguration );
    ListenerRegistry.getInstance( ).register( ClockTick.class, this );
    ListenerRegistry.getInstance( ).register( Hertz.class, this );
  }
  
  static class Broken extends BasicService {
    
    Broken( ServiceConfiguration serviceConfiguration ) throws ServiceRegistrationException {
      super( serviceConfiguration );
      super.setGoal( Component.State.BROKEN );
      try {
        super.stateMachine.transition( Component.State.BROKEN );
      } catch ( IllegalStateException ex ) {
        LOG.error( ex, ex );
      } catch ( ExistingTransitionException ex ) {
        LOG.error( ex, ex );
      }
    }
    
  }
  
  @Override
  public final String getName( ) {
    return this.serviceConfiguration.getFullName( ).toString( );
  }
  
  @Override
  public Boolean isLocal( ) {
    return this.serviceConfiguration.isVmLocal( );
  }
  
  @Override
  public KeyPair getKeys( ) {
    return SystemCredentialProvider.getCredentialProvider( this.serviceConfiguration.getComponentId( ) ).getKeyPair( );
  }
  
  @Override
  public X509Certificate getCertificate( ) {
    return SystemCredentialProvider.getCredentialProvider( this.serviceConfiguration.getComponentId( ) ).getCertificate( );
  }
  
  /**
   * @return the service configuration
   */
  @Override
  public ServiceConfiguration getServiceConfiguration( ) {
    return this.serviceConfiguration;
  }
  
  @Override
  public Component getComponent( ) {
    return this.serviceConfiguration.lookupComponent( );
  }
  
  @Override
  public ComponentId getComponentId( ) {
    return this.serviceConfiguration.getComponentId( );
  }
  
  @Override
  public String toString( ) {
    return String.format( "Service %s name=%s serviceConfiguration=%s\n",
                          this.getComponentId( ), this.getName( ), this.getServiceConfiguration( ) );
  }
  
  @Override
  public Dispatcher getDispatcher( ) {
    throw new IllegalStateException( this.serviceConfiguration + " does not support the operation: " + Thread.currentThread( ).getStackTrace( )[1] );
  }
  
  @Override
  public void enqueue( Request request ) {
    LOG.error( "Discarding request submitted to a basic service: " + request );
  }
  
  @Override
  public boolean checkTransition( Transition transition ) {
    return this.stateMachine.isLegalTransition( transition );
  }
  
  @Override
  public Component.State getGoal( ) {
    return this.goal;
  }
  
  @Override
  public final void fireEvent( Event event ) {
    if ( event instanceof LifecycleEvent ) {
      super.fireLifecycleEvent( event );
    } else if ( event instanceof Hertz && Bootstrap.isFinished( ) && ( ( Hertz ) event ).isAsserted( 10l ) ) {
      final ServiceConfiguration config = this.getServiceConfiguration( );
      if ( config.lookupComponent( ).hasService( config ) ) {
        if ( Component.State.STOPPED.ordinal( ) < config.lookupState( ).ordinal( ) ) {
          try {
            Threads.lookup( Empyrean.class ).submit( new Runnable( ) {
              @Override
              public void run( ) {
                if ( !Bootstrap.isFinished( ) ) {
                  return;
                } else {
                  try {
                    if ( Component.State.ENABLED.equals( config.lookupService( ).getGoal( ) ) && Component.State.DISABLED.isIn( config ) ) {
                      config.lookupComponent( ).enableTransition( config ).get( );
                    } else if ( Component.State.DISABLED.equals( config.lookupService( ).getGoal( ) ) && Component.State.ENABLED.isIn( config ) ) {
                      config.lookupComponent( ).disableTransition( config ).get( );
                    } else if ( BasicService.this.stateMachine.getState( ).ordinal( ) > State.NOTREADY.ordinal( ) ) {
                      BasicService.this.stateMachine.transition( BasicService.this.stateMachine.getState( ) ).get( );
                    } else if ( State.NOTREADY.isIn( BasicService.this.getServiceConfiguration( ) ) ) {
                      config.lookupComponent( ).disableTransition( config ).get( );
                    }
                  } catch ( InterruptedException ex ) {
                    Thread.currentThread( ).interrupt( );
                  } catch ( Throwable ex ) {
                    LOG.debug( "CheckRunner caught an exception: " + ex );
                    BasicService.this.getServiceConfiguration( ).info( ex );
                  }
                }
              }
            } ).get( );
          } catch ( InterruptedException ex ) {
            Thread.currentThread( ).interrupt( );
          } catch ( ExecutionException ex ) {
            config.error( ex.getCause( ) );
            //          config.lookupService( ).setGoal( Component.State.DISABLED );
          }
        }
      }
    }
  }
  
  @Override
  public InetSocketAddress getSocketAddress( ) {
    return this.serviceConfiguration.getSocketAddress( );
  }
  
  @Override
  public void setGoal( State state ) {
    this.goal = state;
  }
  
  @Override
  public int hashCode( ) {
    final int prime = 31;
    int result = 1;
    result = prime * result + ( ( this.serviceConfiguration == null )
      ? 0
      : this.serviceConfiguration.hashCode( ) );
    return result;
  }
  
  @Override
  public boolean equals( Object obj ) {
    if ( this == obj ) {
      return true;
    }
    if ( obj == null ) {
      return false;
    }
    if ( getClass( ) != obj.getClass( ) ) {
      return false;
    }
    BasicService other = ( BasicService ) obj;
    if ( this.serviceConfiguration == null ) {
      if ( other.serviceConfiguration != null ) {
        return false;
      }
    } else if ( !this.serviceConfiguration.equals( other.serviceConfiguration ) ) {
      return false;
    }
    return true;
  }
  
  public boolean isBusy( ) {
    return this.stateMachine.isBusy( );
  }
  
  public String getPartition( ) {
    return this.serviceConfiguration.getPartition( );
  }
  
  public FullName getFullName( ) {
    return this.serviceConfiguration.getFullName( );
  }
  
  @Override
  public int compareTo( ServiceConfiguration that ) {
    return this.serviceConfiguration.compareTo( that );
  }
  
  @Override
  public StateMachine<ServiceConfiguration, State, Transition> getStateMachine( ) {
    return this.stateMachine;
  }
  
  /**
   * @see com.eucalyptus.component.Service#start()
   */
  @Override
  public void start( ) {}

  /**
   * @see com.eucalyptus.component.Service#stop()
   */
  @Override
  public void stop( ) {}
}
