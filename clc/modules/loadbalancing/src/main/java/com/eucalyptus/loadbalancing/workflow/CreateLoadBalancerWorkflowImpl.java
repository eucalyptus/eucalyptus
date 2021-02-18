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

import com.amazonaws.services.simpleworkflow.flow.annotations.Asynchronous;
import com.amazonaws.services.simpleworkflow.flow.core.Promise;
import com.amazonaws.services.simpleworkflow.flow.core.TryCatchFinally;
import com.eucalyptus.component.annotation.ComponentPart;
import com.eucalyptus.loadbalancing.common.LoadBalancing;
import com.eucalyptus.loadbalancing.workflow.LoadBalancingActivitiesClient;
import com.eucalyptus.loadbalancing.workflow.LoadBalancingActivitiesClientImpl;

/**
 * @author Sang-Min Park (sangmin.park@hpe.com)
 */
@ComponentPart(LoadBalancing.class)
public class CreateLoadBalancerWorkflowImpl implements CreateLoadBalancerWorkflow {
  private static Logger LOG = Logger.getLogger(CreateLoadBalancerWorkflowImpl.class);
  final LoadBalancingActivitiesClient client =
      new LoadBalancingActivitiesClientImpl(null, LoadBalancingJsonDataConverter.getDefault(),
          null);
  TryCatchFinally task = null;
  Promise<Boolean> admission = null;
  Promise<String> roleName = null;
  Promise<String> instanceProfile = null;
  Promise<String> iamPolicy = null;
  Promise<SecurityGroupSetupActivityResult> securityGroup = null;
  Promise<CreateTagActivityResult> tag = null;
  private ElbWorkflowState state =
      ElbWorkflowState.WORKFLOW_RUNNING;

  @Override
  public void createLoadBalancer(final String accountId, final String loadbalancer,
      final String[] availabilityZones) {
    task = new TryCatchFinally() {
      @Override
      protected void doTry() throws Throwable {
        admission = doAdmission(
            client.createLbAdmissionControl(accountId, loadbalancer, availabilityZones));
        roleName = client.iamRoleSetup(accountId, loadbalancer, admission);
        instanceProfile =
            client.instanceProfileSetup(Promise.asPromise(accountId),
                Promise.asPromise(loadbalancer), roleName);
        iamPolicy =
            client.iamPolicySetup(Promise.asPromise(accountId), Promise.asPromise(loadbalancer),
                roleName);
        securityGroup =
            client.securityGroupSetup(accountId, loadbalancer, admission);
        tag =
            client.createLbTagCreator(Promise.asPromise(accountId), Promise.asPromise(loadbalancer),
                getSecurityGroupId(securityGroup));
      }

      @Override
      protected void doCatch(Throwable e) throws Throwable {
        if (e instanceof CancellationException) {
          LOG.warn("Creating loadbalancer stops due to failed admission control");
          state = ElbWorkflowState.WORKFLOW_CANCELLED;
          return;
        }
        // rollback activities
        Promise<Void> tagRollback = null;
        if (tag != null && tag.isReady()) {
          tagRollback = client.createLbTagCreatorRollback(tag);
        }
        if (securityGroup != null && securityGroup.isReady()) {
          client.securityGroupSetupRollback(Promise.asPromise(accountId),
              Promise.asPromise(loadbalancer), securityGroup, tagRollback);
        }

        final LoadBalancingActivityException ex = Exceptions.lookupActivityException(e);
        if (ex != null && ex instanceof NotEnoughResourcesException) {
          state = ElbWorkflowState.WORKFLOW_FAILED
              .withReason("Not enough resources to create loadbalancer")
              .withStatusCode(400);
        } else if (ex != null && ex instanceof InvalidConfigurationRequestException) {
          state = ElbWorkflowState.WORKFLOW_FAILED
              .withReason("Requested configuration change is invalid")
              .withStatusCode(400);
        } else {
          state = ElbWorkflowState.WORKFLOW_FAILED;
          LOG.error("Failed to create the loadbalancer. Rollback completed", e);
        }
      }

      @Override
      protected void doFinally() throws Throwable {
        if (state == ElbWorkflowState.WORKFLOW_RUNNING) {
          state = ElbWorkflowState.WORKFLOW_SUCCESS;
        }
      }
    };
  }

  @Asynchronous
  private Promise<String> getSecurityGroupId(
      Promise<SecurityGroupSetupActivityResult> securityGroup) {
    return Promise.asPromise(securityGroup.get().getCreatedGroupId());
  }

  @Asynchronous
  private Promise<Boolean> doAdmission(Promise<Boolean> admissionControl) {
    if (admissionControl.get()) {
      return Promise.asPromise(true);
    } else {
      task.cancel(null);
      return Promise.asPromise(false);
    }
  }

  @Override
  public ElbWorkflowState getState() {
    return state;
  }
}