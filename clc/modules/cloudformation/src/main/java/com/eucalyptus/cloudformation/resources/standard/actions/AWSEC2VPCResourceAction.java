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
import com.eucalyptus.cloudformation.resources.standard.info.AWSEC2VPCResourceInfo;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.AWSEC2VPCProperties;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.EC2Tag;
import com.eucalyptus.cloudformation.template.JsonHelper;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.Topology;
import com.eucalyptus.compute.common.AttributeBooleanValueType;
import com.eucalyptus.compute.common.Compute;
import com.eucalyptus.compute.common.CreateTagsResponseType;
import com.eucalyptus.compute.common.CreateTagsType;
import com.eucalyptus.compute.common.CreateVpcResponseType;
import com.eucalyptus.compute.common.CreateVpcType;
import com.eucalyptus.compute.common.DeleteVpcResponseType;
import com.eucalyptus.compute.common.DeleteVpcType;
import com.eucalyptus.compute.common.DescribeVpcsResponseType;
import com.eucalyptus.compute.common.DescribeVpcsType;
import com.eucalyptus.compute.common.ModifyVpcAttributeResponseType;
import com.eucalyptus.compute.common.ModifyVpcAttributeType;
import com.eucalyptus.compute.common.ResourceTag;
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
public class AWSEC2VPCResourceAction extends ResourceAction {

  private AWSEC2VPCProperties properties = new AWSEC2VPCProperties();
  private AWSEC2VPCResourceInfo info = new AWSEC2VPCResourceInfo();
  @Override
  public ResourceProperties getResourceProperties() {
    return properties;
  }

  @Override
  public void setResourceProperties(ResourceProperties resourceProperties) {
    properties = (AWSEC2VPCProperties) resourceProperties;
  }

  @Override
  public ResourceInfo getResourceInfo() {
    return info;
  }

  @Override
  public void setResourceInfo(ResourceInfo resourceInfo) {
    info = (AWSEC2VPCResourceInfo) resourceInfo;
  }

  @Override
  public int getNumCreateSteps() {
    return 3;
  }

  @Override
  public void create(int stepNum) throws Exception {
    ServiceConfiguration configuration = Topology.lookup(Compute.class);
    switch (stepNum) {
      case 0: // create vpc
        CreateVpcType createVpcType = new CreateVpcType();
        createVpcType.setCidrBlock(properties.getCidrBlock());
        createVpcType.setEffectiveUserId(info.getEffectiveUserId());
        if (properties.getInstanceTenancy() == null) {
          createVpcType.setInstanceTenancy("default");
        } else if (!"default".equals(properties.getInstanceTenancy()) && !"dedicated".equals(properties.getInstanceTenancy())) {
          throw new ValidationErrorException("InstanceTenancy must be 'dedicated' or 'default");
        } else {
          createVpcType.setInstanceTenancy(properties.getInstanceTenancy());
        }
        CreateVpcResponseType createVpcResponseType = AsyncRequests.<CreateVpcType,CreateVpcResponseType> sendSync(configuration, createVpcType);
        info.setPhysicalResourceId(createVpcResponseType.getVpc().getVpcId());
        info.setReferenceValueJson(JsonHelper.getStringFromJsonNode(new TextNode(info.getPhysicalResourceId())));
        break;
      case 1: // set dns entries
        ModifyVpcAttributeType modifyVpcAttributeType = new ModifyVpcAttributeType();
        boolean enableDnsSupport = true; // default value
        boolean enableDnsHostnames = false; // default value
        if (properties.getEnableDnsSupport() != null) {
          enableDnsSupport = properties.getEnableDnsSupport();
        }
        if (properties.getEnableDnsHostnames() != null) {
          enableDnsHostnames = properties.getEnableDnsHostnames();
        }
        modifyVpcAttributeType.setVpcId(info.getPhysicalResourceId());
        modifyVpcAttributeType.setEnableDnsSupport(createAttributeBooleanValueType(enableDnsSupport));
        modifyVpcAttributeType.setEnableDnsHostnames(createAttributeBooleanValueType(enableDnsHostnames));
        modifyVpcAttributeType.setEffectiveUserId(info.getEffectiveUserId());
        // TODO: does the below return any errors?
        AsyncRequests.<ModifyVpcAttributeType,ModifyVpcAttributeResponseType> sendSync(configuration, modifyVpcAttributeType);
        break;
      case 2: // tags
        if (properties.getTags() != null && !properties.getTags().isEmpty()) {
          CreateTagsType createTagsType = new CreateTagsType();
          createTagsType.setEffectiveUserId(info.getEffectiveUserId());
          createTagsType.setResourcesSet(Lists.newArrayList(info.getPhysicalResourceId()));
          createTagsType.setTagSet(createTagSet(properties.getTags()));
          AsyncRequests.<CreateTagsType,CreateTagsResponseType> sendSync(configuration, createTagsType);
        }
        break;
      default:
        throw new IllegalStateException("Invalid step " + stepNum);
    }
  }

  private ArrayList<ResourceTag> createTagSet(List<EC2Tag> tags) {
    ArrayList<ResourceTag> resourceTags = Lists.newArrayList();
    for (EC2Tag tag: tags) {
      ResourceTag resourceTag = new ResourceTag();
      resourceTag.setKey(tag.getKey());
      resourceTag.setValue(tag.getValue());
      resourceTags.add(resourceTag);
    }
    return resourceTags;
  }

  private AttributeBooleanValueType createAttributeBooleanValueType(boolean value) {
    AttributeBooleanValueType attributeBooleanValueType = new AttributeBooleanValueType();
    attributeBooleanValueType.setValue(value);
    return attributeBooleanValueType;
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
    DescribeVpcsType describeVpcsType = new DescribeVpcsType();
    describeVpcsType.setEffectiveUserId(info.getEffectiveUserId());
    setVpcId(describeVpcsType, info.getPhysicalResourceId());
    DescribeVpcsResponseType describeVpcsResponseType = AsyncRequests.<DescribeVpcsType,DescribeVpcsResponseType> sendSync(configuration, describeVpcsType);
    if (describeVpcsResponseType.getVpcSet() == null || describeVpcsResponseType.getVpcSet().getItem() == null || describeVpcsResponseType.getVpcSet().getItem().isEmpty()) {
      return; // already deleted
    }
    DeleteVpcType deleteVpcType = new DeleteVpcType();
    deleteVpcType.setEffectiveUserId(info.getEffectiveUserId());
    deleteVpcType.setVpcId(info.getPhysicalResourceId());
    AsyncRequests.<DeleteVpcType,DeleteVpcResponseType> sendSync(configuration, deleteVpcType);
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


