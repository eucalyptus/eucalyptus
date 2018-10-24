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

import com.amazonaws.services.simpleworkflow.flow.core.AndPromise
import com.amazonaws.services.simpleworkflow.flow.core.Promise
import com.amazonaws.services.simpleworkflow.flow.core.Settable
import com.eucalyptus.cloudformation.entity.StackEntityHelper
import com.eucalyptus.cloudformation.resources.ResourceAction
import com.eucalyptus.cloudformation.resources.ResourceResolverManager
import com.eucalyptus.cloudformation.template.dependencies.DependencyManager
import com.eucalyptus.cloudformation.workflow.steps.DeleteMultiStepPromise
import com.eucalyptus.cloudformation.workflow.steps.StepBasedResourceAction
import com.google.common.base.Throwables
import com.google.common.collect.Lists
import com.google.common.collect.Maps
import com.netflix.glisten.WorkflowOperations
import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import org.apache.log4j.Logger

/**
 * Created by ethomas on 10/6/14.
 */
@CompileStatic(TypeCheckingMode.SKIP)
public class CommonDeleteRollbackPromises {
  private static final Logger LOG = Logger.getLogger(CommonDeleteRollbackPromises.class);
  @Delegate
  WorkflowOperations<StackActivityClient> workflowOperations;
  String stackOperationInProgressStatus;
  String stackOperationInProgressStatusReason;
  String stackOperationFailedStatus;
  String stackOperationCompleteStatus;
  boolean deleteStackRecordsWhenSuccessful;

  CommonDeleteRollbackPromises(WorkflowOperations<StackActivityClient> workflowOperations, String stackOperationInProgressStatus,
                               String stackOperationInProgressStatusReason, String stackOperationFailedStatus, String stackOperationCompleteStatus,
                               boolean deleteStackRecordsWhenSuccessful) {
    this.workflowOperations = workflowOperations
    this.stackOperationInProgressStatus = stackOperationInProgressStatus
    this.stackOperationInProgressStatusReason = stackOperationInProgressStatusReason
    this.stackOperationFailedStatus = stackOperationFailedStatus
    this.stackOperationCompleteStatus = stackOperationCompleteStatus
    this.deleteStackRecordsWhenSuccessful = deleteStackRecordsWhenSuccessful
  }

  Promise<String> getPromise(String stackId, String accountId, String resourceDependencyManagerJson, String effectiveUserId, int stackVersion, String retainedResourcesStr) {
    Promise<String> deleteInitialStackPromise = activities.createGlobalStackEvent(
      stackId,
      accountId,
      stackOperationInProgressStatus,
      stackOperationInProgressStatusReason,
      stackVersion
    ) ;
    waitFor(deleteInitialStackPromise) {
      DependencyManager resourceDependencyManager = StackEntityHelper.jsonToResourceDependencyManager(
        resourceDependencyManagerJson
      );
      Map<String, Settable<String>> deletedResourcePromiseMap = Maps.newConcurrentMap();
      for (String resourceId : resourceDependencyManager.getNodes()) {
        deletedResourcePromiseMap.put(resourceId, new Settable<String>()); // placeholder promise
      }
      doTry {
        // This is in case any part of deleting the stack fails
        // Now for each resource, set up the promises and the dependencies they have for each other (remember the order is reversed)
        for (String resourceId : resourceDependencyManager.getNodes()) {
          String resourceIdLocalCopy = new String(resourceId);
          // passing "resourceId" into a waitFor() uses the for reference pointer after the for loop has expired
          Collection<Promise<String>> promisesDependedOn = Lists.newArrayList();
          // We have the opposite direction in delete than create,
          for (String dependingResourceId : resourceDependencyManager.getDependentNodes(resourceIdLocalCopy)) {
            promisesDependedOn.add(deletedResourcePromiseMap.get(dependingResourceId));
          }
          AndPromise dependentAndPromise = new AndPromise(promisesDependedOn);
          waitFor(dependentAndPromise) {
            Promise<String> currentResourcePromise = getDeletePromise(resourceIdLocalCopy, stackId, accountId, effectiveUserId, stackVersion, retainedResourcesStr);
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
          if (resourceFailure) {
            return waitFor(activities.determineDeleteResourceFailures(stackId, accountId, stackVersion)) { String errorMessage ->
              activities.createGlobalStackEvent(
                stackId,
                accountId,
                stackOperationFailedStatus,
                errorMessage, stackVersion
              );
            }
          } else {
            return waitFor(
              activities.createGlobalStackEvent(stackId, accountId,
                stackOperationCompleteStatus,
                "", stackVersion)
            ) {

              if (deleteStackRecordsWhenSuccessful) {
                activities.deleteAllStackRecords(stackId, accountId);
              } else {
                promiseFor("");
              }
            }
          }
        }
      }.withCatch { Throwable t ->
        CommonDeleteRollbackPromises.LOG.error(t);
        CommonDeleteRollbackPromises.LOG.debug(t, t);
        Throwable cause = Throwables.getRootCause(t);
        Promise<String> errorMessagePromise = Promise.asPromise((cause != null) && (cause.getMessage() != null) ? cause.getMessage() : "");
        if (cause != null && cause instanceof ResourceFailureException) {
          errorMessagePromise = activities.determineDeleteResourceFailures(stackId, accountId, stackVersion);
        }
        waitFor(errorMessagePromise) { String errorMessage ->
          activities.createGlobalStackEvent(
            stackId,
            accountId,
            stackOperationFailedStatus,
            errorMessage, stackVersion
          );
        }
      }.getResult()
    }
  }

  Promise<String> getDeletePromise(String resourceId,
                                   String stackId,
                                   String accountId,
                                   String effectiveUserId,
                                   int deletedResourceVersion,
                                   String retainedResourcesStr) {
    Promise<String> getResourceTypePromise = activities.getResourceType(stackId, accountId, resourceId, deletedResourceVersion);
    waitFor(getResourceTypePromise) { String resourceType ->
      ResourceAction resourceAction = new ResourceResolverManager().resolveResourceAction(resourceType);
      Promise<String> initPromise = activities.initDeleteResource(resourceId, stackId, accountId, effectiveUserId, deletedResourceVersion, retainedResourcesStr);
      waitFor(initPromise) { String result ->
        if ("SKIP".equals(result)) {
          return promiseFor("SUCCESS");
        } else {
          return doTry {
            StepBasedResourceAction action = (StepBasedResourceAction) resourceAction
            Promise<String> deletePromise = new DeleteMultiStepPromise(
                workflowOperations, action.getDeleteStepIds( ), action
            ).getDeletePromise(resourceId, stackId, accountId, effectiveUserId, deletedResourceVersion);
            waitFor(deletePromise) {
              return activities.finalizeDeleteResource(resourceId, stackId, accountId, effectiveUserId, deletedResourceVersion);
            }
          }.withCatch { Throwable t->
            Throwable rootCause = Throwables.getRootCause(t);
            return activities.failDeleteResource(resourceId, stackId, accountId, effectiveUserId, rootCause.getMessage(), deletedResourceVersion);
          }.getResult();
        }
      }
    }
  }
}