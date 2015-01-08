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
import com.eucalyptus.cloudformation.resources.ResourceAction
import com.eucalyptus.cloudformation.workflow.StackActivity
import com.netflix.glisten.WorkflowOperations
import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode

/**
 * Created by ethomas on 9/28/14.
 */
@CompileStatic(TypeCheckingMode.SKIP)
class CreateMultiStepPromise extends MultiStepPromise {

  CreateMultiStepPromise(
      final WorkflowOperations<StackActivity> workflowOperations,
      final Collection<String> stepIds,
      final ResourceAction resourceAction
  ) {
    super( workflowOperations, stepIds, resourceAction )
  }

  @Override
  protected Step getStep( final String stepId ) {
    return resourceAction.getCreateStep( stepId )
  }

  Promise<String> getCreatePromise( String resourceId, String stackId, String accountId, String effectiveUserId ) {
    getPromise( "Resource ${resourceId} creation timeout for stack ${stackId}" as String) { String stepId ->
      activities.performCreateStep( stepId, resourceId, stackId, accountId, effectiveUserId )
    }
  }
}
