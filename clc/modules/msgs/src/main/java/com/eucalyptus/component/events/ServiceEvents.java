package com.eucalyptus.component.events;

/*************************************************************************
 * Copyright 2009-2013 Eucalyptus Systems, Inc.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 3 of the License.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 * 
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 * 
 * This file may incorporate work covered under the following copyright
 * and permission notice:
 * 
 * Software License Agreement (BSD License)
 * 
 * Copyright (c) 2008, Regents of the University of California
 * All rights reserved.
 * 
 * Redistribution and use of this software in source and binary forms,
 * with or without modification, are permitted provided that the
 * following conditions are met:
 * 
 * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 * 
 * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer
 * in the documentation and/or other materials provided with the
 * distribution.
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

import java.util.concurrent.Future;

import com.eucalyptus.component.Topology;
import com.google.common.base.Joiner;
import org.apache.log4j.Logger;

import com.eucalyptus.bootstrap.Host;
import com.eucalyptus.bootstrap.Hosts;
import com.eucalyptus.component.Component;
import com.eucalyptus.component.ComponentRegistrationHandler;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.ServiceConfigurations;
import com.eucalyptus.empyrean.DisableServiceType;
import com.eucalyptus.empyrean.Empyrean;
import com.eucalyptus.empyrean.EnableServiceType;
import com.eucalyptus.empyrean.ServiceId;
import com.eucalyptus.empyrean.ServiceTransitionType;
import com.eucalyptus.empyrean.StartServiceType;
import com.eucalyptus.empyrean.StopServiceType;
import com.eucalyptus.event.Event;
import com.eucalyptus.event.EventFailedException;
import com.eucalyptus.event.ListenerRegistry;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.TypeMappers;
import com.eucalyptus.util.async.AsyncRequests;
import com.eucalyptus.util.async.Futures;
import com.google.common.base.Function;

/**
 * @author chris grzegorczyk <grze@eucalyptus.com>
 */
public class ServiceEvents {
  private static Logger LOG = Logger.getLogger( ServiceEvents.class );
  
  public static void fire( final ServiceConfiguration config, Component.State state ) {
    try {
      ServiceTransitionType msg = null;
      switch ( state ) {
        case ENABLED:
          msg = new EnableServiceType( );
          break;
        case DISABLED:
          msg = new DisableServiceType( );
          break;
        case STOPPED:
          msg = new StopServiceType( );
          break;
//TODO:GRZE: Handle other state transitions here.
//        case INITIALIZED:
//        case LOADED:
        case NOTREADY:
          msg = new StartServiceType( );
          break;
        default:
          break;
      }
      if ( msg != null ) {
        msg.getServices( ).add( TypeMappers.transform( config, ServiceId.class ) );
        for ( Host h : Hosts.list( ) ) {
          if ( !h.isLocalHost( ) && !h.getHostAddresses( ).contains( config.getInetAddress( ) ) ) {
            try {
              AsyncRequests.sendSync( ServiceConfigurations.createEphemeral( Empyrean.INSTANCE, h.getBindAddress( ) ), msg );
            } catch ( Exception ex ) {
              LOG.error( ex, ex );
            }
          }
        }
      }
    } catch ( Exception ex ) {
      LOG.error( ex, ex );
    }
  }
  
  public static RegistrationBuilder forConfiguration( final ServiceConfiguration configuration ) {
    return new RegistrationBuilder( ) {
      {
        this.config = configuration;
      }
    };
  }
  
  public enum DeregisterFunction implements Function<ServiceConfiguration, Future<ServiceConfiguration>> {
    INSTANCE;
    
    @Override
    public Future<ServiceConfiguration> apply( ServiceConfiguration input ) {
      return ServiceEvents.forConfiguration( input ).deregister( );
    }
  }
  
  public static Function<? super ServiceConfiguration, Future<ServiceConfiguration>> deregisterFunction( ) {
    return DeregisterFunction.INSTANCE;
  }
  
  public enum RegisterFunction implements Function<ServiceConfiguration, Future<ServiceConfiguration>> {
    INSTANCE;
    
    @Override
    public Future<ServiceConfiguration> apply( ServiceConfiguration input ) {
      return ServiceEvents.forConfiguration( input ).register( );
    }
  }
  
  public static Function<? super ServiceConfiguration, Future<ServiceConfiguration>> registerFunction( ) {
    return RegisterFunction.INSTANCE;
  }
  
  public static class RegistrationBuilder {
    ServiceConfiguration config;
    
    public Future<ServiceConfiguration> register( ) {
      LOG.debug( "Registering: " + config );
      try {
        ServiceConfiguration resultConfig = ComponentRegistrationHandler.register( config.getComponentId( ),
                                                                                   config.getPartition( ),
                                                                                   config.getName( ),
                                                                                   config.getHostName( ),
                                                                                   config.getPort( ) );
        if ( resultConfig != null ) {
          ServiceEvent event = new RegistrationEvent( ).setConfiguration( resultConfig );
          try {
            ListenerRegistry.getInstance( ).fireEvent( event );
          } catch ( EventFailedException e ) {
            LOG.trace( e );
          }
          return Futures.predestinedFuture( resultConfig );
        } else {
          return Futures.predestinedFuture( config );//GRZE:SIGH: where is this case actually needed...
        }
      } catch ( Exception e ) {
        final String errorMessage = Joiner.on( " " ).join( "Registering ", config.getFullName( ), " failed because of ", e.getMessage( ) );
        LOG.debug( errorMessage );
        LOG.trace( e, e );
        return Futures.<ServiceConfiguration> predestinedFailedFuture( Exceptions.toUndeclared( errorMessage ) );
     }
    }
    
    public Future<ServiceConfiguration> deregister( ) {
      LOG.debug( "Deregistering: " + config );
      try {
        ServiceConfiguration resultConfig = ComponentRegistrationHandler.deregister( config.getComponentId( ), config.getName( ) );
        if ( resultConfig != null ) {
          DeregistrationEvent event = new DeregistrationEvent().setConfiguration( resultConfig );
          try {
            ListenerRegistry.getInstance().fireEvent( event );
          } catch ( EventFailedException e ) {
            LOG.trace( e );
          }
          try {
            Topology.destroy( resultConfig ).get();
          } catch ( Exception e ) {
            LOG.error( e );
          }
          return Futures.predestinedFuture( resultConfig );
        } else {
          return Futures.predestinedFuture( config );//GRZE:SIGH: where is this case actually needed...
        }
      } catch ( Exception e ) {
        final String errorMessage = Joiner.on( " " ).join( "Deregistering ", config.getFullName( ), " failed because of ", e.getMessage( ) );
        LOG.debug( errorMessage );
        LOG.trace( e, e );
        return Futures.<ServiceConfiguration> predestinedFailedFuture( Exceptions.toUndeclared( errorMessage ) );
      }
    }
    
  }
  
  public static class ServiceEvent implements Event {
    
    private ServiceConfiguration configuration;
    
    public ServiceConfiguration getConfiguration( ) {
      return configuration;
    }
    
    public <T extends ServiceEvent> T setConfiguration( ServiceConfiguration configuration ) {
      this.configuration = configuration;
      return ( T ) this;
    }
    
  }
  
  public static class EnabledEvent extends ServiceEvent {}
  
  public static class DisabledEvent extends ServiceEvent {}
  
  public static class NotreadyEvent extends ServiceEvent {}
  
  public static class StartedEvent extends ServiceEvent {}
  
  public static class StoppedEvent extends ServiceEvent {}
  
  public static class RegistrationEvent extends ServiceEvent {}
  
  public static class UpdateRegistrationEvent extends ServiceEvent {}
  
  public static class DeregistrationEvent extends ServiceEvent {}
}
