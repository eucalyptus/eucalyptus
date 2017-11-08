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

package com.eucalyptus.cloudformation.workflow;

import com.amazonaws.services.simpleworkflow.flow.annotations.Activities;
import com.amazonaws.services.simpleworkflow.flow.annotations.ActivityRegistrationOptions;

@ActivityRegistrationOptions(
  defaultTaskScheduleToStartTimeoutSeconds = 900,
  defaultTaskStartToCloseTimeoutSeconds = 450,
  defaultTaskHeartbeatTimeoutSeconds = 450,
  defaultTaskScheduleToCloseTimeoutSeconds = 1350
)
@Activities(version="2.0")
public interface StackActivity {

  public String initCreateResource(String resourceId, String stackId, String accountId, String effectiveUserId, String reverseDependentResourcesJson, int stackVersion);
  public String validateAWSParameterTypes(String stackId, String accountId, String effectiveUserId, int stackVersion);
  public Boolean performCreateStep(String stepId, String resourceId, String stackId, String accountId, String effectiveUserId, int createdResourceVersion);
  public Boolean performDeleteStep(String stepId, String resourceId, String stackId, String accountId, String effectiveUserId, int deletedResourceVersion);
  public String determineCreateResourceFailures(String stackId, String accountId, int createdResourceVersion);
  public String determineUpdateResourceFailures(String stackId, String accountId, int updatedResourceVersion);
  public String determineDeleteResourceFailures(String stackId, String accountId, int deletedResourceVersion);
  public String initDeleteResource(String resourceId, String stackId, String accountId, String effectiveUserId, int deletedResourceVersion, String retainedResourcesStr);
  public String createGlobalStackEvent(String stackId, String accountId, String resourceStatus, String resourceStatusReason, int stackVersion);
  public String finalizeCreateStack(String stackId, String accountId, String effectiveUserId, int createdStackVersion);
  public String finalizeUpdateStack(String stackId, String accountId, String effectiveUserId, int updatedStackVersion);
  public String deleteAllStackRecords(String stackId, String accountId);
  public String getResourceType(String stackId, String accountId, String resourceId, int resourceVersion);
  public String finalizeCreateResource(String resourceId, String stackId, String accountId, String effectiveUserId, int createdResourceVersion);
  public String finalizeDeleteResource(String resourceId, String stackId, String accountId, String effectiveUserId, int updatedResourceVersion);
  public String failDeleteResource(String resourceId, String stackId, String accountId, String effectiveUserId, String errorMessage, int deletedResourceVersion);
  public String getCreateWorkflowExecutionCloseStatus(String stackId);
  public String getUpdateWorkflowExecutionCloseStatus(String stackId);
  public String getStackStatus(String stackId, String accountId, int stackVersion);
  public String setStackStatus(String stackId, String accountId, String status, String statusReason, int stackVersion);
  public String cancelCreateAndMonitorWorkflows(String stackId);
  public String verifyCreateAndMonitorWorkflowsClosed(String stackId);
  public String cancelOutstandingCreateResources(String stackId, String accountId, String cancelMessage, int createdResourceVersion);
  public String cancelOutstandingUpdateResources(String stackId, String accountId, String cancelMessage, int updatedResourceVersion);
  public String initUpdateResource(String resourceId, String stackId, String accountId, String effectiveUserId, String reverseDependentResourcesJson, int updatedResourceVersion);
  public String finalizeUpdateResource(String resourceId, String stackId, String accountId, String effectiveUserId, int updatedResourceVersion);
  public Boolean performUpdateStep(String updateTypeAndDirectionStr, String stepId, String resourceId, String stackId, String accountId, String effectiveUserId, int updatedResourceVersion);
  public String finalizeUpdateCleanupStack(String stackId, String accountId, String statusMessage, int updatedStackVersion);
  public String initUpdateCleanupResource(String resourceId, String stackId, String accountId, String effectiveUserId, int updatedResourceVersion);
  public String failUpdateCleanupResource(String resourceId, String stackId, String accountId, String effectiveUserId, String errorMessage, int updatedResourceVersion);
  public String finalizeUpdateCleanupResource(String resourceId, String stackId, String accountId, String effectiveUserId, int updatedResourceVersion);


  public String initUpdateRollbackResource(String resourceId, String stackId, String accountId, String effectiveUserId, int rolledBackResourceVersion);
  public String finalizeUpdateRollbackResource(String resourceId, String stackId, String accountId, String effectiveUserId, int rolledBackResourceVersion);


  public String finalizeUpdateRollbackStack(String stackId, String accountId, int rolledBackStackVersion);
  public String failUpdateRollbackStack(String stackId, String accountId, int rolledBackStackVersion, String errorMessage);
  public String failUpdateUnsupportedResource(String resourceId, String stackId, String accountId, String effectiveUserId, String errorMessage, int updatedResourceVersion);


  public String finalizeUpdateRollbackCleanupStack(String stackId, String accountId, String statusMessage, int rolledBackStackVersion);
  public String initUpdateRollbackCleanupResource(String resourceId, String stackId, String accountId, String effectiveUserId, int rolledBackResourceVersion);
  public String failUpdateRollbackCleanupResource(String resourceId, String stackId, String accountId, String effectiveUserId, String errorMessage, int rolledBackResourceVersion);
  public String finalizeUpdateRollbackCleanupResource(String resourceId, String stackId, String accountId, String effectiveUserId, int rolledBackResourceVersion);

  public String initUpdateRollbackStack(String stackId, String accountId, int rolledBackStackVersion);

  public String flattenStackForDelete(String stackId, String accountId);

  public String checkResourceAlreadyRolledBackOrStartedRollback(String stackId, String accountId, String resourceId);

  public String addCompletedUpdateRollbackResource(String stackId, String accountId, String resourceId);

  public Boolean checkInnerStackUpdate(String resourceId, String stackId, String accountId, String effectiveUserId, int updatedResourceVersion);

  public String initUpdateCleanupInnerStackUpdateResource(String resourceId, String stackId, String accountId, String effectiveUserId, int updatedResourceVersion);
  public String finalizeUpdateCleanupInnerStackUpdateResource(String resourceId, String stackId, String accountId, String effectiveUserId, int updatedResourceVersion);
  public Boolean performUpdateCleanupInnerStackUpdateStep(String stepId, String resourceId, String stackId, String accountId, String effectiveUserId, int updatedResourceVersion);


  public String initUpdateRollbackCleanupInnerStackUpdateResource(String resourceId, String stackId, String accountId, String effectiveUserId, int updatedResourceVersion);
  public String finalizeUpdateRollbackCleanupInnerStackUpdateResource(String resourceId, String stackId, String accountId, String effectiveUserId, int updatedResourceVersion);
  public Boolean performUpdateRollbackCleanupInnerStackUpdateStep(String stepId, String resourceId, String stackId, String accountId, String effectiveUserId, int updatedResourceVersion);

  public String kickOffUpdateRollbackCleanupStackWorkflow(String stackId, String accountId, String effectiveUserId);
  public String kickOffUpdateRollbackStackWorkflow(String stackId, String accountId, String outerStackArn, String effectiveUserId);
  public String kickOffUpdateCleanupStackWorkflow(String stackId, String accountId, String effectiveUserId);

  public String kickOffDeleteStackWorkflow(String effectiveUserId, String stackId, String stackName, String stackAccountId, String stackAccountAlias, String resourceDependencyManagerJson, int deletedStackVersion, String retainedResourcesStr);
  public String kickOffRollbackStackWorkflow(String effectiveUserId, String stackId, String stackName, String accountId, String accountAlias, String resourceDependencyManagerJson, int rolledBackStackVersion);
  public String logMessage(String level, String message);
  public String cancelOutstandingDeleteResources(String stackId, String accountId, String cancelMessage, int deletedResourceVersion);

  public String getWorkflowExecutionCloseStatus(String stackId, String workflowType);
  public String getStackStatusIfLatest(String stackId, String accountId, int stackVersion);
  public String setStackStatusIfLatest(String stackId, String accountId, String status, String statusReason, int stackVersion);

}
