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
import com.eucalyptus.cloudformation.workflow.create.CreateStep;
import com.eucalyptus.cloudformation.workflow.delete.DeleteStep;
import com.eucalyptus.crypto.Crypto;
import com.google.common.collect.Maps;

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

  protected Map<String, CreateStep> createSteps = Maps.newLinkedHashMap();

  public final CreateStep getCreateStep(String stepId) {
    return createSteps.get(stepId);
  }

  protected Map<String, DeleteStep> deleteSteps = Maps.newLinkedHashMap();

  public final DeleteStep getDeleteStep(String stepId) {
    return deleteSteps.get(stepId);
  }

  public abstract Promise<String> getCreatePromise(CreateStackWorkflowImpl createStackWorkflow, String resourceId, String stackId, String accountId, String effectiveUserId);

  public abstract Promise<String> getDeletePromise(DeleteStackWorkflowImpl deleteStackWorkflow, String resourceId, String stackId, String accountId, String effectiveUserId);

}
