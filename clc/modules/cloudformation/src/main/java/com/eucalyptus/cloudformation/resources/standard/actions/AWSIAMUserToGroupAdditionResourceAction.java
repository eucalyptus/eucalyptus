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
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.Topology;
import com.eucalyptus.component.id.Euare;
import com.eucalyptus.util.async.AsyncRequests;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by ethomas on 2/3/14.
 */
public class AWSIAMUserToGroupAdditionResourceAction extends ResourceAction {

  private AWSIAMUserToGroupAdditionProperties properties = new AWSIAMUserToGroupAdditionProperties();
  private AWSIAMUserToGroupAdditionResourceInfo info = new AWSIAMUserToGroupAdditionResourceInfo();
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
  public void create(int stepNum) throws Exception {
    switch (stepNum) {
      case 0:
        ServiceConfiguration configuration = Topology.lookup(Euare.class);
        // make sure group exists
        boolean seenAllGroups = false;
        boolean foundGroup = false;
        String groupMarker = null;
        while (!seenAllGroups && !foundGroup) {
          ListGroupsType listGroupsType = new ListGroupsType();
          listGroupsType.setEffectiveUserId(info.getEffectiveUserId());
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
              if (groupType.getGroupName().equals(properties.getGroupName())) {
                foundGroup = true;
                break;
              }
            }
          }
        }
        if (!foundGroup) throw new ValidationErrorException("No such group " + properties.getGroupName());

        boolean seenAllUsers = false;
        List<String> currentUsers = Lists.newArrayList();
        String userMarker = null;
        while (!seenAllUsers) {
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
            for (UserType userType: listUsersResponseType.getListUsersResult().getUsers().getMemberList()) {
              currentUsers.add(userType.getUserName());
            }
          }
        }
        List<String> nonexistantUsers = Lists.newArrayList();
        for (String user: properties.getUsers()) {
          if (!currentUsers.contains(user)) {
            nonexistantUsers.add(user);
          }
        }
        if (!nonexistantUsers.isEmpty()) {
          throw new ValidationErrorException("No such user(s) " + nonexistantUsers.toString());
        }
        for (String userName: properties.getUsers()) {
          AddUserToGroupType addUserToGroupType = new AddUserToGroupType();
          addUserToGroupType.setGroupName(properties.getGroupName());
          addUserToGroupType.setUserName(userName);
          addUserToGroupType.setEffectiveUserId(info.getEffectiveUserId());
          AsyncRequests.<AddUserToGroupType,AddUserToGroupResponseType> sendSync(configuration, addUserToGroupType);
        }
        info.setPhysicalResourceId(getDefaultPhysicalResourceId());
        info.setReferenceValueJson(JsonHelper.getStringFromJsonNode(new TextNode(info.getPhysicalResourceId())));
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
    // if no group, bye...
    boolean seenAllGroups = false;
    boolean foundGroup = false;
    String groupMarker = null;
    while (!seenAllGroups && !foundGroup) {
      ListGroupsType listGroupsType = new ListGroupsType();
      listGroupsType.setEffectiveUserId(info.getEffectiveUserId());
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
          if (groupType.getGroupName().equals(properties.getGroupName())) {
            foundGroup = true;
            break;
          }
        }
      }
    }
    if (!foundGroup) return;
    // for each user in the group, remove them
    List<String> realUsersToRemoveFromGroup = Lists.newArrayList();
    Set<String> passedInUsers = properties.getUsers() == null ? new HashSet<String>() : Sets.newHashSet(properties.getUsers());
    boolean seenAllUsers = false;
    String userMarker = null;
    while (!seenAllUsers) {
      GetGroupType getGroupType = new GetGroupType();
      getGroupType.setGroupName(properties.getGroupName());
      getGroupType.setEffectiveUserId(info.getEffectiveUserId());
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
      RemoveUserFromGroupType removeUserFromGroupType = new RemoveUserFromGroupType();
      removeUserFromGroupType.setGroupName(properties.getGroupName());
      removeUserFromGroupType.setUserName(user);
      removeUserFromGroupType.setEffectiveUserId(info.getEffectiveUserId());
      AsyncRequests.<RemoveUserFromGroupType,RemoveUserFromGroupResponseType> sendSync(configuration, removeUserFromGroupType);
    }
  }

  @Override
  public void rollbackCreate() throws Exception {
    delete();
  }

}


