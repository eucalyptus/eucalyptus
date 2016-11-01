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
package com.eucalyptus.simplequeue.config;

import com.eucalyptus.configurable.ConfigurableClass;
import com.eucalyptus.configurable.ConfigurableField;
import com.eucalyptus.configurable.ConfigurableProperty;
import com.eucalyptus.configurable.ConfigurablePropertyException;
import com.eucalyptus.configurable.PropertyChangeListener;
import com.eucalyptus.configurable.PropertyChangeListeners;
import com.eucalyptus.simpleworkflow.common.client.Config;
import com.eucalyptus.ws.WebServices;

/**
 * Created by ethomas on 10/28/16.
 */
@ConfigurableClass( root = "services.simplequeue", description = "Parameters controlling simple queue (SQS)")
public class SimpleQueueProperties {
  @ConfigurableField(
    initial = "SimpleQueueDomain",
    description = "The simple workflow service domain for simplequeue",
    changeListener = Config.NameValidatingChangeListener.class )
  public static volatile String SWF_DOMAIN = "SimpleQueueDomain";
  @ConfigurableField(
    initial = "SimpleQueueTaskList",
    description = "The simple workflow service task list for simplequeue",
    changeListener = Config.NameValidatingChangeListener.class )
  public static volatile String SWF_TASKLIST = "SimpleQueueTaskList";
  @ConfigurableField(
    initial = "{\"ConnectionTimeout\": 10000, \"MaxConnections\": 100}",
    description = "JSON configuration for the simplequeue simple workflow client",
    changeListener = Config.ClientConfigurationValidatingChangeListener.class )
  public static volatile String SWF_CLIENT_CONFIG = "{\"ConnectionTimeout\": 10000, \"MaxConnections\": 100}";
  @ConfigurableField(
    initial = "{\"PollThreadCount\": 8, \"TaskExecutorThreadPoolSize\": 16, \"MaximumPollRateIntervalMilliseconds\": 50 }",
    description = "JSON configuration for the simplequeue simple workflow activity worker",
    changeListener = Config.ActivityWorkerConfigurationValidatingChangeListener.class )
  public static volatile String SWF_ACTIVITY_WORKER_CONFIG = "{\"PollThreadCount\": 8, \"TaskExecutorThreadPoolSize\": 16, \"MaximumPollRateIntervalMilliseconds\": 50 }";
  @ConfigurableField(
    initial = "{ \"DomainRetentionPeriodInDays\": 1, \"PollThreadCount\": 8, \"MaximumPollRateIntervalMilliseconds\": 50 }",
    description = "JSON configuration for the simplequeue simple workflow decision worker",
    changeListener = Config.WorkflowWorkerConfigurationValidatingChangeListener.class )
  public static volatile String SWF_WORKFLOW_WORKER_CONFIG = "{ \"DomainRetentionPeriodInDays\": 1, \"PollThreadCount\": 8, \"MaximumPollRateIntervalMilliseconds\": 50 }";
  @ConfigurableField( description = "How long a queue is considered 'active' in seconds after it has been accessed.",
    initial = "80", changeListener = PropertyChangeListeners.IsPositiveInteger.class )
  public volatile static int ACTIVE_QUEUE_TIME_SECS = 21600;
  @ConfigurableField( description = "Maximum number of characters in a queue name.",
    initial = "80", changeListener = PropertyChangeListeners.IsPositiveInteger.class )
  public volatile static int MAX_QUEUE_NAME_LENGTH_CHARS = 80;
  @ConfigurableField( description = "Maximum number of characters in a label.",
    initial = "80", changeListener = PropertyChangeListeners.IsPositiveInteger.class )
  public volatile static int MAX_LABEL_LENGTH_CHARS = 80;
  @ConfigurableField( description = "Maximum value for delay seconds.",
    initial = "900", changeListener = WebServices.CheckNonNegativeLongPropertyChangeListener.class )
  public volatile static int MAX_DELAY_SECONDS = 900;
  @ConfigurableField( description = "Maximum value for maximum message size.",
    initial = "262144", changeListener = CheckMin1024IntPropertyChangeListener.class )
  public volatile static int MAX_MAXIMUM_MESSAGE_SIZE = 262144;
  @ConfigurableField( description = "Maximum value for message retention period.",
    initial = "1209600", changeListener = CheckMin60IntPropertyChangeListener.class )
  public volatile static int MAX_MESSAGE_RETENTION_PERIOD = 1209600;
  @ConfigurableField( description = "Maximum value for receive message wait time seconds.",
    initial = "20", changeListener = WebServices.CheckNonNegativeLongPropertyChangeListener.class )
  public volatile static int MAX_RECEIVE_MESSAGE_WAIT_TIME_SECONDS = 20;
  @ConfigurableField( description = "Maximum value for visibility timeout.",
    initial = "43200", changeListener = WebServices.CheckNonNegativeLongPropertyChangeListener.class )
  public volatile static int MAX_VISIBILITY_TIMEOUT = 43200;
  @ConfigurableField( description = "Maximum value for maxReceiveCount (dead letter queue).",
    initial = "1000", changeListener = PropertyChangeListeners.IsPositiveInteger.class )
  public volatile static int MAX_MAX_RECEIVE_COUNT = 1000;
  @ConfigurableField( description = "Maximum value for maxNumberOfMessages (ReceiveMessages).",
    initial = "10", changeListener = PropertyChangeListeners.IsPositiveInteger.class )
  public volatile static int MAX_RECEIVE_MESSAGE_MAX_NUMBER_OF_MESSAGES = 10;
  @ConfigurableField( description = "Maximum length of message attribute name. (chars)",
    initial = "256", changeListener = PropertyChangeListeners.IsPositiveInteger.class )
  public volatile static int MAX_MESSAGE_ATTRIBUTE_NAME_LENGTH = 256;
  @ConfigurableField( description = "Maximum number of bytes in message attribute type. (bytes)",
    initial = "256", changeListener = PropertyChangeListeners.IsPositiveInteger.class )
  public volatile static int MAX_MESSAGE_ATTRIBUTE_TYPE_LENGTH = 256;
  @ConfigurableField( description = "Maximum number of entries in a batch request",
    initial = "10", changeListener = PropertyChangeListeners.IsPositiveInteger.class )
  public volatile static int MAX_NUM_BATCH_ENTRIES = 10;
  @ConfigurableField( description = "Maximum length of batch id. (chars)",
    initial = "80", changeListener = PropertyChangeListeners.IsPositiveInteger.class )
  public volatile static int MAX_BATCH_ID_LENGTH = 80;
  @ConfigurableField(
    initial = "true",
    description = "Set 'true' to allow CloudWatch Metrics for SQS",
    changeListener = WebServices.CheckBooleanPropertyChangeListener.class )
  public static volatile String ENABLE_METRICS_COLLECTION = "true";
  @ConfigurableField(
    initial = "true",
    description = "Set 'true' to allow Long Polling for SQS",
    changeListener = WebServices.CheckBooleanPropertyChangeListener.class )
  public static volatile String ENABLE_LONG_POLLING = "true";

  public abstract static class CheckMinIntPropertyChangeListener implements PropertyChangeListener {
    protected int minValue = 0;

    public CheckMinIntPropertyChangeListener(int minValue) {
      this.minValue = minValue;
    }

    @Override
    public void fireChange(ConfigurableProperty t, Object newValue ) throws ConfigurablePropertyException {
      long value;
      try {
        value = Long.parseLong((String) newValue);
      } catch (Exception ex) {
        throw new ConfigurablePropertyException("Invalid value " + newValue);
      }
      if (value > minValue ) {
        throw new ConfigurablePropertyException("Invalid value " + newValue);
      }
    }
  }

  public static class CheckMin1024IntPropertyChangeListener extends CheckMinIntPropertyChangeListener {
    public CheckMin1024IntPropertyChangeListener() {
      super(1024);
    }
  }

  public static class CheckMin60IntPropertyChangeListener extends CheckMinIntPropertyChangeListener {
    public CheckMin60IntPropertyChangeListener() {
      super(60);
    }
  }
}
