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

/** *******************************************************************************/
public class AddressInfoType extends EucalyptusData {

  private String publicIp;
  private String allocationId;
  private String domain;
  private String instanceId;
  private String associationId;
  private String networkInterfaceId;
  private String networkInterfaceOwnerId;
  private String privateIpAddress;

  public AddressInfoType( final String publicIp, final String domain, final String instanceId ) {
    this.publicIp = publicIp;
    this.domain = domain;
    this.instanceId = instanceId;
  }

  public AddressInfoType( ) {
  }

  public String getPublicIp( ) {
    return publicIp;
  }

  public void setPublicIp( String publicIp ) {
    this.publicIp = publicIp;
  }

  public String getAllocationId( ) {
    return allocationId;
  }

  public void setAllocationId( String allocationId ) {
    this.allocationId = allocationId;
  }

  public String getDomain( ) {
    return domain;
  }

  public void setDomain( String domain ) {
    this.domain = domain;
  }

  public String getInstanceId( ) {
    return instanceId;
  }

  public void setInstanceId( String instanceId ) {
    this.instanceId = instanceId;
  }

  public String getAssociationId( ) {
    return associationId;
  }

  public void setAssociationId( String associationId ) {
    this.associationId = associationId;
  }

  public String getNetworkInterfaceId( ) {
    return networkInterfaceId;
  }

  public void setNetworkInterfaceId( String networkInterfaceId ) {
    this.networkInterfaceId = networkInterfaceId;
  }

  public String getNetworkInterfaceOwnerId( ) {
    return networkInterfaceOwnerId;
  }

  public void setNetworkInterfaceOwnerId( String networkInterfaceOwnerId ) {
    this.networkInterfaceOwnerId = networkInterfaceOwnerId;
  }

  public String getPrivateIpAddress( ) {
    return privateIpAddress;
  }

  public void setPrivateIpAddress( String privateIpAddress ) {
    this.privateIpAddress = privateIpAddress;
  }
}
