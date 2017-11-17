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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import com.eucalyptus.binding.HttpEmbedded;
import com.google.common.collect.Lists;
import edu.ucsb.eucalyptus.msgs.EucalyptusData;

public class IpPermissionType extends EucalyptusData {

  private String ipProtocol;
  private Integer fromPort;
  private Integer toPort;
  @HttpEmbedded( multiple = true )
  private ArrayList<UserIdGroupPairType> groups = new ArrayList<UserIdGroupPairType>( );
  @HttpEmbedded( multiple = true )
  private ArrayList<CidrIpType> ipRanges = new ArrayList<CidrIpType>( );

  public IpPermissionType( ) {
  }

  public IpPermissionType( String ipProtocol, Integer fromPort, Integer toPort ) {
    this.ipProtocol = ipProtocol;
    this.fromPort = fromPort;
    this.toPort = toPort;
  }

  public List<String> getCidrIpRanges( ) {
    List<String> ranges = Lists.newArrayList( );
    for ( CidrIpType ipRange : ipRanges ) {
      ranges.add( ipRange.getCidrIp( ) );
    }

    return ranges;
  }

  public void setCidrIpRanges( Collection<String> cidrIps ) {
    ArrayList<CidrIpType> ranges = Lists.newArrayList( );
    for ( String cidrIp : cidrIps ) {
      CidrIpType ipType = new CidrIpType( );
      ipType.setCidrIp( cidrIp );
      ranges.add( ipType );
    }

    ipRanges = ranges;
  }

  public String getIpProtocol( ) {
    return ipProtocol;
  }

  public void setIpProtocol( String ipProtocol ) {
    this.ipProtocol = ipProtocol;
  }

  public Integer getFromPort( ) {
    return fromPort;
  }

  public void setFromPort( Integer fromPort ) {
    this.fromPort = fromPort;
  }

  public Integer getToPort( ) {
    return toPort;
  }

  public void setToPort( Integer toPort ) {
    this.toPort = toPort;
  }

  public ArrayList<UserIdGroupPairType> getGroups( ) {
    return groups;
  }

  public void setGroups( ArrayList<UserIdGroupPairType> groups ) {
    this.groups = groups;
  }

  public ArrayList<CidrIpType> getIpRanges( ) {
    return ipRanges;
  }

  public void setIpRanges( ArrayList<CidrIpType> ipRanges ) {
    this.ipRanges = ipRanges;
  }
}
