/*************************************************************************
 * Copyright 2009-2014 Eucalyptus Systems, Inc.
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

import edu.ucsb.eucalyptus.msgs.EucalyptusData
import edu.ucsb.eucalyptus.msgs.GroovyAddClassUUID;

public class VirtualNetworkingMessage extends ComputeMessage {

  public VirtualNetworkingMessage( ) {
    super( );
  }

  public VirtualNetworkingMessage( ComputeMessage msg ) {
    super( msg );
  }

  public VirtualNetworkingMessage( String userId ) {
    super( userId );
  }
  
}

public class AttachmentType extends EucalyptusData {
  String vpcId;
  String state;
}

public class VpnGatewayType extends EucalyptusData {
  String vpnGatewayId;
  String state;
  String type;
  String availabilityZone;
  ArrayList<AttachmentType> attachments;
}

public class CustomerGatewayType extends EucalyptusData {
  String customerGatewayId;
  String state;
  String type;
  String ipAddress;
  Integer bgpAsn;
}

public class VpnConnectionType extends EucalyptusData {
  String vpnConnectionId;
  String state;
  String customerGatewayConfiguration;
  String type;
  String customerGatewayId;
  String vpnGatewayId;
}
public class VpcType extends EucalyptusData {
  String vpcId;
  String state;
  String cidrBlock;
  String dhcpOptionsId;
}
public class SubnetType extends EucalyptusData {
  String subnetId;
  String state;
  String vpcId;
  String cidrBlock;
  String availableIpAddressCount;
  String availabilityZone;
}
public class CustomerGatewayIdSetItemType extends EucalyptusData {
  String customerGatewayId;
}
public class VpnGatewayIdSetItemType extends EucalyptusData {
  String vpnGatewayId;
}
public class VpnConnectionIdSetItemType extends EucalyptusData {
  String vpnConnectionId;
}
public class VpcIdSetItemType extends EucalyptusData {
  String vpcId;
}
public class SubnetIdSetItemType extends EucalyptusData {
  String subnetId;
}
public class DhcpOptionsIdSetItemType extends EucalyptusData {
  String dhcpOptionsId;
}
public class DhcpConfigurationItemType extends EucalyptusData {
  String key;
  ArrayList<DhcpValueType> valueSet;
}
public class DhcpOptionsType extends EucalyptusData {
  String dhcpOptionsId;
  ArrayList<DhcpConfigurationItemType> dhcpConfigurationSet;
}  
public class DhcpValueType extends EucalyptusData {
  String value;
}
public class CreateCustomerGatewayType extends VirtualNetworkingMessage {
  String type;
  String ipAddress;
  Integer bgpAsn;
}
public class CreateCustomerGatewayResponseType extends VirtualNetworkingMessage {
  String requestId;
  CustomerGatewayType customerGateway;
}

public class DeleteCustomerGatewayType extends VirtualNetworkingMessage {
  String customerGatewayId;
}
public class DeleteCustomerGatewayResponseType extends VirtualNetworkingMessage {
}

public class DescribeCustomerGatewaysType extends VirtualNetworkingMessage {
  ArrayList<String> customerGatewaySet;
  ArrayList<Filter> filterSet;
}
public class DescribeCustomerGatewaysResponseType extends VirtualNetworkingMessage {
  ArrayList<CustomerGatewayType> customerGatewaySet;
}
public class CreateVpnGatewayType extends VirtualNetworkingMessage {
  String type;
  String availabilityZone;
}
public class CreateVpnGatewayResponseType extends VirtualNetworkingMessage {
  String requestId;
  String vpnGateway;
}

public class DeleteVpnGatewayType extends VirtualNetworkingMessage {
  String vpnGatewayId;
}
public class DeleteVpnGatewayResponseType extends VirtualNetworkingMessage {
}
public class DescribeVpnGatewaysType extends VirtualNetworkingMessage {
  ArrayList<String> vpnGatewaySet;
  ArrayList<Filter> filterSet;
}
public class DescribeVpnGatewaysResponseType extends VirtualNetworkingMessage {
  ArrayList<VpnGatewayType> vpnGatewaySet;
}
public class CreateVpnConnectionType extends VirtualNetworkingMessage {
  String type;
  String customerGatewayId;
  String vpnGatewayId;
}
public class CreateVpnConnectionResponseType extends VirtualNetworkingMessage {
  VpnConnectionType vpnConnection;
}
public class DeleteVpnConnectionType extends VirtualNetworkingMessage {
  String vpnConnectionId;
}
public class DeleteVpnConnectionResponseType extends VirtualNetworkingMessage {
}
public class DescribeVpnConnectionsType extends VirtualNetworkingMessage {
  ArrayList<String> vpnConnectionSet;
  ArrayList<Filter> filterSet;
}
public class DescribeVpnConnectionsResponseType extends VirtualNetworkingMessage {
  ArrayList<VpnConnectionType> vpnConnectionSet;
}
public class AttachVpnGatewayType extends VirtualNetworkingMessage {
  String vpnGatewayId;
  String vpcId;
}
public class AttachVpnGatewayResponseType extends VirtualNetworkingMessage {
  AttachmentType attachment;
}
public class DetachVpnGatewayType extends VirtualNetworkingMessage {
  String vpnGatewayId;
  String vpcId;
}
public class DetachVpnGatewayResponseType extends VirtualNetworkingMessage {
}
public class CreateVpcType extends VirtualNetworkingMessage {
  String cidrBlock;
}
public class CreateVpcResponseType extends VirtualNetworkingMessage {
  VpcType vpc;
}
public class DescribeVpcsType extends VirtualNetworkingMessage {
  ArrayList<String> vpcSet;
  ArrayList<Filter> filterSet;
}
public class DescribeVpcsResponseType extends VirtualNetworkingMessage {
  ArrayList<VpcType> vpcSet;
}
public class DeleteVpcType extends VirtualNetworkingMessage {
  String vpcId;
}
public class DeleteVpcResponseType extends VirtualNetworkingMessage {
}
public class CreateSubnetType extends VirtualNetworkingMessage {
  String vpcId;
  String cidrBlock;
  String availabilityZone;
}
public class CreateSubnetResponseType extends VirtualNetworkingMessage {
  SubnetType subnet;
}
public class DescribeSubnetsType extends VirtualNetworkingMessage {
  ArrayList<String> subnetSet;
  ArrayList<Filter> filterSet;
}
public class DescribeSubnetsResponseType extends VirtualNetworkingMessage {
  ArrayList<SubnetType> subnetSet;
}
public class DeleteSubnetType extends VirtualNetworkingMessage {
  String subnetId;
}
public class DeleteSubnetResponseType extends VirtualNetworkingMessage {
}
public class DeleteDhcpOptionsType extends VirtualNetworkingMessage {
  String dhcpOptionsId;
}
public class DeleteDhcpOptionsResponseType extends VirtualNetworkingMessage {
}
public class DescribeDhcpOptionsType extends VirtualNetworkingMessage {
  ArrayList<String> dhcpOptionsSet;
}
public class DescribeDhcpOptionsResponseType extends VirtualNetworkingMessage {
  ArrayList<DhcpOptionsType> dhcpOptionsSet;
}
public class CreateDhcpOptionsType extends VirtualNetworkingMessage {
  ArrayList<DhcpConfigurationItemType> dhcpConfigurationSet;
}
public class CreateDhcpOptionsResponseType extends VirtualNetworkingMessage {
  DhcpOptionsType dhcpOptions;
}
public class AssociateDhcpOptionsType extends VirtualNetworkingMessage {
  String dhcpOptionsId;
  String vpcId;
}
public class AssociateDhcpOptionsResponseType extends VirtualNetworkingMessage {
}
