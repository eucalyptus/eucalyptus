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

import java.io.Serializable;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.Collection;
import com.eucalyptus.bootstrap.CanBootstrap;
import com.eucalyptus.component.Component.State;
import com.eucalyptus.util.HasFullName;
import com.eucalyptus.util.fsm.HasStateMachine;
import com.eucalyptus.util.fsm.StateMachine;

public interface ServiceConfiguration extends Serializable, HasFullName<ServiceConfiguration>, HasStateMachine<ServiceConfiguration, Component.State, Component.Transition> {
  
  public abstract void setName( String name );
  
  public abstract void setPartition( String partition );
  
  public InetSocketAddress getSocketAddress( );
  
  public abstract String getHostName( );

  public abstract void setHostName( String hostName );

  public abstract Integer getPort( );
  
  public abstract void setPort( Integer port );
  
  public abstract String getServicePath( );
  
  public abstract void setServicePath( String servicePath );
  
  public abstract URI getUri( );

  public abstract Boolean isVmLocal( );
  
  public abstract Partition lookupPartition( );
  
  public abstract CanBootstrap lookupBootstrapper( );

  public abstract StateMachine<ServiceConfiguration, Component.State, Component.Transition> lookupStateMachine( );
  
  public abstract State lookupState( );

  public abstract ComponentId getComponentId( );
  
  public abstract Boolean isHostLocal( );

  public abstract InetAddress getInetAddress( );

}
