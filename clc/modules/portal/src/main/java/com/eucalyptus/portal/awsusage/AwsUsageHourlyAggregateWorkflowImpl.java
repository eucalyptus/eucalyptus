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
package com.eucalyptus.portal.awsusage;

import com.amazonaws.services.simpleworkflow.flow.annotations.Asynchronous;
import com.amazonaws.services.simpleworkflow.flow.core.Promise;
import com.amazonaws.services.simpleworkflow.flow.core.Promises;
import com.amazonaws.services.simpleworkflow.flow.core.TryCatchFinally;
import com.eucalyptus.component.annotation.ComponentPart;

import com.eucalyptus.portal.BillingProperties;
import com.eucalyptus.portal.common.Portal;
import com.eucalyptus.portal.workflow.AwsUsageActivitiesClient;
import com.eucalyptus.portal.workflow.AwsUsageActivitiesClientImpl;
import com.eucalyptus.portal.workflow.AwsUsageHourlyAggregateWorkflow;
import com.eucalyptus.portal.workflow.AwsUsageRecord;
import com.eucalyptus.portal.workflow.BillingWorkflowState;
import com.eucalyptus.simpleworkflow.common.client.Hourly;
import com.google.common.collect.Lists;
import org.apache.log4j.Logger;
import java.util.List;
import java.util.Map;

// first hour of a day is skipped because daily aggregate workflow replaces hourly run
@Hourly(value = AwsUsageHourlyWorkflowStarter.class, minute = 0, skipHour = {0})
@ComponentPart(Portal.class)
public class AwsUsageHourlyAggregateWorkflowImpl implements AwsUsageHourlyAggregateWorkflow {
  private static Logger LOG     =
          Logger.getLogger(  AwsUsageHourlyAggregateWorkflowImpl.class );
  final AwsUsageActivitiesClient client =
          new AwsUsageActivitiesClientImpl();
  private BillingWorkflowState state =
          BillingWorkflowState.WORKFLOW_RUNNING;
  TryCatchFinally task = null;

  @Override
  public void aggregateHourly() {
    task = new TryCatchFinally() {
      @Override
      protected void doTry() throws Throwable {
        doAggregate();
      }

      @Override
      protected void doCatch(Throwable e) throws Throwable {
        waitCleanup(client.cleanupQueues());
        state = BillingWorkflowState.WORKFLOW_FAILED;
        LOG.error("Workflow for aggregating AWS usage hourly records has failed: ", e);
      }

      @Override
      protected void doFinally() throws Throwable {
        if (state == BillingWorkflowState.WORKFLOW_RUNNING)
          state = BillingWorkflowState.WORKFLOW_SUCCESS;
      }
    };
  }

  @Asynchronous
  void doAggregate() {
    /* HOW THIS WORKFLOW WORKS?
       inspect all events and create temporary queue for each unique account
       copy (&delete) events to the temporary account queues
       for each account queue; do
         for each record types (service/operation); do
           count # of unique resources in the event
           return list of records for the type
           write the records into persistence storage (i.e., cassandra)
       delete account queue
    */
    final Promise<Map<String, String>> queueForAccounts =
            client.createAccountQueues(BillingProperties.SENSOR_QUEUE_NAME);

    final Promise<List<Void>> processed =
            writeSerialized(
                    processAccountQueues(queueForAccounts)
            );
    waitFor(
            client.deleteAccountQueues(
                    getQueues(queueForAccounts),
                    processed)
    );
  }

  @Asynchronous
  Promise<List<List<AwsUsageRecord>>> processAccountQueues(final Promise<Map<String, String>> queueForAccounts) {
    final List<Promise<List<AwsUsageRecord>>> accountRecords = Lists.newArrayList();
    for (final String accountId : queueForAccounts.get().keySet()) {
      final String queueName = queueForAccounts.get().get(accountId);
      accountRecords.add(
              client.getAwsReportHourlyUsageRecord(
                      Promise.asPromise(accountId),
                      Promise.asPromise(queueName)
              )
      );
    }
    return Promises.listOfPromisesToPromise(accountRecords);
  }

  @Asynchronous
  Promise<List<Void>> writeSerialized( final Promise<List<List<AwsUsageRecord>>> accountRecords ) {
    final List<Promise<Void>> result = Lists.newArrayList();
    Promise<Void> run = Promise.Void();
    for (final List<AwsUsageRecord> accountRecord : accountRecords.get() ) {
      run = client.writeAwsReportUsage( accountRecord, run);
      result.add(run);
    }
    return Promises.listOfPromisesToPromise(result);
  }

  @Asynchronous
  Promise<List<String>> getQueues(final Promise<Map<String, String>> queueMap) {
    return Promise.asPromise(Lists.newArrayList(queueMap.get().values()));
  }

  @Asynchronous
  void waitFor(final Promise<Void> task) {
    LOG.debug("Finished writing AWS usage hourly records");
  }

  @Asynchronous
  void waitCleanup(final Promise<Void> task) { LOG.error("Failed writing AWS usage hourly records"); }

  @Override
  public BillingWorkflowState getState() {
    return state;
  }
}
