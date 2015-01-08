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

package com.eucalyptus.cloudformation.resources;

import com.amazonaws.services.simpleworkflow.flow.core.Promise;
import com.eucalyptus.cloudformation.entity.StackEntity;
import com.eucalyptus.cloudformation.workflow.CreateStackWorkflowImpl;
import com.eucalyptus.cloudformation.workflow.DeleteStackWorkflowImpl;
import com.eucalyptus.cloudformation.workflow.StackActivity;
import com.eucalyptus.cloudformation.workflow.steps.Step;
import com.google.common.collect.Maps;
import com.netflix.glisten.WorkflowOperations;

import java.util.Map;

public abstract class ResourceAction {
  public abstract ResourceProperties getResourceProperties();
  public abstract void setResourceProperties(ResourceProperties resourceProperties);
  public abstract ResourceInfo getResourceInfo();
  public abstract void setResourceInfo(ResourceInfo resourceInfo);
  protected StackEntity stackEntity;

  public StackEntity getStackEntity() {
    return stackEntity;
  }

  public void setStackEntity(StackEntity stackEntity) {
    this.stackEntity = stackEntity;
  }

  public final String getDefaultPhysicalResourceId(int maxLength) {
    String stackName = (getStackEntity() != null && getStackEntity().getStackName() != null) ?
      getStackEntity().getStackName() : "UNKNOWN";
    String logicalResourceId = (getResourceInfo() != null && getResourceInfo().getLogicalResourceId() != null) ?
      getResourceInfo().getLogicalResourceId() : "UNKNOWN";
    return ResourceActionHelper.getDefaultPhysicalResourceId(stackName, logicalResourceId, maxLength);
  }

  public final String getDefaultPhysicalResourceId() {
    return getDefaultPhysicalResourceId(Integer.MAX_VALUE);
  }

  protected Map<String, Step> createSteps = Maps.newLinkedHashMap();

  public final Step getCreateStep(String stepId) {
    return createSteps.get(stepId);
  }

  protected Map<String, Step> deleteSteps = Maps.newLinkedHashMap();

  public final Step getDeleteStep(String stepId) {
    return deleteSteps.get(stepId);
  }

  public abstract Promise<String> getCreatePromise(WorkflowOperations<StackActivity> workflowOperations, String resourceId, String stackId, String accountId, String effectiveUserId);

  public abstract Promise<String> getDeletePromise(WorkflowOperations<StackActivity> workflowOperations, String resourceId, String stackId, String accountId, String effectiveUserId);

}
