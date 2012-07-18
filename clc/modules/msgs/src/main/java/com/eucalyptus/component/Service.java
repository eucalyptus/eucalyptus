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

import java.net.InetSocketAddress;
import java.security.KeyPair;
import java.security.cert.X509Certificate;
import java.util.Collection;
import com.eucalyptus.component.Component.State;
import com.eucalyptus.component.Component.Transition;
import com.eucalyptus.event.Event;
import com.eucalyptus.event.EventListener;
import com.eucalyptus.util.HasFullName;
import com.eucalyptus.util.async.Request;
import com.eucalyptus.util.fsm.StateMachine;

public interface Service extends EventListener<Event>, HasFullName<ServiceConfiguration> {
  
  public abstract Dispatcher getDispatcher( );
  
  public abstract String toString( );
  
  public abstract void enqueue( Request request );
  
  public abstract Boolean isLocal( );
  
  public abstract KeyPair getKeys( );
  
  public abstract X509Certificate getCertificate( );
  
  public abstract ServiceConfiguration getServiceConfiguration( );
  
  public abstract Component getComponent( );
  
  public abstract ComponentId getComponentId( );
  
  public abstract boolean checkTransition( Transition transition );
  
  public abstract State getGoal( );
  
  InetSocketAddress getSocketAddress( );
  
  public abstract void setGoal( State state );
  
  public abstract StateMachine<ServiceConfiguration, State, Transition> getStateMachine( );

  public abstract void start( );

  public abstract void stop( );
  
}
