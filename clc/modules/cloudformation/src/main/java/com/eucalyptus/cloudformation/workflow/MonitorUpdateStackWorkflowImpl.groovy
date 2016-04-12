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
import com.eucalyptus.cloudformation.InternalFailureException
import com.eucalyptus.cloudformation.entity.Status
import com.eucalyptus.component.annotation.ComponentPart
import com.eucalyptus.simpleworkflow.common.workflow.WorkflowUtils
import com.netflix.glisten.WorkflowOperations
import com.netflix.glisten.impl.swf.SwfWorkflowOperations
import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import org.apache.log4j.Logger

import java.util.concurrent.TimeUnit

@ComponentPart(CloudFormation)
@CompileStatic(TypeCheckingMode.SKIP)
public class MonitorUpdateStackWorkflowImpl implements MonitorUpdateStackWorkflow {
  private static final Logger LOG = Logger.getLogger(MonitorUpdateStackWorkflowImpl.class);

  @Delegate
  WorkflowOperations<StackActivityClient> workflowOperations = SwfWorkflowOperations.of(StackActivityClient)
  WorkflowUtils workflowUtils = new WorkflowUtils( workflowOperations )

  @Override
  void monitorUpdateStack(String stackId, String accountId, String oldResourceDependencyManagerJson, String resourceDependencyManagerJson, String effectiveUserId, int updatedStackVersion, String outerStackArn) {
    try {
      doTry {
        Promise<String> closeStatusPromise = workflowUtils.fixedPollWithTimeout((int) TimeUnit.DAYS.toSeconds(365), 10) {
          retry(new ExponentialRetryPolicy(2L).withMaximumAttempts(6)) {
            activities.getUpdateWorkflowExecutionCloseStatus(stackId)
          }
        }
        waitFor(closeStatusPromise) { String closedStatus ->
          if (!closedStatus) {
            throw new InternalFailureException("Stack update timeout stack id ${stackId}");
          }
          waitFor(activities.getStackStatus(stackId, accountId, updatedStackVersion)) { String stackStatus ->
            waitFor(dealWithPrematureClosure(closedStatus, stackStatus, stackId, accountId, updatedStackVersion)) { String revisedStackStatus ->
              determineRollbackOrCleanupAction(revisedStackStatus, outerStackArn, stackId, accountId, oldResourceDependencyManagerJson, resourceDependencyManagerJson, effectiveUserId, updatedStackVersion);
            }
          }
        }
      } withCatch { Throwable t->
        MonitorUpdateStackWorkflowImpl.LOG.error(t);
        MonitorUpdateStackWorkflowImpl.LOG.debug(t, t);
      }
    } catch (Exception ex) {
      MonitorUpdateStackWorkflowImpl.LOG.error(ex);
      MonitorUpdateStackWorkflowImpl.LOG.debug(ex, ex);
    }
  }

  private Promise<String> dealWithPrematureClosure(String closedStatus, String stackStatus, String stackId, String accountId, int updatedStackVersion ) {
    if (Status.UPDATE_IN_PROGRESS.toString().equals(stackStatus)) {
      // Once here, stack update has failed.  Only in some cases do we know why.
      String statusReason = "";
      if ("CANCELED".equals(closedStatus)) {
        statusReason = "Stack update was canceled by user.";
      } else if ("TERMINATED".equals(closedStatus)) {
        statusReason = "Stack update was terminated by user.";
      } else if ("TIMED_OUT".equals(closedStatus)) {
        statusReason = "Stack update timed out.";
      } else if ("COMPLETED".equals(closedStatus)) {
        statusReason = "";
      } else if ("FAILED".equals(closedStatus)) {
         statusReason = "Stack update workflow failed.";
      } else if ("CONTINUED_AS_NEW".equals(closedStatus)) {
        throw new InternalFailureException("Unsupported close status for workflow " + closedStatus);
      } else {
        throw new InternalFailureException("Unsupported close status for workflow " + closedStatus);
      }
      Promise<String> cancelOutstandingCreateResources = activities.cancelOutstandingCreateResources(stackId, accountId, "Resource update cancelled.", updatedStackVersion);
      Promise<String> cancelOutstandingUpdateResources = waitFor(cancelOutstandingCreateResources) {
        activities.cancelOutstandingUpdateResources(stackId, accountId, "Resource update cancelled.", updatedStackVersion);
      };
      Promise<String> setStackStatusPromise = waitFor(cancelOutstandingUpdateResources) {
        activities.setStackStatus(stackId, accountId,
            Status.UPDATE_ROLLBACK_IN_PROGRESS.toString(), statusReason, updatedStackVersion)
      };
      return waitFor(setStackStatusPromise) {
        return promiseFor(Status.UPDATE_ROLLBACK_IN_PROGRESS.toString());
      }
    } else {
      return promiseFor(stackStatus);
    }
  }

  private Promise<String> determineRollbackOrCleanupAction(String stackStatus, String outerStackArn, String stackId, String accountId,
                                                           String oldResourceDependencyManagerJson, String resourceDependencyManagerJson, String effectiveUserId, int updatedStackVersion ) {

    if (outerStackArn != null) {
      return promiseFor("");
    } else if (Status.UPDATE_COMPLETE_CLEANUP_IN_PROGRESS.toString().equals(stackStatus)) {
      return activities.kickOffUpdateCleanupStackWorkflow(stackId, accountId, effectiveUserId);
    } else if (Status.UPDATE_FAILED.toString().equals(stackStatus) || Status.UPDATE_ROLLBACK_IN_PROGRESS.toString().equals(stackStatus)) {
      return activities.kickOffUpdateRollbackStackWorkflow(stackId, accountId, effectiveUserId);
    } else {
      throw new InternalFailureException("Unexpected stack status " + stackStatus + " during update monitoring");
    }
  }

}
