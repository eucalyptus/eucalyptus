/*************************************************************************
 * Copyright 2009-2013 Eucalyptus Systems, Inc.
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
package com.eucalyptus.cloudwatch.domain.metricdata;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.junit.Ignore;

import com.eucalyptus.cloudwatch.domain.metricdata.MetricEntity.MetricType;
import com.eucalyptus.cloudwatch.domain.metricdata.MetricEntity.Units;

@Ignore("Manual development test")
public class MetricTest {
  private static final Logger LOG = Logger.getLogger(MetricTest.class);

  public static void addStuff() {
    Map<String, String> dimensionMap1 = new HashMap<String, String>();
    dimensionMap1.put("dim1", "dim1");
    Map<String, String> dimensionMap2 = new HashMap<String, String>();
    dimensionMap2.put("dim1", "dim1");
    dimensionMap2.put("dim2", "dim2");
    Map<String, String> dimensionMap3 = new HashMap<String, String>();
    dimensionMap3.put("dim1", "dim1");
    dimensionMap3.put("dim2", "dim2");
    dimensionMap3.put("dim3", "dim3");
    dimensionMap3.put("dim4", "dim4");
    dimensionMap3.put("dim5", "dim5");
    Map<String, String> dimensionMap4 = new HashMap<String, String>();
    dimensionMap4.put("dim1", "dim1");
    dimensionMap4.put("dim2", "dim2");
    dimensionMap4.put("dim3", "dim3");
    dimensionMap4.put("dim4", "dim4");
    dimensionMap4.put("dim5", "dim5");
    dimensionMap4.put("dim6", "dim6");
    dimensionMap4.put("dim7", "dim7");
    dimensionMap4.put("dim8", "dim8");
    dimensionMap4.put("dim9", "dim9");
    dimensionMap4.put("dim10", "dim10");
    MetricManager.addMetric("account1", "metric1", "name1", null,
        MetricType.Custom, Units.None, new Date(), 1.0, 1.0, 1.0, 1.0);
    MetricManager.addMetric("account1", "metric1", "name1",
        dimensionMap1, MetricType.System, Units.BitsPerSecond,
        new Date(System.currentTimeMillis() - 10000000), 1.0, 2.0, 2.0, 2.0);
    MetricManager.addMetric("account1", "metric1", "name1",
        dimensionMap2, MetricType.System, Units.Count,
        new Date(System.currentTimeMillis() - 20000000), 1.0, 3.0, 3.0, 3.0);
    MetricManager.addMetric("account1", "metric1", "name1",
        dimensionMap3, MetricType.System, Units.Count,
        new Date(System.currentTimeMillis() - 20000000), 1.0, 3.0, 3.0, 3.0);
    MetricManager.addMetric("account1", "metric1", "name1",
        dimensionMap4, MetricType.System, Units.Count,
        new Date(System.currentTimeMillis() - 20000000), 1.0, 3.0, 3.0, 3.0);
    for (MetricEntity me : MetricManager.getAllMetrics()) {
      LOG.fatal("Metric:" + me);
    }
    MetricManager.deleteAllMetrics();
  }
}
