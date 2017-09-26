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

import edu.ucsb.eucalyptus.msgs.EucalyptusData;

public class InstanceNetworkInterfaceSetItemType extends EucalyptusData {

  private String networkInterfaceId;
  private String subnetId;
  private String vpcId;
  private String description;
  private String ownerId;
  private String status;
  private String macAddress;
  private String privateIpAddress;
  private String privateDnsName;
  private Boolean sourceDestCheck;
  private GroupSetType groupSet;
  private InstanceNetworkInterfaceAttachmentType attachment = new InstanceNetworkInterfaceAttachmentType( );
  private InstanceNetworkInterfaceAssociationType association;
  private InstancePrivateIpAddressesSetType privateIpAddressesSet;

  public InstanceNetworkInterfaceSetItemType( ) {
  }

  public InstanceNetworkInterfaceSetItemType( final String networkInterfaceId, final String subnetId, final String vpcId, final String description, final String ownerId, final String status, final String macAddress, final String privateIpAddress, final String privateDnsName, final Boolean sourceDestCheck, final GroupSetType groupSet, final InstanceNetworkInterfaceAttachmentType attachment, final InstanceNetworkInterfaceAssociationType association, final InstancePrivateIpAddressesSetType privateIpAddressesSet ) {
    this.networkInterfaceId = networkInterfaceId;
    this.subnetId = subnetId;
    this.vpcId = vpcId;
    this.description = description;
    this.ownerId = ownerId;
    this.status = status;
    this.macAddress = macAddress;
    this.privateIpAddress = privateIpAddress;
    this.privateDnsName = privateDnsName;
    this.sourceDestCheck = sourceDestCheck;
    this.groupSet = groupSet;
    this.attachment = attachment;
    this.association = association;
    this.privateIpAddressesSet = privateIpAddressesSet;
  }

  public String getNetworkInterfaceId( ) {
    return networkInterfaceId;
  }

  public void setNetworkInterfaceId( String networkInterfaceId ) {
    this.networkInterfaceId = networkInterfaceId;
  }

  public String getSubnetId( ) {
    return subnetId;
  }

  public void setSubnetId( String subnetId ) {
    this.subnetId = subnetId;
  }

  public String getVpcId( ) {
    return vpcId;
  }

  public void setVpcId( String vpcId ) {
    this.vpcId = vpcId;
  }

  public String getDescription( ) {
    return description;
  }

  public void setDescription( String description ) {
    this.description = description;
  }

  public String getOwnerId( ) {
    return ownerId;
  }

  public void setOwnerId( String ownerId ) {
    this.ownerId = ownerId;
  }

  public String getStatus( ) {
    return status;
  }

  public void setStatus( String status ) {
    this.status = status;
  }

  public String getMacAddress( ) {
    return macAddress;
  }

  public void setMacAddress( String macAddress ) {
    this.macAddress = macAddress;
  }

  public String getPrivateIpAddress( ) {
    return privateIpAddress;
  }

  public void setPrivateIpAddress( String privateIpAddress ) {
    this.privateIpAddress = privateIpAddress;
  }

  public String getPrivateDnsName( ) {
    return privateDnsName;
  }

  public void setPrivateDnsName( String privateDnsName ) {
    this.privateDnsName = privateDnsName;
  }

  public Boolean getSourceDestCheck( ) {
    return sourceDestCheck;
  }

  public void setSourceDestCheck( Boolean sourceDestCheck ) {
    this.sourceDestCheck = sourceDestCheck;
  }

  public GroupSetType getGroupSet( ) {
    return groupSet;
  }

  public void setGroupSet( GroupSetType groupSet ) {
    this.groupSet = groupSet;
  }

  public InstanceNetworkInterfaceAttachmentType getAttachment( ) {
    return attachment;
  }

  public void setAttachment( InstanceNetworkInterfaceAttachmentType attachment ) {
    this.attachment = attachment;
  }

  public InstanceNetworkInterfaceAssociationType getAssociation( ) {
    return association;
  }

  public void setAssociation( InstanceNetworkInterfaceAssociationType association ) {
    this.association = association;
  }

  public InstancePrivateIpAddressesSetType getPrivateIpAddressesSet( ) {
    return privateIpAddressesSet;
  }

  public void setPrivateIpAddressesSet( InstancePrivateIpAddressesSetType privateIpAddressesSet ) {
    this.privateIpAddressesSet = privateIpAddressesSet;
  }
}
