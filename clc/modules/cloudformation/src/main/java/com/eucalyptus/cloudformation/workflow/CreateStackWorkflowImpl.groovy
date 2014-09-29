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
import com.eucalyptus.cloudformation.entity.StackEntityHelper
import com.eucalyptus.cloudformation.entity.StackResourceEntity
import com.eucalyptus.cloudformation.resources.ResourceAction
import com.eucalyptus.cloudformation.resources.ResourceResolverManager
import com.eucalyptus.cloudformation.template.JsonHelper
import com.eucalyptus.cloudformation.template.dependencies.DependencyManager
import com.eucalyptus.cloudformation.workflow.create.ResourceFailureException
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.google.common.base.Throwables
import com.google.common.collect.Lists
import com.google.common.collect.Maps
import com.google.common.collect.Sets
import com.netflix.glisten.WorkflowOperations
import com.netflix.glisten.impl.swf.SwfWorkflowOperations
import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import org.apache.log4j.Logger

@CompileStatic(TypeCheckingMode.SKIP)
public class CreateStackWorkflowImpl implements CreateStackWorkflow {
  private static final Logger LOG = Logger.getLogger(CreateStackWorkflowImpl.class);

  @Delegate
  WorkflowOperations<StackActivity> workflowOperations = SwfWorkflowOperations.of(StackActivity);

  @Override
  public void createStack(String stackId, String accountId, String resourceDependencyManagerJson, String effectiveUserId, String onFailure) {
    try {
      Promise<String> createInitialStackPromise = promiseFor(
        activities.createGlobalStackEvent(
          stackId,
          accountId,
          StackResourceEntity.Status.CREATE_IN_PROGRESS.toString(),
          "User Initiated"
        )
      );
      waitFor(createInitialStackPromise) {
        DependencyManager resourceDependencyManager = StackEntityHelper.jsonToResourceDependencyManager(
          resourceDependencyManagerJson
        );
        Map<String, Settable<String>> createdResourcePromiseMap = Maps.newConcurrentMap();
        for (String resourceId : resourceDependencyManager.getNodes()) {
          createdResourcePromiseMap.put(resourceId, new Settable<String>()); // placeholder promise
        }
        doTry {
          // This is in case any part of setting up the stack fails
          // Now for each resource, set up the promises and the dependencies they have for each other
          for (String resourceId : resourceDependencyManager.getNodes()) {
            String resourceIdLocalCopy = new String(resourceId); // passing "resourceId" into a waitFor() uses the for reference pointer after the for loop has expired
            Collection<Promise<String>> promisesDependedOn = Lists.newArrayList();
            for (String dependingResourceId : resourceDependencyManager.getReverseDependentNodes(resourceIdLocalCopy)) {
              promisesDependedOn.add(createdResourcePromiseMap.get(dependingResourceId));
            }
            AndPromise dependentAndPromise = new AndPromise(promisesDependedOn);
            waitFor(dependentAndPromise) {
              String reverseDependentResourcesJson = new ObjectMapper().writeValueAsString(
                resourceDependencyManager.getReverseDependentNodes(resourceIdLocalCopy) == null ?
                  Lists.<String>newArrayList() :
                  resourceDependencyManager.getReverseDependentNodes(resourceIdLocalCopy)
              );
              Promise<String> currentResourcePromise = getCreatePromise(resourceIdLocalCopy, stackId, accountId, effectiveUserId, reverseDependentResourcesJson);
              createdResourcePromiseMap.get(resourceIdLocalCopy).chain(currentResourcePromise);
              return currentResourcePromise;
            }
          }
          AndPromise allResourcePromises = new AndPromise(createdResourcePromiseMap.values());
          waitFor(allResourcePromises) {
            waitFor(promiseFor(activities.finalizeCreateStack(stackId, accountId))) {
              promiseFor(activities.createGlobalStackEvent(stackId, accountId,
                StackResourceEntity.Status.CREATE_COMPLETE.toString(),
                "Complete!"));
            }
          }
        }.withCatch { Throwable t ->
          CreateStackWorkflowImpl.LOG.error(t);
          CreateStackWorkflowImpl.LOG.debug(t, t);
          Throwable cause = Throwables.getRootCause(t);
          Promise<String> errorMessagePromise = Promise.asPromise((cause != null) && (cause.getMessage() != null) ? cause.getMessage() : "");
          if (cause != null && cause instanceof ResourceFailureException) {
            errorMessagePromise = promiseFor(activities.determineResourceFailures(stackId, accountId));
          }
          waitFor(errorMessagePromise) { String errorMessage ->
            promiseFor(activities.createGlobalStackEvent(
              stackId,
              accountId,
              StackResourceEntity.Status.CREATE_FAILED.toString(),
              errorMessage)
            );
          }
        }.getResult()
      }
    } catch (Exception ex) {
      CreateStackWorkflowImpl.LOG.error(ex);
      CreateStackWorkflowImpl.LOG.debug(ex, ex);
    }
  }

  Promise<String> getCreatePromise(String resourceId,
                                   String stackId,
                                   String accountId,
                                   String effectiveUserId,
                                   String reverseDependentResourcesJson) {
    Promise<String> getResourceTypePromise = promiseFor(activities.getResourceType(stackId, accountId, resourceId));
    waitFor(getResourceTypePromise) { String resourceType ->
      ResourceAction resourceAction = new ResourceResolverManager().resolveResourceAction(resourceType);
      Promise<String> initPromise = promiseFor(activities.initResource(resourceId, stackId, accountId, effectiveUserId, reverseDependentResourcesJson));
      waitFor(initPromise) { String result ->
        if ("SKIP".equals(result)) {
          return promiseFor("");
        } else {
          Promise<String> createPromise = resourceAction.getCreatePromise(this,
            resourceId, stackId, accountId, effectiveUserId, reverseDependentResourcesJson);
          waitFor(createPromise) {
            activities.finalizeCreateResource(resourceId, stackId, accountId, effectiveUserId);
          }
        }
      }
    }
  }
}
