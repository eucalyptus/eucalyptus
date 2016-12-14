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

package com.eucalyptus.cloudformation.workflow;

import com.amazonaws.services.simpleworkflow.flow.annotations.Activities;
import com.amazonaws.services.simpleworkflow.flow.annotations.ActivityRegistrationOptions;

@ActivityRegistrationOptions(
  defaultTaskScheduleToStartTimeoutSeconds = 900,
  defaultTaskStartToCloseTimeoutSeconds = 450,
  defaultTaskHeartbeatTimeoutSeconds = 450,
  defaultTaskScheduleToCloseTimeoutSeconds = 1350
)
@Activities(version="1.0")
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

}
