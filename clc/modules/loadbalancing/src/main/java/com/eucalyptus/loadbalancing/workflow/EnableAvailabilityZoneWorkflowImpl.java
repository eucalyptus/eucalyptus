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
import java.util.Map;
import java.util.concurrent.CancellationException;

import org.apache.log4j.Logger;

import com.amazonaws.services.simpleworkflow.flow.annotations.Asynchronous;
import com.amazonaws.services.simpleworkflow.flow.core.Promise;
import com.amazonaws.services.simpleworkflow.flow.core.TryCatchFinally;
import com.eucalyptus.component.annotation.ComponentPart;
import com.eucalyptus.loadbalancing.common.LoadBalancing;
/**
 * @author Sang-Min Park (sangmin.park@hpe.com)
 *
 */
@ComponentPart(LoadBalancing.class)
public class EnableAvailabilityZoneWorkflowImpl
    implements EnableAvailabilityZoneWorkflow {
  private static Logger    LOG     = Logger.getLogger(  EnableAvailabilityZoneWorkflowImpl.class );
  
  final LoadBalancingActivitiesClient client = 
      new LoadBalancingActivitiesClientImpl(null, LoadBalancingJsonDataConverter.getDefault(), null);

  private ElbWorkflowState state = ElbWorkflowState.WORKFLOW_RUNNING;
  private Promise<List<String>> persistedZones = null;
  private Promise<AutoscalingGroupSetupActivityResult> autoscaleGroup = null;
  private TryCatchFinally task = null;
  
  @Override
  public void enableAvailabilityZone(final String accountId, final String loadbalancer,
      final List<String> availabilityZones, final Map<String, String> zoneToSubnetIdMap) {
    
    task = new TryCatchFinally() {
      @Override
      protected void doTry() throws Throwable {
      Promise<String> iamRole = 
          client.iamRoleSetup(Promise.asPromise(accountId), Promise.asPromise(loadbalancer));
      Promise<String> instanceProfile = 
          client.instanceProfileSetup(Promise.asPromise(accountId), Promise.asPromise(loadbalancer), iamRole);
      Promise<String> securityGroupName = 
          getSecurityGroup(client.securityGroupSetup(Promise.asPromise(accountId), Promise.asPromise(loadbalancer)));
      autoscaleGroup =
          client.autoscalingGroupSetup(Promise.asPromise(accountId), Promise.asPromise(loadbalancer), 
              instanceProfile, securityGroupName, Promise.asPromise(availabilityZones), 
              Promise.asPromise(zoneToSubnetIdMap));
      persistedZones =
          client.enableAvailabilityZonesPersistUpdatedZones(Promise.asPromise(accountId), 
              Promise.asPromise(loadbalancer), Promise.asPromise(availabilityZones), 
              Promise.asPromise(zoneToSubnetIdMap), autoscaleGroup);
      client.enableAvailabilityZonesPersistBackendInstanceState(Promise.asPromise(accountId), 
          Promise.asPromise(loadbalancer), persistedZones);
      }

      @Override
      protected void doCatch(Throwable e) throws Throwable {
        if ( e instanceof CancellationException) {
          LOG.warn("Workflow for enabling availability zones is cancelled");
          state = ElbWorkflowState.WORKFLOW_CANCELLED;
          return;
        }
        // rollback activities
        if(persistedZones!=null && persistedZones.isReady()) {
          client.enableAvailabilityZonesPersistUpdatedZonesRollback(Promise.asPromise(accountId), 
              Promise.asPromise(loadbalancer), persistedZones);
        }
        if(autoscaleGroup!=null && autoscaleGroup.isReady()) {
          client.autoscalingGroupSetupRollback(Promise.asPromise(accountId), 
              Promise.asPromise(loadbalancer), autoscaleGroup);
        }
        // OK to not rollback iamRole, instance profile, and security group activities.
        // The executed activities here are only for reading the resource names
        
        state = ElbWorkflowState.WORKFLOW_FAILED;
        LOG.error("Workflow for enabling availability zone has failed", e);
      }

      @Override
      protected void doFinally() throws Throwable {
        if (state == ElbWorkflowState.WORKFLOW_RUNNING)
          state = ElbWorkflowState.WORKFLOW_SUCCESS;
      }
    };
  }
  
  @Asynchronous
  private String waitForVm(Promise<String> activity) {
    return activity.get();
  }
  
  @Asynchronous
  private  Promise<String> getSecurityGroup(Promise<SecurityGroupSetupActivityResult> sgroupActivity) {
    return Promise.asPromise(sgroupActivity.get().getGroupName());
  }

  @Override
  public ElbWorkflowState getState() {
    return state;
  }
}
