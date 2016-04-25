/*************************************************************************
 * Copyright 2014 Eucalyptus Systems, Inc.
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
import com.eucalyptus.cloudformation.workflow.RetryAfterConditionCheckFailedException
import com.eucalyptus.cloudformation.workflow.StackActivityClient
import com.eucalyptus.simpleworkflow.common.workflow.WorkflowUtils
import com.netflix.glisten.WorkflowOperations
import groovy.transform.CompileStatic
import groovy.transform.PackageScope
import groovy.transform.TypeCheckingMode

import java.util.concurrent.TimeUnit

/**
 * Created by ethomas
 */
@CompileStatic(TypeCheckingMode.SKIP)
@PackageScope
abstract class MultiStepPromise {

  @Delegate
  private final WorkflowOperations<StackActivityClient> workflowOperations;
  private final WorkflowUtils workflowUtils;
  private final List<String> stepIds;
  protected final StepBasedResourceAction resourceAction;

  MultiStepPromise( WorkflowOperations<StackActivityClient> workflowOperations,
                    Collection<String> stepIds,
                    StepBasedResourceAction resourceAction
  ) {
    this.workflowOperations = workflowOperations;
    this.workflowUtils = new WorkflowUtils( workflowOperations );
    this.stepIds = stepIds;
    this.resourceAction = resourceAction;
  }

  protected abstract Step getStep( String stepId );

  protected Promise<String> getPromise( final String failureError, final Closure<Promise<Boolean>> stepClosure ) {
    Promise<String> previousPromise = promiseFor(""); // this value is a placeholder
    for ( String stepId : stepIds ) {
      final String stepIdLocal = new String(stepId); // If you access "stepId" from the wait for, it is executed after the for loop is finished.  You need the value during this iteration
      final Step step = getStep( stepIdLocal );
      previousPromise = waitFor( previousPromise ) {
        Promise<Boolean> stepPromise = invokeOrPoll( step.timeout ) {
          stepClosure.call( stepIdLocal )
        }
        waitFor( stepPromise ) { Boolean result ->
          if ( !result ) {
            throw new RetryAfterConditionCheckFailedException( failureError )
          }
          promiseFor( "" )
        }
      }
    }
    return previousPromise;
  }

  def <T> Promise<T> invokeOrPoll( Integer timeout, Closure<Promise<T>> activity ) {
    if ( timeout ) {
      workflowUtils.exponentialPollWithTimeout( timeout, 5, 1.15, resourceAction.getMultiStepTimeoutPollMaximumInterval(), activity )
    } else {
      invoke( activity )
    }
  }

  def <T> Promise<T> invoke( Closure<Promise<T>> activity ) {
    activity.call( )
  }
}
