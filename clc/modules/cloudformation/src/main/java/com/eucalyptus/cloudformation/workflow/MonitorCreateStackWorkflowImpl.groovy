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
import com.eucalyptus.cloudformation.CloudFormation
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