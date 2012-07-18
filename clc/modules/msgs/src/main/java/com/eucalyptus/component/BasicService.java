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
 ************************************************************************/

package com.eucalyptus.component;

import java.net.URI;
import java.util.concurrent.ExecutionException;
import org.apache.log4j.Logger;
import com.eucalyptus.bootstrap.OrderedShutdown;
import com.eucalyptus.component.Component.State;
import com.eucalyptus.component.Component.Transition;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.empyrean.Empyrean;
import com.eucalyptus.util.fsm.StateMachine;

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
            LOG.warn( "SHUTDOWN Service: " + BasicService.this.serviceConfiguration.getName( ) );
            ServiceTransitions.pathTo( BasicService.this.serviceConfiguration, Component.State.PRIMORDIAL ).get( );
          } catch ( final InterruptedException ex ) {
            Thread.currentThread( ).interrupt( );
          } catch ( final ExecutionException ex ) {
            LOG.error( ex, ex );
          }
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
