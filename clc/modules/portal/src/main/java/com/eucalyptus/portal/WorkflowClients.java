/*************************************************************************
 * Copyright 2017 Ent. Services Development Corporation LP
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
package com.eucalyptus.portal;

import com.amazonaws.services.simpleworkflow.AmazonSimpleWorkflow;
import com.amazonaws.services.simpleworkflow.flow.WorkflowClientExternal;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.portal.workflow.*;
import com.eucalyptus.simpleworkflow.common.client.Config;
import com.eucalyptus.util.Exceptions;
import com.google.common.collect.Lists;
import java.util.List;

public class WorkflowClients {
  private static final String TASK_LIST =
          BillingProperties.SWF_TASKLIST;
  private static final String DOMAIN =
          BillingProperties.SWF_DOMAIN;

  private static AmazonSimpleWorkflow getSimpleWorkflow() {
    AmazonSimpleWorkflow swfService = null;
    try{
      swfService = Config.buildClient(
              BillingAWSCredentialsProvider.BillingUserSupplier.INSTANCE
      );
      return swfService;
    }catch(final AuthException ex) {
      throw Exceptions.toUndeclared(ex);
    }
  }

  static class WorkflowOptionsBuilder {
    WorkflowClientExternal client = null;
    WorkflowOptionsBuilder(final WorkflowClientExternal client) {
      this.client = client;
    }

    WorkflowOptionsBuilder withTag(final String tagKey, final String tagValue) {
      final List<String> tagList = Lists.newArrayList();
      if(tagKey!=null && tagValue !=null)
        tagList.add(tagKey+":"+tagValue);
      this.client.getSchedulingOptions().setTagList(tagList);
      return this;
    }

    WorkflowOptionsBuilder withWorkflowTimeout(final long seconds) {
      this.client.getSchedulingOptions().setExecutionStartToCloseTimeoutSeconds(seconds);
      return this;
    }

    WorkflowOptionsBuilder withTaskTimeout(final long seconds) {
      this.client.getSchedulingOptions().setTaskStartToCloseTimeoutSeconds(seconds);
      return this;
    }

    WorkflowClientExternal build() {
      return this.client;
    }
  }

  public static AwsUsageHourlyAggregateWorkflowClientExternal getAwsUsageHourlyAggregateWorkflow(final String workflowId) {
    final AwsUsageHourlyAggregateWorkflowClientExternalFactory factory =
            new AwsUsageHourlyAggregateWorkflowClientExternalFactoryImpl(getSimpleWorkflow(),  DOMAIN);
    return (AwsUsageHourlyAggregateWorkflowClientExternal)
            new WorkflowOptionsBuilder(factory.getClient(workflowId)).build();
  }

  public static AwsUsageDailyAggregateWorkflowClientExternal getAwsUsageDailyAggregateWorkflow(final String workflowId) {
    final AwsUsageDailyAggregateWorkflowClientExternalFactory factory =
            new AwsUsageDailyAggregateWorkflowClientExternalFactoryImpl(getSimpleWorkflow(),  DOMAIN);
    return (AwsUsageDailyAggregateWorkflowClientExternal)
            new WorkflowOptionsBuilder(factory.getClient(workflowId)).build();
  }


  public static ResourceUsageEventWorkflowClientExternal getResourceUsageEventWorkflow(final String workflowId) {
    final ResourceUsageEventWorkflowClientExternalFactory factory =
            new ResourceUsageEventWorkflowClientExternalFactoryImpl( getSimpleWorkflow(), DOMAIN);
    return (ResourceUsageEventWorkflowClientExternal)
            new WorkflowOptionsBuilder(factory.getClient(workflowId)).build();
  }

  public static MonthlyReportGeneratorWorkflowClientExternal getMonthlyReportGeneratorWorkflow(final String workflowId) {
    final MonthlyReportGeneratorWorkflowClientExternalFactory factory =
            new MonthlyReportGeneratorWorkflowClientExternalFactoryImpl( getSimpleWorkflow(), DOMAIN);
    return (MonthlyReportGeneratorWorkflowClientExternal)
            new WorkflowOptionsBuilder(factory.getClient(workflowId)).build();
  }

  public static InstanceLogWorkflowClientExternal getInstanceLogWorkflow(final String workflowId) {
    final InstanceLogWorkflowClientExternalFactory factory =
            new InstanceLogWorkflowClientExternalFactoryImpl( getSimpleWorkflow(), DOMAIN);
    return (InstanceLogWorkflowClientExternal)
            new WorkflowOptionsBuilder(factory.getClient(workflowId)).build();
  }
}
