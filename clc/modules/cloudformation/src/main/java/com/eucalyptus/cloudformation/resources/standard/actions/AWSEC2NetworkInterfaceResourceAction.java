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
import com.eucalyptus.cloudformation.resources.EC2Helper;
import com.eucalyptus.cloudformation.resources.ResourceAction;
import com.eucalyptus.cloudformation.resources.ResourceInfo;
import com.eucalyptus.cloudformation.resources.ResourceProperties;
import com.eucalyptus.cloudformation.resources.standard.TagHelper;
import com.eucalyptus.cloudformation.resources.standard.info.AWSEC2NetworkInterfaceResourceInfo;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.AWSEC2NetworkInterfaceProperties;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.EC2Tag;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.PrivateIpAddressSpecification;
import com.eucalyptus.cloudformation.template.JsonHelper;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.Topology;
import com.eucalyptus.compute.common.Compute;
import com.eucalyptus.compute.common.CreateNetworkInterfaceResponseType;
import com.eucalyptus.compute.common.CreateNetworkInterfaceType;
import com.eucalyptus.compute.common.CreateTagsResponseType;
import com.eucalyptus.compute.common.CreateTagsType;
import com.eucalyptus.compute.common.DeleteNetworkInterfaceResponseType;
import com.eucalyptus.compute.common.DeleteNetworkInterfaceType;
import com.eucalyptus.compute.common.DescribeNetworkInterfacesResponseType;
import com.eucalyptus.compute.common.DescribeNetworkInterfacesType;
import com.eucalyptus.compute.common.NetworkInterfaceIdSetItemType;
import com.eucalyptus.compute.common.NetworkInterfaceIdSetType;
import com.eucalyptus.compute.common.NetworkInterfacePrivateIpAddressesSetItemType;
import com.eucalyptus.compute.common.PrivateIpAddressesSetItemRequestType;
import com.eucalyptus.compute.common.PrivateIpAddressesSetRequestType;
import com.eucalyptus.compute.common.SecurityGroupIdSetItemType;
import com.eucalyptus.compute.common.SecurityGroupIdSetType;
import com.eucalyptus.util.async.AsyncRequests;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ethomas on 2/3/14.
 */
public class AWSEC2NetworkInterfaceResourceAction extends ResourceAction {

  private AWSEC2NetworkInterfaceProperties properties = new AWSEC2NetworkInterfaceProperties();
  private AWSEC2NetworkInterfaceResourceInfo info = new AWSEC2NetworkInterfaceResourceInfo();
  @Override
  public ResourceProperties getResourceProperties() {
    return properties;
  }

  @Override
  public void setResourceProperties(ResourceProperties resourceProperties) {
    properties = (AWSEC2NetworkInterfaceProperties) resourceProperties;
  }

  @Override
  public ResourceInfo getResourceInfo() {
    return info;
  }

  @Override
  public void setResourceInfo(ResourceInfo resourceInfo) {
    info = (AWSEC2NetworkInterfaceResourceInfo) resourceInfo;
  }

  @Override
  public int getNumCreateSteps() {
    return 3;
  }

  @Override
  public void create(int stepNum) throws Exception {
    ServiceConfiguration configuration = Topology.lookup(Compute.class);
    switch (stepNum) {
      case 0: // create network interface
        CreateNetworkInterfaceType createNetworkInterfaceType = new CreateNetworkInterfaceType();
        createNetworkInterfaceType.setEffectiveUserId(info.getEffectiveUserId());
        createNetworkInterfaceType.setSubnetId(properties.getSubnetId());
        if (!Strings.isNullOrEmpty(properties.getDescription())) {
          createNetworkInterfaceType.setDescription(properties.getDescription());
        }
        if (!Strings.isNullOrEmpty(properties.getPrivateIpAddress())) {
          createNetworkInterfaceType.setPrivateIpAddress(properties.getPrivateIpAddress());
        }
        /// TODO: do something with SourceDestCheck
        if (properties.getPrivateIpAddresses() != null && !properties.getPrivateIpAddresses().isEmpty()) {
          createNetworkInterfaceType.setPrivateIpAddressesSet(convertPrivateIpAddresses(properties.getPrivateIpAddresses()));
        }
        // TODO: should we check each group ourself(?)
        if (properties.getGroupSet() != null && !properties.getGroupSet().isEmpty()) {
          createNetworkInterfaceType.setGroupSet(convertGroupSet(properties.getGroupSet()));
        }
        if (properties.getSecondaryPrivateIpAddressCount() != null) {
          properties.setSecondaryPrivateIpAddressCount(properties.getSecondaryPrivateIpAddressCount());
        }
        CreateNetworkInterfaceResponseType createNetworkInterfaceResponseType = AsyncRequests.<CreateNetworkInterfaceType, CreateNetworkInterfaceResponseType>sendSync(configuration, createNetworkInterfaceType);
        info.setPhysicalResourceId(createNetworkInterfaceResponseType.getNetworkInterface().getNetworkInterfaceId());
        break;
      case 1: // get private ip addresses.  Note: this is done separately, because an exception is thrown if not exactly one item is primary and we won't persist the network interface id,
        // but it will have been created
        DescribeNetworkInterfacesType describeNetworkInterfacesType = new DescribeNetworkInterfacesType();
        describeNetworkInterfacesType.setEffectiveUserId(info.getEffectiveUserId());
        describeNetworkInterfacesType.setNetworkInterfaceIdSet(convertNetworkInterfaceIdSet(info.getPhysicalResourceId()));
        DescribeNetworkInterfacesResponseType describeNetworkInterfacesResponseType = AsyncRequests.<DescribeNetworkInterfacesType, DescribeNetworkInterfacesResponseType>sendSync(configuration, describeNetworkInterfacesType);
        if (describeNetworkInterfacesResponseType.getNetworkInterfaceSet() == null || describeNetworkInterfacesResponseType.getNetworkInterfaceSet().getItem() == null ||
          describeNetworkInterfacesResponseType.getNetworkInterfaceSet().getItem().size() != 1) {
          throw new ValidationErrorException("Network interface " + info.getPhysicalResourceId() + " either does not exist or is not unique");
        }
        // Get the private ip addresses
        String primaryIp = null;
        boolean foundPrimary = false;
        ArrayNode secondaryIpArrayNode = new ObjectMapper().createArrayNode();
        for (NetworkInterfacePrivateIpAddressesSetItemType networkInterfacePrivateIpAddressesSetItemType : describeNetworkInterfacesResponseType.getNetworkInterfaceSet().getItem().get(0).getPrivateIpAddressesSet().getItem()) {
          if (networkInterfacePrivateIpAddressesSetItemType.getPrimary()) {
            if (foundPrimary) {
              throw new ValidationErrorException("Network interface " + info.getPhysicalResourceId() + " has a non-unique primary private ip address");
            } else {
              primaryIp = networkInterfacePrivateIpAddressesSetItemType.getPrivateIpAddress();
              foundPrimary = true;
            }
          } else {
            secondaryIpArrayNode.add(networkInterfacePrivateIpAddressesSetItemType.getPrivateIpAddress());
          }
        }
        if (!foundPrimary) {
          throw new ValidationErrorException("Network interface " + info.getPhysicalResourceId() + " has no primary private ip address");
        }
        info.setPrimaryPrivateIpAddress(JsonHelper.getStringFromJsonNode(new TextNode(primaryIp)));
        info.setSecondaryPrivateIpAddresses(JsonHelper.getStringFromJsonNode(secondaryIpArrayNode));
        break;
      case 2: // tags
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

  private NetworkInterfaceIdSetType convertNetworkInterfaceIdSet(String networkInterfaceId) {
    NetworkInterfaceIdSetType networkInterfaceIdSetType = new NetworkInterfaceIdSetType();
    ArrayList<NetworkInterfaceIdSetItemType> item = Lists.newArrayList();
    NetworkInterfaceIdSetItemType networkInterfaceIdSetItemType = new NetworkInterfaceIdSetItemType();
    networkInterfaceIdSetItemType.setNetworkInterfaceId(networkInterfaceId);
    item.add(networkInterfaceIdSetItemType);
    networkInterfaceIdSetType.setItem(item);
    return networkInterfaceIdSetType;
  }


  private SecurityGroupIdSetType convertGroupSet(List<String> groupSet) {
    SecurityGroupIdSetType securityGroupIdSetType = new SecurityGroupIdSetType();
    ArrayList<SecurityGroupIdSetItemType> item = Lists.newArrayList();
    for (String groupId: groupSet) {
      SecurityGroupIdSetItemType securityGroupIdSetItemType = new SecurityGroupIdSetItemType();
      securityGroupIdSetItemType.setGroupId(groupId);
      item.add(securityGroupIdSetItemType);
    }
    securityGroupIdSetType.setItem(item);
    return securityGroupIdSetType;
  }

  private PrivateIpAddressesSetRequestType convertPrivateIpAddresses(List<PrivateIpAddressSpecification> privateIpAddresses) {
    PrivateIpAddressesSetRequestType privateIpAddressesSetRequestType = new PrivateIpAddressesSetRequestType();
    ArrayList<PrivateIpAddressesSetItemRequestType> item = Lists.newArrayList();
    for (PrivateIpAddressSpecification privateIpAddressSpecification: privateIpAddresses) {
      PrivateIpAddressesSetItemRequestType privateIpAddressesSetItemRequestType = new PrivateIpAddressesSetItemRequestType();
      privateIpAddressesSetItemRequestType.setPrivateIpAddress(privateIpAddressSpecification.getPrivateIpAddress());
      privateIpAddressesSetItemRequestType.setPrimary(privateIpAddressSpecification.getPrimary());
      item.add(privateIpAddressesSetItemRequestType);
    }
    privateIpAddressesSetRequestType.setItem(item);
    return privateIpAddressesSetRequestType;
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

    // check if network interface still exists (return otherwise)
    DescribeNetworkInterfacesType describeNetworkInterfacesType = new DescribeNetworkInterfacesType();
    describeNetworkInterfacesType.setEffectiveUserId(info.getEffectiveUserId());
    describeNetworkInterfacesType.setNetworkInterfaceIdSet(convertNetworkInterfaceIdSet(info.getPhysicalResourceId()));
    DescribeNetworkInterfacesResponseType describeNetworkInterfacesResponseType = AsyncRequests.<DescribeNetworkInterfacesType, DescribeNetworkInterfacesResponseType>sendSync(configuration, describeNetworkInterfacesType);
    if (describeNetworkInterfacesResponseType.getNetworkInterfaceSet() == null || describeNetworkInterfacesResponseType.getNetworkInterfaceSet().getItem() == null ||
      describeNetworkInterfacesResponseType.getNetworkInterfaceSet().getItem().isEmpty()) {
      return;
    }
    DeleteNetworkInterfaceType deleteNetworkInterfaceType = new DeleteNetworkInterfaceType();
    deleteNetworkInterfaceType.setEffectiveUserId(info.getEffectiveUserId());
    deleteNetworkInterfaceType.setNetworkInterfaceId(info.getPhysicalResourceId());
    AsyncRequests.<DeleteNetworkInterfaceType, DeleteNetworkInterfaceResponseType>sendSync(configuration, deleteNetworkInterfaceType);

  }

  @Override
  public void rollbackCreate() throws Exception {
    delete();
  }

}


