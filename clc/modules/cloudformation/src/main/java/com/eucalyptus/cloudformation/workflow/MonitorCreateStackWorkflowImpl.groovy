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
import com.eucalyptus.cloudformation.entity.StackWorkflowEntity
import com.eucalyptus.cloudformation.entity.Status
import com.eucalyptus.component.annotation.ComponentPart
import com.netflix.glisten.WorkflowOperations
import com.netflix.glisten.impl.swf.SwfWorkflowOperations
import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode

@ComponentPart(CloudFormation)
@CompileStatic(TypeCheckingMode.SKIP)
public class MonitorCreateStackWorkflowImpl implements MonitorCreateStackWorkflow {

  @Delegate
  WorkflowOperations<StackActivityClient> workflowOperations = SwfWorkflowOperations.of(StackActivityClient)
  WorkflowUtils workflowUtils = new WorkflowUtils( workflowOperations )

  final String baseWorkflowType = StackWorkflowEntity.WorkflowType.CREATE_STACK_WORKFLOW.toString();
  final String monitoringWorkflowType = StackWorkflowEntity.WorkflowType.MONITOR_CREATE_STACK_WORKFLOW.toString();

  @Override
  public void monitorCreateStack(final String stackId, final String stackName, final String accountId, final String accountAlias,
                                 final String resourceDependencyManagerJson, final String effectiveUserId, final String onFailure,
                                 final int createdStackVersion) {
    CommonMonitorPromises commonMonitorStackWorkflowImpl = new CommonMonitorPromises(
      accountId: accountId,
      stackId: stackId,
      baseWorkflowType: baseWorkflowType,
      monitoringWorkflowType: monitoringWorkflowType,
      stackVersion: createdStackVersion,
      stackActionAsNoun: "creation",
      expectedTimeoutStackStatus: "CREATE_IN_PROGRESS",
      expectedTimeoutClosure: { String statusReason->
        waitFor(activities.logMessage("INFO", monitoringWorkflowType + " next step is cancelling outstanding resources. Determining next steps. (Stack id: ${stackId})")) {
          Promise<String> cancelOutstandingResources = activities.cancelOutstandingCreateResources(stackId, accountId, "Resource creation cancelled", createdStackVersion);
          Promise<String> setStackStatusPromise = waitFor(cancelOutstandingResources) {
            activities.setStackStatusIfLatest(stackId, accountId,
              Status.CREATE_FAILED.toString(), statusReason, createdStackVersion)
          };
          return waitFor(setStackStatusPromise) {
            Promise<String> createGlobalStackEventPromise = activities.createGlobalStackEvent(stackId,
              accountId, Status.CREATE_FAILED.toString(), statusReason, createdStackVersion);
            waitFor(createGlobalStackEventPromise) {
              performRollback(stackId, stackName, accountId, accountAlias, resourceDependencyManagerJson, effectiveUserId, onFailure, createdStackVersion);
            }
          }
        }
      },
      otherSupportedStackStatusMap : [
        "CREATE_FAILED" : {
          performRollback(stackId, stackName, accountId, accountAlias, resourceDependencyManagerJson, effectiveUserId, onFailure, createdStackVersion);
        }
      ]
    );
    commonMonitorStackWorkflowImpl.monitor();
  }

  private Promise<String> performRollback(final String stackId, final String stackName, final String accountId,
                                          final String accountAlias, final String resourceDependencyManagerJson,
                                          final String effectiveUserId, final String onFailure, final int createdStackVersion) {
    if ("DO_NOTHING".equals(onFailure)) {
      return waitFor(activities.logMessage("INFO", "MonitorCreateWorkflow next step is to do nothing (per on-failure option).  Finishing workflow. (Stack id: ${stackId}")) {
        promiseFor("")
      };
    } else if ("DELETE".equals(onFailure)) {
      return waitFor(activities.logMessage("INFO", monitoringWorkflowType + " next step is to delete the stack (per on-failure option).  Finishing workflow and kicking off delete stack workflow. (Stack id: ${stackId}")) {
        activities.kickOffDeleteStackWorkflow(effectiveUserId, stackId, stackName, accountId, accountAlias, resourceDependencyManagerJson, createdStackVersion, "");
      };
    } else if ("ROLLBACK".equals(onFailure)) {
      return waitFor(activities.logMessage("INFO", monitoringWorkflowType + " next step is to roll back the stack (per on-failure option).  Finishing workflow and kicking off rollback stack workflow. (Stack id: ${stackId}")) {
        activities.kickOffRollbackStackWorkflow(effectiveUserId, stackId, stackName, accountId, accountAlias, resourceDependencyManagerJson, createdStackVersion);
      };
    } else {
      return waitFor(activities.logMessage("ERROR", monitoringWorkflowType + " invalid onFailure value " + onFailure + ".  Finishing workflow. (Stack id: ${stackId}")) {
        promiseFor("")
      };
    }
  }

}
