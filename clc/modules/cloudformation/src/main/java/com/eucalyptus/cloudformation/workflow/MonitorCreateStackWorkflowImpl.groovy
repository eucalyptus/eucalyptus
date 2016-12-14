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
public class MonitorCreateStackWorkflowImpl implements MonitorCreateStackWorkflow {
  private static final Logger LOG = Logger.getLogger(MonitorCreateStackWorkflowImpl.class);

  @Delegate
  WorkflowOperations<StackActivityClient> workflowOperations = SwfWorkflowOperations.of(StackActivityClient)
  WorkflowUtils workflowUtils = new WorkflowUtils( workflowOperations )

  @Override
  void monitorCreateStack(String stackId, String accountId, String resourceDependencyManagerJson, String effectiveUserId, String onFailure, int createdStackVersion) {
    try {
      Promise<String> closeStatusPromise = workflowUtils.fixedPollWithTimeout( (int)TimeUnit.DAYS.toSeconds( 365 ), 30 ) {
        retry( new ExponentialRetryPolicy( 2L ).withMaximumAttempts( 6 ) ){
          activities.getCreateWorkflowExecutionCloseStatus(stackId)
        }
      }
      waitFor( closeStatusPromise ) { String closedStatus ->
        if ( !closedStatus ) {
          throw new InternalFailureException( "Stack create timeout stack id ${stackId}" );
        }
        waitFor( activities.getStackStatus(stackId, accountId, createdStackVersion) ) { String stackStatus ->
          determineRollbackAction( closedStatus, stackStatus, stackId, accountId, resourceDependencyManagerJson, effectiveUserId, onFailure, createdStackVersion );
        }
      }
    } catch (Exception ex) {
      MonitorCreateStackWorkflowImpl.LOG.error(ex);
      MonitorCreateStackWorkflowImpl.LOG.debug(ex, ex);
    }
  }

  private Promise<String> determineRollbackAction(String closedStatus, String stackStatus, String stackId, String accountId,
   String resourceDependencyManagerJson, String effectiveUserId, String onFailure, int createdStackVersion) {
    if ("CREATE_COMPLETE".equals(stackStatus) || (stackStatus == null)) { // stackStatus == null could happen if we did an update just after a create
      return promiseFor(""); // just done...
    } else if ("CREATE_IN_PROGRESS".equals(stackStatus)) {
      // Once here, stack creation has failed.  Only in some cases do we know why.
      String statusReason = "";
      if ("CANCELED".equals(closedStatus)) {
        statusReason = "Stack creation was canceled by user.";
      } else if ("TERMINATED".equals(closedStatus)) {
        statusReason = "Stack creation was terminated by user.";
      } else if ("TIMED_OUT".equals(closedStatus)) {
        statusReason = "Stack creation timed out.";
      } else if ("COMPLETED".equals(closedStatus)) {
        statusReason = "";
      } else if ("FAILED".equals(closedStatus)) {
        statusReason = "Stack creation workflow failed.";
      } else if ("CONTINUED_AS_NEW".equals(closedStatus)) {
        throw new InternalFailureException("Unsupported close status for workflow " + closedStatus);
      } else {
        throw new InternalFailureException("Unsupported close status for workflow " + closedStatus);
      }
      Promise<String> cancelOutstandingResources = activities.cancelOutstandingCreateResources(stackId, accountId, "Resource creation cancelled", createdStackVersion);
      Promise<String> setStackStatusPromise = waitFor(cancelOutstandingResources) {
        activities.setStackStatus(stackId, accountId,
          Status.CREATE_FAILED.toString(), statusReason, createdStackVersion)
      };
      return waitFor(setStackStatusPromise) {
        Promise<String> createGlobalStackEventPromise = activities.createGlobalStackEvent(stackId,
          accountId, Status.CREATE_FAILED.toString(), statusReason, createdStackVersion);
        waitFor(createGlobalStackEventPromise) {
          performRollback(stackId, accountId, resourceDependencyManagerJson, effectiveUserId, onFailure, createdStackVersion);
        }
      }
    } else if ("CREATE_FAILED".equals(stackStatus)) {
      return performRollback(stackId, accountId, resourceDependencyManagerJson, effectiveUserId, onFailure, createdStackVersion);
    } else {
      throw new InternalFailureException("Unexpected stack status " + stackStatus + " during create monitoring");
    }
  }

  private Promise<String> performRollback(String stackId, String accountId, String resourceDependencyManagerJson, String effectiveUserId, String onFailure, int createdStackVersion) {
    if ("DO_NOTHING".equals(onFailure)) {
      return promiseFor("");
    } else if ("DELETE".equals(onFailure)) {
      return new CommonDeleteRollbackPromises(workflowOperations,
        Status.DELETE_IN_PROGRESS.toString(),
        "Create stack failed.  Delete requested by user.",
        Status.DELETE_FAILED.toString(),
        Status.DELETE_COMPLETE.toString(),
        true).getPromise(stackId, accountId, resourceDependencyManagerJson, effectiveUserId, createdStackVersion, "");

    } else if ("ROLLBACK".equals(onFailure)) {
      return new CommonDeleteRollbackPromises(workflowOperations,
        Status.ROLLBACK_IN_PROGRESS.toString(),
        "Create stack failed.  Rollback requested by user.",
        Status.ROLLBACK_FAILED.toString(),
        Status.ROLLBACK_COMPLETE.toString(),
        false).getPromise(stackId, accountId, resourceDependencyManagerJson, effectiveUserId, createdStackVersion, "");
    } else {
      throw new InternalFailureException("Invalid onFailure value " + onFailure);
    }
  }

}
