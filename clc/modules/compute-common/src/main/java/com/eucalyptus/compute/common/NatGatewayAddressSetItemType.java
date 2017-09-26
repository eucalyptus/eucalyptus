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

public class NatGatewayAddressSetItemType extends EucalyptusData {

  private String networkInterfaceId;
  private String privateIp;
  private String allocationId;
  private String publicIp;

  public NatGatewayAddressSetItemType( ) {
  }

  public NatGatewayAddressSetItemType( final String networkInterfaceId, final String privateIp, final String allocationId, final String publicIp ) {
    this.networkInterfaceId = networkInterfaceId;
    this.privateIp = privateIp;
    this.allocationId = allocationId;
    this.publicIp = publicIp;
  }

  public String getNetworkInterfaceId( ) {
    return networkInterfaceId;
  }

  public void setNetworkInterfaceId( String networkInterfaceId ) {
    this.networkInterfaceId = networkInterfaceId;
  }

  public String getPrivateIp( ) {
    return privateIp;
  }

  public void setPrivateIp( String privateIp ) {
    this.privateIp = privateIp;
  }

  public String getAllocationId( ) {
    return allocationId;
  }

  public void setAllocationId( String allocationId ) {
    this.allocationId = allocationId;
  }

  public String getPublicIp( ) {
    return publicIp;
  }

  public void setPublicIp( String publicIp ) {
    this.publicIp = publicIp;
  }
}
