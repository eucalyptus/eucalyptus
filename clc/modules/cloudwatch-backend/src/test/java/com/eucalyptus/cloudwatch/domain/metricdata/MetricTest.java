package com.eucalyptus.cloudwatch.domain.metricdata; /*************************************************************************
 * Copyright 2009-2013 Ent. Services Development Corporation LP
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

import com.eucalyptus.cloudwatch.common.internal.domain.metricdata.MetricEntity;
import com.eucalyptus.cloudwatch.common.internal.domain.metricdata.MetricManager;
import com.eucalyptus.cloudwatch.common.internal.domain.metricdata.MetricEntity.MetricType;
import com.eucalyptus.cloudwatch.common.internal.domain.metricdata.Units;
import org.apache.log4j.Logger;
import org.junit.Ignore;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

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
