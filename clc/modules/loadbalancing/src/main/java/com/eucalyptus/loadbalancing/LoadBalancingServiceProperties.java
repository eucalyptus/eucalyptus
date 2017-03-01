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
package com.eucalyptus.loadbalancing;

import com.eucalyptus.configurable.ConfigurableClass;
import com.eucalyptus.configurable.ConfigurableField;
import com.eucalyptus.configurable.ConfigurableFieldType;
import com.eucalyptus.simpleworkflow.common.client.Config;
import com.eucalyptus.util.Exceptions;
/**
 * @author Sang-Min Park (sangmin.park@hpe.com)
 *
 */
@ConfigurableClass(root = "services.loadbalancing", description = "Parameters controlling loadbalancing")
public class LoadBalancingServiceProperties {

  private static final String DEFAULT_SWF_ACTIVITY_WORKER_CONFIG =
      "{\"PollThreadCount\": 4, \"TaskExecutorThreadPoolSize\": 32, \"MaximumPollRateIntervalMilliseconds\": 50, \"MaximumPollRatePerSecond\": 20 }";

  private static final String DEFAULT_SWF_WORKFLOW_WORKER_CONFIG =
      "{ \"DomainRetentionPeriodInDays\": 1, \"PollThreadCount\": 4, \"MaximumPollRateIntervalMilliseconds\": 50, \"MaximumPollRatePerSecond\": 20 }";

  @ConfigurableField( displayName = "number_of_vm_per_zone",
      description = "number of VMs per loadbalancer zone",
      initial = "1",
      readonly = false,
      type = ConfigurableFieldType.KEYVALUE
      )
  public static String VM_PER_ZONE = "1";
  // com.eucalyptus.loadbalancing.activities.EventHandlerChainNew.vm_per_zone

  @ConfigurableField(
      initial = "LoadbalancingDomain",
      description = "The simple workflow service domain for ELB",
      changeListener = Config.NameValidatingChangeListener.class )
  public static volatile String SWF_DOMAIN = "LoadbalancingDomain";

  @ConfigurableField(
      initial = "LoadBalancerTasks",
      description = "The simple workflow service task list for ELB",
      changeListener = Config.NameValidatingChangeListener.class )
  public static volatile String SWF_TASKLIST = "LoadBalancerTasks";

  @ConfigurableField(
      initial = DEFAULT_SWF_ACTIVITY_WORKER_CONFIG,
      description = "JSON configuration for the ELB simple workflow activity worker",
      changeListener = Config.ActivityWorkerConfigurationValidatingChangeListener.class )
  public static volatile String SWF_ACTIVITY_WORKER_CONFIG = DEFAULT_SWF_ACTIVITY_WORKER_CONFIG;

  @ConfigurableField(
      initial = DEFAULT_SWF_WORKFLOW_WORKER_CONFIG,
      description = "JSON configuration for the ELB simple workflow decision worker",
      changeListener = Config.WorkflowWorkerConfigurationValidatingChangeListener.class )
  public static volatile String SWF_WORKFLOW_WORKER_CONFIG = DEFAULT_SWF_WORKFLOW_WORKER_CONFIG;

  public static int getCapacityPerZone( ) {
    int numVm = 1;
    try{
      numVm = Integer.parseInt(VM_PER_ZONE);
    }catch(NumberFormatException ex){
      throw Exceptions.toUndeclared("unable to parse loadbalancer_num_vm");
    }
    return numVm;
  }
}
