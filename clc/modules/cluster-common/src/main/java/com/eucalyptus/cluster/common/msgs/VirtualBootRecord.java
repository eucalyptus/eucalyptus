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
package com.eucalyptus.cluster.common.msgs;

import com.google.common.base.MoreObjects;
import edu.ucsb.eucalyptus.msgs.EucalyptusData;

public class VirtualBootRecord extends EucalyptusData implements Cloneable {

  private String id = "none";
  private String resourceLocation = "none";
  private String type;
  private String guestDeviceName = "none";
  private Long size = -1l;
  private String format = "none";

  public VirtualBootRecord( ) {
  }

  public VirtualBootRecord( String id, String resourceLocation, String type, String guestDeviceName, Long sizeBytes, String format ) {
    this.id = id;
    this.resourceLocation = resourceLocation;
    this.type = type;
    this.guestDeviceName = guestDeviceName;
    this.size = sizeBytes;
    this.format = format;
  }

  public boolean hasValidId( ) {
    return !"none".equals( this.id );
  }

  public VirtualBootRecord clone( ) {
    return (VirtualBootRecord) super.clone( );
  }

  public String getId( ) {
    return id;
  }

  public void setId( String id ) {
    this.id = id;
  }

  public String getResourceLocation( ) {
    return resourceLocation;
  }

  public void setResourceLocation( String resourceLocation ) {
    this.resourceLocation = resourceLocation;
  }

  public String getType( ) {
    return type;
  }

  public void setType( String type ) {
    this.type = type;
  }

  public String getGuestDeviceName( ) {
    return guestDeviceName;
  }

  public void setGuestDeviceName( String guestDeviceName ) {
    this.guestDeviceName = guestDeviceName;
  }

  public Long getSize( ) {
    return size;
  }

  public void setSize( Long size ) {
    this.size = size;
  }

  public String getFormat( ) {
    return format;
  }

  public void setFormat( String format ) {
    this.format = format;
  }

  @Override
  public String toString( ) {
    return MoreObjects.toStringHelper( this )
        .add( "id", id )
        .add( "resourceLocation", resourceLocation )
        .add( "type", type )
        .add( "guestDeviceName", guestDeviceName )
        .add( "size", size )
        .add( "format", format )
        .toString( );
  }
}
