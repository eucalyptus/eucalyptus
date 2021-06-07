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
package com.eucalyptus.compute.common;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;

public class NetworkInterfaceAssociationType extends EucalyptusData {

  private String publicIp;
  private String publicDnsName;
  private String ipOwnerId;
  private String allocationId;
  private String associationId;

  public NetworkInterfaceAssociationType( ) {
  }

  public NetworkInterfaceAssociationType( final String publicIp, final String publicDnsName, final String ipOwnerId, final String allocationId, final String associationId ) {
    this.publicIp = publicIp;
    this.publicDnsName = publicDnsName;
    this.ipOwnerId = ipOwnerId;
    this.allocationId = allocationId;
    this.associationId = associationId;
  }

  public String getPublicIp( ) {
    return publicIp;
  }

  public void setPublicIp( String publicIp ) {
    this.publicIp = publicIp;
  }

  public String getPublicDnsName( ) {
    return publicDnsName;
  }

  public void setPublicDnsName( String publicDnsName ) {
    this.publicDnsName = publicDnsName;
  }

  public String getIpOwnerId( ) {
    return ipOwnerId;
  }

  public void setIpOwnerId( String ipOwnerId ) {
    this.ipOwnerId = ipOwnerId;
  }

  public String getAllocationId( ) {
    return allocationId;
  }

  public void setAllocationId( String allocationId ) {
    this.allocationId = allocationId;
  }

  public String getAssociationId( ) {
    return associationId;
  }

  public void setAssociationId( String associationId ) {
    this.associationId = associationId;
  }
}
