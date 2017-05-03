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

import com.eucalyptus.cluster.common.msgs.DescribeSensorsResponseType
import com.eucalyptus.cluster.common.msgs.DescribeSensorsType
import com.eucalyptus.cluster.common.msgs.MetricCounterType
import com.eucalyptus.cluster.common.msgs.MetricDimensionsType
import com.eucalyptus.cluster.common.msgs.MetricDimensionsValuesType
import com.eucalyptus.cluster.common.msgs.MetricsResourceType
import com.eucalyptus.cluster.common.msgs.SensorsResourceType
import com.eucalyptus.cluster.common.msgs.VmKeyInfo
import com.eucalyptus.cluster.common.msgs.VmRunType
import com.eucalyptus.cluster.service.fake.FakeClusterService
import com.eucalyptus.compute.common.internal.network.NetworkGroup
import edu.ucsb.eucalyptus.msgs.BaseMessage
import com.eucalyptus.cluster.common.msgs.VmTypeInfo
import groovy.transform.CompileStatic
import org.junit.Test

import java.util.concurrent.TimeUnit

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertNotNull

/**
 *
 */
@CompileStatic
class ClusterServiceTest {

  static class TestFakeVmRunType extends BaseMessage {
    String       reservationId;
    String       platform
    Integer      launchIndex
    String       instanceId
    String       ownerId
    String       accountId
    String       uuid
    String       macAddress
    List<String> networkNames
    List<String> networkIds
    String       privateAddress
    VmTypeInfo   vmTypeInfo
    VmKeyInfo    keyInfo
  }

  @Test
  void testDescribeSensors( ) {
    long currentTime = 0;
    final FakeClusterService service = new FakeClusterService( )
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

    // verify no metrics yet
    currentTime = TimeUnit.MINUTES.toMillis( 2 )
    service.describeSensors( new DescribeSensorsType(
        historySize: 5,
        collectionIntervalTimeMs: 150000,
        instanceIds: [ 'i-00000001' ] as ArrayList<String>
    ) ).with { DescribeSensorsResponseType response ->
      response.sensorsResources.each { SensorsResourceType sensorsResource ->
        assertNotNull( 'resourceName', sensorsResource.resourceName )
        assertNotNull( 'resourceType', sensorsResource.resourceType )
        assertNotNull( 'resourceUuid', sensorsResource.resourceUuid )
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

    // verify one metric
    currentTime = TimeUnit.MINUTES.toMillis( 3 )
    service.describeSensors( new DescribeSensorsType(
        historySize: 5,
        collectionIntervalTimeMs: 150000,
        instanceIds: [ 'i-00000001' ] as ArrayList<String>
    ) ).with { DescribeSensorsResponseType response ->
      response.sensorsResources.each { SensorsResourceType sensorsResource ->
        assertNotNull( 'resourceName', sensorsResource.resourceName )
        assertNotNull( 'resourceType', sensorsResource.resourceType )
        assertNotNull( 'resourceUuid', sensorsResource.resourceUuid )
        sensorsResource.metrics.each { MetricsResourceType metricsResource ->
          assertNotNull( 'metricName', metricsResource.metricName )
          metricsResource.counters.each { MetricCounterType metricCounter ->
            assertNotNull( 'type', metricCounter.type )
            assertNotNull( 'collectionIntervalMs', metricCounter.collectionIntervalMs )
            metricCounter.dimensions.each { MetricDimensionsType metricDimensions ->
              assertNotNull( 'dimensionName', metricDimensions.dimensionName )
              assertNotNull( 'sequenceNum', metricDimensions.sequenceNum )
              assertEquals( 'values', 0, metricDimensions.values.size( ) )
              metricDimensions.values.each { MetricDimensionsValuesType metricDimensionsValues ->
                assertNotNull( 'timestamp', metricDimensionsValues.timestamp )
                assertNotNull( 'value', metricDimensionsValues.value )
              }
            }
          }
        }
      }
    }


    // verify full history of metrics
    currentTime = TimeUnit.HOURS.toMillis( 24 )
    service.describeSensors( new DescribeSensorsType(
        historySize: 5,
        collectionIntervalTimeMs: 150000,
        instanceIds: [ 'i-00000001' ] as ArrayList<String>
    ) ).with { DescribeSensorsResponseType response ->
      response.sensorsResources.each { SensorsResourceType sensorsResource ->
        assertNotNull( 'resourceName', sensorsResource.resourceName )
        assertNotNull( 'resourceType', sensorsResource.resourceType )
        assertNotNull( 'resourceUuid', sensorsResource.resourceUuid )
        sensorsResource.metrics.each { MetricsResourceType metricsResource ->
          assertNotNull( 'metricName', metricsResource.metricName )
          metricsResource.counters.each { MetricCounterType metricCounter ->
            assertNotNull( 'type', metricCounter.type )
            assertNotNull( 'collectionIntervalMs', metricCounter.collectionIntervalMs )
            metricCounter.dimensions.each { MetricDimensionsType metricDimensions ->
              assertNotNull( 'dimensionName', metricDimensions.dimensionName )
              assertNotNull( 'sequenceNum', metricDimensions.sequenceNum )
              assertEquals( 'values', 0, metricDimensions.values.size( ) )
              metricDimensions.values.each { MetricDimensionsValuesType metricDimensionsValues ->
                assertNotNull( 'timestamp', metricDimensionsValues.timestamp )
                assertNotNull( 'value', metricDimensionsValues.value )
              }
            }
          }
        }
      }
    }
  }
}
