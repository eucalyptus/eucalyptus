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
import org.apache.log4j.Logger;
import org.mule.config.ConfigResource;
import com.eucalyptus.bootstrap.BootstrapException;
import com.eucalyptus.bootstrap.Bootstrapper;
import com.eucalyptus.records.EventRecord;
import com.eucalyptus.records.EventType;
import com.eucalyptus.records.Record;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.HasName;
import com.eucalyptus.util.NetworkUtil;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/**
 * @author decker
 *
 */
public class Component implements ComponentInformation, HasName<Component> {
  private static Logger                            LOG             = Logger.getLogger( Component.class );
  public static String                             DISABLE_PATTERN = "euca.disable.%s";
  public static String                             REMOTE_PATTERN  = "euca.remote.%s";
  private final String                             name;
  private final com.eucalyptus.bootstrap.Component component;
  private final Configuration                      configuration;
  private ServiceBuilder<ServiceConfiguration>     builder;
  private Lifecycle                                lifecycle;
  private Boolean                                  enabled;
  private Boolean                                  local;
  private Map<String, Service>                     services        = Maps.newConcurrentHashMap( );
  
  Component( String name, URI configFile ) throws ServiceRegistrationException {
    this.name = name;
    boolean enabled = false, local = false;
    if ( System.getProperty( String.format( DISABLE_PATTERN, this.name ) ) == null ) {
      enabled = true;
      if ( System.getProperty( String.format( REMOTE_PATTERN, this.name ) ) == null ) {
        local = true;
      }
    }
    this.enabled = enabled;
    this.local = local;
    this.component = initComponent( );
    if ( configFile != null ) {
      this.configuration = new Configuration( this, configFile );
      Components.register( this.configuration );
    } else {
      this.configuration = new Configuration( this );
    }
    this.lifecycle = new Lifecycle( this );
    Components.register( this.lifecycle );
    this.builder = new DefaultServiceBuilder( this );
  }
  
  public void removeService( final ServiceConfiguration config ) throws ServiceRegistrationException {
    Service service = this.lookupServiceByHost( config.getHostName( ) );
    this.builder.fireStop( config );
    DispatcherFactory.remove( service );
    Components.deregister( service );
    EventRecord.caller( Component.class, config.isLocal( ) ? EventType.COMPONENT_SERVICE_STOP : EventType.COMPONENT_SERVICE_STOP, this.getName( ),
                        service.getName( ), service.getUri( ).toString( ) ).info( );
    this.services.remove( service.getName( ) );
  }
  
  /**
   * Builds a Service instance for this component using a service configuration created with the specified URI.
   * @return
   * @throws ServiceRegistrationException
   */
  public Service buildService( URI uri ) throws ServiceRegistrationException {
    this.enabled = true;
    ServiceConfiguration config = this.builder.toConfiguration( uri );
    Service service = new Service( this, config );
    return this.setupService( config, service );
  }
  
  /**
   * Builds a Service instance for this component using the provided service configuration.
   * @return
   * @throws ServiceRegistrationException
   */
  public Service buildService( ServiceConfiguration config ) throws ServiceRegistrationException {
    this.enabled = true;
    Service service = new Service( this, config );
    return this.setupService( config, service );
  }
  
  
  /**
   * Builds a Service instance for this component using the local default values.
   * @return
   * @throws ServiceRegistrationException
   */
  public Service buildService( ) throws ServiceRegistrationException {
    this.enabled = true;
    ServiceConfiguration conf = this.builder.toConfiguration( this.getConfiguration( ).getLocalUri( ) );
    Service service = new Service( this, conf );
    return this.setupService( conf, service );
  }
  
  private Service setupService( ServiceConfiguration config, Service service ) throws ServiceRegistrationException {
    this.services.put( service.getName( ), service );
    Components.register( service );
    EventRecord.caller( Component.class, config.isLocal( ) ? EventType.COMPONENT_SERVICE_INIT : EventType.COMPONENT_SERVICE_INIT, this.getName( ),
                        service.getName( ), service.getUri( ).toString( ) ).info( );
    return service;
  }
  
  public void startService( ServiceConfiguration service ) throws ServiceRegistrationException {
    EventRecord.caller( Component.class, EventType.COMPONENT_SERVICE_START, this.getName( ), service.getName( ), service.getUri( ).toString( ) ).info( );
    this.builder.fireStart( service );
    this.lifecycle.setState( Lifecycles.State.STARTED );
  }
  
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
  
  public com.eucalyptus.bootstrap.Component getPeer( ) {
    return this.component;
  }
  
  public String getName( ) {
    return this.name;
  }
  
  public String toString( ) {
    Record rec = EventRecord.caller( Component.class, EventType.COMPONENT_INFO, this.getName( ), "enabled", this.isEnabled( ), "local", this.isLocal( ),
                                          "state", this.getLifecycle( ).getState( ) );
    if( this.getConfiguration( ).getResource( ) != null ) {
      for ( ConfigResource cfg : this.getConfiguration( ).getResource( ).getConfigurations( ) ) {
        rec.next( ).append( ConfigResource.class, EventType.COMPONENT_INFO, this.getName( ), "->" + cfg.getUrl( ) );
      }
    }
    for ( Bootstrapper b : this.configuration.getBootstrappers( ) ) {
      rec.next( ).append( Bootstrapper.class, EventType.COMPONENT_INFO, this.getName( ), "->" + b.getClass( ).getSimpleName( ) );
    }
    return rec.toString( );
  }
  
  public Configuration getConfiguration( ) {
    return this.configuration;
  }
  
  public ServiceBuilder<ServiceConfiguration> getBuilder( ) {
    return this.builder;
  }
  
  public void markRemote( ) {
    this.local = false;
  }
  
  public void markDisabled( ) {
    this.local = false;
    this.enabled = false;
  }
  
  public Boolean isEnabled( ) {
    return this.enabled;
  }
  
  public Boolean isLocal( ) {
    return this.local;
  }
  
  @Override
  public int compareTo( Component that ) {
    return this.getName( ).compareTo( that.getName( ) );
  }
  
  public List<ServiceConfiguration> list( ) throws ServiceRegistrationException {
    return this.builder.list( );
  }
  
  public URI getUri( String hostName, Integer port ) {
    return this.getConfiguration( ).makeUri( hostName, port );
  }
  
  public void setBuilder( ServiceBuilder<ServiceConfiguration> builder ) {
    this.builder = builder;
  }
  
  Lifecycle getLifecycle( ) {
    return this.lifecycle;
  }
  
  public Boolean isInitialized( ) {
    return Lifecycles.State.INITIALIZED.equals( this.getLifecycle( ).getState( ) );
  }
  
  public Boolean isRunning( ) {
    return Lifecycles.State.STARTED.equals( this.getLifecycle( ).getState( ) );
  }

  public NavigableSet<Service> getServices( ) {
    return Sets.newTreeSet( this.services.values( ) );
  }

  public String getState( ) {
    return this.lifecycle.getState( ).name( );
  }
  
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
      hostName = "localhost";
      for ( Service s : this.services.values( ) ) {
        if ( hostName.equals( s.getEndpoint( ).getHost( ) ) ) {
          return s;
        }
      }
    }
    LOG.error( this.services.values( ) );
    throw new NoSuchElementException( "No service found matching hostname: " + hostName + " for component: " + this.getName( ) );
  }

  public Boolean isRunningLocally( ) {
    try {
      for( Service s : this.services.values( ) ) {
        if( s.isLocal( ) ) {
          return true;
        }
      }
      return false;
    } catch ( NoSuchElementException ex ) {
      LOG.trace( ex, ex );
      return false;
    }
  }
}
