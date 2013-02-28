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

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.log4j.Logger;
import org.mule.DefaultMuleEvent;
import org.mule.DefaultMuleMessage;
import org.mule.DefaultMuleSession;
import org.mule.RequestContext;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.MuleSession;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.transport.DispatchException;
import org.mule.config.spring.SpringXmlConfigurationBuilder;
import org.mule.module.client.MuleClient;
import org.mule.transport.AbstractConnector;
import org.mule.transport.vm.VMMessageDispatcherFactory;
import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.principal.Principals;
import com.eucalyptus.auth.principal.User;
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
import com.eucalyptus.util.EucalyptusCloudException;
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
      endpoint = muleCtx.getRegistry( ).lookupEndpointFactory( ).getOutboundEndpoint( dest );
      if ( !endpoint.getConnector( ).isStarted( ) ) {
        endpoint.getConnector( ).start( );
      }
    } catch ( MuleException ex ) {
      LOG.error( ex, ex );
      throw new ServiceDispatchException( "Failed to dispatch message to " + dest + " caused by failure to obtain service endpoint reference: "
                                          + ex.getMessage( ), ex );
    }
    MuleMessage muleMsg = new DefaultMuleMessage( msg );
    MuleSession muleSession;
    try {
      muleSession = new DefaultMuleSession( muleMsg, ( ( AbstractConnector ) endpoint.getConnector( ) ).getSessionHandler( ),
                                                          ServiceContextManager.getContext( ) );
    } catch ( MuleException ex ) {
      LOG.error( ex, ex );
      throw new ServiceDispatchException( "Failed to dispatch message to " + dest + " caused by failure to contruct session: " + ex.getMessage( ), ex );
    }
    MuleEvent muleEvent = new DefaultMuleEvent( muleMsg, endpoint, muleSession, false );
    final Context ctx = msg instanceof BaseMessage
      ? Contexts.createWrapped( dest, ( BaseMessage ) msg )
      : null;
    try {
      dispatcherFactory.create( endpoint ).dispatch( muleEvent );
    } catch ( DispatchException ex ) {
      LOG.error( ex, ex );
      throw new ServiceDispatchException( "Error while dispatching message (" + msg + ")t o " + dest + " caused by: " + ex.getMessage( ), ex );
    } catch ( MuleException ex ) {
      LOG.error( ex, ex );
      throw new ServiceDispatchException( "Failed to dispatch message to " + dest + " caused by failure to obtain service dispatcher reference: "
                                          + ex.getMessage( ), ex );
    } 
    Threads.enqueue( Empyrean.class, ServiceContext.class, new Callable<Boolean>( ) {
      @Override
      public Boolean call( ) {
        try {
          TimeUnit.SECONDS.sleep( 60 );
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
      //TODO:STEVE: Review approach to impersonation
      //TODO:GRZE: Review approach to impersonation
      final String userId = ((BaseMessage)msg).getEffectiveUserId();
      try {
        if ( userId != null && !Principals.isFakeIdentify(userId) && ctx.hasAdministrativePrivileges() ) {
          final User user= Accounts.lookupUserById( userId );
          ctx.setUser( user );
        }
      } catch ( final AuthException e ) {
        throw new EucalyptusCloudException( "User not found: " + userId );
      }
    }
    try {
      MuleMessage reply = ServiceContextManager.getClient( ).sendDirect( dest, null, new DefaultMuleMessage( msg ) );
      
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
  
}
