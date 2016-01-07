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
  public String initCreateResource(String resourceId, String stackId, String accountId, String effectiveUserId, String reverseDependentResourcesJson, int updateVersion);
  public String validateAWSParameterTypes(String stackId, String accountId, String effectiveUserId, int updateVersion);
  public Boolean performCreateStep(String stepId, String resourceId, String stackId, String accountId, String effectiveUserId, int updateVersion);
  public Boolean performDeleteStep(String stepId, String resourceId, String stackId, String accountId, String effectiveUserId, int updateVersion);
  public Boolean performUpdateCleanupStep(String stepId, String resourceId, String stackId, String accountId, String effectiveUserId, int updateVersion);
  public String determineCreateResourceFailures(String stackId, String accountId, int updateVersion);
  public String determineDeleteResourceFailures(String stackId, String accountId, int updateVersion);
  public String initDeleteResource(String resourceId, String stackId, String accountId, String effectiveUserId, int updateVersion);
  public String createGlobalStackEvent(String stackId, String accountId, String resourceStatus, String resourceStatusReason, int updateVersion);
  public String finalizeCreateStack(String stackId, String accountId, String effectiveUserId, int updateVersion);
  public String finalizeUpdateStack(String stackId, String accountId, String effectiveUserId, int updateVersion);
  public String deleteAllStackRecords(String stackId, String accountId);
  public String getResourceType(String stackId, String accountId, String resourceId, int updateVersion);
  public String finalizeCreateResource(String resourceId, String stackId, String accountId, String effectiveUserId, int updateVersion);
  public String finalizeDeleteResource(String resourceId, String stackId, String accountId, String effectiveUserId, int updateVersion);
  public String failDeleteResource(String resourceId, String stackId, String accountId, String effectiveUserId, String errorMessage, int updateVersion);
  public String getCreateWorkflowExecutionCloseStatus(String stackId);
  public String getUpdateWorkflowExecutionCloseStatus(String stackId);
  public String getStackStatus(String stackId, String accountId, int updateVersion);
  public String setStackStatus(String stackId, String accountId, String status, String statusReason, int updateVersion);
  public String cancelCreateAndMonitorWorkflows(String stackId);
  public String verifyCreateAndMonitorWorkflowsClosed(String stackId);
  public Integer getAWSCloudFormationWaitConditionTimeout(String resourceId, String stackId, String accountId, String effectiveUserId, int updateVersion);
  public String cancelOutstandingCreateResources(String stackId, String accountId, String cancelMessage, int updateVersion);
  public String cancelOutstandingUpdateResources(String stackId, String accountId, String cancelMessage, int updateVersion);
  public String initUpdateResource(String resourceId, String stackId, String accountId, String effectiveUserId, String reverseDependentResourcesJson, int updateVersion);
  public String finalizeUpdateResource(String resourceId, String stackId, String accountId, String effectiveUserId, int updateVersion);
  public Boolean performUpdateNoInterruptionStep(String stepId, String resourceId, String stackId, String accountId, String effectiveUserId, int updateVersion);
  public Boolean performUpdateSomeInterruptionStep(String stepId, String resourceId, String stackId, String accountId, String effectiveUserId, int updateVersion);
  public Boolean performUpdateWithReplacementStep(String stepId, String resourceId, String stackId, String accountId, String effectiveUserId, int updateVersion);
  public String finalizeUpdateCleanupStack(String stackId, String accountId, String statusMessage, int updateVersion);
  public String initUpdateCleanupResource(String resourceId, String stackId, String accountId, String effectiveUserId, int updateVersion);
  public String failUpdateCleanupResource(String resourceId, String stackId, String accountId, String effectiveUserId, String errorMessage, int updateVersion);
  public String finalizeUpdateCleanupResource(String resourceId, String stackId, String accountId, String effectiveUserId, int updateVersion);
}
