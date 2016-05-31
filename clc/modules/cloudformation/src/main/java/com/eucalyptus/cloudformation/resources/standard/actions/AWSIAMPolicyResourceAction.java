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
import com.eucalyptus.auth.euare.PutGroupPolicyResponseType;
import com.eucalyptus.auth.euare.PutGroupPolicyType;
import com.eucalyptus.auth.euare.PutRolePolicyResponseType;
import com.eucalyptus.auth.euare.PutRolePolicyType;
import com.eucalyptus.auth.euare.PutUserPolicyResponseType;
import com.eucalyptus.auth.euare.PutUserPolicyType;
import com.eucalyptus.cloudformation.resources.IAMHelper;
import com.eucalyptus.cloudformation.resources.ResourceAction;
import com.eucalyptus.cloudformation.resources.ResourceInfo;
import com.eucalyptus.cloudformation.resources.ResourceProperties;
import com.eucalyptus.cloudformation.resources.standard.info.AWSIAMPolicyResourceInfo;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.AWSIAMPolicyProperties;
import com.eucalyptus.cloudformation.template.JsonHelper;
import com.eucalyptus.cloudformation.util.MessageHelper;
import com.eucalyptus.cloudformation.workflow.steps.Step;
import com.eucalyptus.cloudformation.workflow.steps.StepBasedResourceAction;
import com.eucalyptus.cloudformation.workflow.steps.UpdateStep;
import com.eucalyptus.cloudformation.workflow.updateinfo.UpdateType;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.Topology;
import com.eucalyptus.component.id.Euare;
import com.eucalyptus.util.async.AsyncRequests;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.collect.Sets;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Created by ethomas on 2/3/14.
 */
public class AWSIAMPolicyResourceAction extends StepBasedResourceAction {

  private AWSIAMPolicyProperties properties = new AWSIAMPolicyProperties();
  private AWSIAMPolicyResourceInfo info = new AWSIAMPolicyResourceInfo();

  public AWSIAMPolicyResourceAction() {
    super(fromEnum(CreateSteps.class), fromEnum(DeleteSteps.class), fromUpdateEnum(UpdateNoInterruptionSteps.class), null);
  }

  @Override
  public UpdateType getUpdateType(ResourceAction resourceAction, boolean stackTagsChanged) {
    UpdateType updateType = info.supportsTags() && stackTagsChanged ? UpdateType.NO_INTERRUPTION : UpdateType.NONE;
    AWSIAMPolicyResourceAction otherAction = (AWSIAMPolicyResourceAction) resourceAction;
    if (!Objects.equals(properties.getGroups(), otherAction.properties.getGroups())) {
      updateType = UpdateType.max(updateType, UpdateType.NO_INTERRUPTION);
    }
    if (!Objects.equals(properties.getPolicyDocument(), otherAction.properties.getPolicyDocument())) {
      updateType = UpdateType.max(updateType, UpdateType.NO_INTERRUPTION);
    }
    if (!Objects.equals(properties.getPolicyName(), otherAction.properties.getPolicyName())) {
      updateType = UpdateType.max(updateType, UpdateType.NO_INTERRUPTION);
    }
    if (!Objects.equals(properties.getRoles(), otherAction.properties.getRoles())) {
      updateType = UpdateType.max(updateType, UpdateType.NO_INTERRUPTION);
    }
    if (!Objects.equals(properties.getUsers(), otherAction.properties.getUsers())) {
      updateType = UpdateType.max(updateType, UpdateType.NO_INTERRUPTION);
    }
    return updateType;
  }

  private enum CreateSteps implements Step {
    CREATE_POLICY {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        AWSIAMPolicyResourceAction action = (AWSIAMPolicyResourceAction) resourceAction;
        ServiceConfiguration configuration = Topology.lookup(Euare.class);
        // just get fields
        action.info.setPhysicalResourceId(action.getDefaultPhysicalResourceId());
        action.info.setCreatedEnoughToDelete(true);
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
        if (!Boolean.TRUE.equals(action.info.getCreatedEnoughToDelete())) return action;

        // find all roles that still exist from the list and remove the policy
        Set<String> passedInRoles = IAMHelper.collectionToSetAndNullToEmpty(action.properties.getRoles());
        List<String> realRolesToRemovePolicyFrom = IAMHelper.getExistingRoles(configuration, passedInRoles, action.info.getEffectiveUserId());
        for (String role: realRolesToRemovePolicyFrom) {
          DeleteRolePolicyType deleteRolePolicyType = MessageHelper.createMessage(DeleteRolePolicyType.class, action.info.getEffectiveUserId());
          deleteRolePolicyType.setRoleName(role);
          deleteRolePolicyType.setPolicyName(action.properties.getPolicyName());
          AsyncRequests.<DeleteRolePolicyType,DeleteRolePolicyResponseType> sendSync(configuration, deleteRolePolicyType);
        }

        // find all users that still exist from the list and remove the policy
        Set<String> passedInUsers = action.properties.getUsers() == null ? new HashSet<String>() : Sets.newHashSet(action.properties.getUsers());
        List<String> realUsersToRemovePolicyFrom = IAMHelper.getExistingUsers(configuration, passedInUsers, action.info.getEffectiveUserId());
        for (String user: realUsersToRemovePolicyFrom) {
          DeleteUserPolicyType deleteUserPolicyType = MessageHelper.createMessage(DeleteUserPolicyType.class, action.info.getEffectiveUserId());
          deleteUserPolicyType.setUserName(user);
          deleteUserPolicyType.setPolicyName(action.properties.getPolicyName());
          AsyncRequests.<DeleteUserPolicyType,DeleteUserPolicyResponseType> sendSync(configuration, deleteUserPolicyType);
        }

        // find all groups that still exist from the list and remove the policy
        Set<String> passedInGroups = IAMHelper.collectionToSetAndNullToEmpty(action.properties.getGroups());
        List<String> realGroupsToRemovePolicyFrom = IAMHelper.getExistingGroups(configuration, passedInGroups, action.info.getEffectiveUserId());
        for (String group: realGroupsToRemovePolicyFrom) {
          DeleteGroupPolicyType deleteGroupPolicyType = MessageHelper.createMessage(DeleteGroupPolicyType.class, action.info.getEffectiveUserId());
          deleteGroupPolicyType.setGroupName(group);
          deleteGroupPolicyType.setPolicyName(action.properties.getPolicyName());
          AsyncRequests.<DeleteGroupPolicyType,DeleteGroupPolicyResponseType> sendSync(configuration, deleteGroupPolicyType);
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

  private enum UpdateNoInterruptionSteps implements UpdateStep {
    UPDATE_ATTACHMENT_TO_GROUPS {
      @Override
      public ResourceAction perform(ResourceAction oldResourceAction, ResourceAction newResourceAction) throws Exception {
        AWSIAMPolicyResourceAction oldAction = (AWSIAMPolicyResourceAction) oldResourceAction;
        AWSIAMPolicyResourceAction newAction = (AWSIAMPolicyResourceAction) newResourceAction;
        ServiceConfiguration configuration = Topology.lookup(Euare.class);
        Set<String> oldGroupNames = IAMHelper.collectionToSetAndNullToEmpty(oldAction.properties.getGroups());
        Set<String> newGroupNames = IAMHelper.collectionToSetAndNullToEmpty(newAction.properties.getGroups());
        // add the policy to the new group
        for (String groupName : newGroupNames) {
          PutGroupPolicyType putGroupPolicyType = MessageHelper.createMessage(PutGroupPolicyType.class, newAction.info.getEffectiveUserId());
          putGroupPolicyType.setGroupName(groupName);
          putGroupPolicyType.setPolicyName(newAction.properties.getPolicyName());
          putGroupPolicyType.setPolicyDocument(newAction.properties.getPolicyDocument().toString());
          AsyncRequests.<PutGroupPolicyType, PutGroupPolicyResponseType>sendSync(configuration, putGroupPolicyType);
        }
        // if the policy name has changed, remove it from all the old groups
        // otherwise remove it from just the old groups that are not new groups
        Collection<String> targetOldGroupNames =
          oldAction.properties.getPolicyName().equals(newAction.properties.getPolicyName()) ?
            Sets.difference(oldGroupNames, newGroupNames) : oldGroupNames;
        for (String groupName : targetOldGroupNames) {
          DeleteGroupPolicyType deleteGroupPolicyType = MessageHelper.createMessage(DeleteGroupPolicyType.class, newAction.info.getEffectiveUserId());
          deleteGroupPolicyType.setGroupName(groupName);
          deleteGroupPolicyType.setPolicyName(oldAction.properties.getPolicyName());
          AsyncRequests.<DeleteGroupPolicyType, DeleteGroupPolicyResponseType>sendSync(configuration, deleteGroupPolicyType);
        }
        return newAction;
      }
    },
    UPDATE_ATTACHMENT_TO_USERS {
      @Override
      public ResourceAction perform(ResourceAction oldResourceAction, ResourceAction newResourceAction) throws Exception {
        AWSIAMPolicyResourceAction oldAction = (AWSIAMPolicyResourceAction) oldResourceAction;
        AWSIAMPolicyResourceAction newAction = (AWSIAMPolicyResourceAction) newResourceAction;
        ServiceConfiguration configuration = Topology.lookup(Euare.class);
        Set<String> oldUserNames = IAMHelper.collectionToSetAndNullToEmpty(oldAction.properties.getUsers());
        Set<String> newUserNames = IAMHelper.collectionToSetAndNullToEmpty(newAction.properties.getUsers());
        // add the policy to the new user
        for (String userName: newUserNames) {
          PutUserPolicyType putUserPolicyType = MessageHelper.createMessage(PutUserPolicyType.class, newAction.info.getEffectiveUserId());
          putUserPolicyType.setUserName(userName);
          putUserPolicyType.setPolicyName(newAction.properties.getPolicyName());
          putUserPolicyType.setPolicyDocument(newAction.properties.getPolicyDocument().toString());
          AsyncRequests.<PutUserPolicyType,PutUserPolicyResponseType> sendSync(configuration, putUserPolicyType);
        }
        // if the policy name has changed, remove it from all the old groups
        // otherwise remove it from just the old groups that are not new groups
        Collection<String> targetOldUserNames =
          oldAction.properties.getPolicyName().equals(newAction.properties.getPolicyName()) ?
            Sets.difference(oldUserNames, newUserNames) : oldUserNames;
        for (String userName: targetOldUserNames) {
          DeleteUserPolicyType deleteUserPolicyType = MessageHelper.createMessage(DeleteUserPolicyType.class, newAction.info.getEffectiveUserId());
          deleteUserPolicyType.setUserName(userName);
          deleteUserPolicyType.setPolicyName(oldAction.properties.getPolicyName());
          AsyncRequests.<DeleteUserPolicyType,DeleteUserPolicyResponseType> sendSync(configuration, deleteUserPolicyType);
        }
        return newAction;
      }
    },
    UPDATE_ATTACHMENT_TO_ROLES {
      @Override
      public ResourceAction perform(ResourceAction oldResourceAction, ResourceAction newResourceAction) throws Exception {
        AWSIAMPolicyResourceAction oldAction = (AWSIAMPolicyResourceAction) oldResourceAction;
        AWSIAMPolicyResourceAction newAction = (AWSIAMPolicyResourceAction) newResourceAction;
        ServiceConfiguration configuration = Topology.lookup(Euare.class);
        Set<String> oldRoleNames = IAMHelper.collectionToSetAndNullToEmpty(oldAction.properties.getRoles());
        Set<String> newRoleNames = IAMHelper.collectionToSetAndNullToEmpty(newAction.properties.getRoles());
        // add the policy to the new role
        for (String roleName: newRoleNames) {
          PutRolePolicyType putRolePolicyType = MessageHelper.createMessage(PutRolePolicyType.class, newAction.info.getEffectiveUserId());
          putRolePolicyType.setRoleName(roleName);
          putRolePolicyType.setPolicyName(newAction.properties.getPolicyName());
          putRolePolicyType.setPolicyDocument(newAction.properties.getPolicyDocument().toString());
          AsyncRequests.<PutRolePolicyType,PutRolePolicyResponseType> sendSync(configuration, putRolePolicyType);
        }
        // if the policy name has changed, remove it from all the old roles
        // otherwise remove it from just the old roles that are not new roles
        Collection<String> targetOldRoleNames =
          oldAction.properties.getPolicyName().equals(newAction.properties.getPolicyName()) ?
            Sets.difference(oldRoleNames, newRoleNames) : oldRoleNames;
        for (String roleName: targetOldRoleNames) {
          DeleteRolePolicyType deleteRolePolicyType = MessageHelper.createMessage(DeleteRolePolicyType.class, newAction.info.getEffectiveUserId());
          deleteRolePolicyType.setRoleName(roleName);
          deleteRolePolicyType.setPolicyName(oldAction.properties.getPolicyName());
          AsyncRequests.<DeleteRolePolicyType,DeleteRolePolicyResponseType> sendSync(configuration, deleteRolePolicyType);
        }
        return newAction;
      }
    };

    @Nullable
    @Override
    public Integer getTimeout() {
      return null;
    }
  }


}


