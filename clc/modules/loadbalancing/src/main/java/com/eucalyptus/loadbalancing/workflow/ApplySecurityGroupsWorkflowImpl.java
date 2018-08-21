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
          new LoadBalancingActivitiesClientImpl(null, LoadBalancingJsonDataConverter.getDefault(), null);
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
