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

import com.amazonaws.services.simpleworkflow.flow.core.AndPromise;
import org.apache.log4j.Logger;

import com.amazonaws.services.simpleworkflow.flow.ActivitySchedulingOptions;
import com.amazonaws.services.simpleworkflow.flow.ActivityTaskTimedOutException;
import com.amazonaws.services.simpleworkflow.flow.DecisionContextProvider;
import com.amazonaws.services.simpleworkflow.flow.DecisionContextProviderImpl;
import com.amazonaws.services.simpleworkflow.flow.WorkflowClock;
import com.amazonaws.services.simpleworkflow.flow.annotations.Asynchronous;
import com.amazonaws.services.simpleworkflow.flow.core.Promise;
import com.amazonaws.services.simpleworkflow.flow.core.Promises;
import com.amazonaws.services.simpleworkflow.flow.core.Settable;
import com.amazonaws.services.simpleworkflow.flow.core.TryCatchFinally;
import com.eucalyptus.component.annotation.ComponentPart;
import com.eucalyptus.loadbalancing.common.LoadBalancing;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * @author Sang-Min Park (sangmin.park@hpe.com)
 */
@ComponentPart(LoadBalancing.class)
public class CloudWatchPutMetricWorkflowImpl implements CloudWatchPutMetricWorkflow {
  private static Logger LOG = Logger.getLogger(CloudWatchPutMetricWorkflowImpl.class);
  private String accountId = null;
  private String loadbalancer = null;

  final LoadBalancingVmActivitiesClient vmClient =
      new LoadBalancingVmActivitiesClientImpl(null, LoadBalancingJsonDataConverter.getDefault(),
          null);
  final LoadBalancingActivitiesClient client =
      new LoadBalancingActivitiesClientImpl(null, LoadBalancingJsonDataConverter.getDefault(),
          null);
  final CloudWatchPutMetricWorkflowSelfClient selfClient =
      new CloudWatchPutMetricWorkflowSelfClientImpl(null,
          LoadBalancingJsonDataConverter.getDefault(), null);

  TryCatchFinally task = null;
  private DecisionContextProvider contextProvider
      = new DecisionContextProviderImpl();

  // continuous workflow generates enourmous amount of history
  // ideally we should use continueAsNewWorkflow, but the current EUCA SWF lacks it
  // TODO: implement SWF:continueAsNewWorkflow 
  private int MAX_PUT_PER_WORKFLOW = 10;
  private final int PUT_PERIOD_SEC = 30;
  final WorkflowClock clock =
      contextProvider.getDecisionContext().getWorkflowClock();

  @Override
  public void putCloudWatchMetric(final String accountId, final String loadbalancer) {
    this.accountId = accountId;
    this.loadbalancer = loadbalancer;
    final Settable<Boolean> exception = new Settable<Boolean>();

    task = new TryCatchFinally() {
      @Override
      protected void doTry() throws Throwable {
        putCloudWatchMetricPeriodic(0);
      }

      @Override
      protected void doCatch(Throwable ex) throws Throwable {
        if (ex instanceof ActivityTaskTimedOutException) {
          LOG.warn("Put metric activity timed out");
        } else if (ex instanceof CancellationException) {
          ;
        } else {
          LOG.warn("Put metric workflow failed", ex);
        }
        exception.set(true);
        throw ex;
      }

      @Override
      protected void doFinally() throws Throwable {
        if (exception.isReady() && exception.get()) {
          return;
        } else if (task.isCancelRequested()) {
          return;
        } else {
          selfClient.getSchedulingOptions().setTagList(
              Lists.newArrayList(String.format("account:%s", accountId),
                  String.format("loadbalancer:%s", loadbalancer)));
          selfClient.putCloudWatchMetric(accountId, loadbalancer);
        }
      }
    };
  }

  @Asynchronous
  private void putCloudWatchMetricPeriodic(final int count, Promise<?>... waitFor) {
    if (count >= MAX_PUT_PER_WORKFLOW) {
      return;
    }

    final Promise<List<String>> servoInstances =
        client.lookupServoInstances(this.accountId, this.loadbalancer);
    // collect metrics from servos
    doPutMetric(count, servoInstances);
  }

  @Asynchronous
  private void doPutMetric(final int count, final Promise<List<String>> servoInstances) {
    final Map<String, Promise<String>> metrics = Maps.newHashMap();
    final List<String> instances = servoInstances.get();
    for (final String instanceId : instances) {
      metrics.put(instanceId, getCloudWatchMetricsFromVM(instanceId));
    }

    final Promise<Map<String, String>> metricMap = Promises.mapOfPromisesToPromise(metrics);
    final List<Promise<Void>> activities = Lists.newArrayList();
    activities.add(
        client.putCloudWatchMetrics(
            Promise.asPromise(this.accountId), Promise.asPromise(this.loadbalancer), metricMap)
    );
    activities.add(
        client.putCloudWatchInstanceHealth(this.accountId, this.loadbalancer)
    );

    final Promise<Void> timer = startDaemonTimer(PUT_PERIOD_SEC);
    putCloudWatchMetricPeriodic(count + 1,
        new AndPromise(timer, Promises.listOfPromisesToPromise(activities)));
  }

  @Asynchronous
  private Promise<String> getCloudWatchMetricsFromVM(final String instanceId) {
    final Settable<String> failure = new Settable<String>();
    final Settable<String> result = new Settable<String>();

    new TryCatchFinally() {
      protected void doTry() throws Throwable {

        final ActivitySchedulingOptions scheduler =
            new ActivitySchedulingOptions();
        scheduler.setTaskList(instanceId);
        scheduler.setScheduleToCloseTimeoutSeconds(120L); /// account for VM startup delay
        scheduler.setStartToCloseTimeoutSeconds(10L);
        result.chain(vmClient.getCloudWatchMetrics(scheduler));
      }

      protected void doCatch(Throwable e) {
        failure.set(instanceId);
      }

      protected void doFinally() throws Throwable {
        if (result.isReady()) {
          failure.set(null);
        } else if (failure.isReady()) {
          result.set(null);
        } else {
          result.set(null);
          failure.set(null);
        }
      }
    };
    return done(result, failure);
  }

  @Asynchronous
  private Promise<String> done(final Settable<String> result, final Settable<String> failure) {
    if (result.get() != null) {
      return Promise.asPromise(result.get());
    } else if (failure.get() != null) {
      return checkInstanceFailure(failure);
    } else {
      return Promise.asPromise(null); // this shouldn't happen
    }
  }

  @Asynchronous
  private Promise<String> checkInstanceFailure(Promise<String> failure) {
    final String instanceId = failure.get();
    if (instanceId != null) {
      client.recordInstanceTaskFailure(instanceId);
    }
    return Promise.asPromise(null);
  }

  @Asynchronous(daemon = true)
  private Promise<Void> startDaemonTimer(int seconds) {
    Promise<Void> timer = clock.createTimer(seconds);
    return timer;
  }
}