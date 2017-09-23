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
import com.eucalyptus.cloudformation.resources.standard.info.AWSSQSQueueResourceInfo;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.AWSSQSQueueProperties;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.SQSRedrivePolicy;
import com.eucalyptus.cloudformation.template.JsonHelper;
import com.eucalyptus.cloudformation.util.MessageHelper;
import com.eucalyptus.cloudformation.workflow.steps.Step;
import com.eucalyptus.cloudformation.workflow.steps.StepBasedResourceAction;
import com.eucalyptus.cloudformation.workflow.steps.UpdateStep;
import com.eucalyptus.cloudformation.workflow.updateinfo.UpdateType;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.Topology;
import com.eucalyptus.simplequeue.common.msgs.Attribute;
import com.eucalyptus.simplequeue.common.msgs.CreateQueueResponseType;
import com.eucalyptus.simplequeue.common.msgs.CreateQueueType;
import com.eucalyptus.simplequeue.common.msgs.DeleteQueueType;
import com.eucalyptus.simplequeue.common.msgs.GetQueueAttributesResponseType;
import com.eucalyptus.simplequeue.common.msgs.GetQueueAttributesType;
import com.eucalyptus.simplequeue.common.msgs.SetQueueAttributesType;
import com.eucalyptus.simplequeue.common.SimpleQueue;
import com.eucalyptus.util.async.AsyncRequests;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.collect.Lists;

import javax.annotation.Nullable;
import java.util.Objects;

/**
 * Created by ethomas on 2/3/14.
 */
public class AWSSQSQueueResourceAction extends StepBasedResourceAction {

  private AWSSQSQueueProperties properties = new AWSSQSQueueProperties();
  private AWSSQSQueueResourceInfo info = new AWSSQSQueueResourceInfo();

  public AWSSQSQueueResourceAction() {
    super(fromEnum(CreateSteps.class), fromEnum(DeleteSteps.class), fromUpdateEnum(UpdateNoInterruptionSteps.class), null);
  }

  private enum CreateSteps implements Step {
    CREATE_QUEUE {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        AWSSQSQueueResourceAction action = (AWSSQSQueueResourceAction) resourceAction;
        ServiceConfiguration configuration = Topology.lookup(SimpleQueue.class);
        if (action.properties.getQueueName() == null) {
          action.properties.setQueueName(action.getDefaultPhysicalResourceId());
        }
        // no need to check already exists, service does that
        CreateQueueType createQueueType = MessageHelper.createMessage(CreateQueueType.class, action.info.getEffectiveUserId());
        createQueueType.setQueueName(action.properties.getQueueName());
        if (action.properties.getDelaySeconds() != null) {
          Attribute attribute = new Attribute();
          attribute.setName("DelaySeconds");
          attribute.setValue("" + action.properties.getDelaySeconds());
          createQueueType.getAttribute().add(attribute);
        }
        if (action.properties.getMaximumMessageSize() != null) {
          Attribute attribute = new Attribute();
          attribute.setName("MaximumMessageSize");
          attribute.setValue("" + action.properties.getMaximumMessageSize());
          createQueueType.getAttribute().add(attribute);
        }
        if (action.properties.getMessageRetentionPeriod() != null) {
          Attribute attribute = new Attribute();
          attribute.setName("MessageRetentionPeriod");
          attribute.setValue("" + action.properties.getMessageRetentionPeriod());
          createQueueType.getAttribute().add(attribute);
        }
        if (action.properties.getReceiveMessageWaitTimeSeconds() != null) {
          Attribute attribute = new Attribute();
          attribute.setName("ReceiveMessageWaitTimeSeconds");
          attribute.setValue("" + action.properties.getReceiveMessageWaitTimeSeconds());
          createQueueType.getAttribute().add(attribute);
        }
        if (action.properties.getRedrivePolicy() != null) {
          Attribute attribute = new Attribute();
          attribute.setName("RedrivePolicy");
          attribute.setValue(convertRedrivePolicyToJsonString(action.properties.getRedrivePolicy()));
          createQueueType.getAttribute().add(attribute);
        }
        if (action.properties.getVisibilityTimeout() != null) {
          Attribute attribute = new Attribute();
          attribute.setName("VisibilityTimeout");
          attribute.setValue("" + action.properties.getVisibilityTimeout());
          createQueueType.getAttribute().add(attribute);
        }
        CreateQueueResponseType createQueueResponseType = AsyncRequests.sendSync(configuration, createQueueType);
        action.info.setPhysicalResourceId(createQueueResponseType.getCreateQueueResult().getQueueUrl());
        action.info.setCreatedEnoughToDelete(true);
        action.info.setReferenceValueJson(JsonHelper.getStringFromJsonNode(new TextNode(action.info.getPhysicalResourceId())));
        action.info.setQueueName(JsonHelper.getStringFromJsonNode(new TextNode(action.properties.getQueueName())));

        GetQueueAttributesType getQueueAttributesType = MessageHelper.createMessage(GetQueueAttributesType.class, action.info.getEffectiveUserId());
        getQueueAttributesType.setAttributeName(Lists.newArrayList("QueueArn"));
        getQueueAttributesType.setQueueUrl(action.info.getPhysicalResourceId());
        GetQueueAttributesResponseType getQueueAttributesResponseType = AsyncRequests.sendSync(configuration, getQueueAttributesType);
        // should only return one attribute
        action.info.setArn(JsonHelper.getStringFromJsonNode(new TextNode(getQueueAttributesResponseType.getGetQueueAttributesResult().getAttribute().get(0).getValue())));
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
    DELETE_QUEUE {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        AWSSQSQueueResourceAction action = (AWSSQSQueueResourceAction) resourceAction;
        ServiceConfiguration configuration = Topology.lookup(SimpleQueue.class);
        if (!Boolean.TRUE.equals(action.info.getCreatedEnoughToDelete())) return action;
        DeleteQueueType deleteQueueType = MessageHelper.createMessage(DeleteQueueType.class, action.info.getEffectiveUserId());
        deleteQueueType.setQueueUrl(action.info.getPhysicalResourceId());
        AsyncRequests.sendSync(configuration, deleteQueueType);
        return action;
      }
    };

    @Nullable
    @Override
    public Integer getTimeout() {
      return null;
    }
  }

  private enum UpdateNoInterruptionSteps implements UpdateStep {
    UPDATE_QUEUE {
      @Override
      public ResourceAction perform(ResourceAction oldResourceAction, ResourceAction newResourceAction) throws Exception {
        AWSSQSQueueResourceAction oldAction = (AWSSQSQueueResourceAction) oldResourceAction;
        AWSSQSQueueResourceAction newAction = (AWSSQSQueueResourceAction) newResourceAction;
        // just update the values.
        ServiceConfiguration configuration = Topology.lookup(SimpleQueue.class);
        SetQueueAttributesType setQueueAttributesType = MessageHelper.createMessage(SetQueueAttributesType.class, newAction.info.getEffectiveUserId());
        setQueueAttributesType.setQueueUrl(newAction.info.getPhysicalResourceId());
        Attribute attribute = new Attribute();
        attribute.setName("DelaySeconds");
        attribute.setValue(newAction.properties.getDelaySeconds() == null ? "0" : "" + newAction.properties.getDelaySeconds());
        setQueueAttributesType.getAttribute().add(attribute);

        attribute = new Attribute();
        attribute.setName("MaximumMessageSize");
        attribute.setValue(newAction.properties.getMaximumMessageSize() == null ? "262144" : "" + newAction.properties.getMaximumMessageSize());
        setQueueAttributesType.getAttribute().add(attribute);

        attribute = new Attribute();
        attribute.setName("MessageRetentionPeriod");
        attribute.setValue(newAction.properties.getMessageRetentionPeriod() == null ? "345600" : "" + newAction.properties.getMessageRetentionPeriod());
        setQueueAttributesType.getAttribute().add(attribute);

        attribute = new Attribute();
        attribute.setName("ReceiveMessageWaitTimeSeconds");
        attribute.setValue(newAction.properties.getReceiveMessageWaitTimeSeconds() == null ? "0" : "" + newAction.properties.getReceiveMessageWaitTimeSeconds());
        setQueueAttributesType.getAttribute().add(attribute);

        attribute = new Attribute();
        attribute.setName("RedrivePolicy");
        attribute.setValue(newAction.properties.getRedrivePolicy() == null ? "" : convertRedrivePolicyToJsonString(newAction.properties.getRedrivePolicy()));
        setQueueAttributesType.getAttribute().add(attribute);

        attribute = new Attribute();
        attribute.setName("VisibilityTimeout");
        attribute.setValue(newAction.properties.getVisibilityTimeout() == null ? "30" : "" + newAction.properties.getVisibilityTimeout());
        setQueueAttributesType.getAttribute().add(attribute);

        AsyncRequests.sendSync(configuration, setQueueAttributesType);
        return newAction;
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
    properties = (AWSSQSQueueProperties) resourceProperties;
  }

  @Override
  public ResourceInfo getResourceInfo() {
    return info;
  }

  @Override
  public void setResourceInfo(ResourceInfo resourceInfo) {
    info = (AWSSQSQueueResourceInfo) resourceInfo;
  }

  @Override
  public UpdateType getUpdateType(ResourceAction resourceAction, boolean stackTagsChanged) {
    UpdateType updateType = info.supportsTags() && stackTagsChanged ? UpdateType.NO_INTERRUPTION : UpdateType.NONE;
    AWSSQSQueueResourceAction otherAction = (AWSSQSQueueResourceAction) resourceAction;
    if (!Objects.equals(properties.getDelaySeconds(), otherAction.properties.getDelaySeconds())) {
      updateType = UpdateType.max(updateType, UpdateType.NO_INTERRUPTION);
    }
    if (!Objects.equals(properties.getMaximumMessageSize(), otherAction.properties.getMaximumMessageSize())) {
      updateType = UpdateType.max(updateType, UpdateType.NO_INTERRUPTION);
    }
    if (!Objects.equals(properties.getMessageRetentionPeriod(), otherAction.properties.getMessageRetentionPeriod())) {
      updateType = UpdateType.max(updateType, UpdateType.NO_INTERRUPTION);
    }
    if (!Objects.equals(properties.getQueueName(), otherAction.properties.getQueueName())) {
      updateType = UpdateType.max(updateType, UpdateType.NEEDS_REPLACEMENT);
    }
    if (!Objects.equals(properties.getReceiveMessageWaitTimeSeconds(), otherAction.properties.getReceiveMessageWaitTimeSeconds())) {
      updateType = UpdateType.max(updateType, UpdateType.NO_INTERRUPTION);
    }
    if (!Objects.equals(properties.getRedrivePolicy(), otherAction.properties.getRedrivePolicy())) {
      updateType = UpdateType.max(updateType, UpdateType.NO_INTERRUPTION);
    }
    if (!Objects.equals(properties.getVisibilityTimeout(), otherAction.properties.getVisibilityTimeout())) {
      updateType = UpdateType.max(updateType, UpdateType.NO_INTERRUPTION);
    }
    return updateType;
  }


  private static String convertRedrivePolicyToJsonString(SQSRedrivePolicy sqsRedrivePolicy) {
    if (sqsRedrivePolicy == null) return "";
    // Note: we could use a json parser, but this is simple
    StringBuilder builder = new StringBuilder("{");
    String delimiter = "";
    if (sqsRedrivePolicy.getDeadLetterTargetArn() != null) {
      builder.append("\"deadLetterTargetArn\" : \""+sqsRedrivePolicy.getDeadLetterTargetArn() + "\"");
      delimiter = ",";
    }
    if (sqsRedrivePolicy.getMaxReceiveCount() != null) {
      builder.append(delimiter + "\"maxReceiveCount\" : \""+sqsRedrivePolicy.getMaxReceiveCount() + "\"");
    }
    builder.append("}");
    return builder.toString();
  }

}


