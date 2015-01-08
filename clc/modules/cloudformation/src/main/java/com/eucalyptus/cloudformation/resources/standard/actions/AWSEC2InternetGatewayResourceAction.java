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
import com.eucalyptus.cloudformation.resources.EC2Helper;
import com.eucalyptus.cloudformation.resources.ResourceAction;
import com.eucalyptus.cloudformation.resources.ResourceInfo;
import com.eucalyptus.cloudformation.resources.ResourceProperties;
import com.eucalyptus.cloudformation.resources.standard.TagHelper;
import com.eucalyptus.cloudformation.resources.standard.info.AWSEC2InternetGatewayResourceInfo;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.AWSEC2InternetGatewayProperties;
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
import com.eucalyptus.compute.common.CreateInternetGatewayResponseType;
import com.eucalyptus.compute.common.CreateInternetGatewayType;
import com.eucalyptus.compute.common.CreateTagsResponseType;
import com.eucalyptus.compute.common.CreateTagsType;
import com.eucalyptus.compute.common.DeleteInternetGatewayResponseType;
import com.eucalyptus.compute.common.DeleteInternetGatewayType;
import com.eucalyptus.compute.common.DescribeInternetGatewaysResponseType;
import com.eucalyptus.compute.common.DescribeInternetGatewaysType;
import com.eucalyptus.compute.common.InternetGatewayIdSetItemType;
import com.eucalyptus.compute.common.InternetGatewayIdSetType;
import com.eucalyptus.util.async.AsyncRequests;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.collect.Lists;
import com.netflix.glisten.WorkflowOperations;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ethomas on 2/3/14.
 */
public class AWSEC2InternetGatewayResourceAction extends ResourceAction {

  private AWSEC2InternetGatewayProperties properties = new AWSEC2InternetGatewayProperties();
  private AWSEC2InternetGatewayResourceInfo info = new AWSEC2InternetGatewayResourceInfo();

  public AWSEC2InternetGatewayResourceAction() {
    for (CreateSteps createStep: CreateSteps.values()) {
      createSteps.put(createStep.name(), createStep);
    }
    for (DeleteSteps deleteStep: DeleteSteps.values()) {
      deleteSteps.put(deleteStep.name(), deleteStep);
    }

  }
  private enum CreateSteps implements Step {
    CREATE_GATEWAY {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        AWSEC2InternetGatewayResourceAction action = (AWSEC2InternetGatewayResourceAction) resourceAction;
        ServiceConfiguration configuration = Topology.lookup(Compute.class);
        CreateInternetGatewayType createInternetGatewayType = MessageHelper.createMessage(CreateInternetGatewayType.class, action.info.getEffectiveUserId());
        CreateInternetGatewayResponseType createInternetGatewayResponseType = AsyncRequests.<CreateInternetGatewayType,CreateInternetGatewayResponseType> sendSync(configuration, createInternetGatewayType);
        action.info.setPhysicalResourceId(createInternetGatewayResponseType.getInternetGateway().getInternetGatewayId());
        action.info.setReferenceValueJson(JsonHelper.getStringFromJsonNode(new TextNode(action.info.getPhysicalResourceId())));
        return action;
      }
    },
    CREATE_TAGS {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        AWSEC2InternetGatewayResourceAction action = (AWSEC2InternetGatewayResourceAction) resourceAction;
        ServiceConfiguration configuration = Topology.lookup(Compute.class);
        List<EC2Tag> tags = TagHelper.getEC2StackTags(action.info, action.getStackEntity());
        if (action.properties.getTags() != null && !action.properties.getTags().isEmpty()) {
          TagHelper.checkReservedEC2TemplateTags(action.properties.getTags());
          tags.addAll(action.properties.getTags());
        }
        // due to stack aws: tags
        CreateTagsType createTagsType = MessageHelper.createPrivilegedMessage(CreateTagsType.class, action.info.getEffectiveUserId());
        createTagsType.setResourcesSet(Lists.newArrayList(action.info.getPhysicalResourceId()));
        createTagsType.setTagSet(EC2Helper.createTagSet(tags));
        AsyncRequests.<CreateTagsType, CreateTagsResponseType>sendSync(configuration, createTagsType);
        return action;
      }
    };

    @Override
    public Integer getTimeout( ) {
      return null;
    }
  }

  private enum DeleteSteps implements Step {
    DELETE_GATEWAY {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        AWSEC2InternetGatewayResourceAction action = (AWSEC2InternetGatewayResourceAction) resourceAction;
        ServiceConfiguration configuration = Topology.lookup(Compute.class);
        if (action.info.getPhysicalResourceId() == null) return action;
        // Check gateway (return if gone)
        DescribeInternetGatewaysType describeInternetGatewaysType = MessageHelper.createMessage(DescribeInternetGatewaysType.class, action.info.getEffectiveUserId());
        action.setInternetGatewayId(describeInternetGatewaysType, action.info.getPhysicalResourceId());
        DescribeInternetGatewaysResponseType describeInternetGatewaysResponseType = AsyncRequests.<DescribeInternetGatewaysType,DescribeInternetGatewaysResponseType> sendSync(configuration, describeInternetGatewaysType);
        if (describeInternetGatewaysResponseType.getInternetGatewaySet() == null || describeInternetGatewaysResponseType.getInternetGatewaySet().getItem() == null || describeInternetGatewaysResponseType.getInternetGatewaySet().getItem().isEmpty()) {
          return action; // already deleted
        }
        DeleteInternetGatewayType deleteInternetGatewayType = MessageHelper.createMessage(DeleteInternetGatewayType.class, action.info.getEffectiveUserId());
        deleteInternetGatewayType.setInternetGatewayId(action.info.getPhysicalResourceId());
        AsyncRequests.<DeleteInternetGatewayType,DeleteInternetGatewayResponseType> sendSync(configuration, deleteInternetGatewayType);
        return action;
      }
    };

    @Override
    public Integer getTimeout( ) {
      return null;
    }
  }

  @Override
  public ResourceProperties getResourceProperties() {
    return properties;
  }

  @Override
  public void setResourceProperties(ResourceProperties resourceProperties) {
    properties = (AWSEC2InternetGatewayProperties) resourceProperties;
  }

  @Override
  public ResourceInfo getResourceInfo() {
    return info;
  }

  @Override
  public void setResourceInfo(ResourceInfo resourceInfo) {
    info = (AWSEC2InternetGatewayResourceInfo) resourceInfo;
  }

  private void setInternetGatewayId(DescribeInternetGatewaysType describeInternetGatewaysType, String internetGatewayId) {
    InternetGatewayIdSetType internetGatewaySet = new InternetGatewayIdSetType();
    describeInternetGatewaysType.setInternetGatewayIdSet(internetGatewaySet);

    ArrayList<InternetGatewayIdSetItemType> item = Lists.newArrayList();
    internetGatewaySet.setItem(item);

    InternetGatewayIdSetItemType internetGatewayIdSetItemType = new InternetGatewayIdSetItemType();
    item.add(internetGatewayIdSetItemType);

    internetGatewayIdSetItemType.setInternetGatewayId(internetGatewayId);
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



