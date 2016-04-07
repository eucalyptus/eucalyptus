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
import com.eucalyptus.cloudformation.entity.StackEntityHelper
import com.eucalyptus.cloudformation.entity.Status
import com.eucalyptus.cloudformation.resources.ResourceAction
import com.eucalyptus.cloudformation.resources.ResourceResolverManager
import com.eucalyptus.cloudformation.template.dependencies.DependencyManager
import com.eucalyptus.cloudformation.workflow.updateinfo.UpdateType
import com.eucalyptus.cloudformation.workflow.updateinfo.UpdateTypeAndDirection
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.common.base.Throwables
import com.google.common.collect.Lists
import com.google.common.collect.Maps
import com.netflix.glisten.WorkflowOperations
import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import org.apache.log4j.Logger

/**
 * Created by ethomas on 12/11/15.
 */
@CompileStatic(TypeCheckingMode.SKIP)
public class CommonUpdateRollbackPromises {
  private static final Logger LOG = Logger.getLogger(CommonUpdateRollbackPromises.class);
  @Delegate
  WorkflowOperations<StackActivityClient> workflowOperations;


  public CommonUpdateRollbackPromises(WorkflowOperations<StackActivityClient> workflowOperations) {
    this.workflowOperations = workflowOperations;
  }

  public Promise<String> performRollbackAndCleanup(String stackId, String accountId, String oldResourceDependencyManagerJson, String resourceDependencyManagerJson, String effectiveUserId, int rolledBackStackVersion, boolean continuingRollback) {
    Promise<String> recordUpdateRollbackInfo = continuingRollback ? promiseFor("") : activities.recordUpdateRollbackInfo(stackId, accountId, oldResourceDependencyManagerJson, resourceDependencyManagerJson, rolledBackStackVersion);
    waitFor(recordUpdateRollbackInfo) {
      Promise<String> rollbackResult = performRollback(stackId, accountId, oldResourceDependencyManagerJson, effectiveUserId, rolledBackStackVersion, continuingRollback);
      waitFor(rollbackResult) { String result ->
        if ("SUCCESS".equals(result)) {
          return performRollbackCleanup(stackId, accountId, resourceDependencyManagerJson, effectiveUserId, rolledBackStackVersion);
        } else return promiseFor("");
      }
    }
  }

  private Promise<String> performRollback(String stackId, String accountId, String oldResourceDependencyManagerJson, String effectiveUserId, int rolledBackStackVersion, boolean continuingRollback) {
    Promise<String> rollbackInitialStackPromise = continuingRollback ? promiseFor("") : activities.rollbackStackState(stackId, accountId, rolledBackStackVersion);
    waitFor(rollbackInitialStackPromise) {
      DependencyManager oldResourceDependencyManager = StackEntityHelper.jsonToResourceDependencyManager(
          oldResourceDependencyManagerJson
      );
      Map<String, Settable<String>> rollbackResourcePromiseMap = Maps.newConcurrentMap();
      for (String resourceId : oldResourceDependencyManager.getNodes()) {
        rollbackResourcePromiseMap.put(resourceId, new Settable<String>()); // placeholder promise
      }
      doTry {
        // Now for each resource, set up the promises and the dependencies they have for each other
        for (String resourceId : oldResourceDependencyManager.getNodes()) {
          String resourceIdLocalCopy = new String(resourceId);
          // passing "resourceId" into a waitFor() uses the for reference pointer after the for loop has expired
          Collection<Promise<String>> promisesDependedOn = Lists.newArrayList();
          for (String dependingResourceId : oldResourceDependencyManager.getReverseDependentNodes(resourceIdLocalCopy)) {
            promisesDependedOn.add(rollbackResourcePromiseMap.get(dependingResourceId));
          }
          AndPromise dependentAndPromise = new AndPromise(promisesDependedOn);
          waitFor(dependentAndPromise) {
            String reverseDependentResourcesJson = new ObjectMapper().writeValueAsString(
                oldResourceDependencyManager.getReverseDependentNodes(resourceIdLocalCopy) == null ?
                    Lists.<String> newArrayList() :
                    oldResourceDependencyManager.getReverseDependentNodes(resourceIdLocalCopy)
            );
            Promise<String> currentResourcePromise = getUpdateRollbackPromise(resourceIdLocalCopy, stackId, accountId, effectiveUserId, reverseDependentResourcesJson, rolledBackStackVersion);
            rollbackResourcePromiseMap.get(resourceIdLocalCopy).chain(currentResourcePromise);
            return currentResourcePromise;
          }
        }
        AndPromise allResourcePromises = new AndPromise(rollbackResourcePromiseMap.values());
        waitFor(allResourcePromises) {
          return activities.finalizeUpdateRollbackStack(stackId, accountId, rolledBackStackVersion);
        }
      }.withCatch { Throwable t ->
        CreateStackWorkflowImpl.LOG.error(t);
        CreateStackWorkflowImpl.LOG.debug(t, t);
        Throwable cause = Throwables.getRootCause(t);
        Promise<String> errorMessagePromise = Promise.asPromise((cause != null) && (cause.getMessage() != null) ? cause.getMessage() : "");
        if (cause != null && cause instanceof ResourceFailureException) {
          errorMessagePromise = activities.determineUpdateResourceFailures(stackId, accountId, rolledBackStackVersion);
        }
        waitFor(errorMessagePromise) { String errorMessage ->
          activities.failUpdateRollbackStack(stackId, accountId, rolledBackStackVersion, errorMessage);
        }
      }.getResult()
    }
  }

  Promise<String> getUpdateRollbackPromise(String resourceId,
                                           String stackId,
                                           String accountId,
                                           String effectiveUserId,
                                           String reverseDependentResourcesJson,
                                           int rolledBackResourceVersion) {
    Promise<String> checkAlreadyRolledBackOrStartedRollbackPromise = activities.checkResourceAlreadyRolledBackOrStartedRollback(stackId, accountId, resourceId);
    waitFor(checkAlreadyRolledBackOrStartedRollbackPromise) { String status ->
      if ("COMPLETED".equals(status)) {
        return promiseFor("");
      } else {
        int updatedResourceVersion = rolledBackResourceVersion - 1;
        // rolledBackResourceVersion is 1 more than the updated resource version.
        // The location of resources are: updatedResourceVersion - 1 : original version of stack, updatedResourceVersion: updated (failed) version of stack, updatedResourceVersion + 1: new version of original stack
        // In this case we know there is a resource at version updatedResourceVersion - 1
        // unless we've already started rollback, in which case there is a resource at rolledBackResourceVersion
        Promise<String> getResourceTypePromise = activities.getResourceType(stackId, accountId, resourceId, "STARTED".equals(status) ? rolledBackResourceVersion : updatedResourceVersion - 1);
        waitFor(getResourceTypePromise) { String resourceType ->
          final ResourceAction resourceAction = new ResourceResolverManager().resolveResourceAction(resourceType);
          Promise<String> initPromise = activities.initUpdateRollbackResource(resourceId, stackId, accountId, effectiveUserId, rolledBackResourceVersion);
          waitFor(initPromise) { String result ->
            Promise<String> resourcePromise;
            if ("SKIP".equals(result) || "NONE".equals(result)) {
              resourcePromise = promiseFor("");
            } else if ("CREATE".equals(result)) {
              resourcePromise = promiseFor(""); // item was created, will be cleaned up
            } else {
              Promise<String> updatePromise;
              if ("NO_PROPERTIES".equals(result) || UpdateType.UNSUPPORTED.toString().equals(result)) {
                // UNSUPPORTED means nothing changed.
                updatePromise = promiseFor("");
              } else if (UpdateType.NO_INTERRUPTION.toString().equals(result)) {
                updatePromise = resourceAction.getUpdatePromise(UpdateTypeAndDirection.UPDATE_ROLLBACK_NO_INTERRUPTION, workflowOperations, resourceId, stackId, accountId, effectiveUserId, rolledBackResourceVersion);
              } else if (UpdateType.SOME_INTERRUPTION.toString().equals(result)) {
                updatePromise = resourceAction.getUpdatePromise(UpdateTypeAndDirection.UPDATE_ROLLBACK_SOME_INTERRUPTION, workflowOperations, resourceId, stackId, accountId, effectiveUserId, rolledBackResourceVersion);
              } else {
                updatePromise = resourceAction.getUpdatePromise(UpdateTypeAndDirection.UPDATE_ROLLBACK_WITH_REPLACEMENT, workflowOperations, resourceId, stackId, accountId, effectiveUserId, rolledBackResourceVersion);
              }
              resourcePromise = waitFor(updatePromise) {
                activities.finalizeUpdateRollbackResource(resourceId, stackId, accountId, effectiveUserId, rolledBackResourceVersion);
              }
            }
            waitFor(resourcePromise) {
              activities.addCompletedUpdateRollbackResource(stackId, accountId, resourceId);
            }
          }
        }
      }
    }
  }

  private Promise<String> performRollbackCleanup(String stackId, String accountId, String resourceDependencyManagerJson, String effectiveUserId, int rolledBackStackVersion) {
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
          Promise<String> currentResourcePromise = getUpdateRollbackCleanupPromise(resourceIdLocalCopy, stackId, accountId, effectiveUserId, rolledBackStackVersion);
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
        return waitFor(activities.createGlobalStackEvent(stackId, accountId, Status.UPDATE_ROLLBACK_COMPLETE.toString(), errorMessage, rolledBackStackVersion)) {
          activities.finalizeUpdateRollbackCleanupStack(stackId, accountId, errorMessage, rolledBackStackVersion);
        }
      }
    }.withCatch { Throwable t ->
      CreateStackWorkflowImpl.LOG.error(t);
      CreateStackWorkflowImpl.LOG.debug(t, t);
      final String errorMessage = "One or more resources could not be deleted.";
      return waitFor(activities.createGlobalStackEvent(stackId, accountId, Status.UPDATE_ROLLBACK_COMPLETE.toString(), errorMessage, rolledBackStackVersion)) {
        activities.finalizeUpdateRollbackCleanupStack(stackId, accountId, errorMessage, rolledBackStackVersion);
      }
    }.getResult()
  }

  Promise<String> getUpdateRollbackCleanupPromise(String resourceId,
                                                  String stackId,
                                                  String accountId,
                                                  String effectiveUserId,
                                                  int rolledBackResourceVersion) {
    // all of these items are from the previous stack version
    Promise<String> getResourceTypePromise = activities.getResourceType(stackId, accountId, resourceId, rolledBackResourceVersion - 1);
    waitFor(getResourceTypePromise) { String resourceType ->
      ResourceAction resourceAction = new ResourceResolverManager().resolveResourceAction(resourceType);
      Promise<String> initPromise = activities.initUpdateRollbackCleanupResource(resourceId, stackId, accountId, effectiveUserId, rolledBackResourceVersion);
      waitFor(initPromise) { String result ->
        if ("SKIP".equals(result)) {
          return promiseFor("SUCCESS");
        } else {
          doTry {
            waitFor(resourceAction.getUpdateRollbackCleanupPromise(workflowOperations, resourceId, stackId, accountId, effectiveUserId, rolledBackResourceVersion)) {
              return activities.finalizeUpdateRollbackCleanupResource(resourceId, stackId, accountId, effectiveUserId, rolledBackResourceVersion);
            }
          }.withCatch { Throwable t ->
            Throwable rootCause = Throwables.getRootCause(t);
            return activities.failUpdateRollbackCleanupResource(resourceId, stackId, accountId, effectiveUserId, rootCause.getMessage(), rolledBackResourceVersion);
          }.getResult();
        }
      }
    }
  }




}
