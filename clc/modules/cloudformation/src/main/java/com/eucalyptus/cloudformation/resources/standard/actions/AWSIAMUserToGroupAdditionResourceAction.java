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
import com.eucalyptus.auth.euare.GetGroupResponseType;
import com.eucalyptus.auth.euare.GetGroupType;
import com.eucalyptus.auth.euare.GroupType;
import com.eucalyptus.auth.euare.ListGroupsResponseType;
import com.eucalyptus.auth.euare.ListGroupsType;
import com.eucalyptus.auth.euare.ListUsersResponseType;
import com.eucalyptus.auth.euare.ListUsersType;
import com.eucalyptus.auth.euare.RemoveUserFromGroupResponseType;
import com.eucalyptus.auth.euare.RemoveUserFromGroupType;
import com.eucalyptus.auth.euare.UserType;
import com.eucalyptus.cloudformation.ValidationErrorException;
import com.eucalyptus.cloudformation.resources.ResourceAction;
import com.eucalyptus.cloudformation.resources.ResourceInfo;
import com.eucalyptus.cloudformation.resources.ResourceProperties;
import com.eucalyptus.cloudformation.resources.standard.info.AWSIAMUserToGroupAdditionResourceInfo;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.AWSIAMUserToGroupAdditionProperties;
import com.eucalyptus.cloudformation.template.JsonHelper;
import com.eucalyptus.cloudformation.util.MessageHelper;
import com.eucalyptus.cloudformation.workflow.StackActivity;
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
import com.google.common.collect.Sets;
import com.netflix.glisten.WorkflowOperations;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by ethomas on 2/3/14.
 */
public class AWSIAMUserToGroupAdditionResourceAction extends ResourceAction {

  private AWSIAMUserToGroupAdditionProperties properties = new AWSIAMUserToGroupAdditionProperties();
  private AWSIAMUserToGroupAdditionResourceInfo info = new AWSIAMUserToGroupAdditionResourceInfo();

  public AWSIAMUserToGroupAdditionResourceAction() {
    for (CreateSteps createStep: CreateSteps.values()) {
      createSteps.put(createStep.name(), createStep);
    }
    for (DeleteSteps deleteStep: DeleteSteps.values()) {
      deleteSteps.put(deleteStep.name(), deleteStep);
    }
  }

  private enum CreateSteps implements Step {
    ADD_USER_TO_GROUP {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        AWSIAMUserToGroupAdditionResourceAction action = (AWSIAMUserToGroupAdditionResourceAction) resourceAction;
        ServiceConfiguration configuration = Topology.lookup(Euare.class);
        // make sure group exists
        boolean seenAllGroups = false;
        boolean foundGroup = false;
        String groupMarker = null;
        while (!seenAllGroups && !foundGroup) {
          ListGroupsType listGroupsType = MessageHelper.createMessage(ListGroupsType.class, action.info.getEffectiveUserId());
          if (groupMarker != null) {
            listGroupsType.setMarker(groupMarker);
          }
          ListGroupsResponseType listGroupsResponseType = AsyncRequests.<ListGroupsType,ListGroupsResponseType> sendSync(configuration, listGroupsType);
          if (listGroupsResponseType.getListGroupsResult().getIsTruncated() == Boolean.TRUE) {
            groupMarker = listGroupsResponseType.getListGroupsResult().getMarker();
          } else {
            seenAllGroups = true;
          }
          if (listGroupsResponseType.getListGroupsResult().getGroups() != null && listGroupsResponseType.getListGroupsResult().getGroups().getMemberList() != null) {
            for (GroupType groupType: listGroupsResponseType.getListGroupsResult().getGroups().getMemberList()) {
              if (groupType.getGroupName().equals(action.properties.getGroupName())) {
                foundGroup = true;
                break;
              }
            }
          }
        }
        if (!foundGroup) throw new ValidationErrorException("No such group " + action.properties.getGroupName());

        boolean seenAllUsers = false;
        List<String> currentUsers = Lists.newArrayList();
        String userMarker = null;
        while (!seenAllUsers) {
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
              currentUsers.add(userType.getUserName());
            }
          }
        }
        List<String> nonexistantUsers = Lists.newArrayList();
        for (String user: action.properties.getUsers()) {
          if (!currentUsers.contains(user)) {
            nonexistantUsers.add(user);
          }
        }
        if (!nonexistantUsers.isEmpty()) {
          throw new ValidationErrorException("No such user(s) " + nonexistantUsers.toString());
        }
        for (String userName: action.properties.getUsers()) {
          AddUserToGroupType addUserToGroupType = MessageHelper.createMessage(AddUserToGroupType.class, action.info.getEffectiveUserId());
          addUserToGroupType.setGroupName(action.properties.getGroupName());
          addUserToGroupType.setUserName(userName);
          AsyncRequests.<AddUserToGroupType,AddUserToGroupResponseType> sendSync(configuration, addUserToGroupType);
        }
        action.info.setPhysicalResourceId(action.getDefaultPhysicalResourceId());
        action.info.setReferenceValueJson(JsonHelper.getStringFromJsonNode(new TextNode(action.info.getPhysicalResourceId())));
        return action;
      }

      @Override
      public RetryPolicy getRetryPolicy() {
        return null;
      }
    }
  }

  private enum DeleteSteps implements Step {
    REMOVE_USER_FROM_GROUP {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        AWSIAMUserToGroupAdditionResourceAction action = (AWSIAMUserToGroupAdditionResourceAction) resourceAction;
        ServiceConfiguration configuration = Topology.lookup(Euare.class);
        if (action.info.getPhysicalResourceId() == null) return action;

        // if no group, bye...
        boolean seenAllGroups = false;
        boolean foundGroup = false;
        String groupMarker = null;
        while (!seenAllGroups && !foundGroup) {
          ListGroupsType listGroupsType = MessageHelper.createMessage(ListGroupsType.class, action.info.getEffectiveUserId());
          if (groupMarker != null) {
            listGroupsType.setMarker(groupMarker);
          }
          ListGroupsResponseType listGroupsResponseType = AsyncRequests.<ListGroupsType,ListGroupsResponseType> sendSync(configuration, listGroupsType);
          if (listGroupsResponseType.getListGroupsResult().getIsTruncated() == Boolean.TRUE) {
            groupMarker = listGroupsResponseType.getListGroupsResult().getMarker();
          } else {
            seenAllGroups = true;
          }
          if (listGroupsResponseType.getListGroupsResult().getGroups() != null && listGroupsResponseType.getListGroupsResult().getGroups().getMemberList() != null) {
            for (GroupType groupType: listGroupsResponseType.getListGroupsResult().getGroups().getMemberList()) {
              if (groupType.getGroupName().equals(action.properties.getGroupName())) {
                foundGroup = true;
                break;
              }
            }
          }
        }
        if (!foundGroup) return action;
        // for each user in the group, remove them
        List<String> realUsersToRemoveFromGroup = Lists.newArrayList();
        Set<String> passedInUsers = action.properties.getUsers() == null ? new HashSet<String>() : Sets.newHashSet(action.properties.getUsers());
        boolean seenAllUsers = false;
        String userMarker = null;
        while (!seenAllUsers) {
          GetGroupType getGroupType = MessageHelper.createMessage(GetGroupType.class, action.info.getEffectiveUserId());
          getGroupType.setGroupName(action.properties.getGroupName());
          if (userMarker != null) {
            getGroupType.setMarker(userMarker);
          }
          GetGroupResponseType getGroupResponseType = AsyncRequests.<GetGroupType,GetGroupResponseType> sendSync(configuration, getGroupType);
          if (getGroupResponseType.getGetGroupResult().getIsTruncated() == Boolean.TRUE) {
            userMarker = getGroupResponseType.getGetGroupResult().getMarker();
          } else {
            seenAllUsers = true;
          }
          if (getGroupResponseType.getGetGroupResult().getUsers() != null && getGroupResponseType.getGetGroupResult().getUsers().getMemberList() != null) {
            for (UserType userType: getGroupResponseType.getGetGroupResult().getUsers().getMemberList()) {
              if (passedInUsers.contains(userType.getUserName())) {
                realUsersToRemoveFromGroup.add(userType.getUserName());
              }
            }
          }
        }
        for (String user: realUsersToRemoveFromGroup) {
          RemoveUserFromGroupType removeUserFromGroupType = MessageHelper.createMessage(RemoveUserFromGroupType.class, action.info.getEffectiveUserId());
          removeUserFromGroupType.setGroupName(action.properties.getGroupName());
          removeUserFromGroupType.setUserName(user);
          AsyncRequests.<RemoveUserFromGroupType,RemoveUserFromGroupResponseType> sendSync(configuration, removeUserFromGroupType);
        }
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
    properties = (AWSIAMUserToGroupAdditionProperties) resourceProperties;
  }

  @Override
  public ResourceInfo getResourceInfo() {
    return info;
  }

  @Override
  public void setResourceInfo(ResourceInfo resourceInfo) {
    info = (AWSIAMUserToGroupAdditionResourceInfo) resourceInfo;
  }

  @Override
  public Promise<String> getCreatePromise(WorkflowOperations<StackActivity> workflowOperations, String resourceId, String stackId, String accountId, String effectiveUserId) {
    List<String> stepIds = Lists.transform(Lists.newArrayList(CreateSteps.values()), StepTransform.INSTANCE);
    return new MultiStepWithRetryCreatePromise(workflowOperations, stepIds, this).getCreatePromise(resourceId, stackId, accountId, effectiveUserId);
  }

  @Override
  public Promise<String> getDeletePromise(WorkflowOperations<StackActivity> workflowOperations, String resourceId, String stackId, String accountId, String effectiveUserId) {
    List<String> stepIds = Lists.transform(Lists.newArrayList(DeleteSteps.values()), StepTransform.INSTANCE);
    return new MultiStepWithRetryDeletePromise(workflowOperations, stepIds, this).getDeletePromise(resourceId, stackId, accountId, effectiveUserId);
  }

}


