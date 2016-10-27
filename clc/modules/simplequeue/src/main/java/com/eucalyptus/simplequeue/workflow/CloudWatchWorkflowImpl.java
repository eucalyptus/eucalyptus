/*************************************************************************
 * Copyright 2009-2015 Eucalyptus Systems, Inc.
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
package com.eucalyptus.simplequeue.workflow;

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
import com.eucalyptus.simplequeue.SimpleQueue;
import com.eucalyptus.simplequeue.workflow.CloudWatchActivitiesClient;
import com.eucalyptus.simplequeue.workflow.CloudWatchActivitiesClientImpl;
import com.eucalyptus.simplequeue.workflow.CloudWatchWorkflowSelfClient;
import com.eucalyptus.simplequeue.workflow.CloudWatchWorkflowSelfClientImpl;
import com.google.common.collect.Lists;
import org.apache.log4j.Logger;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CancellationException;

/**
 * Created by ethomas on 10/26/16.
 */
@ComponentPart(SimpleQueue.class)
public class CloudWatchWorkflowImpl implements CloudWatchWorkflow {
  private static final Logger LOG = Logger.getLogger(CloudWatchWorkflowImpl.class);
  private DecisionContextProvider contextProvider
    = new DecisionContextProviderImpl();

  private CloudWatchWorkflowSelfClient workflowSelfClient
    = new CloudWatchWorkflowSelfClientImpl();

  private WorkflowClock clock
    = contextProvider.getDecisionContext().getWorkflowClock();

  final CloudWatchActivitiesClient activitiesClient =
    new CloudWatchActivitiesClientImpl();

  TryCatchFinally task = null;

  private int MAX_PUT_PER_WORKFLOW = 10;
  private final int PUT_PERIOD_SEC = 300;

  @Override
  public void sendMetrics() {
    final Settable<Boolean> exception = new Settable<Boolean>();
    task = new TryCatchFinally() {
      @Override
      protected void doTry() throws Throwable {
        performPeriodicAction(0);
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
        if (exception.isReady() && exception.get())
          return;
        else if (task.isCancelRequested())
          return;
        else {
          workflowSelfClient.sendMetrics();
        }
      }
    };
  }

  @Asynchronous
  private void performPeriodicAction(final int count, Promise<?>... waitFor) {
    if (count >= MAX_PUT_PER_WORKFLOW) {
      return;
    }

    final Promise<Collection<String>> partitions = activitiesClient.getPartitions();
    doSendMetrics(count, partitions);
  }

  @Asynchronous
  private void doSendMetrics(final int count, Promise<Collection<String>> partitionsPromise) {
    final List<Promise<Void>> activities = Lists.newArrayList();
    final Collection<String> partitions = partitionsPromise.get();
    for (final String partition : partitions) {
      activities.add(activitiesClient.performWork(Promise.asPromise(partition)));
    }

    final Promise<Void> timer = startDaemonTimer(PUT_PERIOD_SEC);
    performPeriodicAction(count + 1,
      new AndPromise(timer, Promises.listOfPromisesToPromise(activities)));
  }


  @Asynchronous(daemon = true)
  private Promise<Void> startDaemonTimer(int seconds) {
    Promise<Void> timer = clock.createTimer(seconds);
    return timer;
  }

}
