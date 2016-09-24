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
 *
 */
@ComponentPart(LoadBalancing.class)
public class CreateLoadBalancerWorkflowImpl implements CreateLoadBalancerWorkflow {
  private static Logger    LOG     = Logger.getLogger(  CreateLoadBalancerWorkflowImpl.class );
  final LoadBalancingActivitiesClient client = 
      new LoadBalancingActivitiesClientImpl();
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
  public void createLoadBalancer(final String accountId, final String loadbalancer, final String[] availabilityZones) {
      task = new TryCatchFinally() {
      @Override
      protected void doTry() throws Throwable {
        admission = doAdmission(
            client.createLbAdmissionControl(accountId, loadbalancer, availabilityZones));
        roleName = client.iamRoleSetup(accountId, loadbalancer, admission);
        instanceProfile = 
            client.instanceProfileSetup(Promise.asPromise(accountId), Promise.asPromise(loadbalancer), roleName);
        iamPolicy = 
            client.iamPolicySetup(Promise.asPromise(accountId), Promise.asPromise(loadbalancer), roleName);
        securityGroup = 
            client.securityGroupSetup(accountId, loadbalancer, admission);
        tag = client.createLbTagCreator(Promise.asPromise(accountId), Promise.asPromise(loadbalancer), getSecurityGroupId(securityGroup));
      }

      @Override
      protected void doCatch(Throwable e) throws Throwable {
        if ( e instanceof CancellationException) {
          LOG.warn("Creating loadbalancer stops due to failed admission control");
          state = ElbWorkflowState.WORKFLOW_CANCELLED;
          return;
        }
        
        // rollback activities
        Promise<Void> tagRollback = null;
        if ( tag!=null && tag.isReady() ) {
          tagRollback=client.createLbTagCreatorRollback(tag);
        }
        if ( securityGroup!=null && securityGroup.isReady()) {
          client.securityGroupSetupRollback(Promise.asPromise(accountId), 
              Promise.asPromise(loadbalancer), securityGroup, tagRollback);
        }
        
        state = ElbWorkflowState.WORKFLOW_FAILED;
        LOG.error("Failed to create the loadbalancer. Rollback completed", e);
      }

      @Override
      protected void doFinally() throws Throwable {
        if (state == ElbWorkflowState.WORKFLOW_RUNNING)
          state = ElbWorkflowState.WORKFLOW_SUCCESS;
      }
    };
  }

  @Asynchronous
  private Promise<String> getSecurityGroupId(Promise<SecurityGroupSetupActivityResult> securityGroup) {
    return Promise.asPromise(securityGroup.get().getCreatedGroupId());
  }
  
  @Asynchronous
  private Promise<Boolean> doAdmission (Promise<Boolean> admissionControl) {
    if(admissionControl.get()) {
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