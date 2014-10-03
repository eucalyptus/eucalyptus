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
import com.eucalyptus.auth.euare.AddUserToGroupResponseType;
import com.eucalyptus.auth.euare.AddUserToGroupType;
import com.eucalyptus.auth.euare.CreateLoginProfileResponseType;
import com.eucalyptus.auth.euare.CreateLoginProfileType;
import com.eucalyptus.auth.euare.CreateUserResponseType;
import com.eucalyptus.auth.euare.CreateUserType;
import com.eucalyptus.auth.euare.DeleteUserPolicyResponseType;
import com.eucalyptus.auth.euare.DeleteUserPolicyType;
import com.eucalyptus.auth.euare.DeleteUserResponseType;
import com.eucalyptus.auth.euare.DeleteUserType;
import com.eucalyptus.auth.euare.GroupType;
import com.eucalyptus.auth.euare.ListGroupsForUserResponseType;
import com.eucalyptus.auth.euare.ListGroupsForUserType;
import com.eucalyptus.auth.euare.ListUsersResponseType;
import com.eucalyptus.auth.euare.ListUsersType;
import com.eucalyptus.auth.euare.PutUserPolicyResponseType;
import com.eucalyptus.auth.euare.PutUserPolicyType;
import com.eucalyptus.auth.euare.RemoveUserFromGroupResponseType;
import com.eucalyptus.auth.euare.RemoveUserFromGroupType;
import com.eucalyptus.auth.euare.UserType;
import com.eucalyptus.cloudformation.resources.ResourceAction;
import com.eucalyptus.cloudformation.resources.ResourceInfo;
import com.eucalyptus.cloudformation.resources.ResourceProperties;
import com.eucalyptus.cloudformation.resources.standard.info.AWSIAMUserResourceInfo;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.AWSIAMUserProperties;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.EmbeddedIAMPolicy;
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
public class AWSIAMUserResourceAction extends ResourceAction {

  private AWSIAMUserProperties properties = new AWSIAMUserProperties();
  private AWSIAMUserResourceInfo info = new AWSIAMUserResourceInfo();

  public AWSIAMUserResourceAction() {
    for (CreateSteps createStep: CreateSteps.values()) {
      createSteps.put(createStep.name(), createStep);
    }
    for (DeleteSteps deleteStep: DeleteSteps.values()) {
      deleteSteps.put(deleteStep.name(), deleteStep);
    }
  }

  private enum CreateSteps implements Step {
    CREATE_USER {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        AWSIAMUserResourceAction action = (AWSIAMUserResourceAction) resourceAction;
        ServiceConfiguration configuration = Topology.lookup(Euare.class);
        String userName = action.getDefaultPhysicalResourceId();
        CreateUserType createUserType = MessageHelper.createMessage(CreateUserType.class, action.info.getEffectiveUserId());
        createUserType.setUserName(userName);
        createUserType.setPath(action.properties.getPath());
        CreateUserResponseType createUserResponseType = AsyncRequests.<CreateUserType,CreateUserResponseType> sendSync(configuration, createUserType);
        String arn = createUserResponseType.getCreateUserResult().getUser().getArn();
        action.info.setPhysicalResourceId(userName);
        action.info.setArn(JsonHelper.getStringFromJsonNode(new TextNode(arn)));
        action.info.setReferenceValueJson(JsonHelper.getStringFromJsonNode(new TextNode(action.info.getPhysicalResourceId())));
        return action;
      }

      @Override
      public RetryPolicy getRetryPolicy() {
        return null;
      }
    },
    ADD_LOGIN_PROFILE {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        AWSIAMUserResourceAction action = (AWSIAMUserResourceAction) resourceAction;
        ServiceConfiguration configuration = Topology.lookup(Euare.class);
        if (action.properties.getLoginProfile() != null) {
          CreateLoginProfileType createLoginProfileType = MessageHelper.createMessage(CreateLoginProfileType.class, action.info.getEffectiveUserId());
          createLoginProfileType.setPassword(action.properties.getLoginProfile().getPassword());
          createLoginProfileType.setUserName(action.info.getPhysicalResourceId());
          AsyncRequests.<CreateLoginProfileType,CreateLoginProfileResponseType> sendSync(configuration, createLoginProfileType);
        }
        return action;
      }

      @Override
      public RetryPolicy getRetryPolicy() {
        return null;
      }
    },
    ADD_POLICIES {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        AWSIAMUserResourceAction action = (AWSIAMUserResourceAction) resourceAction;
        ServiceConfiguration configuration = Topology.lookup(Euare.class);
        if (action.properties.getPolicies() != null) {
          for (EmbeddedIAMPolicy policy: action.properties.getPolicies()) {
            PutUserPolicyType putUserPolicyType = MessageHelper.createMessage(PutUserPolicyType.class, action.info.getEffectiveUserId());
            putUserPolicyType.setUserName(action.info.getPhysicalResourceId());
            putUserPolicyType.setPolicyName(policy.getPolicyName());
            putUserPolicyType.setPolicyDocument(policy.getPolicyDocument().toString());
            AsyncRequests.<PutUserPolicyType,PutUserPolicyResponseType> sendSync(configuration, putUserPolicyType);
          }
        }
        return action;
      }

      @Override
      public RetryPolicy getRetryPolicy() {
        return null;
      }
    },
    ADD_GROUPS {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        AWSIAMUserResourceAction action = (AWSIAMUserResourceAction) resourceAction;
        ServiceConfiguration configuration = Topology.lookup(Euare.class);
        if (action.properties.getGroups() != null) {
          for (String groupName: action.properties.getGroups()) {
            AddUserToGroupType addUserToGroupType = MessageHelper.createMessage(AddUserToGroupType.class, action.info.getEffectiveUserId());
            addUserToGroupType.setGroupName(groupName);
            addUserToGroupType.setUserName(action.info.getPhysicalResourceId());
            AsyncRequests.<AddUserToGroupType,AddUserToGroupResponseType> sendSync(configuration, addUserToGroupType);
          }
        }
        return action;
      }

      @Override
      public RetryPolicy getRetryPolicy() {
        return null;
      }
    }
  }

  private enum DeleteSteps implements Step {
    DELETE_USER {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        AWSIAMUserResourceAction action = (AWSIAMUserResourceAction) resourceAction;
        ServiceConfiguration configuration = Topology.lookup(Euare.class);
        if (action.info.getPhysicalResourceId() == null) return action;

        // if no user, bye...
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
            for (UserType UserType: listUsersResponseType.getListUsersResult().getUsers().getMemberList()) {
              if (UserType.getUserName().equals(action.info.getPhysicalResourceId())) {
                foundUser = true;
                break;
              }
            }
          }
        }
        if (!foundUser) return action;

        // remove user from groups we added (if there)
        if (action.properties.getGroups() != null) {
          boolean seenAllGroups = false;
          List<String> currentGroups = Lists.newArrayList();
          String groupMarker = null;
          while (!seenAllGroups) {
            ListGroupsForUserType listGroupsForUserType = MessageHelper.createMessage(ListGroupsForUserType.class, action.info.getEffectiveUserId());
            listGroupsForUserType.setUserName(action.info.getPhysicalResourceId());
            if (groupMarker != null) {
              listGroupsForUserType.setMarker(groupMarker);
            }
            ListGroupsForUserResponseType listGroupsForUserResponseType = AsyncRequests.<ListGroupsForUserType,ListGroupsForUserResponseType> sendSync(configuration, listGroupsForUserType);
            if (listGroupsForUserResponseType.getListGroupsForUserResult().getIsTruncated() == Boolean.TRUE) {
              groupMarker = listGroupsForUserResponseType.getListGroupsForUserResult().getMarker();
            } else {
              seenAllGroups = true;
            }
            if (listGroupsForUserResponseType.getListGroupsForUserResult().getGroups() != null && listGroupsForUserResponseType.getListGroupsForUserResult().getGroups().getMemberList() != null) {
              for (GroupType groupType: listGroupsForUserResponseType.getListGroupsForUserResult().getGroups().getMemberList()) {
                currentGroups.add(groupType.getGroupName());
              }
            }
          }
          for (String groupName: action.properties.getGroups()) {
            if (currentGroups.contains(groupName)) {
              RemoveUserFromGroupType removeUserFromGroupType = MessageHelper.createMessage(RemoveUserFromGroupType.class, action.info.getEffectiveUserId());
              removeUserFromGroupType.setGroupName(groupName);
              removeUserFromGroupType.setUserName(action.info.getPhysicalResourceId());
              AsyncRequests.<RemoveUserFromGroupType,RemoveUserFromGroupResponseType> sendSync(configuration, removeUserFromGroupType);
            }
          }
          // Note: the above will not add externally added groups, but this is by design...
        }

        // remove all policies added by us.  (Note: this could cause issues if an admin added some, but we delete what we create)
        // Note: deleting a non-existing policy doesn't do anything so we just delete them all...
        if (action.properties.getPolicies() != null) {
          for (EmbeddedIAMPolicy policy: action.properties.getPolicies()) {
            DeleteUserPolicyType deleteUserPolicyType = MessageHelper.createMessage(DeleteUserPolicyType.class, action.info.getEffectiveUserId());
            deleteUserPolicyType.setUserName(action.info.getPhysicalResourceId());
            deleteUserPolicyType.setPolicyName(policy.getPolicyName());
            AsyncRequests.<DeleteUserPolicyType,DeleteUserPolicyResponseType> sendSync(configuration, deleteUserPolicyType);
          }
        }
        DeleteUserType deleteUserType = MessageHelper.createMessage(DeleteUserType.class, action.info.getEffectiveUserId());
        deleteUserType.setUserName(action.info.getPhysicalResourceId());
        AsyncRequests.<DeleteUserType,DeleteUserResponseType> sendSync(configuration, deleteUserType);
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
    properties = (AWSIAMUserProperties) resourceProperties;
  }

  @Override
  public ResourceInfo getResourceInfo() {
    return info;
  }

  @Override
  public void setResourceInfo(ResourceInfo resourceInfo) {
    info = (AWSIAMUserResourceInfo) resourceInfo;
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


