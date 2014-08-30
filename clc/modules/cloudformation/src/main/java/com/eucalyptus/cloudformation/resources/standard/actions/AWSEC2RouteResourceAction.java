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


import com.eucalyptus.cloudformation.ValidationErrorException;
import com.eucalyptus.cloudformation.resources.ResourceAction;
import com.eucalyptus.cloudformation.resources.ResourceInfo;
import com.eucalyptus.cloudformation.resources.ResourceProperties;
import com.eucalyptus.cloudformation.resources.standard.info.AWSEC2RouteResourceInfo;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.AWSEC2RouteProperties;
import com.eucalyptus.cloudformation.template.JsonHelper;
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

import java.util.List;

/**
 * Created by ethomas on 2/3/14.
 */
public class AWSEC2RouteResourceAction extends ResourceAction {

  private AWSEC2RouteProperties properties = new AWSEC2RouteProperties();
  private AWSEC2RouteResourceInfo info = new AWSEC2RouteResourceInfo();
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

  @Override
  public void create(int stepNum) throws Exception {
    ServiceConfiguration configuration = Topology.lookup(Compute.class);
    switch (stepNum) {
      case 0:
        // property validation
        validateProperties();
        // Make sure route table exists.
        if (properties.getRouteTableId().isEmpty()) {
          throw new ValidationErrorException("RouteTableId is a required field");
        }
        DescribeRouteTablesType describeRouteTablesType = new DescribeRouteTablesType();
        describeRouteTablesType.setEffectiveUserId(info.getEffectiveUserId());
        RouteTableIdSetType routeTableIdSet = new RouteTableIdSetType();
        RouteTableIdSetItemType routeTableIdSetItem = new RouteTableIdSetItemType();
        routeTableIdSetItem.setRouteTableId(properties.getRouteTableId());
        routeTableIdSet.setItem(Lists.newArrayList(routeTableIdSetItem));
        describeRouteTablesType.setRouteTableIdSet(routeTableIdSet);
        DescribeRouteTablesResponseType describeRouteTablesResponseType = AsyncRequests.<DescribeRouteTablesType, DescribeRouteTablesResponseType>sendSync(configuration, describeRouteTablesType);
        if (describeRouteTablesResponseType.getRouteTableSet() == null || describeRouteTablesResponseType.getRouteTableSet().getItem() == null ||
          describeRouteTablesResponseType.getRouteTableSet().getItem().isEmpty()) {
          throw new ValidationErrorException("No such route table with id '" + properties.getRouteTableId());
        }
        CreateRouteType createRouteType = new CreateRouteType();
        createRouteType.setEffectiveUserId(info.getEffectiveUserId());
        createRouteType.setRouteTableId(properties.getRouteTableId());
        if (!Strings.isNullOrEmpty(properties.getGatewayId())) {
          createRouteType.setGatewayId(properties.getGatewayId());
        }
        if (!Strings.isNullOrEmpty(properties.getInstanceId())) {
          createRouteType.setInstanceId(properties.getInstanceId());
        }
        if (!Strings.isNullOrEmpty(properties.getVpcPeeringConnectionId())) {
          createRouteType.setVpcPeeringConnectionId(properties.getVpcPeeringConnectionId());
        }
        if (!Strings.isNullOrEmpty(properties.getNetworkInterfaceId())) {
          createRouteType.setNetworkInterfaceId(properties.getNetworkInterfaceId());
        }
        createRouteType.setDestinationCidrBlock(properties.getDestinationCidrBlock());
        CreateRouteResponseType createRouteResponseType = AsyncRequests.<CreateRouteType, CreateRouteResponseType>sendSync(configuration, createRouteType);
        info.setPhysicalResourceId(getDefaultPhysicalResourceId());
        info.setReferenceValueJson(JsonHelper.getStringFromJsonNode(new TextNode(info.getPhysicalResourceId())));
        break;
      default:
        throw new IllegalStateException("Invalid step " + stepNum);
    }
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

  @Override
  public void update(int stepNum) throws Exception {
    throw new UnsupportedOperationException();
  }

  public void rollbackUpdate() throws Exception {
    // can't update so rollbackUpdate should be a NOOP
  }

  @Override
  public void delete() throws Exception {
    if (info.getPhysicalResourceId() == null) return;

    ServiceConfiguration configuration = Topology.lookup(Compute.class);

    DescribeRouteTablesType describeRouteTablesType = new DescribeRouteTablesType();
    describeRouteTablesType.setEffectiveUserId(info.getEffectiveUserId());
    RouteTableIdSetType routeTableIdSet = new RouteTableIdSetType();
    RouteTableIdSetItemType routeTableIdSetItem = new RouteTableIdSetItemType();
    routeTableIdSetItem.setRouteTableId(properties.getRouteTableId());
    routeTableIdSet.setItem(Lists.newArrayList(routeTableIdSetItem));
    describeRouteTablesType.setRouteTableIdSet(routeTableIdSet);
    DescribeRouteTablesResponseType describeRouteTablesResponseType = AsyncRequests.<DescribeRouteTablesType, DescribeRouteTablesResponseType>sendSync(configuration, describeRouteTablesType);
    if (describeRouteTablesResponseType.getRouteTableSet() == null || describeRouteTablesResponseType.getRouteTableSet().getItem() == null ||
      describeRouteTablesResponseType.getRouteTableSet().getItem().isEmpty()) {
      return;
    }
    // see if the route is there...
    boolean foundRoute = false;
    for (RouteTableType routeTableType: describeRouteTablesResponseType.getRouteTableSet().getItem()) {
      if (!properties.getRouteTableId().equals(routeTableType.getRouteTableId())) continue;
      if (routeTableType.getRouteSet() == null || routeTableType.getRouteSet().getItem() == null ||
        routeTableType.getRouteSet().getItem().isEmpty()) {
        continue; // no routes
      }
      for (RouteType routeType : routeTableType.getRouteSet().getItem()) {
        if (equalRoutes(properties, routeType)) {
          foundRoute = true;
          break;
        }
      }
    }
    if (!foundRoute) return;
    DeleteRouteType deleteRouteType = new DeleteRouteType();
    deleteRouteType.setEffectiveUserId(info.getEffectiveUserId());
    deleteRouteType.setRouteTableId(properties.getRouteTableId());
    deleteRouteType.setDestinationCidrBlock(properties.getDestinationCidrBlock());
    DeleteRouteResponseType deleteRouteResponseType = AsyncRequests.<DeleteRouteType, DeleteRouteResponseType>sendSync(configuration, deleteRouteType);
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
  public void rollbackCreate() throws Exception {
    delete();
  }

}


