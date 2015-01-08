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
  public int getNumCreateSteps() {
    return 4;
  }

  @Override
  public void create(int stepNum) throws Exception {
    ServiceConfiguration configuration = Topology.lookup(Euare.class);
    switch (stepNum) {
      case 0: // create user
        String userName = getDefaultPhysicalResourceId();
        CreateUserType createUserType = new CreateUserType();
        createUserType.setEffectiveUserId(info.getEffectiveUserId());
        createUserType.setUserName(userName);
        createUserType.setPath(properties.getPath());
        CreateUserResponseType createUserResponseType = AsyncRequests.<CreateUserType,CreateUserResponseType> sendSync(configuration, createUserType);
        String arn = createUserResponseType.getCreateUserResult().getUser().getArn();
        info.setPhysicalResourceId(userName);
        info.setArn(JsonHelper.getStringFromJsonNode(new TextNode(arn)));
        info.setReferenceValueJson(JsonHelper.getStringFromJsonNode(new TextNode(info.getPhysicalResourceId())));
        break;
      case 1: // add password (login profile)
        if (properties.getLoginProfile() != null) {
          CreateLoginProfileType createLoginProfileType = new CreateLoginProfileType();
          createLoginProfileType.setPassword(properties.getLoginProfile().getPassword());
          createLoginProfileType.setUserName(info.getPhysicalResourceId());
          createLoginProfileType.setEffectiveUserId(info.getEffectiveUserId());
          AsyncRequests.<CreateLoginProfileType,CreateLoginProfileResponseType> sendSync(configuration, createLoginProfileType);         
        }
        break;
      case 2: // add policies
        if (properties.getPolicies() != null) {
          for (EmbeddedIAMPolicy policy: properties.getPolicies()) {
            PutUserPolicyType putUserPolicyType = new PutUserPolicyType();
            putUserPolicyType.setUserName(info.getPhysicalResourceId());
            putUserPolicyType.setPolicyName(policy.getPolicyName());
            putUserPolicyType.setPolicyDocument(policy.getPolicyDocument().toString());
            putUserPolicyType.setEffectiveUserId(info.getEffectiveUserId());
            AsyncRequests.<PutUserPolicyType,PutUserPolicyResponseType> sendSync(configuration, putUserPolicyType);
          }
        }
        break;
      case 3: // add groups
        if (properties.getGroups() != null) {
          for (String groupName: properties.getGroups()) {
            AddUserToGroupType addUserToGroupType = new AddUserToGroupType();
            addUserToGroupType.setGroupName(groupName);
            addUserToGroupType.setUserName(info.getPhysicalResourceId());
            addUserToGroupType.setEffectiveUserId(info.getEffectiveUserId());
            AsyncRequests.<AddUserToGroupType,AddUserToGroupResponseType> sendSync(configuration, addUserToGroupType);
          }
        }
        break;
      default:
        throw new IllegalStateException("Invalid step " + stepNum);
    }
  }

  @Override
  public void update(int stepNum) throws Exception {
    throw new UnsupportedOperationException();
  }

  public void rollbackUpdate() throws Exception {
    // can't update so rollbackUpdate should be a NOOP
  }

  @Override
  public void delete() throws Exception {
    if (info.getPhysicalResourceId() == null) return;
    ServiceConfiguration configuration = Topology.lookup(Euare.class);
    // if no user, bye...
    boolean seenAllUsers = false;
    boolean foundUser = false;
    String userMarker = null;
    while (!seenAllUsers && !foundUser) {
      ListUsersType listUsersType = new ListUsersType();
      listUsersType.setEffectiveUserId(info.getEffectiveUserId());
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
          if (UserType.getUserName().equals(info.getPhysicalResourceId())) {
            foundUser = true;
            break;
          }
        }
      }
    }
    if (!foundUser) return;

    // remove user from groups we added (if there)
    if (properties.getGroups() != null) {
      boolean seenAllGroups = false;
      List<String> currentGroups = Lists.newArrayList();
      String groupMarker = null;
      while (!seenAllGroups) {
        ListGroupsForUserType listGroupsForUserType = new ListGroupsForUserType();
        listGroupsForUserType.setUserName(info.getPhysicalResourceId());
        listGroupsForUserType.setEffectiveUserId(info.getEffectiveUserId());
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
      for (String groupName: properties.getGroups()) {
        if (currentGroups.contains(groupName)) {
          RemoveUserFromGroupType removeUserFromGroupType = new RemoveUserFromGroupType();
          removeUserFromGroupType.setGroupName(groupName);
          removeUserFromGroupType.setUserName(info.getPhysicalResourceId());
          removeUserFromGroupType.setEffectiveUserId(info.getEffectiveUserId());
          AsyncRequests.<RemoveUserFromGroupType,RemoveUserFromGroupResponseType> sendSync(configuration, removeUserFromGroupType);
        }
      }
      // Note: the above will not add externally added groups, but this is by design...
    }

    // remove all policies added by us.  (Note: this could cause issues if an admin added some, but we delete what we create)
    // Note: deleting a non-existing policy doesn't do anything so we just delete them all...
    if (properties.getPolicies() != null) {
      for (EmbeddedIAMPolicy policy: properties.getPolicies()) {
        DeleteUserPolicyType deleteUserPolicyType = new DeleteUserPolicyType();
        deleteUserPolicyType.setUserName(info.getPhysicalResourceId());
        deleteUserPolicyType.setPolicyName(policy.getPolicyName());
        deleteUserPolicyType.setEffectiveUserId(info.getEffectiveUserId());
        AsyncRequests.<DeleteUserPolicyType,DeleteUserPolicyResponseType> sendSync(configuration, deleteUserPolicyType);
      }
    }
    DeleteUserType deleteUserType = new DeleteUserType();
    deleteUserType.setUserName(info.getPhysicalResourceId());
    deleteUserType.setEffectiveUserId(info.getEffectiveUserId());
    AsyncRequests.<DeleteUserType,DeleteUserResponseType> sendSync(configuration, deleteUserType);
  }

  @Override
  public void rollbackCreate() throws Exception {
    delete();
  }


}


