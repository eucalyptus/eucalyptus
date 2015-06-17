/*************************************************************************
 * Copyright 2009-2014 Eucalyptus Systems, Inc.
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

import com.eucalyptus.cloudwatch.common.internal.metricdata.Units;
import org.apache.log4j.Logger;

import com.eucalyptus.cloudwatch.common.backend.msgs.Dimension;
import com.eucalyptus.cloudwatch.common.backend.msgs.MetricDatum;
import com.eucalyptus.cloudwatch.domain.listmetrics.ListMetricManager;
import com.eucalyptus.cloudwatch.domain.metricdata.MetricEntity.MetricType;
import com.google.common.base.Supplier;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class MetricDataQueue {
  private static final Logger LOG = Logger.getLogger(MetricDataQueue.class);
  final static LinkedBlockingQueue<MetricQueueItem> dataQueue = new LinkedBlockingQueue<MetricQueueItem>();

  private static final ScheduledExecutorService dataFlushTimer = Executors
      .newSingleThreadScheduledExecutor();

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
        dataQueue.drainTo(dataBatch, 15000);
        LOG.debug("Timing:dataBatch.size()="+dataBatch.size());
        long t1 = System.currentTimeMillis();
        List<SimpleMetricEntity> simpleDataBatch = convertToSimpleDataBatch(dataBatch);
        long t2 = System.currentTimeMillis();
        LOG.debug("Timing:dataBatch.convertToSimpleDataBatch():time="+(t2-t1));
        simpleDataBatch = aggregate(simpleDataBatch);
        long t3 = System.currentTimeMillis();
        LOG.debug("Timing:dataBatch.aggregate():time="+(t3-t2));
        MetricManager.addMetricBatch(simpleDataBatch);
        long t4 = System.currentTimeMillis();
        LOG.debug("Timing:dataBatch.MetricManager.addMetricBatch():time="+(t4-t3));
        ListMetricManager.addMetricBatch(simpleDataBatch);
        long t5 = System.currentTimeMillis();
        LOG.debug("Timing:ListMetricManager.addMetricBatch:time="+(t5-t4));
      } catch (Throwable ex) {
        LOG.debug("error");
        ex.printStackTrace();
        LOG.error(ex,ex);
      } finally {
        long after = System.currentTimeMillis();
        LOG.debug("Timing:time="+(after-before));
      }
    }
  };

  static {
    dataFlushTimer.scheduleAtFixedRate(safeRunner, 0, 1, TimeUnit.MINUTES);
  }

  public static List<SimpleMetricEntity> aggregate(List<SimpleMetricEntity> dataBatch) {
    HashMap<PutMetricDataAggregationKey, SimpleMetricEntity> aggregationMap = Maps.newHashMap();
    for (SimpleMetricEntity item: dataBatch) {
      item.setTimestamp(MetricManager.stripSeconds(item.getTimestamp()));
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
