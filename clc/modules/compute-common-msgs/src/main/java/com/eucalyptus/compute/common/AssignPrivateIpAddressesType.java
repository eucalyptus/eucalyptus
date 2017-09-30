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

public class AssignPrivateIpAddressesType extends VpcMessage {

  private String networkInterfaceId;
  @HttpEmbedded
  private AssignPrivateIpAddressesSetRequestType privateIpAddressesSet;
  private Integer secondaryPrivateIpAddressCount;
  private Boolean allowReassignment;

  public String getNetworkInterfaceId( ) {
    return networkInterfaceId;
  }

  public void setNetworkInterfaceId( String networkInterfaceId ) {
    this.networkInterfaceId = networkInterfaceId;
  }

  public AssignPrivateIpAddressesSetRequestType getPrivateIpAddressesSet( ) {
    return privateIpAddressesSet;
  }

  public void setPrivateIpAddressesSet( AssignPrivateIpAddressesSetRequestType privateIpAddressesSet ) {
    this.privateIpAddressesSet = privateIpAddressesSet;
  }

  public Integer getSecondaryPrivateIpAddressCount( ) {
    return secondaryPrivateIpAddressCount;
  }

  public void setSecondaryPrivateIpAddressCount( Integer secondaryPrivateIpAddressCount ) {
    this.secondaryPrivateIpAddressCount = secondaryPrivateIpAddressCount;
  }

  public Boolean getAllowReassignment( ) {
    return allowReassignment;
  }

  public void setAllowReassignment( Boolean allowReassignment ) {
    this.allowReassignment = allowReassignment;
  }
}
