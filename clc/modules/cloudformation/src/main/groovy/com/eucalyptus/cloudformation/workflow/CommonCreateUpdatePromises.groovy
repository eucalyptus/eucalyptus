/*************************************************************************
 * Copyright 2009-2014 Ent. Services Development Corporation LP
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
package com.eucalyptus.cloudformation.workflow

import com.amazonaws.services.simpleworkflow.flow.core.Promise
import com.eucalyptus.cloudformation.resources.ResourceAction
import com.eucalyptus.cloudformation.resources.ResourceResolverManager
import com.eucalyptus.cloudformation.workflow.steps.CreateMultiStepPromise
import com.eucalyptus.cloudformation.workflow.steps.StepBasedResourceAction
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
                                   String reverseDependentResourcesJson,
                                   int createdResourceVersion) {
    Promise<String> getResourceTypePromise = activities.getResourceType(stackId, accountId, resourceId, createdResourceVersion);
    waitFor(getResourceTypePromise) { String resourceType ->
      ResourceAction resourceAction = new ResourceResolverManager().resolveResourceAction(resourceType);
      Promise<String> initPromise = activities.initCreateResource(resourceId, stackId, accountId, effectiveUserId, reverseDependentResourcesJson, createdResourceVersion);
      waitFor(initPromise) { String result ->
        if ("SKIP".equals(result)) {
          return promiseFor("");
        } else {
          StepBasedResourceAction action = (StepBasedResourceAction) resourceAction;
          Promise<String> createPromise = new CreateMultiStepPromise(
              workflowOperations, action.getCreateStepIds( ), action
          ).getCreatePromise(resourceId, stackId, accountId, effectiveUserId, createdResourceVersion);
          waitFor(createPromise) {
            activities.finalizeCreateResource(resourceId, stackId, accountId, effectiveUserId, createdResourceVersion);
          }
        }
      }
    }
  }
}
