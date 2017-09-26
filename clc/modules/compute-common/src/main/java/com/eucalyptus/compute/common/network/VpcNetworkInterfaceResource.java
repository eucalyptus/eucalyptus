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
package com.eucalyptus.compute.common.network;

import java.util.ArrayList;
import java.util.Objects;

public class VpcNetworkInterfaceResource extends NetworkResource {

  private Integer device;
  private String mac;
  private String vpc;
  private String subnet;
  private String privateIp;
  private String description;
  private Boolean deleteOnTerminate;
  private ArrayList<String> networkGroupIds;
  private String attachmentId;

  @Override
  public String getType( ) {
    return "network-interface";
  }

  public Integer getDevice( ) {
    return device;
  }

  public void setDevice( Integer device ) {
    this.device = device;
  }

  public String getMac( ) {
    return mac;
  }

  public void setMac( String mac ) {
    this.mac = mac;
  }

  public String getVpc( ) {
    return vpc;
  }

  public void setVpc( String vpc ) {
    this.vpc = vpc;
  }

  public String getSubnet( ) {
    return subnet;
  }

  public void setSubnet( String subnet ) {
    this.subnet = subnet;
  }

  public String getPrivateIp( ) {
    return privateIp;
  }

  public void setPrivateIp( String privateIp ) {
    this.privateIp = privateIp;
  }

  public String getDescription( ) {
    return description;
  }

  public void setDescription( String description ) {
    this.description = description;
  }

  public Boolean getDeleteOnTerminate( ) {
    return deleteOnTerminate;
  }

  public void setDeleteOnTerminate( Boolean deleteOnTerminate ) {
    this.deleteOnTerminate = deleteOnTerminate;
  }

  public ArrayList<String> getNetworkGroupIds( ) {
    return networkGroupIds;
  }

  public void setNetworkGroupIds( ArrayList<String> networkGroupIds ) {
    this.networkGroupIds = networkGroupIds;
  }

  public String getAttachmentId( ) {
    return attachmentId;
  }

  public void setAttachmentId( String attachmentId ) {
    this.attachmentId = attachmentId;
  }

  @Override
  public boolean equals( final Object o ) {
    if ( this == o ) return true;
    if ( o == null || getClass( ) != o.getClass( ) ) return false;
    if ( !super.equals( o ) ) return false;
    final VpcNetworkInterfaceResource that = (VpcNetworkInterfaceResource) o;
    return Objects.equals( getDevice( ), that.getDevice( ) ) &&
        Objects.equals( getMac( ), that.getMac( ) ) &&
        Objects.equals( getVpc( ), that.getVpc( ) ) &&
        Objects.equals( getSubnet( ), that.getSubnet( ) ) &&
        Objects.equals( getPrivateIp( ), that.getPrivateIp( ) ) &&
        Objects.equals( getDescription( ), that.getDescription( ) ) &&
        Objects.equals( getDeleteOnTerminate( ), that.getDeleteOnTerminate( ) ) &&
        Objects.equals( getNetworkGroupIds( ), that.getNetworkGroupIds( ) ) &&
        Objects.equals( getAttachmentId( ), that.getAttachmentId( ) );
  }

  @Override
  public int hashCode( ) {
    return Objects.hash( super.hashCode( ), getDevice( ), getMac( ), getVpc( ), getSubnet( ),
        getPrivateIp( ), getDescription( ), getDeleteOnTerminate( ), getNetworkGroupIds( ), getAttachmentId( ) );
  }

  @Override
  public String toString( ) {
    return toStringHelper( this )
        .add( "device", device )
        .add( "mac", mac )
        .add( "vpc", vpc )
        .add( "subnet", subnet )
        .add( "privateIp", privateIp )
        .add( "description", description )
        .add( "deleteOnTerminate", deleteOnTerminate )
        .add( "networkGroupIds", networkGroupIds )
        .add( "attachmentId", attachmentId )
        .toString( );
  }
}
