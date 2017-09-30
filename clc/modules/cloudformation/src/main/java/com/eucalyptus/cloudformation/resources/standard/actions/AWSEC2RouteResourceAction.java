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
import com.eucalyptus.cloudformation.resources.standard.info.AWSEC2RouteResourceInfo;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.AWSEC2RouteProperties;
import com.eucalyptus.cloudformation.template.JsonHelper;
import com.eucalyptus.cloudformation.util.MessageHelper;
import com.eucalyptus.cloudformation.workflow.steps.Step;
import com.eucalyptus.cloudformation.workflow.steps.StepBasedResourceAction;
import com.eucalyptus.cloudformation.workflow.steps.UpdateStep;
import com.eucalyptus.cloudformation.workflow.updateinfo.UpdateType;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.Topology;
import com.eucalyptus.compute.common.CloudFilters;
import com.eucalyptus.compute.common.Compute;
import com.eucalyptus.compute.common.CreateRouteResponseType;
import com.eucalyptus.compute.common.CreateRouteType;
import com.eucalyptus.compute.common.DeleteRouteResponseType;
import com.eucalyptus.compute.common.DeleteRouteType;
import com.eucalyptus.compute.common.DescribeRouteTablesResponseType;
import com.eucalyptus.compute.common.DescribeRouteTablesType;
import com.eucalyptus.compute.common.ReplaceRouteResponseType;
import com.eucalyptus.compute.common.ReplaceRouteType;
import com.eucalyptus.compute.common.RouteTableType;
import com.eucalyptus.compute.common.RouteType;
import com.eucalyptus.util.async.AsyncRequests;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;

/**
 * Created by ethomas on 2/3/14.
 */
public class AWSEC2RouteResourceAction extends StepBasedResourceAction {

  private AWSEC2RouteProperties properties = new AWSEC2RouteProperties();
  private AWSEC2RouteResourceInfo info = new AWSEC2RouteResourceInfo();

  public AWSEC2RouteResourceAction() {
    super(fromEnum(CreateSteps.class), fromEnum(DeleteSteps.class), fromUpdateEnum(UpdateNoInterruptionSteps.class), null);
  }

  @Override
  public UpdateType getUpdateType(ResourceAction resourceAction, boolean stackTagsChanged) {
    UpdateType updateType = info.supportsTags() && stackTagsChanged ? UpdateType.NO_INTERRUPTION : UpdateType.NONE;
    AWSEC2RouteResourceAction otherAction = (AWSEC2RouteResourceAction) resourceAction;
    if (!Objects.equals(properties.getDestinationCidrBlock(), otherAction.properties.getDestinationCidrBlock())) {
      updateType = UpdateType.max(updateType, UpdateType.NEEDS_REPLACEMENT);
    }
    if (!Objects.equals(properties.getGatewayId(), otherAction.properties.getGatewayId())) {
      updateType = UpdateType.max(updateType, UpdateType.NO_INTERRUPTION);
    }
    if (!Objects.equals(properties.getInstanceId(), otherAction.properties.getInstanceId())) {
      updateType = UpdateType.max(updateType, UpdateType.NO_INTERRUPTION);
    }
    if (!Objects.equals(properties.getNatGatewayId(), otherAction.properties.getNatGatewayId())) {
      updateType = UpdateType.max(updateType, UpdateType.NO_INTERRUPTION);
    }
    if (!Objects.equals(properties.getNetworkInterfaceId(), otherAction.properties.getNetworkInterfaceId())) {
      updateType = UpdateType.max(updateType, UpdateType.NO_INTERRUPTION);
    }
    if (!Objects.equals(properties.getRouteTableId(), otherAction.properties.getRouteTableId())) {
      updateType = UpdateType.max(updateType, UpdateType.NEEDS_REPLACEMENT);
    }
    if (!Objects.equals(properties.getVpcPeeringConnectionId(), otherAction.properties.getVpcPeeringConnectionId())) {
      updateType = UpdateType.max(updateType, UpdateType.NO_INTERRUPTION);
    }
    return updateType;
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
        describeRouteTablesType.getFilterSet( ).add( CloudFilters.filter( "route-table-id", action.properties.getRouteTableId( ) ) );
        DescribeRouteTablesResponseType describeRouteTablesResponseType = AsyncRequests.sendSync(configuration, describeRouteTablesType);
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
        if (!Strings.isNullOrEmpty(action.properties.getNatGatewayId())) {
          createRouteType.setNatGatewayId(action.properties.getNatGatewayId());
        }
        if (!Strings.isNullOrEmpty(action.properties.getNetworkInterfaceId())) {
          createRouteType.setNetworkInterfaceId(action.properties.getNetworkInterfaceId());
        }
        createRouteType.setDestinationCidrBlock(action.properties.getDestinationCidrBlock());
        CreateRouteResponseType createRouteResponseType = AsyncRequests.<CreateRouteType, CreateRouteResponseType>sendSync(configuration, createRouteType);
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
    DELETE_ROUTE {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        AWSEC2RouteResourceAction action = (AWSEC2RouteResourceAction) resourceAction;
        ServiceConfiguration configuration = Topology.lookup(Compute.class);
        if (!Boolean.TRUE.equals(action.info.getCreatedEnoughToDelete())) return action;

        DescribeRouteTablesType describeRouteTablesType = MessageHelper.createMessage(DescribeRouteTablesType.class, action.info.getEffectiveUserId());
        describeRouteTablesType.getFilterSet( ).add( CloudFilters.filter( "route-table-id", action.properties.getRouteTableId( ) ) );
        DescribeRouteTablesResponseType describeRouteTablesResponseType = AsyncRequests.sendSync(configuration, describeRouteTablesType);
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

  private enum UpdateNoInterruptionSteps implements UpdateStep {
    UPDATE_ROUTE {
      @Override
      public ResourceAction perform(ResourceAction oldResourceAction, ResourceAction newResourceAction) throws Exception {
        AWSEC2RouteResourceAction oldAction = (AWSEC2RouteResourceAction) oldResourceAction;
        AWSEC2RouteResourceAction newAction = (AWSEC2RouteResourceAction) newResourceAction;
        ServiceConfiguration configuration = Topology.lookup(Compute.class);
        // property validation
        newAction.validateProperties();
        // Make sure route table exists.
        if (newAction.properties.getRouteTableId().isEmpty()) {
          throw new ValidationErrorException("RouteTableId is a required field");
        }
        DescribeRouteTablesType describeRouteTablesType = MessageHelper.createMessage(DescribeRouteTablesType.class, newAction.info.getEffectiveUserId());
        describeRouteTablesType.getFilterSet( ).add( CloudFilters.filter( "route-table-id", newAction.properties.getRouteTableId( ) ) );
        DescribeRouteTablesResponseType describeRouteTablesResponseType = AsyncRequests.sendSync(configuration, describeRouteTablesType);
        if (describeRouteTablesResponseType.getRouteTableSet() == null || describeRouteTablesResponseType.getRouteTableSet().getItem() == null ||
          describeRouteTablesResponseType.getRouteTableSet().getItem().isEmpty()) {
          throw new ValidationErrorException("No such route table with id '" + newAction.properties.getRouteTableId());
        }
        ReplaceRouteType replaceRouteType = MessageHelper.createMessage(ReplaceRouteType.class, newAction.info.getEffectiveUserId());
        replaceRouteType.setRouteTableId(newAction.properties.getRouteTableId());
        if (!Strings.isNullOrEmpty(newAction.properties.getGatewayId())) {
          replaceRouteType.setGatewayId(newAction.properties.getGatewayId());
        }
        if (!Strings.isNullOrEmpty(newAction.properties.getInstanceId())) {
          replaceRouteType.setInstanceId(newAction.properties.getInstanceId());
        }
        if (!Strings.isNullOrEmpty(newAction.properties.getVpcPeeringConnectionId())) {
          replaceRouteType.setVpcPeeringConnectionId(newAction.properties.getVpcPeeringConnectionId());
        }
        if (!Strings.isNullOrEmpty(newAction.properties.getNatGatewayId())) {
          replaceRouteType.setNatGatewayId(newAction.properties.getNatGatewayId());
        }
        if (!Strings.isNullOrEmpty(newAction.properties.getNetworkInterfaceId())) {
          replaceRouteType.setNetworkInterfaceId(newAction.properties.getNetworkInterfaceId());
        }
        replaceRouteType.setDestinationCidrBlock(newAction.properties.getDestinationCidrBlock());
        ReplaceRouteResponseType replaceRouteResponseType = AsyncRequests.<ReplaceRouteType, ReplaceRouteResponseType>sendSync(configuration, replaceRouteType);
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
    // You must provide only one of the following: a GatewayID, InstanceID, NatGatewayIdm NetworkInterfaceId, or VpcPeeringConnectionId.
    List<String> oneOfTheseParams = Lists.newArrayList(properties.getGatewayId(), properties.getInstanceId(), properties.getNatGatewayId(),
      properties.getNetworkInterfaceId(), properties.getVpcPeeringConnectionId());
    int numNonNullOrEmpty = 0;
    for (String item: oneOfTheseParams) {
      if (!Strings.isNullOrEmpty(item)) numNonNullOrEmpty++;
    }
    if (numNonNullOrEmpty != 1) {
      throw new ValidationErrorException("Exactly one of GatewayID, InstanceID, NatGatewayId, NetworkInterfaceId, or VpcPeeringConnectionId must be specified");
    }
  }

  private boolean equalRoutes(AWSEC2RouteProperties properties, RouteType routeType) {
    if (!properties.getDestinationCidrBlock().equals(routeType.getDestinationCidrBlock())) return false;
    if (differentIfNotNullOrEmpty(properties.getInstanceId(), routeType.getInstanceId())) return false;
    if (differentIfNotNullOrEmpty(properties.getGatewayId(), routeType.getGatewayId())) return false;
    if (differentIfNotNullOrEmpty(properties.getNatGatewayId(), routeType.getNatGatewayId())) return false;
    if (differentIfNotNullOrEmpty(properties.getNetworkInterfaceId(), routeType.getNetworkInterfaceId())) return false;
    if (differentIfNotNullOrEmpty(properties.getVpcPeeringConnectionId(), routeType.getVpcPeeringConnectionId())) return false;
    return true;
  }

  private boolean differentIfNotNullOrEmpty(String s1, String s2) {
    if (Strings.isNullOrEmpty(s1) && Strings.isNullOrEmpty(s2)) return false;
    if (Strings.isNullOrEmpty(s1) != Strings.isNullOrEmpty(s2)) return true;
    return !s1.equals(s2);
  }



}


