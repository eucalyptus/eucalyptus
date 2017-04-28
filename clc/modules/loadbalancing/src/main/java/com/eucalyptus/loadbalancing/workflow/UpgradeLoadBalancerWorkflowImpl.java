package com.eucalyptus.loadbalancing.workflow;

/*************************************************************************
 * (c) Copyright 2016 Hewlett Packard Enterprise Development Company LP
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

import com.amazonaws.services.simpleworkflow.flow.ActivityTaskTimedOutException;
import com.amazonaws.services.simpleworkflow.flow.core.Settable;
import com.amazonaws.services.simpleworkflow.flow.core.TryCatchFinally;
import com.eucalyptus.component.annotation.ComponentPart;
import com.eucalyptus.loadbalancing.common.LoadBalancing;
import com.eucalyptus.simpleworkflow.common.client.Once;
import org.apache.log4j.Logger;
import java.util.concurrent.CancellationException;

/**
 * @author Sang-Min Park (sangmin.park@hpe.com)
 *
 */
@ComponentPart(LoadBalancing.class)
@Once(value = UpgradeLoadBalancerWorkflowStarter.class, dependsOn = LoadBalancing.class)
public class UpgradeLoadBalancerWorkflowImpl implements  UpgradeLoadBalancerWorkflow {
  private static Logger LOG     =
          Logger.getLogger(  UpgradeLoadBalancerWorkflowImpl.class );

  private final LoadBalancingActivitiesClient client =
          new LoadBalancingActivitiesClientImpl();
  private ElbWorkflowState state =
          ElbWorkflowState.WORKFLOW_RUNNING;

  private TryCatchFinally task = null;
  @Override
  public void upgradeLoadBalancer() {
    final Settable<Boolean> exception = new Settable<Boolean>();
    task = new TryCatchFinally() {
      @Override
      protected void doTry() throws Throwable {
        client.upgrade4_4();
      }

      @Override
      protected void doCatch(final Throwable ex) throws Throwable {
        if (ex instanceof ActivityTaskTimedOutException) {
          LOG.warn("Workflow for upgrading loadbalancers has timed-out");
        }else if (ex instanceof CancellationException) {
          LOG.warn("Workflow for upgrading loadbalancers has been cancelled");
        }else {
          LOG.warn("Workflow for upgrading loadbalancers has failed");
        }
        state = ElbWorkflowState.WORKFLOW_FAILED;
        exception.set(true);
        throw ex;
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
    return state;
  }
}
