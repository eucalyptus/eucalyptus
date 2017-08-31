/*************************************************************************
 * Copyright 2009-2015 Ent. Services Development Corporation LP
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
package com.eucalyptus.cluster.callback.reporting;

import com.eucalyptus.cloudwatch.common.internal.domain.metricdata.Units;
import com.eucalyptus.cloudwatch.common.msgs.Dimension;
import com.eucalyptus.cloudwatch.common.msgs.MetricDatum;
import com.eucalyptus.cloudwatch.common.msgs.StatisticSet;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionResource;
import com.google.common.collect.Iterables;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import org.apache.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;

import javax.persistence.EntityTransaction;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DefaultAbsoluteMetricConverter {

  private static final Logger LOG = Logger.getLogger(DefaultAbsoluteMetricConverter.class);

  protected static List<AbsoluteMetricQueueItem> dealWithAbsoluteMetrics(
    Iterable<AbsoluteMetricQueueItem> dataBatch) {
    List<AbsoluteMetricQueueItem> regularMetrics = new ArrayList<AbsoluteMetricQueueItem>();
    // We need to do some sorting to allow fewer db lookups.  There is also logic for different metric types, so they will be sorted now.

    // Some points do not actually go in.  If a data point represents an absolute value, the first one does not go in.
    // Also, some data points are added while we go through the list (derived metrics)

    // Deal with the absolute metrics
    // CPUUtilization
    // VolumeReadOps
    // VolumeWriteOps
    // VolumeConsumedReadWriteOps
    // VolumeReadBytes
    // VolumeWriteBytes
    // VolumeTotalReadTime
    // VolumeTotalWriteTime
    // VolumeTotalReadWriteTime (used to calculate VolumeIdleTime)
    // DiskReadOps
    // DiskWriteOps
    // DiskReadBytes
    // DiskWriteBytes
    // NetworkIn
    // NetworkOut

    Multimap<String, AbsoluteMetricQueueItem> instanceMetricMap = LinkedListMultimap.create();
    Multimap<String, AbsoluteMetricQueueItem> volumeMetricMap = LinkedListMultimap.create();
    for (final AbsoluteMetricQueueItem item : dataBatch) {
      String nameSpace = item.getNamespace();
      MetricDatum datum = item.getMetricDatum();
      if (AbsoluteMetricHelper.AWS_EBS_NAMESPACE.equals(nameSpace)) {
        String volumeId = null;
        if ((datum.getDimensions() != null) && (datum.getDimensions().getMember() != null)) {
          for (Dimension dimension : datum.getDimensions().getMember()) {
            if (AbsoluteMetricHelper.VOLUME_ID_DIM_NAME.equals(dimension.getName())) {
              volumeId = dimension.getValue();
            }
          }
        }
        if (volumeId == null) {
          continue; // this data point doesn't count.
        } else {
          volumeMetricMap.put(volumeId, item);
        }
      } else if (AbsoluteMetricHelper.AWS_EC2_NAMESPACE.equals(nameSpace)) {
        String instanceId = null;
        if ((datum.getDimensions() != null) && (datum.getDimensions().getMember() != null)) {
          for (Dimension dimension : datum.getDimensions().getMember()) {
            if (AbsoluteMetricHelper.INSTANCE_ID_DIM_NAME.equals(dimension.getName())) {
              instanceId = dimension.getValue();
            }
          }
        }
        if (instanceId == null) {
          continue; // this data point doesn't count.
        } else {
          instanceMetricMap.put(instanceId, item);
        }
      } else {
        // not really an absolute metric, just leave it alone
        regularMetrics.add(item);
      }
    }
    for (List<String> partialVolumeKeySet: Iterables.partition(volumeMetricMap.keySet(), AbsoluteMetricQueue.ABSOLUTE_METRIC_NUM_DB_OPERATIONS_PER_TRANSACTION)) {
      try (final TransactionResource db = Entities.transactionFor(AbsoluteMetricHistory.class)) {
        int numVolumes = 0;
        for (String volumeId: partialVolumeKeySet) {
          AbsoluteMetricCache cache = new AbsoluteMetricCache(db);
          cache.load(AbsoluteMetricHelper.AWS_EBS_NAMESPACE, AbsoluteMetricHelper.VOLUME_ID_DIM_NAME, volumeId);
          for (AbsoluteMetricQueueItem item : volumeMetricMap.get(volumeId)) {
            String accountId = item.getAccountId();
            String nameSpace = item.getNamespace();
            MetricDatum datum = item.getMetricDatum();
            if (AbsoluteMetricHelper.EBS_ABSOLUTE_METRICS.containsKey(datum.getMetricName())) {
              // we check if the point below is a 'first' point, or maybe a point in the past.  Either case reject it.
              if (!adjustAbsoluteVolumeStatisticSet(cache, datum, datum.getMetricName(), AbsoluteMetricHelper.EBS_ABSOLUTE_METRICS.get(datum.getMetricName()), volumeId))
                continue;
            }
            // special cases
            // 1) VolumeThroughputPercentage -- this is 100% for provisioned volumes, and we need to insert a
            //                                  data point for every timestamp that a volume event occurs.
            //                                  To make sure we don't duplicate the effort, we choose one event at random, VolumeReadOps,
            //                                  and create this new metric arbitrarily
            if (AbsoluteMetricHelper.VOLUME_READ_OPS_METRIC_NAME.equals(datum.getMetricName())) { // special case
              regularMetrics.add(AbsoluteMetricHelper.createVolumeThroughputMetric(accountId, nameSpace, datum));
            }
            // 2) VolumeIdleTime -- we piggy back off of the metric we don't need VolumeTotalReadWriteTime, and convert it to VolumeIdleTime
            if (AbsoluteMetricHelper.VOLUME_TOTAL_READ_WRITE_TIME_METRIC_NAME.equals(datum.getMetricName())) {
              AbsoluteMetricHelper.convertVolumeTotalReadWriteTimeToVolumeIdleTime(datum);
            }
            // 3) VolumeQueueLength -- this one comes in essentially correct, but we don't have a time duration for it, so we piggy back off
            //                         the absolute metric framework
            if (AbsoluteMetricHelper.VOLUME_QUEUE_LENGTH_METRIC_NAME.equals(datum.getMetricName())) {
              if (!adjustAbsoluteVolumeQueueLengthStatisticSet(cache, datum, volumeId)) continue;
            }
            // Once here, our item has been appropriately adjusted.  Add it
            regularMetrics.add(item);
          }
          numVolumes++;
          if (numVolumes % AbsoluteMetricQueue.ABSOLUTE_METRIC_NUM_DB_OPERATIONS_UNTIL_SESSION_FLUSH == 0) {
            Entities.flushSession(AbsoluteMetricHistory.class);
            Entities.clearSession(AbsoluteMetricHistory.class);
          }
        }
        db.commit();
      }
    }
    for (List<String> partialInstanceKeySet: Iterables.partition(instanceMetricMap.keySet(), AbsoluteMetricQueue.ABSOLUTE_METRIC_NUM_DB_OPERATIONS_PER_TRANSACTION)) {
      try (final TransactionResource db = Entities.transactionFor(AbsoluteMetricHistory.class)) {
        int numInstances = 0;
        for (String instanceId: partialInstanceKeySet) {
          AbsoluteMetricCache cache = new AbsoluteMetricCache(db);
          cache.load(AbsoluteMetricHelper.AWS_EC2_NAMESPACE, AbsoluteMetricHelper.INSTANCE_ID_DIM_NAME, instanceId);
          for (AbsoluteMetricQueueItem item : instanceMetricMap.get(instanceId)) {
            String accountId = item.getAccountId();
            String nameSpace = item.getNamespace();
            MetricDatum datum = item.getMetricDatum();
            if (AbsoluteMetricHelper.EC2_ABSOLUTE_METRICS.containsKey(datum.getMetricName())) {
              if (!adjustAbsoluteInstanceStatisticSet(cache, datum, datum.getMetricName(), AbsoluteMetricHelper.EC2_ABSOLUTE_METRICS.get(datum.getMetricName()), instanceId))
                continue;
            } else if (AbsoluteMetricHelper.CPU_UTILIZATION_MS_ABSOLUTE_METRIC_NAME.equals(datum.getMetricName())) { // special case
              // we check if the point below is a 'first' point, or maybe a point in the past.  Either case reject it.
              if (!adjustAbsoluteInstanceCPUStatisticSet(cache, datum, AbsoluteMetricHelper.CPU_UTILIZATION_MS_ABSOLUTE_METRIC_NAME, AbsoluteMetricHelper.CPU_UTILIZATION_METRIC_NAME, instanceId))
                continue;
            }
            // Once here, our item has been appropriately adjusted.  Add it
            regularMetrics.add(item);
          }
          numInstances++;
          if (numInstances % AbsoluteMetricQueue.ABSOLUTE_METRIC_NUM_DB_OPERATIONS_UNTIL_SESSION_FLUSH == 0) {
            Entities.flushSession(AbsoluteMetricHistory.class);
            Entities.clearSession(AbsoluteMetricHistory.class);
          }
        }
        db.commit();
      }
    }
    return regularMetrics;
  }

  private static boolean adjustAbsoluteInstanceCPUStatisticSet(AbsoluteMetricCache cache, MetricDatum datum, String absoluteMetricName,
                                                       String relativeMetricName, String instanceId) {
    AbsoluteMetricHelper.MetricDifferenceInfo info = AbsoluteMetricHelper.calculateDifferenceSinceLastEvent(cache, AbsoluteMetricHelper.AWS_EC2_NAMESPACE, absoluteMetricName, AbsoluteMetricHelper.INSTANCE_ID_DIM_NAME, instanceId, datum.getTimestamp(), datum.getValue());
    if (info != null) {
      // calculate percentage
      double percentage = 0.0;
      if (info.getElapsedTimeInMillis() != 0) {
        // don't want to divide by 0
        percentage = 100.0 * (info.getValueDifference() / info.getElapsedTimeInMillis());
      }
      datum.setMetricName(relativeMetricName);
      datum.setValue(null);
      StatisticSet statisticSet = new StatisticSet();
      statisticSet.setMaximum(percentage);
      statisticSet.setMinimum(percentage);
      double sampleCount = (double) info.getElapsedTimeInMillis() / 60000.0; // number of minutes (this weights the value)
      statisticSet.setSum(sampleCount * percentage);
      statisticSet.setSampleCount(sampleCount);
      datum.setStatisticValues(statisticSet);
      datum.setUnit(Units.Percent.toString());
      return true; //don't continue;
    }
    return false; // continue
  }

  private static boolean adjustAbsoluteInstanceStatisticSet(AbsoluteMetricCache cache, MetricDatum datum, String absoluteMetricName,
                                                    String relativeMetricName, String instanceId) {
    if (instanceId == null) return false;
    AbsoluteMetricHelper.MetricDifferenceInfo info = AbsoluteMetricHelper.calculateDifferenceSinceLastEvent(cache, AbsoluteMetricHelper.AWS_EC2_NAMESPACE, absoluteMetricName, AbsoluteMetricHelper.INSTANCE_ID_DIM_NAME, instanceId, datum.getTimestamp(), datum.getValue());
    if (info != null) {
      datum.setMetricName(relativeMetricName);
      // we need to weigh this data based on the time.  use a statistic set instead of the value
      datum.setValue(null);
      StatisticSet statisticSet = new StatisticSet();
      double sampleCount = (double) info.getElapsedTimeInMillis() / 60000.0; // number of minutes (this weights the value)
      statisticSet.setSum(info.getValueDifference());
      statisticSet.setMaximum(info.getValueDifference() / sampleCount);
      statisticSet.setMinimum(info.getValueDifference() / sampleCount);
      statisticSet.setSampleCount(sampleCount);
      datum.setStatisticValues(statisticSet);
      return true; //don't continue;
    }
    return false; // continue
  }

  static boolean adjustAbsoluteVolumeStatisticSet(AbsoluteMetricCache cache, MetricDatum datum,
                                                  String absoluteMetricName, String relativeMetricName, String volumeId) {
    if (volumeId == null) return false;
    AbsoluteMetricHelper.MetricDifferenceInfo info = AbsoluteMetricHelper.calculateDifferenceSinceLastEvent(cache, AbsoluteMetricHelper.AWS_EBS_NAMESPACE, absoluteMetricName, AbsoluteMetricHelper.VOLUME_ID_DIM_NAME, volumeId, datum.getTimestamp(), datum.getValue());
    if (info != null) {
      datum.setMetricName(relativeMetricName);
      // we need to weigh this data based on the time.  use a statistic set instead of the value
      datum.setValue(null);
      StatisticSet statisticSet = new StatisticSet();
      double sampleCount = (double) info.getElapsedTimeInMillis() / 60000.0; // number of minutes (this weights the value)
      statisticSet.setSum(info.getValueDifference());
      statisticSet.setMaximum(info.getValueDifference() / sampleCount);
      statisticSet.setMinimum(info.getValueDifference() / sampleCount);
      statisticSet.setSampleCount(sampleCount);
      datum.setStatisticValues(statisticSet);
      return true; //don't continue;
    }
    return false; // continue
  }

  static boolean adjustAbsoluteVolumeQueueLengthStatisticSet(AbsoluteMetricCache cache,
                                                             MetricDatum datum, String volumeId) {
    // the metric value is correct, we just need a statistic set with the sample count.
    // to get this we create a placeholder absolute metric, value always 0, just to get time duration/sample count
    MetricDatum absolutePlaceHolder = new MetricDatum();
    absolutePlaceHolder.setMetricName(AbsoluteMetricHelper.VOLUME_QUEUE_LENGTH_PLACEHOLDER_ABSOLUTE_METRIC_NAME);
    absolutePlaceHolder.setValue(0.0);
    absolutePlaceHolder.setTimestamp(datum.getTimestamp());
    if (!adjustAbsoluteVolumeStatisticSet(cache, absolutePlaceHolder, absolutePlaceHolder.getMetricName(), AbsoluteMetricHelper.VOLUME_QUEUE_LENGTH_PLACEHOLDER_METRIC_NAME, volumeId)) return false;
    // otherwise, we have a duration/sample count
    double sampleCount = absolutePlaceHolder.getStatisticValues().getSampleCount();
    double value = datum.getValue();
    datum.setValue(null);
    StatisticSet statisticSet = new StatisticSet();
    statisticSet.setMaximum(value);
    statisticSet.setMinimum(value);
    statisticSet.setSum(value * sampleCount);
    statisticSet.setSampleCount(sampleCount);
    datum.setStatisticValues(statisticSet);
    return true;
  }

  public static class AbsoluteMetricCache {
    private EntityTransaction db;
    private Set<AbsoluteMetricLoadCacheKey> loaded = Sets.newHashSet();
    private Map<AbsoluteMetricCacheKey, AbsoluteMetricHistory> cacheMap = Maps.newHashMap();
    public AbsoluteMetricCache(EntityTransaction db) {
      this.db = db;
    }

    public void load(String namespace, String dimensionName, String dimensionValue) {
      AbsoluteMetricLoadCacheKey loadKey = new AbsoluteMetricLoadCacheKey(namespace, dimensionName);
      if (!loaded.contains(loadKey)) {
        Criteria criteria = Entities.createCriteria(AbsoluteMetricHistory.class)
          .add( Restrictions.eq("namespace", namespace) )
//          .add( Restrictions.eq( "dimensionName", dimensionName ) );
          .add( Restrictions.eq( "dimensionValue", dimensionValue ) );
        List<AbsoluteMetricHistory> list = (List<AbsoluteMetricHistory>) criteria.list();
        for (AbsoluteMetricHistory item: list) {
          cacheMap.put(new AbsoluteMetricCacheKey(item), item);
        }
        loaded.add(loadKey);
      }
    }

    public AbsoluteMetricHistory lookup(String namespace, String metricName,
                                        String dimensionName, String dimensionValue) {
      return cacheMap.get(new AbsoluteMetricCacheKey(namespace, metricName, dimensionName, dimensionValue));
    }

    public void put(String namespace, String metricName, String dimensionName,
                    String dimensionValue, AbsoluteMetricHistory lastEntity) {
      cacheMap.put(new AbsoluteMetricCacheKey(namespace, metricName, dimensionName, dimensionValue), lastEntity);
    }

  }

  public static class AbsoluteMetricLoadCacheKey {
    private String namespace;
    private String dimensionValue;

    public String getNamespace() {
      return namespace;
    }

    public void setNamespace(String namespace) {
      this.namespace = namespace;
    }

    public String getDimensionValue() {
      return dimensionValue;
    }

    public void setDimensionValue(String dimensionValue) {
      this.dimensionValue = dimensionValue;
    }

    public AbsoluteMetricLoadCacheKey(String namespace, String dimensionValue) {
      this.namespace = namespace;
      this.dimensionValue = dimensionValue;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      AbsoluteMetricLoadCacheKey that = (AbsoluteMetricLoadCacheKey) o;

      if (dimensionValue != null ? !dimensionValue.equals(that.dimensionValue) : that.dimensionValue != null)
        return false;
      if (namespace != null ? !namespace.equals(that.namespace) : that.namespace != null) return false;

      return true;
    }

    @Override
    public int hashCode() {
      int result = namespace != null ? namespace.hashCode() : 0;
      result = 31 * result + (dimensionValue != null ? dimensionValue.hashCode() : 0);
      return result;
    }
  }

  public static class AbsoluteMetricCacheKey {
    private String namespace;
    private String metricName;
    private String dimensionName;
    private String dimensionValue;
    public String getNamespace() {
      return namespace;
    }
    public void setNamespace(String namespace) {
      this.namespace = namespace;
    }
    public String getMetricName() {
      return metricName;
    }
    public void setMetricName(String metricName) {
      this.metricName = metricName;
    }
    public String getDimensionName() {
      return dimensionName;
    }
    public void setDimensionName(String dimensionName) {
      this.dimensionName = dimensionName;
    }
    public String getDimensionValue() {
      return dimensionValue;
    }
    public void setDimensionValue(String dimensionValue) {
      this.dimensionValue = dimensionValue;
    }
    public AbsoluteMetricCacheKey(String namespace, String metricName,
                                  String dimensionName, String dimensionValue) {
      super();
      this.namespace = namespace;
      this.metricName = metricName;
      this.dimensionName = dimensionName;
      this.dimensionValue = dimensionValue;
    }
    public AbsoluteMetricCacheKey(AbsoluteMetricHistory item) {
      super();
      this.namespace = item.getNamespace();
      this.metricName = item.getMetricName();
      this.dimensionName = item.getDimensionName();
      this.dimensionValue = item.getDimensionValue();
    }
    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result
        + ((dimensionName == null) ? 0 : dimensionName.hashCode());
      result = prime * result
        + ((dimensionValue == null) ? 0 : dimensionValue.hashCode());
      result = prime * result
        + ((metricName == null) ? 0 : metricName.hashCode());
      result = prime * result
        + ((namespace == null) ? 0 : namespace.hashCode());
      return result;
    }
    @Override
    public boolean equals(Object obj) {
      if (this == obj)
        return true;
      if (obj == null)
        return false;
      if (getClass() != obj.getClass())
        return false;
      AbsoluteMetricCacheKey other = (AbsoluteMetricCacheKey) obj;
      if (dimensionName == null) {
        if (other.dimensionName != null)
          return false;
      } else if (!dimensionName.equals(other.dimensionName))
        return false;
      if (dimensionValue == null) {
        if (other.dimensionValue != null)
          return false;
      } else if (!dimensionValue.equals(other.dimensionValue))
        return false;
      if (metricName == null) {
        if (other.metricName != null)
          return false;
      } else if (!metricName.equals(other.metricName))
        return false;
      if (namespace == null) {
        if (other.namespace != null)
          return false;
      } else if (!namespace.equals(other.namespace))
        return false;
      return true;
    }
  }
}
