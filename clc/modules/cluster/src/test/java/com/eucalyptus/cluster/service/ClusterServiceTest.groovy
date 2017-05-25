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
package com.eucalyptus.cluster.service

import com.eucalyptus.cluster.common.msgs.DescribeResourcesResponseType
import com.eucalyptus.cluster.common.msgs.DescribeResourcesType
import com.eucalyptus.cluster.common.msgs.DescribeSensorsResponseType
import com.eucalyptus.cluster.common.msgs.DescribeSensorsType
import com.eucalyptus.cluster.common.msgs.MetricCounterType
import com.eucalyptus.cluster.common.msgs.MetricDimensionsType
import com.eucalyptus.cluster.common.msgs.MetricsResourceType
import com.eucalyptus.cluster.common.msgs.SensorsResourceType
import com.eucalyptus.cluster.common.msgs.VmDescribeType
import com.eucalyptus.cluster.common.msgs.VmKeyInfo
import com.eucalyptus.cluster.common.msgs.VmRunType
import com.eucalyptus.cluster.service.conf.ClusterEucaConfLoader
import com.eucalyptus.cluster.service.fake.FakeClusterNodeServiceFactory
import com.eucalyptus.cluster.service.node.ClusterNodes
import com.eucalyptus.compute.common.internal.network.NetworkGroup
import com.eucalyptus.cluster.common.msgs.VmTypeInfo
import groovy.transform.CompileStatic
import org.junit.Test

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertNotNull

/**
 *
 */
@CompileStatic
class ClusterServiceTest {

  @Test
  void testDescribeResources( ) {
    final ClusterEucaConfLoader loader = new ClusterEucaConfLoader( { [
        NODES: '"10.20.40.1 10.20.40.2 10.20.40.3 10.20.40.4 10.20.40.5"'
    ] } )
    final ClusterNodes nodes = new ClusterNodes(
        loader,
        new FakeClusterNodeServiceFactory( false )
    )
    nodes.refreshResources( );
    final ClusterService service = new ClusterServiceImpl( loader, nodes )
    service.describeResources( new DescribeResourcesType(
        instanceTypes: [
            new VmTypeInfo(
                name: 'e1.example',
                memory: 1000,
                disk: 10,
                cores: 100,
                rootDeviceName: 'sda1'
            )
        ] as ArrayList<VmTypeInfo>
    ) ).with { DescribeResourcesResponseType describeResourcesResponse ->
      assertEquals( 'node count', 5, describeResourcesResponse.nodes.size( ) )
      assertEquals( 'resource count', 1, describeResourcesResponse.resources.size( ) )
      describeResourcesResponse.resources.getAt( 0 ).with {
        assertEquals( 'max instances', 5000, maxInstances )
        assertEquals( 'available instances', 5000, availableInstances )
        assertNotNull( 'resource vmtype', instanceType )
        assertEquals( 'instance type name', 'e1.example', instanceType.name )
      }
    }
  }

  @Test
  void testDescribeSensors( ) {
    final ClusterEucaConfLoader loader = new ClusterEucaConfLoader( { [
        NODES: '"10.20.40.1 10.20.40.2 10.20.40.3 10.20.40.4 10.20.40.5"'
    ] } )
    final ClusterNodes nodes = new ClusterNodes(
        loader,
        new FakeClusterNodeServiceFactory( false )
    )
    nodes.refreshResources( );
    final ClusterService service = new ClusterServiceImpl( loader, nodes )
    service.runVm( (VmRunType) VmRunType.builder( )
        .reservationId( 'r-00000001' )
        .platform( 'linux' )
        .launchIndex( 1 )
        .instanceId( 'i-00000001' )
//        ownerId: 'eucalyptus',
//        accountId: '111111111111',
//        .owner( )
        .naturalId( UUID.randomUUID( ).toString( ) )
        .macAddress( 'DO:OD:00:00:00:01' )
        .privateAddress( '10.10.10.1' )
        .networkNames( [ new NetworkGroup( groupId: 'sg-00000001', displayName: 'group-1' ) ] )
        .networkIds([ new NetworkGroup( groupId: 'sg-00000001', displayName: 'group-1'  ) ] )
        .keyInfo( new VmKeyInfo(
          value: 'ssh-rsa AAAAB3NzaC1yc2EAAAABIwAAAQEA4dW1AXL6L7fA3HiRH8YfSfRLEFZSYfJLFdKI9zUtTPWvCiRPY2NsLOcRbVTYkMq10CRN2ALkAviEildGIO2tEyygynYVl5zq3ACi+yv9L1aDTc+4KUV1ob6DbGu6ZV02t3Pf0d/sJ8uuYsHt4gcHpm7mVlZIgSLXCBqtJyLmfxzc9ZnJHAmZITTX4cE8XzmdLO+i0Iu8JKTeNgtV1Fr4fPA5gtI4SzAmtvwQaErTJ0T7WoKj8OOu4cYjvbo7O1Qnjk63XPO9aJHfBq2AeQX6FrGXBGTxvKGpq7h6lL/XAJyn8/8YnW3hlGBfb1hkWBfZC2NYo3fQ1zCYcQBWnfgghw== mailman@QA-SERVER-6755'
        ) )
        .vmTypeInfo( new VmTypeInfo(
            name: 'm1.small',
            memory: 512,
            disk: 5,
            cores: 1
        ) )
        .create( ) )

    service.describeSensors( new DescribeSensorsType(
        historySize: 5,
        collectionIntervalTimeMs: 150000,
        instanceIds: [ 'i-00000001' ] as ArrayList<String>
    ) ).with { DescribeSensorsResponseType response ->
      assertEquals( 'sensor resource count', 1, response.sensorsResources.size( ) )
      response.sensorsResources.each { SensorsResourceType sensorsResource ->
        assertNotNull( 'resourceName', sensorsResource.resourceName )
        assertNotNull( 'resourceType', sensorsResource.resourceType )
        assertNotNull( 'resourceUuid', sensorsResource.resourceUuid )
        assertEquals( 'metric count', 14, sensorsResource.metrics.size( ) )
        sensorsResource.metrics.each { MetricsResourceType metricsResource ->
          assertNotNull( 'metricName', metricsResource.metricName )
          metricsResource.counters.each { MetricCounterType metricCounter ->
            assertNotNull( 'type', metricCounter.type )
            assertNotNull( 'collectionIntervalMs', metricCounter.collectionIntervalMs )
            metricCounter.dimensions.each { MetricDimensionsType metricDimensions ->
              assertNotNull( 'dimensionName', metricDimensions.dimensionName )
              assertNotNull( 'sequenceNum', metricDimensions.sequenceNum )
              assertEquals( 'values', 0, metricDimensions.values.size( ) )
            }
          }
        }
      }
    }
  }

  @Test
  void testDescribeVms( ) {
    final ClusterEucaConfLoader loader = new ClusterEucaConfLoader( { [
        NODES: '"10.20.40.1 10.20.40.2 10.20.40.3 10.20.40.4 10.20.40.5"'
    ] } )
    final ClusterNodes nodes = new ClusterNodes(
        loader,
        new FakeClusterNodeServiceFactory( false )
    )
    nodes.refreshResources( );
    final ClusterService service = new ClusterServiceImpl( loader, nodes )
    final String instanceUuid = UUID.randomUUID( ).toString( )
    service.runVm( (VmRunType) VmRunType.builder( )
        .reservationId( 'r-00000001' )
        .platform( 'linux' )
        .launchIndex( 1 )
        .instanceId( 'i-00000001' )
//        ownerId: 'eucalyptus',
//        accountId: '111111111111',
//        .owner( )
        .naturalId( instanceUuid )
        .macAddress( 'DO:OD:00:00:00:01' )
        .privateAddress( '10.10.10.1' )
        .networkNames( [ new NetworkGroup( groupId: 'sg-00000001', displayName: 'group-1' ) ] )
        .networkIds([ new NetworkGroup( groupId: 'sg-00000001', displayName: 'group-1'  ) ] )
        .keyInfo( new VmKeyInfo(
          value: 'ssh-rsa AAAAB3NzaC1yc2EAAAABIwAAAQEA4dW1AXL6L7fA3HiRH8YfSfRLEFZSYfJLFdKI9zUtTPWvCiRPY2NsLOcRbVTYkMq10CRN2ALkAviEildGIO2tEyygynYVl5zq3ACi+yv9L1aDTc+4KUV1ob6DbGu6ZV02t3Pf0d/sJ8uuYsHt4gcHpm7mVlZIgSLXCBqtJyLmfxzc9ZnJHAmZITTX4cE8XzmdLO+i0Iu8JKTeNgtV1Fr4fPA5gtI4SzAmtvwQaErTJ0T7WoKj8OOu4cYjvbo7O1Qnjk63XPO9aJHfBq2AeQX6FrGXBGTxvKGpq7h6lL/XAJyn8/8YnW3hlGBfb1hkWBfZC2NYo3fQ1zCYcQBWnfgghw== mailman@QA-SERVER-6755'
        ) )
        .vmTypeInfo( new VmTypeInfo(
        name: 'm1.small',
        memory: 512,
        disk: 5,
        cores: 1
    ) ).create( ) )

    service.describeVms( new VmDescribeType( ) ).with {
      assertEquals( 'vm count', 1, vms.size( ) )
      vms.getAt( 0 ).with {
        assertEquals( 'id', 'i-00000001', instanceId )
        assertEquals( 'uuid', instanceUuid, uuid )
        assertEquals( 'reservation id', 'r-00000001', reservationId )
        assertEquals( 'image id', 'r-00000001', reservationId )
      }
    }
  }

}
