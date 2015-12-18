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
package com.eucalyptus.cloudwatch;

import com.eucalyptus.cloudwatch.common.QueueThruput.Aggregates;
import com.eucalyptus.cloudwatch.common.config.CloudWatchConfigProperties;
import com.eucalyptus.cloudwatch.common.QueueThruput

import static org.junit.Assert.*

import org.junit.Test

class QueueThruputTest {

  @Test
  public void testSize() {
    for (int i=0; i < CloudWatchConfigProperties.CLOUDWATCH_MONITORING_HISTORY_SIZE * 3;i++)
      QueueThruput.addDataPoint(QueueThruput.MonitoredAction.CLUSTER_TIMING, i);
    assertEquals(0, QueueThruput.getDataPoints(QueueThruput.MonitoredAction.CLUSTER_SIZE).size());
    assertEquals(CloudWatchConfigProperties.CLOUDWATCH_MONITORING_HISTORY_SIZE * 3, 
      QueueThruput.getDataPoints(QueueThruput.MonitoredAction.CLUSTER_TIMING).size(), 
      );
  }

  @Test
  public void testAgg() {
    QueueThruput.addDataPoint(QueueThruput.MonitoredAction.PUT_DATA_QUEUE_SIZE, 1);
    QueueThruput.addDataPoint(QueueThruput.MonitoredAction.PUT_DATA_QUEUE_SIZE, 4);
    QueueThruput.addDataPoint(QueueThruput.MonitoredAction.PUT_DATA_QUEUE_SIZE, 5);
    QueueThruput.addDataPoint(QueueThruput.MonitoredAction.PUT_DATA_QUEUE_SIZE, 18);
    QueueThruput.addDataPoint(QueueThruput.MonitoredAction.PUT_DATA_QUEUE_SIZE, 25);
    QueueThruput.Aggregates res = QueueThruput.getAggregates(QueueThruput.MonitoredAction.PUT_DATA_QUEUE_SIZE);
    assertEquals(1, res.min);
    assertEquals(25, res.max);
    assertEquals(5, res.count);
    assertEquals(10.6, res.mean, 0.01);
    assertEquals(5, res.median, 0.01);
    QueueThruput.addDataPoint(QueueThruput.MonitoredAction.PUT_DATA_QUEUE_SIZE, 28);
    res = QueueThruput.getAggregates(QueueThruput.MonitoredAction.PUT_DATA_QUEUE_SIZE);
    assertEquals(1, res.min);
    assertEquals(28, res.max);
    assertEquals(6, res.count);
    assertEquals(13.5, res.mean, 0.01);
    assertEquals(11.5, res.median, 0.01);
    res = QueueThruput.getAggregates(QueueThruput.MonitoredAction.LIST_METRIC_TIMING);
    assertEquals(0, res.min);
    assertEquals(0, res.max);
    assertEquals(0, res.count);
    assertEquals(0, res.mean, 0.01);
    assertEquals(0, res.median, 0.01);
    QueueThruput.addDataPoint(QueueThruput.MonitoredAction.LIST_METRIC_TIMING, Long.MAX_VALUE);
    QueueThruput.addDataPoint(QueueThruput.MonitoredAction.LIST_METRIC_TIMING, 5);
    res = QueueThruput.getAggregates(QueueThruput.MonitoredAction.LIST_METRIC_TIMING);
    assertEquals(5, res.min);
    assertEquals(Long.MAX_VALUE, res.max);
    assertEquals(Double.NaN, res.mean, 0.01);
  }
  
  @Test(expected=IllegalArgumentException.class)
  public void testNegative() {
    QueueThruput.addDataPoint(QueueThruput.MonitoredAction.CLUSTER_TIMING, -1);
  }
}
