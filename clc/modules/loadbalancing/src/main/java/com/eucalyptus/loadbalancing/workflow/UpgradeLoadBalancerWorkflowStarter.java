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
import com.eucalyptus.simpleworkflow.common.client.WorkflowStarterException;
import org.apache.log4j.Logger;

public class UpgradeLoadBalancerWorkflowStarter implements WorkflowStarter {
  private static Logger LOG     =
          Logger.getLogger(  UpgradeLoadBalancerWorkflowStarter.class );
  private static final String ELB_UPGRADE_LOADBALANCER_WORKFLOW_ID =
          "upgrade-loadbalancer-workflow-01";

  @Override
  public void start() throws WorkflowStarterException {
    try {
      final UpgradeLoadBalancerWorkflowClientExternal workflow =
              WorkflowClients.getUpgradeLoadBalancerClient(ELB_UPGRADE_LOADBALANCER_WORKFLOW_ID);
      workflow.upgradeLoadBalancer();
    } catch(final WorkflowExecutionAlreadyStartedException ex ) {
      ;
    }catch(final Exception ex) {
      throw new WorkflowStarterException("Failed to start loadbalancing upgrade workflow", ex);
    }
  }

  @Override
  public String name() {
    return "LOADBALANCING_SERVICE_UPGRADE_WORKFLOW";
  }
}
