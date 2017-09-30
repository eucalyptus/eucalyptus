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
import edu.ucsb.eucalyptus.msgs.ComputeMessageValidation;

public class CreateNetworkInterfaceType extends VpcMessage {

  private String subnetId;
  @ComputeMessageValidation.FieldRegex( ComputeMessageValidation.FieldRegexValue.STRING_255 )
  private String description;
  @ComputeMessageValidation.FieldRegex( ComputeMessageValidation.FieldRegexValue.IP_ADDRESS )
  private String privateIpAddress;
  @HttpEmbedded
  private SecurityGroupIdSetType groupSet;
  @HttpEmbedded
  private PrivateIpAddressesSetRequestType privateIpAddressesSet;
  private Integer secondaryPrivateIpAddressCount;

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
}
