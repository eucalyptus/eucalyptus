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
package com.eucalyptus.cloudformation.workflow

import com.amazonaws.services.simpleworkflow.flow.core.Promise
import com.eucalyptus.cloudformation.resources.ResourceAction
import com.eucalyptus.cloudformation.resources.ResourceResolverManager
import com.netflix.glisten.WorkflowOperations
import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import org.apache.log4j.Logger

/**
 * Created by ethomas on 12/11/15.
 */
@CompileStatic(TypeCheckingMode.SKIP)
public class CommonCreateUpdatePromises {
  private static final Logger LOG = Logger.getLogger(CommonCreateUpdatePromises.class);
  @Delegate
  WorkflowOperations<StackActivityClient> workflowOperations;


  public CommonCreateUpdatePromises(WorkflowOperations<StackActivityClient> workflowOperations) {
    this.workflowOperations = workflowOperations;
  }

  Promise<String> getCreatePromise(String resourceId,
                                   String stackId,
                                   String accountId,
                                   String effectiveUserId,
                                   String reverseDependentResourcesJson) {
    Promise<String> getResourceTypePromise = activities.getResourceType(stackId, accountId, resourceId);
    waitFor(getResourceTypePromise) { String resourceType ->
      ResourceAction resourceAction = new ResourceResolverManager().resolveResourceAction(resourceType);
      Promise<String> initPromise = activities.initCreateResource(resourceId, stackId, accountId, effectiveUserId, reverseDependentResourcesJson);
      waitFor(initPromise) { String result ->
        if ("SKIP".equals(result)) {
          return promiseFor("");
        } else {
          Promise<String> createPromise = resourceAction.getCreatePromise(workflowOperations, resourceId, stackId, accountId, effectiveUserId);
          waitFor(createPromise) {
            activities.finalizeCreateResource(resourceId, stackId, accountId, effectiveUserId);
          }
        }
      }
    }
  }
}
