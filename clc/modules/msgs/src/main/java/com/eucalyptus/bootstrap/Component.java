/*******************************************************************************
 *Copyright (c) 2009 Eucalyptus Systems, Inc.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, only version 3 of the License.
 * 
 * 
 * This file is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 * 
 * You should have received a copy of the GNU General Public License along
 * with this program. If not, see <http://www.gnu.org/licenses/>.
 * 
 * Please contact Eucalyptus Systems, Inc., 130 Castilian
 * Dr., Goleta, CA 93101 USA or visit <http://www.eucalyptus.com/licenses/>
 * if you need additional information or have any questions.
 * 
 * This file may incorporate work covered under the following copyright and
 * permission notice:
 * 
 * Software License Agreement (BSD License)
 * 
 * Copyright (c) 2008, Regents of the University of California
 * All rights reserved.
 * 
 * Redistribution and use of this software in source and binary forms, with
 * or without modification, are permitted provided that the following
 * conditions are met:
 * 
 * Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 * 
 * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
 * OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. USERS OF
 * THIS SOFTWARE ACKNOWLEDGE THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE
 * LICENSED MATERIAL, COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS
 * SOFTWARE, AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 * IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA, SANTA
 * BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY, WHICH IN
 * THE REGENTS’ DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
 * OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
 * WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
 * ANY SUCH LICENSES OR RIGHTS.
 *******************************************************************************/
/*
 * Author: chris grzegorczyk <grze@eucalyptus.com>
 */
package com.eucalyptus.bootstrap;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import org.apache.log4j.Logger;
import com.eucalyptus.component.Components;

@Deprecated
public enum Component {
  bootstrap( ),
  eucalyptus( ),
  walrus( ),
  dns( ),
  storage( ),
  db( ),
  jetty( ),
  configuration( ),
  cluster( true ),
  accounts( true ),
  servicebus( true ),
  remoteComponents( true ),
  systemAuthentication( true ),
  any( true );
  private static Logger LOG = Logger.getLogger( Component.class );
  private Boolean       dummy;
  
  private Component( ) {
    this.dummy = false;
  }
  
  private Component( Boolean whatever ) {
    this( );
    this.dummy = whatever;
  }
  
  public void markHasKeys( ) {}
  
  public Boolean isHasKeys( ) {
    return Components.lookup( this ).getKeys( ).getKeys( ) != null;
  }
  
  public void markEnabled( ) {
    Components.lookup( this ).getLifecycle( ).setEnabled( true );
  }
  
  public void markDisabled( ) {
    Components.lookup( this ).getLifecycle( ).setEnabled( false );
  }
  
  public Boolean isEnabled( ) {
    return Components.lookup( this ).getLifecycle( ).isEnabled( );
  }
  
  public Boolean isLocal( ) {
    return Components.lookup( this ).getLifecycle( ).isLocal( );
  }
  
  public void markLocal( ) {
    Components.lookup( this ).getLifecycle( ).markLocal( );
  }
  
  public String getHostAddress( ) {
    return Components.lookup( this ).getLifecycle( ).getHost( );
  }
  
  public void setHostAddress( String address ) {
    Components.lookup( this ).getLifecycle( ).setHost( address );
  }
  
  public URI getUri( ) {
    return Components.lookup( this ).getLifecycle( ).getUri( );
  }
  
  public int getPort( ) {
    return Components.lookup( this ).getLifecycle( ).getPort( );
  }
  
  public void setPort( int port ) {
    Components.lookup( this ).getLifecycle( ).setPort( port );
  }
  
  public String getRegistryKey( String hostName ) {
    return Components.lookup( this ).getChildKey( hostName );
  }
  
  public String getLocalAddress( ) {
    return this.getLocalUri( ).toASCIIString( );
  }
  
  public URI getLocalUri( ) {
    return Components.lookup( this ).getConfiguration( ).getLocalUri( );
  }
  
  public Boolean isInitialized( ) {
    return Components.lookup( this ).getLifecycle( ).isInitialized( );
  }
  
  public Boolean isDummy( ) {
    return this.dummy;
  }
  
  public static List<Component> list() {
    return Arrays.asList( Component.values( ) );
  }

}
