/*******************************************************************************
 *Copyright (c) 2009 Eucalyptus Systems, Inc.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, only version 3 of the License.
 * 
 * 
 * This file is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 * 
 * You should have received a copy of the GNU General Public License along
 * with this program. If not, see <http://www.gnu.org/licenses/>.
 * 
 * Please contact Eucalyptus Systems, Inc., 130 Castilian
 * Dr., Goleta, CA 93101 USA or visit <http://www.eucalyptus.com/licenses/>
 * if you need additional information or have any questions.
 * 
 * This file may incorporate work covered under the following copyright and
 * permission notice:
 * 
 * Software License Agreement (BSD License)
 * 
 * Copyright (c) 2008, Regents of the University of California
 * All rights reserved.
 * 
 * Redistribution and use of this software in source and binary forms, with
 * or without modification, are permitted provided that the following
 * conditions are met:
 * 
 * Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 * 
 * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
 * OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. USERS OF
 * THIS SOFTWARE ACKNOWLEDGE THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE
 * LICENSED MATERIAL, COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS
 * SOFTWARE, AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 * IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA, SANTA
 * BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY, WHICH IN
 * THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
 * OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
 * WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
 * ANY SUCH LICENSES OR RIGHTS.
 *******************************************************************************
 * @author chris grzegorczyk <grze@eucalyptus.com>
 */
package com.eucalyptus.component;

import java.net.InetAddress;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.log4j.Logger;
import com.eucalyptus.bootstrap.Bootstrap;
import com.eucalyptus.empyrean.ServiceInfoType;
import com.eucalyptus.records.EventRecord;
import com.eucalyptus.records.EventType;
import com.eucalyptus.util.Assertions;
import com.eucalyptus.util.FullName;
import com.eucalyptus.util.HasName;
import com.eucalyptus.util.Internets;
import com.eucalyptus.util.async.CheckedListenableFuture;
import com.eucalyptus.util.async.Futures;
import com.eucalyptus.util.fsm.Automata;
import com.eucalyptus.util.fsm.HasStateMachine;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

public class Component implements HasName<Component> {
  private static Logger               LOG = Logger.getLogger( Component.class );
  private final ComponentId           identity;
  private final ServiceRegistry       serviceRegistry;
  private final ComponentBootstrapper bootstrapper;
  
  public enum State implements Automata.State<State> {
    NONE, BROKEN, PRIMORDIAL, INITIALIZED, LOADED, STOPPED, NOTREADY, DISABLED, ENABLED;
    
    public boolean isIn( HasStateMachine<?, State, ?> arg0 ) {
      return this.equals( arg0.getStateMachine( ).getState( ) );
    }
  }
  
  public enum Transition implements Automata.Transition<Transition> {
    INITIALIZING, LOADING, STARTING, READY_CHECK, STOPPING, STOPPING_NOTREADY, ENABLING, ENABLED_CHECK, DISABLING, DISABLED_CHECK, DESTROYING, FAILED_TO_PREPARE, RELOADING;
  }
  
  Component( ComponentId componentId ) throws ServiceRegistrationException {
    this.identity = componentId;
    this.serviceRegistry = new ServiceRegistry( );
    this.bootstrapper = new ComponentBootstrapper( this );
  }
  
  public Service getLocalService( ) {
    return this.serviceRegistry.getLocalService( );
  }
  
  private boolean inState( State queryState ) {
    return queryState.equals( this.getState( ) );
  }
  
  /**
   * @return the identity
   */
  public ComponentId getComponentId( ) {
    return this.identity;
  }
  
  /**
   * @return
   * @see com.eucalyptus.component.ComponentId#name()
   */
  public String name( ) {
    return this.identity.name( );
  }
  
  public final List<ServiceInfoType> getServiceSnapshot( String localhostAddr ) {
    return this.serviceRegistry.getServiceInfos( localhostAddr );
  }
  
  public State getState( ) {
    return this.hasLocalService( )
      ? this.getLocalServiceConfiguration( ).lookupState( )
      : State.NONE;
  }
  
  /**
   * Get the name of this component. This is the proper short name; e.g., 'eucalyptus', 'walrus',
   * etc, as used in the META-INF descriptor file.
   * 
   * @return Component name
   */
  public String getName( ) {
    return this.identity.name( );
  }
  
  public ServiceBuilder<? extends ServiceConfiguration> getBuilder( ) {
    return ServiceBuilderRegistry.lookup( this.identity );
  }
  
  /**
   * Can the component be run locally (i.e., is the needed code available)
   * 
   * @return true if the component could be run locally.
   */
  public Boolean isAvailableLocally( ) {
    return this.identity.isAlwaysLocal( ) || ( !this.identity.isCloudLocal( ) || Bootstrap.isCloudController( ) );
  }
  
  /**
   * True if the component has not been explicitly configured as running in remote-mode where only
   * partial services are provided locally. That is, even if
   * the code is available locally we do not prepare the service bootstrappers to run, but the local
   * service endpoint is still configured (i.e. for
   * {@link com.eucalyptus.component.id.ComponentService.dns}).
   * 
   * @return true if the component has not been explicitly marked as remote.
   */
  public Boolean isRunningRemoteMode( ) {
    return this.isAvailableLocally( ) && this.identity.runLimitedServices( );
  }
  
  public NavigableSet<ServiceConfiguration> lookupServiceConfigurations( ) {
    return this.serviceRegistry.getServices( );
  }
  
  public URI getUri( ) {
    NavigableSet<ServiceConfiguration> services = this.serviceRegistry.getServices( );
    if ( this.getComponentId( ).isCloudLocal( ) && services.size( ) != 1 && !"db".equals( this.getName( ) ) ) {
      throw new RuntimeException( "Cloud local component has " + services.size( ) + " registered services (Should be exactly 1): " + this + " "
                                  + services.toString( ) );
    } else if ( this.getComponentId( ).isCloudLocal( ) && services.size( ) != 1 && "db".equals( this.getName( ) ) ) {
      return this.getComponentId( ).getLocalEndpointUri( );
    } else if ( this.getComponentId( ).isCloudLocal( ) && services.size( ) == 1 ) {
      return services.first( ).getUri( );
    } else {
      for ( ServiceConfiguration s : services ) {
        if ( s.isVmLocal( ) ) {
          return s.getUri( );
        }
      }
      throw new RuntimeException( "Attempting to get the URI for a service which is either not a singleton or has no locally defined service endpoint." );
    }
  }
  
  public Boolean hasLocalService( ) {
    return this.serviceRegistry.hasLocalService( );
  }
  
  public Boolean hasEnabledService( ) {
    return !this.enabledServices( ).isEmpty( );
  }
  
  public Boolean isEnabledLocally( ) {
    return this.serviceRegistry.hasLocalService( ) && State.ENABLED.equals( this.getLocalServiceConfiguration( ).lookupState( ) );
  }
  
  public Boolean isRunningLocally( ) {
    return this.isEnabledLocally( );
  }
  
  /**
   * @return the bootstrapper
   */
  public ComponentBootstrapper getBootstrapper( ) {
    return this.bootstrapper;
  }
  
  /**
   * @param config
   * @return
   * @throws NoSuchElementException
   * @see com.eucalyptus.component.Component.ServiceRegistry#lookup(com.eucalyptus.component.ServiceConfiguration)
   */
  public Service lookupService( ServiceConfiguration config ) throws NoSuchElementException {
    return this.serviceRegistry.lookup( config );
  }
  
  @Deprecated
  public ServiceConfiguration lookupServiceConfiguration( String name ) {
    return this.serviceRegistry.getService( name );
  }
  
  NavigableSet<Service> lookupServices( String partition ) {
    return this.serviceRegistry.lookupPartition( partition );
  }
  
  /**
   * Builds a Service instance for this component using the local default
   * values.
   * 
   * @return Service instance of the service
   * @throws ServiceRegistrationException
   */
  public ServiceConfiguration initService( ) throws ServiceRegistrationException {
    if ( !this.isAvailableLocally( ) ) {
      throw new ServiceRegistrationException( "The component " + this.getName( ) + " is not being loaded automatically." );
    } else {//if ( this.identity.isAlwaysLocal( ) || this.identity.isCloudLocal( ) ) {
      URI uri = this.getComponentId( ).getLocalEndpointUri( );
      String fakeName = Internets.localHostAddress( );
      ServiceConfiguration config = this.getBuilder( ).newInstance( this.getComponentId( ).getPartition( ), fakeName,
                                                                    uri.getHost( ), uri.getPort( ) );
      this.serviceRegistry.register( config );
      return config;
    }
  }
  
  /**
   * Builds a Service instance for this cloudLocal component when Eucalyptus is remote.
   * 
   * @return Service instance of the service
   * @throws ServiceRegistrationException
   */
  public ServiceConfiguration initRemoteService( InetAddress addr ) throws ServiceRegistrationException {
    if ( !Bootstrap.isCloudController( ) && this.getComponentId( ).isCloudLocal( ) ) {
      ServiceConfiguration config = this.getBuilder( ).newInstance( this.getComponentId( ).getPartition( ), addr.getHostAddress( ), addr.getHostAddress( ),
                                                                    this.getComponentId( ).getPort( ) );
      this.serviceRegistry.register( config );
      return config;
    } else {
      throw new ServiceRegistrationException( "The component " + this.getName( ) + " is being skipped since it should only be run local to the cloud controller." );
    }
  }
  
  /**
   * Builds a Service instance for this component using the provided service
   * configuration.
   * 
   * @return Service instance of the service
   * @throws ServiceRegistrationException
   */
  public CheckedListenableFuture<ServiceConfiguration> loadService( final ServiceConfiguration config ) throws ServiceRegistrationException {
    this.lookupRegisteredService( config );
    if ( State.INITIALIZED.isIn( config ) ) {
      try {
        config.lookupStateMachine( ).transitionByName( Transition.LOADING ).get( );
      } catch ( Throwable ex ) {
        throw new ServiceRegistrationException( "Failed to load service: " + config + " because of: " + ex.getMessage( ), ex );
      }
    } 
    if ( State.LOADED.isIn( config ) ) {
      return Futures.predestinedFuture( config );
    } else {
      return Futures.predestinedFuture( config );
    }
  }
  
  public CheckedListenableFuture<ServiceConfiguration> disableTransition( ServiceConfiguration config ) {
    this.setServiceGoalState( config, State.DISABLED );
    return ServiceTransitions.transitionChain( config, State.DISABLED );
  }
  
  public CheckedListenableFuture<ServiceConfiguration> stopTransition( final ServiceConfiguration configuration ) {
    this.setServiceGoalState( configuration, State.STOPPED );
    return ServiceTransitions.transitionChain( configuration, State.STOPPED );
  }
  
  public void destroyTransition( final ServiceConfiguration configuration ) throws ServiceRegistrationException {
    try {
      Service service = null;
      if ( this.serviceRegistry.hasService( configuration ) ) {
        service = this.serviceRegistry.lookup( configuration );
      }
      try {
        ServiceTransitions.destroyTransitionChain( configuration ).get( );
      } catch ( ExecutionException ex1 ) {
        LOG.error( ex1 );
      } catch ( InterruptedException ex1 ) {
        LOG.error( ex1 );
      }
      try {
        EventRecord.caller( Component.class, EventType.COMPONENT_SERVICE_DESTROY, this.getName( ), configuration.getFullName( ),
                            configuration.getUri( ).toString( ) ).info( );
        this.serviceRegistry.deregister( configuration );
      } catch ( Throwable ex ) {
        throw new ServiceRegistrationException( "Failed to destroy service: " + configuration + " because of: " + ex.getMessage( ), ex );
      }
    } catch ( NoSuchElementException ex ) {
      throw new ServiceRegistrationException( "Failed to find service corresponding to: " + configuration, ex );
    }
  }
  
  public CheckedListenableFuture<ServiceConfiguration> enableTransition( final ServiceConfiguration configuration ) throws IllegalStateException {
    State goal = this.serviceRegistry.getServices( ).size( ) == 1
    ? State.ENABLED
      : State.DISABLED;
    this.setServiceGoalState( configuration, goal );
    return ServiceTransitions.transitionChain( configuration, goal );
  }
  
  public CheckedListenableFuture<ServiceConfiguration> startTransition( final ServiceConfiguration configuration ) throws IllegalStateException {
    State goal = this.serviceRegistry.getServices( ).size( ) == 1
    ? State.ENABLED
      : State.DISABLED;
    this.setServiceGoalState( configuration, goal );
    return ServiceTransitions.transitionChain( configuration, State.NOTREADY );
  }
  
  private void setServiceGoalState( final ServiceConfiguration configuration, final State goalState ) throws NoSuchElementException {
    Service service = null;
    if ( this.serviceRegistry.hasService( configuration ) ) {
      service = this.serviceRegistry.lookup( configuration );
      service.setGoal( goalState );
    } else {
      try {
        service = this.serviceRegistry.register( configuration );
        service.setGoal( goalState );
      } catch ( ServiceRegistrationException ex ) {
        LOG.error( ex , ex );
      }
    }
  }
  
  public NavigableSet<ServiceConfiguration> enabledServices( ) {
    return Sets.newTreeSet( Iterables.filter( this.serviceRegistry.getServices( ), Components.Predicates.enabledService( ) ) );
  }
  
  NavigableSet<ServiceConfiguration> enabledPartitionServices( final String partitionName ) {
    Iterable<ServiceConfiguration> services = Iterables.filter( this.serviceRegistry.getServices( ),
                                                                Predicates.and( Components.Predicates.enabledService( ),
                                                                                Components.Predicates.serviceInPartition( partitionName ) ) );
    return Sets.newTreeSet( services );
  }
  
  private ConcurrentNavigableMap<String, ServiceCheckRecord> errors = new ConcurrentSkipListMap<String, ServiceCheckRecord>( );
  
  public boolean hasService( ServiceConfiguration config ) {
    return this.serviceRegistry.hasService( config );
  }
  
  /**
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString( ) {
    return String.format( "Component %s available=%s local-service=%s\n",
                          this.identity.name( ), this.isAvailableLocally( ), this.serviceRegistry.hasLocalService( )
                            ? this.serviceRegistry.getLocalService( )
                            : "none" );
  }
  
  /**
   * Components are ordered by the lexicographic ordering of the name.
   * 
   * @see java.lang.Comparable#compareTo(java.lang.Object)
   */
  @Override
  public int compareTo( Component that ) {
    return this.getName( ).compareTo( that.getName( ) );
  }
  
  /**
   * @see java.lang.Object#hashCode()
   * @return
   */
  @Override
  public int hashCode( ) {
    final int prime = 31;
    int result = 1;
    result = prime * result + ( ( this.identity == null )
      ? 0
      : this.identity.hashCode( ) );
    return result;
  }
  
  /**
   * @see java.lang.Object#equals(java.lang.Object)
   * @param obj
   * @return
   */
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
    Component other = ( Component ) obj;
    if ( this.identity == null ) {
      if ( other.identity != null ) {
        return false;
      }
    } else if ( !this.identity.equals( other.identity ) ) {
      return false;
    }
    return true;
  }
  
  public Service lookupRegisteredService( final ServiceConfiguration config ) throws ServiceRegistrationException, NoSuchElementException {
    Service service = null;
    if ( ( config.isVmLocal( ) || Internets.testLocal( config.getHostName( ) ) ) && !this.serviceRegistry.hasLocalService( ) ) {
      service = this.serviceRegistry.register( config );
    } else if ( this.serviceRegistry.hasService( config ) ) {
      service = this.serviceRegistry.lookup( config );
    } else {
      service = this.serviceRegistry.register( config );
    }
    return service;
  }
  
  class ServiceRegistry {
    private final AtomicReference<Service>           localService = new AtomicReference( null );
    private final Map<ServiceConfiguration, Service> services     = Maps.newConcurrentMap( );
    
    public boolean hasLocalService( ) {
      return !Component.this.identity.runLimitedServices( ) && ( this.localService.get( ) != null && !( this.localService.get( ) instanceof MissingService ) );
    }
    
    public Service getLocalService( ) {
      Service ret = this.localService.get( );
      if ( ret == null ) {
        throw new NoSuchElementException( "Attempt to access a local service reference when none exists for: " + Component.this.toString( ) );
      } else {
        return ret;
      }
    }
    
    /**
     * Obtain a snapshot of the current service state. Note that this method creates a new set and
     * changes to the returned set will not be reflected in the underlying services set.
     * 
     * @return {@link NavigableSet<Service>} of the registered service of this {@link Component}
     *         type.
     */
    public NavigableSet<ServiceConfiguration> getServices( ) {
      return Sets.newTreeSet( this.services.keySet( ) );
    }
    
    List<ServiceInfoType> getServiceInfos( final String localhostAddr ) {
      List<ServiceInfoType> serviceSnapshot = Lists.newArrayList( );
      for ( final Service s : this.services.values( ) ) {
        if ( State.ENABLED.equals( s.getServiceConfiguration( ).lookupState( ) ) ) {
          serviceSnapshot.add( 0, new ServiceInfoType( ) {
            {
              setPartition( s.getServiceConfiguration( ).getPartition( ) );
              setName( s.getServiceConfiguration( ).getName( ) );
              setType( Component.this.getName( ) );
              if ( s.getServiceConfiguration( ).isVmLocal( ) ) {
                getUris( ).add( s.getComponentId( ).makeExternalRemoteUri( localhostAddr, s.getComponentId( ).getPort( ) ).toASCIIString( ) );
              } else {
                getUris( ).add( s.getServiceConfiguration( ).getUri( ).toASCIIString( ) );
              }
            }
          } );
        } else {
          serviceSnapshot.add( new ServiceInfoType( ) {
            {
              setPartition( s.getServiceConfiguration( ).getPartition( ) );
              setName( s.getServiceConfiguration( ).getName( ) );
              setType( Component.this.getName( ) );
              if ( s.getServiceConfiguration( ).isVmLocal( ) ) {
                getUris( ).add( s.getComponentId( ).makeExternalRemoteUri( localhostAddr, s.getComponentId( ).getPort( ) ).toASCIIString( ) );
              } else {
                getUris( ).add( s.getServiceConfiguration( ).getUri( ).toASCIIString( ) );
              }
            }
          } );
        }
      }
      return serviceSnapshot;
    }
    
    /**
     * Deregisters the service with the provided {@link FullName}.
     * 
     * @param fullName
     * @return {@link Service} instance of the deregistered service.
     * @throws NoSuchElementException if no {@link Service} is registered with the provided
     *           {@link FullName}
     */
    public Service deregister( ServiceConfiguration config ) throws NoSuchElementException {
      Service ret = this.services.remove( config );
      if ( ret == null ) {
        throw new NoSuchElementException( "Failed to lookup service corresponding to full-name: " + config );
      } else if ( ret.getServiceConfiguration( ).isVmLocal( ) ) {
        this.localService.compareAndSet( ret, null );
      }
      try {
        ret.cleanUp( );
      } catch ( Exception ex ) {
        LOG.error( ex , ex );
      }
      return ret;
    }
    
    /**
     * Returns the {@link Service} instance which was registered with the provided
     * {@link ServiceConfiguration}, if it exists. If a service with the given name
     * does not exist a
     * NoSuchElementException is thrown.
     * 
     * @see #lookup(FullName)
     * @param configuration
     * @return {@link Service} corresponding to provided the {@link ServiceConfiguration}
     * @throws NoSuchElementException
     */
    public Service lookup( ServiceConfiguration config ) throws NoSuchElementException {
      if ( !this.services.containsKey( config ) ) {
        throw new NoSuchElementException( "Failed to lookup service corresponding to service configuration: " + config );
      } else {
        return this.services.get( config );
      }
    }
        
    /**
     * List the services registered within a give partition.
     * 
     * @param partition - name of the partition.
     * @return a new set with the list of service registered in the give partition.
     * @throws NoSuchElementException if no {@link Service}s are registered within the given
     *           partition.
     */
    public NavigableSet<Service> lookupPartition( String partition ) throws NoSuchElementException {
      NavigableSet<Service> partitionServices = Sets.newTreeSet( );
      for ( Service s : this.services.values( ) ) {
        if ( partition.equals( s.getServiceConfiguration( ).getPartition( ) ) ) {
          partitionServices.add( s );
        }
      }
      if ( partitionServices.isEmpty( ) ) {
        throw new NoSuchElementException( "No services were found in partition: " + partition + " for component " + Component.this.getComponentId( ) );
      }
      return partitionServices;
    }
    
    /**
     * Register the given {@link Service} with the registry. Only used internally.
     * 
     * @param service
     * @throws ServiceRegistrationException 
     */
    Service register( ServiceConfiguration config ) throws ServiceRegistrationException {
      Service service = Services.newServiceInstance( config );
      if ( config.isVmLocal( ) || Internets.testLocal( config.getHostName( ) ) ) {
        this.localService.set( service );
      }
      this.services.put( config, service );
      EventRecord.caller( Component.class, EventType.COMPONENT_SERVICE_REGISTERED,
                          Component.this.getName( ),
                          service.getServiceConfiguration( ).isVmLocal( )
                            ? "local"
                            : "remote",
                          config.getName( ), config.getUri( ) ).info( );
      return service;
    }
    
    /**
     * Returns the {@link Service} instance which was registered with the provided service name if
     * it
     * exists. If a service with the given name does not exist a NoSuchElementException is thrown.
     * 
     * @param name - the name used to register the specific service.
     * @return
     * @throws NoSuchElementException
     * @deprecated {@link #getServices(ServiceConfiguration)}
     */
    public ServiceConfiguration getService( String name ) throws NoSuchElementException {
      Assertions.assertNotNull( name );
      for ( ServiceConfiguration s : this.services.keySet( ) ) {
        if ( s.getName( ).equals( name ) ) {
          return s;
        }
      }
      throw new NoSuchElementException( "No service found matching name: " + name + " for component: " + Component.this.getName( ) );
    }
    
    /**
     * TODO: DOCUMENT Component.java
     * 
     * @param config
     * @return
     */
    public boolean hasService( ServiceConfiguration config ) {
      return this.services.containsKey( config.getFullName( ) );
    }
  }
  
  public ServiceConfiguration getLocalServiceConfiguration( ) {
    return this.serviceRegistry.getLocalService( ).getServiceConfiguration( );
  }
  
}
