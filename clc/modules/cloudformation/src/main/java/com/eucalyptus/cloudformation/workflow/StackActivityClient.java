/*************************************************************************
 * Copyright 2009-2015 Ent. Services Development Corporation LP
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
package com.eucalyptus.cloudformation.workflow;

import com.amazonaws.services.simpleworkflow.flow.annotations.Activities;
import com.amazonaws.services.simpleworkflow.flow.annotations.Activity;
import com.amazonaws.services.simpleworkflow.flow.annotations.ActivityRegistrationOptions;
import com.amazonaws.services.simpleworkflow.flow.core.Promise;

/**
 * StackActivity but more promising.
 *
 * @see StackActivity
 */
@ActivityRegistrationOptions(
    defaultTaskScheduleToStartTimeoutSeconds = 900,
    defaultTaskStartToCloseTimeoutSeconds = 450,
    defaultTaskHeartbeatTimeoutSeconds = 450,
    defaultTaskScheduleToCloseTimeoutSeconds = 1350
)
@Activities(version="2.0")
public interface StackActivityClient {
  @Activity(name = "StackActivity.initCreateResource")
  Promise<String> initCreateResource(String resourceId, String stackId, String accountId, String effectiveUserId, String reverseDependentResourcesJson, int stackVersion);

  @Activity(name = "StackActivity.validateAWSParameterTypes")
  Promise<String> validateAWSParameterTypes(String stackId, String accountId, String effectiveUserId, int stackVersion);

  @Activity(name = "StackActivity.performCreateStep")
  Promise<Boolean> performCreateStep(String stepId, String resourceId, String stackId, String accountId, String effectiveUserId, int createdResourceVersion);

  @Activity(name = "StackActivity.performDeleteStep")
  Promise<Boolean> performDeleteStep(String stepId, String resourceId, String stackId, String accountId, String effectiveUserId, int deletedResourceVersion);

  @Activity(name = "StackActivity.determineCreateResourceFailures")
  Promise<String> determineCreateResourceFailures(String stackId, String accountId, int createdResourceVersion);

  @Activity(name = "StackActivity.determineUpdateResourceFailures")
  Promise<String> determineUpdateResourceFailures(String stackId, String accountId, int updatedResourceVersion);

  @Activity(name = "StackActivity.determineDeleteResourceFailures")
  Promise<String> determineDeleteResourceFailures(String stackId, String accountId, int deletedResourceVersion);

  @Activity(name = "StackActivity.initDeleteResource")
  Promise<String> initDeleteResource(String resourceId, String stackId, String accountId, String effectiveUserId, int deletedResourceVersion, String retainedResourcesStr);

  @Activity(name = "StackActivity.createGlobalStackEvent")
  Promise<String> createGlobalStackEvent(String stackId, String accountId, String resourceStatus, String resourceStatusReason, int stackVersion);

  @Activity(name = "StackActivity.finalizeCreateStack")
  Promise<String> finalizeCreateStack(String stackId, String accountId, String effectiveUserId, int createdStackVersion);

  @Activity(name = "StackActivity.finalizeUpdateStack")
  Promise<String> finalizeUpdateStack(String stackId, String accountId, String effectiveUserId, int updatedStackVersion);

  @Activity(name = "StackActivity.deleteAllStackRecords")
  Promise<String> deleteAllStackRecords(String stackId, String accountId);

  @Activity(name = "StackActivity.getResourceType")
  Promise<String> getResourceType(String stackId, String accountId, String resourceId, int resourceVersion);

  @Activity(name = "StackActivity.finalizeCreateResource")
  Promise<String> finalizeCreateResource(String resourceId, String stackId, String accountId, String effectiveUserId, int createdResourceVersion);

  @Activity(name = "StackActivity.finalizeDeleteResource")
  Promise<String> finalizeDeleteResource(String resourceId, String stackId, String accountId, String effectiveUserId, int updatedResourceVersion);

  @Activity(name = "StackActivity.failDeleteResource")
  Promise<String> failDeleteResource(String resourceId, String stackId, String accountId, String effectiveUserId, String errorMessage, int deletedResourceVersion);

  @Activity(name = "StackActivity.getCreateWorkflowExecutionCloseStatus")
  Promise<String> getCreateWorkflowExecutionCloseStatus(String stackId);

  @Activity(name = "StackActivity.getUpdateWorkflowExecutionCloseStatus")
  Promise<String> getUpdateWorkflowExecutionCloseStatus(String stackId);

  @Activity(name = "StackActivity.getStackStatus")
  Promise<String> getStackStatus(String stackId, String accountId, int stackVersion);

  @Activity(name = "StackActivity.setStackStatus")
  Promise<String> setStackStatus(String stackId, String accountId, String status, String statusReason, int stackVersion);

  @Activity(name = "StackActivity.cancelCreateAndMonitorWorkflows")
  Promise<String> cancelCreateAndMonitorWorkflows(String stackId);

  @Activity(name = "StackActivity.verifyCreateAndMonitorWorkflowsClosed")
  Promise<String> verifyCreateAndMonitorWorkflowsClosed(String stackId);

  @Activity(name = "StackActivity.cancelOutstandingCreateResources")
  Promise<String> cancelOutstandingCreateResources(String stackId, String accountId, String cancelMessage, int createdResourceVersion);

  @Activity(name = "StackActivity.cancelOutstandingUpdateResources")
  Promise<String> cancelOutstandingUpdateResources(String stackId, String accountId, String cancelMessage, int updatedResourceVersion);

  @Activity(name = "StackActivity.initUpdateResource")
  Promise<String> initUpdateResource(String resourceId, String stackId, String accountId, String effectiveUserId, String reverseDependentResourcesJson, int updatedResourceVersion);

  @Activity(name = "StackActivity.finalizeUpdateResource")
  Promise<String> finalizeUpdateResource(String resourceId, String stackId, String accountId, String effectiveUserId, int updatedResourceVersion);

  @Activity(name = "StackActivity.performUpdateStep")
  Promise<Boolean> performUpdateStep(String updateTypeAndDirectionStr, String stepId, String resourceId, String stackId, String accountId, String effectiveUserId, int updatedResourceVersion);

  @Activity(name = "StackActivity.finalizeUpdateCleanupStack")
  Promise<String> finalizeUpdateCleanupStack(String stackId, String accountId, String statusMessage, int updatedStackVersion);

  @Activity(name = "StackActivity.initUpdateCleanupResource")
  Promise<String> initUpdateCleanupResource(String resourceId, String stackId, String accountId, String effectiveUserId, int updatedResourceVersion);

  @Activity(name = "StackActivity.failUpdateCleanupResource")
  Promise<String> failUpdateCleanupResource(String resourceId, String stackId, String accountId, String effectiveUserId, String errorMessage, int updatedResourceVersion);

  @Activity(name = "StackActivity.finalizeUpdateCleanupResource")
  Promise<String> finalizeUpdateCleanupResource(String resourceId, String stackId, String accountId, String effectiveUserId, int updatedResourceVersion);

  @Activity(name = "StackActivity.failUpdateUnsupportedResource")
  Promise<String> failUpdateUnsupportedResource(String resourceId, String stackId, String accountId, String effectiveUserId, String errorMessage, int updatedResourceVersion);

  @Activity(name = "StackActivity.initUpdateRollbackResource")
  Promise<String> initUpdateRollbackResource(String resourceId, String stackId, String accountId, String effectiveUserId, int rolledBackResourceVersion);
  @Activity(name = "StackActivity.finalizeUpdateRollbackResource")
  Promise<String> finalizeUpdateRollbackResource(String resourceId, String stackId, String accountId, String effectiveUserId, int rolledBackResourceVersion);

  @Activity(name = "StackActivity.finalizeUpdateRollbackStack")
  Promise<String> finalizeUpdateRollbackStack(String stackId, String accountId, int rolledBackStackVersion);
  @Activity(name = "StackActivity.failUpdateRollbackStack")
  Promise<String> failUpdateRollbackStack(String stackId, String accountId, int rolledBackStackVersion, String errorMessage);


  @Activity(name = "StackActivity.finalizeUpdateRollbackCleanupStack")
  Promise<String> finalizeUpdateRollbackCleanupStack(String stackId, String accountId, String statusMessage, int rolledBackStackVersion);
  @Activity(name = "StackActivity.initUpdateRollbackCleanupResource")
  Promise<String> initUpdateRollbackCleanupResource(String resourceId, String stackId, String accountId, String effectiveUserId, int rolledBackResourceVersion);
  @Activity(name = "StackActivity.failUpdateRollbackCleanupResource")
  Promise<String> failUpdateRollbackCleanupResource(String resourceId, String stackId, String accountId, String effectiveUserId, String errorMessage, int rolledBackResourceVersion);
  @Activity(name = "StackActivity.finalizeUpdateRollbackCleanupResource")
  Promise<String> finalizeUpdateRollbackCleanupResource(String resourceId, String stackId, String accountId, String effectiveUserId, int rolledBackResourceVersion);

  @Activity(name = "StackActivity.initUpdateRollbackStack")
  Promise<String> initUpdateRollbackStack(String stackId, String accountId, int rolledBackStackVersion);

  @Activity(name = "StackActivity.flattenStackForDelete")
  Promise<String> flattenStackForDelete(String stackId, String accountId);

  @Activity(name = "StackActivity.checkResourceAlreadyRolledBackOrStartedRollback")
  Promise<String> checkResourceAlreadyRolledBackOrStartedRollback(String stackId, String accountId, String resourceId);

  @Activity(name = "StackActivity.addCompletedUpdateRollbackResource")
  Promise<String> addCompletedUpdateRollbackResource(String stackId, String accountId, String resourceId);

  @Activity(name = "StackActivity.checkInnerStackUpdate")
  Promise<Boolean> checkInnerStackUpdate(String resourceId, String stackId, String accountId, String effectiveUserId, int updatedResourceVersion);

  @Activity(name = "StackActivity.initUpdateCleanupInnerStackUpdateResource")
  Promise<String> initUpdateCleanupInnerStackUpdateResource(String resourceId, String stackId, String accountId, String effectiveUserId, int updatedResourceVersion);
  @Activity(name = "StackActivity.finalizeUpdateCleanupInnerStackUpdateResource")
  Promise<String> finalizeUpdateCleanupInnerStackUpdateResource(String resourceId, String stackId, String accountId, String effectiveUserId, int updatedResourceVersion);
  @Activity(name = "StackActivity.performUpdateCleanupInnerStackUpdateStep")
  Promise<Boolean> performUpdateCleanupInnerStackUpdateStep(String stepId, String resourceId, String stackId, String accountId, String effectiveUserId, int updatedResourceVersion);

  @Activity(name = "StackActivity.initUpdateRollbackCleanupInnerStackUpdateResource")
  Promise<String> initUpdateRollbackCleanupInnerStackUpdateResource(String resourceId, String stackId, String accountId, String effectiveUserId, int updatedResourceVersion);
  @Activity(name = "StackActivity.finalizeUpdateRollbackCleanupInnerStackUpdateResource")
  Promise<String> finalizeUpdateRollbackCleanupInnerStackUpdateResource(String resourceId, String stackId, String accountId, String effectiveUserId, int updatedResourceVersion);
  @Activity(name = "StackActivity.performUpdateRollbackCleanupInnerStackUpdateStep")
  Promise<Boolean> performUpdateRollbackCleanupInnerStackUpdateStep(String stepId, String resourceId, String stackId, String accountId, String effectiveUserId, int updatedResourceVersion);

  @Activity(name = "StackActivity.kickOffUpdateRollbackCleanupStackWorkflow")
  Promise<String> kickOffUpdateRollbackCleanupStackWorkflow(String stackId, String accountId, String effectiveUserId);

  @Activity(name = "StackActivity.kickOffUpdateRollbackStackWorkflow")
  Promise<String> kickOffUpdateRollbackStackWorkflow(String stackId, String accountId, String outerStackArn, String effectiveUserId);

  @Activity(name = "StackActivity.kickOffUpdateCleanupStackWorkflow")
  Promise<String> kickOffUpdateCleanupStackWorkflow(String stackId, String accountId, String effectiveUserId);

  @Activity(name = "StackActivity.kickOffDeleteStackWorkflow")
  Promise<String> kickOffDeleteStackWorkflow(String effectiveUserId, String stackId, String stackName, String stackAccountId, String stackAccountAlias, String resourceDependencyManagerJson, int deletedStackVersion, String retainedResourcesStr);

  @Activity(name = "StackActivity.kickOffRollbackStackWorkflow")
  Promise<String> kickOffRollbackStackWorkflow(String effectiveUserId, String stackId, String stackName, String accountId, String accountAlias, String resourceDependencyManagerJson, int rolledBackStackVersion);

  @Activity(name = "StackActivity.logMessage")
  Promise<String> logMessage(String level, String message);

  @Activity(name = "StackActivity.cancelOutstandingDeleteResources")
  Promise<String> cancelOutstandingDeleteResources(String stackId, String accountId, String cancelMessage, int deletedResourceVersion);

  @Activity(name = "StackActivity.getWorkflowExecutionCloseStatus")
  Promise<String> getWorkflowExecutionCloseStatus(String stackId, String workflowType);

  @Activity(name = "StackActivity.getStackStatusIfLatest")
  Promise<String> getStackStatusIfLatest(String stackId, String accountId, int stackVersion);

  @Activity(name = "StackActivity.setStackStatusIfLatest")
  Promise<String> setStackStatusIfLatest(String stackId, String accountId, String status, String statusReason, int stackVersion);


}
