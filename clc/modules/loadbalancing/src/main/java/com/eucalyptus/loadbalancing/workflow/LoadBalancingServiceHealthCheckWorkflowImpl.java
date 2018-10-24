/*************************************************************************
 * Copyright 2009-2016 Ent. Services Development Corporation LP
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
package com.eucalyptus.loadbalancing.workflow;

import com.eucalyptus.simpleworkflow.common.client.Repeating;
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
@Repeating(value = LoadBalancingServiceHealthCheckWorkflowStarter.class, sleepSeconds = 60, dependsOn = LoadBalancing.class)
public class LoadBalancingServiceHealthCheckWorkflowImpl
implements LoadBalancingServiceHealthCheckWorkflow {
  private static Logger    LOG     = 
      Logger.getLogger(  LoadBalancingServiceHealthCheckWorkflowImpl.class );

  final LoadBalancingActivitiesClient client = 
      new LoadBalancingActivitiesClientImpl(null, LoadBalancingJsonDataConverter.getDefault(), null);
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
    final Promise<Void> checkServoElasticIp = client.checkServoElasticIp(checkDns);
    final Promise<Void> checkBackend = client.checkBackendInstances(checkServoElasticIp);
    final Promise<Void> cleanupServo = client.cleanupServoInstances(checkBackend);
    final Promise<Void> cleanupSecurityGroup = client.cleanupSecurityGroups(cleanupServo);
    final Promise<Void> recycleServo = client.recycleFailedServoInstances(cleanupSecurityGroup);
    performServiceHealthCheckPeriodic(count+1, new AndPromise(timer, recycleServo));
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
