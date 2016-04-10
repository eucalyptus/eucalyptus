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
import com.amazonaws.services.simpleworkflow.flow.interceptors.ExponentialRetryPolicy
import com.eucalyptus.cloudformation.CloudFormation
import com.eucalyptus.cloudformation.InternalFailureException
import com.eucalyptus.cloudformation.entity.StackEntityHelper
import com.eucalyptus.cloudformation.entity.Status
import com.eucalyptus.cloudformation.resources.ResourceAction
import com.eucalyptus.cloudformation.resources.ResourceResolverManager
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

import java.util.concurrent.TimeUnit

@ComponentPart(CloudFormation)
@CompileStatic(TypeCheckingMode.SKIP)
public class MonitorUpdateStackWorkflowImpl implements MonitorUpdateStackWorkflow {
  private static final Logger LOG = Logger.getLogger(MonitorUpdateStackWorkflowImpl.class);

  @Delegate
  WorkflowOperations<StackActivityClient> workflowOperations = SwfWorkflowOperations.of(StackActivityClient)
  WorkflowUtils workflowUtils = new WorkflowUtils( workflowOperations )

  @Override
  void monitorUpdateStack(String stackId, String accountId, String oldResourceDependencyManagerJson, String resourceDependencyManagerJson, String effectiveUserId, int updatedStackVersion, String outerStackArn) {
    try {
      doTry {
        Promise<String> closeStatusPromise = workflowUtils.fixedPollWithTimeout((int) TimeUnit.DAYS.toSeconds(365), 10) {
          retry(new ExponentialRetryPolicy(2L).withMaximumAttempts(6)) {
            activities.getUpdateWorkflowExecutionCloseStatus(stackId)
          }
        }
        waitFor(closeStatusPromise) { String closedStatus ->
          if (!closedStatus) {
            throw new InternalFailureException("Stack update timeout stack id ${stackId}");
          }
          waitFor(activities.getStackStatus(stackId, accountId, updatedStackVersion)) { String stackStatus ->
            waitFor(dealWithPrematureClosure(closedStatus, stackStatus, stackId, accountId, updatedStackVersion)) { String revisedStackStatus ->
              determineRollbackOrCleanupAction(revisedStackStatus, outerStackArn, stackId, accountId, oldResourceDependencyManagerJson, resourceDependencyManagerJson, effectiveUserId, updatedStackVersion);
            }
          }
        }
      } withCatch { Throwable t->
        MonitorUpdateStackWorkflowImpl.LOG.error(t);
        MonitorUpdateStackWorkflowImpl.LOG.debug(t, t);
      }
    } catch (Exception ex) {
      MonitorUpdateStackWorkflowImpl.LOG.error(ex);
      MonitorUpdateStackWorkflowImpl.LOG.debug(ex, ex);
    }
  }

  private Promise<String> dealWithPrematureClosure(String closedStatus, String stackStatus, String stackId, String accountId, int updatedStackVersion ) {
    if (Status.UPDATE_IN_PROGRESS.toString().equals(stackStatus)) {
      // Once here, stack update has failed.  Only in some cases do we know why.
      String statusReason = "";
      if ("CANCELED".equals(closedStatus)) {
        statusReason = "Stack update was canceled by user.";
      } else if ("TERMINATED".equals(closedStatus)) {
        statusReason = "Stack update was terminated by user.";
      } else if ("TIMED_OUT".equals(closedStatus)) {
        statusReason = "Stack update timed out.";
      } else if ("COMPLETED".equals(closedStatus)) {
        statusReason = "";
      } else if ("FAILED".equals(closedStatus)) {
         statusReason = "Stack update workflow failed.";
      } else if ("CONTINUED_AS_NEW".equals(closedStatus)) {
        throw new InternalFailureException("Unsupported close status for workflow " + closedStatus);
      } else {
        throw new InternalFailureException("Unsupported close status for workflow " + closedStatus);
      }
      Promise<String> cancelOutstandingCreateResources = activities.cancelOutstandingCreateResources(stackId, accountId, "Resource update cancelled.", updatedStackVersion);
      Promise<String> cancelOutstandingUpdateResources = waitFor(cancelOutstandingCreateResources) {
        activities.cancelOutstandingUpdateResources(stackId, accountId, "Resource update cancelled.", updatedStackVersion);
      };
      Promise<String> setStackStatusPromise = waitFor(cancelOutstandingUpdateResources) {
        activities.setStackStatus(stackId, accountId,
            Status.UPDATE_ROLLBACK_IN_PROGRESS.toString(), statusReason, updatedStackVersion)
      };
      return waitFor(setStackStatusPromise) {
        return promiseFor(Status.UPDATE_ROLLBACK_IN_PROGRESS.toString());
      }
    } else {
      return promiseFor(stackStatus);
    }
  }

  private Promise<String> determineRollbackOrCleanupAction(String stackStatus, String outerStackArn, String stackId, String accountId,
                                                           String oldResourceDependencyManagerJson, String resourceDependencyManagerJson, String effectiveUserId, int updatedStackVersion ) {
    Promise<String> waitForOuterStackRollbackOrCleanupSignalPromise = workflowUtils.fixedPollWithTimeout((int) TimeUnit.DAYS.toSeconds(365), 10) {
      retry(new ExponentialRetryPolicy(2L).withMaximumAttempts(6)) {
        activities.waitForOuterStackRollbackOrCleanupSignal(stackId, accountId, outerStackArn); // TODO: determine resource version?
      }
    };

    waitFor(waitForOuterStackRollbackOrCleanupSignalPromise) { String action ->
      if (action.equals("CLEANUP") || (action.equals("NORMAL") && Status.UPDATE_COMPLETE_CLEANUP_IN_PROGRESS.toString().equals(stackStatus))) {
        return performCleanup(stackId, accountId, oldResourceDependencyManagerJson, effectiveUserId, updatedStackVersion ); // just done...
      } else if (action.equals("ROLLBACK") ||
          (action.equals("NORMAL") && (Status.UPDATE_FAILED.toString().equals(stackStatus) || Status.UPDATE_ROLLBACK_IN_PROGRESS.toString().equals(stackStatus)))) {
        int rolledBackStackVersion = updatedStackVersion + 1;
        return new CommonUpdateRollbackPromises(workflowOperations, workflowUtils).performRollbackAndCleanup(stackId, accountId, outerStackArn, oldResourceDependencyManagerJson, resourceDependencyManagerJson, effectiveUserId, rolledBackStackVersion, false);
      } else {
        throw new InternalFailureException("Unexpected stack status " + stackStatus + " during update monitoring");
      }
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
