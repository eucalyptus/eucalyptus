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

package com.eucalyptus.cloudformation.workflow

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
