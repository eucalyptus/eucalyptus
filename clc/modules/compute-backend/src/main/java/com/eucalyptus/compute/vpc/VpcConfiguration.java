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
