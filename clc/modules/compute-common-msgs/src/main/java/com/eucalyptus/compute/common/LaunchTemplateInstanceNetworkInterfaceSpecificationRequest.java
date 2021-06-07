/*
 * Copyright 2018 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.compute.common;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class LaunchTemplateInstanceNetworkInterfaceSpecificationRequest extends EucalyptusData {

  private Boolean associatePublicIpAddress;
  private Boolean deleteOnTermination;
  private String description;
  private Integer deviceIndex;
  private SecurityGroupIdStringList groups;
  private Integer ipv6AddressCount;
  private InstanceIpv6AddressListRequest ipv6Addresses;
  private String networkInterfaceId;
  private String privateIpAddress;
  private PrivateIpAddressSpecificationList privateIpAddresses;
  private Integer secondaryPrivateIpAddressCount;
  private String subnetId;

  public Boolean getAssociatePublicIpAddress( ) {
    return associatePublicIpAddress;
  }

  public void setAssociatePublicIpAddress( final Boolean associatePublicIpAddress ) {
    this.associatePublicIpAddress = associatePublicIpAddress;
  }

  public Boolean getDeleteOnTermination( ) {
    return deleteOnTermination;
  }

  public void setDeleteOnTermination( final Boolean deleteOnTermination ) {
    this.deleteOnTermination = deleteOnTermination;
  }

  public String getDescription( ) {
    return description;
  }

  public void setDescription( final String description ) {
    this.description = description;
  }

  public Integer getDeviceIndex( ) {
    return deviceIndex;
  }

  public void setDeviceIndex( final Integer deviceIndex ) {
    this.deviceIndex = deviceIndex;
  }

  public SecurityGroupIdStringList getGroups( ) {
    return groups;
  }

  public void setGroups( final SecurityGroupIdStringList groups ) {
    this.groups = groups;
  }

  public Integer getIpv6AddressCount( ) {
    return ipv6AddressCount;
  }

  public void setIpv6AddressCount( final Integer ipv6AddressCount ) {
    this.ipv6AddressCount = ipv6AddressCount;
  }

  public InstanceIpv6AddressListRequest getIpv6Addresses( ) {
    return ipv6Addresses;
  }

  public void setIpv6Addresses( final InstanceIpv6AddressListRequest ipv6Addresses ) {
    this.ipv6Addresses = ipv6Addresses;
  }

  public String getNetworkInterfaceId( ) {
    return networkInterfaceId;
  }

  public void setNetworkInterfaceId( final String networkInterfaceId ) {
    this.networkInterfaceId = networkInterfaceId;
  }

  public String getPrivateIpAddress( ) {
    return privateIpAddress;
  }

  public void setPrivateIpAddress( final String privateIpAddress ) {
    this.privateIpAddress = privateIpAddress;
  }

  public PrivateIpAddressSpecificationList getPrivateIpAddresses( ) {
    return privateIpAddresses;
  }

  public void setPrivateIpAddresses( final PrivateIpAddressSpecificationList privateIpAddresses ) {
    this.privateIpAddresses = privateIpAddresses;
  }

  public Integer getSecondaryPrivateIpAddressCount( ) {
    return secondaryPrivateIpAddressCount;
  }

  public void setSecondaryPrivateIpAddressCount( final Integer secondaryPrivateIpAddressCount ) {
    this.secondaryPrivateIpAddressCount = secondaryPrivateIpAddressCount;
  }

  public String getSubnetId( ) {
    return subnetId;
  }

  public void setSubnetId( final String subnetId ) {
    this.subnetId = subnetId;
  }

}
