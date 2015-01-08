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


import com.eucalyptus.auth.euare.DeleteGroupPolicyResponseType;
import com.eucalyptus.auth.euare.DeleteGroupPolicyType;
import com.eucalyptus.auth.euare.DeleteRolePolicyResponseType;
import com.eucalyptus.auth.euare.DeleteRolePolicyType;
import com.eucalyptus.auth.euare.DeleteUserPolicyResponseType;
import com.eucalyptus.auth.euare.DeleteUserPolicyType;
import com.eucalyptus.auth.euare.GroupType;
import com.eucalyptus.auth.euare.ListGroupsResponseType;
import com.eucalyptus.auth.euare.ListGroupsType;
import com.eucalyptus.auth.euare.ListRolesResponseType;
import com.eucalyptus.auth.euare.ListRolesType;
import com.eucalyptus.auth.euare.ListUsersResponseType;
import com.eucalyptus.auth.euare.ListUsersType;
import com.eucalyptus.auth.euare.PutGroupPolicyResponseType;
import com.eucalyptus.auth.euare.PutGroupPolicyType;
import com.eucalyptus.auth.euare.PutRolePolicyResponseType;
import com.eucalyptus.auth.euare.PutRolePolicyType;
import com.eucalyptus.auth.euare.PutUserPolicyResponseType;
import com.eucalyptus.auth.euare.PutUserPolicyType;
import com.eucalyptus.auth.euare.RemoveUserFromGroupResponseType;
import com.eucalyptus.auth.euare.RemoveUserFromGroupType;
import com.eucalyptus.auth.euare.RoleType;
import com.eucalyptus.auth.euare.UserType;
import com.eucalyptus.cloudformation.resources.ResourceAction;
import com.eucalyptus.cloudformation.resources.ResourceInfo;
import com.eucalyptus.cloudformation.resources.ResourceProperties;
import com.eucalyptus.cloudformation.resources.standard.info.AWSIAMPolicyResourceInfo;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.AWSIAMPolicyProperties;
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
public class AWSIAMPolicyResourceAction extends ResourceAction {

  private AWSIAMPolicyProperties properties = new AWSIAMPolicyProperties();
  private AWSIAMPolicyResourceInfo info = new AWSIAMPolicyResourceInfo();
  @Override
  public ResourceProperties getResourceProperties() {
    return properties;
  }

  @Override
  public void setResourceProperties(ResourceProperties resourceProperties) {
    properties = (AWSIAMPolicyProperties) resourceProperties;
  }

  @Override
  public ResourceInfo getResourceInfo() {
    return info;
  }

  @Override
  public void setResourceInfo(ResourceInfo resourceInfo) {
    info = (AWSIAMPolicyResourceInfo) resourceInfo;
  }

  @Override
  public int getNumCreateSteps() {
    return 4;
  }

  @Override
  public void create(int stepNum) throws Exception {
    ServiceConfiguration configuration = Topology.lookup(Euare.class);
    switch (stepNum) {
      case 0: // create policy (just get fields)
        info.setPhysicalResourceId(getDefaultPhysicalResourceId());
        info.setReferenceValueJson(JsonHelper.getStringFromJsonNode(new TextNode(info.getPhysicalResourceId())));
        break;
      case 1: // attach to groups...
        if (properties.getGroups() != null) {
          for (String groupName: properties.getGroups()) {
            PutGroupPolicyType putGroupPolicyType = new PutGroupPolicyType();
            putGroupPolicyType.setGroupName(groupName);
            putGroupPolicyType.setPolicyName(properties.getPolicyName());
            putGroupPolicyType.setPolicyDocument(properties.getPolicyDocument().toString());
            putGroupPolicyType.setEffectiveUserId(info.getEffectiveUserId());
            AsyncRequests.<PutGroupPolicyType,PutGroupPolicyResponseType> sendSync(configuration, putGroupPolicyType);
          }
        }
        break;
      case 2: // attach to users...
        if (properties.getUsers() != null) {
          for (String userName: properties.getUsers()) {
            PutUserPolicyType putUserPolicyType = new PutUserPolicyType();
            putUserPolicyType.setUserName(userName);
            putUserPolicyType.setPolicyName(properties.getPolicyName());
            putUserPolicyType.setPolicyDocument(properties.getPolicyDocument().toString());
            putUserPolicyType.setEffectiveUserId(info.getEffectiveUserId());
            AsyncRequests.<PutUserPolicyType,PutUserPolicyResponseType> sendSync(configuration, putUserPolicyType);
          }
        }
        break;
      case 3: // attach to roles...
        if (properties.getRoles() != null) {
          for (String roleName: properties.getRoles()) {
            PutRolePolicyType putRolePolicyType = new PutRolePolicyType();
            putRolePolicyType.setRoleName(roleName);
            putRolePolicyType.setPolicyName(properties.getPolicyName());
            putRolePolicyType.setPolicyDocument(properties.getPolicyDocument().toString());
            putRolePolicyType.setEffectiveUserId(info.getEffectiveUserId());
            AsyncRequests.<PutRolePolicyType,PutRolePolicyResponseType> sendSync(configuration, putRolePolicyType);
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

    // find all roles that still exist from the list and remove the policy
    if (properties.getRoles() != null && properties.getRoles().size() > 0) {
      List<String> realRolesToRemovePolicyFrom = Lists.newArrayList();
      Set<String> passedInRoles = properties.getRoles() == null ? new HashSet<String>() : Sets.newHashSet(properties.getRoles());
      boolean seenAllRoles = false;
      String roleMarker = null;
      while (!seenAllRoles) {
        ListRolesType listRolesType = new ListRolesType();
        listRolesType.setEffectiveUserId(info.getEffectiveUserId());
        if (roleMarker != null) {
          listRolesType.setMarker(roleMarker);
        }
        ListRolesResponseType listRolesResponseType = AsyncRequests.<ListRolesType,ListRolesResponseType> sendSync(configuration, listRolesType);
        if (listRolesResponseType.getListRolesResult().getIsTruncated() == Boolean.TRUE) {
          roleMarker = listRolesResponseType.getListRolesResult().getMarker();
        } else {
          seenAllRoles = true;
        }
        if (listRolesResponseType.getListRolesResult().getRoles() != null && listRolesResponseType.getListRolesResult().getRoles().getMember() != null) {
          for (RoleType roleType: listRolesResponseType.getListRolesResult().getRoles().getMember()) {
            if (passedInRoles.contains(roleType.getRoleName())) {
              realRolesToRemovePolicyFrom.add(roleType.getRoleName());
            }
          }
        }
      }
      for (String role: realRolesToRemovePolicyFrom) {
        DeleteRolePolicyType deleteRolePolicyType = new DeleteRolePolicyType();
        deleteRolePolicyType.setRoleName(role);
        deleteRolePolicyType.setPolicyName(properties.getPolicyName());
        deleteRolePolicyType.setEffectiveUserId(info.getEffectiveUserId());
        AsyncRequests.<DeleteRolePolicyType,DeleteRolePolicyResponseType> sendSync(configuration, deleteRolePolicyType);
      }
    }

    // find all users that still exist from the list and remove the policy
    if (properties.getUsers() != null && properties.getUsers().size() > 0) {
      List<String> realUsersToRemovePolicyFrom = Lists.newArrayList();
      Set<String> passedInUsers = properties.getUsers() == null ? new HashSet<String>() : Sets.newHashSet(properties.getUsers());
      boolean seenAllUsers = false;
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
            if (passedInUsers.contains(userType.getUserName())) {
              realUsersToRemovePolicyFrom.add(userType.getUserName());
            }
          }
        }
      }
      for (String user: realUsersToRemovePolicyFrom) {
        DeleteUserPolicyType deleteUserPolicyType = new DeleteUserPolicyType();
        deleteUserPolicyType.setUserName(user);
        deleteUserPolicyType.setPolicyName(properties.getPolicyName());
        deleteUserPolicyType.setEffectiveUserId(info.getEffectiveUserId());
        AsyncRequests.<DeleteUserPolicyType,DeleteUserPolicyResponseType> sendSync(configuration, deleteUserPolicyType);
      }
    }

    // find all groups that still exist from the list and remove the policy
    if (properties.getGroups() != null && properties.getGroups().size() > 0) {
      List<String> realGroupsToRemovePolicyFrom = Lists.newArrayList();
      Set<String> passedInGroups = properties.getGroups() == null ? new HashSet<String>() : Sets.newHashSet(properties.getGroups());
      boolean seenAllGroups = false;
      String groupMarker = null;
      while (!seenAllGroups) {
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
            if (passedInGroups.contains(groupType.getGroupName())) {
              realGroupsToRemovePolicyFrom.add(groupType.getGroupName());
            }
          }
        }
      }
      for (String group: realGroupsToRemovePolicyFrom) {
        DeleteGroupPolicyType deleteGroupPolicyType = new DeleteGroupPolicyType();
        deleteGroupPolicyType.setGroupName(group);
        deleteGroupPolicyType.setPolicyName(properties.getPolicyName());
        deleteGroupPolicyType.setEffectiveUserId(info.getEffectiveUserId());
        AsyncRequests.<DeleteGroupPolicyType,DeleteGroupPolicyResponseType> sendSync(configuration, deleteGroupPolicyType);
      }
    }
  }

  @Override
  public void rollbackCreate() throws Exception {
    delete();
  }


}


