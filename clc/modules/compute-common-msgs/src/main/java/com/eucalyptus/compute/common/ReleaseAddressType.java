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


public class ReleaseAddressType extends VmAddressMessage {

  private String publicIp;
  private String allocationId;

  public ReleaseAddressType( final String publicIp ) {
    this.publicIp = publicIp;
  }

  public ReleaseAddressType( ) {
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
}
