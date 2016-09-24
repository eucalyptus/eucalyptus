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

import com.amazonaws.services.simpleworkflow.flow.core.Promise;
import com.amazonaws.services.simpleworkflow.flow.core.TryCatchFinally;
import com.eucalyptus.component.annotation.ComponentPart;
import com.eucalyptus.loadbalancing.common.msgs.Listener;
import com.eucalyptus.loadbalancing.common.LoadBalancing;

/**
 * @author Sang-Min Park (sangmin.park@hpe.com)
 *
 */
@ComponentPart(LoadBalancing.class)
public class CreateLoadBalancerListenersWorkflowImpl implements CreateLoadBalancerListenersWorkflow {
  private static Logger    LOG     = Logger.getLogger(  CreateLoadBalancerListenersWorkflowImpl.class );
  
  final LoadBalancingActivitiesClient client = 
      new LoadBalancingActivitiesClientImpl();
  private ElbWorkflowState state = 
      ElbWorkflowState.WORKFLOW_RUNNING;
  TryCatchFinally task = null;
 
  private Promise<AuthorizeSSLCertificateActivityResult> authCert = null;
  private Promise<AuthorizeIngressRuleActivityResult> ingressRule = null;
  @Override
  public void createLoadBalancerListeners(final String accountId, final String loadbalancer,
      final Listener[] listeners) {
    task = new TryCatchFinally(){
      @Override
      protected void doTry() throws Throwable {
        Promise<Void> checkSSL = client.createListenerCheckSSLCertificateId(accountId, loadbalancer, listeners);
        authCert = 
            client.createListenerAuthorizeSSLCertificate(Promise.asPromise(accountId), 
                Promise.asPromise(loadbalancer), Promise.asPromise(listeners), checkSSL);
        ingressRule = client.createListenerAuthorizeIngressRule(Promise.asPromise(accountId), 
            Promise.asPromise(loadbalancer), Promise.asPromise(listeners), checkSSL);
        
        Promise<Void> healthCheck = client.createListenerUpdateHealthCheckConfig(Promise.asPromise(accountId),
            Promise.asPromise(loadbalancer), Promise.asPromise(listeners), checkSSL);
        Promise<Void> sslDefaultPolicy = client.createListenerAddDefaultSSLPolicy(Promise.asPromise(accountId),
            Promise.asPromise(loadbalancer), Promise.asPromise(listeners), checkSSL);
      }
      
      @Override
      protected void doCatch(Throwable e) throws Throwable {
        if ( e instanceof CancellationException) {
          LOG.warn("Workflow for creating ELB listener is cancelled");
          state = ElbWorkflowState.WORKFLOW_CANCELLED;
          return;
        }
        // rollback activities
        if(authCert!=null && authCert.isReady()) {
          client.createListenerAuthorizeSSLCertificateRollback(Promise.asPromise(accountId), 
              Promise.asPromise(loadbalancer), authCert);
        }
        
        if(ingressRule!=null && ingressRule.isReady()) {
          client.createListenerAuthorizeIngressRuleRollback(Promise.asPromise(accountId), 
              Promise.asPromise(loadbalancer), ingressRule);
        }
       
        state = ElbWorkflowState.WORKFLOW_FAILED;
        LOG.error("Workflow for creating loadbalancer listener failed", e);
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
