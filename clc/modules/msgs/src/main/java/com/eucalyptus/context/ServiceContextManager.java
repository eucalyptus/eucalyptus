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
 *    THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
 *    OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
 *    WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
 *    ANY SUCH LICENSES OR RIGHTS.
 *******************************************************************************
 * @author chris grzegorczyk <grze@eucalyptus.com>
 */

package com.eucalyptus.context;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.apache.log4j.Logger;
import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.context.MuleContextFactory;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.service.Service;
import org.mule.config.ConfigResource;
import org.mule.config.spring.SpringXmlConfigurationBuilder;
import org.mule.context.DefaultMuleContextFactory;
import org.mule.module.client.MuleClient;
import com.eucalyptus.bootstrap.Bootstrap;
import com.eucalyptus.component.Component;
import com.eucalyptus.component.ComponentId;
import com.eucalyptus.component.Components;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.empyrean.Empyrean;
import com.eucalyptus.system.Threads;
import com.eucalyptus.util.Assertions;
import com.eucalyptus.util.Logs;
import com.eucalyptus.util.Templates;
import com.eucalyptus.util.async.Futures;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

public class ServiceContextManager {
  private static Logger                                CONFIG_LOG        = Logger.getLogger( "Configs" );
  private static Logger                                LOG               = Logger.getLogger( ServiceContextManager.class );
  private static final ServiceContextManager           singleton         = new ServiceContextManager( );
  
  private static final MuleContextFactory              contextFactory    = new DefaultMuleContextFactory( );
  private final ConcurrentNavigableMap<String, String> endpointToService = new ConcurrentSkipListMap<String, String>( );
  private final ConcurrentNavigableMap<String, String> serviceToEndpoint = new ConcurrentSkipListMap<String, String>( );
  private final List<ComponentId>                      enabledCompIds    = Lists.newArrayList( );
  private final AtomicBoolean                          running           = new AtomicBoolean( true );
  private final ReentrantReadWriteLock                 canHas            = new ReentrantReadWriteLock( );
  private final Lock                                   canHasWrite;
  private final Lock                                   canHasRead;
  private final BlockingQueue<ServiceConfiguration>    queue             = new LinkedBlockingQueue<ServiceConfiguration>( );
  private MuleContext                                  context;
  private MuleClient                                   client;
  private ExecutorService                              executor          = Executors.newFixedThreadPool( 1 );
  
  private ServiceContextManager( ) {
    this.canHasRead = this.canHas.readLock( );
    this.canHasWrite = this.canHas.writeLock( );
    executor.submit( new Runnable( ) {
      public void run( ) {
        if ( !ServiceContextManager.this.running.compareAndSet( false, true ) ) {
          return;
        } else {
          while ( ServiceContextManager.this.running.get( ) ) {
            ServiceConfiguration event;
            try {
              if ( ( event = ServiceContextManager.this.queue.poll( 2000, TimeUnit.MILLISECONDS ) ) != null ) {
                if( event.isVmLocal( ) ) {
                  if ( ServiceContextManager.this.canHasWrite.tryLock( ) ) {
                    try {
                      ServiceContextManager.this.update( );
                    } catch ( Throwable ex ) {
                      LOG.error( ex, ex );
                    } finally {
                      ServiceContextManager.this.canHasWrite.unlock( );
                    }
                  }
                }
              }
            } catch ( InterruptedException e1 ) {
              Thread.currentThread( ).interrupt( );
              ServiceContextManager.this.running.set( false );
              return;
            } catch ( final Throwable e ) {
              LOG.error( e, e );
            }
          }
        }
      }
    } );
  }
  
  private List<ComponentId> shouldReload( ) {
    List<Component> components = Components.whichAreEnabledLocally( );
    List<ComponentId> currentComponentIds = Components.toIds( components );
    if ( this.context == null ) {
      return currentComponentIds;
    } else if ( !this.enabledCompIds.equals( currentComponentIds ) ) {
      return currentComponentIds;
    } else {
      return Lists.newArrayList( );
    }
  }
  
  public static final void restartSync( ) {
    restartSync( Components.lookup( Empyrean.class ).getLocalServiceConfiguration( ) );
  }
  
  public static final void restartSync( ServiceConfiguration config ) {
    singleton.queue.add( config );
  }
  
  private void update( ) throws ServiceInitializationException {
    this.canHasWrite.lock( );
    try {
      List<ComponentId> reloadComponentIds = this.shouldReload( );
      
      if ( !reloadComponentIds.isEmpty( ) ) {
        if ( this.context != null ) {
          this.shutdown( );
        }
        this.context = this.createContext( reloadComponentIds );
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
  
  private MuleContext createContext( List<ComponentId> currentComponentIds ) throws ServiceInitializationException {
    this.canHasWrite.lock( );
    try {
      LOG.error( "Restarting service context with these enabled services: " + currentComponentIds );
      Set<ConfigResource> configs = Sets.newHashSet( );
      MuleContext muleCtx = null;
      for ( ComponentId componentId : currentComponentIds ) {
        Component component = Components.lookup( componentId );
        String errMsg = "Failed to render model for: " + componentId + " because of: ";
        LOG.info( "-> Rendering configuration for " + componentId.name( ) );
        try {
          String outString = Templates.prepare( componentId.getServiceModelFileName( ) )
                                      .withProperty( "components", currentComponentIds )
                                      .withProperty( "thisComponent", componentId )
                                      .evaluate( componentId.getServiceModel( ) );
          ConfigResource configRsc = createConfigResource( componentId, outString );
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
    } finally {
      this.canHasWrite.unlock( );
    }
  }
  
  private static ConfigResource createConfigResource( ComponentId componentId, String outString ) {
    ByteArrayInputStream bis = new ByteArrayInputStream( outString.getBytes( ) );
    Logs.extreme( ).trace( "===================================" );
    Logs.extreme( ).trace( outString );
    Logs.extreme( ).trace( "===================================" );
    ConfigResource configRsc = new ConfigResource( componentId.getServiceModelFileName( ), bis );
    return configRsc;
  }
  
  private static String FAIL_MSG = "ESB client not ready because the service bus has not been started.";
  
  public static MuleClient getClient( ) throws MuleException {
    singleton.canHasRead.lock( );
    try {
      return singleton.client;
    } finally {
      singleton.canHasRead.unlock( );
    }
  }
  
  public static MuleContext getContext( ) throws MuleException, ServiceInitializationException {
    singleton.canHasRead.lock( );
    try {
      if ( singleton.context == null ) {
        singleton.update( );
      }
      return singleton.context;
    } finally {
      singleton.canHasRead.unlock( );
    }
  }
  
  private void stop( ) {
    this.canHasWrite.lock( );
    try {
      if ( this.context != null ) {
        try {
          for ( int i = 0; i < 10 && Contexts.hasOutstandingRequests( ); i++ ) {
            try {
              TimeUnit.SECONDS.sleep( 1 );
            } catch ( InterruptedException ex ) {
              Thread.currentThread( ).interrupt( );
            }
          }
          this.context.stop( );
          this.context.dispose( );
        } catch ( MuleException ex ) {
          LOG.error( ex, ex );
        }
        this.context = null;
      }
    } finally {
      this.canHasWrite.unlock( );
    }
  }
  
  public static void shutdown( ) {
    singleton.stop( );
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
      singleton.canHasRead.unlock( );
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
      singleton.canHasRead.unlock( );
    }
  }
  
}
