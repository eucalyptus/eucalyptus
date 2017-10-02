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
package com.eucalyptus.network

import com.eucalyptus.cluster.common.Cluster
import com.eucalyptus.cluster.common.broadcast.*
import com.eucalyptus.cluster.common.msgs.NodeInfo
import com.eucalyptus.compute.common.internal.network.NetworkGroup
import com.eucalyptus.compute.common.internal.network.NetworkPeer
import com.eucalyptus.compute.common.internal.network.NetworkRule
import com.eucalyptus.compute.common.internal.vm.VmInstance
import com.eucalyptus.compute.common.internal.vm.VmNetworkConfig
import com.eucalyptus.compute.common.internal.vpc.*
import com.eucalyptus.compute.common.internal.vpc.Subnet as ComputeSubnet
import com.eucalyptus.compute.vpc.RouteKey
import com.eucalyptus.network.config.*
import com.eucalyptus.network.config.Cluster as ConfigCluster
import com.eucalyptus.util.CollectionUtils
import com.eucalyptus.util.CompatFunction
import com.eucalyptus.util.CompatPredicate
import com.eucalyptus.util.Lens
import com.eucalyptus.util.Strings as EucaStrings
import com.eucalyptus.util.TypeMapper
import com.eucalyptus.util.TypeMappers
import com.eucalyptus.vm.VmInstances
import com.google.common.base.*
import com.google.common.collect.*
import groovy.transform.CompileStatic
import groovy.transform.Immutable
import groovy.transform.PackageScope
import io.vavr.collection.Array
import io.vavr.control.Option
import org.apache.log4j.Logger

import javax.annotation.Nullable

import static com.eucalyptus.cluster.common.broadcast.BNI.*
import static com.eucalyptus.compute.common.internal.vm.VmInstance.VmStateSet.TORNDOWN
import static com.google.common.collect.Iterables.tryFind

@PackageScope
@CompileStatic
class NetworkInfoBroadcasts {

  private static final Logger logger = Logger.getLogger( NetworkInfoBroadcasts )

  protected static volatile boolean MODE_CLEAN =
      Boolean.valueOf( System.getProperty( 'com.eucalyptus.network.broadcastModeClean', 'true' ) )

  /**
   * All state used for building network configuration must be passed in (and be part of the fingerprint)
   */
  @PackageScope
  static BNetworkInfo buildNetworkConfiguration( final Optional<NetworkConfiguration> configuration,
                                                 final NetworkInfoSource networkInfoSource,
                                                 final String version,
                                                 final Supplier<List<Cluster>> clusterSupplier,
                                                 final Supplier<List<Cluster>> otherClusterSupplier,
                                                 final Supplier<String> clcHostSupplier,
                                                 final Function<List<String>,List<String>> systemNameserverLookup,
                                                 final Set<String> dirtyPublicAddresses,
                                                 final Set<RouteKey> invalidStateRoutes /*out*/ ) {
    boolean vpcmido = 'VPCMIDO' == configuration.orNull()?.mode
    boolean managed = ( ( 'MANAGED' == configuration.orNull()?.mode ) || ( 'MANAGED-NOVLAN' == configuration.orNull()?.mode ) )
    Iterable<Cluster> clusters = vpcmido ?
        Lists.newArrayList( Iterables.filter( Iterables.concat( clusterSupplier.get( ), otherClusterSupplier.get( ) ), uniquePartitionPredicate( ) ) ) :
        clusterSupplier.get( )
    Optional<NetworkConfiguration> networkConfiguration = configuration.isPresent( ) ?
        Optional.of( NetworkConfigurations.explode( configuration.get( ), clusters.collect{ Cluster cluster -> cluster.partition } ) ) :
        configuration
    ImmutableBNetworkInfo.Builder info = networkConfiguration
        .transform( TypeMappers.lookup( NetworkConfiguration, ImmutableBNetworkInfo.Builder ) )
        .or( networkInfo( ).configuration( BNI.configuration( ).o( ) ) )
    info.setValueVersion( version )
    ImmutableBNIConfiguration.Builder bniConfiguration = BNI.configuration( )
        .addAllProperties( info.o( ).configuration( ).simpleProperties( ) as Iterable<BNIPropertyBase> )

    // populate clusters
    BNIClusters bniClusters = BNI.clusters()
        .name( 'clusters' )
        .setIterableClusters( clusters.findResults{ Cluster cluster ->
          ConfigCluster configCluster = networkConfiguration.orNull()?.clusters?.find{ ConfigCluster configCluster -> cluster.partition == configCluster.name }
          configCluster && ( vpcmido || configCluster.subnet ) ?
              BNI.cluster( )
                  .name( configCluster.name )
                  .setValueSubnet( vpcmido ?
                    subnet( )
                      .name( '172.31.0.0' )
                      .property( property( 'subnet', '172.31.0.0' ) )
                      .property( property( 'netmask', '255.255.0.0' ) )
                      .property( property( 'gateway', '172.31.0.1' ) )
                      .o( ) :
                    subnet( )
                      .name( configCluster.subnet.subnet ) // broadcast name is always the subnet value
                      .property( property('subnet', configCluster.subnet.subnet ) )
                      .property( property('netmask', configCluster.subnet.netmask ) )
                      .property( property('gateway', configCluster.subnet.gateway ) )
                      .o( )
                  )
                  .property( property( 'enabledCCIp', InetAddress.getByName(cluster.hostName).hostAddress ) )
                  .property( property( 'macPrefix', configCluster.macPrefix ) )
                  .property( vpcmido ?
                      property( 'privateIps', '172.31.0.5' ) :
                      property( ).name( 'privateIps' ).setIterableValues( configCluster.privateIps ).o( ) )
                  .property( nodes( )
                      .name( 'nodes' )
                      .setIterableNodes( cluster.nodeMap.values().collect{ NodeInfo nodeInfo -> node( ).name( nodeInfo.name ).o( ) } as Iterable<BNINode> )
                      .o( ) )
                  .o( )

              :
              configCluster && managed ? BNI.cluster( )
                  .name( configCluster.name )
                  .property( property('enabledCCIp', InetAddress.getByName(cluster.hostName).hostAddress ) )
                  .property( property('macPrefix', configCluster.macPrefix ) )
                  .property( nodes( )
                      .name( 'nodes' )
                      .setIterableNodes( cluster.nodeMap.values().collect{ NodeInfo nodeInfo -> node( ).name( nodeInfo.name ).o( ) } as Iterable<BNINode> )
                      .o( ) )
                  .o( )
              :
              null
        } as List<BNICluster> )
        .o( )

    // populate dynamic properties
    List<String> dnsServers = networkConfiguration.orNull()?.instanceDnsServers?:systemNameserverLookup.apply(['127.0.0.1'])
    bniConfiguration
      .property( property( 'enabledCLCIp', clcHostSupplier.get() ) )
      .property( property( 'instanceDNSDomain', networkConfiguration.orNull()?.instanceDnsDomain?:EucaStrings.trimPrefix('.',"${VmInstances.INSTANCE_SUBDOMAIN}.internal") ) )
      .property( property( ).name('instanceDNSServers').setIterableValues(dnsServers ).o( ) )


    boolean hasEdgePublicGateway = networkConfiguration.orNull()?.publicGateway != null
    if ( hasEdgePublicGateway ) {
      bniConfiguration
          .property( property( 'publicGateway', networkConfiguration.orNull()?.publicGateway ) )
    }

    Iterable<VmInstanceNetworkView> instances = Iterables.filter(
        networkInfoSource.instances,
        { VmInstanceNetworkView instance -> !TORNDOWN.contains(instance.state) && !instance.omit } as Predicate<VmInstanceNetworkView> )

    // populate nodes
    ImmutableBNIClusters.Builder clustersBuilder = BNI.clusters( ).name( bniClusters.name( ) )
    Map<List<String>,Collection<String>> nodeInstanceMap = ((Multimap<List<String>,String>) instances.inject( HashMultimap.create( ) ){
      Multimap<List<String>,String> map, VmInstanceNetworkView instance ->
        map.put( [ instance.partition, instance.node ], instance.id )
        map
    }).asMap()
    bniClusters.clusters( ).each { BNICluster cluster ->
      ImmutableBNICluster.Builder clusterBuilder = BNI.cluster( )
        .name( cluster.name( ) )
        .subnet( cluster.subnet( ) )
        .properties( cluster.simpleProperties( ) as Array<BNIPropertyBase> )
      cluster.nodes( ).each { BNINodes node ->
        clusterBuilder.property( nodes( )
                      .name( 'nodes' )
                      .setIterableNodes( node.nodes( ).collect{ BNINode bniNode ->
                        Collection<String> instanceIds = nodeInstanceMap.get( [ cluster.name( ), bniNode.name( ) ] )
                        instanceIds ?
                          BNI.node( ).from( bniNode ).setIterableInstanceIds( instanceIds ).o( ) :
                          bniNode
                      } )
                      .o( ) )
      }
      clustersBuilder.cluster( clusterBuilder.o( ) )
    }
    for ( BNIMidonet midonet : info.o( ).configuration( ).midonet( ) ) {
      bniConfiguration.property( midonet )
    }
    for ( BNISubnets subnets : info.o( ).configuration( ).subnets( ) ) {
      bniConfiguration.property( subnets )
    }
    for ( BNIManagedSubnets subnets : info.o( ).configuration( ).managedSubnets( ) ) {
      bniConfiguration.property( subnets )
    }
    bniConfiguration.property( clustersBuilder.o( ) );
    info.configuration( bniConfiguration.o( ) );

    // populate vpcs
    Iterable<VpcNetworkView> vpcs = networkInfoSource.vpcs
    Iterable<SubnetNetworkView> subnets = networkInfoSource.subnets
    Iterable<NetworkAclNetworkView> networkAcls = networkInfoSource.networkAcls
    Iterable<RouteTableNetworkView> routeTables = networkInfoSource.routeTables
    Iterable<InternetGatewayNetworkView> internetGateways = networkInfoSource.internetGateways
    Iterable<NatGatewayNetworkView> natGateways = networkInfoSource.natGateways
    Iterable<NetworkInterfaceNetworkView> networkInterfaces = networkInfoSource.networkInterfaces
    Set<String> activeInstances = (Set<String>) instances.inject( Sets.newHashSetWithExpectedSize( 500 ) ) {
      Set<String> instanceIds, VmInstanceNetworkView inst -> instanceIds.add( inst.id ); instanceIds
    }
    Set<String> activeVpcs = (Set<String>) networkInterfaces.inject( Sets.newHashSetWithExpectedSize( 500 ) ){
      Set<String> vpcIds, NetworkInterfaceNetworkView networkInterface ->
        if ( networkInterface.instanceId && activeInstances.contains( networkInterface.instanceId ) ) {
          vpcIds.add( networkInterface.vpcId )
        }
        vpcIds
    }
    natGateways.forEach( { NatGatewayNetworkView natGateway ->
      if ( natGateway.state == NatGateway.State.available ) activeVpcs.add( natGateway.vpcId )
    } )
    Set<String> activeDhcpOptionSets = (Set<String>) vpcs.inject( Sets.newHashSetWithExpectedSize( 500 ) ) {
      Set<String> dhcpOptionSetIds, VpcNetworkView vpc ->
        if ( activeVpcs.contains( vpc.id ) && vpc.dhcpOptionSetId ) {
          dhcpOptionSetIds.add( vpc.dhcpOptionSetId )
        }
        dhcpOptionSetIds
    }
    Map<String,Collection<String>> vpcIdToInternetGatewayIds = (Map<String,Collection<String>> ) ((ArrayListMultimap<String,String>)internetGateways.inject(ArrayListMultimap.<String,String>create()){
      ListMultimap<String,String> map, InternetGatewayNetworkView internetGateway ->
        if ( internetGateway.vpcId ) map.put( internetGateway.vpcId, internetGateway.id )
        map
    }).asMap( )
    Predicate<RouteNetworkView> activeRoutePredicate =
        activeRoutePredicate( internetGateways, natGateways, instances, networkInterfaces, invalidStateRoutes )
    info.addAllVpcs( vpcs.findAll{ VpcNetworkView vpc -> activeVpcs.contains(vpc.id) }.collect{ Object vpcViewObj ->
      final VpcNetworkView vpc = vpcViewObj as VpcNetworkView
      BNI.vpc( )
          .name( vpc.id )
          .ownerId( vpc.ownerAccountNumber )
          .cidr( vpc.cidr )
          .dhcpOptionSet( Option.of( vpc.dhcpOptionSetId ) )
          .addAllSubnets( subnets.findAll{ SubnetNetworkView subnet -> subnet.vpcId == vpc.id }.collect{ Object subnetViewObj ->
            SubnetNetworkView subnet = subnetViewObj as SubnetNetworkView
            vpcSubnet( )
                .name( subnet.id )
                .ownerId( subnet.ownerAccountNumber )
                .cidr( subnet.cidr )
                .cluster( subnet.availabilityZone )
                .networkAcl( Option.of( subnet.networkAcl ) )
                .routeTable(
                    tryFind( routeTables, { RouteTableNetworkView routeTable -> routeTable.subnetIds.contains( subnet.id ) } as Predicate<RouteTableNetworkView>).or(
                        Iterables.find( routeTables, { RouteTableNetworkView routeTable -> routeTable.main && routeTable.vpcId == vpc.id } as Predicate<RouteTableNetworkView> ) ).id )
                .o( )
          } as Iterable<BNIVpcSubnet> )
          .addAllNetworkAcls( networkAcls.findAll{ NetworkAclNetworkView networkAcl -> networkAcl.vpcId == vpc.id }.collect { Object networkAclObj ->
            NetworkAclNetworkView networkAcl = networkAclObj as NetworkAclNetworkView
            BNI.networkAcl( )
                .name( networkAcl.id )
                .ownerId( networkAcl.ownerAccountNumber )
                .setValueIngressEntries( networkAclEntries( Lists.transform( networkAcl.ingressRules, TypeMappers.lookup( NetworkAclEntryNetworkView, BNINetworkAclEntry ) ) ) )
                .setValueEgressEntries( networkAclEntries( Lists.transform( networkAcl.egressRules, TypeMappers.lookup( NetworkAclEntryNetworkView, BNINetworkAclEntry ) ) ) )
                .o( )

          } as Iterable<BNINetworkAcl> )
          .addAllRouteTables( routeTables.findAll{ RouteTableNetworkView routeTable -> routeTable.vpcId == vpc.id }.collect { Object routeTableObj ->
            RouteTableNetworkView routeTable = routeTableObj as RouteTableNetworkView
            BNI.routeTable( )
                .name( routeTable.id )
                .ownerId( routeTable.ownerAccountNumber )
                .addAllRoutes( Lists.newArrayList( Iterables.transform( Iterables.filter( routeTable.routes, activeRoutePredicate ), TypeMappers.lookup( RouteNetworkView, BNIRoute ) ) ) )
                .o( )
          } as Iterable<BNIRouteTable> )
          .addAllNatGateways( natGateways.findAll{ NatGatewayNetworkView natGateway -> natGateway.vpcId == vpc.id && natGateway.state == NatGateway.State.available }.collect{ Object natGatewayObj ->
            NatGatewayNetworkView natGateway = natGatewayObj as NatGatewayNetworkView
            BNI.natGateway( )
                .name( natGateway.id )
                .ownerId( natGateway.ownerAccountNumber )
                .vpc( natGateway.vpcId )
                .subnet( natGateway.subnetId )
                .macAddress( Option.of( Strings.emptyToNull( natGateway.macAddress ) ) )
                .publicIp( Option.of( VmNetworkConfig.DEFAULT_IP==natGateway.publicIp||dirtyPublicAddresses.contains(natGateway.publicIp) ? null : natGateway.publicIp ) )
                .privateIp( Option.of( natGateway.privateIp ) )
                .o( )
          } as Iterable<BNINatGateway> )
          .addAllInternetGateways( vpcIdToInternetGatewayIds.get( vpc.id ) as List<String>?:[] as List<String> )
          .o( )
    } as Iterable<BNIVpc> )
    vpcs.findAll{ VpcNetworkView vpc -> !activeVpcs.contains(vpc.id) }.each { Object vpcViewObj -> // processing for any inactive vpcs
      final VpcNetworkView vpc = vpcViewObj as VpcNetworkView
      routeTables.findAll{ RouteTableNetworkView routeTable -> routeTable.vpcId == vpc.id }.each { Object routeTableObj ->
        RouteTableNetworkView routeTable = routeTableObj as RouteTableNetworkView
        CollectionUtils.each( routeTable.routes, activeRoutePredicate )
      }
    }

    // populate instances
    Map<String,Collection<NetworkInterfaceNetworkView>> instanceIdToNetworkInterfaces = (Map<String,Collection<NetworkInterfaceNetworkView>> ) ((ArrayListMultimap<String,NetworkInterfaceNetworkView>) networkInterfaces.inject(ArrayListMultimap.<String,NetworkInterfaceNetworkView>create()){
      ListMultimap<String,NetworkInterfaceNetworkView> map, NetworkInterfaceNetworkView networkInterface ->
        if ( networkInterface.instanceId &&
            networkInterface.state != NetworkInterface.State.available &&
            networkInterface.attachmentStatus != NetworkInterfaceAttachment.Status.detaching &&
            networkInterface.attachmentStatus != NetworkInterfaceAttachment.Status.detached
        ) {
          map.put( networkInterface.instanceId, networkInterface )
        }
        map
    }).asMap( )
    info.addAllInstances( instances.collect{ VmInstanceNetworkView instance ->
      BNI.instance( )
          .name( instance.id )
          .ownerId( instance.ownerAccountNumber )
          .vpc( Option.of( instance.vpcId ) )
          .subnet( Option.of( instance.subnetId ) )
          .macAddress( Option.of(  Strings.emptyToNull( instance.macAddress ) ) )
          .publicIp( Option.of( VmNetworkConfig.DEFAULT_IP==instance.publicAddress||dirtyPublicAddresses.contains(instance.publicAddress) ? null : instance.publicAddress ) )
          .privateIp( Option.of( instance.privateAddress ) )
          .addAllSecurityGroups( Lists.newArrayList(instance.securityGroupIds) )
          .addAllNetworkInterfaces( ( instanceIdToNetworkInterfaces.get( instance.id )?.collect{ NetworkInterfaceNetworkView networkInterface ->
            BNI.networkInterface( )
                .name( networkInterface.id )
                .ownerId( networkInterface.ownerAccountNumber )
                .deviceIndex( networkInterface.deviceIndex )
                .attachmentId( networkInterface.attachmentId )
                .macAddress( Option.of( networkInterface.macAddress ) )
                .privateIp( Option.of( networkInterface.privateIp ) )
                .publicIp( Option.of( dirtyPublicAddresses.contains(networkInterface.publicIp) ? null : networkInterface.publicIp ) )
                .sourceDestCheck( networkInterface.sourceDestCheck)
                .vpc( networkInterface.vpcId )
                .subnet( networkInterface.subnetId )
                .addAllSecurityGroups( networkInterface.securityGroupIds )
                .o( )
          } ?: [ ] ) as List<BNINetworkInterface> )
          .o( )
    } as Iterable<BNIInstance> )

    // populate dhcp option sets
    Iterable<DhcpOptionSetNetworkView> dhcpOptionSets = networkInfoSource.dhcpOptionSets
    info.addAllDhcpOptionSets( dhcpOptionSets.findAll{ DhcpOptionSetNetworkView dhcpOptionSet -> activeDhcpOptionSets.contains( dhcpOptionSet.id ) }.collect { Object dhcpObj ->
      DhcpOptionSetNetworkView dhcpOptionSet = (DhcpOptionSetNetworkView) dhcpObj
      BNI.dhcpOptionSet( )
          .name( dhcpOptionSet.id )
          .ownerId( dhcpOptionSet.ownerAccountNumber )
          .addAllProperties( dhcpOptionSet.options.collect{ DhcpOptionNetworkView option ->
            if ( 'domain-name-servers' == option.key && 'AmazonProvidedDNS' == option.values?.getAt( 0 ) ) {
              property( ).name( 'domain-name-servers' ).addAllValues( dnsServers ).o( )
            } else {
              property( ).name( option.key ).addAllValues( option.values ).o( )
            }
          } as Iterable<BNIPropertyBase> )
          .o( )
    } as Iterable<BNIDhcpOptionSet> );

    // populate internet gateways
    info.addAllInternetGateways( internetGateways.findAll{ Object gatewayObj ->
      InternetGatewayNetworkView gateway = gatewayObj as InternetGatewayNetworkView
      activeVpcs.contains(gateway.vpcId)
    }.collect { Object internetGatewayObj ->
      InternetGatewayNetworkView internetGateway = internetGatewayObj as InternetGatewayNetworkView
      BNI.internetGateway( )
          .name( internetGateway.id )
          .ownerId( internetGateway.ownerAccountNumber )
          .o( )
    } as Iterable<BNIInternetGateway> )

    // populate security groups
    Iterable<NetworkGroupNetworkView> groups = networkInfoSource.securityGroups
    Set<String> securityGroupIds = (Set<String>) groups.inject( Sets.newHashSetWithExpectedSize( 1000 ) ){
      Set<String> groupIds, NetworkGroupNetworkView securityGroup -> groupIds.addAll( securityGroup.id ); groupIds
    }
    Set<String> activeSecurityGroups = (Set<String>) instances.inject( Sets.newHashSetWithExpectedSize( 1000 ) ){
      Set<String> activeGroups, VmInstanceNetworkView instance -> activeGroups.addAll( instance.securityGroupIds ); activeGroups
    }
    networkInterfaces.inject( activeSecurityGroups ){ Set<String> activeGroups, NetworkInterfaceNetworkView networkInterface ->
      if ( networkInterface.instanceId && activeInstances.contains( networkInterface.instanceId ) ) {
        activeGroups.addAll( networkInterface.securityGroupIds )
      }
      activeGroups
    }
    groups.inject( activeSecurityGroups ){  Set<String> activeGroups, NetworkGroupNetworkView securityGroup ->
      Iterables.concat( securityGroup.egressPermissions, securityGroup.ingressPermissions ).each {
        IPPermissionNetworkView permission ->
        if ( securityGroup.id != permission.groupId && securityGroupIds.contains( permission.groupId ) &&
             (!securityGroup.vpcId || activeVpcs.contains(securityGroup.vpcId))) {
          activeGroups.add( permission.groupId )
        }
      }
      activeGroups
    }
    info.addAllSecurityGroups( groups.findAll{  NetworkGroupNetworkView group -> activeSecurityGroups.contains( group.id ) }.collect{ Object groupObj ->
      NetworkGroupNetworkView group = (NetworkGroupNetworkView) groupObj
      securityGroup( )
          .name( group.id )
          .ownerId( group.ownerAccountNumber )
          .setValueIngressRules( securityGroupRules( )
            .addAllRules( group.ingressPermissions
              .findAll{ IPPermissionNetworkView ipPermission ->
                      !ipPermission.groupId || activeSecurityGroups.contains( ipPermission.groupId ) }
              .collect{ Object ipPermissionObj ->
                IPPermissionNetworkView ipPermission = (IPPermissionNetworkView) ipPermissionObj
                securityGroupIpPermission( )
                    .protocol( ipPermission.protocol )
                    .fromPort( Option.of( ipPermission.fromPort ) )
                    .toPort( Option.of( ipPermission.toPort ) )
                    .icmpType( Option.of( ipPermission.icmpType ) )
                    .icmpCode( Option.of( ipPermission.icmpCode ) )
                    .groupId( Option.of( ipPermission.groupId ) )
                    .groupOwnerId( Option.of( ipPermission.groupOwnerAccountNumber ) )
                    .cidr( Option.of( ipPermission.cidr ) )
                    .o( )
              } as Iterable<BNISecurityGroupIpPermission> )
            .o( ) )
          .setValueEgressRules( securityGroupRules( )
            .addAllRules( group.egressPermissions
              .findAll{ IPPermissionNetworkView ipPermission ->
                      !ipPermission.groupId || activeSecurityGroups.contains( ipPermission.groupId ) }
              .collect{ Object ipPermissionObj ->
                IPPermissionNetworkView ipPermission = (IPPermissionNetworkView) ipPermissionObj
                securityGroupIpPermission( )
                    .protocol( ipPermission.protocol )
                    .fromPort( Option.of( ipPermission.fromPort ) )
                    .toPort( Option.of( ipPermission.toPort ) )
                    .icmpType( Option.of( ipPermission.icmpType ) )
                    .icmpCode( Option.of( ipPermission.icmpCode ) )
                    .groupId( Option.of( ipPermission.groupId ) )
                    .groupOwnerId( Option.of( ipPermission.groupOwnerAccountNumber ) )
                    .cidr( Option.of( ipPermission.cidr ) )
                    .o( )
              } as Iterable<BNISecurityGroupIpPermission> )
            .o( ) )
          .o( )
    } as Iterable<BNISecurityGroup> )

    BNetworkInfo bni = info.o( )

    if ( MODE_CLEAN ) {
      bni = modeClean( vpcmido, bni )
    }

    if ( logger.isTraceEnabled( ) ) {
      logger.trace( "Constructed network information for ${Iterables.size( instances )} instance(s), ${Iterables.size( groups )} security group(s)" )
    }

    bni
  }

  private static Predicate<RouteNetworkView> activeRoutePredicate(
      final Iterable<InternetGatewayNetworkView> internetGateways,
      final Iterable<NatGatewayNetworkView> natGateways,
      final Iterable<VmInstanceNetworkView> instances,
      final Iterable<NetworkInterfaceNetworkView> networkInterfaces,
      final Set<RouteKey> invalidRoutes
  ) {
    final Map<String,String> networkInterfaceAndInstanceIds = networkInterfaces
        .findAll{ NetworkInterfaceNetworkView eni -> eni.instanceId }
        .collectEntries( Maps.newHashMapWithExpectedSize( 500 ) ){
      Object eniObj -> NetworkInterfaceNetworkView eni = (NetworkInterfaceNetworkView)eniObj
            [ eni.id, eni.instanceId ]
    }
    final Set<String> instanceIds = Sets.newHashSet( instances
        .findAll{ VmInstanceNetworkView vm -> vm.state == VmInstance.VmState.RUNNING }
        .collect{ Object vm -> ((VmInstanceNetworkView)vm).id } )
    final Set<String> internetGatewayIds = Sets.newHashSet( internetGateways
        .findAll{ InternetGatewayNetworkView ig -> ig.vpcId } // only attached gateways have active routes
        .collect{ Object ig -> ((InternetGatewayNetworkView)ig).id } )
    final Set<String> natGatewayIds = Sets.newHashSet( natGateways
        .findAll{ NatGatewayNetworkView ng -> ng.state == NatGateway.State.available }
        .collect{ Object ng -> ((NatGatewayNetworkView)ng).id } )
    return { RouteNetworkView routeNetworkView ->
      final boolean active =
          (!routeNetworkView.gatewayId && !routeNetworkView.networkInterfaceId &&
           !routeNetworkView.instanceId && !routeNetworkView.natGatewayId) || // local route
          internetGatewayIds.contains( routeNetworkView.gatewayId ) ||
          natGatewayIds.contains( routeNetworkView.natGatewayId ) ||
          ( networkInterfaceAndInstanceIds.containsKey( routeNetworkView.networkInterfaceId ) &&
              instanceIds.contains( networkInterfaceAndInstanceIds.get( routeNetworkView.networkInterfaceId ) ) )
      if ( active != routeNetworkView.active ) {
          invalidRoutes << new RouteKey( routeNetworkView.routeTableId, routeNetworkView.destinationCidr )
      }
      active
    } as Predicate<RouteNetworkView>
  }

  private static Set<IPPermissionNetworkView> explodePermissions( NetworkRule networkRule ) {
    Set<IPPermissionNetworkView> rules = Sets.newLinkedHashSet( )

    // Rules without a protocol number are pre-VPC support
    if ( networkRule.getProtocolNumber( ) != null ) {
      NetworkRule.Protocol protocol = networkRule.protocol
      Integer protocolNumber = networkRule.protocolNumber
      Integer fromPort = protocol?.extractLowPort( networkRule )
      Integer toPort = protocol?.extractHighPort( networkRule )
      Integer icmpType = protocol?.extractIcmpType( networkRule )
      Integer icmpCode = protocol?.extractIcmpCode( networkRule )

      rules.addAll( networkRule.networkPeers.collect{ NetworkPeer peer ->
        new IPPermissionNetworkView(
            protocolNumber,
            fromPort,
            toPort,
            icmpType,
            icmpCode,
            peer.groupId,
            peer.userQueryKey,
            null
        )
      } )
      rules.addAll( networkRule.ipRanges.collect{ String cidr ->
        new IPPermissionNetworkView(
            protocolNumber,
            fromPort,
            toPort,
            icmpType,
            icmpCode,
            null,
            null,
            cidr
        )
      } )
    }

    rules
  }

  private static boolean validInstanceMetadata( final VmInstance instance) {
    !Strings.isNullOrEmpty( instance.privateAddress ) &&
        !VmNetworkConfig.DEFAULT_IP.equals( instance.privateAddress ) &&
        !instance.networkGroups.isEmpty( ) &&
        !Strings.isNullOrEmpty( VmInstances.toNodeHost( ).apply( instance ) )
  }

  @SuppressWarnings("GroovyUnusedDeclaration")
  private static BNetworkInfo modeClean( final boolean vpc, final BNetworkInfo networkInfo ) {
    BNetworkInfo info = networkInfo
    Set<String> removedResources = Sets.newHashSet( )
    if ( vpc ) {
      info = modeCleanVpc( networkInfo, removedResources )
    } else {
      info = modeCleanEdge( networkInfo, removedResources )
    }
    final Lens<BNetworkInfo,Array<BNICluster>> clusterArrayLens = clusterArrayLens( );
    final Lens<BNICluster,Array<BNINode>> nodeArrayLens = nodeArrayLens( );
    return clusterArrayLens.modify( { Array<BNICluster> clusterArray -> clusterArray.map(
      nodeArrayLens.modify(
          { Array<BNINode> nodeArray -> nodeArray.map( { BNINode node -> BNI.node( ).from( node ).instanceIds( node.instanceIds( ).removeAll( removedResources ) ).o( ) } ) }
      )
    ) } )
        .andThen( instancesLens( ).modify(
            { Array<BNIInstance> instanceArray -> instanceArray.map( { BNIInstance instance -> BNI.instance( ).from( instance ).securityGroups( instance.securityGroups( ).removeAll( removedResources ) ).o( ) } ) }
        ) )
        .apply( info );
  }

  /**
   *
   * Remove any EC2-Classic platform info:
   * - instances running without a vpc or without any network interfaces
   */
  private static BNetworkInfo modeCleanVpc( final BNetworkInfo info, final Set<String> removedResources ) {
    final CompatPredicate<BNIInstance> goodInstances =
        { BNIInstance instance -> instance.vpc( ).isDefined( ) && !instance.networkInterfaces( ).isEmpty( ) } as CompatPredicate<BNIInstance>
    Array<BNIInstance> removedInstances = info.instances( ).filter( goodInstances.negate( ) )

    CompatFunction<BNIHasName,String> nameOf = { BNIHasName named -> named.name( ) } as CompatFunction<BNIHasName,String>
    for ( BNIHasName named : removedInstances ) { removedResources.add( named.name( ) ) }

    instancesLens( ).modify( { Array<BNIInstance> instances -> info.instances( ).filter( goodInstances ) } )
        .apply( info )
  }

  /**
   * Remove any EC2-VPC platform info:
   * - instances running in a a vpc
   * - instances running with network interfaces attached
   * - security groups using egress rules
   * - security groups using ingress rules with invalid protocols
   * - vpcs
   * - internet gateways
   * - dhcp option sets
   */
  private static BNetworkInfo modeCleanEdge( final BNetworkInfo info, final Set<String> removedResources  ) {
    CompatPredicate<BNIInstance> goodInstances =
        { BNIInstance instance -> !instance.vpc( ).isDefined( ) && instance.networkInterfaces( ).isEmpty( ) } as CompatPredicate<BNIInstance>
    Array<BNIInstance> removedInstances = info.instances( ).filter( goodInstances.negate( ) )

    CompatPredicate<BNISecurityGroup> goodGroups = { BNISecurityGroup securityGroup ->
      boolean good = securityGroup.egressRules( ).isEmpty( ) || securityGroup.egressRules( ).get( ).rules( ).isEmpty( )
      if ( good && securityGroup.ingressRules( ).isDefined( ) ) {
        good = !securityGroup.ingressRules().get().rules().exists( { BNISecurityGroupIpPermission permission ->
          !NetworkRule.isValidProtocol( permission.protocol( ) ) } as CompatPredicate<BNISecurityGroupIpPermission> )
      }
      good
    } as CompatPredicate<BNISecurityGroup>
    Array<BNISecurityGroup> removedGroups = info.securityGroups( ).filter( goodGroups.negate( ) )

    CompatFunction<BNIHasName,String> nameOf = { BNIHasName named -> named.name( ) } as CompatFunction<BNIHasName,String>
    for ( BNIHasName named : info.dhcpOptionSets( ) ) { removedResources.add( named.name( ) ) }
    for ( BNIHasName named : info.internetGateways( ) ) { removedResources.add( named.name( ) ) }
    for ( BNIHasName named : info.vpcs( ) ) { removedResources.add( named.name( ) ) }
    for ( BNIHasName named : removedGroups ) { removedResources.add( named.name( ) ) }
    for ( BNIHasName named : removedInstances ) { removedResources.add( named.name( ) ) }

    dhcpOptionSetsLens( ).modify( { Array<BNIDhcpOptionSet> dhcpOptionSets -> Array.empty( ) } )
        .andThen( instancesLens( ).modify( { Array<BNIInstance> instances -> info.instances( ).filter( goodInstances ) } ) )
        .andThen( internetGatewaysLens( ).modify( { Array<BNIInternetGateway> internetGateways -> Array.empty( ) } ) )
        .andThen( securityGroupLens( ).modify( { Array<BNISecurityGroup> groups -> groups.filter( goodGroups ) } ) )
        .andThen( vpcsLens( ).modify( { Array<BNIVpc> vpcs -> Array.empty( ) } ) )
        .apply( info )
  }

  static interface NetworkInfoSource {
    Iterable<VmInstanceNetworkView> getInstances( );
    Iterable<NetworkGroupNetworkView> getSecurityGroups( );
    Iterable<VpcNetworkView> getVpcs( );
    Iterable<SubnetNetworkView> getSubnets( );
    Iterable<DhcpOptionSetNetworkView> getDhcpOptionSets( );
    Iterable<NetworkAclNetworkView> getNetworkAcls( );
    Iterable<RouteTableNetworkView> getRouteTables( );
    Iterable<InternetGatewayNetworkView> getInternetGateways( );
    Iterable<NetworkInterfaceNetworkView> getNetworkInterfaces( );
    Iterable<NatGatewayNetworkView> getNatGateways( );
    Map<String,Iterable<? extends VersionedNetworkView>> getView( );
  }

  @TypeMapper
  static enum NetworkConfigurationToNetworkInfo implements Function<NetworkConfiguration, ImmutableBNetworkInfo.Builder> {
    INSTANCE;

    @Override
    ImmutableBNetworkInfo.Builder apply( final NetworkConfiguration networkConfiguration ) {
      ManagedSubnet managedSubnet = networkConfiguration.managedSubnet
      networkInfo()
          .configuration( configuration( )
              .property( property("mode", networkConfiguration.mode ?: 'EDGE' ) )
              .property( property( ).name("publicIps").addAllValues( (networkConfiguration.publicIps?:[ ]) as Iterable<String> ).o( ) )
              .addAllProperties( [ networkConfiguration?.mido ? midonet( )
                  .name("mido")
                  .addAllProperties( [ networkConfiguration?.mido?.gatewayHost ? gateways( )
                      .name("gateways")
                      .gateway( gateway( )
                          .addAllProperties( [
                              networkConfiguration?.mido?.gatewayHost ?
                                  property( 'gatewayHost', networkConfiguration.mido.gatewayHost ) :
                                  null,
                              networkConfiguration?.mido?.gatewayIP ?
                                  property( 'gatewayIP',networkConfiguration.mido.gatewayIP ) :
                                  null,
                              networkConfiguration?.mido?.gatewayInterface ?
                                  property('gatewayInterface', networkConfiguration.mido.gatewayInterface ) :
                                  null,
                          ].findAll( ) as Iterable<BNIPropertyBase> )
                          .o( ) )
                      .o( ) :
                      networkConfiguration?.mido?.gateways ? gateways( )
                      .name("gateways")
                      .addAllGateways( networkConfiguration?.mido?.gateways?.collect{ MidonetGateway midoGateway ->
                        gateway( )
                            .addAllProperties( ( midoGateway.gatewayIP ?
                            [ // legacy format
                              property('gatewayHost', midoGateway.gatewayHost ),
                              property('gatewayIP', midoGateway.gatewayIP ),
                              property('gatewayInterface', midoGateway.gatewayInterface ),
                            ] : [
                            property( 'ip', midoGateway.ip ),
                            property( 'externalCidr', midoGateway.externalCidr ),
                            property( 'externalDevice', midoGateway.externalDevice ),
                            property( 'externalIp', midoGateway.externalIp ),
                            midoGateway.externalRouterIp ?
                                property('externalRouterIp', midoGateway.externalRouterIp ) :
                                null,
                            midoGateway.bgpPeerIp ?
                                property('bgpPeerIp', midoGateway.bgpPeerIp ) :
                                null,
                            midoGateway.bgpPeerAsn ?
                                property('bgpPeerAsn', midoGateway.bgpPeerAsn ) :
                                null,
                            midoGateway.bgpAdRoutes ?
                                property( ).name( 'bgpAdRoutes' ).setIterableValues( midoGateway.bgpAdRoutes ).o( ) :
                                null,
                            ] ).findAll( ) as Iterable<BNIPropertyBase> )
                            .o( )
                      } as Iterable<BNIMidonetGateway> ).o( ) :
                      null
                  ].findAll( ) as Iterable<BNIPropertyBase> )
                  .addAllProperties( [
                      networkConfiguration?.mido?.eucanetdHost ?
                          property('eucanetdHost', networkConfiguration.mido.eucanetdHost) :
                          null,
                      networkConfiguration?.mido?.bgpAsn ?
                          property( 'bgpAsn', networkConfiguration.mido.bgpAsn ) :
                          null,
                      networkConfiguration?.mido?.publicNetworkCidr ?
                          property( 'publicNetworkCidr', networkConfiguration.mido.publicNetworkCidr ) :
                          null,
                      networkConfiguration?.mido?.publicGatewayIP ?
                          property( 'publicGatewayIP', networkConfiguration.mido.publicGatewayIP ) :
                          null,
                  ].findAll( ) as Iterable<BNIPropertyBase> )
                  .o( ) :
                  null,
                  networkConfiguration.subnets ? subnets( )
                      .name( 'subnets' )
                      .addAllSubnets( networkConfiguration.subnets.collect{ EdgeSubnet edgeSubnet ->
                          subnet( )
                            .name( edgeSubnet.subnet )  // broadcast name is always the subnet value
                            .property( property( 'subnet', edgeSubnet.subnet ) )
                            .property( property( 'netmask', edgeSubnet.netmask ) )
                            .property( property( 'gateway', edgeSubnet.gateway ) )
                            .o( )
                      } as Iterable<BNISubnet> )
                      .o( ) :
                      null,
                  managedSubnet ? managedSubnets( )
                      .name( 'managedSubnet' )
                      .managedSubnet( BNI.managedSubnet( )
                          .name( managedSubnet.subnet ) // broadcast name is always the subnet value
                          .property( property( 'subnet', managedSubnet.subnet ) )
                          .property( property( 'netmask', managedSubnet.netmask ) )
                          .property( property( 'minVlan', ( managedSubnet.minVlan ?: ManagedSubnet.MIN_VLAN ) as String ) )
                          .property( property( 'maxVlan', ( managedSubnet.maxVlan ?: ManagedSubnet.MAX_VLAN ) as String ) )
                          .property( property( 'segmentSize', ( managedSubnet.segmentSize ?: ManagedSubnet.DEF_SEGMENT_SIZE ) as String ) )
                          .o( ) )
                      .o( ) :
                      null
              ].findAll( ) as Iterable<BNIPropertyBase> )
              .o( ) )
    }
  }

  private static Predicate<Cluster> uniquePartitionPredicate( Set<String> partitionNames = Sets.newHashSet( ) ) {
    { Cluster cluster -> partitionNames.add( cluster.partition )  } as Predicate<Cluster>
  }

  interface VersionedNetworkView {
    String getId( )
    int getVersion( )
  }

  @Immutable
  static class VmInstanceNetworkView implements Comparable<VmInstanceNetworkView>, VersionedNetworkView {
    String id
    int version
    VmInstance.VmState state
    Boolean omit
    String ownerAccountNumber
    String vpcId
    String subnetId
    String macAddress
    String privateAddress
    String publicAddress
    String partition
    String node
    List<String> securityGroupIds

    int compareTo( VmInstanceNetworkView o ) {
      this.id <=> o.id
    }
  }

  @TypeMapper
  static enum VmInstanceToVmInstanceNetworkView implements Function<VmInstance,VmInstanceNetworkView> {
    INSTANCE;

    @Override
    VmInstanceNetworkView apply( final VmInstance instance ) {
      new VmInstanceNetworkView(
          instance.instanceId,
          instance.version,
          instance.state,
          MoreObjects.firstNonNull( instance.runtimeState.zombie, false ) || !validInstanceMetadata( instance ),
          instance.ownerAccountNumber,
          instance.bootRecord.vpcId,
          instance.bootRecord.subnetId,
          instance.macAddress,
          instance.privateAddress,
          instance.publicAddress,
          instance.partition,
          Strings.nullToEmpty( VmInstances.toNodeHost( ).apply( instance ) ),
          instance.networkGroups.collect{ NetworkGroup group -> group.groupId }
      )
    }
  }

  @Immutable
  static class IPPermissionNetworkView {
    Integer protocol
    Integer fromPort
    Integer toPort
    Integer icmpType
    Integer icmpCode
    String groupId
    String groupOwnerAccountNumber
    String cidr

    boolean equals(final o) {
      if (this.is(o)) return true
      if (getClass() != o.class) return false

      final IPPermissionNetworkView that = (IPPermissionNetworkView) o

      if (cidr != that.cidr) return false
      if (fromPort != that.fromPort) return false
      if (groupId != that.groupId) return false
      if (groupOwnerAccountNumber != that.groupOwnerAccountNumber) return false
      if (icmpCode != that.icmpCode) return false
      if (icmpType != that.icmpType) return false
      if (protocol != that.protocol) return false
      if (toPort != that.toPort) return false

      return true
    }

    int hashCode() {
      int result
      result = (protocol != null ? protocol.hashCode() : 0)
      result = 31 * result + (fromPort != null ? fromPort.hashCode() : 0)
      result = 31 * result + (toPort != null ? toPort.hashCode() : 0)
      result = 31 * result + (icmpType != null ? icmpType.hashCode() : 0)
      result = 31 * result + (icmpCode != null ? icmpCode.hashCode() : 0)
      result = 31 * result + (groupId != null ? groupId.hashCode() : 0)
      result = 31 * result + (groupOwnerAccountNumber != null ? groupOwnerAccountNumber.hashCode() : 0)
      result = 31 * result + (cidr != null ? cidr.hashCode() : 0)
      return result
    }
  }

  @Immutable
  static class NetworkGroupNetworkView implements Comparable<NetworkGroupNetworkView>, VersionedNetworkView {
    String id
    int version
    String ownerAccountNumber
    String vpcId
    List<IPPermissionNetworkView> ingressPermissions
    List<IPPermissionNetworkView> egressPermissions

    int compareTo( NetworkGroupNetworkView o ) {
      this.id <=> o.id
    }
  }

  @TypeMapper
  static enum NetworkGroupToNetworkGroupNetworkView implements Function<NetworkGroup,NetworkGroupNetworkView> {
    INSTANCE;

    @SuppressWarnings("UnnecessaryQualifiedReference")
    @Override
    NetworkGroupNetworkView apply( final NetworkGroup group ) {
      new NetworkGroupNetworkView(
          group.groupId,
          group.version,
          group.ownerAccountNumber,
          group.vpcId,
          group.ingressNetworkRules.collect{ NetworkRule networkRule -> NetworkInfoBroadcasts.explodePermissions( networkRule ) }.flatten( ) as List<IPPermissionNetworkView>,
          group.egressNetworkRules.collect{ NetworkRule networkRule -> NetworkInfoBroadcasts.explodePermissions( networkRule ) }.flatten( ) as List<IPPermissionNetworkView>
      )
    }
  }

  @Immutable
  static class VpcNetworkView implements Comparable<VpcNetworkView>, VersionedNetworkView {
    String id
    int version
    String ownerAccountNumber
    String cidr
    String dhcpOptionSetId

    int compareTo( VpcNetworkView o ) {
      this.id <=> o.id
    }
  }

  @TypeMapper
  static enum VpcToVpcNetworkView implements Function<Vpc,VpcNetworkView> {
    INSTANCE;

    @Override
    VpcNetworkView apply( final Vpc vpc ) {
      new VpcNetworkView(
          vpc.displayName,
          vpc.version,
          vpc.ownerAccountNumber,
          vpc.cidr,
          vpc.dhcpOptionSet?.displayName
      )
    }
  }

  @Immutable
  static class SubnetNetworkView implements Comparable<SubnetNetworkView>, VersionedNetworkView {
    String id
    int version
    String ownerAccountNumber
    String vpcId
    String cidr
    String availabilityZone
    String networkAcl

    int compareTo( SubnetNetworkView o ) {
      this.id <=> o.id
    }
  }

  @TypeMapper
  static enum SubnetToSubnetNetworkView implements Function<ComputeSubnet,SubnetNetworkView> {
    INSTANCE;

    @Override
    SubnetNetworkView apply( final ComputeSubnet subnet ) {
      new SubnetNetworkView(
          subnet.displayName,
          subnet.version,
          subnet.ownerAccountNumber,
          subnet.vpc.displayName,
          subnet.cidr,
          subnet.availabilityZone,
          subnet.networkAcl.displayName
      )
    }
  }

  @Immutable
  static class DhcpOptionSetNetworkView implements Comparable<DhcpOptionSetNetworkView>, VersionedNetworkView {
    String id
    int version
    String ownerAccountNumber
    List<DhcpOptionNetworkView> options

    int compareTo( DhcpOptionSetNetworkView o ) {
      this.id <=> o.id
    }
  }

  @Immutable
  static class DhcpOptionNetworkView {
    String key
    List<String> values
  }

  @TypeMapper
  static enum DhcpOptionSetToDhcpOptionSetNetworkView implements Function<DhcpOptionSet,DhcpOptionSetNetworkView> {
    INSTANCE;

    @Override
    DhcpOptionSetNetworkView apply( final DhcpOptionSet dhcpOptionSet ) {
      new DhcpOptionSetNetworkView(
          dhcpOptionSet.displayName,
          dhcpOptionSet.version,
          dhcpOptionSet.ownerAccountNumber,
          ImmutableList.copyOf( dhcpOptionSet.dhcpOptions.collect{ DhcpOption option -> new DhcpOptionNetworkView(
              option.key,
              ImmutableList.copyOf( option.values )
          ) } )
      )
    }
  }

  @Immutable
  static class NetworkAclNetworkView implements Comparable<NetworkAclNetworkView>, VersionedNetworkView {
    String id
    int version
    String ownerAccountNumber
    String vpcId
    List<NetworkAclEntryNetworkView> ingressRules
    List<NetworkAclEntryNetworkView> egressRules

    int compareTo( NetworkAclNetworkView o ) {
      this.id <=> o.id
    }
  }

  @Immutable
  static class NetworkAclEntryNetworkView {
    Integer number
    Integer protocol
    String action
    String cidr
    Integer icmpCode
    Integer icmpType
    Integer portRangeFrom
    Integer portRangeTo
  }

  @TypeMapper
  static enum NetworkAclToNetworkAclNetworkView implements Function<NetworkAcl,NetworkAclNetworkView> {
    INSTANCE;

    @Override
    NetworkAclNetworkView apply( final NetworkAcl networkAcl ) {
      List<NetworkAclEntry> orderedEntries = NetworkAcls.ENTRY_ORDERING.sortedCopy( networkAcl.entries )
      new NetworkAclNetworkView(
          networkAcl.displayName,
          networkAcl.version,
          networkAcl.ownerAccountNumber,
          networkAcl.vpc.displayName,
          ImmutableList.copyOf( orderedEntries.findAll{ NetworkAclEntry entry -> !entry.egress }.collect{ NetworkAclEntry entry -> TypeMappers.transform( entry, NetworkAclEntryNetworkView  ) } ),
          ImmutableList.copyOf( orderedEntries.findAll{ NetworkAclEntry entry -> entry.egress }.collect{ NetworkAclEntry entry -> TypeMappers.transform( entry, NetworkAclEntryNetworkView  ) } )
      )
    }
  }

  @TypeMapper
  static enum NetworkAclEntryToNetworkAclEntryNetworkView implements Function<NetworkAclEntry,NetworkAclEntryNetworkView> {
    INSTANCE;

    @Override
    NetworkAclEntryNetworkView apply( final NetworkAclEntry networkAclEntry ) {
      new NetworkAclEntryNetworkView(
          networkAclEntry.ruleNumber,
          networkAclEntry.protocol,
          String.valueOf( networkAclEntry.ruleAction ),
          networkAclEntry.cidr,
          networkAclEntry.icmpCode,
          networkAclEntry.icmpType,
          networkAclEntry.portRangeFrom,
          networkAclEntry.portRangeTo
      )
    }
  }

  @TypeMapper
  static enum NetworkAclEntryNetworkViewToNINetworkAclRule implements Function<NetworkAclEntryNetworkView,BNINetworkAclEntry> {
    INSTANCE;

    @Override
    BNINetworkAclEntry apply(@Nullable final NetworkAclEntryNetworkView networkAclEntry ) {
      BNI.networkAclEntry( )
        .number( networkAclEntry.number )
        .protocol( networkAclEntry.protocol )
        .action( networkAclEntry.action )
        .cidr( networkAclEntry.cidr )
        .icmpCode( networkAclEntry.icmpCode )
        .icmpType( networkAclEntry.icmpType )
        .portRangeFrom( networkAclEntry.portRangeFrom )
        .portRangeTo( networkAclEntry.portRangeTo )
        .o( )
    }
  }

  @Immutable
  static class RouteTableNetworkView implements Comparable<RouteTableNetworkView>, VersionedNetworkView {
    String id
    int version
    String ownerAccountNumber
    String vpcId
    boolean main
    List<String> subnetIds // associated subnets
    List<RouteNetworkView> routes

    int compareTo( RouteTableNetworkView o ) {
      this.id <=> o.id
    }
  }

  @Immutable
  static class RouteNetworkView {
    boolean active
    String routeTableId
    String destinationCidr
    String gatewayId
    String natGatewayId
    String networkInterfaceId
    String instanceId
  }

  @TypeMapper
  static enum RouteTableToRouteTableNetworkView implements Function<RouteTable,RouteTableNetworkView> {
    INSTANCE;

    @Override
    RouteTableNetworkView apply( final RouteTable routeTable ) {
      new RouteTableNetworkView(
          routeTable.displayName,
          routeTable.version,
          routeTable.ownerAccountNumber,
          routeTable.vpc.displayName,
          routeTable.main,
          ImmutableList.copyOf( routeTable.routeTableAssociations.findResults{ RouteTableAssociation association -> association.subnetId } as Collection<String> ),
          ImmutableList.copyOf( routeTable.routes.collect{ Route route -> TypeMappers.transform( route, RouteNetworkView ) } ),
      )
    }
  }

  @TypeMapper
  static enum RouteToRouteNetworkView implements Function<Route,RouteNetworkView> {
    INSTANCE;

    @Override
    RouteNetworkView apply(@Nullable final Route route) {
      new RouteNetworkView(
          route.state == Route.State.active,
          route.routeTable.displayName,
          route.destinationCidr,
          route.internetGatewayId,
          route.natGatewayId,
          route.networkInterfaceId,
          route.instanceId
      )
    }
  }

  @TypeMapper
  static enum RouteNetworkViewToNIRoute implements Function<RouteNetworkView,BNIRoute> {
    INSTANCE;

    @Override
    BNIRoute apply(@Nullable final RouteNetworkView routeNetworkView) {
      route( )
        .destinationCidr( routeNetworkView.destinationCidr )
        .gatewayId( Option.of( routeNetworkView.gatewayId?:(routeNetworkView.networkInterfaceId||routeNetworkView.natGatewayId?null:'local') ) )
        .networkInterfaceId( Option.of( routeNetworkView.networkInterfaceId ) )
        .natGatewayId( Option.of( routeNetworkView.natGatewayId ) )
        .o( )
    }
  }

  @Immutable
  static class InternetGatewayNetworkView implements Comparable<InternetGatewayNetworkView>, VersionedNetworkView {
    String id
    int version
    String ownerAccountNumber
    String vpcId

    int compareTo( InternetGatewayNetworkView o ) {
      this.id <=> o.id
    }
  }

  @TypeMapper
  static enum InternetGatewayToInternetGatewayNetworkView implements Function<InternetGateway,InternetGatewayNetworkView> {
    INSTANCE;

    @Override
    InternetGatewayNetworkView apply( final InternetGateway internetGateway ) {
      new InternetGatewayNetworkView(
          internetGateway.displayName,
          internetGateway.version,
          internetGateway.ownerAccountNumber,
          internetGateway.vpc?.displayName
      )
    }
  }

  @Immutable
  static class NetworkInterfaceNetworkView implements Comparable<NetworkInterfaceNetworkView>, VersionedNetworkView {
    String id
    int version
    NetworkInterface.State state
    NetworkInterfaceAttachment.Status attachmentStatus
    String ownerAccountNumber
    String instanceId
    String attachmentId
    Integer deviceIndex
    String macAddress
    String privateIp
    String publicIp
    Boolean sourceDestCheck
    String vpcId
    String subnetId
    List<String> securityGroupIds

    int compareTo( NetworkInterfaceNetworkView o ) {
      this.id <=> o.id
    }
  }

  @TypeMapper
  static enum VpcNetworkInterfaceToNetworkInterfaceNetworkView implements Function<NetworkInterface,NetworkInterfaceNetworkView> {
    INSTANCE;

    @Override
    NetworkInterfaceNetworkView apply( final NetworkInterface networkInterface ) {
      new NetworkInterfaceNetworkView(
          networkInterface.displayName,
          networkInterface.version,
          networkInterface.state,
          networkInterface?.attachment?.status ?: NetworkInterfaceAttachment.Status.detached,
          networkInterface.ownerAccountNumber,
          networkInterface.attachment?.instanceId,
          networkInterface.attachment?.attachmentId,
          networkInterface.attachment?.deviceIndex,
          networkInterface.macAddress,
          networkInterface.privateIpAddress,
          networkInterface?.association?.publicIp,
          networkInterface.sourceDestCheck,
          networkInterface.vpc.displayName,
          networkInterface.subnet.displayName,
          networkInterface.networkGroups.collect{ NetworkGroup group -> group.groupId }
      )
    }
  }

  @Immutable
  static class NatGatewayNetworkView implements Comparable<NatGatewayNetworkView>, VersionedNetworkView {
    String id
    int version
    NatGateway.State state
    String ownerAccountNumber
    String macAddress
    String privateIp
    String publicIp
    String vpcId
    String subnetId

    int compareTo( NatGatewayNetworkView o ) {
      this.id <=> o.id
    }
  }

  @TypeMapper
  static enum NatGatewayToNatGatewayNetworkView implements Function<NatGateway,NatGatewayNetworkView> {
    INSTANCE;

    @Override
    NatGatewayNetworkView apply( final NatGateway natGateway ) {
      new NatGatewayNetworkView(
          natGateway.displayName,
          natGateway.version,
          natGateway.state,
          natGateway.ownerAccountNumber,
          natGateway.macAddress,
          natGateway.privateIpAddress,
          natGateway.publicIpAddress,
          natGateway.vpcId,
          natGateway.subnetId
      )
    }
  }
}
