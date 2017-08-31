/*************************************************************************
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

package com.eucalyptus.cluster.callback.reporting;

import java.util.ArrayList;
import java.util.Date;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import javax.persistence.EntityTransaction;

import com.eucalyptus.cloudwatch.common.internal.domain.metricdata.Units;
import com.eucalyptus.cloudwatch.common.msgs.Dimension;
import com.eucalyptus.cloudwatch.common.msgs.Dimensions;
import com.eucalyptus.cloudwatch.common.msgs.MetricDatum;
import com.eucalyptus.cloudwatch.common.msgs.StatisticSet;
import com.eucalyptus.entities.TransactionResource;
import com.google.common.collect.ImmutableMap;
import org.apache.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;

import com.eucalyptus.cluster.callback.reporting.DefaultAbsoluteMetricConverter.AbsoluteMetricCache;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.records.Logs;

public class AbsoluteMetricHelper {
  static final double TOLERANCE = 0.0000001; // arbitrary to check double "equality"
  static final Map<String, String> EBS_ABSOLUTE_METRICS =
    new ImmutableMap.Builder<String, String>()
      .put("VolumeReadOpsAbsolute", "VolumeReadOps")
      .put("VolumeWriteOpsAbsolute", "VolumeWriteOps")
      .put("VolumeReadBytesAbsolute", "VolumeReadBytes")
      .put("VolumeWriteBytesAbsolute", "VolumeWriteBytes")
      .put("VolumeConsumedReadWriteOpsAbsolute", "VolumeConsumedReadWriteOps")
      .put("VolumeTotalReadTimeAbsolute", "VolumeTotalReadTime")
      .put("VolumeTotalWriteTimeAbsolute", "VolumeTotalWriteTime")
      .put("VolumeTotalReadWriteTimeAbsolute", "VolumeTotalReadWriteTime")
      .build();
  static final Map<String, String> EC2_ABSOLUTE_METRICS =
    new ImmutableMap.Builder<String, String>()
      .put("DiskReadOpsAbsolute", "DiskReadOps")
      .put("DiskWriteOpsAbsolute", "DiskWriteOps")
      .put("DiskReadBytesAbsolute", "DiskReadBytes")
      .put("DiskWriteBytesAbsolute", "DiskWriteBytes")
      .put("NetworkInAbsolute", "NetworkIn")
      .put("NetworkOutAbsolute", "NetworkOut")
      .build();
  static final long MAX_DIFFERENCE_DURATION_MS = TimeUnit.MINUTES.toMillis(15L); // 15 minutes, also max reporting time
  static final String AWS_EBS_NAMESPACE = "AWS/EBS";
  static final String VOLUME_ID_DIM_NAME = "VolumeId";
  static final String AWS_EC2_NAMESPACE = "AWS/EC2";
  static final String INSTANCE_ID_DIM_NAME = "InstanceId";
  static final String VOLUME_READ_OPS_METRIC_NAME = "VolumeReadOps";
  static final String VOLUME_QUEUE_LENGTH_METRIC_NAME = "VolumeQueueLength";
  static final String VOLUME_TOTAL_READ_WRITE_TIME_METRIC_NAME = "VolumeTotalReadWriteTime";
  static final String VOLUME_QUEUE_LENGTH_PLACEHOLDER_METRIC_NAME = "VolumeQueueLengthPlaceHolder";

  static final String VOLUME_QUEUE_LENGTH_PLACEHOLDER_ABSOLUTE_METRIC_NAME = "VolumeQueueLengthPlaceHolderAbsolute";


  static final String CPU_UTILIZATION_MS_ABSOLUTE_METRIC_NAME = "CPUUtilizationMSAbsolute";
  static final String CPU_UTILIZATION_METRIC_NAME = "CPUUtilization";
  private static final Logger LOG = Logger.getLogger(AbsoluteMetricHelper.class);

  static void convertVolumeTotalReadWriteTimeToVolumeIdleTime(final MetricDatum datum) {
    // we convert this to VolumeIdleTime = Period Length - VolumeTotalReadWriteTime on the period (though won't be negative)
    datum.setMetricName("VolumeIdleTime");
    double totalReadWriteTime = datum.getStatisticValues().getSum(); // value is in seconds
    double totalPeriodTime = 60.0 * datum.getStatisticValues().getSampleCount();
    double totalIdleTime = totalPeriodTime - totalReadWriteTime;
    if (totalIdleTime < 0) totalIdleTime = 0; // if we have read and written more than in the period, don't go negative
    datum.getStatisticValues().setSum(totalIdleTime);
    double averageIdleTime = totalIdleTime / datum.getStatisticValues().getSampleCount();
    datum.getStatisticValues().setMaximum(averageIdleTime);
    datum.getStatisticValues().setMinimum(averageIdleTime);
  }

  static AbsoluteMetricQueueItem createVolumeThroughputMetric(String accountId, String nameSpace, MetricDatum datum) {
    // add volume throughput percentage.  (The guess is that there will be a set of volume metrics.
    // Attach it to one so it will be sent as many times as the others.
    // add one
    MetricDatum vtpDatum = new MetricDatum();
    vtpDatum.setMetricName("VolumeThroughputPercentage");
    vtpDatum.setTimestamp(datum.getTimestamp());
    vtpDatum.setUnit(Units.Percent.toString());
    // should be 100% but weigh it the same
    if (datum.getValue() != null) {
      vtpDatum.setValue(100.0); // Any time we have a volume data, this value is 100%
    } else if (datum.getStatisticValues() != null) {
      StatisticSet statisticSet = new StatisticSet();
      statisticSet.setMaximum(100.0);
      statisticSet.setMinimum(100.0);
      statisticSet.setSum(100.0 * datum.getStatisticValues().getSampleCount());
      statisticSet.setSampleCount(datum.getStatisticValues().getSampleCount());
      vtpDatum.setStatisticValues(statisticSet);
    }
    // use the same dimensions as current metric
    Dimensions vtpDimensions = new Dimensions();
    ArrayList<Dimension> vtpDimensionsMember = new ArrayList<Dimension>();
    if ( datum.getDimensions( ) != null ) for ( final Dimension dimension: datum.getDimensions( ).getMember( ) ) {
      Dimension vtpDimension = new Dimension();
      vtpDimension.setName(dimension.getName());
      vtpDimension.setValue(dimension.getValue());
      vtpDimensionsMember.add(vtpDimension);
    }
    vtpDimensions.setMember(vtpDimensionsMember);
    vtpDatum.setDimensions(vtpDimensions);
    AbsoluteMetricQueueItem vtpQueueItem = new AbsoluteMetricQueueItem();
    vtpQueueItem.setAccountId(accountId);
    vtpQueueItem.setNamespace(nameSpace);
    vtpQueueItem.setMetricDatum(vtpDatum);
    return vtpQueueItem;
  }

  public static class MetricDifferenceInfo {
    private Double valueDifference;
    private Long elapsedTimeInMillis;
    public Double getValueDifference() {
      return valueDifference;
    }
    public Long getElapsedTimeInMillis() {
      return elapsedTimeInMillis;
    }
    public MetricDifferenceInfo(Double valueDifference, Long elapsedTimeInMillis) {
      this.valueDifference = valueDifference;
      this.elapsedTimeInMillis = elapsedTimeInMillis;
    }
    
  }
  public static MetricDifferenceInfo calculateDifferenceSinceLastEvent(AbsoluteMetricCache cache, String namespace, String metricName, String dimensionName, String dimensionValue, Date newTimestamp, Double newMetricValue) {
    LOG.trace("namespace="+namespace+",metricName="+metricName+",dimensionName="+dimensionName+",dimensionValue="+dimensionValue+",newTimestamp="+newTimestamp+",newMetricValue="+newMetricValue);
    MetricDifferenceInfo returnValue = null;
    AbsoluteMetricHistory lastEntity = cache.lookup(namespace, metricName, dimensionName, dimensionValue);
    if (lastEntity == null) {
      // first data point, add it and return null (nothing to diff against)
      LOG.trace("First entry");
      lastEntity = new AbsoluteMetricHistory();
      lastEntity.setNamespace(namespace);
      lastEntity.setMetricName(metricName);
      lastEntity.setDimensionName(dimensionName);
      lastEntity.setDimensionValue(dimensionValue);
      lastEntity.setTimestamp(newTimestamp);
      lastEntity.setLastMetricValue(newMetricValue);
      Entities.persist(lastEntity);
      cache.put(namespace, metricName, dimensionName, dimensionValue, lastEntity);
      returnValue =  null;
    } else {
      long elapsedTimeInMillis = newTimestamp.getTime() - lastEntity.getTimestamp().getTime();
      LOG.trace("lastTimestamp="+lastEntity.getTimestamp());
      double valueDifference = newMetricValue - lastEntity.getLastMetricValue();
      if (elapsedTimeInMillis < 0) {
        LOG.trace("earlier point, kicking out");
        // a negative value of elapsedTimeInMillis means this data point is not useful
        return null;
      } else if (elapsedTimeInMillis == 0) {
        if (Math.abs(valueDifference) > TOLERANCE) {
          LOG.warn("Getting different values " + newMetricValue + " and " + lastEntity.getLastMetricValue() + " for absolute metric " + metricName + " at the same timestamp " + newTimestamp + ", keeping the second value.");
        }
        return null; // not a useful data point either
      } else if (elapsedTimeInMillis > MAX_DIFFERENCE_DURATION_MS) {
        // Too much time has passed, a useful data point, but we will not report the 'difference'.  We will reset.
        LOG.trace("too much time has passed, (" + elapsedTimeInMillis + " ms), starting over");
        lastEntity.setTimestamp(newTimestamp);
        lastEntity.setLastMetricValue(newMetricValue);
        returnValue = null;
      } else if (elapsedTimeInMillis > 0) { 
        lastEntity.setTimestamp(newTimestamp);
        lastEntity.setLastMetricValue(newMetricValue);
        if (valueDifference < -TOLERANCE) { // value has gone "down" (or down more than the TOLERANCE)
          // if the value difference is negative (i.e. has gone down, the assumption is that the NC has restarted, and the new
          // value started from some time in the past.  Best thing to do here is either assume it is a first point again, or
          // assume the previous point had a 0 value.  Not sure which is the better choice, but for now, we will make it a "first"
          // point again
          returnValue = null;
        } else { // truncate differences within AbsoluteMetricCommon.TOLERANCE to zero
          if (Math.abs(valueDifference) < TOLERANCE) {
            valueDifference = 0.0;
          } 
          returnValue = new MetricDifferenceInfo(valueDifference, elapsedTimeInMillis);
        }
      }
      if (returnValue != null) {
        LOG.trace("new values=valueDifference="+valueDifference+",elapsedTimeInMillis="+elapsedTimeInMillis);
      } else {
        LOG.trace("sending null value out");
      }
    }
    return returnValue;
  }
 
  public static MetricDifferenceInfo calculateDifferenceSinceLastEvent(String namespace, String metricName, String dimensionName, String dimensionValue, Date newTimestamp, Double newMetricValue) {
    LOG.trace("namespace="+namespace+",metricName="+metricName+",dimensionName="+dimensionName+",dimensionValue="+dimensionValue+",newTimestamp="+newTimestamp+",newMetricValue="+newMetricValue);
    MetricDifferenceInfo returnValue = null;
    try (final TransactionResource db = Entities.transactionFor(AbsoluteMetricHistory.class)) {
      Criteria criteria = Entities.createCriteria(AbsoluteMetricHistory.class)
          .add( Restrictions.eq( "namespace", namespace ) )
          .add( Restrictions.eq( "metricName", metricName ) )
          .add( Restrictions.eq( "dimensionName", dimensionName ) )
          .add( Restrictions.eq( "dimensionValue", dimensionValue ) );
      AbsoluteMetricHistory lastEntity = (AbsoluteMetricHistory) criteria.uniqueResult();
      if (lastEntity == null) {
        // first data point, add it and return null (nothing to diff against)
        LOG.trace("First entry");
        lastEntity = new AbsoluteMetricHistory();
        lastEntity.setNamespace(namespace);
        lastEntity.setMetricName(metricName);
        lastEntity.setDimensionName(dimensionName);
        lastEntity.setDimensionValue(dimensionValue);
        lastEntity.setTimestamp(newTimestamp);
        lastEntity.setLastMetricValue(newMetricValue);
        Entities.persist(lastEntity);
        returnValue =  null;
      } else {
        long elapsedTimeInMillis = newTimestamp.getTime() - lastEntity.getTimestamp().getTime();
        LOG.trace("lastTimestamp="+lastEntity.getTimestamp());
        double valueDifference = newMetricValue - lastEntity.getLastMetricValue();
        if (elapsedTimeInMillis < 0) {
          LOG.trace("earlier point, kicking out");
          // a negative value of elapsedTimeInMillis means this data point is not useful
          returnValue = null;
        } else if (elapsedTimeInMillis == 0) {
          if (Math.abs(valueDifference) > TOLERANCE) {
            LOG.warn("Getting different values " + newMetricValue + " and " + lastEntity.getLastMetricValue() + " for absolute metric " + metricName + " at the same timestamp " + newTimestamp + ", keeping the second value.");
          }
          returnValue = null; // not a useful data point either
        } else if (elapsedTimeInMillis > MAX_DIFFERENCE_DURATION_MS) {
          // Too much time has passed, a useful data point, but we will not report the 'difference'.  We will reset.
          LOG.trace("too much time has passed, (" + elapsedTimeInMillis + " ms), starting over");
          lastEntity.setTimestamp(newTimestamp);
          lastEntity.setLastMetricValue(newMetricValue);
          returnValue = null;
        } else if (elapsedTimeInMillis > 0) { 
          lastEntity.setTimestamp(newTimestamp);
          lastEntity.setLastMetricValue(newMetricValue);
          if (valueDifference < -TOLERANCE) { // value has gone "down" (or down more than the TOLERANCE)
            // if the value difference is negative (i.e. has gone down, the assumption is that the NC has restarted, and the new
            // value started from some time in the past.  Best thing to do here is either assume it is a first point again, or
            // assume the previous point had a 0 value.  Not sure which is the better choice, but for now, we will make it a "first"
            // point again
            returnValue = null;
          } else { // truncate differences within AbsoluteMetricCommon.TOLERANCE to zero
            if (Math.abs(valueDifference) < TOLERANCE) {
              valueDifference = 0.0;
            } 
            returnValue = new MetricDifferenceInfo(valueDifference, elapsedTimeInMillis);
          }
        }
        if (returnValue != null) {
          LOG.trace("new values=valueDifference="+valueDifference+",elapsedTimeInMillis="+elapsedTimeInMillis);
        } else {
          LOG.trace("sending null value out");
        }
      }
      db.commit();
    }
    return returnValue;
  }

  /**
   * Delete all absolute metric history before a certain date
   * @param before the date to delete before (inclusive)
   */
  public static void deleteAbsoluteMetricHistory(Date before) {
    try (final TransactionResource db = Entities.transactionFor(AbsoluteMetricHistory.class)) {
      Map<String, Date> criteria = new HashMap<String, Date>();
      criteria.put("before", before);
      Entities.deleteAllMatching(AbsoluteMetricHistory.class, "WHERE timestamp < :before", criteria);
      db.commit();
    }
  }
}  
