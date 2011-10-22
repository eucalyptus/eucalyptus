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

import java.util.Map;
import java.util.concurrent.Callable;
import org.apache.log4j.Logger;
import com.eucalyptus.bootstrap.Bootstrap;
import com.eucalyptus.component.Component.State;
import com.eucalyptus.component.Topology.ServiceKey;
import com.eucalyptus.records.EventRecord;
import com.eucalyptus.records.EventType;
import com.eucalyptus.records.Logs;
import com.eucalyptus.util.Exceptions;
import com.google.common.base.Function;
import com.google.common.base.Supplier;
import com.google.common.collect.MapMaker;

public class TopologyChanges {
  private static Logger                                                                           LOG                      = Logger.getLogger( TopologyChanges.class );
  private static final Map<Component.State, Function<ServiceConfiguration, ServiceConfiguration>> cloudTransitionCallables = new MapMaker( ).makeComputingMap( TopologyFunctionGenerator.INSTANCE ); //TODO:GRZE: CacheBuilder
                                                                                                                                                                                                     
  static Function<ServiceConfiguration, ServiceConfiguration> get( Component.State state ) {
    return cloudTransitionCallables.get( state );
  }
  
  static Function<ServiceConfiguration, ServiceConfiguration> check( ) {
    return Transitions.CHECK;
  }
  
  public static Callable<ServiceConfiguration> callable( final ServiceConfiguration config, final Function<ServiceConfiguration, ServiceConfiguration> function ) {
    final String functionName = function.getClass( ).toString( ).replaceAll( "^.*\\.", "" );
    final Long queueStart = System.currentTimeMillis( );
    final Callable<ServiceConfiguration> call = new Callable<ServiceConfiguration>( ) {
      
      @Override
      public ServiceConfiguration call( ) throws Exception {
        if ( Bootstrap.isShuttingDown( ) ) {
          return null;
        } else {
          if ( config.isVmLocal( ) ) {
            Bootstrap.awaitFinished( );
          }
          final Long serviceStart = System.currentTimeMillis( );
          LOG.trace( EventRecord.here( Topology.class, EventType.DEQUEUE, functionName, config.getFullName( ).toString( ),
                                       Long.toString( serviceStart - queueStart ), "ms" ) );
          
          try {
            final ServiceConfiguration result = function.apply( config );
            LOG.trace( EventRecord.here( Topology.class, EventType.QUEUE, functionName, config.getFullName( ).toString( ),
                                         Long.toString( System.currentTimeMillis( ) - serviceStart ), "ms" ) );
            return result;
          } catch ( Exception ex ) {
            Throwable t = Exceptions.unwrapCause( ex );
            Logs.extreme( ).error( t, t );
            LOG.error( config.getFullName( ) + " failed to transition because of: "
                       + t );
            throw ex;
          }
        }
      }
    };
    return call;
  }
  
  private enum Transitions implements Function<ServiceConfiguration, ServiceConfiguration> {
    START( Component.State.NOTREADY ),
    STOP( Component.State.STOPPED ),
    INIT( Component.State.INITIALIZED ),
    LOAD( Component.State.LOADED ),
    DESTROY( Component.State.PRIMORDIAL ),
    ENABLE( Component.State.ENABLED ) {
      @Override
      public ServiceConfiguration apply( ServiceConfiguration config ) {
        if ( Topology.guard( ).tryEnable( config ) ) {
          try {
            return super.apply( config );
          } catch ( RuntimeException ex ) {
            Topology.guard( ).tryDisable( config );
            throw ex;
          }
        } else {
          throw new IllegalStateException( "Failed to ENABLE " + config.getFullName( ) );
//          try {
//            return ServiceTransitions.pathTo( config, Component.State.DISABLED ).get( );
//          } catch ( InterruptedException ex ) {
//            Thread.currentThread( ).interrupt( );
//            throw Exceptions.toUndeclared( ex );
//          } catch ( ExecutionException ex ) {
//            throw Exceptions.toUndeclared( ex );
//          }
        }
      }
    },
    DISABLE( Component.State.DISABLED ) {
      @Override
      public ServiceConfiguration apply( ServiceConfiguration config ) {
        ServiceKey serviceKey = null;
        try {
          serviceKey = ServiceKey.create( config );
          return super.apply( config );
        } finally {
          if ( serviceKey != null ) {
            Topology.guard( ).tryDisable( config );
          }
        }
      }
    },
    CHECK {
      @Override
      public ServiceConfiguration apply( ServiceConfiguration config ) {
        if ( !Bootstrap.isFinished( ) ) {
          LOG.debug( this.toString( )
                     + " aborted because bootstrap is not complete for service: "
                     + config );
          return config;
        } else {
          return super.apply( config );
        }
      }
    };
    
    private final Component.State  state;
    protected final TopologyChange tc;
    
    private Transitions( ) {
      this.state = null;
      this.tc = new TopologyChange( this );
    }
    
    private Transitions( State state ) {
      this.state = state;
      this.tc = new TopologyChange( this );
    }
    
    @Override
    public ServiceConfiguration apply( ServiceConfiguration input ) {
      Components.lookup( input.getComponentId( ) ).setup( input );
      return this.tc.apply( input );
    }
    
    @Override
    public String toString( ) {
      return this.getClass( )
                 .toString( )
                 .replaceAll( "^[^\\$]*\\$", "" )
                 .replaceAll( "\\$[^\\$]*$", "" ) + "."
             + this.name( );
    }
    
  }
  
  private static class TopologyChange implements Function<ServiceConfiguration, ServiceConfiguration>, Supplier<Component.State> {
    private final TopologyChanges.Transitions transitionName;
    
    TopologyChange( Transitions transitionName ) {
      this.transitionName = transitionName;
    }
    
    @Override
    public ServiceConfiguration apply( ServiceConfiguration input ) {
      State nextState = null;
      if ( ( nextState = findNextCheckState( input.lookupState( ) ) ) == null ) {
        return input;
      } else {
        return this.doTopologyChange( this, input, nextState );
      }
    }
    
    private ServiceConfiguration doTopologyChange( TopologyChange tc, ServiceConfiguration input, State nextState ) throws RuntimeException {
      State initialState = input.lookupState( );
      ServiceConfiguration endResult = input;
      try {
        endResult = ServiceTransitions.pathTo( input, nextState ).get( );
        LOG.trace( this.toString( endResult, initialState, nextState ) );
        return endResult;
      } catch ( Exception ex ) {
        Exceptions.maybeInterrupted( ex );
        LOG.trace( this.toString( input, initialState, nextState, ex ) );
        throw Exceptions.toUndeclared( ex );
      }
    }
    
    private String toString( ServiceConfiguration endResult, State initialState, State nextState, Throwable... throwables ) {
      return String.format( "%s %s %s->%s=%s [%s]", this.toString( ), endResult.getFullName( ), initialState, nextState, endResult.lookupState( ),
                            ( throwables != null && throwables.length > 0
                              ? Exceptions.causeString( throwables[0] )
                              : "WINNING" ) );
    }
    
    @Override
    public String toString( ) {
      return this.getClass( )
                 .toString( )
                 .replaceAll( "^[^\\$]*\\$", "" )
                 .replaceAll( "\\$[^\\$]*$", "" ) + "."
             + this.transitionName.name( );
    }
    
    @Override
    public State get( ) {
      return this.transitionName.state;
    }
    
    private State findNextCheckState( State initialState ) {
      if ( this.get( ) == null ) {
        if ( State.NOTREADY.equals( initialState ) || State.BROKEN.equals( initialState ) ) {
          return State.DISABLED;
        } else if ( initialState.ordinal( ) < State.NOTREADY.ordinal( ) ) {
          return null;
        } else {
          return initialState;
        }
      } else {
        return this.get( );
      }
    }
    
  }
  
  enum TopologyFunctionGenerator implements Function<Component.State, Function<ServiceConfiguration, ServiceConfiguration>> {
    INSTANCE;
    
    @Override
    public Function<ServiceConfiguration, ServiceConfiguration> apply( State input ) {
      for ( Transitions c : Transitions.values( ) ) {
        if ( input.equals( c.state ) ) {
          return c;
        } else if ( input.name( ).startsWith( c.name( ) ) ) {
          return c;
        }
      }
      return Transitions.CHECK;
    }
    
  }
  
}
