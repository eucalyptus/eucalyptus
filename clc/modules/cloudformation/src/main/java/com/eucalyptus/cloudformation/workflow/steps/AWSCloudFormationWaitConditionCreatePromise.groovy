/*************************************************************************
 * Copyright 2009-2014 Eucalyptus Systems, Inc.
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
package com.eucalyptus.cloudformation.workflow.steps

import com.amazonaws.services.simpleworkflow.flow.core.Promise
import com.eucalyptus.cloudformation.workflow.StackActivityClient
import com.eucalyptus.cloudformation.workflow.RetryAfterConditionCheckFailedException
import com.eucalyptus.simpleworkflow.common.workflow.WorkflowUtils
import com.netflix.glisten.WorkflowOperations
import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode

import java.util.concurrent.TimeUnit

/**
 * Created by ethomas on 9/28/14.
 */
@CompileStatic(TypeCheckingMode.SKIP)
public class AWSCloudFormationWaitConditionCreatePromise {

  public AWSCloudFormationWaitConditionCreatePromise(WorkflowOperations<StackActivityClient> workflowOperations, String stepId) {
    this.workflowOperations = workflowOperations
    this.workflowUtils = new WorkflowUtils( workflowOperations )
    this.stepId = stepId
  }
  @Delegate
  private final WorkflowOperations<StackActivityClient> workflowOperations
  private final WorkflowUtils workflowUtils
  private final String stepId

  public Promise<String> getCreatePromise(String resourceId, String stackId, String accountId, String effectiveUserId) {
    Promise<Integer> timeoutPromise =
        activities.getAWSCloudFormationWaitConditionTimeout(resourceId, stackId, accountId, effectiveUserId)
    return waitFor( timeoutPromise ) { Integer timeout ->
      waitFor( workflowUtils.exponentialPollWithTimeout( timeout, 10, 1.15, (int)TimeUnit.MINUTES.toSeconds( 2 ) ) {
        activities.performCreateStep( stepId, resourceId, stackId, accountId, effectiveUserId )
      } ) { Boolean created ->
        if ( !created ) {
          throw new RetryAfterConditionCheckFailedException( "Resource ${resourceId} timeout for stack ${stackId}" )
        }
        promiseFor( "" )
      }
    }
  }
}
