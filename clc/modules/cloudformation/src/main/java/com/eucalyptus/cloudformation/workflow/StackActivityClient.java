/*************************************************************************
 * Copyright 2009-2015 Eucalyptus Systems, Inc.
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
@Activities(version="1.0")
public interface StackActivityClient {
  @Activity(name = "StackActivity.initCreateResource")
  Promise<String> initCreateResource(String resourceId, String stackId, String accountId, String effectiveUserId, String reverseDependentResourcesJson);

  @Activity(name = "StackActivity.validateAWSParameterTypes")
  Promise<String> validateAWSParameterTypes(String stackId, String accountId, String effectiveUserId);

  @Activity(name = "StackActivity.performCreateStep")
  Promise<Boolean> performCreateStep(String stepId, String resourceId, String stackId, String accountId, String effectiveUserId);

  @Activity(name = "StackActivity.performDeleteStep")
  Promise<Boolean> performDeleteStep(String stepId, String resourceId, String stackId, String accountId, String effectiveUserId);

  @Activity(name = "StackActivity.performUpdateCleanupStep")
  Promise<Boolean> performUpdateCleanupStep(String stepId, String resourceId, String stackId, String accountId, String effectiveUserId);

  @Activity(name = "StackActivity.determineCreateResourceFailures")
  Promise<String> determineCreateResourceFailures(String stackId, String accountId);

  @Activity(name = "StackActivity.determineDeleteResourceFailures")
  Promise<String> determineDeleteResourceFailures(String stackId, String accountId);

  @Activity(name = "StackActivity.initDeleteResource")
  Promise<String> initDeleteResource(String resourceId, String stackId, String accountId, String effectiveUserId);

  @Activity(name = "StackActivity.createGlobalStackEvent")
  Promise<String> createGlobalStackEvent(String stackId, String accountId, String resourceStatus, String resourceStatusReason);

  @Activity(name = "StackActivity.finalizeCreateStack")
  Promise<String> finalizeCreateStack(String stackId, String accountId, String effectiveUserId);

  @Activity(name = "StackActivity.deleteAllStackRecords")
  Promise<String> deleteAllStackRecords(String stackId, String accountId);

  @Activity(name = "StackActivity.getResourceType")
  Promise<String> getResourceType(String stackId, String accountId, String resourceId);

  @Activity(name = "StackActivity.getResourceTypeForUpdate")
  Promise<String> getResourceTypeForUpdate(String stackId, String accountId, String resourceId);


  @Activity(name = "StackActivity.finalizeCreateResource")
  Promise<String> finalizeCreateResource(String resourceId, String stackId, String accountId, String effectiveUserId);

  @Activity(name = "StackActivity.finalizeDeleteResource")
  Promise<String> finalizeDeleteResource(String resourceId, String stackId, String accountId, String effectiveUserId);

  @Activity(name = "StackActivity.failDeleteResource")
  Promise<String> failDeleteResource(String resourceId, String stackId, String accountId, String effectiveUserId, String errorMessage);

  @Activity(name = "StackActivity.getCreateWorkflowExecutionCloseStatus")
  Promise<String> getCreateWorkflowExecutionCloseStatus(String stackId);

  @Activity(name = "StackActivity.getUpdateWorkflowExecutionCloseStatus")
  Promise<String> getUpdateWorkflowExecutionCloseStatus(String stackId);

  @Activity(name = "StackActivity.getStackStatus")
  Promise<String> getStackStatus(String stackId, String accountId);

  @Activity(name = "StackActivity.setStackStatus")
  Promise<String> setStackStatus(String stackId, String accountId, String status, String statusReason);

  @Activity(name = "StackActivity.cancelCreateAndMonitorWorkflows")
  Promise<String> cancelCreateAndMonitorWorkflows(String stackId);

  @Activity(name = "StackActivity.verifyCreateAndMonitorWorkflowsClosed")
  Promise<String> verifyCreateAndMonitorWorkflowsClosed(String stackId);

  @Activity(name = "StackActivity.getAWSCloudFormationWaitConditionTimeout")
  Promise<Integer> getAWSCloudFormationWaitConditionTimeout(String resourceId, String stackId, String accountId, String effectiveUserId);

  @Activity(name = "StackActivity.cancelOutstandingCreateResources")
  Promise<String> cancelOutstandingCreateResources(String stackId, String accountId, String cancelMessage);

  @Activity(name = "StackActivity.cancelOutstandingUpdateResources")
  Promise<String> cancelOutstandingUpdateResources(String stackId, String accountId, String cancelMessage);

  @Activity(name = "StackActivity.initUpdateResource")
  Promise<String> initUpdateResource(String resourceId, String stackId, String accountId, String effectiveUserId, String reverseDependentResourcesJson);

  @Activity(name = "StackActivity.finalizeUpdateResource")
  Promise<String> finalizeUpdateResource(String resourceId, String stackId, String accountId, String effectiveUserId);

  @Activity(name = "StackActivity.performUpdateNoInterruptionStep")
  Promise<Boolean> performUpdateNoInterruptionStep(String stepId, String resourceId, String stackId, String accountId, String effectiveUserId);

  @Activity(name = "StackActivity.performUpdateSomeInterruptionStep")
  Promise<Boolean> performUpdateSomeInterruptionStep(String stepId, String resourceId, String stackId, String accountId, String effectiveUserId);

  @Activity(name = "StackActivity.performUpdateWithReplacementStep")
  Promise<Boolean> performUpdateWithReplacementStep(String stepId, String resourceId, String stackId, String accountId, String effectiveUserId);

  @Activity(name = "StackActivity.finalizeUpdateCleanupStack")
  Promise<String> finalizeUpdateCleanupStack(String stackId, String accountId, String statusMessage);

  @Activity(name = "StackActivity.initUpdateCleanupResource")
  Promise<String> initUpdateCleanupResource(String resourceId, String stackId, String accountId, String effectiveUserId);

  @Activity(name = "StackActivity.failUpdateCleanupResource")
  Promise<String> failUpdateCleanupResource(String resourceId, String stackId, String accountId, String effectiveUserId, String errorMessage);

  @Activity(name = "StackActivity.finalizeUpdateCleanupResource")
  Promise<String> finalizeUpdateCleanupResource(String resourceId, String stackId, String accountId, String effectiveUserId);

  @Activity(name = "StackActivity.removeUpdateCleanupResourceIfAppropriateFromStack")
  Promise<String> removeUpdateCleanupResourceIfAppropriateFromStack(String resourceId, String stackId, String accountId);
}