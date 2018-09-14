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

    NetworkConfigurationApi.NetworkConfiguration result = NetworkConfigurations.parse( config )
    println result

    NetworkConfigurationApi.NetworkConfiguration expected = ImmutableNetworkConfigurationApi.NetworkConfiguration.builder( )
        .mode('EDGE')
        .instanceDnsDomain('eucalyptus.internal')
        .instanceDnsServer('1.2.3.4')
        .macPrefix('d0:0d')
        .publicIp('10.111.200.1-10.111.200.2')
        .publicGateway('10.111.0.1')
        .privateIp('1.0.0.33-1.0.0.34')
        .subnet(ImmutableNetworkConfigurationApi.EdgeSubnet.builder( )
            .name('1.0.0.0')
            .subnet('1.0.0.0')
            .netmask('255.255.0.0')
            .gateway('1.0.0.1')
            .o( ) )
        .cluster(ImmutableNetworkConfigurationApi.Cluster.builder( )
            .name('edgecluster0')
            .macPrefix('d0:0d')
            .subnet(ImmutableNetworkConfigurationApi.EdgeSubnet.builder( )
                .name('1.0.0.0')
                .subnet('1.0.0.0')
                .netmask('255.255.0.0')
                .gateway('1.0.0.1')
                .o( ) )
            .privateIp('1.0.0.33')
            .privateIp('1.0.0.34')
            .o( ) )
        .o( )

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

    NetworkConfigurationApi.NetworkConfiguration result = NetworkConfigurations.parse( config )
    println result

    NetworkConfigurationApi.NetworkConfiguration expected = ImmutableNetworkConfigurationApi.NetworkConfiguration.builder( )
        .publicIp('10.111.200.1-10.111.200.2')
        .cluster(ImmutableNetworkConfigurationApi.Cluster.builder( )
            .name('edgecluster0')
            .macPrefix('d0:0d')
            .subnet(ImmutableNetworkConfigurationApi.EdgeSubnet.builder( )
                .subnet('1.0.0.0')
                .netmask('255.255.0.0')
                .gateway('1.0.0.1')
                .o( ) )
            .privateIp('1.0.0.33')
            .privateIp('1.0.0.34')
            .o( ) )
        .o( )

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

    NetworkConfigurationApi.NetworkConfiguration result = NetworkConfigurations.parse( config )
    println result

    NetworkConfigurationApi.NetworkConfiguration expected = ImmutableNetworkConfigurationApi.NetworkConfiguration.builder( )
        .macPrefix('d0:0d')
        .publicIp('10.111.200.1-10.111.200.2')
        .privateIp('1.0.0.33-1.0.0.34')
        .subnet(ImmutableNetworkConfigurationApi.EdgeSubnet.builder( )
            .name('1.0.0.0')
            .subnet('1.0.0.0')
            .netmask('255.255.0.0')
            .gateway('1.0.0.1')
            .o( ) )
        .cluster(ImmutableNetworkConfigurationApi.Cluster.builder( )
            .name('edgecluster0')
            .subnet(ImmutableNetworkConfigurationApi.EdgeSubnet.builder( )
                .name('1.0.0.0')
                .o( ) )
            .o( ) )
        .o( )

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

    NetworkConfigurationApi.NetworkConfiguration result = NetworkConfigurations.parse( config )
    println result

    NetworkConfigurationApi.NetworkConfiguration expected = ImmutableNetworkConfigurationApi.NetworkConfiguration.builder( )
        .macPrefix('d0:0d')
        .publicIp('10.111.200.1-10.111.200.2')
        .privateIp('1.0.0.33-1.0.0.34')
        .subnet(ImmutableNetworkConfigurationApi.EdgeSubnet.builder( )
            .name('1.0.0.0')
            .subnet('1.0.0.0')
            .netmask('255.255.0.0')
            .gateway('1.0.0.1')
            .o( ) )
        .o( )

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

    NetworkConfigurationApi.NetworkConfiguration result = NetworkConfigurations.parse( config )
    println result

    NetworkConfigurationApi.NetworkConfiguration expected = ImmutableNetworkConfigurationApi.NetworkConfiguration.builder( )
        .mode('VPCMIDO')
        .macPrefix('d0:0d')
        .mido(ImmutableNetworkConfigurationApi.Midonet.builder( )
            .eucanetdHost('a')
            .gateway(ImmutableNetworkConfigurationApi.MidonetGateway.builder( )
                .gatewayIP('10.0.0.1')
                .gatewayHost('b')
                .gatewayInterface('foo')
                .o( ) )
            .publicNetworkCidr('0.0.0.0/0')
            .publicGatewayIP('10.0.0.1')
            .o( ) )
        .publicIp('10.111.200.1-10.111.200.2')
        .o( )

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

    NetworkConfigurationApi.NetworkConfiguration result = NetworkConfigurations.parse( config )
    println result

    NetworkConfigurationApi.NetworkConfiguration expected = ImmutableNetworkConfigurationApi.NetworkConfiguration.builder( )
        .mode('VPCMIDO')
        .macPrefix('d0:0d')
        .mido(ImmutableNetworkConfigurationApi.Midonet.builder( )
            .eucanetdHost('a')
            .gatewayHost('b')
            .gatewayIP('10.0.0.1')
            .gatewayInterface('foo')
            .publicNetworkCidr('0.0.0.0/0')
            .publicGatewayIP('10.0.0.1')
            .o( ) )
        .publicIp('10.111.200.1-10.111.200.2')
        .o( )

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

    NetworkConfigurationApi.NetworkConfiguration result = NetworkConfigurations.parse( config )
    println result

    NetworkConfigurationApi.NetworkConfiguration expected2 = ImmutableNetworkConfigurationApi.NetworkConfiguration.builder( )
        .mode('VPCMIDO')
        .macPrefix('d0:0d')
        .mido(ImmutableNetworkConfigurationApi.Midonet.builder( )
            .gateway(ImmutableNetworkConfigurationApi.MidonetGateway.builder( )
                .ip('10.111.5.11')
                .externalCidr('10.116.128.0/17')
                .externalDevice('em1.116')
                .externalIp('10.116.133.11')
                .bgpPeerIp('10.116.133.173')
                .bgpPeerAsn('65000')
                .bgpAdRoute('10.116.150.0/24')
                .o( ) )
            .gateway(ImmutableNetworkConfigurationApi.MidonetGateway.builder( )
                .ip('10.111.5.22')
                .externalCidr('10.117.128.0/17')
                .externalDevice('em1.117')
                .externalIp('10.117.133.22')
                .bgpPeerIp('10.117.133.173')
                .bgpPeerAsn('65001')
                .bgpAdRoute('10.117.150.0/24')
                .o( ) )
            .bgpAsn('64512')
            .o( ) )
        .publicIp('10.111.200.1-10.111.200.2')
        .o( )

    assertEquals( 'Result does not match template', expected2, result )
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
      assertEquals( 'Parsing error', 'At least one gateway is required "Mido.Gateways"', nce.message )
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
  void testVpcMidoBgpPeerIpFirstInExternalCidrParse() {
    String config = """
    {
        "Mode": "VPCMIDO",
        "InstanceDnsDomain": "eucalyptus.internal",
        "InstanceDnsServers": [
            "10.39.12.252",
            "10.39.39.252"
        ],
        "PublicIps": [
            "10.39.16.1-10.39.16.255",
            "10.39.17.1-10.39.17.255",
            "10.39.18.1-10.39.18.255",
            "10.39.19.1-10.39.19.255"
        ],
        "Mido": {
            "BgpAsn": "66100",
            "Gateways": [
                {
                    "Ip": "10.39.12.15",
                    "ExternalDevice": "eno2.319",
                    "ExternalCidr": "10.39.14.0/28",
                    "ExternalIp": "10.39.14.6",
                    "BgpPeerIp": "10.39.14.1",
                    "BgpPeerAsn": "66821",
                    "BgpAdRoutes": [
                        "10.39.16.0/21"
                    ]
                },
                {
                    "Ip": "10.39.12.16",
                    "ExternalDevice": "eno2.318",
                    "ExternalCidr": "10.39.15.0/28",
                    "ExternalIp": "10.39.15.6",
                    "BgpPeerIp": "10.39.15.1",
                    "BgpPeerAsn": "66821",
                    "BgpAdRoutes": [
                        "10.39.16.0/21"
                    ]
                }
            ]
        }
    }
    """.stripIndent()

    NetworkConfigurations.parse( config )
  }

  @Test
  void testVpcMidoInvalidBgpPeerIpFirstInExternalCidrParse() {
    String config = """
    {
        "Mode": "VPCMIDO",
        "InstanceDnsDomain": "eucalyptus.internal",
        "InstanceDnsServers": [
            "10.39.12.252",
            "10.39.39.252"
        ],
        "PublicIps": [
            "10.39.16.1-10.39.16.255",
            "10.39.17.1-10.39.17.255",
            "10.39.18.1-10.39.18.255",
            "10.39.19.1-10.39.19.255"
        ],
        "Mido": {
            "BgpAsn": "66100",
            "Gateways": [
                {
                    "Ip": "10.39.12.15",
                    "ExternalDevice": "eno2.319",
                    "ExternalCidr": "10.39.14.0/28",
                    "ExternalIp": "10.39.14.6",
                    "BgpPeerIp": "10.39.14.0",
                    "BgpPeerAsn": "66821",
                    "BgpAdRoutes": [
                        "10.39.16.0/21"
                    ]
                },
                {
                    "Ip": "10.39.12.16",
                    "ExternalDevice": "eno2.318",
                    "ExternalCidr": "10.39.15.0/28",
                    "ExternalIp": "10.39.15.6",
                    "BgpPeerIp": "10.39.15.1",
                    "BgpPeerAsn": "66821",
                    "BgpAdRoutes": [
                        "10.39.16.0/21"
                    ]
                }
            ]
        }
    }
    """.stripIndent()

    try {
      NetworkConfigurations.parse( config )
      fail( "Expected error due to BgpPeerIp not within ExternalCidr" )
    } catch ( NetworkConfigurationException nce ) {
      assertEquals( 'Parsing error',
          'BgpPeerIp must be within ExternalCidr "Mido.Gateways[0].BgpPeerIp"', nce.message )
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
