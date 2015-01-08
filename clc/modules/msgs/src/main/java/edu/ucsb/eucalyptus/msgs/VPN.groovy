/*************************************************************************
 * Copyright 2009-2012 Eucalyptus Systems, Inc.
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
 *
 * This file may incorporate work covered under the following copyright
 * and permission notice:
 *
 *   Software License Agreement (BSD License)
 *
 *   Copyright (c) 2008, Regents of the University of California
 *   All rights reserved.
 *
 *   Redistribution and use of this software in source and binary forms,
 *   with or without modification, are permitted provided that the
 *   following conditions are met:
 *
 *     Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *     Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer
 *     in the documentation and/or other materials provided with the
 *     distribution.
 *
 *   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *   "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *   LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 *   FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 *   COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 *   INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 *   BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *   LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 *   CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 *   LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 *   ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 *   POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 *   THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 *   COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 *   AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 *   IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 *   SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 *   WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 *   REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 *   IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 *   NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/

@GroovyAddClassUUID
package edu.ucsb.eucalyptus.msgs;

public class VirtualNetworkingMessage extends EucalyptusMessage {

  public VirtualNetworkingMessage( ) {
    super( );
  }

  public VirtualNetworkingMessage( EucalyptusMessage msg ) {
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
