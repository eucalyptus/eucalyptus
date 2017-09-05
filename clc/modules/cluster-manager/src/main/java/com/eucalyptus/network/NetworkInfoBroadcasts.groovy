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
import com.eucalyptus.cluster.common.broadcast.NICluster
import com.eucalyptus.cluster.common.broadcast.NIClusters
import com.eucalyptus.cluster.common.broadcast.NIConfiguration
import com.eucalyptus.cluster.common.broadcast.NIDhcpOptionSet
import com.eucalyptus.cluster.common.broadcast.NIInstance
import com.eucalyptus.cluster.common.broadcast.NIInternetGateway
import com.eucalyptus.cluster.common.broadcast.NIManagedSubnet
import com.eucalyptus.cluster.common.broadcast.NIManagedSubnets
import com.eucalyptus.cluster.common.broadcast.NIMidonet
import com.eucalyptus.cluster.common.broadcast.NIMidonetGateway
import com.eucalyptus.cluster.common.broadcast.NIMidonetGateways
import com.eucalyptus.cluster.common.broadcast.NINatGateway
import com.eucalyptus.cluster.common.broadcast.NINetworkAcl
import com.eucalyptus.cluster.common.broadcast.NINetworkAclEntry
import com.eucalyptus.cluster.common.broadcast.NINetworkInterface
import com.eucalyptus.cluster.common.broadcast.NINode
import com.eucalyptus.cluster.common.broadcast.NINodes
import com.eucalyptus.cluster.common.broadcast.NIProperty
import com.eucalyptus.cluster.common.broadcast.NIRoute
import com.eucalyptus.cluster.common.broadcast.NIRouteTable
import com.eucalyptus.cluster.common.broadcast.NISecurityGroup
import com.eucalyptus.cluster.common.broadcast.NISecurityGroupIpPermission
import com.eucalyptus.cluster.common.broadcast.NISubnet
import com.eucalyptus.cluster.common.broadcast.NISubnets
import com.eucalyptus.cluster.common.broadcast.NIVpc
import com.eucalyptus.cluster.common.broadcast.NIVpcSubnet
import com.eucalyptus.cluster.common.broadcast.NetworkInfo
import com.eucalyptus.compute.common.internal.network.NetworkGroup
import com.eucalyptus.compute.common.internal.network.NetworkPeer
import com.eucalyptus.compute.common.internal.network.NetworkRule
import com.eucalyptus.compute.common.internal.vm.VmInstance
import com.eucalyptus.compute.common.internal.vm.VmNetworkConfig
import com.eucalyptus.compute.common.internal.vpc.DhcpOption
import com.eucalyptus.compute.common.internal.vpc.DhcpOptionSet
import com.eucalyptus.compute.common.internal.vpc.InternetGateway
import com.eucalyptus.compute.common.internal.vpc.NatGateway
import com.eucalyptus.compute.common.internal.vpc.NetworkAcl
import com.eucalyptus.compute.common.internal.vpc.NetworkAclEntry
import com.eucalyptus.compute.common.internal.vpc.NetworkAcls
import com.eucalyptus.compute.common.internal.vpc.NetworkInterface
import com.eucalyptus.compute.common.internal.vpc.NetworkInterfaceAttachment
import com.eucalyptus.compute.common.internal.vpc.Route
import com.eucalyptus.compute.common.internal.vpc.RouteTable
import com.eucalyptus.compute.common.internal.vpc.RouteTableAssociation
import com.eucalyptus.compute.common.internal.vpc.Subnet
import com.eucalyptus.compute.common.internal.vpc.Vpc
import com.eucalyptus.compute.vpc.RouteKey
import com.eucalyptus.network.config.Cluster as ConfigCluster;
import com.eucalyptus.network.config.EdgeSubnet
import com.eucalyptus.network.config.ManagedSubnet
import com.eucalyptus.network.config.MidonetGateway
import com.eucalyptus.network.config.NetworkConfiguration
import com.eucalyptus.network.config.NetworkConfigurations
import com.eucalyptus.util.CollectionUtils
import com.eucalyptus.util.TypeMapper
import com.eucalyptus.util.TypeMappers
import com.eucalyptus.util.Strings as EucaStrings;
import com.eucalyptus.vm.VmInstances
import com.google.common.base.Function
import com.google.common.base.MoreObjects
import com.google.common.base.Optional
import com.google.common.base.Predicate
import com.google.common.base.Strings
import com.google.common.base.Supplier
import com.google.common.collect.ArrayListMultimap
import com.google.common.collect.HashMultimap
import com.google.common.collect.ImmutableList
import com.google.common.collect.Iterables
import com.google.common.collect.ListMultimap
import com.google.common.collect.Lists
import com.google.common.collect.Maps
import com.google.common.collect.Multimap
import com.google.common.collect.Sets
import com.eucalyptus.cluster.common.msgs.NodeInfo
import groovy.transform.Immutable
import groovy.transform.PackageScope
import org.apache.log4j.Logger

import javax.annotation.Nullable

import static com.eucalyptus.compute.common.internal.vm.VmInstance.VmStateSet.TORNDOWN
import static com.google.common.collect.Iterables.tryFind

@PackageScope
class NetworkInfoBroadcasts {

  private static final Logger logger = Logger.getLogger( NetworkInfoBroadcasts )

  protected static volatile boolean MODE_CLEAN =
      Boolean.valueOf( System.getProperty( 'com.eucalyptus.network.broadcastModeClean', 'true' ) )

  /**
   * All state used for building network configuration must be passed in (and be part of the fingerprint)
   */
  @PackageScope
  static NetworkInfo buildNetworkConfiguration( final Optional<NetworkConfiguration> configuration,
                                                final NetworkInfoSource networkInfoSource,
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
    NetworkInfo info = networkConfiguration
        .transform( TypeMappers.lookup( NetworkConfiguration, NetworkInfo ) )
        .or( new NetworkInfo( ) )

    // populate clusters
    info.configuration.clusters = new NIClusters(
        name: 'clusters',
        clusters: clusters.findResults{ Cluster cluster ->
          ConfigCluster configCluster = networkConfiguration.orNull()?.clusters?.find{ ConfigCluster configCluster -> cluster.partition == configCluster.name }
          configCluster && ( vpcmido || configCluster.subnet ) ?
              new NICluster(
                  name: configCluster.name,
                  subnet: vpcmido ? new NISubnet(
                      name: '172.31.0.0',
                      properties: [
                          new NIProperty( name: 'subnet', values: [ '172.31.0.0' ]),
                          new NIProperty( name: 'netmask', values: [ '255.255.0.0' ]),
                          new NIProperty( name: 'gateway', values: [ '172.31.0.1' ])
                      ]
                  ) : new NISubnet(
                      name: configCluster.subnet.subnet, // broadcast name is always the subnet value
                      properties: [
                          new NIProperty( name: 'subnet', values: [ configCluster.subnet.subnet ]),
                          new NIProperty( name: 'netmask', values: [ configCluster.subnet.netmask ]),
                          new NIProperty( name: 'gateway', values: [ configCluster.subnet.gateway ])
                      ]
                  ),
                  properties: [
                      new NIProperty( name: 'enabledCCIp', values: [ InetAddress.getByName(cluster.hostName).hostAddress ] ),
                      new NIProperty( name: 'macPrefix', values: [ configCluster.macPrefix ] ),
                      vpcmido ? new NIProperty( name: 'privateIps', values: [ '172.31.0.5' ] ) : new NIProperty( name: 'privateIps', values: configCluster.privateIps )
                  ],
                  nodes: new NINodes(
                      name: 'nodes',
                      nodes: cluster.nodeMap.values().collect{ NodeInfo nodeInfo -> new NINode( name: nodeInfo.name ) }
                  )
              ) :
              configCluster && managed ? new NICluster(
                  name: configCluster.name,
                  properties: [
                      new NIProperty( name: 'enabledCCIp', values: [ InetAddress.getByName(cluster.hostName).hostAddress ] ),
                      new NIProperty( name: 'macPrefix', values: [ configCluster.macPrefix ] )
                  ],
                  nodes: new NINodes(
                      name: 'nodes',
                      nodes: cluster.nodeMap.values().collect{ NodeInfo nodeInfo -> new NINode( name: nodeInfo.name ) }
                  )
              ) :
                  null
        } as List<NICluster>
    )

    // populate dynamic properties
    List<String> dnsServers = networkConfiguration.orNull()?.instanceDnsServers?:systemNameserverLookup.apply(['127.0.0.1'])
    info.configuration.properties.addAll( [
        new NIProperty( name: 'enabledCLCIp', values: [clcHostSupplier.get()]),
        new NIProperty( name: 'instanceDNSDomain', values: [networkConfiguration.orNull()?.instanceDnsDomain?:EucaStrings.trimPrefix('.',"${VmInstances.INSTANCE_SUBDOMAIN}.internal")]),
        new NIProperty( name: 'instanceDNSServers', values: dnsServers )
    ]  )

    boolean hasEdgePublicGateway = networkConfiguration.orNull()?.publicGateway != null
    if ( hasEdgePublicGateway ) {
      info.configuration.properties.add(
          new NIProperty( name: 'publicGateway', values: [networkConfiguration.orNull()?.publicGateway] )
      )
    }

    Iterable<VmInstanceNetworkView> instances = Iterables.filter(
        networkInfoSource.instances,
        { VmInstanceNetworkView instance -> !TORNDOWN.contains(instance.state) && !instance.omit } as Predicate<VmInstanceNetworkView> )

    // populate nodes
    ((Multimap<List<String>,String>) instances.inject( HashMultimap.create( ) ){
      Multimap<List<String>,String> map, VmInstanceNetworkView instance ->
        map.put( [ instance.partition, instance.node ], instance.id )
        map
    }).asMap().each{ Map.Entry<List<String>,Collection<String>> entry ->
      info.configuration.clusters.clusters.find{ NICluster cluster -> cluster.name == entry.key[0] }?.with{
        NINode node = nodes.nodes.find{ NINode node -> node.name == entry.key[1] }
        if ( node ) {
          node.instanceIds = entry.value ? entry.value as List<String> : (List<String>) null
        } else {
          null
        }
      }
    }

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
    info.vpcs.addAll( vpcs.findAll{ VpcNetworkView vpc -> activeVpcs.contains(vpc.id) }.collect{ Object vpcViewObj ->
      final VpcNetworkView vpc = vpcViewObj as VpcNetworkView
      new NIVpc(
          vpc.id,
          vpc.ownerAccountNumber,
          vpc.cidr,
          vpc.dhcpOptionSetId,
          subnets.findAll{ SubnetNetworkView subnet -> subnet.vpcId == vpc.id }.collect{ Object subnetViewObj ->
            SubnetNetworkView subnet = subnetViewObj as SubnetNetworkView
            new NIVpcSubnet(
                name: subnet.id,
                ownerId: subnet.ownerAccountNumber,
                cidr: subnet.cidr,
                cluster: subnet.availabilityZone,
                networkAcl: subnet.networkAcl,
                routeTable:
                    tryFind( routeTables, { RouteTableNetworkView routeTable -> routeTable.subnetIds.contains( subnet.id ) } as Predicate<RouteTableNetworkView>).or(
                        Iterables.find( routeTables, { RouteTableNetworkView routeTable -> routeTable.main && routeTable.vpcId == vpc.id } as Predicate<RouteTableNetworkView> ) ).id
            )
          },
          networkAcls.findAll{ NetworkAclNetworkView networkAcl -> networkAcl.vpcId == vpc.id }.collect { Object networkAclObj ->
            NetworkAclNetworkView networkAcl = networkAclObj as NetworkAclNetworkView
            new NINetworkAcl(
                name: networkAcl.id,
                ownerId: networkAcl.ownerAccountNumber,
                ingressEntries: Lists.transform( networkAcl.ingressRules, TypeMappers.lookup( NetworkAclEntryNetworkView, NINetworkAclEntry ) ) as List<NINetworkAclEntry>,
                egressEntries: Lists.transform( networkAcl.egressRules, TypeMappers.lookup( NetworkAclEntryNetworkView, NINetworkAclEntry ) ) as List<NINetworkAclEntry>
            )
          },
          routeTables.findAll{ RouteTableNetworkView routeTable -> routeTable.vpcId == vpc.id }.collect { Object routeTableObj ->
            RouteTableNetworkView routeTable = routeTableObj as RouteTableNetworkView
            new NIRouteTable(
                name: routeTable.id,
                ownerId: routeTable.ownerAccountNumber,
                routes: Lists.newArrayList( Iterables.transform( Iterables.filter( routeTable.routes, activeRoutePredicate ), TypeMappers.lookup( RouteNetworkView, NIRoute ) ) ) as List<NIRoute>
            )
          },
          natGateways.findAll{ NatGatewayNetworkView natGateway -> natGateway.vpcId == vpc.id && natGateway.state == NatGateway.State.available }.collect{ Object natGatewayObj ->
            NatGatewayNetworkView natGateway = natGatewayObj as NatGatewayNetworkView
            new NINatGateway(
                name: natGateway.id,
                ownerId: natGateway.ownerAccountNumber,
                vpc: natGateway.vpcId,
                subnet: natGateway.subnetId,
                macAddress: Strings.emptyToNull( natGateway.macAddress ),
                publicIp: VmNetworkConfig.DEFAULT_IP==natGateway.publicIp||dirtyPublicAddresses.contains(natGateway.publicIp) ? null : natGateway.publicIp,
                privateIp: natGateway.privateIp,
            )
          },
          vpcIdToInternetGatewayIds.get( vpc.id ) as List<String>?:[] as List<String>
      )
    } )
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
    info.instances.addAll( instances.collect{ VmInstanceNetworkView instance ->
      new NIInstance(
          name: instance.id,
          ownerId: instance.ownerAccountNumber,
          vpc: instance.vpcId,
          subnet: instance.subnetId,
          macAddress: Strings.emptyToNull( instance.macAddress ),
          publicIp: VmNetworkConfig.DEFAULT_IP==instance.publicAddress||dirtyPublicAddresses.contains(instance.publicAddress) ? null : instance.publicAddress,
          privateIp: instance.privateAddress,
          securityGroups: Lists.newArrayList(instance.securityGroupIds),
          networkInterfaces: instanceIdToNetworkInterfaces.get( instance.id )?.collect{ NetworkInterfaceNetworkView networkInterface ->
            new NINetworkInterface(
                name: networkInterface.id,
                ownerId: networkInterface.ownerAccountNumber,
                deviceIndex: networkInterface.deviceIndex,
                attachmentId: networkInterface.attachmentId,
                macAddress: networkInterface.macAddress,
                privateIp: networkInterface.privateIp,
                publicIp: dirtyPublicAddresses.contains(networkInterface.publicIp) ? null : networkInterface.publicIp,
                sourceDestCheck: networkInterface.sourceDestCheck,
                vpc: networkInterface.vpcId,
                subnet: networkInterface.subnetId,
                securityGroups: networkInterface.securityGroupIds
            )
          } ?: [ ] as List<NINetworkInterface>
      )
    } )

    // populate dhcp option sets
    Iterable<DhcpOptionSetNetworkView> dhcpOptionSets = networkInfoSource.dhcpOptionSets
    info.dhcpOptionSets.addAll( dhcpOptionSets.findAll{ DhcpOptionSetNetworkView dhcpOptionSet -> activeDhcpOptionSets.contains( dhcpOptionSet.id ) }.collect { Object dhcpObj ->
      DhcpOptionSetNetworkView dhcpOptionSet = (DhcpOptionSetNetworkView) dhcpObj
      new NIDhcpOptionSet(
          name: dhcpOptionSet.id,
          ownerId: dhcpOptionSet.ownerAccountNumber,
          properties: dhcpOptionSet.options.collect{ DhcpOptionNetworkView option ->
            if ( 'domain-name-servers' == option.key && 'AmazonProvidedDNS' == option.values?.getAt( 0 ) ) {
              new NIProperty( 'domain-name-servers', dnsServers )
            } else {
              new NIProperty( option.key, option.values )
            }
          }
      )
    } )

    // populate internet gateways
    info.internetGateways.addAll( internetGateways.findAll{ Object gatewayObj ->
      InternetGatewayNetworkView gateway = gatewayObj as InternetGatewayNetworkView
      activeVpcs.contains(gateway.vpcId)
    }.collect { Object internetGatewayObj ->
      InternetGatewayNetworkView internetGateway = internetGatewayObj as InternetGatewayNetworkView
      new NIInternetGateway(
          name: internetGateway.id,
          ownerId: internetGateway.ownerAccountNumber,
      )
    } )

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
    info.securityGroups.addAll( groups.findAll{  NetworkGroupNetworkView group -> activeSecurityGroups.contains( group.id ) }.collect{ Object groupObj ->
      NetworkGroupNetworkView group = (NetworkGroupNetworkView) groupObj
      new NISecurityGroup(
          name: group.id,
          ownerId: group.ownerAccountNumber,
          ingressRules: group.ingressPermissions
          .findAll{ IPPermissionNetworkView ipPermission ->
                      !ipPermission.groupId || activeSecurityGroups.contains( ipPermission.groupId ) }
          .collect{ Object ipPermissionObj ->
              IPPermissionNetworkView ipPermission = (IPPermissionNetworkView) ipPermissionObj
            new NISecurityGroupIpPermission(
                ipPermission.protocol,
                ipPermission.fromPort,
                ipPermission.toPort,
                ipPermission.icmpType,
                ipPermission.icmpCode,
                ipPermission.groupId,
                ipPermission.groupOwnerAccountNumber,
                ipPermission.cidr
            )
          } as List<NISecurityGroupIpPermission>,
          egressRules: group.egressPermissions
          .findAll{ IPPermissionNetworkView ipPermission ->
                      !ipPermission.groupId || activeSecurityGroups.contains( ipPermission.groupId ) }
          .collect{ Object ipPermissionObj ->
              IPPermissionNetworkView ipPermission = (IPPermissionNetworkView) ipPermissionObj
            new NISecurityGroupIpPermission(
                ipPermission.protocol,
                ipPermission.fromPort,
                ipPermission.toPort,
                ipPermission.icmpType,
                ipPermission.icmpCode,
                ipPermission.groupId,
                ipPermission.groupOwnerAccountNumber,
                ipPermission.cidr
            )
          } as List<NISecurityGroupIpPermission>
      )
    } )

    if ( MODE_CLEAN ) {
      modeClean( vpcmido, info )
    }

    if ( logger.isTraceEnabled( ) ) {
      logger.trace( "Constructed network information for ${Iterables.size( instances )} instance(s), ${Iterables.size( groups )} security group(s)" )
    }

    info
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

  private static Set<String> modeClean( final boolean vpc, final NetworkInfo networkInfo ) {
    Set<String> removedResources
    if ( vpc ) {
      removedResources = modeCleanVpc( networkInfo )
    } else {
      removedResources = modeCleanEdge( networkInfo )
    }
    networkInfo.configuration.clusters.clusters.forEach{ NICluster cluster ->
      cluster.nodes.nodes.forEach{ NINode node ->
        node.instanceIds.removeAll( removedResources )
      }
    }
    networkInfo.instances.forEach{ NIInstance instance ->
      instance.securityGroups.removeAll( removedResources )
    }
    removedResources
  }

  /**
   * Remove any EC2-Classic platform info:
   * - instances running without a vpc or without any network interfaces
   */
  private static Set<String> modeCleanVpc( final NetworkInfo networkInfo ) {
    Set<String> removedResources = Sets.newHashSet( )
    Iterator<NIInstance> instanceIterator = networkInfo.instances.iterator( )
    while ( instanceIterator.hasNext( ) ) {
      NIInstance instance = instanceIterator.next( )
      if ( !instance.vpc || !instance.networkInterfaces ) {
        instanceIterator.remove( )
        removedResources.add( instance.name )
      }
    }
    removedResources
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
  private static Set<String> modeCleanEdge( final NetworkInfo networkInfo ) {
    Set<String> removedResources = Sets.newHashSet( )
    Iterator<NIInstance> instanceIterator = networkInfo.instances.iterator( )
    while ( instanceIterator.hasNext( ) ) {
      NIInstance instance = instanceIterator.next( )
      if ( instance.vpc || instance.networkInterfaces ) {
        instanceIterator.remove( )
        removedResources.add( instance.name )
      }
    }

    Iterator<NISecurityGroup> securityGroupIterator = networkInfo.securityGroups.iterator( )
    while ( securityGroupIterator.hasNext( ) ) {
      NISecurityGroup securityGroup = securityGroupIterator.next()
      boolean remove = securityGroup.egressRules
      if ( !remove ) {
        remove = securityGroup.ingressRules.find{ NISecurityGroupIpPermission permission ->
          !NetworkRule.isValidProtocol( permission.protocol )
        }
      }
      if ( remove ) {
        securityGroupIterator.remove( )
        removedResources.add( securityGroup.name )
      }
    }

    Iterator<NIVpc> vpcIterator = networkInfo.vpcs.iterator( )
    while ( vpcIterator.hasNext( ) ) {
      NIVpc vpc = vpcIterator.next( )
      vpcIterator.remove( )
      removedResources.add( vpc.name )
    }

    Iterator<NIInternetGateway> internetGatewayIterator = networkInfo.internetGateways.iterator( )
    while ( internetGatewayIterator.hasNext( ) ) {
      NIInternetGateway internetGateway = internetGatewayIterator.next( )
      internetGatewayIterator.remove( )
      removedResources.add( internetGateway.name )
    }

    Iterator<NIDhcpOptionSet> dhcpOptionSetIterator = networkInfo.dhcpOptionSets.iterator( )
    while ( dhcpOptionSetIterator.hasNext( ) ) {
      NIDhcpOptionSet dhcpOptionSet = dhcpOptionSetIterator.next( )
      dhcpOptionSetIterator.remove( )
      removedResources.add( dhcpOptionSet.name )
    }

    removedResources
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
  static enum NetworkConfigurationToNetworkInfo implements Function<NetworkConfiguration, NetworkInfo> {
    INSTANCE;

    @Override
    NetworkInfo apply( final NetworkConfiguration networkConfiguration ) {
      ManagedSubnet managedSubnet = networkConfiguration.managedSubnet
      new NetworkInfo(
          configuration: new NIConfiguration(
              properties: [
                  new NIProperty( name: 'mode', values: [ networkConfiguration.mode ?: 'EDGE' ] ),
                  networkConfiguration.publicIps ?
                      new NIProperty( name: 'publicIps', values: networkConfiguration.publicIps ) :
                      null
              ].findAll( ) as List<NIProperty>,
              midonet: networkConfiguration?.mido ? new NIMidonet(
                  name: "mido",
                  gateways: networkConfiguration?.mido?.gatewayHost ?
                      new NIMidonetGateways(
                          name: 'gateways',
                          gateways : [
                              new NIMidonetGateway(
                                  properties: [
                                      networkConfiguration?.mido?.gatewayHost ?
                                          new NIProperty(
                                              name: 'gatewayHost',
                                              values: [ networkConfiguration.mido.gatewayHost ] ) :
                                          null,
                                      networkConfiguration?.mido?.gatewayIP ?
                                          new NIProperty(
                                              name: 'gatewayIP',
                                              values: [ networkConfiguration.mido.gatewayIP ] ) :
                                          null,
                                      networkConfiguration?.mido?.gatewayInterface ?
                                          new NIProperty(
                                              name: 'gatewayInterface',
                                              values: [ networkConfiguration.mido.gatewayInterface ] ) :
                                          null,
                                  ].findAll( ) as List<NIProperty>,
                              )
                          ]
                      ) :
                      networkConfiguration?.mido?.gateways ?
                          new NIMidonetGateways(
                              name: 'gateways',
                              gateways : networkConfiguration?.mido?.gateways?.collect{ MidonetGateway gateway ->
                                new NIMidonetGateway(
                                    properties: ( gateway.gatewayIP?
                                    [ // legacy format
                                        new NIProperty( name: 'gatewayHost', values: [ gateway.gatewayHost ] ),
                                        new NIProperty( name: 'gatewayIP', values: [ gateway.gatewayIP ] ),
                                        new NIProperty( name: 'gatewayInterface', values: [ gateway.gatewayInterface ] ),
                                    ] : [
                                        new NIProperty( name: 'ip', values: [ gateway.ip ] ),
                                        new NIProperty( name: 'externalCidr', values: [ gateway.externalCidr ] ),
                                        new NIProperty( name: 'externalDevice', values: [ gateway.externalDevice ] ),
                                        new NIProperty( name: 'externalIp', values: [ gateway.externalIp ] ),
                                        gateway.externalRouterIp ?
                                            new NIProperty( name: 'externalRouterIp', values: [ gateway.externalRouterIp ] ) :
                                            null,
                                        gateway.bgpPeerIp ?
                                            new NIProperty( name: 'bgpPeerIp', values: [ gateway.bgpPeerIp ] ) :
                                            null,
                                        gateway.bgpPeerAsn ?
                                          new NIProperty( name: 'bgpPeerAsn', values: [ gateway.bgpPeerAsn ] ) :
                                          null,
                                        gateway.bgpAdRoutes ?
                                          new NIProperty( name: 'bgpAdRoutes', values: gateway.bgpAdRoutes ) :
                                          null,
                                    ] ).findAll( ) as List<NIProperty>,
                                )
                              } as List<NIMidonetGateway>
                          ) :
                          null,
                  properties: [
                      networkConfiguration?.mido?.eucanetdHost ?
                          new NIProperty( name: 'eucanetdHost', values: [ networkConfiguration.mido.eucanetdHost ] ) :
                          null,
                      networkConfiguration?.mido?.bgpAsn ?
                          new NIProperty( name: 'bgpAsn', values: [ networkConfiguration.mido.bgpAsn ] ) :
                          null,
                      networkConfiguration?.mido?.publicNetworkCidr ?
                          new NIProperty( name: 'publicNetworkCidr', values: [ networkConfiguration.mido.publicNetworkCidr ] ) :
                          null,
                      networkConfiguration?.mido?.publicGatewayIP ?
                          new NIProperty( name: 'publicGatewayIP', values: [ networkConfiguration.mido.publicGatewayIP ] ) :
                          null,
                  ].findAll( ) as List<NIProperty>,
              ) : null,
              subnets: networkConfiguration.subnets ? new NISubnets(
                  name: "subnets",
                  subnets: networkConfiguration.subnets.collect{ EdgeSubnet subnet ->
                    new NISubnet(
                        name: subnet.subnet,  // broadcast name is always the subnet value
                        properties: [
                            new NIProperty( name: 'subnet', values: [ subnet.subnet ]),
                            new NIProperty( name: 'netmask', values: [ subnet.netmask ]),
                            new NIProperty( name: 'gateway', values: [ subnet.gateway ])
                        ]
                    )
                  }
              ) : null,
              managedSubnet: managedSubnet ? new NIManagedSubnets(
                  name: "managedSubnet",
                  managedSubnet: new NIManagedSubnet(
                      name: managedSubnet.subnet,  // broadcast name is always the subnet value
                      properties: [
                          new NIProperty( name: 'subnet', values: [ managedSubnet.subnet ] ),
                          new NIProperty( name: 'netmask', values: [ managedSubnet.netmask ] ),
                          new NIProperty( name: 'minVlan', values: [ ( managedSubnet.minVlan ?: ManagedSubnet.MIN_VLAN )  as String ] ),
                          new NIProperty( name: 'maxVlan', values: [ ( managedSubnet.maxVlan ?: ManagedSubnet.MAX_VLAN ) as String ] ),
                          new NIProperty( name: 'segmentSize', values: [ ( managedSubnet.segmentSize ?: ManagedSubnet.DEF_SEGMENT_SIZE ) as String ] )
                      ]
                  )
              ) : null
          )
      )
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
  static enum SubnetToSubnetNetworkView implements Function<Subnet,SubnetNetworkView> {
    INSTANCE;

    @Override
    SubnetNetworkView apply( final Subnet subnet ) {
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
  static enum NetworkAclEntryNetworkViewToNINetworkAclRule implements Function<NetworkAclEntryNetworkView,NINetworkAclEntry> {
    INSTANCE;

    @Override
    NINetworkAclEntry apply(@Nullable final NetworkAclEntryNetworkView networkAclEntry ) {
      new NINetworkAclEntry(
          networkAclEntry.number,
          networkAclEntry.protocol,
          networkAclEntry.action,
          networkAclEntry.cidr,
          networkAclEntry.icmpCode,
          networkAclEntry.icmpType,
          networkAclEntry.portRangeFrom,
          networkAclEntry.portRangeTo
      )
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
  static enum RouteNetworkViewToNIRoute implements Function<RouteNetworkView,NIRoute> {
    INSTANCE;

    @Override
    NIRoute apply(@Nullable final RouteNetworkView routeNetworkView) {
      new NIRoute(
          routeNetworkView.destinationCidr,
          routeNetworkView.gatewayId?:(routeNetworkView.networkInterfaceId||routeNetworkView.natGatewayId?null:'local'),
          routeNetworkView.networkInterfaceId,
          routeNetworkView.natGatewayId
      )
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
