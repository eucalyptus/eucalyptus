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
import com.eucalyptus.cloudformation.resources.standard.info.AWSEC2NetworkAclResourceInfo;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.AWSEC2NetworkAclProperties;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.EC2Tag;
import com.eucalyptus.cloudformation.template.JsonHelper;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.Topology;
import com.eucalyptus.compute.common.Compute;
import com.eucalyptus.compute.common.CreateNetworkAclResponseType;
import com.eucalyptus.compute.common.CreateNetworkAclType;
import com.eucalyptus.compute.common.CreateTagsResponseType;
import com.eucalyptus.compute.common.CreateTagsType;
import com.eucalyptus.compute.common.DeleteNetworkAclResponseType;
import com.eucalyptus.compute.common.DeleteNetworkAclType;
import com.eucalyptus.compute.common.DescribeNetworkAclsResponseType;
import com.eucalyptus.compute.common.DescribeNetworkAclsType;
import com.eucalyptus.compute.common.NetworkAclIdSetItemType;
import com.eucalyptus.compute.common.NetworkAclIdSetType;
import com.eucalyptus.compute.common.ResourceTag;
import com.eucalyptus.util.async.AsyncRequests;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.collect.Lists;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ethomas on 2/3/14.
 */
public class AWSEC2NetworkAclResourceAction extends ResourceAction {

  private AWSEC2NetworkAclProperties properties = new AWSEC2NetworkAclProperties();
  private AWSEC2NetworkAclResourceInfo info = new AWSEC2NetworkAclResourceInfo();
  @Override
  public ResourceProperties getResourceProperties() {
    return properties;
  }

  @Override
  public void setResourceProperties(ResourceProperties resourceProperties) {
    properties = (AWSEC2NetworkAclProperties) resourceProperties;
  }

  @Override
  public ResourceInfo getResourceInfo() {
    return info;
  }

  @Override
  public void setResourceInfo(ResourceInfo resourceInfo) {
    info = (AWSEC2NetworkAclResourceInfo) resourceInfo;
  }

  @Override
  public int getNumCreateSteps() {
    return 2;
  }
  @Override
  public void create(int stepNum) throws Exception {
    ServiceConfiguration configuration = Topology.lookup(Compute.class);
    switch (stepNum) {
      case 0: // create network acl
        CreateNetworkAclType createNetworkAclType = new CreateNetworkAclType();
        createNetworkAclType.setEffectiveUserId(info.getEffectiveUserId());
        createNetworkAclType.setVpcId(properties.getVpcId());
        CreateNetworkAclResponseType createNetworkAclResponseType = AsyncRequests.<CreateNetworkAclType, CreateNetworkAclResponseType> sendSync(configuration, createNetworkAclType);
        info.setPhysicalResourceId(createNetworkAclResponseType.getNetworkAcl().getNetworkAclId());
        info.setReferenceValueJson(JsonHelper.getStringFromJsonNode(new TextNode(info.getPhysicalResourceId())));
        break;
      case 1: // tag network acl
        if (properties.getTags() != null && !properties.getTags().isEmpty()) {
          CreateTagsType createTagsType = new CreateTagsType();
          createTagsType.setEffectiveUserId(info.getEffectiveUserId());
          createTagsType.setResourcesSet(Lists.newArrayList(info.getPhysicalResourceId()));
          createTagsType.setTagSet(EC2Helper.createTagSet(properties.getTags()));
          AsyncRequests.<CreateTagsType,CreateTagsResponseType> sendSync(configuration, createTagsType);
        }
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
    // See if network acl is there
    DescribeNetworkAclsType describeNetworkAclsType = new DescribeNetworkAclsType();
    describeNetworkAclsType.setEffectiveUserId(info.getEffectiveUserId());
    NetworkAclIdSetType networkAclIdSet = new NetworkAclIdSetType();
    NetworkAclIdSetItemType networkAclIdSetItem = new NetworkAclIdSetItemType();
    networkAclIdSetItem.setNetworkAclId(info.getPhysicalResourceId());
    networkAclIdSet.setItem(Lists.newArrayList(networkAclIdSetItem));
    describeNetworkAclsType.setNetworkAclIdSet(networkAclIdSet);
    DescribeNetworkAclsResponseType describeNetworkAclsResponseType = AsyncRequests.<DescribeNetworkAclsType, DescribeNetworkAclsResponseType> sendSync(configuration, describeNetworkAclsType);
    if (describeNetworkAclsResponseType.getNetworkAclSet() == null || describeNetworkAclsResponseType.getNetworkAclSet().getItem() == null ||
      describeNetworkAclsResponseType.getNetworkAclSet().getItem().isEmpty()) {
      return; // no network acl
    }
    DeleteNetworkAclType DeleteNetworkAclType = new DeleteNetworkAclType();
    DeleteNetworkAclType.setEffectiveUserId(info.getEffectiveUserId());
    DeleteNetworkAclType.setNetworkAclId(info.getPhysicalResourceId());
    DeleteNetworkAclResponseType DeleteNetworkAclResponseType = AsyncRequests.<DeleteNetworkAclType, DeleteNetworkAclResponseType> sendSync(configuration, DeleteNetworkAclType);
  }

  @Override
  public void rollbackCreate() throws Exception {
    delete();
  }

}


