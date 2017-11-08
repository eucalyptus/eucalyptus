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
package com.eucalyptus.compute.vpc;

import com.eucalyptus.configurable.ConfigurableClass;
import com.eucalyptus.configurable.ConfigurableField;

/**
 *
 */
@ConfigurableClass(root = "cloud.vpc", description = "Parameters controlling VPC")
public class VpcConfiguration {

  @ConfigurableField( initial = "200", description = "Maximum number of subnets for each VPC." )
  public static volatile int subnetsPerVpc = 200;

  @ConfigurableField( initial = "200", description = "Maximum number of network ACLs for each VPC." )
  public static volatile int networkAclsPerVpc = 200;

  @ConfigurableField( initial = "20", description = "Maximum number of rules per direction for each network ACL." )
  public static volatile int rulesPerNetworkAcl = 20;

  @ConfigurableField( initial = "200", description = "Maximum number of route tables for each VPC." )
  public static volatile int routeTablesPerVpc = 200;

  @ConfigurableField( initial = "50", description = "Maximum number of routes for each route table." )
  public static volatile int routesPerTable = 50;

  @ConfigurableField( initial = "100", description = "Maximum number of security groups for each VPC." )
  public static volatile int securityGroupsPerVpc = 100;

  @ConfigurableField( initial = "50", description = "Maximum number of associated security groups for each network interface ." )
  public static volatile int rulesPerSecurityGroup = 50;

  @ConfigurableField( initial = "5", description = "Maximum number of associated security groups for each network interface ." )
  public static volatile int securityGroupsPerNetworkInterface = 5;

  @ConfigurableField( initial = "5", description = "Maximum number of NAT gateways for each availability zone." )
  public static volatile int natGatewaysPerAvailabilityZone = 5;

  @ConfigurableField( initial = "true", description = "Enable default VPC." )
  public static volatile boolean defaultVpc = true;

  public static int getSubnetsPerVpc() {
    return subnetsPerVpc;
  }

  public static int getNetworkAclsPerVpc() {
    return networkAclsPerVpc;
  }

  public static int getRulesPerNetworkAcl() {
    return rulesPerNetworkAcl;
  }

  public static int getRouteTablesPerVpc() {
    return routeTablesPerVpc;
  }

  public static int getRoutesPerTable() {
    return routesPerTable;
  }

  public static int getSecurityGroupsPerVpc() {
    return securityGroupsPerVpc;
  }

  public static int getRulesPerSecurityGroup() {
    return rulesPerSecurityGroup;
  }

  public static int getSecurityGroupsPerNetworkInterface() {
    return securityGroupsPerNetworkInterface;
  }

  public static int getNatGatewaysPerAvailabilityZone() { return natGatewaysPerAvailabilityZone; }

  public static boolean getDefaultVpc() { return defaultVpc; }
}
