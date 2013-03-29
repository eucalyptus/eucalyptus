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
import com.eucalyptus.cloudwatch.domain.absolute.AbsoluteMetricHelper;
import com.eucalyptus.cloudwatch.domain.absolute.AbsoluteMetricHelper.MetricDifferenceInfo;
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
            MetricManager.addMetric(metricData.getAccountId(), 
                metricData.getMetricName(), metricData.getNamespace(), 
                metricData.getDimensionMap(), metricData.getMetricType(), 
                metricData.getUnits(), metricData.getTimestamp(), 
                metricData.getSampleSize(), metricData.getSampleMax(), 
                metricData.getSampleMin(), metricData.getSampleSum());
            ListMetricManager.addMetric(metricData.getAccountId(), metricData.getMetricName(),
                metricData.getNamespace(), metricData.getDimensionMap(), metricData.getMetricType());
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
  public void insertMetricData(final String ownerAccountId, final String nameSpace,
      final List<MetricDatum> metricDatum, final MetricType metricType) {
    List<MetricDatum> extraData = new ArrayList<MetricDatum>(); // some metrics are derived
    Date now = new Date();
    for (final MetricDatum datum : metricDatum) {
      // Deal with some absolute metrics
      // VolumeReadOps
      // VolumeWriteOps
      // VolumeReadBytes
      // VolumeWriteBytes
      // VolumeTotalReadTime
      // VolumeTotalWriteTime
      // CPUUtilization (special case)
      if ("AWS/EBS".equals(nameSpace) && metricType == MetricType.System) {
        String volumeId = null;
        if ((datum.getDimensions() != null) && (datum.getDimensions().getMember() != null)) {
          for (Dimension dimension: datum.getDimensions().getMember()) {
            if ("VolumeId".equals(dimension.getName())) {
              volumeId = dimension.getValue();
            }
          }
          if (volumeId != null) {
            if ("VolumeReadOpsTotal".equals(datum.getMetricName())) {
              if (!adjustAbsoluteVolumeValue(datum, "VolumeReadOpsTotal", "VolumeReadOps", volumeId)) continue;
              extraData.add(createVolumeThroughputMetric(datum));
            } 
            if ("VolumeWriteOpsTotal".equals(datum.getMetricName())) {
              if (!adjustAbsoluteVolumeValue(datum, "VolumeWriteOpsTotal", "VolumeWriteOps", volumeId)) continue;
            } 
            if ("VolumeReadBytesTotal".equals(datum.getMetricName())) {
              if (!adjustAbsoluteVolumeValue(datum, "VolumeReadBytesTotal", "VolumeReadBytes", volumeId)) continue;
            } 
            if ("VolumeWriteBytesTotal".equals(datum.getMetricName())) {
              if (!adjustAbsoluteVolumeValue(datum, "VolumeWriteBytesTotal", "VolumeWriteBytes", volumeId)) continue;
            } 
            if ("TotalVolumeReadTimeTotal".equals(datum.getMetricName())) {
              if (!adjustAbsoluteVolumeValue(datum, "TotalVolumeReadTimeTotal", "TotalVolumeReadTime", volumeId)) continue;
            } 
            if ("TotalVolumeWriteTimeTotal".equals(datum.getMetricName())) {
              if (!adjustAbsoluteVolumeValue(datum, "TotalVolumeWriteTimeTotal", "TotalVolumeWriteTime", volumeId)) continue;
            }
          }
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
          if (instanceId != null) {
            if ("CPUUtilizationMS".equals(datum.getMetricName())) {
              if (!adjustAbsoluteCPUValue(datum, "CPUUtilizationMS", "CPUUtilization", instanceId)) continue;
            } 
          }
        }        
      }
    }
    metricDatum.addAll(extraData);
    for (final MetricDatum datum : metricDatum) {
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

  private MetricDatum createVolumeThroughputMetric(MetricDatum datum) {
    // add volume throughput percentage.  (The guess is that there will be a set of volume metrics.  
    // Attach it to one so it will be sent as many times as the others.
    // add one
    MetricDatum vtpDatum = new MetricDatum();
    vtpDatum.setMetricName("VolumeThroughputPercentage");
    vtpDatum.setTimestamp(datum.getTimestamp());
    vtpDatum.setUnit(Units.Percent.toString());
    vtpDatum.setValue(100.0); // Any time we have a volume data, this value is 100%
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

  private boolean adjustAbsoluteCPUValue(MetricDatum datum, String absoluteMetricName,
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
      datum.setValue(percentage);
      datum.setUnit(Units.Percent.toString());
      return true; //don't continue;
    }
    return false; // continue
  }

  private boolean adjustAbsoluteVolumeValue(MetricDatum datum,
      String absoluteMetricName, String relativeMetricName, String volumeId) {
    MetricDifferenceInfo info = AbsoluteMetricHelper.calculateDifferenceSinceLastEvent("AWS/EBS", absoluteMetricName, "VolumeId", volumeId, datum.getTimestamp(), datum.getValue());
    if (info != null) {
      datum.setMetricName(relativeMetricName);
      datum.setValue(info.getValueDifference());
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

  private List<MetricQueueItem> collectNetworkIO (List<MetricQueueItem> dataBatch) {
    
    //List<MetricQueueItem> 
    
    for(MetricQueueItem networkItem : dataBatch) {
	if ( networkItem.getMetricType() == MetricType.System && networkItem.getMetricName().startsWith("Network") ) {
	    
	}
    }
      
    return dataBatch;    
  }
}
