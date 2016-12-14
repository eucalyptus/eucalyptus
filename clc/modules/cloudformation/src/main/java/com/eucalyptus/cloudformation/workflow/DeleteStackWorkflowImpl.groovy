/*************************************************************************
 * Copyright 2013-2014 Eucalyptus Systems, Inc.
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

package com.eucalyptus.cloudformation.workflow

import com.amazonaws.services.simpleworkflow.flow.core.Promise
import com.amazonaws.services.simpleworkflow.flow.interceptors.ExponentialRetryPolicy
import com.eucalyptus.cloudformation.CloudFormation
import com.eucalyptus.cloudformation.entity.Status
import com.eucalyptus.component.annotation.ComponentPart
import com.netflix.glisten.WorkflowOperations
import com.netflix.glisten.impl.swf.SwfWorkflowOperations
import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import org.apache.log4j.Logger

@ComponentPart(CloudFormation)
@CompileStatic(TypeCheckingMode.SKIP)
public class DeleteStackWorkflowImpl implements DeleteStackWorkflow {
  private static final Logger LOG = Logger.getLogger(DeleteStackWorkflowImpl.class);
  @Delegate
  WorkflowOperations<StackActivityClient> workflowOperations = SwfWorkflowOperations.of(StackActivityClient);

  @Override
  public void deleteStack(String stackId, String accountId, String resourceDependencyManagerJson, String effectiveUserId, int deletedStackVersion, String retainedResourcesStr) {
    try {
      // cancel existing creae/monitor workflows...
      ExponentialRetryPolicy retryPolicy = new ExponentialRetryPolicy(10L).withMaximumRetryIntervalSeconds(10L).withExceptionsToRetry([RetryAfterConditionCheckFailedException.class])
      Promise<String> cancelWorkflowsPromise = activities.cancelCreateAndMonitorWorkflows(stackId);
      waitFor(cancelWorkflowsPromise) {
        waitFor(
          retry(retryPolicy) {
            activities.verifyCreateAndMonitorWorkflowsClosed(stackId);
          }
        ) {
          Promise<String> flattenStackPromise = activities.flattenStackForDelete(stackId, accountId);
          waitFor(flattenStackPromise) {
            new CommonDeleteRollbackPromises(workflowOperations,
              Status.DELETE_IN_PROGRESS.toString(),
              "User Initiated",
              Status.DELETE_FAILED.toString(),
              Status.DELETE_COMPLETE.toString(),
              true).getPromise(stackId, accountId, resourceDependencyManagerJson, effectiveUserId, deletedStackVersion, retainedResourcesStr);
          }
        }
      }
    } catch (Exception ex) {
      DeleteStackWorkflowImpl.LOG.error(ex);
      DeleteStackWorkflowImpl.LOG.debug(ex, ex);
    }
  }
}
