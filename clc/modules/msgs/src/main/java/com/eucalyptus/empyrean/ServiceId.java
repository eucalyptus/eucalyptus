/*************************************************************************
 * (c) Copyright 2017 Hewlett Packard Enterprise Development Company LP
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
 ************************************************************************/
package com.eucalyptus.empyrean;

import java.net.URI;
import java.util.ArrayList;
import edu.ucsb.eucalyptus.msgs.EucalyptusData;

public class ServiceId extends EucalyptusData {

  /**
   * UUID of the registration
   **/
  private String uuid;
  /**
   * The resource partition name
   **/
  private String partition;
  /**
   * The registration name
   **/
  private String name;
  /**
   * name of the ComponentId
   **/
  private String type;
  /**
   * full name of the registration
   **/
  private String fullName;
  private ArrayList<String> uris = new ArrayList<String>( );
  private String uri;
  private String host;

  public String getUri( ) {
    return ( uris.isEmpty( ) ? "none" : uris.get( 0 ) );
  }

  public void setUri( String uri ) {
    this.uris.remove( uri );
    this.uris.add( 0, uri );
    this.uri = uri;
    this.host = null;
  }

  public void setServiceUri( URI serviceUri ) {
    String uri = serviceUri.toASCIIString( );
    this.uris.remove( uri );
    this.uris.add( 0, uri );
    this.uri = uri;
    this.host = serviceUri.getHost( );
  }

  public String getUuid( ) {
    return uuid;
  }

  public void setUuid( String uuid ) {
    this.uuid = uuid;
  }

  public String getPartition( ) {
    return partition;
  }

  public void setPartition( String partition ) {
    this.partition = partition;
  }

  public String getName( ) {
    return name;
  }

  public void setName( String name ) {
    this.name = name;
  }

  public String getType( ) {
    return type;
  }

  public void setType( String type ) {
    this.type = type;
  }

  public String getFullName( ) {
    return fullName;
  }

  public void setFullName( String fullName ) {
    this.fullName = fullName;
  }

  public ArrayList<String> getUris( ) {
    return uris;
  }

  public void setUris( ArrayList<String> uris ) {
    this.uris = uris;
  }

  public String getHost( ) {
    return host;
  }

  public void setHost( String host ) {
    this.host = host;
  }
}
