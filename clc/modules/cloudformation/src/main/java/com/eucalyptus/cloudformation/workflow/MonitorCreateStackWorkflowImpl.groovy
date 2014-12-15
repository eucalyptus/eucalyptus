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
import com.eucalyptus.cloudformation.entity.StackEntity
import com.eucalyptus.cloudformation.entity.StackResourceEntity
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
  WorkflowOperations<StackActivity> workflowOperations = SwfWorkflowOperations.of(StackActivity)
  WorkflowUtils workflowUtils = new WorkflowUtils( workflowOperations )

  @Override
  void monitorCreateStack(String stackId, String accountId, String resourceDependencyManagerJson, String effectiveUserId, String onFailure) {
    try {
      Promise<String> closeStatusPromise = workflowUtils.exponentialPollWithTimeout( (int)TimeUnit.DAYS.toSeconds( 365 ) ) {
        retry( new ExponentialRetryPolicy( 2L ).withMaximumAttempts( 6 ) ){
          promiseFor( activities.getWorkflowExecutionStatus( stackId ) )
        }
      }
      waitFor( closeStatusPromise ) { String closedStatus ->
        if ( !closedStatus ) {
          throw new InternalFailureException( "Stack create timeout stack id ${stackId}" );
        }
        waitFor( promiseFor( activities.getStackStatus( stackId, accountId ) ) ) { String stackStatus ->
          determineRollbackAction( closedStatus, stackStatus, stackId, accountId, resourceDependencyManagerJson, effectiveUserId, onFailure );
        }
      }
    } catch (Exception ex) {
      MonitorCreateStackWorkflowImpl.LOG.error(ex);
      MonitorCreateStackWorkflowImpl.LOG.debug(ex, ex);
    }
  }

  private Promise<String> determineRollbackAction(String closedStatus, String stackStatus, String stackId, String accountId,
   String resourceDependencyManagerJson, String effectiveUserId, String onFailure) {
  if ("CREATE_COMPLETE".equals(stackStatus)) {
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
      Promise<String> setStackStatusPromise = promiseFor(activities.setStackStatus(stackId, accountId,
        StackEntity.Status.CREATE_FAILED.toString(), statusReason));
      return waitFor(setStackStatusPromise) {
        Promise<String> createGlobalStackEventPromise = promiseFor(activities.createGlobalStackEvent(stackId,
          accountId, StackResourceEntity.Status.CREATE_FAILED.toString(), statusReason));
        waitFor(createGlobalStackEventPromise) {
          performRollback(stackId, accountId, resourceDependencyManagerJson, effectiveUserId, onFailure);
        }
      }
    } else if ("CREATE_FAILED".equals(stackStatus)) {
      return performRollback(stackId, accountId, resourceDependencyManagerJson, effectiveUserId, onFailure);
    } else {
      throw new InternalFailureException("Unexpected stack status " + stackStatus + " during create monitoring");
    }
  }

  private Promise<String> performRollback(String stackId, String accountId, String resourceDependencyManagerJson, String effectiveUserId, String onFailure) {
    if ("DO_NOTHING".equals(onFailure)) {
      return promiseFor("");
    } else if ("DELETE".equals(onFailure)) {
      return new CommonDeleteRollbackPromises(workflowOperations,
        StackResourceEntity.Status.DELETE_IN_PROGRESS.toString(),
        "Create stack failed.  Delete requested by user.",
        StackResourceEntity.Status.DELETE_FAILED.toString(),
        StackResourceEntity.Status.DELETE_COMPLETE.toString(),
        true).getPromise(stackId, accountId, resourceDependencyManagerJson, effectiveUserId);

    } else if ("ROLLBACK".equals(onFailure)) {
      return new CommonDeleteRollbackPromises(workflowOperations,
        StackResourceEntity.Status.ROLLBACK_IN_PROGRESS.toString(),
        "Create stack failed.  Rollback requested by user.",
        StackResourceEntity.Status.ROLLBACK_FAILED.toString(),
        StackResourceEntity.Status.ROLLBACK_COMPLETE.toString(),
        false).getPromise(stackId, accountId, resourceDependencyManagerJson, effectiveUserId);
    } else {
      throw new InternalFailureException("Invalid onFailure value " + onFailure);
    }
  }

}
