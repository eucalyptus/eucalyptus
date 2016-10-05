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
 *
 */
@ComponentPart(LoadBalancing.class)
public class CloudWatchPutMetricWorkflowImpl implements CloudWatchPutMetricWorkflow {
  private static Logger    LOG     = Logger.getLogger(  CloudWatchPutMetricWorkflowImpl.class );
  private String accountId = null;
  private String loadbalancer = null;

  final LoadBalancingVmActivitiesClient vmClient = 
      new LoadBalancingVmActivitiesClientImpl();
  final LoadBalancingActivitiesClient client =
      new LoadBalancingActivitiesClientImpl();
  final CloudWatchPutMetricWorkflowSelfClient selfClient =
      new CloudWatchPutMetricWorkflowSelfClientImpl();

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
        }else if (ex instanceof CancellationException){
          ;
        }else {
          LOG.warn("Put metric workflow failed", ex);
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

   final Promise<List<String>> servoInstances = client.lookupServoInstances(this.accountId, this.loadbalancer);
   // collect metrics from servos
   doPutMetric(count, servoInstances);
  }
  
  @Asynchronous
  private void doPutMetric(final int count, final Promise<List<String>> servoInstances) {
    final Map<String, Promise<String>> metrics = Maps.newHashMap();
    final List<String> instances = servoInstances.get();
    for(final String instanceId : instances) {
      final ActivitySchedulingOptions scheduler =
          new ActivitySchedulingOptions();
      scheduler.setTaskList(instanceId);
      scheduler.setScheduleToCloseTimeoutSeconds(120L); /// account for VM startup delay
      scheduler.setStartToCloseTimeoutSeconds(10L);
      metrics.put(instanceId, vmClient.getCloudWatchMetrics(scheduler));
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
    putCloudWatchMetricPeriodic(count+1,
            new AndPromise(timer, Promises.listOfPromisesToPromise(activities)));
  }


  @Asynchronous(daemon = true)
  private Promise<Void> startDaemonTimer(int seconds) {
    Promise<Void> timer = clock.createTimer(seconds);
    return timer;
  }
}