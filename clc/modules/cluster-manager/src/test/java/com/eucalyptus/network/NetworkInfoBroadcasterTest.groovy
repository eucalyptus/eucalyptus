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

import com.eucalyptus.cluster.Cluster
import com.eucalyptus.cluster.ClusterConfiguration
import com.eucalyptus.cluster.NICluster
import com.eucalyptus.cluster.NIClusters
import com.eucalyptus.cluster.NIConfiguration
import com.eucalyptus.cluster.NIInstance
import com.eucalyptus.cluster.NINode
import com.eucalyptus.cluster.NINodes
import com.eucalyptus.cluster.NIProperty
import com.eucalyptus.cluster.NISubnet
import com.eucalyptus.cluster.NISubnets
import com.eucalyptus.cluster.NetworkInfo
import com.eucalyptus.network.config.Cluster as ConfigCluster
import com.eucalyptus.network.config.NetworkConfiguration
import com.eucalyptus.network.config.Subnet
import com.eucalyptus.util.TypeMappers
import com.eucalyptus.vm.VmInstance.VmState
import com.google.common.base.Function
import com.google.common.base.Optional
import com.google.common.base.Supplier
import edu.ucsb.eucalyptus.cloud.NodeInfo
import org.junit.BeforeClass
import org.junit.Test
import static org.junit.Assert.*

/**
 *
 */
class NetworkInfoBroadcasterTest {

  @BeforeClass
  static void setup( ) {
    TypeMappers.TypeMapperDiscovery discovery = new TypeMappers.TypeMapperDiscovery()
    discovery.processClass( NetworkInfoBroadcaster.NetworkConfigurationToNetworkInfo )
    discovery.processClass( NetworkInfoBroadcaster.NetworkGroupToNetworkGroupNetworkView )
    discovery.processClass( NetworkInfoBroadcaster.VmInstanceToVmInstanceNetworkView )
  }

  @Test
  void testBasicBroadcast( ) {
    NetworkInfo info = NetworkInfoBroadcaster.buildNetworkConfiguration(
      Optional.of( new NetworkConfiguration(
          instanceDnsDomain: 'eucalyptus.internal',
          instanceDnsServers: [ '1.2.3.4' ],
          publicIps: [ '2.0.0.0-2.0.0.255' ],
          privateIps: [ '10.0.0.0-10.0.0.255' ],
          subnets: [
              new Subnet(
                  name: 'default',
                  subnet: '10.0.0.0',
                  netmask: '255.255.0.0',
                  gateway: '10.0.1.0'
              ),
              new Subnet(
                  name: 'global',
                  subnet: '192.168.0.0',
                  netmask: '255.255.0.0',
                  gateway: '192.168.0.1'
              )
          ],
          clusters: [
              new ConfigCluster(
                  name: 'cluster1',
                  subnet: new Subnet(
                      name: 'default',
                  )
              )
          ]
      ) ),
      { [ cluster('cluster1', '6.6.6.6', [ 'node1' ]) ] } as Supplier<List<Cluster>>,
      { [ instance( 'i-00000001', 'cluster1', 'node1', '000000000002', '00:00:00:00:00:00', '2.0.0.0', '10.0.0.0' ) ] } as Supplier<List<NetworkInfoBroadcaster.VmInstanceNetworkView>>,
      { [ group( 'sg-00000001', '000000000002', [] ) ] } as Supplier<List<NetworkInfoBroadcaster.NetworkGroupNetworkView>>,
      { '1.1.1.1' } as Supplier<String>,
      { [ '127.0.0.1' ] } as Function<List<String>, List<String>>
    )
    assertEquals( 'basic broadcast', new NetworkInfo(
        configuration: new NIConfiguration(
            properties: [
                new NIProperty( name: 'publicIps', values: ['2.0.0.0-2.0.0.255'] ),
                new NIProperty( name: 'enabledCLCIp', values: ['1.1.1.1'] ),
                new NIProperty( name: 'instanceDNSDomain', values: ['eucalyptus.internal'] ),
                new NIProperty( name: 'instanceDNSServers', values: ['1.2.3.4'] ),
            ],
            subnets: new NISubnets( name: 'subnets', subnets: [
                new NISubnet(
                    name: '192.168.0.0',
                    properties: [
                        new NIProperty( name: 'subnet', values: ['192.168.0.0'] ),
                        new NIProperty( name: 'netmask', values: ['255.255.0.0'] ),
                        new NIProperty( name: 'gateway', values: ['192.168.0.1'] )
                    ]
                )
            ] ),
            clusters: new NIClusters( name: 'clusters', clusters: [
                new NICluster(
                    name: 'cluster1',
                    subnet: new NISubnet(
                        name: '10.0.0.0',
                        properties: [
                            new NIProperty( name: 'subnet', values: ['10.0.0.0'] ),
                            new NIProperty( name: 'netmask', values: ['255.255.0.0'] ),
                            new NIProperty( name: 'gateway', values: ['10.0.1.0'] )
                        ]
                    ),
                    properties: [
                        new NIProperty( name: 'enabledCCIp', values: ['6.6.6.6'] ),
                        new NIProperty( name: 'macPrefix', values: ['d0:0d'] ),
                        new NIProperty( name: 'privateIps', values: ['10.0.0.0-10.0.0.255'] ),
                    ],
                    nodes: new NINodes( name: 'nodes', nodes: [
                      new NINode(
                          name: 'node1',
                          instanceIds: [ 'i-00000001' ]
                      )
                    ] )
                )
            ] ),
        ),
        instances: [
          new NIInstance(
              name: 'i-00000001',
              ownerId: '000000000002',
              macAddress: '00:00:00:00:00:00',
              publicIp: '2.0.0.0',
              privateIp: '10.0.0.0',
              securityGroups: [],
          )
        ],
        securityGroups: [ ]
    ), info )
  }

  @Test
  void testBroadcastDefaults( ) {
    NetworkInfo info = NetworkInfoBroadcaster.buildNetworkConfiguration(
        Optional.of( new NetworkConfiguration(
            publicIps: [ '2.0.0.0-2.0.0.255' ],
            privateIps: [ '10.0.0.0-10.0.0.255' ],
            subnets: [
                new Subnet(
                    subnet: '10.0.0.0',
                    netmask: '255.255.0.0',
                    gateway: '10.0.1.0'
                )
            ],
            clusters: [
                new ConfigCluster(
                    name: 'cluster1'
                )
            ]
        ) ),
        { [ cluster('cluster1', '6.6.6.6', [ 'node1' ]) ] } as Supplier<List<Cluster>>,
        { [ instance( 'i-00000001', 'cluster1', 'node1', '000000000002', '00:00:00:00:00:00', '2.0.0.0', '10.0.0.0' ) ] } as Supplier<List<NetworkInfoBroadcaster.VmInstanceNetworkView>>,
        { [ ] } as Supplier<List<NetworkInfoBroadcaster.NetworkGroupNetworkView>>,
        { '1.1.1.1' } as Supplier<String>,
        { [ '127.0.0.1' ] } as Function<List<String>, List<String>>
    )
    assertEquals( 'broadcast defaults', new NetworkInfo(
        configuration: new NIConfiguration(
            properties: [
                new NIProperty( name: 'publicIps', values: ['2.0.0.0-2.0.0.255'] ),
                new NIProperty( name: 'enabledCLCIp', values: ['1.1.1.1'] ),
                new NIProperty( name: 'instanceDNSDomain', values: ['eucalyptus.internal'] ),
                new NIProperty( name: 'instanceDNSServers', values: ['127.0.0.1'] ),
            ],
            clusters: new NIClusters( name: 'clusters', clusters: [
                new NICluster(
                    name: 'cluster1',
                    subnet: new NISubnet(
                        name: '10.0.0.0',
                        properties: [
                            new NIProperty( name: 'subnet', values: ['10.0.0.0'] ),
                            new NIProperty( name: 'netmask', values: ['255.255.0.0'] ),
                            new NIProperty( name: 'gateway', values: ['10.0.1.0'] )
                        ]
                    ),
                    properties: [
                        new NIProperty( name: 'enabledCCIp', values: ['6.6.6.6'] ),
                        new NIProperty( name: 'macPrefix', values: ['d0:0d'] ),
                        new NIProperty( name: 'privateIps', values: ['10.0.0.0-10.0.0.255'] ),
                    ],
                    nodes: new NINodes( name: 'nodes', nodes: [
                        new NINode(
                            name: 'node1',
                            instanceIds: [ 'i-00000001' ]
                        )
                    ] )
                )
            ] ),
        ),
        instances: [
            new NIInstance(
                name: 'i-00000001',
                ownerId: '000000000002',
                macAddress: '00:00:00:00:00:00',
                publicIp: '2.0.0.0',
                privateIp: '10.0.0.0',
                securityGroups: [],
            )
        ],
        securityGroups: [ ]
    ), info )
  }

  private static Cluster cluster( String partition, String host, List<String> nodes = [ ] ) {
    Cluster cluster = new Cluster( new ClusterConfiguration( partition: partition, hostName: host ), (Void) null ){ }
    nodes.each{ String node -> cluster.nodeMap.put( node, new NodeInfo( name: node ) ) }
    cluster
  }

  private static NetworkInfoBroadcaster.VmInstanceNetworkView instance( String id, String partition, String node, String ownerAccountNumber, String mac, String publicAddress, String privateAddress ) {
    new NetworkInfoBroadcaster.VmInstanceNetworkView(
      id,
      VmState.RUNNING,
      ownerAccountNumber,
      mac,
      privateAddress,
      publicAddress,
      partition,
      node,
      [ ],
    )
  }

  private static NetworkInfoBroadcaster.NetworkGroupNetworkView group( String id, String ownerAccountNumber, List<String> rules ) {
    new NetworkInfoBroadcaster.NetworkGroupNetworkView(
      id,
      ownerAccountNumber,
      rules,
    )
  }
}
