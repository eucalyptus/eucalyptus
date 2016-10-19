/*************************************************************************
 * Copyright 2009-2016 Eucalyptus Systems, Inc.
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

import static org.junit.Assert.*;

import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import com.eucalyptus.util.metrics.MonitoredAction;
import com.eucalyptus.util.metrics.ThruputMetrics;

public class PartialMetricsTest {

  @Test
  public void testStartStop() throws Exception {
    long end = System.currentTimeMillis();
    long start = end - 1000;
    ThruputMetrics.startOperation(MonitoredAction.CREATE_VOLUME, "vol-123456", start).get(3, TimeUnit.SECONDS);
    ThruputMetrics.endOperation(MonitoredAction.CREATE_VOLUME, "vol-123456", end).get(3, TimeUnit.SECONDS);
    ThruputMetrics.DataPoint[] res = ThruputMetrics.getDataPoints(MonitoredAction.CREATE_VOLUME);
    assertEquals(1, res.length);
    assertEquals(1000, res[0].value);
  }

  @Test
  public void testNoStart() throws Exception {
    ThruputMetrics.endOperation(MonitoredAction.CREATE_SNAPSHOT, "vol-123450", System.currentTimeMillis()).get(3, TimeUnit.SECONDS);
    ThruputMetrics.DataPoint[] res = ThruputMetrics.getDataPoints(MonitoredAction.CREATE_SNAPSHOT);
    assertEquals(0, res.length);
  }

}
