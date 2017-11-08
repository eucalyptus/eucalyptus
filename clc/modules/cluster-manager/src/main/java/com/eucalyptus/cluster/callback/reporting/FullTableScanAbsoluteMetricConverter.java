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
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.log4j.Logger;
import org.hibernate.CacheMode;
import org.hibernate.Criteria;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class FullTableScanAbsoluteMetricConverter {

  private static final Logger LOG = Logger.getLogger(FullTableScanAbsoluteMetricConverter.class);

  protected static List<AbsoluteMetricQueueItem> dealWithAbsoluteMetrics(Iterable<AbsoluteMetricQueueItem> dataBatch) {
    List<AbsoluteMetricQueueItem> regularMetrics = Lists.newArrayList();
    List<SimpleAbsoluteMetricHistory> absoluteMetricsToInsert = Lists.newArrayList();
    SortedAbsoluteMetrics sortedAbsoluteMetrics = sortAbsoluteMetrics(dataBatch);
    regularMetrics.addAll(sortedAbsoluteMetrics.getRegularMetrics());
    AbsoluteMetricMap absoluteMetricMap = sortedAbsoluteMetrics.getAbsoluteMetricMap();
    try (final TransactionResource db = Entities.transactionFor(AbsoluteMetricHistory.class)) {
      int count = 0;
      Criteria criteria = Entities.createCriteria(AbsoluteMetricHistory.class);
      final ScrollableResults absoluteMetrics = criteria.setCacheMode(CacheMode.IGNORE).scroll(ScrollMode.FORWARD_ONLY);
      try {
        while ( absoluteMetrics.next( ) ) {
          AbsoluteMetricHistory absoluteMetricHistory = (AbsoluteMetricHistory) absoluteMetrics.get( 0 );
          if ( absoluteMetricMap.containsKey(
              absoluteMetricHistory.getNamespace( ), absoluteMetricHistory.getMetricName( ),
              absoluteMetricHistory.getDimensionName( ), absoluteMetricHistory.getDimensionValue( ) ) ) {
            MetricsAndOtherFields metricsAndOtherFields =
                absoluteMetricMap.getMetricsAndOtherFields( absoluteMetricHistory.getNamespace( ),
                    absoluteMetricHistory.getMetricName( ), absoluteMetricHistory.getDimensionName( ),
                    absoluteMetricHistory.getDimensionValue( ) );
            Map<TimestampAndMetricValue, MetricDatum> metricDatumMap = metricsAndOtherFields.getMetricDatumMap( );
            SequentialMetrics sequentialMetrics = calculateSequentialMetrics( absoluteMetricHistory, metricDatumMap, metricsAndOtherFields.getAccountId( ), metricsAndOtherFields.getRelativeMetricName( ) );
            absoluteMetricMap.removeEntries( absoluteMetricHistory.getNamespace( ), absoluteMetricHistory.getMetricName( ),
                absoluteMetricHistory.getDimensionName( ), absoluteMetricHistory.getDimensionValue( ) );
            for ( AbsoluteMetricQueueItem regularMetric : sequentialMetrics.getRegularMetrics( ) ) {
              if ( AbsoluteMetricHelper.AWS_EBS_NAMESPACE.equals( regularMetric.getNamespace( ) ) ) {
                if ( AbsoluteMetricHelper.VOLUME_READ_OPS_METRIC_NAME.equals( regularMetric.getMetricDatum( ).getMetricName( ) ) ) { // special case
                  regularMetrics.add( AbsoluteMetricHelper.createVolumeThroughputMetric( regularMetric.getAccountId( ), regularMetric.getNamespace( ), regularMetric.getMetricDatum( ) ) );
                } else if ( AbsoluteMetricHelper.VOLUME_TOTAL_READ_WRITE_TIME_METRIC_NAME.equals( regularMetric.getMetricDatum( ).getMetricName( ) ) ) {
                  AbsoluteMetricHelper.convertVolumeTotalReadWriteTimeToVolumeIdleTime( regularMetric.getMetricDatum( ) );
                }
              }
              regularMetrics.add( regularMetric );
            }
            absoluteMetricHistory.setTimestamp( sequentialMetrics.getUpdateTimestamp( ) );
            absoluteMetricHistory.setLastMetricValue( sequentialMetrics.getUpdateValue( ) );
            if ( ++count % AbsoluteMetricQueue.ABSOLUTE_METRIC_NUM_DB_OPERATIONS_UNTIL_SESSION_FLUSH == 0 ) {
              Entities.flushSession( AbsoluteMetricHistory.class );
              Entities.clearSession( AbsoluteMetricHistory.class );
            }
          }
        }
      } finally {
        absoluteMetrics.close( );
      }
      db.commit();
    }
    // Now parse entries only in the map...
    for (AbsoluteMetricMap.NamespaceMetricNameAndDimension namespaceMetricNameAndDimension: absoluteMetricMap.keySet()) {
      AbsoluteMetricHistory absoluteMetricHistory = new AbsoluteMetricHistory();
      absoluteMetricHistory.setNamespace(namespaceMetricNameAndDimension.getNamespace());
      absoluteMetricHistory.setMetricName(namespaceMetricNameAndDimension.getMetricName());
      absoluteMetricHistory.setDimensionName(namespaceMetricNameAndDimension.getDimensionName());
      absoluteMetricHistory.setDimensionValue(namespaceMetricNameAndDimension.getDimensionValue());
      MetricsAndOtherFields metricsAndOtherFields = absoluteMetricMap.get(namespaceMetricNameAndDimension);
      Map<TimestampAndMetricValue, MetricDatum> metricDataMap = metricsAndOtherFields.getMetricDatumMap();
      if (metricDataMap.size() == 0) continue;
      TimestampAndMetricValue firstValue = metricDataMap.keySet().iterator().next();
      metricDataMap.remove(firstValue);
      absoluteMetricHistory.setLastMetricValue(firstValue.getMetricValue());
      absoluteMetricHistory.setTimestamp(firstValue.getTimestamp());
      if (metricDataMap.size() != 0) {
        SequentialMetrics sequentialMetrics = calculateSequentialMetrics(absoluteMetricHistory, metricDataMap,
          metricsAndOtherFields.getAccountId(), metricsAndOtherFields.getRelativeMetricName());
        for (AbsoluteMetricQueueItem regularMetric: sequentialMetrics.getRegularMetrics()) {
          if (AbsoluteMetricHelper.AWS_EBS_NAMESPACE.equals(regularMetric.getNamespace())) {
            if (AbsoluteMetricHelper.VOLUME_READ_OPS_METRIC_NAME.equals(regularMetric.getMetricDatum().getMetricName())) { // special case
              regularMetrics.add(AbsoluteMetricHelper.createVolumeThroughputMetric(regularMetric.getAccountId(), regularMetric.getNamespace(), regularMetric.getMetricDatum()));
            } else if (AbsoluteMetricHelper.VOLUME_TOTAL_READ_WRITE_TIME_METRIC_NAME.equals(regularMetric.getMetricDatum().getMetricName())) {
              AbsoluteMetricHelper.convertVolumeTotalReadWriteTimeToVolumeIdleTime(regularMetric.getMetricDatum());
            }
          }
          regularMetrics.add(regularMetric);
        }
        absoluteMetricHistory.setTimestamp(sequentialMetrics.getUpdateTimestamp());
        absoluteMetricHistory.setLastMetricValue(sequentialMetrics.getUpdateValue());
      }
      absoluteMetricsToInsert.add(convertToSimpleAbsoluteMetricHistory(absoluteMetricHistory));
    }

    // insert all new points
    try (final TransactionResource db = Entities.transactionFor(AbsoluteMetricHistory.class)) {
      int count = 0;
      for (SimpleAbsoluteMetricHistory simpleAbsoluteMetricHistory : absoluteMetricsToInsert) {
        Entities.persist(convertToAbsoluteMetricHistory(simpleAbsoluteMetricHistory));
        if (++count % AbsoluteMetricQueue.ABSOLUTE_METRIC_NUM_DB_OPERATIONS_UNTIL_SESSION_FLUSH == 0) {
          Entities.flushSession(AbsoluteMetricHistory.class);
          Entities.clearSession(AbsoluteMetricHistory.class);
        }
      }
      db.commit();
    }
    return regularMetrics;
  }

  private static SimpleAbsoluteMetricHistory convertToSimpleAbsoluteMetricHistory(AbsoluteMetricHistory absoluteMetricHistory) {
    SimpleAbsoluteMetricHistory simpleAbsoluteMetricHistory = new SimpleAbsoluteMetricHistory();
    simpleAbsoluteMetricHistory.setNamespace(absoluteMetricHistory.getNamespace());
    simpleAbsoluteMetricHistory.setMetricName(absoluteMetricHistory.getMetricName());
    simpleAbsoluteMetricHistory.setDimensionName(absoluteMetricHistory.getDimensionName());
    simpleAbsoluteMetricHistory.setDimensionValue(absoluteMetricHistory.getDimensionValue());
    simpleAbsoluteMetricHistory.setTimestamp(absoluteMetricHistory.getTimestamp());
    simpleAbsoluteMetricHistory.setLastMetricValue(absoluteMetricHistory.getLastMetricValue());
    return simpleAbsoluteMetricHistory;
  }

  private static AbsoluteMetricHistory convertToAbsoluteMetricHistory(SimpleAbsoluteMetricHistory simpleAbsoluteMetricHistory) {
    AbsoluteMetricHistory absoluteMetricHistory = new AbsoluteMetricHistory();
    absoluteMetricHistory.setNamespace(simpleAbsoluteMetricHistory.getNamespace());
    absoluteMetricHistory.setMetricName(simpleAbsoluteMetricHistory.getMetricName());
    absoluteMetricHistory.setDimensionName(simpleAbsoluteMetricHistory.getDimensionName());
    absoluteMetricHistory.setDimensionValue(simpleAbsoluteMetricHistory.getDimensionValue());
    absoluteMetricHistory.setTimestamp(simpleAbsoluteMetricHistory.getTimestamp());
    absoluteMetricHistory.setLastMetricValue(simpleAbsoluteMetricHistory.getLastMetricValue());
    return absoluteMetricHistory;
  }

  private static SequentialMetrics calculateSequentialMetrics(AbsoluteMetricHistory absoluteMetricHistory,
                                                              Map<TimestampAndMetricValue, MetricDatum> metricDatumMap,
                                                              String accountId,
                                                              String relativeMetricName) {
    SequentialMetrics sequentialMetrics = new SequentialMetrics();

    Date lastDate = absoluteMetricHistory.getTimestamp();
    Double lastValue = absoluteMetricHistory.getLastMetricValue();

    for (TimestampAndMetricValue value : metricDatumMap.keySet()) {
      double valueDiff = value.getMetricValue() - lastValue;

      // VolumeQueueLength is a special case.  Values in the table are set to 0 but the data set uses the values passed in.
      boolean isVolumeQueueLengthCase = AbsoluteMetricHelper.AWS_EBS_NAMESPACE.equals(absoluteMetricHistory.getNamespace()) &&
        AbsoluteMetricHelper.VOLUME_QUEUE_LENGTH_PLACEHOLDER_ABSOLUTE_METRIC_NAME.equals(absoluteMetricHistory.getMetricName());

      // CPUUtilization is also a special case.  The value is a percent.
      boolean isCPUUtilizationCase = AbsoluteMetricHelper.AWS_EC2_NAMESPACE.equals(absoluteMetricHistory.getNamespace()) &&
        AbsoluteMetricHelper.CPU_UTILIZATION_MS_ABSOLUTE_METRIC_NAME.equals(absoluteMetricHistory.getMetricName());


      long timeDiff = value.getTimestamp().getTime() - lastDate.getTime();
      if (timeDiff < 0) {
        // a negative value of timeDiff means this data point is not useful
        continue;
      } else if (timeDiff == 0) {
        if (Math.abs(valueDiff) > AbsoluteMetricHelper.TOLERANCE) {
          LOG.warn("Getting different values " + value.getMetricValue() + " and " + lastValue + " for absolute metric " + absoluteMetricHistory.getMetricName() + " at the same timestamp " + lastDate + ", keeping the second value.");
        }
        continue;
      } else {
        // definitely update the value
        lastDate = value.getTimestamp();
        lastValue = isVolumeQueueLengthCase ? 0 : value.getMetricValue();
        if (timeDiff > AbsoluteMetricHelper.MAX_DIFFERENCE_DURATION_MS) {
          // Too much time has passed, a useful data point, but we will not report the 'difference'.  We will reset.
          continue;
        } else if (valueDiff < -AbsoluteMetricHelper.TOLERANCE) { // value has gone "down" (or down more than the AbsoluteMetricCommon.TOLERANCE)
          // if the value difference is negative (i.e. has gone down, the assumption is that the NC has restarted, and the new
          // value started from some time in the past.  Best thing to do here is either assume it is a first point again, or
          // assume the previous point had a 0 value.  Not sure which is the better choice, but for now, we will make it a "first"
          // point again
          continue;
        } else { // truncate differences within AbsoluteMetricCommon.TOLERANCE to zero
          if (Math.abs(valueDiff) < AbsoluteMetricHelper.TOLERANCE) {
            valueDiff = 0.0;
          }
          AbsoluteMetricQueueItem regularMetric = new AbsoluteMetricQueueItem();
          regularMetric.setNamespace(absoluteMetricHistory.getNamespace());
          regularMetric.setAccountId(accountId);
          MetricDatum datum = metricDatumMap.get(value);
          regularMetric.setMetricDatum(datum);
          datum.setMetricName(relativeMetricName);
          datum.setValue(null);
          StatisticSet statisticSet = new StatisticSet();
          double sampleCount = (double) timeDiff / 60000.0; // number of minutes (this weights the value)
          if (isVolumeQueueLengthCase) {
            datum.setMetricName(AbsoluteMetricHelper.VOLUME_QUEUE_LENGTH_METRIC_NAME); // other values are for the absolute metric history table only
            statisticSet.setSum(value.getMetricValue() * sampleCount);
            statisticSet.setMaximum(value.getMetricValue());
            statisticSet.setMinimum(value.getMetricValue());
          } else if (isCPUUtilizationCase) {
            double percentage = 100.0 * (valueDiff / timeDiff);
            statisticSet.setSum(percentage * sampleCount);
            statisticSet.setMaximum(percentage);
            statisticSet.setMinimum(percentage);
            datum.setUnit(Units.Percent.toString());
          } else {
            statisticSet.setSum(valueDiff);
            statisticSet.setMaximum(valueDiff / sampleCount);
            statisticSet.setMinimum(valueDiff / sampleCount);
          }
          statisticSet.setSampleCount(sampleCount);
          datum.setStatisticValues(statisticSet);
          datum.setValue(null);
          sequentialMetrics.getRegularMetrics().add(regularMetric);
        }
      }
    }
    sequentialMetrics.setUpdateTimestamp(lastDate);
    sequentialMetrics.setUpdateValue(lastValue);
    return sequentialMetrics;
  }

  private static SortedAbsoluteMetrics sortAbsoluteMetrics(Iterable<AbsoluteMetricQueueItem> dataBatch) {
    SortedAbsoluteMetrics sortedAbsoluteMetrics = new SortedAbsoluteMetrics();
    for (AbsoluteMetricQueueItem item : dataBatch) {
      String accountId = item.getAccountId();
      String namespace = item.getNamespace();
      MetricDatum datum = item.getMetricDatum();
      if (AbsoluteMetricHelper.AWS_EBS_NAMESPACE.equals(namespace)) {
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
          if (AbsoluteMetricHelper.EBS_ABSOLUTE_METRICS.containsKey(datum.getMetricName())) {
            addAccountAbsoluteMetricMap(sortedAbsoluteMetrics, accountId, AbsoluteMetricHelper.AWS_EBS_NAMESPACE, datum.getMetricName(), AbsoluteMetricHelper.VOLUME_ID_DIM_NAME, volumeId, AbsoluteMetricHelper.EBS_ABSOLUTE_METRICS.get(datum.getMetricName()), datum.getTimestamp(), datum.getValue(), datum);
          } else if (AbsoluteMetricHelper.VOLUME_QUEUE_LENGTH_METRIC_NAME.equals(datum.getMetricName())) {
            addAccountAbsoluteMetricMap(sortedAbsoluteMetrics, accountId, AbsoluteMetricHelper.AWS_EBS_NAMESPACE, AbsoluteMetricHelper.VOLUME_QUEUE_LENGTH_PLACEHOLDER_ABSOLUTE_METRIC_NAME, AbsoluteMetricHelper.VOLUME_ID_DIM_NAME, volumeId, AbsoluteMetricHelper.VOLUME_QUEUE_LENGTH_PLACEHOLDER_METRIC_NAME, datum.getTimestamp(), datum.getValue(), datum);
          } else {
            sortedAbsoluteMetrics.getRegularMetrics().add(item);
          }
        }
      } else if (AbsoluteMetricHelper.AWS_EC2_NAMESPACE.equals(namespace)) {
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
        } else if (AbsoluteMetricHelper.EC2_ABSOLUTE_METRICS.containsKey(datum.getMetricName())) {
          addAccountAbsoluteMetricMap(sortedAbsoluteMetrics, accountId, AbsoluteMetricHelper.AWS_EC2_NAMESPACE, datum.getMetricName(), AbsoluteMetricHelper.INSTANCE_ID_DIM_NAME, instanceId, AbsoluteMetricHelper.EC2_ABSOLUTE_METRICS.get(datum.getMetricName()), datum.getTimestamp(), datum.getValue(), datum);
        } else if (AbsoluteMetricHelper.CPU_UTILIZATION_MS_ABSOLUTE_METRIC_NAME.equals(datum.getMetricName())) {
          addAccountAbsoluteMetricMap(sortedAbsoluteMetrics, accountId, AbsoluteMetricHelper.AWS_EC2_NAMESPACE, AbsoluteMetricHelper.CPU_UTILIZATION_MS_ABSOLUTE_METRIC_NAME, AbsoluteMetricHelper.INSTANCE_ID_DIM_NAME, instanceId, AbsoluteMetricHelper.CPU_UTILIZATION_METRIC_NAME, datum.getTimestamp(), datum.getValue(), datum);
        } else {
          sortedAbsoluteMetrics.getRegularMetrics().add(item);
        }
      } else {
        // not really an absolute metric, just leave it alone
        sortedAbsoluteMetrics.getRegularMetrics().add(item);
      }
    }
    return sortedAbsoluteMetrics;
  }

  private static void addAccountAbsoluteMetricMap(SortedAbsoluteMetrics sortedAbsoluteMetrics, String accountId, String namespace, String metricName, String dimensionName, String dimensionValue, String relativeMetricName, Date timestamp, Double lastMetricValue, MetricDatum datum) {
    sortedAbsoluteMetrics.getAbsoluteMetricMap().addEntry(accountId, namespace, metricName, dimensionName, dimensionValue, relativeMetricName, timestamp, lastMetricValue, datum);
  }

  /**
  * Created by ethomas on 7/15/15.
  */
  public static class AbsoluteMetricMap {

    private static class NamespaceMetricNameAndDimension {
      private String namespace;
      private String metricName;
      private String dimensionName;
      private String dimensionValue;

      NamespaceMetricNameAndDimension(String namespace, String metricName, String dimensionName, String dimensionValue) {
        this.namespace = namespace;
        this.metricName = metricName;
        this.dimensionName = dimensionName;
        this.dimensionValue = dimensionValue;
      }

      @Override
      public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        NamespaceMetricNameAndDimension namespaceMetricNameAndDimension = (NamespaceMetricNameAndDimension) o;

        if (dimensionName != null ? !dimensionName.equals(namespaceMetricNameAndDimension.dimensionName) : namespaceMetricNameAndDimension.dimensionName != null)
          return false;
        if (dimensionValue != null ? !dimensionValue.equals(namespaceMetricNameAndDimension.dimensionValue) : namespaceMetricNameAndDimension.dimensionValue != null)
          return false;
        if (metricName != null ? !metricName.equals(namespaceMetricNameAndDimension.metricName) : namespaceMetricNameAndDimension.metricName != null) return false;
        if (namespace != null ? !namespace.equals(namespaceMetricNameAndDimension.namespace) : namespaceMetricNameAndDimension.namespace != null) return false;

        return true;
      }

      @Override
      public int hashCode() {
        int result = namespace != null ? namespace.hashCode() : 0;
        result = 31 * result + (metricName != null ? metricName.hashCode() : 0);
        result = 31 * result + (dimensionName != null ? dimensionName.hashCode() : 0);
        result = 31 * result + (dimensionValue != null ? dimensionValue.hashCode() : 0);
        return result;
      }

      public String getNamespace() {
        return namespace;
      }

      public String getMetricName() {
        return metricName;
      }

      public String getDimensionName() {
        return dimensionName;
      }

      public String getDimensionValue() {
        return dimensionValue;
      }
    }

    private Map<NamespaceMetricNameAndDimension, MetricsAndOtherFields> metricMap = Maps.newHashMap();

    public void addEntry(String accountId, String namespace, String metricName, String dimensionName,
                         String dimensionValue, String relativeMetricName, Date timestamp, Double lastMetricValue, MetricDatum datum) {
      NamespaceMetricNameAndDimension namespaceMetricNameAndDimension = new NamespaceMetricNameAndDimension(namespace,
        metricName, dimensionName, dimensionValue);
      if (!metricMap.containsKey(namespaceMetricNameAndDimension)) {
        metricMap.put(namespaceMetricNameAndDimension, new MetricsAndOtherFields(accountId, relativeMetricName));
      }
      if (relativeMetricName == null) { throw new IllegalArgumentException("RelativeMetricName can not be null"); }
      if (accountId == null) { throw new IllegalArgumentException("AccountId can not be null"); }
      MetricsAndOtherFields value = metricMap.get(namespaceMetricNameAndDimension);
      if (!relativeMetricName.equals(value.getRelativeMetricName())) {
        throw new IllegalArgumentException("RelativeMetricName has two values for " + namespace + "/" + dimensionName + "/" + dimensionValue);
      }
      if (!accountId.equals(value.getAccountId())) {
        throw new IllegalArgumentException("RelativeMetricName has two values for " + namespace + "/" + dimensionName + "/" + dimensionValue);
      }
      value.getMetricDatumMap().put(new TimestampAndMetricValue(timestamp, lastMetricValue), datum);
    }

    public boolean containsKey(String namespace, String metricName, String dimensionName, String dimensionValue) {
      return metricMap.containsKey(new NamespaceMetricNameAndDimension(namespace, metricName, dimensionName, dimensionValue));
    }
    public MetricsAndOtherFields getMetricsAndOtherFields(String namespace, String metricName, String dimensionName, String dimensionValue) {
      return metricMap.get(new NamespaceMetricNameAndDimension(namespace, metricName, dimensionName, dimensionValue));
    }

    public void removeEntries(String namespace, String metricName, String dimensionName, String dimensionValue) {
      metricMap.remove(new NamespaceMetricNameAndDimension(namespace, metricName, dimensionName, dimensionValue));
    }

    public Set<NamespaceMetricNameAndDimension> keySet() {
      return metricMap.keySet();
    }

    public MetricsAndOtherFields get(NamespaceMetricNameAndDimension namespaceMetricNameAndDimension) {
      return metricMap.get(namespaceMetricNameAndDimension);
    }
  }

  static class SimpleAbsoluteMetricHistory {
    public SimpleAbsoluteMetricHistory() {
    }
    private String namespace;
    private String metricName;
    private String dimensionName;
    private String dimensionValue;
    private Date timestamp;
    private Double lastMetricValue;
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
    public Date getTimestamp() {
      return timestamp;
    }
    public void setTimestamp(Date timestamp) {
      this.timestamp = timestamp;
    }
    public Double getLastMetricValue() {
      return lastMetricValue;
    }
    public void setLastMetricValue(Double lastMetricValue) {
      this.lastMetricValue = lastMetricValue;
    }
  }

  private static class MetricsAndOtherFields {
    private String accountId;
    private String relativeMetricName;
    private Map<TimestampAndMetricValue, MetricDatum> metricDatumMap = Maps.newTreeMap();

    MetricsAndOtherFields(String accountId, String relativeMetricName) {
      this.accountId = accountId;
      this.relativeMetricName = relativeMetricName;
    }

    public String getAccountId() {
      return accountId;
    }

    public String getRelativeMetricName() {
      return relativeMetricName;
    }

    public Map<TimestampAndMetricValue, MetricDatum> getMetricDatumMap() {
      return metricDatumMap;
    }
  }

  static class TimestampAndMetricValue implements Comparable<TimestampAndMetricValue> {
    private Date timestamp;
    private Double metricValue;

    TimestampAndMetricValue(Date timestamp, Double metricValue) {
      this.timestamp = timestamp;
      this.metricValue = metricValue;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      TimestampAndMetricValue that = (TimestampAndMetricValue) o;

      if (metricValue != null ? !metricValue.equals(that.metricValue) : that.metricValue != null)
        return false;
      if (timestamp != null ? !timestamp.equals(that.timestamp) : that.timestamp != null) return false;

      return true;
    }

    @Override
    public int hashCode() {
      int result = timestamp != null ? timestamp.hashCode() : 0;
      result = 31 * result + (metricValue != null ? metricValue.hashCode() : 0);
      return result;
    }

    public Date getTimestamp() {
      return timestamp;
    }

    public Double getMetricValue() {
      return metricValue;
    }

    public void setTimestamp(Date timestamp) {
      this.timestamp = timestamp;
    }

    public void setMetricValue(Double metricValue) {
      this.metricValue = metricValue;
    }

    @Override
    public int compareTo(TimestampAndMetricValue that) {
      if (this.timestamp.compareTo(that.timestamp) < 0) {
        return -1;
      } else if (this.timestamp.compareTo(that.timestamp) > 0) {
        return 1;
      }

      if (this.metricValue.compareTo(that.metricValue) < 0) {
        return -1;
      } else if (this.metricValue.compareTo(that.metricValue) > 0) {
        return 1;
      }
      return 0;
    }
  }

  private static class SortedAbsoluteMetrics {
    private List<AbsoluteMetricQueueItem> regularMetrics = Lists.newArrayList();
    private AbsoluteMetricMap absoluteMetricMap = new AbsoluteMetricMap();

    SortedAbsoluteMetrics() {
    }

    public List<AbsoluteMetricQueueItem> getRegularMetrics() {
      return regularMetrics;
    }

    public AbsoluteMetricMap getAbsoluteMetricMap() {
      return absoluteMetricMap;
    }
  }

  private static class SequentialMetrics {
    private Collection<AbsoluteMetricQueueItem> regularMetrics = Lists.newArrayList();

    private Date updateTimestamp;
    private Double updateValue;

    public void setUpdateTimestamp(Date updateTimestamp) {
      this.updateTimestamp = updateTimestamp;
    }

    public void setUpdateValue(Double updateValue) {
      this.updateValue = updateValue;
    }

    public Collection<AbsoluteMetricQueueItem> getRegularMetrics() {
      return regularMetrics;
    }

    public Date getUpdateTimestamp() {
      return updateTimestamp;
    }

    public Double getUpdateValue() {
      return updateValue;
    }
  }
}
