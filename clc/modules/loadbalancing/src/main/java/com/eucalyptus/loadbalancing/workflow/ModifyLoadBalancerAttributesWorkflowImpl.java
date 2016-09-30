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
import com.eucalyptus.loadbalancing.common.msgs.LoadBalancerAttributes;

/**
 * @author Sang-Min Park (sangmin.park@hpe.com)
 *
 */
@ComponentPart(LoadBalancing.class)
public class ModifyLoadBalancerAttributesWorkflowImpl
    implements ModifyLoadBalancerAttributesWorkflow {
private static Logger    LOG     = Logger.getLogger(  ModifyLoadBalancerAttributesWorkflowImpl.class );
  
  final LoadBalancingActivitiesClient client = 
      new LoadBalancingActivitiesClientImpl();
  private ElbWorkflowState state = 
      ElbWorkflowState.WORKFLOW_RUNNING;
  TryCatchFinally task = null;
  private  Promise<AccessLogPolicyActivityResult> policyCreator = null;
  private String accountNumber = null;
  private String loadbalancer = null;

  @Override
  public void modifyLoadBalancerAttributes(final String accountNumber,
      final String loadbalancer, final LoadBalancerAttributes attributes) {
    this.accountNumber = accountNumber;
    this.loadbalancer = loadbalancer;
    task = new TryCatchFinally() {
      @Override
      protected void doTry() throws Throwable {
        if(attributes.getAccessLog() == null)
          return;
        
        if(attributes.getAccessLog().getEnabled()) {
          policyCreator =
              client.modifyLoadBalancerAttributesCreateAccessLogPolicy(accountNumber, 
                  loadbalancer, attributes.getAccessLog().getEnabled(),
                      attributes.getAccessLog().getS3BucketName(),
                      attributes.getAccessLog().getS3BucketPrefix() != null ?  attributes.getAccessLog().getS3BucketPrefix() : "",
                      attributes.getAccessLog().getEmitInterval());
        } else {
          final Promise<Void> policyRemover =
              client.modifyLoadBalancerAttributesDeleteAccessLogPolicy(accountNumber, 
                  loadbalancer, attributes.getAccessLog().getEnabled(),
                      attributes.getAccessLog().getS3BucketName(),
                      attributes.getAccessLog().getS3BucketPrefix() != null ?  attributes.getAccessLog().getS3BucketPrefix() : "",
                      attributes.getAccessLog().getEmitInterval());
        }
        
        final Promise<Void> persistence =
            client.modifyLoadBalancerAttributesPersistAttributes(accountNumber, 
                loadbalancer, attributes.getAccessLog().getEnabled(),
                    attributes.getAccessLog().getS3BucketName(),
                    attributes.getAccessLog().getS3BucketPrefix() != null ?  attributes.getAccessLog().getS3BucketPrefix() : "",
                    attributes.getAccessLog().getEmitInterval());
      }

      @Override
      protected void doCatch(Throwable e) throws Throwable {
        if ( e instanceof CancellationException) {
          LOG.warn("Workflow for modifying attributes is cancelled");
          state = ElbWorkflowState.WORKFLOW_CANCELLED;
          return;
        }
        
        // rollback if necessary
        if (policyCreator != null) {
          rollback(policyCreator);
        }
        
        state = ElbWorkflowState.WORKFLOW_FAILED;
        LOG.error("Workflow for modifying attributes has failed", e);        
      }

      @Override
      protected void doFinally() throws Throwable {
        if (state == ElbWorkflowState.WORKFLOW_RUNNING)
          state = ElbWorkflowState.WORKFLOW_SUCCESS;        
      }      
    };
  }

  @Asynchronous
  void rollback(final Promise<AccessLogPolicyActivityResult> policyActivity) {
    final AccessLogPolicyActivityResult result = policyActivity.get();
    if(result.isShouldRollback()) {
      client.modifyLoadBalancerAttributesCreateAccessLogPolicyRollback(Promise.asPromise(accountNumber), 
          Promise.asPromise(loadbalancer), policyCreator);
    }
  }
  
  @Override
  public ElbWorkflowState getState() {
    return state;
  }
}
