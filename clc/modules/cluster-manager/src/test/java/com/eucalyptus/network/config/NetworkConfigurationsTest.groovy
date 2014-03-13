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

import com.eucalyptus.network.PrivateAddresses
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
  void testBuildConfigurationFromProperties( ) {
    Optional<NetworkConfiguration> networkConfigurationOptional =
      NetworkConfigurations.buildNetworkConfigurationFromProperties( [], [
        VNET_MODE: '"EDGE"',
        VNET_DOMAINNAME: '"eucalyptus.internal"',
        VNET_DNS: '"10.1.1.254"',
        VNET_PUBLICIPS: '"10.111.103.26 10.111.103.27 10.111.103.28 10.111.103.29"',
        VNET_PRIVATEIPS: '"10.111.103.30 10.111.103.36 10.111.103.38 10.111.103.42"',
        VNET_SUBNET: '"10.111.0.0"',
        VNET_NETMASK: '"255.255.0.0"',
        VNET_ROUTER: '"10.111.0.1"'
      ] )
    assertTrue( "Expected network configuration", networkConfigurationOptional.isPresent( ) )

    NetworkConfiguration result = networkConfigurationOptional.get( )
    println result

    NetworkConfiguration expected = new NetworkConfiguration(
        instanceDnsDomain: 'eucalyptus.internal',
        instanceDnsServers: [ '10.1.1.254' ],
        publicIps: [ '10.111.103.26', '10.111.103.27', '10.111.103.28', '10.111.103.29' ],
        privateIps: [ '10.111.103.30', '10.111.103.36', '10.111.103.38', '10.111.103.42' ],
        subnets: [
            new Subnet(
                subnet: "10.111.0.0",
                netmask: "255.255.0.0",
                gateway: "10.111.0.1"
            )
        ],
    )

    assertEquals( 'Result does not match template', expected, result )
  }

  @Test
  void testConfiguredPrivateIPsForCluster(){
    // verify behavior with empty configuration
    NetworkConfigurations.getPrivateAddresses( new NetworkConfiguration( ), 'cluster1' ).with{
      assertFalse( 'expected no addresses', iterator( ).hasNext( ) )
    }

    // verify configuration from top level
    NetworkConfigurations.getPrivateAddresses( new NetworkConfiguration(
        privateIps: [ '10.10.10.10-10.10.10.11' ],
        clusters: [
            new Cluster( name: 'cluster0', privateIps: [ '10.20.10.10-20.10.10.11' ] )
        ] ), 'cluster1' ).with{ Iterable<Integer> ips ->
      assertEquals( 'private address list from top level', [ '10.10.10.10', '10.10.10.11' ], Lists.newArrayList( ips.collect( PrivateAddresses.&fromInteger ) ) )
    }

    // verify configuration from cluster level
    NetworkConfigurations.getPrivateAddresses( new NetworkConfiguration(
        privateIps: [ '1.1.1.1' ],
        clusters: [
            new Cluster( name: 'cluster0', privateIps: [ '10.20.10.10-20.10.10.11' ] ),
            new Cluster( name: 'cluster1', privateIps: [ '10.10.10.10-10.10.10.11' ] ),
            new Cluster( name: 'cluster2', privateIps: [ '10.30.10.10-10.30.10.11' ] )
        ] ), 'cluster1' ).with{ Iterable<Integer> ips ->
      assertEquals( 'private address list from top level', [ '10.10.10.10', '10.10.10.11' ], Lists.newArrayList( ips.collect( PrivateAddresses.&fromInteger ) ) )
    }
  }
}
