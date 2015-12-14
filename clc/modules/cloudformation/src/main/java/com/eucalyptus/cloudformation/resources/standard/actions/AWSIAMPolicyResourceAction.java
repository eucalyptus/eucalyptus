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
import com.eucalyptus.auth.euare.RoleType;
import com.eucalyptus.auth.euare.UserType;
import com.eucalyptus.cloudformation.resources.ResourceAction;
import com.eucalyptus.cloudformation.resources.ResourceInfo;
import com.eucalyptus.cloudformation.resources.ResourceProperties;
import com.eucalyptus.cloudformation.resources.standard.info.AWSIAMPolicyResourceInfo;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.AWSIAMPolicyProperties;
import com.eucalyptus.cloudformation.template.JsonHelper;
import com.eucalyptus.cloudformation.util.MessageHelper;
import com.eucalyptus.cloudformation.workflow.steps.Step;
import com.eucalyptus.cloudformation.workflow.steps.StepBasedResourceAction;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.Topology;
import com.eucalyptus.component.id.Euare;
import com.eucalyptus.util.async.AsyncRequests;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by ethomas on 2/3/14.
 */
public class AWSIAMPolicyResourceAction extends StepBasedResourceAction {

  private AWSIAMPolicyProperties properties = new AWSIAMPolicyProperties();
  private AWSIAMPolicyResourceInfo info = new AWSIAMPolicyResourceInfo();

  public AWSIAMPolicyResourceAction() {
    super(fromEnum(CreateSteps.class), fromEnum(DeleteSteps.class), null, null, null);
  }

  private enum CreateSteps implements Step {
    CREATE_POLICY {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        AWSIAMPolicyResourceAction action = (AWSIAMPolicyResourceAction) resourceAction;
        ServiceConfiguration configuration = Topology.lookup(Euare.class);
        // just get fields
        action.info.setPhysicalResourceId(action.getDefaultPhysicalResourceId());
        action.info.setReferenceValueJson(JsonHelper.getStringFromJsonNode(new TextNode(action.info.getPhysicalResourceId())));
        return action;
      }
    },
    ATTACH_TO_GROUPS {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        AWSIAMPolicyResourceAction action = (AWSIAMPolicyResourceAction) resourceAction;
        ServiceConfiguration configuration = Topology.lookup(Euare.class);
        if (action.properties.getGroups() != null) {
          for (String groupName: action.properties.getGroups()) {
            PutGroupPolicyType putGroupPolicyType = MessageHelper.createMessage(PutGroupPolicyType.class, action.info.getEffectiveUserId());
            putGroupPolicyType.setGroupName(groupName);
            putGroupPolicyType.setPolicyName(action.properties.getPolicyName());
            putGroupPolicyType.setPolicyDocument(action.properties.getPolicyDocument().toString());
            AsyncRequests.<PutGroupPolicyType,PutGroupPolicyResponseType> sendSync(configuration, putGroupPolicyType);
          }
        }
        return action;
      }
    },
    ATTACH_TO_USERS {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        AWSIAMPolicyResourceAction action = (AWSIAMPolicyResourceAction) resourceAction;
        ServiceConfiguration configuration = Topology.lookup(Euare.class);
        if (action.properties.getUsers() != null) {
          for (String userName: action.properties.getUsers()) {
            PutUserPolicyType putUserPolicyType = MessageHelper.createMessage(PutUserPolicyType.class, action.info.getEffectiveUserId());
            putUserPolicyType.setUserName(userName);
            putUserPolicyType.setPolicyName(action.properties.getPolicyName());
            putUserPolicyType.setPolicyDocument(action.properties.getPolicyDocument().toString());
            AsyncRequests.<PutUserPolicyType,PutUserPolicyResponseType> sendSync(configuration, putUserPolicyType);
          }
        }
        return action;
      }
    },
    ATTACH_TO_ROLES {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        AWSIAMPolicyResourceAction action = (AWSIAMPolicyResourceAction) resourceAction;
        ServiceConfiguration configuration = Topology.lookup(Euare.class);
        if (action.properties.getRoles() != null) {
          for (String roleName: action.properties.getRoles()) {
            PutRolePolicyType putRolePolicyType = MessageHelper.createMessage(PutRolePolicyType.class, action.info.getEffectiveUserId());
            putRolePolicyType.setRoleName(roleName);
            putRolePolicyType.setPolicyName(action.properties.getPolicyName());
            putRolePolicyType.setPolicyDocument(action.properties.getPolicyDocument().toString());
            AsyncRequests.<PutRolePolicyType,PutRolePolicyResponseType> sendSync(configuration, putRolePolicyType);
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
    DELETE_POLICY {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        AWSIAMPolicyResourceAction action = (AWSIAMPolicyResourceAction) resourceAction;
        ServiceConfiguration configuration = Topology.lookup(Euare.class);
        if (action.info.getPhysicalResourceId() == null) return action;

        // find all roles that still exist from the list and remove the policy
        if (action.properties.getRoles() != null && action.properties.getRoles().size() > 0) {
          List<String> realRolesToRemovePolicyFrom = Lists.newArrayList();
          Set<String> passedInRoles = action.properties.getRoles() == null ? new HashSet<String>() : Sets.newHashSet(action.properties.getRoles());
          boolean seenAllRoles = false;
          String roleMarker = null;
          while (!seenAllRoles) {
            ListRolesType listRolesType = MessageHelper.createMessage(ListRolesType.class, action.info.getEffectiveUserId());
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
            DeleteRolePolicyType deleteRolePolicyType = MessageHelper.createMessage(DeleteRolePolicyType.class, action.info.getEffectiveUserId());
            deleteRolePolicyType.setRoleName(role);
            deleteRolePolicyType.setPolicyName(action.properties.getPolicyName());
            AsyncRequests.<DeleteRolePolicyType,DeleteRolePolicyResponseType> sendSync(configuration, deleteRolePolicyType);
          }
        }

        // find all users that still exist from the list and remove the policy
        if (action.properties.getUsers() != null && action.properties.getUsers().size() > 0) {
          List<String> realUsersToRemovePolicyFrom = Lists.newArrayList();
          Set<String> passedInUsers = action.properties.getUsers() == null ? new HashSet<String>() : Sets.newHashSet(action.properties.getUsers());
          boolean seenAllUsers = false;
          String userMarker = null;
          while (!seenAllUsers) {
            ListUsersType listUsersType = MessageHelper.createMessage(ListUsersType.class, action.info.getEffectiveUserId());;
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
            DeleteUserPolicyType deleteUserPolicyType = MessageHelper.createMessage(DeleteUserPolicyType.class, action.info.getEffectiveUserId());
            deleteUserPolicyType.setUserName(user);
            deleteUserPolicyType.setPolicyName(action.properties.getPolicyName());
            AsyncRequests.<DeleteUserPolicyType,DeleteUserPolicyResponseType> sendSync(configuration, deleteUserPolicyType);
          }
        }

        // find all groups that still exist from the list and remove the policy
        if (action.properties.getGroups() != null && action.properties.getGroups().size() > 0) {
          List<String> realGroupsToRemovePolicyFrom = Lists.newArrayList();
          Set<String> passedInGroups = action.properties.getGroups() == null ? new HashSet<String>() : Sets.newHashSet(action.properties.getGroups());
          boolean seenAllGroups = false;
          String groupMarker = null;
          while (!seenAllGroups) {
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
                if (passedInGroups.contains(groupType.getGroupName())) {
                  realGroupsToRemovePolicyFrom.add(groupType.getGroupName());
                }
              }
            }
          }
          for (String group: realGroupsToRemovePolicyFrom) {
            DeleteGroupPolicyType deleteGroupPolicyType = MessageHelper.createMessage(DeleteGroupPolicyType.class, action.info.getEffectiveUserId());
            deleteGroupPolicyType.setGroupName(group);
            deleteGroupPolicyType.setPolicyName(action.properties.getPolicyName());
            AsyncRequests.<DeleteGroupPolicyType,DeleteGroupPolicyResponseType> sendSync(configuration, deleteGroupPolicyType);
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



}


