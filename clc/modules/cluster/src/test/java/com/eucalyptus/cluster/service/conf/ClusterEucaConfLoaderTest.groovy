/*************************************************************************
 * (c) Copyright 2017 Hewlett Packard Enterprise Development Company LP
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
 ************************************************************************/
package com.eucalyptus.cluster.service.conf

import org.hamcrest.Matchers
import org.junit.Test

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertThat

/**
 *
 */
class ClusterEucaConfLoaderTest {

  @Test
  void testBasicLoad( ) {
    ClusterEucaConf configuration = new ClusterEucaConfLoader( { [
        EUCALYPTUS: "/",
        LOGLEVEL: "INFO",
        EUCA_USER: "eucalyptus",
        CLOUD_OPTS: "--debug -Dcom.eucalyptus.cluster.service.enable=true",
        CC_PORT: "8774",
        SCHEDPOLICY: '"ROUNDROBIN"',
        NODES: '"10.111.5.210 10.111.5.211"',
        NC_PORT: '"8775"',
        HYPERVISOR: "kvm",
        MAX_CORES: "32",
        INSTANCE_PATH: "/var/lib/eucalyptus/instances",
        USE_VIRTIO_ROOT: "1",
        USE_VIRTIO_DISK: "1",
        USE_VIRTIO_NET: "1",
        VNET_MODE: "EDGE",
        VNET_PRIVINTERFACE: "br0",
        VNET_PUBINTERFACE: "br0",
        VNET_BRIDGE: "br0",
        VNET_DHCPDAEMON: "/usr/sbin/dhcpd",
        METADATA_USE_VM_PRIVATE: "N",
        DISABLE_TUNNELING: "Y",
        MAX_INSTANCES_PER_CC: "128"
    ] } ).load( System.currentTimeMillis( ) )
    assertEquals( 'Scheduler', 'ROUNDROBIN', configuration.scheduler )
    assertThat( 'Nodes', configuration.nodes, Matchers.contains( '10.111.5.210', '10.111.5.211') )
    assertEquals( 'Port', 8775, configuration.nodePort )
    assertEquals( 'Max instances', 128, configuration.maxInstances )
  }

  @Test
  void testDefaultsLoad( ) {
    ClusterEucaConf configuration = new ClusterEucaConfLoader( { [:] } ).load( System.currentTimeMillis( ) )
    assertEquals( 'Scheduler', 'ROUNDROBIN', configuration.scheduler )
    assertThat( 'Nodes', configuration.nodes, Matchers.empty( ) )
    assertEquals( 'Port', 8775, configuration.nodePort )
    assertEquals( 'Max instances', 10_000, configuration.maxInstances )
  }
}
