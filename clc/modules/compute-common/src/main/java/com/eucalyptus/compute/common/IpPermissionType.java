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
      ( (ArrayList<String>) ranges ).add( ipRange.getCidrIp( ) );
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
