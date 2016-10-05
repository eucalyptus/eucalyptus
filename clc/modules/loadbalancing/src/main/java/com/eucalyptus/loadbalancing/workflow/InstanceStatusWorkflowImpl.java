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

import com.amazonaws.services.simpleworkflow.flow.core.OrPromise;
import com.eucalyptus.loadbalancing.common.msgs.HealthCheck;
import org.apache.log4j.Logger;

import com.amazonaws.services.simpleworkflow.flow.ActivitySchedulingOptions;
import com.amazonaws.services.simpleworkflow.flow.ActivityTaskTimedOutException;
import com.amazonaws.services.simpleworkflow.flow.DecisionContextProvider;
import com.amazonaws.services.simpleworkflow.flow.DecisionContextProviderImpl;
import com.amazonaws.services.simpleworkflow.flow.WorkflowClock;
import com.amazonaws.services.simpleworkflow.flow.annotations.Asynchronous;
import com.amazonaws.services.simpleworkflow.flow.core.AndPromise;
import com.amazonaws.services.simpleworkflow.flow.core.Promise;
import com.amazonaws.services.simpleworkflow.flow.core.Promises;
import com.amazonaws.services.simpleworkflow.flow.core.Settable;
import com.amazonaws.services.simpleworkflow.flow.core.TryCatchFinally;
import com.eucalyptus.component.annotation.ComponentPart;
import com.eucalyptus.loadbalancing.common.LoadBalancing;
import com.google.common.collect.Lists;

/**
 * @author Sang-Min Park (sangmin.park@hpe.com)
 *
 */
@ComponentPart(LoadBalancing.class)
public class InstanceStatusWorkflowImpl implements InstanceStatusWorkflow {
  private static Logger    LOG     = Logger.getLogger(  InstanceStatusWorkflowImpl.class );
  
  private String accountId = null;
  private String loadbalancer = null;

  final LoadBalancingVmActivitiesClient vmClient = 
      new LoadBalancingVmActivitiesClientImpl();
  final LoadBalancingActivitiesClient client =
      new LoadBalancingActivitiesClientImpl();
  final InstanceStatusWorkflowSelfClient selfClient =
      new InstanceStatusWorkflowSelfClientImpl();

  TryCatchFinally task = null;
  private DecisionContextProvider contextProvider
       = new DecisionContextProviderImpl();
  private Settable<Boolean> signalReceived = new Settable<Boolean>();
  final WorkflowClock clock =
          contextProvider.getDecisionContext().getWorkflowClock();

  // continuous workflow generates enormous amount of history
  // ideally we should use continueAsNewWorkflow, but the current EUCA SWF lacks it
  // TODO: implement SWF:continueAsNewWorkflow 
  private int MAX_POLL_PER_WORKFLOW = 10;
  static public final int MIN_POLLING_PERIOD_SEC = 10;
  private int pollingPeriodSec = MIN_POLLING_PERIOD_SEC;
  
  @Override
  public void pollInstanceStatus(final String accountId, final String loadbalancer) {
    this.accountId = accountId;
    this.loadbalancer = loadbalancer;    
    final Settable<Boolean> exception = new Settable<Boolean>();
    task = new TryCatchFinally() {
      @Override
      protected void doTry() throws Throwable {
       final Promise<HealthCheck> healthCheck =
               client.lookupLoadBalancerHealthCheck(Promise.asPromise(accountId),
                       Promise.asPromise(loadbalancer));
       pollInstanceStatus(healthCheck);
      }
      
      @Override
      protected void doCatch(Throwable ex) throws Throwable {
        if (ex instanceof ActivityTaskTimedOutException) {
          LOG.warn("Instance polling task timed out");
        }else if (ex instanceof CancellationException){
          ;
        }else {
          LOG.warn("Instance polling workflow failed", ex);
        }
        exception.set(true);
        throw ex;
      }

      @Override
      protected void doFinally() throws Throwable {
        if(exception.isReady() && exception.get())
          return;
        else if (task.isCancelRequested())
          return;
        else {
          selfClient.getSchedulingOptions().setTagList(
              Lists.newArrayList(String.format("account:%s", accountId), 
                  String.format("loadbalancer:%s", loadbalancer)));
          selfClient.pollInstanceStatus(accountId, loadbalancer);
        }  
      }
    };
  }

  @Asynchronous
  private void pollInstanceStatus(final Promise<HealthCheck> healthCheck) {
    final HealthCheck hc = healthCheck.get();
    if ( hc != null && hc.getInterval() != null ) {
      this.pollingPeriodSec = Math.max(MIN_POLLING_PERIOD_SEC, hc.getInterval().intValue());
    }
    pollInstanceStatusPeriodic(0);
  }
  
  @Asynchronous 
  private void pollInstanceStatusPeriodic(final int count,
                                          Promise<?>... waitFor) {
   if (signalReceived.isReady()  || count >= MAX_POLL_PER_WORKFLOW) {
      return;
   }
   final Promise<List<String>> servoInstances = client.lookupServoInstances(this.accountId, this.loadbalancer);
   doPollStatus(count, servoInstances);   
  }
  
  @Asynchronous
  private void doPollStatus(final int count, final Promise<List<String>> servoInstances) {
    final List<String> instances = servoInstances.get();
    final List<Promise<Void>> activities = Lists.newArrayList();
    for(final String instanceId : instances) {
      final ActivitySchedulingOptions scheduler =
          new ActivitySchedulingOptions();
      scheduler.setTaskList(instanceId);
      scheduler.setScheduleToCloseTimeoutSeconds(120L); /// account for VM startup delay
      scheduler.setStartToCloseTimeoutSeconds(10L);
      activities.add(
              client.updateInstanceStatus(
                      Promise.asPromise(accountId),
                      Promise.asPromise(loadbalancer),
                      client.filterInstanceStatus(
                              Promise.asPromise(accountId),
                              Promise.asPromise(loadbalancer),
                              Promise.asPromise(instanceId),
                              vmClient.getInstanceStatus(scheduler)
                      )
              )
      );
    }

    final Promise<Void> timer = startDaemonTimer(this.pollingPeriodSec);
    final OrPromise waitOrSignal = new OrPromise(timer, signalReceived);
    pollInstanceStatusPeriodic(count+1,
            new AndPromise(
                    Promises.listOfPromisesToPromise(activities),
                    waitOrSignal
            )
    );
  }

  @Asynchronous(daemon = true)
  private Promise<Void> startDaemonTimer(int seconds) {
    Promise<Void> timer = clock.createTimer(seconds);
    return timer;
  }

  @Override
  public void pollImmediately() {
    if(!signalReceived.isReady())
      signalReceived.set(true);
  }
}
