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
