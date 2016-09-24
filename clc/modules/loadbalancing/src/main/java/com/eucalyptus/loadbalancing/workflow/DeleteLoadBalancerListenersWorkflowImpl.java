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

import com.amazonaws.services.simpleworkflow.flow.core.TryCatchFinally;
import com.eucalyptus.component.annotation.ComponentPart;
import com.eucalyptus.loadbalancing.common.LoadBalancing;
import com.google.common.collect.Lists;

/**
 * @author Sang-Min Park (sangmin.park@hpe.com)
 *
 */
@ComponentPart(LoadBalancing.class)
public class DeleteLoadBalancerListenersWorkflowImpl
    implements DeleteLoadBalancerListenersWorkflow {
  private static Logger    LOG     = Logger.getLogger(  DeleteLoadBalancerListenersWorkflowImpl.class );
  
  final LoadBalancingActivitiesClient client = 
      new LoadBalancingActivitiesClientImpl();
  private ElbWorkflowState state = 
      ElbWorkflowState.WORKFLOW_RUNNING;
  TryCatchFinally task = null;

  @Override
  public void deleteLoadBalancerListeners(final String accountId, final String loadbalancer,
      final Integer[] portsToDelete) {
    task = new TryCatchFinally() {
      @Override
      protected void doTry() throws Throwable {
        client.deleteListenerRevokeSSLCertificatePolicy(accountId, loadbalancer, Lists.newArrayList(portsToDelete));
        client.deleteListenerRevokeIngressRule(accountId, loadbalancer, Lists.newArrayList(portsToDelete));
      }

      @Override
      protected void doCatch(Throwable e) throws Throwable {
        if ( e instanceof CancellationException) {
          LOG.warn("Workflow for deleting ELB listener is cancelled");
          state = ElbWorkflowState.WORKFLOW_CANCELLED;
          return;
        }
        state = ElbWorkflowState.WORKFLOW_FAILED;
        LOG.error("Failed to delete the listener", e);
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
