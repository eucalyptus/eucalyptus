/*************************************************************************
 * Copyright 2016 Ent. Services Development Corporation LP
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
import com.eucalyptus.simplequeue.common.SimpleQueue;
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
