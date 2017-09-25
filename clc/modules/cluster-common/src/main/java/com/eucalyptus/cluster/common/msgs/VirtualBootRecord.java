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
