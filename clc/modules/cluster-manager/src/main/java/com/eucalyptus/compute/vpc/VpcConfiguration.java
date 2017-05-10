/*************************************************************************
 * Copyright 2009-2016 Eucalyptus Systems, Inc.
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

import java.util.List;
import java.util.function.Function;
import com.eucalyptus.compute.common.internal.vpc.Vpcs;
import com.eucalyptus.configurable.ConfigurableClass;
import com.eucalyptus.configurable.ConfigurableField;
import com.eucalyptus.configurable.ConfigurableProperty;
import com.eucalyptus.configurable.ConfigurablePropertyException;
import com.eucalyptus.configurable.PropertyChangeListener;
import com.eucalyptus.util.Cidr;
import com.eucalyptus.util.FUtils;
import com.google.common.base.CharMatcher;
import com.google.common.base.MoreObjects;
import com.google.common.base.Optional;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Range;

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

  @ConfigurableField( initial = Vpcs.DEFAULT_VPC_CIDR, description = "CIDR to use when creating default VPCs.",
      changeListener = DefaultVpcCidrChangeListener.class )
  public static volatile String defaultVpcCidr = Vpcs.DEFAULT_VPC_CIDR;

  @ConfigurableField( description = "Comma separated list of reserved CIDRs.",
      changeListener = CidrListChangeListener.class )
  public static volatile String reservedCidrs = "";

  private static final Splitter LIST_SPLITTER =
      Splitter.on( CharMatcher.anyOf(", ;:") ).trimResults( ).omitEmptyStrings( );

  private static final Function<String,List<Cidr>> RESERVED_CIDR_TRANSFORM =
      FUtils.memoizeLast( VpcConfiguration::toCidrs );

  private static List<Cidr> toCidrs( final String cidrValues ) {
    //noinspection StaticPseudoFunctionalStyleMethod
    return Lists.newArrayList( Optional.presentInstances(
        Iterables.transform( LIST_SPLITTER.split( cidrValues ), Cidr.parse( ) ) ) );
  }

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

  public static String getDefaultVpcCidr() { return MoreObjects.firstNonNull( defaultVpcCidr, Vpcs.DEFAULT_VPC_CIDR ); }

  public static List<Cidr> getReservedCidrs() { return RESERVED_CIDR_TRANSFORM.apply( reservedCidrs ); }

  public static class DefaultVpcCidrChangeListener implements PropertyChangeListener<String> {

    @Override
    public void fireChange( final ConfigurableProperty t, final String newValue ) throws ConfigurablePropertyException {
      if ( newValue != null ) {
        final Cidr cidr = Cidr.parse( ).apply( newValue ).orNull( );
        if ( cidr == null ) {
          throw new ConfigurablePropertyException( "Invalid cidr: " + newValue );
        }
        if ( !Range.closed( 16, 24 ).contains( cidr.getPrefix( ) ) ) {
          throw new ConfigurablePropertyException( "Invalid cidr range (/16-24): " + newValue );
        }
      }
    }
  }

  public static class CidrListChangeListener implements PropertyChangeListener<String> {
    @Override
    public void fireChange( final ConfigurableProperty t, final String newValue ) throws ConfigurablePropertyException {
      if ( !Strings.isNullOrEmpty( newValue ) ) {
        for ( final String cidr : LIST_SPLITTER.split( newValue ) ) {
          if ( !Cidr.parse( ).apply( cidr ).isPresent( ) ) {
            throw new ConfigurablePropertyException( "Invalid cidr: " + cidr );
          }
        }
      }
    }
  }
}
