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
import java.io.StringWriter;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicMarkableReference;
import java.util.concurrent.atomic.AtomicReference;
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
import com.eucalyptus.component.ComponentIds;
import com.eucalyptus.component.Components;
import com.eucalyptus.empyrean.Empyrean;
import com.eucalyptus.event.Event;
import com.eucalyptus.event.EventListener;
import com.eucalyptus.event.Hertz;
import com.eucalyptus.event.ListenerRegistry;
import com.eucalyptus.system.LogLevels;
import com.eucalyptus.system.Threads;
import com.eucalyptus.system.Threads.ThreadPool;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import edu.emory.mathcs.backport.java.util.concurrent.TimeUnit;

public class ServiceContextManager {
  private static Logger                                       LOG                            = Logger.getLogger( ServiceContextManager.class );
  private static Logger                                       CONFIG_LOG                     = Logger.getLogger( "Configs" );
  
  private static final VelocityEngine                         ve                             = new VelocityEngine( );
  static {
    ve.setProperty( RuntimeConstants.RUNTIME_LOG_LOGSYSTEM_CLASS,
                    "org.apache.velocity.runtime.log.Log4JLogChute" );
    ve.setProperty( "runtime.log.logsystem.log4j.logger", ServiceContextManager.class.toString( ) );
    ve.init( );
  }
  private static List<Component>                              last                           = Lists.newArrayList( );
  private static Integer                                      SERVICE_CONTEXT_RELOAD_TIMEOUT = 10 * 1000;
  private static String                                       FAIL_MSG                       = "ESB client not ready because the service bus has not been started.";
  private static final AtomicMarkableReference<MuleContext>   context                        = new AtomicMarkableReference<MuleContext>( null, false );
  private static final ConcurrentNavigableMap<String, String> endpointToService              = new ConcurrentSkipListMap<String, String>( );
  private static final ConcurrentNavigableMap<String, String> serviceToEndpoint              = new ConcurrentSkipListMap<String, String>( );
  private static final MuleContextFactory                     contextFactory                 = new DefaultMuleContextFactory( );
  private static SpringXmlConfigurationBuilder                builder;
  private static SpringXmlConfigurationBuilder                oldBuilder;
  
  private static final AtomicReference<MuleClient>            client                         = new AtomicReference<MuleClient>( null );
  private static final ThreadPool                             ctxMgmtThreadPool              = Threads.lookup( Empyrean.class, ServiceContext.class ).limitTo( 1 );
  
  static MuleClient getClient( ) throws MuleException {
    boolean[] bit = new boolean[1];
    MuleContext muleCtx = context.get( bit );
    if ( context.getReference( ) == null ) {
      BootstrapException failEx = new BootstrapException( FAIL_MSG );
      throw failEx;
    } else if ( client.get( ) == null && client.compareAndSet( null, new MuleClient( context.getReference( ) ) ) ) {
      return client.get( );
    } else {
      return client.get( );
    }
  }
  
  public static final void restart( ) {
    if ( !Bootstrap.isFinished( ) ) {
//      caller = new Callable<MuleContext>( ) {
//        @Override
//        public MuleContext call( ) throws Exception {
//          try {
//            while ( !Bootstrap.isFinished( ) ) {
//              TimeUnit.MILLISECONDS.sleep( 30 );
//            }
//            startup( );
//          } catch ( Throwable ex ) {
//            LOG.error( ex, ex );
//          }
//          return context.getReference( );
//        }
//      };
//      ctxMgmtThreadPool.submit( caller );
      LOG.error( "Ignoring spurious context restart trigger duing system bootstrap." );
    } else {
      ctxMgmtThreadPool.submit( new Callable<MuleContext>( ) {
        @Override
        public MuleContext call( ) throws Exception {
          try {
            startup( );
          } catch ( Throwable ex ) {
            LOG.error( ex, ex );
          }
          return context.getReference( );
        }
      } );
    }
  }
  
  static String mapEndpointToService( String endpoint ) throws ServiceDispatchException {
    String dest = endpoint;
    if ( ( endpoint.startsWith( "vm://" ) && !endpointToService.containsKey( endpoint ) ) || endpoint == null ) {
      throw new ServiceDispatchException( "Failed to find destination: " + endpoint, new IllegalArgumentException( "No such endpoint: " + endpoint
                                                                                                                   + " in endpoints="
                                                                                                                   + endpointToService.entrySet( ) ) );
    }
    if ( endpoint.startsWith( "vm://" ) ) {
      dest = endpointToService.get( endpoint );
    }
    return dest;
  }
  
  static String mapServiceToEndpoint( String service ) {
    String dest = service;
    if ( ( !service.startsWith( "vm://" ) && !serviceToEndpoint.containsKey( service ) ) || service == null ) {
      dest = "vm://RequestQueue";
    } else if ( !service.startsWith( "vm://" ) ) {
      dest = serviceToEndpoint.get( dest );
    }
    return dest;
  }
  
  static boolean loadContext( ) {
    LOG.info( "The following components have been identified as active: " );
    List<Component> components = Components.whichAreEnabledLocally( );
    for ( Component c : components ) {
      LOG.info( "-> " + c.getComponentId( ) );
    }
    Set<ConfigResource> configs;
    try {
      configs = renderServiceConfigurations( components );
      for ( ConfigResource cfg : configs ) {
        LOG.info( "-> Rendered cfg: " + cfg.getResourceName( ) );
      }
      try {
        if ( builder != null ) {
          oldBuilder = builder;
        } else {
          builder = new SpringXmlConfigurationBuilder( configs.toArray( new ConfigResource[] {} ) );
        }
      } catch ( Exception e ) {
        if ( oldBuilder != null ) {
          builder = oldBuilder;
        }
        LOG.fatal( "Failed to bootstrap services.", e );
        return false;
      }
    } catch ( Throwable ex ) {
      LOG.error( ex, ex );
    }
    return true;
  }
  
  private static Set<ConfigResource> renderServiceConfigurations( List<Component> components ) {
    Set<ConfigResource> configs = Sets.newHashSet( );
    for ( Component component : components ) {
      VelocityContext context = new VelocityContext( );
      context.put( "components", Components.toIds( components ) );
      context.put( "thisComponent", component.getComponentId( ) );
      LOG.info( "-> Rendering configuration for " + component.getComponentId( ).name( ) );
      StringWriter out = new StringWriter( );
      try {
        ve.evaluate( context, out, component.getComponentId( ).getServiceModelFileName( ), component.getComponentId( ).getServiceModelAsReader( ) );
        out.flush( );
        out.close( );
        String outString = out.toString( );
        ByteArrayInputStream bis = new ByteArrayInputStream( outString.getBytes( ) );
        if ( LogLevels.EXTREME ) {
          CONFIG_LOG.trace( "===================================" );
          CONFIG_LOG.trace( outString );
          CONFIG_LOG.trace( "===================================" );
        }
        ConfigResource configRsc = new ConfigResource( component.getComponentId( ).getServiceModelFileName( ), bis );
        configs.add( configRsc );
      } catch ( Throwable ex ) {
        LOG.error( "Failed to render service model configuration for: " + component.getComponentId( ) + " because of: " + ex.getMessage( ), ex );
      }
    }
    return configs;
  }
  
  public static void shutdown( ) {
    MuleContext muleCtx = context.getReference( );
    if ( muleCtx != null ) {
      context.compareAndSet( muleCtx, null, false, true );
      try {
        shutdownContext( muleCtx );
      } finally {
        context.compareAndSet( null, null, true, false );
      }
    } else {
      context.compareAndSet( null, null, false, false );
    }
  }
  
  public static MuleContext getContext( ) throws ServiceInitializationException, ServiceStateException {
    boolean[] bit = new boolean[1];
    MuleContext ref = null;
    if ( ( ref = context.get( bit ) ) == null && bit[0] ) {
      Integer i = 0;
      do {
        try {
          TimeUnit.MILLISECONDS.sleep( 500 );
          i += 500;
          LOG.trace( "Waiting for service context to start." );
        } catch ( InterruptedException ex ) {
          LOG.error( ex, ex );
        }
      } while ( i < SERVICE_CONTEXT_RELOAD_TIMEOUT && ( ref = context.get( bit ) ) == null && bit[0] );
      if ( ref == null ) {
        throw new ServiceStateException( "Timed-out obtaining reference service context.  Waited for : " + SERVICE_CONTEXT_RELOAD_TIMEOUT + " milliseconds." );
      } else {
        return ref;
      }
    } else if ( ref == null && !bit[0] ) {
      try {
        for ( int i = 0; ( ref = context.getReference( ) ) == null && i < 15; i++ ) {
          LOG.trace( "Waiting for service context to start." );
          TimeUnit.MILLISECONDS.sleep( 1000 );
        }
        if ( ref == null ) {
          throw new ServiceStateException( "Attempt to reference service context before it is ready." );
        } else {
          return ref;
        }
      } catch ( InterruptedException ex ) {
        LOG.error( ex, ex );
        throw new ServiceStateException( "Attempt to reference service context before it is ready." );
      }
    } else {
      return ref;
    }
  }
  
  public static boolean startup( ) throws ServiceInitializationException {
    try {
      LOG.info( "Loading system bus." );
      loadContext( );
    } catch ( Exception e ) {
      LOG.fatal( "Failed to configure services.", e );
      return false;
    }
    try {
      MuleContext muleCtx = context.getReference( );
      if ( context.compareAndSet( null, null, false, true ) ) {
        MuleContext newCtx = createContext( );
        startContext( newCtx );
        context.compareAndSet( null, newCtx, true, false );
      } else if ( context.compareAndSet( muleCtx, null, false, true ) ) {//already had a context, assume it needs shutdown
        MuleContext newCtx = null;
        try {
          shutdownContext( muleCtx );
          newCtx = createContext( );
          startContext( newCtx );
        } finally {
          context.compareAndSet( null, newCtx, true, false );
        }
      }
    } catch ( Throwable ex ) {
      LOG.error( ex, ex );
      return false;
    }
    return true;
  }
  
  private static void startContext( MuleContext ctx ) throws ServiceInitializationException {
    try {
      if ( !ctx.isInitialised( ) ) {
        ctx.initialise( );
      }
    } catch ( Throwable e ) {
      LOG.error( e, e );
      throw new ServiceInitializationException( "Failed to initialize service context.", e );
    }
    try {
      ctx.start( );
      endpointToService.clear( );
      serviceToEndpoint.clear( );
      for ( Object o : ctx.getRegistry( ).lookupServices( ) ) {
        Service s = ( Service ) o;
        for ( Object p : s.getInboundRouter( ).getEndpoints( ) ) {
          InboundEndpoint in = ( InboundEndpoint ) p;
          endpointToService.put( in.getEndpointURI( ).toString( ), s.getName( ) );
          serviceToEndpoint.put( s.getName( ), in.getEndpointURI( ).toString( ) );
        }
      }
    } catch ( Throwable e ) {
      LOG.error( e, e );
      throw new ServiceInitializationException( "Failed to start service context.", e );
    }
  }
  
  private static void shutdownContext( MuleContext ctx ) {
    try {
      ctx.stop( );
      ctx.dispose( );
    } catch ( Exception ex ) {
      LOG.trace( ex, ex );
    }
  }
  
  private static synchronized MuleContext createContext( ) throws ServiceInitializationException {
    List<Component> components = Components.whichAreEnabledLocally( );
    if ( !checkStateChanged( components ) && context.getReference( ) != null ) {
      return context.getReference( );
    } else {
      LOG.info( "The following components have been identified as active: " );
      for ( Component c : components ) {
        LOG.info( "-> " + c.getComponentId( ) );
      }
      Set<ConfigResource> configs = renderServiceConfigurations( components );
      for ( ConfigResource cfg : configs ) {
        LOG.info( "-> Rendered cfg: " + cfg.getResourceName( ) );
      }
      try {
        builder = new SpringXmlConfigurationBuilder( configs.toArray( new ConfigResource[] {} ) );
      } catch ( Throwable ex ) {
        LOG.fatal( "Failed to bootstrap services.", ex );
        throw new ServiceInitializationException( "Failed to bootstrap service context because of: " + ex.getMessage( ), ex );
      }
      MuleContext muleCtx;
      try {
        muleCtx = contextFactory.createMuleContext( builder );
        last = components;
      } catch ( InitialisationException ex ) {
        LOG.error( ex, ex );
        throw new ServiceInitializationException( "Failed to initialize service context because of: " + ex.getMessage( ), ex );
      } catch ( ConfigurationException ex ) {
        LOG.error( ex, ex );
        throw new ServiceInitializationException( "Failed to initialize service context because of: " + ex.getMessage( ), ex );
      }
      return muleCtx;
    }
  }
  
  public static class ReloadListener implements EventListener<Event> {
    public static void register( ) {
      ListenerRegistry.getInstance( ).register( Hertz.class, new ReloadListener( ) );
    }
    
    @Override
    public void fireEvent( Event event ) {
      if ( event instanceof Hertz ) {
        check( );
      }
    }
    
  }
  
  public static boolean check( ) {
    if ( checkStateChanged( Components.whichAreEnabledLocally( ) ) ) {
      ServiceContextManager.restart( );
    } else if ( context.getReference( ) == null ) {
      try {
        ServiceContextManager.startup( );
      } catch ( ServiceInitializationException ex ) {
        LOG.error( ex, ex );
      }
    }
    return true;
  }
  
  private static boolean checkStateChanged( List<Component> components ) {
    return last.isEmpty( ) || ( last.containsAll( components ) && last.size( ) == components.size( ) );
  }
  
}
