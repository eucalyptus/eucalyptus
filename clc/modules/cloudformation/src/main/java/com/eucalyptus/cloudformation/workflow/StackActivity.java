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
import com.amazonaws.services.simpleworkflow.flow.core.Promise;

@ActivityRegistrationOptions(
  defaultTaskScheduleToStartTimeoutSeconds = 900,
  defaultTaskStartToCloseTimeoutSeconds = 450,
  defaultTaskHeartbeatTimeoutSeconds = 450,
  defaultTaskScheduleToCloseTimeoutSeconds = 1350
)
@Activities(version="1.0")
public interface StackActivity {
  public String initCreateResource(String resourceId, String stackId, String accountId, String effectiveUserId, String reverseDependentResourcesJson);
  public String validateAWSParameterTypes(String stackId, String accountId, String effectiveUserId);
  public Boolean performCreateStep(String stepId, String resourceId, String stackId, String accountId, String effectiveUserId);
  public Boolean performDeleteStep(String stepId, String resourceId, String stackId, String accountId, String effectiveUserId);
  public Boolean performUpdateCleanupStep(String stepId, String resourceId, String stackId, String accountId, String effectiveUserId);
  public String determineCreateResourceFailures(String stackId, String accountId);
  public String determineDeleteResourceFailures(String stackId, String accountId);
  public String initDeleteResource(String resourceId, String stackId, String accountId, String effectiveUserId);
  public String createGlobalStackEvent(String stackId, String accountId, String resourceStatus, String resourceStatusReason);
  public String finalizeCreateStack(String stackId, String accountId, String effectiveUserId);
  public String deleteAllStackRecords(String stackId, String accountId);
  public String getResourceType(String stackId, String accountId, String resourceId);
  public String finalizeCreateResource(String resourceId, String stackId, String accountId, String effectiveUserId);
  public String finalizeDeleteResource(String resourceId, String stackId, String accountId, String effectiveUserId);
  public String failDeleteResource(String resourceId, String stackId, String accountId, String effectiveUserId, String errorMessage);
  public String getCreateWorkflowExecutionCloseStatus(String stackId);
  public String getUpdateWorkflowExecutionCloseStatus(String stackId);
  public String getStackStatus(String stackId, String accountId);
  public String setStackStatus(String stackId, String accountId, String status, String statusReason);
  public String cancelCreateAndMonitorWorkflows(String stackId);
  public String verifyCreateAndMonitorWorkflowsClosed(String stackId);
  public Integer getAWSCloudFormationWaitConditionTimeout(String resourceId, String stackId, String accountId, String effectiveUserId);
  public String cancelOutstandingCreateResources(String stackId, String accountId, String cancelMessage);
  public String cancelOutstandingUpdateResources(String stackId, String accountId, String cancelMessage);
  public String initUpdateResource(String resourceId, String stackId, String accountId, String effectiveUserId, String reverseDependentResourcesJson);
  public String finalizeUpdateResource(String resourceId, String stackId, String accountId, String effectiveUserId);
  public Boolean performUpdateNoInterruptionStep(String stepId, String resourceId, String stackId, String accountId, String effectiveUserId);
  public Boolean performUpdateSomeInterruptionStep(String stepId, String resourceId, String stackId, String accountId, String effectiveUserId);
  public Boolean performUpdateWithReplacementStep(String stepId, String resourceId, String stackId, String accountId, String effectiveUserId);
  public String finalizeUpdateCleanupStack(String stackId, String accountId, String statusMessage);
  public String initUpdateCleanupResource(String resourceId, String stackId, String accountId, String effectiveUserId);
  public String failUpdateCleanupResource(String resourceId, String stackId, String accountId, String effectiveUserId, String errorMessage);
  public String finalizeUpdateCleanupResource(String resourceId, String stackId, String accountId, String effectiveUserId);
  public String removeUpdateCleanupResourceFromStack(String resourceId, String stackId, String accountId);
}
