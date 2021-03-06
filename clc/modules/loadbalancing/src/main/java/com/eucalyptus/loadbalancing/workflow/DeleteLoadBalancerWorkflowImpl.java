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

import java.util.concurrent.CancellationException;

import org.apache.log4j.Logger;

import com.amazonaws.services.simpleworkflow.flow.core.Promise;
import com.amazonaws.services.simpleworkflow.flow.core.TryCatchFinally;
import com.eucalyptus.component.annotation.ComponentPart;
import com.eucalyptus.loadbalancing.common.LoadBalancing;

/**
 * @author Sang-Min Park (sangmin.park@hpe.com)
 *
 */
@ComponentPart(LoadBalancing.class)
public class DeleteLoadBalancerWorkflowImpl implements DeleteLoadBalancerWorkflow {
  private static Logger    LOG     = Logger.getLogger(  DeleteLoadBalancerWorkflowImpl.class );
  
  final LoadBalancingActivitiesClient client = 
      new LoadBalancingActivitiesClientImpl();
  private ElbWorkflowState state = 
      ElbWorkflowState.WORKFLOW_RUNNING;
  TryCatchFinally task = null;

  @Override
  public void deleteLoadBalancer(final String accountId, final String loadbalancer) {
    task = new TryCatchFinally() {
      @Override
      protected void doTry() throws Throwable {
        final Promise<Void> dns = client.deleteLoadBalancerDeactivateDns(accountId, loadbalancer);
        final Promise<Void> scalingGroup = client.deleteLoadBalancerDeleteScalingGroup(accountId, loadbalancer);
        final Promise<Void> instanceProfile = client.deleteLoadBalancerDeleteInstanceProfile(accountId, loadbalancer, scalingGroup);
        final Promise<Void> iam = client.deleteLoadBalancerDeleteIamRole(accountId, loadbalancer, instanceProfile);
        final Promise<Void> securityGroup = client.deleteLoadBalancerDeleteSecurityGroup(accountId, loadbalancer, scalingGroup);
      }
      
      @Override
      protected void doCatch(Throwable e) throws Throwable {
        if ( e instanceof CancellationException) {
          LOG.warn("Workflow for deleting ELB is cancelled");
          state = ElbWorkflowState.WORKFLOW_CANCELLED;
          return;
        }
  
        state = ElbWorkflowState.WORKFLOW_FAILED;
        LOG.error("Workflow for deleting ELB has failed", e);
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
