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
        "PublicIps": [
            "10.111.200.1"
        ],
        "PrivateIps": [
            "1.0.0.33"
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
        publicIps: [ '10.111.200.1' ],
        privateIps: [ '1.0.0.33' ],
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
}
