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
package com.eucalyptus.cloudwatch.service.queue.metricdata;

import com.eucalyptus.cloudwatch.common.internal.domain.listmetrics.ListMetricManager;
import com.eucalyptus.cloudwatch.common.internal.domain.metricdata.MetricEntity.MetricType;
import com.eucalyptus.cloudwatch.common.internal.domain.metricdata.MetricManager;
import com.eucalyptus.cloudwatch.common.internal.domain.metricdata.MetricUtils;
import com.eucalyptus.cloudwatch.common.internal.domain.metricdata.SimpleMetricEntity;
import com.eucalyptus.cloudwatch.common.internal.domain.metricdata.Units;
import com.eucalyptus.cloudwatch.common.msgs.Dimension;
import com.eucalyptus.cloudwatch.common.msgs.MetricDatum;
import com.eucalyptus.cloudwatch.service.queue.listmetrics.ListMetricQueue;
import com.eucalyptus.system.Threads;
import com.eucalyptus.util.metrics.MonitoredAction;
import com.eucalyptus.util.metrics.ThruputMetrics;
import com.google.common.base.MoreObjects;
import com.google.common.base.Supplier;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.primitives.Longs;

import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MetricDataQueue {
  private static final Logger LOG = Logger.getLogger(MetricDataQueue.class);
  final static LinkedBlockingQueue<MetricQueueItem> dataQueue = new LinkedBlockingQueue<MetricQueueItem>();

  private static final ScheduledExecutorService dataFlushTimer = Executors
      .newSingleThreadScheduledExecutor( Threads.threadFactory( "cloudwatch-metric-data-flush-%d" ) );

  private static MetricDataQueue singleton = getInstance();

  public static MetricDataQueue getInstance() {
    synchronized (MetricDataQueue.class) {
      if (singleton == null)
        singleton = new MetricDataQueue();
    }
    return singleton;
  }

  private void queue(Supplier<MetricQueueItem> metriMetaDataSupplier) {
    final MetricQueueItem metricData = metriMetaDataSupplier.get();
    dataQueue.offer(metricData);
  }

  private static Runnable safeRunner = new Runnable() {
    @Override
    public void run() {
      long before = System.currentTimeMillis();
      try {
        List<MetricQueueItem> dataBatch = Lists.newArrayList();
        dataQueue.drainTo(dataBatch);
        ThruputMetrics.addDataPoint(MonitoredAction.PUT_DATA_QUEUE_SIZE, dataBatch.size( ));
        long t1 = System.currentTimeMillis();
        List<SimpleMetricEntity> simpleDataBatch = convertToSimpleDataBatch(dataBatch);
        long t2 = System.currentTimeMillis();
        ThruputMetrics.addDataPoint(MonitoredAction.PUT_DATA_QUEUE_CONVERT, t2-t1);
        simpleDataBatch = aggregate(simpleDataBatch);
        long t3 = System.currentTimeMillis();
        ThruputMetrics.addDataPoint(MonitoredAction.PUT_DATA_QUEUE_AGGREGATE, t3-t2);
        MetricManager.addMetricBatch(simpleDataBatch);
        long t4 = System.currentTimeMillis();
        ThruputMetrics.addDataPoint(MonitoredAction.PUT_DATA_QUEUE_MERTIC_ADD_BATCH, t4-t3);
        ListMetricQueue.getInstance().addAll(simpleDataBatch);
        long t5 = System.currentTimeMillis();
        ThruputMetrics.addDataPoint(MonitoredAction.PUT_DATA_QUEUE_MERTIC_QUEUE_ADDALL, t5-t4);
      } catch (Throwable ex) {
        LOG.debug("PutMetricDataQueue:error");
        ex.printStackTrace();
        LOG.error(ex,ex);
      } finally {
        ThruputMetrics.addDataPoint(MonitoredAction.PUT_DATA_TIMING, System.currentTimeMillis()-before);
      }
    }
  };

  static {
    final String PROP_METRICS_FLUSH_INTERVAL = "com.eucalyptus.cloudwatch.metricsFlushInterval";
    final long DEFAULT_METRICS_FLUSH_INTERVAL = 60L;
    final long METRICS_FLUSH_INTERVAL = MoreObjects.firstNonNull(
        Longs.tryParse(
            System.getProperty(
                PROP_METRICS_FLUSH_INTERVAL,
                String.valueOf( DEFAULT_METRICS_FLUSH_INTERVAL ) ) ),
        DEFAULT_METRICS_FLUSH_INTERVAL );
    dataFlushTimer.scheduleAtFixedRate(safeRunner, 0, METRICS_FLUSH_INTERVAL, TimeUnit.SECONDS);
  }

  public static List<SimpleMetricEntity> aggregate(List<SimpleMetricEntity> dataBatch) {
    HashMap<PutMetricDataAggregationKey, SimpleMetricEntity> aggregationMap = Maps.newHashMap();
    for (SimpleMetricEntity item: dataBatch) {
      item.setTimestamp(MetricUtils.stripSeconds(item.getTimestamp()));
      PutMetricDataAggregationKey key = new PutMetricDataAggregationKey(item);
      if (!aggregationMap.containsKey(key)) {
        aggregationMap.put(key, new SimpleMetricEntity(item));
      } else {
        SimpleMetricEntity totalSoFar = aggregationMap.get(key);
        totalSoFar.setSampleMax(Math.max(item.getSampleMax(), totalSoFar.getSampleMax()));
        totalSoFar.setSampleMin(Math.min(item.getSampleMin(), totalSoFar.getSampleMin()));
        totalSoFar.setSampleSize(totalSoFar.getSampleSize() + item.getSampleSize());
        totalSoFar.setSampleSum(totalSoFar.getSampleSum() + item.getSampleSum());
      }
    }
    return Lists.newArrayList(aggregationMap.values());
  }

  protected static List<SimpleMetricEntity> convertToSimpleDataBatch(
      List<MetricQueueItem> stupidDataBatch) {
    ArrayList<SimpleMetricEntity> returnValue = new ArrayList<SimpleMetricEntity>();
    for(MetricQueueItem item: stupidDataBatch) {
      SimpleMetricEntity metricMetadata = new SimpleMetricEntity();
      metricMetadata.setAccountId(item.getAccountId());
      MetricDatum datum = item.getMetricDatum();
      
      metricMetadata.setMetricName(datum.getMetricName());
      metricMetadata.setNamespace(item.getNamespace());
      final List<Dimension> dimensions = datum.getDimensions( ) == null ?
          Collections.<Dimension>emptyList( ) :
          datum.getDimensions( ).getMember( );
      metricMetadata.setDimensionMap(makeDimensionMap(dimensions));
      metricMetadata.setMetricType(item.getMetricType());
      metricMetadata.setUnits(Units.fromValue(datum.getUnit()));
      metricMetadata.setTimestamp(datum.getTimestamp());
      if (datum.getValue() != null) { // Either or case taken care of in service
        metricMetadata.setSampleMax(datum.getValue());
        metricMetadata.setSampleMin(datum.getValue());
        metricMetadata.setSampleSum(datum.getValue());
        metricMetadata.setSampleSize(1.0);
      } else if ((datum.getStatisticValues() != null) &&
            (datum.getStatisticValues().getMaximum() != null) &&
            (datum.getStatisticValues().getMinimum() != null) &&
            (datum.getStatisticValues().getSum() != null) &&
            (datum.getStatisticValues().getSampleCount() != null)) {
          metricMetadata.setSampleMax(datum.getStatisticValues().getMaximum());
          metricMetadata.setSampleMin(datum.getStatisticValues().getMinimum());
          metricMetadata.setSampleSum(datum.getStatisticValues().getSum());
          metricMetadata.setSampleSize(datum.getStatisticValues().getSampleCount());
      } else {
        throw new RuntimeException("Statistics set (all values) or Value must be set"); 
      }
      returnValue.add(metricMetadata);
    }
    return returnValue;
  }

  public void insertMetricData(final String ownerAccountId, final String nameSpace,
                               final List<MetricDatum> metricDatum, final MetricType metricType) {
    // Some points do not actually go in.  If a data point represents an absolute value, the first one does not go in.
    // Also, some data points are added while we go through the list (derived metrics)
    Date now = new Date();

    for (final MetricDatum datum : metricDatum) {
      scrub(datum, now);
      queue(new Supplier<MetricQueueItem>() {
        @Override
        public MetricQueueItem get() {
          MetricQueueItem metricMetadata = new MetricQueueItem();
          metricMetadata.setAccountId(ownerAccountId);
          metricMetadata.setMetricDatum(datum);
          metricMetadata.setNamespace(nameSpace);
          metricMetadata.setMetricType(metricType);
          return metricMetadata;
        }
      });
    }
  }

  private void scrub(MetricDatum datum, Date now) {
    if (datum.getUnit() == null || datum.getUnit().trim().isEmpty()) datum.setUnit(Units.None.toString());
    if (datum.getTimestamp() == null) datum.setTimestamp(now);
  }

  private static Map<String, String> makeDimensionMap(
    final List<Dimension> dimensions
  ) {
    Map<String,String> returnValue = Maps.newTreeMap();
    for (Dimension dimension: dimensions) {
      returnValue.put(dimension.getName(), dimension.getValue());
    }
    return returnValue;
  }


}
