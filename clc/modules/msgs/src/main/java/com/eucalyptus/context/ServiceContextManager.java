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
 *    THE REGENTS’ DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
 *    OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
 *    WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
 *    ANY SUCH LICENSES OR RIGHTS.
 *******************************************************************************
 * @author chris grzegorczyk <grze@eucalyptus.com>
 */

package com.eucalyptus.context;

import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicMarkableReference;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import org.apache.log4j.Logger;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.config.ConfigurationException;
import org.mule.api.context.MuleContextFactory;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.service.Service;
import org.mule.config.ConfigResource;
import org.mule.config.spring.SpringXmlConfigurationBuilder;
import org.mule.context.DefaultMuleContextFactory;
import org.mule.module.client.MuleClient;
import com.eucalyptus.bootstrap.Bootstrap;
import com.eucalyptus.bootstrap.BootstrapException;
import com.eucalyptus.component.Component;
import com.eucalyptus.component.ComponentId;
import com.eucalyptus.component.Components;
import com.eucalyptus.empyrean.Empyrean;
import com.eucalyptus.event.Event;
import com.eucalyptus.event.EventListener;
import com.eucalyptus.event.Hertz;
import com.eucalyptus.event.ListenerRegistry;
import com.eucalyptus.system.LogLevels;
import com.eucalyptus.system.Threads;
import com.eucalyptus.util.Assertions;
import com.eucalyptus.util.Templates;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ServiceContextManager implements EventListener<Event> {
  private static Logger                                CONFIG_LOG        = Logger.getLogger( "Configs" );
  private static Logger                                LOG               = Logger.getLogger( ServiceContextManager.class );
  private AtomicInteger                                pendingCount      = new AtomicInteger( 0 );
  private static final ServiceContextManager           singleton         = new ServiceContextManager( );
  
  private static final MuleContextFactory              contextFactory    = new DefaultMuleContextFactory( );
  private final ConcurrentNavigableMap<String, String> endpointToService = new ConcurrentSkipListMap<String, String>( );
  private final ConcurrentNavigableMap<String, String> serviceToEndpoint = new ConcurrentSkipListMap<String, String>( );
  private final List<ComponentId>                      enabledCompIds    = Lists.newArrayList( );
  private final ReentrantReadWriteLock                 canHas            = new ReentrantReadWriteLock( );
  private final Lock                                   canHasWrite;
  private final Lock                                   canHasRead;
  
  private MuleContext                                  context;
  private MuleClient                                   client;
  
  private ServiceContextManager( ) {
    this.canHasRead = this.canHas.readLock( );
    this.canHasWrite = this.canHas.writeLock( );
    ListenerRegistry.getInstance( ).register( Hertz.class, this );
  }
  
  @Override
  public void fireEvent( Event event ) {
    if ( event instanceof Hertz ) {
      if ( Bootstrap.isFinished( ) && this.canHasWrite.tryLock( ) && this.pendingCount.getAndSet( 0 ) > 0 ) {
        Threads.lookup( Empyrean.class, ServiceContextManager.class ).submit( new Runnable( ) {
          @Override
          public void run( ) {
            if ( ServiceContextManager.this.canHasWrite.tryLock( ) ) {
              try {
                ServiceContextManager.this.update( );
                ServiceContextManager.this.pendingCount.set( 0 );
              } catch ( Throwable ex ) {
                LOG.error( ex, ex );
              }
            } 
          }
        } );
      } else if ( this.shouldReload( ) ) {
        this.pendingCount.incrementAndGet( );
      }
    }
  }
  
  private boolean shouldReload( ) {
    List<Component> components = Components.whichAreEnabledLocally( );
    List<ComponentId> currentComponentIds = Components.toIds( components );
    if ( this.context == null ) {
      return true;
    } else if ( currentComponentIds.isEmpty( ) ) {
      return true;
    } else if ( !this.enabledCompIds.containsAll( currentComponentIds ) ) {
      return true;
    } else if ( this.enabledCompIds.size( ) != currentComponentIds.size( ) ) {
      return true;
    } else {
      return false;
    }
  }
  
  public static final void start( ) {
    restart( );
  }
  
  public static final void restart( ) {
    singleton.pendingCount.incrementAndGet( );
  }
  
  private void update( ) throws ServiceInitializationException {
    this.canHasWrite.lock( );
    try {
      List<Component> components = Components.whichAreEnabledLocally( );
      List<ComponentId> currentComponentIds = Components.toIds( components );
      if ( this.shouldReload( ) ) {
        if ( this.context != null ) {
          this.shutdown( );
        }
        this.context = this.createContext( components, currentComponentIds );
        Assertions.assertNotNull( this.context, "BUG: failed to build mule context for reasons unknown" );
        try {
          this.context.start( );
          this.client = new MuleClient( this.context );
          this.endpointToService.clear( );
          this.serviceToEndpoint.clear( );
          for ( Object o : this.context.getRegistry( ).lookupServices( ) ) {
            Service s = ( Service ) o;
            for ( Object p : s.getInboundRouter( ).getEndpoints( ) ) {
              InboundEndpoint in = ( InboundEndpoint ) p;
              this.endpointToService.put( in.getEndpointURI( ).toString( ), s.getName( ) );
              this.serviceToEndpoint.put( s.getName( ), in.getEndpointURI( ).toString( ) );
            }
          }
        } catch ( Throwable e ) {
          LOG.error( e, e );
          throw new ServiceInitializationException( "Failed to start service this.context.", e );
        }
      }
    } finally {
      this.canHasWrite.unlock( );
    }
  }
  
  private MuleContext createContext( List<Component> components, List<ComponentId> currentComponentIds ) throws ServiceInitializationException {
    Set<ConfigResource> configs = Sets.newHashSet( );
    MuleContext muleCtx = null;
    for ( Component component : components ) {
      ComponentId id = component.getComponentId( );
      String errMsg = "Failed to render model for: " + component.getComponentId( ) + " because of: ";
      LOG.info( "-> Rendering configuration for " + component.getComponentId( ).name( ) );
      try {
        String outString = Templates.prepare( id.getServiceModelFileName( ) )
                                    .withProperty( "components", Components.toIds( components ) )
                                    .withProperty( "thisComponent", id )
                                    .evaluate( id.getServiceModel( ) );
        ConfigResource configRsc = createConfigResource( component, outString );
        configs.add( configRsc );
      } catch ( Exception ex ) {
        LOG.error( errMsg + ex.getMessage( ), ex );
        throw new ServiceInitializationException( errMsg + ex.getMessage( ), ex );
      }
    }
    try {
      SpringXmlConfigurationBuilder builder = new SpringXmlConfigurationBuilder( configs.toArray( new ConfigResource[] {} ) );
      muleCtx = contextFactory.createMuleContext( builder );
      this.enabledCompIds.clear( );
      this.enabledCompIds.addAll( currentComponentIds );
    } catch ( Exception ex ) {
      LOG.error( ex, ex );
      throw new ServiceInitializationException( "Failed to build service context because of: " + ex.getMessage( ), ex );
    }
    return muleCtx;
  }
  
  private static ConfigResource createConfigResource( Component component, String outString ) {
    ByteArrayInputStream bis = new ByteArrayInputStream( outString.getBytes( ) );
    if ( LogLevels.EXTREME ) {
      CONFIG_LOG.trace( "===================================" );
      CONFIG_LOG.trace( outString );
      CONFIG_LOG.trace( "===================================" );
    }
    ConfigResource configRsc = new ConfigResource( component.getComponentId( ).getServiceModelFileName( ), bis );
    return configRsc;
  }
  
  private static String FAIL_MSG = "ESB client not ready because the service bus has not been started.";
  
  public static MuleClient getClient( ) throws MuleException {
    singleton.canHasRead.lock( );
    try {
      return singleton.client;
    } finally {
      singleton.canHasRead.lock( );
    }
  }
  
  public static MuleContext getContext( ) throws MuleException {
    singleton.canHasRead.lock( );
    try {
      if ( singleton.context == null ) {
        start( );
      }
      return singleton.context;
    } finally {
      singleton.canHasRead.lock( );
    }
  }
  
  public static void shutdown( ) {
    singleton.canHasWrite.lock( );
    try {
      if ( singleton.context != null ) {
        try {
          singleton.context.stop( );
          singleton.context.dispose( );
          singleton.context = null;
        } catch ( Exception ex ) {
          LOG.trace( ex, ex );
        }
      }
    } finally {
      singleton.canHasWrite.unlock( );
    }
  }
  
  public static String mapServiceToEndpoint( String service ) {
    singleton.canHasRead.lock( );
    try {
      String dest = service;
      if ( ( !service.startsWith( "vm://" ) && !singleton.serviceToEndpoint.containsKey( service ) ) || service == null ) {
        dest = "vm://RequestQueue";
      } else if ( !service.startsWith( "vm://" ) ) {
        dest = singleton.serviceToEndpoint.get( dest );
      }
      return dest;
    } finally {
      singleton.canHasRead.lock( );
    }
  }
  
  public static String mapEndpointToService( String endpoint ) throws ServiceDispatchException {
    singleton.canHasRead.lock( );
    try {
      String dest = endpoint;
      if ( ( endpoint.startsWith( "vm://" ) && !singleton.endpointToService.containsKey( endpoint ) ) || endpoint == null ) {
        throw new ServiceDispatchException( "No such endpoint: " + endpoint + " in endpoints=" + singleton.endpointToService.entrySet( ) );
        
      }
      if ( endpoint.startsWith( "vm://" ) ) {
        dest = singleton.endpointToService.get( endpoint );
      }
      return dest;
    } finally {
      singleton.canHasRead.lock( );
    }
  }
  
}
