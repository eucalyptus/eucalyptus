/*************************************************************************
 * (c) Copyright 2016 Hewlett Packard Enterprise Development Company LP
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
import com.eucalyptus.simplequeue.config.SimpleQueueProperties;
import com.eucalyptus.simplequeue.persistence.Queue;
import com.eucalyptus.simpleworkflow.stateful.NotifyResponseType;
import com.eucalyptus.simpleworkflow.stateful.NotifyType;
import com.eucalyptus.simpleworkflow.stateful.PolledNotifications;
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

  public static void addSQSMetricDatum(PutMetricDataType putMetricDataType, Queue queue, Date date, String metricName, double value, String unit) {
    MetricDatum metricDatum = new MetricDatum();
    metricDatum.setTimestamp(roundDown5Minutes(date));
    metricDatum.setDimensions(getDimensions(queue));
    metricDatum.setMetricName(metricName);
    metricDatum.setValue(value);
    metricDatum.setUnit(unit);
    putMetricDataType.getMetricData().getMember().add(metricDatum);
  }

  public static void addSQSMetricDatum(PutMetricDataType putMetricDataType, Queue queue, Date date, String metricName, double sampleCount, double minimum, double maximum, double sum, String unit) {
    MetricDatum metricDatum = new MetricDatum();
    metricDatum.setTimestamp(roundDown5Minutes(date));
    metricDatum.setDimensions(getDimensions(queue));
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

  public static PutMetricDataType getSQSPutMetricDataType(Queue queue) throws AuthException {
    PutMetricDataType putMetricDataType = new PutMetricDataType();
    putMetricDataType.setUserId(queue.getAccountId());
    putMetricDataType.markPrivileged();
    putMetricDataType.setNamespace(Constants.AWS_SQS);
    putMetricDataType.setMetricData(new MetricData());
    return putMetricDataType;
  }

  private static Dimensions getDimensions(Queue queue) {
    Dimensions dimensions = new Dimensions();
    Dimension dimension = new Dimension();
    dimension.setName(Constants.QUEUE_NAME);
    dimension.setValue(queue.getQueueName());
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
