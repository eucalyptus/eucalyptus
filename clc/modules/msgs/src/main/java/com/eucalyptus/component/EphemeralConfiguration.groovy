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

package com.eucalyptus.component

import java.net.URI
import javax.persistence.Transient
import com.eucalyptus.config.ComponentConfiguration

class EphemeralConfiguration extends ComponentConfiguration {
  private static final long serialVersionUID = 1;
  URI         uri;
  ComponentId c;
  
  public EphemeralConfiguration( ComponentId c, String partition, String name, URI uri ) {
    super( partition, name, uri.getHost( ), uri.getPort( ), uri.getPath( ) );
    this.uri = uri;
    this.c = c;
  }
  
  public ComponentId lookupComponentId( ) {
    return c;
  }
  
  public URI getUri( ) {
    return this.uri;
  }
  
  @Override
  public String getName( ) {
    return super.getName( );
  }
  
  @Override
  public Boolean isVmLocal( ) {
    return super.isVmLocal( );
  }

  @Override
  public Boolean isHostLocal( ) {
    return super.isHostLocal( );
  }

  @Override
  public int compareTo( ServiceConfiguration that ) {
    return super.compareTo( that );
  }
  
  @Override
  public String toString( ) {
    return super.toString( );
  }
  
  @Override
  public int hashCode( ) {
    return super.hashCode( );
  }
  
  @Override
  public boolean equals( Object that ) {
    return super.equals( that );
  }
  
  @Override
  public String getPartition( ) {
    return super.getPartition( );
  }
  
  @Override
  public void setPartition( String partition ) {
    super.setPartition( partition );
  }
  
  @Override
  public String getHostName( ) {
    return super.getHostName( );
  }
  
  @Override
  public void setHostName( String hostName ) {
    super.setHostName( hostName );
  }
  
  @Override
  public Integer getPort( ) {
    return super.getPort( );
  }
  
  @Override
  public void setPort( Integer port ) {
    super.setPort( port );
  }
  
  @Override
  public String getServicePath( ) {
    return super.getServicePath( );
  }
  
  @Override
  public void setServicePath( String servicePath ) {
    super.setServicePath( servicePath );
  }
  
  @Override
  public void setName( String name ) {
    super.setName( name );
  }

  public String getSourceHostName( ) {
    return null;
  }

  public void setSourceHostName( String aliasHostName ) {}
}
