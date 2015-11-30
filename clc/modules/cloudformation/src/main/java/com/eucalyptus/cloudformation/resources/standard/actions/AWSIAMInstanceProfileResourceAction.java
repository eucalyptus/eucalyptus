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
import com.eucalyptus.auth.euare.AddRoleToInstanceProfileResponseType;
import com.eucalyptus.auth.euare.AddRoleToInstanceProfileType;
import com.eucalyptus.auth.euare.CreateInstanceProfileResponseType;
import com.eucalyptus.auth.euare.CreateInstanceProfileType;
import com.eucalyptus.auth.euare.DeleteInstanceProfileResponseType;
import com.eucalyptus.auth.euare.DeleteInstanceProfileType;
import com.eucalyptus.auth.euare.InstanceProfileType;
import com.eucalyptus.auth.euare.ListInstanceProfilesResponseType;
import com.eucalyptus.auth.euare.ListInstanceProfilesType;
import com.eucalyptus.cloudformation.resources.ResourceAction;
import com.eucalyptus.cloudformation.resources.ResourceInfo;
import com.eucalyptus.cloudformation.resources.ResourceProperties;
import com.eucalyptus.cloudformation.resources.standard.info.AWSIAMInstanceProfileResourceInfo;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.AWSIAMInstanceProfileProperties;
import com.eucalyptus.cloudformation.template.JsonHelper;
import com.eucalyptus.cloudformation.util.MessageHelper;
import com.eucalyptus.cloudformation.workflow.StackActivityClient;
import com.eucalyptus.cloudformation.workflow.steps.CreateMultiStepPromise;
import com.eucalyptus.cloudformation.workflow.steps.DeleteMultiStepPromise;
import com.eucalyptus.cloudformation.workflow.steps.Step;
import com.eucalyptus.cloudformation.workflow.steps.StepTransform;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.Topology;
import com.eucalyptus.component.id.Euare;
import com.eucalyptus.util.async.AsyncRequests;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.collect.Lists;
import com.netflix.glisten.WorkflowOperations;

import java.util.List;
import javax.annotation.Nullable;

/**
 * Created by ethomas on 2/3/14.
 */
public class AWSIAMInstanceProfileResourceAction extends ResourceAction {

  private AWSIAMInstanceProfileProperties properties = new AWSIAMInstanceProfileProperties();
  private AWSIAMInstanceProfileResourceInfo info = new AWSIAMInstanceProfileResourceInfo();

  public AWSIAMInstanceProfileResourceAction() {
    for (CreateSteps createStep: CreateSteps.values()) {
      createSteps.put(createStep.name(), createStep);
    }
    for (DeleteSteps deleteStep: DeleteSteps.values()) {
      deleteSteps.put(deleteStep.name(), deleteStep);
    }
  }

  private enum CreateSteps implements Step {
    CREATE_INSTANCE_PROFILE {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        AWSIAMInstanceProfileResourceAction action = (AWSIAMInstanceProfileResourceAction) resourceAction;
        ServiceConfiguration configuration = Topology.lookup(Euare.class);
        String instanceProfileName = action.getDefaultPhysicalResourceId();
        CreateInstanceProfileType createInstanceProfileType = MessageHelper.createMessage(CreateInstanceProfileType.class, action.info.getEffectiveUserId());
        createInstanceProfileType.setPath(action.properties.getPath());
        createInstanceProfileType.setInstanceProfileName(instanceProfileName);
        CreateInstanceProfileResponseType createInstanceProfileResponseType = AsyncRequests.<CreateInstanceProfileType,CreateInstanceProfileResponseType> sendSync(configuration, createInstanceProfileType);
        String arn = createInstanceProfileResponseType.getCreateInstanceProfileResult().getInstanceProfile().getArn();
        action.info.setPhysicalResourceId(instanceProfileName);
        action.info.setArn(JsonHelper.getStringFromJsonNode(new TextNode(arn)));
        action.info.setReferenceValueJson(JsonHelper.getStringFromJsonNode(new TextNode(action.info.getPhysicalResourceId())));
        return action;
      }
    },
    ADD_ROLES {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        AWSIAMInstanceProfileResourceAction action = (AWSIAMInstanceProfileResourceAction) resourceAction;
        ServiceConfiguration configuration = Topology.lookup(Euare.class);
        if (action.properties.getRoles() != null) {
          for (String roleName: action.properties.getRoles()) {
            AddRoleToInstanceProfileType addRoleToInstanceProfileType = MessageHelper.createMessage(AddRoleToInstanceProfileType.class, action.info.getEffectiveUserId());
            addRoleToInstanceProfileType.setInstanceProfileName(action.info.getPhysicalResourceId());
            addRoleToInstanceProfileType.setRoleName(roleName);
            AsyncRequests.<AddRoleToInstanceProfileType,AddRoleToInstanceProfileResponseType> sendSync(configuration, addRoleToInstanceProfileType);
          }
        }
        return action;
      }
    };

    @Nullable
    @Override
    public Integer getTimeout() {
      return null;
    }
  }

  private enum DeleteSteps implements Step {
    DELETE_INSTANCE_PROFILE {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        AWSIAMInstanceProfileResourceAction action = (AWSIAMInstanceProfileResourceAction) resourceAction;
        ServiceConfiguration configuration = Topology.lookup(Euare.class);
        if (action.info.getPhysicalResourceId() == null) return action;

        // if no instance profile, bye...
        boolean seenAllInstanceProfiles = false;
        boolean foundInstanceProfile = false;
        String instanceProfileMarker = null;
        while (!seenAllInstanceProfiles && !foundInstanceProfile) {
          ListInstanceProfilesType listInstanceProfilesType = MessageHelper.createMessage(ListInstanceProfilesType.class, action.info.getEffectiveUserId());
          if (instanceProfileMarker != null) {
            listInstanceProfilesType.setMarker(instanceProfileMarker);
          }
          ListInstanceProfilesResponseType listInstanceProfilesResponseType = AsyncRequests.<ListInstanceProfilesType,ListInstanceProfilesResponseType> sendSync(configuration, listInstanceProfilesType);
          if (listInstanceProfilesResponseType.getListInstanceProfilesResult().getIsTruncated() == Boolean.TRUE) {
            instanceProfileMarker = listInstanceProfilesResponseType.getListInstanceProfilesResult().getMarker();
          } else {
            seenAllInstanceProfiles = true;
          }
          if (listInstanceProfilesResponseType.getListInstanceProfilesResult().getInstanceProfiles() != null && listInstanceProfilesResponseType.getListInstanceProfilesResult().getInstanceProfiles().getMember() != null) {
            for (InstanceProfileType instanceProfileType: listInstanceProfilesResponseType.getListInstanceProfilesResult().getInstanceProfiles().getMember()) {
              if (instanceProfileType.getInstanceProfileName().equals(action.info.getPhysicalResourceId())) {
                foundInstanceProfile = true;
                break;
              }
            }
          }
        }
        // we can delete the instance profile without detaching the role

        if (!foundInstanceProfile) return action;

        DeleteInstanceProfileType deleteInstanceProfileType = MessageHelper.createMessage(DeleteInstanceProfileType.class, action.info.getEffectiveUserId());
        deleteInstanceProfileType.setInstanceProfileName(action.info.getPhysicalResourceId());
        AsyncRequests.<DeleteInstanceProfileType,DeleteInstanceProfileResponseType> sendSync(configuration, deleteInstanceProfileType);
        return action;
      }
    };

    @Nullable
    @Override
    public Integer getTimeout() {
      return null;
    }
  }

  @Override
  public ResourceProperties getResourceProperties() {
    return properties;
  }

  @Override
  public void setResourceProperties(ResourceProperties resourceProperties) {
    properties = (AWSIAMInstanceProfileProperties) resourceProperties;
  }

  @Override
  public ResourceInfo getResourceInfo() {
    return info;
  }

  @Override
  public void setResourceInfo(ResourceInfo resourceInfo) {
    info = (AWSIAMInstanceProfileResourceInfo) resourceInfo;
  }

  @Override
  public Promise<String> getCreatePromise(WorkflowOperations<StackActivityClient> workflowOperations, String resourceId, String stackId, String accountId, String effectiveUserId) {
    List<String> stepIds = Lists.transform(Lists.newArrayList(CreateSteps.values()), StepTransform.INSTANCE);
    return new CreateMultiStepPromise(workflowOperations, stepIds, this).getCreatePromise(resourceId, stackId, accountId, effectiveUserId);
  }

  @Override
  public Promise<String> getDeletePromise(WorkflowOperations<StackActivityClient> workflowOperations, String resourceId, String stackId, String accountId, String effectiveUserId) {
    List<String> stepIds = Lists.transform(Lists.newArrayList(DeleteSteps.values()), StepTransform.INSTANCE);
    return new DeleteMultiStepPromise(workflowOperations, stepIds, this).getDeletePromise(resourceId, stackId, accountId, effectiveUserId);
  }

}


