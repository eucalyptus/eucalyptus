/*************************************************************************
 * Copyright 2009-2014 Eucalyptus Systems, Inc.
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
package com.eucalyptus.cloudwatch.service.queue.listmetrics;

import com.eucalyptus.cloudwatch.common.internal.domain.listmetrics.ListMetric;
import com.eucalyptus.cloudwatch.common.internal.domain.listmetrics.ListMetricManager;
import com.eucalyptus.cloudwatch.common.internal.domain.metricdata.SimpleMetricEntity;
import com.eucalyptus.system.Threads;
import com.eucalyptus.util.metrics.MonitoredAction;
import com.eucalyptus.util.metrics.ThruputMetrics;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import org.apache.log4j.Logger;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ListMetricQueue {

  private static final Logger LOG = Logger.getLogger(ListMetricQueue.class);
  private static class NoDupQueue<T> {
    private LinkedHashSet<T> items = Sets.newLinkedHashSet();

    public synchronized void drainTo(List<T> list) {
      list.addAll(items);
      items.clear();

    }
    public synchronized void drainTo(List<T> list, int maxItems) {
      List<T> intermediateList = Lists.newArrayList();
      int ctr=0;
      for (T item: items) {
        intermediateList.add(item);
        ctr++;
        if (ctr == maxItems) break;
      }
      list.addAll(intermediateList);
      items.removeAll(intermediateList);
    }

    public synchronized void putAll(List<T> list) {
      items.addAll(list);
    }
    public synchronized void put(T item) {
      items.add(item);
    }
  }


  final static NoDupQueue<ListMetricQueueItem> dataQueue = new NoDupQueue<ListMetricQueueItem>();

  private static final ScheduledExecutorService dataFlushTimer = Executors
    .newSingleThreadScheduledExecutor( Threads.threadFactory( "cloudwatch-list-metrics-flush-%d" ) );

  private static ListMetricQueue singleton = getInstance();

  public static ListMetricQueue getInstance() {
    synchronized (ListMetricQueue.class) {
      if (singleton == null)
        singleton = new ListMetricQueue();
    }
    return singleton;
  }

  private void queue(ListMetricQueueItem metricData) {
    dataQueue.put(metricData);
  }

  private static Runnable safeRunner = new Runnable() {
    @Override
    public void run() {
      long before = System.currentTimeMillis();
      try {
        List<ListMetricQueueItem> dataBatch = Lists.newArrayList();
        dataQueue.drainTo(dataBatch);
        ThruputMetrics.addDataPoint(MonitoredAction.LIST_METRIC_SIZE, dataBatch.size( ));
        long t2 = System.currentTimeMillis();
        dataBatch = prune(dataBatch);
        long t3 = System.currentTimeMillis();
        ThruputMetrics.addDataPoint(MonitoredAction.LIST_METRIC_PRUNE, t3-t2);
        List<ListMetric> listMetrics = convertToListMetrics(dataBatch);
        long t4 = System.currentTimeMillis();
        ThruputMetrics.addDataPoint(MonitoredAction.LIST_METRIC_CONVERT, t4-t3);
        ListMetricManager.addMetricBatch(listMetrics);
        long t5 = System.currentTimeMillis();
        ThruputMetrics.addDataPoint(MonitoredAction.LIST_METRIC_MERTIC_ADD_BATCH, t5-t4);
      } catch (Throwable ex) {
        LOG.debug("ListMetricQueue:error");
        ex.printStackTrace();
        LOG.error(ex,ex);
      } finally {
        ThruputMetrics.addDataPoint(MonitoredAction.LIST_METRIC_TIMING, System.currentTimeMillis()-before);
      }
    }
  };

  private static List<ListMetric> convertToListMetrics(List<ListMetricQueueItem> dataBatch) {
    if (dataBatch == null) return null;
    List<ListMetric> listMetrics = Lists.newArrayList();
    for (ListMetricQueueItem item: dataBatch) {
      listMetrics.add(ListMetricManager.createListMetric(item.getAccountId(), item.getMetricName(), item.getMetricType(),
        item.getNamespace(), item.getDimensionMap()));
    }
    return listMetrics;
  }

  private static List<ListMetricQueueItem> prune(List<ListMetricQueueItem> dataBatch) {
    Set<ListMetricQueueItem> intermediateSet = Sets.newLinkedHashSet(dataBatch);
    List<ListMetricQueueItem> prunedList = Lists.newArrayList(intermediateSet);
    return prunedList;
  }

  static {
    dataFlushTimer.scheduleAtFixedRate(safeRunner, 0, 5, TimeUnit.MINUTES);
  }

  public void addAll(List<SimpleMetricEntity> dataBatch) {
    for (SimpleMetricEntity item: dataBatch) {
      ListMetricQueueItem metricMetadata = new ListMetricQueueItem();
      metricMetadata.setAccountId(item.getAccountId());
      metricMetadata.setNamespace(item.getNamespace());
      metricMetadata.setMetricName(item.getMetricName());
      metricMetadata.setDimensionMap(item.getDimensionMap());
      metricMetadata.setMetricType(item.getMetricType());
      queue(metricMetadata);
    }
  }

}
