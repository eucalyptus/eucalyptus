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
 * Please contact Eucalyptus Systems, Inc., 6750 Navigator Way, Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 ************************************************************************/
package com.eucalyptus.cloudwatch.common;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.log4j.Logger;

import com.eucalyptus.cloudwatch.common.config.CloudWatchConfigProperties;

public class QueueThruput {

  public enum MonitoredAction {
    CLUSTER_SIZE("Cluster:Timing:dataBatch.size"),
    CLUSTER_DEAL_WITH_ABSOLUTE_METRICS("Cluster:Timing:dataBatch.dealWithAbsoluteMetrics():time"),
    CLUSTER_FOLD_METRICS("Cluster:Timing:dataBatch.foldMetrics():time"),
    CLUSTER_CONVERT_TO_PUT_METRIC_DATA_LIST("Cluster:Timing:dataBatch.convertToPutMetricDataList():time"),
    CLUSTER_CONSOLIDATE_PUT_METRIC_DATA_LIST("Cluster:Timing:dataBatch.consolidatePutMetricDataList():time"),
    CLUSTER_LIST_METRIC_MANAGER_CALL_PUT_METRIC_DATA("Cluster:Timing:ListMetricManager.callPutMetricData():time"),
    CLUSTER_TIMING("Cluster:Timing:time"),
    PUT_DATA_QUEUE_SIZE("PutMetricDataQueue:Timing:dataBatch.size"),
    PUT_DATA_QUEUE_CONVERT("PutMetricDataQueue:Timing:dataBatch.convertToSimpleDataBatch():time"),
    PUT_DATA_QUEUE_AGGREGATE("PutMetricDataQueue:Timing:dataBatch.aggregate():time"),
    PUT_DATA_QUEUE_MERTIC_ADD_BATCH("PutMetricDataQueue:Timing:dataBatch.MetricManager.addMetricBatch():time"),
    PUT_DATA_QUEUE_MERTIC_QUEUE_ADDALL("PutMetricDataQueue:Timing:ListMetricQueue.addAll():time"),
    PUT_DATA_TIMING("PutMetricDataQueue:Timing:time"),
    LIST_METRIC_SIZE("ListMetricQueue:Timing:dataBatch.size"),
    LIST_METRIC_PRUNE("ListMetricQueue:Timing:dataBatch.pruneDuplicates:time"),
    LIST_METRIC_CONVERT("ListMetricQueue:Timing:convertToListMetrics:time"),
    LIST_METRIC_MERTIC_ADD_BATCH("ListMetricQueue:Timing:ListMetricManager.addMetricBatch:time"),
    LIST_METRIC_TIMING("ListMetricQueue:Timing:time");
    
    private String name;
    private MonitoredAction(String name) {
      this.name = name;
    }
    @Override
    public String toString() { return name; }
  }

  public static class DataPoint {
    long collectionTimeMs;
    long value;
    
    public DataPoint(long value) {
      this.collectionTimeMs = System.currentTimeMillis();
      this.value = value;
    }
  }
  
  public static class Aggregates {
    final int count;
    final double mean;
    final double median;
    final long min;
    final long max;

    public Aggregates(int count, double mean, double median, long min, long max) {
      this.count = count;
      this.mean = mean;
      this.median = median;
      this.min = min;
      this.max = max;
    }
  }

  private static final Logger LOG = Logger.getLogger(QueueThruput.class);
  private static final Map<MonitoredAction, List<DataPoint>> data = new HashMap<MonitoredAction,List<DataPoint>>();
  private static final ReentrantReadWriteLock storageLock = new ReentrantReadWriteLock();
  private static final ReentrantReadWriteLock.ReadLock read = storageLock.readLock();
  private static final ReentrantReadWriteLock.WriteLock write = storageLock.writeLock();
  private static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy.MM.dd G 'at' HH:mm:ss z");
  
  private static final ScheduledExecutorService dataCleanupTimer = Executors
      .newSingleThreadScheduledExecutor();
  
  static {
    dataCleanupTimer.scheduleAtFixedRate(new Runnable( ) {
      @Override
      public void run() {
        write.lock();
        for(MonitoredAction action : MonitoredAction.values()){
          List<DataPoint> dataList = data.get(action);
          if (dataList == null)
            continue;
          if ( dataList.size() > CloudWatchConfigProperties.CLOUDWATCH_MONITORING_HISTORY_SIZE) {
            // remove first/oldest data-points
            dataList.subList(0, dataList.size() - CloudWatchConfigProperties.CLOUDWATCH_MONITORING_HISTORY_SIZE).clear();
          }
        }
        write.unlock();
      }
    }, 0, 10, TimeUnit.SECONDS);
  };
  
  /*
   * Add new data point (non-negative long) for storing and logging
   * Function throws IllegalArgumentException for negative input value
   */
  public static void addDataPoint(MonitoredAction action, long newDataPoint) {
    if (newDataPoint < 0)
      throw new IllegalArgumentException();
    write.lock();
    if (data.containsKey(action)) {
      data.get(action).add(new DataPoint(newDataPoint));
    } else {
      List<DataPoint> list = new LinkedList<DataPoint>();
      list.add(new DataPoint(newDataPoint));
      data.put(action, list);
    }
    write.unlock();
    
    if (LOG.isTraceEnabled()) {
      StringBuilder sb = new StringBuilder(action.name);
      sb.append("=");
      sb.append(newDataPoint);
      LOG.trace(sb.toString());
    }
  }
  
  /*
   * Return all known data point for an action
   */
  public static List<DataPoint> getDataPoints(MonitoredAction action) {
    read.lock();
    List<DataPoint> ret;
    if (data.containsKey(action)) {
      ret = Collections.unmodifiableList(data.get(action));
    } else {
      ret = Collections.emptyList();
    }
    read.unlock();
    return ret;
  }
  
  /*
   * Return mean, median, min, max, and count for all know data point for an action
   * Warning: function does not provide valid results if sum of all values exceeds Long.MAX_VALUE
   */
  public static Aggregates getAggregates(MonitoredAction action) {
    List<DataPoint> l = getDataPoints(action);
    if (l.size() ==0)
      return new Aggregates(0, 0.0, 0.0, 0, 0);
    long val[] = new long[l.size()];
    int i=0;
    long sum = 0;
    for(DataPoint p:l){
      val[i] = p.value;
      sum += p.value;
      i++;
    }
    Arrays.sort(val);
    double median = ((val.length & 1) == 1) ? val[val.length/2]/1.0d : (val[(val.length-1)/2] + val[(val.length+1)/2])/2.0d;
    if (sum < 0) {
      // assuming sum did not exceeded 1.5 * Long.MAX_VALUE
      LOG.error("Max long value is exceeded while calculating aggregates for CW queues metrics.");
      return new Aggregates(val.length, Double.NaN, median, val[0], val[val.length-1]);
    }
    return new Aggregates(val.length, sum/1.0d/val.length, median, val[0], val[val.length-1]);
  }

  /*
   * Return last N data points as a string 
   * [action]
   * [Time] [Value]
   */
  public static String getDataPoints(int count) {
    StringBuilder sb = new StringBuilder();
    read.lock();
    for(MonitoredAction action : MonitoredAction.values()){
      List<DataPoint> dataList = data.get(action);
      if (dataList == null)
        continue;
      sb.append(action.name).append("\n");
      for(DataPoint p:dataList.subList(dataList.size()>count ? dataList.size()-count : 0, dataList.size()))
        sb.append(dateFormat.format( new Date(p.collectionTimeMs) )).append("\t").append(p.value).append("\n");
    }
    read.unlock();
    return sb.toString();
  }
}
