/*************************************************************************
 * Copyright 2017 Ent. Services Development Corporation LP
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
package com.eucalyptus.portal.instanceusage;

import com.amazonaws.services.simpleworkflow.flow.annotations.Asynchronous;
import com.amazonaws.services.simpleworkflow.flow.core.Promise;
import com.amazonaws.services.simpleworkflow.flow.core.Promises;
import com.amazonaws.services.simpleworkflow.flow.core.TryCatchFinally;
import com.eucalyptus.component.annotation.ComponentPart;
import com.eucalyptus.portal.BillingProperties;
import com.eucalyptus.portal.common.Portal;
import com.eucalyptus.portal.workflow.BillingWorkflowState;
import com.eucalyptus.portal.workflow.InstanceLogActivitiesClient;
import com.eucalyptus.portal.workflow.InstanceLogActivitiesClientImpl;
import com.eucalyptus.portal.workflow.InstanceLogWorkflow;
import com.eucalyptus.simpleworkflow.common.client.Hourly;
import com.google.common.collect.Lists;
import org.apache.log4j.Logger;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Hourly(InstanceLogWorkflowStarter.class)
@ComponentPart(Portal.class)
public class InstanceLogWorkflowImpl implements InstanceLogWorkflow {
  private static Logger LOG     =
          Logger.getLogger(  InstanceLogWorkflowImpl.class );
  final InstanceLogActivitiesClient client =
          new InstanceLogActivitiesClientImpl();
  private BillingWorkflowState state =
          BillingWorkflowState.WORKFLOW_RUNNING;
  TryCatchFinally task = null;

  @Override
  public void logInstanceHourly() {
    task = new TryCatchFinally() {
      @Override
      protected void doTry() throws Throwable {
        doLog();
      }

      @Override
      protected void doCatch(Throwable e) throws Throwable {
        waitCleanup(client.cleanupQueues());
        state = BillingWorkflowState.WORKFLOW_FAILED;
        LOG.error("Workflow logging instance usage has failed: ", e);
      }

      @Override
      protected void doFinally() throws Throwable {
        if (state == BillingWorkflowState.WORKFLOW_RUNNING)
          state = BillingWorkflowState.WORKFLOW_SUCCESS;
      }
    };
  }

  @Asynchronous
  public void doLog() {
    final Promise<Map<String, String>> accountQueues =
            client.distributeEvents(BillingProperties.INSTANCE_HOUR_SENSOR_QUEUE_NAME);
    deleteQueues( accountQueues, processQueues(accountQueues) );
  }

  @Asynchronous
  public Promise<List<Void>> processQueues(final Promise<Map<String, String>> futureQueues) {
    final Map<String, String> queues = futureQueues.get();
    final List<Promise<Void>> result = Lists.newArrayList();

    Promise<Void> run = Promise.Void();
    for (final String accountId : queues.keySet()) {
      final String queueName = queues.get(accountId);
      run = client.persist(
              Promise.asPromise(accountId),
              Promise.asPromise(queueName),
              run); // persist is serialized
      result.add(run);
    }
    return Promises.listOfPromisesToPromise(result);
  }

  @Asynchronous
  public void deleteQueues(final Promise<Map<String, String>> futureQueues, Promise<List<Void>> processed) {
    final List<String> queues = futureQueues.get().values().stream()
            .collect(Collectors.toList());
    waitFor(client.deleteQueues(queues));
  }

  @Asynchronous
  void waitFor(final Promise<Void> task) {
    LOG.debug("Finished writing instance hour records");
  }

  @Asynchronous
  void waitCleanup(final Promise<Void> task) { LOG.error("Failed writing instance hour records"); }


  @Override
  public BillingWorkflowState getState() {
    return this.state;
  }
}
