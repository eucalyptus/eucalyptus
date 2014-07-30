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
package com.eucalyptus.network

import com.eucalyptus.bootstrap.Bootstrap
import com.eucalyptus.bootstrap.Databases
import com.eucalyptus.bootstrap.Hosts
import com.eucalyptus.cluster.Cluster
import com.eucalyptus.cluster.Clusters
import com.eucalyptus.cluster.NICluster
import com.eucalyptus.cluster.NIClusters
import com.eucalyptus.cluster.NIConfiguration
import com.eucalyptus.cluster.NIDhcpOptionSet
import com.eucalyptus.cluster.NIInstance
import com.eucalyptus.cluster.NIInternetGateway
import com.eucalyptus.cluster.NINetworkAcl
import com.eucalyptus.cluster.NINetworkAclEntry
import com.eucalyptus.cluster.NINetworkInterface
import com.eucalyptus.cluster.NINode
import com.eucalyptus.cluster.NINodes
import com.eucalyptus.cluster.NIProperty
import com.eucalyptus.cluster.NIRoute
import com.eucalyptus.cluster.NIRouteTable
import com.eucalyptus.cluster.NISecurityGroup
import com.eucalyptus.cluster.NISecurityGroupIpPermission
import com.eucalyptus.cluster.NISubnet
import com.eucalyptus.cluster.NISubnets
import com.eucalyptus.cluster.NIVpc
import com.eucalyptus.cluster.NIVpcSubnet
import com.eucalyptus.cluster.NetworkInfo
import com.eucalyptus.cluster.callback.BroadcastNetworkInfoCallback
import com.eucalyptus.component.Topology
import com.eucalyptus.component.id.Eucalyptus
import com.eucalyptus.compute.vpc.DhcpOption
import com.eucalyptus.compute.vpc.DhcpOptionSet
import com.eucalyptus.compute.vpc.InternetGateway
import com.eucalyptus.compute.vpc.NetworkAcl
import com.eucalyptus.compute.vpc.NetworkAclEntry
import com.eucalyptus.compute.vpc.NetworkAcls
import com.eucalyptus.compute.vpc.NetworkInterface as VpcNetworkInterface
import com.eucalyptus.compute.vpc.Route
import com.eucalyptus.compute.vpc.RouteTable
import com.eucalyptus.compute.vpc.Vpc
import com.eucalyptus.compute.vpc.Subnet as VpcSubnet
import com.eucalyptus.entities.EntityCache
import com.eucalyptus.event.ClockTick
import com.eucalyptus.event.Listeners
import com.eucalyptus.event.EventListener as EucaEventListener
import com.eucalyptus.network.config.Cluster as ConfigCluster
import com.eucalyptus.network.config.NetworkConfiguration
import com.eucalyptus.network.config.NetworkConfigurations
import com.eucalyptus.network.config.Subnet
import com.eucalyptus.system.BaseDirectory
import com.eucalyptus.system.Threads
import com.eucalyptus.util.Strings as EucaStrings
import com.eucalyptus.util.TypeMapper
import com.eucalyptus.util.TypeMappers
import com.eucalyptus.util.async.AsyncRequests
import com.eucalyptus.util.async.UnconditionalCallback
import com.eucalyptus.vm.VmInstance
import com.eucalyptus.vm.VmInstance.VmState
import com.eucalyptus.vm.VmInstances
import com.eucalyptus.vm.VmNetworkConfig
import com.google.common.base.Charsets
import com.google.common.base.Function
import com.google.common.base.Optional
import com.google.common.base.Predicate
import com.google.common.base.Strings
import com.google.common.base.Supplier
import com.google.common.base.Suppliers
import com.google.common.collect.ArrayListMultimap
import com.google.common.collect.HashMultimap
import com.google.common.collect.ImmutableList
import com.google.common.collect.Iterables
import com.google.common.collect.ListMultimap
import com.google.common.collect.Lists
import com.google.common.collect.Maps
import com.google.common.collect.Multimap
import com.google.common.collect.Sets
import com.google.common.io.Files as GFiles
import edu.ucsb.eucalyptus.cloud.NodeInfo
import edu.ucsb.eucalyptus.msgs.BroadcastNetworkInfoResponseType
import groovy.transform.CompileStatic
import groovy.transform.Immutable
import groovy.transform.PackageScope
import org.apache.log4j.Logger

import javax.annotation.Nullable
import javax.xml.bind.JAXBContext
import java.nio.file.Files as JFiles
import java.nio.file.StandardCopyOption
import java.util.concurrent.Callable
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

import static com.eucalyptus.vm.VmInstance.VmStateSet.TORNDOWN

/**
 *
 */
@CompileStatic
class NetworkInfoBroadcaster {
  private static final Logger logger = Logger.getLogger( NetworkInfoBroadcaster )

  private static final AtomicLong lastBroadcastTime = new AtomicLong( 0L );
  private static final ConcurrentMap<String,Long> activeBroadcastMap = Maps.<String,Long>newConcurrentMap( ) as ConcurrentMap<String, Long>
  private static final EntityCache<VmInstance,VmInstanceNetworkView> instanceCache = new EntityCache<>( VmInstance.named(null), TypeMappers.lookup( VmInstance, VmInstanceNetworkView )  );
  private static final EntityCache<NetworkGroup,NetworkGroupNetworkView> securityGroupCache = new EntityCache<>( NetworkGroup.withNaturalId( null ), TypeMappers.lookup( NetworkGroup, NetworkGroupNetworkView )  );

  private static final EntityCache<Vpc,VpcNetworkView> vpcCache = new EntityCache<>( Vpc.exampleWithOwner( null ), TypeMappers.lookup( Vpc, VpcNetworkView )  );
  private static final EntityCache<VpcSubnet,SubnetNetworkView> subnetCache = new EntityCache<>( VpcSubnet.exampleWithOwner( null ), TypeMappers.lookup( VpcSubnet, SubnetNetworkView )  );
  private static final EntityCache<DhcpOptionSet,DhcpOptionSetNetworkView> dhcpOptionsCache = new EntityCache<>( DhcpOptionSet.exampleWithOwner( null ), TypeMappers.lookup( DhcpOptionSet, DhcpOptionSetNetworkView )  );
  private static final EntityCache<NetworkAcl,NetworkAclNetworkView> networkAclCache = new EntityCache<>( NetworkAcl.exampleWithOwner( null ), TypeMappers.lookup( NetworkAcl, NetworkAclNetworkView )  );
  private static final EntityCache<RouteTable,RouteTableNetworkView> routeTableCache = new EntityCache<>( RouteTable.exampleWithOwner( null ), TypeMappers.lookup( RouteTable, RouteTableNetworkView )  );
  private static final EntityCache<InternetGateway,InternetGatewayNetworkView> internetGatewayCache = new EntityCache<>( InternetGateway.exampleWithOwner( null ), TypeMappers.lookup( InternetGateway, InternetGatewayNetworkView )  );
  private static final EntityCache<VpcNetworkInterface,NetworkInterfaceNetworkView> networkInterfaceCache = new EntityCache<>( VpcNetworkInterface.exampleWithOwner( null ), TypeMappers.lookup( VpcNetworkInterface, NetworkInterfaceNetworkView )  );

  interface NetworkInfoSource {
    Iterable<VmInstanceNetworkView> getInstances( );
    Iterable<NetworkGroupNetworkView> getSecurityGroups( );
    Iterable<VpcNetworkView> getVpcs( );
    Iterable<SubnetNetworkView> getSubnets( );
    Iterable<DhcpOptionSetNetworkView> getDhcpOptionSets( );
    Iterable<NetworkAclNetworkView> getNetworkAcls( );
    Iterable<RouteTableNetworkView> getRouteTables( );
    Iterable<InternetGatewayNetworkView> getInternetGateways( );
    Iterable<NetworkInterfaceNetworkView> getNetworkInterfaces( );
  }

  private static NetworkInfoSource cacheSource( ) {
    new NetworkInfoSource( ) {
      @Override Iterable<VmInstanceNetworkView> getInstances( ) { instanceCache.get( ) }
      @Override Iterable<NetworkGroupNetworkView> getSecurityGroups( ) { securityGroupCache.get( ) }
      @Override Iterable<VpcNetworkView> getVpcs( ) { vpcCache.get( ) }
      @Override Iterable<SubnetNetworkView> getSubnets( ) { subnetCache.get( ) }
      @Override Iterable<DhcpOptionSetNetworkView> getDhcpOptionSets( ) { dhcpOptionsCache.get( ) }
      @Override Iterable<NetworkAclNetworkView> getNetworkAcls( ) { networkAclCache.get( ) }
      @Override Iterable<RouteTableNetworkView> getRouteTables( ) { routeTableCache.get( ) }
      @Override Iterable<InternetGatewayNetworkView> getInternetGateways( ) { internetGatewayCache.get( ) }
      @Override Iterable<NetworkInterfaceNetworkView> getNetworkInterfaces( ) { networkInterfaceCache.get( ) }
    }
  }

  static void requestNetworkInfoBroadcast( ) {
    final long requestedTime = System.currentTimeMillis( )
    Callable broadcastRequest = Closure.IDENTITY
    broadcastRequest = {
          final long currentTime = System.currentTimeMillis( )
          final long lastBroadcast = lastBroadcastTime.get( )
          if ( requestedTime >= lastBroadcast &&
              lastBroadcast + TimeUnit.SECONDS.toMillis( NetworkGroups.MIN_BROADCAST_INTERVAL ) < currentTime  ) {
            if ( lastBroadcastTime.compareAndSet( lastBroadcast, currentTime ) ) {
              try {
                broadcastNetworkInfo( )
              } catch( e ) {
                logger.error( "Error broadcasting network information", e )
              }
            } else { // re-evaluate
              broadcastTask( broadcastRequest )
            }
          } else if ( requestedTime >= lastBroadcastTime.get() ) {
            sleep( 100 ) // pause and re-evaluate to allow for min time between broadcasts
            broadcastTask( broadcastRequest )
          }
        }
    broadcastTask( broadcastRequest )
  }

  private static void broadcastTask( Callable task ) {
    Threads.enqueue( Eucalyptus, NetworkInfoBroadcaster, 5, task )
  }

  @SuppressWarnings("UnnecessaryQualifiedReference")
  static void broadcastNetworkInfo( ){
    // populate with info directly from configuration
    final Optional<NetworkConfiguration> networkConfiguration = NetworkConfigurations.networkConfiguration
    final List<Cluster> clusters = Clusters.getInstance( ).listValues( )

    final NetworkInfo info = NetworkInfoBroadcaster.buildNetworkConfiguration(
        networkConfiguration,
        cacheSource( ),
        Suppliers.ofInstance( clusters ) as Supplier<List<Cluster>>,
        { Topology.lookup(Eucalyptus).inetAddress.hostAddress } as Supplier<String>,
        NetworkConfigurations.&loadSystemNameservers as Function<List<String>,List<String>> )

    final JAXBContext jc = JAXBContext.newInstance( "com.eucalyptus.cluster" )
    final StringWriter writer = new StringWriter( 8192 )
    jc.createMarshaller().marshal( info, writer )

    final String networkInfo = writer.toString( )
    if ( logger.isTraceEnabled( ) ) {
      logger.trace( "Broadcasting network information:\n${networkInfo}" )
    }

    final File newView = BaseDirectory.RUN.getChildFile( "global_network_info.xml.temp" )
    if ( !newView.delete( ) ) logger.warn( "Error deleting stale network view ${newView.getAbsolutePath()}" )
    GFiles.write( networkInfo, newView, Charsets.UTF_8 )
    JFiles.move( newView.toPath( ), BaseDirectory.RUN.getChildFile( "global_network_info.xml" ).toPath( ), StandardCopyOption.REPLACE_EXISTING )

    final BroadcastNetworkInfoCallback callback = new BroadcastNetworkInfoCallback( networkInfo )
    clusters.each { Cluster cluster ->
      final Long broadcastTime = System.currentTimeMillis( )
      if ( null == activeBroadcastMap.putIfAbsent( cluster.partition, broadcastTime ) ) {
        try {
          AsyncRequests.newRequest( callback.newInstance( ) ).then( new UnconditionalCallback<BroadcastNetworkInfoResponseType>() {
            @Override
            void fire() {
              activeBroadcastMap.remove( cluster.partition, broadcastTime )
            }
          } ).dispatch( cluster.configuration )
        } catch ( e ) {
          activeBroadcastMap.remove( cluster.partition, broadcastTime )
          logger.error( "Error broadcasting network information to cluster ${cluster.partition} (${cluster.name})" as String, e )
        }
      } else {
        logger.warn( "Skipping network information broadcast for active partition ${cluster.partition}" )
      }
      void
    }
  }

  @PackageScope
  static NetworkInfo buildNetworkConfiguration( final Optional<NetworkConfiguration> configuration,
                                                final NetworkInfoSource networkInfoSource,
                                                final Supplier<List<Cluster>> clusterSupplier,
                                                final Supplier<String> clcHostSupplier,
                                                final Function<List<String>,List<String>> systemNameserverLookup ) {
    Iterable<Cluster> clusters = clusterSupplier.get( )
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
          configCluster && configCluster.subnet ?
            new NICluster(
                name: configCluster.name,
                subnet: new NISubnet(
                    name: configCluster.subnet.subnet, // broadcast name is always the subnet value
                    properties: [
                        new NIProperty( name: 'subnet', values: [ configCluster.subnet.subnet ]),
                        new NIProperty( name: 'netmask', values: [ configCluster.subnet.netmask ]),
                        new NIProperty( name: 'gateway', values: [ configCluster.subnet.gateway ])
                    ]
                ),
                properties: [
                    new NIProperty( name: 'enabledCCIp', values: [ InetAddress.getByName(cluster.hostName).hostAddress ]),
                    new NIProperty( name: 'macPrefix', values: [ configCluster.macPrefix?:VmInstances.MAC_PREFIX ] ),
                    new NIProperty( name: 'privateIps', values: configCluster.privateIps )
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
        new NIProperty( name: 'instanceDNSServers', values: dnsServers ),
    ]  )

    Iterable<VmInstanceNetworkView> instances = Iterables.filter(
        networkInfoSource.instances,
        { VmInstanceNetworkView instance -> !TORNDOWN.contains(instance.state) } as Predicate<VmInstanceNetworkView> )

    // populate nodes
    ((Multimap<List<String>,String>) instances.inject( HashMultimap.create( ) ){
      Multimap<List<String>,String> map, VmInstanceNetworkView instance ->
        map.put( [ instance.partition, instance.node ], instance.instanceId )
        map
    }).asMap().each{ Map.Entry<List<String>,Collection<String>> entry ->
      info.configuration.clusters.clusters.find{ NICluster cluster -> cluster.name == entry.key[0] }?.with{
        NINode node = nodes.nodes.find{ NINode node -> node.name == entry.key[1] }
        if ( node ) {
          node.instanceIds = entry.value ? entry.value as List<String> : null
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
    Map<String,List<String>> vpcIdToInternetGatewayIds = (Map<String,List<String>> ) ((ArrayListMultimap<String,String>)internetGateways.inject(ArrayListMultimap.<String,String>create()){
      ListMultimap<String,String> map, InternetGatewayNetworkView internetGateway ->
        if ( internetGateway.vpcId ) map.put( internetGateway.vpcId, internetGateway.internetGatewayId )
        map
    }).asMap( )
    info.vpcs.addAll( vpcs.collect{ VpcNetworkView vpc ->
      new NIVpc(
        vpc.vpcId,
        vpc.ownerAccountNumber,
        vpc.cidr,
        vpc.dhcpOptionSetId,
        subnets.findAll{ SubnetNetworkView subnet -> subnet.vpcId == vpc.vpcId }.collect{ SubnetNetworkView subnet ->
          new NIVpcSubnet(
              name: subnet.subnetId,
              ownerId: subnet.ownerAccountNumber,
              cidr: subnet.cidr,
              cluster: subnet.availabilityZone,
              networkAcl: subnet.networkAcl,
              routeTable: subnet.routeTable
          )
        },
        networkAcls.findAll{ NetworkAclNetworkView networkAcl -> networkAcl.vpcId == vpc.vpcId }.collect { NetworkAclNetworkView networkAcl ->
          new NINetworkAcl(
              name: networkAcl.networkAclId,
              ownerId: networkAcl.ownerAccountNumber,
              ingressEntries: Lists.transform( networkAcl.ingressRules, TypeMappers.lookup( NetworkAclEntryNetworkView, NINetworkAclEntry ) ) as List<NINetworkAclEntry>,
              egressEntries: Lists.transform( networkAcl.egressRules, TypeMappers.lookup( NetworkAclEntryNetworkView, NINetworkAclEntry ) ) as List<NINetworkAclEntry>
          )
        },
        routeTables.findAll{ RouteTableNetworkView routeTable -> routeTable.vpcId == vpc.vpcId }.collect { RouteTableNetworkView routeTable ->
          new NIRouteTable(
              name: routeTable.routeTableId,
              ownerId: routeTable.ownerAccountNumber,
              routes: Lists.transform( routeTable.routes, TypeMappers.lookup( RouteNetworkView, NIRoute ) ) as List<NIRoute>
          )
        },
        vpcIdToInternetGatewayIds.get( vpc.vpcId )?:[] as List<String>
      )
    } )

    // populate instances
    Iterable<NetworkInterfaceNetworkView> networkInterfaces = networkInfoSource.networkInterfaces
    Map<String,List<NetworkInterfaceNetworkView>> instanceIdToNetworkInterfaces = (Map<String,List<NetworkInterfaceNetworkView>> ) ((ArrayListMultimap<String,NetworkInterfaceNetworkView>) networkInterfaces.inject(ArrayListMultimap.<String,NetworkInterfaceNetworkView>create()){
      ListMultimap<String,NetworkInterfaceNetworkView> map, NetworkInterfaceNetworkView networkInterface ->
        if ( networkInterface.instanceId ) map.put( networkInterface.instanceId, networkInterface )
        map
    }).asMap( )
    info.instances.addAll( instances.collect{ VmInstanceNetworkView instance ->
      new NIInstance(
          name: instance.instanceId,
          ownerId: instance.ownerAccountNumber,
          vpc: instance.vpcId,
          subnet: instance.subnetId,
          macAddress: Strings.emptyToNull( instance.macAddress ),
          publicIp: VmNetworkConfig.DEFAULT_IP==instance.publicAddress||PublicAddresses.isDirty(instance.publicAddress) ? null : instance.publicAddress,
          privateIp: instance.privateAddress,
          securityGroups: instance.securityGroupIds,
          networkInterfaces: instanceIdToNetworkInterfaces.get( instance.instanceId )?.collect{ NetworkInterfaceNetworkView networkInterface ->
            new NINetworkInterface(
                name: networkInterface.networkInterfaceId,
                ownerId: networkInterface.ownerAccountNumber,
                deviceIndex: networkInterface.deviceIndex,
                macAddress: networkInterface.macAddress,
                privateIp: networkInterface.privateIp,
                sourceDestCheck: networkInterface.sourceDestCheck,
                securityGroups: instance.securityGroupIds //TODO:STEVE: use network interface security groups here
            )
          } ?: [ ] as List<NINetworkInterface>
      )
    } )

    // populate dhcp option sets
    Iterable<DhcpOptionSetNetworkView> dhcpOptionSets = networkInfoSource.dhcpOptionSets
    info.dhcpOptionSets.addAll( dhcpOptionSets.collect { DhcpOptionSetNetworkView dhcpOptionSet ->
      new NIDhcpOptionSet(
          name: dhcpOptionSet.dhcpOptionSetId,
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
    info.internetGateways.addAll( internetGateways.collect { InternetGatewayNetworkView internetGateway ->
      new NIInternetGateway(
          name: internetGateway.internetGatewayId,
          ownerId: internetGateway.ownerAccountNumber,
      )
    } )

    // populate security groups
    Set<String> activeSecurityGroups = (Set<String>) instances.inject( Sets.newHashSetWithExpectedSize( 1000 ) ){
      Set<String> groups, VmInstanceNetworkView instance -> groups.addAll( instance.securityGroupIds ); groups
    }
    Iterable<NetworkGroupNetworkView> groups = networkInfoSource.securityGroups
    info.securityGroups.addAll( groups.findAll{  NetworkGroupNetworkView group -> activeSecurityGroups.contains( group.groupId ) }.collect{ NetworkGroupNetworkView group ->
      new NISecurityGroup(
          name: group.groupId,
          ownerId: group.ownerAccountNumber,
          rules: group.rules,
          ingressRules: group.ingressPermissions.collect{ IPPermissionNetworkView ipPermission ->
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
          egressRules: group.egressPermissions.collect{ IPPermissionNetworkView ipPermission ->
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

    if ( logger.isTraceEnabled( ) ) {
      logger.trace( "Constructed network information for ${Iterables.size( instances )} instance(s), ${Iterables.size( groups )} security group(s)" )
    }

    info
  }

  private static Set<String> explodeRules( NetworkRule networkRule ) {
    Set<String> rules = Sets.newLinkedHashSet( )
    // Only EC2-Classic rules supported by this format
    if ( !networkRule.isVpcOnly( ) ) {
      String rule = String.format(
          "-P %s -%s %d%s%d ",
          networkRule.protocol,
          NetworkRule.Protocol.icmp == networkRule.protocol ? "t" : "p",
          networkRule.lowPort,
          NetworkRule.Protocol.icmp == networkRule.protocol ? ":" : "-",
          networkRule.highPort);
      rules.addAll(networkRule.networkPeers.collect { NetworkPeer peer ->
        String.format("%s -o %s -u %s", rule, peer.groupId, peer.userQueryKey)
      })
      rules.addAll(networkRule.ipRanges.collect { String cidr ->
        String.format("%s -s %s", rule, cidr)
      })
    }
    rules
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

  @TypeMapper
  enum NetworkConfigurationToNetworkInfo implements Function<NetworkConfiguration, NetworkInfo> {
    INSTANCE;

    @Override
    NetworkInfo apply( final NetworkConfiguration networkConfiguration ) {
      new NetworkInfo(
          configuration: new NIConfiguration(
              properties: ( networkConfiguration.publicIps ? [
                  new NIProperty( name: 'publicIps', values: networkConfiguration.publicIps )
              ] : [ ] ) as List<NIProperty>,
              subnets: networkConfiguration.subnets ? new NISubnets(
                  name: "subnets",
                  subnets: networkConfiguration.subnets.collect{ Subnet subnet ->
                      new NISubnet(
                          name: subnet.subnet,  // broadcast name is always the subnet value
                          properties: [
                              new NIProperty( name: 'subnet', values: [ subnet.subnet ]),
                              new NIProperty( name: 'netmask', values: [ subnet.netmask ]),
                              new NIProperty( name: 'gateway', values: [ subnet.gateway ])
                          ]
                      )
                  }
              ) : null
          )
      )
    }
  }

  @Immutable
  static class VmInstanceNetworkView {
    String instanceId
    VmState state
    String ownerAccountNumber
    String vpcId
    String subnetId
    String macAddress
    String privateAddress
    String publicAddress
    String partition
    String node
    List<String> securityGroupIds
  }

  @TypeMapper
  enum VmInstanceToVmInstanceNetworkView implements Function<VmInstance,VmInstanceNetworkView> {
    INSTANCE;

    @Override
    VmInstanceNetworkView apply( final VmInstance instance) {
      new VmInstanceNetworkView(
          instance.instanceId,
          instance.state,
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
  static class NetworkGroupNetworkView {
    String groupId
    String ownerAccountNumber
    List<String> rules
    List<IPPermissionNetworkView> ingressPermissions
    List<IPPermissionNetworkView> egressPermissions
  }

  @TypeMapper
  enum NetworkGroupToNetworkGroupNetworkView implements Function<NetworkGroup,NetworkGroupNetworkView> {
    INSTANCE;

    @SuppressWarnings("UnnecessaryQualifiedReference")
    @Override
    NetworkGroupNetworkView apply( final NetworkGroup group ) {
      new NetworkGroupNetworkView(
          group.groupId,
          group.ownerAccountNumber,
          group.ingressNetworkRules.collect{ NetworkRule networkRule -> NetworkInfoBroadcaster.explodeRules( networkRule ) }.flatten( ) as List<String>,
          group.ingressNetworkRules.collect{ NetworkRule networkRule -> NetworkInfoBroadcaster.explodePermissions( networkRule ) }.flatten( ) as List<IPPermissionNetworkView>,
          group.egressNetworkRules.collect{ NetworkRule networkRule -> NetworkInfoBroadcaster.explodePermissions( networkRule ) }.flatten( ) as List<IPPermissionNetworkView>
      )
    }
  }

  @Immutable
  static class VpcNetworkView {
    String vpcId
    String ownerAccountNumber
    String cidr
    String dhcpOptionSetId
  }

  @TypeMapper
  enum VpcToVpcNetworkView implements Function<Vpc,VpcNetworkView> {
    INSTANCE;

    @Override
    VpcNetworkView apply( final Vpc vpc ) {
      new VpcNetworkView(
          vpc.displayName,
          vpc.ownerAccountNumber,
          vpc.cidr,
          vpc.dhcpOptionSet.displayName
      )
    }
  }

  @Immutable
  static class SubnetNetworkView {
    String subnetId
    String ownerAccountNumber
    String vpcId
    String cidr
    String availabilityZone
    String networkAcl
    String routeTable
  }

  @TypeMapper
  enum SubnetToSubnetNetworkView implements Function<VpcSubnet,SubnetNetworkView> {
    INSTANCE;

    @Override
    SubnetNetworkView apply( final VpcSubnet subnet ) {
      new SubnetNetworkView(
          subnet.displayName,
          subnet.ownerAccountNumber,
          subnet.vpc.displayName,
          subnet.cidr,
          subnet.availabilityZone,
          subnet.networkAcl.displayName,
          subnet.routeTable.displayName
      )
    }
  }

  @Immutable
  static class DhcpOptionSetNetworkView {
    String dhcpOptionSetId
    String ownerAccountNumber
    List<DhcpOptionNetworkView> options
  }

  @Immutable
  static class DhcpOptionNetworkView {
    String key
    List<String> values
  }

  @TypeMapper
  enum DhcpOptionSetToDhcpOptionSetNetworkView implements Function<DhcpOptionSet,DhcpOptionSetNetworkView> {
    INSTANCE;

    @Override
    DhcpOptionSetNetworkView apply( final DhcpOptionSet dhcpOptionSet ) {
      new DhcpOptionSetNetworkView(
          dhcpOptionSet.displayName,
          dhcpOptionSet.ownerAccountNumber,
          ImmutableList.copyOf( dhcpOptionSet.dhcpOptions.collect{ DhcpOption option -> new DhcpOptionNetworkView(
              option.key,
              ImmutableList.copyOf( option.values )
          ) } )
      )
    }
  }

  @Immutable
  static class NetworkAclNetworkView {
    String networkAclId
    String ownerAccountNumber
    String vpcId
    List<NetworkAclEntryNetworkView> ingressRules
    List<NetworkAclEntryNetworkView> egressRules
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
  enum NetworkAclToNetworkAclNetworkView implements Function<NetworkAcl,NetworkAclNetworkView> {
    INSTANCE;

    @Override
    NetworkAclNetworkView apply( final NetworkAcl networkAcl ) {
      List<NetworkAclEntry> orderedEntries = NetworkAcls.ENTRY_ORDERING.sortedCopy( networkAcl.entries )
      new NetworkAclNetworkView(
          networkAcl.displayName,
          networkAcl.ownerAccountNumber,
          networkAcl.vpc.displayName,
          ImmutableList.copyOf( orderedEntries.findAll{ NetworkAclEntry entry -> !entry.egress }.collect{ NetworkAclEntry entry -> TypeMappers.transform( entry, NetworkAclEntryNetworkView  ) } ),
          ImmutableList.copyOf( orderedEntries.findAll{ NetworkAclEntry entry -> entry.egress }.collect{ NetworkAclEntry entry -> TypeMappers.transform( entry, NetworkAclEntryNetworkView  ) } )
      )
    }
  }

  @TypeMapper
  enum NetworkAclEntryToNetworkAclEntryNetworkView implements Function<NetworkAclEntry,NetworkAclEntryNetworkView> {
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
  enum NetworkAclEntryNetworkViewToNINetworkAclRule implements Function<NetworkAclEntryNetworkView,NINetworkAclEntry> {
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
  static class RouteTableNetworkView {
    String routeTableId
    String ownerAccountNumber
    String vpcId
    List<RouteNetworkView> routes
  }

  @Immutable
  static class RouteNetworkView {
    String destinationCidr
    String gatewayId
  }

  @TypeMapper
  enum RouteTableToRouteTableNetworkView implements Function<RouteTable,RouteTableNetworkView> {
    INSTANCE;

    @Override
    RouteTableNetworkView apply( final RouteTable routeTable ) {
      new RouteTableNetworkView(
          routeTable.displayName,
          routeTable.ownerAccountNumber,
          routeTable.vpc.displayName,
          ImmutableList.copyOf( routeTable.routes.collect{ Route route -> TypeMappers.transform( route, RouteNetworkView ) } ),
      )
    }
  }

  @TypeMapper
  enum RouteToRouteNetworkView implements Function<Route,RouteNetworkView> {
    INSTANCE;

    @Override
    RouteNetworkView apply(@Nullable final Route route) {
      new RouteNetworkView(
          route.destinationCidr,
          route.getInternetGateway()?.displayName ?: 'local'
      )
    }
  }

  @TypeMapper
  enum RouteNetworkViewToNIRoute implements Function<RouteNetworkView,NIRoute> {
    INSTANCE;

    @Override
    NIRoute apply(@Nullable final RouteNetworkView routeNetworkView) {
      new NIRoute(
          routeNetworkView.destinationCidr,
          routeNetworkView.gatewayId
      )
    }
  }

  @Immutable
  static class InternetGatewayNetworkView {
    String internetGatewayId
    String ownerAccountNumber
    String vpcId
  }

  @TypeMapper
  enum InternetGatewayToInternetGatewayNetworkView implements Function<InternetGateway,InternetGatewayNetworkView> {
    INSTANCE;

    @Override
    InternetGatewayNetworkView apply( final InternetGateway internetGateway ) {
      new InternetGatewayNetworkView(
          internetGateway.displayName,
          internetGateway.ownerAccountNumber,
          internetGateway.vpc?.displayName
      )
    }
  }

  @Immutable
  static class NetworkInterfaceNetworkView {
    String networkInterfaceId
    String ownerAccountNumber
    String instanceId
    Integer deviceIndex
    String macAddress
    String privateIp
    Boolean sourceDestCheck
    List<String> securityGroupIds
  }

  @TypeMapper
  enum VpcNetworkInterfaceToNetworkInterfaceNetworkView implements Function<VpcNetworkInterface,NetworkInterfaceNetworkView> {
    INSTANCE;

    @Override
    NetworkInterfaceNetworkView apply( final VpcNetworkInterface networkInterface ) {
      new NetworkInterfaceNetworkView(
          networkInterface.displayName,
          networkInterface.ownerAccountNumber,
          networkInterface.attachment?.instanceId,
          networkInterface.attachment?.deviceIndex,
          networkInterface.macAddress,
          networkInterface.privateIpAddress,
          networkInterface.sourceDestCheck,
          [ ] as List<String> //TODO:STEVE: security groups for network interfaces
      )
    }
  }

  public static class NetworkInfoBroadcasterEventListener implements EucaEventListener<ClockTick> {
    private final int intervalTicks = 3
    private final int activeBroadcastTimeoutMins = 3
    private volatile int counter = 0

    public static void register( ) {
      Listeners.register( ClockTick.class, new NetworkInfoBroadcasterEventListener( ) )
    }

    @SuppressWarnings("UnnecessaryQualifiedReference")
    @Override
    public void fireEvent( final ClockTick event ) {
      NetworkInfoBroadcaster.activeBroadcastMap.each{ Map.Entry<String,Long> entry ->
        if ( entry.value + TimeUnit.MINUTES.toMillis( activeBroadcastTimeoutMins ) < System.currentTimeMillis( ) &&
            NetworkInfoBroadcaster.activeBroadcastMap.remove( entry.key, entry.value ) ) {
          logger.warn( "Timed out active network information broadcast for partition ${entry.key}" )
        }
      }

      if ( counter++%intervalTicks == 0 &&
          EdgeNetworking.enabled &&
          Hosts.coordinator &&
          !Bootstrap.isShuttingDown() &&
          !Databases.isVolatile() ) {
        requestNetworkInfoBroadcast( )
      }
    }
  }
}
