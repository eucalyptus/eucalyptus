/*************************************************************************
 * Copyright 2009-2014 Ent. Services Development Corporation LP
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
package com.eucalyptus.cloudwatch.service.queue.listmetrics;

import com.eucalyptus.cloudwatch.common.internal.domain.listmetrics.ListMetric;
import com.eucalyptus.cloudwatch.common.internal.domain.listmetrics.ListMetricManager;
import com.eucalyptus.cloudwatch.common.internal.domain.metricdata.SimpleMetricEntity;
import com.eucalyptus.system.Threads;
import com.eucalyptus.util.metrics.MonitoredAction;
import com.eucalyptus.util.metrics.ThruputMetrics;
import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.primitives.Longs;

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
    final String PROP_LIST_METRICS_FLUSH_INTERVAL = "com.eucalyptus.cloudwatch.listMetricsFlushInterval";
    final long DEFAULT_LIST_METRICS_FLUSH_INTERVAL = 300L;
    final long LIST_METRICS_FLUSH_INTERVAL = MoreObjects.firstNonNull(
        Longs.tryParse(
            System.getProperty(
                PROP_LIST_METRICS_FLUSH_INTERVAL,
                String.valueOf( DEFAULT_LIST_METRICS_FLUSH_INTERVAL ) ) ),
            DEFAULT_LIST_METRICS_FLUSH_INTERVAL );
    dataFlushTimer.scheduleAtFixedRate(safeRunner, 0, LIST_METRICS_FLUSH_INTERVAL, TimeUnit.SECONDS);
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
