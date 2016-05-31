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
import com.eucalyptus.cloudformation.resources.standard.actions.AWSCloudFormationStackResourceAction
import com.eucalyptus.cloudformation.template.dependencies.DependencyManager
import com.eucalyptus.component.annotation.ComponentPart
import com.eucalyptus.simpleworkflow.common.workflow.WorkflowUtils
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
public class UpdateRollbackCleanupStackWorkflowImpl implements UpdateRollbackCleanupStackWorkflow {
  private static final Logger LOG = Logger.getLogger(UpdateRollbackCleanupStackWorkflowImpl.class);

  @Delegate
  WorkflowOperations<StackActivityClient> workflowOperations = SwfWorkflowOperations.of(StackActivityClient)
  WorkflowUtils workflowUtils = new WorkflowUtils( workflowOperations )

  @Override
  void performUpdateRollbackCleanupStack(String stackId, String accountId, String resourceDependencyManagerJson, String effectiveUserId, int rolledBackStackVersion) {
    try {
      doTry {
        return performRollbackCleanup(stackId, accountId, resourceDependencyManagerJson, effectiveUserId, rolledBackStackVersion);
      } withCatch { Throwable t->
        UpdateRollbackCleanupStackWorkflowImpl.LOG.error(t);
        UpdateRollbackCleanupStackWorkflowImpl.LOG.debug(t, t);
      }
    } catch (Exception ex) {
      UpdateRollbackCleanupStackWorkflowImpl.LOG.error(ex);
      UpdateRollbackCleanupStackWorkflowImpl.LOG.debug(ex, ex);
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
    // we know there is at least a resource in the previous stack version, so we check that for resource type
    Promise<String> getResourceTypePromise = activities.getResourceType(stackId, accountId, resourceId, rolledBackResourceVersion - 1);
    waitFor(getResourceTypePromise) { String resourceType ->
      ResourceAction resourceAction = new ResourceResolverManager().resolveResourceAction(resourceType);
      Promise<Boolean> checkInnerStackUpdateSpecialCasePromise = activities.checkInnerStackUpdate(resourceId, stackId, accountId, effectiveUserId, rolledBackResourceVersion)
      waitFor(checkInnerStackUpdateSpecialCasePromise) { Boolean innerStackUpdateSpecialCase ->
        if (Boolean.TRUE.equals(innerStackUpdateSpecialCase)) {
          Promise<String> initPromise = activities.initUpdateRollbackCleanupInnerStackUpdateResource(resourceId, stackId, accountId, effectiveUserId, rolledBackResourceVersion);
          waitFor(initPromise) {
            waitFor(((AWSCloudFormationStackResourceAction) resourceAction).getUpdateRollbackCleanupUpdatePromise(workflowOperations, resourceId, stackId, accountId, effectiveUserId, rolledBackResourceVersion)) {
              return activities.finalizeUpdateRollbackCleanupInnerStackUpdateResource(resourceId, stackId, accountId, effectiveUserId, rolledBackResourceVersion);
            }
          }
        } else {
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
  }

}
