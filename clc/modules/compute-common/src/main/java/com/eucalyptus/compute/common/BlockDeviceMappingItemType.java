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
package com.eucalyptus.compute.common;

import com.eucalyptus.binding.HttpEmbedded;
import edu.ucsb.eucalyptus.msgs.EucalyptusData;

public class BlockDeviceMappingItemType extends EucalyptusData {

  private String virtualName;
  private String deviceName;
  private Integer size;
  private String format;
  private Boolean noDevice;
  @HttpEmbedded( multiple = true )
  private EbsDeviceMapping ebs;

  public BlockDeviceMappingItemType( final String virtualName, final String deviceName ) {
    this.virtualName = virtualName;
    this.deviceName = deviceName;
  }

  public BlockDeviceMappingItemType( ) {
  }

  @Override
  public int hashCode( ) {
    final int prime = 31;
    int result = 1;
    result = ( (int) ( prime * result + ( ( deviceName == null ) ? 0 : deviceName.hashCode( ) ) ) );
    return result;
  }

  @Override
  public boolean equals( Object obj ) {
    if ( this == obj ) return true;
    if ( obj == null ) return false;
    if ( !getClass( ).equals( obj.getClass( ) ) ) return false;
    BlockDeviceMappingItemType other = (BlockDeviceMappingItemType) obj;
    if ( deviceName == null ) {
      if ( other.deviceName != null ) return false;
    } else if ( !deviceName.equals( other.deviceName ) ) return false;
    return true;
  }

  public String getVirtualName( ) {
    return virtualName;
  }

  public void setVirtualName( String virtualName ) {
    this.virtualName = virtualName;
  }

  public String getDeviceName( ) {
    return deviceName;
  }

  public void setDeviceName( String deviceName ) {
    this.deviceName = deviceName;
  }

  public Integer getSize( ) {
    return size;
  }

  public void setSize( Integer size ) {
    this.size = size;
  }

  public String getFormat( ) {
    return format;
  }

  public void setFormat( String format ) {
    this.format = format;
  }

  public Boolean getNoDevice( ) {
    return noDevice;
  }

  public void setNoDevice( Boolean noDevice ) {
    this.noDevice = noDevice;
  }

  public EbsDeviceMapping getEbs( ) {
    return ebs;
  }

  public void setEbs( EbsDeviceMapping ebs ) {
    this.ebs = ebs;
  }
}
