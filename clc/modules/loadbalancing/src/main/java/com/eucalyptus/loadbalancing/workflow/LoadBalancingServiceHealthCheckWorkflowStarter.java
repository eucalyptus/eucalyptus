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
package com.eucalyptus.loadbalancing.workflow;

import com.amazonaws.services.simpleworkflow.model.WorkflowExecutionAlreadyStartedException;
import com.eucalyptus.simpleworkflow.common.client.WorkflowStarter;
import org.apache.log4j.Logger;

public class LoadBalancingServiceHealthCheckWorkflowStarter implements  WorkflowStarter {
  private static Logger LOG     =
          Logger.getLogger(  LoadBalancingServiceHealthCheckWorkflowStarter.class );

  private static final String ELB_SERVICE_STATE_WORKFLOW_ID =
          "loadbalancing-service-state-workflow-01";
  @Override
  public void start() {
    try{
      final LoadBalancingServiceHealthCheckWorkflowClientExternal workflow =
              WorkflowClients.getServiceStateWorkflowClient(ELB_SERVICE_STATE_WORKFLOW_ID);
      workflow.performServiceHealthCheck();
    }catch(final WorkflowExecutionAlreadyStartedException ex ) {
      ;
    }catch(final Exception ex) {
      LOG.error("Failed to start loadbalancing service state workflow", ex);
    }
  }

  @Override
  public String name() {
    return "LOADBALANCING_SERVICE_HEALTHCHECK_WORKFLOW";
  }
}
