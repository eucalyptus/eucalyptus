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
import java.util.concurrent.Future;
import org.apache.log4j.Logger;
import com.eucalyptus.bootstrap.Bootstrap;
import com.eucalyptus.component.Component.State;
import com.eucalyptus.component.Topology.ServiceKey;
import com.eucalyptus.util.async.CheckedListenableFuture;
import com.google.common.base.Function;

public class TopologyChanges {
  private static Logger LOG = Logger.getLogger( TopologyChanges.class );
  
  static Function<ServiceConfiguration, ServiceConfiguration> checkFunction( ) {
    if ( Bootstrap.isCloudController( ) ) {
      return CloudTopologyCallables.CHECK;
    } else {
      return RemoteTopologyCallables.CHECK;
    }
  }
  
  static Function<ServiceConfiguration, ServiceConfiguration> disableFunction( ) {
    if ( Bootstrap.isCloudController( ) ) {
      return CloudTopologyCallables.DISABLE;
    } else {
      return RemoteTopologyCallables.DISABLE;
    }
  }
  
  static Function<ServiceConfiguration, ServiceConfiguration> enableFunction( ) {
    if ( Bootstrap.isCloudController( ) ) {
      return CloudTopologyCallables.ENABLE;
    } else {
      return RemoteTopologyCallables.ENABLE;
    }
  }
  
  enum RemoteTopologyCallables implements Function<ServiceConfiguration, ServiceConfiguration> {
    ENABLE {
      @Override
      public ServiceConfiguration apply( ServiceConfiguration config ) {
        try {
          ServiceKey serviceKey = ServiceKey.create( config );
          if ( Topology.getInstance( ).getGuard( ).tryEnable( serviceKey, config ) ) {
            CheckedListenableFuture<ServiceConfiguration> transition = ServiceTransitions.transitionChain( config, Component.State.ENABLED );
            try {
              return transition.get( );
            } catch ( InterruptedException ex ) {
              Thread.currentThread( ).interrupt( );
              throw ex;
            } catch ( Exception ex ) {
              Topology.getInstance( ).getGuard( ).tryDisable( serviceKey, config );
              throw ex;
            }
          } else {
            CheckedListenableFuture<ServiceConfiguration> transition = ServiceTransitions.transitionChain( config, Component.State.DISABLED );
            try {
              return transition.get( );
            } catch ( InterruptedException ex ) {
              Thread.currentThread( ).interrupt( );
              throw ex;
            }
          }
        } catch ( Exception ex ) {
          LOG.error( ex, ex );
          throw new UndeclaredThrowableException( ex );
        }
      }
    },
    DISABLE {
      @Override
      public ServiceConfiguration apply( ServiceConfiguration config ) {
        try {
          ServiceKey serviceKey = ServiceKey.create( config );
          Future<ServiceConfiguration> transition = ServiceTransitions.transitionChain( config, Component.State.DISABLED );
          ServiceConfiguration result = transition.get( );
          Topology.getInstance( ).getGuard( ).tryDisable( serviceKey, config );
          return result;
        } catch ( InterruptedException ex ) {
          Thread.currentThread( ).interrupt( );
          throw new UndeclaredThrowableException( ex );
        } catch ( Exception ex ) {
          LOG.error( ex, ex );
          throw new UndeclaredThrowableException( ex );
        }
      }
    },
    CHECK {
      @Override
      public ServiceConfiguration apply( ServiceConfiguration config ) {
        if ( !Bootstrap.isFinished( ) ) {
          LOG.debug( this.toString( ) + " aborted because bootstrap is not complete" );
          return config;
        } else if ( config.isVmLocal( ) && !config.lookupStateMachine( ).isBusy( ) ) {
          State initialState = config.lookupState( );
          State nextState = config.lookupState( );
          if ( initialState.ordinal( ) < State.NOTREADY.ordinal( ) ) {
            return config;
          } else if ( State.NOTREADY.equals( initialState ) ) {
            nextState = State.DISABLED;
          }
          try {
            Future<ServiceConfiguration> result = config.lookupStateMachine( ).transition( initialState );
            State endState = result.get( ).lookupState( );
            LOG.debug( this.toString( ) + " completed for: " + result + " trying " + initialState + "->" + nextState + " ended in: " + endState );
            return result.get( );
          } catch ( InterruptedException ex ) {
            Thread.currentThread( ).interrupt( );
            return config;
          } catch ( Exception ex ) {
            LOG.error( ex, ex );
//            if( ServiceExceptions.filterExceptions( config, ex ) ) {
            throw new UndeclaredThrowableException( ex );
//            } else {
//              return config;
//            }
          }
        } else {
          return config;
        }
      }
    };
    
    @Override
    public String toString( ) {
      return this.getClass( ).getSimpleName( ) + "." + this.name( );
    }
    
  }
  
  /**
   * ServiceConfiguration empyrean = ServiceConfigurations.createEphemeral(Empyrean.INSTANCE, InetAddress.getByName("192.168.51.116"));
   * DescribeServicesType msg = new DescribeServicesType();
   * msg.listAll = true;
   * return AsyncRequests.sendSync(empyrean,msg)
   */
  
  enum CloudTopologyCallables implements Function<ServiceConfiguration, ServiceConfiguration> {
    ENABLE {
      @Override
      public ServiceConfiguration apply( ServiceConfiguration config ) {
        try {
          ServiceKey serviceKey = ServiceKey.create( config );
          if ( Topology.getInstance( ).getGuard( ).tryEnable( serviceKey, config ) ) {
            CheckedListenableFuture<ServiceConfiguration> transition = ServiceTransitions.transitionChain( config, Component.State.ENABLED );
            try {
              return transition.get( );
            } catch ( InterruptedException ex ) {
              Thread.currentThread( ).interrupt( );
              throw ex;
            } catch ( Exception ex ) {
              Topology.getInstance( ).getGuard( ).tryDisable( serviceKey, config );
              throw ex;
            }
          } else {
            CheckedListenableFuture<ServiceConfiguration> transition = ServiceTransitions.transitionChain( config, Component.State.DISABLED );
            try {
              return transition.get( );
            } catch ( InterruptedException ex ) {
              Thread.currentThread( ).interrupt( );
              throw ex;
            }
          }
        } catch ( Exception ex ) {
          LOG.error( ex, ex );
          throw new UndeclaredThrowableException( ex );
        }
      }
    },
    DISABLE {
      @Override
      public ServiceConfiguration apply( ServiceConfiguration config ) {
        try {
          ServiceKey serviceKey = ServiceKey.create( config );
          Future<ServiceConfiguration> transition = ServiceTransitions.transitionChain( config, Component.State.DISABLED );
          ServiceConfiguration result = transition.get( );
          Topology.getInstance( ).getGuard( ).tryDisable( serviceKey, config );
          return result;
        } catch ( InterruptedException ex ) {
          Thread.currentThread( ).interrupt( );
          throw new UndeclaredThrowableException( ex );
        } catch ( Exception ex ) {
          LOG.error( ex, ex );
          throw new UndeclaredThrowableException( ex );
        }
      }
    },
    CHECK {
      @Override
      public ServiceConfiguration apply( ServiceConfiguration config ) {
        if ( !Bootstrap.isFinished( ) ) {
          return config;
        } else if ( !config.lookupStateMachine( ).isBusy( ) ) {
          State initialState = config.lookupState( );
          State nextState = config.lookupState( );
          if ( initialState.ordinal( ) < State.NOTREADY.ordinal( ) ) {
            return config;
          } else if ( State.NOTREADY.equals( initialState ) ) {
            nextState = State.DISABLED;
          }
          try {
            Future<ServiceConfiguration> result = config.lookupStateMachine( ).transition( nextState );
            State endState = result.get( ).lookupState( );
            LOG.debug( this.toString( ) + " completed for: " + result + " trying " + initialState + "->" + nextState + " ended in: " + endState );
            return result.get( );
          } catch ( InterruptedException ex ) {
            Thread.currentThread( ).interrupt( );
            return config;
          } catch ( Exception ex ) {
            LOG.error( ex, ex );
            throw new UndeclaredThrowableException( ex );
          }
        } else {
          return config;
        }
      }
    };
    
    @Override
    public String toString( ) {
      return this.getClass( ).getSimpleName( ) + "." + this.name( );
    }
    
  }
  
}
