/*************************************************************************
 * Copyright 2009-2013 Ent. Services Development Corporation LP
 *
 * Redistribution and use of this software in source and binary forms,
 * with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *   Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 *   Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer
 *   in the documentation and/or other materials provided with the
 *   distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 ************************************************************************/
package com.eucalyptus.cloudformation.resources.standard.actions;


import com.eucalyptus.auth.euare.common.msgs.AddUserToGroupResponseType;
import com.eucalyptus.auth.euare.common.msgs.AddUserToGroupType;
import com.eucalyptus.auth.euare.common.msgs.AttachUserPolicyType;
import com.eucalyptus.auth.euare.common.msgs.AttachedPolicyType;
import com.eucalyptus.auth.euare.common.msgs.CreateLoginProfileResponseType;
import com.eucalyptus.auth.euare.common.msgs.CreateLoginProfileType;
import com.eucalyptus.auth.euare.common.msgs.CreateUserResponseType;
import com.eucalyptus.auth.euare.common.msgs.CreateUserType;
import com.eucalyptus.auth.euare.common.msgs.DeleteLoginProfileResponseType;
import com.eucalyptus.auth.euare.common.msgs.DeleteLoginProfileType;
import com.eucalyptus.auth.euare.common.msgs.DeleteUserPolicyResponseType;
import com.eucalyptus.auth.euare.common.msgs.DeleteUserPolicyType;
import com.eucalyptus.auth.euare.common.msgs.DeleteUserResponseType;
import com.eucalyptus.auth.euare.common.msgs.DeleteUserType;
import com.eucalyptus.auth.euare.common.msgs.DetachUserPolicyType;
import com.eucalyptus.auth.euare.common.msgs.GroupType;
import com.eucalyptus.auth.euare.common.msgs.ListAttachedUserPoliciesResponseType;
import com.eucalyptus.auth.euare.common.msgs.ListAttachedUserPoliciesType;
import com.eucalyptus.auth.euare.common.msgs.ListGroupsForUserResponseType;
import com.eucalyptus.auth.euare.common.msgs.ListGroupsForUserType;
import com.eucalyptus.auth.euare.common.msgs.PutUserPolicyResponseType;
import com.eucalyptus.auth.euare.common.msgs.PutUserPolicyType;
import com.eucalyptus.auth.euare.common.msgs.RemoveUserFromGroupResponseType;
import com.eucalyptus.auth.euare.common.msgs.RemoveUserFromGroupType;
import com.eucalyptus.auth.euare.common.msgs.UpdateLoginProfileResponseType;
import com.eucalyptus.auth.euare.common.msgs.UpdateLoginProfileType;
import com.eucalyptus.auth.euare.common.msgs.UpdateUserResponseType;
import com.eucalyptus.auth.euare.common.msgs.UpdateUserType;
import com.eucalyptus.auth.euare.common.msgs.UserType;
import com.eucalyptus.cloudformation.ValidationErrorException;
import com.eucalyptus.cloudformation.resources.IAMHelper;
import com.eucalyptus.cloudformation.resources.ResourceAction;
import com.eucalyptus.cloudformation.resources.ResourceInfo;
import com.eucalyptus.cloudformation.resources.ResourceProperties;
import com.eucalyptus.cloudformation.resources.standard.info.AWSIAMUserResourceInfo;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.AWSIAMUserProperties;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.EmbeddedIAMPolicy;
import com.eucalyptus.cloudformation.template.JsonHelper;
import com.eucalyptus.cloudformation.util.MessageHelper;
import com.eucalyptus.cloudformation.workflow.steps.Step;
import com.eucalyptus.cloudformation.workflow.steps.StepBasedResourceAction;
import com.eucalyptus.cloudformation.workflow.steps.UpdateStep;
import com.eucalyptus.cloudformation.workflow.updateinfo.UpdateType;
import com.eucalyptus.cloudformation.workflow.updateinfo.UpdateTypeAndDirection;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.Topology;
import com.eucalyptus.component.id.Euare;
import com.eucalyptus.util.async.AsyncExceptions;
import com.eucalyptus.util.async.AsyncRequests;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.base.MoreObjects;
import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.log4j.Logger;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Created by ethomas on 2/3/14.
 */
public class AWSIAMUserResourceAction extends StepBasedResourceAction {
  private static final Logger LOG = Logger.getLogger(AWSIAMUserResourceAction.class);
  private AWSIAMUserProperties properties = new AWSIAMUserProperties();
  private AWSIAMUserResourceInfo info = new AWSIAMUserResourceInfo();

  public AWSIAMUserResourceAction() {
    super(fromEnum(CreateSteps.class), fromEnum(DeleteSteps.class), fromUpdateEnum(UpdateNoInterruptionSteps.class), null);
    // In this case, update with replacement has a precondition check before essentially the same steps as "create".  We add both.
    Map<String, UpdateStep> updateWithReplacementMap = Maps.newLinkedHashMap();
    updateWithReplacementMap.putAll(fromUpdateEnum(UpdateWithReplacementPreCreateSteps.class));
    updateWithReplacementMap.putAll(createStepsToUpdateWithReplacementSteps(fromEnum(CreateSteps.class)));
    setUpdateSteps(UpdateTypeAndDirection.UPDATE_WITH_REPLACEMENT, updateWithReplacementMap);
  }

  private static final String DEFAULT_PATH = "/";
  @Override
  public UpdateType getUpdateType(ResourceAction resourceAction, boolean stackTagsChanged) {
    UpdateType updateType = info.supportsTags() && stackTagsChanged ? UpdateType.NO_INTERRUPTION : UpdateType.NONE;
    AWSIAMUserResourceAction otherAction = (AWSIAMUserResourceAction) resourceAction;
    if (!Objects.equals(properties.getPath(), otherAction.properties.getPath())) {
      updateType = UpdateType.max(updateType, UpdateType.NO_INTERRUPTION);
    }
    if (!Objects.equals(properties.getGroups(), otherAction.properties.getGroups())) {
      updateType = UpdateType.max(updateType, UpdateType.NO_INTERRUPTION);
    }
    if (!Objects.equals(properties.getLoginProfile(), otherAction.properties.getLoginProfile())) {
      updateType = UpdateType.max(updateType, UpdateType.NO_INTERRUPTION);
    }
    if (!Objects.equals(properties.getManagedPolicyArns(), otherAction.properties.getManagedPolicyArns())) {
      updateType = UpdateType.max(updateType, UpdateType.NO_INTERRUPTION);
    }
    if (!Objects.equals(properties.getPolicies(), otherAction.properties.getPolicies())) {
      updateType = UpdateType.max(updateType, UpdateType.NO_INTERRUPTION);
    }
    if (!Objects.equals(properties.getUserName(), otherAction.properties.getUserName())) {
      updateType = UpdateType.max(updateType, UpdateType.NEEDS_REPLACEMENT);
    }
    return updateType;
  }

  private enum CreateSteps implements Step {
    CREATE_USER {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        AWSIAMUserResourceAction action = (AWSIAMUserResourceAction) resourceAction;
        ServiceConfiguration configuration = Topology.lookup(Euare.class);
        String userName = action.properties.getUserName() != null ? action.properties.getUserName() : action.getDefaultPhysicalResourceId();
        CreateUserType createUserType = MessageHelper.createMessage(CreateUserType.class, action.info.getEffectiveUserId());
        createUserType.setUserName(userName);
        createUserType.setPath(MoreObjects.firstNonNull(action.properties.getPath(), DEFAULT_PATH));
        CreateUserResponseType createUserResponseType = AsyncRequests.<CreateUserType,CreateUserResponseType> sendSync(configuration, createUserType);
        String arn = createUserResponseType.getCreateUserResult().getUser().getArn();
        action.info.setPhysicalResourceId(userName);
        action.info.setCreatedEnoughToDelete(true);
        action.info.setArn(JsonHelper.getStringFromJsonNode(new TextNode(arn)));
        action.info.setReferenceValueJson(JsonHelper.getStringFromJsonNode(new TextNode(action.info.getPhysicalResourceId())));
        return action;
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
    },
    ADD_MANAGED_POLICIES {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        AWSIAMUserResourceAction action = (AWSIAMUserResourceAction) resourceAction;
        ServiceConfiguration configuration = Topology.lookup(Euare.class);
        if (action.properties.getPolicies() != null) {
          for (String managedPolicyArn: action.properties.getManagedPolicyArns()) {
            AttachUserPolicyType attachUserPolicyType = MessageHelper.createMessage(AttachUserPolicyType.class, action.info.getEffectiveUserId());
            attachUserPolicyType.setUserName(action.info.getPhysicalResourceId());
            attachUserPolicyType.setPolicyArn(managedPolicyArn);
            AsyncRequests.sendSync(configuration, attachUserPolicyType);
          }
        }
        return action;
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
    };

    @Nullable
    @Override
    public Integer getTimeout() {
      return null;
    }
  }

  private enum DeleteSteps implements Step {
    DELETE_USER {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        AWSIAMUserResourceAction action = (AWSIAMUserResourceAction) resourceAction;
        ServiceConfiguration configuration = Topology.lookup(Euare.class);
        if (!Boolean.TRUE.equals(action.info.getCreatedEnoughToDelete())) return action;

        // if no user, bye...
        if (!IAMHelper.userExists(configuration, action.info.getPhysicalResourceId(), action.info.getEffectiveUserId())) return action;

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
            if (Boolean.TRUE.equals(listGroupsForUserResponseType.getListGroupsForUserResult().getIsTruncated())) {
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

  private static Set<String> getPolicyNames(AWSIAMUserResourceAction action) {
    Set<String> policyNames = Sets.newLinkedHashSet();
    if (action.properties.getPolicies() != null) {
      for (EmbeddedIAMPolicy policy : action.properties.getPolicies()) {
        policyNames.add(policy.getPolicyName());
      }
    }
    return policyNames;
  }

  private static Set<String> getManagedPolicyArns(AWSIAMUserResourceAction action) {
    Set<String> managedPolicyArns = Sets.newHashSet();
    if (action != null && action.properties != null && action.properties.getManagedPolicyArns() != null) {
      managedPolicyArns.addAll(action.properties.getManagedPolicyArns());
    }
    return managedPolicyArns;
  }

  private static Set<String> getExistingManagedPolicyArns(AWSIAMUserResourceAction newAction) throws Exception {
    ServiceConfiguration configuration = Topology.lookup(Euare.class);
    ListAttachedUserPoliciesType listAttachedUserPoliciesType = MessageHelper.createMessage(ListAttachedUserPoliciesType.class, newAction.info.getEffectiveUserId());
    listAttachedUserPoliciesType.setUserName(newAction.info.getPhysicalResourceId());
    ListAttachedUserPoliciesResponseType listAttachedUserPoliciesResponseType = AsyncRequests.sendSync(configuration, listAttachedUserPoliciesType);
    Set<String> result = Sets.newHashSet();
    if (listAttachedUserPoliciesResponseType != null && listAttachedUserPoliciesResponseType.getListAttachedUserPoliciesResult() != null &&
      listAttachedUserPoliciesResponseType.getListAttachedUserPoliciesResult().getAttachedPolicies() != null) {
      for (AttachedPolicyType attachedPolicyType : listAttachedUserPoliciesResponseType.getListAttachedUserPoliciesResult().getAttachedPolicies()) {
        result.add(attachedPolicyType.getPolicyArn());
      }
    }
    return result;
  }

  private enum UpdateNoInterruptionSteps implements UpdateStep {
    UPDATE_USER {
      @Override
      public ResourceAction perform(ResourceAction oldResourceAction, ResourceAction newResourceAction) throws Exception {
        AWSIAMUserResourceAction oldAction = (AWSIAMUserResourceAction) oldResourceAction;
        AWSIAMUserResourceAction newAction = (AWSIAMUserResourceAction) newResourceAction;
        ServiceConfiguration configuration = Topology.lookup(Euare.class);
        String userName = newAction.info.getPhysicalResourceId();
        UpdateUserType updateUserType = MessageHelper.createMessage(UpdateUserType.class, newAction.info.getEffectiveUserId());
        updateUserType.setUserName(userName);
        updateUserType.setNewPath(MoreObjects.firstNonNull(newAction.properties.getPath(), DEFAULT_PATH));
        UpdateUserResponseType updateUserResponseType = AsyncRequests.<UpdateUserType,UpdateUserResponseType> sendSync(configuration, updateUserType);
        return newAction;
      }
    },
    UPDATE_LOGIN_PROFILE {
      @Override
      public ResourceAction perform(ResourceAction oldResourceAction, ResourceAction newResourceAction) throws Exception {
        AWSIAMUserResourceAction oldAction = (AWSIAMUserResourceAction) oldResourceAction;
        AWSIAMUserResourceAction newAction = (AWSIAMUserResourceAction) newResourceAction;
        ServiceConfiguration configuration = Topology.lookup(Euare.class);
        if (newAction.properties.getLoginProfile() != null) {
          UpdateLoginProfileType updateLoginProfileType = MessageHelper.createMessage(UpdateLoginProfileType.class, newAction.info.getEffectiveUserId());
          updateLoginProfileType.setPassword(newAction.properties.getLoginProfile().getPassword());
          updateLoginProfileType.setUserName(newAction.info.getPhysicalResourceId());
          try {
            AsyncRequests.<UpdateLoginProfileType, UpdateLoginProfileResponseType>sendSync(configuration, updateLoginProfileType);
          } catch ( final Exception e ) {
            final Optional<AsyncExceptions.AsyncWebServiceError> error = AsyncExceptions.asWebServiceError(e);
            if (error.isPresent() && Strings.nullToEmpty(error.get().getCode()).equals("NoSuchEntity")) {
              CreateLoginProfileType createLoginProfileType = MessageHelper.createMessage(CreateLoginProfileType.class, newAction.info.getEffectiveUserId());
              createLoginProfileType.setPassword(newAction.properties.getLoginProfile().getPassword());
              createLoginProfileType.setUserName(newAction.info.getPhysicalResourceId());
              AsyncRequests.<CreateLoginProfileType,CreateLoginProfileResponseType> sendSync(configuration, createLoginProfileType);
            } else throw e;
          }
        } else {
          DeleteLoginProfileType deleteLoginProfileType = MessageHelper.createMessage(DeleteLoginProfileType.class, newAction.info.getEffectiveUserId());
          deleteLoginProfileType.setUserName(newAction.info.getPhysicalResourceId());
          try {
            AsyncRequests.<DeleteLoginProfileType,DeleteLoginProfileResponseType> sendSync(configuration, deleteLoginProfileType);
          } catch ( final Exception e ) {
            final Optional<AsyncExceptions.AsyncWebServiceError> error = AsyncExceptions.asWebServiceError(e);
            if (error.isPresent() && Strings.nullToEmpty(error.get().getCode()).equals("NoSuchEntity")) {
              // we don't care.  (already deleted or never there)
            } else throw e;
          }
        }
        return newAction;
      }
    },
    UPDATE_POLICIES {
      public ResourceAction perform(ResourceAction oldResourceAction, ResourceAction newResourceAction) throws Exception {
        AWSIAMUserResourceAction oldAction = (AWSIAMUserResourceAction) oldResourceAction;
        AWSIAMUserResourceAction newAction = (AWSIAMUserResourceAction) newResourceAction;
        ServiceConfiguration configuration = Topology.lookup(Euare.class);
        Set<String> oldPolicyNames = getPolicyNames(oldAction);
        Set<String> newPolicyNames = getPolicyNames(newAction);
        if (newAction.properties.getPolicies() != null) {
          for (EmbeddedIAMPolicy policy: newAction.properties.getPolicies()) {
            PutUserPolicyType putUserPolicyType = MessageHelper.createMessage(PutUserPolicyType.class, newAction.info.getEffectiveUserId());
            putUserPolicyType.setUserName(newAction.info.getPhysicalResourceId());
            putUserPolicyType.setPolicyName(policy.getPolicyName());
            putUserPolicyType.setPolicyDocument(policy.getPolicyDocument().toString());
            AsyncRequests.<PutUserPolicyType,PutUserPolicyResponseType> sendSync(configuration, putUserPolicyType);
          }
        }
        // delete all the old policies not in the new set (deleting policies is idempotent thankfully)
        for (String oldPolicyName : Sets.difference(oldPolicyNames, newPolicyNames)) {
          DeleteUserPolicyType deleteUserPolicyType = MessageHelper.createMessage(DeleteUserPolicyType.class, newAction.info.getEffectiveUserId());
          deleteUserPolicyType.setUserName(newAction.info.getPhysicalResourceId());
          deleteUserPolicyType.setPolicyName(oldPolicyName);
          AsyncRequests.<DeleteUserPolicyType,DeleteUserPolicyResponseType> sendSync(configuration, deleteUserPolicyType);
        }
        return newAction;
      }
    },
    UPDATE_MANAGED_POLICIES {
      public ResourceAction perform(ResourceAction oldResourceAction, ResourceAction newResourceAction) throws Exception {
        AWSIAMUserResourceAction oldAction = (AWSIAMUserResourceAction) oldResourceAction;
        AWSIAMUserResourceAction newAction = (AWSIAMUserResourceAction) newResourceAction;
        ServiceConfiguration configuration = Topology.lookup(Euare.class);
        Set<String> oldManagedPolicyArns = getManagedPolicyArns(oldAction);
        Set<String> newManagedPolicyArns = getManagedPolicyArns(newAction);
        Set<String> existingManagedPolicyArns = getExistingManagedPolicyArns(newAction);
        // the policies to add are the new policies that are not old and not existing
        Set<String> managedPolicyArnsToAdd = Sets.difference(newManagedPolicyArns, Sets.union(oldManagedPolicyArns, existingManagedPolicyArns));
        for (String managedPolicyArn: managedPolicyArnsToAdd) {
          AttachUserPolicyType attachUserPolicyType = MessageHelper.createMessage(AttachUserPolicyType.class, newAction.info.getEffectiveUserId());
          attachUserPolicyType.setUserName(newAction.info.getPhysicalResourceId());
          attachUserPolicyType.setPolicyArn(managedPolicyArn);
          AsyncRequests.sendSync(configuration, attachUserPolicyType);
        }

        // the policies to remove from the resource are the old policies that are not new, but they must also exist.
        Set<String> managedPolicyArnsToRemove = Sets.difference(Sets.intersection(existingManagedPolicyArns, oldManagedPolicyArns), newManagedPolicyArns);
        for (String managedPolicyArn: managedPolicyArnsToRemove) {
          DetachUserPolicyType detachUserPolicyType = MessageHelper.createMessage(DetachUserPolicyType.class, newAction.info.getEffectiveUserId());
          detachUserPolicyType.setUserName(newAction.info.getPhysicalResourceId());
          detachUserPolicyType.setPolicyArn(managedPolicyArn);
          try {
            AsyncRequests.sendSync(configuration, detachUserPolicyType);
          } catch ( final Exception e ) {
            final Optional<AsyncExceptions.AsyncWebServiceError> error = AsyncExceptions.asWebServiceError(e);
            if (error.isPresent() && Strings.nullToEmpty(error.get().getCode()).equals("NoSuchEntity")) {
              // we don't care.  (already deleted or never there)
            } else throw e;
          }
        }
        return newAction;
      }
    },
    UPDATE_GROUPS {
      @Override
      public ResourceAction perform(ResourceAction oldResourceAction, ResourceAction newResourceAction) throws Exception {
        AWSIAMUserResourceAction oldAction = (AWSIAMUserResourceAction) oldResourceAction;
        AWSIAMUserResourceAction newAction = (AWSIAMUserResourceAction) newResourceAction;
        ServiceConfiguration configuration = Topology.lookup(Euare.class);
        Set<String> oldGroupNames = IAMHelper.collectionToSetAndNullToEmpty(oldAction.properties.getGroups());
        Set<String> newGroupNames = IAMHelper.collectionToSetAndNullToEmpty(newAction.properties.getGroups());
        // only add groups not already added (due to conflict error if double adding)
        Set<String> existingGroupsForUser = IAMHelper.getGroupNamesForUser(configuration, newAction.info.getPhysicalResourceId(), newAction.info.getEffectiveUserId());
        for (String groupName: Sets.difference(newGroupNames, existingGroupsForUser)) {
          AddUserToGroupType addUserToGroupType = MessageHelper.createMessage(AddUserToGroupType.class, newAction.info.getEffectiveUserId());
          addUserToGroupType.setGroupName(groupName);
          addUserToGroupType.setUserName(newAction.info.getPhysicalResourceId());
          AsyncRequests.<AddUserToGroupType,AddUserToGroupResponseType> sendSync(configuration, addUserToGroupType);
        }
        for (String groupName: Sets.difference(oldGroupNames, newGroupNames)) {
          RemoveUserFromGroupType removeUserFromGroupType = MessageHelper.createMessage(RemoveUserFromGroupType.class, oldAction.info.getEffectiveUserId());
          removeUserFromGroupType.setGroupName(groupName);
          removeUserFromGroupType.setUserName(oldAction.info.getPhysicalResourceId());
          try {
            AsyncRequests.<RemoveUserFromGroupType,RemoveUserFromGroupResponseType> sendSync(configuration, removeUserFromGroupType);
          } catch ( final Exception e ) {
            final Optional<AsyncExceptions.AsyncWebServiceError> error = AsyncExceptions.asWebServiceError(e);
            if (error.isPresent() && Strings.nullToEmpty(error.get().getCode()).equals("NoSuchEntity")) {
              // we don't care.  (already deleted or never there)
            } else throw e;
          }
        }
        return newAction;
      }
    },
    UPDATE_ARN {
      @Override
      public ResourceAction perform(ResourceAction oldResourceAction, ResourceAction newResourceAction) throws Exception {
        AWSIAMUserResourceAction oldAction = (AWSIAMUserResourceAction) oldResourceAction;
        AWSIAMUserResourceAction newAction = (AWSIAMUserResourceAction) newResourceAction;
        ServiceConfiguration configuration = Topology.lookup(Euare.class);
        String userName = newAction.info.getPhysicalResourceId();
        UserType user = IAMHelper.getUser(configuration, userName, newAction.info.getEffectiveUserId());
        newAction.info.setArn(JsonHelper.getStringFromJsonNode(new TextNode(user.getArn())));
        return newAction;
      }
    };


    @Nullable
    @Override
    public Integer getTimeout() {
      return null;
    }
  }

  private enum UpdateWithReplacementPreCreateSteps implements UpdateStep {
    CHECK_CHANGED_USER_NAME {
      @Override
      public ResourceAction perform(ResourceAction oldResourceAction, ResourceAction newResourceAction) throws Exception {
        AWSIAMUserResourceAction oldAction = (AWSIAMUserResourceAction) oldResourceAction;
        AWSIAMUserResourceAction newAction = (AWSIAMUserResourceAction) newResourceAction;
        if (Objects.equals(oldAction.properties.getUserName(), newAction.properties.getUserName()) && oldAction.properties.getUserName() != null) {
          throw new ValidationErrorException("CloudFormation cannot update a stack when a custom-named resource requires replacing. Rename "+oldAction.properties.getUserName()+" and update the stack again.");
        }
        return newAction;
      }

      @Nullable
      @Override
      public Integer getTimeout() {
        return null;
      }
    }
  }

}


