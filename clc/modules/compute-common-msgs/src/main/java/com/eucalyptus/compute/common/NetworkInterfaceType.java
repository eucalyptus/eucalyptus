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

import java.util.Collection;
import java.util.Collections;
import com.eucalyptus.util.CompatFunction;
import edu.ucsb.eucalyptus.msgs.EucalyptusData;

public class NetworkInterfaceType extends EucalyptusData implements VpcTagged {

  private String networkInterfaceId;
  private String subnetId;
  private String vpcId;
  private String availabilityZone;
  private String description;
  private String ownerId;
  private String requesterId;
  private Boolean requesterManaged;
  private String status;
  private String macAddress;
  private String privateIpAddress;
  private String privateDnsName;
  private Boolean sourceDestCheck;
  private String interfaceType;
  private GroupSetType groupSet = new GroupSetType( );
  private NetworkInterfaceAttachmentType attachment;
  private NetworkInterfaceAssociationType association;
  private ResourceTagSetType tagSet = new ResourceTagSetType( );
  private NetworkInterfacePrivateIpAddressesSetType privateIpAddressesSet = new NetworkInterfacePrivateIpAddressesSetType( );

  public NetworkInterfaceType( ) {
  }

  public NetworkInterfaceType( final String networkInterfaceId, final String subnetId, final String vpcId, final String availabilityZone, final String description, final String ownerId, final String requesterId, final Boolean requesterManaged, final String status, final String macAddress, final String privateIpAddress, final String privateDnsName, final Boolean sourceDestCheck, final String interfaceType, final NetworkInterfaceAssociationType association, final NetworkInterfaceAttachmentType attachment, final Collection<GroupItemType> securityGroups ) {
    this.networkInterfaceId = networkInterfaceId;
    this.subnetId = subnetId;
    this.vpcId = vpcId;
    this.availabilityZone = availabilityZone;
    this.description = description;
    this.ownerId = ownerId;
    this.requesterId = requesterId;
    this.requesterManaged = requesterManaged;
    this.status = status;
    this.macAddress = macAddress;
    this.privateIpAddress = privateIpAddress;
    this.privateDnsName = privateDnsName;
    this.sourceDestCheck = sourceDestCheck;
    this.interfaceType = interfaceType;
    this.association = association;
    this.attachment = attachment;
    this.privateIpAddressesSet = new NetworkInterfacePrivateIpAddressesSetType( Collections.singleton( new NetworkInterfacePrivateIpAddressesSetItemType( privateIpAddress, privateDnsName, true, association ) ) );
    this.groupSet = new GroupSetType( securityGroups );
  }

  public static CompatFunction<NetworkInterfaceType, String> id( ) {
    return new CompatFunction<NetworkInterfaceType, String>( ) {
      @Override
      public String apply( final NetworkInterfaceType networkInterfaceType ) {
        return networkInterfaceType.getNetworkInterfaceId( );
      }
    };
  }

  public String getNetworkInterfaceId( ) {
    return networkInterfaceId;
  }

  public void setNetworkInterfaceId( String networkInterfaceId ) {
    this.networkInterfaceId = networkInterfaceId;
  }

  public String getSubnetId( ) {
    return subnetId;
  }

  public void setSubnetId( String subnetId ) {
    this.subnetId = subnetId;
  }

  public String getVpcId( ) {
    return vpcId;
  }

  public void setVpcId( String vpcId ) {
    this.vpcId = vpcId;
  }

  public String getAvailabilityZone( ) {
    return availabilityZone;
  }

  public void setAvailabilityZone( String availabilityZone ) {
    this.availabilityZone = availabilityZone;
  }

  public String getDescription( ) {
    return description;
  }

  public void setDescription( String description ) {
    this.description = description;
  }

  public String getOwnerId( ) {
    return ownerId;
  }

  public void setOwnerId( String ownerId ) {
    this.ownerId = ownerId;
  }

  public String getRequesterId( ) {
    return requesterId;
  }

  public void setRequesterId( String requesterId ) {
    this.requesterId = requesterId;
  }

  public Boolean getRequesterManaged( ) {
    return requesterManaged;
  }

  public void setRequesterManaged( Boolean requesterManaged ) {
    this.requesterManaged = requesterManaged;
  }

  public String getStatus( ) {
    return status;
  }

  public void setStatus( String status ) {
    this.status = status;
  }

  public String getMacAddress( ) {
    return macAddress;
  }

  public void setMacAddress( String macAddress ) {
    this.macAddress = macAddress;
  }

  public String getPrivateIpAddress( ) {
    return privateIpAddress;
  }

  public void setPrivateIpAddress( String privateIpAddress ) {
    this.privateIpAddress = privateIpAddress;
  }

  public String getPrivateDnsName( ) {
    return privateDnsName;
  }

  public void setPrivateDnsName( String privateDnsName ) {
    this.privateDnsName = privateDnsName;
  }

  public Boolean getSourceDestCheck( ) {
    return sourceDestCheck;
  }

  public void setSourceDestCheck( Boolean sourceDestCheck ) {
    this.sourceDestCheck = sourceDestCheck;
  }

  public String getInterfaceType( ) {
    return interfaceType;
  }

  public void setInterfaceType( String interfaceType ) {
    this.interfaceType = interfaceType;
  }

  public GroupSetType getGroupSet( ) {
    return groupSet;
  }

  public void setGroupSet( GroupSetType groupSet ) {
    this.groupSet = groupSet;
  }

  public NetworkInterfaceAttachmentType getAttachment( ) {
    return attachment;
  }

  public void setAttachment( NetworkInterfaceAttachmentType attachment ) {
    this.attachment = attachment;
  }

  public NetworkInterfaceAssociationType getAssociation( ) {
    return association;
  }

  public void setAssociation( NetworkInterfaceAssociationType association ) {
    this.association = association;
  }

  public ResourceTagSetType getTagSet( ) {
    return tagSet;
  }

  public void setTagSet( ResourceTagSetType tagSet ) {
    this.tagSet = tagSet;
  }

  public NetworkInterfacePrivateIpAddressesSetType getPrivateIpAddressesSet( ) {
    return privateIpAddressesSet;
  }

  public void setPrivateIpAddressesSet( NetworkInterfacePrivateIpAddressesSetType privateIpAddressesSet ) {
    this.privateIpAddressesSet = privateIpAddressesSet;
  }
}
