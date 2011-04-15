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
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.log4j.Logger;
import com.eucalyptus.bootstrap.Bootstrap;
import com.eucalyptus.component.ServiceEvents.ServiceEvent;
import com.eucalyptus.empyrean.Empyrean;
import com.eucalyptus.empyrean.ServiceInfoType;
import com.eucalyptus.records.EventRecord;
import com.eucalyptus.records.EventType;
import com.eucalyptus.system.Threads;
import com.eucalyptus.util.Assertions;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.FullName;
import com.eucalyptus.util.HasName;
import com.eucalyptus.util.Internets;
import com.eucalyptus.util.async.CheckedListenableFuture;
import com.eucalyptus.util.async.Futures;
import com.eucalyptus.util.fsm.TransitionFuture;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

public class Component implements HasName<Component> {
  private static Logger               LOG   = Logger.getLogger( Component.class );
  private final ComponentId           identity;
  private final ServiceRegistry       serviceRegistry;
  private final ComponentBootstrapper bootstrapper;
  
  public enum State {
    MISSING, BROKEN, PRIMORDIAL, INITIALIZED, LOADED, STOPPED, NOTREADY, DISABLED, ENABLED;
  }
  
  public enum Transition {
    INITIALIZING, LOADING, STARTING, READY_CHECK, STOPPING, ENABLING, ENABLED_CHECK, DISABLING, DISABLED_CHECK, DESTROYING;
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
   * @return
   * @see com.eucalyptus.component.Component.ServiceRegistry#getServices()
   */
  public NavigableSet<Service> lookupServices( ) {
    return this.serviceRegistry.getServices( );
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
    return this.getLocalService( ).getState( );
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
  
  public List<ServiceConfiguration> lookupServiceConfigurations( ) throws ServiceRegistrationException {
    return Lists.newArrayList( Iterables.transform( this.serviceRegistry.getServices( ), new Function<Service, ServiceConfiguration>( ) {
      
      @Override
      public ServiceConfiguration apply( Service arg0 ) {
        return arg0.getServiceConfiguration( );
      }
    } ) );
  }
  
  /**
   * TODO: DOCUMENT Component.java
   * 
   * @param hostName
   * @param port
   * @return
   */
  public URI getUri( String hostName, Integer port ) {
    return this.getComponentId( ).makeRemoteUri( hostName, port );
  }
  
  public URI getUri( ) {
    NavigableSet<Service> services = this.serviceRegistry.getServices( );
    if ( this.getComponentId( ).isCloudLocal( ) && services.size( ) != 1 && !"db".equals( this.getName( ) ) ) {
      throw new RuntimeException( "Cloud local component has " + services.size( ) + " registered services (Should be exactly 1): " + this + " "
                                  + services.toString( ) );
    } else if ( this.getComponentId( ).isCloudLocal( ) && services.size( ) != 1 && "db".equals( this.getName( ) ) ) {
      return this.getComponentId( ).getLocalEndpointUri( );
    } else if ( this.getComponentId( ).isCloudLocal( ) && services.size( ) == 1 ) {
      return services.first( ).getServiceConfiguration( ).getUri( );
    } else {
      for ( Service s : services ) {
        if ( s.isLocal( ) ) {
          return s.getServiceConfiguration( ).getUri( );
        }
      }
      throw new RuntimeException( "Attempting to get the URI for a service which is either not a singleton or has no locally defined service endpoint." );
    }
  }
  
  public Boolean hasLocalService( ) {
    return this.serviceRegistry.hasLocalService( );
  }
  
  public Boolean hasServiceEnabled( ) {
    return State.ENABLED.equals( this.getState( ) );
  }
  
  public Boolean isRunningLocally( ) {
    return State.ENABLED.equals( this.getState( ) );// && this.serviceRegistry.hasLocalService( );
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
  
  /**
   * @param fullName
   * @return
   * @throws NoSuchElementException
   * @see com.eucalyptus.component.Component.ServiceRegistry#lookup(com.eucalyptus.util.FullName)
   */
  public Service lookupService( FullName fullName ) throws NoSuchElementException {
    return this.serviceRegistry.lookup( fullName );
  }
  
  @Deprecated
  public Service lookupService( String name ) {
    return this.serviceRegistry.getService( name );
  }
  
  public NavigableSet<Service> lookupServices( String partition ) {
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
      String fakeName = Internets.localhost( );
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
      throw Exceptions.debug( new ServiceRegistrationException( "The component " + this.getName( ) + " cannot be loaded since it is disabled." ) );
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
    Service service = null;
    if ( this.serviceRegistry.hasService( config ) ) {
      service = this.serviceRegistry.lookup( config );
    } else {
      service = this.serviceRegistry.register( config );
    }
    if ( State.INITIALIZED.equals( service.getState( ) ) ) {
      try {
        return service.transition( Transition.LOADING );
      } catch ( Throwable ex ) {
        throw new ServiceRegistrationException( "Failed to load service: " + config + " because of: " + ex.getMessage( ), ex );
      }
    } else if ( State.LOADED.equals( service.getState( ) ) ) {
      return Futures.predestinedFuture( service.getServiceConfiguration( ) );
    } else {
      return Futures.predestinedFuture( service.getServiceConfiguration( ) );
    }
  }
  
  public CheckedListenableFuture<ServiceConfiguration> startService( final ServiceConfiguration config ) throws ServiceRegistrationException {
    EventRecord.caller( Component.class, EventType.COMPONENT_SERVICE_START, this.getName( ), config.getName( ), config.getUri( ).toString( ) ).info( );
    final Service service = this.serviceRegistry.lookup( config );
    service.setGoal( this.serviceRegistry.getServices( ).isEmpty( )
      //TODO:GRZE: initial state setup happens here
      ? State.ENABLED
      : State.DISABLED );
    if ( service.getState( ).equals( State.LOADED ) ) {
      final CheckedListenableFuture<ServiceConfiguration> future = Futures.newGenericFuture( );
      try {
        service.transition( Transition.STARTING ).addListener( new Callable<ServiceConfiguration>( ) {
          @Override
          public ServiceConfiguration call( ) {
            try {
              service.transition( State.DISABLED );
              future.set( service.getServiceConfiguration( ) );
            } catch ( Throwable ex ) {
              future.setException( ex );
              Exceptions.trace( new ServiceRegistrationException( "Failed to mark service disabled: " + config + " because of: " + ex.getMessage( ), ex ) );
            }
            return service.getServiceConfiguration( );
          }
        } );
      } catch ( Throwable ex ) {
        future.setException( new ServiceRegistrationException( "Failed to start service: " + config + " because of: " + ex.getMessage( ), ex ) );
      }
      return future;
    } else if ( service.getState( ).equals( State.NOTREADY ) ) {
      try {
        return service.transition( State.DISABLED );
      } catch ( Throwable ex ) {
        final CheckedListenableFuture<ServiceConfiguration> future = Futures.newGenericFuture( );
        future.setException( new ServiceRegistrationException( "Failed to mark service disabled: " + config + " because of: " + ex.getMessage( ), ex ) );
        return future;
      }
    } else {
      return Futures.predestinedFuture( service.getServiceConfiguration( ) );
    }
  }
  
  public CheckedListenableFuture<ServiceConfiguration> enableService( final ServiceConfiguration config ) throws ServiceRegistrationException {
    EventRecord.caller( Component.class, EventType.COMPONENT_SERVICE_ENABLED, this.getName( ), config.getName( ), config.getUri( ).toString( ) ).info( );
    final Service service = this.serviceRegistry.lookup( config );
    service.setGoal( State.ENABLED );
    if ( State.NOTREADY.equals( service.getState( ) ) ) {
      final CheckedListenableFuture<ServiceConfiguration> future = new TransitionFuture<ServiceConfiguration>( );
      try {
        service.transition( Transition.READY_CHECK ).addListener( new Callable<ServiceConfiguration>( ) {
          @Override
          public ServiceConfiguration call( ) {
            try {
              service.transition( State.ENABLED );
              future.set( service.getServiceConfiguration( ) );
            } catch ( Throwable ex ) {
              future.setException( ex );
              Exceptions.trace( new ServiceRegistrationException( "Failed to mark service enabled: " + config + " because of: " + ex.getMessage( ), ex ) );
            }
            return service.getServiceConfiguration( );
          }
        } );
      } catch ( Throwable ex ) {
        future.setException( new ServiceRegistrationException( "Failed to perform ready-check for service: " + config + " because of: " + ex.getMessage( ),
                                                               ex ) );
      }
      return future;
    } else if ( State.DISABLED.equals( service.getState( ) ) ) {
      try {
        CheckedListenableFuture<ServiceConfiguration> ret = service.transition( State.ENABLED );
        return ret;
      } catch ( Throwable ex ) {
        final CheckedListenableFuture<ServiceConfiguration> future = Futures.newGenericFuture( );
        future.setException( new ServiceRegistrationException( "Failed to mark service enabled: " + config + " because of: " + ex.getMessage( ), ex ) );
        return future;
      }
    } else {
      return Futures.predestinedFuture( service.getServiceConfiguration( ) );
    }
  }
  
  public CheckedListenableFuture<ServiceConfiguration> disableService( ServiceConfiguration config ) throws ServiceRegistrationException {
    EventRecord.caller( Component.class, EventType.COMPONENT_SERVICE_DISABLED, this.getName( ), config.getName( ), config.getUri( ).toString( ) ).info( );
    final Service service = this.serviceRegistry.lookup( config );
    try {
      return service.transition( State.DISABLED );
    } catch ( Throwable ex ) {
      throw new ServiceRegistrationException( "Failed to disable service: " + config + " because of: " + ex.getMessage( ), ex );
    }
  }
  
  public CheckedListenableFuture<ServiceConfiguration> stopService( final ServiceConfiguration config ) throws ServiceRegistrationException {
    EventRecord.caller( Component.class, EventType.COMPONENT_SERVICE_STOPPED, this.getName( ), config.getName( ), config.getUri( ).toString( ) ).info( );
    final Service service = this.serviceRegistry.lookup( config );
    if ( State.ENABLED.equals( service.getState( ) ) ) {
      try {
        final CheckedListenableFuture<ServiceConfiguration> future = new TransitionFuture<ServiceConfiguration>( );
        service.transition( State.DISABLED ).addListener( new Runnable( ) {
          @Override
          public void run( ) {
            try {
              service.transition( State.STOPPED );
              future.set( service.getServiceConfiguration( ) );
            } catch ( Throwable ex ) {
              Exceptions.trace( new ServiceRegistrationException( "Failed to stop service: " + config + " because of: " + ex.getMessage( ), ex ) );
              future.setException( ex );
            }
          }
        }, Threads.currentThreadExecutor( ) );
        return future;
      } catch ( Throwable ex ) {
        throw new ServiceRegistrationException( "Failed to disable service: " + config + " because of: " + ex.getMessage( ), ex );
      }
    } else if ( State.DISABLED.equals( service.getState( ) ) || State.NOTREADY.equals( service.getState( ) ) ) {
      try {
        return service.transition( State.STOPPED );
      } catch ( Throwable ex ) {
        throw new ServiceRegistrationException( "Failed to stop service: " + config + " because of: " + ex.getMessage( ), ex );
      }
    } else {
      return Futures.predestinedFuture( service.getServiceConfiguration( ) );
    }
  }
  
  public CheckedListenableFuture<ServiceConfiguration> destroyService( final ServiceConfiguration config ) throws ServiceRegistrationException {
    try {
      Service service = this.serviceRegistry.deregister( config );
      if ( State.STOPPED.ordinal( ) < this.getLocalService( ).getState( ).ordinal( ) ) {
        this.stopService( config );
      }
      try {
        EventRecord.caller( Component.class, EventType.COMPONENT_SERVICE_DESTROY, this.getName( ), service.getName( ),
                            service.getServiceConfiguration( ).getUri( ).toString( ) ).info( );
        return this.getLocalService( ).transition( Transition.DESTROYING );
      } catch ( Throwable ex ) {
        throw new ServiceRegistrationException( "Failed to destroy service: " + config + " because of: " + ex.getMessage( ), ex );
      }
    } catch ( NoSuchElementException ex ) {
      throw new ServiceRegistrationException( "Failed to find service corresponding to: " + config, ex );
    }
  }
  
  private final Callable<ServiceConfiguration> noTransition = new Callable<ServiceConfiguration>( ) {
                                                              
                                                              @Override
                                                              public ServiceConfiguration call( ) throws Exception {
                                                                return Component.this.getLocalService( ).getServiceConfiguration( );
                                                              }
                                                            };
  
  private Callable<ServiceConfiguration> makeEnableCallable( final ServiceConfiguration configuration, final CheckedListenableFuture<ServiceConfiguration> transitionFuture ) {
    return new Callable<ServiceConfiguration>( ) {
      @Override
      public ServiceConfiguration call( ) throws Exception {
        EventRecord.here( Component.class, EventType.CALLBACK, EventType.COMPONENT_SERVICE_ENABLE.toString( ), configuration.getFullName( ).toString( ) ).debug( );
        try {
          transitionFuture.set( Component.this.enableService( configuration ).get( ) );
        } catch ( Throwable ex ) {
          transitionFuture.setException( ex );
          LOG.error( ex );
        }
        return Component.this.getLocalService( ).getServiceConfiguration( );
      }
    };
  }
  
  private Callable<ServiceConfiguration> makeStartCallable( final ServiceConfiguration configuration, final CheckedListenableFuture<ServiceConfiguration> transitionFuture, final Callable<ServiceConfiguration> subsequentTransition ) {
    return new Callable<ServiceConfiguration>( ) {
      @Override
      public ServiceConfiguration call( ) throws ServiceRegistrationException {
        EventRecord.here( Component.class, EventType.CALLBACK, EventType.COMPONENT_SERVICE_START.toString( ), configuration.getFullName( ).toString( ) ).debug( );
        try {
          if ( subsequentTransition != null ) {
            Component.this.startService( configuration ).addListener( subsequentTransition );
          } else {
            try {
              transitionFuture.set( Component.this.startService( configuration ).get( ) );
            } catch ( Throwable ex ) {
              transitionFuture.setException( ex );
              LOG.error( ex );
            }
          }
        } catch ( ServiceRegistrationException ex ) {
          transitionFuture.setException( ex );
          LOG.error( ex, ex );
          throw ex;
        }
        return Component.this.getLocalService( ).getServiceConfiguration( );
      }
    };
  }
  
  private Callable<ServiceConfiguration> makeLoadCallable( final ServiceConfiguration configuration, final CheckedListenableFuture<ServiceConfiguration> transitionFuture, final Callable<ServiceConfiguration> subsequentTransition ) {
    return new Callable<ServiceConfiguration>( ) {
      @Override
      public ServiceConfiguration call( ) throws ServiceRegistrationException {
        EventRecord.here( Component.class, EventType.CALLBACK, EventType.COMPONENT_SERVICE_LOAD.toString( ), configuration.getFullName( ).toString( ) ).debug( );
        try {
          if ( subsequentTransition != null ) {
            Component.this.loadService( configuration ).addListener( subsequentTransition );
          } else {
            try {
              transitionFuture.set( Component.this.loadService( configuration ).get( ) );
            } catch ( Throwable ex ) {
              transitionFuture.setException( ex );
              LOG.error( ex );
            }
          }
        } catch ( ServiceRegistrationException ex ) {
          transitionFuture.setException( ex );
          LOG.error( ex, ex );
          throw ex;
        }
        return Component.this.getLocalService( ).getServiceConfiguration( );
      }
    };
  }
  
  public CheckedListenableFuture<ServiceConfiguration> startTransition( final ServiceConfiguration configuration ) throws IllegalStateException {
    final CheckedListenableFuture<ServiceConfiguration> transitionFuture = Futures.newGenericFuture( );
    Callable<ServiceConfiguration> transition = null;
    switch ( this.getState( ) ) {
      case LOADED:
      case STOPPED:
        transition = makeStartCallable( configuration, null, makeEnableCallable( configuration, transitionFuture ) );
        break;
      case INITIALIZED:
        transition = makeLoadCallable( configuration, null,
                                       makeStartCallable( configuration, null,
                                                          makeEnableCallable( configuration, transitionFuture ) ) );
        break;
      case DISABLED:
      case ENABLED:
      case NOTREADY:
        transition = noTransition;
        transitionFuture.set( this.getLocalService( ).getServiceConfiguration( ) );
        break;
      default:
        throw new IllegalStateException( "Failed to find transition for current component state: " + this.toString( ) );
    }
    Threads.lookup( Empyrean.class ).submit( transition );
    return transitionFuture;
  }
  
  public CheckedListenableFuture<ServiceConfiguration> enableTransition( final ServiceConfiguration configuration ) throws IllegalStateException {
    final CheckedListenableFuture<ServiceConfiguration> transitionFuture = Futures.newGenericFuture( );
    Callable<ServiceConfiguration> transition = null;
    switch ( this.getState( ) ) {
      case NOTREADY:
      case DISABLED:
        transition = makeEnableCallable( configuration, transitionFuture );
        break;
      case LOADED:
      case STOPPED:
        transition = makeStartCallable( configuration, null,
                                        makeEnableCallable( configuration, transitionFuture ) );
        break;
      case INITIALIZED:
        transition = makeLoadCallable( configuration, null,
                                       makeStartCallable( configuration, null,
                                                          makeEnableCallable( configuration, transitionFuture ) ) );
        break;
      case ENABLED:
        transition = noTransition;
        transitionFuture.set( this.getLocalService( ).getServiceConfiguration( ) );
        break;
      default:
        throw new IllegalStateException( "Failed to find transition for current component state: " + this.toString( ) );
    }
    Threads.lookup( Empyrean.class ).submit( transition );
    return transitionFuture;
  }
  
  /**
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString( ) {
    return String.format( "Component %s available=%s mark-remote=%s local-service=%s\n",
                          this.identity.name( ), this.isAvailableLocally( ), this.isRunningRemoteMode( ), this.serviceRegistry.hasLocalService( ) ? this.serviceRegistry.getLocalService( ) : "none" );
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
  
  public NavigableSet<Service> getServices( ) {
    return this.lookupServices( );
  }
  
  public Iterable<Service> enabledServices( ) {
    return Iterables.filter( this.serviceRegistry.getServices( ), new Predicate<Service>( ) {
      
      @Override
      public boolean apply( Service arg0 ) {
        return Component.State.ENABLED.equals( arg0.getState( ) );
      }
    } );
  }
  
  private ConcurrentNavigableMap<String, ServiceEvents.ServiceEvent> errors = new ConcurrentSkipListMap<String, ServiceEvents.ServiceEvent>( );
  
  public void submitError( Throwable t ) {
    ServiceConfiguration config = this.hasLocalService( )
      ? this.getLocalService( ).getServiceConfiguration( )
      : ServiceConfigurations.createEphemeral( this, Internets.localhostAddress( ) );
    ServiceEvent e = ServiceEvents.createError( this.getLocalService( ).getServiceConfiguration( ), t );
    this.errors.put( e.getUuid( ), e );
  }
  
  public boolean hasService( ServiceConfiguration config ) {
    return this.serviceRegistry.hasService( config );
  }
  
  class ServiceRegistry {
    private final AtomicReference<Service> localService = new AtomicReference( null );
    private final Map<FullName, Service>   services     = Maps.newConcurrentMap( );
    
    public boolean hasLocalService( ) {
      return ( this.localService.get( ) != null && !( this.localService.get( ) instanceof MissingService ) );
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
    public NavigableSet<Service> getServices( ) {
      return Sets.newTreeSet( this.services.values( ) );
    }
    
    List<ServiceInfoType> getServiceInfos( final String localhostAddr ) {
      List<ServiceInfoType> serviceSnapshot = Lists.newArrayList( );
      for ( final Service s : this.services.values( ) ) {
        if ( State.ENABLED.equals( s.getState( ) ) ) {
          serviceSnapshot.add( 0, new ServiceInfoType( ) {
            {
              setPartition( s.getServiceConfiguration( ).getPartition( ) );
              setName( s.getServiceConfiguration( ).getName( ) );
              setType( Component.this.getName( ) );
              if ( s.getServiceConfiguration( ).getUri( ).toASCIIString( ).startsWith( "vm" ) ) {
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
              if ( s.getServiceConfiguration( ).getUri( ).toASCIIString( ).startsWith( "vm" ) ) {
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
    
    public Service deregister( ServiceConfiguration config ) throws NoSuchElementException {
      return this.deregister( config.getFullName( ) );
    }
    
    /**
     * Deregisters the service with the provided {@link FullName}.
     * 
     * @param fullName
     * @return {@link Service} instance of the deregistered service.
     * @throws NoSuchElementException if no {@link Service} is registered with the provided
     *           {@link FullName}
     */
    public Service deregister( FullName fullName ) throws NoSuchElementException {
      Service ret = this.services.remove( fullName );
      if ( ret == null ) {
        throw new NoSuchElementException( "Failed to lookup service corresponding to full-name: " + fullName );
      } else if ( ret.getServiceConfiguration( ).isLocal( ) ) {
        this.localService.compareAndSet( ret, null );
      }
      return ret;
    }
    
    /**
     * Returns the {@link Service} instance which was registered with the provided
     * {@link ServiceConfiguration}, if it exists. If a service with the given name does not exist a
     * NoSuchElementException is thrown.
     * 
     * @see #lookup(FullName)
     * @param configuration
     * @return {@link Service} corresponding to provided the {@link ServiceConfiguration}
     * @throws NoSuchElementException
     */
    public Service lookup( ServiceConfiguration config ) throws NoSuchElementException {
      return this.lookup( config.getFullName( ) );
    }
    
    /**
     * Returns the {@link Service} instance which was registered with the provided {@link FullName},
     * if it exists. If a service with the given name does not exist a
     * NoSuchElementException is thrown.
     * 
     * @param configuration
     * @return {@link Service} corresponding to provided the {@link FullName}
     * @throws NoSuchElementException
     */
    public Service lookup( FullName fullName ) throws NoSuchElementException {
      if ( !this.services.containsKey( fullName ) ) {
        throw new NoSuchElementException( "Failed to lookup service corresponding to full-name: " + fullName );
      } else {
        return this.services.get( fullName );
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
     */
    Service register( ServiceConfiguration config ) {
      Service service = Services.newServiceInstance( config );
      if ( config.isLocal( ) ) {
        this.localService.set( service );
      }
      this.services.put( config.getFullName( ), service );
      EventRecord.caller( Component.class, EventType.COMPONENT_SERVICE_REGISTERED,
                          Component.this.getName( ),
                          service.getServiceConfiguration( ).isLocal( )
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
    public Service getService( String name ) throws NoSuchElementException {
      Assertions.assertNotNull( name );
      for ( Service s : this.services.values( ) ) {
        if ( s.getServiceConfiguration( ).getName( ).equals( name ) ) {
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
  
}
