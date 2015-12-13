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

import com.amazonaws.services.simpleworkflow.flow.core.AndPromise
import com.amazonaws.services.simpleworkflow.flow.core.Promise
import com.amazonaws.services.simpleworkflow.flow.core.Settable
import com.eucalyptus.cloudformation.CloudFormation
import com.eucalyptus.cloudformation.entity.StackEntityHelper
import com.eucalyptus.cloudformation.entity.Status
import com.eucalyptus.cloudformation.resources.ResourceAction
import com.eucalyptus.cloudformation.resources.ResourceResolverManager
import com.eucalyptus.cloudformation.template.dependencies.DependencyManager
import com.eucalyptus.component.annotation.ComponentPart
import com.fasterxml.jackson.databind.ObjectMapper
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
public class UpdateStackWorkflowImpl implements UpdateStackWorkflow {
  private static final Logger LOG = Logger.getLogger(UpdateStackWorkflowImpl.class);

  @Delegate
  WorkflowOperations<StackActivityClient> workflowOperations = SwfWorkflowOperations.of(StackActivityClient);

  @Override
  public void updateStack(String stackId, String accountId, String resourceDependencyManagerJson, String effectiveUserId) {
    try {
      Promise<String> updateInitialStackPromise =
        activities.createGlobalStackEvent(
          stackId,
          accountId,
          Status.UPDATE_IN_PROGRESS.toString(),
          "User Initiated"
        );

      waitFor(updateInitialStackPromise) {
        DependencyManager resourceDependencyManager = StackEntityHelper.jsonToResourceDependencyManager(
          resourceDependencyManagerJson
        );
        Map<String, Settable<String>> updatedResourcePromiseMap = Maps.newConcurrentMap();
        for (String resourceId : resourceDependencyManager.getNodes()) {
          updatedResourcePromiseMap.put(resourceId, new Settable<String>()); // placeholder promise
        }
        doTry {
          // This is in case any part of setting up the stack fails
          // AWS has added some new parameter types whose values are not validated until now, so we do the same.  (Why?)
          Promise<String> validateAWSParameterTypesPromise = activities.validateAWSParameterTypes(stackId, accountId, effectiveUserId);
          waitFor(validateAWSParameterTypesPromise) {
            // Now for each resource, set up the promises and the dependencies they have for each other
            for (String resourceId : resourceDependencyManager.getNodes()) {
              String resourceIdLocalCopy = new String(resourceId); // passing "resourceId" into a waitFor() uses the for reference pointer after the for loop has expired
              Collection<Promise<String>> promisesDependedOn = Lists.newArrayList();
              for (String dependingResourceId : resourceDependencyManager.getReverseDependentNodes(resourceIdLocalCopy)) {
                promisesDependedOn.add(updatedResourcePromiseMap.get(dependingResourceId));
              }
              AndPromise dependentAndPromise = new AndPromise(promisesDependedOn);
              waitFor(dependentAndPromise) {
                String reverseDependentResourcesJson = new ObjectMapper().writeValueAsString(
                  resourceDependencyManager.getReverseDependentNodes(resourceIdLocalCopy) == null ?
                    Lists.<String>newArrayList() :
                    resourceDependencyManager.getReverseDependentNodes(resourceIdLocalCopy)
                );
                Promise<String> currentResourcePromise = getUpdatePromise(resourceIdLocalCopy, stackId, accountId, effectiveUserId, reverseDependentResourcesJson);
                updatedResourcePromiseMap.get(resourceIdLocalCopy).chain(currentResourcePromise);
                return currentResourcePromise;
              }
            }
            AndPromise allResourcePromises = new AndPromise(updatedResourcePromiseMap.values());
            waitFor(allResourcePromises) {
              waitFor(activities.finalizeCreateStack(stackId, accountId, effectiveUserId)) {
                activities.createGlobalStackEvent(stackId, accountId,
                  Status.UPDATE_COMPLETE_CLEANUP_IN_PROGRESS.toString(),
                  "");
              }
            }
          }
        }.withCatch { Throwable t ->
          UpdateStackWorkflowImpl.LOG.error(t);
          UpdateStackWorkflowImpl.LOG.debug(t, t);
          Throwable cause = Throwables.getRootCause(t);
          Promise<String> errorMessagePromise = Promise.asPromise((cause != null) && (cause.getMessage() != null) ? cause.getMessage() : "");
          if (cause != null && cause instanceof ResourceFailureException) {
            errorMessagePromise = activities.determineCreateResourceFailures(stackId, accountId);
          }
          waitFor(errorMessagePromise) { String errorMessage ->
            activities.createGlobalStackEvent(
              stackId,
              accountId,
              Status.UPDATE_FAILED.toString(), //TODO: implement rollback.
              errorMessage
            );
          }
        }.getResult()
      }
    } catch (Exception ex) {
      UpdateStackWorkflowImpl.LOG.error(ex);
      UpdateStackWorkflowImpl.LOG.debug(ex, ex);
    }
  }

  Promise<String> getUpdatePromise(String resourceId,
                                   String stackId,
                                   String accountId,
                                   String effectiveUserId,
                                   String reverseDependentResourcesJson) {
    Promise<String> getResourceTypePromise = activities.getResourceType(stackId, accountId, resourceId);
    waitFor(getResourceTypePromise) { String resourceType ->
      ResourceAction resourceAction = new ResourceResolverManager().resolveResourceAction(resourceType);
      Promise<String> initPromise = activities.initUpdateResource(resourceId, stackId, accountId, effectiveUserId, reverseDependentResourcesJson);
      waitFor(initPromise) { String result ->
        if ("SKIP".equals(result) || "NONE".equals(result)) {
          return promiseFor("");
        } else if ("CREATE".equals(result)) {
          return new CommonCreateUpdatePromises(workflowOperations).getCreatePromise(resourceId, accountId, effectiveUserId, reverseDependentResourcesJson);
        } else {
          Promise<String> updatePromise;
          if ("NO_PROPERTIES".equals(result)) {
            updatePromise = promiseFor("");
          } else if (UpdateType.NO_INTERRUPTION.toString().equals(result)) {
            updatePromise = resourceAction.getUpdateNoInterruptionPromise(workflowOperations, resourceId, stackId, accountId, effectiveUserId);
          } else if (UpdateType.SOME_INTERRUPTION.toString().equals(result)) {
            updatePromise = resourceAction.getUpdateSomeInterruptionPromise(workflowOperations, resourceId, stackId, accountId, effectiveUserId);
          } else {
            updatePromise = resourceAction.getUpdateWithReplacementPromise(workflowOperations, resourceId, stackId, accountId, effectiveUserId);
          }
          waitFor(updatePromise) {
            activities.finalizeUpdateResource(resourceId, stackId, accountId, effectiveUserId);
          }
        }
      }
    }
  }
}
