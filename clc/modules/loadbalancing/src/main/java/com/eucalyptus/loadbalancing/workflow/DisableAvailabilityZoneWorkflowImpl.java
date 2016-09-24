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
      new LoadBalancingActivitiesClientImpl();
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
