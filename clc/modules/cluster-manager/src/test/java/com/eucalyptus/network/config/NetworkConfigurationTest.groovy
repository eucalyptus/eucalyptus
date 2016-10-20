/*************************************************************************
 * Copyright 2009-2016 Eucalyptus Systems, Inc.
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
package com.eucalyptus.network.config

import org.hamcrest.CoreMatchers

import static org.junit.Assert.*
import org.junit.Test

/**
 *
 */
class NetworkConfigurationTest {

  @Test
  void testFullParse() {
    String config = """
    {
        "Mode": "EDGE",
        "InstanceDnsDomain": "eucalyptus.internal",
        "InstanceDnsServers": [
           "1.2.3.4"
        ],
        "MacPrefix": "d0:0d",
        "PublicIps": [
            "10.111.200.1-10.111.200.2"
        ],
        "PublicGateway": "10.111.0.1",
        "PrivateIps": [
            "1.0.0.33-1.0.0.34"
        ],
        "Subnets": [
            {
                "Name": "1.0.0.0",
                "Subnet": "1.0.0.0",
                "Netmask": "255.255.0.0",
                "Gateway": "1.0.0.1"
            }
        ],
        "Clusters": [
            {
                "Name": "edgecluster0",
                "MacPrefix": "d0:0d",
                "Subnet": {
                    "Name": "1.0.0.0",
                    "Subnet": "1.0.0.0",
                    "Netmask": "255.255.0.0",
                    "Gateway": "1.0.0.1"
                },
                "PrivateIps": [
                    "1.0.0.33",
                    "1.0.0.34"
                ]
            }
        ]
    }
    """.stripIndent()

    NetworkConfiguration result = NetworkConfigurations.parse( config )
    println result

    NetworkConfiguration expected = new NetworkConfiguration(
        mode: 'EDGE',
        instanceDnsDomain: 'eucalyptus.internal',
        instanceDnsServers: [ '1.2.3.4' ],
        macPrefix: 'd0:0d',
        publicIps: [ '10.111.200.1-10.111.200.2' ],
        publicGateway: '10.111.0.1',
        privateIps: [ '1.0.0.33-1.0.0.34' ],
        subnets: [
          new EdgeSubnet(
              name: "1.0.0.0",
              subnet: "1.0.0.0",
              netmask: "255.255.0.0",
              gateway: "1.0.0.1"
          )
        ],
        clusters: [
            new Cluster(
                name: 'edgecluster0',
                macPrefix: 'd0:0d',
                subnet:
                    new EdgeSubnet(
                        name: "1.0.0.0",
                        subnet: "1.0.0.0",
                        netmask: "255.255.0.0",
                        gateway: "1.0.0.1"
                    ),
                privateIps: [ '1.0.0.33', '1.0.0.34' ]
            )
        ]
    )

    assertEquals( 'Result does not match template', expected, result )
  }

  @Test
  void testMinimalTopLevelParse() {
    String config = """
    {
        "PublicIps": [
            "10.111.200.1-10.111.200.2"
        ],

        "Clusters": [
            {
                "Name": "edgecluster0",
                "MacPrefix": "d0:0d",
                "Subnet": {
                    "Subnet": "1.0.0.0",
                    "Netmask": "255.255.0.0",
                    "Gateway": "1.0.0.1"
                },
                "PrivateIps": [
                    "1.0.0.33",
                    "1.0.0.34"
                ]
            }
        ]
    }
    """.stripIndent()

    NetworkConfiguration result = NetworkConfigurations.parse( config )
    println result

    NetworkConfiguration expected = new NetworkConfiguration(
        publicIps: [ '10.111.200.1-10.111.200.2' ],
        clusters: [
            new Cluster(
                name: 'edgecluster0',
                macPrefix: 'd0:0d',
                subnet:
                    new EdgeSubnet(
                        subnet: "1.0.0.0",
                        netmask: "255.255.0.0",
                        gateway: "1.0.0.1"
                    ),
                privateIps: [ '1.0.0.33', '1.0.0.34' ]
            )
        ]
    )

    assertEquals( 'Result does not match template', expected, result )
  }

  @Test
  void testMinimalClusterLevelParse() {
    String config = """
    {
        "MacPrefix": "d0:0d",
        "PublicIps": [
            "10.111.200.1-10.111.200.2"
        ],
        "PrivateIps": [
            "1.0.0.33-1.0.0.34"
        ],
        "Subnets": [
            {
                "Name": "1.0.0.0",
                "Subnet": "1.0.0.0",
                "Netmask": "255.255.0.0",
                "Gateway": "1.0.0.1"
            }
        ],
        "Clusters": [
            {
                "Name": "edgecluster0",
                "Subnet": {
                    "Name": "1.0.0.0"
                }
            }
        ]
    }
    """.stripIndent()

    NetworkConfiguration result = NetworkConfigurations.parse( config )
    println result

    NetworkConfiguration expected = new NetworkConfiguration(
        macPrefix: 'd0:0d',
        publicIps: [ '10.111.200.1-10.111.200.2' ],
        privateIps: [ '1.0.0.33-1.0.0.34' ],
        subnets: [
            new EdgeSubnet(
                name: "1.0.0.0",
                subnet: "1.0.0.0",
                netmask: "255.255.0.0",
                gateway: "1.0.0.1"
            )
        ],
        clusters: [
            new Cluster(
                name: 'edgecluster0',
                subnet:
                    new EdgeSubnet(
                        name: "1.0.0.0"
                    )
            )
        ]
    )

    assertEquals( 'Result does not match template', expected, result )
  }

  @Test
  void testNoClusterLevelParse() {
    String config = """
    {
        "MacPrefix": "d0:0d",
        "PublicIps": [
            "10.111.200.1-10.111.200.2"
        ],
        "PrivateIps": [
            "1.0.0.33-1.0.0.34"
        ],
        "Subnets": [
            {
                "Name": "1.0.0.0",
                "Subnet": "1.0.0.0",
                "Netmask": "255.255.0.0",
                "Gateway": "1.0.0.1"
            }
        ]
    }
    """.stripIndent()

    NetworkConfiguration result = NetworkConfigurations.parse( config )
    println result

    NetworkConfiguration expected = new NetworkConfiguration(
        macPrefix: 'd0:0d',
        publicIps: [ '10.111.200.1-10.111.200.2' ],
        privateIps: [ '1.0.0.33-1.0.0.34' ],
        subnets: [
            new EdgeSubnet(
                name: "1.0.0.0",
                subnet: "1.0.0.0",
                netmask: "255.255.0.0",
                gateway: "1.0.0.1"
            )
        ]
    )

    assertEquals( 'Result does not match template', expected, result )
  }

  @Test( expected = NetworkConfigurationException )
  void testValidateClusterMacPrefix( ) {
    String config = """
    {
        "PublicIps": [
            "10.111.200.1-10.111.200.2"
        ],
        "Clusters": [
            {
                "Name": "edgecluster0",
                "MacPrefix": "d0:0d:",
                "Subnet": {
                    "Subnet": "1.0.0.0",
                    "Netmask": "255.255.0.0",
                    "Gateway": "1.0.0.1"
                },
                "PrivateIps": [
                    "1.0.0.33",
                    "1.0.0.34"
                ]
            }
        ]
    }
    """.stripIndent()

    NetworkConfigurations.parse( config )
  }

  @Test( expected = NetworkConfigurationException )
  void testValidateTopLevelMacPrefix() {
    String config = """
    {
        "MacPrefix": "d0:0d:",
        "PublicIps": [
            "10.111.200.1-10.111.200.2"
        ],
        "PrivateIps": [
            "1.0.0.33-1.0.0.34"
        ],
        "Subnets": [
            {
                "Name": "1.0.0.0",
                "Subnet": "1.0.0.0",
                "Netmask": "255.255.0.0",
                "Gateway": "1.0.0.1"
            }
        ],
        "Clusters": [
            {
                "Name": "edgecluster0",
                "Subnet": {
                    "Name": "1.0.0.0"
                }
            }
        ]
    }
    """.stripIndent()

    NetworkConfigurations.parse( config )
  }

  @Test( expected = NetworkConfigurationException )
  void testValidateInstanceDnsDomain( ) {
    String config = """
    {
        "InstanceDnsDomain": ".eucalyptus.internal",
        "PublicIps": [
            "10.111.200.1-10.111.200.2"
        ],
        "Clusters": [
            {
                "Name": "edgecluster0",
                "MacPrefix": "d0:0d",
                "Subnet": {
                    "Subnet": "1.0.0.0",
                    "Netmask": "255.255.0.0",
                    "Gateway": "1.0.0.1"
                },
                "PrivateIps": [
                    "1.0.0.33",
                    "1.0.0.34"
                ]
            }
        ]
    }
    """.stripIndent()

    NetworkConfigurations.parse( config )
  }

  @Test( expected = NetworkConfigurationException )
  void testValidateTopLevelSubnetSubnet() {
    String config = """
    {
        "PublicIps": [
            "10.111.200.1-10.111.200.2"
        ],
        "PrivateIps": [
            "1.0.0.33-1.0.0.34"
        ],
        "Subnets": [
            {
                "Name": "subnet0",
                "Subnet": "1.0.0.1",
                "Netmask": "255.255.0.0",
                "Gateway": "1.0.0.1"
            }
        ],
        "Clusters": [
            {
                "Name": "edgecluster0",
                "Subnet": {
                    "Name": "subnet0"
                }
            }
        ]
    }
    """.stripIndent()

    NetworkConfigurations.parse( config )
  }

  @Test( expected = NetworkConfigurationException )
  void testValidateTopLevelSubnetNetmask() {
    String config = """
    {
        "PublicIps": [
            "10.111.200.1-10.111.200.2"
        ],
        "PrivateIps": [
            "1.0.0.33-1.0.0.34"
        ],
        "Subnets": [
            {
                "Subnet": "1.0.0.0",
                "Netmask": "255.255.0.1",
                "Gateway": "1.0.0.1"
            }
        ],
        "Clusters": [
            {
                "Name": "edgecluster0",
                "Subnet": {
                    "Name": "1.0.0.0"
                }
            }
        ]
    }
    """.stripIndent()

    NetworkConfigurations.parse( config )
  }

  @Test( expected = NetworkConfigurationException )
  void testValidateTopLevelSubnetGateway() {
    String config = """
    {
        "PublicIps": [
            "10.111.200.1-10.111.200.2"
        ],
        "PrivateIps": [
            "1.0.0.33-1.0.0.34"
        ],
        "Subnets": [
            {
                "Subnet": "1.0.0.0",
                "Netmask": "255.255.0.0",
                "Gateway": "1.1.0.1"
            }
        ],
        "Clusters": [
            {
                "Name": "edgecluster0",
                "Subnet": {
                    "Name": "1.0.0.0"
                }
            }
        ]
    }
    """.stripIndent()

    NetworkConfigurations.parse( config )
  }

  @Test
  void testVpcMidoOldConfigFormatParse() {
    String config = """
    {
        "Mode": "VPCMIDO",
        "MacPrefix": "d0:0d",
        "PublicIps": [
            "10.111.200.1-10.111.200.2"
        ],
        "Mido": {
          "EucanetdHost": "a",
          "Gateways": [
            {
              "GatewayHost": "b",
              "GatewayIP": "10.0.0.1",
              "GatewayInterface": "foo"
            }
          ],
          "PublicNetworkCidr": "0.0.0.0/0",
          "PublicGatewayIP": "10.0.0.1"
        }
    }
    """.stripIndent()

    NetworkConfiguration result = NetworkConfigurations.parse( config )
    println result

    NetworkConfiguration expected = new NetworkConfiguration(
        mode: 'VPCMIDO',
        macPrefix: 'd0:0d',
        publicIps: [ '10.111.200.1-10.111.200.2' ],
        mido: new Midonet(
                eucanetdHost: 'a',
                gateways: [
                    new MidonetGateway(
                        gatewayHost: 'b',
                        gatewayIP: '10.0.0.1',
                        gatewayInterface: 'foo',
                    )
                ],
                publicNetworkCidr: '0.0.0.0/0',
                publicGatewayIP: '10.0.0.1'
        )
    )

    assertEquals( 'Result does not match template', expected, result )
  }

  @Test
  void testVpcMidoOldConfigFormatSingleGatewayParse() {
    String config = """
    {
        "Mode": "VPCMIDO",
        "MacPrefix": "d0:0d",
        "PublicIps": [
            "10.111.200.1-10.111.200.2"
        ],
        "Mido": {
          "EucanetdHost": "a",
          "GatewayHost": "b",
          "GatewayIP": "10.0.0.1",
          "GatewayInterface": "foo",
          "PublicNetworkCidr": "0.0.0.0/0",
          "PublicGatewayIP": "10.0.0.1"
        }
    }
    """.stripIndent()

    NetworkConfiguration result = NetworkConfigurations.parse( config )
    println result

    NetworkConfiguration expected = new NetworkConfiguration(
        mode: 'VPCMIDO',
        macPrefix: 'd0:0d',
        publicIps: [ '10.111.200.1-10.111.200.2' ],
        mido: new Midonet(
            eucanetdHost: 'a',
            gatewayHost: 'b',
            gatewayIP: '10.0.0.1',
            gatewayInterface: 'foo',
            publicNetworkCidr: '0.0.0.0/0',
            publicGatewayIP: '10.0.0.1'
        )
    )

    assertEquals( 'Result does not match template', expected, result )
  }

  @Test
  void testVpcMidoParse() {
    String config = """
    {
        "Mode": "VPCMIDO",
        "MacPrefix": "d0:0d",
        "PublicIps": [
            "10.111.200.1-10.111.200.2"
        ],
        "Mido": {
          "BgpAsn": "64512",
          "Gateways": [
            {
              "Ip": "10.111.5.11",
              "ExternalDevice": "em1.116",
              "ExternalCidr": "10.116.128.0/17",
              "ExternalIp": "10.116.133.11",
              "BgpPeerIp": "10.116.133.173",
              "BgpPeerAsn": "65000",
              "BgpAdRoutes": [
                "10.116.150.0/24"
              ]
            },
            {
              "Ip": "10.111.5.22",
              "ExternalDevice": "em1.117",
              "ExternalCidr": "10.117.128.0/17",
              "ExternalIp": "10.117.133.22",
              "BgpPeerIp": "10.117.133.173",
              "BgpPeerAsn": "65001",
              "BgpAdRoutes": [
                  "10.117.150.0/24"
              ]
            }
          ]
        }
    }
    """.stripIndent()

    NetworkConfiguration result = NetworkConfigurations.parse( config )
    println result

    NetworkConfiguration expected = new NetworkConfiguration(
        mode: 'VPCMIDO',
        macPrefix: 'd0:0d',
        publicIps: [ '10.111.200.1-10.111.200.2' ],
        mido: new Midonet(
            bgpAsn: '64512',
            gateways: [
                new MidonetGateway(
                    ip: '10.111.5.11',
                    externalDevice: 'em1.116',
                    externalCidr: '10.116.128.0/17',
                    externalIp: '10.116.133.11',
                    bgpPeerIp: '10.116.133.173',
                    bgpPeerAsn: '65000',
                    bgpAdRoutes: [
                        '10.116.150.0/24'
                    ]
                ),
                new MidonetGateway(
                    ip: '10.111.5.22',
                    externalDevice: 'em1.117',
                    externalCidr: '10.117.128.0/17',
                    externalIp: '10.117.133.22',
                    bgpPeerIp: '10.117.133.173',
                    bgpPeerAsn: '65001',
                    bgpAdRoutes: [
                        '10.117.150.0/24'
                    ]
                ),
            ]
        )
    )

    assertEquals( 'Result does not match template', expected, result )
  }

  @Test
  void testVpcMidoInvalidMissingGatewaysParse() {
    String config = """
    {
        "Mode": "VPCMIDO",
        "MacPrefix": "d0:0d",
        "PublicIps": [
            "10.111.200.1-10.111.200.2"
        ],
        "Mido": {
          "EucanetdHost": "a",
          "BgpAsn": "64512"
        }
    }
    """.stripIndent()

    try {
      NetworkConfigurations.parse( config )
      fail( "Expected error due to missing Mido.Gateways property" )
    } catch ( NetworkConfigurationException nce ) {
      assertEquals( 'Parsing error', 'Missing required property "Mido.Gateways"', nce.message )
    }
  }

  @Test
  void testVpcMidoInvalidEmptyGatewaysParse() {
    String config = """
    {
        "Mode": "VPCMIDO",
        "MacPrefix": "d0:0d",
        "PublicIps": [
            "10.111.200.1-10.111.200.2"
        ],
        "Mido": {
          "EucanetdHost": "a",
          "BgpAsn": "64512",
          "Gateways": []
        }
    }
    """.stripIndent()

    try {
      NetworkConfigurations.parse( config )
      fail( "Expected error due to missing Mido.Gateways property" )
    } catch ( NetworkConfigurationException nce ) {
      assertEquals( 'Parsing error', 'At least one gateway is required "Mido.Gateways"', nce.message )
    }
  }

  @Test
  void testVpcMidoInvalidMixedGatewayParse() {
    String config = """
    {
        "Mode": "VPCMIDO",
        "MacPrefix": "d0:0d",
        "PublicIps": [
            "10.111.200.1-10.111.200.2"
        ],
        "Mido": {
          "EucanetdHost": "a",
          "BgpAsn": "64512",
          "Gateways": [
            {
              "GatewayIP": "10.111.5.11",
              "Ip": "10.111.5.11",
              "ExternalDevice": "em1.116",
              "ExternalCidr": "10.116.128.0/17",
              "ExternalIp": "10.116.133.11",
              "BgpPeerIp": "10.116.133.173",
              "BgpPeerAsn": "65000",
              "BgpAdRoutes": [
                "10.116.150.0/24"
              ]
            }
          ]
        }
    }
    """.stripIndent()

    try {
      NetworkConfigurations.parse( config )
      fail( "Expected error due to legacy GatewayIP property used with new properties" )
    } catch ( NetworkConfigurationException nce ) {
      assertEquals( 'Parsing error', 'Invalid use of property "Mido.Gateways[0].GatewayIP"', nce.message )
    }
  }

  @Test
  void testVpcMidoInvalidExternalIpGatewayParse() {
    String config = """
    {
        "Mode": "VPCMIDO",
        "MacPrefix": "d0:0d",
        "PublicIps": [
            "10.111.200.1-10.111.200.2"
        ],
        "Mido": {
          "EucanetdHost": "a",
          "BgpAsn": "64512",
          "Gateways": [
            {
              "Ip": "10.111.5.11",
              "ExternalDevice": "em1.116",
              "ExternalCidr": "10.116.128.0/17",
              "ExternalIp": "10.116.128.0",
              "BgpPeerIp": "10.116.133.173",
              "BgpPeerAsn": "65000",
              "BgpAdRoutes": [
                "10.116.150.0/24"
              ]
            }
          ]
        }
    }
    """.stripIndent()

    try {
      NetworkConfigurations.parse( config )
      fail( "Expected error due to ExternalIP not within ExternalCidr" )
    } catch ( NetworkConfigurationException nce ) {
      assertEquals( 'Parsing error',
          'ExternalIp must be within ExternalCidr "Mido.Gateways[0].ExternalIp"', nce.message )
    }
  }

  @Test
  void testVpcMidoInvalidParse() {
    String config = """
    {
        "Mode": "VPCMIDO",
        "MacPrefix": "d0:0d",
        "PublicIps": [
            "10.111.200.1-10.111.200.2"
        ]
    }
    """.stripIndent()

    try {
      NetworkConfigurations.parse( config )
      fail( "Expected error due to missing Mido property" )
    } catch ( NetworkConfigurationException nce ) {
      assertEquals( 'Parsing error', 'Missing required property "Mido"', nce.message )
    }
  }

  @Test
  void testUnknownPropertyParse() {
    String config = """
    {
        "PublicIpss": [
            "10.111.200.1-10.111.200.2"
        ]
    }
    """.stripIndent()

    try {
      NetworkConfigurations.parse( config )
      fail( "Expected error due to incorrect PublicIpss property" )
    } catch ( NetworkConfigurationException nce ) {
      assertThat( 'Parsing error starts with field',
          nce.message, CoreMatchers.startsWith('Unrecognized field "PublicIpss"') )
      assertThat( 'Parsing error does not mention MetaClass',
          nce.message, CoreMatchers.not( CoreMatchers.containsString('MetaClass') ) )
    }
  }
}
