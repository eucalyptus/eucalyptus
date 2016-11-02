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
 * Please contact Eucalyptus Systems, Inc., 6750 Navigator Way, Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 ************************************************************************/
package com.eucalyptus.util.metrics;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import com.eucalyptus.util.metrics.MetricsConfiguration;
import com.eucalyptus.util.metrics.ThruputMetrics;
import com.eucalyptus.util.metrics.ThruputMetrics.Aggregates;
import com.eucalyptus.util.metrics.ThruputMetrics.DataPoint;
import com.eucalyptus.util.metrics.MonitoredAction

import static org.junit.Assert.*

import org.junit.Test

class QueueThruputTest {

  @Test
  public void testSize() {
    for (int i=0; i < MetricsConfiguration.METRICS_COLLECTION_SIZE * 3;i++)
      ThruputMetrics.addDataPoint(MonitoredAction.CLUSTER_TIMING, i).get(3, TimeUnit.SECONDS);
    assertEquals(0, ThruputMetrics.getDataPoints(MonitoredAction.CLUSTER_SIZE).size());
    assertEquals(MetricsConfiguration.METRICS_COLLECTION_SIZE,
      ThruputMetrics.getDataPoints(MonitoredAction.CLUSTER_TIMING).size(), 
      );
  }

  @Test
  public void testAgg() {
    ThruputMetrics.addDataPoint(MonitoredAction.PUT_DATA_QUEUE_SIZE, 18).get(3, TimeUnit.SECONDS);
    ThruputMetrics.addDataPoint(MonitoredAction.PUT_DATA_QUEUE_SIZE, 25).get(3, TimeUnit.SECONDS);
    ThruputMetrics.addDataPoint(MonitoredAction.PUT_DATA_QUEUE_SIZE, 1).get(3, TimeUnit.SECONDS);
    ThruputMetrics.addDataPoint(MonitoredAction.PUT_DATA_QUEUE_SIZE, 4).get(3, TimeUnit.SECONDS);
    ThruputMetrics.addDataPoint(MonitoredAction.PUT_DATA_QUEUE_SIZE, 5).get(3, TimeUnit.SECONDS);
    ThruputMetrics.Aggregates res = ThruputMetrics.getAggregates(MonitoredAction.PUT_DATA_QUEUE_SIZE);
    assertEquals(1, res.min);
    assertEquals(25, res.max);
    assertEquals(5, res.count);
    assertEquals(10.6, res.mean, 0.01);
    assertEquals(5, res.median, 0.01);
    ThruputMetrics.addDataPoint(MonitoredAction.PUT_DATA_QUEUE_SIZE, 28).get(3, TimeUnit.SECONDS);
    res = ThruputMetrics.getAggregates(MonitoredAction.PUT_DATA_QUEUE_SIZE);
    // set is { 1, 4, 5, 18, 25, 28 }
    assertEquals(1, res.min);
    assertEquals(28, res.max);
    assertEquals(6, res.count);
    assertEquals(13.5, res.mean, 0.01);
    assertEquals(11.5, res.median, 0.01);
    assertEquals(4, res.firstQuartile, 0.01);
    assertEquals(25, res.thirdQuartile, 0.01);
    res = ThruputMetrics.getAggregates(MonitoredAction.LIST_METRIC_TIMING);
    assertEquals(0, res.min);
    assertEquals(0, res.max);
    assertEquals(0, res.count);
    assertEquals(0, res.mean, 0.01);
    assertEquals(0, res.median, 0.01);
    assertEquals(0, res.firstQuartile, 0.01);
    assertEquals(0, res.thirdQuartile, 0.01);
    ThruputMetrics.addDataPoint(MonitoredAction.LIST_METRIC_TIMING, 5).get(3, TimeUnit.SECONDS);
    res = ThruputMetrics.getAggregates(MonitoredAction.LIST_METRIC_TIMING);
    assertEquals(5, res.min);
    assertEquals(5, res.max);
    assertEquals(5, res.mean, 0.01);
    assertEquals(5, res.firstQuartile, 0.01);
    assertEquals(5, res.thirdQuartile, 0.01);
    ThruputMetrics.addDataPoint(MonitoredAction.LIST_METRIC_TIMING, Long.MAX_VALUE).get(3, TimeUnit.SECONDS);
    ThruputMetrics.addDataPoint(MonitoredAction.LIST_METRIC_TIMING, 6).get(3, TimeUnit.SECONDS);
    ThruputMetrics.addDataPoint(MonitoredAction.LIST_METRIC_TIMING, 1).get(3, TimeUnit.SECONDS);
    res = ThruputMetrics.getAggregates(MonitoredAction.LIST_METRIC_TIMING);
    // set is { 1, 5, 6, Long.MAX_VALUE }
    assertEquals(1, res.min);
    assertEquals(Long.MAX_VALUE, res.max);
    assertEquals(Double.NaN, res.mean, 0.01);
    assertEquals(1, res.firstQuartile, 0.01);
    assertEquals(6, res.thirdQuartile, 0.01);
  }
  
  @Test
  public void testNegative() {
    for(int i=-1; i>-50; i--)
      ThruputMetrics.addDataPoint(MonitoredAction.EXPORT_VOLUME, i).get(3, TimeUnit.SECONDS);
    DataPoint[] res = ThruputMetrics.getDataPoints(MonitoredAction.EXPORT_VOLUME);
    assertEquals(0, res.length);
  }

  @Test
  public void testResize() {
    for(int i=0; i<100; i++)
      ThruputMetrics.addDataPoint(MonitoredAction.UNEXPORT_VOLUME, i).get(3, TimeUnit.SECONDS);
    assertEquals(100, ThruputMetrics.getDataPoints(MonitoredAction.UNEXPORT_VOLUME).length);
    ThruputMetrics.changeSize(20);
    assertEquals(20, ThruputMetrics.getDataPoints(MonitoredAction.UNEXPORT_VOLUME).length);
    ThruputMetrics.changeSize(50);
    DataPoint[] res = ThruputMetrics.getDataPoints(MonitoredAction.UNEXPORT_VOLUME);
    assertEquals(20, res.length);
    assertEquals(99, res[19].value);
    for(int i=0; i<50; i++)
      ThruputMetrics.addDataPoint(MonitoredAction.UNEXPORT_VOLUME, 100 + i).get(3, TimeUnit.SECONDS);
    res = ThruputMetrics.getDataPoints(MonitoredAction.UNEXPORT_VOLUME);
    assertEquals(50, res.length);
    // last one should be 100 + 49
    assertEquals(149, res[49].value);
  }
}

