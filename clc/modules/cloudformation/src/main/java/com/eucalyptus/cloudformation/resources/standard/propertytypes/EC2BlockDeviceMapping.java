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
package com.eucalyptus.cloudformation.resources.standard.propertytypes;

import java.util.Objects;
import com.eucalyptus.cloudformation.resources.annotations.Property;
import com.eucalyptus.cloudformation.resources.annotations.Required;
import com.google.common.base.MoreObjects;

public class EC2BlockDeviceMapping {

  @Property
  @Required
  private String deviceName;

  @Property
  private EC2EBSBlockDevice ebs;

  @Property

  private Object noDevice;

  @Property
  private String virtualName;

  public String getDeviceName( ) {
    return deviceName;
  }

  public void setDeviceName( String deviceName ) {
    this.deviceName = deviceName;
  }

  public EC2EBSBlockDevice getEbs( ) {
    return ebs;
  }

  public void setEbs( EC2EBSBlockDevice ebs ) {
    this.ebs = ebs;
  }

  public Object getNoDevice( ) {
    return noDevice;
  }

  public void setNoDevice( Object noDevice ) {
    this.noDevice = noDevice;
  }

  public String getVirtualName( ) {
    return virtualName;
  }

  public void setVirtualName( String virtualName ) {
    this.virtualName = virtualName;
  }

  @Override
  public boolean equals( final Object o ) {
    if ( this == o ) return true;
    if ( o == null || getClass( ) != o.getClass( ) ) return false;
    final EC2BlockDeviceMapping that = (EC2BlockDeviceMapping) o;
    return Objects.equals( getDeviceName( ), that.getDeviceName( ) ) &&
        Objects.equals( getEbs( ), that.getEbs( ) ) &&
        Objects.equals( getNoDevice( ), that.getNoDevice( ) ) &&
        Objects.equals( getVirtualName( ), that.getVirtualName( ) );
  }

  @Override
  public int hashCode( ) {
    return Objects.hash( getDeviceName( ), getEbs( ), getNoDevice( ), getVirtualName( ) );
  }

  @Override
  public String toString( ) {
    return MoreObjects.toStringHelper( this )
        .add( "deviceName", deviceName )
        .add( "ebs", ebs )
        .add( "noDevice", noDevice )
        .add( "virtualName", virtualName )
        .toString( );
  }
}
