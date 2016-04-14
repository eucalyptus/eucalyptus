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
import com.eucalyptus.cloudformation.entity.VersionedStackEntity;
import com.eucalyptus.cloudformation.workflow.StackActivityClient;
import com.eucalyptus.cloudformation.workflow.updateinfo.UpdateType;
import com.eucalyptus.cloudformation.workflow.updateinfo.UpdateTypeAndDirection;
import com.netflix.glisten.WorkflowOperations;

public abstract class ResourceAction {
  public abstract ResourceProperties getResourceProperties();

  public abstract void setResourceProperties(ResourceProperties resourceProperties);

  public abstract ResourceInfo getResourceInfo();

  public abstract void setResourceInfo(ResourceInfo resourceInfo);

  protected VersionedStackEntity stackEntity;

  public VersionedStackEntity getStackEntity() {
    return stackEntity;
  }

  public void setStackEntity(VersionedStackEntity stackEntity) {
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

  public abstract Promise<String> getCreatePromise(WorkflowOperations<StackActivityClient> workflowOperations, String resourceId, String stackId, String accountId, String effectiveUserId, int createdResourceVersion);

  public abstract Promise<String> getDeletePromise(WorkflowOperations<StackActivityClient> workflowOperations, String resourceId, String stackId, String accountId, String effectiveUserId, int updatedResourceVersion);

  public abstract Promise<String> getUpdateCleanupPromise(WorkflowOperations<StackActivityClient> workflowOperations, String resourceId, String stackId, String accountId, String effectiveUserId, int updatedResourceVersion);
  public abstract Promise<String> getUpdateRollbackCleanupPromise(WorkflowOperations<StackActivityClient> workflowOperations, String resourceId, String stackId, String accountId, String effectiveUserId, int rolledBackResourceVersion);

  public void refreshAttributes() throws Exception {
    return; // Most resources will not support this action
  }
  public abstract UpdateType getUpdateType(ResourceAction resourceAction, boolean stackTagsChanged) throws Exception;

  public boolean mustCheckUpdateTypeEvenIfNoPropertiesChanged() { // there are a couple of cases, specifically EIP and EIPAssociation if instance has been 'updated'
    return false;
  }

  public abstract Promise<String> getUpdatePromise(UpdateTypeAndDirection updateTypeAndDirection, WorkflowOperations<StackActivityClient> workflowOperations, String resourceId, String stackId, String accountId, String effectiveUserId, int updatedResourceVersion);

}

