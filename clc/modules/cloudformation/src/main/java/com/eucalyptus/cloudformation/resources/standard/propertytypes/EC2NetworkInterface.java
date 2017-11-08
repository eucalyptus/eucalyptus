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

import java.util.ArrayList;
import java.util.Objects;
import com.eucalyptus.cloudformation.resources.annotations.Property;
import com.eucalyptus.cloudformation.resources.annotations.Required;
import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;

public class EC2NetworkInterface {

  @Property
  private Boolean associatePublicIpAddress;

  @Property
  private Boolean deleteOnTermination;

  @Property
  private String description;

  @Required
  @Property
  private Integer deviceIndex;

  @Property
  private ArrayList<String> groupSet = Lists.newArrayList( );

  @Property
  private String networkInterfaceId;

  @Property
  private String privateIpAddress;

  @Property
  private ArrayList<EC2NetworkInterfacePrivateIPSpecification> privateIpAddresses = Lists.newArrayList( );

  @Property
  private Integer secondaryPrivateIpAddressCount;

  @Property
  private String subnetId;

  public Boolean getAssociatePublicIpAddress( ) {
    return associatePublicIpAddress;
  }

  public void setAssociatePublicIpAddress( Boolean associatePublicIpAddress ) {
    this.associatePublicIpAddress = associatePublicIpAddress;
  }

  public Boolean getDeleteOnTermination( ) {
    return deleteOnTermination;
  }

  public void setDeleteOnTermination( Boolean deleteOnTermination ) {
    this.deleteOnTermination = deleteOnTermination;
  }

  public String getDescription( ) {
    return description;
  }

  public void setDescription( String description ) {
    this.description = description;
  }

  public Integer getDeviceIndex( ) {
    return deviceIndex;
  }

  public void setDeviceIndex( Integer deviceIndex ) {
    this.deviceIndex = deviceIndex;
  }

  public ArrayList<String> getGroupSet( ) {
    return groupSet;
  }

  public void setGroupSet( ArrayList<String> groupSet ) {
    this.groupSet = groupSet;
  }

  public String getNetworkInterfaceId( ) {
    return networkInterfaceId;
  }

  public void setNetworkInterfaceId( String networkInterfaceId ) {
    this.networkInterfaceId = networkInterfaceId;
  }

  public String getPrivateIpAddress( ) {
    return privateIpAddress;
  }

  public void setPrivateIpAddress( String privateIpAddress ) {
    this.privateIpAddress = privateIpAddress;
  }

  public ArrayList<EC2NetworkInterfacePrivateIPSpecification> getPrivateIpAddresses( ) {
    return privateIpAddresses;
  }

  public void setPrivateIpAddresses( ArrayList<EC2NetworkInterfacePrivateIPSpecification> privateIpAddresses ) {
    this.privateIpAddresses = privateIpAddresses;
  }

  public Integer getSecondaryPrivateIpAddressCount( ) {
    return secondaryPrivateIpAddressCount;
  }

  public void setSecondaryPrivateIpAddressCount( Integer secondaryPrivateIpAddressCount ) {
    this.secondaryPrivateIpAddressCount = secondaryPrivateIpAddressCount;
  }

  public String getSubnetId( ) {
    return subnetId;
  }

  public void setSubnetId( String subnetId ) {
    this.subnetId = subnetId;
  }

  @Override
  public boolean equals( final Object o ) {
    if ( this == o ) return true;
    if ( o == null || getClass( ) != o.getClass( ) ) return false;
    final EC2NetworkInterface that = (EC2NetworkInterface) o;
    return Objects.equals( getAssociatePublicIpAddress( ), that.getAssociatePublicIpAddress( ) ) &&
        Objects.equals( getDeleteOnTermination( ), that.getDeleteOnTermination( ) ) &&
        Objects.equals( getDescription( ), that.getDescription( ) ) &&
        Objects.equals( getDeviceIndex( ), that.getDeviceIndex( ) ) &&
        Objects.equals( getGroupSet( ), that.getGroupSet( ) ) &&
        Objects.equals( getNetworkInterfaceId( ), that.getNetworkInterfaceId( ) ) &&
        Objects.equals( getPrivateIpAddress( ), that.getPrivateIpAddress( ) ) &&
        Objects.equals( getPrivateIpAddresses( ), that.getPrivateIpAddresses( ) ) &&
        Objects.equals( getSecondaryPrivateIpAddressCount( ), that.getSecondaryPrivateIpAddressCount( ) ) &&
        Objects.equals( getSubnetId( ), that.getSubnetId( ) );
  }

  @Override
  public int hashCode( ) {
    return Objects.hash( getAssociatePublicIpAddress( ), getDeleteOnTermination( ), getDescription( ), getDeviceIndex( ), getGroupSet( ), getNetworkInterfaceId( ), getPrivateIpAddress( ), getPrivateIpAddresses( ), getSecondaryPrivateIpAddressCount( ), getSubnetId( ) );
  }

  @Override
  public String toString( ) {
    return MoreObjects.toStringHelper( this )
        .add( "associatePublicIpAddress", associatePublicIpAddress )
        .add( "deleteOnTermination", deleteOnTermination )
        .add( "description", description )
        .add( "deviceIndex", deviceIndex )
        .add( "groupSet", groupSet )
        .add( "networkInterfaceId", networkInterfaceId )
        .add( "privateIpAddress", privateIpAddress )
        .add( "privateIpAddresses", privateIpAddresses )
        .add( "secondaryPrivateIpAddressCount", secondaryPrivateIpAddressCount )
        .add( "subnetId", subnetId )
        .toString( );
  }
}
