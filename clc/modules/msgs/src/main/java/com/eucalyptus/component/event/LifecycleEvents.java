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
 *    THE REGENTS DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
 *    OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
 *    WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
 *    ANY SUCH LICENSES OR RIGHTS.
 *******************************************************************************
 * @author chris grzegorczyk <grze@eucalyptus.com>
 */

package com.eucalyptus.component.event;

import java.util.Date;
import java.util.List;
import java.util.UUID;
import org.apache.log4j.Logger;
import com.eucalyptus.component.Component;
import com.eucalyptus.component.Components;
import com.eucalyptus.component.ServiceCheckRecord;
import com.eucalyptus.component.ServiceChecks;
import com.eucalyptus.component.ServiceChecks.CheckException;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.Topology;
import com.eucalyptus.empyrean.ServiceStatusType;
import com.eucalyptus.event.GenericEvent;
import com.google.common.collect.Lists;
import edu.emory.mathcs.backport.java.util.Arrays;

public class LifecycleEvents {
  private static Logger LOG = Logger.getLogger( LifecycleEvents.class );
  
  public static class Enable extends AbstractLifecycleEvent {
    public Enable( ServiceConfiguration configuration ) {
      super( LifecycleEvent.Type.ENABLE, configuration );
    }
  }
  
  public static class Disable extends AbstractLifecycleEvent {
    public Disable( ServiceConfiguration configuration ) {
      super( LifecycleEvent.Type.DISABLE, configuration );
    }
  }
  
  public static class Start extends AbstractLifecycleEvent {
    public Start( ServiceConfiguration configuration ) {
      super( LifecycleEvent.Type.START, configuration );
    }
  }
  
  public static class Stop extends AbstractLifecycleEvent {
    public Stop( ServiceConfiguration configuration ) {
      super( LifecycleEvent.Type.STOP, configuration );
    }
  }
  
  public static class AbstractServiceEvent extends GenericEvent<ServiceStatusType> implements LifecycleEvent.Check {
    private final LifecycleEvent.Type      lifecycleEventType;
    private final ServiceConfiguration     serviceConfiguration;
    private final List<ServiceCheckRecord> details;
    private final Date                     timestamp;
    private final String                   uuid;
    
    public AbstractServiceEvent( Type lifecycleEventType, ServiceConfiguration serviceConfiguration, ServiceCheckRecord... details ) {
      this( lifecycleEventType, serviceConfiguration, Arrays.asList( details ) );
    }
    
    public AbstractServiceEvent( Type lifecycleEventType, ServiceConfiguration serviceConfiguration, List<ServiceCheckRecord> details ) {
      super( );
      this.lifecycleEventType = lifecycleEventType;
      this.uuid = UUID.randomUUID( ).toString( );
      this.timestamp = new Date( );
      this.serviceConfiguration = serviceConfiguration;
      this.details = Lists.newArrayList( details );
    }
    
    public ServiceConfiguration getServiceConfiguration( ) {
      return this.serviceConfiguration;
    }
    
    @Override
    public List<ServiceCheckRecord> getDetails( ) {
      return this.details;
    }
    
    @Override
    public String getUuid( ) {
      return this.uuid;
    }
    
    @Override
    public Date getTimestamp( ) {
      return this.timestamp;
    }
    
    @Override
    public ServiceConfiguration getReference( ) {
      return this.serviceConfiguration;
    }
    
    @Override
    public Type getLifecycleEventType( ) {
      return this.lifecycleEventType;
    }
    
    @Override
    public String toString( ) {
      StringBuilder builder = new StringBuilder( );
      if ( this.details.isEmpty( ) ) {
        builder.append( "AbstractServiceEvent " )
               .append( this.serviceConfiguration.getFullName( ) )
               .append( " type=" ).append( this.lifecycleEventType )
               .append( " uuid=" ).append( this.uuid )
               .append( " ts=" ).append( this.timestamp );
      } else {
        for ( ServiceCheckRecord r : this.details ) {
          builder.append( "AbstractServiceEvent " )
                 .append( this.serviceConfiguration.getFullName( ) )
                 .append( " type=" ).append( this.lifecycleEventType )
                 .append( " uuid=" ).append( this.uuid )
                 .append( " ts=" ).append( this.timestamp )
                 .append( " event=" ).append( r ).append( "\n" );
        }
      }
      return builder.toString( );
    }
    
  }
  
  public static class ServiceErrorEvent extends AbstractServiceEvent {
    ServiceErrorEvent( ServiceConfiguration serviceConfiguration, ServiceCheckRecord event ) {
      super( LifecycleEvent.Type.ERROR, serviceConfiguration, event );
    }
    
  }
  
  public static class ServiceStateEvent extends AbstractServiceEvent implements LifecycleEvent {
    private final int             serviceEpoch;
    private final Component.State serviceState;
    
    ServiceStateEvent( ServiceConfiguration config, CheckException... exs ) {
      this( Topology.epoch( ), config.lookupStateMachine( ).getState( ).toString( ), config, exs );
    }
    
    ServiceStateEvent( int serviceEpoch, String serviceState, ServiceConfiguration config, CheckException... exs ) {
      super( LifecycleEvent.Type.STATE, config, Lists.transform( Arrays.asList( exs ), ServiceChecks.Functions.checkExToRecord( ) ) );
      this.serviceEpoch = serviceEpoch;
      Component.State tempState = Component.State.NOTREADY;
      try {
        tempState = Component.State.valueOf( serviceState );
      } catch ( Exception ex ) {
        LOG.error( ex, ex );
      }
      this.serviceState = tempState;
    }
    
    public int getServiceEpoch( ) {
      return this.serviceEpoch;
    }
    
    public Component.State getServiceState( ) {
      return this.serviceState;
    }
    
  }
  
  public static LifecycleEvent error( ServiceConfiguration config, Throwable t ) {
    return error( null, config, t );
  }
  
  public static LifecycleEvent error( String correlationId, ServiceConfiguration config, Throwable t ) {
    return new ServiceErrorEvent( config, ServiceChecks.newEventRecord( correlationId, config, t ) );
  }
  
  public static ServiceStateEvent info( ServiceConfiguration config, CheckException... exs ) {
    return new ServiceStateEvent( config, exs );
  }
  
  public static ServiceStateEvent info( String correlationId, int serviceEpoch, String serviceState, ServiceConfiguration config, CheckException... e ) {
    return new ServiceStateEvent( serviceEpoch, serviceState, config, e );
  }
  
  public static LifecycleEvent disable( ServiceConfiguration config ) {
    return new Disable( config );
  }
  
  public static LifecycleEvent enable( ServiceConfiguration config ) {
    return new Enable( config );
  }
  
  public static LifecycleEvent start( ServiceConfiguration config ) {
    return new Start( config );
  }
  
  public static LifecycleEvent stop( ServiceConfiguration config ) {
    return new Stop( config );
  }
  
}
