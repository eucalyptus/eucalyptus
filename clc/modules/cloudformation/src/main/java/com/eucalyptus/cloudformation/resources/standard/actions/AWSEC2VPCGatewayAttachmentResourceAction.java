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


import com.amazonaws.services.simpleworkflow.flow.core.Promise;
import com.amazonaws.services.simpleworkflow.flow.interceptors.RetryPolicy;
import com.eucalyptus.cloudformation.ValidationErrorException;
import com.eucalyptus.cloudformation.resources.ResourceAction;
import com.eucalyptus.cloudformation.resources.ResourceInfo;
import com.eucalyptus.cloudformation.resources.ResourceProperties;
import com.eucalyptus.cloudformation.resources.standard.info.AWSEC2VPCGatewayAttachmentResourceInfo;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.AWSEC2VPCGatewayAttachmentProperties;
import com.eucalyptus.cloudformation.template.JsonHelper;
import com.eucalyptus.cloudformation.util.MessageHelper;
import com.eucalyptus.cloudformation.workflow.StackActivity;
import com.eucalyptus.cloudformation.workflow.steps.MultiStepWithRetryCreatePromise;
import com.eucalyptus.cloudformation.workflow.steps.MultiStepWithRetryDeletePromise;
import com.eucalyptus.cloudformation.workflow.steps.Step;
import com.eucalyptus.cloudformation.workflow.steps.StepTransform;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.Topology;
import com.eucalyptus.compute.common.AttachInternetGatewayResponseType;
import com.eucalyptus.compute.common.AttachInternetGatewayType;
import com.eucalyptus.compute.common.AttachVpnGatewayResponseType;
import com.eucalyptus.compute.common.AttachVpnGatewayType;
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
import com.eucalyptus.compute.common.InternetGatewayIdSetItemType;
import com.eucalyptus.compute.common.InternetGatewayIdSetType;
import com.eucalyptus.compute.common.VpcIdSetItemType;
import com.eucalyptus.compute.common.VpcIdSetType;
import com.eucalyptus.compute.common.VpnGatewayIdSetItemType;
import com.eucalyptus.compute.common.VpnGatewayIdSetType;
import com.eucalyptus.util.async.AsyncRequests;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.collect.Lists;
import com.netflix.glisten.WorkflowOperations;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ethomas on 2/3/14.
 */
public class AWSEC2VPCGatewayAttachmentResourceAction extends ResourceAction {

  private AWSEC2VPCGatewayAttachmentProperties properties = new AWSEC2VPCGatewayAttachmentProperties();
  private AWSEC2VPCGatewayAttachmentResourceInfo info = new AWSEC2VPCGatewayAttachmentResourceInfo();

  public AWSEC2VPCGatewayAttachmentResourceAction() {
    for (CreateSteps createStep: CreateSteps.values()) {
      createSteps.put(createStep.name(), createStep);
    }
    for (DeleteSteps deleteStep: DeleteSteps.values()) {
      deleteSteps.put(deleteStep.name(), deleteStep);
    }

  }
  private enum CreateSteps implements Step {
    CREATE_ATTACHMENT {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        AWSEC2VPCGatewayAttachmentResourceAction action = (AWSEC2VPCGatewayAttachmentResourceAction) resourceAction;
        ServiceConfiguration configuration = Topology.lookup(Compute.class);
        if (action.properties.getVpcId() != null && action.properties.getVpnGatewayId() != null) {
          throw new ValidationErrorException("Both VpcId and VpnGatewayId can not be set.");
        }
        if (action.properties.getVpcId() == null && action.properties.getVpnGatewayId() == null) {
          throw new ValidationErrorException("One of VpcId or VpnGatewayId must be set.");
        }
        if (action.properties.getVpcId() != null) {
          AttachInternetGatewayType attachInternetGatewayType = MessageHelper.createMessage(AttachInternetGatewayType.class, action.info.getEffectiveUserId());
          attachInternetGatewayType.setInternetGatewayId(action.properties.getInternetGatewayId());
          attachInternetGatewayType.setVpcId(action.properties.getVpcId());
          AsyncRequests.<AttachInternetGatewayType,AttachInternetGatewayResponseType> sendSync(configuration, attachInternetGatewayType);
        } else {
          // TODO: we don't support this right now so maybe log an error if they try to go this way.
          AttachVpnGatewayType attachVpnGatewayType = MessageHelper.createMessage(AttachVpnGatewayType.class, action.info.getEffectiveUserId());
          attachVpnGatewayType.setVpnGatewayId(action.properties.getVpnGatewayId());
          AsyncRequests.<AttachVpnGatewayType,AttachVpnGatewayResponseType> sendSync(configuration, attachVpnGatewayType);
        }
        action.info.setPhysicalResourceId(action.getDefaultPhysicalResourceId());
        action.info.setReferenceValueJson(JsonHelper.getStringFromJsonNode(new TextNode(action.info.getPhysicalResourceId())));
        return action;
      }

      @Override
      public RetryPolicy getRetryPolicy() {
        return null;
      }
    }
  }

  private enum DeleteSteps implements Step {
    DELETE_ATTACHMENT {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        AWSEC2VPCGatewayAttachmentResourceAction action = (AWSEC2VPCGatewayAttachmentResourceAction) resourceAction;
        ServiceConfiguration configuration = Topology.lookup(Compute.class);
        if (action.info.getPhysicalResourceId() == null) return action;

        // Check gateway (return if gone)
        DescribeInternetGatewaysType describeInternetGatewaysType = MessageHelper.createMessage(DescribeInternetGatewaysType.class, action.info.getEffectiveUserId());
        action.setInternetGatewayId(describeInternetGatewaysType, action.properties.getInternetGatewayId());
        DescribeInternetGatewaysResponseType describeInternetGatewaysResponseType = AsyncRequests.<DescribeInternetGatewaysType, DescribeInternetGatewaysResponseType>sendSync(configuration, describeInternetGatewaysType);
        if (describeInternetGatewaysResponseType.getInternetGatewaySet() == null || describeInternetGatewaysResponseType.getInternetGatewaySet().getItem() == null || describeInternetGatewaysResponseType.getInternetGatewaySet().getItem().isEmpty()) {
          return action; // already deleted
        }
        if (action.properties.getVpcId() != null) {
          // Check vpc (return if gone)
          DescribeVpcsType describeVpcsType = MessageHelper.createMessage(DescribeVpcsType.class, action.info.getEffectiveUserId());
          action.setVpcId(describeVpcsType, action.properties.getVpcId());
          DescribeVpcsResponseType describeVpcsResponseType = AsyncRequests.<DescribeVpcsType, DescribeVpcsResponseType>sendSync(configuration, describeVpcsType);
          if (describeVpcsResponseType.getVpcSet() == null || describeVpcsResponseType.getVpcSet().getItem() == null || describeVpcsResponseType.getVpcSet().getItem().isEmpty()) {
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

      @Override
      public RetryPolicy getRetryPolicy () {
        return null;
      }
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

  private void setInternetGatewayId(DescribeInternetGatewaysType describeInternetGatewaysType, String internetGatewayId) {
    InternetGatewayIdSetType internetGatewaySet = new InternetGatewayIdSetType();
    describeInternetGatewaysType.setInternetGatewayIdSet(internetGatewaySet);

    ArrayList<InternetGatewayIdSetItemType> item = Lists.newArrayList();
    internetGatewaySet.setItem(item);

    InternetGatewayIdSetItemType internetgatewayIdSetItem = new InternetGatewayIdSetItemType();
    item.add(internetgatewayIdSetItem);

    internetgatewayIdSetItem.setInternetGatewayId(internetGatewayId);
  }


  private void setVpcId(DescribeVpcsType describeVpcsType, String vpcId) {
    VpcIdSetType vpcSet = new VpcIdSetType();
    describeVpcsType.setVpcSet(vpcSet);

    ArrayList<VpcIdSetItemType> item = Lists.newArrayList();
    vpcSet.setItem(item);

    VpcIdSetItemType vpcIdSetItem = new VpcIdSetItemType();
    item.add(vpcIdSetItem);

    vpcIdSetItem.setVpcId(vpcId);
  }

  @Override
  public Promise<String> getCreatePromise(WorkflowOperations<StackActivity> workflowOperations, String resourceId, String stackId, String accountId, String effectiveUserId) {
    List<String> stepIds = Lists.transform(Lists.newArrayList(CreateSteps.values()), StepTransform.INSTANCE);
    return new MultiStepWithRetryCreatePromise(workflowOperations, stepIds, this).getCreatePromise(resourceId, stackId, accountId, effectiveUserId);
  }

  @Override
  public Promise<String> getDeletePromise(WorkflowOperations<StackActivity> workflowOperations, String resourceId, String stackId, String accountId, String effectiveUserId) {
    List<String> stepIds = Lists.transform(Lists.newArrayList(DeleteSteps.values()), StepTransform.INSTANCE);
    return new MultiStepWithRetryDeletePromise(workflowOperations, stepIds, this).getDeletePromise(resourceId, stackId, accountId, effectiveUserId);
  }

}


