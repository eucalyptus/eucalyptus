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

package com.eucalyptus.component;

import java.net.InetSocketAddress;
import java.security.KeyPair;
import java.security.cert.X509Certificate;
import java.util.Collection;
import org.apache.log4j.Logger;
import com.eucalyptus.component.Component.State;
import com.eucalyptus.component.Component.Transition;
import com.eucalyptus.component.auth.SystemCredentials;
import com.eucalyptus.event.Event;
import com.eucalyptus.util.FullName;
import com.eucalyptus.util.async.Request;
import com.eucalyptus.util.fsm.StateMachine;
import com.eucalyptus.util.fsm.StateMachineBuilder;

public class MissingService extends AbstractService implements Service {
  private static Logger              LOG = Logger.getLogger( MissingService.class );
  private final ServiceConfiguration serviceConfiguration;
  private final StateMachineBuilder<ServiceConfiguration, State, Transition> stateMachine;
  
  MissingService( ServiceConfiguration serviceConfiguration ) {
    this.serviceConfiguration = serviceConfiguration;
    this.stateMachine = new StateMachineBuilder<ServiceConfiguration, State, Transition>( serviceConfiguration, State.BROKEN );

  }
  
  @Override
  public final String getName( ) {
    return this.serviceConfiguration.getFullName( ).toString( );
  }
  
  @Override
  public Boolean isLocal( ) {
    return this.serviceConfiguration.isVmLocal( );
  }
  
  @Override
  public KeyPair getKeys( ) {
    return SystemCredentials.getCredentialProvider( this.serviceConfiguration.getComponentId( ) ).getKeyPair( );
  }
  
  @Override
  public X509Certificate getCertificate( ) {
    return SystemCredentials.getCredentialProvider( this.serviceConfiguration.getComponentId( ) ).getCertificate( );
  }
  
  /**
   * @see java.lang.Comparable#compareTo(java.lang.Object)
   * @param that
   * @return
   */
  @Override
  public int compareTo( ServiceConfiguration that ) {
    return this.serviceConfiguration.compareTo( that );
  }
  
  /**
   * @return the service configuration
   */
  @Override
  public ServiceConfiguration getServiceConfiguration( ) {
    return this.serviceConfiguration;
  }
  
  @Override
  public Component getComponent( ) {
    return this.serviceConfiguration.lookupComponent( );
  }
  
  @Override
  public ComponentId getComponentId( ) {
    return this.serviceConfiguration.getComponentId( );
  }
  
  @Override
  public FullName getFullName( ) {
    return this.serviceConfiguration.getFullName( );
  }
  
  @Override
  public String toString( ) {
    return String.format( "Service %s name=%s serviceConfiguration=%s\n",
                          this.getComponentId( ), this.getName( ), this.getServiceConfiguration( ) );
  }
  
  @Override
  public String getPartition( ) {
    return this.serviceConfiguration.getPartition( );
  }
    
  @Override
  public Dispatcher getDispatcher( ) {
    throw new IllegalStateException( this.serviceConfiguration + " does not support the operation: " + Thread.currentThread( ).getStackTrace( )[1] );
  }
  
  @Override
  public Collection<ServiceCheckRecord> getDetails( ) {
    throw new IllegalStateException( this.serviceConfiguration + " does not support the operation: " + Thread.currentThread( ).getStackTrace( )[1] );
  }
  
  @Override
  public void enqueue( Request request ) {
    LOG.error( "Discarding request submitted to a basic service: " + request );
  }
  
  @Override
  public boolean checkTransition( Transition transition ) {
    return false;
  }
  
  @Override
  public Component.State getGoal( ) {
    return Component.State.PRIMORDIAL;
  }
  
  @Override
  public InetSocketAddress getSocketAddress( ) {
    throw new RuntimeException( this.serviceConfiguration + " does not support the operation: " + Thread.currentThread( ).getStackTrace( )[1] );
  }
  
  @Override
  public void setGoal( State state ) {}
  
  @Override
  public void fireEvent( Event event ) {
    LOG.debug( "MissingService " + this.serviceConfiguration + "ignored the event: " + event );
  }
  
  @Override
  public StateMachine<ServiceConfiguration, State, Transition> getStateMachine( ) {
    return null;
  }

  /**
   * @see com.eucalyptus.component.Service#start()
   */
  @Override
  public void start( ) {}

  /**
   * @see com.eucalyptus.component.Service#stop()
   */
  @Override
  public void stop( ) {}
  
}
