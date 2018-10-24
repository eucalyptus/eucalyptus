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
package com.eucalyptus.simplequeue.config;

import com.eucalyptus.configurable.ConfigurableClass;
import com.eucalyptus.configurable.ConfigurableField;
import com.eucalyptus.configurable.ConfigurableProperty;
import com.eucalyptus.configurable.ConfigurablePropertyException;
import com.eucalyptus.configurable.PropertyChangeListener;
import com.eucalyptus.configurable.PropertyChangeListeners;
import com.eucalyptus.configurable.StaticPropertyEntry;
import com.eucalyptus.simpleworkflow.common.client.Config;
import com.eucalyptus.system.Ats;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;

/**
 * Created by ethomas on 10/28/16.
 */
@ConfigurableClass( root = "services.simplequeue", description = "Parameters controlling simple queue (SQS)")
public class SimpleQueueProperties {

  private static final String DEFAULT_SWF_ACTIVITY_WORKER_CONFIG =
      "{\"PollThreadCount\": 4, \"TaskExecutorThreadPoolSize\": 8, \"MaximumPollRateIntervalMilliseconds\": 50, \"MaximumPollRatePerSecond\": 20 }";

  private static final String DEFAULT_SWF_WORKFLOW_WORKER_CONFIG =
      "{ \"DomainRetentionPeriodInDays\": 1, \"PollThreadCount\": 2, \"MaximumPollRateIntervalMilliseconds\": 50, \"MaximumPollRatePerSecond\": 20 }";

  public static volatile String SWF_DOMAIN = "SimpleQueueDomain";
  @ConfigurableField(
    initial = "SimpleQueueTaskList",
    description = "The simple workflow service task list for simplequeue",
    changeListener = Config.NameValidatingChangeListener.class )
  public static volatile String SWF_TASKLIST = "SimpleQueueTaskList";
  @ConfigurableField(
    initial = DEFAULT_SWF_ACTIVITY_WORKER_CONFIG,
    description = "JSON configuration for the simplequeue simple workflow activity worker",
    changeListener = Config.ActivityWorkerConfigurationValidatingChangeListener.class )
  public static volatile String SWF_ACTIVITY_WORKER_CONFIG = DEFAULT_SWF_ACTIVITY_WORKER_CONFIG;
  @ConfigurableField(
    initial = DEFAULT_SWF_WORKFLOW_WORKER_CONFIG,
    description = "JSON configuration for the simplequeue simple workflow decision worker",
    changeListener = Config.WorkflowWorkerConfigurationValidatingChangeListener.class )
  public static volatile String SWF_WORKFLOW_WORKER_CONFIG = DEFAULT_SWF_WORKFLOW_WORKER_CONFIG;
  @ConfigurableField( description = "How long a queue is considered 'active' in seconds after it has been accessed.",
    initial = "21600", changeListener = MinValuePropertyChangeListener.class )
  @MinValue(min = 1)
  public volatile static int ACTIVE_QUEUE_TIME_SECS = 21600;
  @ConfigurableField( description = "Maximum number of characters in a queue name.",
    initial = "80", changeListener = MinValuePropertyChangeListener.class )   
  @MinValue(min = 1)
  public volatile static int MAX_QUEUE_NAME_LENGTH_CHARS = 80;
  @ConfigurableField( description = "Maximum number of characters in a label.",
    initial = "80", changeListener = MinValuePropertyChangeListener.class )   
  @MinValue(min = 1)
  public volatile static int MAX_LABEL_LENGTH_CHARS = 80;
  @ConfigurableField( description = "Maximum value for delay seconds.",
    initial = "900", changeListener = MinValuePropertyChangeListener.class )
  @MinValue(min = 0)
  public volatile static int MAX_DELAY_SECONDS = 900;
  @ConfigurableField( description = "Maximum value for maximum message size.",
    initial = "262144", changeListener = MinValuePropertyChangeListener.class )
  @MinValue(min = 1024)
  public volatile static int MAX_MAXIMUM_MESSAGE_SIZE = 262144;
  @ConfigurableField( description = "Maximum value for message retention period.",
    initial = "1209600", changeListener = MinValuePropertyChangeListener.class )
  @MinValue(min = 60)
  public volatile static int MAX_MESSAGE_RETENTION_PERIOD = 1209600;
  @ConfigurableField( description = "Maximum value for receive message wait time seconds.",
    initial = "20", changeListener = MinValuePropertyChangeListener.class )
  @MinValue(min = 0)
  public volatile static int MAX_RECEIVE_MESSAGE_WAIT_TIME_SECONDS = 20;
  @ConfigurableField( description = "Maximum value for visibility timeout.",
    initial = "43200", changeListener = MinValuePropertyChangeListener.class )
  @MinValue(min = 0)
  public volatile static int MAX_VISIBILITY_TIMEOUT = 43200;
  @ConfigurableField( description = "Maximum value for maxReceiveCount (dead letter queue).",
    initial = "1000", changeListener = MinValuePropertyChangeListener.class )
  @MinValue(min = 1)
  public volatile static int MAX_MAX_RECEIVE_COUNT = 1000;
  @ConfigurableField( description = "Maximum value for maxNumberOfMessages (ReceiveMessages).",
    initial = "10", changeListener = MinValuePropertyChangeListener.class )   
  @MinValue(min = 1)
  public volatile static int MAX_RECEIVE_MESSAGE_MAX_NUMBER_OF_MESSAGES = 10;
  @ConfigurableField( description = "Maximum length of message attribute name. (chars)",
    initial = "256", changeListener = MinValuePropertyChangeListener.class )   
  @MinValue(min = 1)
  public volatile static int MAX_MESSAGE_ATTRIBUTE_NAME_LENGTH = 256;
  @ConfigurableField( description = "Maximum number of bytes in message attribute type. (bytes)",
    initial = "256", changeListener = MinValuePropertyChangeListener.class )   
  @MinValue(min = 1)
  public volatile static int MAX_MESSAGE_ATTRIBUTE_TYPE_LENGTH = 256;
  @ConfigurableField( description = "Maximum number of entries in a batch request",
    initial = "10", changeListener = MinValuePropertyChangeListener.class )   
  @MinValue(min = 1)
  public volatile static int MAX_NUM_BATCH_ENTRIES = 10;
  @ConfigurableField( description = "Maximum length of batch id. (chars)",
    initial = "80", changeListener = MinValuePropertyChangeListener.class )
  @MinValue(min = 1)
  public volatile static int MAX_BATCH_ID_LENGTH = 80;
  @ConfigurableField(
    initial = "true",
    description = "Set 'true' to allow CloudWatch Metrics for SQS",
    changeListener = PropertyChangeListeners.IsBoolean.class )
  public static volatile Boolean ENABLE_METRICS_COLLECTION = true;
  @ConfigurableField(
    initial = "true",
    description = "Set 'true' to allow Long Polling for SQS",
    changeListener = PropertyChangeListeners.IsBoolean.class )
  public static volatile Boolean ENABLE_LONG_POLLING = true;

  @Target(ElementType.FIELD)
  @Retention(RetentionPolicy.RUNTIME)
  public @interface MinValue {
    long min() default 0;
  }

  public static class MinValuePropertyChangeListener implements PropertyChangeListener {
    @SuppressWarnings( "unchecked" )
    @Override
    public void fireChange( final ConfigurableProperty t, final Object newValue ) throws ConfigurablePropertyException {
      if ( !(t instanceof StaticPropertyEntry) ) {
        throw new ConfigurablePropertyException( "Invalid use of listener" );
      }
      final StaticPropertyEntry staticPropertyEntry = (StaticPropertyEntry) t;
      final Field field = staticPropertyEntry.getField( );
      final MinValue minValue;
      long value;
      try {
        minValue = Ats.from(field).get( MinValue.class );
        if (minValue == null) {
          throw new ConfigurablePropertyException("This listener requires an @MinValue annotation");
        }
        value = Long.parseLong((String) newValue);
      } catch (Exception ex) {
        throw new ConfigurablePropertyException("Invalid value " + newValue);
      }
      if (value < minValue.min() ) {
        throw new ConfigurablePropertyException("Invalid value " + newValue);
      }
    }
  }

}
