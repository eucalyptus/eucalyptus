/*************************************************************************
 * Copyright 2009-2015 Ent. Services Development Corporation LP
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


import com.eucalyptus.cloudformation.ValidationErrorException;
import com.eucalyptus.cloudformation.resources.ResourceAction;
import com.eucalyptus.cloudformation.resources.ResourceInfo;
import com.eucalyptus.cloudformation.resources.ResourceProperties;
import com.eucalyptus.cloudformation.resources.standard.info.AWSEC2VPCGatewayAttachmentResourceInfo;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.AWSEC2VPCGatewayAttachmentProperties;
import com.eucalyptus.cloudformation.template.JsonHelper;
import com.eucalyptus.cloudformation.util.MessageHelper;
import com.eucalyptus.cloudformation.workflow.steps.Step;
import com.eucalyptus.cloudformation.workflow.steps.StepBasedResourceAction;
import com.eucalyptus.cloudformation.workflow.steps.UpdateStep;
import com.eucalyptus.cloudformation.workflow.updateinfo.UpdateType;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.Topology;
import com.eucalyptus.compute.common.AttachInternetGatewayResponseType;
import com.eucalyptus.compute.common.AttachInternetGatewayType;
import com.eucalyptus.compute.common.AttachVpnGatewayResponseType;
import com.eucalyptus.compute.common.AttachVpnGatewayType;
import com.eucalyptus.compute.common.CloudFilters;
import com.eucalyptus.compute.common.Compute;
import com.eucalyptus.compute.common.DescribeInternetGatewaysResponseType;
import com.eucalyptus.compute.common.DescribeInternetGatewaysType;
import com.eucalyptus.compute.common.DescribeVpcsResponseType;
import com.eucalyptus.compute.common.DescribeVpcsType;
import com.eucalyptus.compute.common.DescribeVpnGatewaysResponseType;
import com.eucalyptus.compute.common.DescribeVpnGatewaysType;
import com.eucalyptus.compute.common.DetachInternetGatewayResponseType;
import com.eucalyptus.compute.common.DetachInternetGatewayType;
import com.eucalyptus.compute.common.DetachVpnGatewayResponseType;
import com.eucalyptus.compute.common.DetachVpnGatewayType;
import com.eucalyptus.compute.common.VpnGatewayIdSetItemType;
import com.eucalyptus.compute.common.VpnGatewayIdSetType;
import com.eucalyptus.util.async.AsyncRequests;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.collect.Lists;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Objects;

/**
 * Created by ethomas on 2/3/14.
 */
public class AWSEC2VPCGatewayAttachmentResourceAction extends StepBasedResourceAction {

  private AWSEC2VPCGatewayAttachmentProperties properties = new AWSEC2VPCGatewayAttachmentProperties();
  private AWSEC2VPCGatewayAttachmentResourceInfo info = new AWSEC2VPCGatewayAttachmentResourceInfo();

  public AWSEC2VPCGatewayAttachmentResourceAction() {
    super(fromEnum(CreateSteps.class), fromEnum(DeleteSteps.class), fromUpdateEnum(UpdateNoInterruptionSteps.class), null);
  }

  @Override
  public UpdateType getUpdateType(ResourceAction resourceAction, boolean stackTagsChanged) {
    UpdateType updateType = info.supportsTags() && stackTagsChanged ? UpdateType.NO_INTERRUPTION : UpdateType.NONE;
    AWSEC2VPCGatewayAttachmentResourceAction otherAction = (AWSEC2VPCGatewayAttachmentResourceAction) resourceAction;
    if (!Objects.equals(properties.getInternetGatewayId(), otherAction.properties.getInternetGatewayId())) {
      updateType = UpdateType.max(updateType, UpdateType.NO_INTERRUPTION);
    }
    if (!Objects.equals(properties.getVpnGatewayId(), otherAction.properties.getVpnGatewayId())) {
      updateType = UpdateType.max(updateType, UpdateType.NO_INTERRUPTION);
    }
    if (!Objects.equals(properties.getVpcId(), otherAction.properties.getVpcId())) {
      updateType = UpdateType.max(updateType, UpdateType.NO_INTERRUPTION);
    }
    return updateType;
  }

  private enum CreateSteps implements Step {
    CREATE_ATTACHMENT {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        AWSEC2VPCGatewayAttachmentResourceAction action = (AWSEC2VPCGatewayAttachmentResourceAction) resourceAction;
        ServiceConfiguration configuration = Topology.lookup(Compute.class);
        if (action.properties.getInternetGatewayId() != null && action.properties.getVpnGatewayId() != null) {
          throw new ValidationErrorException("Both InternetGatewayId and VpnGatewayId can not be set.");
        }
        if (action.properties.getInternetGatewayId() == null && action.properties.getVpnGatewayId() == null) {
          throw new ValidationErrorException("One of InternetGatewayId or VpnGatewayId must be set.");
        }
        if (action.properties.getInternetGatewayId() != null) {
          AttachInternetGatewayType attachInternetGatewayType = MessageHelper.createMessage(AttachInternetGatewayType.class, action.info.getEffectiveUserId());
          attachInternetGatewayType.setInternetGatewayId(action.properties.getInternetGatewayId());
          attachInternetGatewayType.setVpcId(action.properties.getVpcId());
          AsyncRequests.<AttachInternetGatewayType,AttachInternetGatewayResponseType> sendSync(configuration, attachInternetGatewayType);
        } else {
          // TODO: we don't support this right now so maybe log an error if they try to go this way.
          AttachVpnGatewayType attachVpnGatewayType = MessageHelper.createMessage(AttachVpnGatewayType.class, action.info.getEffectiveUserId());
          attachVpnGatewayType.setVpnGatewayId(action.properties.getVpnGatewayId());
          attachVpnGatewayType.setVpcId(action.properties.getVpcId());
          AsyncRequests.<AttachVpnGatewayType,AttachVpnGatewayResponseType> sendSync(configuration, attachVpnGatewayType);
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
    DELETE_ATTACHMENT {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        AWSEC2VPCGatewayAttachmentResourceAction action = (AWSEC2VPCGatewayAttachmentResourceAction) resourceAction;
        ServiceConfiguration configuration = Topology.lookup(Compute.class);
        if (!Boolean.TRUE.equals(action.info.getCreatedEnoughToDelete())) return action;

        // Check vpc (return if gone)
        DescribeVpcsType describeVpcsType = MessageHelper.createMessage(DescribeVpcsType.class, action.info.getEffectiveUserId());
        describeVpcsType.getFilterSet( ).add( CloudFilters.filter( "vpc-id", action.properties.getVpcId( ) ) );
        DescribeVpcsResponseType describeVpcsResponseType = AsyncRequests.sendSync(configuration, describeVpcsType);
        if (describeVpcsResponseType.getVpcSet() == null ||
          describeVpcsResponseType.getVpcSet().getItem() == null ||
          describeVpcsResponseType.getVpcSet().getItem().isEmpty()) {
          return action; // already deleted
        }
        if (action.properties.getInternetGatewayId() != null) {
          // Check gateway (return if gone)
          DescribeInternetGatewaysType describeInternetGatewaysType = MessageHelper.createMessage(DescribeInternetGatewaysType.class, action.info.getEffectiveUserId());
          describeInternetGatewaysType.getFilterSet( ).add( CloudFilters.filter( "internet-gateway-id", action.properties.getInternetGatewayId( ) ) );
          DescribeInternetGatewaysResponseType describeInternetGatewaysResponseType = AsyncRequests.sendSync(configuration, describeInternetGatewaysType);
          if (describeInternetGatewaysResponseType.getInternetGatewaySet() == null || describeInternetGatewaysResponseType.getInternetGatewaySet().getItem() == null || describeInternetGatewaysResponseType.getInternetGatewaySet().getItem().isEmpty()) {
            return action; // already deleted
          }
          DetachInternetGatewayType detachInternetGatewayType = MessageHelper.createMessage(DetachInternetGatewayType.class, action.info.getEffectiveUserId());
          detachInternetGatewayType.setVpcId(action.properties.getVpcId());
          detachInternetGatewayType.setInternetGatewayId(action.properties.getInternetGatewayId());
          AsyncRequests.<DetachInternetGatewayType, DetachInternetGatewayResponseType>sendSync(configuration, detachInternetGatewayType);
        } else {
          // Check vpn gateway (return if gone)
          DescribeVpnGatewaysType describeVpnGatewaysType = MessageHelper.createMessage(DescribeVpnGatewaysType.class, action.info.getEffectiveUserId());
          action.setVpnGatewayId(describeVpnGatewaysType, action.properties.getVpnGatewayId());
          DescribeVpnGatewaysResponseType describeVpnGatewaysResponseType = AsyncRequests.<DescribeVpnGatewaysType, DescribeVpnGatewaysResponseType>sendSync(configuration, describeVpnGatewaysType);
          if (describeVpnGatewaysResponseType.getVpnGatewaySet() == null || describeVpnGatewaysResponseType.getVpnGatewaySet().getItem() == null || describeVpnGatewaysResponseType.getVpnGatewaySet().getItem().isEmpty()) {
            return action; // already deleted
          }
          DetachVpnGatewayType detachVpnGatewayType = MessageHelper.createMessage(DetachVpnGatewayType.class, action.info.getEffectiveUserId());
          detachVpnGatewayType.setVpcId(action.properties.getVpcId());
          detachVpnGatewayType.setVpnGatewayId(action.properties.getVpnGatewayId());
          AsyncRequests.<DetachVpnGatewayType, DetachVpnGatewayResponseType>sendSync(configuration, detachVpnGatewayType);
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

  private enum UpdateNoInterruptionSteps implements UpdateStep {
    UPDATE_ATTACHMENT {
      @Override
      public ResourceAction perform(ResourceAction oldResourceAction, ResourceAction newResourceAction) throws Exception {
        AWSEC2VPCGatewayAttachmentResourceAction oldAction = (AWSEC2VPCGatewayAttachmentResourceAction) oldResourceAction;
        AWSEC2VPCGatewayAttachmentResourceAction newAction = (AWSEC2VPCGatewayAttachmentResourceAction) newResourceAction;
        ServiceConfiguration configuration = Topology.lookup(Compute.class);
        if (newAction.properties.getInternetGatewayId() != null && newAction.properties.getVpnGatewayId() != null) {
          throw new ValidationErrorException("Both InternetGatewayId and VpnGatewayId can not be set.");
        }
        if (newAction.properties.getInternetGatewayId() == null && newAction.properties.getVpnGatewayId() == null) {
          throw new ValidationErrorException("One of InternetGatewayId or VpnGatewayId must be set.");
        }
        // detach old items (TODO: error checking if not there)
        if (oldAction.properties.getInternetGatewayId() != null) {
          DetachInternetGatewayType detachInternetGatewayType = MessageHelper.createMessage(DetachInternetGatewayType.class, oldAction.info.getEffectiveUserId());
          detachInternetGatewayType.setVpcId(oldAction.properties.getVpcId());
          detachInternetGatewayType.setInternetGatewayId(oldAction.properties.getInternetGatewayId());
          AsyncRequests.<DetachInternetGatewayType, DetachInternetGatewayResponseType>sendSync(configuration, detachInternetGatewayType);
        } else {
          DetachVpnGatewayType detachVpnGatewayType = MessageHelper.createMessage(DetachVpnGatewayType.class, oldAction.info.getEffectiveUserId());
          detachVpnGatewayType.setVpcId(oldAction.properties.getVpcId());
          detachVpnGatewayType.setVpnGatewayId(oldAction.properties.getVpnGatewayId());
          AsyncRequests.<DetachVpnGatewayType, DetachVpnGatewayResponseType>sendSync(configuration, detachVpnGatewayType);
        }
        if (newAction.properties.getInternetGatewayId() != null) {
          AttachInternetGatewayType attachInternetGatewayType = MessageHelper.createMessage(AttachInternetGatewayType.class, newAction.info.getEffectiveUserId());
          attachInternetGatewayType.setInternetGatewayId(newAction.properties.getInternetGatewayId());
          attachInternetGatewayType.setVpcId(newAction.properties.getVpcId());
          AsyncRequests.<AttachInternetGatewayType,AttachInternetGatewayResponseType> sendSync(configuration, attachInternetGatewayType);
        } else {
          // TODO: we don't support this right now so maybe log an error if they try to go this way.
          AttachVpnGatewayType attachVpnGatewayType = MessageHelper.createMessage(AttachVpnGatewayType.class, newAction.info.getEffectiveUserId());
          attachVpnGatewayType.setVpnGatewayId(newAction.properties.getVpnGatewayId());
          attachVpnGatewayType.setVpcId(newAction.properties.getVpcId());
          AsyncRequests.<AttachVpnGatewayType,AttachVpnGatewayResponseType> sendSync(configuration, attachVpnGatewayType);
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



  @Override
  public ResourceProperties getResourceProperties() {
    return properties;
  }

  @Override
  public void setResourceProperties(ResourceProperties resourceProperties) {
    properties = (AWSEC2VPCGatewayAttachmentProperties) resourceProperties;
  }

  @Override
  public ResourceInfo getResourceInfo() {
    return info;
  }

  @Override
  public void setResourceInfo(ResourceInfo resourceInfo) {
    info = (AWSEC2VPCGatewayAttachmentResourceInfo) resourceInfo;
  }

  private void setVpnGatewayId(DescribeVpnGatewaysType describeVpnGatewaysType, String vpnGatewayId) {
    VpnGatewayIdSetType vpnGatewaySet = new VpnGatewayIdSetType();
    describeVpnGatewaysType.setVpnGatewaySet(vpnGatewaySet);

    ArrayList<VpnGatewayIdSetItemType> item = Lists.newArrayList();
    vpnGatewaySet.setItem(item);

    VpnGatewayIdSetItemType vpnGatewayIdSetItem = new VpnGatewayIdSetItemType();
    item.add(vpnGatewayIdSetItem);

    vpnGatewayIdSetItem.setVpnGatewayId(vpnGatewayId);
  }



}


