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
import com.eucalyptus.cloudformation.resources.EC2Helper;
import com.eucalyptus.cloudformation.resources.ResourceAction;
import com.eucalyptus.cloudformation.resources.ResourceInfo;
import com.eucalyptus.cloudformation.resources.ResourceProperties;
import com.eucalyptus.cloudformation.resources.standard.TagHelper;
import com.eucalyptus.cloudformation.resources.standard.info.AWSEC2RouteTableResourceInfo;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.AWSEC2RouteTableProperties;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.EC2Tag;
import com.eucalyptus.cloudformation.template.JsonHelper;
import com.eucalyptus.cloudformation.util.MessageHelper;
import com.eucalyptus.cloudformation.workflow.CreateStackWorkflowImpl;
import com.eucalyptus.cloudformation.workflow.DeleteStackWorkflowImpl;
import com.eucalyptus.cloudformation.workflow.steps.MultiStepWithRetryCreatePromise;
import com.eucalyptus.cloudformation.workflow.steps.MultiStepWithRetryDeletePromise;
import com.eucalyptus.cloudformation.workflow.steps.Step;
import com.eucalyptus.cloudformation.workflow.steps.StepTransform;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.Topology;
import com.eucalyptus.compute.common.Compute;
import com.eucalyptus.compute.common.CreateRouteTableResponseType;
import com.eucalyptus.compute.common.CreateRouteTableType;
import com.eucalyptus.compute.common.CreateTagsResponseType;
import com.eucalyptus.compute.common.CreateTagsType;
import com.eucalyptus.compute.common.DeleteRouteTableResponseType;
import com.eucalyptus.compute.common.DeleteRouteTableType;
import com.eucalyptus.compute.common.DescribeRouteTablesResponseType;
import com.eucalyptus.compute.common.DescribeRouteTablesType;
import com.eucalyptus.compute.common.RouteTableIdSetItemType;
import com.eucalyptus.compute.common.RouteTableIdSetType;
import com.eucalyptus.util.async.AsyncRequests;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.collect.Lists;

import java.util.List;

/**
 * Created by ethomas on 2/3/14.
 */
public class AWSEC2RouteTableResourceAction extends ResourceAction {

  private AWSEC2RouteTableProperties properties = new AWSEC2RouteTableProperties();
  private AWSEC2RouteTableResourceInfo info = new AWSEC2RouteTableResourceInfo();

  public AWSEC2RouteTableResourceAction() {
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
        AWSEC2RouteTableResourceAction action = (AWSEC2RouteTableResourceAction) resourceAction;
        ServiceConfiguration configuration = Topology.lookup(Compute.class);
        CreateRouteTableType createRouteTableType = MessageHelper.createMessage(CreateRouteTableType.class, action.info.getEffectiveUserId());
        createRouteTableType.setVpcId(action.properties.getVpcId());
        CreateRouteTableResponseType createRouteTableResponseType = AsyncRequests.<CreateRouteTableType, CreateRouteTableResponseType> sendSync(configuration, createRouteTableType);
        action.info.setPhysicalResourceId(createRouteTableResponseType.getRouteTable().getRouteTableId());
        action.info.setReferenceValueJson(JsonHelper.getStringFromJsonNode(new TextNode(action.info.getPhysicalResourceId())));
        return action;
      }

      @Override
      public RetryPolicy getRetryPolicy() {
        return null;
      }
    },
    CREATE_TAGS {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        AWSEC2RouteTableResourceAction action = (AWSEC2RouteTableResourceAction) resourceAction;
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
      @Override
      public RetryPolicy getRetryPolicy() {
        return null;
      }
    }
  }

  private enum DeleteSteps implements Step {
    DELETE_ROUTE_TABLE {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        AWSEC2RouteTableResourceAction action = (AWSEC2RouteTableResourceAction) resourceAction;
        ServiceConfiguration configuration = Topology.lookup(Compute.class);
        if (action.info.getPhysicalResourceId() == null) return action;

        // See if route table is there
        DescribeRouteTablesType describeRouteTablesType = MessageHelper.createMessage(DescribeRouteTablesType.class, action.info.getEffectiveUserId());
        RouteTableIdSetType routeTableIdSet = new RouteTableIdSetType();
        RouteTableIdSetItemType routeTableIdSetItem = new RouteTableIdSetItemType();
        routeTableIdSetItem.setRouteTableId(action.info.getPhysicalResourceId());
        routeTableIdSet.setItem(Lists.newArrayList(routeTableIdSetItem));
        describeRouteTablesType.setRouteTableIdSet(routeTableIdSet);
        DescribeRouteTablesResponseType describeRouteTablesResponseType = AsyncRequests.<DescribeRouteTablesType, DescribeRouteTablesResponseType> sendSync(configuration, describeRouteTablesType);
        if (describeRouteTablesResponseType.getRouteTableSet() == null || describeRouteTablesResponseType.getRouteTableSet().getItem() == null ||
          describeRouteTablesResponseType.getRouteTableSet().getItem().isEmpty()) {
          return action; // no route table
        }
        DeleteRouteTableType DeleteRouteTableType = MessageHelper.createMessage(DeleteRouteTableType.class, action.info.getEffectiveUserId());
        DeleteRouteTableType.setRouteTableId(action.info.getPhysicalResourceId());
        DeleteRouteTableResponseType DeleteRouteTableResponseType = AsyncRequests.<DeleteRouteTableType, DeleteRouteTableResponseType> sendSync(configuration, DeleteRouteTableType);
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
    properties = (AWSEC2RouteTableProperties) resourceProperties;
  }

  @Override
  public ResourceInfo getResourceInfo() {
    return info;
  }

  @Override
  public void setResourceInfo(ResourceInfo resourceInfo) {
    info = (AWSEC2RouteTableResourceInfo) resourceInfo;
  }

  @Override
  public Promise<String> getCreatePromise(CreateStackWorkflowImpl createStackWorkflow, String resourceId, String stackId, String accountId, String effectiveUserId) {
    List<String> stepIds = Lists.transform(Lists.newArrayList(CreateSteps.values()), StepTransform.INSTANCE);
    return new MultiStepWithRetryCreatePromise(createStackWorkflow, stepIds, this).getCreatePromise(resourceId, stackId, accountId, effectiveUserId);
  }

  @Override
  public Promise<String> getDeletePromise(DeleteStackWorkflowImpl deleteStackWorkflow, String resourceId, String stackId, String accountId, String effectiveUserId) {
    List<String> stepIds = Lists.transform(Lists.newArrayList(DeleteSteps.values()), StepTransform.INSTANCE);
    return new MultiStepWithRetryDeletePromise(deleteStackWorkflow, stepIds, this).getDeletePromise(resourceId, stackId, accountId, effectiveUserId);
  }

}



