/*************************************************************************
 * Copyright 2009-2014 Ent. Services Development Corporation LP
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
