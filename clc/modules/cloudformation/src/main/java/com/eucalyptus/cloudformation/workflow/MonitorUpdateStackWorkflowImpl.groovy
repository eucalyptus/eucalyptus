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
import com.eucalyptus.cloudformation.common.CloudFormation
import com.eucalyptus.cloudformation.InternalFailureException
import com.eucalyptus.cloudformation.entity.StackWorkflowEntity
import com.eucalyptus.cloudformation.entity.Status
import com.eucalyptus.component.annotation.ComponentPart
import com.eucalyptus.simpleworkflow.common.workflow.WorkflowUtils
import com.netflix.glisten.WorkflowOperations
import com.netflix.glisten.impl.swf.SwfWorkflowOperations
import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import org.apache.log4j.Logger

@ComponentPart(CloudFormation)
@CompileStatic(TypeCheckingMode.SKIP)
public class MonitorUpdateStackWorkflowImpl implements MonitorUpdateStackWorkflow {
  private static final Logger LOG = Logger.getLogger(MonitorUpdateStackWorkflowImpl.class);

  @Delegate
  WorkflowOperations<StackActivityClient> workflowOperations = SwfWorkflowOperations.of(StackActivityClient)
  WorkflowUtils workflowUtils = new WorkflowUtils( workflowOperations )

  final String baseWorkflowType = StackWorkflowEntity.WorkflowType.UPDATE_STACK_WORKFLOW.toString();
  final String monitoringWorkflowType = StackWorkflowEntity.WorkflowType.MONITOR_UPDATE_STACK_WORKFLOW.toString();

  @Override
  public void monitorUpdateStack(final String stackId, final String accountId, final String effectiveUserId,
                                 final int updatedStackVersion, final String outerStackArn) {
    CommonMonitorPromises commonMonitorStackWorkflowImpl = new CommonMonitorPromises(
      accountId: accountId,
      stackId: stackId,
      baseWorkflowType: baseWorkflowType,
      monitoringWorkflowType: monitoringWorkflowType,
      stackVersion: updatedStackVersion,
      stackActionAsNoun: "update",
      expectedTimeoutStackStatus: "UPDATE_IN_PROGRESS",
      expectedTimeoutClosure: { String statusReason ->
        waitFor(activities.logMessage("INFO", monitoringWorkflowType + " next step is cancelling outstanding resources, and setting stack status to 'UPDATE_ROLLBACK_IN_PROGRESS'. Determining next steps. (Stack id: ${stackId})")) {
          Promise<String> cancelOutstandingCreateResources = activities.cancelOutstandingCreateResources(stackId, accountId, "Resource update cancelled.", updatedStackVersion);
          Promise<String> cancelOutstandingUpdateResources = waitFor(cancelOutstandingCreateResources) {
            activities.cancelOutstandingUpdateResources(stackId, accountId, "Resource update cancelled.", updatedStackVersion);
          };
          Promise<String> setStackStatusPromise = waitFor(cancelOutstandingUpdateResources) {
            activities.setStackStatusIfLatest(stackId, accountId,
              Status.UPDATE_ROLLBACK_IN_PROGRESS.toString(), statusReason, updatedStackVersion)
          };
          waitFor(setStackStatusPromise) {
            determineRollbackOrCleanupAction(Status.UPDATE_ROLLBACK_IN_PROGRESS.toString(),
              outerStackArn, stackId, accountId, effectiveUserId);
          }
        }
      },
      otherSupportedStackStatusMap: [
        "UPDATE_FAILED" : {
          determineRollbackOrCleanupAction("UPDATE_FAILED", outerStackArn, stackId, accountId, effectiveUserId);
        },
        "UPDATE_ROLLBACK_IN_PROGRESS" : {
          determineRollbackOrCleanupAction("UPDATE_ROLLBACK_IN_PROGRESS", outerStackArn, stackId, accountId, effectiveUserId);
        },
        "UPDATE_COMPLETE_CLEANUP_IN_PROGRESS": {
          determineRollbackOrCleanupAction("UPDATE_COMPLETE_CLEANUP_IN_PROGRESS", outerStackArn, stackId, accountId, effectiveUserId);
        }
      ]
    );
    commonMonitorStackWorkflowImpl.monitor();
  }

  private Promise<String> determineRollbackOrCleanupAction(String revisedStackStatus, String outerStackArn, String stackId, String accountId, String effectiveUserId) {
    if (outerStackArn != null) {
      return activities.logMessage("INFO", monitoringWorkflowType + " refers to an inner stack, another workflow will handle rollback or cleanup. (Stack id: ${stackId})");
    } else if (Status.UPDATE_COMPLETE_CLEANUP_IN_PROGRESS.toString().equals(revisedStackStatus)) {
      waitFor(activities.logMessage("INFO", monitoringWorkflowType + " next step is to kick off an update cleanup workflow. (Stack id: ${stackId})")) {
        return activities.kickOffUpdateCleanupStackWorkflow(stackId, accountId, effectiveUserId)
      };
    } else if (Status.UPDATE_FAILED.toString().equals(revisedStackStatus) || Status.UPDATE_ROLLBACK_IN_PROGRESS.toString().equals(revisedStackStatus)) {
      waitFor(activities.logMessage("INFO", monitoringWorkflowType + " next step is to kick off an update rollback workflow. (Stack id: ${stackId})")) {
        return activities.kickOffUpdateRollbackStackWorkflow(stackId, accountId, outerStackArn, effectiveUserId);
      }
    } else {
      throw new InternalFailureException("Unexpected stack status " + revisedStackStatus + " during update monitoring");
    }
  }
}
