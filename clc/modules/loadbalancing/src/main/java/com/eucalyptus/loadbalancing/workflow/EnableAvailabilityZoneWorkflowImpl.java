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
      new LoadBalancingActivitiesClientImpl();

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
