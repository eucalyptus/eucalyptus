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
import com.google.common.collect.Lists
import groovy.transform.CompileStatic
import io.vavr.control.Option
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
    NetworkConfigurations.getMacPrefix( Option.<NetworkConfigurationApi.NetworkConfiguration>of(
        ImmutableNetworkConfigurationApi.NetworkConfiguration.builder( ).o( )
    ) ).with{ String macPrefix ->
      assertEquals( 'default mac prefix', VmInstances.MAC_PREFIX, macPrefix )
    }

    // verify configuration from top level
    NetworkConfigurations.getMacPrefix( Option.<NetworkConfigurationApi.NetworkConfiguration>of(
        ImmutableNetworkConfigurationApi.NetworkConfiguration.builder( )
            .setValueMacPrefix( '00:00' )
            .o( )
    )  ).with{ String macPrefix ->
      assertEquals( 'top level mac prefix', '00:00', macPrefix )
    }
    NetworkConfigurations.getMacPrefix( Option.<NetworkConfigurationApi.NetworkConfiguration>of(
        ImmutableNetworkConfigurationApi.NetworkConfiguration.builder( )
            .setValueMacPrefix( '00:00' )
            .cluster( ImmutableNetworkConfigurationApi.Cluster.builder().setValueName('cluster0').o() )
            .o( )
    ) ).with{ String macPrefix ->
      assertEquals( 'top level mac prefix, cluster without macPrefix ignored', '00:00', macPrefix )
    }
    NetworkConfigurations.getMacPrefix( Option.<NetworkConfigurationApi.NetworkConfiguration>of(
        ImmutableNetworkConfigurationApi.NetworkConfiguration.builder( )
            .setValueMacPrefix( '00:00' )
            .cluster( ImmutableNetworkConfigurationApi.Cluster.builder( )
                .setValueName('cluster0')
                .setValueMacPrefix( '11:00' )
                .o( ) )
            .cluster( ImmutableNetworkConfigurationApi.Cluster.builder( )
                .setValueName('cluster1')
                .setValueMacPrefix( '11:11' )
                .o( ) )
            .o( )
    ) ).with{ String macPrefix ->
      assertEquals( 'top level mac prefix, multiple clusters ignored', '00:00', macPrefix )
    }

    // verify configuration from cluster level
    NetworkConfigurations.getMacPrefix( Option.<NetworkConfigurationApi.NetworkConfiguration>of(
        ImmutableNetworkConfigurationApi.NetworkConfiguration.builder( )
            .setValueMacPrefix( '00:00' )
            .cluster( ImmutableNetworkConfigurationApi.Cluster.builder( )
                .setValueName('cluster0')
                .setValueMacPrefix( '11:11' )
                .o( ) )
            .o( )
    ) ).with{ String macPrefix ->
      assertEquals( 'top level mac prefix, cluster ignored', '11:11', macPrefix )
    }
  }

  @Test
  void testConfiguredPrivateIPsForCluster(){
    // verify configuration from top level
    NetworkConfigurations.getPrivateAddresses(
        ImmutableNetworkConfigurationApi.NetworkConfiguration.builder( )
        .privateIp('10.10.10.10-10.10.10.11')
        .subnet(ImmutableNetworkConfigurationApi.EdgeSubnet.builder( )
            .setValueSubnet('10.10.10.0')
            .setValueNetmask('255.255.255.0')
            .setValueGateway('10.10.10.1')
            .o( ) )
        .cluster(ImmutableNetworkConfigurationApi.Cluster.builder( )
            .setValueName('cluster0')
            .privateIp('10.20.10.10-20.10.10.11')
            .o( ) )
        .o( )
    , 'cluster1' ).left.with{ Iterable<Integer> ips ->
      assertEquals( 'private address list from top level', [ '10.10.10.10', '10.10.10.11' ], Lists.newArrayList( ips.collect( PrivateAddresses.&fromInteger ) ) )
    }

    // verify configuration from cluster level
    NetworkConfigurations.getPrivateAddresses(
        ImmutableNetworkConfigurationApi.NetworkConfiguration.builder( )
            .privateIp('1.1.1.1')
            .subnet(ImmutableNetworkConfigurationApi.EdgeSubnet.builder( )
                .setValueSubnet('10.0.0.0')
                .setValueNetmask('255.0.0.0')
                .setValueGateway('10.0.0.1')
                .o( ) )
            .cluster(ImmutableNetworkConfigurationApi.Cluster.builder( )
                .setValueName('cluster0')
                .privateIp('10.20.10.10-20.10.10.11')
                .o( ) )
            .cluster(ImmutableNetworkConfigurationApi.Cluster.builder( )
                .setValueName('cluster1')
                .privateIp('10.10.10.10-10.10.10.11')
                .o( ) )
            .cluster(ImmutableNetworkConfigurationApi.Cluster.builder( )
                .setValueName('cluster2')
                .privateIp('10.30.10.10-10.30.10.11')
                .o( ) )
            .o( )
        , 'cluster1' ).left.with{ Iterable<Integer> ips ->
      assertEquals( 'private address list from cluster level', [ '10.10.10.10', '10.10.10.11' ], Lists.newArrayList( ips.collect( PrivateAddresses.&fromInteger ) ) )
    }
  }

  @Test
  void testExplodeFull() {
    NetworkConfigurationApi.NetworkConfiguration result = NetworkConfigurations.explode(
        ImmutableNetworkConfigurationApi.NetworkConfiguration.builder( )
            .setValueInstanceDnsDomain('eucalyptus.internal')
            .instanceDnsServer('10.1.1.254')
            .setValueMacPrefix('ab:cd')
            .publicIp('10.111.103.26')
            .publicIp('10.111.103.27')
            .publicIp('10.111.103.28')
            .publicIp('10.111.103.29')
            .setValuePublicGateway('10.111.0.1')
            .privateIp('10.111.103.30')
            .privateIp('10.111.103.36')
            .privateIp('10.111.103.38')
            .privateIp('10.111.103.42')
            .subnet(ImmutableNetworkConfigurationApi.EdgeSubnet.builder( )
                .setValueSubnet('10.111.0.0')
                .setValueNetmask('255.255.0.0')
                .setValueGateway('10.111.0.1')
                .o( ) )
            .o( ),
        [ 'cluster1' ]
    )

    NetworkConfigurationApi.NetworkConfiguration expected = ImmutableNetworkConfigurationApi.NetworkConfiguration.builder( )
        .setValueInstanceDnsDomain('eucalyptus.internal')
        .instanceDnsServer('10.1.1.254')
        .setValueMacPrefix('ab:cd')
        .publicIp('10.111.103.26')
        .publicIp('10.111.103.27')
        .publicIp('10.111.103.28')
        .publicIp('10.111.103.29')
        .setValuePublicGateway('10.111.0.1')
        .privateIp('10.111.103.30')
        .privateIp('10.111.103.36')
        .privateIp('10.111.103.38')
        .privateIp('10.111.103.42')
        .cluster(ImmutableNetworkConfigurationApi.Cluster.builder( )
            .setValueName('cluster1')
            .setValueMacPrefix('ab:cd')
                .setValueSubnet(ImmutableNetworkConfigurationApi.EdgeSubnet.builder( )
                .setValueName('10.111.0.0')
                .setValueSubnet('10.111.0.0')
                .setValueNetmask('255.255.0.0')
                .setValueGateway('10.111.0.1')
                .o( ) )
            .privateIp('10.111.103.30')
            .privateIp('10.111.103.36')
            .privateIp('10.111.103.38')
            .privateIp('10.111.103.42')
            .o( ) )
        .o( )

    assertEquals( 'Result does not match template', expected, result )
  }

  @Test
  void testExplodeMinimal() {
    NetworkConfigurationApi.NetworkConfiguration result = NetworkConfigurations.explode(
        ImmutableNetworkConfigurationApi.NetworkConfiguration.builder( )
            .publicIp('10.111.103.26')
            .publicIp('10.111.103.27')
            .publicIp('10.111.103.28')
            .publicIp('10.111.103.29')
            .subnet(ImmutableNetworkConfigurationApi.EdgeSubnet.builder( )
                .setValueSubnet('10.111.0.0')
                .setValueNetmask('255.255.0.0')
                .setValueGateway('10.111.0.1')
                .o( ) )
            .o( ),
        [ 'cluster1' ]
    )

    NetworkConfigurationApi.NetworkConfiguration expected = ImmutableNetworkConfigurationApi.NetworkConfiguration.builder( )
        .publicIp('10.111.103.26')
        .publicIp('10.111.103.27')
        .publicIp('10.111.103.28')
        .publicIp('10.111.103.29')
        .cluster(ImmutableNetworkConfigurationApi.Cluster.builder( )
            .setValueName('cluster1')
            .setValueMacPrefix('d0:0d')
            .setValueSubnet(ImmutableNetworkConfigurationApi.EdgeSubnet.builder( )
                .setValueName('10.111.0.0')
                .setValueSubnet('10.111.0.0')
                .setValueNetmask('255.255.0.0')
                .setValueGateway('10.111.0.1')
                .o( ) )
            .privateIp('10.111.0.2-10.111.255.254')
            .o( ) )
        .o( )

    assertEquals( 'Result does not match template', expected, result )
  }

  @Test( expected = NetworkConfigurationException )
  void testValidateSingleSubnet() {
    NetworkConfigurations.validate(
        ImmutableNetworkConfigurationApi.NetworkConfiguration.builder( )
            .publicIp(  '10.111.103.26' )
            .o( ) )
  }

  @Test( expected = NetworkConfigurationException )
  void testValidateSingleSubnetIPRanges() {
    NetworkConfigurations.validate(
        ImmutableNetworkConfigurationApi.NetworkConfiguration.builder( )
            .publicIp('10.111.103.26')
            .privateIp('10.1.1.1')
            .subnet(ImmutableNetworkConfigurationApi.EdgeSubnet.builder( )
                .setValueSubnet('10.111.0.0')
                .setValueNetmask('255.255.0.0')
                .setValueGateway('10.111.0.1')
                .o( ) )
            .o( ) )
  }

  @Test( expected = NetworkConfigurationException )
  void testValidateClusterSubnetIPRanges() {
    NetworkConfigurations.validate(
        ImmutableNetworkConfigurationApi.NetworkConfiguration.builder( )
            .publicIp('10.111.103.26')
            .privateIp('10.1.1.1')
            .cluster(ImmutableNetworkConfigurationApi.Cluster.builder( )
                .setValueName('cluster1')
                .setValueSubnet(ImmutableNetworkConfigurationApi.EdgeSubnet.builder( )
                    .setValueName('10.111.0.0')
                    .setValueSubnet('10.111.0.0')
                    .setValueNetmask('255.255.0.0')
                    .setValueGateway('10.111.0.1')
                    .o( ) )
                .privateIp('10.1.1.1')
                .o( ) )
            .o( ) )
  }
}
