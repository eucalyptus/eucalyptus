/*************************************************************************
 * Copyright 2009-2013 Eucalyptus Systems, Inc.
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

import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.log4j.Logger;
import org.mule.DefaultMuleEvent;
import org.mule.RequestContext;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.MuleSession;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.transport.Connector;
import org.mule.api.transport.ConnectorException;
import org.mule.api.transport.DispatchException;
import org.mule.api.transport.MessageDispatcher;
import org.mule.config.i18n.MessageFactory;
import org.mule.config.spring.SpringXmlConfigurationBuilder;
import org.mule.module.client.MuleClient;
import org.mule.session.DefaultMuleSession;
import org.mule.transport.vm.VMConnector;
import org.mule.transport.vm.VMMessageDispatcherFactory;
import com.eucalyptus.bootstrap.Bootstrap;
import com.eucalyptus.bootstrap.BootstrapException;
import com.eucalyptus.component.ComponentId;
import com.eucalyptus.configurable.ConfigurableClass;
import com.eucalyptus.configurable.ConfigurableField;
import com.eucalyptus.configurable.ConfigurableProperty;
import com.eucalyptus.configurable.ConfigurablePropertyException;
import com.eucalyptus.configurable.PropertyChangeListener;
import com.eucalyptus.empyrean.Empyrean;
import com.eucalyptus.system.Threads;
import com.eucalyptus.util.Exceptions;
import edu.ucsb.eucalyptus.msgs.BaseMessage;

@ConfigurableClass( root = "bootstrap.servicebus", description = "Parameters having to do with the service bus." )
public class ServiceContext {
  static Logger                                LOG                      = Logger.getLogger( ServiceContext.class );
  private static SpringXmlConfigurationBuilder builder;
  @ConfigurableField( initial = "256", description = "Max queue length allowed per service stage.", changeListener = HupListener.class )
  public static Integer                        MAX_OUTSTANDING_MESSAGES = 256;
  @ConfigurableField( initial = "16", description = "Max queue length allowed per service stage.", changeListener = HupListener.class )
  public static Integer                        WORKERS_PER_STAGE        = 16;                                      //TODO:GRZE: finish this thought later.
  @ConfigurableField( initial = "0", description = "Do a soft reset.", changeListener = HupListener.class )
  public static Integer                        HUP                      = 0;
  @ConfigurableField( initial = "64", description = "Internal connector core pool size." )
  public static Integer                        MIN_SCHEDULER_CORE_SIZE  = 64;
  public static Integer                        CONTEXT_TIMEOUT          = Integer.parseInt( System.getProperty( "com.eucalyptus.context.timeoutSeconds", "60" ) );

  public static class HupListener implements PropertyChangeListener {
    @Override
    public void fireChange( ConfigurableProperty t, Object newValue ) throws ConfigurablePropertyException {
      if ( Bootstrap.isFinished( ) ) {
        ServiceContextManager.restartSync( );
      }
    }
  }
  
  private static final VMMessageDispatcherFactory  dispatcherFactory = new VMMessageDispatcherFactory( );
  private static final AtomicReference<MuleClient> client            = new AtomicReference<MuleClient>( null );
  private static final BootstrapException          failEx            = new BootstrapException(
                                                                                                    "Attempt to use esb client before the service bus has been started." );
  
  public static void dispatch( String dest, Object msg ) throws Exception {
    dest = ServiceContextManager.mapServiceToEndpoint( dest );
    MuleContext muleCtx;
    try {
      muleCtx = ServiceContextManager.getContext( );
    } catch ( Exception ex ) {
      LOG.error( ex, ex );
      throw new ServiceDispatchException( "Failed to dispatch message to " + dest + " caused by failure to obtain service context reference: "
                                          + ex.getMessage( ), ex );
    }
    OutboundEndpoint endpoint;
    try {
      endpoint = muleCtx.getEndpointFactory( ).getOutboundEndpoint( dest );
      perhapsConfigureConnector( endpoint.getConnector( ) );
    } catch ( MuleException ex ) {
      LOG.error( ex, ex );
      throw new ServiceDispatchException( "Failed to dispatch message to " + dest + " caused by failure to obtain service endpoint reference: "
                                          + ex.getMessage( ), ex );
    }
    MuleSession muleSession = new DefaultMuleSession( );
    final Context ctx = msg instanceof BaseMessage
      ? Contexts.createWrapped( dest, ( BaseMessage ) msg )
      : null;
    MessageDispatcher dispatcher = null;
    try {
      dispatcher = dispatcherFactory.create( endpoint );
      dispatcher.initialise( );
      dispatcher.start( );
      MuleMessage muleMsg = dispatcher.createMuleMessage( msg );
      MuleEvent muleEvent = new DefaultMuleEvent( muleMsg, endpoint.getExchangePattern(), (FlowConstruct) null, muleSession );
      dispatcher.process( muleEvent );
    } catch ( DispatchException ex ) {
      LOG.error( ex, ex );
      throw new ServiceDispatchException( "Error while dispatching message (" + msg + ") to " + dest + " caused by: " + ex.getMessage( ), ex );
    } catch ( MuleException ex ) {
      LOG.error( ex, ex );
      throw new ServiceDispatchException( "Failed to dispatch message to " + dest + " caused by failure to obtain service dispatcher reference: "
                                          + ex.getMessage( ), ex );
    } finally {
      if ( dispatcher != null ) dispatcher.dispose( );
    }
    final long clearContextTime = System.currentTimeMillis( ) + TimeUnit.SECONDS.toMillis( CONTEXT_TIMEOUT );
    Threads.enqueue( Empyrean.class, ServiceContext.class, new Callable<Boolean>( ) {
      @Override
      public Boolean call( ) {
        try {
          long sleepTime = clearContextTime - System.currentTimeMillis( );
          if ( sleepTime > 1 ) {
            Thread.sleep( sleepTime );
          }
          Contexts.clear( ctx );
        } catch ( InterruptedException ex ) {
          Thread.currentThread( ).interrupt( );
        }
        return true;
      }
    } );
  }
  
  public static <T> T send( ComponentId dest, Object msg ) throws Exception {
    return send( dest.getLocalEndpointName( ), msg );
  }
  
  public static <T> T send( String dest, Object msg ) throws Exception {
    dest = ServiceContextManager.mapEndpointToService( dest );
    MuleEvent context = RequestContext.getEvent( );
    Context ctx = null;
    if ( msg instanceof BaseMessage ) {
      ctx = Contexts.createWrapped( dest, ( BaseMessage ) msg );
    }
    try {
      MuleMessage reply = ServiceContextManager.getClient( ).sendDirect( dest, null, msg, null );
      
      if ( reply.getExceptionPayload( ) != null ) {
        throw Exceptions.trace( new ServiceDispatchException( reply.getExceptionPayload( ).getRootException( ).getMessage( ),
                                                                    reply.getExceptionPayload( ).getRootException( ) ) );
      } else {
        return ( T ) reply.getPayload( );
      }
    } catch ( Exception e ) {
      throw Exceptions.trace( new ServiceDispatchException( "Failed to send message " + msg.getClass( ).getSimpleName( ) + " to service " + dest
                                                                  + " because: " + e.getMessage( ), e ) );
    } finally {
      if ( ctx != null ) {
        Contexts.clear( ctx );
      }
      RequestContext.setEvent( context );
    }
  }
  
  private static void perhapsConfigureConnector( final Connector connector ) throws MuleException {
    if ( !connector.isStarted( ) ) try {
      connector.start( );
    } catch ( IllegalArgumentException e ) {
      if ( !connector.isStarted( ) ) {
        throw new ConnectorException( MessageFactory.createStaticMessage( "Error starting connector" ), connector,  e );  
      }
    }
    
    if ( connector instanceof VMConnector ) {
      final VMConnector vmConnector = (VMConnector) connector;
      final ScheduledExecutorService scheduledExecutorService = vmConnector.getScheduler();
      if ( scheduledExecutorService instanceof ScheduledThreadPoolExecutor ) {
        final ScheduledThreadPoolExecutor threadPoolExecutor = 
            (ScheduledThreadPoolExecutor) scheduledExecutorService;  
        if ( threadPoolExecutor.getCorePoolSize( ) < MIN_SCHEDULER_CORE_SIZE  ) {
          threadPoolExecutor.setCorePoolSize( MIN_SCHEDULER_CORE_SIZE );   
        }
      }
    }
  }
}
