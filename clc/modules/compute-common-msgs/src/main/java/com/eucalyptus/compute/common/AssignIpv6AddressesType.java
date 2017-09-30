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
import com.google.common.collect.Lists;

public class AssignIpv6AddressesType extends VpcIpv6ComputeMessage {

  private Integer ipv6AddressCount;
  private ArrayList<String> Ipv6Addresses = Lists.newArrayList( );
  private String networkInterfaceId;

  public Integer getIpv6AddressCount( ) {
    return ipv6AddressCount;
  }

  public void setIpv6AddressCount( Integer ipv6AddressCount ) {
    this.ipv6AddressCount = ipv6AddressCount;
  }

  public ArrayList<String> getIpv6Addresses( ) {
    return Ipv6Addresses;
  }

  public void setIpv6Addresses( ArrayList<String> Ipv6Addresses ) {
    this.Ipv6Addresses = Ipv6Addresses;
  }

  public String getNetworkInterfaceId( ) {
    return networkInterfaceId;
  }

  public void setNetworkInterfaceId( String networkInterfaceId ) {
    this.networkInterfaceId = networkInterfaceId;
  }
}
