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
import com.eucalyptus.cloudformation.resources.standard.info.AWSEC2NetworkInterfaceAttachmentResourceInfo;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.AWSEC2NetworkInterfaceAttachmentProperties;
import com.eucalyptus.cloudformation.template.JsonHelper;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.Topology;
import com.eucalyptus.compute.common.AssociateAddressResponseType;
import com.eucalyptus.compute.common.AssociateAddressType;
import com.eucalyptus.compute.common.AttachNetworkInterfaceResponseType;
import com.eucalyptus.compute.common.AttachNetworkInterfaceType;
import com.eucalyptus.compute.common.Compute;
import com.eucalyptus.compute.common.DescribeAddressesResponseType;
import com.eucalyptus.compute.common.DescribeAddressesType;
import com.eucalyptus.compute.common.DescribeInstancesResponseType;
import com.eucalyptus.compute.common.DescribeInstancesType;
import com.eucalyptus.compute.common.DescribeNetworkInterfacesResponseType;
import com.eucalyptus.compute.common.DescribeNetworkInterfacesType;
import com.eucalyptus.compute.common.DetachNetworkInterfaceResponseType;
import com.eucalyptus.compute.common.DetachNetworkInterfaceType;
import com.eucalyptus.compute.common.NetworkInterfaceIdSetItemType;
import com.eucalyptus.compute.common.NetworkInterfaceIdSetType;
import com.eucalyptus.compute.common.NetworkInterfaceSetType;
import com.eucalyptus.compute.common.NetworkInterfaceType;
import com.eucalyptus.util.async.AsyncRequests;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;

import java.util.ArrayList;

/**
 * Created by ethomas on 2/3/14.
 */
public class AWSEC2NetworkInterfaceAttachmentResourceAction extends ResourceAction {

  private AWSEC2NetworkInterfaceAttachmentProperties properties = new AWSEC2NetworkInterfaceAttachmentProperties();
  private AWSEC2NetworkInterfaceAttachmentResourceInfo info = new AWSEC2NetworkInterfaceAttachmentResourceInfo();
  @Override
  public ResourceProperties getResourceProperties() {
    return properties;
  }

  @Override
  public void setResourceProperties(ResourceProperties resourceProperties) {
    properties = (AWSEC2NetworkInterfaceAttachmentProperties) resourceProperties;
  }

  @Override
  public ResourceInfo getResourceInfo() {
    return info;
  }

  @Override
  public void setResourceInfo(ResourceInfo resourceInfo) {
    info = (AWSEC2NetworkInterfaceAttachmentResourceInfo) resourceInfo;
  }

  @Override
  public void create(int stepNum) throws Exception {
    switch (stepNum) {
      case 0:
        ServiceConfiguration configuration = Topology.lookup(Compute.class);
        AttachNetworkInterfaceType attachNetworkInterfaceType = new AttachNetworkInterfaceType();
        attachNetworkInterfaceType.setEffectiveUserId(info.getEffectiveUserId());
        if (properties.getInstanceId() != null) {
          DescribeInstancesType describeInstancesType = new DescribeInstancesType();
          describeInstancesType.setInstancesSet(Lists.newArrayList(properties.getInstanceId()));
          describeInstancesType.setEffectiveUserId(info.getEffectiveUserId());
          DescribeInstancesResponseType describeInstancesResponseType = AsyncRequests.<DescribeInstancesType,DescribeInstancesResponseType> sendSync(configuration, describeInstancesType);
          if (describeInstancesResponseType.getReservationSet() == null || describeInstancesResponseType.getReservationSet().isEmpty()) {
            throw new ValidationErrorException("No such instance " + properties.getInstanceId());
          }
          attachNetworkInterfaceType.setInstanceId(properties.getInstanceId());
        }
        if (properties.getNetworkInterfaceId() != null) {
          DescribeNetworkInterfacesType describeNetworkInterfacesType = new DescribeNetworkInterfacesType();
          describeNetworkInterfacesType.setEffectiveUserId(info.getEffectiveUserId());
          describeNetworkInterfacesType.setNetworkInterfaceIdSet(convertNetworkInterfaceIdSet(properties.getNetworkInterfaceId()));
          DescribeNetworkInterfacesResponseType describeNetworkInterfacesResponseType = AsyncRequests.<DescribeNetworkInterfacesType, DescribeNetworkInterfacesResponseType>sendSync(configuration, describeNetworkInterfacesType);
          if (describeNetworkInterfacesResponseType.getNetworkInterfaceSet() == null || describeNetworkInterfacesResponseType.getNetworkInterfaceSet().getItem() == null ||
            describeNetworkInterfacesResponseType.getNetworkInterfaceSet().getItem().isEmpty()) {
            throw new ValidationErrorException("No such network interface " + properties.getNetworkInterfaceId());
          }
          attachNetworkInterfaceType.setNetworkInterfaceId(properties.getNetworkInterfaceId());
        }
        attachNetworkInterfaceType.setDeviceIndex(properties.getDeviceIndex());

        // TODO: figure out to do with delete on terminate...
        AttachNetworkInterfaceResponseType attachNetworkInterfaceResponseType = AsyncRequests.<AttachNetworkInterfaceType, AttachNetworkInterfaceResponseType> sendSync(configuration, attachNetworkInterfaceType);
        info.setPhysicalResourceId(attachNetworkInterfaceResponseType.getAttachmentId());
        info.setReferenceValueJson(JsonHelper.getStringFromJsonNode(new TextNode(info.getPhysicalResourceId())));
        // TODO: wait until attached
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
    describeNetworkInterfacesType.setNetworkInterfaceIdSet(convertNetworkInterfaceIdSet(properties.getNetworkInterfaceId()));
    DescribeNetworkInterfacesResponseType describeNetworkInterfacesResponseType = AsyncRequests.<DescribeNetworkInterfacesType, DescribeNetworkInterfacesResponseType>sendSync(configuration, describeNetworkInterfacesType);
    if (describeNetworkInterfacesResponseType.getNetworkInterfaceSet() == null || describeNetworkInterfacesResponseType.getNetworkInterfaceSet().getItem() == null ||
      describeNetworkInterfacesResponseType.getNetworkInterfaceSet().getItem().isEmpty()) {
      return;
    }
    // TODO: looks like this object has state, deal with attaching, etc.
    // see if my attachment still exists
    boolean foundAttachment = false;
    for (NetworkInterfaceType networkInterfaceType : describeNetworkInterfacesResponseType.getNetworkInterfaceSet().getItem()) {
      if (networkInterfaceType.getAttachment() != null && networkInterfaceType.getAttachment().getAttachmentId() != null
        && networkInterfaceType.getAttachment().getAttachmentId().equals(info.getPhysicalResourceId())) {
        foundAttachment = true;
        break;
      }
    }
    if (!foundAttachment) return;
    DetachNetworkInterfaceType detachNetworkInterfaceType = new DetachNetworkInterfaceType();
    detachNetworkInterfaceType.setEffectiveUserId(info.getEffectiveUserId());
    detachNetworkInterfaceType.setAttachmentId(info.getPhysicalResourceId());
    AsyncRequests.<DetachNetworkInterfaceType, DetachNetworkInterfaceResponseType> sendSync(configuration, detachNetworkInterfaceType);
    // TODO: wait until detached
  }

  @Override
  public void rollbackCreate() throws Exception {
    delete();
  }

}


