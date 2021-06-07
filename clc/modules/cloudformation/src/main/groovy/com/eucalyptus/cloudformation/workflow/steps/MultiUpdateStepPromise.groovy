/*************************************************************************
 * Copyright 2014 Ent. Services Development Corporation LP
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
package com.eucalyptus.cloudformation.workflow.steps

import com.amazonaws.services.simpleworkflow.flow.core.Promise
import com.eucalyptus.cloudformation.workflow.RetryAfterConditionCheckFailedException
import com.eucalyptus.cloudformation.workflow.StackActivityClient
import com.eucalyptus.cloudformation.workflow.WorkflowUtils
import com.netflix.glisten.WorkflowOperations
import groovy.transform.CompileStatic
import groovy.transform.PackageScope
import groovy.transform.TypeCheckingMode

/**
 * Created by ethomas
 */
@CompileStatic(TypeCheckingMode.SKIP)
@PackageScope
abstract class MultiUpdateStepPromise {
  @Delegate
  private final WorkflowOperations<StackActivityClient> workflowOperations;
  private final WorkflowUtils workflowUtils;
  private final List<String> stepIds;
  protected final StepBasedResourceAction resourceAction;

  MultiUpdateStepPromise( WorkflowOperations<StackActivityClient> workflowOperations,
                    Collection<String> stepIds,
                    StepBasedResourceAction resourceAction
  ) {
    this.workflowOperations = workflowOperations;
    this.workflowUtils = new WorkflowUtils( workflowOperations );
    this.stepIds = stepIds;
    this.resourceAction = resourceAction;
  }

  protected abstract UpdateStep getStep( String stepId );

  protected Promise<String> getPromise( final String failureError, final Closure<Promise<Boolean>> stepClosure ) {
    Promise<String> previousPromise = promiseFor(""); // this value is a placeholder
    for ( String stepId : stepIds ) {
      final String stepIdLocal = new String(stepId); // If you access "stepId" from the wait for, it is executed after the for loop is finished.  You need the value during this iteration
      final UpdateStep step = getStep( stepIdLocal );
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
