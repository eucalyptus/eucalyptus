/*************************************************************************
 * Copyright 2016 Ent. Services Development Corporation LP
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
package com.eucalyptus.simplequeue.async;

import com.eucalyptus.auth.AuthException;
import com.eucalyptus.cloudwatch.common.CloudWatch;
import com.eucalyptus.cloudwatch.common.msgs.Dimension;
import com.eucalyptus.cloudwatch.common.msgs.Dimensions;
import com.eucalyptus.cloudwatch.common.msgs.MetricData;
import com.eucalyptus.cloudwatch.common.msgs.MetricDatum;
import com.eucalyptus.cloudwatch.common.msgs.PutMetricDataType;
import com.eucalyptus.cloudwatch.common.msgs.StatisticSet;
import com.eucalyptus.component.Topology;
import com.eucalyptus.simplequeue.Constants;
import com.eucalyptus.simplequeue.persistence.Queue;
import com.eucalyptus.simpleworkflow.stateful.NotifyResponseType;
import com.eucalyptus.util.async.AsyncRequests;
import com.eucalyptus.util.concurrent.ListenableFuture;
import org.apache.log4j.Logger;

import java.util.Date;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * Created by ethomas on 10/28/16.
 */
public class CloudWatchClient {

  static final Logger LOG = Logger.getLogger(CloudWatchClient.class);

  private static Date roundDown5Minutes(Date date) {
    if (date == null) return date;
    long timestamp = date.getTime();
    long unitStep = TimeUnit.MINUTES.toMillis(5);
    return new Date(timestamp - timestamp %  unitStep);
  }

  public static void addSQSMetricDatum(PutMetricDataType putMetricDataType, Queue.Key queueKey, Date date, String metricName, double value, String unit) {
    MetricDatum metricDatum = new MetricDatum();
    metricDatum.setTimestamp(roundDown5Minutes(date));
    metricDatum.setDimensions(getDimensions(queueKey));
    metricDatum.setMetricName(metricName);
    metricDatum.setValue(value);
    metricDatum.setUnit(unit);
    putMetricDataType.getMetricData().getMember().add(metricDatum);
  }

  public static void addSQSMetricDatum(PutMetricDataType putMetricDataType, Queue.Key queueKey, Date date, String metricName, double sampleCount, double minimum, double maximum, double sum, String unit) {
    MetricDatum metricDatum = new MetricDatum();
    metricDatum.setTimestamp(roundDown5Minutes(date));
    metricDatum.setDimensions(getDimensions(queueKey));
    metricDatum.setMetricName(metricName);
    StatisticSet statisticSet = new StatisticSet();
    statisticSet.setMaximum(maximum);
    statisticSet.setMinimum(minimum);
    statisticSet.setSampleCount(sampleCount);
    statisticSet.setSum(sum);
    metricDatum.setStatisticValues(statisticSet);
    metricDatum.setUnit(unit);
    putMetricDataType.getMetricData().getMember().add(metricDatum);
  }

  public static PutMetricDataType getSQSPutMetricDataType(Queue.Key queueKey) throws AuthException {
    PutMetricDataType putMetricDataType = new PutMetricDataType();
    putMetricDataType.setUserId(queueKey.getAccountId());
    putMetricDataType.markPrivileged();
    putMetricDataType.setNamespace(Constants.AWS_SQS);
    putMetricDataType.setMetricData(new MetricData());
    return putMetricDataType;
  }

  private static Dimensions getDimensions(Queue.Key queueKey) {
    Dimensions dimensions = new Dimensions();
    Dimension dimension = new Dimension();
    dimension.setName(Constants.QUEUE_NAME);
    dimension.setValue(queueKey.getQueueName());
    dimensions.getMember().add(dimension);
    return dimensions;
  }

  public static void putMetricData(PutMetricDataType putMetricDataType) {
    try {
      final ListenableFuture<NotifyResponseType> dispatchFuture =
        AsyncRequests.dispatch(Topology.lookup(CloudWatch.class), putMetricDataType);
      dispatchFuture.addListener(new Runnable() {
        @Override
        public void run() {
          try {
            dispatchFuture.get();
          } catch (final InterruptedException e) {
            LOG.info("Interrupted while sending put metric data request", e);
          } catch (final ExecutionException e) {
            LOG.error("Error while sending put metric data request", e);
          }
        }
      });
    } catch (final Exception e) {
      LOG.error("Error while sending put metric data request", e);
    }
  }
}
