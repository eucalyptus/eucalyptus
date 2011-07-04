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

import java.net.InetAddress;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.apache.log4j.Logger;
import com.eucalyptus.bootstrap.BootstrapArgs;
import com.eucalyptus.component.TopologyChanges.CloudTopologyCallables;
import com.eucalyptus.component.TopologyChanges.RemoteTopologyCallables;
import com.eucalyptus.empyrean.Empyrean;
import com.eucalyptus.empyrean.ServiceId;
import com.eucalyptus.event.Event;
import com.eucalyptus.event.EventListener;
import com.eucalyptus.event.Hertz;
import com.eucalyptus.event.ListenerRegistry;
import com.eucalyptus.records.EventRecord;
import com.eucalyptus.records.EventType;
import com.eucalyptus.system.Threads;
import com.eucalyptus.system.Threads.ThreadPool;
import com.eucalyptus.util.Logs;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class Topology implements EventListener<Event> {
  private static Logger                                         LOG          = Logger.getLogger( Topology.class );
  private static final Topology                                 singleton    = new Topology( );                                                        //TODO:GRZE:handle differently for remote case?
  private Integer                                               currentEpoch = 0;//TODO:GRZE: get the right initial epoch value from membership bootstrap
  private TransitionGuard                                       guard;
  private final ConcurrentMap<ServiceKey, ServiceConfiguration> services     = new ConcurrentSkipListMap<Topology.ServiceKey, ServiceConfiguration>( );
  
  private Topology( ) {
    super( );
    ListenerRegistry.getInstance( ).register( Hertz.class, this );
  }
  
  public static Topology getInstance( ) {
    return singleton;
  }
  
  private ThreadPool getWorker( ) {
    return Threads.lookup( Empyrean.class, Topology.class, "worker" ).limitTo( 1 );
  }
  
  private Integer getEpoch( ) {
    return this.currentEpoch;
  }
  
  public static int epoch( ) {
    return Topology.getInstance( ).getEpoch( );
  }
  
  public static List<ServiceId> partitionRelativeView( final Partition partition, final InetAddress localAddr ) {
    final String localhostAddr = localAddr.getHostAddress( );
    Function<ServiceConfiguration, ServiceId> toServiceId = new Function<ServiceConfiguration, ServiceId>( ) {
      
      @Override
      public ServiceId apply( final ServiceConfiguration input ) {
        return new ServiceId( ) {
          {
            setPartition( input.getPartition( ) );
            setName( input.getName( ) );
            setType( input.getComponentId( ).name( ) );
            if ( input.isVmLocal( ) ) {
              getUris( ).add( input.getComponentId( ).makeExternalRemoteUri( localhostAddr, input.getComponentId( ).getPort( ) ).toASCIIString( ) );
            } else {
              getUris( ).add( input.getUri( ).toASCIIString( ) );
            }
          }
        };
      }
    };
    return Lists.transform( Topology.getInstance( ).lookupPartitionView( partition ), toServiceId );
  }
  
  private <T> Future<T> submit( final Callable<T> callable ) {
    Logs.exhaust( ).debug( EventRecord.here( Topology.class, EventType.ENQUEUE, Topology.this.toString( ), callable.toString( ) ) );
    final Long queueStart = System.currentTimeMillis( );

    return this.getWorker( ).submit( new Callable<T>( ) {
      
      @Override
      public T call( ) throws Exception {
        Long serviceStart = System.currentTimeMillis( );
        Logs.exhaust( ).debug( EventRecord.here( Topology.class, EventType.DEQUEUE, Topology.this.toString( ), callable.toString( ) )
                                          .append( EventType.QUEUE_TIME.name( ), Long.toString( serviceStart - queueStart ) ) );

        try {
          T result = callable.call( );

          Long finish = System.currentTimeMillis( );
          Logs.exhaust( ).debug( EventRecord.here( Topology.class, EventType.QUEUE, Topology.this.toString( ), callable.toString( ) )
                                            .append( EventType.SERVICE_TIME.name( ), Long.toString( finish - serviceStart ) ) );
          return result;
        } catch ( Exception ex ) {
          LOG.error( ex , ex );
          throw ex;
        }
      }
    } );
  }
  
  private Future<ServiceConfiguration> submitExternal( final ServiceConfiguration config, final Function<ServiceConfiguration, ServiceConfiguration> function ) {
    Logs.exhaust( ).debug( EventRecord.here( Topology.class, EventType.ENQUEUE, Topology.this.toString( ), function.toString( ), config.toString( ) ) );
    final Long queueStart = System.currentTimeMillis( );
    return Threads.lookup( Empyrean.class, Topology.class, "submitExternal" ).submit( new Callable<ServiceConfiguration>( ) {
      
      @Override
      public ServiceConfiguration call( ) throws Exception {
        Long serviceStart = System.currentTimeMillis( );
        Logs.exhaust( ).debug( EventRecord.here( Topology.class, EventType.DEQUEUE, Topology.this.toString( ), function.toString( ), config.toString( ) )
                                          .append( EventType.QUEUE_TIME.name( ), Long.toString( serviceStart - queueStart ) ) );
        try {
          ServiceConfiguration result = function.apply( config );

          Long finish = System.currentTimeMillis( );
          Logs.exhaust( ).debug( EventRecord.here( Topology.class, EventType.QUEUE, Topology.this.toString( ), function.toString( ), config.toString( ) )
                                            .append( EventType.SERVICE_TIME.name( ), Long.toString( finish - serviceStart ) ) );
          return result;
        } catch ( Exception ex ) {
          LOG.error( ex , ex );
          throw ex;
        }
      }
    } );
  }
  
  private Future<ServiceConfiguration> submit( final ServiceConfiguration config, final Function<ServiceConfiguration, ServiceConfiguration> function ) {
    EventRecord.here( Topology.class, EventType.ENQUEUE, Topology.this.toString( ), function.toString( ), config.toString( ) ).info( );
    final Long queueStart = System.currentTimeMillis( );
    
    return this.getWorker( ).submit( new Callable<ServiceConfiguration>( ) {
      
      @Override
      public ServiceConfiguration call( ) throws Exception {
        Long serviceStart = System.currentTimeMillis( );
        EventRecord.here( Topology.class, EventType.DEQUEUE, Topology.this.toString( ), function.toString( ), config.toString( ) )
                   .append( EventType.QUEUE_TIME.name( ), Long.toString( serviceStart - queueStart ) )
                   .info( );
        
        try {
          ServiceConfiguration result = function.apply( config );
          
          Long finish = System.currentTimeMillis( );
          EventRecord.here( Topology.class, EventType.QUEUE, Topology.this.toString( ), function.toString( ), config.toString( ) )
                     .append( EventType.SERVICE_TIME.name( ), Long.toString( finish - serviceStart ) )
                     .info( );

          return result;
        } catch ( Exception ex ) {
          LOG.error( ex , ex );
          throw ex;
        }
      }
    } );
  }
  
  public static Future<ServiceConfiguration> stop( final ServiceConfiguration config ) throws ServiceRegistrationException {
    if ( BootstrapArgs.isCloudController( ) ) {
      return Topology.getInstance( ).submitExternal( config, CloudTopologyCallables.STOP );
    } else {
      return Topology.getInstance( ).submitExternal( config, RemoteTopologyCallables.STOP );
    }
  }
  
  public static Future<ServiceConfiguration> start( final ServiceConfiguration config ) throws ServiceRegistrationException {
    if ( BootstrapArgs.isCloudController( ) ) {
      return Topology.getInstance( ).submitExternal( config, CloudTopologyCallables.START );
    } else {
      return Topology.getInstance( ).submitExternal( config, RemoteTopologyCallables.START );
    }
  }
  
  public static Future<ServiceConfiguration> enable( final ServiceConfiguration config ) throws ServiceRegistrationException {
    if ( BootstrapArgs.isCloudController( ) ) {
      return Topology.getInstance( ).submit( config, CloudTopologyCallables.ENABLE );
    } else {
      return Topology.getInstance( ).submit( config, RemoteTopologyCallables.ENABLE );
    }
  }
  
  public static Future<ServiceConfiguration> disable( final ServiceConfiguration config ) throws ServiceRegistrationException {
    return Topology.getInstance( ).submit( config, TopologyChanges.disableFunction( ) );
  }
  
  public List<ServiceConfiguration> lookupPartitionView( final Partition partition ) {
    return Lists.newArrayList( Iterables.filter( this.services.values( ), new Predicate<ServiceConfiguration>( ) {
      
      @Override
      public boolean apply( final ServiceConfiguration config ) {
        if ( config.getComponentId( ).isUserService( ) ) {
          return true;
        } else if ( config.getComponentId( ).isRootService( ) && !config.getComponentId( ).isPartitioned( ) ) {
          return true;
        } else if ( config.getComponentId( ).isRootService( ) && config.getComponentId( ).isPartitioned( )
                    && partition.getName( ).equals( config.getPartition( ) ) ) {
          return true;
        } else {
          return false;
        }
      }
    } ) );
  }
  
  private ServiceConfiguration lookup( final ServiceKey serviceKey ) {
    return this.services.get( serviceKey );
  }
  
  interface TransitionGuard {
    boolean tryEnable( final ServiceConfiguration config ) throws ServiceRegistrationException;
    
    boolean nextEpoch( );
    
    boolean tryDisable( final ServiceConfiguration config ) throws ServiceRegistrationException;
  }
  
  private TransitionGuard cloudControllerGuard( ) {
    return new TransitionGuard( ) {
      
      @Override
      public boolean nextEpoch( ) {
        Topology.this.currentEpoch++;
        return true;
      }
      
      @Override
      public boolean tryEnable( final ServiceConfiguration config ) throws ServiceRegistrationException {
        final ServiceKey serviceKey = ServiceKey.create( config );
        ServiceConfiguration curr = Topology.this.services.putIfAbsent( serviceKey, config );
        if ( curr != null && !curr.equals( config ) ) {
          return false;
        } else if ( curr != null && curr.equals( config ) ) {
          return true;
        } else {
          Topology.this.currentEpoch++;
          return true;
        }
      }
      
      @Override
      public boolean tryDisable( final ServiceConfiguration config ) throws ServiceRegistrationException {
        final ServiceKey serviceKey = ServiceKey.create( config );
        return !config.equals( Topology.this.services.get( serviceKey ) ) || ( Topology.this.services.remove( serviceKey, config ) && this.nextEpoch( ) );
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
      public boolean tryEnable( final ServiceConfiguration config ) throws ServiceRegistrationException {
        final ServiceKey serviceKey = ServiceKey.create( config );
        ServiceConfiguration curr = Topology.this.services.put( serviceKey, config );
        if ( curr != null && !curr.equals( config ) ) {
          return false;
        } else if ( curr != null && curr.equals( config ) ) {
          return true;
        } else {
          return true;
        }
      }
      
      @Override
      public boolean tryDisable( final ServiceConfiguration config ) throws ServiceRegistrationException {
        final ServiceKey serviceKey = ServiceKey.create( config );
        return ( Topology.this.services.remove( serviceKey, config ) || !config.equals( Topology.this.services.get( serviceKey ) ) ) && this.nextEpoch( );
      }
    };
  }
  
  public static class ServiceKey implements Comparable<ServiceKey> {
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
      builder.append( "ServiceKey " ).append( this.componentId.name( ) ).append( ":" );
      if ( this.partition == null ) {
        builder.append( "cloud-global-service" );
      } else {
        builder.append( "partition=" ).append( this.partition );
      }
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

    @Override
    public int compareTo( ServiceKey that ) {
      if( this.componentId.equals( that.componentId ) ) {
        if( this.partition == null && that.partition == null) {
          return 0;
        } else if( this.partition != null ) {
          return this.partition.compareTo( that.partition );
        } else {
          return -1;
        }
      } else {
        return this.componentId.compareTo( that.componentId );
      }
    }

    
  }
  
  public TransitionGuard getGuard( ) {
    return ( BootstrapArgs.isCloudController( )
      ? cloudControllerGuard( )
      : remoteGuard( ) );
  }
  
  @Override
  public String toString( ) {
    StringBuilder builder = new StringBuilder( );
    builder.append( "Topology:currentEpoch=" ).append( this.currentEpoch ).append( ":guard=" ).append( BootstrapArgs.isCloudController( )
      ? "cloud"
      : "remote" );
    return builder.toString( );
  }
  
  /**
   * @see com.eucalyptus.event.EventListener#fireEvent(com.eucalyptus.event.Event)
   */
  @Override
  public void fireEvent( Event event ) {
    if ( event instanceof Hertz && ( ( Hertz ) event ).isAsserted( 15l ) ) {
      this.runChecks( );
    }
  }
  
  private void runChecks( ) {
    this.getWorker( ).submit( new Runnable( ) {
      
      @Override
      public void run( ) {
        List<ServiceConfiguration> checkServicesList = ServiceConfigurations.collect( new Predicate<ServiceConfiguration>( ) {
          
          @Override
          public boolean apply( ServiceConfiguration arg0 ) {
            if ( BootstrapArgs.isCloudController( ) ) {
              return true;
            } else {
              return arg0.isVmLocal( );
            }
          }
        } );
        Logs.exhaust( ).debug( "PARTITIONS ==============================\n" + Joiner.on( "\n\t" ).join( Topology.this.services.keySet( ) ) );
        Logs.exhaust( ).debug( "PRIMARY =================================\n" + Joiner.on( "\n\t" ).join( Topology.this.services.values( ) ) );
        Predicate<Future<?>> futureIsDone = new Predicate<Future<?>>( ) {
          
          @Override
          public boolean apply( Future<?> arg0 ) {
            if( !arg0.isDone( ) ) {
              try {
                arg0.get( 100, TimeUnit.MILLISECONDS );
              } catch ( InterruptedException ex ) {
                Thread.currentThread( ).interrupt( );
              } catch ( ExecutionException ex ) {
                LOG.error( ex );
              } catch ( TimeoutException ex ) {
              }
            }
            return arg0.isDone( );
          }
        };
        Map<ServiceConfiguration, Future<ServiceConfiguration>> futures = Maps.newHashMap( );
        for ( ServiceConfiguration config : checkServicesList ) {
          futures.put( config, Topology.getInstance( ).submitExternal( config, TopologyChanges.checkFunction( ) ) );
        }
        for ( int i = 0; i < 100 && !Iterables.all( futures.values( ), futureIsDone ); i++ );
        final List<ServiceConfiguration> disabledServices = Lists.newArrayList( );
        final List<ServiceConfiguration> checkedServices = Lists.newArrayList( );
        for ( Map.Entry<ServiceConfiguration, Future<ServiceConfiguration>> result : futures.entrySet( ) ) {
          try {
            ServiceConfiguration resultConfig = result.getValue( ).get( );
            checkedServices.add( resultConfig );
          } catch ( InterruptedException ex ) {
            LOG.error( ex, ex );
            Thread.currentThread( ).interrupt( );
          } catch ( Throwable ex ) {
            LOG.debug( "Error while inspecting result of CHECK for: \n\t" + result.getKey( ) + ": \n\t" + ex.getMessage( ) );
            try {
              disabledServices.add( result.getKey( ) );
              Topology.this.getGuard( ).tryDisable( result.getKey( ) );
            } catch ( ServiceRegistrationException ex1 ) {
              LOG.error( ex1, ex1 );
            }
            LOG.error( ex, ex );
          }
        }
        Logs.exhaust( ).debug( "CHECK ===================================\n" + Joiner.on( "\n\t" ).join( checkedServices ) );
        Logs.exhaust( ).debug( "DISABLED ================================\n" + Joiner.on( "\n\t" ).join( disabledServices ) );
        if ( BootstrapArgs.isCloudController( ) ) {
          final Predicate<ServiceConfiguration> predicate = new Predicate<ServiceConfiguration>( ) {
            
            @Override
            public boolean apply( ServiceConfiguration arg0 ) {
              try {
                ServiceKey key = ServiceKey.create( arg0 );
                if ( !BootstrapArgs.isCloudController( ) ) {
                  Logs.exhaust( ).debug( "FAILOVER-REJECT: " + arg0 + ": not cloud controller." );
                  return false;
                } else if ( disabledServices.contains( arg0 ) ) {
                  Logs.exhaust( ).debug( "FAILOVER-REJECT: " + arg0 + ": service was just DISABLED." );
                  return false;
                } else if ( Component.State.NOTREADY.isIn( arg0 ) ) {
                  Logs.exhaust( ).debug( "FAILOVER-REJECT: " + arg0 + ": service is NOTREADY." );
                  return false;
                } else if ( Topology.this.services.containsKey( key ) && arg0.equals( Topology.this.services.get( key ) ) ) {
                  Logs.exhaust( ).debug( "FAILOVER-REJECT: " + arg0 + ": service is ENABLED." );
                  return false;
                } else if ( !Topology.this.services.containsKey( key ) ) {
                  Logs.exhaust( ).debug( "FAILOVER-ACCEPT: " + arg0 + ": service for partition: " + key );
                  return true;
                } else {
                  Logs.exhaust( ).debug( "FAILOVER-ACCEPT: " + arg0 );
                  return true;
                }
              } catch ( ServiceRegistrationException ex ) {
                LOG.error( ex, ex );
                return false;
              }
            }
          };
          List<ServiceConfiguration> failoverServicesList = ServiceConfigurations.collect( predicate );
          Logs.exhaust( ).debug( "FAILOVER ================================\n" + Joiner.on( "\n\t" ).join( failoverServicesList ) );
          for ( ServiceConfiguration config : failoverServicesList ) {
            try {
              Topology.getInstance( ).submitExternal( config, CloudTopologyCallables.ENABLE ).get( );
            } catch ( InterruptedException ex ) {
              LOG.error( ex, ex );
              Thread.currentThread( ).interrupt( );
            } catch ( ExecutionException ex ) {
              LOG.error( ex, ex );
            } catch ( Exception ex ) {
              LOG.error( ex, ex );
            }
          }
        }
      }
    } );
  }
  
}
