package com.eucalyptus.cloudwatch.domain.metricdata;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.log4j.Logger;

import com.eucalyptus.cloudwatch.Dimension;
import com.eucalyptus.cloudwatch.MetricDatum;
import com.eucalyptus.cloudwatch.domain.listmetrics.ListMetricManager;
import com.eucalyptus.cloudwatch.domain.metricdata.MetricEntity.MetricType;
import com.eucalyptus.cloudwatch.domain.metricdata.MetricEntity.Units;
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

  private static AtomicBoolean busy = new AtomicBoolean(false);

  private void queue(Supplier<MetricQueueItem> metriMetaDataSupplier) {
    final MetricQueueItem metricData = metriMetaDataSupplier.get();
    dataQueue.offer(metricData);
    if (!busy.get()) {
      flushDataQueue();
    }
  }

  private void flushDataQueue() {
    busy.set(true);

    Runnable safeRunner = new Runnable() {
      @Override
      public void run() {
        try {
          List<MetricQueueItem> dataBatch = Lists.newArrayList();
          dataQueue.drainTo(dataBatch);
          dataBatch = aggregate(dataBatch);
          for (final MetricQueueItem metricData : dataBatch) {
            MetricManager.addMetric(metricData.getAccountId(), metricData.getUserId(), 
                metricData.getMetricName(), metricData.getNamespace(), 
                metricData.getDimensionMap(), metricData.getMetricType(), 
                metricData.getUnits(), metricData.getTimestamp(), 
                metricData.getSampleSize(), metricData.getSampleMax(), 
                metricData.getSampleMin(), metricData.getSampleSum());
            ListMetricManager.addMetric(metricData.getAccountId(), metricData.getMetricName(),
                metricData.getNamespace(), metricData.getDimensionMap());
          }
          dataQueue.clear();
          busy.set(false);
        } catch (Exception ex) {
          ex.printStackTrace();
          LOG.error(ex,ex);
        }
      }

    };
    dataFlushTimer.schedule(safeRunner, 1, TimeUnit.MINUTES);
  }

  public static List<MetricQueueItem> aggregate(List<MetricQueueItem> dataBatch) {
    HashMap<PutMetricDataAggregationKey, MetricQueueItem> aggregationMap = Maps.newHashMap();
    for (MetricQueueItem item: dataBatch) {
      item.setTimestamp(MetricManager.stripSeconds(item.getTimestamp()));
      PutMetricDataAggregationKey key = new PutMetricDataAggregationKey(item);
      if (!aggregationMap.containsKey(key)) {
        aggregationMap.put(key, new MetricQueueItem(item));
      } else {
        MetricQueueItem totalSoFar = aggregationMap.get(key);
        totalSoFar.setSampleMax(Math.max(item.getSampleMax(), totalSoFar.getSampleMax()));
        totalSoFar.setSampleMin(Math.min(item.getSampleMin(), totalSoFar.getSampleMin()));
        totalSoFar.setSampleSize(totalSoFar.getSampleSize() + item.getSampleSize());
        totalSoFar.setSampleSum(totalSoFar.getSampleSum() + item.getSampleSum());
      }
    }
    return Lists.newArrayList(aggregationMap.values());
  }
  public void insertMetricData(final String ownerId,
      final String ownerAccountId, final String nameSpace,
      final List<MetricDatum> metricDatum, final MetricType metricType) {
    Date now = new Date();
    for (final MetricDatum datum : metricDatum) {
      scrub(datum, now);
      final ArrayList<Dimension> dimensions = datum.getDimensions().getMember(); 
      queue(new Supplier<MetricQueueItem>() {
        @Override
        public MetricQueueItem get() {
          MetricQueueItem metricMetadata = new MetricQueueItem();
          metricMetadata.setAccountId(ownerAccountId);
          metricMetadata.setUserId(ownerId);
          metricMetadata.setMetricName(datum.getMetricName());
          metricMetadata.setNamespace(nameSpace);
          metricMetadata.setDimensionMap(makeDimensionMap(dimensions));
          metricMetadata.setMetricType(metricType);
          metricMetadata.setUnits(Units.fromValue(datum.getUnit())); 
          metricMetadata.setTimestamp(datum.getTimestamp());
          if (datum.getValue() != null) { // TODO: make sure both value and statistics sets are not set
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
            throw new RuntimeException("Values missing"); // TODO: clarify
          }
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
      ArrayList<Dimension> dimensions) {
    Map<String,String> returnValue = Maps.newTreeMap();
    for (Dimension dimension: dimensions) {
      returnValue.put(dimension.getName(), dimension.getValue());
    }
    return returnValue;
  }

}
