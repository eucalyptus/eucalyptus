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
import com.eucalyptus.cloudformation.resources.standard.info.AWSEC2VPCDHCPOptionsAssociationResourceInfo;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.AWSEC2VPCDHCPOptionsAssociationProperties;
import com.eucalyptus.cloudformation.template.JsonHelper;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.Topology;
import com.eucalyptus.compute.common.AssociateDhcpOptionsResponseType;
import com.eucalyptus.compute.common.AssociateDhcpOptionsType;
import com.eucalyptus.compute.common.Compute;
import com.eucalyptus.compute.common.DeleteDhcpOptionsResponseType;
import com.eucalyptus.compute.common.DeleteDhcpOptionsType;
import com.eucalyptus.compute.common.DescribeDhcpOptionsResponseType;
import com.eucalyptus.compute.common.DescribeDhcpOptionsType;
import com.eucalyptus.compute.common.DescribeVpcsResponseType;
import com.eucalyptus.compute.common.DescribeVpcsType;
import com.eucalyptus.compute.common.DhcpOptionsIdSetItemType;
import com.eucalyptus.compute.common.DhcpOptionsIdSetType;
import com.eucalyptus.compute.common.VpcIdSetItemType;
import com.eucalyptus.compute.common.VpcIdSetType;
import com.eucalyptus.util.async.AsyncRequests;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.collect.Lists;

import java.util.ArrayList;

/**
 * Created by ethomas on 2/3/14.
 */
public class AWSEC2VPCDHCPOptionsAssociationResourceAction extends ResourceAction {

  private AWSEC2VPCDHCPOptionsAssociationProperties properties = new AWSEC2VPCDHCPOptionsAssociationProperties();
  private AWSEC2VPCDHCPOptionsAssociationResourceInfo info = new AWSEC2VPCDHCPOptionsAssociationResourceInfo();
  @Override
  public ResourceProperties getResourceProperties() {
    return properties;
  }

  @Override
  public void setResourceProperties(ResourceProperties resourceProperties) {
    properties = (AWSEC2VPCDHCPOptionsAssociationProperties) resourceProperties;
  }

  @Override
  public ResourceInfo getResourceInfo() {
    return info;
  }

  @Override
  public void setResourceInfo(ResourceInfo resourceInfo) {
    info = (AWSEC2VPCDHCPOptionsAssociationResourceInfo) resourceInfo;
  }

  @Override
  public void create(int stepNum) throws Exception {
    ServiceConfiguration configuration = Topology.lookup(Compute.class);
    switch (stepNum) {
      case 0: // create dhcp options
        // Check dhcp options (if not "default")
        if (!"default".equals(properties.getDhcpOptionsId())) {
          DescribeDhcpOptionsType describeDhcpOptionsType = new DescribeDhcpOptionsType();
          describeDhcpOptionsType.setEffectiveUserId(info.getEffectiveUserId());
          setDhcpOptionsId(describeDhcpOptionsType, properties.getDhcpOptionsId());
          DescribeDhcpOptionsResponseType describeDhcpOptionsResponseType = AsyncRequests.<DescribeDhcpOptionsType,DescribeDhcpOptionsResponseType> sendSync(configuration, describeDhcpOptionsType);
          if (describeDhcpOptionsResponseType.getDhcpOptionsSet() == null || describeDhcpOptionsResponseType.getDhcpOptionsSet().getItem() == null || describeDhcpOptionsResponseType.getDhcpOptionsSet().getItem().isEmpty()) {
            throw new ValidationErrorException("No such dhcp options: " + properties.getDhcpOptionsId());
          }
        }
        // check vpc
        DescribeVpcsType describeVpcsType = new DescribeVpcsType();
        describeVpcsType.setEffectiveUserId(info.getEffectiveUserId());
        setVpcId(describeVpcsType, properties.getVpcId());
        DescribeVpcsResponseType describeVpcsResponseType = AsyncRequests.<DescribeVpcsType,DescribeVpcsResponseType> sendSync(configuration, describeVpcsType);
        if (describeVpcsResponseType.getVpcSet() == null || describeVpcsResponseType.getVpcSet().getItem() == null || describeVpcsResponseType.getVpcSet().getItem().isEmpty()) {
          throw new ValidationErrorException("No such vpc: " + properties.getVpcId());
        }
        AssociateDhcpOptionsType associateDhcpOptionsType = new AssociateDhcpOptionsType();
        associateDhcpOptionsType.setEffectiveUserId(info.getEffectiveUserId());
        associateDhcpOptionsType.setDhcpOptionsId(properties.getDhcpOptionsId());
        associateDhcpOptionsType.setVpcId(properties.getVpcId());
        AsyncRequests.<AssociateDhcpOptionsType,AssociateDhcpOptionsResponseType> sendSync(configuration, associateDhcpOptionsType);
        info.setPhysicalResourceId(getDefaultPhysicalResourceId());
        info.setReferenceValueJson(JsonHelper.getStringFromJsonNode(new TextNode(info.getPhysicalResourceId())));
        break;
      default:
        throw new IllegalStateException("Invalid step " + stepNum);
    }
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

  private void setDhcpOptionsId(DescribeDhcpOptionsType describeDhcpOptionsType, String dhcpOptionsId) {
    DhcpOptionsIdSetType dhcpOptionsSet = new DhcpOptionsIdSetType();
    ArrayList<DhcpOptionsIdSetItemType> item = Lists.newArrayList();
    DhcpOptionsIdSetItemType dhcpOptionsIdSetItemType = new DhcpOptionsIdSetItemType();
    dhcpOptionsIdSetItemType.setDhcpOptionsId(dhcpOptionsId);
    item.add(dhcpOptionsIdSetItemType);
    dhcpOptionsSet.setItem(item);
    item.add(dhcpOptionsIdSetItemType);
    describeDhcpOptionsType.setDhcpOptionsSet(dhcpOptionsSet);
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
    // Check dhcp options (if not "default")
    if (!"default".equals(properties.getDhcpOptionsId())) {
      DescribeDhcpOptionsType describeDhcpOptionsType = new DescribeDhcpOptionsType();
      describeDhcpOptionsType.setEffectiveUserId(info.getEffectiveUserId());
      setDhcpOptionsId(describeDhcpOptionsType, properties.getDhcpOptionsId());
      DescribeDhcpOptionsResponseType describeDhcpOptionsResponseType = AsyncRequests.<DescribeDhcpOptionsType,DescribeDhcpOptionsResponseType> sendSync(configuration, describeDhcpOptionsType);
      if (describeDhcpOptionsResponseType.getDhcpOptionsSet() == null || describeDhcpOptionsResponseType.getDhcpOptionsSet().getItem() == null || describeDhcpOptionsResponseType.getDhcpOptionsSet().getItem().isEmpty()) {
        return;
      }
    }
    // check vpc
    DescribeVpcsType describeVpcsType = new DescribeVpcsType();
    describeVpcsType.setEffectiveUserId(info.getEffectiveUserId());
    setVpcId(describeVpcsType, properties.getVpcId());
    DescribeVpcsResponseType describeVpcsResponseType = AsyncRequests.<DescribeVpcsType,DescribeVpcsResponseType> sendSync(configuration, describeVpcsType);
    if (describeVpcsResponseType.getVpcSet() == null || describeVpcsResponseType.getVpcSet().getItem() == null || describeVpcsResponseType.getVpcSet().getItem().isEmpty()) {
      return;
    }
    AssociateDhcpOptionsType associateDhcpOptionsType = new AssociateDhcpOptionsType();
    associateDhcpOptionsType.setEffectiveUserId(info.getEffectiveUserId());
    associateDhcpOptionsType.setDhcpOptionsId("default");
    associateDhcpOptionsType.setVpcId(properties.getVpcId());
    AsyncRequests.<AssociateDhcpOptionsType,AssociateDhcpOptionsResponseType> sendSync(configuration, associateDhcpOptionsType);
  }

  @Override
  public void rollbackCreate() throws Exception {
    delete();
  }

}


