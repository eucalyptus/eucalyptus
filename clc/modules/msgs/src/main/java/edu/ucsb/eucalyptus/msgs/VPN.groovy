package edu.ucsb.eucalyptus.msgs;


public class VpmMessage extends EucalyptusMessage {
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
public class CreateCustomerGatewayType extends VpmMessage {
  String type;
  String ipAddress;
  Integer bgpAsn;
}
public class CreateCustomerGatewayResponseType extends VpmMessage {
  String requestId;
  CustomerGatewayType customerGateway;
}

public class DeleteCustomerGatewayType extends VpmMessage {
  String customerGatewayId;
}
public class DeleteCustomerGatewayResponseType extends VpmMessage {
}

public class DescribeCustomerGatewaysType extends VpmMessage {
  ArrayList<String> customerGatewaySet;
  ArrayList<Filter> filterSet;
}
public class DescribeCustomerGatewaysResponseType extends VpmMessage {
  ArrayList<CustomerGatewayType> customerGatewaySet;
}
public class CreateVpnGatewayType extends VpmMessage {
  String type;
  String availabilityZone;
}
public class CreateVpnGatewayResponseType extends VpmMessage {
  String requestId;
  String vpnGateway;
}

public class DeleteVpnGatewayType extends VpmMessage {
  String vpnGatewayId;
}
public class DeleteVpnGatewayResponseType extends VpmMessage {
}
public class DescribeVpnGatewaysType extends VpmMessage {
  ArrayList<String> vpnGatewaySet;
  ArrayList<Filter> filterSet;
}
public class DescribeVpnGatewaysResponseType extends VpmMessage {
  ArrayList<VpnGatewayType> vpnGatewaySet;
}
public class CreateVpnConnectionType extends VpmMessage {
  String type;
  String customerGatewayId;
  String vpnGatewayId;
}
public class CreateVpnConnectionResponseType extends VpmMessage {
  VpnConnectionType vpnConnection;
}
public class DeleteVpnConnectionType extends VpmMessage {
  String vpnConnectionId;
}
public class DeleteVpnConnectionResponseType extends VpmMessage {
}
public class DescribeVpnConnectionsType extends VpmMessage {
  ArrayList<String> vpnConnectionSet;
  ArrayList<Filter> filterSet;
}
public class DescribeVpnConnectionsResponseType extends VpmMessage {
  ArrayList<VpnConnectionType> vpnConnectionSet;
}
public class AttachVpnGatewayType extends VpmMessage {
  String vpnGatewayId;
  String vpcId;
}
public class AttachVpnGatewayResponseType extends VpmMessage {
  AttachmentType attachment;
}
public class DetachVpnGatewayType extends VpmMessage {
  String vpnGatewayId;
  String vpcId;
}
public class DetachVpnGatewayResponseType extends VpmMessage {
}
public class CreateVpcType extends VpmMessage {
  String cidrBlock;
}
public class CreateVpcResponseType extends VpmMessage {
  VpcType vpc;
}
public class DescribeVpcsType extends VpmMessage {
  ArrayList<String> vpcSet;
  ArrayList<Filter> filterSet;
}
public class DescribeVpcsResponseType extends VpmMessage {
  ArrayList<VpcType> vpcSet;
}
public class DeleteVpcType extends VpmMessage {
  String vpcId;
}
public class DeleteVpcResponseType extends VpmMessage {
}
public class CreateSubnetType extends VpmMessage {
  String vpcId;
  String cidrBlock;
  String availabilityZone;
}
public class CreateSubnetResponseType extends VpmMessage {
  SubnetType subnet;
}
public class DescribeSubnetsType extends VpmMessage {
  ArrayList<String> subnetSet;
  ArrayList<Filter> filterSet;
}
public class DescribeSubnetsResponseType extends VpmMessage {
  ArrayList<SubnetType> subnetSet;
}
public class DeleteSubnetType extends VpmMessage {
  String subnetId;
}
public class DeleteSubnetResponseType extends VpmMessage {
}
public class DeleteDhcpOptionsType extends VpmMessage {
  String dhcpOptionsId;
}
public class DeleteDhcpOptionsResponseType extends VpmMessage {
}
public class DescribeDhcpOptionsType extends VpmMessage {
  ArrayList<String> dhcpOptionsSet;
}
public class DescribeDhcpOptionsResponseType extends VpmMessage {
  ArrayList<DhcpOptionsType> dhcpOptionsSet;
}
public class CreateDhcpOptionsType extends VpmMessage {
  ArrayList<DhcpConfigurationItemType> dhcpConfigurationSet;
}
public class CreateDhcpOptionsResponseType extends VpmMessage {
  DhcpOptionsType dhcpOptions;
}
public class AssociateDhcpOptionsType extends VpmMessage {
  String dhcpOptionsId;
  String vpcId;
}
public class AssociateDhcpOptionsResponseType extends VpmMessage {
}
