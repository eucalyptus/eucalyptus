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
package com.eucalyptus.cloudformation.resources.standard.info;

import com.eucalyptus.cloudformation.resources.ResourceInfo;
import com.eucalyptus.cloudformation.resources.annotations.AttributeJson;
import com.google.common.base.MoreObjects;

public class AWSEC2NetworkInterfaceResourceInfo extends ResourceInfo {

  @AttributeJson
  private String primaryPrivateIpAddress;
  @AttributeJson
  private String secondaryPrivateIpAddresses;

  public AWSEC2NetworkInterfaceResourceInfo( ) {
    setType( "AWS::EC2::NetworkInterface" );
  }

  @Override
  public boolean supportsTags( ) {
    return true;
  }

  public String getPrimaryPrivateIpAddress( ) {
    return primaryPrivateIpAddress;
  }

  public void setPrimaryPrivateIpAddress( String primaryPrivateIpAddress ) {
    this.primaryPrivateIpAddress = primaryPrivateIpAddress;
  }

  public String getSecondaryPrivateIpAddresses( ) {
    return secondaryPrivateIpAddresses;
  }

  public void setSecondaryPrivateIpAddresses( String secondaryPrivateIpAddresses ) {
    this.secondaryPrivateIpAddresses = secondaryPrivateIpAddresses;
  }

  @Override
  public String toString( ) {
    return MoreObjects.toStringHelper( this )
        .add( "primaryPrivateIpAddress", primaryPrivateIpAddress )
        .add( "secondaryPrivateIpAddresses", secondaryPrivateIpAddresses )
        .toString( );
  }
}
