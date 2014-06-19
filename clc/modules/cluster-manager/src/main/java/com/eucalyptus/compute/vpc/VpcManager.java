/*************************************************************************
 * Copyright 2009-2014 Eucalyptus Systems, Inc.
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
 *
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 ************************************************************************/
package com.eucalyptus.compute.vpc;


import com.eucalyptus.util.EucalyptusCloudException;
import edu.ucsb.eucalyptus.msgs.AcceptVpcPeeringConnectionResponseType;
import edu.ucsb.eucalyptus.msgs.AcceptVpcPeeringConnectionType;
import edu.ucsb.eucalyptus.msgs.AssignPrivateIpAddressesResponseType;
import edu.ucsb.eucalyptus.msgs.AssignPrivateIpAddressesType;
import edu.ucsb.eucalyptus.msgs.AssociateDhcpOptionsResponseType;
import edu.ucsb.eucalyptus.msgs.AssociateDhcpOptionsType;
import edu.ucsb.eucalyptus.msgs.AssociateRouteTableResponseType;
import edu.ucsb.eucalyptus.msgs.AssociateRouteTableType;
import edu.ucsb.eucalyptus.msgs.AttachInternetGatewayResponseType;
import edu.ucsb.eucalyptus.msgs.AttachInternetGatewayType;
import edu.ucsb.eucalyptus.msgs.AttachNetworkInterfaceResponseType;
import edu.ucsb.eucalyptus.msgs.AttachNetworkInterfaceType;
import edu.ucsb.eucalyptus.msgs.AttachVpnGatewayResponseType;
import edu.ucsb.eucalyptus.msgs.AttachVpnGatewayType;
import edu.ucsb.eucalyptus.msgs.CreateCustomerGatewayResponseType;
import edu.ucsb.eucalyptus.msgs.CreateCustomerGatewayType;
import edu.ucsb.eucalyptus.msgs.CreateDhcpOptionsResponseType;
import edu.ucsb.eucalyptus.msgs.CreateDhcpOptionsType;
import edu.ucsb.eucalyptus.msgs.CreateInternetGatewayResponseType;
import edu.ucsb.eucalyptus.msgs.CreateInternetGatewayType;
import edu.ucsb.eucalyptus.msgs.CreateNetworkAclEntryResponseType;
import edu.ucsb.eucalyptus.msgs.CreateNetworkAclEntryType;
import edu.ucsb.eucalyptus.msgs.CreateNetworkAclResponseType;
import edu.ucsb.eucalyptus.msgs.CreateNetworkAclType;
import edu.ucsb.eucalyptus.msgs.CreateNetworkInterfaceResponseType;
import edu.ucsb.eucalyptus.msgs.CreateNetworkInterfaceType;
import edu.ucsb.eucalyptus.msgs.CreateRouteResponseType;
import edu.ucsb.eucalyptus.msgs.CreateRouteTableResponseType;
import edu.ucsb.eucalyptus.msgs.CreateRouteTableType;
import edu.ucsb.eucalyptus.msgs.CreateRouteType;
import edu.ucsb.eucalyptus.msgs.CreateSubnetResponseType;
import edu.ucsb.eucalyptus.msgs.CreateSubnetType;
import edu.ucsb.eucalyptus.msgs.CreateVpcPeeringConnectionResponseType;
import edu.ucsb.eucalyptus.msgs.CreateVpcPeeringConnectionType;
import edu.ucsb.eucalyptus.msgs.CreateVpcResponseType;
import edu.ucsb.eucalyptus.msgs.CreateVpcType;
import edu.ucsb.eucalyptus.msgs.CreateVpnConnectionResponseType;
import edu.ucsb.eucalyptus.msgs.CreateVpnConnectionRouteResponseType;
import edu.ucsb.eucalyptus.msgs.CreateVpnConnectionRouteType;
import edu.ucsb.eucalyptus.msgs.CreateVpnConnectionType;
import edu.ucsb.eucalyptus.msgs.CreateVpnGatewayResponseType;
import edu.ucsb.eucalyptus.msgs.CreateVpnGatewayType;
import edu.ucsb.eucalyptus.msgs.DeleteCustomerGatewayResponseType;
import edu.ucsb.eucalyptus.msgs.DeleteCustomerGatewayType;
import edu.ucsb.eucalyptus.msgs.DeleteDhcpOptionsResponseType;
import edu.ucsb.eucalyptus.msgs.DeleteDhcpOptionsType;
import edu.ucsb.eucalyptus.msgs.DeleteInternetGatewayResponseType;
import edu.ucsb.eucalyptus.msgs.DeleteInternetGatewayType;
import edu.ucsb.eucalyptus.msgs.DeleteNetworkAclEntryResponseType;
import edu.ucsb.eucalyptus.msgs.DeleteNetworkAclEntryType;
import edu.ucsb.eucalyptus.msgs.DeleteNetworkAclResponseType;
import edu.ucsb.eucalyptus.msgs.DeleteNetworkAclType;
import edu.ucsb.eucalyptus.msgs.DeleteNetworkInterfaceResponseType;
import edu.ucsb.eucalyptus.msgs.DeleteNetworkInterfaceType;
import edu.ucsb.eucalyptus.msgs.DeleteRouteResponseType;
import edu.ucsb.eucalyptus.msgs.DeleteRouteTableResponseType;
import edu.ucsb.eucalyptus.msgs.DeleteRouteTableType;
import edu.ucsb.eucalyptus.msgs.DeleteRouteType;
import edu.ucsb.eucalyptus.msgs.DeleteSubnetResponseType;
import edu.ucsb.eucalyptus.msgs.DeleteSubnetType;
import edu.ucsb.eucalyptus.msgs.DeleteVpcPeeringConnectionResponseType;
import edu.ucsb.eucalyptus.msgs.DeleteVpcPeeringConnectionType;
import edu.ucsb.eucalyptus.msgs.DeleteVpcResponseType;
import edu.ucsb.eucalyptus.msgs.DeleteVpcType;
import edu.ucsb.eucalyptus.msgs.DeleteVpnConnectionResponseType;
import edu.ucsb.eucalyptus.msgs.DeleteVpnConnectionRouteResponseType;
import edu.ucsb.eucalyptus.msgs.DeleteVpnConnectionRouteType;
import edu.ucsb.eucalyptus.msgs.DeleteVpnConnectionType;
import edu.ucsb.eucalyptus.msgs.DeleteVpnGatewayResponseType;
import edu.ucsb.eucalyptus.msgs.DeleteVpnGatewayType;
import edu.ucsb.eucalyptus.msgs.DescribeAccountAttributesResponseType;
import edu.ucsb.eucalyptus.msgs.DescribeAccountAttributesType;
import edu.ucsb.eucalyptus.msgs.DescribeCustomerGatewaysResponseType;
import edu.ucsb.eucalyptus.msgs.DescribeCustomerGatewaysType;
import edu.ucsb.eucalyptus.msgs.DescribeDhcpOptionsResponseType;
import edu.ucsb.eucalyptus.msgs.DescribeDhcpOptionsType;
import edu.ucsb.eucalyptus.msgs.DescribeInternetGatewaysResponseType;
import edu.ucsb.eucalyptus.msgs.DescribeInternetGatewaysType;
import edu.ucsb.eucalyptus.msgs.DescribeNetworkAclsResponseType;
import edu.ucsb.eucalyptus.msgs.DescribeNetworkAclsType;
import edu.ucsb.eucalyptus.msgs.DescribeNetworkInterfaceAttributeResponseType;
import edu.ucsb.eucalyptus.msgs.DescribeNetworkInterfaceAttributeType;
import edu.ucsb.eucalyptus.msgs.DescribeNetworkInterfacesResponseType;
import edu.ucsb.eucalyptus.msgs.DescribeNetworkInterfacesType;
import edu.ucsb.eucalyptus.msgs.DescribeRouteTablesResponseType;
import edu.ucsb.eucalyptus.msgs.DescribeRouteTablesType;
import edu.ucsb.eucalyptus.msgs.DescribeSubnetsResponseType;
import edu.ucsb.eucalyptus.msgs.DescribeSubnetsType;
import edu.ucsb.eucalyptus.msgs.DescribeVpcAttributeResponseType;
import edu.ucsb.eucalyptus.msgs.DescribeVpcAttributeType;
import edu.ucsb.eucalyptus.msgs.DescribeVpcPeeringConnectionsResponseType;
import edu.ucsb.eucalyptus.msgs.DescribeVpcPeeringConnectionsType;
import edu.ucsb.eucalyptus.msgs.DescribeVpcsResponseType;
import edu.ucsb.eucalyptus.msgs.DescribeVpcsType;
import edu.ucsb.eucalyptus.msgs.DescribeVpnConnectionsResponseType;
import edu.ucsb.eucalyptus.msgs.DescribeVpnConnectionsType;
import edu.ucsb.eucalyptus.msgs.DescribeVpnGatewaysResponseType;
import edu.ucsb.eucalyptus.msgs.DescribeVpnGatewaysType;
import edu.ucsb.eucalyptus.msgs.DetachInternetGatewayResponseType;
import edu.ucsb.eucalyptus.msgs.DetachInternetGatewayType;
import edu.ucsb.eucalyptus.msgs.DetachNetworkInterfaceResponseType;
import edu.ucsb.eucalyptus.msgs.DetachNetworkInterfaceType;
import edu.ucsb.eucalyptus.msgs.DetachVpnGatewayResponseType;
import edu.ucsb.eucalyptus.msgs.DetachVpnGatewayType;
import edu.ucsb.eucalyptus.msgs.DisableVgwRoutePropagationResponseType;
import edu.ucsb.eucalyptus.msgs.DisableVgwRoutePropagationType;
import edu.ucsb.eucalyptus.msgs.DisassociateAddressResponseType;
import edu.ucsb.eucalyptus.msgs.DisassociateAddressType;
import edu.ucsb.eucalyptus.msgs.DisassociateRouteTableResponseType;
import edu.ucsb.eucalyptus.msgs.DisassociateRouteTableType;
import edu.ucsb.eucalyptus.msgs.EnableVgwRoutePropagationResponseType;
import edu.ucsb.eucalyptus.msgs.EnableVgwRoutePropagationType;
import edu.ucsb.eucalyptus.msgs.ModifyNetworkInterfaceAttributeResponseType;
import edu.ucsb.eucalyptus.msgs.ModifyNetworkInterfaceAttributeType;
import edu.ucsb.eucalyptus.msgs.ModifyVpcAttributeResponseType;
import edu.ucsb.eucalyptus.msgs.ModifyVpcAttributeType;
import edu.ucsb.eucalyptus.msgs.RejectVpcPeeringConnectionResponseType;
import edu.ucsb.eucalyptus.msgs.RejectVpcPeeringConnectionType;
import edu.ucsb.eucalyptus.msgs.ReplaceNetworkAclAssociationResponseType;
import edu.ucsb.eucalyptus.msgs.ReplaceNetworkAclAssociationType;
import edu.ucsb.eucalyptus.msgs.ReplaceNetworkAclEntryResponseType;
import edu.ucsb.eucalyptus.msgs.ReplaceNetworkAclEntryType;
import edu.ucsb.eucalyptus.msgs.ReplaceRouteResponseType;
import edu.ucsb.eucalyptus.msgs.ReplaceRouteTableAssociationResponseType;
import edu.ucsb.eucalyptus.msgs.ReplaceRouteTableAssociationType;
import edu.ucsb.eucalyptus.msgs.ReplaceRouteType;
import edu.ucsb.eucalyptus.msgs.ResetNetworkInterfaceAttributeResponseType;
import edu.ucsb.eucalyptus.msgs.ResetNetworkInterfaceAttributeType;
import edu.ucsb.eucalyptus.msgs.UnassignPrivateIpAddressesResponseType;
import edu.ucsb.eucalyptus.msgs.UnassignPrivateIpAddressesType;

/**
 *
 */
public class VpcManager {
  public AcceptVpcPeeringConnectionResponseType acceptVpcPeeringConnection(AcceptVpcPeeringConnectionType request) throws EucalyptusCloudException {
    AcceptVpcPeeringConnectionResponseType reply = request.getReply( );
    return reply;
  }

  public AssignPrivateIpAddressesResponseType assignPrivateIpAddresses(AssignPrivateIpAddressesType request) throws EucalyptusCloudException {
    AssignPrivateIpAddressesResponseType reply = request.getReply( );
    return reply;
  }

  public AssociateDhcpOptionsResponseType associateDhcpOptions(AssociateDhcpOptionsType request) throws EucalyptusCloudException {
    AssociateDhcpOptionsResponseType reply = request.getReply( );
    return reply;
  }

  public AssociateRouteTableResponseType associateRouteTable(AssociateRouteTableType request) throws EucalyptusCloudException {
    AssociateRouteTableResponseType reply = request.getReply( );
    return reply;
  }

  public AttachInternetGatewayResponseType attachInternetGateway(AttachInternetGatewayType request) throws EucalyptusCloudException {
    AttachInternetGatewayResponseType reply = request.getReply( );
    return reply;
  }

  public AttachNetworkInterfaceResponseType attachNetworkInterface(AttachNetworkInterfaceType request) throws EucalyptusCloudException {
    AttachNetworkInterfaceResponseType reply = request.getReply( );
    return reply;
  }

  public AttachVpnGatewayResponseType attachVpnGateway(AttachVpnGatewayType request) throws EucalyptusCloudException {
    AttachVpnGatewayResponseType reply = request.getReply( );
    return reply;
  }

  public CreateCustomerGatewayResponseType createCustomerGateway(CreateCustomerGatewayType request) throws EucalyptusCloudException {
    CreateCustomerGatewayResponseType reply = request.getReply( );
    return reply;
  }

  public CreateDhcpOptionsResponseType createDhcpOptions(CreateDhcpOptionsType request) throws EucalyptusCloudException {
    CreateDhcpOptionsResponseType reply = request.getReply( );
    return reply;
  }

  public CreateInternetGatewayResponseType createInternetGateway(CreateInternetGatewayType request) throws EucalyptusCloudException {
    CreateInternetGatewayResponseType reply = request.getReply( );
    return reply;
  }

  public CreateNetworkAclResponseType createNetworkAcl(CreateNetworkAclType request) throws EucalyptusCloudException {
    CreateNetworkAclResponseType reply = request.getReply( );
    return reply;
  }

  public CreateNetworkAclEntryResponseType createNetworkAclEntry(CreateNetworkAclEntryType request) throws EucalyptusCloudException {
    CreateNetworkAclEntryResponseType reply = request.getReply( );
    return reply;
  }

  public CreateNetworkInterfaceResponseType createNetworkInterface(CreateNetworkInterfaceType request) throws EucalyptusCloudException {
    CreateNetworkInterfaceResponseType reply = request.getReply( );
    return reply;
  }

  public CreateRouteResponseType createRoute(CreateRouteType request) throws EucalyptusCloudException {
    CreateRouteResponseType reply = request.getReply( );
    return reply;
  }

  public CreateRouteTableResponseType createRouteTable(CreateRouteTableType request) throws EucalyptusCloudException {
    CreateRouteTableResponseType reply = request.getReply( );
    return reply;
  }

  public CreateSubnetResponseType createSubnet(CreateSubnetType request) throws EucalyptusCloudException {
    CreateSubnetResponseType reply = request.getReply( );
    return reply;
  }

  public CreateVpcResponseType createVpc(CreateVpcType request) throws EucalyptusCloudException {
    CreateVpcResponseType reply = request.getReply( );
    return reply;
  }

  public CreateVpcPeeringConnectionResponseType createVpcPeeringConnection(CreateVpcPeeringConnectionType request) throws EucalyptusCloudException {
    CreateVpcPeeringConnectionResponseType reply = request.getReply( );
    return reply;
  }

  public CreateVpnConnectionResponseType createVpnConnection(CreateVpnConnectionType request) throws EucalyptusCloudException {
    CreateVpnConnectionResponseType reply = request.getReply( );
    return reply;
  }

  public CreateVpnConnectionRouteResponseType createVpnConnectionRoute(CreateVpnConnectionRouteType request) throws EucalyptusCloudException {
    CreateVpnConnectionRouteResponseType reply = request.getReply( );
    return reply;
  }

  public CreateVpnGatewayResponseType createVpnGateway(CreateVpnGatewayType request) throws EucalyptusCloudException {
    CreateVpnGatewayResponseType reply = request.getReply( );
    return reply;
  }

  public DeleteCustomerGatewayResponseType deleteCustomerGateway(DeleteCustomerGatewayType request) throws EucalyptusCloudException {
    DeleteCustomerGatewayResponseType reply = request.getReply( );
    return reply;
  }

  public DeleteDhcpOptionsResponseType deleteDhcpOptions(DeleteDhcpOptionsType request) throws EucalyptusCloudException {
    DeleteDhcpOptionsResponseType reply = request.getReply( );
    return reply;
  }

  public DeleteInternetGatewayResponseType deleteInternetGateway(DeleteInternetGatewayType request) throws EucalyptusCloudException {
    DeleteInternetGatewayResponseType reply = request.getReply( );
    return reply;
  }

  public DeleteNetworkAclResponseType deleteNetworkAcl(DeleteNetworkAclType request) throws EucalyptusCloudException {
    DeleteNetworkAclResponseType reply = request.getReply( );
    return reply;
  }

  public DeleteNetworkAclEntryResponseType deleteNetworkAclEntry(DeleteNetworkAclEntryType request) throws EucalyptusCloudException {
    DeleteNetworkAclEntryResponseType reply = request.getReply( );
    return reply;
  }

  public DeleteNetworkInterfaceResponseType deleteNetworkInterface(DeleteNetworkInterfaceType request) throws EucalyptusCloudException {
    DeleteNetworkInterfaceResponseType reply = request.getReply( );
    return reply;
  }

  public DeleteRouteResponseType deleteRoute(DeleteRouteType request) throws EucalyptusCloudException {
    DeleteRouteResponseType reply = request.getReply( );
    return reply;
  }

  public DeleteRouteTableResponseType deleteRouteTable(DeleteRouteTableType request) throws EucalyptusCloudException {
    DeleteRouteTableResponseType reply = request.getReply( );
    return reply;
  }

  public DeleteSubnetResponseType deleteSubnet(DeleteSubnetType request) throws EucalyptusCloudException {
    DeleteSubnetResponseType reply = request.getReply( );
    return reply;
  }

  public DeleteVpcResponseType deleteVpc(DeleteVpcType request) throws EucalyptusCloudException {
    DeleteVpcResponseType reply = request.getReply( );
    return reply;
  }

  public DeleteVpcPeeringConnectionResponseType deleteVpcPeeringConnection(DeleteVpcPeeringConnectionType request) throws EucalyptusCloudException {
    DeleteVpcPeeringConnectionResponseType reply = request.getReply( );
    return reply;
  }

  public DeleteVpnConnectionResponseType deleteVpnConnection(DeleteVpnConnectionType request) throws EucalyptusCloudException {
    DeleteVpnConnectionResponseType reply = request.getReply( );
    return reply;
  }

  public DeleteVpnConnectionRouteResponseType deleteVpnConnectionRoute(DeleteVpnConnectionRouteType request) throws EucalyptusCloudException {
    DeleteVpnConnectionRouteResponseType reply = request.getReply( );
    return reply;
  }

  public DeleteVpnGatewayResponseType deleteVpnGateway(DeleteVpnGatewayType request) throws EucalyptusCloudException {
    DeleteVpnGatewayResponseType reply = request.getReply( );
    return reply;
  }

  public DescribeAccountAttributesResponseType describeAccountAttributes(DescribeAccountAttributesType request) throws EucalyptusCloudException {
    DescribeAccountAttributesResponseType reply = request.getReply( );
    return reply;
  }

  public DescribeCustomerGatewaysResponseType describeCustomerGateways(DescribeCustomerGatewaysType request) throws EucalyptusCloudException {
    DescribeCustomerGatewaysResponseType reply = request.getReply( );
    return reply;
  }

  public DescribeDhcpOptionsResponseType describeDhcpOptions(DescribeDhcpOptionsType request) throws EucalyptusCloudException {
    DescribeDhcpOptionsResponseType reply = request.getReply( );
    return reply;
  }

  public DescribeInternetGatewaysResponseType describeInternetGateways(DescribeInternetGatewaysType request) throws EucalyptusCloudException {
    DescribeInternetGatewaysResponseType reply = request.getReply( );
    return reply;
  }

  public DescribeNetworkAclsResponseType describeNetworkAcls(DescribeNetworkAclsType request) throws EucalyptusCloudException {
    DescribeNetworkAclsResponseType reply = request.getReply( );
    return reply;
  }

  public DescribeNetworkInterfaceAttributeResponseType describeNetworkInterfaceAttribute(DescribeNetworkInterfaceAttributeType request) throws EucalyptusCloudException {
    DescribeNetworkInterfaceAttributeResponseType reply = request.getReply( );
    return reply;
  }

  public DescribeNetworkInterfacesResponseType describeNetworkInterfaces(DescribeNetworkInterfacesType request) throws EucalyptusCloudException {
    DescribeNetworkInterfacesResponseType reply = request.getReply( );
    return reply;
  }

  public DescribeRouteTablesResponseType describeRouteTables(DescribeRouteTablesType request) throws EucalyptusCloudException {
    DescribeRouteTablesResponseType reply = request.getReply( );
    return reply;
  }

  public DescribeSubnetsResponseType describeSubnets(DescribeSubnetsType request) throws EucalyptusCloudException {
    DescribeSubnetsResponseType reply = request.getReply( );
    return reply;
  }

  public DescribeVpcAttributeResponseType describeVpcAttribute(DescribeVpcAttributeType request) throws EucalyptusCloudException {
    DescribeVpcAttributeResponseType reply = request.getReply( );
    return reply;
  }

  public DescribeVpcPeeringConnectionsResponseType describeVpcPeeringConnections(DescribeVpcPeeringConnectionsType request) throws EucalyptusCloudException {
    DescribeVpcPeeringConnectionsResponseType reply = request.getReply( );
    return reply;
  }

  public DescribeVpcsResponseType describeVpcs(DescribeVpcsType request) throws EucalyptusCloudException {
    DescribeVpcsResponseType reply = request.getReply( );
    return reply;
  }

  public DescribeVpnConnectionsResponseType describeVpnConnections(DescribeVpnConnectionsType request) throws EucalyptusCloudException {
    DescribeVpnConnectionsResponseType reply = request.getReply( );
    return reply;
  }

  public DescribeVpnGatewaysResponseType describeVpnGateways(DescribeVpnGatewaysType request) throws EucalyptusCloudException {
    DescribeVpnGatewaysResponseType reply = request.getReply( );
    return reply;
  }

  public DetachInternetGatewayResponseType detachInternetGateway(DetachInternetGatewayType request) throws EucalyptusCloudException {
    DetachInternetGatewayResponseType reply = request.getReply( );
    return reply;
  }

  public DetachNetworkInterfaceResponseType detachNetworkInterface(DetachNetworkInterfaceType request) throws EucalyptusCloudException {
    DetachNetworkInterfaceResponseType reply = request.getReply( );
    return reply;
  }

  public DetachVpnGatewayResponseType detachVpnGateway(DetachVpnGatewayType request) throws EucalyptusCloudException {
    DetachVpnGatewayResponseType reply = request.getReply( );
    return reply;
  }

  public DisableVgwRoutePropagationResponseType disableVgwRoutePropagation(DisableVgwRoutePropagationType request) throws EucalyptusCloudException {
    DisableVgwRoutePropagationResponseType reply = request.getReply( );
    return reply;
  }

  public DisassociateAddressResponseType disassociateAddress(DisassociateAddressType request) throws EucalyptusCloudException {
    DisassociateAddressResponseType reply = request.getReply( );
    return reply;
  }

  public DisassociateRouteTableResponseType disassociateRouteTable(DisassociateRouteTableType request) throws EucalyptusCloudException {
    DisassociateRouteTableResponseType reply = request.getReply( );
    return reply;
  }

  public EnableVgwRoutePropagationResponseType enableVgwRoutePropagation(EnableVgwRoutePropagationType request) throws EucalyptusCloudException {
    EnableVgwRoutePropagationResponseType reply = request.getReply( );
    return reply;
  }

  public ModifyNetworkInterfaceAttributeResponseType modifyNetworkInterfaceAttribute(ModifyNetworkInterfaceAttributeType request) throws EucalyptusCloudException {
    ModifyNetworkInterfaceAttributeResponseType reply = request.getReply( );
    return reply;
  }

  public ModifyVpcAttributeResponseType modifyVpcAttribute(ModifyVpcAttributeType request) throws EucalyptusCloudException {
    ModifyVpcAttributeResponseType reply = request.getReply( );
    return reply;
  }

  public RejectVpcPeeringConnectionResponseType rejectVpcPeeringConnection(RejectVpcPeeringConnectionType request) throws EucalyptusCloudException {
    RejectVpcPeeringConnectionResponseType reply = request.getReply( );
    return reply;
  }

  public ReplaceNetworkAclAssociationResponseType replaceNetworkAclAssociation(ReplaceNetworkAclAssociationType request) throws EucalyptusCloudException {
    ReplaceNetworkAclAssociationResponseType reply = request.getReply( );
    return reply;
  }

  public ReplaceNetworkAclEntryResponseType replaceNetworkAclEntry(ReplaceNetworkAclEntryType request) throws EucalyptusCloudException {
    ReplaceNetworkAclEntryResponseType reply = request.getReply( );
    return reply;
  }

  public ReplaceRouteResponseType replaceRoute(ReplaceRouteType request) throws EucalyptusCloudException {
    ReplaceRouteResponseType reply = request.getReply( );
    return reply;
  }

  public ReplaceRouteTableAssociationResponseType replaceRouteTableAssociation(ReplaceRouteTableAssociationType request) throws EucalyptusCloudException {
    ReplaceRouteTableAssociationResponseType reply = request.getReply( );
    return reply;
  }

  public ResetNetworkInterfaceAttributeResponseType resetNetworkInterfaceAttribute(ResetNetworkInterfaceAttributeType request) throws EucalyptusCloudException {
    ResetNetworkInterfaceAttributeResponseType reply = request.getReply( );
    return reply;
  }

  public UnassignPrivateIpAddressesResponseType unassignPrivateIpAddresses(UnassignPrivateIpAddressesType request) throws EucalyptusCloudException {
    UnassignPrivateIpAddressesResponseType reply = request.getReply( );
    return reply;
  }
}
