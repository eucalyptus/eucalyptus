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
import java.net.InetAddress;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Future;
import org.apache.log4j.Logger;
import com.eucalyptus.bootstrap.Bootstrap;
import com.eucalyptus.context.ServiceStateException;
import com.eucalyptus.empyrean.Empyrean;
import com.eucalyptus.empyrean.ServiceId;
import com.eucalyptus.system.Threads;
import com.eucalyptus.system.Threads.ThreadPool;
import com.eucalyptus.util.TypeMappers;
import com.eucalyptus.util.async.CheckedListenableFuture;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class Topology {
  private static Logger                                         LOG          = Logger.getLogger( Topology.class );
  private static final Topology                                 singleton    = new Topology( );                                                        //TODO:GRZE:handle differently for remote case?
  private Integer                                               currentEpoch = 0;
  private final TransitionGuard                                 guard;
  private final ConcurrentMap<ServiceKey, ServiceConfiguration> services     = Maps.newConcurrentMap( );
  private static ThreadPool                                     worker       = Threads.lookup( Empyrean.class, Topology.class, "worker" ).limitTo( 1 );
  
  public enum CloudTopologyCallables implements Function<ServiceConfiguration, ServiceConfiguration> {
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
    };
  }
  
  public enum RemoteTopologyCallables implements Function<ServiceConfiguration, ServiceConfiguration> {
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
    };
  }
  
  private Topology( ) {
    super( );
    this.guard = ( Bootstrap.isCloudController( )
      ? cloudControllerGuard( )
      : remoteGuard( ) );
  }
  
  public static Topology getInstance( ) {
    return singleton;
  }
  
  private Integer getEpoch( ) {
    return this.currentEpoch;
  }
  
  public static int epoch( ) {
    return Topology.getInstance( ).getEpoch( );
  }
  
  public static List<ServiceId> partitionRelativeView( final Partition partition, final InetAddress localAddr ) {
    final String localhostAddr = localAddr.getHostAddress( );
    return Lists.transform( Topology.getInstance( ).lookupPartitionView( partition ),
                            TypeMappers.lookup( ServiceConfiguration.class, ServiceId.class ) );
  }
  
  private Future<ServiceConfiguration> submit( final ServiceConfiguration config, final Function<ServiceConfiguration, ServiceConfiguration> function ) {
    return this.worker.submit( new Callable<ServiceConfiguration>( ) {
      
      @Override
      public ServiceConfiguration call( ) throws Exception {
        return function.apply( config );
      }
    } );
  }
  
  public static Future<ServiceConfiguration> enable( final ServiceConfiguration config ) throws ServiceRegistrationException {
    if ( Bootstrap.isCloudController( ) ) {
      return Topology.getInstance( ).submit( config, CloudTopologyCallables.ENABLE );
    } else {
      return Topology.getInstance( ).submit( config, RemoteTopologyCallables.ENABLE );
    }
  }
  
  public static Future<ServiceConfiguration> disable( final ServiceConfiguration config ) throws ServiceRegistrationException {
    if ( Bootstrap.isCloudController( ) ) {
      return Topology.getInstance( ).submit( config, CloudTopologyCallables.DISABLE );
    } else {
      return Topology.getInstance( ).submit( config, RemoteTopologyCallables.DISABLE );
    }
  }
  
  public List<ServiceConfiguration> lookupPartitionView( final Partition partition ) {
    return Lists.newArrayList( Iterables.filter( this.services.values( ), new Predicate<ServiceConfiguration>( ) {
      
      @Override
      public boolean apply( final ServiceConfiguration config ) {
        if ( config.getComponentId( ).isRootService( ) && !config.getComponentId( ).isPartitioned( ) ) {
          return true;
        } else if ( config.getComponentId( ).isRootService( ) && !config.getComponentId( ).isPartitioned( )
                    && partition.getName( ).equals( config.getPartition( ) ) ) {
          return true;
        } else {
          return false;
        }
      }
    } ) );
  }
  
  public static ServiceConfiguration lookup( final Class<? extends ComponentId> compIdClass ) {
    ComponentId compId = ComponentIds.lookup( compIdClass );
    return Topology.lookup( compId, null );
  }
  
  public static ServiceConfiguration lookup( final ComponentId compId ) {
    return Topology.lookup( compId, null );
  }
  
  public static ServiceConfiguration lookup( final ComponentId compId, final String partition ) throws IllegalArgumentException, NoSuchElementException {
    ServiceKey serviceKey = ServiceKey.create( compId, partition );
    return Topology.getInstance( ).lookup( serviceKey );
  }
  
  private ServiceConfiguration lookup( final ServiceKey serviceKey ) {
    return this.services.get( serviceKey );
  }
  
  interface TransitionGuard {
    boolean tryEnable( final ServiceKey serviceKey, final ServiceConfiguration config ) throws ServiceStateException;
    
    boolean nextEpoch( );
    
    boolean tryDisable( final ServiceKey serviceKey, final ServiceConfiguration config ) throws ServiceStateException;
  }
  
  private TransitionGuard cloudControllerGuard( ) {
    return new TransitionGuard( ) {
      
      @Override
      public boolean nextEpoch( ) {
        Topology.this.currentEpoch++;
        return true;
      }
      
      @Override
      public boolean tryEnable( final ServiceKey serviceKey, final ServiceConfiguration config ) throws ServiceStateException {
        ServiceConfiguration curr = Topology.this.services.putIfAbsent( serviceKey, config );
        if ( curr != null ) {
          return false;
        } else {
          Topology.this.currentEpoch++;
          return true;
        }
      }
      
      @Override
      public boolean tryDisable( final ServiceKey serviceKey, final ServiceConfiguration config ) {
        return ( Topology.this.services.remove( serviceKey, config ) || !config.equals( Topology.this.services.get( serviceKey ) ) ) && this.nextEpoch( );
      }
      
    };
  }
  
  private TransitionGuard remoteGuard( ) {
    return new TransitionGuard( ) {
      
      @Override
      public boolean nextEpoch( ) {
        return true;
      }
      
      @Override
      public boolean tryEnable( final ServiceKey serviceKey, final ServiceConfiguration config ) throws ServiceStateException {
        ServiceConfiguration curr = Topology.this.services.put( serviceKey, config );
        if ( curr != null ) {
          return false;
        } else {
          return true;
        }
      }
      
      @Override
      public boolean tryDisable( final ServiceKey serviceKey, final ServiceConfiguration config ) {
        return ( Topology.this.services.remove( serviceKey, config ) || !config.equals( Topology.this.services.get( serviceKey ) ) ) && this.nextEpoch( );
      }
    };
  }
  
  public static class ServiceKey {
    private final Partition   partition;
    private final ComponentId componentId;
    
    static ServiceKey create( final ServiceConfiguration config ) throws ServiceRegistrationException {
      if ( config.getComponentId( ).isPartitioned( ) ) {
        Partition p = Partitions.lookup( config );
        return new ServiceKey( config.getComponentId( ), p );
      } else {
        return new ServiceKey( config.getComponentId( ) );
      }
    }
    
    public static ServiceKey create( final ComponentId compId, final String partition ) throws IllegalArgumentException, NoSuchElementException {
      if ( compId.isPartitioned( ) && partition == null ) {
        throw new IllegalArgumentException( "Cannot lookup a partitioned component when no partition is specified: " + compId );
      } else if ( compId.isPartitioned( ) ) {
        Partition p = Partitions.lookup( partition );
        return new ServiceKey( compId, p );
      } else {
        return new ServiceKey( compId );
      }
    }
    
    ServiceKey( final ComponentId componentId ) {
      this( componentId, null );
    }
    
    ServiceKey( final ComponentId componentId, final Partition partition ) {
      super( );
      this.partition = partition;
      this.componentId = componentId;
    }
    
    public Partition getPartition( ) {
      return this.partition;
    }
    
    @Override
    public String toString( ) {
      StringBuilder builder = new StringBuilder( );
      builder.append( "EnabledService:partition=" ).append( this.partition == null
        ? "null"
        : this.partition ).append( ":componentId=" ).append( this.componentId );
      return builder.toString( );
    }
    
    public ComponentId getComponentId( ) {
      return this.componentId;
    }
    
    @Override
    public int hashCode( ) {
      final int prime = 31;
      int result = 1;
      result = prime * result + ( ( this.componentId == null )
        ? 0
        : this.componentId.hashCode( ) );
      result = prime * result + ( ( this.partition == null )
        ? 0
        : this.partition.hashCode( ) );
      return result;
    }
    
    @Override
    public boolean equals( final Object obj ) {
      if ( this == obj ) {
        return true;
      }
      if ( obj == null ) {
        return false;
      }
      if ( getClass( ) != obj.getClass( ) ) {
        return false;
      }
      ServiceKey other = ( ServiceKey ) obj;
      if ( this.componentId == null ) {
        if ( other.componentId != null ) {
          return false;
        }
      } else if ( !this.componentId.equals( other.componentId ) ) {
        return false;
      }
      if ( this.partition == null ) {
        if ( other.partition != null ) {
          return false;
        }
      } else if ( !this.partition.equals( other.partition ) ) {
        return false;
      }
      return true;
    }
    
  }
  
  public TransitionGuard getGuard( ) {
    return this.guard;
  }
  
}
