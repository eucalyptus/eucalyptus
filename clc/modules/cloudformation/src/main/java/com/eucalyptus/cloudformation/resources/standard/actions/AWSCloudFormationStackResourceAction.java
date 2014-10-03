/*************************************************************************
 * Copyright 2009-2013 Eucalyptus Systems, Inc.
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
package com.eucalyptus.cloudformation.resources.standard.actions;


import com.amazonaws.services.simpleworkflow.flow.core.Promise;
import com.amazonaws.services.simpleworkflow.flow.interceptors.RetryPolicy;
import com.eucalyptus.cloudformation.resources.ResourceAction;
import com.eucalyptus.cloudformation.resources.ResourceInfo;
import com.eucalyptus.cloudformation.resources.ResourceProperties;
import com.eucalyptus.cloudformation.resources.standard.info.AWSCloudFormationStackResourceInfo;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.AWSCloudFormationStackProperties;
import com.eucalyptus.cloudformation.workflow.CreateStackWorkflowImpl;
import com.eucalyptus.cloudformation.workflow.DeleteStackWorkflowImpl;
import com.eucalyptus.cloudformation.workflow.steps.MultiStepWithRetryCreatePromise;
import com.eucalyptus.cloudformation.workflow.steps.MultiStepWithRetryDeletePromise;
import com.eucalyptus.cloudformation.workflow.steps.Step;
import com.eucalyptus.cloudformation.workflow.steps.StepTransform;
import com.google.common.collect.Lists;

import java.util.List;

/**
 * Created by ethomas on 2/3/14.
 */
public class AWSCloudFormationStackResourceAction extends ResourceAction {

  private AWSCloudFormationStackProperties properties = new AWSCloudFormationStackProperties();
  private AWSCloudFormationStackResourceInfo info = new AWSCloudFormationStackResourceInfo();

  public AWSCloudFormationStackResourceAction() {
    for (CreateSteps createStep: CreateSteps.values()) {
      createSteps.put(createStep.name(), createStep);
    }
    for (DeleteSteps deleteStep: DeleteSteps.values()) {
      deleteSteps.put(deleteStep.name(), deleteStep);
    }

  }

  private enum CreateSteps implements Step {
    NOT_SUPPORTED {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        throw new UnsupportedOperationException();
      }
      @Override
      public RetryPolicy getRetryPolicy() {
        return null;
      }
    }
  }

  private enum DeleteSteps implements Step {
    DO_NOTHING {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        return resourceAction; // delete should do nothing because create can't succeed.
      }
      @Override
      public RetryPolicy getRetryPolicy() {
        return null;
      }
    }
  }


  @Override
  public ResourceProperties getResourceProperties() {
    return properties;
  }

  @Override
  public void setResourceProperties(ResourceProperties resourceProperties) {
    properties = (AWSCloudFormationStackProperties) resourceProperties;
  }

  @Override
  public ResourceInfo getResourceInfo() {
    return info;
  }

  @Override
  public void setResourceInfo(ResourceInfo resourceInfo) {
    info = (AWSCloudFormationStackResourceInfo) resourceInfo;
  }

  @Override
  public Promise<String> getCreatePromise(CreateStackWorkflowImpl createStackWorkflow, String resourceId, String stackId, String accountId, String effectiveUserId) {
    List<String> stepIds = Lists.transform(Lists.newArrayList(CreateSteps.values()), StepTransform.INSTANCE);
    return new MultiStepWithRetryCreatePromise(createStackWorkflow, stepIds, this).getCreatePromise(resourceId, stackId, accountId, effectiveUserId);
  }

  @Override
  public Promise<String> getDeletePromise(DeleteStackWorkflowImpl deleteStackWorkflow, String resourceId, String stackId, String accountId, String effectiveUserId) {
    List<String> stepIds = Lists.transform(Lists.newArrayList(DeleteSteps.values()), StepTransform.INSTANCE);
    return new MultiStepWithRetryDeletePromise(deleteStackWorkflow, stepIds, this).getDeletePromise(resourceId, stackId, accountId, effectiveUserId);
  }

}


