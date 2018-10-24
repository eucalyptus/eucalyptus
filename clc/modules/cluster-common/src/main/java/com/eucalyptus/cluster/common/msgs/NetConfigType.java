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
