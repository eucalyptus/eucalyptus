/*************************************************************************
 * Copyright 2013-2014 Ent. Services Development Corporation LP
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
  void monitorUpdateStack(String stackId, String accountId, String effectiveUserId, int updatedStackVersion, String outerStackArn) {
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
              determineRollbackOrCleanupAction(revisedStackStatus, outerStackArn, stackId, accountId, effectiveUserId);
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

  private Promise<String> determineRollbackOrCleanupAction(String stackStatus, String outerStackArn, String stackId, String accountId, String effectiveUserId) {

    if (outerStackArn != null) {
      return promiseFor("");
    } else if (Status.UPDATE_COMPLETE_CLEANUP_IN_PROGRESS.toString().equals(stackStatus)) {
      return activities.kickOffUpdateCleanupStackWorkflow(stackId, accountId, effectiveUserId);
    } else if (Status.UPDATE_FAILED.toString().equals(stackStatus) || Status.UPDATE_ROLLBACK_IN_PROGRESS.toString().equals(stackStatus)) {
      return activities.kickOffUpdateRollbackStackWorkflow(stackId, accountId, outerStackArn, effectiveUserId);
    } else {
      throw new InternalFailureException("Unexpected stack status " + stackStatus + " during update monitoring");
    }
  }

}
