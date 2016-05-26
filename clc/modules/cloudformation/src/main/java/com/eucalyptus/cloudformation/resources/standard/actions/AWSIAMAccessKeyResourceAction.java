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


import com.eucalyptus.auth.euare.CreateAccessKeyResponseType;
import com.eucalyptus.auth.euare.CreateAccessKeyType;
import com.eucalyptus.auth.euare.DeleteAccessKeyResponseType;
import com.eucalyptus.auth.euare.DeleteAccessKeyType;
import com.eucalyptus.auth.euare.UpdateAccessKeyResponseType;
import com.eucalyptus.auth.euare.UpdateAccessKeyType;
import com.eucalyptus.cloudformation.ValidationErrorException;
import com.eucalyptus.cloudformation.resources.IAMHelper;
import com.eucalyptus.cloudformation.resources.ResourceAction;
import com.eucalyptus.cloudformation.resources.ResourceInfo;
import com.eucalyptus.cloudformation.resources.ResourceProperties;
import com.eucalyptus.cloudformation.resources.standard.info.AWSIAMAccessKeyResourceInfo;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.AWSIAMAccessKeyProperties;
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

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Objects;

/**
 * Created by ethomas on 2/3/14.
 */
public class AWSIAMAccessKeyResourceAction extends StepBasedResourceAction {

  private AWSIAMAccessKeyProperties properties = new AWSIAMAccessKeyProperties();
  private AWSIAMAccessKeyResourceInfo info = new AWSIAMAccessKeyResourceInfo();

  public AWSIAMAccessKeyResourceAction() {
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
    AWSIAMAccessKeyResourceAction otherAction = (AWSIAMAccessKeyResourceAction) resourceAction;
    if (!Objects.equals(properties.getUserName(), otherAction.properties.getUserName())) {
      updateType = UpdateType.max(updateType, UpdateType.NEEDS_REPLACEMENT);
    }
    if (!Objects.equals(properties.getStatus(), otherAction.properties.getStatus())) {
      updateType = UpdateType.max(updateType, UpdateType.NO_INTERRUPTION);
    }
    if (!Objects.equals(properties.getSerial(), otherAction.properties.getSerial())) {
      updateType = UpdateType.max(updateType, UpdateType.NEEDS_REPLACEMENT);
    }
    return updateType;
  }

  private enum CreateSteps implements Step {
    CREATE_KEY {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        AWSIAMAccessKeyResourceAction action = (AWSIAMAccessKeyResourceAction) resourceAction;
        ServiceConfiguration configuration = Topology.lookup(Euare.class);
        if (action.properties.getStatus() == null) action.properties.setStatus("Active");
        if (!"Active".equals(action.properties.getStatus()) && !"Inactive".equals(action.properties.getStatus())) {
          throw new ValidationErrorException("Invalid status " + action.properties.getStatus());
        }
        CreateAccessKeyType createAccessKeyType = MessageHelper.createMessage(CreateAccessKeyType.class, action.info.getEffectiveUserId());
        createAccessKeyType.setUserName(action.properties.getUserName());
        CreateAccessKeyResponseType createAccessKeyResponseType = AsyncRequests.<CreateAccessKeyType,CreateAccessKeyResponseType> sendSync(configuration, createAccessKeyType);
        // access key id = physical resource id
        action.info.setPhysicalResourceId(createAccessKeyResponseType.getCreateAccessKeyResult().getAccessKey().getAccessKeyId());
        action.info.setCreatedEnoughToDelete(true);
        action.info.setSecretAccessKey(JsonHelper.getStringFromJsonNode(new TextNode(createAccessKeyResponseType.getCreateAccessKeyResult().getAccessKey().getSecretAccessKey())));
        action.info.setReferenceValueJson(JsonHelper.getStringFromJsonNode(new TextNode(action.info.getPhysicalResourceId())));
        return action;
      }
    },
    SET_STATUS {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        AWSIAMAccessKeyResourceAction action = (AWSIAMAccessKeyResourceAction) resourceAction;
        if (action.properties.getStatus() == null) action.properties.setStatus("Active");
        ServiceConfiguration configuration = Topology.lookup(Euare.class);
        UpdateAccessKeyType updateAccessKeyType = MessageHelper.createMessage(UpdateAccessKeyType.class, action.info.getEffectiveUserId());
        updateAccessKeyType.setUserName(action.properties.getUserName());
        updateAccessKeyType.setAccessKeyId(action.info.getPhysicalResourceId());
        updateAccessKeyType.setStatus(action.properties.getStatus());
        AsyncRequests.<UpdateAccessKeyType,UpdateAccessKeyResponseType> sendSync(configuration, updateAccessKeyType);
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
    DELETE_KEY {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        AWSIAMAccessKeyResourceAction action = (AWSIAMAccessKeyResourceAction) resourceAction;
        ServiceConfiguration configuration = Topology.lookup(Euare.class);
        if (!Boolean.TRUE.equals(action.info.getCreatedEnoughToDelete())) return action;

        if (action.properties.getStatus() == null) action.properties.setStatus("Active");
        // if no user, bye.
        if (!IAMHelper.userExists(configuration, action.properties.getUserName(), action.info.getEffectiveUserId())) return action;

        if (!IAMHelper.accessKeyExists(configuration, action.info.getPhysicalResourceId(),
          action.properties.getUserName(), action.info.getEffectiveUserId())) return action;
        DeleteAccessKeyType deleteAccessKeyType = MessageHelper.createMessage(DeleteAccessKeyType.class, action.info.getEffectiveUserId());
        deleteAccessKeyType.setUserName(action.properties.getUserName());
        deleteAccessKeyType.setAccessKeyId(action.info.getPhysicalResourceId());
        AsyncRequests.<DeleteAccessKeyType,DeleteAccessKeyResponseType> sendSync(configuration, deleteAccessKeyType);
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
    properties = (AWSIAMAccessKeyProperties) resourceProperties;
  }

  @Override
  public ResourceInfo getResourceInfo() {
    return info;
  }

  @Override
  public void setResourceInfo(ResourceInfo resourceInfo) {
    info = (AWSIAMAccessKeyResourceInfo) resourceInfo;
  }


  private enum UpdateNoInterruptionSteps implements UpdateStep {
    UPDATE_STATUS {
      @Override
      public ResourceAction perform(ResourceAction oldResourceAction, ResourceAction newResourceAction) throws Exception {
        AWSIAMAccessKeyResourceAction oldAction = (AWSIAMAccessKeyResourceAction) oldResourceAction;
        AWSIAMAccessKeyResourceAction newAction = (AWSIAMAccessKeyResourceAction) newResourceAction;
        if (newAction.properties.getStatus() == null) newAction.properties.setStatus("Active");
        ServiceConfiguration configuration = Topology.lookup(Euare.class);
        UpdateAccessKeyType updateAccessKeyType = MessageHelper.createMessage(UpdateAccessKeyType.class, newAction.info.getEffectiveUserId());
        updateAccessKeyType.setUserName(newAction.properties.getUserName());
        updateAccessKeyType.setAccessKeyId(newAction.info.getPhysicalResourceId());
        updateAccessKeyType.setStatus(newAction.properties.getStatus());
        AsyncRequests.<UpdateAccessKeyType,UpdateAccessKeyResponseType> sendSync(configuration, updateAccessKeyType);
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
    CHECK_SERIAL {
      @Override
      public ResourceAction perform(ResourceAction oldResourceAction, ResourceAction newResourceAction) throws Exception {
        AWSIAMAccessKeyResourceAction oldAction = (AWSIAMAccessKeyResourceAction) oldResourceAction;
        AWSIAMAccessKeyResourceAction newAction = (AWSIAMAccessKeyResourceAction) newResourceAction;
        int oldSerial = oldAction.properties.getSerial() != null ? oldAction.properties.getSerial().intValue() : 0;
        int newSerial = oldAction.properties.getSerial() != null ? newAction.properties.getSerial().intValue() : 0;
        if (newSerial < oldSerial && Objects.equals(oldAction.properties.getUserName(), newAction.properties.getUserName())) {
          throw new ValidationErrorException("AccessKey Serial cannot be decreased");
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


