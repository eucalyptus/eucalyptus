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


import com.eucalyptus.auth.euare.CreateRoleResponseType;
import com.eucalyptus.auth.euare.CreateRoleType;
import com.eucalyptus.auth.euare.DeleteRolePolicyResponseType;
import com.eucalyptus.auth.euare.DeleteRolePolicyType;
import com.eucalyptus.auth.euare.DeleteRoleResponseType;
import com.eucalyptus.auth.euare.DeleteRoleType;
import com.eucalyptus.auth.euare.PutRolePolicyResponseType;
import com.eucalyptus.auth.euare.PutRolePolicyType;
import com.eucalyptus.auth.euare.UpdateAssumeRolePolicyResponseType;
import com.eucalyptus.auth.euare.UpdateAssumeRolePolicyType;
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
import com.eucalyptus.util.async.AsyncRequests;
import com.fasterxml.jackson.databind.node.TextNode;
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


