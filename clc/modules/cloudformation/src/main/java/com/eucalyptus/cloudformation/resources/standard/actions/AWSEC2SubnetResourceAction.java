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
import com.eucalyptus.auth.Accounts;
import com.eucalyptus.cloudformation.resources.EC2Helper;
import com.eucalyptus.cloudformation.resources.ResourceAction;
import com.eucalyptus.cloudformation.resources.ResourceInfo;
import com.eucalyptus.cloudformation.resources.ResourceProperties;
import com.eucalyptus.cloudformation.resources.standard.TagHelper;
import com.eucalyptus.cloudformation.resources.standard.info.AWSEC2SubnetResourceInfo;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.AWSEC2SubnetProperties;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.EC2Tag;
import com.eucalyptus.cloudformation.template.JsonHelper;
import com.eucalyptus.cloudformation.util.MessageHelper;
import com.eucalyptus.cloudformation.workflow.StackActivity;
import com.eucalyptus.cloudformation.workflow.steps.CreateMultiStepPromise;
import com.eucalyptus.cloudformation.workflow.steps.DeleteMultiStepPromise;
import com.eucalyptus.cloudformation.workflow.steps.Step;
import com.eucalyptus.cloudformation.workflow.steps.StepTransform;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.Topology;
import com.eucalyptus.compute.common.Compute;
import com.eucalyptus.compute.common.CreateSubnetResponseType;
import com.eucalyptus.compute.common.CreateSubnetType;
import com.eucalyptus.compute.common.CreateTagsResponseType;
import com.eucalyptus.compute.common.CreateTagsType;
import com.eucalyptus.compute.common.DeleteSubnetResponseType;
import com.eucalyptus.compute.common.DeleteSubnetType;
import com.eucalyptus.compute.common.DescribeSubnetsResponseType;
import com.eucalyptus.compute.common.DescribeSubnetsType;
import com.eucalyptus.compute.common.DescribeVpcsResponseType;
import com.eucalyptus.compute.common.DescribeVpcsType;
import com.eucalyptus.compute.common.SubnetIdSetItemType;
import com.eucalyptus.compute.common.SubnetIdSetType;
import com.eucalyptus.compute.common.VpcIdSetItemType;
import com.eucalyptus.compute.common.VpcIdSetType;
import com.eucalyptus.util.async.AsyncRequests;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.collect.Lists;
import com.netflix.glisten.WorkflowOperations;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;

/**
 * Created by ethomas on 2/3/14.
 */
public class AWSEC2SubnetResourceAction extends ResourceAction {

  private AWSEC2SubnetProperties properties = new AWSEC2SubnetProperties();
  private AWSEC2SubnetResourceInfo info = new AWSEC2SubnetResourceInfo();

  public AWSEC2SubnetResourceAction() {
    for (CreateSteps createStep: CreateSteps.values()) {
      createSteps.put(createStep.name(), createStep);
    }
    for (DeleteSteps deleteStep: DeleteSteps.values()) {
      deleteSteps.put(deleteStep.name(), deleteStep);
    }

  }
  private enum CreateSteps implements Step {
    CREATE_ROUTE_TABLE {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        AWSEC2SubnetResourceAction action = (AWSEC2SubnetResourceAction) resourceAction;
        ServiceConfiguration configuration = Topology.lookup(Compute.class);
        CreateSubnetType createSubnetType = MessageHelper.createMessage(CreateSubnetType.class, action.info.getEffectiveUserId());
        createSubnetType.setVpcId(action.properties.getVpcId());
        if (action.properties.getAvailabilityZone() != null) {
          createSubnetType.setAvailabilityZone(action.properties.getAvailabilityZone());
        }
        createSubnetType.setCidrBlock(action.properties.getCidrBlock());
        CreateSubnetResponseType createSubnetResponseType = AsyncRequests.<CreateSubnetType,CreateSubnetResponseType> sendSync(configuration, createSubnetType);
        action.info.setPhysicalResourceId(createSubnetResponseType.getSubnet().getSubnetId());
        action.info.setReferenceValueJson(JsonHelper.getStringFromJsonNode(new TextNode(action.info.getPhysicalResourceId())));
        action.info.setAvailabilityZone(JsonHelper.getStringFromJsonNode(new TextNode(createSubnetResponseType.getSubnet().getAvailabilityZone())));
        return action;
      }
    },
    CREATE_TAGS {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        AWSEC2SubnetResourceAction action = (AWSEC2SubnetResourceAction) resourceAction;
        ServiceConfiguration configuration = Topology.lookup(Compute.class);
        // Create 'system' tags as admin user
        String effectiveAdminUserId = Accounts.lookupUserById(action.info.getEffectiveUserId()).getAccount().lookupAdmin().getUserId();
        CreateTagsType createSystemTagsType = MessageHelper.createPrivilegedMessage(CreateTagsType.class, effectiveAdminUserId);
        createSystemTagsType.setResourcesSet(Lists.newArrayList(action.info.getPhysicalResourceId()));
        createSystemTagsType.setTagSet(EC2Helper.createTagSet(TagHelper.getEC2SystemTags(action.info, action.getStackEntity())));
        AsyncRequests.<CreateTagsType, CreateTagsResponseType>sendSync(configuration, createSystemTagsType);
        // Create non-system tags as regular user
        List<EC2Tag> tags = TagHelper.getEC2StackTags(action.getStackEntity());
        if (action.properties.getTags() != null && !action.properties.getTags().isEmpty()) {
          TagHelper.checkReservedEC2TemplateTags(action.properties.getTags());
          tags.addAll(action.properties.getTags());
        }
        if (!tags.isEmpty()) {
          CreateTagsType createTagsType = MessageHelper.createMessage(CreateTagsType.class, action.info.getEffectiveUserId());
          createTagsType.setResourcesSet(Lists.newArrayList(action.info.getPhysicalResourceId()));
          createTagsType.setTagSet(EC2Helper.createTagSet(tags));
          AsyncRequests.<CreateTagsType, CreateTagsResponseType>sendSync(configuration, createTagsType);
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
    DELETE_ROUTE_TABLE {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        AWSEC2SubnetResourceAction action = (AWSEC2SubnetResourceAction) resourceAction;
        ServiceConfiguration configuration = Topology.lookup(Compute.class);
        if (action.info.getPhysicalResourceId() == null) return action;

        // Check vpc (return if gone)
        DescribeVpcsType describeVpcsType = MessageHelper.createMessage(DescribeVpcsType.class, action.info.getEffectiveUserId());
        action.setVpcId(describeVpcsType, action.properties.getVpcId());
        DescribeVpcsResponseType describeVpcsResponseType = AsyncRequests.<DescribeVpcsType,DescribeVpcsResponseType> sendSync(configuration, describeVpcsType);
        if (describeVpcsResponseType.getVpcSet() == null || describeVpcsResponseType.getVpcSet().getItem() == null || describeVpcsResponseType.getVpcSet().getItem().isEmpty()) {
          return action; // already deleted
        }
        // check subnet (return if gone)
        DescribeSubnetsType describeSubnetsType = MessageHelper.createMessage(DescribeSubnetsType.class, action.info.getEffectiveUserId());
        action.setSubnetId(describeSubnetsType, action.info.getPhysicalResourceId());
        DescribeSubnetsResponseType describeSubnetsResponseType = AsyncRequests.<DescribeSubnetsType,DescribeSubnetsResponseType> sendSync(configuration, describeSubnetsType);
        if (describeSubnetsResponseType.getSubnetSet() == null || describeSubnetsResponseType.getSubnetSet().getItem() == null || describeSubnetsResponseType.getSubnetSet().getItem().isEmpty()) {
          return action; // already deleted
        }
        DeleteSubnetType deleteSubnetType = MessageHelper.createMessage(DeleteSubnetType.class, action.info.getEffectiveUserId());
        deleteSubnetType.setSubnetId(action.info.getPhysicalResourceId());
        AsyncRequests.<DeleteSubnetType,DeleteSubnetResponseType> sendSync(configuration, deleteSubnetType);
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
    properties = (AWSEC2SubnetProperties) resourceProperties;
  }

  @Override
  public ResourceInfo getResourceInfo() {
    return info;
  }

  @Override
  public void setResourceInfo(ResourceInfo resourceInfo) {
    info = (AWSEC2SubnetResourceInfo) resourceInfo;
  }


  private void setSubnetId(DescribeSubnetsType describeSubnetsType, String subnetId) {
    SubnetIdSetType subnetSet = new SubnetIdSetType();
    describeSubnetsType.setSubnetSet(subnetSet);

    ArrayList<SubnetIdSetItemType> item = Lists.newArrayList();
    subnetSet.setItem(item);

    SubnetIdSetItemType subnetIdSetItem = new SubnetIdSetItemType();
    item.add(subnetIdSetItem);

    subnetIdSetItem.setSubnetId(subnetId);
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
    return new CreateMultiStepPromise(workflowOperations, stepIds, this).getCreatePromise(resourceId, stackId, accountId, effectiveUserId);
  }

  @Override
  public Promise<String> getDeletePromise(WorkflowOperations<StackActivity> workflowOperations, String resourceId, String stackId, String accountId, String effectiveUserId) {
    List<String> stepIds = Lists.transform(Lists.newArrayList(DeleteSteps.values()), StepTransform.INSTANCE);
    return new DeleteMultiStepPromise(workflowOperations, stepIds, this).getDeletePromise(resourceId, stackId, accountId, effectiveUserId);
  }

}


