/*************************************************************************
 * Copyright 2009-2014 Ent. Services Development Corporation LP
 *
 * Redistribution and use of this software in source and binary forms,
 * with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *   Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 *   Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer
 *   in the documentation and/or other materials provided with the
 *   distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
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
