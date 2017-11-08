/*************************************************************************
 * Copyright 2009-2016 Ent. Services Development Corporation LP
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

  private static final String DEFAULT_SWF_CLIENT_CONFIG =
      "{\"ConnectionTimeout\": 10000, \"MaxConnections\": 100}";

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
      initial = DEFAULT_SWF_CLIENT_CONFIG,
      description = "JSON configuration for the ELB simple workflow client",
      changeListener = Config.ClientConfigurationValidatingChangeListener.class )
  public static volatile String SWF_CLIENT_CONFIG = DEFAULT_SWF_CLIENT_CONFIG;

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
