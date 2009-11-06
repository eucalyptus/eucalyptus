package edu.ucsb.eucalyptus.msgs;


public class VPNMessageType extends EucalyptusMessage {
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
public class FilterType extends EucalyptusData {
  String name;
  ArrayList<ValueType> valueSet;
}
public class ValueType extends EucalyptusData {
  String value;
}
public class CreateCustomerGatewayType extends VPNMessageType {
  String type;
  String ipAddress;
  Integer bgpAsn;
}
public class CreateCustomerGatewayResponseType extends VPNMessageType {
  String requestId;
  CustomerGatewayType customerGateway;
}

public class DeleteCustomerGatewayType extends VPNMessageType {
  String customerGatewayId;
}
public class DeleteCustomerGatewayResponseType extends VPNMessageType {
}

public class DescribeCustomerGatewaysType extends VPNMessageType {
  ArrayList<String> customerGatewaySet;
  ArrayList<FilterType> filterSet;
}
public class DescribeCustomerGatewaysResponseType extends VPNMessageType {
  ArrayList<CustomerGatewayType> customerGatewaySet;
}
public class CreateVpnGatewayType extends VPNMessageType {
  String type;
  String availabilityZone;
}
public class CreateVpnGatewayResponseType extends VPNMessageType {
  String requestId;
  String vpnGateway;
}

public class DeleteVpnGatewayType extends VPNMessageType {
  String vpnGatewayId;
}
public class DeleteVpnGatewayResponseType extends VPNMessageType {
}
public class DescribeVpnGatewaysType extends VPNMessageType {
  ArrayList<String> vpnGatewaySet;
  ArrayList<FilterType> filterSet;
}
public class DescribeVpnGatewaysResponseType extends VPNMessageType {
  ArrayList<VpnGatewayType> vpnGatewaySet;
}
public class CreateVpnConnectionType extends VPNMessageType {
  String type;
  String customerGatewayId;
  String vpnGatewayId;
}
public class CreateVpnConnectionResponseType extends VPNMessageType {
  VpnConnectionType vpnConnection;
}
public class DeleteVpnConnectionType extends VPNMessageType {
  String vpnConnectionId;
}
public class DeleteVpnConnectionResponseType extends VPNMessageType {
}
public class DescribeVpnConnectionsType extends VPNMessageType {
  ArrayList<String> vpnConnectionSet;
  ArrayList<FilterType> filterSet;
}
public class DescribeVpnConnectionsResponseType extends VPNMessageType {
  ArrayList<VpnConnectionType> vpnConnectionSet;
}
public class AttachVpnGatewayType extends VPNMessageType {
  String vpnGatewayId;
  String vpcId;
}
public class AttachVpnGatewayResponseType extends VPNMessageType {
  AttachmentType attachment;
}
public class DetachVpnGatewayType extends VPNMessageType {
  String vpnGatewayId;
  String vpcId;
}
public class DetachVpnGatewayResponseType extends VPNMessageType {
}
public class CreateVpcType extends VPNMessageType {
  String cidrBlock;
}
public class CreateVpcResponseType extends VPNMessageType {
  VpcType vpc;
}
public class DescribeVpcsType extends VPNMessageType {
  ArrayList<String> vpcSet;
  ArrayList<FilterType> filterSet;
}
public class DescribeVpcsResponseType extends VPNMessageType {
  ArrayList<VpcType> vpcSet;
}
public class DeleteVpcType extends VPNMessageType {
  String vpcId;
}
public class DeleteVpcResponseType extends VPNMessageType {
}
public class CreateSubnetType extends VPNMessageType {
  String vpcId;
  String cidrBlock;
  String availabilityZone;
}
public class CreateSubnetResponseType extends VPNMessageType {
  SubnetType subnet;
}
public class DescribeSubnetsType extends VPNMessageType {
  ArrayList<String> subnetSet;
  ArrayList<FilterType> filterSet;
}
public class DescribeSubnetsResponseType extends VPNMessageType {
  ArrayList<SubnetType> subnetSet;
}
public class DeleteSubnetType extends VPNMessageType {
  String subnetId;
}
public class DeleteSubnetResponseType extends VPNMessageType {
}
public class DeleteDhcpOptionsType extends VPNMessageType {
  String dhcpOptionsId;
}
public class DeleteDhcpOptionsResponseType extends VPNMessageType {
}
public class DescribeDhcpOptionsType extends VPNMessageType {
  ArrayList<String> dhcpOptionsSet;
}
public class DescribeDhcpOptionsResponseType extends VPNMessageType {
  ArrayList<DhcpOptionsType> dhcpOptionsSet;
}
public class CreateDhcpOptionsType extends VPNMessageType {
  ArrayList<DhcpConfigurationItemType> dhcpConfigurationSet;
}
public class CreateDhcpOptionsResponseType extends VPNMessageType {
  DhcpOptionsType dhcpOptions;
}
public class AssociateDhcpOptionsType extends VPNMessageType {
  String dhcpOptionsId;
  String vpcId;
}
public class AssociateDhcpOptionsResponseType extends VPNMessageType {
}
