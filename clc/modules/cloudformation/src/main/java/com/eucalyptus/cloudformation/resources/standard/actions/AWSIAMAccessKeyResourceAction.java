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
import com.eucalyptus.auth.euare.AccessKeyMetadataType;
import com.eucalyptus.auth.euare.CreateAccessKeyResponseType;
import com.eucalyptus.auth.euare.CreateAccessKeyType;
import com.eucalyptus.auth.euare.DeleteAccessKeyResponseType;
import com.eucalyptus.auth.euare.DeleteAccessKeyType;
import com.eucalyptus.auth.euare.ListAccessKeysResponseType;
import com.eucalyptus.auth.euare.ListAccessKeysType;
import com.eucalyptus.auth.euare.ListUsersResponseType;
import com.eucalyptus.auth.euare.ListUsersType;
import com.eucalyptus.auth.euare.UpdateAccessKeyResponseType;
import com.eucalyptus.auth.euare.UpdateAccessKeyType;
import com.eucalyptus.auth.euare.UserType;
import com.eucalyptus.cloudformation.ValidationErrorException;
import com.eucalyptus.cloudformation.resources.ResourceAction;
import com.eucalyptus.cloudformation.resources.ResourceInfo;
import com.eucalyptus.cloudformation.resources.ResourceProperties;
import com.eucalyptus.cloudformation.resources.standard.info.AWSIAMAccessKeyResourceInfo;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.AWSIAMAccessKeyProperties;
import com.eucalyptus.cloudformation.template.JsonHelper;
import com.eucalyptus.cloudformation.util.MessageHelper;
import com.eucalyptus.cloudformation.workflow.CreateStackWorkflowImpl;
import com.eucalyptus.cloudformation.workflow.DeleteStackWorkflowImpl;
import com.eucalyptus.cloudformation.workflow.steps.MultiStepWithRetryCreatePromise;
import com.eucalyptus.cloudformation.workflow.steps.MultiStepWithRetryDeletePromise;
import com.eucalyptus.cloudformation.workflow.steps.Step;
import com.eucalyptus.cloudformation.workflow.steps.StepTransform;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.Topology;
import com.eucalyptus.component.id.Euare;
import com.eucalyptus.util.async.AsyncRequests;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.collect.Lists;

import java.util.List;

/**
 * Created by ethomas on 2/3/14.
 */
public class AWSIAMAccessKeyResourceAction extends ResourceAction {

  private AWSIAMAccessKeyProperties properties = new AWSIAMAccessKeyProperties();
  private AWSIAMAccessKeyResourceInfo info = new AWSIAMAccessKeyResourceInfo();

  public AWSIAMAccessKeyResourceAction() {
    for (CreateSteps createStep: CreateSteps.values()) {
      createSteps.put(createStep.name(), createStep);
    }
    for (DeleteSteps deleteStep: DeleteSteps.values()) {
      deleteSteps.put(deleteStep.name(), deleteStep);
    }

  }
  private enum CreateSteps implements Step {
    CREATE_KEY {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        AWSIAMAccessKeyResourceAction action = (AWSIAMAccessKeyResourceAction) resourceAction;
        ServiceConfiguration configuration = Topology.lookup(Euare.class);
        if (!"Active".equals(action.properties.getStatus()) && !"Inactive".equals(action.properties.getStatus())) {
          throw new ValidationErrorException("Invalid status " + action.properties.getStatus());
        }
        CreateAccessKeyType createAccessKeyType = MessageHelper.createMessage(CreateAccessKeyType.class, action.info.getEffectiveUserId());
        createAccessKeyType.setUserName(action.properties.getUserName());
        CreateAccessKeyResponseType createAccessKeyResponseType = AsyncRequests.<CreateAccessKeyType,CreateAccessKeyResponseType> sendSync(configuration, createAccessKeyType);
        // access key id = physical resource id
        action.info.setPhysicalResourceId(createAccessKeyResponseType.getCreateAccessKeyResult().getAccessKey().getAccessKeyId());
        action.info.setSecretAccessKey(JsonHelper.getStringFromJsonNode(new TextNode(createAccessKeyResponseType.getCreateAccessKeyResult().getAccessKey().getSecretAccessKey())));
        action.info.setReferenceValueJson(JsonHelper.getStringFromJsonNode(new TextNode(action.info.getPhysicalResourceId())));
        return action;
      }

      @Override
      public RetryPolicy getRetryPolicy() {
        return null;
      }
    },
    SET_STATUS {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        AWSIAMAccessKeyResourceAction action = (AWSIAMAccessKeyResourceAction) resourceAction;
        ServiceConfiguration configuration = Topology.lookup(Euare.class);
        UpdateAccessKeyType updateAccessKeyType = MessageHelper.createMessage(UpdateAccessKeyType.class, action.info.getEffectiveUserId());
        updateAccessKeyType.setUserName(action.properties.getUserName());
        updateAccessKeyType.setAccessKeyId(action.info.getPhysicalResourceId());
        updateAccessKeyType.setStatus(action.properties.getStatus());
        AsyncRequests.<UpdateAccessKeyType,UpdateAccessKeyResponseType> sendSync(configuration, updateAccessKeyType);
        return action;
      }

      @Override
      public RetryPolicy getRetryPolicy() {
        return null;
      }
    }

  }

  private enum DeleteSteps implements Step {
    DELETE_KEY {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        AWSIAMAccessKeyResourceAction action = (AWSIAMAccessKeyResourceAction) resourceAction;
        ServiceConfiguration configuration = Topology.lookup(Euare.class);
        if (action.info.getPhysicalResourceId() == null) return action;

        if (action.properties.getStatus() == null) action.properties.setStatus("Active");
        // if no user, return
        boolean seenAllUsers = false;
        boolean foundUser = false;
        String userMarker = null;
        while (!seenAllUsers && !foundUser) {
          ListUsersType listUsersType = MessageHelper.createMessage(ListUsersType.class, action.info.getEffectiveUserId());
          if (userMarker != null) {
            listUsersType.setMarker(userMarker);
          }
          ListUsersResponseType listUsersResponseType = AsyncRequests.<ListUsersType,ListUsersResponseType> sendSync(configuration, listUsersType);
          if (listUsersResponseType.getListUsersResult().getIsTruncated() == Boolean.TRUE) {
            userMarker = listUsersResponseType.getListUsersResult().getMarker();
          } else {
            seenAllUsers = true;
          }
          if (listUsersResponseType.getListUsersResult().getUsers() != null && listUsersResponseType.getListUsersResult().getUsers().getMemberList() != null) {
            for (UserType userType: listUsersResponseType.getListUsersResult().getUsers().getMemberList()) {
              if (userType.getUserName().equals(action.properties.getUserName())) {
                foundUser = true;
                break;
              }
            }
          }
        }
        if (!foundUser) return action;

        boolean seenAllAccessKeys = false;
        boolean foundAccessKey = false;
        String accessKeyMarker = null;
        while (!seenAllAccessKeys && !foundAccessKey) {
          ListAccessKeysType listAccessKeysType = MessageHelper.createMessage(ListAccessKeysType.class, action.info.getEffectiveUserId());
          listAccessKeysType.setUserName(action.properties.getUserName());
          if (accessKeyMarker != null) {
            listAccessKeysType.setMarker(accessKeyMarker);
          }
          ListAccessKeysResponseType listAccessKeysResponseType = AsyncRequests.<ListAccessKeysType,ListAccessKeysResponseType> sendSync(configuration, listAccessKeysType);
          if (listAccessKeysResponseType.getListAccessKeysResult().getIsTruncated() == Boolean.TRUE) {
            accessKeyMarker = listAccessKeysResponseType.getListAccessKeysResult().getMarker();
          } else {
            seenAllAccessKeys = true;
          }
          if (listAccessKeysResponseType.getListAccessKeysResult().getAccessKeyMetadata() != null && listAccessKeysResponseType.getListAccessKeysResult().getAccessKeyMetadata().getMemberList() != null) {
            for (AccessKeyMetadataType accessKeyMetadataType: listAccessKeysResponseType.getListAccessKeysResult().getAccessKeyMetadata().getMemberList()) {
              if (accessKeyMetadataType.getAccessKeyId().equals(action.info.getPhysicalResourceId())) {
                foundAccessKey = true;
                break;
              }
            }
          }
        }
        if (!foundAccessKey) return action;
        DeleteAccessKeyType deleteAccessKeyType = MessageHelper.createMessage(DeleteAccessKeyType.class, action.info.getEffectiveUserId());
        deleteAccessKeyType.setUserName(action.properties.getUserName());
        deleteAccessKeyType.setAccessKeyId(action.info.getPhysicalResourceId());
        AsyncRequests.<DeleteAccessKeyType,DeleteAccessKeyResponseType> sendSync(configuration, deleteAccessKeyType);
        return action;
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
    properties = (AWSIAMAccessKeyProperties) resourceProperties;
  }

  @Override
  public ResourceInfo getResourceInfo() {
    return info;
  }

  @Override
  public void setResourceInfo(ResourceInfo resourceInfo) {
    info = (AWSIAMAccessKeyResourceInfo) resourceInfo;
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


