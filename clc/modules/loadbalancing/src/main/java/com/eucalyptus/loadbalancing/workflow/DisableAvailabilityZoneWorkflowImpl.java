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

import java.util.List;
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
public class DisableAvailabilityZoneWorkflowImpl
    implements DisableAvailabilityZoneWorkflow {
  private static Logger    LOG     = Logger.getLogger(  DisableAvailabilityZoneWorkflowImpl.class );
  
  final LoadBalancingActivitiesClient client = 
      new LoadBalancingActivitiesClientImpl(null, LoadBalancingJsonDataConverter.getDefault(), null);
  private ElbWorkflowState state = 
      ElbWorkflowState.WORKFLOW_RUNNING;
  TryCatchFinally task = null;

  private Promise<List<String>> updatedServos = null;
  private Promise<List<String>> updatedZones = null;
  @Override
  public void disableAvailabilityZone(final String accountId, final String loadbalancer,
      final List<String> availabilityZones) {

    task = new TryCatchFinally() {
      @Override
      protected void doTry() throws Throwable {
        updatedServos = client.disableAvailabilityZonesPersistRetiredServoInstances(accountId, 
            loadbalancer, availabilityZones);
        updatedZones = client.disableAvailabilityZonesUpdateAutoScalingGroup(accountId, 
            loadbalancer, availabilityZones);
        Promise<Void> zoneUpdateActivity = client.disableAvailabilityZonesPersistUpdatedZones(Promise.asPromise(accountId), 
            Promise.asPromise(loadbalancer), updatedZones);
        Promise<Void> backendUpdateActivity = client.disableAvailabilityZonesPersistBackendInstanceState(Promise.asPromise(accountId), 
            Promise.asPromise(loadbalancer), updatedZones);
      }
      
      @Override
      protected void doCatch(Throwable e) throws Throwable {
        if ( e instanceof CancellationException) {
          LOG.warn("Workflow for disabling ELB availability zone is cancelled");
          state = ElbWorkflowState.WORKFLOW_CANCELLED;
          return;
        }
        
        if(updatedServos != null && updatedServos.isReady()) {
          client.disableAvailabilityZonesPersistRetiredServoInstancesRollback(Promise.asPromise(accountId), 
              Promise.asPromise(loadbalancer), updatedServos);
        }
        
        if(updatedZones != null && updatedZones.isReady()) {
          client.disableAvailabilityZonesUpdateAutoScalingGroupRollback(Promise.asPromise(accountId), 
              Promise.asPromise(loadbalancer), updatedZones);
        }
        
        state = ElbWorkflowState.WORKFLOW_FAILED;
        LOG.error("Failed to disable ELB availability zone. Rollback complete.", e);
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
