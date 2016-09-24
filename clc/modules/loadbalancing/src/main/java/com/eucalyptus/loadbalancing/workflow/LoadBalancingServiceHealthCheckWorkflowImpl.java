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

import org.apache.log4j.Logger;

import com.amazonaws.services.simpleworkflow.flow.DecisionContextProvider;
import com.amazonaws.services.simpleworkflow.flow.DecisionContextProviderImpl;
import com.amazonaws.services.simpleworkflow.flow.WorkflowClock;
import com.amazonaws.services.simpleworkflow.flow.annotations.Asynchronous;
import com.amazonaws.services.simpleworkflow.flow.core.AndPromise;
import com.amazonaws.services.simpleworkflow.flow.core.Promise;
import com.amazonaws.services.simpleworkflow.flow.core.TryCatchFinally;
import com.eucalyptus.component.annotation.ComponentPart;
import com.eucalyptus.loadbalancing.common.LoadBalancing;

/**
 * @author Sang-Min Park (sangmin.park@hpe.com)
 *
 */
@ComponentPart(LoadBalancing.class)
public class LoadBalancingServiceHealthCheckWorkflowImpl
implements LoadBalancingServiceHealthCheckWorkflow {
  private static Logger    LOG     = 
      Logger.getLogger(  LoadBalancingServiceHealthCheckWorkflowImpl.class );

  final LoadBalancingActivitiesClient client = 
      new LoadBalancingActivitiesClientImpl();
  private ElbWorkflowState state = 
      ElbWorkflowState.WORKFLOW_RUNNING;
  TryCatchFinally task = null;
  private DecisionContextProvider contextProvider = 
      new DecisionContextProviderImpl();
  final WorkflowClock clock = 
      contextProvider.getDecisionContext().getWorkflowClock();
  private final int MAX_UPDATE_PER_WORKFLOW = 10;
  //// TODO: Make this configurable for scale
  private final int UPDATE_PERIOD_SEC = 60;
 
  @Override
  public void performServiceHealthCheck() {
    task = new TryCatchFinally() {
      @Override
      protected void doTry() throws Throwable {
        performServiceHealthCheckPeriodic(0);
      }

      @Override
      protected void doCatch(Throwable e) throws Throwable {
        state = ElbWorkflowState.WORKFLOW_FAILED;
        LOG.error("Workflow for updating ELB service state has failed: ", e);   
      }

      @Override
      protected void doFinally() throws Throwable {
        if (state == ElbWorkflowState.WORKFLOW_RUNNING)
          state = ElbWorkflowState.WORKFLOW_SUCCESS;
      }
    };
  }

  @Asynchronous
  void performServiceHealthCheckPeriodic(final int count,  final Promise<?>... waitFor)  {
    if (count >= MAX_UPDATE_PER_WORKFLOW) {
      return;
    }
    final Promise<Void> timer = startDaemonTimer(UPDATE_PERIOD_SEC);
    final Promise<Void> checkContinousWorkflows = client.runContinousWorkflows();
    final Promise<Void> checkServo = client.checkServoInstances(checkContinousWorkflows);
    final Promise<Void> checkDns = client.checkServoInstanceDns(checkServo);
    final Promise<Void> checkBackend = client.checkBackendInstances(checkDns);
    final Promise<Void> cleanupServo = client.cleanupServoInstances(checkBackend);
    final Promise<Void> cleanupSecurityGroup = client.cleanupSecurityGroups(cleanupServo);
    performServiceHealthCheckPeriodic(count+1, new AndPromise(timer, cleanupSecurityGroup));
  }

  @Asynchronous(daemon = true)
  private Promise<Void> startDaemonTimer(int seconds) {
      Promise<Void> timer = clock.createTimer(seconds);
      return timer;
  }
  
  @Override
  public ElbWorkflowState getState() {
    return state;
  }
}
