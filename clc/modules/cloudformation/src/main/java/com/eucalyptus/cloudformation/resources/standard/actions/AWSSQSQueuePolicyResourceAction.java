/*************************************************************************
 * Copyright 2016 Ent. Services Development Corporation LP
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


import com.eucalyptus.cloudformation.resources.ResourceAction;
import com.eucalyptus.cloudformation.resources.ResourceInfo;
import com.eucalyptus.cloudformation.resources.ResourceProperties;
import com.eucalyptus.cloudformation.resources.standard.info.AWSSQSQueuePolicyResourceInfo;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.AWSSQSQueuePolicyProperties;
import com.eucalyptus.cloudformation.template.JsonHelper;
import com.eucalyptus.cloudformation.util.MessageHelper;
import com.eucalyptus.cloudformation.workflow.steps.Step;
import com.eucalyptus.cloudformation.workflow.steps.StepBasedResourceAction;
import com.eucalyptus.cloudformation.workflow.steps.UpdateStep;
import com.eucalyptus.cloudformation.workflow.updateinfo.UpdateType;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.Topology;
import com.eucalyptus.simplequeue.common.msgs.Attribute;
import com.eucalyptus.simplequeue.common.msgs.SetQueueAttributesType;
import com.eucalyptus.simplequeue.common.SimpleQueue;
import com.eucalyptus.util.async.AsyncExceptions;
import com.eucalyptus.util.async.AsyncRequests;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.common.collect.Sets;

import javax.annotation.Nullable;
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


