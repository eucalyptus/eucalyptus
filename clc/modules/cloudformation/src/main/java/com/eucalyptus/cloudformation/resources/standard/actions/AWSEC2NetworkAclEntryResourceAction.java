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
import com.eucalyptus.cloudformation.resources.standard.info.AWSEC2NetworkAclEntryResourceInfo;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.AWSEC2NetworkAclEntryProperties;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.EC2ICMP;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.EC2PortRange;
import com.eucalyptus.cloudformation.template.JsonHelper;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.Topology;
import com.eucalyptus.compute.common.Compute;
import com.eucalyptus.compute.common.CreateNetworkAclEntryResponseType;
import com.eucalyptus.compute.common.CreateNetworkAclEntryType;
import com.eucalyptus.compute.common.DeleteNetworkAclEntryResponseType;
import com.eucalyptus.compute.common.DeleteNetworkAclEntryType;
import com.eucalyptus.compute.common.DescribeNetworkAclsResponseType;
import com.eucalyptus.compute.common.DescribeNetworkAclsType;
import com.eucalyptus.compute.common.IcmpTypeCodeType;
import com.eucalyptus.compute.common.NetworkAclEntryType;
import com.eucalyptus.compute.common.NetworkAclIdSetItemType;
import com.eucalyptus.compute.common.NetworkAclIdSetType;
import com.eucalyptus.compute.common.NetworkAclType;
import com.eucalyptus.compute.common.PortRangeType;
import com.eucalyptus.util.async.AsyncRequests;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.collect.Lists;

/**
 * Created by ethomas on 2/3/14.
 */
public class AWSEC2NetworkAclEntryResourceAction extends ResourceAction {

  private AWSEC2NetworkAclEntryProperties properties = new AWSEC2NetworkAclEntryProperties();
  private AWSEC2NetworkAclEntryResourceInfo info = new AWSEC2NetworkAclEntryResourceInfo();
  @Override
  public ResourceProperties getResourceProperties() {
    return properties;
  }

  @Override
  public void setResourceProperties(ResourceProperties resourceProperties) {
    properties = (AWSEC2NetworkAclEntryProperties) resourceProperties;
  }

  @Override
  public ResourceInfo getResourceInfo() {
    return info;
  }

  @Override
  public void setResourceInfo(ResourceInfo resourceInfo) {
    info = (AWSEC2NetworkAclEntryResourceInfo) resourceInfo;
  }

  @Override
  public void create(int stepNum) throws Exception {
    ServiceConfiguration configuration = Topology.lookup(Compute.class);
    switch (stepNum) {
      case 0:
        // See if network acl is there
        if (properties.getNetworkAclId().isEmpty()) {
          throw new ValidationErrorException("NetworkAclId is a required field");
        }
        DescribeNetworkAclsType describeNetworkAclsType = new DescribeNetworkAclsType();
        describeNetworkAclsType.setEffectiveUserId(info.getEffectiveUserId());
        NetworkAclIdSetType networkAclIdSet = new NetworkAclIdSetType();
        NetworkAclIdSetItemType networkAclIdSetItem = new NetworkAclIdSetItemType();
        networkAclIdSetItem.setNetworkAclId(properties.getNetworkAclId());
        networkAclIdSet.setItem(Lists.newArrayList(networkAclIdSetItem));
        describeNetworkAclsType.setNetworkAclIdSet(networkAclIdSet);
        DescribeNetworkAclsResponseType describeNetworkAclsResponseType = AsyncRequests.<DescribeNetworkAclsType, DescribeNetworkAclsResponseType> sendSync(configuration, describeNetworkAclsType);
        if (describeNetworkAclsResponseType.getNetworkAclSet() == null || describeNetworkAclsResponseType.getNetworkAclSet().getItem() == null ||
          describeNetworkAclsResponseType.getNetworkAclSet().getItem().isEmpty()) {
          throw new ValidationErrorException("No such network acl with id '" + properties.getNetworkAclId());
        }
        CreateNetworkAclEntryType createNetworkAclEntryType = new CreateNetworkAclEntryType();
        createNetworkAclEntryType.setEffectiveUserId(info.getEffectiveUserId());
        createNetworkAclEntryType.setCidrBlock(properties.getCidrBlock());
        createNetworkAclEntryType.setEgress(properties.getEgress());
        createNetworkAclEntryType.setIcmpTypeCode(convertIcmpTypeCode(properties.getIcmp()));
        createNetworkAclEntryType.setNetworkAclId(properties.getNetworkAclId());
        createNetworkAclEntryType.setPortRange(convertPortRange(properties.getPortRange()));
        createNetworkAclEntryType.setProtocol(properties.getProtocol() == null ? null : String.valueOf(properties.getProtocol()));
        createNetworkAclEntryType.setRuleAction(properties.getRuleAction());
        createNetworkAclEntryType.setRuleNumber(properties.getRuleNumber());
        CreateNetworkAclEntryResponseType CreateNetworkAclEntryResponseType = AsyncRequests.<CreateNetworkAclEntryType, CreateNetworkAclEntryResponseType>sendSync(configuration, createNetworkAclEntryType);
        info.setPhysicalResourceId(getDefaultPhysicalResourceId());
        info.setReferenceValueJson(JsonHelper.getStringFromJsonNode(new TextNode(info.getPhysicalResourceId())));
        break;
      default:
        throw new IllegalStateException("Invalid step " + stepNum);
    }
  }

  private PortRangeType convertPortRange(EC2PortRange portRange) {
    if (portRange == null) return null;
    PortRangeType portRangeType = new PortRangeType();
    portRangeType.setFrom(portRange.getFrom());
    portRangeType.setTo(portRange.getTo());
    return portRangeType;
  }

  private IcmpTypeCodeType convertIcmpTypeCode(EC2ICMP icmp) {
    if (icmp == null) return null;
    IcmpTypeCodeType icmpTypeCodeType = new IcmpTypeCodeType();
    icmpTypeCodeType.setCode(icmp.getCode());
    icmpTypeCodeType.setType(icmp.getType());
    return icmpTypeCodeType;
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
    // See if network ACL is there
    DescribeNetworkAclsType describeNetworkAclsType = new DescribeNetworkAclsType();
    describeNetworkAclsType.setEffectiveUserId(info.getEffectiveUserId());
    NetworkAclIdSetType networkAclIdSet = new NetworkAclIdSetType();
    NetworkAclIdSetItemType networkAclIdSetItem = new NetworkAclIdSetItemType();
    networkAclIdSetItem.setNetworkAclId(properties.getNetworkAclId());
    networkAclIdSet.setItem(Lists.newArrayList(networkAclIdSetItem));
    describeNetworkAclsType.setNetworkAclIdSet(networkAclIdSet);
    DescribeNetworkAclsResponseType describeNetworkAclsResponseType = AsyncRequests.<DescribeNetworkAclsType, DescribeNetworkAclsResponseType> sendSync(configuration, describeNetworkAclsType);
    if (describeNetworkAclsResponseType.getNetworkAclSet() == null || describeNetworkAclsResponseType.getNetworkAclSet().getItem() == null ||
      describeNetworkAclsResponseType.getNetworkAclSet().getItem().isEmpty()) {
      return; // no network acl
    }
    // now see if the entry is in the rule
    boolean foundEntry = false;
    for (NetworkAclType networkAclType: describeNetworkAclsResponseType.getNetworkAclSet().getItem()) {
      if (networkAclType.getEntrySet() == null || networkAclType.getEntrySet().getItem() == null) continue;
      for (NetworkAclEntryType networkAclEntryType : networkAclType.getEntrySet().getItem()) {
        if (properties.getRuleNumber().equals(networkAclEntryType.getRuleNumber()) && properties.getEgress().equals(networkAclEntryType.getEgress())) {
          foundEntry = true;
          break;
        }
      }
    }
    if (!foundEntry) return; // no rule to remove
    DeleteNetworkAclEntryType deleteNetworkAclEntryType = new DeleteNetworkAclEntryType();
    deleteNetworkAclEntryType.setEffectiveUserId(info.getEffectiveUserId());
    deleteNetworkAclEntryType.setNetworkAclId(properties.getNetworkAclId());
    deleteNetworkAclEntryType.setEgress(properties.getEgress());
    deleteNetworkAclEntryType.setRuleNumber(properties.getRuleNumber());
    DeleteNetworkAclEntryResponseType deleteNetworkAclEntryResponseType = AsyncRequests.<DeleteNetworkAclEntryType, DeleteNetworkAclEntryResponseType> sendSync(configuration, deleteNetworkAclEntryType);
  }

  @Override
  public void rollbackCreate() throws Exception {
    delete();
  }

}


