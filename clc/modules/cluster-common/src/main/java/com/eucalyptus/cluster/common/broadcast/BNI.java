/*************************************************************************
 * Copyright 2009-2014 Ent. Services Development Corporation LP
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
package com.eucalyptus.cluster.common.broadcast;

import java.io.IOException;
import java.util.function.Predicate;
import com.eucalyptus.cluster.common.broadcast.impl.Mapping;
import com.eucalyptus.util.FUtils;
import com.eucalyptus.util.Lens;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.vavr.collection.Array;
import io.vavr.control.Option;

/**
 *
 */
public interface BNI {

  static String toString( BNetworkInfo info ) throws IOException {
    return Mapping.mapper( ).writeValueAsString( info );
  }
  
  static String toFormattedString( BNetworkInfo info ) throws IOException {
    return Mapping.mapper( ).configure( SerializationFeature.INDENT_OUTPUT, true ).writeValueAsString( info );
  }

  static BNetworkInfo parse( final String text ) throws IOException {
    return Mapping.mapper( ).readValue( text, BNetworkInfo.class );
  }

  /**
   * TODO clean up configuration properties (nodeArrayLens also needs work)
   */
  static Lens<BNetworkInfo,Array<BNICluster>> clusterArrayLens( ) {
    return configurationLens( )
        .compose( Lens.of(
            BNIConfiguration::clusters,
            clusters -> (BNIConfiguration configuration) -> {
              ImmutableBNIConfiguration.Builder configurationBuilder = configuration( );
              configurationBuilder.properties( Array.ofAll( configuration.simpleProperties( ) ) );
              for ( final BNIMidonet midonet : configuration.midonet( ) ) {
                configurationBuilder.property( midonet );
              }
              for ( final BNISubnets subnets : configuration.subnets( ) ) {
                configurationBuilder.property( subnets );
              }
              for ( final BNIManagedSubnets subnets : configuration.managedSubnets( ) ) {
                configurationBuilder.property( subnets );
              }
              for ( final BNIClusters bniCluster : clusters ) {
                configurationBuilder.property( bniCluster );
              }
              return configurationBuilder.o( );
            } ) )
        .compose( Lens.of(
            clustersOption -> clustersOption.map( BNIClusters::clusters ).getOrElse( Array::empty ),
            clusterArray -> (Option<BNIClusters> clustersOption) ->
                clustersOption.isDefined( ) ?
                    Option.of( clusters( ).from( clustersOption.get( ) ).clusters( clusterArray ).o( ) ) :
                    clustersOption
        ) );
  }

  static Lens<BNetworkInfo,BNIConfiguration> configurationLens( ) {
    return Lens.of(
        BNetworkInfo::configuration,
        configuration -> info -> networkInfo( ).from( info ).configuration( configuration ).o( )
    );
  }

  static Lens<BNetworkInfo,Array<BNIDhcpOptionSet>> dhcpOptionSetsLens( ) {
    return Lens.of(
        BNetworkInfo::dhcpOptionSets,
        dhcpOptionSets -> info -> networkInfo( ).from( info ).dhcpOptionSets( dhcpOptionSets ).o( )
    );
  }

  static Lens<BNetworkInfo,Array<BNIInstance>> instancesLens( ) {
    return Lens.of(
        BNetworkInfo::instances,
        instances -> info -> networkInfo( ).from( info ).instances( instances ).o( )
    );
  }

  static Lens<BNetworkInfo,Array<BNIInternetGateway>> internetGatewaysLens( ) {
    return Lens.of(
        BNetworkInfo::internetGateways,
        internetGateways -> info -> networkInfo( ).from( info ).internetGateways( internetGateways ).o( )
    );
  }

  static Lens<BNICluster,Array<BNINode>> nodeArrayLens( ) {
    return Lens.of(
        BNICluster::nodes,
        nodesOption -> cluster -> nodesOption.isDefined( ) ?
            cluster( ).from( cluster ).properties(
                cluster.properties( ).filter( ((Predicate<Object>)BNINodes.class::isInstance).negate() )
            ).property( nodesOption.get( ) ).o( ) :
            cluster )
        .compose( Lens.of(
            nodesOption -> nodesOption.map( BNINodes::nodes ).getOrElse( Array::empty ),
            nodeArray -> (Option<BNINodes> nodesOption) ->
                nodesOption.isDefined( ) ?
                    Option.of( nodes( ).from( nodesOption.get( ) ).nodes( nodeArray ).o( ) ) :
                    nodesOption

        ) );
  }

  static Lens<BNetworkInfo,Array<BNISecurityGroup>> securityGroupLens( ) {
    return Lens.of(
        BNetworkInfo::securityGroups,
        securityGroups -> info -> networkInfo( ).from( info ).securityGroups( securityGroups ).o( )
    );
  }

  static Lens<BNetworkInfo,Array<BNIVpc>> vpcsLens( ) {
    return Lens.of(
        BNetworkInfo::vpcs,
        vpcs -> info -> networkInfo( ).from( info ).vpcs( vpcs ).o( )
    );
  }

  static ImmutableBNICluster.Builder cluster( ) {
    return ImmutableBNICluster.builder( );
  }

  static ImmutableBNIClusters.Builder clusters( ) {
    return ImmutableBNIClusters.builder( );
  }

  static ImmutableBNIConfiguration.Builder configuration( ) {
    return ImmutableBNIConfiguration.builder( );
  }

  static ImmutableBNIDhcpOptionSet.Builder dhcpOptionSet( ) {
    return ImmutableBNIDhcpOptionSet.builder( );
  }

  static ImmutableBNIMidonetGateway.Builder gateway( ) {
    return ImmutableBNIMidonetGateway.builder( );
  }

  static ImmutableBNIMidonetGateways.Builder gateways( ) {
    return ImmutableBNIMidonetGateways.builder( );
  }

  static ImmutableBNIInstance.Builder instance( ) {
    return ImmutableBNIInstance.builder( );
  }

  static ImmutableBNIInternetGateway.Builder internetGateway( ) {
    return ImmutableBNIInternetGateway.builder( );
  }

  static ImmutableBNIManagedSubnet.Builder managedSubnet( ) {
    return ImmutableBNIManagedSubnet.builder( );
  }

  static ImmutableBNIManagedSubnets.Builder managedSubnets( ) {
    return ImmutableBNIManagedSubnets.builder( );
  }

  static ImmutableBNIMidonet.Builder midonet( ) {
    return ImmutableBNIMidonet.builder( );
  }

  static ImmutableBNINatGateway.Builder natGateway( ) {
    return ImmutableBNINatGateway.builder( );
  }

  static ImmutableBNINetworkAclEntries.Builder networkAclEntries( ) {
    return ImmutableBNINetworkAclEntries.builder( );
  }

  static BNINetworkAclEntries networkAclEntries( Iterable<BNINetworkAclEntry> entries ) {
    return networkAclEntries( ).addAllEntries( entries ).o( );
  }

  static ImmutableBNINetworkAclEntry.Builder networkAclEntry( ) {
    return ImmutableBNINetworkAclEntry.builder( );
  }

  static ImmutableBNINode.Builder node( ) {
    return ImmutableBNINode.builder( );
  }

  static ImmutableBNINodes.Builder nodes( ) {
    return ImmutableBNINodes.builder( );
  }

  static ImmutableBNINetworkAcl.Builder networkAcl( ) {
    return ImmutableBNINetworkAcl.builder( );
  }

  static ImmutableBNetworkInfo.Builder networkInfo( ) {
    return ImmutableBNetworkInfo.builder( );
  }

  static ImmutableBNINetworkInterface.Builder networkInterface( ) {
    return ImmutableBNINetworkInterface.builder( );
  }

  static ImmutableBNIProperty.Builder property( ) {
    return ImmutableBNIProperty.builder( );
  }

  static BNIProperty property( final String name, final String value ) {
    return ImmutableBNIProperty.builder( ).name( name ).value( value ).o( );
  }

  static ImmutableBNIRoute.Builder route( ) {
    return ImmutableBNIRoute.builder( );
  }

  static ImmutableBNIRouteTable.Builder routeTable( ) {
    return ImmutableBNIRouteTable.builder( );
  }

  static ImmutableBNISecurityGroup.Builder securityGroup( ) {
    return ImmutableBNISecurityGroup.builder( );
  }

  static ImmutableBNISecurityGroupIpPermission.Builder securityGroupIpPermission( ) {
    return ImmutableBNISecurityGroupIpPermission.builder( );
  }

  static ImmutableBNISecurityGroupRules.Builder securityGroupRules( ) {
    return ImmutableBNISecurityGroupRules.builder( );
  }

  static ImmutableBNISubnet.Builder subnet( ) {
    return ImmutableBNISubnet.builder( );
  }

  static ImmutableBNISubnets.Builder subnets( ) {
    return ImmutableBNISubnets.builder( );
  }

  static ImmutableBNIVpc.Builder vpc( ) {
    return ImmutableBNIVpc.builder( );
  }

  static ImmutableBNIVpcSubnet.Builder vpcSubnet( ) {
    return ImmutableBNIVpcSubnet.builder( );
  }
}
