/*************************************************************************
 * Copyright 2009-2016 Eucalyptus Systems, Inc.
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
package com.eucalyptus.util.metrics;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.log4j.Logger;

import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.system.Threads;
import com.eucalyptus.util.LockResource;
import com.google.common.collect.EvictingQueue;

import org.apache.commons.collections.map.LRUMap;

public class ThruputMetrics {

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
    final long firstQuartile;
    final long thirdQuartile;
    final long min;
    final long max;

    public Aggregates(int count, double mean, long firstQuartile, double median,
        long thirdQuartile, long min, long max) {
      this.count = count;
      this.mean = mean;
      this.median = median;
      this.firstQuartile = firstQuartile;
      this.thirdQuartile = thirdQuartile;
      this.min = min;
      this.max = max;
    }
  }

  private static final Logger LOG = Logger.getLogger(ThruputMetrics.class);
  private static final Map<MonitoredAction, EvictingQueue<DataPoint>> data = new HashMap<MonitoredAction,EvictingQueue<DataPoint>>();
  private static final ReentrantReadWriteLock storageLock = new ReentrantReadWriteLock();
  private static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy.MM.dd G 'at' HH:mm:ss z");
  private static final LRUMap paritalOperations = new LRUMap(10000);

  private static String operationKey(MonitoredAction action, String id, boolean start) {
    StringBuilder sb = new StringBuilder(start ? "S":"E");
    // to make shorter keys
    sb.append(" A:").append(action.ordinal()).append(" ID:").append(id);
    return sb.toString();
  }
  
  /**
   * Adds start time for monitored action that will be finished later.
   * If the same action was already recorded as ended due to asynchronous communication,
   * adds its execution time as a new data point.
   */
  public static Future<Boolean> startOperation(final MonitoredAction action, final String id, final long startTime) {
    return Threads.enqueue(Eucalyptus.class, ThruputMetrics.class,
      new Callable<Boolean>() {
        @Override
        public Boolean call() throws Exception {
          String startKey = operationKey(action, id, true);
          String endKey = operationKey(action, id, false);
          Long endTime = (Long) paritalOperations.get(endKey);
          if (endTime != null) {
            paritalOperations.remove(endKey);
            if (endTime - startTime > 0)
              addDataPointNoThread(action, endTime - startTime);
          } else {
            paritalOperations.put(startKey, startTime);
          }
          return true;
        }
    });
  }

  /**
   * Adds end time for monitored action that was started before and adds its execution
   * time as a new data point.
   */
  public static Future<Boolean> endOperation(final MonitoredAction action, final String id, final long endTime) {
    return Threads.enqueue(Eucalyptus.class, ThruputMetrics.class,
      new Callable<Boolean>() {
        @Override
        public Boolean call() throws Exception {
          String startKey = operationKey(action, id, true);
          String endKey = operationKey(action, id, false);
          Long startTime = (Long) paritalOperations.get(startKey);
          if (startTime != null) {
            paritalOperations.remove(startKey);
            if (endTime - startTime > 0)
              addDataPointNoThread(action, endTime - startTime);
          } else {
            paritalOperations.put(endKey, endTime);
          }
          return true;
        }
    });
  }

  private static void addDataPointNoThread(MonitoredAction action, long newDataPoint) {
    try ( final LockResource lock = LockResource.lock(storageLock.writeLock()) ) {
      if (data.containsKey(action)) {
        data.get(action).add(new DataPoint(newDataPoint));
      } else {
        EvictingQueue<DataPoint> newQ = EvictingQueue.create(MetricsConfiguration.METRICS_COLLECTION_SIZE);
        newQ.add(new DataPoint(newDataPoint));
        data.put(action, newQ);
      }
    }
    
    if (LOG.isTraceEnabled()) {
      StringBuilder sb = new StringBuilder(action.name);
      sb.append("=");
      sb.append(newDataPoint);
      LOG.trace(sb.toString());
    }
  }

  /**
   * Adds new data point (non-negative long) for storing and logging.
   * Function ignores negative input values
   */
  public static void addDataPoint(final MonitoredAction action, final long newDataPoint) {
    if (newDataPoint < 0)
      return;
    Threads.enqueue(Eucalyptus.class, ThruputMetrics.class,
        new Callable<Boolean>() {
          @Override
          public Boolean call() throws Exception {
            if (newDataPoint < 0)
              throw new IllegalArgumentException();
            addDataPointNoThread(action, newDataPoint);
            return true;
          }
    });
  }

  /**
   * Returns all known data point for an action.
   */
  public static DataPoint[] getDataPoints(MonitoredAction action) {
    try ( final LockResource lock = LockResource.lock(storageLock.readLock()) ) {
      DataPoint[] ret;
      if (data.containsKey(action)) {
        ret = data.get(action).toArray(new DataPoint[0]);
      } else {
        ret = new DataPoint[0];
      }
      return ret;
    }
  }
  
  /**
   * Clean data points for a giving action
   */
  public static void clearDatapoints(MonitoredAction action) {
    try ( final LockResource lock = LockResource.lock(storageLock.writeLock()) ) {
      if (data.containsKey(action))
        data.get(action).clear();
    }
  }

  /**
   * Clean all data points
   */
  public static void clearAllDatapoints() {
    try ( final LockResource lock = LockResource.lock(storageLock.writeLock()) ) {
      for(MonitoredAction action : MonitoredAction.values()) {
        if (data.containsKey(action))
          data.get(action).clear();
      }
    }
  }

  /**
   * Returns mean, first quartile, median, third quartile, min, max, and count
   * for all known data point for an action.
   * Warning: function does not provide valid results if sum of all values exceeds Long.MAX_VALUE.
   */
  public static Aggregates getAggregates(MonitoredAction action) {
    DataPoint[] l = getDataPoints(action);
    if (l.length == 0)
      return new Aggregates(0, 0.0, 0, 0.0, 0, 0, 0);
    long val[] = new long[l.length];
    int i=0;
    long sum = 0;
    for(DataPoint p:l){
      val[i] = p.value;
      sum += p.value;
      i++;
    }
    Arrays.sort(val);
    double median = ((val.length & 1) == 1) ? val[val.length/2]/1.0d : (val[(val.length-1)/2] + val[(val.length+1)/2])/2.0d;
    // percentiles are calculated using Nearest Rank method
    long firstQuartile = val.length > 1 ? val[(int)Math.round(25*val.length/100.0) - 1] : val[0];
    long thirdQuartile = val.length > 1 ? val[(int)Math.round(75*val.length/100.0) - 1] : val[0];
    if (sum < 0) {
      // assuming sum did not exceeded 1.5 * Long.MAX_VALUE
      LOG.error("Max long value is exceeded while calculating aggregates for metrics.");
      return new Aggregates(val.length, Double.NaN, firstQuartile, median, thirdQuartile, val[0], val[val.length-1]);
    }
    return new Aggregates(val.length, sum/1.0d/val.length, firstQuartile, median, thirdQuartile, val[0], val[val.length-1]);
  }

  /*
   * Return last N data points as a string 
   * [action]
   * [Time] [Value]
   */
  public static String getDataPoints(int count) {
    StringBuilder sb = new StringBuilder();
    try ( final LockResource lock = LockResource.lock(storageLock.readLock()) ) {
      for(MonitoredAction action : MonitoredAction.values()){
        DataPoint[] dataPoints = getDataPoints(action);
        if (dataPoints.length == 0)
          continue;
        sb.append(action.name).append("\n");
        for(int i = dataPoints.length > count ? dataPoints.length - count : 0; i < dataPoints.length; i++)
          sb.append(dateFormat.format( new Date(dataPoints[i].collectionTimeMs) )).append("\t")
          .append(dataPoints[i].value).append("\n");
      }
    }
    return sb.toString();
  }

  public static void changeSize(int newSize) {
    try ( final LockResource lock = LockResource.lock(storageLock.writeLock()) ) {
      for(MonitoredAction action : MonitoredAction.values()){
        if (data.containsKey(action)) {
          EvictingQueue<DataPoint> newQ = EvictingQueue.create(newSize);
          DataPoint[] values = data.get(action).toArray(new DataPoint[0]);
          for(int i = values.length > newSize ? values.length - newSize : 0; i < values.length; i++)
            newQ.add(values[i]);
          data.put(action, newQ);
        }
      }
    }
  }
}
