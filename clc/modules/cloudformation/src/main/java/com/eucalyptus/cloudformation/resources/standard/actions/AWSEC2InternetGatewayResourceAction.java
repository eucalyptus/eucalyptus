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
import com.eucalyptus.cloudformation.resources.standard.info.AWSEC2InternetGatewayResourceInfo;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.AWSEC2InternetGatewayProperties;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.EC2Tag;
import com.eucalyptus.cloudformation.template.JsonHelper;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.Topology;
import com.eucalyptus.compute.common.Compute;
import com.eucalyptus.compute.common.CreateInternetGatewayResponseType;
import com.eucalyptus.compute.common.CreateInternetGatewayType;
import com.eucalyptus.compute.common.CreateSubnetResponseType;
import com.eucalyptus.compute.common.CreateSubnetType;
import com.eucalyptus.compute.common.CreateTagsResponseType;
import com.eucalyptus.compute.common.CreateTagsType;
import com.eucalyptus.compute.common.DeleteInternetGatewayResponseType;
import com.eucalyptus.compute.common.DeleteInternetGatewayType;
import com.eucalyptus.compute.common.DeleteSubnetResponseType;
import com.eucalyptus.compute.common.DeleteSubnetType;
import com.eucalyptus.compute.common.DescribeInternetGatewaysResponseType;
import com.eucalyptus.compute.common.DescribeInternetGatewaysType;
import com.eucalyptus.compute.common.DescribeSubnetsResponseType;
import com.eucalyptus.compute.common.DescribeSubnetsType;
import com.eucalyptus.compute.common.DescribeVpcsResponseType;
import com.eucalyptus.compute.common.DescribeVpcsType;
import com.eucalyptus.compute.common.InternetGatewayIdSetItemType;
import com.eucalyptus.compute.common.InternetGatewayIdSetType;
import com.eucalyptus.compute.common.ResourceTag;
import com.eucalyptus.compute.common.SubnetIdSetItemType;
import com.eucalyptus.compute.common.SubnetIdSetType;
import com.eucalyptus.compute.common.VpcIdSetItemType;
import com.eucalyptus.compute.common.VpcIdSetType;
import com.eucalyptus.util.async.AsyncRequests;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.collect.Lists;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ethomas on 2/3/14.
 */
public class AWSEC2InternetGatewayResourceAction extends ResourceAction {

  private AWSEC2InternetGatewayProperties properties = new AWSEC2InternetGatewayProperties();
  private AWSEC2InternetGatewayResourceInfo info = new AWSEC2InternetGatewayResourceInfo();
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

  @Override
  public int getNumCreateSteps() {
    return 2;
  }

  @Override
  public void create(int stepNum) throws Exception {
    ServiceConfiguration configuration = Topology.lookup(Compute.class);
    switch (stepNum) {
      case 0: // create gateway
        CreateInternetGatewayType createInternetGatewayType = new CreateInternetGatewayType();
        createInternetGatewayType.setEffectiveUserId(info.getEffectiveUserId());
        CreateInternetGatewayResponseType createInternetGatewayResponseType = AsyncRequests.<CreateInternetGatewayType,CreateInternetGatewayResponseType> sendSync(configuration, createInternetGatewayType);
        info.setPhysicalResourceId(createInternetGatewayResponseType.getInternetGateway().getInternetGatewayId());
        info.setReferenceValueJson(JsonHelper.getStringFromJsonNode(new TextNode(info.getPhysicalResourceId())));
        break;
      case 1: // tags
        List<EC2Tag> tags = TagHelper.getEC2StackTags(info, getStackEntity());
        if (properties.getTags() != null && !properties.getTags().isEmpty()) {
          TagHelper.checkReservedEC2TemplateTags(properties.getTags());
          tags.addAll(properties.getTags());
        }
        CreateTagsType createTagsType = new CreateTagsType();
        createTagsType.setUserId(info.getEffectiveUserId());
        createTagsType.markPrivileged(); // due to stack aws: tags
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
    // Check gateway (return if gone)
    DescribeInternetGatewaysType describeInternetGatewaysType = new DescribeInternetGatewaysType();
    describeInternetGatewaysType.setEffectiveUserId(info.getEffectiveUserId());
    setInternetGatewayId(describeInternetGatewaysType, info.getPhysicalResourceId());
    DescribeInternetGatewaysResponseType describeInternetGatewaysResponseType = AsyncRequests.<DescribeInternetGatewaysType,DescribeInternetGatewaysResponseType> sendSync(configuration, describeInternetGatewaysType);
    if (describeInternetGatewaysResponseType.getInternetGatewaySet() == null || describeInternetGatewaysResponseType.getInternetGatewaySet().getItem() == null || describeInternetGatewaysResponseType.getInternetGatewaySet().getItem().isEmpty()) {
      return; // already deleted
    }
    DeleteInternetGatewayType deleteInternetGatewayType = new DeleteInternetGatewayType();
    deleteInternetGatewayType.setEffectiveUserId(info.getEffectiveUserId());
    deleteInternetGatewayType.setInternetGatewayId(info.getPhysicalResourceId());
    AsyncRequests.<DeleteInternetGatewayType,DeleteInternetGatewayResponseType> sendSync(configuration, deleteInternetGatewayType);
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
  public void rollbackCreate() throws Exception {
    delete();
  }

}



