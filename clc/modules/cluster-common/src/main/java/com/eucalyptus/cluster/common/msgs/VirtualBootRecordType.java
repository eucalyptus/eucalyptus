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

import edu.ucsb.eucalyptus.msgs.EucalyptusData;

public class VirtualBootRecordType extends EucalyptusData {

  private String resourceLocation;
  private String guestDeviceName;
  private Long size;
  private String format;
  private String id;
  private String type;

  public String getResourceLocation( ) {
    return resourceLocation;
  }

  public void setResourceLocation( String resourceLocation ) {
    this.resourceLocation = resourceLocation;
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

  public String getId( ) {
    return id;
  }

  public void setId( String id ) {
    this.id = id;
  }

  public String getType( ) {
    return type;
  }

  public void setType( String type ) {
    this.type = type;
  }
}
