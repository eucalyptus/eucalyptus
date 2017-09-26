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
