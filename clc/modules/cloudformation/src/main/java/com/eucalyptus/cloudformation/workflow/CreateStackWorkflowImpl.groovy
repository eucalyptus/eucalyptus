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
public class CreateStackWorkflowImpl implements CreateStackWorkflow {
  private static final Logger LOG = Logger.getLogger(CreateStackWorkflowImpl.class);

  @Delegate
  WorkflowOperations<StackActivityClient> workflowOperations = SwfWorkflowOperations.of(StackActivityClient);

  @Override
  public void createStack(String stackId, String accountId, String resourceDependencyManagerJson, String effectiveUserId, String onFailure, int createdStackVersion) {
    try {
      doTry {
        return performCreateStack(stackId, accountId, resourceDependencyManagerJson, effectiveUserId, onFailure, createdStackVersion);
      } withCatch { Throwable t->
        CreateStackWorkflowImpl.LOG.error(t);
        CreateStackWorkflowImpl.LOG.debug(t, t);
      }
    } catch (Exception ex) {
      CreateStackWorkflowImpl.LOG.error(ex);
      CreateStackWorkflowImpl.LOG.debug(ex, ex);
    }
  }

  private Promise<String> performCreateStack(String stackId, String accountId, String resourceDependencyManagerJson, String effectiveUserId, String onFailure, int createdStackVersion) {
    Promise<String> createInitialStackPromise =
      activities.createGlobalStackEvent(
        stackId,
        accountId,
        Status.CREATE_IN_PROGRESS.toString(),
        "User Initiated", createdStackVersion
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
        // AWS has added some new parameter types whose values are not validated until now, so we do the same.  (Why?)
        Promise<String> validateAWSParameterTypesPromise = activities.validateAWSParameterTypes(stackId, accountId, effectiveUserId, createdStackVersion);
        waitFor(validateAWSParameterTypesPromise) {
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
              Promise<String> currentResourcePromise = getCreatePromise(resourceIdLocalCopy, stackId, accountId, effectiveUserId, reverseDependentResourcesJson, createdStackVersion);
              createdResourcePromiseMap.get(resourceIdLocalCopy).chain(currentResourcePromise);
              return currentResourcePromise;
            }
          }
          AndPromise allResourcePromises = new AndPromise(createdResourcePromiseMap.values());
          waitFor(allResourcePromises) {
            waitFor(activities.finalizeCreateStack(stackId, accountId, effectiveUserId, createdStackVersion)) {
              activities.createGlobalStackEvent(stackId, accountId,
                Status.CREATE_COMPLETE.toString(),
                "", createdStackVersion);
            }
          }
        }
      }.withCatch { Throwable t ->
        CreateStackWorkflowImpl.LOG.error(t);
        CreateStackWorkflowImpl.LOG.debug(t, t);
        Throwable cause = Throwables.getRootCause(t);
        Promise<String> errorMessagePromise = Promise.asPromise((cause != null) && (cause.getMessage() != null) ? cause.getMessage() : "");
        if (cause != null && cause instanceof ResourceFailureException) {
          errorMessagePromise = activities.determineCreateResourceFailures(stackId, accountId, createdStackVersion);
        }
        waitFor(errorMessagePromise) { String errorMessage ->
          activities.createGlobalStackEvent(
            stackId,
            accountId,
            Status.CREATE_FAILED.toString(),
            errorMessage,
            createdStackVersion
          );
        }
      }.getResult()
    }
  }

  Promise<String> getCreatePromise(String resourceId,
                                   String stackId,
                                   String accountId,
                                   String effectiveUserId,
                                   String reverseDependentResourcesJson,
                                   int createdResourceVersion) {
    return new CommonCreateUpdatePromises(workflowOperations).getCreatePromise(resourceId, stackId, accountId, effectiveUserId, reverseDependentResourcesJson, createdResourceVersion);
  }
}
