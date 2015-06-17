/*************************************************************************
 * Copyright 2009-2015 Eucalyptus Systems, Inc.
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
package com.eucalyptus.cluster.callback.cloudwatch;

import com.eucalyptus.bootstrap.Bootstrap;
import com.eucalyptus.cloudwatch.common.CloudWatch;
import com.eucalyptus.cloudwatch.common.CloudWatchBackend;
import com.eucalyptus.cloudwatch.common.internal.metricdata.Units;
import com.eucalyptus.cloudwatch.common.msgs.Dimension;
import com.eucalyptus.cloudwatch.common.msgs.Dimensions;
import com.eucalyptus.cloudwatch.common.msgs.MetricData;
import com.eucalyptus.cloudwatch.common.msgs.MetricDatum;
import com.eucalyptus.cloudwatch.common.msgs.PutMetricDataResponseType;
import com.eucalyptus.cloudwatch.common.msgs.PutMetricDataType;
import com.eucalyptus.cloudwatch.common.msgs.StatisticSet;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.Topology;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.records.Logs;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.async.AsyncRequests;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import edu.ucsb.eucalyptus.msgs.BaseMessage;
import org.apache.log4j.Logger;

import javax.persistence.EntityTransaction;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import com.eucalyptus.cluster.callback.cloudwatch.AbsoluteMetricHelper.MetricDifferenceInfo;
import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;

/**
 * Created by ethomas on 6/16/15.
 */
public class AbsoluteMetricQueue {

  static {
    ScheduledExecutorService dbCleanupService = Executors
      .newSingleThreadScheduledExecutor();
    dbCleanupService.scheduleAtFixedRate(new DBCleanupService(), 1, 24,
      TimeUnit.HOURS);
  }


  private static final Logger LOG = Logger.getLogger(AbsoluteMetricQueue.class);
  final static LinkedBlockingQueue<AbsoluteMetricQueueItem> dataQueue = new LinkedBlockingQueue<AbsoluteMetricQueueItem>();

  private static final ScheduledExecutorService dataFlushTimer = Executors
    .newSingleThreadScheduledExecutor();

  private static AbsoluteMetricQueue singleton = getInstance();

  public static AbsoluteMetricQueue getInstance() {
    synchronized (AbsoluteMetricQueue.class) {
      if (singleton == null)
        singleton = new AbsoluteMetricQueue();
    }
    return singleton;
  }

  private static Runnable safeRunner = new Runnable() {
    @Override
    public void run() {
      long before = System.currentTimeMillis();
      try {
        List<AbsoluteMetricQueueItem> dataBatch = Lists.newArrayList();
        dataQueue.drainTo(dataBatch);
        //        dataQueue.drainTo(dataBatch, 15000);
        LOG.debug("Cluster:Timing:dataBatch.size()=" + dataBatch.size());
        long t1 = System.currentTimeMillis();
        dataBatch = dealWithAbsoluteMetrics(dataBatch);
        long t2 = System.currentTimeMillis();
        LOG.debug("Cluster:Timing:dataBatch.foldMetrics():time="+(t2-t1));
        List<PutMetricDataType> putMetricDataTypeList =convertToPutMetricDataList(dataBatch);
        long t3 = System.currentTimeMillis();
        LOG.debug("Cluster:Timing:dataBatch.convertToPutMetricDataList():time="+(t3-t2));
        putMetricDataTypeList = CloudWatchHelper.consolidatePutMetricDataList(putMetricDataTypeList);
        long t4 = System.currentTimeMillis();
        LOG.debug("Cluster:Timing:dataBatch.consolidatePutMetricDataList():time="+(t4-t3));
        callPutMetricData(putMetricDataTypeList);
        long t5 = System.currentTimeMillis();
        LOG.debug("Timing:ListMetricManager.callPutMetricData():time="+(t5-t4));
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

  private static List<PutMetricDataType> convertToPutMetricDataList(List<AbsoluteMetricQueueItem> dataBatch) {
    final List<PutMetricDataType> putMetricDataTypeList = Lists.newArrayList();
    for (AbsoluteMetricQueueItem item: dataBatch) {
      PutMetricDataType putMetricDataType = new PutMetricDataType();
      putMetricDataType.setUserId(item.getAccountId());
      putMetricDataType.markPrivileged();
      putMetricDataType.setNamespace(item.getNamespace());
      MetricData metricData = new MetricData();
      ArrayList member = Lists.newArrayList(item.getMetricDatum());
      metricData.setMember(member);
      putMetricDataType.setMetricData(metricData);
      putMetricDataTypeList.add(putMetricDataType);
    }
    return putMetricDataTypeList;
  }

  private static ServiceConfiguration createServiceConfiguration() {
    return Topology.lookup(CloudWatch.class);
  }

  private static void callPutMetricData(List<PutMetricDataType> putMetricDataList) throws Exception {
    ServiceConfiguration serviceConfiguration = createServiceConfiguration();
    for (PutMetricDataType putMetricData: putMetricDataList) {
      BaseMessage reply = AsyncRequests.dispatch(serviceConfiguration, putMetricData).get();
      if (!(reply instanceof PutMetricDataResponseType)) {
        throw new EucalyptusCloudException("Unable to send put metric data to cloud watch");
      }
    }
  }

  static {
    dataFlushTimer.scheduleAtFixedRate(safeRunner, 0, 1, TimeUnit.MINUTES);
  }

  protected static List<AbsoluteMetricQueueItem> dealWithAbsoluteMetrics(
    List<AbsoluteMetricQueueItem> dataBatch) {
    List<AbsoluteMetricQueueItem> dataToInsert = new ArrayList<AbsoluteMetricQueueItem>();
    EntityTransaction db = Entities.get(AbsoluteMetricHistory.class);
    try {
      AbsoluteMetricCache cache = new AbsoluteMetricCache(db);
      // Some points do not actually go in.  If a data point represents an absolute value, the first one does not go in.
      // Also, some data points are added while we go through the list (derived metrics)

      for (final AbsoluteMetricQueueItem item : dataBatch) {
        String accountId = item.getAccountId();
        String nameSpace = item.getNamespace();
        MetricDatum datum = item.getMetricDatum();
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

        if ("AWS/EBS".equals(nameSpace)) {
          String volumeId = null;
          if ((datum.getDimensions() != null) && (datum.getDimensions().getMember() != null)) {
            for (Dimension dimension: datum.getDimensions().getMember()) {
              if ("VolumeId".equals(dimension.getName())) {
                volumeId = dimension.getValue();
                cache.load(nameSpace, "VolumeId", volumeId);
              }
            }
          }
          if (EBS_ABSOLUTE_METRICS.containsKey(datum.getMetricName())) {
            // we check if the point below is a 'first' point, or maybe a point in the past.  Either case reject it.
            if (!adjustAbsoluteVolumeStatisticSet(cache, datum, datum.getMetricName(), EBS_ABSOLUTE_METRICS.get(datum.getMetricName()), volumeId)) continue;
          }
          // special cases
          // 1) VolumeThroughputPercentage -- this is 100% for provisioned volumes, and we need to insert a
          //                                  data point for every timestamp that a volume event occurs.
          //                                  To make sure we don't duplicate the effort, we choose one event at random, VolumeReadOps,
          //                                  and create this new metric arbitrarily
          if ("VolumeReadOps".equals(datum.getMetricName())) { // special case
            dataToInsert.add(createVolumeThroughputMetric(accountId, nameSpace, datum));
          }
          // 2) VolumeIdleTime -- we piggy back off of the metric we don't need VolumeTotalReadWriteTime, and convert it to VolumeIdleTime
          if ("VolumeTotalReadWriteTime".equals(datum.getMetricName())) {
            convertVolumeTotalReadWriteTimeToVolumeIdleTime(datum);
          }
          // 3) VolumeQueueLength -- this one comes in essentially correct, but we don't have a time duration for it, so we piggy back off
          //                         the absolute metric framework
          if ("VolumeQueueLength".equals(datum.getMetricName())) {
            if (!adjustAbsoluteVolumeQueueLengthStatisticSet(cache, datum, volumeId)) continue;
          }
        }

        if ("AWS/EC2".equals(nameSpace)) {
          String instanceId = null;
          if ((datum.getDimensions() != null) && (datum.getDimensions().getMember() != null)) {
            for (Dimension dimension: datum.getDimensions().getMember()) {
              if ("InstanceId".equals(dimension.getName())) {
                instanceId = dimension.getValue();
                cache.load(nameSpace, "InstanceId", instanceId);
              }
            }
          }
          if (EC2_ABSOLUTE_METRICS.containsKey(datum.getMetricName())) {
            if (!adjustAbsoluteInstanceStatisticSet(cache, datum, datum.getMetricName(), EC2_ABSOLUTE_METRICS.get(datum.getMetricName()), instanceId)) continue;
          } else if ("CPUUtilizationMSAbsolute".equals(datum.getMetricName())) { // special case
            // we check if the point below is a 'first' point, or maybe a point in the past.  Either case reject it.
            if (!adjustAbsoluteInstanceCPUStatisticSet(cache, datum, "CPUUtilizationMSAbsolute", "CPUUtilization", instanceId)) continue;
          }
        }
        dataToInsert.add(item); // this data point is ok
      }
      db.commit();
    } catch (RuntimeException ex) {
      Logs.extreme().error(ex, ex);
      throw ex;
    } finally {
      if (db.isActive())
        db.rollback();
    }
    return dataToInsert;
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
      .put("NetworkInAbsolute", "NetworkIn")
      .put("NetworkOutAbsolute", "NetworkOut")
      .build();

  private static boolean adjustAbsoluteVolumeQueueLengthStatisticSet(AbsoluteMetricCache cache,
                                                                     MetricDatum datum, String volumeId) {
    // the metric value is correct, we just need a statistic set with the sample count.
    // to get this we create a placeholder absolute metric, value always 0, just to get time duration/sample count
    MetricDatum absolutePlaceHolder = new MetricDatum();
    absolutePlaceHolder.setMetricName("VolumeQueueLengthPlaceHolderAbsolute");
    absolutePlaceHolder.setValue(0.0);
    absolutePlaceHolder.setTimestamp(datum.getTimestamp());
    if (!adjustAbsoluteVolumeStatisticSet(cache, absolutePlaceHolder, absolutePlaceHolder.getMetricName(), "VolumeQueueLengthPlaceHolder", volumeId)) return false;
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

  private static void convertVolumeTotalReadWriteTimeToVolumeIdleTime(final MetricDatum datum) {
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

  private static AbsoluteMetricQueueItem createVolumeThroughputMetric(String accountId, String nameSpace, MetricDatum datum) {
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

  private static boolean adjustAbsoluteInstanceCPUStatisticSet(AbsoluteMetricCache cache, MetricDatum datum, String absoluteMetricName,
                                                               String relativeMetricName, String instanceId) {
    MetricDifferenceInfo info = AbsoluteMetricHelper.calculateDifferenceSinceLastEvent(cache, "AWS/EC2", absoluteMetricName, "InstanceId", instanceId, datum.getTimestamp(), datum.getValue());
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
    MetricDifferenceInfo info = AbsoluteMetricHelper.calculateDifferenceSinceLastEvent(cache, "AWS/EC2", absoluteMetricName, "InstanceId", instanceId, datum.getTimestamp(), datum.getValue());
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


  private static boolean adjustAbsoluteVolumeStatisticSet(AbsoluteMetricCache cache, MetricDatum datum,
                                                          String absoluteMetricName, String relativeMetricName, String volumeId) {
    if (volumeId == null) return false;
    MetricDifferenceInfo info = AbsoluteMetricHelper.calculateDifferenceSinceLastEvent(cache, "AWS/EBS", absoluteMetricName, "VolumeId", volumeId, datum.getTimestamp(), datum.getValue());
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


  private void scrub(AbsoluteMetricQueueItem absoluteMetricQueueItem, Date now) {
    MetricDatum datum = absoluteMetricQueueItem.getMetricDatum();
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

  public void addQueueItems(List<AbsoluteMetricQueueItem> queueItems) {
    Date now = new Date();

    for (final AbsoluteMetricQueueItem queueItem : queueItems) {
      scrub(queueItem, now);
      dataQueue.offer(queueItem);
    }
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
          .add( Restrictions.eq( "dimensionName", dimensionName ) );
//            .add( Restrictions.eq( "dimensionValue", dimensionValue ) );
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
    private String dimensionName;

    public String getNamespace() {
      return namespace;
    }
    public void setNamespace(String namespace) {
      this.namespace = namespace;
    }
    public String getDimensionName() {
      return dimensionName;
    }
    public void setDimensionName(String dimensionName) {
      this.dimensionName = dimensionName;
    }
    private AbsoluteMetricLoadCacheKey(String namespace, String dimensionName) {
      super();
      this.namespace = namespace;
      this.dimensionName = dimensionName;
    }
    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result
        + ((dimensionName == null) ? 0 : dimensionName.hashCode());
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
      AbsoluteMetricLoadCacheKey other = (AbsoluteMetricLoadCacheKey) obj;
      if (dimensionName == null) {
        if (other.dimensionName != null)
          return false;
      } else if (!dimensionName.equals(other.dimensionName))
        return false;
      if (namespace == null) {
        if (other.namespace != null)
          return false;
      } else if (!namespace.equals(other.namespace))
        return false;
      return true;
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

  private static class DBCleanupService implements Runnable {
    @Override
    public void run() {
      LOG.info("Calling absolute metric history (cloudwatch) db cleanup service");
      if (!( Bootstrap.isFinished() &&
        Topology.isEnabled(CloudWatchBackend.class) )) {
        LOG.info("Cloudwatch service is not ENABLED");
        return;
      }

      Date twoWeeksAgo = new Date(System.currentTimeMillis() - 2 * 7 * 24 * 60 * 60 * 1000L);
      try {
        AbsoluteMetricHelper.deleteAbsoluteMetricHistory(twoWeeksAgo);
      } catch (Exception ex) {
        LOG.error(ex);
        LOG.error(ex, ex);
      }
      LOG.info("Done cleaning up absolute metric history (cloudwatch) db");
    }
  }

}
