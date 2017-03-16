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

package com.eucalyptus.component;

import java.net.URI;
import org.apache.log4j.Logger;
import com.eucalyptus.bootstrap.OrderedShutdown;
import com.eucalyptus.component.Component.State;
import com.eucalyptus.component.Component.Transition;
import com.eucalyptus.records.Logs;
import com.eucalyptus.util.fsm.StateMachine;
import com.google.common.base.MoreObjects;

public class BasicService {
  private static Logger                                                                   LOG            = Logger.getLogger( BasicService.class );
  private final ServiceConfiguration                                                      serviceConfiguration;
  private final StateMachine<ServiceConfiguration, Component.State, Component.Transition> stateMachine;
  private final State                                                                     goal           = Component.State.ENABLED;
  public static String                                                                    LOCAL_HOSTNAME = "@localhost";
  
  BasicService( final ServiceConfiguration serviceConfiguration ) {
    super( );
    this.serviceConfiguration = serviceConfiguration;
    this.stateMachine = new ServiceState( this.serviceConfiguration );
    URI remoteUri;
    if ( this.getServiceConfiguration( ).isVmLocal( ) ) {
      remoteUri = ServiceUris.internal( this.getServiceConfiguration( ).getComponentId( ) );
    } else {
      remoteUri = ServiceUris.internal( this.getServiceConfiguration( ) );
    }
    
    if ( this.serviceConfiguration.isVmLocal( ) ) {
      ComponentId compId = BasicService.this.serviceConfiguration.getComponentId( );
      OrderedShutdown.registerShutdownHook( compId.getClass( ), new Runnable( ) {
        @Override
        public void run( ) {
          try {
            ServiceTransitions.pathTo( BasicService.this.serviceConfiguration, Component.State.PRIMORDIAL ).get( );
            LOG.warn( "SHUTDOWN Service: " + BasicService.this.serviceConfiguration.getFullName( ) );
          } catch ( final InterruptedException ex ) {
            Thread.currentThread( ).interrupt( );
          } catch ( final Exception ex ) {
            LOG.error( ex );
            Logs.extreme( ).error( ex, ex );
          }
        }

        @Override
        public String toString( ) {
          return MoreObjects.toStringHelper( "BasicService.Runnable" )
              .add( "serviceConfiguration", BasicService.this.serviceConfiguration.getFullName( ) )
              .toString( );
        }
      } );
    }
  }
  
  public final String getName( ) {
    return this.serviceConfiguration.getFullName( ).toString( );
  }
  
  public Boolean isLocal( ) {
    return this.serviceConfiguration.isVmLocal( );
  }
  
  ServiceConfiguration getServiceConfiguration( ) {
    return this.serviceConfiguration;
  }
  
  @Override
  public String toString( ) {
    return String.format( "Service %s name=%s serviceConfiguration=%s\n",
                          this.getServiceConfiguration( ).getComponentId( ).name( ), this.getName( ), this.getServiceConfiguration( ) );
  }
  
  @Override
  public int hashCode( ) {
    final int prime = 31;
    int result = 1;
    result = prime * result + ( ( this.serviceConfiguration == null )
        ? 0
            : this.serviceConfiguration.hashCode( ) );
    return result;
  }
  
  @Override
  public boolean equals( final Object obj ) {
    if ( this == obj ) {
      return true;
    }
    if ( obj == null ) {
      return false;
    }
    if ( this.getClass( ) != obj.getClass( ) ) {
      return false;
    }
    final BasicService other = ( BasicService ) obj;
    if ( this.serviceConfiguration == null ) {
      if ( other.serviceConfiguration != null ) {
        return false;
      }
    } else if ( !this.serviceConfiguration.equals( other.serviceConfiguration ) ) {
      return false;
    }
    return true;
  }
  
  public int compareTo( final ServiceConfiguration that ) {
    return this.serviceConfiguration.compareTo( that );
  }

  public StateMachine<ServiceConfiguration, State, Transition> getStateMachine( ) {
    return this.stateMachine;
  }
}
