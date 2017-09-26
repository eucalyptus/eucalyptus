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

public class InstanceNetworkInterfaceAssociationType extends EucalyptusData {

  private String publicIp;
  private String publicDnsName;
  private String ipOwnerId;

  public InstanceNetworkInterfaceAssociationType( ) {
  }

  public InstanceNetworkInterfaceAssociationType( final String publicIp, final String publicDnsName, final String ipOwnerId ) {
    this.publicIp = publicIp;
    this.publicDnsName = publicDnsName;
    this.ipOwnerId = ipOwnerId;
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
}
