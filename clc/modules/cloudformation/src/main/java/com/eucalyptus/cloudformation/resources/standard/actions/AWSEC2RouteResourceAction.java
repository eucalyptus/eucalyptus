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
import com.eucalyptus.cloudformation.ValidationErrorException;
import com.eucalyptus.cloudformation.resources.ResourceAction;
import com.eucalyptus.cloudformation.resources.ResourceInfo;
import com.eucalyptus.cloudformation.resources.ResourceProperties;
import com.eucalyptus.cloudformation.resources.standard.info.AWSEC2RouteResourceInfo;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.AWSEC2RouteProperties;
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
import com.eucalyptus.compute.common.CreateRouteResponseType;
import com.eucalyptus.compute.common.CreateRouteType;
import com.eucalyptus.compute.common.DeleteRouteResponseType;
import com.eucalyptus.compute.common.DeleteRouteType;
import com.eucalyptus.compute.common.DescribeRouteTablesResponseType;
import com.eucalyptus.compute.common.DescribeRouteTablesType;
import com.eucalyptus.compute.common.RouteTableIdSetItemType;
import com.eucalyptus.compute.common.RouteTableIdSetType;
import com.eucalyptus.compute.common.RouteTableType;
import com.eucalyptus.compute.common.RouteType;
import com.eucalyptus.util.async.AsyncRequests;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.netflix.glisten.WorkflowOperations;

import java.util.List;
import javax.annotation.Nullable;

/**
 * Created by ethomas on 2/3/14.
 */
public class AWSEC2RouteResourceAction extends ResourceAction {

  private AWSEC2RouteProperties properties = new AWSEC2RouteProperties();
  private AWSEC2RouteResourceInfo info = new AWSEC2RouteResourceInfo();

  public AWSEC2RouteResourceAction() {
    for (CreateSteps createStep: CreateSteps.values()) {
      createSteps.put(createStep.name(), createStep);
    }
    for (DeleteSteps deleteStep: DeleteSteps.values()) {
      deleteSteps.put(deleteStep.name(), deleteStep);
    }

  }
  private enum CreateSteps implements Step {
    CREATE_ROUTE {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        AWSEC2RouteResourceAction action = (AWSEC2RouteResourceAction) resourceAction;
        ServiceConfiguration configuration = Topology.lookup(Compute.class);
        // property validation
        action.validateProperties();
        // Make sure route table exists.
        if (action.properties.getRouteTableId().isEmpty()) {
          throw new ValidationErrorException("RouteTableId is a required field");
        }
        DescribeRouteTablesType describeRouteTablesType = MessageHelper.createMessage(DescribeRouteTablesType.class, action.info.getEffectiveUserId());
        RouteTableIdSetType routeTableIdSet = new RouteTableIdSetType();
        RouteTableIdSetItemType routeTableIdSetItem = new RouteTableIdSetItemType();
        routeTableIdSetItem.setRouteTableId(action.properties.getRouteTableId());
        routeTableIdSet.setItem(Lists.newArrayList(routeTableIdSetItem));
        describeRouteTablesType.setRouteTableIdSet(routeTableIdSet);
        DescribeRouteTablesResponseType describeRouteTablesResponseType = AsyncRequests.<DescribeRouteTablesType, DescribeRouteTablesResponseType>sendSync(configuration, describeRouteTablesType);
        if (describeRouteTablesResponseType.getRouteTableSet() == null || describeRouteTablesResponseType.getRouteTableSet().getItem() == null ||
          describeRouteTablesResponseType.getRouteTableSet().getItem().isEmpty()) {
          throw new ValidationErrorException("No such route table with id '" + action.properties.getRouteTableId());
        }
        CreateRouteType createRouteType = MessageHelper.createMessage(CreateRouteType.class, action.info.getEffectiveUserId());
        createRouteType.setRouteTableId(action.properties.getRouteTableId());
        if (!Strings.isNullOrEmpty(action.properties.getGatewayId())) {
          createRouteType.setGatewayId(action.properties.getGatewayId());
        }
        if (!Strings.isNullOrEmpty(action.properties.getInstanceId())) {
          createRouteType.setInstanceId(action.properties.getInstanceId());
        }
        if (!Strings.isNullOrEmpty(action.properties.getVpcPeeringConnectionId())) {
          createRouteType.setVpcPeeringConnectionId(action.properties.getVpcPeeringConnectionId());
        }
        if (!Strings.isNullOrEmpty(action.properties.getNetworkInterfaceId())) {
          createRouteType.setNetworkInterfaceId(action.properties.getNetworkInterfaceId());
        }
        createRouteType.setDestinationCidrBlock(action.properties.getDestinationCidrBlock());
        CreateRouteResponseType createRouteResponseType = AsyncRequests.<CreateRouteType, CreateRouteResponseType>sendSync(configuration, createRouteType);
        action.info.setPhysicalResourceId(action.getDefaultPhysicalResourceId());
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
    DELETE_ROUTE {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        AWSEC2RouteResourceAction action = (AWSEC2RouteResourceAction) resourceAction;
        ServiceConfiguration configuration = Topology.lookup(Compute.class);
        if (action.info.getPhysicalResourceId() == null) return action;

        DescribeRouteTablesType describeRouteTablesType = MessageHelper.createMessage(DescribeRouteTablesType.class, action.info.getEffectiveUserId());
        RouteTableIdSetType routeTableIdSet = new RouteTableIdSetType();
        RouteTableIdSetItemType routeTableIdSetItem = new RouteTableIdSetItemType();
        routeTableIdSetItem.setRouteTableId(action.properties.getRouteTableId());
        routeTableIdSet.setItem(Lists.newArrayList(routeTableIdSetItem));
        describeRouteTablesType.setRouteTableIdSet(routeTableIdSet);
        DescribeRouteTablesResponseType describeRouteTablesResponseType = AsyncRequests.<DescribeRouteTablesType, DescribeRouteTablesResponseType>sendSync(configuration, describeRouteTablesType);
        if (describeRouteTablesResponseType.getRouteTableSet() == null || describeRouteTablesResponseType.getRouteTableSet().getItem() == null ||
          describeRouteTablesResponseType.getRouteTableSet().getItem().isEmpty()) {
          return action;
        }
        // see if the route is there...
        boolean foundRoute = false;
        for (RouteTableType routeTableType: describeRouteTablesResponseType.getRouteTableSet().getItem()) {
          if (!action.properties.getRouteTableId().equals(routeTableType.getRouteTableId())) continue;
          if (routeTableType.getRouteSet() == null || routeTableType.getRouteSet().getItem() == null ||
            routeTableType.getRouteSet().getItem().isEmpty()) {
            continue; // no routes
          }
          for (RouteType routeType : routeTableType.getRouteSet().getItem()) {
            if (action.equalRoutes(action.properties, routeType)) {
              foundRoute = true;
              break;
            }
          }
        }
        if (!foundRoute) return action;
        DeleteRouteType deleteRouteType = MessageHelper.createMessage(DeleteRouteType.class, action.info.getEffectiveUserId());
        deleteRouteType.setRouteTableId(action.properties.getRouteTableId());
        deleteRouteType.setDestinationCidrBlock(action.properties.getDestinationCidrBlock());
        DeleteRouteResponseType deleteRouteResponseType = AsyncRequests.<DeleteRouteType, DeleteRouteResponseType>sendSync(configuration, deleteRouteType);
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
    properties = (AWSEC2RouteProperties) resourceProperties;
  }

  @Override
  public ResourceInfo getResourceInfo() {
    return info;
  }

  @Override
  public void setResourceInfo(ResourceInfo resourceInfo) {
    info = (AWSEC2RouteResourceInfo) resourceInfo;
  }

  private void validateProperties() throws ValidationErrorException {
    // You must provide only one of the following: a GatewayID, InstanceID, NetworkInterfaceId, or VpcPeeringConnectionId.
    List<String> oneOfTheseParams = Lists.newArrayList(properties.getGatewayId(), properties.getInstanceId(),
      properties.getNetworkInterfaceId(), properties.getVpcPeeringConnectionId());
    int numNonNullOrEmpty = 0;
    for (String item: oneOfTheseParams) {
      if (!Strings.isNullOrEmpty(item)) numNonNullOrEmpty++;
    }
    if (numNonNullOrEmpty != 1) {
      throw new ValidationErrorException("Exactly one of GatewayID, InstanceID, NetworkInterfaceId, or VpcPeeringConnectionId must be specified");
    }
  }

  private boolean equalRoutes(AWSEC2RouteProperties properties, RouteType routeType) {
    if (!properties.getDestinationCidrBlock().equals(routeType.getDestinationCidrBlock())) return false;
    if (differentIfNotNullOrEmpty(properties.getInstanceId(), routeType.getInstanceId())) return false;
    if (differentIfNotNullOrEmpty(properties.getGatewayId(), routeType.getGatewayId())) return false;
    if (differentIfNotNullOrEmpty(properties.getNetworkInterfaceId(), routeType.getNetworkInterfaceId())) return false;
    if (differentIfNotNullOrEmpty(properties.getVpcPeeringConnectionId(), routeType.getVpcPeeringConnectionId())) return false;
    return true;
  }

  private boolean differentIfNotNullOrEmpty(String s1, String s2) {
    if (Strings.isNullOrEmpty(s1) && Strings.isNullOrEmpty(s2)) return false;
    if (Strings.isNullOrEmpty(s1) != Strings.isNullOrEmpty(s2)) return true;
    return !s1.equals(s2);
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


