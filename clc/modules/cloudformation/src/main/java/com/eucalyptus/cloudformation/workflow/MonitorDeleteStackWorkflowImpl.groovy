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
public class MonitorDeleteStackWorkflowImpl implements MonitorDeleteStackWorkflow {

  @Delegate
  WorkflowOperations<StackActivityClient> workflowOperations = SwfWorkflowOperations.of(StackActivityClient)
  WorkflowUtils workflowUtils = new WorkflowUtils( workflowOperations )

  final String baseWorkflowType = StackWorkflowEntity.WorkflowType.DELETE_STACK_WORKFLOW.toString();
  final String monitoringWorkflowType = StackWorkflowEntity.WorkflowType.MONITOR_DELETE_STACK_WORKFLOW.toString();

  @Override
  public void monitorDeleteStack(final String stackId, final String accountId, final String effectiveUserId, final int deletedStackVersion) {
    CommonMonitorPromises commonMonitorStackWorkflowImpl = new CommonMonitorPromises(
      accountId: accountId,
      stackId: stackId,
      baseWorkflowType: baseWorkflowType,
      monitoringWorkflowType: monitoringWorkflowType,
      stackVersion: deletedStackVersion,
      stackActionAsNoun: "deletion",
      expectedTimeoutStackStatus: "DELETE_IN_PROGRESS",
      expectedTimeoutClosure: { String statusReason->
        waitFor(activities.logMessage("INFO", monitoringWorkflowType + " next step is cancelling outstanding resources, marking stack 'DELETE_FAILED', and finishing workflow. (Stack id: ${stackId})")) {
          Promise<String> cancelOutstandingResources = activities.cancelOutstandingDeleteResources(stackId, accountId, "Resource deletion cancelled", deletedStackVersion);
          Promise<String> setStackStatusPromise = waitFor(cancelOutstandingResources) {
            activities.setStackStatusIfLatest(stackId, accountId,
              Status.DELETE_FAILED.toString(), statusReason, deletedStackVersion)
          };
          return waitFor(setStackStatusPromise) {
            return activities.createGlobalStackEvent(stackId,
              accountId, Status.DELETE_FAILED.toString(), statusReason, deletedStackVersion);
          }
        }
      }
    );
    commonMonitorStackWorkflowImpl.monitor();
  }
}
