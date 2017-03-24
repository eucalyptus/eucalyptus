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

import com.amazonaws.services.simpleworkflow.flow.DecisionContextProvider;
import com.amazonaws.services.simpleworkflow.flow.DecisionContextProviderImpl;
import com.amazonaws.services.simpleworkflow.flow.WorkflowClock;
import com.amazonaws.services.simpleworkflow.flow.annotations.Asynchronous;
import com.amazonaws.services.simpleworkflow.flow.core.Promise;
import com.amazonaws.services.simpleworkflow.flow.core.TryCatchFinally;
import com.eucalyptus.component.annotation.ComponentPart;
import com.eucalyptus.portal.common.Portal;
import com.eucalyptus.portal.workflow.AwsUsageActivitiesClient;
import com.eucalyptus.portal.workflow.AwsUsageActivitiesClientImpl;
import com.eucalyptus.portal.workflow.BillingWorkflowState;
import com.eucalyptus.portal.workflow.ResourceUsageEventWorkflow;
import com.eucalyptus.simpleworkflow.common.client.Repeating;
import org.apache.log4j.Logger;
import java.util.Random;

@Repeating(value = ResourceUsageWorkflowStarter.class, sleepSeconds = 300)
@ComponentPart(Portal.class)
public class ResourceUsageEventWorkflowImpl implements ResourceUsageEventWorkflow {
  private static Logger LOG     =
          Logger.getLogger(  ResourceUsageEventWorkflowImpl.class );
  final AwsUsageActivitiesClient client =
          new AwsUsageActivitiesClientImpl();
  private BillingWorkflowState state =
          BillingWorkflowState.WORKFLOW_RUNNING;
  TryCatchFinally task = null;
  private DecisionContextProvider contextProvider =
          new DecisionContextProviderImpl();
  final WorkflowClock clock =
          contextProvider.getDecisionContext().getWorkflowClock();
  private final int ITERATION_PER_WORKFLOW = 1; // should be bound to limit workflow history
  private final int FIRING_DELAY_SEC = 1200;

  @Override
  public void fireEvents() {
    task = new TryCatchFinally() {
      @Override
      protected void doTry() throws Throwable {
        fireEventsPeriodic(0);
      }

      @Override
      protected void doCatch(Throwable e) throws Throwable {
        state = BillingWorkflowState.WORKFLOW_FAILED;
        LOG.error("Workflow for firing resource usage events failed: ", e);
      }

      @Override
      protected void doFinally() throws Throwable {
        if (state == BillingWorkflowState.WORKFLOW_RUNNING)
          state = BillingWorkflowState.WORKFLOW_SUCCESS;
      }
    };
  }

  @Asynchronous
  void fireEventsPeriodic(final int count, final Promise<?>... waitFor) {
    if (count >= ITERATION_PER_WORKFLOW) {
      return;
    }
    final Random rand = new Random();
    final Promise<Void> volume = client.fireVolumeUsage(
                    startDaemonTimer( rand.nextInt(FIRING_DELAY_SEC)));
    final Promise<Void> snapshot = client.fireSnapshotUsage(
            startDaemonTimer( rand.nextInt(FIRING_DELAY_SEC)));
    final Promise<Void> address = client.fireAddressUsage(
                    startDaemonTimer(rand.nextInt(FIRING_DELAY_SEC)));
    final Promise<Void> s3object = client.fireS3ObjectUsage(
            startDaemonTimer(rand.nextInt(FIRING_DELAY_SEC)));
     final Promise<Void> loadbalancers = client.fireLoadBalancerUsage(
            startDaemonTimer(rand.nextInt(FIRING_DELAY_SEC)));

    fireEventsPeriodic(count+1, volume, snapshot, address, s3object, loadbalancers);
  }


  @Asynchronous(daemon = true)
  private Promise<Void> startDaemonTimer(int seconds) {
    Promise<Void> timer = clock.createTimer(seconds);
    return timer;
  }

  @Asynchronous
  public BillingWorkflowState getState() {
    return state;
  }
}
