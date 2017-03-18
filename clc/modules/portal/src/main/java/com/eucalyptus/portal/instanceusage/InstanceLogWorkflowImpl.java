/*************************************************************************
 * (c) Copyright 2017 Hewlett Packard Enterprise Development Company LP
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
import com.google.common.collect.Lists;
import org.apache.log4j.Logger;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

    for (final String accountId : queues.keySet()) {
      final String queueName = queues.get(accountId);
      result.add(
              client.persist(
                      Promise.asPromise(accountId),
                      Promise.asPromise(queueName)
              )
      );
    }
    return Promises.listOfPromisesToPromise(result);
  }

  @Asynchronous
  public void deleteQueues(final Promise<Map<String, String>> futureQueues, Promise<List<Void>> processed) {
    final List<String> queues = futureQueues.get().values().stream()
            .collect(Collectors.toList());
    client.deleteQueues(queues);
  }

  @Override
  public BillingWorkflowState getState() {
    return this.state;
  }
}
