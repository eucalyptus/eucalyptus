/*************************************************************************
 * Copyright 2008 Regents of the University of California
 * Copyright 2009-2015 Ent. Services Development Corporation LP
 *
 * Redistribution and use of this software in source and binary forms,
 * with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *   Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 *   Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer
 *   in the documentation and/or other materials provided with the
 *   distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 * THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 * COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 * AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 * IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 * SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 * WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 * REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 * IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 * NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/

package com.eucalyptus.context;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Stream;
import org.apache.log4j.Logger;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import com.eucalyptus.bootstrap.Bootstrap;
import com.eucalyptus.bootstrap.Bootstrapper;
import com.eucalyptus.bootstrap.DelegatingResourcePatternResolver;
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
import com.eucalyptus.util.Classes;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.LockResource;
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
        } catch ( final Exception ex ) {
          LOG.error( ex, ex );
        }
      }, "bootstrap-service-context" ).start( );
      return true;
    }
  }

  private static Logger                                CONFIG_LOG        = Logger.getLogger( "Configs" );
  private static Logger                                LOG               = Logger.getLogger( ServiceContextManager.class );
  private static ServiceContextManager                 singleton         = new ServiceContextManager( );

  private final List<ComponentId>                      enabledCompIds    = Lists.newArrayList( );
  private final AtomicBoolean                          running           = new AtomicBoolean( true );
  private final Lock                                   canHasWrite;
  private final Lock                                   canHasRead;
  private final BlockingQueue<ServiceConfiguration>    queue             = new LinkedBlockingQueue<>();
  private volatile ConfigurableApplicationContext      context;

  private ServiceContextManager( ) {
    ReentrantReadWriteLock canHas = new ReentrantReadWriteLock();
    this.canHasRead = canHas.readLock( );
    this.canHasWrite = canHas.writeLock( );
    OrderedShutdown.registerPostShutdownHook(ServiceContextManager::shutdown);
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
      try ( final LockResource lock = LockResource.lock( canHasWrite ) ){
        if (context == null) {
          context = createContext();
          checkParam(context, notNullValue());
          try {
            this.context.start();
          } catch (final Exception e) {
            LOG.error(e, e);
            throw Exceptions.toUndeclared(new ServiceInitializationException("Failed to start service this.context.", e));
          }
        }
      }
    }
  }

  private static final String EMPTY_CONTEXT = "<beans xmlns=\"http://www.springframework.org/schema/beans\" " +
      "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " +
      "xsi:schemaLocation=\"http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans-3.2.xsd\"/>";

  private ConfigurableApplicationContext createContext( ) {
    // Many-to-one services must be ordered last so they do not receive messages
    // for backend components that use sub-classes of the frontend components.
    final List<ComponentId> currentComponentIds = Lists.newArrayList( Ordering.natural( )
        .onResultOf( Functions.forPredicate( ComponentIds.manyToOne( ) ) )
        .compound( Ordering.natural( ).onResultOf(
            Functions.compose( Classes.canonicalNameFunction( ), Functions.<ComponentId>identity( ) ) ) )
        .sortedCopy( ComponentIds.list( ) ) );
    LOG.info( "Restarting service context with these enabled services: " + currentComponentIds );
    final Set<Resource> configs = Sets.newHashSet( );
    ConfigurableApplicationContext context = null;
    for ( final ComponentId componentId : currentComponentIds ) {
      final Component component = Components.lookup( componentId );
      final String errMsg = "Failed to render model for: " + componentId + " because of: ";
      LOG.info( "-> Rendering configuration for " + componentId.name( ) );
      try {
        final String serviceModel = this.loadModel( componentId );
        final String outString = Templates.prepare( componentId.getServiceModelFileName( ) )
                                          .withProperty( "components", currentComponentIds )
                                          .withProperty( "ComponentMessages", ComponentMessages.class )
                                          .withProperty( "thisComponent", componentId )
                                          .evaluate( serviceModel );
        final Resource configRsc = createConfigResource( componentId, outString );
        configs.add( configRsc );
      } catch ( final Exception ex ) {
        LOG.error( errMsg + ex.getMessage( ), ex );
      }
    }
    try {
      final Resource[] configResources = configs.toArray( new Resource[ configs.size( ) ] );
      context = new ClassPathXmlApplicationContext(  ){
        @Override
        protected Resource[] getConfigResources( ) {
          return configResources;
        }

        @Override
        protected ResourcePatternResolver getResourcePatternResolver( ) {
          return new DelegatingResourcePatternResolver( super.getResourcePatternResolver( ) ) {
            @Override
            public Resource[] getResources( final String locationPattern ) throws IOException {
              return Stream.of( super.getResources( locationPattern ) )
                  .filter( resource ->
                      resource.getFilename( ) == null || !resource.getFilename( ).startsWith( "JiBX_" ) )
                  .toArray( Resource[]::new );
            }
          };
        }
      };
      context.refresh( );
      this.enabledCompIds.clear( );
      this.enabledCompIds.addAll( currentComponentIds );
    } catch ( final Exception ex ) {
      LOG.error( ex, ex );
    }
    return context;
  }

  private String loadModel( final ComponentId componentId ) {
    try {
      return Resources.toString( Resources.getResource( componentId.getServiceModelFileName( ) ), StandardCharsets.UTF_8 );
    } catch ( final Exception ex ) {
      return EMPTY_CONTEXT;
    }
  }

  private static Resource createConfigResource( final ComponentId componentId, final String outString ) {
    Logs.extreme( ).trace( "===================================" );
    Logs.extreme( ).trace( outString );
    Logs.extreme( ).trace( "===================================" );
    return new ByteArrayResource( outString.getBytes( ), componentId.getServiceModelFileName( ) );
  }

  private static String FAIL_MSG = "ESB client not ready because the service bus has not been started.";

  static ApplicationContext getContext( ) {
    singleton.update( );
    return singleton.context;
  }

  private void stop( ) {
    try ( final LockResource lock = LockResource.lock( this.canHasWrite ) ) {
      if ( this.context != null ) {
        try {
          this.context.stop( );
        } catch ( final Exception ex ) {
          LOG.error( ex, ex );
        }
        try {
          this.context.close( );
        } catch ( final Exception ex ) {
          LOG.error( ex, ex );
        }
      }
    }
  }

  public static void shutdown( ) {
    singleton.stop( );
  }

}
