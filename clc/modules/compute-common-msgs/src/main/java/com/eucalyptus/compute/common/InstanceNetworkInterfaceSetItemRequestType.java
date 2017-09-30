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
import com.eucalyptus.util.StreamUtil;
import edu.ucsb.eucalyptus.msgs.EucalyptusData;

public class InstanceNetworkInterfaceSetItemRequestType extends EucalyptusData {

  private String networkInterfaceId;
  private Integer deviceIndex;
  private String subnetId;
  private String description;
  private String privateIpAddress;
  @HttpEmbedded
  private SecurityGroupIdSetType groupSet;
  private Boolean deleteOnTermination;
  @HttpEmbedded
  private PrivateIpAddressesSetRequestType privateIpAddressesSet;
  private Integer secondaryPrivateIpAddressCount;
  private Boolean associatePublicIpAddress;

  public InstanceNetworkInterfaceSetItemRequestType( ) {
  }

  public InstanceNetworkInterfaceSetItemRequestType( final Integer deviceIndex ) {
    this.deviceIndex = deviceIndex;
  }

  public void securityGroups( Iterable<String> groupIds ) {
    groupSet = new SecurityGroupIdSetType( StreamUtil.ofAll( groupIds ).map( SecurityGroupIdSetItemType.forGroupId( ) ).toJavaList( ) );
  }

  public String getNetworkInterfaceId( ) {
    return networkInterfaceId;
  }

  public void setNetworkInterfaceId( String networkInterfaceId ) {
    this.networkInterfaceId = networkInterfaceId;
  }

  public Integer getDeviceIndex( ) {
    return deviceIndex;
  }

  public void setDeviceIndex( Integer deviceIndex ) {
    this.deviceIndex = deviceIndex;
  }

  public String getSubnetId( ) {
    return subnetId;
  }

  public void setSubnetId( String subnetId ) {
    this.subnetId = subnetId;
  }

  public String getDescription( ) {
    return description;
  }

  public void setDescription( String description ) {
    this.description = description;
  }

  public String getPrivateIpAddress( ) {
    return privateIpAddress;
  }

  public void setPrivateIpAddress( String privateIpAddress ) {
    this.privateIpAddress = privateIpAddress;
  }

  public SecurityGroupIdSetType getGroupSet( ) {
    return groupSet;
  }

  public void setGroupSet( SecurityGroupIdSetType groupSet ) {
    this.groupSet = groupSet;
  }

  public Boolean getDeleteOnTermination( ) {
    return deleteOnTermination;
  }

  public void setDeleteOnTermination( Boolean deleteOnTermination ) {
    this.deleteOnTermination = deleteOnTermination;
  }

  public PrivateIpAddressesSetRequestType getPrivateIpAddressesSet( ) {
    return privateIpAddressesSet;
  }

  public void setPrivateIpAddressesSet( PrivateIpAddressesSetRequestType privateIpAddressesSet ) {
    this.privateIpAddressesSet = privateIpAddressesSet;
  }

  public Integer getSecondaryPrivateIpAddressCount( ) {
    return secondaryPrivateIpAddressCount;
  }

  public void setSecondaryPrivateIpAddressCount( Integer secondaryPrivateIpAddressCount ) {
    this.secondaryPrivateIpAddressCount = secondaryPrivateIpAddressCount;
  }

  public Boolean getAssociatePublicIpAddress( ) {
    return associatePublicIpAddress;
  }

  public void setAssociatePublicIpAddress( Boolean associatePublicIpAddress ) {
    this.associatePublicIpAddress = associatePublicIpAddress;
  }
}
