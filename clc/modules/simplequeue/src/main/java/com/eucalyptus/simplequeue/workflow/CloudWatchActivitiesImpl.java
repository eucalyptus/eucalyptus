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
 *
 *  This file may incorporate work covered under the following copyright and permission notice:
 *
 *   Copyright 2010-2016 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License").
 *   You may not use this file except in compliance with the License.
 *   A copy of the License is located at
 *
 *    http://aws.amazon.com/apache2.0
 *
 *   or in the "license" file accompanying this file. This file is distributed
 *   on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 *   express or implied. See the License for the specific language governing
 *   permissions and limitations under the License.
 ************************************************************************/
package com.eucalyptus.simplequeue.workflow;

import com.eucalyptus.auth.AuthException;
import com.eucalyptus.cloudwatch.common.CloudWatch;
import com.eucalyptus.cloudwatch.common.msgs.MetricDatum;
import com.eucalyptus.cloudwatch.common.msgs.PutMetricDataType;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.Topology;
import com.eucalyptus.component.annotation.ComponentPart;
import com.eucalyptus.simplequeue.Constants;
import com.eucalyptus.simplequeue.SimpleQueue;
import com.eucalyptus.simplequeue.async.CloudWatchClient;
import com.eucalyptus.simplequeue.config.SimpleQueueProperties;
import com.eucalyptus.simplequeue.persistence.PersistenceFactory;
import com.eucalyptus.simplequeue.persistence.Queue;
import com.eucalyptus.util.async.AsyncRequests;
import org.apache.log4j.Logger;

import java.util.Collection;
import java.util.Date;
import java.util.Map;

/**
 * Created by ethomas on 10/25/16.
 */
@ComponentPart(SimpleQueue.class)
public class CloudWatchActivitiesImpl implements CloudWatchActivities {

  private static final Logger LOG = Logger.getLogger(CloudWatchActivitiesImpl.class);

  @Override
  public Collection<String> getPartitions() {
    return PersistenceFactory.getQueuePersistence().getPartitionTokens();
  }

  @Override
  public void performWork(String partitionInfo) {
    if (!"false".equalsIgnoreCase(SimpleQueueProperties.ENABLE_METRICS_COLLECTION)) {
      // send a cloudwatch statistic
      ServiceConfiguration cwConfiguration = Topology.lookup(CloudWatch.class);
      for (Queue queue : PersistenceFactory.getQueuePersistence().listActiveQueues(partitionInfo)) {
        PutMetricDataType putMetricDataType = null;
        try {
          putMetricDataType = CloudWatchClient.getSQSPutMetricDataType(queue);
        } catch (AuthException e) {
          LOG.warn("Unable to get account info for queue " + queue.getArn() + ", skipping metrics");
          continue;
        }
        try {
          Date now = new Date();
          CloudWatchClient.addSQSMetricDatum(putMetricDataType, queue, now, Constants.APPROXIMATE_AGE_OF_OLDEST_MESSAGE,
            (double) PersistenceFactory.getMessagePersistence().getApproximateAgeOfOldestMessage(queue), "Seconds");

          Map<String, String> messageCounts = PersistenceFactory.getMessagePersistence().getApproximateMessageCounts(queue);

          CloudWatchClient.addSQSMetricDatum(putMetricDataType, queue, now, Constants.APPROXIMATE_NUMBER_OF_MESSAGES_DELAYED,
            (double) Long.parseLong(messageCounts.get(Constants.APPROXIMATE_NUMBER_OF_MESSAGES_DELAYED)), "Count");

          CloudWatchClient.addSQSMetricDatum(putMetricDataType, queue, now, Constants.APPROXIMATE_NUMBER_OF_MESSAGES_VISIBLE,
            (double) Long.parseLong(messageCounts.get(Constants.APPROXIMATE_NUMBER_OF_MESSAGES)), "Count");

          CloudWatchClient.addSQSMetricDatum(putMetricDataType, queue, now, Constants.APPROXIMATE_NUMBER_OF_MESSAGES_NOT_VISIBLE,
            (double) Long.parseLong(messageCounts.get(Constants.APPROXIMATE_NUMBER_OF_MESSAGES_NOT_VISIBLE)), "Count");

          //Add empty values for num deleted/sent/received to mimic AWS
          CloudWatchClient.addSQSMetricDatum(putMetricDataType, queue, now, Constants.NUMBER_OF_EMPTY_RECEIVES, 0.0, "Count");
          CloudWatchClient.addSQSMetricDatum(putMetricDataType, queue, now, Constants.NUMBER_OF_MESSAGES_DELETED, 0.0, "Count");
          CloudWatchClient.addSQSMetricDatum(putMetricDataType, queue, now, Constants.NUMBER_OF_MESSAGES_RECEIVED, 0.0, "Count");
          CloudWatchClient.addSQSMetricDatum(putMetricDataType, queue, now, Constants.NUMBER_OF_MESSAGES_SENT, 0.0, "Count");

          CloudWatchClient.putMetricData(putMetricDataType);
        } catch (Exception ex) {
          LOG.warn("Unable to send metrics for " + queue.getArn() + ", Reason: " + ex.getMessage());
        }
      }
    }
  }
}
