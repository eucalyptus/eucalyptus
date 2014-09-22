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


import com.eucalyptus.cloudformation.resources.EC2Helper;
import com.eucalyptus.cloudformation.resources.ResourceAction;
import com.eucalyptus.cloudformation.resources.ResourceInfo;
import com.eucalyptus.cloudformation.resources.ResourceProperties;
import com.eucalyptus.cloudformation.resources.standard.TagHelper;
import com.eucalyptus.cloudformation.resources.standard.info.AWSEC2RouteTableResourceInfo;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.AWSEC2RouteTableProperties;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.EC2Tag;
import com.eucalyptus.cloudformation.template.JsonHelper;
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
import com.eucalyptus.compute.common.ResourceTag;
import com.eucalyptus.compute.common.RouteTableIdSetItemType;
import com.eucalyptus.compute.common.RouteTableIdSetType;
import com.eucalyptus.util.async.AsyncRequests;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.collect.Lists;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ethomas on 2/3/14.
 */
public class AWSEC2RouteTableResourceAction extends ResourceAction {

  private AWSEC2RouteTableProperties properties = new AWSEC2RouteTableProperties();
  private AWSEC2RouteTableResourceInfo info = new AWSEC2RouteTableResourceInfo();
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
  public int getNumCreateSteps() {
    return 2;
  }
  @Override
  public void create(int stepNum) throws Exception {
    ServiceConfiguration configuration = Topology.lookup(Compute.class);
    switch (stepNum) {
      case 0: // create route table
        CreateRouteTableType createRouteTableType = new CreateRouteTableType();
        createRouteTableType.setEffectiveUserId(info.getEffectiveUserId());
        createRouteTableType.setVpcId(properties.getVpcId());
        CreateRouteTableResponseType createRouteTableResponseType = AsyncRequests.<CreateRouteTableType, CreateRouteTableResponseType> sendSync(configuration, createRouteTableType);
        info.setPhysicalResourceId(createRouteTableResponseType.getRouteTable().getRouteTableId());
        info.setReferenceValueJson(JsonHelper.getStringFromJsonNode(new TextNode(info.getPhysicalResourceId())));
        break;
      case 1: // tag route table
        List<EC2Tag> tags = TagHelper.getEC2StackTags(info, getStackEntity());
        if (properties.getTags() != null && !properties.getTags().isEmpty()) {
          tags.addAll(properties.getTags());
        }
        CreateTagsType createTagsType = new CreateTagsType();
        createTagsType.setEffectiveUserId(info.getEffectiveUserId());
        createTagsType.setResourcesSet(Lists.newArrayList(info.getPhysicalResourceId()));
        createTagsType.setTagSet(EC2Helper.createTagSet(tags));
        AsyncRequests.<CreateTagsType,CreateTagsResponseType> sendSync(configuration, createTagsType);
        break;
      default:
        throw new IllegalStateException("Invalid step " + stepNum);
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
    // See if route table is there
    DescribeRouteTablesType describeRouteTablesType = new DescribeRouteTablesType();
    describeRouteTablesType.setEffectiveUserId(info.getEffectiveUserId());
    RouteTableIdSetType routeTableIdSet = new RouteTableIdSetType();
    RouteTableIdSetItemType routeTableIdSetItem = new RouteTableIdSetItemType();
    routeTableIdSetItem.setRouteTableId(info.getPhysicalResourceId());
    routeTableIdSet.setItem(Lists.newArrayList(routeTableIdSetItem));
    describeRouteTablesType.setRouteTableIdSet(routeTableIdSet);
    DescribeRouteTablesResponseType describeRouteTablesResponseType = AsyncRequests.<DescribeRouteTablesType, DescribeRouteTablesResponseType> sendSync(configuration, describeRouteTablesType);
    if (describeRouteTablesResponseType.getRouteTableSet() == null || describeRouteTablesResponseType.getRouteTableSet().getItem() == null || 
      describeRouteTablesResponseType.getRouteTableSet().getItem().isEmpty()) {
      return; // no route table
    }
    DeleteRouteTableType DeleteRouteTableType = new DeleteRouteTableType();
    DeleteRouteTableType.setEffectiveUserId(info.getEffectiveUserId());
    DeleteRouteTableType.setRouteTableId(info.getPhysicalResourceId());
    DeleteRouteTableResponseType DeleteRouteTableResponseType = AsyncRequests.<DeleteRouteTableType, DeleteRouteTableResponseType> sendSync(configuration, DeleteRouteTableType);
  }

  @Override
  public void rollbackCreate() throws Exception {
    delete();
  }

}



