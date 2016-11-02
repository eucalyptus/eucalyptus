/*************************************************************************
 * Copyright 2009-2016 Eucalyptus Systems, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 *
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 ************************************************************************/

@GroovyAddClassUUID
package com.eucalyptus.compute.common

import com.eucalyptus.binding.HttpEmbedded
import com.eucalyptus.binding.HttpParameterMapping
import com.eucalyptus.binding.HttpValue
import com.google.common.base.Function
import com.google.common.collect.Lists
import com.google.common.collect.Maps
import edu.ucsb.eucalyptus.msgs.GroovyAddClassUUID
import edu.ucsb.eucalyptus.msgs.EucalyptusData
import edu.ucsb.eucalyptus.msgs.ComputeMessageValidation

import javax.annotation.Nonnull

import static com.eucalyptus.util.MessageValidation.validateRecursively;
import static com.eucalyptus.util.MessageValidation.ValidatableMessage;
import static edu.ucsb.eucalyptus.msgs.ComputeMessageValidation.FieldRange
import static edu.ucsb.eucalyptus.msgs.ComputeMessageValidation.FieldRegex
import static edu.ucsb.eucalyptus.msgs.ComputeMessageValidation.FieldRegexValue

class VpcMessage extends ComputeMessage implements ValidatableMessage {

  Map<String,String> validate( ) {
    validateRecursively(
        Maps.<String,String>newTreeMap( ),
        new ComputeMessageValidation.ComputeMessageValidationAssistant( ),
        "",
        this )
  }
}

interface VpcTagged {
  ResourceTagSetType getTagSet( )
  void setTagSet( ResourceTagSetType tags )
}
class DescribeVpcAttributeType extends VpcMessage {
  String vpcId;
  String attribute

  void hasEnableDnsSupport( boolean flag ) {
    if ( flag ) attribute = "enableDnsSupport"
  }

  void hasEnableDnsHostnames( boolean flag ) {
    if ( flag ) attribute = "enableDnsHostnames"
  }

  Object getAttrObject( ){ null }
  void setAttrObject( Object value ) { }

  DescribeVpcAttributeType() {  }
}
class DhcpOptionsIdSetType extends EucalyptusData {
  DhcpOptionsIdSetType() {  }
  @HttpParameterMapping (parameter = "DhcpOptionsId")
  @HttpEmbedded(multiple=true)
  ArrayList<DhcpOptionsIdSetItemType> item = new ArrayList<DhcpOptionsIdSetItemType>();
}
class ModifySubnetAttributeResponseType extends VpcMessage {
  ModifySubnetAttributeResponseType() {  }
}
class DetachInternetGatewayResponseType extends VpcMessage {
  DetachInternetGatewayResponseType() {  }
}
class BlockDeviceMappingType extends EucalyptusData {
  BlockDeviceMappingType() {  }
  ArrayList<BlockDeviceMappingItemType> item = new ArrayList<BlockDeviceMappingItemType>();
}
class NetworkInterfaceAttachmentType extends EucalyptusData {
  String attachmentId;
  String instanceId;
  String instanceOwnerId;
  Integer deviceIndex;
  String status;
  Date attachTime;
  Boolean deleteOnTermination;
  NetworkInterfaceAttachmentType() {  }

  NetworkInterfaceAttachmentType(
      final String attachmentId,
      final String instanceId,
      final String instanceOwnerId,
      final Integer deviceIndex,
      final String status,
      final Date attachTime,
      final Boolean deleteOnTermination) {
    this.attachmentId = attachmentId
    this.instanceId = instanceId
    this.instanceOwnerId = instanceOwnerId
    this.deviceIndex = deviceIndex
    this.status = status
    this.attachTime = attachTime
    this.deleteOnTermination = deleteOnTermination
  }
}
class DescribeSubnetsResponseType extends VpcMessage {
  SubnetSetType subnetSet = new SubnetSetType();
  DescribeSubnetsResponseType() {  }
}
class NetworkInterfacePrivateIpAddressesSetItemType extends EucalyptusData {
  String privateIpAddress;
  String privateDnsName;
  Boolean primary;
  NetworkInterfaceAssociationType association;
  NetworkInterfacePrivateIpAddressesSetItemType() {  }
}
class VpnConnectionOptionsRequestType extends EucalyptusData {
  Boolean staticRoutesOnly;
  VpnConnectionOptionsRequestType() {  }
}
class DeleteNetworkAclEntryResponseType extends VpcMessage {
  DeleteNetworkAclEntryResponseType() {  }
}
class CreateVpnConnectionRouteType extends VpcMessage {
  String vpnConnectionId;
  String destinationCidrBlock;
  CreateVpnConnectionRouteType() {  }
}
class DetachNetworkInterfaceResponseType extends VpcMessage {
  DetachNetworkInterfaceResponseType() {  }
}
class DescribeNetworkInterfaceAttributeResponseType extends VpcMessage {
  String networkInterfaceId;
  NullableAttributeValueType description;
  AttributeBooleanValueType sourceDestCheck;
  GroupSetType groupSet;
  NetworkInterfaceAttachmentType attachment;
  DescribeNetworkInterfaceAttributeResponseType() {  }
}
class DetachVpnGatewayType extends VpcMessage {
  String vpnGatewayId;
  String vpcId;
  DetachVpnGatewayType() {  }
}
class DhcpOptionsSetType extends EucalyptusData {
  DhcpOptionsSetType() {  }
  ArrayList<DhcpOptionsType> item = new ArrayList<DhcpOptionsType>();
}
class DescribeCustomerGatewaysType extends VpcMessage {
  @HttpEmbedded
  CustomerGatewayIdSetType customerGatewaySet;
  @HttpParameterMapping (parameter = "Filter")
  @HttpEmbedded( multiple = true )
  ArrayList<Filter> filterSet = new ArrayList<Filter>();
  DescribeCustomerGatewaysType() {  }
}
class DescribeVpnGatewaysResponseType extends VpcMessage {
  VpnGatewaySetType vpnGatewaySet = new VpnGatewaySetType();
  DescribeVpnGatewaysResponseType() {  }
}
class DetachInternetGatewayType extends VpcMessage {
  String internetGatewayId;
  String vpcId;
  DetachInternetGatewayType() {  }
}
class DeleteVpnConnectionType extends VpcMessage {
  String vpnConnectionId;
  DeleteVpnConnectionType() {  }
}
class AttachNetworkInterfaceResponseType extends VpcMessage {
  String attachmentId;
  AttachNetworkInterfaceResponseType() {  }
}
class AssignPrivateIpAddressesSetRequestType extends EucalyptusData {
  AssignPrivateIpAddressesSetRequestType() {  }
  @HttpParameterMapping( parameter = "PrivateIpAddress" )
  @HttpEmbedded(multiple=true)
  ArrayList<AssignPrivateIpAddressesSetItemRequestType> item = new ArrayList<AssignPrivateIpAddressesSetItemRequestType>();
}
class DescribeAccountAttributesType extends VpcMessage {
  @HttpEmbedded
  AccountAttributeNameSetType accountAttributeNameSet;
  @HttpParameterMapping (parameter = "Filter")
  @HttpEmbedded( multiple = true )
  ArrayList<Filter> filterSet = new ArrayList<Filter>();
  DescribeAccountAttributesType() {  }

  Iterable<String> attributeNames( ) {
    accountAttributeNameSet?.item?.collect{ AccountAttributeNameSetItemType item -> item.attributeName } ?: [ ]
  }
}
class RouteTableIdSetType extends EucalyptusData {
  RouteTableIdSetType() {  }
  @HttpParameterMapping (parameter = "RouteTableId")
  @HttpEmbedded(multiple=true)
  ArrayList<RouteTableIdSetItemType> item = new ArrayList<RouteTableIdSetItemType>();
}
class DeleteInternetGatewayType extends VpcMessage {
  String internetGatewayId;
  DeleteInternetGatewayType() {  }
}
class DeleteNatGatewayType extends VpcMessage {
  String natGatewayId
}
class DeleteNatGatewayResponseType extends VpcMessage {
  String natGatewayId
}
class VpcType extends EucalyptusData implements VpcTagged {
  String vpcId;
  String state;
  String cidrBlock;
  String dhcpOptionsId;
  ResourceTagSetType tagSet;
  String instanceTenancy;
  Boolean isDefault;
  VpcType () { }

  VpcType( final String vpcId,
           final String state,
           final String cidrBlock,
           final String dhcpOptionsId,
           final Boolean isDefault ) {
    this.vpcId = vpcId
    this.state = state
    this.cidrBlock = cidrBlock
    this.dhcpOptionsId = dhcpOptionsId
    this.instanceTenancy = 'default'
    this.isDefault = isDefault
  }

  static Function<VpcType, String> id( ) {
    { VpcType vpc -> vpc.vpcId } as Function<VpcType, String>
  }
}
class RouteSetType extends EucalyptusData {
  RouteSetType() {  }
  ArrayList<RouteType> item = new ArrayList<RouteType>();
}
class UnassignPrivateIpAddressesResponseType extends VpcMessage {
  UnassignPrivateIpAddressesResponseType() {  }
}
class CreateVpnConnectionRouteResponseType extends VpcMessage {
  CreateVpnConnectionRouteResponseType() {  }
}
class RouteTableAssociationSetType extends EucalyptusData {
  RouteTableAssociationSetType() {  }
  ArrayList<RouteTableAssociationType> item = new ArrayList<RouteTableAssociationType>();
}
class FilterType extends EucalyptusData {
  String name;
  ValueSetType valueSet;
  FilterType() {  }
}
class ResetNetworkInterfaceAttributeResponseType extends VpcMessage {
  ResetNetworkInterfaceAttributeResponseType() {  }
}
class PortRangeType extends EucalyptusData {
  @FieldRange( max = 65535l )
  Integer from;
  @FieldRange( max = 65535l )
  Integer to;
  PortRangeType() {  }
}
class DeleteRouteType extends VpcMessage {
  String routeTableId;
  @FieldRegex( FieldRegexValue.CIDR )
  String destinationCidrBlock;
  DeleteRouteType() {  }
}
class VpnConnectionIdSetItemType extends EucalyptusData {
  @HttpValue
  String vpnConnectionId;
  VpnConnectionIdSetItemType() {  }
}
class CreateVpnConnectionResponseType extends VpcMessage {
  VpnConnectionType vpnConnection;
  CreateVpnConnectionResponseType() {  }
}
class DeleteVpcType extends VpcMessage {
  String vpcId;
  DeleteVpcType() {  }
}
class ModifySubnetAttributeType extends VpcMessage {
  String subnetId;
  AttributeBooleanValueType mapPublicIpOnLaunch;
  ModifySubnetAttributeType() {  }
}
class NetworkAclAssociationType extends EucalyptusData {
  String networkAclAssociationId;
  String networkAclId;
  String subnetId;
  NetworkAclAssociationType() {  }

  NetworkAclAssociationType(final String networkAclAssociationId,
                            final String networkAclId,
                            final String subnetId) {
    this.networkAclAssociationId = networkAclAssociationId
    this.networkAclId = networkAclId
    this.subnetId = subnetId
  }
}
class DescribeDhcpOptionsResponseType extends VpcMessage {
  DhcpOptionsSetType dhcpOptionsSet = new DhcpOptionsSetType();
  DescribeDhcpOptionsResponseType() {  }
}
class CreateInternetGatewayResponseType extends VpcMessage {
  InternetGatewayType internetGateway;
  CreateInternetGatewayResponseType() {  }
}
class DescribeVpnGatewaysType extends VpcMessage {
  @HttpEmbedded
  VpnGatewayIdSetType vpnGatewaySet;
  @HttpParameterMapping (parameter = "Filter")
  @HttpEmbedded( multiple = true )
  ArrayList<Filter> filterSet = new ArrayList<Filter>();
  DescribeVpnGatewaysType() {  }
}
class EmptyElementType extends EucalyptusData {
  EmptyElementType() {  }
}
class InternetGatewayAttachmentSetType extends EucalyptusData {
  InternetGatewayAttachmentSetType() {  }
  ArrayList<InternetGatewayAttachmentType> item = new ArrayList<InternetGatewayAttachmentType>();
}
class NetworkAclIdSetType extends EucalyptusData {
  NetworkAclIdSetType() {  }
  @HttpParameterMapping (parameter = "NetworkAclId")
  @HttpEmbedded(multiple=true)
  ArrayList<NetworkAclIdSetItemType> item = new ArrayList<NetworkAclIdSetItemType>();
}
class DescribeInternetGatewaysResponseType extends VpcMessage {
  InternetGatewaySetType internetGatewaySet = new InternetGatewaySetType();
  DescribeInternetGatewaysResponseType() {  }
}
class SubnetType extends EucalyptusData implements VpcTagged {
  String subnetId;
  String state;
  String vpcId;
  String cidrBlock;
  Integer availableIpAddressCount;
  String availabilityZone;
  Boolean defaultForAz;
  Boolean mapPublicIpOnLaunch;
  ResourceTagSetType tagSet;

  SubnetType() {  }

  SubnetType(
      final String subnetId,
      final String state,
      final String vpcId,
      final String cidrBlock,
      final Integer availableIpAddressCount,
      final String availabilityZone,
      final Boolean defaultForAz,
      final Boolean mapPublicIpOnLaunch ) {
    this.subnetId = subnetId
    this.state = state
    this.vpcId = vpcId
    this.cidrBlock = cidrBlock
    this.availableIpAddressCount = availableIpAddressCount
    this.availabilityZone = availabilityZone
    this.defaultForAz = defaultForAz
    this.mapPublicIpOnLaunch = mapPublicIpOnLaunch
  }

  static Function<SubnetType, String> id( ) {
    { SubnetType subnet -> subnet.subnetId } as Function<SubnetType, String>
  }

  static Function<SubnetType, String> zone( ) {
    { SubnetType subnet -> subnet.availabilityZone } as Function<SubnetType, String>
  }
}
class DescribeRouteTablesResponseType extends VpcMessage {
  RouteTableSetType routeTableSet = new RouteTableSetType();
  DescribeRouteTablesResponseType() {  }
}
class AssociateRouteTableType extends VpcMessage {
  String routeTableId;
  String subnetId;
  AssociateRouteTableType() {  }
}
class ModifyNetworkInterfaceAttachmentType extends EucalyptusData {
  String attachmentId;
  Boolean deleteOnTermination;
  ModifyNetworkInterfaceAttachmentType() {  }
}
class DisableVgwRoutePropagationResponseType extends VpcMessage {
  DisableVgwRoutePropagationResponseType() {  }
}
class PlacementRequestType extends EucalyptusData {
  String availabilityZone;
  String groupName;
  String tenancy;
  PlacementRequestType() {  }
}
class InternetGatewayAttachmentType extends EucalyptusData {
  String vpcId;
  String state;
  InternetGatewayAttachmentType() {  }
}
class CustomerGatewaySetType extends EucalyptusData {
  CustomerGatewaySetType() {  }
  ArrayList<CustomerGatewayType> item = new ArrayList<CustomerGatewayType>();
}
class DescribeNetworkInterfaceAttributeType extends VpcMessage {
  String networkInterfaceId;
  String attribute

  void hasAttachment( boolean flag ) {
    if ( flag ) attribute = "attachment"
  }

  void hasDescription( boolean flag ) {
    if ( flag ) attribute = "description"
  }

  void hasGroupSet( boolean flag ) {
    if ( flag ) attribute = "groupSet"
  }

  void hasSourceDestCheck( boolean flag ) {
    if ( flag ) attribute = "sourceDestCheck"
  }

  Object getAttrObject( ){ null }
  void setAttrObject( Object value ) { }

  DescribeNetworkInterfaceAttributeType() {  }
}
class VpnGatewaySetType extends EucalyptusData {
  VpnGatewaySetType() {  }
  ArrayList<VpnGatewayType> item = new ArrayList<VpnGatewayType>();
}
class ProductCodesSetType extends EucalyptusData {
  ProductCodesSetType() {  }
  ArrayList<ProductCodesSetItemType> item = new ArrayList<ProductCodesSetItemType>();
}
class EnableVgwRoutePropagationType extends VpcMessage {
  String routeTableId;
  String gatewayId;

  EnableVgwRoutePropagationType() {  }
}
class ReplaceNetworkAclEntryType extends VpcMessage {
  String networkAclId;
  @FieldRange( min = 1l, max = 32766l )
  Integer ruleNumber;
  @FieldRegex( FieldRegexValue.EC2_PROTOCOL )
  String protocol;
  @FieldRegex( FieldRegexValue.EC2_ACL_ACTION )
  String ruleAction;
  Boolean egress = false
  @FieldRegex( FieldRegexValue.CIDR )
  String cidrBlock;
  @HttpParameterMapping( parameter = "Icmp" )
  IcmpTypeCodeType icmpTypeCode;
  PortRangeType portRange;
  ReplaceNetworkAclEntryType() {  }
}
class VpcPeeringConnectionSetType extends EucalyptusData {
  VpcPeeringConnectionSetType() {  }
  ArrayList<VpcPeeringConnectionType> item = new ArrayList<VpcPeeringConnectionType>();
}
class DeleteVpnConnectionRouteType extends VpcMessage {
  String vpnConnectionId;
  String destinationCidrBlock;
  DeleteVpnConnectionRouteType() {  }
}
class DetachNetworkInterfaceType extends VpcMessage {
  String attachmentId;
  Boolean force;
  DetachNetworkInterfaceType() {  }
}
class VpnStaticRouteType extends EucalyptusData {
  String destinationCidrBlock;
  String source;
  String state;
  VpnStaticRouteType() {  }
}
class CreateSubnetResponseType extends VpcMessage {
  SubnetType subnet;
  CreateSubnetResponseType() {  }
}
class VpnConnectionType extends EucalyptusData {
  String vpnConnectionId;
  String state;
  String customerGatewayConfiguration;
  String type;
  String customerGatewayId;
  String vpnGatewayId;
  ResourceTagSetType tagSet;
  VgwTelemetryType vgwTelemetry;
  VpnConnectionOptionsResponseType options;
  VpnStaticRoutesSetType routes;
  VpnConnectionType() {  }
}
class CreateNetworkInterfaceType extends VpcMessage {
  String subnetId;
  @FieldRegex( FieldRegexValue.STRING_255 )
  String description;
  @FieldRegex( FieldRegexValue.IP_ADDRESS )
  String privateIpAddress;
  @HttpEmbedded
  SecurityGroupIdSetType groupSet;
  @HttpEmbedded
  PrivateIpAddressesSetRequestType privateIpAddressesSet;
  Integer secondaryPrivateIpAddressCount;
  CreateNetworkInterfaceType() {  }
}
class AttachInternetGatewayResponseType extends VpcMessage {
  AttachInternetGatewayResponseType() {  }
}
class ModifyVpcAttributeResponseType extends VpcMessage {
  ModifyVpcAttributeResponseType() {  }
}
class DisassociateRouteTableType extends VpcMessage {
  String associationId;
  DisassociateRouteTableType() {  }
}
class RejectVpcPeeringConnectionType extends VpcMessage {
  String vpcPeeringConnectionId;
  RejectVpcPeeringConnectionType() {  }
}
class RouteTableIdSetItemType extends EucalyptusData {
  @HttpValue
  String routeTableId;
  RouteTableIdSetItemType() {  }
}
class AssociateRouteTableResponseType extends VpcMessage {
  String associationId;
  AssociateRouteTableResponseType() {  }
}
class InternetGatewayIdSetItemType extends EucalyptusData {
  @HttpValue
  String internetGatewayId;
  InternetGatewayIdSetItemType() {  }
}
class VpcPeeringConnectionType extends EucalyptusData {
  String vpcPeeringConnectionId;
  VpcPeeringConnectionVpcInfoType requesterVpcInfo;
  VpcPeeringConnectionVpcInfoType accepterVpcInfo;
  VpcPeeringConnectionStateReasonType status;
  Date expirationTime;
  ResourceTagSetType tagSet;
  VpcPeeringConnectionType() {  }
}
class DeleteNetworkAclEntryType extends VpcMessage {
  String networkAclId;
  @FieldRange( min = 1l, max = 32766l )
  Integer ruleNumber;
  Boolean egress = false
  DeleteNetworkAclEntryType() {  }
}
class AttachmentSetType extends EucalyptusData {
  AttachmentSetType() {  }
  ArrayList<AttachmentType> item = new ArrayList<AttachmentType>();
}
class NetworkAclEntrySetType extends EucalyptusData {
  NetworkAclEntrySetType() {  }
  ArrayList<NetworkAclEntryType> item = new ArrayList<NetworkAclEntryType>();
}
class EbsBlockDeviceType extends EucalyptusData {
  String snapshotId;
  Integer volumeSize;
  Boolean deleteOnTermination;
  String volumeType;
  Integer iops;
  Boolean encrypted;
  EbsBlockDeviceType() {  }
}
class DeleteVpnConnectionRouteResponseType extends VpcMessage {
  DeleteVpnConnectionRouteResponseType() {  }
}
class VpnConnectionIdSetType extends EucalyptusData {
  VpnConnectionIdSetType() {  }
  @HttpParameterMapping (parameter = "VpnConnectionId")
  @HttpEmbedded(multiple=true)
  ArrayList<VpnConnectionIdSetItemType> item = new ArrayList<VpnConnectionIdSetItemType>();
}
class UserIdGroupPairSetType extends EucalyptusData {
  UserIdGroupPairSetType() {  }
  ArrayList<UserIdGroupPairType> item = new ArrayList<UserIdGroupPairType>();
}
class CreateVpnGatewayResponseType extends VpcMessage {
  VpnGatewayType vpnGateway;
  CreateVpnGatewayResponseType() {  }
}
class NetworkAclIdSetItemType extends EucalyptusData {
  @HttpValue
  String networkAclId;
  NetworkAclIdSetItemType() {  }
}
class AssociateDhcpOptionsResponseType extends VpcMessage {
  AssociateDhcpOptionsResponseType() {  }
}
class DescribeNetworkAclsType extends VpcMessage {
  @HttpEmbedded
  NetworkAclIdSetType networkAclIdSet;
  @HttpParameterMapping (parameter = "Filter")
  @HttpEmbedded( multiple = true )
  ArrayList<Filter> filterSet = new ArrayList<Filter>();
  DescribeNetworkAclsType() {  }

  Collection<String> networkAclIds( ) {
    networkAclIdSet?.item?.collect{ NetworkAclIdSetItemType item -> item?.networkAclId }?:[]
  }
}
class DeleteCustomerGatewayResponseType extends VpcMessage {
  DeleteCustomerGatewayResponseType() {  }
}
class NetworkInterfaceType extends EucalyptusData implements VpcTagged {
  String networkInterfaceId;
  String subnetId;
  String vpcId;
  String availabilityZone;
  String description;
  String ownerId;
  String requesterId;
  Boolean requesterManaged;
  String status;
  String macAddress;
  String privateIpAddress;
  String privateDnsName;
  Boolean sourceDestCheck;
  String interfaceType // does not appear to be in responses as of 2015-10-01
  GroupSetType groupSet = new GroupSetType();
  NetworkInterfaceAttachmentType attachment;
  NetworkInterfaceAssociationType association;
  ResourceTagSetType tagSet = new ResourceTagSetType( )
  NetworkInterfacePrivateIpAddressesSetType privateIpAddressesSet = new NetworkInterfacePrivateIpAddressesSetType( );
  NetworkInterfaceType() {  }

  NetworkInterfaceType(
      final String networkInterfaceId,
      final String subnetId,
      final String vpcId,
      final String availabilityZone,
      final String description,
      final String ownerId,
      final String requesterId,
      final Boolean requesterManaged,
      final String status,
      final String macAddress,
      final String privateIpAddress,
      final String privateDnsName,
      final Boolean sourceDestCheck,
      final String interfaceType,
      final NetworkInterfaceAssociationType association,
      final NetworkInterfaceAttachmentType attachment,
      final Collection<GroupItemType> securityGroups ) {
    this.networkInterfaceId = networkInterfaceId
    this.subnetId = subnetId
    this.vpcId = vpcId
    this.availabilityZone = availabilityZone
    this.description = description
    this.ownerId = ownerId
    this.requesterId = requesterId
    this.requesterManaged = requesterManaged
    this.status = status
    this.macAddress = macAddress
    this.privateIpAddress = privateIpAddress
    this.privateDnsName = privateDnsName
    this.sourceDestCheck = sourceDestCheck
    this.interfaceType = interfaceType
    this.association = association
    this.attachment = attachment;
    this.privateIpAddressesSet = new NetworkInterfacePrivateIpAddressesSetType(
        item: [
            new NetworkInterfacePrivateIpAddressesSetItemType(
                privateIpAddress: privateIpAddress,
                privateDnsName: privateDnsName,
                primary: true,
                association: association
            )
        ] as ArrayList<NetworkInterfacePrivateIpAddressesSetItemType>
    )
    this.groupSet = new GroupSetType( securityGroups )
  }
  static Function<NetworkInterfaceType, String> id( ) {
    { NetworkInterfaceType networkInterface -> networkInterface.networkInterfaceId } as Function<NetworkInterfaceType, String>
  }
}
class CustomerGatewayIdSetItemType extends EucalyptusData {
  @HttpValue
  String customerGatewayId;
  CustomerGatewayIdSetItemType() {  }
}
class CreateVpcPeeringConnectionType extends VpcMessage {
  String vpcId;
  String peerVpcId;
  String peerOwnerId;
  CreateVpcPeeringConnectionType() {  }
}
class NetworkInterfaceSetType extends EucalyptusData {
  NetworkInterfaceSetType() {  }
  ArrayList<NetworkInterfaceType> item = new ArrayList<NetworkInterfaceType>();
}
class ReplaceRouteResponseType extends VpcMessage {
  ReplaceRouteResponseType() {  }
}
class VpcPeeringConnectionVpcInfoType extends EucalyptusData {
  String ownerId;
  String vpcId;
  String cidrBlock;
  VpcPeeringConnectionVpcInfoType() {  }
}
class DescribeNetworkInterfacesType extends VpcMessage {
  @HttpEmbedded
  NetworkInterfaceIdSetType networkInterfaceIdSet;
  @HttpParameterMapping (parameter = "Filter")
  @HttpEmbedded( multiple = true )
  ArrayList<Filter> filterSet = new ArrayList<Filter>();
  DescribeNetworkInterfacesType() {  }

  Collection<String> networkInterfaceIds( ) {
    networkInterfaceIdSet?.item?.collect{ NetworkInterfaceIdSetItemType item -> item?.networkInterfaceId }?:[]
  }
}
class ReplaceRouteTableAssociationResponseType extends VpcMessage {
  String newAssociationId;
  ReplaceRouteTableAssociationResponseType() {  }
}
class DeleteSubnetType extends VpcMessage {
  String subnetId;
  DeleteSubnetType() {  }
}
class ModifyNetworkInterfaceAttributeResponseType extends VpcMessage {
  ModifyNetworkInterfaceAttributeResponseType() {  }
}
class GroupSetType extends EucalyptusData {
  GroupSetType( ) {  }
  GroupSetType( Collection<GroupItemType> item ) {
    this.item = Lists.newArrayList( item )
  }
  ArrayList<GroupItemType> item = new ArrayList<GroupItemType>();
}
class VgwTelemetryType extends EucalyptusData {
  VgwTelemetryType() {  }
  ArrayList<VpnTunnelTelemetryType> item = new ArrayList<VpnTunnelTelemetryType>();
}
class AttachInternetGatewayType extends VpcMessage {
  String internetGatewayId;
  String vpcId;
  AttachInternetGatewayType() {  }
}
class SubnetIdSetItemType extends EucalyptusData {
  @HttpValue
  String subnetId;
  SubnetIdSetItemType() {  }
}
class CustomerGatewayType extends EucalyptusData {
  String customerGatewayId;
  String state;
  String type;
  String ipAddress;
  Integer bgpAsn;
  ResourceTagSetType tagSet;
  CustomerGatewayType() {  }
}
class DescribeVpnConnectionsType extends VpcMessage {
  @HttpEmbedded
  VpnConnectionIdSetType vpnConnectionSet;
  @HttpParameterMapping (parameter = "Filter")
  @HttpEmbedded( multiple = true )
  ArrayList<Filter> filterSet = new ArrayList<Filter>();
  DescribeVpnConnectionsType() {  }
}
class DetachVpnGatewayResponseType extends VpcMessage {
  DetachVpnGatewayResponseType() {  }
}
class VpcSetType extends EucalyptusData {
  VpcSetType() {  }
  ArrayList<VpcType> item = new ArrayList<VpcType>();
}
class VpnGatewayType extends EucalyptusData {
  String vpnGatewayId;
  String state;
  String type;
  String availabilityZone;
  AttachmentSetType attachments;
  ResourceTagSetType tagSet;
  VpnGatewayType() {  }
}
class CreateNetworkInterfaceResponseType extends VpcMessage {
  NetworkInterfaceType networkInterface;
  CreateNetworkInterfaceResponseType() {  }
}
class CreateRouteTableResponseType extends VpcMessage {
  RouteTableType routeTable;
  CreateRouteTableResponseType() {  }
}
class CreateRouteResponseType extends VpcMessage {
  CreateRouteResponseType() {  }
}
class CreateVpnGatewayType extends VpcMessage {
  String type;
  String availabilityZone;
  CreateVpnGatewayType() {  }
}
class DeleteVpcPeeringConnectionResponseType extends VpcMessage {
  DeleteVpcPeeringConnectionResponseType() {  }
}
class DeleteNetworkAclResponseType extends VpcMessage {
  DeleteNetworkAclResponseType() {  }
}
class DescribeSubnetsType extends VpcMessage {
  @HttpEmbedded
  SubnetIdSetType subnetSet;
  @HttpParameterMapping (parameter = "Filter")
  @HttpEmbedded( multiple = true )
  ArrayList<Filter> filterSet = new ArrayList<Filter>();
  DescribeSubnetsType() {  }

  Collection<String> subnetIds( ) {
    subnetSet?.item?.collect{ SubnetIdSetItemType item -> item?.subnetId }?:[]
  }
}
class AccountAttributeSetType extends EucalyptusData {
  AccountAttributeSetType() {  }
  ArrayList<AccountAttributeSetItemType> item = new ArrayList<AccountAttributeSetItemType>();
}
class DeleteSubnetResponseType extends VpcMessage {
  DeleteSubnetResponseType() {  }
}
class NetworkInterfaceIdSetType extends EucalyptusData {
  NetworkInterfaceIdSetType() {  }
  @HttpParameterMapping (parameter = "NetworkInterfaceId")
  @HttpEmbedded(multiple=true)
  ArrayList<NetworkInterfaceIdSetItemType> item = new ArrayList<NetworkInterfaceIdSetItemType>();
}
class AccountAttributeNameSetType extends EucalyptusData {
  AccountAttributeNameSetType() {  }
  @HttpParameterMapping( parameter = "AttributeName" )
  @HttpEmbedded(multiple=true)
  ArrayList<AccountAttributeNameSetItemType> item = new ArrayList<AccountAttributeNameSetItemType>();
}
class RejectVpcPeeringConnectionResponseType extends VpcMessage {
  RejectVpcPeeringConnectionResponseType() {  }
}
class CreateNetworkAclEntryResponseType extends VpcMessage {
  CreateNetworkAclEntryResponseType() {  }
}
class NetworkAclEntryType extends EucalyptusData {
  Integer ruleNumber;
  String protocol;
  String ruleAction;
  Boolean egress;
  String cidrBlock;
  IcmpTypeCodeType icmpTypeCode;
  PortRangeType portRange;
  NetworkAclEntryType() {  }

  NetworkAclEntryType(
      final Integer ruleNumber,
      final String protocol,
      final String ruleAction,
      final Boolean egress,
      final String cidrBlock,
      final Integer icmpCode,
      final Integer icmpType,
      final Integer portRangeFrom,
      final Integer portRangeTo ) {
    this.ruleNumber = ruleNumber
    this.protocol = protocol
    this.ruleAction = ruleAction
    this.egress = egress
    this.cidrBlock = cidrBlock
    if ( icmpCode ) {
      this.icmpTypeCode = new IcmpTypeCodeType( code: icmpCode, type: icmpType )
    }
    if ( portRangeFrom ) {
      this.portRange = new PortRangeType( from: portRangeFrom, to: portRangeTo )
    }
  }
}
class VpnConnectionOptionsResponseType extends EucalyptusData {
  Boolean staticRoutesOnly;
  VpnConnectionOptionsResponseType() {  }
}
class VpcPeeringConnectionStateReasonType extends EucalyptusData {
  String code;
  String message;
  VpcPeeringConnectionStateReasonType() {  }
}
class ResourceTagSetItemType extends EucalyptusData {
  String key;
  String value;
  ResourceTagSetItemType() {  }

  ResourceTagSetItemType(final String key, final String value) {
    this.key = key
    this.value = value
  }
}
class AcceptVpcPeeringConnectionType extends VpcMessage {
  String vpcPeeringConnectionId;
  AcceptVpcPeeringConnectionType() {  }
}
class DeleteRouteTableResponseType extends VpcMessage {
  DeleteRouteTableResponseType() {  }
}
class CreateVpnConnectionType extends VpcMessage {
  String type;
  String customerGatewayId;
  String vpnGatewayId;
  VpnConnectionOptionsRequestType options;
  CreateVpnConnectionType() {  }
}
class CreateVpcResponseType extends VpcMessage {
  VpcType vpc;
  CreateVpcResponseType() {  }
}
class InternetGatewayIdSetType extends EucalyptusData {
  InternetGatewayIdSetType() {  }
  @HttpParameterMapping (parameter = "InternetGatewayId")
  @HttpEmbedded(multiple=true)
  ArrayList<InternetGatewayIdSetItemType> item = new ArrayList<InternetGatewayIdSetItemType>();
}
class DhcpConfigurationItemType extends EucalyptusData {
  String key;
  @HttpEmbedded
  DhcpValueSetType valueSet;
  DhcpConfigurationItemType() {  }
  DhcpConfigurationItemType( String key, List<String> values ) {
    this.key = key
    this.valueSet = new DhcpValueSetType( item: Lists.newArrayList( values.collect{ String value -> new DhcpValueType( value: value ) } ) )
  }
  List<String> values( ) {
    valueSet?.item?.collect{ DhcpValueType valueType -> valueType.value }?:[]
  }
}
class PropagatingVgwType extends EucalyptusData {
  String gatewayId;
  PropagatingVgwType() {  }
}
class InternetGatewaySetType extends EucalyptusData {
  InternetGatewaySetType() {  }
  ArrayList<InternetGatewayType> item = new ArrayList<InternetGatewayType>();
}
class EnableVgwRoutePropagationResponseType extends VpcMessage {
  EnableVgwRoutePropagationResponseType() {  }
}
class VpcIdSetType extends EucalyptusData {
  VpcIdSetType() {  }
  @HttpParameterMapping (parameter = "VpcId")
  @HttpEmbedded( multiple = true )
  ArrayList<VpcIdSetItemType> item = new ArrayList<VpcIdSetItemType>();
}
class ReplaceRouteTableAssociationType extends VpcMessage {
  String associationId;
  String routeTableId;
  ReplaceRouteTableAssociationType() {  }
}
class DescribeDhcpOptionsType extends VpcMessage {
  @HttpEmbedded
  DhcpOptionsIdSetType dhcpOptionsSet;
  @HttpParameterMapping (parameter = "Filter")
  @HttpEmbedded( multiple = true )
  ArrayList<Filter> filterSet = new ArrayList<Filter>();
  DescribeDhcpOptionsType() {  }

  Collection<String> dhcpOptionsIds( ) {
    dhcpOptionsSet?.item?.collect{ DhcpOptionsIdSetItemType item -> item?.dhcpOptionsId }?:[]
  }
}
class NetworkAclSetType extends EucalyptusData {
  NetworkAclSetType() {  }
  ArrayList<NetworkAclType> item = new ArrayList<NetworkAclType>();
}
class VpnGatewayIdSetItemType extends EucalyptusData {
  @HttpValue
  String vpnGatewayId;
  VpnGatewayIdSetItemType() {  }
}
class ResourceTagSetType extends EucalyptusData {
  ResourceTagSetType() {  }
  ArrayList<ResourceTagSetItemType> item = new ArrayList<ResourceTagSetItemType>();
}
class CustomerGatewayIdSetType extends EucalyptusData {
  CustomerGatewayIdSetType() {  }
  @HttpParameterMapping (parameter = "CustomerGatewayId" )
  @HttpEmbedded(multiple=true)
  ArrayList<CustomerGatewayIdSetItemType> item = new ArrayList<CustomerGatewayIdSetItemType>();
}
class AssociateDhcpOptionsType extends VpcMessage {
  String dhcpOptionsId;
  String vpcId;
  AssociateDhcpOptionsType() {  }
}
class DescribeInternetGatewaysType extends VpcMessage {
  @HttpEmbedded
  InternetGatewayIdSetType internetGatewayIdSet;
  @HttpParameterMapping (parameter = "Filter")
  @HttpEmbedded( multiple = true )
  ArrayList<Filter> filterSet = new ArrayList<Filter>();
  DescribeInternetGatewaysType() {  }

  Collection<String> internetGatewayIds( ) {
    internetGatewayIdSet?.item?.collect{ InternetGatewayIdSetItemType item -> item?.internetGatewayId }?:[]
  }
}
class ModifyNetworkInterfaceAttributeType extends VpcMessage {
  String networkInterfaceId;
  NullableAttributeValueType description;
  AttributeBooleanValueType sourceDestCheck;
  @HttpEmbedded
  SecurityGroupIdSetType groupSet;
  ModifyNetworkInterfaceAttachmentType attachment;
  ModifyNetworkInterfaceAttributeType() {  }
}
class DhcpOptionsIdSetItemType extends EucalyptusData {
  @HttpValue
  String dhcpOptionsId;
  DhcpOptionsIdSetItemType() {  }
}
class InternetGatewayType extends EucalyptusData implements VpcTagged {
  String internetGatewayId;
  InternetGatewayAttachmentSetType attachmentSet = new InternetGatewayAttachmentSetType( );
  ResourceTagSetType tagSet;
  InternetGatewayType( ) {  }
  InternetGatewayType( String internetGatewayId, String attachedVpcId ) {
    this.internetGatewayId = internetGatewayId
    if ( attachedVpcId ) {
      attachmentSet.item.add(
          new InternetGatewayAttachmentType( vpcId: attachedVpcId, state: 'available')
      )
    }
  }
  static Function<InternetGatewayType, String> id( ) {
    { InternetGatewayType internetGateway -> internetGateway.internetGatewayId } as Function<InternetGatewayType, String>
  }
}
class VpnConnectionSetType extends EucalyptusData {
  VpnConnectionSetType() {  }
  ArrayList<VpnConnectionType> item = new ArrayList<VpnConnectionType>();
}
class CreateDhcpOptionsType extends VpcMessage {
  @HttpEmbedded
  DhcpConfigurationItemSetType dhcpConfigurationSet;
  CreateDhcpOptionsType() {  }
}
class AttributeBooleanValueType extends EucalyptusData {
  Boolean value;
  AttributeBooleanValueType() {  }
}
class AttributeBooleanFlatValueType extends EucalyptusData {
  @HttpValue
  Boolean value;
  AttributeBooleanFlatValueType() {  }
}
class RouteTableSetType extends EucalyptusData {
  RouteTableSetType() {  }
  ArrayList<RouteTableType> item = new ArrayList<RouteTableType>();
}
class DeleteDhcpOptionsResponseType extends VpcMessage {
  DeleteDhcpOptionsResponseType() {  }
}
class DhcpValueSetType extends EucalyptusData {
  DhcpValueSetType() {  }
  @HttpEmbedded( multiple = true )
  @HttpParameterMapping( parameter = "Value" )
  ArrayList<DhcpValueType> item = new ArrayList<DhcpValueType>();
}
class RouteTableAssociationType extends EucalyptusData {
  String routeTableAssociationId;
  String routeTableId;
  String subnetId;
  Boolean main;
  RouteTableAssociationType() {  }

  RouteTableAssociationType(
      final String routeTableAssociationId,
      final String routeTableId,
      final String subnetId,
      final Boolean main ) {
    this.routeTableAssociationId = routeTableAssociationId
    this.routeTableId = routeTableId
    this.subnetId = subnetId
    this.main = main
  }
}
class DeleteRouteTableType extends VpcMessage {
  String routeTableId;
  DeleteRouteTableType() {  }
}
class ReservationSetType extends EucalyptusData {
  ReservationSetType() {  }
  ArrayList<ReservationInfoType> item = new ArrayList<ReservationInfoType>();
}
class DescribeVpcPeeringConnectionsType extends VpcMessage {
  @HttpEmbedded
  VpcPeeringConnectionIdSetType vpcPeeringConnectionIdSet;
  @HttpParameterMapping (parameter = "Filter")
  @HttpEmbedded( multiple = true )
  ArrayList<Filter> filterSet = new ArrayList<Filter>();
  DescribeVpcPeeringConnectionsType() {  }
}
class VpnStaticRoutesSetType extends EucalyptusData {
  VpnStaticRoutesSetType() {  }
  ArrayList<VpnStaticRouteType> item = new ArrayList<VpnStaticRouteType>();
}
class DeleteVpcPeeringConnectionType extends VpcMessage {
  String vpcPeeringConnectionId;
  DeleteVpcPeeringConnectionType() {  }
}
class AccountAttributeValueSetType extends EucalyptusData {
  AccountAttributeValueSetType() {  }
  ArrayList<AccountAttributeValueSetItemType> item = new ArrayList<AccountAttributeValueSetItemType>();
}
class DhcpOptionsType extends EucalyptusData implements VpcTagged {
  String dhcpOptionsId;
  DhcpConfigurationItemSetType dhcpConfigurationSet;
  ResourceTagSetType tagSet;
  DhcpOptionsType() {  }
  DhcpOptionsType( String dhcpOptionsId, Collection<DhcpConfigurationItemType> configuration ) {
    this.dhcpOptionsId = dhcpOptionsId
    this.dhcpConfigurationSet = new DhcpConfigurationItemSetType( configuration )
  }

  static Function<DhcpOptionsType, String> id( ) {
    { DhcpOptionsType dhcpOptionsType -> dhcpOptionsType.dhcpOptionsId } as Function<DhcpOptionsType, String>
  }
}
class ModifyVpcAttributeType extends VpcMessage {
  String vpcId;
  AttributeBooleanValueType enableDnsSupport;
  AttributeBooleanValueType enableDnsHostnames;
  ModifyVpcAttributeType() {  }
}
class AttachVpnGatewayType extends VpcMessage {
  String vpnGatewayId;
  String vpcId;
  AttachVpnGatewayType() {  }
}
class AttributeValueType extends EucalyptusData {
  String value;
  AttributeValueType() {  }
}
class NetworkAclType extends EucalyptusData implements VpcTagged {
  String networkAclId;
  String vpcId;
  Boolean _default;
  NetworkAclEntrySetType entrySet;
  NetworkAclAssociationSetType associationSet;
  ResourceTagSetType tagSet;
  NetworkAclType() {  }

  NetworkAclType( final String networkAclId,
                  final String vpcId,
                  final Boolean _default,
                  final Collection<NetworkAclEntryType> entries,
                  final Collection<NetworkAclAssociationType> associations ) {
    this.networkAclId = networkAclId
    this.vpcId = vpcId
    this._default = _default
    this.entrySet = new NetworkAclEntrySetType( item: Lists.newArrayList( entries ) )
    this.associationSet = new NetworkAclAssociationSetType( item: Lists.newArrayList( associations ) )
  }

  static Function<NetworkAclType, String> id( ) {
    { NetworkAclType networkAcl -> networkAcl.networkAclId } as Function<NetworkAclType, String>
  }
}
class CreateCustomerGatewayType extends VpcMessage {
  String type;
  String ipAddress;
  Integer bgpAsn;
  CreateCustomerGatewayType() {  }
}
class ProductCodesSetItemType extends EucalyptusData {
  String productCode;
  String type;
  ProductCodesSetItemType() {  }
}
class DescribeVpcPeeringConnectionsResponseType extends VpcMessage {
  VpcPeeringConnectionSetType vpcPeeringConnectionSet = new VpcPeeringConnectionSetType();
  DescribeVpcPeeringConnectionsResponseType() {  }
}
class NetworkInterfaceIdSetItemType extends EucalyptusData {
  @HttpValue
  String networkInterfaceId;
  NetworkInterfaceIdSetItemType() {  }
}
class ReplaceNetworkAclAssociationResponseType extends VpcMessage {
  String newAssociationId;
  ReplaceNetworkAclAssociationResponseType() {  }
}
class AssignPrivateIpAddressesSetItemRequestType extends EucalyptusData {
  @HttpValue
  String privateIpAddress;
  AssignPrivateIpAddressesSetItemRequestType() {  }
}
class NullableAttributeBooleanValueType extends EucalyptusData {
  Boolean value;
  NullableAttributeBooleanValueType() {  }
}
class UnassignPrivateIpAddressesType extends VpcMessage {
  String networkInterfaceId;
  @HttpEmbedded
  AssignPrivateIpAddressesSetRequestType privateIpAddressesSet;
  UnassignPrivateIpAddressesType() {  }
}
class CreateRouteTableType extends VpcMessage {
  String vpcId;
  CreateRouteTableType() {  }
}
class SubnetSetType extends EucalyptusData {
  SubnetSetType() {  }
  ArrayList<SubnetType> item = new ArrayList<SubnetType>();
}
class NetworkInterfacePrivateIpAddressesSetType extends EucalyptusData {
  NetworkInterfacePrivateIpAddressesSetType() {  }
  ArrayList<NetworkInterfacePrivateIpAddressesSetItemType> item = new ArrayList<NetworkInterfacePrivateIpAddressesSetItemType>();
}
class DisableVgwRoutePropagationType extends VpcMessage {
  String routeTableId;
  String gatewayId;

  DisableVgwRoutePropagationType() {  }
}
class DeleteNetworkAclType extends VpcMessage {
  String networkAclId;
  DeleteNetworkAclType() {  }
}
class VpnTunnelTelemetryType extends EucalyptusData {
  String outsideIpAddress;
  String status;
  Date lastStatusChange;
  String statusMessage;
  Integer acceptedRouteCount;
  VpnTunnelTelemetryType() {  }
}
class DeleteNetworkInterfaceResponseType extends VpcMessage {
  DeleteNetworkInterfaceResponseType() {  }
}
class DescribeNetworkInterfacesResponseType extends VpcMessage {
  NetworkInterfaceSetType networkInterfaceSet = new NetworkInterfaceSetType();
  DescribeNetworkInterfacesResponseType() {  }
}
class DeleteRouteResponseType extends VpcMessage {
  DeleteRouteResponseType() {  }
}
class NetworkAclAssociationSetType extends EucalyptusData {
  NetworkAclAssociationSetType() {  }
  ArrayList<NetworkAclAssociationType> item = new ArrayList<NetworkAclAssociationType>();
}
class DeleteVpnConnectionResponseType extends VpcMessage {
  DeleteVpnConnectionResponseType() {  }
}
class CreateNetworkAclResponseType extends VpcMessage {
  NetworkAclType networkAcl;
  CreateNetworkAclResponseType() {  }
}
class IcmpTypeCodeType extends EucalyptusData {
  @FieldRange( min = -1l, max = 255l )
  Integer code;
  @FieldRange( min = -1l, max = 255l )
  Integer type;
  IcmpTypeCodeType() {  }
}
class VpcPeeringConnectionIdSetItemType extends EucalyptusData {
  @HttpValue
  String vpcPeeringConnectionId;
  VpcPeeringConnectionIdSetItemType() {  }
}
class DescribeAccountAttributesResponseType extends VpcMessage {
  AccountAttributeSetType accountAttributeSet = new AccountAttributeSetType();
  DescribeAccountAttributesResponseType() {  }
}
class SubnetIdSetType extends EucalyptusData {
  SubnetIdSetType() {  }
  @HttpParameterMapping (parameter = "SubnetId")
  @HttpEmbedded(multiple=true)
  ArrayList<SubnetIdSetItemType> item = new ArrayList<SubnetIdSetItemType>();
}
class CreateNetworkAclType extends VpcMessage {
  String vpcId;
  CreateNetworkAclType() {  }
}
class RouteTableType extends EucalyptusData implements VpcTagged {
  String routeTableId;
  String vpcId;
  RouteSetType routeSet;
  RouteTableAssociationSetType associationSet;
  PropagatingVgwSetType propagatingVgwSet;
  ResourceTagSetType tagSet;
  RouteTableType() {  }

  RouteTableType( final String routeTableId,
                  final String vpcId,
                  final Collection<RouteType> routes,
                  final Collection<RouteTableAssociationType> associations ) {
    this.routeTableId = routeTableId
    this.vpcId = vpcId
    this.routeSet = new RouteSetType( item: Lists.newArrayList( routes ) )
    this.associationSet = new RouteTableAssociationSetType( item: Lists.newArrayList( associations ) )
    this.propagatingVgwSet = new PropagatingVgwSetType( )
  }

  static Function<RouteTableType, String> id( ) {
    { RouteTableType routeTable -> routeTable.routeTableId } as Function<RouteTableType, String>
  }

}
class AccountAttributeNameSetItemType extends EucalyptusData {
  @HttpValue
  String attributeName;
  AccountAttributeNameSetItemType() {  }
}
class NatGatewayAddressSetItemType extends EucalyptusData {
  String networkInterfaceId
  String privateIp
  String allocationId
  String publicIp
  NatGatewayAddressSetItemType( ) { }
  NatGatewayAddressSetItemType( final String networkInterfaceId,
                                final String privateIp,
                                final String allocationId,
                                final String publicIp ) {
    this.networkInterfaceId = networkInterfaceId
    this.privateIp = privateIp
    this.allocationId = allocationId
    this.publicIp = publicIp
  }
}
class NatGatewayAddressSetType extends EucalyptusData {
  ArrayList<NatGatewayAddressSetItemType> item = Lists.newArrayList( )
}
class NatGatewayType extends EucalyptusData {
  Date createTime
  Date deleteTime
  String failureCode
  String failureMessage
  String natGatewayId
  String vpcId
  String subnetId
  String state
  NatGatewayAddressSetType natGatewayAddressSet = new NatGatewayAddressSetType( );
  NatGatewayType() {  }

  NatGatewayType( final String natGatewayId,
                  final Date createTime,
                  final Date deleteTime,
                  final String failureCode,
                  final String failureMessage,
                  final String vpcId,
                  final String subnetId,
                  final String state,
                  final NatGatewayAddressSetItemType address ) {
    this.natGatewayId = natGatewayId
    this.createTime = createTime
    this.deleteTime = deleteTime
    this.failureCode = failureCode
    this.failureMessage = failureMessage
    this.vpcId = vpcId
    this.subnetId = subnetId
    this.state = state
    if ( address ) {
      natGatewayAddressSet.item.add( address )
    }
  }
}

class NatGatewaySetType extends EucalyptusData {
  ArrayList<NatGatewayType> item = new ArrayList<NatGatewayType>()
}
class DescribeNatGatewaysType extends VpcMessage {
  @HttpParameterMapping (parameter = "Filter")
  @HttpEmbedded( multiple = true )
  ArrayList<Filter> filterSet = new ArrayList<Filter>()
  ArrayList<String> natGatewayId = new ArrayList<String>()
  Integer maxResults
  String nextToken

  Collection<String> natGatewayIds( ) {
    natGatewayId
  }
}
class DescribeNatGatewaysResponseType extends VpcMessage {
  NatGatewaySetType natGatewaySet = new NatGatewaySetType()
}
class DescribeNetworkAclsResponseType extends VpcMessage {
  NetworkAclSetType networkAclSet = new NetworkAclSetType();
  DescribeNetworkAclsResponseType() {  }
}
class IpPermissionSetType extends EucalyptusData {
  IpPermissionSetType() {  }
  ArrayList<IpPermissionType> item = new ArrayList<IpPermissionType>();
}
class ReplaceNetworkAclEntryResponseType extends VpcMessage {
  ReplaceNetworkAclEntryResponseType() {  }
}
class AccountAttributeValueSetItemType extends EucalyptusData {
  String attributeValue;
  AccountAttributeValueSetItemType() {  }
}
class FilterSetType extends EucalyptusData {
  FilterSetType() {  }
  ArrayList<FilterType> item = new ArrayList<FilterType>();
}
class AttachNetworkInterfaceType extends VpcMessage {
  String networkInterfaceId;
  String instanceId;
  @Nonnull
  @FieldRange( min = 1l, max = 255l )
  Integer deviceIndex;
  AttachNetworkInterfaceType() {  }
}
class DescribeVpcsResponseType extends VpcMessage {
  VpcSetType vpcSet = new VpcSetType();
  DescribeVpcsResponseType() {  }
}
class PropagatingVgwSetType extends EucalyptusData {
  PropagatingVgwSetType() {  }
  ArrayList<PropagatingVgwType> item = new ArrayList<PropagatingVgwType>();
}
class SecurityGroupSetType extends EucalyptusData {
  SecurityGroupSetType() {  }
  ArrayList<SecurityGroupItemType> item = new ArrayList<SecurityGroupItemType>();
}
class AttachVpnGatewayResponseType extends VpcMessage {
  AttachmentType attachment;
  AttachVpnGatewayResponseType() {  }
}
class CreateInternetGatewayType extends VpcMessage {
  CreateInternetGatewayType() {  }
}
class CreateNatGatewayType extends VpcMessage {
  String allocationId
  String clientToken
  String subnetId
}
class CreateNatGatewayResponseType extends VpcMessage {
  String clientToken
  NatGatewayType natGateway
}
class DescribeCustomerGatewaysResponseType extends VpcMessage {
  CustomerGatewaySetType customerGatewaySet = new CustomerGatewaySetType();
  DescribeCustomerGatewaysResponseType() {  }
}
class AcceptVpcPeeringConnectionResponseType extends VpcMessage {
  VpcPeeringConnectionType vpcPeeringConnection;
  AcceptVpcPeeringConnectionResponseType() {  }
}
class IpRangeItemType extends EucalyptusData {
  String cidrIp;
  IpRangeItemType() {  }
}
class AssignPrivateIpAddressesType extends VpcMessage {
  String networkInterfaceId;
  @HttpEmbedded
  AssignPrivateIpAddressesSetRequestType privateIpAddressesSet;
  Integer secondaryPrivateIpAddressCount;
  Boolean allowReassignment;
  AssignPrivateIpAddressesType() {  }
}
class DisassociateRouteTableResponseType extends VpcMessage {
  DisassociateRouteTableResponseType() {  }
}
class DeleteVpnGatewayResponseType extends VpcMessage {
  DeleteVpnGatewayResponseType() {  }
}
class DeleteInternetGatewayResponseType extends VpcMessage {
  DeleteInternetGatewayResponseType() {  }
}
class VpcPeeringConnectionIdSetType extends EucalyptusData {
  VpcPeeringConnectionIdSetType() {  }
  @HttpParameterMapping (parameter = "VpcPeeringConnectionId")
  @HttpEmbedded(multiple=true)
  ArrayList<VpcPeeringConnectionIdSetItemType> item = new ArrayList<VpcPeeringConnectionIdSetItemType>();
}
class AccountAttributeSetItemType extends EucalyptusData {
  String attributeName;
  AccountAttributeValueSetType attributeValueSet;
  AccountAttributeSetItemType() {  }
  AccountAttributeSetItemType( String name, List<String> values ) {
    attributeName = name;
    attributeValueSet = new AccountAttributeValueSetType(
        item: values.collect{ String value -> new AccountAttributeValueSetItemType( attributeValue: value ) } as ArrayList<AccountAttributeValueSetItemType>
    )
  }
}
class CreateVpcPeeringConnectionResponseType extends VpcMessage {
  VpcPeeringConnectionType vpcPeeringConnection;
  CreateVpcPeeringConnectionResponseType() {  }
}
class DhcpValueType extends EucalyptusData {
  @HttpValue
  @FieldRegex( FieldRegexValue.STRING_255 )
  String value;
  DhcpValueType() {  }
}
class DescribeRouteTablesType extends VpcMessage {
  @HttpEmbedded
  RouteTableIdSetType routeTableIdSet;
  @HttpParameterMapping (parameter = "Filter")
  @HttpEmbedded( multiple = true )
  ArrayList<Filter> filterSet = new ArrayList<Filter>();
  DescribeRouteTablesType() {  }

  Collection<String> routeTableIds( ) {
    routeTableIdSet?.item?.collect{ RouteTableIdSetItemType item -> item?.routeTableId }?:[]
  }
}
class VpnGatewayIdSetType extends EucalyptusData {
  VpnGatewayIdSetType() {  }
  @HttpParameterMapping (parameter = "VpnGatewayId")
  @HttpEmbedded(multiple=true)
  ArrayList<VpnGatewayIdSetItemType> item = new ArrayList<VpnGatewayIdSetItemType>();
}
class IpRangeSetType extends EucalyptusData {
  IpRangeSetType() {  }
  ArrayList<IpRangeItemType> item = new ArrayList<IpRangeItemType>();
}
class ReplaceRouteType extends VpcMessage {
  String routeTableId;
  @FieldRegex( FieldRegexValue.CIDR )
  String destinationCidrBlock;
  String gatewayId;
  String instanceId;
  String natGatewayId
  String networkInterfaceId;
  String vpcPeeringConnectionId;
  ReplaceRouteType() {  }
}
class AttachmentType extends EucalyptusData {
  String vpcId;
  String state;
  AttachmentType() {  }
}
class ResetNetworkInterfaceAttributeType extends VpcMessage {
  String networkInterfaceId;
  String attribute

  void hasSourceDestCheck( boolean flag ) {
    if ( flag ) attribute = "sourceDestCheck"
  }

  Object getAttrObject( ){ null }
  void setAttrObject( Object value ) { }

  ResetNetworkInterfaceAttributeType() {  }
}
class VpcIdSetItemType extends EucalyptusData {
  @HttpValue
  String vpcId;
  VpcIdSetItemType() {  }
}
class CreateSubnetType extends VpcMessage {
  String vpcId;
  @FieldRegex( FieldRegexValue.CIDR )
  String cidrBlock;
  @FieldRegex( FieldRegexValue.STRING_255 )
  String availabilityZone;
  CreateSubnetType() {  }
}
class CreateRouteType extends VpcMessage {
  String routeTableId;
  @FieldRegex( FieldRegexValue.CIDR )
  String destinationCidrBlock;
  String gatewayId;
  String instanceId;
  String natGatewayId
  String networkInterfaceId;
  String vpcPeeringConnectionId;
  CreateRouteType() {  }
}
class DescribeVpcsType extends VpcMessage {
  @HttpEmbedded
  VpcIdSetType vpcSet;
  @HttpParameterMapping (parameter = "Filter")
  @HttpEmbedded( multiple = true )
  ArrayList<Filter> filterSet = new ArrayList<Filter>();
  DescribeVpcsType() {  }

  Collection<String> vpcIds( ) {
    vpcSet?.item?.collect{ VpcIdSetItemType item -> item?.vpcId }?:[]
  }
}
class DeleteVpnGatewayType extends VpcMessage {
  String vpnGatewayId;
  DeleteVpnGatewayType() {  }
}
class ReplaceNetworkAclAssociationType extends VpcMessage {
  String associationId;
  String networkAclId;
  ReplaceNetworkAclAssociationType() {  }
}
class CreateDhcpOptionsResponseType extends VpcMessage {
  DhcpOptionsType dhcpOptions;
  CreateDhcpOptionsResponseType() {  }
}
class DescribeVpcAttributeResponseType extends VpcMessage {
  String vpcId;
  AttributeBooleanValueType enableDnsSupport;
  AttributeBooleanValueType enableDnsHostnames;
  DescribeVpcAttributeResponseType() {  }
}
class CreateNetworkAclEntryType extends VpcMessage {
  String networkAclId;
  @FieldRange( min = 1l, max = 32766l )
  Integer ruleNumber;
  @FieldRegex( FieldRegexValue.EC2_PROTOCOL )
  String protocol;
  @FieldRegex( FieldRegexValue.EC2_ACL_ACTION )
  String ruleAction;
  Boolean egress = false
  @FieldRegex( FieldRegexValue.CIDR )
  String cidrBlock;
  @HttpParameterMapping( parameter = "Icmp" )
  IcmpTypeCodeType icmpTypeCode;
  PortRangeType portRange;

  CreateNetworkAclEntryType() {  }

  @Override
  Map<String, String> validate() {
    Map<String,String> errors = super.validate( )

    if ( [ 'tcp', '6', 'udp', '17' ].contains( protocol ) &&
        portRange?.from != null &&
        portRange?.to != null &&
        portRange?.from > portRange?.to ) {
      errors.put( 'PortRange', "Invalid TCP/UDP port range (${portRange?.from}:${portRange?.to})" as String )
    }

    errors
  }
}
class DescribeVpnConnectionsResponseType extends VpcMessage {
  VpnConnectionSetType vpnConnectionSet = new VpnConnectionSetType();
  DescribeVpnConnectionsResponseType() {  }
}
class DeleteNetworkInterfaceType extends VpcMessage {
  String networkInterfaceId;
  DeleteNetworkInterfaceType() {  }
}
class NullableAttributeValueType extends EucalyptusData {
  @FieldRegex( FieldRegexValue.STRING_255 )
  String value;
  NullableAttributeValueType() {  }
}
class CreateVpcType extends VpcMessage {
  @FieldRegex( FieldRegexValue.EC2_ACCOUNT_OR_CIDR )
  String cidrBlock;
  @FieldRegex( FieldRegexValue.STRING_255 )
  String instanceTenancy;
  CreateVpcType() {  }
}
class DeleteCustomerGatewayType extends VpcMessage {
  String customerGatewayId;
  DeleteCustomerGatewayType() {  }
}
class DeleteVpcResponseType extends VpcMessage {
  DeleteVpcResponseType() {  }
}
class DeleteDhcpOptionsType extends VpcMessage {
  String dhcpOptionsId;
  DeleteDhcpOptionsType() {  }
}
class AssignPrivateIpAddressesResponseType extends VpcMessage {
  AssignPrivateIpAddressesResponseType() {  }
}
class ValueType extends EucalyptusData {
  String value;
  ValueType() {  }
}
class NetworkInterfaceAssociationType extends EucalyptusData {
  String publicIp;
  String publicDnsName;
  String ipOwnerId;
  String allocationId;
  String associationId;
  NetworkInterfaceAssociationType( ) { }

  NetworkInterfaceAssociationType(
      final String publicIp,
      final String publicDnsName,
      final String ipOwnerId,
      final String allocationId,
      final String associationId
  ) {
    this.publicIp = publicIp
    this.publicDnsName = publicDnsName
    this.ipOwnerId = ipOwnerId
    this.allocationId = allocationId
    this.associationId = associationId
  }
}
class CreateCustomerGatewayResponseType extends VpcMessage {
  CustomerGatewayType customerGateway;
  CreateCustomerGatewayResponseType() {  }
}
class RouteType extends EucalyptusData {
  String destinationCidrBlock;
  String gatewayId;
  String instanceId;
  String instanceOwnerId;
  String natGatewayId
  String networkInterfaceId;
  String vpcPeeringConnectionId;
  String state;
  String origin;
  RouteType() {  }

  RouteType(final String destinationCidrBlock,
            final String state,
            final String origin) {
    this.destinationCidrBlock = destinationCidrBlock
    this.gatewayId = gatewayId
    this.state = state
    this.origin = origin
  }

  RouteType(final String destinationCidrBlock,
            final String instanceId,
            final String instanceOwnerId,
            final String networkInterfaceId,
            final String state,
            final String origin) {
    this.destinationCidrBlock = destinationCidrBlock
    this.instanceId = instanceId
    this.instanceOwnerId = instanceOwnerId
    this.networkInterfaceId = networkInterfaceId
    this.state = state
    this.origin = origin
  }

  RouteType withGatewayId( String gatewayId ) {
    setGatewayId( gatewayId )
    this
  }

  RouteType withNatGatewayId( String natGatewayId ) {
    setNatGatewayId( natGatewayId )
    this
  }
}
class ValueSetType extends EucalyptusData {
  ValueSetType() {  }
  ArrayList<ValueType> item = new ArrayList<ValueType>();
}
class DhcpConfigurationItemSetType extends EucalyptusData {
  DhcpConfigurationItemSetType() {  }
  DhcpConfigurationItemSetType( final Collection<DhcpConfigurationItemType> configuration ) {
    item.addAll( configuration );
  }
  @HttpEmbedded( multiple = true )
  @HttpParameterMapping( parameter = "DhcpConfiguration" )
  ArrayList<DhcpConfigurationItemType> item = new ArrayList<DhcpConfigurationItemType>();
}

class AttachClassicLinkVpcType extends VpcMessage {
  ArrayList<String> securityGroupId
  String instanceId
  String vpcId
}

class AttachClassicLinkVpcResponseType extends VpcMessage {

}

class DescribeClassicLinkInstancesType extends VpcMessage {
  @HttpParameterMapping (parameter = "Filter")
  @HttpEmbedded( multiple = true )
  ArrayList<Filter> filterSet = new ArrayList<Filter>();
  ArrayList<String> instanceId
  Integer maxResults
  String nextToken
}

class DescribeClassicLinkInstancesResponseType extends VpcMessage {
}

class DescribeVpcClassicLinkType extends VpcMessage {
  @HttpParameterMapping (parameter = "Filter")
  @HttpEmbedded( multiple = true )
  ArrayList<Filter> filterSet = new ArrayList<Filter>();
  String vpcId
}

class DescribeVpcClassicLinkResponseType extends VpcMessage {
}

class DetachClassicLinkVpcType extends VpcMessage {
  String instanceId
  String vpcId
}

class DetachClassicLinkVpcResponseType extends VpcMessage {
}

class DisableVpcClassicLinkType extends VpcMessage {
  String vpcId
}

class DisableVpcClassicLinkResponseType extends VpcMessage {
}

class EnableVpcClassicLinkType extends VpcMessage {
  String vpcId
}

class EnableVpcClassicLinkResponseType extends VpcMessage {
}

class CreateVpcEndpointType extends VpcMessage {
  String clientToken
  String policyDocument
  ArrayList<String> routeTableId
  String serviceName
  String vpcId
}

class CreateVpcEndpointResponseType extends VpcMessage {
}

class DeleteVpcEndpointsType extends VpcMessage {
  ArrayList<String> vpcEndpointId
}

class DeleteVpcEndpointsResponseType extends VpcMessage {
}

class DescribePrefixListsType extends VpcMessage {
  @HttpParameterMapping (parameter = "Filter")
  @HttpEmbedded( multiple = true )
  ArrayList<Filter> filterSet = new ArrayList<Filter>();
  Integer maxResults
  String nextToken
  ArrayList<String> prefixListId
}

class DescribePrefixListsResponseType extends VpcMessage {
}

class DescribeVpcEndpointServicesType extends VpcMessage {
  Integer maxResults
  String nextToken
}

class DescribeVpcEndpointServicesResponseType extends VpcMessage {
}

class DescribeVpcEndpointsType extends VpcMessage {
  @HttpParameterMapping (parameter = "Filter")
  @HttpEmbedded( multiple = true )
  ArrayList<Filter> filterSet = new ArrayList<Filter>();
  Integer maxResults
  String nextToken
  ArrayList<String> vpcEndpointId
}

class DescribeVpcEndpointsResponseType extends VpcMessage {
}

class ModifyVpcEndpointType extends VpcMessage {
  ArrayList<String> addRouteTableId
  String policyDocument
  ArrayList<String> removeRouteTableId
  Boolean resetPolicy
  String vpcEndpointId
}

class ModifyVpcEndpointResponseType extends VpcMessage {
}