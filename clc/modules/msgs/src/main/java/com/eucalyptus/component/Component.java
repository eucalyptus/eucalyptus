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
 * THE REGENTS’ DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
 * OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
 * WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
 * ANY SUCH LICENSES OR RIGHTS.
 *******************************************************************************
 * @author chris grzegorczyk <grze@eucalyptus.com>
 */
package com.eucalyptus.component;

import java.net.URI;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.log4j.Logger;
import com.eucalyptus.bootstrap.BootstrapException;
import com.eucalyptus.empyrean.Empyrean;
import com.eucalyptus.event.ClockTick;
import com.eucalyptus.event.Event;
import com.eucalyptus.event.EventListener;
import com.eucalyptus.event.ListenerRegistry;
import com.eucalyptus.records.EventRecord;
import com.eucalyptus.records.EventType;
import com.eucalyptus.system.Threads;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.HasName;
import com.eucalyptus.util.NetworkUtil;
import com.eucalyptus.util.fsm.ExistingTransitionException;
import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import edu.ucsb.eucalyptus.msgs.ServiceInfoType;

/**
 * TODO: DOCUMENT. yes pls.
 */
public class Component implements ComponentInformation, HasName<Component> {
  private static Logger LOG = Logger.getLogger( Component.class );
  
  public enum State {
    BROKEN, PRIMORDIAL, INITIALIZED, LOADED, STOPPED, NOTREADY, DISABLED, ENABLED;
  }
  
  public enum Transition {
    INITIALIZING, LOADING, STARTING, READY_CHECK, STOPPING, ENABLING, ENABLED_CHECK, DISABLING, DISABLED_CHECK, DESTROYING;
    public void transit( Component c ) {
      if ( c.isAvailableLocally( ) ) {
        try {
          c.stateMachine.transition( Transition.this );
        } catch ( Throwable ex ) {
          LOG.error( ex, ex );
          throw new IllegalStateException( "Error while applying transtition " + this.name( ) + " to " + c.getName( ) + " currently in state "
                                           + c.stateMachine.toString( ), ex );
        }
      }
    }
  }
  
  private final String                   name;
  private final ComponentId              identity;
  private final AtomicBoolean            enabled      = new AtomicBoolean( false );
  private final AtomicBoolean            local        = new AtomicBoolean( false );
  private final Map<String, Service>     services     = Maps.newConcurrentHashMap( );
  private final ComponentBootstrapper    bootstrapper;
  private final ComponentState           stateMachine;
  private final AtomicReference<Service> localService = new AtomicReference( null );
  
  Component( ComponentId componentId ) throws ServiceRegistrationException {
    this.name = componentId.getName( );
    this.identity = componentId;
    /** remove **/
    if ( System.getProperty( "euca.disable." + this.name ) == null ) {
      this.enabled.set( true );
      if ( System.getProperty( "euca.remote." + this.name ) == null ) {
        this.local.set( true );
      }
    }
    this.bootstrapper = new ComponentBootstrapper( this );
    this.stateMachine = new ComponentState( this );
  }
  
  public Service getLocalService( ) {
    return this.localService.get( );
  }
  
  /**
   * Builds a Service instance for this component using the local default
   * values.
   * 
   * @return Service instance of the service
   * @throws ServiceRegistrationException
   */
  public void initService( ) throws ServiceRegistrationException {
    if ( this.enabled.get( ) ) {
      ServiceConfiguration config = this.getBuilder( ).toConfiguration( this.getIdentity( ).getLocalEndpointUri( ) );
      Service service = new Service( this, config );
      this.setupService( service );
      
    } else {
      throw new ServiceRegistrationException( "The component " + this.getName( ) + " cannot be loaded since it is disabled." );
    }
  }
  
  /**
   * Builds a Service instance for this component using the provided service
   * configuration.
   * 
   * @return Service instance of the service
   * @throws ServiceRegistrationException
   */
  public void loadService( ServiceConfiguration config ) throws ServiceRegistrationException {
    Service service = new Service( this, config );
    this.setupService( service );
    if ( service.isLocal( ) && State.INITIALIZED.equals( this.getState( ) ) ) {
      try {
        this.stateMachine.transition( Transition.LOADING );
      } catch ( IllegalStateException ex ) {
        LOG.error( ex, ex );
      } catch ( NoSuchElementException ex ) {
        LOG.error( ex, ex );
      } catch ( ExistingTransitionException ex ) {
        LOG.error( ex, ex );
      }
    }
  }
  
  /**
   * 
   * @param config
   * @param service
   * @return
   * @throws ServiceRegistrationException
   */
  private Service setupService( Service service ) throws ServiceRegistrationException {
    if ( service.getServiceConfiguration( ).isLocal( ) ) {
      this.localService.set( service );
    }
    this.services.put( service.getName( ), service );
    Components.register( service );
    EventRecord.caller( Component.class, EventType.COMPONENT_SERVICE_REGISTERED,
                        this.getName( ),
                        service.getServiceConfiguration( ).isLocal( )
                          ? "local"
                          : "remote",
                        service.getName( ), service.getUri( ), service.getDispatcher( ) ).info( );
    return service;
  }
  
  public boolean inState( State queryState ) {
    return queryState.equals( this.getState( ) );
  }
  
  public void startService( ServiceConfiguration service ) throws ServiceRegistrationException {
    EventRecord.caller( Component.class, EventType.COMPONENT_SERVICE_START, this.getName( ), service.getName( ), service.getUri( ).toString( ) ).info( );
    if ( service.isLocal( ) && this.inState( State.LOADED ) ) {
      try {
        this.stateMachine.transition( Transition.STARTING );
      } catch ( IllegalStateException ex1 ) {
        LOG.error( ex1, ex1 );
      } catch ( NoSuchElementException ex1 ) {
        LOG.error( ex1, ex1 );
      } catch ( ExistingTransitionException ex1 ) {
        LOG.error( ex1, ex1 );
      }
      if ( this.inState( State.NOTREADY ) ) {
        try {
          this.stateMachine.transition( State.DISABLED );
        } catch ( IllegalStateException ex ) {
          LOG.error( ex, ex );
        } catch ( NoSuchElementException ex ) {
          LOG.error( ex, ex );
        } catch ( ExistingTransitionException ex ) {
          LOG.error( ex, ex );
        }
      }
    } else {
      this.getBuilder( ).fireStart( service );
    }
  }
  
  public void enableService( ServiceConfiguration service ) throws ServiceRegistrationException {
    EventRecord.caller( Component.class, EventType.COMPONENT_SERVICE_ENABLED, this.getName( ), service.getName( ), service.getUri( ).toString( ) ).info( );
    if ( service.isLocal( ) ) {
      if ( State.NOTREADY.equals( this.stateMachine.getState( ) ) ) {
        try {
          this.stateMachine.transition( Transition.READY_CHECK );
        } catch ( IllegalStateException ex ) {
          LOG.error( ex, ex );
        } catch ( NoSuchElementException ex ) {
          LOG.error( ex, ex );
        } catch ( ExistingTransitionException ex ) {
          LOG.error( ex, ex );
        }
      }
      try {
        this.stateMachine.transition( State.ENABLED );
      } catch ( IllegalStateException ex ) {
        LOG.error( ex, ex );
      } catch ( NoSuchElementException ex ) {
        LOG.error( ex, ex );
      } catch ( ExistingTransitionException ex ) {
        LOG.error( ex, ex );
      }
    } else {
      this.getBuilder( ).fireEnable( service );
    }
  }
  
  public void disableService( ServiceConfiguration service ) throws ServiceRegistrationException {
    EventRecord.caller( Component.class, EventType.COMPONENT_SERVICE_DISABLED, this.getName( ), service.getName( ), service.getUri( ).toString( ) ).info( );
    if ( service.isLocal( ) ) {
      try {
        this.stateMachine.transition( State.DISABLED );
      } catch ( IllegalStateException ex ) {
        LOG.error( ex, ex );
      } catch ( NoSuchElementException ex ) {
        LOG.error( ex, ex );
      } catch ( ExistingTransitionException ex ) {
        LOG.error( ex, ex );
      }
    } else {
      this.getBuilder( ).fireDisable( service );
    }
  }
  
  public void stopService( ServiceConfiguration service ) throws ServiceRegistrationException {
    EventRecord.caller( Component.class, EventType.COMPONENT_SERVICE_STOP, this.getName( ), service.getName( ), service.getUri( ).toString( ) ).info( );
    if ( service.isLocal( ) ) {
      if ( State.ENABLED.equals( this.stateMachine.getState( ) ) ) {
        try {
          this.stateMachine.transition( State.DISABLED );
        } catch ( IllegalStateException ex ) {
          LOG.error( ex, ex );
        } catch ( NoSuchElementException ex ) {
          LOG.error( ex, ex );
        } catch ( ExistingTransitionException ex ) {
          LOG.error( ex, ex );
        }
      }
      DispatcherFactory.remove( this.services.get( service ) );
      try {
        this.stateMachine.transition( State.STOPPED );
      } catch ( IllegalStateException ex ) {
        LOG.error( ex, ex );
      } catch ( NoSuchElementException ex ) {
        LOG.error( ex, ex );
      } catch ( ExistingTransitionException ex ) {
        LOG.error( ex, ex );
      }
    } else {
      this.getBuilder( ).fireStop( service );
    }
  }
  
  public void destroyService( final ServiceConfiguration config ) throws ServiceRegistrationException {
    final boolean configLocal = NetworkUtil.testLocal( config.getHostName( ) );
    Service remove = this.lookupServiceByHost( config.getHostName( ) );
    if ( remove == null ) {
      throw new ServiceRegistrationException( "Failed to find service corresponding to: " + config );
    } else {
      Service service = this.services.remove( remove.getName( ) );
      if ( config.isLocal( ) ) {
        this.localService.set( null );
        try {
          this.stateMachine.transition( Transition.DESTROYING );
        } catch ( IllegalStateException ex ) {
          LOG.error( ex, ex );
        } catch ( NoSuchElementException ex ) {
          LOG.error( ex, ex );
        } catch ( ExistingTransitionException ex ) {
          LOG.error( ex, ex );
        }
      }
      EventRecord.caller( Component.class, EventType.COMPONENT_SERVICE_DESTROY, this.getName( ), service.getName( ), service.getUri( ).toString( ) ).info( );
    }
  }
  
  public final List<ServiceInfoType> getServiceSnapshot( ) {
    List<ServiceInfoType> serviceSnapshot = Lists.newArrayList( );
    for ( final Service s : this.services.values( ) ) {
      if ( State.ENABLED.equals( s.getState( ) ) ) {
        serviceSnapshot.add( 0, new ServiceInfoType( ) {
          {
            setPartition( s.getServiceConfiguration( ).getPartition( ) );
            setName( s.getServiceConfiguration( ).getName( ) );
            setType( Component.this.getName( ) );
            getUris( ).add( s.getServiceConfiguration( ).getUri( ) );
          }
        } );
      } else {
        serviceSnapshot.add( new ServiceInfoType( ) {
          {
            setPartition( s.getServiceConfiguration( ).getPartition( ) );
            setName( s.getServiceConfiguration( ).getName( ) );
            setType( Component.this.getName( ) );
            getUris( ).add( s.getServiceConfiguration( ).getUri( ) );
          }
        } );
      }
    }
    return serviceSnapshot;
  }
  
  public final Iterator<ServiceInfoType> getUnorderedIterator( ) {
    return Iterables.transform( this.services.values( ), new Function<Service, ServiceInfoType>( ) {
      
      @Override
      public ServiceInfoType apply( final Service arg0 ) {
        return new ServiceInfoType( ) {
          {
            setPartition( arg0.getServiceConfiguration( ).getPartition( ) );
            setName( arg0.getServiceConfiguration( ).getName( ) );
            setType( Component.this.getName( ) );
            getUris( ).add( arg0.getServiceConfiguration( ).getUri( ) );
          }
        };
      }
    } ).iterator( );
  }
  
  public State getState( ) {
    return this.stateMachine.getState( );
  }
  
  /**
   * Get the name of this component. This is the proper short name; e.g., 'eucalyptus', 'walrus',
   * etc, as used in the META-INF descriptor file.
   * 
   * @see com.eucalyptus.component.ComponentInformation#getName()
   * @return Component name
   */
  public String getName( ) {
    return this.name;
  }
  
  public String getRegistryKey( String hostName ) {
    if ( NetworkUtil.testLocal( hostName ) ) {
      return this.getName( ) + "@localhost";
    } else {
      return this.getName( ) + "@" + hostName;
    }
  }
  
  public ServiceBuilder<ServiceConfiguration> getBuilder( ) {
    ServiceBuilder<ServiceConfiguration> ret = ServiceBuilderRegistry.get( this.identity );
    return ret != null
      ? ret
      : new DummyServiceBuilder( this );
  }
  
  /**
   * Can the component be run locally (i.e., is the needed code available)
   * 
   * @return true if the component could be run locally.
   */
  public Boolean isAvailableLocally( ) {
    return this.enabled.get( );
  }
  
  /**
   * True if the component has not been explicitly configured as running remotely. That is, even if
   * the code is available locally we do not prepare the service bootstrappers to run, but the local
   * service endpoint is still configured (i.e. for
   * {@link com.eucalyptus.component.id.ComponentService.dns}).
   * 
   * @return true if the component has not been explicitly marked as remote.
   */
  public Boolean isLocal( ) {
    return this.local.get( ); //this.localService.get( ) != null;
  }
  
  /**
   * TODO: DOCUMENT Component.java
   * 
   * @return
   * @throws ServiceRegistrationException
   */
  public List<ServiceConfiguration> list( ) throws ServiceRegistrationException {
    return this.getBuilder( ).list( );
  }
  
  /**
   * TODO: DOCUMENT Component.java
   * 
   * @param hostName
   * @param port
   * @return
   */
  public URI getUri( String hostName, Integer port ) {
    return this.getIdentity( ).makeRemoteUri( hostName, port );
  }
  
  public URI getUri( ) {
    NavigableSet<Service> services = this.getServices( );
    if ( this.getIdentity( ).isCloudLocal( ) && services.size( ) != 1 && !"db".equals( this.name ) ) {
      throw new RuntimeException( "Cloud local component has " + services.size( ) + " registered services (Should be exactly 1): " + this + " "
                                  + services.toString( ) );
    } else if ( this.getIdentity( ).isCloudLocal( ) && services.size( ) != 1 && "db".equals( this.name ) ) {
      return this.getIdentity( ).getLocalEndpointUri( );
    } else if ( this.getIdentity( ).isCloudLocal( ) && services.size( ) == 1 ) {
      return services.first( ).getUri( );
    } else {
      for ( Service s : services ) {
        if ( s.isLocal( ) ) {
          return s.getUri( );
        }
      }
      throw new RuntimeException( "Attempting to get the URI for a service which is either not a singleton or has no locally defined service endpoint." );
    }
  }
  
  /**
   * @return {@link NavigableSet<Service>} of the registered service of this {@link Component} type.
   */
  public NavigableSet<Service> getServices( ) {
    return Sets.newTreeSet( this.services.values( ) );
  }
  
  /**
   * Lookup the {@link Service} instance of this {@link Component} registered as {@code name}
   * 
   * @param name
   * @return Service instance
   * @throws NoSuchElementException if no {@link Service} instance is registered with the name of
   *           {@code name}
   */
  public Service lookupServiceByName( String name ) {
    Exceptions.ifNullArgument( name );
    for ( Service s : this.services.values( ) ) {
      LOG.error( s );
      if ( name.equals( s.getServiceConfiguration( ).getName( ) ) ) {
        return s;
      }
    }
    throw new NoSuchElementException( "No service found matching name: " + name + " for component: " + this.getName( ) );
  }
  
  /**
   * Lookup the {@link Service} instance of this {@link Component} running on {@code hostName}
   * 
   * @param hostName
   * @return Service instance
   * @throws NoSuchElementException if no {@link Service} instance is registered for
   *           {@code hostName}
   */
  public Service lookupServiceByHost( String hostName ) {
    Exceptions.ifNullArgument( hostName );
//ASAP:FIXME:GRZE:    hostName = InetAddress.getByName( hostName ).getCanonicalHostName( );
    for ( Service s : this.services.values( ) ) {
      if ( hostName.equals( s.getServiceConfiguration( ).getHostName( ) ) ) {
        return s;
      }
    }
    for ( Service s : this.services.values( ) ) {
      if ( hostName.equals( s.getEndpoint( ).getHost( ) ) ) {
        return s;
      }
    }
    if ( NetworkUtil.testLocal( hostName ) ) {
      for ( Service s : this.services.values( ) ) {
        if ( hostName.equals( s.getEndpoint( ).getHost( ) ) ) {
          return s;
        }
      }
    }
    if ( NetworkUtil.testLocal( hostName ) ) {
      for ( Service s : this.services.values( ) ) {
        if ( s.isLocal( ) ) {
          return s;
        }
      }
    }
    LOG.error( this.services.values( ) );
    throw new NoSuchElementException( "No service found matching hostname: " + hostName + " for component: " + this.getName( ) );
  }
  
  public Boolean isRunningLocally( ) {
    return State.ENABLED.equals( this.getState( ) ) && this.localService.get( ) != null;
  }
  
  /**
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString( ) {
    return String.format( "Component %s name=%s enabled=%s local=%s state=%s builder=%s\n", this.identity.name( ),
                          this.name, this.enabled, this.local, this.getState( ), this.getBuilder( ) );
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
   * REMOVE: promptly. Don't even think about using this.
   * 
   * @deprecated for sucking, too.
   */
  @Deprecated
  private com.eucalyptus.bootstrap.Component initComponent( ) {
    try {
      com.eucalyptus.bootstrap.Component component = com.eucalyptus.bootstrap.Component.valueOf( name );
      if ( component == null ) {
        throw BootstrapException.throwError( "Error loading component.  Failed to find component named '" + name );
      }
      return component;
    } catch ( Exception e ) {
      throw BootstrapException.throwError( "Error loading component.  Failed to find component named '" + name, e );
    }
  }
  
  /**
   * @return the bootstrapper
   */
  public ComponentBootstrapper getBootstrapper( ) {
    return this.bootstrapper;
  }
  
  public void runChecks( ) {
    if ( this.isAvailableLocally( ) && this.getState( ).ordinal( ) > State.STOPPED.ordinal( ) ) {
      this.stateMachine.transitionSelf( );
    }
  }
  
  public static class CheckEvent implements EventListener {
    public static void register( ) {
      ListenerRegistry.getInstance( ).register( ClockTick.class, new CheckEvent( ) );
    }
    
    @Override
    public void fireEvent( Event event ) {
      for ( final Component c : Components.list( ) ) {
        Threads.lookup( Empyrean.class.getName( ) ).submit( new Runnable( ) {
          @Override
          public void run( ) {
            c.runChecks( );
          }
        } );
      }
    }
  }
  
  /**
   * @return the identity
   */
  public ComponentId getIdentity( ) {
    return this.identity;
  }
  
  /**
   * @return
   * @see com.eucalyptus.component.ComponentId#name()
   */
  public String name( ) {
    return this.identity.name( );
  }
  
  /**
   * @param hostName
   * @param port
   * @return
   * @see com.eucalyptus.component.ComponentId#makeRemoteUri(java.lang.String, java.lang.Integer)
   */
  public URI makeRemoteUri( String hostName, Integer port ) {
    return this.identity.makeRemoteUri( hostName, port );
  }
  
  /**
   * @return
   * @see com.eucalyptus.component.ComponentId#getLocalEndpointName()
   */
  public String getLocalEndpointName( ) {
    return this.identity.getLocalEndpointName( );
  }
}
