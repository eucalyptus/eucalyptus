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

public class NetworkConfigType extends EucalyptusData {

  private String interfaceId;
  private Integer device = 0;
  private String macAddress;
  private String ipAddress;
  private String ignoredPublicIp = "0.0.0.0";
  private String privateDnsName;
  private String publicDnsName;
  private Integer vlan = -1;
  private Long networkIndex = -1l;
  private String attachmentId;

  public NetworkConfigType( ) {
  }

  public NetworkConfigType( final String interfaceId, final Integer device ) {
    this.interfaceId = interfaceId;
    this.device = device;
  }

  public void updateDns( final String domain ) {
    this.ipAddress = ( this.ipAddress == null ? "0.0.0.0" : this.ipAddress );
    this.ignoredPublicIp = ( this.ignoredPublicIp == null ? "0.0.0.0" : this.ignoredPublicIp );
    this.publicDnsName = "euca-" + this.ignoredPublicIp.replaceAll( "\\.", "-" ) + ".eucalyptus." + domain;
    this.privateDnsName = "euca-" + this.ipAddress.replaceAll( "\\.", "-" ) + ".eucalyptus.internal";
  }

  @Override
  public String toString( ) {
    return "NetworkConfig interfaceId=" + interfaceId + ", device=" + String.valueOf( device ) + ", macAddress=" + macAddress + ", ipAddress=" + ipAddress + ", ignoredPublicIp=" + ignoredPublicIp + ", privateDnsName=" + privateDnsName + ", publicDnsName=" + publicDnsName + ", vlan=" + String.valueOf( vlan ) + ", networkIndex=" + String.valueOf( networkIndex );
  }

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

  public String getMacAddress( ) {
    return macAddress;
  }

  public void setMacAddress( String macAddress ) {
    this.macAddress = macAddress;
  }

  public String getIpAddress( ) {
    return ipAddress;
  }

  public void setIpAddress( String ipAddress ) {
    this.ipAddress = ipAddress;
  }

  public String getIgnoredPublicIp( ) {
    return ignoredPublicIp;
  }

  public void setIgnoredPublicIp( String ignoredPublicIp ) {
    this.ignoredPublicIp = ignoredPublicIp;
  }

  public String getPrivateDnsName( ) {
    return privateDnsName;
  }

  public void setPrivateDnsName( String privateDnsName ) {
    this.privateDnsName = privateDnsName;
  }

  public String getPublicDnsName( ) {
    return publicDnsName;
  }

  public void setPublicDnsName( String publicDnsName ) {
    this.publicDnsName = publicDnsName;
  }

  public Integer getVlan( ) {
    return vlan;
  }

  public void setVlan( Integer vlan ) {
    this.vlan = vlan;
  }

  public Long getNetworkIndex( ) {
    return networkIndex;
  }

  public void setNetworkIndex( Long networkIndex ) {
    this.networkIndex = networkIndex;
  }

  public String getAttachmentId( ) {
    return attachmentId;
  }

  public void setAttachmentId( String attachmentId ) {
    this.attachmentId = attachmentId;
  }
}
