package com.eucalyptus.cloudwatch.domain.metricdata;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.persistence.EntityTransaction;

import org.apache.log4j.Logger;
import org.hibernate.exception.ConstraintViolationException;

import com.eucalyptus.cloudwatch.Dimension;
import com.eucalyptus.cloudwatch.MetricDatum;
import com.eucalyptus.cloudwatch.domain.listmetrics.ListMetricManager;
import com.eucalyptus.cloudwatch.domain.metricdata.MetricEntity.MetricType;
import com.eucalyptus.cloudwatch.domain.metricdata.MetricEntity.Units;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.util.OwnerFullName;
import com.google.common.base.Function;
import com.google.common.base.Supplier;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Ordering;

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
        } catch (RuntimeException ex) {
          LOG.error(ex,ex);
        }
      }

    };
    dataFlushTimer.schedule(safeRunner, 1, TimeUnit.MINUTES);
  }

  private static List<MetricQueueItem> aggregate(List<MetricQueueItem> dataBatch) {
    return dataBatch;
  }

  public void insertMetricData(final String ownerId,
      final String ownerAccountId, final String nameSpace,
      final List<MetricDatum> metricDatum) {
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
          metricMetadata.setMetricType(MetricType.Custom);
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

  private static class MetricQueueItem {
    private String accountId;
    private String userId;
    private String metricName;
    private String namespace;
    private Map<String, String> dimensionMap;
    private MetricType metricType;
    private Units units;
    private Date timestamp;
    private Double sampleSize;
    private Double sampleMax;
    private Double sampleMin;
    private Double sampleSum;

    private MetricQueueItem() {
    }

    public String getAccountId() {
      return accountId;
    }

    public void setAccountId(String accountId) {
      this.accountId = accountId;
    }

    public String getUserId() {
      return userId;
    }

    public void setUserId(String userId) {
      this.userId = userId;
    }

    public String getMetricName() {
      return metricName;
    }

    public void setMetricName(String metricName) {
      this.metricName = metricName;
    }

    public String getNamespace() {
      return namespace;
    }

    public void setNamespace(String namespace) {
      this.namespace = namespace;
    }

    public Map<String, String> getDimensionMap() {
      return dimensionMap;
    }

    public void setDimensionMap(Map<String, String> dimensionMap) {
      this.dimensionMap = dimensionMap;
    }

    public MetricType getMetricType() {
      return metricType;
    }

    public void setMetricType(MetricType metricType) {
      this.metricType = metricType;
    }

    public Units getUnits() {
      return units;
    }

    public void setUnits(Units units) {
      this.units = units;
    }

    public Date getTimestamp() {
      return timestamp;
    }

    public void setTimestamp(Date timestamp) {
      this.timestamp = timestamp;
    }

    public Double getSampleSize() {
      return sampleSize;
    }

    public void setSampleSize(Double sampleSize) {
      this.sampleSize = sampleSize;
    }

    public Double getSampleMax() {
      return sampleMax;
    }

    public void setSampleMax(Double sampleMax) {
      this.sampleMax = sampleMax;
    }

    public Double getSampleMin() {
      return sampleMin;
    }

    public void setSampleMin(Double sampleMin) {
      this.sampleMin = sampleMin;
    }

    public Double getSampleSum() {
      return sampleSum;
    }

    public void setSampleSum(Double sampleSum) {
      this.sampleSum = sampleSum;
    }

  }
}
