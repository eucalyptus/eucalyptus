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
package com.eucalyptus.portal.instanceusage;

import com.amazonaws.services.simpleworkflow.flow.StartWorkflowOptions;
import com.amazonaws.services.simpleworkflow.model.WorkflowExecutionAlreadyStartedException;
import com.eucalyptus.portal.WorkflowClients;
import com.eucalyptus.portal.workflow.InstanceLogWorkflowClientExternal;
import com.eucalyptus.simpleworkflow.common.client.WorkflowStarter;
import org.apache.log4j.Logger;

public class InstanceLogWorkflowStarter implements WorkflowStarter{
  private static Logger LOG     =
          Logger.getLogger(  InstanceLogWorkflowStarter.class );

  public static final String BILLING_INSTANCE_LOG_HOURLY_WORKFLOW_ID =
          "billing-instance-log-hourly-workflow-01";
  @Override
  public void start() {
    try {
      final InstanceLogWorkflowClientExternal workflow =
              WorkflowClients.getInstanceLogWorkflow(BILLING_INSTANCE_LOG_HOURLY_WORKFLOW_ID);
      final StartWorkflowOptions options = new StartWorkflowOptions();
      options.setExecutionStartToCloseTimeoutSeconds(720L);
      options.setTaskStartToCloseTimeoutSeconds(60L);
      workflow.logInstanceHourly(options);
    } catch (final WorkflowExecutionAlreadyStartedException ex) {
      ;
    } catch (final Exception ex) {
      LOG.error("Failed to start the workflow that aggregates instance hour reports");
    }
  }

  @Override
  public String name() {
    return "INSTANCE_LOG_HOURLY_WORKFLOW";
  }
}
