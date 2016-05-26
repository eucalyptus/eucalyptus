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
import com.eucalyptus.cloudformation.ValidationErrorException;
import com.eucalyptus.cloudformation.resources.IAMHelper;
import com.eucalyptus.cloudformation.resources.ResourceAction;
import com.eucalyptus.cloudformation.resources.ResourceInfo;
import com.eucalyptus.cloudformation.resources.ResourceProperties;
import com.eucalyptus.cloudformation.resources.standard.info.AWSIAMUserToGroupAdditionResourceInfo;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.AWSIAMUserToGroupAdditionProperties;
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
import java.util.Objects;
import java.util.Set;

/**
 * Created by ethomas on 2/3/14.
 */
public class AWSIAMUserToGroupAdditionResourceAction extends StepBasedResourceAction {

  private AWSIAMUserToGroupAdditionProperties properties = new AWSIAMUserToGroupAdditionProperties();
  private AWSIAMUserToGroupAdditionResourceInfo info = new AWSIAMUserToGroupAdditionResourceInfo();

  public AWSIAMUserToGroupAdditionResourceAction() {
    super(fromEnum(CreateSteps.class), fromEnum(DeleteSteps.class), fromUpdateEnum(UpdateNoInterruptionSteps.class), null);
  }
  @Override
  public UpdateType getUpdateType(ResourceAction resourceAction, boolean stackTagsChanged) {
    UpdateType updateType = info.supportsTags() && stackTagsChanged ? UpdateType.NO_INTERRUPTION : UpdateType.NONE;
    AWSIAMUserToGroupAdditionResourceAction otherAction = (AWSIAMUserToGroupAdditionResourceAction) resourceAction;
    if (!Objects.equals(properties.getGroupName(), otherAction.properties.getGroupName())) {
      updateType = UpdateType.max(updateType, UpdateType.NO_INTERRUPTION);
    }
    if (!Objects.equals(properties.getUsers(), otherAction.properties.getUsers())) {
      updateType = UpdateType.max(updateType, UpdateType.NO_INTERRUPTION);
    }
    return updateType;
  }


  private enum CreateSteps implements Step {
    ADD_USER_TO_GROUP {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        AWSIAMUserToGroupAdditionResourceAction action = (AWSIAMUserToGroupAdditionResourceAction) resourceAction;
        ServiceConfiguration configuration = Topology.lookup(Euare.class);
        if (!IAMHelper.groupExists(configuration, action.properties.getGroupName(), action.info.getEffectiveUserId())) {
          throw new ValidationErrorException("No such group " + action.properties.getGroupName());
        }
        Collection<String> nonexistantUsers = IAMHelper.nonexistantUsers(configuration, action.properties.getUsers(), action.info.getEffectiveUserId());
        if (!nonexistantUsers.isEmpty()) {
          throw new ValidationErrorException("No such user(s) " + nonexistantUsers.toString());
        }
        // only add new users (AddUserToGroup not idempotent)
        Set<String> existingUsersInGroup = IAMHelper.getUserNamesForGroup(configuration, action.properties.getGroupName(), action.info.getEffectiveUserId());
        Set<String> passedInUsers = Sets.newLinkedHashSet();
        if (action.properties.getUsers() != null) {
          passedInUsers.addAll(action.properties.getUsers());
        }
        for (String userName: Sets.difference(passedInUsers, existingUsersInGroup)) {
          AddUserToGroupType addUserToGroupType = MessageHelper.createMessage(AddUserToGroupType.class, action.info.getEffectiveUserId());
          addUserToGroupType.setGroupName(action.properties.getGroupName());
          addUserToGroupType.setUserName(userName);
          AsyncRequests.<AddUserToGroupType,AddUserToGroupResponseType> sendSync(configuration, addUserToGroupType);
        }
        action.info.setPhysicalResourceId(action.getDefaultPhysicalResourceId());
        action.info.setCreatedEnoughToDelete(true);
        action.info.setReferenceValueJson(JsonHelper.getStringFromJsonNode(new TextNode(action.info.getPhysicalResourceId())));
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
    REMOVE_USER_FROM_GROUP {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        AWSIAMUserToGroupAdditionResourceAction action = (AWSIAMUserToGroupAdditionResourceAction) resourceAction;
        ServiceConfiguration configuration = Topology.lookup(Euare.class);
        if (!Boolean.TRUE.equals(action.info.getCreatedEnoughToDelete())) return action;
        IAMHelper.removeUsersFromGroup(configuration, action.properties.getUsers(), action.properties.getGroupName(), action.info.getEffectiveUserId());
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

  private enum UpdateNoInterruptionSteps implements UpdateStep {
    UPDATE_USERS_IN_GROUP {
      @Override
      public ResourceAction perform(ResourceAction oldResourceAction, ResourceAction newResourceAction) throws Exception {
        AWSIAMUserToGroupAdditionResourceAction oldAction = (AWSIAMUserToGroupAdditionResourceAction) oldResourceAction;
        AWSIAMUserToGroupAdditionResourceAction newAction = (AWSIAMUserToGroupAdditionResourceAction) newResourceAction;
        ServiceConfiguration configuration = Topology.lookup(Euare.class);
        if (!IAMHelper.groupExists(configuration, newAction.properties.getGroupName(), newAction.info.getEffectiveUserId())) {
          throw new ValidationErrorException("No such group " + newAction.properties.getGroupName());
        }
        Collection<String> nonexistantUsers = IAMHelper.nonexistantUsers(configuration, newAction.properties.getUsers(), newAction.info.getEffectiveUserId());
        if (!nonexistantUsers.isEmpty()) {
          throw new ValidationErrorException("No such user(s) " + nonexistantUsers.toString());
        }
        // First only add the 'new' users to the group (AddUserToGroup is not idempotent currently)
        Set<String> existingUsersForGroup = IAMHelper.getUserNamesForGroup(configuration, newAction.properties.getGroupName(), newAction.info.getEffectiveUserId());
        Set<String> passedInUsers = Sets.newLinkedHashSet();
        if (newAction.properties.getUsers() != null) {
          passedInUsers.addAll(newAction.properties.getUsers());
        }
        for (String userName: Sets.difference(passedInUsers, existingUsersForGroup)) {
          AddUserToGroupType addUserToGroupType = MessageHelper.createMessage(AddUserToGroupType.class, newAction.info.getEffectiveUserId());
          addUserToGroupType.setGroupName(newAction.properties.getGroupName());
          addUserToGroupType.setUserName(userName);
          AsyncRequests.<AddUserToGroupType,AddUserToGroupResponseType> sendSync(configuration, addUserToGroupType);
        }
        if (!Objects.equals(oldAction.properties.getGroupName(), newAction.properties.getGroupName())) {
          IAMHelper.removeUsersFromGroup(
            configuration,
            oldAction.properties.getUsers(),
            oldAction.properties.getGroupName(),
            oldAction.info.getEffectiveUserId()
          );
        } else {
          IAMHelper.removeUsersFromGroup(
            configuration,
            Sets.difference(
              Sets.newLinkedHashSet(oldAction.properties.getUsers()),
              Sets.newLinkedHashSet(newAction.properties.getUsers())
            ),
            newAction.properties.getGroupName(),
            newAction.info.getEffectiveUserId()
          );
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


