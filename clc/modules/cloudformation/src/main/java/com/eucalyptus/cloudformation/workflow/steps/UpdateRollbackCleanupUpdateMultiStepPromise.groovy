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
package com.eucalyptus.cloudformation.workflow.steps

import com.amazonaws.services.simpleworkflow.flow.core.Promise
import com.amazonaws.services.simpleworkflow.flow.interceptors.ExponentialRetryPolicy
import com.eucalyptus.cloudformation.resources.standard.actions.AWSCloudFormationStackResourceAction
import com.eucalyptus.cloudformation.workflow.StackActivityClient
import com.netflix.glisten.WorkflowOperations
import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode

/**
 * Created by ethomas on 9/28/14.
 */
@CompileStatic(TypeCheckingMode.SKIP)
class UpdateRollbackCleanupUpdateMultiStepPromise extends MultiUpdateStepPromise {

  UpdateRollbackCleanupUpdateMultiStepPromise(
      final WorkflowOperations<StackActivityClient> workflowOperations,
      final Collection<String> stepIds,
      final AWSCloudFormationStackResourceAction resourceAction
  ) {
    super( workflowOperations, stepIds, resourceAction )
  }

  @Override
  protected UpdateStep getStep( final String stepId ) {
    return ((AWSCloudFormationStackResourceAction) resourceAction).getUpdateRollbackCleanupUpdateStep( stepId )
  }

  Promise<String> getUpdateRollbackCleanupUpdatePromise(String resourceId, String stackId, String accountId, String effectiveUserId, int resourceVersion ) {
    getPromise( "Resource ${resourceId} cleanup failed for stack ${stackId}" as String) { String stepId ->
      // The item to perform update cleanup on is one step less than the resource version
      activities.performUpdateRollbackCleanupInnerStackUpdateStep(stepId, resourceId, stackId, accountId, effectiveUserId, resourceVersion - 1)
    }
  }

  @Override
  def <T> Promise<T> invoke(final Closure<Promise<T>> activity) {
    retry( new ExponentialRetryPolicy( 1L ).withMaximumAttempts( 6 ) ) {
      activity.call( )
    }
  }
}
