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

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.log4j.Logger;
import com.eucalyptus.bootstrap.Bootstrap;
import com.eucalyptus.bootstrap.BootstrapArgs;
import com.eucalyptus.bootstrap.Databases;
import com.eucalyptus.bootstrap.Host;
import com.eucalyptus.bootstrap.Hosts;
import com.eucalyptus.component.Component.State;
import com.eucalyptus.empyrean.DestroyServiceType;
import com.eucalyptus.empyrean.Empyrean;
import com.eucalyptus.empyrean.ServiceId;
import com.eucalyptus.empyrean.ServiceTransitionType;
import com.eucalyptus.event.ClockTick;
import com.eucalyptus.event.EventListener;
import com.eucalyptus.event.Listeners;
import com.eucalyptus.records.EventRecord;
import com.eucalyptus.records.EventType;
import com.eucalyptus.records.Logs;
import com.eucalyptus.system.Threads;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.Internets;
import com.eucalyptus.util.TypeMappers;
import com.eucalyptus.util.async.AsyncRequests;
import com.eucalyptus.util.async.Futures;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.base.Supplier;
import com.google.common.base.Throwables;
import com.google.common.collect.Collections2;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.MapMaker;
import com.google.common.primitives.Ints;
import edu.ucsb.eucalyptus.msgs.BaseMessage;

public class Topology {
  private static Logger                                         LOG          = Logger.getLogger( Topology.class );
  private static Topology                                       singleton    = null;                                                                   //TODO:GRZE:handle differently for remote case?
  private Integer                                               currentEpoch = 0;                                                                      //TODO:GRZE: get the right initial epoch value from membership bootstrap
  private final ConcurrentMap<ServiceKey, ServiceConfiguration> services     = new ConcurrentSkipListMap<Topology.ServiceKey, ServiceConfiguration>( );
  
  private enum Queue implements Function<Callable, Future> {
    INTERNAL( 1 ) {
      ServiceConfiguration internal;
      
      @Override
      ServiceConfiguration queue( ) {
        if ( this.internal == null ) {
          this.internal = ServiceConfigurations.createEphemeral( Empyrean.INSTANCE, Topology.class.getSimpleName( ), "internal",
                                                                 ServiceUris.internal( Empyrean.INSTANCE ) );
        }
        return this.internal;
      }
    },
    EXTERNAL( 32 ) {
      ServiceConfiguration external;
      
      @Override
      ServiceConfiguration queue( ) {
        if ( this.external == null ) {
          this.external = ServiceConfigurations.createEphemeral( Empyrean.INSTANCE, Topology.class.getSimpleName( ), "external",
                                                                 ServiceUris.internal( Empyrean.INSTANCE ) );
        }
        return this.external;
      }
    };
    private final int numWorkers;
    
    private Queue( final int numWorkers ) {
      this.numWorkers = numWorkers;
    }
    
    abstract ServiceConfiguration queue( );
    
    @Override
    public Future apply( final Callable call ) {
      Logs.extreme( ).debug( Topology.class.getSimpleName( ) + ": queueing " + call.toString( ) );
      Logs.extreme( ).debug( Threads.currentStackRange( 3, 9 ) );
      return Threads.enqueue( this.queue( ), this.numWorkers, call );
    }
    
    @SuppressWarnings( "unchecked" )
    public <C> Future<C> enqueue( final Callable<C> call ) {
      return this.apply( call );
    }
    
  };
  
  private enum TopologyTimer implements EventListener<ClockTick> {
    INSTANCE;
    private static final AtomicInteger counter = new AtomicInteger( 0 );
    private static final AtomicBoolean busy  = new AtomicBoolean( false );
    
    @Override
    public void fireEvent( final ClockTick event ) {
      Callable<Object> call = new Callable<Object>( ) {
        public Object call( ) {
          try {
            TimeUnit.SECONDS.sleep( 10 );
            return RunChecks.INSTANCE.call( );
          } catch ( InterruptedException ex ) {
            return Exceptions.maybeInterrupted( ex );
          } finally {
            busy.set( false );
          }
        }
      };
      if ( Hosts.isCoordinator( ) && busy.compareAndSet( false, true ) ) {
        try {
          Queue.INTERNAL.enqueue( call );
        } catch ( Exception ex ) {
          busy.set( false );
        }
      } else if ( counter.incrementAndGet( ) % 3 == 0 && busy.compareAndSet( false, true ) ) {
        try {
          Queue.INTERNAL.enqueue( call );
        } catch ( Exception ex ) {
          busy.set( false );
        }
      }
    }
    
  }
  
  private Topology( final int i ) {
    super( );
    this.currentEpoch = i;
    Listeners.register( ClockTick.class, TopologyTimer.INSTANCE );
  }
  
  private static Predicate<ServiceConfiguration> componentFilter( final Class<? extends ComponentId> c ) {
    return new Predicate<ServiceConfiguration>( ) {
      
      @Override
      public boolean apply( final ServiceConfiguration input ) {
        return input.getComponentId( ).getClass( ).equals( c );
      }
    };
  }
  
  public static void populateServices( final ServiceConfiguration config, BaseMessage msg ) {
    try {
      Predicate<ServiceConfiguration> filter = new Predicate<ServiceConfiguration>( ) {
        
        @Override
        public boolean apply( final ServiceConfiguration filterConfig ) {
          ComponentId filteredComponent = filterConfig.getComponentId( );
          ComponentId destComponent = config.getComponentId( );
          if ( filteredComponent.isDistributedService( ) ) {
            if ( destComponent.isAlwaysLocal( ) ) {
              return filterConfig.lookupState( ).ordinal( ) >= Component.State.NOTREADY.ordinal( );
            } else if ( destComponent.isPartitioned( ) && filteredComponent.isPartitioned( ) ) {
              return config.getPartition( ).equals( filterConfig.getPartition( ) );
            } else {
              return true;
            }
          } else {
            return false;
          }
        }
      };
      Function<ServiceConfiguration, ServiceId> typeMapper = TypeMappers.lookup( ServiceConfiguration.class, ServiceId.class );
      if ( Hosts.isCoordinator( ) ) {
        msg.set_epoch( Topology.epoch( ) );
        for ( ServiceConfiguration s : Topology.getInstance( ).getServices( ).values( ) ) {
          if ( !filter.apply( s ) ) {
            continue;
          } else {
            msg.get_services( ).add( typeMapper.apply( s ) );
          }
        }
        for ( Component c : Components.list( ) ) {
          for ( ServiceConfiguration s : c.services( ) ) {
            if ( !filter.apply( s ) ) {
              continue;
            } else {
              if ( !msg.get_services( ).contains( s ) && State.DISABLED.apply( s ) ) {
                msg.get_disabledServices( ).add( typeMapper.apply( s ) );
              } else if ( !msg.get_services( ).contains( s ) && State.NOTREADY.ordinal( ) >= s.getStateMachine( ).getState( ).ordinal( ) ) {
                msg.get_notreadyServices( ).add( typeMapper.apply( s ) );
              }
            }
          }
        }
      }
    } catch ( Exception ex ) {
      Logs.extreme( ).error( ex, ex );
    }
  }
  
  public static void touch( final ServiceTransitionType msg ) {//TODO:GRZE: @Service interceptor
    if ( !Hosts.isCoordinator( ) && ( msg.get_epoch( ) != null ) ) {
      for ( ServiceConfiguration conf : Lists.transform( msg.get_services( ), ServiceConfigurations.ServiceIdToServiceConfiguration.INSTANCE ) ) {
        if ( !conf.isVmLocal( ) ) {
          enable( conf );
        }
      }
      for ( ServiceConfiguration conf : Lists.transform( msg.get_disabledServices( ), ServiceConfigurations.ServiceIdToServiceConfiguration.INSTANCE ) ) {
        if ( !conf.isVmLocal( ) ) {
          try {
            disable( conf ).get( );
          } catch ( InterruptedException ex ) {
            Exceptions.maybeInterrupted( ex );
          } catch ( ExecutionException ex ) {
            Logs.extreme( ).error( ex, ex );
          }
        }
      }
      for ( ServiceConfiguration conf : Lists.transform( msg.get_notreadyServices( ), ServiceConfigurations.ServiceIdToServiceConfiguration.INSTANCE ) ) {
        if ( !conf.isVmLocal( ) ) {
          try {
            disable( conf ).get( );
            transition( State.NOTREADY ).apply( conf ).get( );
          } catch ( InterruptedException ex ) {
            Exceptions.maybeInterrupted( ex );
          } catch ( ExecutionException ex ) {
            Logs.extreme( ).error( ex, ex );
          }
        }
      }
      Topology.getInstance( ).currentEpoch = Ints.max( Topology.getInstance( ).currentEpoch, msg.get_epoch( ) );
    }
  }
  
  public static boolean check( final BaseMessage msg ) {
    if ( !Hosts.isCoordinator( ) && ( msg.get_epoch( ) != null ) ) {
      return Topology.getInstance( ).epoch( ) <= msg.get_epoch( );
    } else {
      return true;
    }
  }
  
  private static Topology getInstance( ) {
    if ( singleton != null ) {
      return singleton;
    } else {
      synchronized ( Topology.class ) {
        if ( singleton != null ) {
          return singleton;
        } else {
          return ( singleton = new Topology( Hosts.maxEpoch( ) ) );
        }
      }
    }
  }
  
  private Integer getEpoch( ) {
    return this.currentEpoch;
  }
  
  public static int epoch( ) {
    return Topology.getInstance( ).getEpoch( );
  }
  
  public static Function<ServiceConfiguration, Future<ServiceConfiguration>> transition( final Component.State toState ) {
    final Function<ServiceConfiguration, Future<ServiceConfiguration>> transition = new Function<ServiceConfiguration, Future<ServiceConfiguration>>( ) {
      private final List<Component.State> serializedStates = Lists.newArrayList( Component.State.ENABLED, Component.State.STOPPED );
      
      @Override
      public Future<ServiceConfiguration> apply( final ServiceConfiguration input ) {
        final Callable<ServiceConfiguration> call = Topology.callable( input, Topology.get( toState ) );
        if ( Bootstrap.isOperational( ) ) {
          final Queue workQueue = ( this.serializedStates.contains( toState )
                                                                             ? Queue.INTERNAL
                                                                             : Queue.EXTERNAL );
          return workQueue.enqueue( call );
        } else {
          try {
            return Futures.predestinedFuture( input );
          } catch ( Exception ex ) {
            return Futures.predestinedFuture( input );
          }
        }
      }
    };
    return transition;
  }
  
  private static Future<ServiceConfiguration> check( final ServiceConfiguration config ) {
    return Queue.EXTERNAL.enqueue( Topology.callable( config, Topology.check( ) ) );
  }
  
  public static Future<ServiceConfiguration> stop( final ServiceConfiguration config ) {
    return transition( State.STOPPED ).apply( config );
  }
  
  public static Future<ServiceConfiguration> destroy( final ServiceConfiguration config ) {
    try {
      ServiceConfigurations.remove( config );
    } catch ( Exception ex ) {
      LOG.error( ex );
      Logs.extreme( ).debug( ex, ex );
    }
    try {
      Topology.disable( config ).get( );
    } catch ( Exception ex ) {
      Exceptions.maybeInterrupted( ex );
      LOG.error( ex );
      Logs.extreme( ).debug( ex, ex );
    }
    try {
      Topology.stop( config ).get( );
    } catch ( Exception ex ) {
      Exceptions.maybeInterrupted( ex );
      LOG.error( ex );
      Logs.extreme( ).debug( ex, ex );
    }
    try {
      Component comp = Components.lookup( config.getComponentId( ) );
      comp.destroy( config );
    } catch ( Exception ex ) {
      Exceptions.maybeInterrupted( ex );
      LOG.error( ex );
      Logs.extreme( ).debug( ex, ex );
    }
    if ( Hosts.isCoordinator( ) ) {
      DestroyServiceType msg = new DestroyServiceType( );
      try {
        msg.getServices( ).add( TypeMappers.transform( config, ServiceId.class ) );
        for ( Host h : Hosts.list( ) ) {
          if ( !h.isLocalHost( ) && h.hasBootstrapped( ) ) { 
            try {
              AsyncRequests.sendSync( ServiceConfigurations.createEphemeral( Empyrean.INSTANCE, h.getBindAddress( ) ), msg );
            } catch ( Exception ex ) {
              Exceptions.maybeInterrupted( ex );
              LOG.error( ex );
              Logs.extreme( ).debug( ex, ex );
            }
          }
        }
      } catch ( Exception ex ) {
        LOG.error( ex );
        Logs.extreme( ).debug( ex, ex );
      }
    }
    try {
      ServiceTransitions.StateCallbacks.PROPERTIES_REMOVE.fire( config );
    } catch ( Exception ex ) {
      Exceptions.maybeInterrupted( ex );
      LOG.error( ex );
      Logs.extreme( ).debug( ex, ex );
    }
    return Futures.predestinedFuture( config );
  }
  
  public static Future<ServiceConfiguration> load( final ServiceConfiguration config ) {
    return transition( State.LOADED ).apply( config );
  }
  
  public static Future<ServiceConfiguration> start( final ServiceConfiguration config ) {
    return transition( State.DISABLED ).apply( config );
  }
  
  public static Future<ServiceConfiguration> enable( final ServiceConfiguration config ) {
    return transition( State.ENABLED ).apply( config );
  }
  
  public static Future<ServiceConfiguration> disable( final ServiceConfiguration config ) {
    return transition( State.DISABLED ).apply( config );
  }
  
  private ServiceConfiguration lookup( final ServiceKey serviceKey ) {
    return this.getServices( ).get( serviceKey );
  }
  
  private interface TransitionGuard {
    boolean tryEnable( final ServiceConfiguration config );
    
    boolean nextEpoch( );
    
    boolean tryDisable( final ServiceConfiguration config );
  }
  
  private TransitionGuard cloudControllerGuard( ) {
    return new TransitionGuard( ) {
      
      @Override
      public boolean nextEpoch( ) {
        Topology.this.currentEpoch++;
        return true;
      }
      
      @Override
      public boolean tryEnable( final ServiceConfiguration config ) {
        final ServiceKey serviceKey = ServiceKey.create( config );
        final ServiceConfiguration curr = Topology.this.getServices( ).putIfAbsent( serviceKey, config );
        if ( ( curr != null ) && !curr.equals( config ) ) {
          return false;
        } else if ( ( curr != null ) && curr.equals( config ) ) {
          return true;
        } else {
          Topology.this.currentEpoch++;
          return true;
        }
      }
      
      @Override
      public boolean tryDisable( final ServiceConfiguration config ) {
        final ServiceKey serviceKey = ServiceKey.create( config );
        return !config.equals( Topology.this.getServices( ).get( serviceKey ) )
               || ( Topology.this.getServices( ).remove( serviceKey, config ) && this.nextEpoch( ) );
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
      public boolean tryEnable( final ServiceConfiguration config ) {
        final ServiceKey serviceKey = ServiceKey.create( config );
        final ServiceConfiguration curr = Topology.this.getServices( ).put( serviceKey, config );
        if ( ( curr != null ) && !curr.equals( config ) ) {
          transition( State.DISABLED ).apply( curr );
          return false;
        } else if ( ( curr != null ) && curr.equals( config ) ) {
          return true;
        } else {
          return true;
        }
      }
      
      @Override
      public boolean tryDisable( final ServiceConfiguration config ) {
        final ServiceKey serviceKey = ServiceKey.create( config );
        return ( Topology.this.getServices( ).remove( serviceKey, config ) || !config.equals( Topology.this.getServices( ).get( serviceKey ) ) )
               && this.nextEpoch( );
      }
    };
  }
  
  public static class ServiceKey implements Comparable<ServiceKey> {
    private final Partition   partition;
    private final ComponentId componentId;
    
    static ServiceKey create( final ComponentId compId, final Partition partition ) {
      return new ServiceKey( compId, partition );
    }
    
    static ServiceKey create( final ServiceConfiguration config ) {
      if ( config.getComponentId( ).isPartitioned( ) && !Empyrean.class.equals( config.getComponentId( ).partitionParent( ) ) ) {
        final Partition p = Partitions.lookup( config );
        return new ServiceKey( config.getComponentId( ), p );
      } else if ( config.getComponentId( ).isAlwaysLocal( ) || ( config.getComponentId( ).isCloudLocal( ) && !config.getComponentId( ).isRegisterable( ) ) ) {
        final Partition p = Partitions.lookupInternal( config );
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
      final StringBuilder builder = new StringBuilder( );
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
      result = prime * result
               + ( ( this.componentId == null )
                                               ? 0
                                               : this.componentId.hashCode( ) );
      result = prime * result
               + ( ( this.partition == null )
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
      if ( this.getClass( ) != obj.getClass( ) ) {
        return false;
      }
      final ServiceKey other = ( ServiceKey ) obj;
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
    public int compareTo( final ServiceKey that ) {
      if ( this.componentId.equals( that.componentId ) ) {
        if ( ( this.partition == null ) && ( that.partition == null ) ) {
          return 0;
        } else if ( this.partition != null ) {
          return this.partition.compareTo( that.partition );
        } else {
          return -1;
        }
      } else {
        return this.componentId.compareTo( that.componentId );
      }
    }
    
  }
  
  protected static TransitionGuard guard( ) {
    return getInstance( ).getGuard( );
  }
  
  public TransitionGuard getGuard( ) {
    return ( Hosts.isCoordinator( )
                                   ? this.cloudControllerGuard( )
                                   : this.remoteGuard( ) );
  }
  
  private ConcurrentMap<ServiceKey, ServiceConfiguration> getServices( ) {
    return this.services;
  }
  
  enum ProceedToDisabledServiceFilter implements Predicate<ServiceConfiguration> {
    INSTANCE;
    @Override
    public boolean apply( final ServiceConfiguration arg0 ) {
      return arg0.lookupState( ).ordinal( ) < Component.State.DISABLED.ordinal( ) && !Component.State.STOPPED.apply( arg0 );
    }
    
  }
  
  enum AlwaysLocalServiceFilter implements Predicate<ServiceConfiguration> {
    INSTANCE;
    @Override
    public boolean apply( final ServiceConfiguration arg0 ) {
      return arg0.isVmLocal( )
             && arg0.getComponentId( ).isAlwaysLocal( )
             && arg0.lookupState( ).ordinal( ) < Component.State.ENABLED.ordinal( )
             && !Component.State.STOPPED.apply( arg0 );
    }
    
  }
  
  enum CheckServiceFilter implements Predicate<ServiceConfiguration> {
    INSTANCE;
    @Override
    public boolean apply( final ServiceConfiguration arg0 ) {
      if ( Hosts.isCoordinator( ) && arg0.getComponentId( ).isDistributedService( ) ) {
        return true;
      } else if ( arg0.isHostLocal( ) && BootstrapArgs.isCloudController( ) ) {
        return true;
      } else {
        return arg0.isVmLocal( );
      }
    }
    
  }
  
  enum SubmitEnable implements Function<ServiceConfiguration, Future<ServiceConfiguration>> {
    INSTANCE;
    
    @Override
    public Future<ServiceConfiguration> apply( final ServiceConfiguration input ) {
      final Callable<ServiceConfiguration> call = Topology.callable( input, Topology.get( State.ENABLED ) );
      final Future<ServiceConfiguration> future = Queue.EXTERNAL.enqueue( call );
      return future;
    }
    
    @Override
    public String toString( ) {
      return "ENABLED";
    }
  }
  
  enum SubmitDisable implements Function<ServiceConfiguration, Future<ServiceConfiguration>> {
    INSTANCE;
    
    @Override
    public Future<ServiceConfiguration> apply( final ServiceConfiguration input ) {
      final Callable<ServiceConfiguration> call = Topology.callable( input, Topology.get( State.DISABLED ) );
      final Future<ServiceConfiguration> future = Queue.EXTERNAL.enqueue( call );
      return future;
    }
    
    @Override
    public String toString( ) {
      return "DISABLED";
    }
    
  }
  
  enum SubmitCheck implements Function<ServiceConfiguration, Future<ServiceConfiguration>> {
    INSTANCE;
    
    @Override
    public Future<ServiceConfiguration> apply( final ServiceConfiguration input ) {
      final Callable<ServiceConfiguration> call = Topology.callable( input, Topology.check( ) );
      final Future<ServiceConfiguration> future = Queue.EXTERNAL.enqueue( call );
      return future;
    }
    
    @Override
    public String toString( ) {
      return "CHECKED";
    }
  }
  
  enum WaitForResults implements Predicate<Future> {
    INSTANCE;
    
    @Override
    public boolean apply( final Future input ) {
      try {
        final Object conf = input.get( 120, TimeUnit.SECONDS );
        return true;
      } catch ( final InterruptedException ex ) {
        Thread.currentThread( ).interrupt( );
      } catch ( final Exception ex ) {
        Logs.extreme( ).trace( ex, ex );
      }
      return false;
    }
    
  }
  
  enum FilterErrorResults implements Predicate<Future> {
    INSTANCE;
    
    @Override
    public boolean apply( final Future input ) {
      try {
        final Object conf = input.get( 30, TimeUnit.SECONDS );
        return false;
      } catch ( final InterruptedException ex ) {
        Thread.currentThread( ).interrupt( );
      } catch ( final Exception ex ) {
        Logs.extreme( ).trace( ex, ex );
      }
      return true;
    }
    
  }
  
  enum ServiceString implements Function<ServiceConfiguration, String> {
    INSTANCE;
    @Override
    public String apply( final ServiceConfiguration input ) {
      return input.getFullName( ) + ":" + input.lookupState( );
    }
  }
  
  enum ExtractFuture implements Function<Future<ServiceConfiguration>, ServiceConfiguration> {
    INSTANCE;
    @Override
    public ServiceConfiguration apply( final Future<ServiceConfiguration> input ) {
      try {
        return input.get( );
      } catch ( final InterruptedException ex ) {
        Thread.currentThread( ).interrupt( );
      } catch ( final Exception ex ) {
        Logs.extreme( ).trace( ex, ex );
      }
      return null;
    }
  }
  
  enum ExtractErrorFuture implements Function<Future<ServiceConfiguration>, Exception> {
    INSTANCE;
    @Override
    public Exception apply( final Future<ServiceConfiguration> input ) {
      try {
        input.get( );
      } catch ( final InterruptedException ex ) {
        Thread.currentThread( ).interrupt( );
      } catch ( final Exception ex ) {
        Logs.extreme( ).trace( ex, ex );
        return ex;
      }
      return null;
    }
  }
  
  enum RunChecks implements Callable<List<ServiceConfiguration>> {
    INSTANCE;
    
    @Override
    public List<ServiceConfiguration> call( ) {
      if ( Databases.isVolatile( ) ) {
        return Lists.newArrayList( );
      }
      /** submit describe operations **/
      final List<ServiceConfiguration> allServices = Lists.newArrayList( );
      for ( final Component c : Components.list( ) ) {
        allServices.addAll( c.services( ) );
      }
      Faults.flush( );
      List<ServiceConfiguration> checkedServices = submitTransitions( allServices, CheckServiceFilter.INSTANCE, SubmitCheck.INSTANCE );
      if ( !checkedServices.isEmpty( ) ) {
        Logs.extreme( ).debug( "CHECKED" + ": " + Joiner.on( "\n" + "CHECKED" + ": " ).join( Collections2.transform( checkedServices, ServiceString.INSTANCE ) ) );
      }
      if ( Faults.isFailstop( ) ) {
        Hosts.failstop( );
        submitTransitions( allServices, CheckServiceFilter.INSTANCE, SubmitCheck.INSTANCE );
        return Lists.newArrayList( );
      } else if ( !Hosts.isCoordinator( ) ) {
        final Predicate<ServiceConfiguration> proceedToDisableFilter = Predicates.and( ServiceConfigurations.filterHostLocal( ),
                                                                                       ProceedToDisabledServiceFilter.INSTANCE );
        submitTransitions( allServices, proceedToDisableFilter, SubmitDisable.INSTANCE );
        submitTransitions( allServices, AlwaysLocalServiceFilter.INSTANCE, SubmitEnable.INSTANCE );
        /** TODO:GRZE: check and disable timeout here **/
        return checkedServices;
      } else {
        /** make promotion decisions **/
        final Predicate<ServiceConfiguration> canPromote = Predicates.and( Predicates.in( checkedServices ), FailoverPredicate.INSTANCE );
        Collections.shuffle( allServices );
        final Collection<ServiceConfiguration> promoteServices = Collections2.filter( allServices, canPromote );
        List<ServiceConfiguration> result = submitTransitions( allServices, canPromote, SubmitEnable.INSTANCE );
        
        /** advance other components as needed **/
        final Predicate<ServiceConfiguration> proceedToDisableFilter = Predicates.and( Predicates.not( Predicates.in( promoteServices ) ),
                                                                                       ProceedToDisabledServiceFilter.INSTANCE );
        submitTransitions( allServices, proceedToDisableFilter, SubmitDisable.INSTANCE );
        return result;
      }
    }
    
    private static List<ServiceConfiguration> submitTransitions( final List<ServiceConfiguration> services,
                                                                       final Predicate<ServiceConfiguration> serviceFilter,
                                                                       final Function<ServiceConfiguration, Future<ServiceConfiguration>> submitFunction ) {
      final Collection<ServiceConfiguration> filteredServices = Collections2.filter( services, serviceFilter );
      final Collection<Future<ServiceConfiguration>> submittedCallables = Collections2.transform( filteredServices, submitFunction );
      final Collection<Future<ServiceConfiguration>> completedServices = Collections2.filter( submittedCallables, WaitForResults.INSTANCE );
      List<ServiceConfiguration> results = Lists.newArrayList( Collections2.transform( completedServices, ExtractFuture.INSTANCE ) );
      printCheckInfo( submitFunction.toString( ), results );
      return results;
    }
    
    private static void printCheckInfo( final String action, final Collection<ServiceConfiguration> result ) {
      if ( !result.isEmpty( ) ) {
        Logs.extreme( ).debug( action + ": " + Joiner.on( "\n" + action + ": " ).join( Collections2.transform( result, ServiceString.INSTANCE ) ) );
      }
    }
  }
  
  enum FailoverPredicate implements Predicate<ServiceConfiguration> {
    INSTANCE;
    @Override
    public boolean apply( final ServiceConfiguration arg0 ) {
      final ServiceKey key = ServiceKey.create( arg0 );
      if ( !Hosts.isCoordinator( ) ) {
        Logs.extreme( ).debug( "FAILOVER-REJECT: " + Internets.localHostInetAddress( )
                               + ": not cloud controller, ignoring promotion for: "
                                   + arg0.getFullName( ) );
        return false;
      } else if ( !arg0.isHostLocal( ) && !Hosts.contains( arg0.getHostName( ) ) ) {
        Logs.extreme( ).debug( "FAILOVER-REJECT: " + arg0.getFullName( )
                               + ": host for the service is not available: " + arg0.getHostName( ) );
        return false;
      } else if ( !arg0.isHostLocal( ) && Hosts.contains( arg0.getHostName( ) ) && !Hosts.lookup( arg0.getHostName( ) ).hasBootstrapped( ) ) {
        Logs.extreme( ).debug( "FAILOVER-REJECT: " + arg0.getFullName( )
                               + ": host for the service has not yet bootstrapped: " + arg0.getHostName( ) );
        return false;
      } else if ( Topology.getInstance( ).getServices( ).containsKey( key ) && arg0.equals( Topology.getInstance( ).getServices( ).get( key ) ) ) {
        Logs.extreme( ).debug( "FAILOVER-REJECT: " + arg0.getFullName( )
                               + ": service is already ENABLED." );
        return false;
      } else if ( !Component.State.DISABLED.equals( arg0.lookupState( ) ) ) {
        Logs.extreme( ).debug( "FAILOVER-REJECT: " + arg0.getFullName( )
                               + ": service is in an invalid state: "
                               + arg0.lookupState( ) );
        return false;
      } else if ( !Topology.getInstance( ).getServices( ).containsKey( key ) ) {
        Logs.extreme( ).debug( "FAILOVER-ACCEPT: " + arg0.getFullName( )
                               + ": service for partition: "
                               + key );
        return true;
      } else {
        Logs.extreme( ).debug( "FAILOVER-ACCEPT: " + arg0 );
        return true;
      }
    }
  }
  
  public static ServiceConfiguration lookup( final Class<? extends ComponentId> compClass, final Partition... maybePartition ) {
    final Partition partition =
      ( ( maybePartition != null ) && ( maybePartition.length > 0 )
                                                                   ? ( ComponentIds.lookup( compClass ).isPartitioned( )
                                                                                                                        ? maybePartition[0]
                                                                                                                        : null )
                                                                   : null );
    ServiceConfiguration res = Topology.getInstance( ).getServices( ).get( ServiceKey.create( ComponentIds.lookup( compClass ), partition ) );
    if ( res == null ) {
      throw new NoSuchElementException( "Failed to lookup ENABLED service of type "
                                        + compClass.getSimpleName( )
                                        + ( partition != null ? " in partition " + partition
                                                             : "." ) );
    } else {
      return res;
    }
  }
  
  public static Collection<ServiceConfiguration> enabledServices( final Class<? extends ComponentId> compId ) {
    return Collections2.filter( enabledServices( ), componentFilter( compId ) );
  }
  
  public static Collection<ServiceConfiguration> enabledServices( ) {
    return Topology.getInstance( ).services.values( );
  }
  
  public static boolean isEnabledLocally( final Class<? extends ComponentId> compClass ) {
    return Iterables.any( Topology.enabledServices( compClass ), ServiceConfigurations.filterHostLocal( ) );
  }
  
  public static boolean isEnabled( final Class<? extends ComponentId> compClass ) {
    return !Topology.enabledServices( compClass ).isEmpty( );
  }
  
  @Override
  public String toString( ) {
    final StringBuilder builder = new StringBuilder( );
    builder.append( "Topology:currentEpoch=" ).append( this.currentEpoch ).append( ":guard=" ).append( Hosts.isCoordinator( )
                                                                                                                             ? "cloud"
                                                                                                                             : "remote" );
    return builder.toString( );
  }
  
  //@noformat
  private static final
  Map<Component.State, Function<ServiceConfiguration, ServiceConfiguration>> 
  cloudTransitionCallables = new MapMaker( ).makeComputingMap( TopologyFunctionGenerator.INSTANCE ); //TODO:GRZE: CacheBuilder
  //@format
  private static Function<ServiceConfiguration, ServiceConfiguration> get( final Component.State state ) {
    return cloudTransitionCallables.get( state );
  }
  
  private static Function<ServiceConfiguration, ServiceConfiguration> check( ) {
    return Transitions.CHECK;
  }
  
  private static Callable<ServiceConfiguration> callable( final ServiceConfiguration config, final Function<ServiceConfiguration, ServiceConfiguration> function ) {
    final Long queueStart = System.currentTimeMillis( );
    final Callable<ServiceConfiguration> call = new Callable<ServiceConfiguration>( ) {
      
      @Override
      public ServiceConfiguration call( ) throws Exception {
        if ( Bootstrap.isShuttingDown( ) ) {
          throw Exceptions.toUndeclared( "System is shutting down." );
        } else {
          final Long serviceStart = System.currentTimeMillis( );
          Logs.extreme( ).debug( EventRecord.here( Topology.class, EventType.DEQUEUE, function.toString( ), config.getFullName( ).toString( ),
                                                   Long.toString( serviceStart - queueStart ), "ms" ) );
          
          try {
            final ServiceConfiguration result = function.apply( config );
            Logs.extreme( ).debug( EventRecord.here( Topology.class, EventType.QUEUE, function.toString( ), config.getFullName( ).toString( ),
                                                     Long.toString( System.currentTimeMillis( ) - serviceStart ), "ms" ) );
            return result;
          } catch ( final Exception ex ) {
            final Throwable t = Exceptions.unwrapCause( ex );
            Logs.extreme( ).error( config.getFullName( ) + " failed to transition because of:\n" + t.getMessage( ) );
            Logs.extreme( ).error( t, t );
            throw ex;
          }
        }
      }
      
      @Override
      public String toString( ) {
        return Topology.class.getSimpleName( ) + ":" + config.getFullName( ) + " " + function.toString( );
      }
    };
    return call;
  }
  
  public enum Transitions implements Function<ServiceConfiguration, ServiceConfiguration>, Supplier<Component.State> {
    START( Component.State.DISABLED ),
    STOP( Component.State.STOPPED ) {

      @Override
      public ServiceConfiguration apply( ServiceConfiguration input ) {
        return super.tc.apply( input );
      }
      
    },
    INITIALIZE( Component.State.INITIALIZED ),
    LOAD( Component.State.LOADED ),
    DESTROY( Component.State.PRIMORDIAL ),
    ENABLE( Component.State.ENABLED ) {
      @Override
      public ServiceConfiguration apply( final ServiceConfiguration config ) {
        boolean busy = config.lookupStateMachine( ).isBusy( );
        boolean tryEnable = false;
        boolean manyToOne = config.getComponentId( ).isManyToOnePartition( );
        if ( manyToOne ) {
          try {
            return super.apply( config );
          } catch ( final RuntimeException ex ) {
            throw ex;
          }
        } else if ( Topology.guard( ).tryEnable( config ) ) {
          tryEnable = true;
          try {
            ServiceConfiguration res = super.apply( config );
            return res;
          } catch ( final RuntimeException ex ) {
            Topology.guard( ).tryDisable( config );
            throw ex;
          }
        } else if ( !busy ) {
//          throw new IllegalStateException( "Failed to ENABLE " + config.getFullName( ) );
          try {
            return ServiceTransitions.pathTo( config, Component.State.DISABLED ).get( );
          } catch ( final InterruptedException ex ) {
            Thread.currentThread( ).interrupt( );
            throw Exceptions.toUndeclared( ex );
          } catch ( final ExecutionException ex ) {
            throw Exceptions.toUndeclared( ex );
          }
        } else {
          throw new IllegalStateException( "Failed to ENABLE " + config.getFullName( ) + " since manyToOne=" + manyToOne + " tryEnable=" + tryEnable + " fsm.isBusy()=" + busy );
        }
      }
    },
    DISABLE( Component.State.DISABLED ) {
      @Override
      public ServiceConfiguration apply( final ServiceConfiguration config ) {
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
      public ServiceConfiguration apply( final ServiceConfiguration config ) {
        if ( !Bootstrap.isFinished( ) ) {
          LOG.debug( this.toString( )
                     + " aborted because bootstrap is not complete for service: "
                     + config );
          return config;
        } else {
          return super.apply( config );
        }
      }
    },
    RESTART;
    
    private final Component.State  state;
    protected final TopologyChange tc;
    
    private Transitions( ) {
      this.state = null;
      this.tc = new TopologyChange( this );
    }
    
    private Transitions( final State state ) {
      this.state = state;
      this.tc = new TopologyChange( this );
    }
    
    @Override
    public ServiceConfiguration apply( final ServiceConfiguration input ) {
      Components.lookup( input.getComponentId( ) ).setup( input );
      return this.tc.apply( input );
    }
    
    @Override
    public String toString( ) {
      return this.name( ) + ":" + this.get( ) + " ";
    }
    
    @Override
    public State get( ) {
      return this.state;
    }
    
  }
  
  private static class TopologyChange implements Function<ServiceConfiguration, ServiceConfiguration>, Supplier<Component.State> {
    private final Topology.Transitions transitionName;
    
    TopologyChange( final Transitions transitionName ) {
      this.transitionName = transitionName;
    }
    
    @Override
    public ServiceConfiguration apply( final ServiceConfiguration input ) {
      State nextState = null;
      if ( ( nextState = this.findNextCheckState( input.lookupState( ) ) ) == null ) {
        return input;
      } else {
        return this.doTopologyChange( input, nextState );
      }
    }
    
    private ServiceConfiguration doTopologyChange( final ServiceConfiguration input, final State nextState ) throws RuntimeException {
      final State initialState = input.lookupState( );
      ServiceConfiguration endResult = input;
      try {
        endResult = ServiceTransitions.pathTo( input, nextState ).get( );
        Logs.extreme( ).debug( this.toString( endResult, initialState, nextState ) );
        return endResult;
      } catch ( final Exception ex ) {
        Exceptions.maybeInterrupted( ex );
        LOG.error( this.toString( input, initialState, nextState, ex ) );
        Logs.extreme( ).error( ex, Throwables.getRootCause( ex ) );
        Logs.extreme( ).error( ex, ex );
        throw Exceptions.toUndeclared( ex );
      } finally {
        boolean enabledEndState = Component.State.ENABLED.equals( endResult.lookupState( ) );
        if ( Bootstrap.isFinished( ) && !enabledEndState && Topology.getInstance( ).services.containsValue( input ) ) {
          Topology.guard( ).tryDisable( endResult );
        }
      }
    }
    
    private String toString( final ServiceConfiguration endResult, final State initialState, final State nextState, final Throwable... throwables ) {
      return String.format( "%s %s %s->%s=%s [%s]\n", this.toString( ), endResult.getFullName( ), initialState, nextState, endResult.lookupState( ),
                            ( ( throwables != null ) && ( throwables.length > 0 )
                                                                                 ? Throwables.getRootCause( throwables[0] ).getMessage( )
                                                                                 : "WINNING" ) );
    }
    
    @Override
    public String toString( ) {
      return this.transitionName.toString( );
    }
    
    @Override
    public State get( ) {
      return this.transitionName.get( );
    }
    
    private State findNextCheckState( final State initialState ) {
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
  
  private enum TopologyFunctionGenerator implements Function<Component.State, Function<ServiceConfiguration, ServiceConfiguration>> {
    INSTANCE;
    
    @Override
    public Function<ServiceConfiguration, ServiceConfiguration> apply( final State input ) {
      for ( final Transitions c : Transitions.values( ) ) {
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
