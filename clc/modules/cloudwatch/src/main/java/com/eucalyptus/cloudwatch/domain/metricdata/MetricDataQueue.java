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
import com.eucalyptus.cloudwatch.Dimensions;
import com.eucalyptus.cloudwatch.MetricDatum;
import com.eucalyptus.cloudwatch.StatisticSet;
import com.eucalyptus.cloudwatch.domain.absolute.AbsoluteMetricHelper;
import com.eucalyptus.cloudwatch.domain.absolute.AbsoluteMetricHelper.MetricDifferenceInfo;
import com.eucalyptus.cloudwatch.domain.listmetrics.ListMetricManager;
import com.eucalyptus.cloudwatch.domain.metricdata.MetricEntity.MetricType;
import com.eucalyptus.cloudwatch.domain.metricdata.MetricEntity.Units;
import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableMap;
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
      try {
        List<MetricQueueItem> dataBatch = Lists.newArrayList();
        dataQueue.drainTo(dataBatch);
        dataBatch = aggregate(dataBatch);
        for (final MetricQueueItem metricData : dataBatch) {
          MetricManager.addMetric(metricData.getAccountId(), 
              metricData.getMetricName(), metricData.getNamespace(), 
              metricData.getDimensionMap(), metricData.getMetricType(), 
              metricData.getUnits(), metricData.getTimestamp(), 
              metricData.getSampleSize(), metricData.getSampleMax(), 
              metricData.getSampleMin(), metricData.getSampleSum());
          ListMetricManager.addMetric(metricData.getAccountId(), metricData.getMetricName(),
              metricData.getNamespace(), metricData.getDimensionMap(), metricData.getMetricType());
        }
      } catch (Exception ex) {
        ex.printStackTrace();
        LOG.error(ex,ex);
      }
    }
  };

  static {
    dataFlushTimer.scheduleWithFixedDelay(safeRunner, 0, 1, TimeUnit.MINUTES);
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

  private static final Map<String, String> EBS_ABSOLUTE_METRICS = 
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

  private static final Map<String, String> EC2_ABSOLUTE_METRICS = 
      new ImmutableMap.Builder<String, String>()
      .put("DiskReadOpsAbsolute", "DiskReadOps")
      .put("DiskWriteOpsAbsolute", "DiskWriteOps")
      .put("DiskReadBytesAbsolute", "DiskReadBytes")
      .put("DiskWriteBytesAbsolute", "DiskWriteBytes")
      .put("NetworkInExternalAbsolute", "NetworkIn") // NetworkIn and NetworkInExternal are two separate metrics but need to be combined
      .put("NetworkInAbsolute", "NetworkIn") 
      .put("NetworkOutExternalAbsolute", "NetworkOut") // NetworkOut and NetworkOutExternal are two separate metrics but need to be combined
      .put("NetworkOutAbsolute", "NetworkOut") 
      .build();

  public void insertMetricData(final String ownerAccountId, final String nameSpace,
      final List<MetricDatum> metricDatum, final MetricType metricType) {
    List<MetricDatum> dataToInsert = new ArrayList<MetricDatum>(); 
    // Some points do not actually go in.  If a data point represents an absolute value, the first one does not go in.
    // Also, some data points are added while we go through the list (derived metrics)
    Date now = new Date();
    for (final MetricDatum datum : metricDatum) {
      LOG.trace("Received metric datum: " + nameSpace + " " + datum.getMetricName() + " " + datum.getTimestamp());
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
      // NetworkInExternal (added as an additional NetworkIn metric)
      // NetworkOut
      // NetworkOutExternal (added as an additional NetworkOut metric)
    
      if ("AWS/EBS".equals(nameSpace) && metricType == MetricType.System) {
        String volumeId = null;
        if ((datum.getDimensions() != null) && (datum.getDimensions().getMember() != null)) {
          for (Dimension dimension: datum.getDimensions().getMember()) {
            if ("VolumeId".equals(dimension.getName())) {
              volumeId = dimension.getValue();
            }
          }
        }
        if (EBS_ABSOLUTE_METRICS.containsKey(datum.getMetricName())) {
          // we check if the point below is a 'first' point, or maybe a point in the past.  Either case reject it.
          if (!adjustAbsoluteVolumeStatisticSet(datum, datum.getMetricName(), EBS_ABSOLUTE_METRICS.get(datum.getMetricName()), volumeId)) continue; 
        }
        // special cases
        // 1) VolumeThroughputPercentage -- this is 100% for provisioned volumes, and we need to insert a
        //                                  data point for every timestamp that a volume event occurs.
        //                                  To make sure we don't duplicate the effort, we choose one event at random, VolumeReadOps,
        //                                  and create this new metric arbitrarily
        if ("VolumeReadOps".equals(datum.getMetricName())) { // special case
          dataToInsert.add(createVolumeThroughputMetric(datum));
        }
        // 2) VolumeIdleTime -- we piggy back off of the metric we don't need VolumeTotalReadWriteTime, and convert it to VolumeIdleTime
        if ("VolumeTotalReadWriteTime".equals(datum.getMetricName())) {
          convertVolumeTotalReadWriteTimeToVolumeIdleTime(datum);
        }
        // 3) VolumeQueueLength -- this one comes in essentially correct, but we don't have a time duration for it, so we piggy back off
        //                         the absolute metric framework
        if ("VolumeQueueLength".equals(datum.getMetricName())) {
          if (!adjustAbsoluteVolumeQueueLengthStatisticSet(datum, volumeId)) continue;
        }
      }
      
      if ("AWS/EC2".equals(nameSpace) && metricType == MetricType.System) {
        String instanceId = null;
        if ((datum.getDimensions() != null) && (datum.getDimensions().getMember() != null)) {
          for (Dimension dimension: datum.getDimensions().getMember()) {
            if ("InstanceId".equals(dimension.getName())) {
              instanceId = dimension.getValue();
            }
          }
        }
        if (EC2_ABSOLUTE_METRICS.containsKey(datum.getMetricName())) {
          if (!adjustAbsoluteInstanceStatisticSet(datum, datum.getMetricName(), EC2_ABSOLUTE_METRICS.get(datum.getMetricName()), instanceId)) continue; 
        } else if ("CPUUtilizationMSAbsolute".equals(datum.getMetricName())) { // special case
          // we check if the point below is a 'first' point, or maybe a point in the past.  Either case reject it.
          if (!adjustAbsoluteInstanceCPUStatisticSet(datum, "CPUUtilizationMSAbsolute", "CPUUtilization", instanceId)) continue;
        } 
      }        
      dataToInsert.add(datum); // this data point is ok
    }
    for (final MetricDatum datum : dataToInsert) {
      scrub(datum, now);
      final ArrayList<Dimension> dimensions = datum.getDimensions().getMember(); 
      queue(new Supplier<MetricQueueItem>() {
        @Override
        public MetricQueueItem get() {
          MetricQueueItem metricMetadata = new MetricQueueItem();
          metricMetadata.setAccountId(ownerAccountId);
          metricMetadata.setMetricName(datum.getMetricName());
          metricMetadata.setNamespace(nameSpace);
          metricMetadata.setDimensionMap(makeDimensionMap(dimensions));
          metricMetadata.setMetricType(metricType);
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
          return metricMetadata;
        }

      });
    }
  }

  private boolean adjustAbsoluteVolumeQueueLengthStatisticSet(
      MetricDatum datum, String volumeId) {
    // the metric value is correct, we just need a statistic set with the sample count.
    // to get this we create a placeholder absolute metric, value always 0, just to get time duration/sample count
    MetricDatum absolutePlaceHolder = new MetricDatum();
    absolutePlaceHolder.setMetricName("VolumeQueueLengthPlaceHolderAbsolute");
    absolutePlaceHolder.setValue(0.0);
    absolutePlaceHolder.setTimestamp(datum.getTimestamp());
    if (!adjustAbsoluteVolumeStatisticSet(absolutePlaceHolder, absolutePlaceHolder.getMetricName(), "VolumeQueueLengthPlaceHolder", volumeId)) return false;
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

  private void convertVolumeTotalReadWriteTimeToVolumeIdleTime(final MetricDatum datum) {
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

  private MetricDatum createVolumeThroughputMetric(MetricDatum datum) {
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
    for (Dimension dimension: datum.getDimensions().getMember()) {
      Dimension vtpDimension = new Dimension();
      vtpDimension.setName(dimension.getName());
      vtpDimension.setValue(dimension.getValue());
      vtpDimensionsMember.add(vtpDimension);
    }
    vtpDimensions.setMember(vtpDimensionsMember);
    vtpDatum.setDimensions(vtpDimensions);
    return vtpDatum;
  }

  private boolean adjustAbsoluteInstanceCPUStatisticSet(MetricDatum datum, String absoluteMetricName,
      String relativeMetricName, String instanceId) {
    MetricDifferenceInfo info = AbsoluteMetricHelper.calculateDifferenceSinceLastEvent("AWS/EC2", absoluteMetricName, "InstanceId", instanceId, datum.getTimestamp(), datum.getValue());
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


  
  
  
  
  private boolean adjustAbsoluteInstanceStatisticSet(MetricDatum datum, String absoluteMetricName,
      String relativeMetricName, String instanceId) {
    if (instanceId == null) return false;
    MetricDifferenceInfo info = AbsoluteMetricHelper.calculateDifferenceSinceLastEvent("AWS/EC2", absoluteMetricName, "InstanceId", instanceId, datum.getTimestamp(), datum.getValue());
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

  
  private boolean adjustAbsoluteVolumeStatisticSet(MetricDatum datum,
      String absoluteMetricName, String relativeMetricName, String volumeId) {
    if (volumeId == null) return false;
    MetricDifferenceInfo info = AbsoluteMetricHelper.calculateDifferenceSinceLastEvent("AWS/EBS", absoluteMetricName, "VolumeId", volumeId, datum.getTimestamp(), datum.getValue());
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
