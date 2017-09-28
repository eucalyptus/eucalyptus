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

package com.eucalyptus.cloudformation.workflow

import com.amazonaws.services.simpleworkflow.flow.core.AndPromise
import com.amazonaws.services.simpleworkflow.flow.core.Promise
import com.amazonaws.services.simpleworkflow.flow.core.Settable
import com.eucalyptus.cloudformation.common.CloudFormation
import com.eucalyptus.cloudformation.entity.StackEntityHelper
import com.eucalyptus.cloudformation.entity.Status
import com.eucalyptus.cloudformation.resources.ResourceAction
import com.eucalyptus.cloudformation.resources.ResourceResolverManager
import com.eucalyptus.cloudformation.resources.standard.actions.AWSCloudFormationStackResourceAction
import com.eucalyptus.cloudformation.template.dependencies.DependencyManager
import com.eucalyptus.component.annotation.ComponentPart
import com.google.common.base.Throwables
import com.google.common.collect.Lists
import com.google.common.collect.Maps
import com.netflix.glisten.WorkflowOperations
import com.netflix.glisten.impl.swf.SwfWorkflowOperations
import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import org.apache.log4j.Logger

@ComponentPart(CloudFormation)
@CompileStatic(TypeCheckingMode.SKIP)
public class UpdateCleanupStackWorkflowImpl implements UpdateCleanupStackWorkflow {
  private static final Logger LOG = Logger.getLogger(UpdateCleanupStackWorkflowImpl.class);

  @Delegate
  WorkflowOperations<StackActivityClient> workflowOperations = SwfWorkflowOperations.of(StackActivityClient)
  WorkflowUtils workflowUtils = new WorkflowUtils( workflowOperations )

  @Override
  void performUpdateCleanupStack(String stackId, String accountId, String oldResourceDependencyManagerJson, String effectiveUserId, int updatedStackVersion) {
    try {
      doTry {
        return performCleanup(stackId, accountId, oldResourceDependencyManagerJson, effectiveUserId, updatedStackVersion);
      } withCatch { Throwable t->
        UpdateCleanupStackWorkflowImpl.LOG.error(t);
        UpdateCleanupStackWorkflowImpl.LOG.debug(t, t);
      }
    } catch (Exception ex) {
      UpdateCleanupStackWorkflowImpl.LOG.error(ex);
      UpdateCleanupStackWorkflowImpl.LOG.debug(ex, ex);
    }
  }

  private Promise<String> performCleanup(String stackId, String accountId, String oldResourceDependencyManagerJson, String effectiveUserId, int updatedStackVersion) {
    DependencyManager oldResourceDependencyManager = StackEntityHelper.jsonToResourceDependencyManager(
      oldResourceDependencyManagerJson
    );
    Map<String, Settable<String>> deletedResourcePromiseMap = Maps.newConcurrentMap();
    for (String resourceId : oldResourceDependencyManager.getNodes()) {
      deletedResourcePromiseMap.put(resourceId, new Settable<String>()); // placeholder promise
    }
    doTry {
      // This is in case any part of deleting the stack fails
      // Now for each resource, set up the promises and the dependencies they have for each other (remember the order is reversed)
      for (String resourceId : oldResourceDependencyManager.getNodes()) {
        String resourceIdLocalCopy = new String(resourceId);
        // passing "resourceId" into a waitFor() uses the for reference pointer after the for loop has expired
        Collection<Promise<String>> promisesDependedOn = Lists.newArrayList();
        // We have the opposite direction in delete than create,
        for (String dependingResourceId : oldResourceDependencyManager.getDependentNodes(resourceIdLocalCopy)) {
          promisesDependedOn.add(deletedResourcePromiseMap.get(dependingResourceId));
        }
        AndPromise dependentAndPromise = new AndPromise(promisesDependedOn);
        waitFor(dependentAndPromise) {
          Promise<String> currentResourcePromise = getUpdateCleanupPromise(resourceIdLocalCopy, stackId, accountId, effectiveUserId, updatedStackVersion);
          deletedResourcePromiseMap.get(resourceIdLocalCopy).chain(currentResourcePromise);
          return currentResourcePromise;
        }
      }
      AndPromise allResourcePromises = new AndPromise(deletedResourcePromiseMap.values());
      waitFor(allResourcePromises) {
        // check if any failures...
        boolean resourceFailure = false;
        for (Promise promise : allResourcePromises.getValues()) {
          if (promise.isReady() && "FAILURE".equals(promise.get())) {
            resourceFailure = true;
            break;
          }
        }
        String errorMessage = resourceFailure ? "One or more resources could not be deleted." : "";
        return waitFor(activities.createGlobalStackEvent(stackId, accountId, Status.UPDATE_COMPLETE.toString(), errorMessage, updatedStackVersion)) {
          activities.finalizeUpdateCleanupStack(stackId, accountId, errorMessage, updatedStackVersion);
        }
      }
    }.withCatch { Throwable t ->
      CreateStackWorkflowImpl.LOG.error(t);
      CreateStackWorkflowImpl.LOG.debug(t, t);
      final String errorMessage = "One or more resources could not be deleted.";
      return waitFor(activities.createGlobalStackEvent(stackId, accountId, Status.UPDATE_COMPLETE.toString(), errorMessage, updatedStackVersion)) {
        activities.finalizeUpdateCleanupStack(stackId, accountId, errorMessage, updatedStackVersion);
      }
    }.getResult()
  }



  Promise<String> getUpdateCleanupPromise(String resourceId,
                                          String stackId,
                                          String accountId,
                                          String effectiveUserId,
                                          int updatedResourceVersion) {
    // all of these items are from the previous stack version
    Promise<String> getResourceTypePromise = activities.getResourceType(stackId, accountId, resourceId, updatedResourceVersion - 1);
    waitFor(getResourceTypePromise) { String resourceType ->
      ResourceAction resourceAction = new ResourceResolverManager().resolveResourceAction(resourceType);
      Promise<Boolean> checkInnerStackUpdateSpecialCasePromise = activities.checkInnerStackUpdate(resourceId, stackId, accountId, effectiveUserId, updatedResourceVersion)
      waitFor(checkInnerStackUpdateSpecialCasePromise) { Boolean innerStackUpdateSpecialCase ->
        if (Boolean.TRUE.equals(innerStackUpdateSpecialCase)) {
          Promise<String> initPromise = activities.initUpdateCleanupInnerStackUpdateResource(resourceId, stackId, accountId, effectiveUserId, updatedResourceVersion);
          waitFor(initPromise) {
            waitFor(((AWSCloudFormationStackResourceAction) resourceAction).getUpdateCleanupUpdatePromise(workflowOperations, resourceId, stackId, accountId, effectiveUserId, updatedResourceVersion)) {
              return activities.finalizeUpdateCleanupInnerStackUpdateResource(resourceId, stackId, accountId, effectiveUserId, updatedResourceVersion);
            }
          }
        } else {
          Promise<String> initPromise = activities.initUpdateCleanupResource(resourceId, stackId, accountId, effectiveUserId, updatedResourceVersion);
          waitFor(initPromise) { String result ->
            if ("SKIP".equals(result)) {
              return promiseFor("SUCCESS");
            } else {
              doTry {
                waitFor(resourceAction.getUpdateCleanupPromise(workflowOperations, resourceId, stackId, accountId, effectiveUserId, updatedResourceVersion)) {
                  return activities.finalizeUpdateCleanupResource(resourceId, stackId, accountId, effectiveUserId, updatedResourceVersion);
                }
              }.withCatch { Throwable t ->
                Throwable rootCause = Throwables.getRootCause(t);
                return activities.failUpdateCleanupResource(resourceId, stackId, accountId, effectiveUserId, rootCause.getMessage(), updatedResourceVersion);
              }.getResult();
            }
          }
        }

      }
    }
  }

}
