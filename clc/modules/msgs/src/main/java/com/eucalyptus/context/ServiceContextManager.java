/*************************************************************************
 * Copyright 2009-2012 Eucalyptus Systems, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 *
 * This file may incorporate work covered under the following copyright
 * and permission notice:
 *
 *   Software License Agreement (BSD License)
 *
 *   Copyright (c) 2008, Regents of the University of California
 *   All rights reserved.
 *
 *   Redistribution and use of this software in source and binary forms,
 *   with or without modification, are permitted provided that the
 *   following conditions are met:
 *
 *     Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *     Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer
 *     in the documentation and/or other materials provided with the
 *     distribution.
 *
 *   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *   "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *   LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 *   FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 *   COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 *   INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 *   BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *   LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 *   CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 *   LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 *   ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 *   POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 *   THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 *   COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 *   AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 *   IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 *   SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 *   WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 *   REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 *   IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 *   NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/

package com.eucalyptus.context;

import java.io.ByteArrayInputStream;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
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
import com.eucalyptus.bootstrap.Bootstrapper;
import com.eucalyptus.bootstrap.OrderedShutdown;
import com.eucalyptus.bootstrap.Provides;
import com.eucalyptus.bootstrap.RunDuring;
import com.eucalyptus.component.Component;
import com.eucalyptus.component.ComponentId;
import com.eucalyptus.component.ComponentIds;
import com.eucalyptus.component.ComponentMessages;
import com.eucalyptus.component.Components;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.empyrean.Empyrean;
import com.eucalyptus.records.Logs;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.Templates;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.io.Resources;
import static com.eucalyptus.util.Parameters.checkParam;
import static org.hamcrest.Matchers.notNullValue;

public class ServiceContextManager {
  @Provides( Empyrean.class )
  @RunDuring( Bootstrap.Stage.RemoteServicesInit )
  public static class ServiceContextBootstrapper extends Bootstrapper.Simple {
    
    public ServiceContextBootstrapper( ) {}
    
    @Override
    public boolean start( ) throws Exception {
      new Thread( ) {
        
        @Override
        public void run( ) {
          try {
            singleton.update( );
            singleton.getClient( );
          } catch ( final Exception ex ) {
            LOG.error( ex, ex );
          }
        }
        
      }.start( );
      return true;
    }
  }
  
  private static Logger                                CONFIG_LOG        = Logger.getLogger( "Configs" );
  private static Logger                                LOG               = Logger.getLogger( ServiceContextManager.class );
  private static ServiceContextManager                 singleton         = new ServiceContextManager( );
  
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
  
  private ServiceContextManager( ) {
    this.canHasRead = this.canHas.readLock( );
    this.canHasWrite = this.canHas.writeLock( );
    OrderedShutdown.registerPreShutdownHook( new Runnable( ) {
      
      @Override
      public void run( ) {
        ServiceContextManager.shutdown( );
      }
      
    } );
  }
  
  public static final void restartSync( ) {
    if ( singleton.canHasWrite.tryLock( ) ) {
      try {
        singleton.update( );
      } catch ( final Exception ex ) {
        LOG.error( Exceptions.causeString( ex ) );
        LOG.error( ex, ex );
      } finally {
        singleton.canHasWrite.unlock( );
      }
    }
  }
  
  private void update( ) {
    if ( this.context != null ) {
      return;
    } else {
      this.canHasWrite.lock( );
      try {
        this.context = this.createContext( );
        checkParam( this.context, notNullValue() );
        try {
          this.context.start( );
          this.client = new MuleClient( this.context );
          this.endpointToService.clear( );
          this.serviceToEndpoint.clear( );
          for ( final Object o : this.context.getRegistry( ).lookupServices( ) ) {
            final Service s = ( Service ) o;
            for ( final Object p : s.getInboundRouter( ).getEndpoints( ) ) {
              final InboundEndpoint in = ( InboundEndpoint ) p;
              this.endpointToService.put( in.getEndpointURI( ).toString( ), s.getName( ) );
              this.serviceToEndpoint.put( s.getName( ), in.getEndpointURI( ).toString( ) );
            }
          }
        } catch ( final Exception e ) {
          LOG.error( e, e );
          throw Exceptions.toUndeclared( new ServiceInitializationException( "Failed to start service this.context.", e ) );
        }
      } finally {
        this.canHasWrite.unlock( );
      }
    }
  }
  
  private static final String EMPTY_MODEL = "  <mule xmlns=\"http://www.mulesource.org/schema/mule/core/2.0\"\n"
                                            +
                                            "      xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n"
                                            +
                                            "      xmlns:spring=\"http://www.springframework.org/schema/beans\"\n"
                                            +
                                            "      xmlns:vm=\"http://www.mulesource.org/schema/mule/vm/2.0\"\n"
                                            +
                                            "      xmlns:euca=\"http://www.eucalyptus.com/schema/cloud/1.6\"\n"
                                            +
                                            "      xsi:schemaLocation=\"\n"
                                            +
                                            "       http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans-2.0.xsd\n"
                                            +
                                            "       http://www.mulesource.org/schema/mule/core/2.0 http://www.mulesource.org/schema/mule/core/2.0/mule.xsd\n"
                                            +
                                            "       http://www.mulesource.org/schema/mule/vm/2.0 http://www.mulesource.org/schema/mule/vm/2.0/mule-vm.xsd\n"
                                            +
                                            "       http://www.eucalyptus.com/schema/cloud/1.6 http://www.eucalyptus.com/schema/cloud/1.6/euca.xsd\">\n"
                                            +
                                            "</mule>\n";
  
  private MuleContext createContext( ) {
    List<ComponentId> currentComponentIds = ComponentIds.list( );
    LOG.error( "Restarting service context with these enabled services: " + currentComponentIds );
    final Set<ConfigResource> configs = Sets.newHashSet( );
    MuleContext muleCtx = null;
    for ( final ComponentId componentId : currentComponentIds ) {
      final Component component = Components.lookup( componentId );
      final String errMsg = "Failed to render model for: " + componentId
                            + " because of: ";
      LOG.info( "-> Rendering configuration for " + componentId.name( ) );
      try {
        final String serviceModel = this.loadModel( componentId );
        final String outString = Templates.prepare( componentId.getServiceModelFileName( ) )
                                          .withProperty( "components", currentComponentIds )
                                          .withProperty( "ComponentMessages", ComponentMessages.class )
                                          .withProperty( "thisComponent", componentId )
                                          .evaluate( serviceModel );
        final ConfigResource configRsc = createConfigResource( componentId, outString );
        configs.add( configRsc );
      } catch ( final Exception ex ) {
        LOG.error( errMsg + ex.getMessage( ), ex );
      }
    }
    try {
      final SpringXmlConfigurationBuilder builder = new SpringXmlConfigurationBuilder( configs.toArray( new ConfigResource[] {} ) );
      muleCtx = contextFactory.createMuleContext( builder );
      this.enabledCompIds.clear( );
      this.enabledCompIds.addAll( currentComponentIds );
    } catch ( final Exception ex ) {
      LOG.error( ex, ex );
    }
    return muleCtx;
  }
  
  private String loadModel( final ComponentId componentId ) {
    try {
      return Resources.toString( Resources.getResource( componentId.getServiceModelFileName( ) ), Charset.defaultCharset( ) );
    } catch ( final Exception ex ) {
      return EMPTY_MODEL;
    }
  }
  
  private static ConfigResource createConfigResource( final ComponentId componentId, final String outString ) {
    final ByteArrayInputStream bis = new ByteArrayInputStream( outString.getBytes( ) );
    Logs.extreme( ).trace( "===================================" );
    Logs.extreme( ).trace( outString );
    Logs.extreme( ).trace( "===================================" );
    final ConfigResource configRsc = new ConfigResource( componentId.getServiceModelFileName( ), bis );
    return configRsc;
  }
  
  private static String FAIL_MSG = "ESB client not ready because the service bus has not been started.";
  
  static MuleClient getClient( ) throws MuleException {
    singleton.update( );
    return singleton.client;
  }
  
  static MuleContext getContext( ) throws MuleException {
    singleton.update( );
    return singleton.context;
  }
  
  private void stop( ) {
    this.canHasWrite.lock( );
    try {
      if ( this.context != null ) {
        try {
          this.context.stop( );
          this.context.dispose( );
        } catch ( final MuleException ex ) {
          LOG.error( ex, ex );
        }
      }
    } finally {
      this.canHasWrite.unlock( );
    }
  }
  
  public static void shutdown( ) {
    singleton.stop( );
  }
  
  public static String mapServiceToEndpoint( final String service ) throws Exception {
    checkParam( service, notNullValue() );
    if ( singleton.canHasRead.tryLock( 120, TimeUnit.SECONDS ) ) {
      try {
        String dest = service;
        if ( ( !service.startsWith( "vm://" ) && !singleton.serviceToEndpoint.containsKey( service ) ) ) {
          dest = "vm://RequestQueue";
        } else if ( !service.startsWith( "vm://" ) ) {
          dest = singleton.serviceToEndpoint.get( dest );
        }
        return dest;
      } finally {
        singleton.canHasRead.unlock( );
      }
    }
    throw Exceptions.notFound( "Failed to dispatch: " + service );
  }
  
  public static String mapEndpointToService( final String endpoint ) throws Exception {
    checkParam( endpoint, notNullValue() );
    if ( singleton.canHasRead.tryLock( 120, TimeUnit.SECONDS ) ) {
      try {
        String dest = endpoint;
        if ( ( endpoint.startsWith( "vm://" ) && !singleton.endpointToService.containsKey( endpoint ) ) ) {
          throw new ServiceDispatchException( "No such endpoint: " + endpoint
                                              + " in endpoints="
                                              + singleton.endpointToService.entrySet( ) );
          
        }
        if ( endpoint.startsWith( "vm://" ) ) {
          dest = singleton.endpointToService.get( endpoint );
        }
        return dest;
      } finally {
        singleton.canHasRead.unlock( );
      }
    }
    throw Exceptions.notFound( "Failed to dispatch: " + endpoint );
  }
  
}
