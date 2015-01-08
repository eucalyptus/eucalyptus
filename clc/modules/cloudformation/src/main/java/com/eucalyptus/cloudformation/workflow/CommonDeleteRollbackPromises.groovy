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

import com.amazonaws.services.simpleworkflow.flow.core.AndPromise
import com.amazonaws.services.simpleworkflow.flow.core.Promise
import com.amazonaws.services.simpleworkflow.flow.core.Settable
import com.eucalyptus.cloudformation.entity.StackEntity
import com.eucalyptus.cloudformation.entity.StackEntityHelper
import com.eucalyptus.cloudformation.entity.StackResourceEntity
import com.eucalyptus.cloudformation.resources.ResourceAction
import com.eucalyptus.cloudformation.resources.ResourceResolverManager
import com.eucalyptus.cloudformation.template.dependencies.DependencyManager
import com.google.common.base.Throwables
import com.google.common.collect.Lists
import com.google.common.collect.Maps
import com.netflix.glisten.WorkflowOperations
import com.netflix.glisten.impl.swf.SwfWorkflowOperations
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
  WorkflowOperations<StackActivity> workflowOperations;
  String stackOperationInProgressStatus;
  String stackOperationInProgressStatusReason;
  String stackOperationFailedStatus;
  String stackOperationCompleteStatus;
  boolean deleteStackRecordsWhenSuccessful;

  CommonDeleteRollbackPromises(WorkflowOperations<StackActivity> workflowOperations, String stackOperationInProgressStatus,
                               String stackOperationInProgressStatusReason, String stackOperationFailedStatus, String stackOperationCompleteStatus,
                               boolean deleteStackRecordsWhenSuccessful) {
    this.workflowOperations = workflowOperations
    this.stackOperationInProgressStatus = stackOperationInProgressStatus
    this.stackOperationInProgressStatusReason = stackOperationInProgressStatusReason
    this.stackOperationFailedStatus = stackOperationFailedStatus
    this.stackOperationCompleteStatus = stackOperationCompleteStatus
    this.deleteStackRecordsWhenSuccessful = deleteStackRecordsWhenSuccessful
  }

  public Promise<?> getPromise(String stackId, String accountId, String resourceDependencyManagerJson, String effectiveUserId) {
    Promise<String> deleteInitialStackPromise = promiseFor(
      activities.createGlobalStackEvent(
        stackId,
        accountId,
        stackOperationInProgressStatus,
        stackOperationInProgressStatusReason
      )
    );
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
            Promise<String> currentResourcePromise = getDeletePromise(resourceIdLocalCopy, stackId, accountId, effectiveUserId);
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
            return waitFor(promiseFor(activities.determineDeleteResourceFailures(stackId, accountId))) { String errorMessage ->
              promiseFor(activities.createGlobalStackEvent(
                stackId,
                accountId,
                stackOperationFailedStatus,
                errorMessage)
              );
            }
          } else {
            return waitFor(
              promiseFor(activities.createGlobalStackEvent(stackId, accountId,
                stackOperationCompleteStatus,
                ""))
            ) {

              if (deleteStackRecordsWhenSuccessful) {
                promiseFor(activities.deleteAllStackRecords(stackId, accountId));
              } else {
                promiseFor("");
              }
            }
          }
        }
      }.withCatch { Throwable t ->
        CreateStackWorkflowImpl.LOG.error(t);
        CreateStackWorkflowImpl.LOG.debug(t, t);
        Throwable cause = Throwables.getRootCause(t);
        Promise<String> errorMessagePromise = Promise.asPromise((cause != null) && (cause.getMessage() != null) ? cause.getMessage() : "");
        if (cause != null && cause instanceof ResourceFailureException) {
          errorMessagePromise = promiseFor(activities.determineDeleteResourceFailures(stackId, accountId));
        }
        waitFor(errorMessagePromise) { String errorMessage ->
          promiseFor(activities.createGlobalStackEvent(
            stackId,
            accountId,
            stackOperationFailedStatus,
            errorMessage)
          );
        }
      }.getResult()
    }
  }

  Promise<String> getDeletePromise(String resourceId,
                                   String stackId,
                                   String accountId,
                                   String effectiveUserId) {
    Promise<String> getResourceTypePromise = promiseFor(activities.getResourceType(stackId, accountId, resourceId));
    waitFor(getResourceTypePromise) { String resourceType ->
      ResourceAction resourceAction = new ResourceResolverManager().resolveResourceAction(resourceType);
      Promise<String> initPromise = promiseFor(activities.initDeleteResource(resourceId, stackId, accountId, effectiveUserId));
      waitFor(initPromise) { String result ->
        if ("SKIP".equals(result)) {
          return promiseFor("SUCCESS");
        } else {
          return doTry {
            waitFor(promiseFor(resourceAction.getDeletePromise(workflowOperations, resourceId, stackId, accountId, effectiveUserId))) {
              return promiseFor(activities.finalizeDeleteResource(resourceId, stackId, accountId, effectiveUserId));
            }
          }.withCatch { Throwable t->
            Throwable rootCause = Throwables.getRootCause(t);
            return promiseFor(activities.failDeleteResource(resourceId, stackId, accountId, effectiveUserId, rootCause.getMessage()));
          }.getResult();
        }
      }
    }
  }
}