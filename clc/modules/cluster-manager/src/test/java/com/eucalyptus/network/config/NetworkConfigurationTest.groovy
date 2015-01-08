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
package com.eucalyptus.network.config

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
        "InstanceDnsDomain": "eucalyptus.internal",
        "InstanceDnsServers": [
           "1.2.3.4"
        ],
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
        instanceDnsDomain: 'eucalyptus.internal',
        instanceDnsServers: [ '1.2.3.4' ],
        macPrefix: 'd0:0d',
        publicIps: [ '10.111.200.1-10.111.200.2' ],
        privateIps: [ '1.0.0.33-1.0.0.34' ],
        subnets: [
          new Subnet(
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
                    new Subnet(
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
                    new Subnet(
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
            new Subnet(
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
                    new Subnet(
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
            new Subnet(
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

}
