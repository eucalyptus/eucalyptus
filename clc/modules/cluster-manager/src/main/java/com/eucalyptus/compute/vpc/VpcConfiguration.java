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

  public static boolean getDefaultVpc() { return defaultVpc; }
}
