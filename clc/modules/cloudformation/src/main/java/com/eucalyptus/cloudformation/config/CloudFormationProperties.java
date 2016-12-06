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
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 ************************************************************************/
package com.eucalyptus.cloudformation.config;

import com.eucalyptus.configurable.ConfigurableClass;
import com.eucalyptus.configurable.ConfigurableField;
import com.eucalyptus.configurable.PropertyChangeListeners;
import com.eucalyptus.simpleworkflow.common.client.Config;

/**
 *
 */
@ConfigurableClass( root = "cloudformation", description = "Parameters controlling cloud formation")
public class CloudFormationProperties {

  private static final String DEFAULT_SWF_CLIENT_CONFIG =
      "{\"ConnectionTimeout\": 10000, \"MaxConnections\": 100}";

  private static final String DEFAULT_SWF_ACTIVITY_WORKER_CONFIG =
      "{\"PollThreadCount\": 8, \"TaskExecutorThreadPoolSize\": 16, \"MaximumPollRateIntervalMilliseconds\": 50, \"MaximumPollRatePerSecond\": 20 }";

  private static final String DEFAULT_SWF_WORKFLOW_WORKER_CONFIG =
      "{ \"DomainRetentionPeriodInDays\": 1, \"PollThreadCount\": 8, \"MaximumPollRateIntervalMilliseconds\": 50, \"MaximumPollRatePerSecond\": 20 }";

  @ConfigurableField(
    initial = "true",
    description = "Set 'true' to only allow 'known' properties in Resources",
    changeListener = PropertyChangeListeners.IsBoolean.class )
  public static volatile Boolean ENFORCE_STRICT_RESOURCE_PROPERTIES = true;

  @ConfigurableField(
      initial = "CloudFormationDomain",
      description = "The simple workflow service domain for cloudformation",
      changeListener = Config.NameValidatingChangeListener.class )
  public static volatile String SWF_DOMAIN = "CloudFormationDomain";

  @ConfigurableField(
      initial = "CloudFormationTaskList",
      description = "The simple workflow service task list for cloudformation",
      changeListener = Config.NameValidatingChangeListener.class )
  public static volatile String SWF_TASKLIST = "CloudFormationTaskList";

  @ConfigurableField(
      initial = DEFAULT_SWF_CLIENT_CONFIG,
      description = "JSON configuration for the cloudformation simple workflow client",
      changeListener = Config.ClientConfigurationValidatingChangeListener.class )
  public static volatile String SWF_CLIENT_CONFIG = DEFAULT_SWF_CLIENT_CONFIG;

  @ConfigurableField(
      initial = DEFAULT_SWF_ACTIVITY_WORKER_CONFIG,
      description = "JSON configuration for the cloudformation simple workflow activity worker",
      changeListener = Config.ActivityWorkerConfigurationValidatingChangeListener.class )
  public static volatile String SWF_ACTIVITY_WORKER_CONFIG = DEFAULT_SWF_ACTIVITY_WORKER_CONFIG;

  @ConfigurableField(
      initial = DEFAULT_SWF_WORKFLOW_WORKER_CONFIG,
      description = "JSON configuration for the cloudformation simple workflow decision worker",
      changeListener = Config.WorkflowWorkerConfigurationValidatingChangeListener.class )
  public static volatile String SWF_WORKFLOW_WORKER_CONFIG = DEFAULT_SWF_WORKFLOW_WORKER_CONFIG;

  // In case we are using AWS SWF
  public static boolean USE_AWS_SWF = "true".equalsIgnoreCase(System.getProperty("cloudformation.use_aws_swf"));
  public static String AWS_ACCESS_KEY = System.getProperty("cloudformation.aws_access_key", "");
  public static String AWS_SECRET_KEY= System.getProperty("cloudformation.aws_secret_key", "");

}
