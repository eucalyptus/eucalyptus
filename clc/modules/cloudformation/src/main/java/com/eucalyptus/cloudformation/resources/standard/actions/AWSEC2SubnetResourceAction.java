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
import com.eucalyptus.cloudformation.resources.standard.info.AWSEC2SubnetResourceInfo;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.AWSEC2SubnetProperties;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.EC2Tag;
import com.eucalyptus.cloudformation.template.JsonHelper;
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
public class AWSEC2SubnetResourceAction extends ResourceAction {

  private AWSEC2SubnetProperties properties = new AWSEC2SubnetProperties();
  private AWSEC2SubnetResourceInfo info = new AWSEC2SubnetResourceInfo();
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

  @Override
  public int getNumCreateSteps() {
    return 2;
  }

  @Override
  public void create(int stepNum) throws Exception {
    ServiceConfiguration configuration = Topology.lookup(Compute.class);
    switch (stepNum) {
      case 0: // create subnet
        CreateSubnetType createSubnetType = new CreateSubnetType();
        createSubnetType.setEffectiveUserId(info.getEffectiveUserId());
        createSubnetType.setVpcId(properties.getVpcId());
        if (properties.getAvailabilityZone() != null) {
          createSubnetType.setAvailabilityZone(properties.getAvailabilityZone());
        }
        createSubnetType.setCidrBlock(properties.getCidrBlock());
        CreateSubnetResponseType createSubnetResponseType = AsyncRequests.<CreateSubnetType,CreateSubnetResponseType> sendSync(configuration, createSubnetType);
        info.setPhysicalResourceId(createSubnetResponseType.getSubnet().getSubnetId());
        info.setReferenceValueJson(JsonHelper.getStringFromJsonNode(new TextNode(info.getPhysicalResourceId())));
        info.setAvailabilityZone(JsonHelper.getStringFromJsonNode(new TextNode(createSubnetResponseType.getSubnet().getAvailabilityZone())));
        break;
      case 1: // tags
        List<EC2Tag> tags = TagHelper.getEC2StackTags(info, getStackEntity());
        if (properties.getTags() != null && !properties.getTags().isEmpty()) {
          tags.addAll(properties.getTags());
        }
        CreateTagsType createTagsType = new CreateTagsType();
        createTagsType.setEffectiveUserId(info.getEffectiveUserId());
        createTagsType.setResourcesSet(Lists.newArrayList(info.getPhysicalResourceId()));
        createTagsType.setTagSet(EC2Helper.createTagSet(properties.getTags()));
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
    // Check vpc (return if gone)
    DescribeVpcsType describeVpcsType = new DescribeVpcsType();
    describeVpcsType.setEffectiveUserId(info.getEffectiveUserId());
    setVpcId(describeVpcsType, properties.getVpcId());
    DescribeVpcsResponseType describeVpcsResponseType = AsyncRequests.<DescribeVpcsType,DescribeVpcsResponseType> sendSync(configuration, describeVpcsType);
    if (describeVpcsResponseType.getVpcSet() == null || describeVpcsResponseType.getVpcSet().getItem() == null || describeVpcsResponseType.getVpcSet().getItem().isEmpty()) {
      return; // already deleted
    }
    // check subnet (return if gone)
    DescribeSubnetsType describeSubnetsType = new DescribeSubnetsType();
    describeSubnetsType.setEffectiveUserId(info.getEffectiveUserId());
    setSubnetId(describeSubnetsType, info.getPhysicalResourceId());
    DescribeSubnetsResponseType describeSubnetsResponseType = AsyncRequests.<DescribeSubnetsType,DescribeSubnetsResponseType> sendSync(configuration, describeSubnetsType);
    if (describeSubnetsResponseType.getSubnetSet() == null || describeSubnetsResponseType.getSubnetSet().getItem() == null || describeSubnetsResponseType.getSubnetSet().getItem().isEmpty()) {
      return; // already deleted
    }
    DeleteSubnetType deleteSubnetType = new DeleteSubnetType();
    deleteSubnetType.setEffectiveUserId(info.getEffectiveUserId());
    deleteSubnetType.setSubnetId(info.getPhysicalResourceId());
    AsyncRequests.<DeleteSubnetType,DeleteSubnetResponseType> sendSync(configuration, deleteSubnetType);
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
  public void rollbackCreate() throws Exception {
    delete();
  }

}


