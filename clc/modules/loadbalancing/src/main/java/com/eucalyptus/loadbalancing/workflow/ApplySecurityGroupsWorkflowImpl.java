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
package com.eucalyptus.loadbalancing.workflow;

import java.util.Map;
import java.util.concurrent.CancellationException;

import com.amazonaws.services.simpleworkflow.flow.core.TryCatchFinally;
import com.eucalyptus.component.annotation.ComponentPart;
import com.eucalyptus.loadbalancing.common.LoadBalancing;
import org.apache.log4j.Logger;

/**
 * @author Sang-Min Park (sangmin.park@hpe.com)
 *
 */
@ComponentPart(LoadBalancing.class)
public class ApplySecurityGroupsWorkflowImpl
    implements ApplySecurityGroupsWorkflow {
  private static Logger LOG     = Logger.getLogger(  ApplySecurityGroupsWorkflowImpl.class );
  final LoadBalancingActivitiesClient client =
          new LoadBalancingActivitiesClientImpl();
  private ElbWorkflowState state =
          ElbWorkflowState.WORKFLOW_RUNNING;
  TryCatchFinally task = null;
  private String accountNumber = null;
  private String loadbalancer = null;

  @Override
  public void applySecurityGroups(final String accountId, final String loadbalancer,
      final Map<String, String> groupIdToNameMap) {
    this.accountNumber = accountNumber;
    this.loadbalancer = loadbalancer;
    task = new TryCatchFinally() {
      @Override
      protected void doTry() throws Throwable {
        client.applySecurityGroupUpdateSecurityGroup(accountId, loadbalancer,
                groupIdToNameMap);
      }

      @Override
      protected void doCatch(Throwable e) throws Throwable {
        if (e instanceof CancellationException) {
          LOG.warn("Workflow for applying security group is cancelled");
          state = ElbWorkflowState.WORKFLOW_CANCELLED;
          return;
        }

        state = ElbWorkflowState.WORKFLOW_FAILED;
        LOG.error("Workflow for applying security group has failed", e);
      }

      @Override
      protected void doFinally() throws Throwable {
        if (state == ElbWorkflowState.WORKFLOW_RUNNING)
          state = ElbWorkflowState.WORKFLOW_SUCCESS;
      }
    };
  }

  @Override
  public ElbWorkflowState getState() {
    return this.state;
  }
}
