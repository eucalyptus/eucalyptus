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
import com.eucalyptus.cloudformation.resources.ResourceAction
import com.eucalyptus.cloudformation.resources.ResourceResolverManager
import com.eucalyptus.cloudformation.template.dependencies.DependencyManager
import com.eucalyptus.cloudformation.workflow.updateinfo.UpdateType
import com.eucalyptus.cloudformation.workflow.updateinfo.UpdateTypeAndDirection
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
public class UpdateRollbackStackWorkflowImpl implements UpdateRollbackStackWorkflow {
  private static final Logger LOG = Logger.getLogger(UpdateRollbackStackWorkflowImpl.class);

  @Delegate
  WorkflowOperations<StackActivityClient> workflowOperations = SwfWorkflowOperations.of(StackActivityClient)
  WorkflowUtils workflowUtils = new WorkflowUtils( workflowOperations )

  @Override
  void performUpdateRollbackStack(String stackId, String accountId, String outerStackArn, String oldResourceDependencyManagerJson, String effectiveUserId, int rolledBackStackVersion) {
    try {
      doTry {
        return performRollbackAndMaybeCleanup(stackId, accountId, outerStackArn, oldResourceDependencyManagerJson, effectiveUserId, rolledBackStackVersion);
      } withCatch { Throwable t->
        UpdateRollbackStackWorkflowImpl.LOG.error(t);
        UpdateRollbackStackWorkflowImpl.LOG.debug(t, t);
      }
    } catch (Exception ex) {
      UpdateRollbackStackWorkflowImpl.LOG.error(ex);
      UpdateRollbackStackWorkflowImpl.LOG.debug(ex, ex);
    }
  }

  public Promise<String> performRollbackAndMaybeCleanup(String stackId, String accountId, String outerStackArn, String oldResourceDependencyManagerJson, String effectiveUserId, int rolledBackStackVersion) {
    Promise<String> rollbackResult = performRollback(stackId, accountId, oldResourceDependencyManagerJson, effectiveUserId, rolledBackStackVersion);
    waitFor(rollbackResult) { String result ->
      if ("SUCCESS".equals(result) && outerStackArn == null) {
        return activities.kickOffUpdateRollbackCleanupStackWorkflow(stackId, accountId, effectiveUserId);
      } else return promiseFor("");
    }
  }

  private Promise<String> performRollback(String stackId, String accountId, String oldResourceDependencyManagerJson, String effectiveUserId, int rolledBackStackVersion) {
    Promise<String> rollbackInitialStackPromise = activities.initUpdateRollbackStack(stackId, accountId, rolledBackStackVersion);
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
            Promise<String> currentResourcePromise = getUpdateRollbackPromise(resourceIdLocalCopy, stackId, accountId, effectiveUserId, rolledBackStackVersion);
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

  Promise<String> getUpdateRollbackPromise(String resourceId, String stackId, String accountId, String effectiveUserId, int rolledBackResourceVersion) {
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

}
