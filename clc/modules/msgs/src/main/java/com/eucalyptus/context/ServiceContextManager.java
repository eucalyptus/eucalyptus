/*************************************************************************
 * Copyright 2009-2015 Eucalyptus Systems, Inc.
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
import java.util.LinkedList;
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
import org.mule.MessageExchangePattern;
import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.context.MuleContextFactory;
import org.mule.api.endpoint.EndpointBuilder;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.service.Service;
import org.mule.api.transformer.Transformer;
import org.mule.config.ConfigResource;
import org.mule.config.DefaultMuleConfiguration;
import org.mule.config.spring.SpringXmlConfigurationBuilder;
import org.mule.context.DefaultMuleContextBuilder;
import org.mule.context.DefaultMuleContextFactory;
import org.mule.endpoint.EndpointURIEndpointBuilder;
import org.mule.module.client.MuleClient;
import org.mule.service.ServiceCompositeMessageSource;
import org.mule.transformer.TransformerUtils;
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
import com.eucalyptus.system.BaseDirectory;
import com.eucalyptus.util.Classes;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.Templates;
import com.google.common.base.Functions;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
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
      new Thread(() -> {
        try {
          singleton.update( );
          ServiceContextManager.getClient( );
        } catch ( final Exception ex ) {
          LOG.error( ex, ex );
        }
      }).start( );
      return true;
    }
  }
  
  private static Logger                                CONFIG_LOG        = Logger.getLogger( "Configs" );
  private static Logger                                LOG               = Logger.getLogger( ServiceContextManager.class );
  private static ServiceContextManager                 singleton         = new ServiceContextManager( );
  
  private static final MuleContextFactory              contextFactory    = new DefaultMuleContextFactory( );
  private final ConcurrentNavigableMap<String, String> endpointToService = new ConcurrentSkipListMap<>();
  private final ConcurrentNavigableMap<String, String> serviceToEndpoint = new ConcurrentSkipListMap<>();
  private final List<ComponentId>                      enabledCompIds    = Lists.newArrayList( );
  private final AtomicBoolean                          running           = new AtomicBoolean( true );
  private final Lock                                   canHasWrite;
  private final Lock                                   canHasRead;
  private final BlockingQueue<ServiceConfiguration>    queue             = new LinkedBlockingQueue<>();
  private volatile MuleContext                         context;
  private MuleClient                                   client;
  
  private ServiceContextManager( ) {
    ReentrantReadWriteLock canHas = new ReentrantReadWriteLock();
    this.canHasRead = canHas.readLock( );
    this.canHasWrite = canHas.writeLock( );
    OrderedShutdown.registerPreShutdownHook(ServiceContextManager::shutdown);
  }
  
  public static void restartSync( ) {
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
    if (context == null) {
      canHasWrite.lock();
      if (context == null) {
        try {
          context = createContext();
          checkParam(context, notNullValue());
          try {
            this.context.start();
            // Extend MuleClient to override method with fix as per pull request:
            // - https://github.com/mulesoft/mule/pull/124
            this.client = new MuleClient(this.context) {
              @Override
              protected InboundEndpoint getDefaultClientEndpoint(final Service service, final Object payload, final boolean sync) throws MuleException {
                if (!(service.getMessageSource() instanceof ServiceCompositeMessageSource)) {
                  throw new IllegalStateException(
                      "Only 'CompositeMessageSource' is supported with MuleClient.sendDirect() and MuleClient.dispatchDirect()");
                }

                // as we are bypassing the message transport layer we need to check that
                InboundEndpoint endpoint = ((ServiceCompositeMessageSource) service.getMessageSource()).getEndpoints().get(0);
                if (endpoint != null) {
                  List<Transformer> transformers = endpoint.getTransformers();
                  if (transformers != null && !transformers.isEmpty()) {
                    // the original code here really did just check the first exception
                    // as far as i can tell
                    if (TransformerUtils.isSourceTypeSupportedByFirst(transformers,
                        payload.getClass())) {
                      return endpoint;
                    } else {
                      EndpointBuilder builder = new EndpointURIEndpointBuilder(endpoint);
                      builder.setTransformers(new LinkedList<>());
                      builder.setExchangePattern(MessageExchangePattern.REQUEST_RESPONSE);
                      return getMuleContext().getEndpointFactory().getInboundEndpoint(builder);
                    }
                  } else {
                    return endpoint;
                  }
                } else {
                  EndpointBuilder builder = new EndpointURIEndpointBuilder("vm://mule.client", getMuleContext());
                  builder.setName("muleClientProvider");
                  endpoint = getMuleContext().getEndpointFactory().getInboundEndpoint(builder);
                }
                return endpoint;
              }
            };
            this.endpointToService.clear();
            this.serviceToEndpoint.clear();
            for (final Service service : this.context.getRegistry().lookupObjects(Service.class)) {
              final ServiceCompositeMessageSource source = (ServiceCompositeMessageSource) service.getMessageSource();
              for (final InboundEndpoint in : source.getEndpoints()) {
                this.endpointToService.put(in.getEndpointURI().toString(), service.getName());
                this.serviceToEndpoint.put(service.getName(), in.getEndpointURI().toString());
              }
            }
          } catch (final Exception e) {
            LOG.error(e, e);
            throw Exceptions.toUndeclared(new ServiceInitializationException("Failed to start service this.context.", e));
          }
        } finally {
          this.canHasWrite.unlock();
        }
      }
    }
  }
                                 
  private static final String EMPTY_MODEL = "<mule xmlns=\"http://www.mulesoft.org/schema/mule/core\"\n"
                                            +
                                            "      xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n"
                                            +
                                            "      xsi:schemaLocation=\"\n"
                                            +
                                            "       http://www.mulesoft.org/schema/mule/core http://www.mulesoft.org/schema/mule/core/3.4/mule.xsd\">\n"
                                            +
                                            "</mule>\n";
  
  private MuleContext createContext( ) {
    // Many-to-one services must be ordered last so they do not receive messages
    // for backend components that use sub-classes of the frontend components.
    final List<ComponentId> currentComponentIds = Lists.newArrayList( Ordering.natural( )
        .onResultOf( Functions.forPredicate( ComponentIds.manyToOne( ) ) )
        .compound( Ordering.natural( ).onResultOf( 
            Functions.compose( Classes.canonicalNameFunction( ), Functions.<ComponentId>identity( ) ) ) )
        .sortedCopy( ComponentIds.list( ) ) );
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
      final DefaultMuleContextBuilder muleContextBuilder = new DefaultMuleContextBuilder( );
      final DefaultMuleConfiguration muleConfiguration = new DefaultMuleConfiguration( );
      muleConfiguration.setWorkingDirectory( BaseDirectory.RUN.getChildPath( "mule" ) );
      muleContextBuilder.setMuleConfiguration( muleConfiguration );
      muleCtx = contextFactory.createMuleContext( builder, muleContextBuilder );
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
    return new ConfigResource( componentId.getServiceModelFileName( ), bis );
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
