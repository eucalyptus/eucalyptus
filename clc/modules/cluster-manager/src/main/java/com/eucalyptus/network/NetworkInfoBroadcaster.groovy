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
import com.eucalyptus.util.TypeMapper
import com.eucalyptus.util.TypeMappers
import com.eucalyptus.util.async.AsyncRequests
import com.eucalyptus.vm.VmInstance
import com.eucalyptus.vm.VmInstances
import com.google.common.base.Function
import com.google.common.base.Optional
import com.google.common.collect.HashMultimap
import com.google.common.collect.Multimap
import com.google.common.collect.Sets
import edu.ucsb.eucalyptus.cloud.entities.SystemConfiguration
import groovy.transform.CompileStatic
import org.apache.log4j.Logger

import javax.xml.bind.JAXBContext

/**
 *
 */
@CompileStatic
class NetworkInfoBroadcaster {
  private static final Logger logger = Logger.getLogger( NetworkInfoBroadcaster )

  static void broadcastNetworkInfo(){
    // populate with info directly from configuration
    Optional<NetworkConfiguration> networkConfiguration = NetworkConfigurations.networkConfiguration
    NetworkInfo info =  networkConfiguration
        .transform( TypeMappers.lookup( NetworkConfiguration, NetworkInfo ) )
        .or( new NetworkInfo( ) )

    // populate clusters
    List<Cluster> clusters = Clusters.getInstance( ).listValues( ) //TODO:STEVE: Seems to be a bootstrap issue here, needs to get configuration to become enabled?
    info.configuration.clusters = new NIClusters(
        name: 'clusters',
        clusters: clusters.collect{ Cluster cluster ->
          ConfigCluster configCluster = networkConfiguration.orNull()?.clusters?.find{ ConfigCluster configCluster -> cluster.name == configCluster.name }
          new NICluster(
              name: (String)cluster.name,
              partition: cluster.partition,
              subnet: new NISubnet(
                  name: configCluster.subnet.name, //TODO:STEVE: Allow for configuration defaults
                  properties: [
                      new NIProperty( name: 'subnet', values: [ configCluster.subnet.subnet ]),
                      new NIProperty( name: 'netmask', values: [ configCluster.subnet.netmask ]),
                      new NIProperty( name: 'gateway', values: [ configCluster.subnet.gateway ])
                  ]
              ),
              properties: [
                  new NIProperty( name: 'enabledCCIp', values: [Topology.lookup(Eucalyptus).inetAddress.hostAddress]),
                  new NIProperty( name: 'macPrefix', values: [ configCluster?.macPrefix?:VmInstances.MAC_PREFIX ]),
                  new NIProperty( name: 'privateIps', values: configCluster?.privateIps )
              ],
              nodes: new NINodes(
                  name: 'nodes',
              )
          )
        }
    )

    // populate dynamic properties
    info.configuration.properties.addAll( [
        new NIProperty( name: 'enabledCLCIp', values: [Topology.lookup(Eucalyptus).inetAddress.hostAddress]), //TODO:STEVE:Why does this not come from BaseMessage?
        new NIProperty( name: 'instanceDNSDomain', values: [SystemConfiguration.systemConfiguration.dnsDomain ]),
        new NIProperty( name: 'instanceDNSServers', values: [SystemConfiguration.systemConfiguration.nameserverAddress]),
    ] )


    Entities.transaction( VmInstance ){
      List<VmInstance> instances = VmInstances.list( VmInstance.VmStateSet.TORNDOWN.not( ) )

      // populate nodes
      ((Multimap<List<String>,String>) instances.inject( HashMultimap.create( ) ){
        Multimap<List<String>,String> map, VmInstance instance ->
          map.put( [ instance.partition, VmInstances.toNodeHost( ).apply( instance ) ], instance.getInstanceId( ) )
          map
      }).asMap().each{ Map.Entry<List<String>,Collection<String>> entry ->
        info.configuration.clusters.clusters.find{ NICluster cluster -> cluster.partition == entry.key[0] }?.with{
          nodes.nodes << new NINode(
              name: entry.key[0],
              instanceIds: entry.value as List<String>
          )
        }
      }

      // populate instances
      info.instances.addAll( instances.collect{ VmInstance instance ->
        new NIInstance(
            name: instance.instanceId,
            ownerId: instance.ownerAccountNumber,
//          macAddress: instance., //TODO:STEVE: Why is this necessary if mac is allocated by the cluster?
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

    }

    JAXBContext jc = JAXBContext.newInstance( "com.eucalyptus.cluster" )
    StringWriter writer = new StringWriter( )
    jc.createMarshaller().marshal( info, writer )

    String networkInfo = writer.toString( )
    BroadcastNetworkInfoCallback callback = new BroadcastNetworkInfoCallback( networkInfo )
    clusters.each { Cluster cluster ->
      AsyncRequests.newRequest( callback.newInstance( ) ).dispatch( cluster.configuration )
    }
  }

  //TODO:STEVE: Get rid of this rule processing, pass in structured format
  private Set<String> explodeRules( NetworkRule networkRule ) {
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
              properties: [
                  new NIProperty( name: 'publicIps', values: networkConfiguration.publicIps )
              ],
              subnets: new NISubnets(
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
              )
          )
      )
    }
  }

  public static class NetworkInfoBroadcasterEventListener implements EucaEventListener<ClockTick> {
    public static void register( ) {
      Listeners.register( ClockTick.class, new NetworkInfoBroadcasterEventListener( ) )
    }

    @SuppressWarnings("UnnecessaryQualifiedReference")
    @Override
    public void fireEvent( final ClockTick event ) {
      if ( Hosts.isCoordinator( ) && !Bootstrap.isShuttingDown( ) && !Databases.isVolatile( ) ) {
        try {
          broadcastNetworkInfo( )
        } catch ( e ) {
          logger.error( "Error updating network configuration", e )
        }
      }
    }
  }
}
