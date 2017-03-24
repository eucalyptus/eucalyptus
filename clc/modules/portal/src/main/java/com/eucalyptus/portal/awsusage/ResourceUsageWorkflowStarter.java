/*************************************************************************
 * (c) Copyright 2017 Hewlett Packard Enterprise Development Company LP
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
package com.eucalyptus.portal.awsusage;

import com.amazonaws.services.simpleworkflow.flow.StartWorkflowOptions;
import com.amazonaws.services.simpleworkflow.model.WorkflowExecutionAlreadyStartedException;
import com.eucalyptus.portal.WorkflowClients;
import com.eucalyptus.portal.workflow.ResourceUsageEventWorkflowClientExternal;
import com.eucalyptus.simpleworkflow.common.client.WorkflowStarter;
import org.apache.log4j.Logger;

public class ResourceUsageWorkflowStarter implements WorkflowStarter {
  private static Logger LOG     =
          Logger.getLogger(  ResourceUsageWorkflowStarter.class );

  public static final String BILLING_RESOURCE_USAGE_EVENT_WORKFLOW_ID =
          "billing-resource-usage-event-workflow-01";

  @Override
  public void start() {
    try{
      final ResourceUsageEventWorkflowClientExternal workflow =
              WorkflowClients.getResourceUsageEventWorkflow(BILLING_RESOURCE_USAGE_EVENT_WORKFLOW_ID);
      final StartWorkflowOptions options = new StartWorkflowOptions();
      options.setExecutionStartToCloseTimeoutSeconds(7200L);
      options.setTaskStartToCloseTimeoutSeconds(60L);
      workflow.fireEvents(options);
    }catch(final WorkflowExecutionAlreadyStartedException ex) {
      ;
    }catch(final Exception ex) {
      LOG.error("Failed to start the workflow that fires resource usage events", ex);
    }
  }

  @Override
  public String name() {
    return "RESOURCE_USAGE_COLLECTION_WORKFLOW";
  }
}
