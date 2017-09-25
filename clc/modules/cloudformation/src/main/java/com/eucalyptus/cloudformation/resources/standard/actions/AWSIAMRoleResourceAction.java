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


import com.eucalyptus.auth.euare.common.msgs.AttachRolePolicyType;
import com.eucalyptus.auth.euare.common.msgs.AttachedPolicyType;
import com.eucalyptus.auth.euare.common.msgs.CreateRoleResponseType;
import com.eucalyptus.auth.euare.common.msgs.CreateRoleType;
import com.eucalyptus.auth.euare.common.msgs.DeleteRolePolicyResponseType;
import com.eucalyptus.auth.euare.common.msgs.DeleteRolePolicyType;
import com.eucalyptus.auth.euare.common.msgs.DeleteRoleResponseType;
import com.eucalyptus.auth.euare.common.msgs.DeleteRoleType;
import com.eucalyptus.auth.euare.common.msgs.DetachRolePolicyType;
import com.eucalyptus.auth.euare.common.msgs.ListAttachedRolePoliciesResponseType;
import com.eucalyptus.auth.euare.common.msgs.ListAttachedRolePoliciesType;
import com.eucalyptus.auth.euare.common.msgs.PutRolePolicyResponseType;
import com.eucalyptus.auth.euare.common.msgs.PutRolePolicyType;
import com.eucalyptus.auth.euare.common.msgs.UpdateAssumeRolePolicyResponseType;
import com.eucalyptus.auth.euare.common.msgs.UpdateAssumeRolePolicyType;
import com.eucalyptus.cloudformation.ValidationErrorException;
import com.eucalyptus.cloudformation.resources.IAMHelper;
import com.eucalyptus.cloudformation.resources.ResourceAction;
import com.eucalyptus.cloudformation.resources.ResourceInfo;
import com.eucalyptus.cloudformation.resources.ResourceProperties;
import com.eucalyptus.cloudformation.resources.standard.info.AWSIAMRoleResourceInfo;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.AWSIAMRoleProperties;
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
public class AWSIAMRoleResourceAction extends StepBasedResourceAction {

  private AWSIAMRoleProperties properties = new AWSIAMRoleProperties();
  private AWSIAMRoleResourceInfo info = new AWSIAMRoleResourceInfo();

  public AWSIAMRoleResourceAction() {
    super(fromEnum(CreateSteps.class), fromEnum(DeleteSteps.class), fromUpdateEnum(UpdateNoInterruptionSteps.class), null);
    // In this case, update with replacement has a precondition check before essentially the same steps as "create".  We add both.
    Map<String, UpdateStep> updateWithReplacementMap = Maps.newLinkedHashMap();
    updateWithReplacementMap.putAll(fromUpdateEnum(UpdateWithReplacementPreCreateSteps.class));
    updateWithReplacementMap.putAll(createStepsToUpdateWithReplacementSteps(fromEnum(CreateSteps.class)));
    setUpdateSteps(UpdateTypeAndDirection.UPDATE_WITH_REPLACEMENT, updateWithReplacementMap);
  }

  @Override
  public UpdateType getUpdateType(ResourceAction resourceAction, boolean stackTagsChanged) {
    UpdateType updateType = info.supportsTags() && stackTagsChanged ? UpdateType.NO_INTERRUPTION : UpdateType.NONE;
    AWSIAMRoleResourceAction otherAction = (AWSIAMRoleResourceAction) resourceAction;
    if (!Objects.equals(properties.getAssumeRolePolicyDocument(), otherAction.properties.getAssumeRolePolicyDocument())) {
      updateType = UpdateType.max(updateType, UpdateType.NO_INTERRUPTION);
    }
    if (!Objects.equals(properties.getPath(), otherAction.properties.getPath())) {
      updateType = UpdateType.max(updateType, UpdateType.NEEDS_REPLACEMENT);
    }
    if (!Objects.equals(properties.getManagedPolicyArns(), otherAction.properties.getManagedPolicyArns())) {
      updateType = UpdateType.max(updateType, UpdateType.NO_INTERRUPTION);
    }
    if (!Objects.equals(properties.getPolicies(), otherAction.properties.getPolicies())) {
      updateType = UpdateType.max(updateType, UpdateType.NO_INTERRUPTION);
    }
    if (!Objects.equals(properties.getRoleName(), otherAction.properties.getRoleName())) {
      updateType = UpdateType.max(updateType, UpdateType.NEEDS_REPLACEMENT);
    }
    return updateType;
  }

  private enum CreateSteps implements Step {
    CREATE_ROLE {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        AWSIAMRoleResourceAction action = (AWSIAMRoleResourceAction) resourceAction;
        ServiceConfiguration configuration = Topology.lookup(Euare.class);
        String roleName = action.properties.getRoleName() != null ? action.properties.getRoleName() : action.getDefaultPhysicalResourceId();
        CreateRoleType createRoleType = MessageHelper.createMessage(CreateRoleType.class, action.info.getEffectiveUserId());
        createRoleType.setRoleName(roleName);
        createRoleType.setPath(action.properties.getPath());
        createRoleType.setAssumeRolePolicyDocument(action.properties.getAssumeRolePolicyDocument().toString());
        CreateRoleResponseType createRoleResponseType = AsyncRequests.<CreateRoleType,CreateRoleResponseType> sendSync(configuration, createRoleType);
        String arn = createRoleResponseType.getCreateRoleResult().getRole().getArn();
        action.info.setPhysicalResourceId(roleName);
        action.info.setCreatedEnoughToDelete(true);
        action.info.setArn(JsonHelper.getStringFromJsonNode(new TextNode(arn)));
        action.info.setReferenceValueJson(JsonHelper.getStringFromJsonNode(new TextNode(action.info.getPhysicalResourceId())));
        return action;
      }
    },
    ADD_POLICIES {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        AWSIAMRoleResourceAction action = (AWSIAMRoleResourceAction) resourceAction;
        ServiceConfiguration configuration = Topology.lookup(Euare.class);
        if (action.properties.getPolicies() != null) {
          for (EmbeddedIAMPolicy policy: action.properties.getPolicies()) {
            PutRolePolicyType putRolePolicyType = MessageHelper.createMessage(PutRolePolicyType.class, action.info.getEffectiveUserId());
            putRolePolicyType.setRoleName(action.info.getPhysicalResourceId());
            putRolePolicyType.setPolicyName(policy.getPolicyName());
            putRolePolicyType.setPolicyDocument(policy.getPolicyDocument().toString());
            AsyncRequests.<PutRolePolicyType,PutRolePolicyResponseType> sendSync(configuration, putRolePolicyType);
          }
        }
        return action;
      }
    },
    ADD_MANAGED_POLICIES {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        AWSIAMRoleResourceAction action = (AWSIAMRoleResourceAction) resourceAction;
        ServiceConfiguration configuration = Topology.lookup(Euare.class);
        if (action.properties.getPolicies() != null) {
          for (String managedPolicyArn: action.properties.getManagedPolicyArns()) {
            AttachRolePolicyType attachRolePolicyType = MessageHelper.createMessage(AttachRolePolicyType.class, action.info.getEffectiveUserId());
            attachRolePolicyType.setRoleName(action.info.getPhysicalResourceId());
            attachRolePolicyType.setPolicyArn(managedPolicyArn);
            AsyncRequests.sendSync(configuration, attachRolePolicyType);
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
    DELETE_ROLE {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        AWSIAMRoleResourceAction action = (AWSIAMRoleResourceAction) resourceAction;
        ServiceConfiguration configuration = Topology.lookup(Euare.class);
        if (!Boolean.TRUE.equals(action.info.getCreatedEnoughToDelete())) return action;

        // if no role, bye...
        if (!IAMHelper.roleExists(configuration, action.info.getPhysicalResourceId(), action.info.getEffectiveUserId())) return action;
        // remove all policies added by us.  (Note: this could cause issues if an admin added some, but we delete what we create)
        // Note: deleting a non-existing policy doesn't do anything so we just delete them all...
        if (action.properties.getPolicies() != null) {
          for (EmbeddedIAMPolicy policy: action.properties.getPolicies()) {
            DeleteRolePolicyType deleteRolePolicyType = MessageHelper.createMessage(DeleteRolePolicyType.class, action.info.getEffectiveUserId());
            deleteRolePolicyType.setRoleName(action.info.getPhysicalResourceId());
            deleteRolePolicyType.setPolicyName(policy.getPolicyName());
            AsyncRequests.<DeleteRolePolicyType,DeleteRolePolicyResponseType> sendSync(configuration, deleteRolePolicyType);
          }
        }
        DeleteRoleType deleteRoleType = MessageHelper.createMessage(DeleteRoleType.class, action.info.getEffectiveUserId());
        deleteRoleType.setRoleName(action.info.getPhysicalResourceId());
        AsyncRequests.<DeleteRoleType,DeleteRoleResponseType> sendSync(configuration, deleteRoleType);
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
    properties = (AWSIAMRoleProperties) resourceProperties;
  }

  @Override
  public ResourceInfo getResourceInfo() {
    return info;
  }

  @Override
  public void setResourceInfo(ResourceInfo resourceInfo) {
    info = (AWSIAMRoleResourceInfo) resourceInfo;
  }

  private static Set<String> getPolicyNames(AWSIAMRoleResourceAction action) {
    Set<String> policyNames = Sets.newLinkedHashSet();
    if (action.properties.getPolicies() != null) {
      for (EmbeddedIAMPolicy policy : action.properties.getPolicies()) {
        policyNames.add(policy.getPolicyName());
      }
    }
    return policyNames;
  }

  private static Set<String> getManagedPolicyArns(AWSIAMRoleResourceAction action) {
    Set<String> managedPolicyArns = Sets.newHashSet();
    if (action != null && action.properties != null && action.properties.getManagedPolicyArns() != null) {
      managedPolicyArns.addAll(action.properties.getManagedPolicyArns());
    }
    return managedPolicyArns;
  }

  private static Set<String> getExistingManagedPolicyArns(AWSIAMRoleResourceAction newAction) throws Exception {
    ServiceConfiguration configuration = Topology.lookup(Euare.class);
    ListAttachedRolePoliciesType listAttachedRolePoliciesType = MessageHelper.createMessage(ListAttachedRolePoliciesType.class, newAction.info.getEffectiveUserId());
    listAttachedRolePoliciesType.setRoleName(newAction.info.getPhysicalResourceId());
    ListAttachedRolePoliciesResponseType listAttachedRolePoliciesResponseType = AsyncRequests.sendSync(configuration, listAttachedRolePoliciesType);
    Set<String> result = Sets.newHashSet();
    if (listAttachedRolePoliciesResponseType != null && listAttachedRolePoliciesResponseType.getListAttachedRolePoliciesResult() != null &&
      listAttachedRolePoliciesResponseType.getListAttachedRolePoliciesResult().getAttachedPolicies() != null) {
      for (AttachedPolicyType attachedPolicyType : listAttachedRolePoliciesResponseType.getListAttachedRolePoliciesResult().getAttachedPolicies()) {
        result.add(attachedPolicyType.getPolicyArn());
      }
    }
    return result;
  }


  private enum UpdateNoInterruptionSteps implements UpdateStep {
    UPDATE_ASSUME_ROLE_DOCUMENT {
      @Override
      public ResourceAction perform(ResourceAction oldResourceAction, ResourceAction newResourceAction) throws Exception {
        AWSIAMRoleResourceAction oldAction = (AWSIAMRoleResourceAction) oldResourceAction;
        AWSIAMRoleResourceAction newAction = (AWSIAMRoleResourceAction) newResourceAction;
        ServiceConfiguration configuration = Topology.lookup(Euare.class);
        UpdateAssumeRolePolicyType updateAssumeRolePolicyType = MessageHelper.createMessage(UpdateAssumeRolePolicyType.class, newAction.info.getEffectiveUserId());
        updateAssumeRolePolicyType.setRoleName(newAction.info.getPhysicalResourceId());
        updateAssumeRolePolicyType.setPolicyDocument(newAction.properties.getAssumeRolePolicyDocument().toString());
        AsyncRequests.<UpdateAssumeRolePolicyType,UpdateAssumeRolePolicyResponseType> sendSync(configuration, updateAssumeRolePolicyType);
        return newAction;
      }
    },
    UPDATE_POLICIES {
      public ResourceAction perform(ResourceAction oldResourceAction, ResourceAction newResourceAction) throws Exception {
        AWSIAMRoleResourceAction oldAction = (AWSIAMRoleResourceAction) oldResourceAction;
        AWSIAMRoleResourceAction newAction = (AWSIAMRoleResourceAction) newResourceAction;
        ServiceConfiguration configuration = Topology.lookup(Euare.class);
        Set<String> oldPolicyNames = getPolicyNames(oldAction);
        Set<String> newPolicyNames = getPolicyNames(newAction);
        if (newAction.properties.getPolicies() != null) {
          for (EmbeddedIAMPolicy policy: newAction.properties.getPolicies()) {
            PutRolePolicyType putRolePolicyType = MessageHelper.createMessage(PutRolePolicyType.class, newAction.info.getEffectiveUserId());
            putRolePolicyType.setRoleName(newAction.info.getPhysicalResourceId());
            putRolePolicyType.setPolicyName(policy.getPolicyName());
            putRolePolicyType.setPolicyDocument(policy.getPolicyDocument().toString());
            AsyncRequests.<PutRolePolicyType,PutRolePolicyResponseType> sendSync(configuration, putRolePolicyType);
          }
        }
        // delete all the old policies not in the new set
        // Note: deleting a non-existing policy doesn't do anything so we just delete them all...
        for (String oldPolicyName : Sets.difference(oldPolicyNames, newPolicyNames)) {
          DeleteRolePolicyType deleteRolePolicyType = MessageHelper.createMessage(DeleteRolePolicyType.class, newAction.info.getEffectiveUserId());
          deleteRolePolicyType.setRoleName(newAction.info.getPhysicalResourceId());
          deleteRolePolicyType.setPolicyName(oldPolicyName);
          AsyncRequests.<DeleteRolePolicyType,DeleteRolePolicyResponseType> sendSync(configuration, deleteRolePolicyType);
        }
        return newAction;
      }
    },
    UPDATE_MANAGED_POLICIES {
      public ResourceAction perform(ResourceAction oldResourceAction, ResourceAction newResourceAction) throws Exception {
        AWSIAMRoleResourceAction oldAction = (AWSIAMRoleResourceAction) oldResourceAction;
        AWSIAMRoleResourceAction newAction = (AWSIAMRoleResourceAction) newResourceAction;
        ServiceConfiguration configuration = Topology.lookup(Euare.class);
        Set<String> oldManagedPolicyArns = getManagedPolicyArns(oldAction);
        Set<String> newManagedPolicyArns = getManagedPolicyArns(newAction);
        Set<String> existingManagedPolicyArns = getExistingManagedPolicyArns(newAction);
        // the policies to add are the new policies that are not old and not existing
        Set<String> managedPolicyArnsToAdd = Sets.difference(newManagedPolicyArns, Sets.union(oldManagedPolicyArns, existingManagedPolicyArns));
        for (String managedPolicyArn: managedPolicyArnsToAdd) {
          AttachRolePolicyType attachRolePolicyType = MessageHelper.createMessage(AttachRolePolicyType.class, newAction.info.getEffectiveUserId());
          attachRolePolicyType.setRoleName(newAction.info.getPhysicalResourceId());
          attachRolePolicyType.setPolicyArn(managedPolicyArn);
          AsyncRequests.sendSync(configuration, attachRolePolicyType);
        }

        // the policies to remove from the resource are the old policies that are not new, but they must also exist.
        Set<String> managedPolicyArnsToRemove = Sets.difference(Sets.intersection(existingManagedPolicyArns, oldManagedPolicyArns), newManagedPolicyArns);
        for (String managedPolicyArn: managedPolicyArnsToRemove) {
          DetachRolePolicyType detachRolePolicyType = MessageHelper.createMessage(DetachRolePolicyType.class, newAction.info.getEffectiveUserId());
          detachRolePolicyType.setRoleName(newAction.info.getPhysicalResourceId());
          detachRolePolicyType.setPolicyArn(managedPolicyArn);
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
    };

    @Nullable
    @Override
    public Integer getTimeout() {
      return null;
    }
  }

  private enum UpdateWithReplacementPreCreateSteps implements UpdateStep {
    CHECK_CHANGED_ROLE_NAME {
      @Override
      public ResourceAction perform(ResourceAction oldResourceAction, ResourceAction newResourceAction) throws Exception {
        AWSIAMRoleResourceAction oldAction = (AWSIAMRoleResourceAction) oldResourceAction;
        AWSIAMRoleResourceAction newAction = (AWSIAMRoleResourceAction) newResourceAction;
        if (Objects.equals(oldAction.properties.getRoleName(), newAction.properties.getRoleName()) && oldAction.properties.getRoleName() != null) {
          throw new ValidationErrorException("CloudFormation cannot update a stack when a custom-named resource requires replacing. Rename "+oldAction.properties.getRoleName()+" and update the stack again.");
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


