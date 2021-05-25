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


import com.eucalyptus.auth.euare.common.msgs.AttachGroupPolicyResponseType;
import com.eucalyptus.auth.euare.common.msgs.AttachGroupPolicyType;
import com.eucalyptus.auth.euare.common.msgs.AttachRolePolicyResponseType;
import com.eucalyptus.auth.euare.common.msgs.AttachRolePolicyType;
import com.eucalyptus.auth.euare.common.msgs.AttachUserPolicyResponseType;
import com.eucalyptus.auth.euare.common.msgs.AttachUserPolicyType;
import com.eucalyptus.auth.euare.common.msgs.CreatePolicyResponseType;
import com.eucalyptus.auth.euare.common.msgs.CreatePolicyType;
import com.eucalyptus.auth.euare.common.msgs.CreatePolicyVersionType;
import com.eucalyptus.auth.euare.common.msgs.DeletePolicyType;
import com.eucalyptus.auth.euare.common.msgs.DeletePolicyVersionType;
import com.eucalyptus.auth.euare.common.msgs.DetachGroupPolicyType;
import com.eucalyptus.auth.euare.common.msgs.DetachRolePolicyType;
import com.eucalyptus.auth.euare.common.msgs.DetachUserPolicyType;
import com.eucalyptus.auth.euare.common.msgs.GetPolicyResponseType;
import com.eucalyptus.auth.euare.common.msgs.GetPolicyType;
import com.eucalyptus.auth.euare.common.msgs.ListEntitiesForPolicyResponseType;
import com.eucalyptus.auth.euare.common.msgs.ListEntitiesForPolicyType;
import com.eucalyptus.auth.euare.common.msgs.PolicyGroup;
import com.eucalyptus.auth.euare.common.msgs.PolicyRole;
import com.eucalyptus.auth.euare.common.msgs.PolicyUser;
import com.eucalyptus.cloudformation.ValidationErrorException;
import com.eucalyptus.cloudformation.resources.IAMHelper;
import com.eucalyptus.cloudformation.resources.ResourceAction;
import com.eucalyptus.cloudformation.resources.ResourceInfo;
import com.eucalyptus.cloudformation.resources.ResourceProperties;
import com.eucalyptus.cloudformation.resources.standard.info.AWSIAMManagedPolicyResourceInfo;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.AWSIAMManagedPolicyProperties;
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
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Created by ethomas on 2/3/14.
 */
public class AWSIAMManagedPolicyResourceAction extends StepBasedResourceAction {

  private AWSIAMManagedPolicyProperties properties = new AWSIAMManagedPolicyProperties();
  private AWSIAMManagedPolicyResourceInfo info = new AWSIAMManagedPolicyResourceInfo();

  public AWSIAMManagedPolicyResourceAction() {
    super(fromEnum(CreateSteps.class), fromEnum(DeleteSteps.class), fromUpdateEnum(UpdateNoInterruptionSteps.class), null);
    Map<String, UpdateStep> updateWithReplacementMap = Maps.newLinkedHashMap();
    updateWithReplacementMap.putAll(fromUpdateEnum(UpdateWithReplacementPreCreateSteps.class));
    updateWithReplacementMap.putAll(createStepsToUpdateWithReplacementSteps(fromEnum(CreateSteps.class)));
    setUpdateSteps(UpdateTypeAndDirection.UPDATE_WITH_REPLACEMENT, updateWithReplacementMap);
  }

  @Override
  public UpdateType getUpdateType(ResourceAction resourceAction, boolean stackTagsChanged) {
    UpdateType updateType = info.supportsTags() && stackTagsChanged ? UpdateType.NO_INTERRUPTION : UpdateType.NONE;
    AWSIAMManagedPolicyResourceAction otherAction = (AWSIAMManagedPolicyResourceAction) resourceAction;
    if (!Objects.equals(properties.getDescription(), otherAction.properties.getDescription())) {
      updateType = UpdateType.max(updateType, UpdateType.NEEDS_REPLACEMENT);
    }
    if (!Objects.equals(properties.getGroups(), otherAction.properties.getGroups())) {
      updateType = UpdateType.max(updateType, UpdateType.NO_INTERRUPTION);
    }
    if (!Objects.equals(properties.getPath(), otherAction.properties.getPath())) {
      updateType = UpdateType.max(updateType, UpdateType.NEEDS_REPLACEMENT);
    }
    if (!Objects.equals(properties.getPolicyDocument(), otherAction.properties.getPolicyDocument())) {
      updateType = UpdateType.max(updateType, UpdateType.NO_INTERRUPTION);
    }
    if (!Objects.equals(properties.getRoles(), otherAction.properties.getRoles())) {
      updateType = UpdateType.max(updateType, UpdateType.NO_INTERRUPTION);
    }
    if (!Objects.equals(properties.getUsers(), otherAction.properties.getUsers())) {
      updateType = UpdateType.max(updateType, UpdateType.NO_INTERRUPTION);
    }
    if (!Objects.equals(properties.getManagedPolicyName(), otherAction.properties.getManagedPolicyName())) {
      updateType = UpdateType.max(updateType, UpdateType.NEEDS_REPLACEMENT);
    }
    return updateType;
  }

  private enum CreateSteps implements Step {
    CREATE_POLICY {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        AWSIAMManagedPolicyResourceAction action = (AWSIAMManagedPolicyResourceAction) resourceAction;
        ServiceConfiguration configuration = Topology.lookup(Euare.class);
        String policyName = action.properties.getManagedPolicyName() != null ? action.properties.getManagedPolicyName() : action.getDefaultPhysicalResourceId(128);
        CreatePolicyType createPolicyType = MessageHelper.createMessage(CreatePolicyType.class, action.info.getEffectiveUserId());
        if (action.properties.getDescription() != null) {
          createPolicyType.setDescription(action.properties.getDescription());
        }
        createPolicyType.setPath(MoreObjects.firstNonNull(action.properties.getPath(), "/"));
        createPolicyType.setPolicyDocument(action.properties.getPolicyDocument().toString());
        createPolicyType.setPolicyName(policyName);
        CreatePolicyResponseType createPolicyResponseType = AsyncRequests.sendSync(configuration, createPolicyType);
        String arn = createPolicyResponseType.getCreatePolicyResult().getPolicy().getArn();
        action.info.setPhysicalResourceId(createPolicyResponseType.getCreatePolicyResult().getPolicy().getArn());
        action.info.setCreatedEnoughToDelete(true);
        action.info.setReferenceValueJson(JsonHelper.getStringFromJsonNode(new TextNode(action.info.getPhysicalResourceId())));
        return action;
      }
    },
    ATTACH_TO_GROUPS {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        AWSIAMManagedPolicyResourceAction action = (AWSIAMManagedPolicyResourceAction) resourceAction;
        ServiceConfiguration configuration = Topology.lookup(Euare.class);
        if (action.properties.getGroups() != null) {
          for (String groupName: action.properties.getGroups()) {
            AttachGroupPolicyType attachGroupPolicyType = MessageHelper.createMessage(AttachGroupPolicyType.class, action.info.getEffectiveUserId());
            attachGroupPolicyType.setGroupName(groupName);
            attachGroupPolicyType.setPolicyArn(action.info.getPhysicalResourceId());
            AsyncRequests.<AttachGroupPolicyType,AttachGroupPolicyResponseType> sendSync(configuration, attachGroupPolicyType);
          }
        }
        return action;
      }
    },
    ATTACH_TO_USERS {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        AWSIAMManagedPolicyResourceAction action = (AWSIAMManagedPolicyResourceAction) resourceAction;
        ServiceConfiguration configuration = Topology.lookup(Euare.class);
        if (action.properties.getUsers() != null) {
          for (String userName: action.properties.getUsers()) {
            AttachUserPolicyType attachUserPolicyType = MessageHelper.createMessage(AttachUserPolicyType.class, action.info.getEffectiveUserId());
            attachUserPolicyType.setUserName(userName);
            attachUserPolicyType.setPolicyArn(action.info.getPhysicalResourceId());
            AsyncRequests.<AttachUserPolicyType,AttachUserPolicyResponseType> sendSync(configuration, attachUserPolicyType);
          }
        }
        return action;
      }
    },
    ATTACH_TO_ROLES {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        AWSIAMManagedPolicyResourceAction action = (AWSIAMManagedPolicyResourceAction) resourceAction;
        ServiceConfiguration configuration = Topology.lookup(Euare.class);
        if (action.properties.getRoles() != null) {
          for (String roleName: action.properties.getRoles()) {
            AttachRolePolicyType attachRolePolicyType = MessageHelper.createMessage(AttachRolePolicyType.class, action.info.getEffectiveUserId());
            attachRolePolicyType.setRoleName(roleName);
            attachRolePolicyType.setPolicyArn(action.info.getPhysicalResourceId());
            AsyncRequests.<AttachRolePolicyType,AttachRolePolicyResponseType> sendSync(configuration, attachRolePolicyType);
          }
        }
        return action;
      }
    }
  }

  private enum DeleteSteps implements Step {
    DELETE_POLICY {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        AWSIAMManagedPolicyResourceAction action = (AWSIAMManagedPolicyResourceAction) resourceAction;
        ServiceConfiguration configuration = Topology.lookup(Euare.class);
        if (!Boolean.TRUE.equals(action.info.getCreatedEnoughToDelete())) return action;

        // detach groups
        if (action.properties.getGroups() != null) {
          for (String group : action.properties.getGroups()) {
            DetachGroupPolicyType detachGroupPolicyType = MessageHelper.createMessage(DetachGroupPolicyType.class, action.info.getEffectiveUserId());
            detachGroupPolicyType.setGroupName(group);
            detachGroupPolicyType.setPolicyArn(action.info.getPhysicalResourceId());
            try {
              AsyncRequests.sendSync(configuration, detachGroupPolicyType);
            } catch (final Exception e) {
              final Optional<AsyncExceptions.AsyncWebServiceError> error = AsyncExceptions.asWebServiceError(e);
              if (error.isPresent() && Strings.nullToEmpty(error.get().getCode()).equals("NoSuchEntity")) {
                // we don't care.  (already deleted or never there)
              } else throw e;
            }
          }
        }

        // detach roles
        if (action.properties.getRoles() != null) {
          for (String role : action.properties.getRoles()) {
            DetachRolePolicyType detachRolePolicyType = MessageHelper.createMessage(DetachRolePolicyType.class, action.info.getEffectiveUserId());
            detachRolePolicyType.setRoleName(role);
            detachRolePolicyType.setPolicyArn(action.info.getPhysicalResourceId());
            try {
              AsyncRequests.sendSync(configuration, detachRolePolicyType);
            } catch (final Exception e) {
              final Optional<AsyncExceptions.AsyncWebServiceError> error = AsyncExceptions.asWebServiceError(e);
              if (error.isPresent() && Strings.nullToEmpty(error.get().getCode()).equals("NoSuchEntity")) {
                // we don't care.  (already deleted or never there)
              } else throw e;
            }
          }
        }

        // detach users
        if (action.properties.getUsers() != null) {
          for (String user : action.properties.getUsers()) {
            DetachUserPolicyType detachUserPolicyType = MessageHelper.createMessage(DetachUserPolicyType.class, action.info.getEffectiveUserId());
            detachUserPolicyType.setUserName(user);
            detachUserPolicyType.setPolicyArn(action.info.getPhysicalResourceId());
            try {
              AsyncRequests.sendSync(configuration, detachUserPolicyType);
            } catch (final Exception e) {
              final Optional<AsyncExceptions.AsyncWebServiceError> error = AsyncExceptions.asWebServiceError(e);
              if (error.isPresent() && Strings.nullToEmpty(error.get().getCode()).equals("NoSuchEntity")) {
                // we don't care.  (already deleted or never there)
              } else throw e;
            }
          }
        }

        // delete policy
        DeletePolicyType deletePolicyType = MessageHelper.createMessage(DeletePolicyType.class, action.info.getEffectiveUserId());
        deletePolicyType.setPolicyArn(action.info.getPhysicalResourceId());
        try {
          AsyncRequests.sendSync(configuration, deletePolicyType);
        } catch (final Exception e) {
          final Optional<AsyncExceptions.AsyncWebServiceError> error = AsyncExceptions.asWebServiceError(e);
          if (error.isPresent() && Strings.nullToEmpty(error.get().getCode()).equals("NoSuchEntity")) {
            // we don't care.  (already deleted or never there)
          } else throw e;
        }

        return action;
      }
    }
  }

  @Override
  public ResourceProperties getResourceProperties() {
    return properties;
  }

  @Override
  public void setResourceProperties(ResourceProperties resourceProperties) {
    properties = (AWSIAMManagedPolicyProperties) resourceProperties;
  }

  @Override
  public ResourceInfo getResourceInfo() {
    return info;
  }

  @Override
  public void setResourceInfo(ResourceInfo resourceInfo) {
    info = (AWSIAMManagedPolicyResourceInfo) resourceInfo;
  }

  private enum UpdateNoInterruptionSteps implements UpdateStep {
    UPDATE_ATTACHMENT_TO_GROUPS {
      @Override
      public ResourceAction perform(ResourceAction oldResourceAction, ResourceAction newResourceAction) throws Exception {
        AWSIAMManagedPolicyResourceAction oldAction = (AWSIAMManagedPolicyResourceAction) oldResourceAction;
        AWSIAMManagedPolicyResourceAction newAction = (AWSIAMManagedPolicyResourceAction) newResourceAction;
        ServiceConfiguration configuration = Topology.lookup(Euare.class);
        Set<String> oldGroupNames = IAMHelper.collectionToSetAndNullToEmpty(oldAction.properties.getGroups());
        Set<String> newGroupNames = IAMHelper.collectionToSetAndNullToEmpty(newAction.properties.getGroups());
        Set<String> existingGroupNames = getExistingAttachedGroups(newAction);
        // the groups to add are the new groups that are not old and not existing
        Set<String> groupNamesToAdd = Sets.difference(newGroupNames, Sets.union(oldGroupNames, existingGroupNames));
        for (String groupName: groupNamesToAdd) {
          AttachGroupPolicyType attachGroupPolicyType = MessageHelper.createMessage(AttachGroupPolicyType.class, newAction.info.getEffectiveUserId());
          attachGroupPolicyType.setGroupName(groupName);
          attachGroupPolicyType.setPolicyArn(newAction.info.getPhysicalResourceId());
          AsyncRequests.sendSync(configuration, attachGroupPolicyType);
        }

        // the groups to remove from the resource are the old groups that are not new, but they must also exist.
        Set<String> groupNamesToRemove = Sets.difference(Sets.intersection(existingGroupNames, oldGroupNames), newGroupNames);
        for (String groupName: groupNamesToRemove) {
          DetachGroupPolicyType detachGroupPolicyType = MessageHelper.createMessage(DetachGroupPolicyType.class, newAction.info.getEffectiveUserId());
          detachGroupPolicyType.setGroupName(groupName);
          detachGroupPolicyType.setPolicyArn(newAction.info.getPhysicalResourceId());
          try {
            AsyncRequests.sendSync(configuration, detachGroupPolicyType);
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
    UPDATE_ATTACHMENT_TO_USERS {
      @Override
      public ResourceAction perform(ResourceAction oldResourceAction, ResourceAction newResourceAction) throws Exception {
        AWSIAMManagedPolicyResourceAction oldAction = (AWSIAMManagedPolicyResourceAction) oldResourceAction;
        AWSIAMManagedPolicyResourceAction newAction = (AWSIAMManagedPolicyResourceAction) newResourceAction;
        ServiceConfiguration configuration = Topology.lookup(Euare.class);
        Set<String> oldUserNames = IAMHelper.collectionToSetAndNullToEmpty(oldAction.properties.getUsers());
        Set<String> newUserNames = IAMHelper.collectionToSetAndNullToEmpty(newAction.properties.getUsers());
        Set<String> existingUserNames = getExistingAttachedUsers(newAction);
        // the users to add are the new users that are not old and not existing
        Set<String> userNamesToAdd = Sets.difference(newUserNames, Sets.union(oldUserNames, existingUserNames));
        for (String userName: userNamesToAdd) {
          AttachUserPolicyType attachUserPolicyType = MessageHelper.createMessage(AttachUserPolicyType.class, newAction.info.getEffectiveUserId());
          attachUserPolicyType.setUserName(userName);
          attachUserPolicyType.setPolicyArn(newAction.info.getPhysicalResourceId());
          AsyncRequests.sendSync(configuration, attachUserPolicyType);
        }

        // the users to remove from the resource are the old users that are not new, but they must also exist.
        Set<String> userNamesToRemove = Sets.difference(Sets.intersection(existingUserNames, oldUserNames), newUserNames);
        for (String userName: userNamesToRemove) {
          DetachUserPolicyType detachUserPolicyType = MessageHelper.createMessage(DetachUserPolicyType.class, newAction.info.getEffectiveUserId());
          detachUserPolicyType.setUserName(userName);
          detachUserPolicyType.setPolicyArn(newAction.info.getPhysicalResourceId());
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
    UPDATE_ATTACHMENT_TO_ROLES {
      @Override
      public ResourceAction perform(ResourceAction oldResourceAction, ResourceAction newResourceAction) throws Exception {
        AWSIAMManagedPolicyResourceAction oldAction = (AWSIAMManagedPolicyResourceAction) oldResourceAction;
        AWSIAMManagedPolicyResourceAction newAction = (AWSIAMManagedPolicyResourceAction) newResourceAction;
        ServiceConfiguration configuration = Topology.lookup(Euare.class);
        Set<String> oldRoleNames = IAMHelper.collectionToSetAndNullToEmpty(oldAction.properties.getRoles());
        Set<String> newRoleNames = IAMHelper.collectionToSetAndNullToEmpty(newAction.properties.getRoles());
        Set<String> existingRoleNames = getExistingAttachedRoles(newAction);
        // the roles to add are the new roles that are not old and not existing
        Set<String> roleNamesToAdd = Sets.difference(newRoleNames, Sets.union(oldRoleNames, existingRoleNames));
        for (String roleName: roleNamesToAdd) {
          AttachRolePolicyType attachRolePolicyType = MessageHelper.createMessage(AttachRolePolicyType.class, newAction.info.getEffectiveUserId());
          attachRolePolicyType.setRoleName(roleName);
          attachRolePolicyType.setPolicyArn(newAction.info.getPhysicalResourceId());
          AsyncRequests.sendSync(configuration, attachRolePolicyType);
        }

        // the roles to remove from the resource are the old roles that are not new, but they must also exist.
        Set<String> roleNamesToRemove = Sets.difference(Sets.intersection(existingRoleNames, oldRoleNames), newRoleNames);
        for (String roleName: roleNamesToRemove) {
          DetachRolePolicyType detachRolePolicyType = MessageHelper.createMessage(DetachRolePolicyType.class, newAction.info.getEffectiveUserId());
          detachRolePolicyType.setRoleName(roleName);
          detachRolePolicyType.setPolicyArn(newAction.info.getPhysicalResourceId());
          try {
            AsyncRequests.sendSync(configuration, detachRolePolicyType);
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
    UPDATE_POLICY_DOCUMENT {
      @Override
      public ResourceAction perform(ResourceAction oldResourceAction, ResourceAction newResourceAction) throws Exception {
        AWSIAMManagedPolicyResourceAction oldAction = (AWSIAMManagedPolicyResourceAction) oldResourceAction;
        AWSIAMManagedPolicyResourceAction newAction = (AWSIAMManagedPolicyResourceAction) newResourceAction;
        ServiceConfiguration configuration = Topology.lookup(Euare.class);
        if (!Objects.equals(oldAction.properties.getPolicyDocument(), newAction.properties.getPolicyDocument())) {
          // get current version....
          GetPolicyType getPolicyType = MessageHelper.createMessage(GetPolicyType.class, newAction.info.getEffectiveUserId());
          getPolicyType.setPolicyArn(newAction.info.getPhysicalResourceId());
          GetPolicyResponseType getPolicyResponseType = AsyncRequests.sendSync(configuration, getPolicyType);
          String defaultPolicyVersionId = null;
          if (getPolicyResponseType != null && getPolicyResponseType.getGetPolicyResult() != null &&
            getPolicyResponseType.getGetPolicyResult().getPolicy() != null &&
            getPolicyResponseType.getGetPolicyResult().getPolicy().getDefaultVersionId() != null) {
            defaultPolicyVersionId = getPolicyResponseType.getGetPolicyResult().getPolicy().getDefaultVersionId();
          }
          if (defaultPolicyVersionId == null) {
            throw new ValidationErrorException("Unable to get default policy version id for managed policy " + newAction.info.getPhysicalResourceId());
          }
          // create new version
          CreatePolicyVersionType createPolicyVersionType = MessageHelper.createMessage(CreatePolicyVersionType.class, newAction.info.getEffectiveUserId());
          createPolicyVersionType.setPolicyArn(newAction.info.getPhysicalResourceId());
          createPolicyVersionType.setPolicyDocument(newAction.properties.getPolicyDocument().toString());
          createPolicyVersionType.setSetAsDefault(true);
          AsyncRequests.sendSync(configuration, createPolicyVersionType);
          // delete old version
          DeletePolicyVersionType deletePolicyVersionType = MessageHelper.createMessage(DeletePolicyVersionType.class, newAction.info.getEffectiveUserId());
          deletePolicyVersionType.setPolicyArn(newAction.info.getPhysicalResourceId());
          deletePolicyVersionType.setVersionId(defaultPolicyVersionId);
          AsyncRequests.sendSync(configuration, deletePolicyVersionType);
        }
        return newAction;
      }
    }
  }

  private enum UpdateWithReplacementPreCreateSteps implements UpdateStep {
    CHECK_CHANGED_MANAGED_POLICY_NAME {
      @Override
      public ResourceAction perform(ResourceAction oldResourceAction, ResourceAction newResourceAction) throws Exception {
        AWSIAMManagedPolicyResourceAction oldAction = (AWSIAMManagedPolicyResourceAction) oldResourceAction;
        AWSIAMManagedPolicyResourceAction newAction = (AWSIAMManagedPolicyResourceAction) newResourceAction;
        if (Objects.equals(oldAction.properties.getManagedPolicyName(), newAction.properties.getManagedPolicyName()) && oldAction.properties.getManagedPolicyName() != null) {
          throw new ValidationErrorException("CloudFormation cannot update a stack when a custom-named resource requires replacing. Rename "+oldAction.properties.getManagedPolicyName()+" and update the stack again.");
        }
        return newAction;
      }
    }
  }

  private static Set<String> getExistingAttachedGroups(AWSIAMManagedPolicyResourceAction action) throws Exception {
    ServiceConfiguration configuration = Topology.lookup(Euare.class);
    ListEntitiesForPolicyType listEntitiesForPolicyType = MessageHelper.createMessage(ListEntitiesForPolicyType.class, action.info.getEffectiveUserId());
    listEntitiesForPolicyType.setPolicyArn(action.info.getPhysicalResourceId());
    listEntitiesForPolicyType.setEntityFilter("Group");
    Set<String> values = Sets.newHashSet();
    ListEntitiesForPolicyResponseType listEntitiesForPolicyResponseType = AsyncRequests.sendSync(configuration, listEntitiesForPolicyType);
    if (listEntitiesForPolicyResponseType != null && listEntitiesForPolicyResponseType.getListEntitiesForPolicyResult() != null && listEntitiesForPolicyResponseType.getListEntitiesForPolicyResult().getPolicyGroups() != null) {
      for (PolicyGroup policyGroup: listEntitiesForPolicyResponseType.getListEntitiesForPolicyResult().getPolicyGroups()) {
        values.add(policyGroup.getGroupName());
      }
    }
    return values;
  }

  private static Set<String> getExistingAttachedRoles(AWSIAMManagedPolicyResourceAction action) throws Exception {
    ServiceConfiguration configuration = Topology.lookup(Euare.class);
    ListEntitiesForPolicyType listEntitiesForPolicyType = MessageHelper.createMessage(ListEntitiesForPolicyType.class, action.info.getEffectiveUserId());
    listEntitiesForPolicyType.setPolicyArn(action.info.getPhysicalResourceId());
    listEntitiesForPolicyType.setEntityFilter("Role");
    Set<String> values = Sets.newHashSet();
    ListEntitiesForPolicyResponseType listEntitiesForPolicyResponseType = AsyncRequests.sendSync(configuration, listEntitiesForPolicyType);
    if (listEntitiesForPolicyResponseType != null && listEntitiesForPolicyResponseType.getListEntitiesForPolicyResult() != null && listEntitiesForPolicyResponseType.getListEntitiesForPolicyResult().getPolicyRoles() != null) {
      for (PolicyRole policyRole: listEntitiesForPolicyResponseType.getListEntitiesForPolicyResult().getPolicyRoles()) {
        values.add(policyRole.getRoleName());
      }
    }
    return values;
  }

  private static Set<String> getExistingAttachedUsers(AWSIAMManagedPolicyResourceAction action) throws Exception {
    ServiceConfiguration configuration = Topology.lookup(Euare.class);
    ListEntitiesForPolicyType listEntitiesForPolicyType = MessageHelper.createMessage(ListEntitiesForPolicyType.class, action.info.getEffectiveUserId());
    listEntitiesForPolicyType.setPolicyArn(action.info.getPhysicalResourceId());
    listEntitiesForPolicyType.setEntityFilter("User");
    Set<String> values = Sets.newHashSet();
    ListEntitiesForPolicyResponseType listEntitiesForPolicyResponseType = AsyncRequests.sendSync(configuration, listEntitiesForPolicyType);
    if (listEntitiesForPolicyResponseType != null && listEntitiesForPolicyResponseType.getListEntitiesForPolicyResult() != null && listEntitiesForPolicyResponseType.getListEntitiesForPolicyResult().getPolicyUsers() != null) {
      for (PolicyUser policyUser : listEntitiesForPolicyResponseType.getListEntitiesForPolicyResult().getPolicyUsers()) {
        values.add(policyUser.getUserName());
      }
    }
    return values;
  }

}


