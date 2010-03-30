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
import java.net.URISyntaxException;
import java.util.List;

import org.apache.log4j.Logger;

import com.eucalyptus.util.NetworkUtil;

public enum Component {
  eucalyptus( "vm://EucalyptusRequestQueue" ),
  bootstrap( "vm://EucalyptusRequestQueue" ),
  walrus( "vm://BukkitInternal" ),
  dns( "vm://DNSControlInternal" ),
  storage( "vm://StorageInternal" ),
  db( "jdbc:hsqldb:hsqls://127.0.0.1:9001/eucalyptus" ),
  cluster( "vm://ClusterSink" ),
  jetty( "vm://HttpServer" ),
  any( true );
  private static Logger      LOG         = Logger.getLogger( Component.class );
  private boolean            local       = false;

  private boolean            enabled     = false;
  private boolean            initialized = true;//FIXME: set this in some useful way
  private boolean            hasKeys     = false;
  private String             hostAddress;
  private int                port        = 8773;
  private String             localUri;
  private URI                uri;
  private String             propertyKey;
  private ResourceProvider   resourceProvider;
  private List<Bootstrapper> bootstrappers;

  private Component( ) {
    this.hostAddress = "localhost";
    this.propertyKey = "euca." + this.name( ) + ".host";
  }

  private Component( String uri ) {
    this( );
    this.localUri = uri;
    this.setUri( uri );
  }

  private Component( boolean whatever ) {
    this( );
    this.local = true;
    this.enabled = true;
  }

  public void markHasKeys( ) {
    this.hasKeys = true;
  }

  public boolean isHasKeys( ) {
    return hasKeys;
  }

  public void markEnabled( ) {
    this.enabled = true;
  }

  public void markDisabled( ) {
    this.enabled = false;
  }

  public boolean isEnabled( ) {
    return enabled;
  }

  public boolean isLocal( ) {
    return local;
  }

  public void markLocal( ) {
    this.local = true;
  }

  public ResourceProvider getResourceProvider( ) {
    return resourceProvider;
  }

  public void setResourceProvider( ResourceProvider resourceProvider ) {
    this.resourceProvider = resourceProvider;
  }

  public List<Bootstrapper> getBootstrappers( ) {
    return bootstrappers;
  }

  public boolean add( Bootstrapper arg0 ) {
    return bootstrappers.add( arg0 );
  }

  public String getHostAddress( ) {
    return this.hostAddress;
  }

  public void setHostAddress( String address ) {
    boolean isLocal = false;
    try {
      isLocal = NetworkUtil.testLocal( address );
    } catch ( Exception e1 ) {
    }
    if ( isLocal ) {
      this.local = true;
      this.hostAddress = "localhost";
      this.setUri( this.localUri );
    } else {
      this.local = false;
      this.hostAddress = address;
      this.setUri( makeUri( address ) );
    }
  }

  public String makeUri( String address ) {
    if ( Component.db.equals( this ) ) {
      return String.format( "jdbc:hsqldb:hsqls://%s:%d/eucalyptus", address, 9001 );
    } else {
      return String.format( "http://%s:%d/internal/%s", address, 8773, this.localUri.replaceAll( "vm://", "" ) );
    }
  }

  public String getPropertyKey( ) {
    return propertyKey;
  }

  public URI getUri( ) {
    return uri;
  }

  private void setUri( String uri ) {
    try {
      this.uri = new URI( uri );
      System.setProperty( this.propertyKey, this.uri.toASCIIString( ) );
      if ( LOG != null ) LOG.info( String.format( "-> Setting address of component %s to %s=%s", this.name( ), this.propertyKey, this.uri.toASCIIString( ) ) );
    } catch ( Exception e ) {
      System.setProperty( this.propertyKey, this.localUri );
      if ( LOG != null ) LOG.info( String.format( "-> Setting address of component %s to %s=%s", this.name( ), this.propertyKey, this.localUri ) );
    }
  }

  public int getPort( ) {
    return port;
  }

  public void setPort( int port ) {
    this.port = port;
  }

  public String getRegistryKey( String hostName ) {
    try {
      if ( NetworkUtil.testLocal( hostName ) ) return this.name( ) + "@localhost";
    } catch ( Exception e ) {
    }
    return this.name( ) + "@" + hostName;
  }

  public String getLocalAddress( ) {
    return localUri;
  }

  public URI getLocalUri( ) {
    if ( Component.db.equals( this ) ) { return null; }
    try {
      return new URI( this.localUri );
    } catch ( URISyntaxException e ) {
      LOG.fatal( "Failed to construct the default local URI object.", e );
      System.exit( 1 );
      return null;
    }
  }

  public boolean isInitialized( ) {
    return initialized;
  }

  public void setInitialized( boolean initialized ) {
    this.initialized = initialized;
  }

}
