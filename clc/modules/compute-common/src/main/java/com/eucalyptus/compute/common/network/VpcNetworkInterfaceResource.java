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
package com.eucalyptus.compute.common.network;

import java.util.ArrayList;
import java.util.Objects;

public class VpcNetworkInterfaceResource extends NetworkResource {
  private static final long serialVersionUID = 1L;

  private Integer device;
  private String mac;
  private String vpc;
  private String subnet;
  private String privateIp;
  private String description;
  private Boolean deleteOnTerminate;
  private ArrayList<String> networkGroupIds;
  private String attachmentId;

  public VpcNetworkInterfaceResource( ) {
  }

  public VpcNetworkInterfaceResource(
      final String ownerId,
      final String value,
      final String mac,
      final String privateIp
  ) {
    super( ownerId, value );
    this.mac = mac;
    this.privateIp = privateIp;
  }

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
