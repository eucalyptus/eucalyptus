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


import com.eucalyptus.auth.euare.common.msgs.AddRoleToInstanceProfileResponseType;
import com.eucalyptus.auth.euare.common.msgs.AddRoleToInstanceProfileType;
import com.eucalyptus.auth.euare.common.msgs.CreateInstanceProfileResponseType;
import com.eucalyptus.auth.euare.common.msgs.CreateInstanceProfileType;
import com.eucalyptus.auth.euare.common.msgs.DeleteInstanceProfileResponseType;
import com.eucalyptus.auth.euare.common.msgs.DeleteInstanceProfileType;
import com.eucalyptus.auth.euare.common.msgs.GetInstanceProfileResponseType;
import com.eucalyptus.auth.euare.common.msgs.GetInstanceProfileType;
import com.eucalyptus.auth.euare.common.msgs.RemoveRoleFromInstanceProfileResponseType;
import com.eucalyptus.auth.euare.common.msgs.RemoveRoleFromInstanceProfileType;
import com.eucalyptus.auth.euare.common.msgs.RoleType;
import com.eucalyptus.cloudformation.ValidationErrorException;
import com.eucalyptus.cloudformation.resources.IAMHelper;
import com.eucalyptus.cloudformation.resources.ResourceAction;
import com.eucalyptus.cloudformation.resources.ResourceInfo;
import com.eucalyptus.cloudformation.resources.ResourceProperties;
import com.eucalyptus.cloudformation.resources.standard.info.AWSIAMInstanceProfileResourceInfo;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.AWSIAMInstanceProfileProperties;
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
import com.google.common.collect.Lists;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;

/**
 * Created by ethomas on 2/3/14.
 */
public class AWSIAMInstanceProfileResourceAction extends StepBasedResourceAction {

  private AWSIAMInstanceProfileProperties properties = new AWSIAMInstanceProfileProperties();
  private AWSIAMInstanceProfileResourceInfo info = new AWSIAMInstanceProfileResourceInfo();

  public AWSIAMInstanceProfileResourceAction() {
    super(fromEnum(CreateSteps.class), fromEnum(DeleteSteps.class),fromUpdateEnum(UpdateNoInterruptionSteps.class), null);
  }

  @Override
  public UpdateType getUpdateType(ResourceAction resourceAction, boolean stackTagsChanged) {
    UpdateType updateType = info.supportsTags() && stackTagsChanged ? UpdateType.NO_INTERRUPTION : UpdateType.NONE;
    AWSIAMInstanceProfileResourceAction otherAction = (AWSIAMInstanceProfileResourceAction) resourceAction;
    if (!Objects.equals(properties.getPath(), otherAction.properties.getPath())) {
      updateType = UpdateType.max(updateType, UpdateType.NEEDS_REPLACEMENT);
    }
    if (!Objects.equals(properties.getRoles(), otherAction.properties.getRoles())) {
      updateType = UpdateType.max(updateType, UpdateType.NO_INTERRUPTION);
    }
    return updateType;
  }

  private enum CreateSteps implements Step {
    CREATE_INSTANCE_PROFILE {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        AWSIAMInstanceProfileResourceAction action = (AWSIAMInstanceProfileResourceAction) resourceAction;
        ServiceConfiguration configuration = Topology.lookup(Euare.class);
        String instanceProfileName = action.getDefaultPhysicalResourceId();
        CreateInstanceProfileType createInstanceProfileType = MessageHelper.createMessage(CreateInstanceProfileType.class, action.info.getEffectiveUserId());
        createInstanceProfileType.setPath(action.properties.getPath());
        createInstanceProfileType.setInstanceProfileName(instanceProfileName);
        CreateInstanceProfileResponseType createInstanceProfileResponseType = AsyncRequests.<CreateInstanceProfileType,CreateInstanceProfileResponseType> sendSync(configuration, createInstanceProfileType);
        String arn = createInstanceProfileResponseType.getCreateInstanceProfileResult().getInstanceProfile().getArn();
        action.info.setPhysicalResourceId(instanceProfileName);
        action.info.setCreatedEnoughToDelete(true);
        action.info.setArn(JsonHelper.getStringFromJsonNode(new TextNode(arn)));
        action.info.setReferenceValueJson(JsonHelper.getStringFromJsonNode(new TextNode(action.info.getPhysicalResourceId())));
        return action;
      }
    },
    ADD_ROLES {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        AWSIAMInstanceProfileResourceAction action = (AWSIAMInstanceProfileResourceAction) resourceAction;
        ServiceConfiguration configuration = Topology.lookup(Euare.class);
        if (action.properties.getRoles() != null) {
          if (action.properties.getRoles().size() > 1) throw new ValidationErrorException("Roles has too many elements. The limit is 1.");
          if (action.properties.getRoles().size() == 0) throw new ValidationErrorException("Property Roles can not be empty.");
          for (String roleName: action.properties.getRoles()) {
            AddRoleToInstanceProfileType addRoleToInstanceProfileType = MessageHelper.createMessage(AddRoleToInstanceProfileType.class, action.info.getEffectiveUserId());
            addRoleToInstanceProfileType.setInstanceProfileName(action.info.getPhysicalResourceId());
            addRoleToInstanceProfileType.setRoleName(roleName);
            AsyncRequests.<AddRoleToInstanceProfileType,AddRoleToInstanceProfileResponseType> sendSync(configuration, addRoleToInstanceProfileType);
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
    DELETE_INSTANCE_PROFILE {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        AWSIAMInstanceProfileResourceAction action = (AWSIAMInstanceProfileResourceAction) resourceAction;
        ServiceConfiguration configuration = Topology.lookup(Euare.class);
        if (!Boolean.TRUE.equals(action.info.getCreatedEnoughToDelete())) return action;

        if (!IAMHelper.instanceProfileExists(configuration, action.info.getPhysicalResourceId(), action.info.getEffectiveUserId())) return action;

        // we can delete the instance profile without detaching the role
        DeleteInstanceProfileType deleteInstanceProfileType = MessageHelper.createMessage(DeleteInstanceProfileType.class, action.info.getEffectiveUserId());
        deleteInstanceProfileType.setInstanceProfileName(action.info.getPhysicalResourceId());
        AsyncRequests.<DeleteInstanceProfileType,DeleteInstanceProfileResponseType> sendSync(configuration, deleteInstanceProfileType);
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
    properties = (AWSIAMInstanceProfileProperties) resourceProperties;
  }

  @Override
  public ResourceInfo getResourceInfo() {
    return info;
  }

  @Override
  public void setResourceInfo(ResourceInfo resourceInfo) {
    info = (AWSIAMInstanceProfileResourceInfo) resourceInfo;
  }


  private static List<String> getRoleNames(GetInstanceProfileResponseType getInstanceProfileResponseType) {
    List<String> returnValue = Lists.newArrayList();
    if (getInstanceProfileResponseType != null && getInstanceProfileResponseType.getGetInstanceProfileResult() != null &&
      getInstanceProfileResponseType.getGetInstanceProfileResult().getInstanceProfile() != null &&
      getInstanceProfileResponseType.getGetInstanceProfileResult().getInstanceProfile().getRoles() != null &&
      getInstanceProfileResponseType.getGetInstanceProfileResult().getInstanceProfile().getRoles().getMember() != null) {
      for (RoleType roleType : getInstanceProfileResponseType.getGetInstanceProfileResult().getInstanceProfile().getRoles().getMember()) {
        if (roleType != null && roleType.getRoleName() != null) returnValue.add(roleType.getRoleName());
      }
    }
    return returnValue;
  }

  private enum UpdateNoInterruptionSteps implements UpdateStep {
    UPDATE_ROLES {
      @Override
      public ResourceAction perform(ResourceAction oldResourceAction, ResourceAction newResourceAction) throws Exception {
        AWSIAMInstanceProfileResourceAction oldAction = (AWSIAMInstanceProfileResourceAction) oldResourceAction;
        AWSIAMInstanceProfileResourceAction newAction = (AWSIAMInstanceProfileResourceAction) newResourceAction;
        ServiceConfiguration configuration = Topology.lookup(Euare.class);
        // This is a weird case.  There can be only 1 role per instance profile, but the API might allow more.
        // As such, we delete the current role if it exists.
        GetInstanceProfileType getInstanceProfileType = MessageHelper.createMessage(GetInstanceProfileType.class, newAction.info.getEffectiveUserId());
        getInstanceProfileType.setInstanceProfileName(newAction.info.getPhysicalResourceId());
        GetInstanceProfileResponseType getInstanceProfileResponseType = AsyncRequests.<GetInstanceProfileType, GetInstanceProfileResponseType>sendSync(configuration, getInstanceProfileType);
        for (String roleName: getRoleNames(getInstanceProfileResponseType)) {
          RemoveRoleFromInstanceProfileType removeRoleFromInstanceProfileType = MessageHelper.createMessage(RemoveRoleFromInstanceProfileType.class, newAction.info.getEffectiveUserId());
          removeRoleFromInstanceProfileType.setInstanceProfileName(newAction.info.getPhysicalResourceId());
          removeRoleFromInstanceProfileType.setRoleName(roleName);
          AsyncRequests.<RemoveRoleFromInstanceProfileType,RemoveRoleFromInstanceProfileResponseType> sendSync(configuration, removeRoleFromInstanceProfileType);
        }
        if (newAction.properties.getRoles() != null) {
          if (newAction.properties.getRoles().size() == 0) throw new ValidationErrorException("Property Roles can not be empty.");
          if (newAction.properties.getRoles().size() > 1) throw new ValidationErrorException("Roles has too many elements. The limit is 1.");
          for (String roleName: newAction.properties.getRoles()) {
            AddRoleToInstanceProfileType addRoleToInstanceProfileType = MessageHelper.createMessage(AddRoleToInstanceProfileType.class, newAction.info.getEffectiveUserId());
            addRoleToInstanceProfileType.setInstanceProfileName(newAction.info.getPhysicalResourceId());
            addRoleToInstanceProfileType.setRoleName(roleName);
            AsyncRequests.<AddRoleToInstanceProfileType,AddRoleToInstanceProfileResponseType> sendSync(configuration, addRoleToInstanceProfileType);
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

}


