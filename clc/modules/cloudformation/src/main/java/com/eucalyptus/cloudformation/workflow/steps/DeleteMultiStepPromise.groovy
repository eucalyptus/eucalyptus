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
package com.eucalyptus.cloudformation.workflow.steps

import com.amazonaws.services.simpleworkflow.flow.core.Promise
import com.amazonaws.services.simpleworkflow.flow.interceptors.ExponentialRetryPolicy
import com.eucalyptus.cloudformation.workflow.StackActivityClient
import com.netflix.glisten.WorkflowOperations
import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode

/**
 * Created by ethomas on 9/28/14.
 */
@CompileStatic(TypeCheckingMode.SKIP)
class DeleteMultiStepPromise extends MultiStepPromise {

  DeleteMultiStepPromise(
      final WorkflowOperations<StackActivityClient> workflowOperations,
      final Collection<String> stepIds,
      final StepBasedResourceAction resourceAction
  ) {
    super( workflowOperations, stepIds, resourceAction )
  }

  @Override
  protected Step getStep( final String stepId ) {
    return resourceAction.getDeleteStep( stepId )
  }

  Promise<String> getDeletePromise( String resourceId, String stackId, String accountId, String effectiveUserId, int deletedResourceVersion ) {
    getPromise( "Resource ${resourceId} deletion failed for stack ${stackId}" as String) { String stepId ->
      activities.performDeleteStep(stepId, resourceId, stackId, accountId, effectiveUserId, deletedResourceVersion)
    }
  }

  @Override
  def <T> Promise<T> invoke(final Closure<Promise<T>> activity) {
    retry( new ExponentialRetryPolicy( 1L ).withMaximumAttempts( 6 ) ) {
      activity.call( )
    }
  }
}
