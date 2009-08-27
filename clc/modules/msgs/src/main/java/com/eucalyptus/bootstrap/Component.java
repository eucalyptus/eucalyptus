/*******************************************************************************
*Copyright (c) 2009  Eucalyptus Systems, Inc.
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
*    SOFTWARE, AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
*    IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA, SANTA
*    BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY, WHICH IN
*    THE REGENTS’ DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
*    OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
*    WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
*    ANY SUCH LICENSES OR RIGHTS.
 ******************************************************************************/
/*
 * Author: chris grzegorczyk <grze@eucalyptus.com>
 */
package com.eucalyptus.bootstrap;

import java.net.URI;
import java.util.List;

import org.apache.log4j.Logger;

import com.eucalyptus.util.NetworkUtil;

public enum Component {
  bootstrap("vm://EucalyptusRequestQueue"),
  eucalyptus("vm://EucalyptusRequestQueue"),
  walrus("vm://BukkitInternal"),
  dns("vm://DNSControlInternal"),
  storage("vm://StorageInternal"),
  db("127.0.0.1"),
  clusters("vm://ClusterSink"),
  jetty("vm://HttpServer"),
  any(true);
  private static Logger LOG = Logger.getLogger( Component.class );
  private boolean local   = false;

  private boolean enabled = false;
  private boolean hasKeys = false;
  private String hostAddress;
  private int port = 8773;
  private String localUri;
  private URI uri;
  private String propertyKey;
  private ResourceProvider   resourceProvider;
  private List<Bootstrapper> bootstrappers;
  
  private Component() {
    this.propertyKey = "euca."+this.name()+".host";
  }
  private Component(String uri) {
    this();
    this.hostAddress = "127.0.0.1";
    this.localUri = uri;
    this.setUri(uri);
  }
  private Component( boolean whatever ) {
    this();
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
      this.setUri( makeUri(address) );
    }
  }
  
  public String makeUri( String address ) {
    if( Component.db.equals( this ) ) {
      return address;
    } else {
      return "http://" + address + ":" + this.getPort( ) + "/internal/" + this.localUri.replaceAll( "vm://", "" );
    }
  }
  
  public String getPropertyKey( ) {
    return propertyKey;
  }
  public URI getUri( ) {
    return uri;
  }
  private void setUri(String uri) {
    try {
      this.uri = new URI(uri);
      System.setProperty( this.propertyKey, this.uri.toASCIIString( ) );
      if( LOG != null ) LOG.info( String.format( "-> Setting address of component %s to %s=%s", this.name( ), this.propertyKey, this.uri.toASCIIString( ) ) );
    } catch ( Exception e ) {
      System.setProperty( this.propertyKey, this.localUri );
      if( LOG != null ) LOG.info( String.format( "-> Setting address of component %s to %s=%s", this.name( ), this.propertyKey, this.localUri ) );
    }
  }
  public int getPort( ) {
    return port;
  }
  public void setPort( int port ) {
    this.port = port;
  }
  
  

}
