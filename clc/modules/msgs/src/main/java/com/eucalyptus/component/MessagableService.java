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
package com.eucalyptus.component;

import java.net.InetSocketAddress;
import java.net.URI;
import java.security.KeyPair;
import java.security.cert.X509Certificate;
import java.util.Collection;
import com.eucalyptus.component.Component.State;
import com.eucalyptus.component.Component.Transition;
import com.eucalyptus.event.Event;
import com.eucalyptus.util.FullName;
import com.eucalyptus.util.async.Request;
import com.eucalyptus.util.fsm.StateMachine;
import com.eucalyptus.ws.client.ServiceDispatcher;

public class MessagableService extends AbstractService implements Service {
  public static String          LOCAL_HOSTNAME = "@localhost";
  private final ServiceEndpoint endpoint;
  private final Dispatcher      localDispatcher;
  private final Dispatcher      remoteDispatcher;
  private final Service         serviceDelegate;
  
  /** ASAP:FIXME:GRZE **/
  
  MessagableService( final Service baseService ) {
    this.serviceDelegate = baseService;
    URI remoteUri;
    if ( this.getServiceConfiguration( ).isVmLocal( ) ) {
      remoteUri = this.getComponentId( ).makeInternalRemoteUri( "127.0.0.1", this.getComponentId( ).getPort( ) );
    } else {
      remoteUri = this.getComponentId( ).makeInternalRemoteUri( this.getServiceConfiguration( ).getHostName( ), this.getServiceConfiguration( ).getPort( ) );
    }
    this.endpoint = new ServiceEndpoint( this, true, baseService.getServiceConfiguration( ).isVmLocal( )
      ? this.getComponentId( ).getLocalEndpointUri( )
      : remoteUri );//TODO:GRZE: fix remote/local swaps
    this.localDispatcher = ServiceDispatcher.makeLocal( this.getServiceConfiguration( ) );
    this.remoteDispatcher = ServiceDispatcher.makeRemote( this.getServiceConfiguration( ) );
  }
  
  MessagableService( final ServiceConfiguration config ) throws ServiceRegistrationException {
    this( new BasicService( config ) );
  }
  
  @Override
  public Dispatcher getDispatcher( ) {
    return this.isLocal( )
      ? this.localDispatcher
      : this.remoteDispatcher;
  }
  
  @Override
  public String toString( ) {
    return String.format( "Service %s name=%s endpoint=%s serviceConfiguration=%s\n",
                          this.getComponentId( ), this.getName( ), this.endpoint, this.getServiceConfiguration( ) );
  }
  
  @Override
  public void enqueue( final Request request ) {
    this.endpoint.enqueue( request );
  }
  
  @Override
  public int hashCode( ) {
    return this.serviceDelegate.hashCode( );
  }
  
  @Override
  public boolean equals( final Object obj ) {
    return this.serviceDelegate.equals( obj );
  }
  
  @Override
  public Boolean isLocal( ) {
    return this.serviceDelegate.isLocal( );
  }
  
  @Override
  public KeyPair getKeys( ) {
    return this.serviceDelegate.getKeys( );
  }
  
  @Override
  public X509Certificate getCertificate( ) {
    return this.serviceDelegate.getCertificate( );
  }
  
  @Override
  public ServiceConfiguration getServiceConfiguration( ) {
    return this.serviceDelegate.getServiceConfiguration( );
  }
  
  @Override
  public Component getComponent( ) {
    return this.serviceDelegate.getComponent( );
  }
  
  @Override
  public ComponentId getComponentId( ) {
    return this.serviceDelegate.getComponentId( );
  }
  
  @Override
  public boolean checkTransition( final Transition transition ) {
    return this.serviceDelegate.checkTransition( transition );
  }
  
  @Override
  public State getGoal( ) {
    return this.serviceDelegate.getGoal( );
  }
  
  @Override
  public void setGoal( final State state ) {
    this.serviceDelegate.setGoal( state );
  }
  
  @Override
  public InetSocketAddress getSocketAddress( ) {
    return this.serviceDelegate.getSocketAddress( );
  }
  
  @Override
  public void fireEvent( final Event event ) {
    this.serviceDelegate.fireEvent( event );
  }
  
  @Override
  public String getName( ) {
    return this.serviceDelegate.getName( );
  }
  
  @Override
  public String getPartition( ) {
    return this.serviceDelegate.getPartition( );
  }
  
  @Override
  public FullName getFullName( ) {
    return this.serviceDelegate.getFullName( );
  }
  
  @Override
  public int compareTo( final ServiceConfiguration o ) {
    return this.serviceDelegate.compareTo( o );
  }
  
  public Collection<ServiceCheckRecord> getDetails( ) {
    return this.serviceDelegate.getDetails( );
  }
  
  public StateMachine<ServiceConfiguration, State, Transition> getStateMachine( ) {
    return this.serviceDelegate.getStateMachine( );
  }
  
  @Override
  public void cleanUp( ) {
    this.serviceDelegate.cleanUp( );
  }

  @Override
  public void start( ) {
    this.endpoint.start( );
  }

  @Override
  public void stop( ) {
    this.endpoint.stop( );
  }
  
}
