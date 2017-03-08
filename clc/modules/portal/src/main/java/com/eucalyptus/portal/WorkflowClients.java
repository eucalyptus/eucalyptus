/*************************************************************************
 * (c) Copyright 2017 Hewlett Packard Enterprise Development Company LP
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 3 of the License.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 ************************************************************************/
package com.eucalyptus.portal;

import com.amazonaws.services.simpleworkflow.AmazonSimpleWorkflow;
import com.amazonaws.services.simpleworkflow.flow.WorkflowClientExternal;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.portal.workflow.AwsUsageDailyAggregateWorkflowClientExternal;
import com.eucalyptus.portal.workflow.AwsUsageDailyAggregateWorkflowClientExternalFactory;
import com.eucalyptus.portal.workflow.AwsUsageDailyAggregateWorkflowClientExternalFactoryImpl;
import com.eucalyptus.portal.workflow.AwsUsageHourlyAggregateWorkflowClientExternal;
import com.eucalyptus.portal.workflow.AwsUsageHourlyAggregateWorkflowClientExternalFactory;
import com.eucalyptus.portal.workflow.AwsUsageHourlyAggregateWorkflowClientExternalFactoryImpl;
import com.eucalyptus.portal.workflow.MonthlyReportGeneratorWorkflow;
import com.eucalyptus.portal.workflow.MonthlyReportGeneratorWorkflowClientExternal;
import com.eucalyptus.portal.workflow.MonthlyReportGeneratorWorkflowClientExternalFactory;
import com.eucalyptus.portal.workflow.MonthlyReportGeneratorWorkflowClientExternalFactoryImpl;
import com.eucalyptus.portal.workflow.ResourceUsageEventWorkflowClientExternal;
import com.eucalyptus.portal.workflow.ResourceUsageEventWorkflowClientExternalFactory;
import com.eucalyptus.portal.workflow.ResourceUsageEventWorkflowClientExternalFactoryImpl;
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
}
