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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;

import org.apache.log4j.Logger;

import com.amazonaws.services.simpleworkflow.flow.ActivitySchedulingOptions;
import com.amazonaws.services.simpleworkflow.flow.ActivityTaskTimedOutException;
import com.amazonaws.services.simpleworkflow.flow.DecisionContextProvider;
import com.amazonaws.services.simpleworkflow.flow.DecisionContextProviderImpl;
import com.amazonaws.services.simpleworkflow.flow.WorkflowClock;
import com.amazonaws.services.simpleworkflow.flow.annotations.Asynchronous;
import com.amazonaws.services.simpleworkflow.flow.core.AndPromise;
import com.amazonaws.services.simpleworkflow.flow.core.OrPromise;
import com.amazonaws.services.simpleworkflow.flow.core.Promise;
import com.amazonaws.services.simpleworkflow.flow.core.Promises;
import com.amazonaws.services.simpleworkflow.flow.core.Settable;
import com.amazonaws.services.simpleworkflow.flow.core.TryCatchFinally;
import com.eucalyptus.component.annotation.ComponentPart;
import com.eucalyptus.loadbalancing.common.LoadBalancing;
import com.eucalyptus.loadbalancing.common.msgs.LoadBalancerServoDescription;
import com.eucalyptus.loadbalancing.common.msgs.LoadBalancerServoDescriptions;
import com.google.common.collect.Lists;

/**
 * @author Sang-Min Park (sangmin.park@hpe.com)
 *
 */
@ComponentPart(LoadBalancing.class)
public class UpdateLoadBalancerWorkflowImpl implements UpdateLoadBalancerWorkflow {
  private static Logger    LOG     = 
      Logger.getLogger(  UpdateLoadBalancerWorkflowImpl.class );

  private final LoadBalancingActivitiesClient client =
      new LoadBalancingActivitiesClientImpl();
  
  private final UpdateLoadBalancerWorkflowSelfClient selfClient =
      new UpdateLoadBalancerWorkflowSelfClientImpl();
  
  private final LoadBalancingVmActivitiesClient vmClient =
      new LoadBalancingVmActivitiesClientImpl();
  private DecisionContextProvider contextProvider = 
      new DecisionContextProviderImpl();
  final WorkflowClock clock = 
      contextProvider.getDecisionContext().getWorkflowClock();

  private Settable<Boolean> signalReceived = new Settable<Boolean>();
  
  private TryCatchFinally task = null;
  private final int MAX_UPDATE_PER_WORKFLOW = 10;
  private final int UPDATE_PERIOD_SEC = 60;
  private String accountId = null;
  private String loadbalancer = null;
  
  @Override
  public void updateLoadBalancer(final String accountId, final String loadbalancer) {
    this.accountId = accountId;
    this.loadbalancer = loadbalancer;
    final Settable<Boolean> exception = new Settable<Boolean>();
    task = new TryCatchFinally() {
      @Override
      protected void doTry() throws Throwable {
        updateInstancesPeriodic(0);
      }

      @Override
      protected void doCatch(final Throwable ex) throws Throwable {
        if (ex instanceof ActivityTaskTimedOutException) {
          ;
        }else if (ex instanceof CancellationException) {
          ;
        }else {
          ;
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
          selfClient.updateLoadBalancer(accountId, loadbalancer);
        }  
      }
    };
  }

  @Asynchronous
  private void updateInstancesPeriodic(final int count,
                                       Promise<?>... waitFor) {
    if (signalReceived.isReady() || count >= MAX_UPDATE_PER_WORKFLOW) {
      return;
    }
    // get map of instance->ELB description
    final Promise<Map<String, LoadBalancerServoDescription>> lookup =
            client.lookupLoadBalancerDescription(this.accountId, this.loadbalancer);
    doUpdateInstances(count, lookup);
  }

  @Asynchronous
  private void doUpdateInstances(final int count,
                                 final Promise<Map<String, LoadBalancerServoDescription>> lookup) {
    // update each instance
    final Map<String, LoadBalancerServoDescription> description = lookup.get();
    final List<Promise<Void>> results = Lists.newArrayList();
    for(final String instanceId : description.keySet()) {
      final LoadBalancerServoDescription desc = description.get(instanceId);
      // update each servo VM
      final String message = prepareMessage(desc);
      final ActivitySchedulingOptions scheduler =
              new ActivitySchedulingOptions();
      scheduler.setTaskList(instanceId);
      scheduler.setScheduleToCloseTimeoutSeconds(120L); /// account for VM startup delay
      scheduler.setStartToCloseTimeoutSeconds(10L);
      results.add(vmClient.setLoadBalancer(message, scheduler));
    }
    final Promise<List<Void>> updated = Promises.listOfPromisesToPromise(results);

    final Promise<Void> timer = startDaemonTimer(UPDATE_PERIOD_SEC);
    final OrPromise waitOrSignal = new OrPromise(timer, signalReceived);
    updateInstancesPeriodic(count+1, new AndPromise(waitOrSignal, updated));
  }

  private String prepareMessage(final LoadBalancerServoDescription lbDescription) {
    final LoadBalancerServoDescriptions lbDescriptions = new LoadBalancerServoDescriptions();
    lbDescriptions.setMember(new ArrayList<LoadBalancerServoDescription>());
    lbDescriptions.getMember().add(lbDescription);
    final String encoded =
            VmWorkflowMarshaller.marshalLoadBalancer(lbDescriptions);
    return encoded;
  }
  
  @Asynchronous(daemon = true)
  private Promise<Void> startDaemonTimer(int seconds) {
      Promise<Void> timer = clock.createTimer(seconds);
      return timer;
  }

  @Override
  public void updateImmediately() {
    if(!signalReceived.isReady())
      signalReceived.set(true);
  }
}