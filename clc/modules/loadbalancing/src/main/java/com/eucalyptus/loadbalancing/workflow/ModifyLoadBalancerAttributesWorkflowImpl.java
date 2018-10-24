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
      new LoadBalancingActivitiesClientImpl(null, LoadBalancingJsonDataConverter.getDefault(), null);
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
