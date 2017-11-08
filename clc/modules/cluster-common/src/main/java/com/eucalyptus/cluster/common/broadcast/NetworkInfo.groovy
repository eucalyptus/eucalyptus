/*************************************************************************
 * Copyright 2009-2016 Ent. Services Development Corporation LP
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
package com.eucalyptus.cluster.common.broadcast

import com.google.common.collect.Lists
import groovy.transform.Canonical
import groovy.transform.CompileStatic

import javax.xml.bind.annotation.XmlAccessType
import javax.xml.bind.annotation.XmlAccessorType
import javax.xml.bind.annotation.XmlAttribute
import javax.xml.bind.annotation.XmlElement
import javax.xml.bind.annotation.XmlElementWrapper
import javax.xml.bind.annotation.XmlRootElement

/**
 *
 */
@Canonical
@CompileStatic
@XmlRootElement(name = "network-data")
@XmlAccessorType( XmlAccessType.NONE )
class NetworkInfo {
  @XmlAttribute String version
  @XmlAttribute(name="applied-version") String appliedVersion
  @XmlAttribute(name="applied-time") String appliedTime
  @XmlElement NIConfiguration configuration = new NIConfiguration()
  @XmlElementWrapper @XmlElement(name="vpc") List<NIVpc> vpcs = Lists.newArrayList()
  @XmlElementWrapper @XmlElement(name="instance") List<NIInstance> instances = Lists.newArrayList()
  @XmlElementWrapper @XmlElement(name="dhcpOptionSet") List<NIDhcpOptionSet> dhcpOptionSets = Lists.newArrayList()
  @XmlElementWrapper @XmlElement(name="internetGateway") List<NIInternetGateway> internetGateways = Lists.newArrayList()
  @XmlElementWrapper @XmlElement(name="securityGroup") List<NISecurityGroup> securityGroups = Lists.newArrayList()
}

@Canonical
@CompileStatic
@XmlAccessorType( XmlAccessType.NONE )
class NIConfiguration {
  @XmlElement(name="property") List<NIProperty> properties = Lists.newArrayList()
  @XmlElement(name="property") NIMidonet midonet
  @XmlElement(name="property") NISubnets subnets
  @XmlElement(name="property") NIManagedSubnets managedSubnet
  @XmlElement(name="property") NIClusters clusters
}

@Canonical
@CompileStatic
@XmlAccessorType( XmlAccessType.NONE )
class NIProperty {
  @XmlAttribute String name
  @XmlElement(name="value") List<String> values = Lists.newArrayList()
}

@Canonical
@CompileStatic
@XmlAccessorType( XmlAccessType.NONE )
class NIMidonet {
  @XmlAttribute String name
  @XmlElement(name="property") NIMidonetGateways gateways
  @XmlElement(name="property") List<NIProperty> properties = Lists.newArrayList()
}

@Canonical
@CompileStatic
@XmlAccessorType( XmlAccessType.NONE )
class NIMidonetGateways {
  @XmlAttribute String name
  @XmlElement(name="gateway") List<NIMidonetGateway> gateways = Lists.newArrayList()
}

@Canonical
@CompileStatic
@XmlAccessorType( XmlAccessType.NONE )
class NIMidonetGateway {
  @XmlElement(name="property") List<NIProperty> properties = Lists.newArrayList()
}

@Canonical
@CompileStatic
@XmlAccessorType( XmlAccessType.NONE )
class NISubnets {
  @XmlAttribute String name
  @XmlElement(name="subnet") List<NISubnet> subnets = Lists.newArrayList()
}

@Canonical
@CompileStatic
@XmlAccessorType( XmlAccessType.NONE )
class NISubnet {
  @XmlAttribute String name
  @XmlElement(name="property") List<NIProperty> properties = Lists.newArrayList()
}

@Canonical
@CompileStatic
@XmlAccessorType( XmlAccessType.NONE )
class NIManagedSubnets {
  @XmlAttribute String name
  @XmlElement(name="managedSubnet") NIManagedSubnet managedSubnet
}

@Canonical
@CompileStatic
@XmlAccessorType( XmlAccessType.NONE )
class NIManagedSubnet {
  @XmlAttribute String name
  @XmlElement(name="property") List<NIProperty> properties = Lists.newArrayList()
}

@Canonical
@CompileStatic
@XmlAccessorType( XmlAccessType.NONE )
class NIClusters {
  @XmlAttribute String name
  @XmlElement(name="cluster") List<NICluster> clusters = Lists.newArrayList()
}

@Canonical
@CompileStatic
@XmlAccessorType( XmlAccessType.NONE )
class NICluster {
  @XmlAttribute String name // partition
  @XmlElement NISubnet subnet
  @XmlElement(name="property") List<NIProperty> properties = Lists.newArrayList()
  @XmlElement(name="property") NINodes nodes
}

@Canonical
@CompileStatic
@XmlAccessorType( XmlAccessType.NONE )
class NINodes {
  @XmlAttribute String name
  @XmlElement(name="node") List<NINode> nodes = Lists.newArrayList()
}

@Canonical
@CompileStatic
@XmlAccessorType( XmlAccessType.NONE )
class NINode {
  @XmlAttribute String name
  @XmlElementWrapper(name="instanceIds") @XmlElement(name="value") List<String> instanceIds = Lists.newArrayList()
}

@Canonical
@CompileStatic
@XmlAccessorType( XmlAccessType.NONE )
class NIInstance {
  @XmlAttribute String name
  @XmlElement String ownerId
  @XmlElement String macAddress
  @XmlElement String publicIp
  @XmlElement String privateIp
  @XmlElement String vpc
  @XmlElement String subnet
  @XmlElementWrapper @XmlElement(name="networkInterface") List<NINetworkInterface> networkInterfaces = Lists.newArrayList( )
  @XmlElementWrapper @XmlElement(name="value") List<String> securityGroups = Lists.newArrayList( )
}

@Canonical
@CompileStatic
@XmlAccessorType( XmlAccessType.NONE )
class NIVpc {
  @XmlAttribute String name
  @XmlElement String ownerId
  @XmlElement String cidr
  @XmlElement String dhcpOptionSet
  @XmlElementWrapper @XmlElement(name="subnet") List<NIVpcSubnet> subnets = Lists.newArrayList( )
  @XmlElementWrapper @XmlElement(name="networkAcl") List<NINetworkAcl> networkAcls = Lists.newArrayList( )
  @XmlElementWrapper @XmlElement(name="routeTable") List<NIRouteTable> routeTables = Lists.newArrayList( )
  @XmlElementWrapper @XmlElement(name="natGateway") List<NINatGateway> natGateways = Lists.newArrayList( )
  @XmlElementWrapper @XmlElement(name="value") List<String> internetGateways = Lists.newArrayList( )
}

@Canonical
@CompileStatic
@XmlAccessorType( XmlAccessType.NONE )
class NIVpcSubnet {
  @XmlAttribute String name
  @XmlElement String ownerId
  @XmlElement String cidr
  @XmlElement String cluster
  @XmlElement String networkAcl
  @XmlElement String routeTable
}

@Canonical
@CompileStatic
@XmlAccessorType( XmlAccessType.NONE )
class NINetworkInterface {
  @XmlAttribute String name
  @XmlElement String ownerId
  @XmlElement Integer deviceIndex
  @XmlElement String attachmentId
  @XmlElement String macAddress
  @XmlElement String privateIp
  @XmlElement String publicIp
  @XmlElement Boolean sourceDestCheck
  @XmlElement String vpc
  @XmlElement String subnet
  @XmlElementWrapper @XmlElement(name="value") List<String> securityGroups = Lists.newArrayList( )
}

@Canonical
@CompileStatic
@XmlAccessorType( XmlAccessType.NONE )
class NINetworkAcl {
  @XmlAttribute String name
  @XmlElement String ownerId
  @XmlElementWrapper @XmlElement(name="entry") List<NINetworkAclEntry> ingressEntries = Lists.newArrayList( )
  @XmlElementWrapper @XmlElement(name="entry") List<NINetworkAclEntry> egressEntries = Lists.newArrayList( )
}

@Canonical
@CompileStatic
@XmlAccessorType( XmlAccessType.NONE )
class NINetworkAclEntry {
  @XmlAttribute Integer number
  @XmlElement Integer protocol
  @XmlElement String action
  @XmlElement String cidr
  @XmlElement Integer icmpCode
  @XmlElement Integer icmpType
  @XmlElement Integer portRangeFrom
  @XmlElement Integer portRangeTo
}

@Canonical
@CompileStatic
@XmlAccessorType( XmlAccessType.NONE )
class NIRouteTable {
  @XmlAttribute String name
  @XmlElement String ownerId
  @XmlElementWrapper @XmlElement(name="route") List<NIRoute> routes = Lists.newArrayList( )
}

@Canonical
@CompileStatic
@XmlAccessorType( XmlAccessType.NONE )
class NIRoute {
  @XmlElement String destinationCidr
  @XmlElement String gatewayId
  @XmlElement String networkInterfaceId
  @XmlElement String natGatewayId
}

@Canonical
@CompileStatic
@XmlAccessorType( XmlAccessType.NONE )
class NIDhcpOptionSet {
  @XmlAttribute String name
  @XmlElement String ownerId
  @XmlElement(name="property") List<NIProperty> properties = Lists.newArrayList()
}

@Canonical
@CompileStatic
@XmlAccessorType( XmlAccessType.NONE )
class NIInternetGateway {
  @XmlAttribute String name
  @XmlElement String ownerId
}

@Canonical
@CompileStatic
@XmlAccessorType( XmlAccessType.NONE )
class NINatGateway {
  @XmlAttribute String name
  @XmlElement String ownerId
  @XmlElement String macAddress
  @XmlElement String publicIp
  @XmlElement String privateIp
  @XmlElement String vpc
  @XmlElement String subnet
}

@Canonical
@CompileStatic
@XmlAccessorType( XmlAccessType.NONE )
class NISecurityGroup {
  @XmlAttribute String name
  @XmlElement String ownerId
  @XmlElementWrapper @XmlElement(name="rule") List<NISecurityGroupIpPermission> ingressRules = Lists.newArrayList()
  @XmlElementWrapper @XmlElement(name="rule") List<NISecurityGroupIpPermission> egressRules = Lists.newArrayList()
}

@Canonical
@CompileStatic
@XmlAccessorType( XmlAccessType.NONE )
class NISecurityGroupIpPermission {
  @XmlElement Integer protocol
  @XmlElement Integer fromPort
  @XmlElement Integer toPort
  @XmlElement Integer icmpType
  @XmlElement Integer icmpCode
  @XmlElement String groupId
  @XmlElement String groupOwnerId
  @XmlElement String cidr
}

