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
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 ************************************************************************/
package com.eucalyptus.cloudformation.config;

import com.eucalyptus.configurable.ConfigurableClass;
import com.eucalyptus.configurable.ConfigurableField;
import com.eucalyptus.simpleworkflow.common.client.Config;

/**
 *
 */
@ConfigurableClass( root = "cloudformation", description = "Parameters controlling cloud formation")
public class CloudFormationProperties {

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
      initial = "{\"ConnectionTimeout\": 10000, \"MaxConnections\": 100}",
      description = "JSON configuration for the cloudformation simple workflow client",
      changeListener = Config.ClientConfigurationValidatingChangeListener.class )
  public static volatile String SWF_CLIENT_CONFIG = "{\"ConnectionTimeout\": 10000, \"MaxConnections\": 100}";

  @ConfigurableField(
      initial = "",
      description = "JSON configuration for the cloudformation simple workflow activity worker",
      changeListener = Config.ActivityWorkerConfigurationValidatingChangeListener.class )
  public static volatile String SWF_ACTIVITY_WORKER_CONFIG = "";

  @ConfigurableField(
      initial = "{ \"DomainRetentionPeriodInDays\": 1, \"PollThreadCount\": 8 }",
      description = "JSON configuration for the cloudformation simple workflow decision worker",
      changeListener = Config.WorkflowWorkerConfigurationValidatingChangeListener.class )
  public static volatile String SWF_WORKFLOW_WORKER_CONFIG = "{ \"DomainRetentionPeriodInDays\": 1, \"PollThreadCount\": 8 }";

  // In case we are using AWS SWF
  public static boolean USE_AWS_SWF = "true".equalsIgnoreCase(System.getProperty("cloudformation.use_aws_swf"));
  public static String AWS_ACCESS_KEY = System.getProperty("cloudformation.aws_access_key", "");
  public static String AWS_SECRET_KEY= System.getProperty("cloudformation.aws_secret_key", "");

}
