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

import com.eucalyptus.loadbalancing.common.msgs.PolicyDescription;
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
    final Promise<Map<String, LoadBalancerServoDescription>> loadbalancer =
            client.lookupLoadBalancerDescription(this.accountId, this.loadbalancer);
    // each policy is a large text and SWF has a  limit on input/output text;
    // so we push the policy in iteration
    final Promise<List<String>> policies =
            client.listLoadBalancerPolicies(this.accountId, this.loadbalancer);
    final Promise<Void> policyUpdate = updatePolicies(loadbalancer, policies);
    doUpdateInstances(count, loadbalancer, policyUpdate); // push LB definition after policies are pushed
  }

  @Asynchronous
  Promise<Void> updatePolicies(final Promise<Map<String, LoadBalancerServoDescription>> loadbalancer,
                                                     final Promise<List<String>> policyNames) {
    final List<Promise<PolicyDescription>> policies = Lists.newArrayList();
    for (final String policyName : policyNames.get()) {
      policies.add( client.getLoadBalancerPolicy(Promise.asPromise(this.accountId),
              Promise.asPromise(this.loadbalancer),
              Promise.asPromise(policyName)) );
    }

    final Promise<Void> policyUpdated =
            pushPolicies(loadbalancer, Promises.listOfPromisesToPromise(policies));
    return policyUpdated;
  }

  @Asynchronous
  private void doUpdateInstances(final int count,
                                 final Promise<Map<String, LoadBalancerServoDescription>> loadbalancer,
                                 final Promise<Void> policyUpdated) {
    // update each instance
    final Map<String, LoadBalancerServoDescription> description = loadbalancer.get();

    final List<Promise<Void>> result = Lists.newArrayList();
    for(final String instanceId : description.keySet()) {
      final LoadBalancerServoDescription desc = description.get(instanceId);
      result.add(doUpdateInstance(instanceId, desc));
    }

    final Promise<Void> timer = startDaemonTimer(UPDATE_PERIOD_SEC);
    final OrPromise waitOrSignal = new OrPromise(timer, signalReceived);
    updateInstancesPeriodic(count+1,
            new AndPromise(waitOrSignal, Promises.listOfPromisesToPromise(result)));
  }

  @Asynchronous
  private Promise<Void> doUpdateInstance(final String instanceId,
                                         final LoadBalancerServoDescription desc) {
    // update each servo VM
    final String message = encodeLoadBalancer(desc);
    final Settable<Void> result = new Settable<Void>();
    final Settable<String> failure = new Settable<String>();
    new TryCatchFinally() {
      protected void doTry() throws Throwable {
        final ActivitySchedulingOptions scheduler =
                new ActivitySchedulingOptions();
        scheduler.setTaskList(instanceId);
        scheduler.setScheduleToCloseTimeoutSeconds(120L); /// account for VM startup delay
        scheduler.setStartToCloseTimeoutSeconds(10L);
        result.chain(vmClient.setLoadBalancer(message, scheduler));
      }

      protected void doCatch(Throwable e) {
        failure.set(instanceId);
      }

      protected void doFinally() throws Throwable {
        if (!failure.isReady()) {
          failure.set(null);
        }
      }
    };
    return checkInstanceFailure(failure);
  }

  @Asynchronous
  private Promise<Void> pushPolicies(final Promise<Map<String, LoadBalancerServoDescription>> loadbalancer,
                              final Promise<List<PolicyDescription>> policies) {
    final Map<String, LoadBalancerServoDescription> description = loadbalancer.get();
    final List<Promise<Void>> result = Lists.newArrayList();
    for(final String instanceId : description.keySet()) {
      result.add(pushPoliciesToVM(instanceId, policies));
    }
    return done(Promises.listOfPromisesToPromise(result));
  }

  @Asynchronous
  private Promise<Void> pushPoliciesToVM(final String instanceId, final Promise<List<PolicyDescription>> policies) {
    final List<Promise<Void>> result =  Lists.newArrayList();
    final Settable<String> failure = new Settable<String>();
    new TryCatchFinally() {
      protected void doTry() throws Throwable {
        final ActivitySchedulingOptions scheduler =
                new ActivitySchedulingOptions();
        scheduler.setTaskList(instanceId);
        scheduler.setScheduleToCloseTimeoutSeconds(120L); /// account for VM startup delay
        scheduler.setStartToCloseTimeoutSeconds(10L);
        for (final PolicyDescription p : policies.get()) {
          result.add(vmClient.setPolicy(encodePolicy(p), scheduler));
        }
      }

      protected void doCatch(Throwable e) {
        failure.set(instanceId);
      }

      protected void doFinally() throws Throwable {
        if (!failure.isReady()) {
          failure.set(null);
        }
      }
    };
    return checkInstanceFailure(failure);
  }

  @Asynchronous
  private Promise<Void> checkInstanceFailure(Promise<String> failure) {
    final String instanceId = failure.get();
    if (instanceId != null) {
      return client.recordInstanceTaskFailure(instanceId);
    }
    return Promise.Void();
  }

  @Asynchronous
  private Promise<Void> done(Promise<List<Void>> result) {
    return Promise.Void();
  }

  private String encodePolicy(final PolicyDescription policy) {
    return VmWorkflowMarshaller.marshalPolicy(policy);
  }

  private String encodeLoadBalancer(final LoadBalancerServoDescription lbDescription) {
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