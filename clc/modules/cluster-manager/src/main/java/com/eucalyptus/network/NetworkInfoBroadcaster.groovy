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
import com.eucalyptus.entities.Entities
import com.eucalyptus.event.ClockTick
import com.eucalyptus.event.Listeners
import com.eucalyptus.event.EventListener as EucaEventListener
import com.eucalyptus.network.config.Cluster as ConfigCluster
import com.eucalyptus.network.config.NetworkConfiguration
import com.eucalyptus.network.config.NetworkConfigurations
import com.eucalyptus.network.config.Subnet
import com.eucalyptus.system.Threads
import com.eucalyptus.util.TypeMapper
import com.eucalyptus.util.TypeMappers
import com.eucalyptus.util.async.AsyncRequests
import com.eucalyptus.vm.VmInstance
import com.eucalyptus.vm.VmInstances
import com.google.common.base.Function
import com.google.common.base.Optional
import com.google.common.base.Strings
import com.google.common.collect.HashMultimap
import com.google.common.collect.Lists
import com.google.common.collect.Multimap
import com.google.common.collect.Sets
import edu.ucsb.eucalyptus.cloud.NodeInfo
import groovy.transform.CompileStatic
import org.apache.log4j.Logger

import javax.xml.bind.JAXBContext
import java.util.concurrent.Callable
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

/**
 *
 */
@CompileStatic
class NetworkInfoBroadcaster {
  private static final Logger logger = Logger.getLogger( NetworkInfoBroadcaster )

  private static final AtomicLong lastBroadcastTime = new AtomicLong( 0L );

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

  static void broadcastNetworkInfo(){
    // populate with info directly from configuration
    Optional<NetworkConfiguration> networkConfiguration = NetworkConfigurations.networkConfiguration
    NetworkInfo info =  networkConfiguration
        .transform( TypeMappers.lookup( NetworkConfiguration, NetworkInfo ) )
        .or( new NetworkInfo( ) )

    // populate clusters
    List<Cluster> clusters = Clusters.getInstance( ).listValues( )
    Subnet defaultSubnet = 1==(networkConfiguration.orNull()?.subnets?.size()?:0) ? networkConfiguration.orNull()?.subnets[0] : null
    info.configuration.clusters = new NIClusters(
        name: 'clusters',
        clusters: clusters.collect{ Cluster cluster ->
          ConfigCluster configCluster = networkConfiguration.orNull()?.clusters?.find{ ConfigCluster configCluster -> cluster.partition == configCluster.name }
          Subnet clusterSubnetFallback = configCluster?.subnet?.name ?
              networkConfiguration.orNull()?.subnets?.find{ Subnet subnet -> (subnet.name?:subnet.subnet)==configCluster?.subnet?.name }?:defaultSubnet :
              defaultSubnet
          new NICluster(
              name: (String)cluster.partition,
              subnet: configCluster?.subnet || defaultSubnet ?  new NISubnet(
                  name: configCluster?.subnet?.name?:configCluster?.subnet?.subnet?:clusterSubnetFallback?.name?:clusterSubnetFallback?.subnet,
                  properties: [
                      new NIProperty( name: 'subnet', values: [ configCluster?.subnet?.subnet?:clusterSubnetFallback?.subnet ]),
                      new NIProperty( name: 'netmask', values: [ configCluster?.subnet?.netmask?:clusterSubnetFallback?.netmask ]),
                      new NIProperty( name: 'gateway', values: [ configCluster?.subnet?.gateway?:clusterSubnetFallback?.gateway ])
                  ]
              ) : null,
              properties: ( [
                  new NIProperty( name: 'enabledCCIp', values: [ InetAddress.getByName(cluster.hostName).hostAddress ]),
                  new NIProperty( name: 'macPrefix', values: [ configCluster?.macPrefix?:VmInstances.MAC_PREFIX ] ),
              ] + ( (configCluster?.privateIps?:networkConfiguration.orNull()?.privateIps) ? [
                  new NIProperty( name: 'privateIps',
                      values: Lists.newArrayList(configCluster?.privateIps?:networkConfiguration.orNull()?.privateIps) )
              ] : [ ] as List<NIProperty> ) ) as List<NIProperty> ,
              nodes: new NINodes(
                  name: 'nodes',
                  nodes: cluster.nodeHostMap.values().collect{ NodeInfo nodeInfo -> new NINode( name: nodeInfo.name ) }
              )
          )
        }
    )

    // populate dynamic properties
    info.configuration.properties.addAll( [
        new NIProperty( name: 'enabledCLCIp', values: [Topology.lookup(Eucalyptus).inetAddress.hostAddress]),
        new NIProperty( name: 'instanceDNSDomain', values: [networkConfiguration.orNull()?.instanceDnsDomain?:"${VmInstances.INSTANCE_SUBDOMAIN}.internal" as String])
    ] + ( networkConfiguration.orNull()?.instanceDnsServers ? [
        new NIProperty( name: 'instanceDNSServers', values: [networkConfiguration.orNull()?.instanceDnsServers?:'']), //TODO:STEVE: Include cloud property for DNS servers?
    ] : [ ] as List<NIProperty>) )

    int instanceCount = Entities.transaction( VmInstance ){
      List<VmInstance> instances = VmInstances.list( VmInstance.VmStateSet.TORNDOWN.not( ) )

      // populate nodes
      ((Multimap<List<String>,String>) instances.inject( HashMultimap.create( ) ){
        Multimap<List<String>,String> map, VmInstance instance ->
          map.put( [ instance.partition, Strings.nullToEmpty( VmInstances.toNodeHost( ).apply( instance ) ) ], instance.getInstanceId( ) )
          map
      }).asMap().each{ Map.Entry<List<String>,Collection<String>> entry ->
        info.configuration.clusters.clusters.find{ NICluster cluster -> cluster.name == entry.key[0] }?.with{
          NINode node = nodes.nodes.find{ NINode node -> node.name == entry.key[1] }
          if ( node ) {
            node.instanceIds = entry.value ? entry.value as List<String> : null
          }
        }
      }

      // populate instances
      info.instances.addAll( instances.collect{ VmInstance instance ->
        new NIInstance(
            name: instance.instanceId,
            ownerId: instance.ownerAccountNumber,
            macAddress: instance.macAddress,
            publicIp: instance.publicAddress,
            privateIp: instance.privateAddress,
            securityGroups: instance.networkGroups.collect{ NetworkGroup group -> group.groupId }
        )
      } )

      // populate security groups
      info.securityGroups.addAll( instances.collect{ VmInstance instance -> instance.networkGroups }.flatten( ).unique( ).collect{ NetworkGroup group ->
        new NISecurityGroup(
            name: group.groupId,
            ownerId: group.ownerAccountNumber,
            rules: group.networkRules.collect{ NetworkRule networkRule -> explodeRules( networkRule ) }.flatten( ) as List<String>
        )
      } )

      instances.size( )
    }

    JAXBContext jc = JAXBContext.newInstance( "com.eucalyptus.cluster" )
    StringWriter writer = new StringWriter( )
    jc.createMarshaller().marshal( info, writer )

    String networkInfo = writer.toString( )
    if ( logger.isTraceEnabled( ) ) {
      logger.trace( "Broadcasting network information:\n${networkInfo}" )
    }

    BroadcastNetworkInfoCallback callback = new BroadcastNetworkInfoCallback( networkInfo )
    clusters.each { Cluster cluster ->
      AsyncRequests.newRequest( callback.newInstance( ) ).dispatch( cluster.configuration )
    }

    logger.debug( "Broadcast network information for ${instanceCount} instance(s)" )
  }

  //TODO:STEVE: Get rid of this rule processing, pass in structured format
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
                          name: subnet.name?:subnet.subnet,
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

  public static class NetworkInfoBroadcasterEventListener implements EucaEventListener<ClockTick> {
    private final int intervalTicks = 3
    private volatile int counter = 0

    public static void register( ) {
      Listeners.register( ClockTick.class, new NetworkInfoBroadcasterEventListener( ) )
    }

    @Override
    public void fireEvent( final ClockTick event ) {
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
