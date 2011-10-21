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
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.TimeUnit;
import org.apache.log4j.Logger;
import com.eucalyptus.bootstrap.Bootstrap;
import com.eucalyptus.bootstrap.BootstrapArgs;
import com.eucalyptus.bootstrap.Hosts;
import com.eucalyptus.empyrean.Empyrean;
import com.eucalyptus.empyrean.ServiceId;
import com.eucalyptus.empyrean.ServiceTransitionType;
import com.eucalyptus.event.Event;
import com.eucalyptus.event.EventListener;
import com.eucalyptus.event.Hertz;
import com.eucalyptus.event.ListenerRegistry;
import com.eucalyptus.records.EventRecord;
import com.eucalyptus.records.EventType;
import com.eucalyptus.records.Logs;
import com.eucalyptus.system.Threads;
import com.eucalyptus.system.Threads.ThreadPool;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.LogUtil;
import com.eucalyptus.util.TypeMappers;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.base.Supplier;
import com.google.common.collect.Collections2;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.primitives.Ints;
import edu.ucsb.eucalyptus.msgs.BaseMessage;

public class Topology implements EventListener<Event> {
  private static Logger                                                                           LOG               = Logger.getLogger( Topology.class );
  private static Topology                                                                         singleton         = null;                                                                   //TODO:GRZE:handle differently for remote case?
  private Integer                                                                                 currentEpoch      = 0;                                                                      //TODO:GRZE: get the right initial epoch value from membership bootstrap
  private final ConcurrentMap<ServiceKey, ServiceConfiguration>                                   services          = new ConcurrentSkipListMap<Topology.ServiceKey, ServiceConfiguration>( );
  private final ServiceConfiguration                                                              internalQueue;
  private final ServiceConfiguration                                                              externalQueue;
  
  private Topology( final int i ) {
    super( );
    this.currentEpoch = i;
    this.internalQueue = ServiceConfigurations.createEphemeral( Empyrean.INSTANCE, Topology.class.getSimpleName( ), "internal",
                                                                ServiceUris.internal( Empyrean.INSTANCE ) );
    this.externalQueue = ServiceConfigurations.createEphemeral( Empyrean.INSTANCE, Topology.class.getSimpleName( ), "external",
                                                                ServiceUris.internal( Empyrean.INSTANCE ) );
    ListenerRegistry.getInstance( ).register( Hertz.class, this );
  }
  
  private static Predicate<Entry<ServiceKey, ServiceConfiguration>> componentFilter( final Class<? extends ComponentId> compId ) {
    return new Predicate<Entry<ServiceKey, ServiceConfiguration>>( ) {
      @Override
      public boolean apply( final Entry<ServiceKey, ServiceConfiguration> arg0 ) {
        return compId.equals( arg0.getKey( ).getComponentId( ) );
      }
    };
  }
  
  private static Predicate<Partition> partitionFilter( final Partition p ) {
    return new Predicate<Partition>( ) {
      
      @Override
      public boolean apply( Partition input ) {
        return p.equals( input );
      }
    };
  }
  
  public List<ServiceConfiguration> partitionView( final Partition partition ) {
    return Lists.newArrayList( Iterables.filter( this.getServices( ).values( ), partitionViewFilter( partition ) ) );
  }
  
  private static Predicate<ServiceConfiguration> partitionViewFilter( final Partition p ) {
    return new Predicate<ServiceConfiguration>( ) {
      
      @Override
      public boolean apply( final ServiceConfiguration config ) {
        if ( config.getComponentId( ).isPublicService( ) ) {
          return true;
        } else if ( config.getComponentId( ).isRootService( ) && !config.getComponentId( ).isPartitioned( ) ) {
          return true;
        } else if ( ( config.getComponentId( ).isRootService( ) || config.getComponentId( ).isRegisterable( ) ) && config.getComponentId( ).isPartitioned( )
                    && p.getName( ).equals( config.getPartition( ) ) ) {
          return true;
        } else {
          return false;
        }
      }
    };
  }
  
  public static void touch( final ServiceTransitionType msg ) {//TODO:GRZE: @Service interceptor
    if ( !Hosts.isCoordinator( ) && ( msg.get_epoch( ) != null ) ) {
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
  
  public static Topology getInstance( ) {
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
  
  private Function<Callable<ServiceConfiguration>, Future<ServiceConfiguration>> externalCompletionFunction( ) {
    return new Function<Callable<ServiceConfiguration>, Future<ServiceConfiguration>>( ) {
      
      @Override
      public Future<ServiceConfiguration> apply( Callable<ServiceConfiguration> input ) {
        CompletionService<ServiceConfiguration> externalCompletionService = externalCompletionService( );
        return externalCompletionService.submit( input );
      }
    };
  }
  
  private CompletionService completionService( final ServiceConfiguration config ) {
    return Threads.lookup( Empyrean.class, Topology.class, "worker-" + config.getComponentId( ).name( ) ).limitTo( 1 ).getCompletionService( );
  }
  
  private ThreadPool externalPool( ) {
    return Threads.lookup( Empyrean.class, Topology.class, "external-worker" ).limitTo( 16 );
  }
  
  private static <T> CompletionService<T> externalCompletionService( ) {
    return getInstance( ).externalPool( ).getCompletionService( );
  }
  
  private Integer getEpoch( ) {
    return this.currentEpoch;
  }
  
  public static int epoch( ) {
    return Topology.getInstance( ).getEpoch( );
  }
  
  public static List<ServiceId> partitionRelativeView( final ServiceConfiguration config, final InetAddress localAddr ) {
    final Partition partition = Partitions.lookup( config );
    return Lists.transform( Topology.getInstance( ).partitionView( partition ), TypeMappers.lookup( ServiceConfiguration.class, ServiceId.class ) );
  }
  
  private RunnableFuture<ServiceConfiguration> submitExternal( final ServiceConfiguration config, final Function<ServiceConfiguration, ServiceConfiguration> function ) {
    final Callable<ServiceConfiguration> call = this.transitionCallable( config, function );
    return Threads.enqueue( this.externalQueue, 32, call );
  }
  
  private RunnableFuture<ServiceConfiguration> submit( final ServiceConfiguration config, final Function<ServiceConfiguration, ServiceConfiguration> function ) {
    final Callable<ServiceConfiguration> call = this.transitionCallable( config, function );
    return Threads.enqueue( this.internalQueue, 1, call );
  }
  
  private static Function<ServiceConfiguration, Callable<ServiceConfiguration>> functionalCallable( final Function<ServiceConfiguration, ServiceConfiguration> function ) {
    return new Function<ServiceConfiguration, Callable<ServiceConfiguration>>( ) {
      
      @Override
      public Callable<ServiceConfiguration> apply( ServiceConfiguration input ) {
        return transitionCallable( input, function );
      }
    };
  }
  
  private static Callable<ServiceConfiguration> transitionCallable( final ServiceConfiguration config, final Function<ServiceConfiguration, ServiceConfiguration> function ) {
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
          } catch ( final Exception ex ) {
            Logs.exhaust( ).error( ex, ex );
            LOG.error( config.getFullName( ) + " failed to transition because of: " + ex );
            throw ex;
          }
        }
      }
    };
    return call;
  }
  
  public static RunnableFuture<ServiceConfiguration> check( final ServiceConfiguration config ) {
    return Topology.getInstance( ).submitExternal( config, TopologyChanges.checkFunction( ) );
  }
  
  public static RunnableFuture<ServiceConfiguration> stop( final ServiceConfiguration config ) {
    return Topology.getInstance( ).submitExternal( config, TopologyChanges.stopFunction( ) );
  }
  
  public static RunnableFuture<ServiceConfiguration> destroy( final ServiceConfiguration config ) {
    return Topology.getInstance( ).submitExternal( config, TopologyChanges.destroyFunction( ) );
  }
  
  public static RunnableFuture<ServiceConfiguration> start( final ServiceConfiguration config ) {
    return Topology.getInstance( ).submitExternal( config, TopologyChanges.startFunction( ) );
  }
  
  public static RunnableFuture<ServiceConfiguration> enable( final ServiceConfiguration config ) {
    return Topology.getInstance( ).submit( config, TopologyChanges.enableFunction( ) );
  }
  
  public static RunnableFuture<ServiceConfiguration> disable( final ServiceConfiguration config ) {
    return Topology.getInstance( ).submit( config, TopologyChanges.disableFunction( ) );
  }
  
  private ServiceConfiguration lookup( final ServiceKey serviceKey ) {
    return this.getServices( ).get( serviceKey );
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
      public boolean tryDisable( final ServiceConfiguration config ) throws ServiceRegistrationException {
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
      public boolean tryEnable( final ServiceConfiguration config ) throws ServiceRegistrationException {
        final ServiceKey serviceKey = ServiceKey.create( config );
        final ServiceConfiguration curr = Topology.this.getServices( ).put( serviceKey, config );
        if ( ( curr != null ) && !curr.equals( config ) ) {
          return false;
        } else if ( ( curr != null ) && curr.equals( config ) ) {
          return true;
        } else {
          return true;
        }
      }
      
      @Override
      public boolean tryDisable( final ServiceConfiguration config ) throws ServiceRegistrationException {
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
      if ( config.getComponentId( ).isPartitioned( ) ) {
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
  
  /**
   * @see com.eucalyptus.event.EventListener#fireEvent(com.eucalyptus.event.Event)
   */
  @Override
  public void fireEvent( final Event event ) {
    if ( ( event instanceof Hertz ) && ( ( Hertz ) event ).isAsserted( 15l ) ) {
      Future enabled = this.completionService( this.internalQueue ).submit( RunChecks.INSTANCE );
    }
  }
  
  enum CheckServiceFilter implements Predicate<ServiceConfiguration> {
    INSTANCE;
    @Override
    public boolean apply( final ServiceConfiguration arg0 ) {
      if ( BootstrapArgs.isCloudController( ) ) {
        return true;
      } else {
        return arg0.isVmLocal( );
      }
    }
    
  }
  
  enum SubmitCheck implements Function<ServiceConfiguration, Callable<ServiceConfiguration>> {
    INSTANCE;
    
    @Override
    public Callable<ServiceConfiguration> apply( ServiceConfiguration input ) {
      return Topology.transitionCallable( input, TopologyChanges.checkFunction( ) );
    }
    
  }
  
  enum WaitForResults implements Predicate<Future<?>> {
    INSTANCE;
    
    @SuppressWarnings( "unchecked" )
    public static <T> List<T> invokeAll( Collection<? extends Callable<T>> submitChecks ) {
      try {
        List<Future<T>> results = Topology.getInstance( ).externalPool( ).invokeAll( submitChecks, 30L, TimeUnit.SECONDS );
        return ( List<T> ) Collections2.transform( Collections2.filter( results, WaitForResults.INSTANCE ), Transform.TRANSFORM );
      } catch ( InterruptedException ex ) {
        Thread.currentThread( ).interrupt( );
        throw Exceptions.toUndeclared( ex );
      }
    }
    
    @Override
    public boolean apply( Future<?> input ) {
      try {
        final Object conf = input.get( );
        LOG.trace( "Operation succeeded for " + conf );
        return true;
      } catch ( final InterruptedException ex ) {
        Thread.currentThread( ).interrupt( );
      } catch ( final Exception ex ) {
        Logs.exhaust( ).trace( ex, ex );
      }
      return false;
    }
    
    enum Transform implements Function<Future<?>, Object> {
      TRANSFORM;
      @Override
      public Object apply( Future<?> input ) {
        try {
          return input.get( );
        } catch ( final InterruptedException ex ) {
          Thread.currentThread( ).interrupt( );
        } catch ( final Exception ex ) {
          Logs.exhaust( ).trace( ex, ex );
        }
        return null;
      }
    }
  }
  
  enum GatherCheckResults implements Supplier<ServiceConfiguration> {
    INSTANCE;
    
    @Override
    public ServiceConfiguration get( ) {
      try {
        CompletionService<ServiceConfiguration> externalCompletionService = Topology.getInstance( ).externalCompletionService( );
        final ServiceConfiguration conf = externalCompletionService.take( ).get( );
        LOG.trace( "CHECK passed: " + conf.getFullName( ).toString( ) );
        return conf;
      } catch ( InterruptedException ex ) {
        Thread.currentThread( ).interrupt( );
        throw Exceptions.toUndeclared( ex );
      } catch ( ExecutionException ex ) {
        throw Exceptions.toUndeclared( ex );
      }
    }
  }
  
  enum RunChecks implements Callable<List<ServiceConfiguration>> {
    INSTANCE;
    
    @Override
    public List<ServiceConfiguration> call( ) {
      /** submit describe operations **/
      Collection<ServiceConfiguration> checkServices = Collections2.filter( ServiceConfigurations.list( ), CheckServiceFilter.INSTANCE );
      Collection<Callable<ServiceConfiguration>> submitChecks = Collections2.transform( checkServices, SubmitCheck.INSTANCE );
      
      /** consume describe results **/
      Collection<ServiceConfiguration> goodServices = WaitForResults.invokeAll( submitChecks );
      LOG.trace( LogUtil.subheader( "CHECKED: " + Joiner.on( "\nCHECKED: " ).join( goodServices ) ) );
      
      /** make promotion decisions **/
      Predicate<ServiceConfiguration> canPromote = Predicates.and( FailoverPredicate.INSTANCE, Predicates.not( Predicates.in( goodServices ) ) );
      Collection<ServiceConfiguration> promoteServices = Collections2.filter( ServiceConfigurations.list( ), canPromote );
      Collection<Callable<ServiceConfiguration>> enableCallables = Collections2.transform( promoteServices,
                                                                                           functionalCallable( TopologyChanges.enableFunction( ) ) );
      List<ServiceConfiguration> enabledServices = WaitForResults.invokeAll( enableCallables );
      LOG.trace( LogUtil.subheader( "ENABLED: " + Joiner.on( "\nENABLED: " ).join( enabledServices ) ) );
      return enabledServices;
    }
  }
  
  enum FailoverPredicate implements Predicate<ServiceConfiguration> {
    INSTANCE;
    @Override
    public boolean apply( final ServiceConfiguration arg0 ) {
      final ServiceKey key = ServiceKey.create( arg0 );
      if ( !Hosts.isCoordinator( ) ) {
        Logs.exhaust( ).debug( "FAILOVER-REJECT: " + arg0 + ": not cloud controller." );
        return false;
      } else if ( Component.State.NOTREADY.equals( arg0.lookupState( ) ) ) {
        Logs.exhaust( ).debug( "FAILOVER-REJECT: " + arg0 + ": service is NOTREADY." );
        return false;
      } else if ( Topology.getInstance( ).getServices( ).containsKey( key ) && arg0.equals( Topology.getInstance( ).getServices( ).get( key ) ) ) {
        Logs.exhaust( ).debug( "FAILOVER-REJECT: " + arg0 + ": service is ENABLED." );
        return false;
      } else if ( !Topology.getInstance( ).getServices( ).containsKey( key ) ) {
        Logs.exhaust( ).debug( "FAILOVER-ACCEPT: " + arg0 + ": service for partition: " + key );
        return true;
      } else {
        Logs.exhaust( ).debug( "FAILOVER-ACCEPT: " + arg0 );
        return true;
      }
    }
  }
  
  public static ServiceConfiguration lookup( final Class<? extends ComponentId> compClass, final Partition... maybePartition ) {
    final Partition partition = ( ( maybePartition != null ) && ( maybePartition.length > 0 )
      ? maybePartition[0]
      : null );
    return Topology.getInstance( ).getServices( ).get( ServiceKey.create( ComponentIds.lookup( compClass ), partition ) );
    
  }
  
  public static Collection<ServiceConfiguration> enabledServices( final Class<? extends ComponentId> compId ) {
    return Topology.getInstance( ).services.values( );
  }
  
  public static boolean isEnabledLocally( final Class<? extends ComponentId> compClass ) {
    return Iterables.any( Topology.enabledServices( compClass ), ServiceConfigurations.filterHostLocal( ) );
  }
  
  @Override
  public String toString( ) {
    final StringBuilder builder = new StringBuilder( );
    builder.append( "Topology:currentEpoch=" ).append( this.currentEpoch ).append( ":guard=" ).append( Hosts.isCoordinator( )
      ? "cloud"
      : "remote" );
    return builder.toString( );
  }
  
}
