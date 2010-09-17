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
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.log4j.Logger;
import com.eucalyptus.bootstrap.BootstrapException;
import com.eucalyptus.records.EventRecord;
import com.eucalyptus.records.EventType;
import com.eucalyptus.records.Record;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.HasName;
import com.eucalyptus.util.NetworkUtil;
import com.eucalyptus.util.async.Callback;
import com.eucalyptus.util.fsm.AtomicMarkedState;
import com.eucalyptus.util.fsm.StateMachineBuilder;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/**
 * TODO: DOCUMENT. yes pls.
 */
public class Component implements ComponentInformation, HasName<Component> {
  private static Logger LOG = Logger.getLogger( Component.class );
  
  public enum State {
    DISABLED, PRIMORDIAL, INITIALIZED, LOADED, RUNNING, STOPPED, PAUSED;
  }
  
  public enum Transition {
    EARLYRUNTIME, INITIALIZE, LOAD, START, STOP, PAUSE;
    /**
     * @see Component#stateMachine
     * @see com.eucalyptus.util.fsm.AtomicMarkedState#transition(java.lang.Enum)
     * @return
     */
    public Callback.Success<Component> getCallback( ) {
      return new Callback.Success<Component>( ) {
        @Override
        public void fire( Component t ) {
          t.stateMachine.transition( Transition.this );
        }
      };
    }
  }
  
  private final String                                          name;
  private final com.eucalyptus.bootstrap.Component              component;
  private final Configuration                                   configuration;
  private final AtomicMarkedState<Component, State, Transition> stateMachine;
  private final AtomicBoolean                                   enabled  = new AtomicBoolean( false );
  private final AtomicBoolean                                   local    = new AtomicBoolean( false );
  private final Map<String, Service>                            services = Maps.newConcurrentHashMap( );
  private ServiceBuilder<ServiceConfiguration>                  builder;                                //TODO: lonely mutable is lonely.
                                                                                                         
  Component( String name, URI configFile ) throws ServiceRegistrationException {
    this.name = name;
    this.component = initComponent( );
    if ( System.getProperty( "euca.disable." + this.name ) == null ) {
      this.enabled.set( true );
      if ( System.getProperty( "euca.remote." + this.name ) == null ) {
        this.local.set( true );
      }
    }
    if ( configFile != null ) {
      this.configuration = new Configuration( this, configFile );
    } else {
      this.configuration = new Configuration( this );
    }
    this.stateMachine = new StateMachineBuilder<Component, State, Transition>( this, State.DISABLED ) {
      {
        on( Transition.EARLYRUNTIME ).from( State.DISABLED ).to( State.PRIMORDIAL ).error( State.DISABLED ).noop( );
        on( Transition.INITIALIZE ).from( State.PRIMORDIAL ).to( State.INITIALIZED ).error( State.DISABLED ).noop( );
        on( Transition.LOAD ).from( State.INITIALIZED ).to( State.LOADED ).error( State.DISABLED ).noop( );
        on( Transition.START ).from( State.LOADED ).to( State.RUNNING ).error( State.DISABLED ).noop( );
        on( Transition.STOP ).from( State.RUNNING ).to( State.STOPPED ).error( State.STOPPED ).noop( );
        on( Transition.PAUSE ).from( State.RUNNING ).to( State.PAUSED ).noop( );
      }
    }.newAtomicState( );
    this.builder = new DummyServiceBuilder( this );
  }
  
  /**
   * TODO: DOCUMENT Component.java
   * 
   * @param config
   * @throws ServiceRegistrationException
   */
  public void removeService( final ServiceConfiguration config ) throws ServiceRegistrationException {
    final boolean configLocal = NetworkUtil.testLocal( config.getHostName( ) );
    Service remove = this.lookupServiceByHost( config.getHostName( ) );
    if ( remove == null ) {
      throw new ServiceRegistrationException( "Failed to find service corresponding to: " + config );
    } else {
      Service service = this.services.remove( remove.getName( ) );
      this.builder.fireStop( config );
      DispatcherFactory.remove( service );
//      Components.deregister( service );
      EventRecord.caller( Component.class, EventType.COMPONENT_SERVICE_STOP, this.getName( ), service.getName( ), service.getUri( ).toString( ) ).info( );
    }
  }
  
  /**
   * Builds a Service instance for this component using a service configuration
   * created with the specified URI.
   * 
   * @return
   * @throws ServiceRegistrationException
   */
  public Service buildService( URI uri ) throws ServiceRegistrationException {
    ServiceConfiguration config = this.builder.toConfiguration( uri );
    Service service = new Service( this, config );
    return this.setupService( config, service );
  }
  
  /**
   * Builds a Service instance for this component using the provided service
   * configuration.
   * 
   * @return Service instance of the service
   * @throws ServiceRegistrationException
   */
  public Service buildService( ServiceConfiguration config ) throws ServiceRegistrationException {
    Service service = new Service( this, config );
    return this.setupService( config, service );
  }
  
  /**
   * Builds a Service instance for this component using the local default
   * values.
   * 
   * @return Service instance of the service
   * @throws ServiceRegistrationException
   */
  public Service buildService( ) throws ServiceRegistrationException {
    ServiceConfiguration conf = this.builder.toConfiguration( this.getConfiguration( ).getLocalUri( ) );
    Service service = new Service( this, conf );
    return this.setupService( conf, service );
  }
  
  /**
   * 
   * @param config
   * @param service
   * @return
   * @throws ServiceRegistrationException
   */
  private Service setupService( ServiceConfiguration config, Service service ) throws ServiceRegistrationException {
    this.services.put( service.getName( ), service );
    Components.register( service );
    EventRecord.caller( Component.class, EventType.COMPONENT_SERVICE_REGISTERED,
                        this.getName( ),
                        config.isLocal( )
                          ? "local"
                          : "remote",
                        service.getName( ), service.getUri( ), service.getDispatcher( ) ).info( );
    return service;
  }
  
  /**
   * TODO: DOCUMENT Component.java
   * 
   * @param service
   * @throws ServiceRegistrationException 
   */
  public void startService( ServiceConfiguration service ) throws ServiceRegistrationException {
    EventRecord.caller( Component.class, EventType.COMPONENT_SERVICE_START, this.getName( ), service.getName( ), service.getUri( ).toString( ) ).info( );
    this.builder.fireStart( service );
  }
  
  /**
   * @see com.eucalyptus.util.fsm.AtomicMarkedState#getState()
   */
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
  
  public Configuration getConfiguration( ) {
    return this.configuration;
  }
  
  /**
   * TODO: DOCUMENT Component.java
   * @return
   */
  public ServiceBuilder<ServiceConfiguration> getBuilder( ) {
    return this.builder;
  }
  
  /**
   * @note the only real use of this method is for the remote stack to mark components disabled
   *       later during bootstrap.
   */
  public void markDisabled( ) {
    this.local.set( false );
    this.enabled.set( false );
  }
  
  /**
   * Can the component be run locally (i.e., is the needed code available)
   * 
   * @return true if the component could be run locally.
   */
  public Boolean isEnabled( ) {
    return this.enabled.get( );
  }
  
  /**
   * True if the component has not been explicitly configured as running remotely. That is, even if
   * the code is available locally we do not prepare the service bootstrappers to run, but the local
   * service endpoint is still configured (i.e. for {@link com.eucalyptus.bootstrap.Component.dns}).
   * 
   * @return true if the component has not been explicitly marked as remote.
   */
  public Boolean isLocal( ) {
    return this.local.get( );
  }
  
  /**
   * TODO: DOCUMENT Component.java
   * @return
   * @throws ServiceRegistrationException
   */
  public List<ServiceConfiguration> list( ) throws ServiceRegistrationException {
    return this.builder.list( );
  }
  
  /**
   * TODO: DOCUMENT Component.java
   * @param hostName
   * @param port
   * @return
   */
  public URI getUri( String hostName, Integer port ) {
    return this.getConfiguration( ).makeUri( hostName, port );
  }
  
  /**
   * TODO: DOCUMENT Component.java
   * @param builder
   */
  void setBuilder( ServiceBuilder<ServiceConfiguration> builder ) {
    this.builder = builder;
  }
  
  /**
   * @return true if the component is in a running state.
   */
  public Boolean isRunning( ) {
    return State.RUNNING.equals( this.getState( ) );
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
  
  /**
   * TODO: DOCUMENT Component.java
   * @return
   */
  public Boolean isRunningLocally( ) {
    try {
      this.lookupServiceByHost( "localhost" );
      return true;
    } catch ( NoSuchElementException ex ) {
      LOG.trace( ex, ex );
      return false;
    }
  }

  /**
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString( ) {
    return String.format( "Component %s name=%s enabled=%s local=%s state=%s builder=%s\nservices=%s\nconfiguration=%s", this.component,
                          this.name, this.enabled, this.local, this.getState( ), this.builder, this.services, this.configuration );
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
   * @deprecated for sucking.
   */
  @Deprecated
  public com.eucalyptus.bootstrap.Component getPeer( ) {
    return this.component;
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
  
}
