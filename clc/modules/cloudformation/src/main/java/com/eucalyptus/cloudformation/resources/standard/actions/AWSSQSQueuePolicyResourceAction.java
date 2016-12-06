/*************************************************************************
 * (c) Copyright 2016 Hewlett Packard Enterprise Development Company LP
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
 ************************************************************************/
package com.eucalyptus.cloudformation.resources.standard.actions;


import com.eucalyptus.cloudformation.ValidationErrorException;
import com.eucalyptus.cloudformation.resources.IAMHelper;
import com.eucalyptus.cloudformation.resources.ResourceAction;
import com.eucalyptus.cloudformation.resources.ResourceInfo;
import com.eucalyptus.cloudformation.resources.ResourceProperties;
import com.eucalyptus.cloudformation.resources.standard.info.AWSIAMUserToGroupAdditionResourceInfo;
import com.eucalyptus.cloudformation.resources.standard.info.AWSSQSQueuePolicyResourceInfo;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.AWSIAMUserToGroupAdditionProperties;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.AWSSQSQueuePolicyProperties;
import com.eucalyptus.cloudformation.template.JsonHelper;
import com.eucalyptus.cloudformation.util.MessageHelper;
import com.eucalyptus.cloudformation.workflow.steps.Step;
import com.eucalyptus.cloudformation.workflow.steps.StepBasedResourceAction;
import com.eucalyptus.cloudformation.workflow.steps.UpdateStep;
import com.eucalyptus.cloudformation.workflow.updateinfo.UpdateType;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.Topology;
import com.eucalyptus.simplequeue.Attribute;
import com.eucalyptus.simplequeue.SetQueueAttributesType;
import com.eucalyptus.simplequeue.SimpleQueue;
import com.eucalyptus.util.async.AsyncExceptions;
import com.eucalyptus.util.async.AsyncRequests;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.common.collect.Sets;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Objects;
import java.util.Set;

/**
 * Created by ethomas on 2/3/14.
 */
public class AWSSQSQueuePolicyResourceAction extends StepBasedResourceAction {

  private AWSSQSQueuePolicyProperties properties = new AWSSQSQueuePolicyProperties();
  private AWSSQSQueuePolicyResourceInfo info = new AWSSQSQueuePolicyResourceInfo();

  public AWSSQSQueuePolicyResourceAction() {
    super(fromEnum(CreateSteps.class), fromEnum(DeleteSteps.class), fromUpdateEnum(UpdateNoInterruptionSteps.class), null);
  }
  @Override
  public UpdateType getUpdateType(ResourceAction resourceAction, boolean stackTagsChanged) {
    UpdateType updateType = info.supportsTags() && stackTagsChanged ? UpdateType.NO_INTERRUPTION : UpdateType.NONE;
    AWSSQSQueuePolicyResourceAction otherAction = (AWSSQSQueuePolicyResourceAction) resourceAction;
    if (!Objects.equals(properties.getPolicyDocument(), properties.getPolicyDocument())) {
      updateType = UpdateType.max(updateType, UpdateType.NO_INTERRUPTION);
    }
    if (!Objects.equals(properties.getQueues(), otherAction.properties.getQueues())) {
      updateType = UpdateType.max(updateType, UpdateType.NO_INTERRUPTION);
    }
    return updateType;
  }


  private enum CreateSteps implements Step {
    ADD_POLICY_TO_QUEUES {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        AWSSQSQueuePolicyResourceAction action = (AWSSQSQueuePolicyResourceAction) resourceAction;
        ServiceConfiguration configuration = Topology.lookup(SimpleQueue.class);
        if (action.properties.getQueues() != null) {
          for (String queueUrl : action.properties.getQueues()) {
            SetQueueAttributesType setQueueAttributesType = MessageHelper.createMessage(SetQueueAttributesType.class, action.info.getEffectiveUserId());
            setQueueAttributesType.setQueueUrl(queueUrl);
            Attribute attribute = new Attribute();
            attribute.setName("Policy");
            attribute.setValue(action.properties.getPolicyDocument().toString());
            setQueueAttributesType.getAttribute().add(attribute);
            AsyncRequests.sendSync(configuration, setQueueAttributesType);
          }
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
    REMOVE_POLICY_FROM_QUEUES {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        AWSSQSQueuePolicyResourceAction action = (AWSSQSQueuePolicyResourceAction) resourceAction;
        ServiceConfiguration configuration = Topology.lookup(SimpleQueue.class);
        if (action.properties.getQueues() != null) {
          for (String queueUrl : action.properties.getQueues()) {
            removePolicyFromQueue(action, configuration, queueUrl);
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

  private static void removePolicyFromQueue(AWSSQSQueuePolicyResourceAction action, ServiceConfiguration configuration, String queueUrl) throws Exception {
    SetQueueAttributesType setQueueAttributesType = MessageHelper.createMessage(SetQueueAttributesType.class, action.info.getEffectiveUserId());
    setQueueAttributesType.setQueueUrl(queueUrl);
    Attribute attribute = new Attribute();
    attribute.setName("Policy");
    attribute.setValue("");
    setQueueAttributesType.getAttribute().add(attribute);
    try {
      AsyncRequests.sendSync(configuration, setQueueAttributesType);
    } catch (final Exception e) {
      final Optional<AsyncExceptions.AsyncWebServiceError> error = AsyncExceptions.asWebServiceError(e);
      if (error.isPresent()) switch (Strings.nullToEmpty(error.get().getCode())) {
        case "QueueDoesNotExist":
          break;
        default:
          throw e;
      }
      else {
        throw e;
      }
    }
  }

  @Override
  public ResourceProperties getResourceProperties() {
    return properties;
  }

  @Override
  public void setResourceProperties(ResourceProperties resourceProperties) {
    properties = (AWSSQSQueuePolicyProperties) resourceProperties;
  }

  @Override
  public ResourceInfo getResourceInfo() {
    return info;
  }

  @Override
  public void setResourceInfo(ResourceInfo resourceInfo) {
    info = (AWSSQSQueuePolicyResourceInfo) resourceInfo;
  }

  private enum UpdateNoInterruptionSteps implements UpdateStep {
    UPDATE_POLICY_IN_QUEUES {
      @Override
      public ResourceAction perform(ResourceAction oldResourceAction, ResourceAction newResourceAction) throws Exception {
        AWSSQSQueuePolicyResourceAction oldAction = (AWSSQSQueuePolicyResourceAction) oldResourceAction;
        AWSSQSQueuePolicyResourceAction newAction = (AWSSQSQueuePolicyResourceAction) newResourceAction;
        ServiceConfiguration configuration = Topology.lookup(SimpleQueue.class);
        Set<String> newQueueUrls = Sets.newHashSet();
        Set<String> oldQueueUrls = Sets.newHashSet();
        if (oldAction.properties.getQueues() != null) {
          oldQueueUrls.addAll(oldAction.properties.getQueues());
        }
        if (newAction.properties.getQueues() != null) {
          newQueueUrls.addAll(newAction.properties.getQueues());
          oldQueueUrls.removeAll(newAction.properties.getQueues());
        }
        for (String queueUrl : newQueueUrls) {
          SetQueueAttributesType setQueueAttributesType = MessageHelper.createMessage(SetQueueAttributesType.class, newAction.info.getEffectiveUserId());
          setQueueAttributesType.setQueueUrl(queueUrl);
          Attribute attribute = new Attribute();
          attribute.setName("Policy");
          attribute.setValue(newAction.properties.getPolicyDocument().toString());
          setQueueAttributesType.getAttribute().add(attribute);
          AsyncRequests.sendSync(configuration, setQueueAttributesType);
        }
        for (String queueUrl : oldQueueUrls) {
          removePolicyFromQueue(newAction, configuration, queueUrl);
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


