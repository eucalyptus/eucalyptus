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
package com.eucalyptus.cluster.common.msgs;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;

public class NetConfigType extends EucalyptusData {

  private String interfaceId;
  private Integer device;
  private String privateMacAddress;
  private String privateIp;
  private String publicIp;
  private Integer vlan;
  private Integer networkIndex;
  private String attachmentId;

  public String getInterfaceId( ) {
    return interfaceId;
  }

  public void setInterfaceId( String interfaceId ) {
    this.interfaceId = interfaceId;
  }

  public Integer getDevice( ) {
    return device;
  }

  public void setDevice( Integer device ) {
    this.device = device;
  }

  public String getPrivateMacAddress( ) {
    return privateMacAddress;
  }

  public void setPrivateMacAddress( String privateMacAddress ) {
    this.privateMacAddress = privateMacAddress;
  }

  public String getPrivateIp( ) {
    return privateIp;
  }

  public void setPrivateIp( String privateIp ) {
    this.privateIp = privateIp;
  }

  public String getPublicIp( ) {
    return publicIp;
  }

  public void setPublicIp( String publicIp ) {
    this.publicIp = publicIp;
  }

  public Integer getVlan( ) {
    return vlan;
  }

  public void setVlan( Integer vlan ) {
    this.vlan = vlan;
  }

  public Integer getNetworkIndex( ) {
    return networkIndex;
  }

  public void setNetworkIndex( Integer networkIndex ) {
    this.networkIndex = networkIndex;
  }

  public String getAttachmentId( ) {
    return attachmentId;
  }

  public void setAttachmentId( String attachmentId ) {
    this.attachmentId = attachmentId;
  }
}
