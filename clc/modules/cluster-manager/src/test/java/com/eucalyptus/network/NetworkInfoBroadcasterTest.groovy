/*************************************************************************
 * Copyright 2009-2015 Eucalyptus Systems, Inc.
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
import com.eucalyptus.cluster.NIInternetGateway
import com.eucalyptus.cluster.NIMidonetGateway
import com.eucalyptus.cluster.NIMidonetGateways
import com.eucalyptus.cluster.NIInstance
import com.eucalyptus.cluster.NIManagedSubnet
import com.eucalyptus.cluster.NIManagedSubnets
import com.eucalyptus.cluster.NIMidonet
import com.eucalyptus.cluster.NINatGateway
import com.eucalyptus.cluster.NINetworkInterface
import com.eucalyptus.cluster.NINode
import com.eucalyptus.cluster.NINodes
import com.eucalyptus.cluster.NIProperty
import com.eucalyptus.cluster.NIRoute
import com.eucalyptus.cluster.NIRouteTable
import com.eucalyptus.cluster.NISubnet
import com.eucalyptus.cluster.NISubnets
import com.eucalyptus.cluster.NIVpc
import com.eucalyptus.cluster.NIVpcSubnet
import com.eucalyptus.cluster.NetworkInfo
import com.eucalyptus.compute.common.internal.vpc.NatGateway
import com.eucalyptus.compute.common.internal.vpc.NetworkInterfaceAttachment
import com.eucalyptus.compute.vpc.RouteKey
import com.eucalyptus.network.config.Cluster as ConfigCluster
import com.eucalyptus.network.config.EdgeSubnet
import com.eucalyptus.network.config.ManagedSubnet
import com.eucalyptus.network.config.Midonet
import com.eucalyptus.network.config.MidonetGateway
import com.eucalyptus.network.config.NetworkConfiguration
import com.eucalyptus.util.TypeMappers
import com.eucalyptus.compute.common.internal.vm.VmInstance.VmState
import com.eucalyptus.compute.common.internal.vpc.NetworkInterface
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
    discovery.processClass( NetworkInfoBroadcasts.NetworkConfigurationToNetworkInfo )
    discovery.processClass( NetworkInfoBroadcasts.NetworkGroupToNetworkGroupNetworkView )
    discovery.processClass( NetworkInfoBroadcasts.VmInstanceToVmInstanceNetworkView )
    discovery.processClass( NetworkInfoBroadcasts.VpcToVpcNetworkView )
    discovery.processClass( NetworkInfoBroadcasts.SubnetToSubnetNetworkView )
    discovery.processClass( NetworkInfoBroadcasts.DhcpOptionSetToDhcpOptionSetNetworkView )
    discovery.processClass( NetworkInfoBroadcasts.RouteTableToRouteTableNetworkView )
    discovery.processClass( NetworkInfoBroadcasts.NetworkAclToNetworkAclNetworkView )
    discovery.processClass( NetworkInfoBroadcasts.VpcNetworkInterfaceToNetworkInterfaceNetworkView )
    discovery.processClass( NetworkInfoBroadcasts.RouteNetworkViewToNIRoute )
    discovery.processClass( NetworkInfoBroadcasts.NetworkAclEntryNetworkViewToNINetworkAclRule )
    discovery.processClass( NetworkInfoBroadcasts.InternetGatewayToInternetGatewayNetworkView )
  }

  @Test
  void testBasicBroadcast( ) {
    NetworkInfo info = NetworkInfoBroadcasts.buildNetworkConfiguration(
      Optional.of( new NetworkConfiguration(
          instanceDnsDomain: 'eucalyptus.internal',
          instanceDnsServers: [ '1.2.3.4' ],
          publicIps: [ '2.0.0.2-2.0.0.255' ],
          publicGateway: '2.0.0.1',
          privateIps: [ '10.0.0.0-10.0.0.255' ],
          subnets: [
              new EdgeSubnet(
                  name: 'default',
                  subnet: '10.0.0.0',
                  netmask: '255.255.0.0',
                  gateway: '10.0.1.0'
              ),
              new EdgeSubnet(
                  name: 'global',
                  subnet: '192.168.0.0',
                  netmask: '255.255.0.0',
                  gateway: '192.168.0.1'
              )
          ],
          clusters: [
              new ConfigCluster(
                  name: 'cluster1',
                  subnet: new EdgeSubnet(
                      name: 'default',
                  )
              )
          ]
      ) ),
      new NetworkInfoBroadcasts.NetworkInfoSource( ) {
        @Override Iterable<NetworkInfoBroadcasts.VmInstanceNetworkView> getInstances() {
          [ instance( 'i-00000001', 'cluster1', 'node1', '000000000002', '00:00:00:00:00:00', '2.0.0.2', '10.0.0.0' ) ]
        }
        @Override Iterable<NetworkInfoBroadcasts.NetworkGroupNetworkView> getSecurityGroups() {
          [ group( 'sg-00000001', '000000000002', [], [], [] ) ]
        }
        @Override Iterable<NetworkInfoBroadcasts.VpcNetworkView> getVpcs() {
          []
        }
        @Override Iterable<NetworkInfoBroadcasts.SubnetNetworkView> getSubnets() {
         []
        }
        @Override Iterable<NetworkInfoBroadcasts.DhcpOptionSetNetworkView> getDhcpOptionSets() {
         []
        }
        @Override Iterable<NetworkInfoBroadcasts.NetworkAclNetworkView> getNetworkAcls() {
          []
        }
        @Override Iterable<NetworkInfoBroadcasts.RouteTableNetworkView> getRouteTables() {
          []
        }
        @Override Iterable<NetworkInfoBroadcasts.InternetGatewayNetworkView> getInternetGateways() {
          []
        }
        @Override Iterable<NetworkInfoBroadcasts.NetworkInterfaceNetworkView> getNetworkInterfaces() {
          []
        }
        @Override Iterable<NetworkInfoBroadcasts.NatGatewayNetworkView> getNatGateways() {
          []
        }
        @Override Map<String,Iterable<? extends NetworkInfoBroadcasts.VmInstanceNetworkView>> getView() {
          [:]
        }
      },
      { [ cluster('cluster1', '6.6.6.6', [ 'node1' ]) ] } as Supplier<List<Cluster>>,
      { '1.1.1.1' } as Supplier<String>,
      { [ '127.0.0.1' ] } as Function<List<String>, List<String>>,
      [] as Set<String>,
      [] as Set<RouteKey>
    )
    assertEquals( 'basic broadcast', new NetworkInfo(
        configuration: new NIConfiguration(
            properties: [
                new NIProperty( name: 'mode', values: ['EDGE'] ),
                new NIProperty( name: 'publicIps', values: ['2.0.0.2-2.0.0.255'] ),
                new NIProperty( name: 'enabledCLCIp', values: ['1.1.1.1'] ),
                new NIProperty( name: 'instanceDNSDomain', values: ['eucalyptus.internal'] ),
                new NIProperty( name: 'instanceDNSServers', values: ['1.2.3.4'] ),
                new NIProperty( name: 'publicGateway', values: ['2.0.0.1'] ),
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
              publicIp: '2.0.0.2',
              privateIp: '10.0.0.0',
              securityGroups: [],
          )
        ],
        securityGroups: [ ]
    ), info )
  }

  @Test
  void testBroadcastDefaults( ) {
    NetworkInfo info = NetworkInfoBroadcasts.buildNetworkConfiguration(
        Optional.of( new NetworkConfiguration(
            publicIps: [ '2.0.0.0-2.0.0.255' ],
            privateIps: [ '10.0.0.0-10.0.0.255' ],
            subnets: [
                new EdgeSubnet(
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
        new NetworkInfoBroadcasts.NetworkInfoSource( ) {
          @Override Iterable<NetworkInfoBroadcasts.VmInstanceNetworkView> getInstances() {
            [ instance( 'i-00000001', 'cluster1', 'node1', '000000000002', '00:00:00:00:00:00', '2.0.0.0', '10.0.0.0' ) ]
          }
          @Override Iterable<NetworkInfoBroadcasts.NetworkGroupNetworkView> getSecurityGroups() {
            []
          }
          @Override Iterable<NetworkInfoBroadcasts.VpcNetworkView> getVpcs() {
            []
          }
          @Override Iterable<NetworkInfoBroadcasts.SubnetNetworkView> getSubnets() {
            []
          }
          @Override Iterable<NetworkInfoBroadcasts.DhcpOptionSetNetworkView> getDhcpOptionSets() {
            []
          }
          @Override Iterable<NetworkInfoBroadcasts.NetworkAclNetworkView> getNetworkAcls() {
            []
          }
          @Override Iterable<NetworkInfoBroadcasts.RouteTableNetworkView> getRouteTables() {
            []
          }
          @Override Iterable<NetworkInfoBroadcasts.InternetGatewayNetworkView> getInternetGateways() {
            []
          }
          @Override Iterable<NetworkInfoBroadcasts.NetworkInterfaceNetworkView> getNetworkInterfaces() {
            []
          }
          @Override Iterable<NetworkInfoBroadcasts.NatGatewayNetworkView> getNatGateways() {
            []
          }
          @Override Map<String,Iterable<? extends NetworkInfoBroadcasts.VmInstanceNetworkView>> getView() {
            [:]
          }
        },
        { [ cluster('cluster1', '6.6.6.6', [ 'node1' ]) ] } as Supplier<List<Cluster>>,
        { '1.1.1.1' } as Supplier<String>,
        { [ '127.0.0.1' ] } as Function<List<String>, List<String>>,
        [] as Set<String>,
        [] as Set<RouteKey>
    )
    assertEquals( 'broadcast defaults', new NetworkInfo(
        configuration: new NIConfiguration(
            properties: [
                new NIProperty( name: 'mode', values: ['EDGE'] ),
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

  @Test
  void testBroadcastVpcMidoSingleGateway( ) {
    Midonet midonet = new Midonet(
        eucanetdHost: 'a-35.qa1.eucalyptus-systems.com',
        gatewayHost: 'a-35.qa1.eucalyptus-systems.com',
        gatewayIP: '10.116.133.77',
        gatewayInterface: 'em1.116',
        publicNetworkCidr: '10.116.0.0/17',
        publicGatewayIP: '10.116.133.67'
    )
    NIMidonet niMidonet = new NIMidonet(
        name: 'mido',
        gateways: new NIMidonetGateways(
          name: 'gateways',
          gateways: [
              new NIMidonetGateway(
                  properties: [
                    new NIProperty( name: 'gatewayHost', values: ['a-35.qa1.eucalyptus-systems.com'] ),
                    new NIProperty( name: 'gatewayIP', values: ['10.116.133.77'] ),
                    new NIProperty( name: 'gatewayInterface', values: ['em1.116'] ),
                  ]
              )
          ]
        ),
        properties: [
            new NIProperty( name: 'eucanetdHost', values: ['a-35.qa1.eucalyptus-systems.com'] ),
            new NIProperty( name: 'publicNetworkCidr', values: ['10.116.0.0/17'] ),
            new NIProperty( name: 'publicGatewayIP', values: ['10.116.133.67'] ),
        ]
    )
    vpcBroadcastTest( midonet, niMidonet )
  }

  @Test
  void testBroadcastVpcMido( ) {
    Midonet midonet = new Midonet(
        eucanetdHost: 'a-35.qa1.eucalyptus-systems.com',
        gateways: [
            new MidonetGateway(
                gatewayHost: 'a-35.qa1.eucalyptus-systems.com',
                gatewayIP: '10.116.133.77',
                gatewayInterface: 'em1.116',
            ),
            new MidonetGateway(
                gatewayHost: 'a-36.qa1.eucalyptus-systems.com',
                gatewayIP: '10.116.133.78',
                gatewayInterface: 'em1.117',
            ),
        ],
        publicNetworkCidr: '10.116.0.0/17',
        publicGatewayIP: '10.116.133.67'
    )
    NIMidonet niMidonet = new NIMidonet(
        name: 'mido',
        gateways: new NIMidonetGateways(
            name: 'gateways',
            gateways: [
                new NIMidonetGateway(
                    properties: [
                        new NIProperty( name: 'gatewayHost', values: ['a-35.qa1.eucalyptus-systems.com'] ),
                        new NIProperty( name: 'gatewayIP', values: ['10.116.133.77'] ),
                        new NIProperty( name: 'gatewayInterface', values: ['em1.116'] ),
                    ]
                ),
                new NIMidonetGateway(
                    properties: [
                        new NIProperty( name: 'gatewayHost', values: ['a-36.qa1.eucalyptus-systems.com'] ),
                        new NIProperty( name: 'gatewayIP', values: ['10.116.133.78'] ),
                        new NIProperty( name: 'gatewayInterface', values: ['em1.117'] ),
                    ]
                ),
            ]
        ),
        properties: [
            new NIProperty( name: 'eucanetdHost', values: ['a-35.qa1.eucalyptus-systems.com'] ),
            new NIProperty( name: 'publicNetworkCidr', values: ['10.116.0.0/17'] ),
            new NIProperty( name: 'publicGatewayIP', values: ['10.116.133.67'] ),
        ]
    )
    vpcBroadcastTest( midonet, niMidonet )
  }

  /**
   * Test to ensure that a vpc is included in the network broadcast
   * even when there are no instances in that vpc.
   */
  @Test
  void testBroadcastVpcMidoNatGatewayOnly( ) {
    Midonet midonet = new Midonet(
        eucanetdHost: 'a-35.qa1.eucalyptus-systems.com',
        gateways: [
            new MidonetGateway(
                gatewayHost: 'a-35.qa1.eucalyptus-systems.com',
                gatewayIP: '10.116.133.77',
                gatewayInterface: 'em1.116',
            ),
        ],
        publicNetworkCidr: '10.116.0.0/17',
        publicGatewayIP: '10.116.133.67'
    )
    NIMidonet niMidonet = new NIMidonet(
        name: 'mido',
        gateways: new NIMidonetGateways(
            name: 'gateways',
            gateways: [
                new NIMidonetGateway(
                    properties: [
                        new NIProperty( name: 'gatewayHost', values: ['a-35.qa1.eucalyptus-systems.com'] ),
                        new NIProperty( name: 'gatewayIP', values: ['10.116.133.77'] ),
                        new NIProperty( name: 'gatewayInterface', values: ['em1.116'] ),
                    ]
                ),
            ]
        ),
        properties: [
            new NIProperty( name: 'eucanetdHost', values: ['a-35.qa1.eucalyptus-systems.com'] ),
            new NIProperty( name: 'publicNetworkCidr', values: ['10.116.0.0/17'] ),
            new NIProperty( name: 'publicGatewayIP', values: ['10.116.133.67'] ),
        ]
    )
    vpcBroadcastTestNatGateway( midonet, niMidonet )
  }

  private void vpcBroadcastTest( final Midonet midoConfig, final NIMidonet midoNetworkInformation ) {
    NetworkInfo info = NetworkInfoBroadcasts.buildNetworkConfiguration(
        Optional.of( new NetworkConfiguration(
            mode: 'VPCMIDO',
            mido: midoConfig,
            publicIps: [ '2.0.0.0-2.0.0.255' ],
        ) ),
        new NetworkInfoBroadcasts.NetworkInfoSource( ) {
          @Override Iterable<NetworkInfoBroadcasts.VmInstanceNetworkView> getInstances() {
            [ instance( 'i-00000001', 'cluster1', 'node1', '000000000002', '00:00:00:00:00:00', '2.0.0.0', '10.0.0.0', 'vpc-00000001', 'subnet-00000001' ) ]
          }
          @Override Iterable<NetworkInfoBroadcasts.NetworkGroupNetworkView> getSecurityGroups() {
            []
          }
          @Override Iterable<NetworkInfoBroadcasts.VpcNetworkView> getVpcs() {
            [ vpc( 'vpc-00000001', '000000000002' ) ]
          }
          @Override Iterable<NetworkInfoBroadcasts.SubnetNetworkView> getSubnets() {
            [ subnet( 'subnet-00000001', '000000000002', 'cluster1', 'vpc-00000001' ) ]
          }
          @Override Iterable<NetworkInfoBroadcasts.DhcpOptionSetNetworkView> getDhcpOptionSets() {
            []
          }
          @Override Iterable<NetworkInfoBroadcasts.NetworkAclNetworkView> getNetworkAcls() {
            []
          }
          @Override Iterable<NetworkInfoBroadcasts.RouteTableNetworkView> getRouteTables() {
            [ routeTable( 'rtb-00000001', '000000000002', 'vpc-00000001', true, [ 'subnet-00000001' ], [
              route( 'rtb-00000001', '192.168.0.0/16', 'igw-00000001' )
            ] ) ]
          }
          @Override Iterable<NetworkInfoBroadcasts.InternetGatewayNetworkView> getInternetGateways() {
            [ internetGateway( 'igw-00000001', '000000000002', 'vpc-00000001' ) ]
          }
          @Override Iterable<NetworkInfoBroadcasts.NetworkInterfaceNetworkView> getNetworkInterfaces() {
            [ networkInterface( 'eni-00000001', '000000000002', 'i-00000001', 'eni-attach-00000001', '00:00:00:00:00:00', '2.0.0.0', '10.0.0.0', 'vpc-00000001', 'subnet-00000001' ) ]
          }
          @Override Iterable<NetworkInfoBroadcasts.NatGatewayNetworkView> getNatGateways() {
            []
          }
          @Override Map<String,Iterable<? extends NetworkInfoBroadcasts.VmInstanceNetworkView>> getView() {
            [:]
          }
        },
        { [ cluster('cluster1', '6.6.6.6', [ 'node1' ]) ] } as Supplier<List<Cluster>>,
        { '1.1.1.1' } as Supplier<String>,
        { [ '127.0.0.1' ] } as Function<List<String>, List<String>>,
        [] as Set<String>,
        [] as Set<RouteKey>,
    )

    assertEquals( 'broadcast vpc midonet', new NetworkInfo(
        configuration: new NIConfiguration(
            properties: [
                new NIProperty( name: 'mode', values: ['VPCMIDO'] ),
                new NIProperty( name: 'publicIps', values: ['2.0.0.0-2.0.0.255'] ),
                new NIProperty( name: 'enabledCLCIp', values: ['1.1.1.1'] ),
                new NIProperty( name: 'instanceDNSDomain', values: ['eucalyptus.internal'] ),
                new NIProperty( name: 'instanceDNSServers', values: ['127.0.0.1'] ),
            ],
            midonet: midoNetworkInformation,
            clusters: new NIClusters( name: 'clusters', clusters: [
                new NICluster(
                    name: 'cluster1',
                    subnet: new NISubnet(
                        name: '172.31.0.0',
                        properties: [
                            new NIProperty( name: 'subnet', values: ['172.31.0.0'] ),
                            new NIProperty( name: 'netmask', values: ['255.255.0.0'] ),
                            new NIProperty( name: 'gateway', values: ['172.31.0.1'] )
                        ]
                    ),
                    properties: [
                        new NIProperty( name: 'enabledCCIp', values: ['6.6.6.6'] ),
                        new NIProperty( name: 'macPrefix', values: ['d0:0d'] ),
                        new NIProperty( name: 'privateIps', values: ['172.31.0.5'] ),
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
        vpcs: [
            new NIVpc(
                name: 'vpc-00000001',
                ownerId: '000000000002',
                cidr: '10.0.0.0/16',
                subnets: [
                  new NIVpcSubnet(
                      name:  'subnet-00000001',
                      ownerId: '000000000002',
                      cidr: '10.0.0.0/16',
                      cluster: 'cluster1',
                      routeTable: 'rtb-00000001'
                  )
                ],
                routeTables: [
                    new NIRouteTable(
                        name: 'rtb-00000001',
                        ownerId: '000000000002',
                        routes: [
                          new NIRoute(
                            destinationCidr: '192.168.0.0/16',
                            gatewayId: 'igw-00000001',
                          )
                        ]
                    )
                ],
                internetGateways: [ 'igw-00000001' ]
            )
        ],
        internetGateways: [
            new NIInternetGateway(
                name: 'igw-00000001',
                ownerId: '000000000002'
            )
        ],
        instances: [
            new NIInstance(
                name: 'i-00000001',
                ownerId: '000000000002',
                macAddress: '00:00:00:00:00:00',
                publicIp: '2.0.0.0',
                privateIp: '10.0.0.0',
                securityGroups: [],
                vpc: 'vpc-00000001',
                subnet: 'subnet-00000001',
                networkInterfaces: [
                    new NINetworkInterface(
                        name: 'eni-00000001',
                        ownerId: '000000000002',
                        deviceIndex: 0,
                        attachmentId: 'eni-attach-00000001',
                        macAddress: '00:00:00:00:00:00',
                        publicIp: '2.0.0.0',
                        privateIp: '10.0.0.0',
                        sourceDestCheck: true,
                        vpc: 'vpc-00000001',
                        subnet: 'subnet-00000001',
                        securityGroups: []
                    )
                ]
            )
        ],
        securityGroups: [ ]
    ), info )
  }

  private void vpcBroadcastTestNatGateway( final Midonet midoConfig, final NIMidonet midoNetworkInformation ) {
    NetworkInfo info = NetworkInfoBroadcasts.buildNetworkConfiguration(
        Optional.of( new NetworkConfiguration(
            mode: 'VPCMIDO',
            mido: midoConfig,
            publicIps: [ '2.0.0.0-2.0.0.255' ],
        ) ),
        new NetworkInfoBroadcasts.NetworkInfoSource( ) {
          @Override Iterable<NetworkInfoBroadcasts.VmInstanceNetworkView> getInstances() {
            [ ]
          }
          @Override Iterable<NetworkInfoBroadcasts.NetworkGroupNetworkView> getSecurityGroups() {
            []
          }
          @Override Iterable<NetworkInfoBroadcasts.VpcNetworkView> getVpcs() {
            [ vpc( 'vpc-00000001', '000000000002' ) ]
          }
          @Override Iterable<NetworkInfoBroadcasts.SubnetNetworkView> getSubnets() {
            [ subnet( 'subnet-00000001', '000000000002', 'cluster1', 'vpc-00000001' ) ]
          }
          @Override Iterable<NetworkInfoBroadcasts.DhcpOptionSetNetworkView> getDhcpOptionSets() {
            []
          }
          @Override Iterable<NetworkInfoBroadcasts.NetworkAclNetworkView> getNetworkAcls() {
            []
          }
          @Override Iterable<NetworkInfoBroadcasts.RouteTableNetworkView> getRouteTables() {
            [ routeTable( 'rtb-00000001', '000000000002', 'vpc-00000001', true, [ 'subnet-00000001' ], [
                route( 'rtb-00000001', '0.0.0.0/0', null, 'nat-00000000000000001' )
            ] ) ]
          }
          @Override Iterable<NetworkInfoBroadcasts.InternetGatewayNetworkView> getInternetGateways() {
            [ internetGateway( 'igw-00000001', '000000000002', 'vpc-00000001' ) ]
          }
          @Override Iterable<NetworkInfoBroadcasts.NetworkInterfaceNetworkView> getNetworkInterfaces() {
            [ ]
          }
          @Override Iterable<NetworkInfoBroadcasts.NatGatewayNetworkView> getNatGateways() {
            [ natGateway( 'nat-00000000000000001', '000000000002', 'vpc-00000001', 'subnet-00000001', '00:00:00:00:00:00', '2.0.0.0', '10.0.0.0'   ) ]
          }
          @Override Map<String,Iterable<? extends NetworkInfoBroadcasts.VmInstanceNetworkView>> getView() {
            [:]
          }
        },
        { [ cluster('cluster1', '6.6.6.6', [ 'node1' ]) ] } as Supplier<List<Cluster>>,
        { '1.1.1.1' } as Supplier<String>,
        { [ '127.0.0.1' ] } as Function<List<String>, List<String>>,
        [] as Set<String>,
        [] as Set<RouteKey>
    )

    assertEquals( 'broadcast vpc midonet', new NetworkInfo(
        configuration: new NIConfiguration(
            properties: [
                new NIProperty( name: 'mode', values: ['VPCMIDO'] ),
                new NIProperty( name: 'publicIps', values: ['2.0.0.0-2.0.0.255'] ),
                new NIProperty( name: 'enabledCLCIp', values: ['1.1.1.1'] ),
                new NIProperty( name: 'instanceDNSDomain', values: ['eucalyptus.internal'] ),
                new NIProperty( name: 'instanceDNSServers', values: ['127.0.0.1'] ),
            ],
            midonet: midoNetworkInformation,
            clusters: new NIClusters( name: 'clusters', clusters: [
                new NICluster(
                    name: 'cluster1',
                    subnet: new NISubnet(
                        name: '172.31.0.0',
                        properties: [
                            new NIProperty( name: 'subnet', values: ['172.31.0.0'] ),
                            new NIProperty( name: 'netmask', values: ['255.255.0.0'] ),
                            new NIProperty( name: 'gateway', values: ['172.31.0.1'] )
                        ]
                    ),
                    properties: [
                        new NIProperty( name: 'enabledCCIp', values: ['6.6.6.6'] ),
                        new NIProperty( name: 'macPrefix', values: ['d0:0d'] ),
                        new NIProperty( name: 'privateIps', values: ['172.31.0.5'] ),
                    ],
                    nodes: new NINodes( name: 'nodes', nodes: [
                        new NINode(
                            name: 'node1',
                            instanceIds: [ ]
                        )
                    ] )
                )
            ] ),
        ),
        vpcs: [
            new NIVpc(
                name: 'vpc-00000001',
                ownerId: '000000000002',
                cidr: '10.0.0.0/16',
                subnets: [
                    new NIVpcSubnet(
                        name:  'subnet-00000001',
                        ownerId: '000000000002',
                        cidr: '10.0.0.0/16',
                        cluster: 'cluster1',
                        routeTable: 'rtb-00000001'
                    )
                ],
                routeTables: [
                    new NIRouteTable(
                        name: 'rtb-00000001',
                        ownerId: '000000000002',
                        routes: [
                            new NIRoute(
                                destinationCidr: '0.0.0.0/0',
                                natGatewayId: 'nat-00000000000000001',
                            )
                        ]
                    )
                ],
                natGateways: [
                  new NINatGateway(
                      name: 'nat-00000000000000001',
                      ownerId:  '000000000002',
                      vpc:  'vpc-00000001',
                      subnet: 'subnet-00000001',
                      macAddress: '00:00:00:00:00:00',
                      publicIp: '2.0.0.0',
                      privateIp: '10.0.0.0'
                  )
                ],
                internetGateways: [ 'igw-00000001' ]
            )
        ],
        internetGateways: [
            new NIInternetGateway(
                name: 'igw-00000001',
                ownerId: '000000000002'
            )
        ],
        instances: [
        ],
        securityGroups: [ ]
    ), info )
  }

  @Test
  void testBroadcastManaged( ) {
    NetworkInfo info = NetworkInfoBroadcasts.buildNetworkConfiguration(
        Optional.of( new NetworkConfiguration(
            mode: 'MANAGED',
            clusters: [
                new ConfigCluster(
                    name: 'cluster1',
                    macPrefix: 'd0:0d'
                )
            ],
            managedSubnet: new ManagedSubnet(
                subnet: '1.101.192.0',
                netmask: '255.255.0.0',
                minVlan: 512,
                maxVlan: 639,
                segmentSize: 32
            ),
            publicIps: [ '2.0.0.0-2.0.0.255' ],
        ) ),
        new NetworkInfoBroadcasts.NetworkInfoSource( ) {
          @Override Iterable<NetworkInfoBroadcasts.VmInstanceNetworkView> getInstances() {
            [ instance( 'i-00000001', 'cluster1', 'node1', '000000000002', '00:00:00:00:00:00', '2.0.0.0', '10.0.0.0' ) ]
          }
          @Override Iterable<NetworkInfoBroadcasts.NetworkGroupNetworkView> getSecurityGroups() {
            []
          }
          @Override Iterable<NetworkInfoBroadcasts.VpcNetworkView> getVpcs() {
            []
          }
          @Override Iterable<NetworkInfoBroadcasts.SubnetNetworkView> getSubnets() {
            []
          }
          @Override Iterable<NetworkInfoBroadcasts.DhcpOptionSetNetworkView> getDhcpOptionSets() {
            []
          }
          @Override Iterable<NetworkInfoBroadcasts.NetworkAclNetworkView> getNetworkAcls() {
            []
          }
          @Override Iterable<NetworkInfoBroadcasts.RouteTableNetworkView> getRouteTables() {
            []
          }
          @Override Iterable<NetworkInfoBroadcasts.InternetGatewayNetworkView> getInternetGateways() {
            []
          }
          @Override Iterable<NetworkInfoBroadcasts.NetworkInterfaceNetworkView> getNetworkInterfaces() {
            []
          }
          @Override Iterable<NetworkInfoBroadcasts.NatGatewayNetworkView> getNatGateways() {
            []
          }
          @Override Map<String,Iterable<? extends NetworkInfoBroadcasts.VmInstanceNetworkView>> getView() {
            [:]
          }
        },
        { [ cluster('cluster1', '6.6.6.6', [ 'node1' ]) ] } as Supplier<List<Cluster>>,
        { '1.1.1.1' } as Supplier<String>,
        { [ '127.0.0.1' ] } as Function<List<String>, List<String>>,
        [] as Set<String>,
        [] as Set<RouteKey>
    )

    assertEquals( 'broadcast managed', new NetworkInfo(
        configuration: new NIConfiguration(
            properties: [
                new NIProperty( name: 'mode', values: ['MANAGED'] ),
                new NIProperty( name: 'publicIps', values: ['2.0.0.0-2.0.0.255'] ),
                new NIProperty( name: 'enabledCLCIp', values: ['1.1.1.1'] ),
                new NIProperty( name: 'instanceDNSDomain', values: ['eucalyptus.internal'] ),
                new NIProperty( name: 'instanceDNSServers', values: ['127.0.0.1'] ),
            ],
            managedSubnet: new NIManagedSubnets(
                name: 'managedSubnet',
                managedSubnet: new NIManagedSubnet(
                    name: "1.101.192.0",
                    properties: [
                        new NIProperty( name: 'subnet', values: ['1.101.192.0'] ),
                        new NIProperty( name: 'netmask', values: ['255.255.0.0'] ),
                        new NIProperty( name: 'minVlan', values: ['512'] ),
                        new NIProperty( name: 'maxVlan', values: ['639'] ),
                        new NIProperty( name: 'segmentSize', values: ['32'] )
                    ]
                )
            ),
            clusters: new NIClusters( name: 'clusters', clusters: [
                new NICluster(
                    name: 'cluster1',
                    properties: [
                        new NIProperty( name: 'enabledCCIp', values: ['6.6.6.6'] ),
                        new NIProperty( name: 'macPrefix', values: ['d0:0d'] ),
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

  private static NetworkInfoBroadcasts.VmInstanceNetworkView instance( String id, String partition, String node, String ownerAccountNumber, String mac, String publicAddress, String privateAddress, String vpcId = null, String subnetId = null ) {
    new NetworkInfoBroadcasts.VmInstanceNetworkView(
      id,
      1,
      VmState.RUNNING,
      false,
      ownerAccountNumber,
      vpcId,
      subnetId,
      mac,
      privateAddress,
      publicAddress,
      partition,
      node,
      [ ],
    )
  }

  private static NetworkInfoBroadcasts.NetworkInterfaceNetworkView networkInterface( String id, String ownerAccountNumber, String instanceId, String attachmentId, String mac, String publicAddress, String privateAddress, String vpcId, String subnetId ) {
    new NetworkInfoBroadcasts.NetworkInterfaceNetworkView(
        id,
        1,
        NetworkInterface.State.in_use,
        NetworkInterfaceAttachment.Status.attached,
        ownerAccountNumber,
        instanceId,
        attachmentId,
        0,
        mac,
        privateAddress,
        publicAddress,
        true,
        vpcId,
        subnetId,
        [ ],
    )
  }

  private static NetworkInfoBroadcasts.VpcNetworkView vpc( String id, String ownerAccountNumber, String cidr = '10.0.0.0/16', String dhcpOptionSetId = null ) {
    new NetworkInfoBroadcasts.VpcNetworkView(
        id,
        1,
        ownerAccountNumber,
        cidr,
        dhcpOptionSetId
    )
  }

  private static NetworkInfoBroadcasts.SubnetNetworkView subnet( String id, String ownerAccountNumber, String partition, String vpcId, String cidr = '10.0.0.0/16', String networkAclId = null ) {
    new NetworkInfoBroadcasts.SubnetNetworkView(
        id,
        1,
        ownerAccountNumber,
        vpcId,
        cidr,
        partition,
        networkAclId
    )
  }

  private static NetworkInfoBroadcasts.InternetGatewayNetworkView internetGateway( String id, String ownerAccountNumber, String vpcId ) {
    new NetworkInfoBroadcasts.InternetGatewayNetworkView(
        id,
        1,
        ownerAccountNumber,
        vpcId
    )
  }

  private static NetworkInfoBroadcasts.NatGatewayNetworkView natGateway( String id, String ownerAccountNumber, String vpcId, String subnetId, String mac, String publicIp, String privateIp ) {
    new NetworkInfoBroadcasts.NatGatewayNetworkView(
        id,
        1,
        NatGateway.State.available,
        ownerAccountNumber,
        mac,
        privateIp,
        publicIp,
        vpcId,
        subnetId
    )
  }

  private static NetworkInfoBroadcasts.RouteTableNetworkView routeTable( String id, String ownerAccountNumber, String vpcId, boolean main, List<String> subnetIds, List<NetworkInfoBroadcasts.RouteNetworkView> routes = [] ) {
    new NetworkInfoBroadcasts.RouteTableNetworkView(
        id,
        1,
        ownerAccountNumber,
        vpcId,
        main,
        subnetIds,
        routes
    )
  }

  private static NetworkInfoBroadcasts.RouteNetworkView route( String routeTableId, String cidr, String internetGatewayId, String natGatewayId = null ) {
    new NetworkInfoBroadcasts.RouteNetworkView(
        true,
        routeTableId,
        cidr,
        internetGatewayId,
        natGatewayId,
        null,
        null
    )
  }

  private static NetworkInfoBroadcasts.NetworkGroupNetworkView group(
      String id,
      String ownerAccountNumber,
      List<String> rules,
      List<NetworkInfoBroadcasts.IPPermissionNetworkView> ingressRules,
      List<NetworkInfoBroadcasts.IPPermissionNetworkView> egressRules
  ) {
    new NetworkInfoBroadcasts.NetworkGroupNetworkView(
      id,
      1,
      ownerAccountNumber,
      rules,
      ingressRules,
      egressRules
    )
  }
}
