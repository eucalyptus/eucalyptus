package com.eucalyptus.loadbalancing.workflow;

/*************************************************************************
 * Copyright 2016 Ent. Services Development Corporation LP
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
 */
@ComponentPart(LoadBalancing.class)
@Once(value = UpgradeLoadBalancerWorkflowStarter.class, dependsOn = LoadBalancing.class)
public class UpgradeLoadBalancerWorkflowImpl implements UpgradeLoadBalancerWorkflow {
  private static Logger LOG =
      Logger.getLogger(UpgradeLoadBalancerWorkflowImpl.class);

  private final LoadBalancingActivitiesClient client =
      new LoadBalancingActivitiesClientImpl(null, LoadBalancingJsonDataConverter.getDefault(),
          null);
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
        } else if (ex instanceof CancellationException) {
          LOG.warn("Workflow for upgrading loadbalancers has been cancelled");
        } else {
          LOG.warn("Workflow for upgrading loadbalancers has failed");
        }
        state = ElbWorkflowState.WORKFLOW_FAILED;
        exception.set(true);
        throw ex;
      }

      @Override
      protected void doFinally() throws Throwable {
        if (state == ElbWorkflowState.WORKFLOW_RUNNING) {
          state = ElbWorkflowState.WORKFLOW_SUCCESS;
        }
      }
    };
  }

  @Override
  public ElbWorkflowState getState() {
    return state;
  }
}
