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

package com.eucalyptus.cloudformation.workflow

import com.eucalyptus.cloudformation.common.CloudFormation
import com.eucalyptus.cloudformation.entity.StackWorkflowEntity
import com.eucalyptus.cloudformation.entity.Status
import com.eucalyptus.component.annotation.ComponentPart
import com.eucalyptus.simpleworkflow.common.workflow.WorkflowUtils
import com.netflix.glisten.WorkflowOperations
import com.netflix.glisten.impl.swf.SwfWorkflowOperations
import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode

@ComponentPart(CloudFormation)
@CompileStatic(TypeCheckingMode.SKIP)
public class MonitorUpdateCleanupStackWorkflowImpl implements MonitorUpdateCleanupStackWorkflow {

  @Delegate
  WorkflowOperations<StackActivityClient> workflowOperations = SwfWorkflowOperations.of(StackActivityClient)
  WorkflowUtils workflowUtils = new WorkflowUtils( workflowOperations )


  final String baseWorkflowType = StackWorkflowEntity.WorkflowType.UPDATE_CLEANUP_STACK_WORKFLOW.toString();
  final String monitoringWorkflowType = StackWorkflowEntity.WorkflowType.MONITOR_UPDATE_CLEANUP_STACK_WORKFLOW.toString();

  @Override
  public void monitorUpdateCleanupStack(final String stackId, final String accountId, final int updatedStackVersion) {
    CommonMonitorPromises commonMonitorStackWorkflowImpl = new CommonMonitorPromises(
      accountId: accountId,
      stackId: stackId,
      baseWorkflowType: baseWorkflowType,
      monitoringWorkflowType: monitoringWorkflowType,
      stackVersion: updatedStackVersion,
      stackActionAsNoun: "update cleanup",
      expectedTimeoutStackStatus: "UPDATE_COMPLETE_CLEANUP_IN_PROGRESS",
      expectedTimeoutClosure: { String statusReason->
        waitFor(activities.logMessage("INFO", monitoringWorkflowType + " next steps are: detaching outstanding resources, marking stack 'UPDATE_COMPLETE', finishing workflow.  (Stack id: ${stackId})")) {
          final String errorMessage = statusReason + "  It may be the case that one or more resources that previously had been associated with the stack were not deleted.  They have been detached from the stack.";
          return waitFor(activities.createGlobalStackEvent(stackId, accountId, Status.UPDATE_COMPLETE.toString(), errorMessage, updatedStackVersion)) {
            activities.finalizeUpdateCleanupStack(stackId, accountId, errorMessage, updatedStackVersion);
          }
        }
      }
    );
    commonMonitorStackWorkflowImpl.monitor();
  }
}
