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


import com.eucalyptus.auth.euare.common.msgs.AttachGroupPolicyType;
import com.eucalyptus.auth.euare.common.msgs.AttachedPolicyType;
import com.eucalyptus.auth.euare.common.msgs.CreateGroupResponseType;
import com.eucalyptus.auth.euare.common.msgs.CreateGroupType;
import com.eucalyptus.auth.euare.common.msgs.DeleteGroupPolicyResponseType;
import com.eucalyptus.auth.euare.common.msgs.DeleteGroupPolicyType;
import com.eucalyptus.auth.euare.common.msgs.DeleteGroupResponseType;
import com.eucalyptus.auth.euare.common.msgs.DeleteGroupType;
import com.eucalyptus.auth.euare.common.msgs.DetachGroupPolicyType;
import com.eucalyptus.auth.euare.common.msgs.GroupType;
import com.eucalyptus.auth.euare.common.msgs.ListAttachedGroupPoliciesResponseType;
import com.eucalyptus.auth.euare.common.msgs.ListAttachedGroupPoliciesType;
import com.eucalyptus.auth.euare.common.msgs.PutGroupPolicyResponseType;
import com.eucalyptus.auth.euare.common.msgs.PutGroupPolicyType;
import com.eucalyptus.auth.euare.common.msgs.UpdateGroupResponseType;
import com.eucalyptus.auth.euare.common.msgs.UpdateGroupType;
import com.eucalyptus.cloudformation.ValidationErrorException;
import com.eucalyptus.cloudformation.resources.IAMHelper;
import com.eucalyptus.cloudformation.resources.ResourceAction;
import com.eucalyptus.cloudformation.resources.ResourceInfo;
import com.eucalyptus.cloudformation.resources.ResourceProperties;
import com.eucalyptus.cloudformation.resources.standard.info.AWSIAMGroupResourceInfo;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.AWSIAMGroupProperties;
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
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Created by ethomas on 2/3/14.
 */
public class AWSIAMGroupResourceAction extends StepBasedResourceAction {

  private AWSIAMGroupProperties properties = new AWSIAMGroupProperties();
  private AWSIAMGroupResourceInfo info = new AWSIAMGroupResourceInfo();

  public AWSIAMGroupResourceAction() {
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
    AWSIAMGroupResourceAction otherAction = (AWSIAMGroupResourceAction) resourceAction;
    if (!Objects.equals(properties.getPath(), otherAction.properties.getPath())) {
      updateType = UpdateType.max(updateType, UpdateType.NO_INTERRUPTION);
    }
    if (!Objects.equals(properties.getPolicies(), otherAction.properties.getPolicies())) {
      updateType = UpdateType.max(updateType, UpdateType.NO_INTERRUPTION);
    }
    if (!Objects.equals(properties.getManagedPolicyArns(), otherAction.properties.getManagedPolicyArns())) {
      updateType = UpdateType.max(updateType, UpdateType.NO_INTERRUPTION);
    }
    if (!Objects.equals(properties.getGroupName(), otherAction.properties.getGroupName())) {
      updateType = UpdateType.max(updateType, UpdateType.NEEDS_REPLACEMENT);
    }
    return updateType;
  }

  private enum CreateSteps implements Step {
    CREATE_GROUP {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        AWSIAMGroupResourceAction action = (AWSIAMGroupResourceAction) resourceAction;
        ServiceConfiguration configuration = Topology.lookup(Euare.class);
        String groupName = action.properties.getGroupName() != null ? action.properties.getGroupName() : action.getDefaultPhysicalResourceId(128);
        CreateGroupType createGroupType = MessageHelper.createMessage(CreateGroupType.class, action.info.getEffectiveUserId());
        createGroupType.setGroupName(groupName);
        createGroupType.setPath(MoreObjects.firstNonNull(action.properties.getPath(), DEFAULT_PATH));
        CreateGroupResponseType createGroupResponseType = AsyncRequests.<CreateGroupType,CreateGroupResponseType> sendSync(configuration, createGroupType);
        String arn = createGroupResponseType.getCreateGroupResult().getGroup().getArn();
        action.info.setPhysicalResourceId(groupName);
        action.info.setCreatedEnoughToDelete(true);
        action.info.setArn(JsonHelper.getStringFromJsonNode(new TextNode(arn)));
        action.info.setReferenceValueJson(JsonHelper.getStringFromJsonNode(new TextNode(action.info.getPhysicalResourceId())));
        return action;
      }
    },
    ADD_POLICIES {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        AWSIAMGroupResourceAction action = (AWSIAMGroupResourceAction) resourceAction;
        ServiceConfiguration configuration = Topology.lookup(Euare.class);
        if (action.properties.getPolicies() != null) {
          for (EmbeddedIAMPolicy policy: action.properties.getPolicies()) {
            PutGroupPolicyType putGroupPolicyType = MessageHelper.createMessage(PutGroupPolicyType.class, action.info.getEffectiveUserId());
            putGroupPolicyType.setGroupName(action.info.getPhysicalResourceId());
            putGroupPolicyType.setPolicyName(policy.getPolicyName());
            putGroupPolicyType.setPolicyDocument(policy.getPolicyDocument().toString());
            AsyncRequests.<PutGroupPolicyType,PutGroupPolicyResponseType> sendSync(configuration, putGroupPolicyType);
          }
        }
        return action;
      }
    },
    ADD_MANAGED_POLICIES {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        AWSIAMGroupResourceAction action = (AWSIAMGroupResourceAction) resourceAction;
        ServiceConfiguration configuration = Topology.lookup(Euare.class);
        if (action.properties.getPolicies() != null) {
          for (String managedPolicyArn: action.properties.getManagedPolicyArns()) {
            AttachGroupPolicyType attachGroupPolicyType = MessageHelper.createMessage(AttachGroupPolicyType.class, action.info.getEffectiveUserId());
            attachGroupPolicyType.setGroupName(action.info.getPhysicalResourceId());
            attachGroupPolicyType.setPolicyArn(managedPolicyArn);
            AsyncRequests.sendSync(configuration, attachGroupPolicyType);
          }
        }
        return action;
      }
    }
  }

  private enum DeleteSteps implements Step {
    DELETE_GROUP {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        AWSIAMGroupResourceAction action = (AWSIAMGroupResourceAction) resourceAction;
        ServiceConfiguration configuration = Topology.lookup(Euare.class);
        if (!Boolean.TRUE.equals(action.info.getCreatedEnoughToDelete())) return action;

        // if no group, bye...
        if (!IAMHelper.groupExists(configuration, action.info.getPhysicalResourceId(), action.info.getEffectiveUserId())) {
          return action;
        }
        // remove all policies added by us.  (Note: this could cause issues if an admin added some, but we delete what we create)
        // Note: deleting a non-existing policy doesn't do anything so we just delete them all...
        if (action.properties.getPolicies() != null) {
          for (EmbeddedIAMPolicy policy: action.properties.getPolicies()) {
            DeleteGroupPolicyType deleteGroupPolicyType = MessageHelper.createMessage(DeleteGroupPolicyType.class, action.info.getEffectiveUserId());
            deleteGroupPolicyType.setGroupName(action.info.getPhysicalResourceId());
            deleteGroupPolicyType.setPolicyName(policy.getPolicyName());
            AsyncRequests.<DeleteGroupPolicyType,DeleteGroupPolicyResponseType> sendSync(configuration, deleteGroupPolicyType);
          }
        }
        DeleteGroupType deleteGroupType = MessageHelper.createMessage(DeleteGroupType.class, action.info.getEffectiveUserId());
        deleteGroupType.setGroupName(action.info.getPhysicalResourceId());
        AsyncRequests.<DeleteGroupType,DeleteGroupResponseType> sendSync(configuration, deleteGroupType);
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
    properties = (AWSIAMGroupProperties) resourceProperties;
  }

  @Override
  public ResourceInfo getResourceInfo() {
    return info;
  }

  @Override
  public void setResourceInfo(ResourceInfo resourceInfo) {
    info = (AWSIAMGroupResourceInfo) resourceInfo;
  }

  private static Set<String> getManagedPolicyArns(AWSIAMGroupResourceAction action) {
    Set<String> managedPolicyArns = Sets.newHashSet();
    if (action != null && action.properties != null && action.properties.getManagedPolicyArns() != null) {
      managedPolicyArns.addAll(action.properties.getManagedPolicyArns());
    }
    return managedPolicyArns;
  }

  private static Set<String> getExistingManagedPolicyArns(AWSIAMGroupResourceAction newAction) throws Exception {
    ServiceConfiguration configuration = Topology.lookup(Euare.class);
    ListAttachedGroupPoliciesType listAttachedGroupPoliciesType = MessageHelper.createMessage(ListAttachedGroupPoliciesType.class, newAction.info.getEffectiveUserId());
    listAttachedGroupPoliciesType.setGroupName(newAction.info.getPhysicalResourceId());
    ListAttachedGroupPoliciesResponseType listAttachedGroupPoliciesResponseType = AsyncRequests.sendSync(configuration, listAttachedGroupPoliciesType);
    Set<String> result = Sets.newHashSet();
    if (listAttachedGroupPoliciesResponseType != null && listAttachedGroupPoliciesResponseType.getListAttachedGroupPoliciesResult() != null &&
      listAttachedGroupPoliciesResponseType.getListAttachedGroupPoliciesResult().getAttachedPolicies() != null) {
      for (AttachedPolicyType attachedPolicyType : listAttachedGroupPoliciesResponseType.getListAttachedGroupPoliciesResult().getAttachedPolicies()) {
        result.add(attachedPolicyType.getPolicyArn());
      }
    }
    return result;
  }

  private enum UpdateNoInterruptionSteps implements UpdateStep {
    UPDATE_GROUP {
      @Override
      public ResourceAction perform(ResourceAction oldResourceAction, ResourceAction newResourceAction) throws Exception {
        AWSIAMGroupResourceAction oldAction = (AWSIAMGroupResourceAction) oldResourceAction;
        AWSIAMGroupResourceAction newAction = (AWSIAMGroupResourceAction) newResourceAction;
        ServiceConfiguration configuration = Topology.lookup(Euare.class);
        String groupName = newAction.info.getPhysicalResourceId();
        UpdateGroupType updateGroupType = MessageHelper.createMessage(UpdateGroupType.class, newAction.info.getEffectiveUserId());
        updateGroupType.setGroupName(groupName);
        updateGroupType.setNewPath(MoreObjects.firstNonNull(newAction.properties.getPath(), DEFAULT_PATH));
        AsyncRequests.<UpdateGroupType, UpdateGroupResponseType>sendSync(configuration, updateGroupType);
        return newAction;
      }
    },
    UPDATE_ARN {
      @Override
      public ResourceAction perform(ResourceAction oldResourceAction, ResourceAction newResourceAction) throws Exception {
        AWSIAMGroupResourceAction oldAction = (AWSIAMGroupResourceAction) oldResourceAction;
        AWSIAMGroupResourceAction newAction = (AWSIAMGroupResourceAction) newResourceAction;
        ServiceConfiguration configuration = Topology.lookup(Euare.class);
        String groupName = newAction.info.getPhysicalResourceId();
        GroupType group = IAMHelper.getGroup(configuration, groupName, newAction.info.getEffectiveUserId());
        newAction.info.setArn(JsonHelper.getStringFromJsonNode(new TextNode(group.getArn())));
        return newAction;
      }
    },
    UPDATE_POLICIES {
      public ResourceAction perform(ResourceAction oldResourceAction, ResourceAction newResourceAction) throws Exception {
        AWSIAMGroupResourceAction oldAction = (AWSIAMGroupResourceAction) oldResourceAction;
        AWSIAMGroupResourceAction newAction = (AWSIAMGroupResourceAction) newResourceAction;
        ServiceConfiguration configuration = Topology.lookup(Euare.class);
        Set<String> oldPolicyNames = IAMHelper.getPolicyNames(oldAction.properties.getPolicies());
        Set<String> newPolicyNames = IAMHelper.getPolicyNames(newAction.properties.getPolicies());
        if (newAction.properties.getPolicies() != null) {
          for (EmbeddedIAMPolicy policy : newAction.properties.getPolicies()) {
            PutGroupPolicyType putGroupPolicyType = MessageHelper.createMessage(PutGroupPolicyType.class, newAction.info.getEffectiveUserId());
            putGroupPolicyType.setGroupName(newAction.info.getPhysicalResourceId());
            putGroupPolicyType.setPolicyName(policy.getPolicyName());
            putGroupPolicyType.setPolicyDocument(policy.getPolicyDocument().toString());
            AsyncRequests.<PutGroupPolicyType, PutGroupPolicyResponseType>sendSync(configuration, putGroupPolicyType);
          }
        }
        // delete all the old policies not in the new set (remember deleting policies that don't exist doesn't do anything)
        for (String oldPolicyName : Sets.difference(oldPolicyNames, newPolicyNames)) {
          DeleteGroupPolicyType deleteGroupPolicyType = MessageHelper.createMessage(DeleteGroupPolicyType.class, newAction.info.getEffectiveUserId());
          deleteGroupPolicyType.setGroupName(newAction.info.getPhysicalResourceId());
          deleteGroupPolicyType.setPolicyName(oldPolicyName);
          AsyncRequests.<DeleteGroupPolicyType, DeleteGroupPolicyResponseType>sendSync(configuration, deleteGroupPolicyType);
        }
        return newAction;
      }
    },
    UPDATE_MANAGED_POLICIES {
      public ResourceAction perform(ResourceAction oldResourceAction, ResourceAction newResourceAction) throws Exception {
        AWSIAMGroupResourceAction oldAction = (AWSIAMGroupResourceAction) oldResourceAction;
        AWSIAMGroupResourceAction newAction = (AWSIAMGroupResourceAction) newResourceAction;
        ServiceConfiguration configuration = Topology.lookup(Euare.class);
        Set<String> oldManagedPolicyArns = getManagedPolicyArns(oldAction);
        Set<String> newManagedPolicyArns = getManagedPolicyArns(newAction);
        Set<String> existingManagedPolicyArns = getExistingManagedPolicyArns(newAction);
        // the policies to add are the new policies that are not old and not existing
        Set<String> managedPolicyArnsToAdd = Sets.difference(newManagedPolicyArns, Sets.union(oldManagedPolicyArns, existingManagedPolicyArns));
        for (String managedPolicyArn: managedPolicyArnsToAdd) {
          AttachGroupPolicyType attachGroupPolicyType = MessageHelper.createMessage(AttachGroupPolicyType.class, newAction.info.getEffectiveUserId());
          attachGroupPolicyType.setGroupName(newAction.info.getPhysicalResourceId());
          attachGroupPolicyType.setPolicyArn(managedPolicyArn);
          AsyncRequests.sendSync(configuration, attachGroupPolicyType);
        }

        // the policies to remove from the resource are the old policies that are not new, but they must also exist.
        Set<String> managedPolicyArnsToRemove = Sets.difference(Sets.intersection(existingManagedPolicyArns, oldManagedPolicyArns), newManagedPolicyArns);
        for (String managedPolicyArn: managedPolicyArnsToRemove) {
          DetachGroupPolicyType detachGroupPolicyType = MessageHelper.createMessage(DetachGroupPolicyType.class, newAction.info.getEffectiveUserId());
          detachGroupPolicyType.setGroupName(newAction.info.getPhysicalResourceId());
          detachGroupPolicyType.setPolicyArn(managedPolicyArn);
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
    }
  }

  private enum UpdateWithReplacementPreCreateSteps implements UpdateStep {
    CHECK_CHANGED_GROUP_NAME {
      @Override
      public ResourceAction perform(ResourceAction oldResourceAction, ResourceAction newResourceAction) throws Exception {
        AWSIAMGroupResourceAction oldAction = (AWSIAMGroupResourceAction) oldResourceAction;
        AWSIAMGroupResourceAction newAction = (AWSIAMGroupResourceAction) newResourceAction;
        if (Objects.equals(oldAction.properties.getGroupName(), newAction.properties.getGroupName()) && oldAction.properties.getGroupName() != null) {
          throw new ValidationErrorException("CloudFormation cannot update a stack when a custom-named resource requires replacing. Rename "+oldAction.properties.getGroupName()+" and update the stack again.");
        }
        return newAction;
      }
    }
  }
}


