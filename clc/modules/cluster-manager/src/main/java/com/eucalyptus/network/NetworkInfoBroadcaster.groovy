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
import com.eucalyptus.cluster.NIInstance
import com.eucalyptus.cluster.NINode
import com.eucalyptus.cluster.NINodes
import com.eucalyptus.cluster.NIProperty
import com.eucalyptus.cluster.NISecurityGroup
import com.eucalyptus.cluster.NISubnet
import com.eucalyptus.cluster.NISubnets
import com.eucalyptus.cluster.NetworkInfo
import com.eucalyptus.cluster.callback.BroadcastNetworkInfoCallback
import com.eucalyptus.component.Topology
import com.eucalyptus.component.id.Eucalyptus
import com.eucalyptus.entities.EntityCache
import com.eucalyptus.event.ClockTick
import com.eucalyptus.event.Listeners
import com.eucalyptus.event.EventListener as EucaEventListener
import com.eucalyptus.network.config.Cluster as ConfigCluster
import com.eucalyptus.network.config.NetworkConfiguration
import com.eucalyptus.network.config.NetworkConfigurations
import com.eucalyptus.network.config.Subnet
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
import com.google.common.base.Function
import com.google.common.base.Optional
import com.google.common.base.Predicate
import com.google.common.base.Strings
import com.google.common.base.Supplier
import com.google.common.base.Suppliers
import com.google.common.collect.HashMultimap
import com.google.common.collect.Iterables
import com.google.common.collect.Maps
import com.google.common.collect.Multimap
import com.google.common.collect.Sets
import edu.ucsb.eucalyptus.cloud.NodeInfo
import edu.ucsb.eucalyptus.msgs.BroadcastNetworkInfoResponseType
import groovy.transform.CompileStatic
import groovy.transform.Immutable
import groovy.transform.PackageScope
import org.apache.log4j.Logger

import javax.xml.bind.JAXBContext
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
        Suppliers.ofInstance( clusters ) as Supplier<List<Cluster>>,
        instanceCache as Supplier<Iterable<VmInstanceNetworkView>>,
        securityGroupCache as Supplier<Iterable<NetworkGroupNetworkView>>,
        { Topology.lookup(Eucalyptus).inetAddress.hostAddress } as Supplier<String>,
        NetworkConfigurations.&loadSystemNameservers as Function<List<String>,List<String>> )

    final JAXBContext jc = JAXBContext.newInstance( "com.eucalyptus.cluster" )
    final StringWriter writer = new StringWriter( 8192 )
    jc.createMarshaller().marshal( info, writer )

    final String networkInfo = writer.toString( )
    if ( logger.isTraceEnabled( ) ) {
      logger.trace( "Broadcasting network information:\n${networkInfo}" )
    }

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
  static NetworkInfo buildNetworkConfiguration( final Optional<NetworkConfiguration> networkConfiguration,
                                                final Supplier<List<Cluster>> clusterSupplier,
                                                final Supplier<Iterable<VmInstanceNetworkView>> instanceSupplier,
                                                final Supplier<Iterable<NetworkGroupNetworkView>> securityGroupSupplier,
                                                final Supplier<String> clcHostSupplier,
                                                final Function<List<String>,List<String>> systemNameserverLookup ) {
    NetworkInfo info = networkConfiguration
        .transform( TypeMappers.lookup( NetworkConfiguration, NetworkInfo ) )
        .or( new NetworkInfo( ) )

    // populate clusters
    info.configuration.clusters = new NIClusters(
        name: 'clusters',
        clusters: clusterSupplier.get( ).collect{ Cluster cluster ->
          ConfigCluster configCluster = networkConfiguration.orNull()?.clusters?.find{ ConfigCluster configCluster -> cluster.partition == configCluster.name }
          Subnet subnet = networkConfiguration.present ?
              NetworkConfigurations.getSubnetForCluster( networkConfiguration.get( ), cluster.partition ) :
              null
          Collection<String> privateIpRanges = networkConfiguration.present ?
              NetworkConfigurations.getPrivateAddressRanges( networkConfiguration.get( ), cluster.partition ) :
              null
          new NICluster(
              name: (String)cluster.partition,
              subnet: subnet ?  new NISubnet(
                  name: subnet.subnet, // broadcast name is always the subnet value
                  properties: [
                      new NIProperty( name: 'subnet', values: [ subnet.subnet ]),
                      new NIProperty( name: 'netmask', values: [ subnet.netmask ]),
                      new NIProperty( name: 'gateway', values: [ subnet.gateway ])
                  ]
              ) : null,
              properties: ( [
                  new NIProperty( name: 'enabledCCIp', values: [ InetAddress.getByName(cluster.hostName).hostAddress ]),
                  new NIProperty( name: 'macPrefix', values: [ configCluster?.macPrefix?:networkConfiguration.orNull()?.macPrefix?:VmInstances.MAC_PREFIX ] ),
              ] + ( privateIpRanges ? [
                  new NIProperty( name: 'privateIps', values: privateIpRanges as List<String> )
              ] : [ ] as List<NIProperty> ) ) as List<NIProperty>,
              nodes: new NINodes(
                  name: 'nodes',
                  nodes: cluster.nodeMap.values().collect{ NodeInfo nodeInfo -> new NINode( name: nodeInfo.name ) }
              )
          )
        }
    )

    // populate dynamic properties
    info.configuration.properties.addAll( [
        new NIProperty( name: 'enabledCLCIp', values: [clcHostSupplier.get()]),
        new NIProperty( name: 'instanceDNSDomain', values: [networkConfiguration.orNull()?.instanceDnsDomain?:EucaStrings.trimPrefix('.',"${VmInstances.INSTANCE_SUBDOMAIN}.internal")]),
        new NIProperty( name: 'instanceDNSServers', values: networkConfiguration.orNull()?.instanceDnsServers?:systemNameserverLookup.apply(['127.0.0.1'])),
    ]  )

    Iterable<VmInstanceNetworkView> instances = Iterables.filter(
        instanceSupplier.get( ),
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

    // populate instances
    info.instances.addAll( instances.collect{ VmInstanceNetworkView instance ->
      new NIInstance(
          name: instance.instanceId,
          ownerId: instance.ownerAccountNumber,
          macAddress: Strings.emptyToNull( instance.macAddress ),
          publicIp: VmNetworkConfig.DEFAULT_IP==instance.publicAddress||PublicAddresses.isDirty(instance.publicAddress) ? null : instance.publicAddress,
          privateIp: instance.privateAddress,
          securityGroups: instance.securityGroupIds
      )
    } )

    // populate security groups
    Set<String> activeSecurityGroups = (Set<String>) instances.inject( Sets.newHashSetWithExpectedSize( 1000 ) ){
      Set<String> groups, VmInstanceNetworkView instance -> groups.addAll( instance.securityGroupIds ); groups
    }
    Iterable<NetworkGroupNetworkView> groups = securityGroupSupplier.get( )
    info.securityGroups.addAll( groups.findAll{  NetworkGroupNetworkView group -> activeSecurityGroups.contains( group.groupId ) }.collect{ NetworkGroupNetworkView group ->
      new NISecurityGroup(
          name: group.groupId,
          ownerId: group.ownerAccountNumber,
          rules: group.rules
      )
    } )

    if ( logger.isTraceEnabled( ) ) {
      logger.trace( "Constructed network information for ${Iterables.size( instances )} instance(s), ${Iterables.size( groups )} security group(s)" )
    }

    info
  }

  private static Set<String> explodeRules( NetworkRule networkRule ) {
    Set<String> rules = Sets.newLinkedHashSet( )
    String rule = String.format(
        "-P %s -%s %d%s%d ",
        networkRule.protocol,
        NetworkRule.Protocol.icmp == networkRule.protocol ? "t" : "p",
        networkRule.lowPort,
        NetworkRule.Protocol.icmp == networkRule.protocol ? ":" : "-",
        networkRule.highPort );
    rules.addAll( networkRule.networkPeers.collect{ NetworkPeer peer ->
      String.format( "%s -o %s -u %s", rule, peer.groupId, peer.userQueryKey )
    } )
    rules.addAll( networkRule.ipRanges.collect{ String cidr ->
      String.format( "%s -s %s", rule, cidr )
    } )
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
  static class NetworkGroupNetworkView {
    String groupId
    String ownerAccountNumber
    List<String> rules

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
          group.networkRules.collect{ NetworkRule networkRule -> NetworkInfoBroadcaster.explodeRules( networkRule ) }.flatten( ) as List<String>
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
