/*************************************************************************
 * Copyright 2009-2015 Ent. Services Development Corporation LP
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

import com.eucalyptus.network.PrivateAddresses
import com.eucalyptus.vm.VmInstances
import com.google.common.base.Optional
import com.google.common.collect.Lists
import groovy.transform.CompileStatic
import org.junit.Test
import static org.junit.Assert.*

/**
 *
 */
@CompileStatic
class NetworkConfigurationsTest {

  @Test
  void testConfiguredMacPrefix(){
    // verify default when not configured
    NetworkConfigurations.getMacPrefix( Optional.of( new NetworkConfiguration(
    ) ) ).with{ String macPrefix ->
      assertEquals( 'default mac prefix', VmInstances.MAC_PREFIX, macPrefix )
    }

    // verify configuration from top level
    NetworkConfigurations.getMacPrefix( Optional.of( new NetworkConfiguration(
        macPrefix: '00:00'
    ) ) ).with{ String macPrefix ->
      assertEquals( 'top level mac prefix', '00:00', macPrefix )
    }
    NetworkConfigurations.getMacPrefix( Optional.of( new NetworkConfiguration(
        macPrefix: '00:00',
        clusters: [
            new Cluster( name: 'cluster0' )
        ]
    ) ) ).with{ String macPrefix ->
      assertEquals( 'top level mac prefix, cluster without macPrefix ignored', '00:00', macPrefix )
    }
    NetworkConfigurations.getMacPrefix( Optional.of( new NetworkConfiguration(
        macPrefix: '00:00',
        clusters: [
            new Cluster(
                name: 'cluster0',
                macPrefix: '11:00'
            ),
            new Cluster(
                name: 'cluster1',
                macPrefix: '11:11'
            )
        ]
    ) ) ).with{ String macPrefix ->
      assertEquals( 'top level mac prefix, multiple clusters ignored', '00:00', macPrefix )
    }

    // verify configuration from cluster level
    NetworkConfigurations.getMacPrefix( Optional.of( new NetworkConfiguration(
        macPrefix: '00:00',
        clusters: [
            new Cluster(
                name: 'cluster0',
                macPrefix: '11:11'
            )
        ]
    ) ) ).with{ String macPrefix ->
      assertEquals( 'top level mac prefix, cluster ignored', '11:11', macPrefix )
    }
  }

  @Test
  void testConfiguredPrivateIPsForCluster(){
    // verify configuration from top level
    NetworkConfigurations.getPrivateAddresses( new NetworkConfiguration(
        privateIps: [ '10.10.10.10-10.10.10.11' ],
        subnets: [
            new EdgeSubnet(
                subnet: "10.10.10.0",
                netmask: "255.255.255.0",
                gateway: "10.10.10.1"
            )
        ],
        clusters: [
            new Cluster( name: 'cluster0', privateIps: [ '10.20.10.10-20.10.10.11' ] )
        ] ), 'cluster1' ).left.with{ Iterable<Integer> ips ->
      assertEquals( 'private address list from top level', [ '10.10.10.10', '10.10.10.11' ], Lists.newArrayList( ips.collect( PrivateAddresses.&fromInteger ) ) )
    }

    // verify configuration from cluster level
    NetworkConfigurations.getPrivateAddresses( new NetworkConfiguration(
        privateIps: [ '1.1.1.1' ],
        subnets: [
            new EdgeSubnet(
                subnet: "10.0.0.0",
                netmask: "255.0.0.0",
                gateway: "10.0.0.1"
            )
        ],
        clusters: [
            new Cluster( name: 'cluster0', privateIps: [ '10.20.10.10-20.10.10.11' ] ),
            new Cluster( name: 'cluster1', privateIps: [ '10.10.10.10-10.10.10.11' ] ),
            new Cluster( name: 'cluster2', privateIps: [ '10.30.10.10-10.30.10.11' ] )
        ] ), 'cluster1' ).left.with{ Iterable<Integer> ips ->
      assertEquals( 'private address list from cluster level', [ '10.10.10.10', '10.10.10.11' ], Lists.newArrayList( ips.collect( PrivateAddresses.&fromInteger ) ) )
    }
  }

  @Test
  void testExplodeFull() {
    NetworkConfiguration result = NetworkConfigurations.explode(
        new NetworkConfiguration(
            instanceDnsDomain: 'eucalyptus.internal',
            instanceDnsServers: [ '10.1.1.254' ],
            macPrefix: 'ab:cd',
            publicIps: [ '10.111.103.26', '10.111.103.27', '10.111.103.28', '10.111.103.29' ],
            publicGateway: '10.111.0.1',
            privateIps: [ '10.111.103.30', '10.111.103.36', '10.111.103.38', '10.111.103.42' ],
            subnets: [
                new EdgeSubnet(
                    subnet: "10.111.0.0",
                    netmask: "255.255.0.0",
                    gateway: "10.111.0.1"
                )
            ],
        ),
        [ 'cluster1' ]
    )

    NetworkConfiguration expected = new NetworkConfiguration(
        instanceDnsDomain: 'eucalyptus.internal',
        instanceDnsServers: [ '10.1.1.254' ],
        macPrefix: 'ab:cd',
        publicIps: [ '10.111.103.26', '10.111.103.27', '10.111.103.28', '10.111.103.29' ],
        publicGateway: '10.111.0.1',
        privateIps: [ '10.111.103.30', '10.111.103.36', '10.111.103.38', '10.111.103.42' ],
        clusters: [
            new Cluster(
                name: 'cluster1',
                macPrefix: 'ab:cd',
                privateIps: [ '10.111.103.30', '10.111.103.36', '10.111.103.38', '10.111.103.42' ],
                subnet: new EdgeSubnet(
                    name: "10.111.0.0",
                    subnet: "10.111.0.0",
                    netmask: "255.255.0.0",
                    gateway: "10.111.0.1"
                )
            )
        ]
    )

    assertEquals( 'Result does not match template', expected, result )
  }

  @Test
  void testExplodeMinimal() {
    NetworkConfiguration result = NetworkConfigurations.explode(
        new NetworkConfiguration(
            publicIps: [ '10.111.103.26', '10.111.103.27', '10.111.103.28', '10.111.103.29' ],
            subnets: [
                new EdgeSubnet(
                    subnet: "10.111.0.0",
                    netmask: "255.255.0.0",
                    gateway: "10.111.0.1"
                )
            ],
        ),
        [ 'cluster1' ]
    )

    NetworkConfiguration expected = new NetworkConfiguration(
        publicIps: [ '10.111.103.26', '10.111.103.27', '10.111.103.28', '10.111.103.29' ],
        clusters: [
            new Cluster(
                name: 'cluster1',
                macPrefix: 'd0:0d',
                privateIps: [ '10.111.0.2-10.111.255.254' ],
                subnet: new EdgeSubnet(
                    name: "10.111.0.0",
                    subnet: "10.111.0.0",
                    netmask: "255.255.0.0",
                    gateway: "10.111.0.1"
                )
            )
        ]
    )

    assertEquals( 'Result does not match template', expected, result )
  }

  @Test( expected = NetworkConfigurationException )
  void testValidateSingleSubnet() {
    NetworkConfigurations.validate(
        new NetworkConfiguration(
            publicIps: [ '10.111.103.26' ]
        )
    )
  }

  @Test( expected = NetworkConfigurationException )
  void testValidateSingleSubnetIPRanges() {
    NetworkConfigurations.validate(
        new NetworkConfiguration(
            publicIps: [ '10.111.103.26' ],
            privateIps: [ '10.1.1.1' ],
            subnets: [
                new EdgeSubnet(
                    subnet: "10.111.0.0",
                    netmask: "255.255.0.0",
                    gateway: "10.111.0.1"
                )
            ],
        )
    )
  }

  @Test( expected = NetworkConfigurationException )
  void testValidateClusterSubnetIPRanges() {
    NetworkConfigurations.validate(
        new NetworkConfiguration(
            publicIps: [ '10.111.103.26' ],
            privateIps: [ '10.1.1.1' ],
            clusters: [
                new Cluster(
                    name: 'cluster1',
                    privateIps: [ '10.1.1.1' ],
                    subnet: new EdgeSubnet(
                        name: "10.111.0.0",
                        subnet: "10.111.0.0",
                        netmask: "255.255.0.0",
                        gateway: "10.111.0.1"
                    )
                )
            ]
        )
    )
  }
}
