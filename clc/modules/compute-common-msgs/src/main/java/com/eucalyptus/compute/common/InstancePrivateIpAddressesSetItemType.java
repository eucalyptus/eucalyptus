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

public class InstancePrivateIpAddressesSetItemType extends EucalyptusData {

  private String privateIpAddress;
  private String privateDnsName;
  private Boolean primary;
  private InstanceNetworkInterfaceAssociationType association;

  public InstancePrivateIpAddressesSetItemType( ) {
  }

  public InstancePrivateIpAddressesSetItemType( final String privateIpAddress, final String privateDnsName, final Boolean primary, final InstanceNetworkInterfaceAssociationType association ) {
    this.privateIpAddress = privateIpAddress;
    this.privateDnsName = privateDnsName;
    this.primary = primary;
    this.association = association;
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

  public Boolean getPrimary( ) {
    return primary;
  }

  public void setPrimary( Boolean primary ) {
    this.primary = primary;
  }

  public InstanceNetworkInterfaceAssociationType getAssociation( ) {
    return association;
  }

  public void setAssociation( InstanceNetworkInterfaceAssociationType association ) {
    this.association = association;
  }
}
