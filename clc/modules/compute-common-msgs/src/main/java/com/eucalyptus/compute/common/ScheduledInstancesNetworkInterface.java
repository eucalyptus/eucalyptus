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
package com.eucalyptus.compute.common;

import java.util.ArrayList;
import com.eucalyptus.binding.HttpEmbedded;
import edu.ucsb.eucalyptus.msgs.EucalyptusData;

public class ScheduledInstancesNetworkInterface extends EucalyptusData {

  private Boolean associatePublicIpAddress;
  private Boolean deleteOnTermination;
  private String description;
  private Integer deviceIndex;
  private ArrayList<String> groups;
  @HttpEmbedded( multiple = true )
  private ArrayList<ScheduledInstancesIpv6Address> ipv6Addresses;
  private Integer ipv6AddressCount;
  private String networkInterfaceId;
  private String privateIpAddress;
  @HttpEmbedded( multiple = true )
  private ArrayList<ScheduledInstancesPrivateIpAddressConfig> privateIpAddressConfigs;
  private Integer secondaryPrivateIpAddressCount;
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

  public ArrayList<String> getGroups( ) {
    return groups;
  }

  public void setGroups( ArrayList<String> groups ) {
    this.groups = groups;
  }

  public ArrayList<ScheduledInstancesIpv6Address> getIpv6Addresses( ) {
    return ipv6Addresses;
  }

  public void setIpv6Addresses( ArrayList<ScheduledInstancesIpv6Address> ipv6Addresses ) {
    this.ipv6Addresses = ipv6Addresses;
  }

  public Integer getIpv6AddressCount( ) {
    return ipv6AddressCount;
  }

  public void setIpv6AddressCount( Integer ipv6AddressCount ) {
    this.ipv6AddressCount = ipv6AddressCount;
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

  public ArrayList<ScheduledInstancesPrivateIpAddressConfig> getPrivateIpAddressConfigs( ) {
    return privateIpAddressConfigs;
  }

  public void setPrivateIpAddressConfigs( ArrayList<ScheduledInstancesPrivateIpAddressConfig> privateIpAddressConfigs ) {
    this.privateIpAddressConfigs = privateIpAddressConfigs;
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
}
