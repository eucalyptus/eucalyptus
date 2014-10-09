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
import com.eucalyptus.cloudformation.resources.standard.info.AWSEC2VPCDHCPOptionsAssociationResourceInfo;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.AWSEC2VPCDHCPOptionsAssociationProperties;
import com.eucalyptus.cloudformation.template.JsonHelper;
import com.eucalyptus.cloudformation.util.MessageHelper;
import com.eucalyptus.cloudformation.workflow.StackActivity;
import com.eucalyptus.cloudformation.workflow.steps.MultiStepWithRetryCreatePromise;
import com.eucalyptus.cloudformation.workflow.steps.MultiStepWithRetryDeletePromise;
import com.eucalyptus.cloudformation.workflow.steps.Step;
import com.eucalyptus.cloudformation.workflow.steps.StepTransform;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.Topology;
import com.eucalyptus.compute.common.AssociateDhcpOptionsResponseType;
import com.eucalyptus.compute.common.AssociateDhcpOptionsType;
import com.eucalyptus.compute.common.Compute;
import com.eucalyptus.compute.common.DescribeDhcpOptionsResponseType;
import com.eucalyptus.compute.common.DescribeDhcpOptionsType;
import com.eucalyptus.compute.common.DescribeVpcsResponseType;
import com.eucalyptus.compute.common.DescribeVpcsType;
import com.eucalyptus.compute.common.DhcpOptionsIdSetItemType;
import com.eucalyptus.compute.common.DhcpOptionsIdSetType;
import com.eucalyptus.compute.common.VpcIdSetItemType;
import com.eucalyptus.compute.common.VpcIdSetType;
import com.eucalyptus.util.async.AsyncRequests;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.collect.Lists;
import com.netflix.glisten.WorkflowOperations;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ethomas on 2/3/14.
 */
public class AWSEC2VPCDHCPOptionsAssociationResourceAction extends ResourceAction {

  private AWSEC2VPCDHCPOptionsAssociationProperties properties = new AWSEC2VPCDHCPOptionsAssociationProperties();
  private AWSEC2VPCDHCPOptionsAssociationResourceInfo info = new AWSEC2VPCDHCPOptionsAssociationResourceInfo();

  public AWSEC2VPCDHCPOptionsAssociationResourceAction() {
    for (CreateSteps createStep: CreateSteps.values()) {
      createSteps.put(createStep.name(), createStep);
    }
    for (DeleteSteps deleteStep: DeleteSteps.values()) {
      deleteSteps.put(deleteStep.name(), deleteStep);
    }

  }
  private enum CreateSteps implements Step {
    CREATE_ASSOCIATION {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        AWSEC2VPCDHCPOptionsAssociationResourceAction action = (AWSEC2VPCDHCPOptionsAssociationResourceAction) resourceAction;
        ServiceConfiguration configuration = Topology.lookup(Compute.class);
        // Check dhcp options (if not "default")
        if (!"default".equals(action.properties.getDhcpOptionsId())) {
          DescribeDhcpOptionsType describeDhcpOptionsType = MessageHelper.createMessage(DescribeDhcpOptionsType.class, action.info.getEffectiveUserId());
          action.setDhcpOptionsId(describeDhcpOptionsType, action.properties.getDhcpOptionsId());
          DescribeDhcpOptionsResponseType describeDhcpOptionsResponseType = AsyncRequests.<DescribeDhcpOptionsType,DescribeDhcpOptionsResponseType> sendSync(configuration, describeDhcpOptionsType);
          if (describeDhcpOptionsResponseType.getDhcpOptionsSet() == null || describeDhcpOptionsResponseType.getDhcpOptionsSet().getItem() == null || describeDhcpOptionsResponseType.getDhcpOptionsSet().getItem().isEmpty()) {
            throw new ValidationErrorException("No such dhcp options: " + action.properties.getDhcpOptionsId());
          }
        }
        // check vpc
        DescribeVpcsType describeVpcsType = MessageHelper.createMessage(DescribeVpcsType.class, action.info.getEffectiveUserId());
        action.setVpcId(describeVpcsType, action.properties.getVpcId());
        DescribeVpcsResponseType describeVpcsResponseType = AsyncRequests.<DescribeVpcsType,DescribeVpcsResponseType> sendSync(configuration, describeVpcsType);
        if (describeVpcsResponseType.getVpcSet() == null || describeVpcsResponseType.getVpcSet().getItem() == null || describeVpcsResponseType.getVpcSet().getItem().isEmpty()) {
          throw new ValidationErrorException("No such vpc: " + action.properties.getVpcId());
        }
        AssociateDhcpOptionsType associateDhcpOptionsType = MessageHelper.createMessage(AssociateDhcpOptionsType.class, action.info.getEffectiveUserId());
        associateDhcpOptionsType.setDhcpOptionsId(action.properties.getDhcpOptionsId());
        associateDhcpOptionsType.setVpcId(action.properties.getVpcId());
        AsyncRequests.<AssociateDhcpOptionsType,AssociateDhcpOptionsResponseType> sendSync(configuration, associateDhcpOptionsType);
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
    DELETE_ASSOCIATION {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        AWSEC2VPCDHCPOptionsAssociationResourceAction action = (AWSEC2VPCDHCPOptionsAssociationResourceAction) resourceAction;
        ServiceConfiguration configuration = Topology.lookup(Compute.class);
        if (action.info.getPhysicalResourceId() == null) return action;

        // Check dhcp options (if not "default")
        if (!"default".equals(action.properties.getDhcpOptionsId())) {
          DescribeDhcpOptionsType describeDhcpOptionsType = MessageHelper.createMessage(DescribeDhcpOptionsType.class, action.info.getEffectiveUserId());
          action.setDhcpOptionsId(describeDhcpOptionsType, action.properties.getDhcpOptionsId());
          DescribeDhcpOptionsResponseType describeDhcpOptionsResponseType = AsyncRequests.<DescribeDhcpOptionsType,DescribeDhcpOptionsResponseType> sendSync(configuration, describeDhcpOptionsType);
          if (describeDhcpOptionsResponseType.getDhcpOptionsSet() == null || describeDhcpOptionsResponseType.getDhcpOptionsSet().getItem() == null || describeDhcpOptionsResponseType.getDhcpOptionsSet().getItem().isEmpty()) {
            return action;
          }
        }
        // check vpc
        DescribeVpcsType describeVpcsType = MessageHelper.createMessage(DescribeVpcsType.class, action.info.getEffectiveUserId());
        action.setVpcId(describeVpcsType, action.properties.getVpcId());
        DescribeVpcsResponseType describeVpcsResponseType = AsyncRequests.<DescribeVpcsType,DescribeVpcsResponseType> sendSync(configuration, describeVpcsType);
        if (describeVpcsResponseType.getVpcSet() == null || describeVpcsResponseType.getVpcSet().getItem() == null || describeVpcsResponseType.getVpcSet().getItem().isEmpty()) {
          return action;
        }
        AssociateDhcpOptionsType associateDhcpOptionsType = MessageHelper.createMessage(AssociateDhcpOptionsType.class, action.info.getEffectiveUserId());
        associateDhcpOptionsType.setDhcpOptionsId("default");
        associateDhcpOptionsType.setVpcId(action.properties.getVpcId());
        AsyncRequests.<AssociateDhcpOptionsType,AssociateDhcpOptionsResponseType> sendSync(configuration, associateDhcpOptionsType);
        return action;
      }

      @Override
      public RetryPolicy getRetryPolicy() {
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
    properties = (AWSEC2VPCDHCPOptionsAssociationProperties) resourceProperties;
  }

  @Override
  public ResourceInfo getResourceInfo() {
    return info;
  }

  @Override
  public void setResourceInfo(ResourceInfo resourceInfo) {
    info = (AWSEC2VPCDHCPOptionsAssociationResourceInfo) resourceInfo;
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

  private void setDhcpOptionsId(DescribeDhcpOptionsType describeDhcpOptionsType, String dhcpOptionsId) {
    DhcpOptionsIdSetType dhcpOptionsSet = new DhcpOptionsIdSetType();
    ArrayList<DhcpOptionsIdSetItemType> item = Lists.newArrayList();
    DhcpOptionsIdSetItemType dhcpOptionsIdSetItemType = new DhcpOptionsIdSetItemType();
    dhcpOptionsIdSetItemType.setDhcpOptionsId(dhcpOptionsId);
    item.add(dhcpOptionsIdSetItemType);
    dhcpOptionsSet.setItem(item);
    item.add(dhcpOptionsIdSetItemType);
    describeDhcpOptionsType.setDhcpOptionsSet(dhcpOptionsSet);
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


