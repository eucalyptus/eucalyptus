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
import com.eucalyptus.cloudformation.resources.standard.info.AWSEC2DHCPOptionsResourceInfo;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.AWSEC2DHCPOptionsProperties;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.CloudFormationResourceTag;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.EC2Tag;
import com.eucalyptus.cloudformation.template.JsonHelper;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.Topology;
import com.eucalyptus.compute.common.Compute;
import com.eucalyptus.compute.common.CreateDhcpOptionsResponseType;
import com.eucalyptus.compute.common.CreateDhcpOptionsType;
import com.eucalyptus.compute.common.CreateRouteTableResponseType;
import com.eucalyptus.compute.common.CreateRouteTableType;
import com.eucalyptus.compute.common.CreateTagsResponseType;
import com.eucalyptus.compute.common.CreateTagsType;
import com.eucalyptus.compute.common.DeleteDhcpOptionsResponseType;
import com.eucalyptus.compute.common.DeleteDhcpOptionsType;
import com.eucalyptus.compute.common.DescribeDhcpOptionsResponseType;
import com.eucalyptus.compute.common.DescribeDhcpOptionsType;
import com.eucalyptus.compute.common.DhcpConfigurationItemSetType;
import com.eucalyptus.compute.common.DhcpConfigurationItemType;
import com.eucalyptus.compute.common.DhcpOptionsIdSetItemType;
import com.eucalyptus.compute.common.DhcpOptionsIdSetType;
import com.eucalyptus.compute.common.DhcpValueSetType;
import com.eucalyptus.compute.common.DhcpValueType;
import com.eucalyptus.util.async.AsyncRequests;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.base.Strings;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import org.junit.After;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ethomas on 2/3/14.
 */
public class AWSEC2DHCPOptionsResourceAction extends ResourceAction {

  private AWSEC2DHCPOptionsProperties properties = new AWSEC2DHCPOptionsProperties();
  private AWSEC2DHCPOptionsResourceInfo info = new AWSEC2DHCPOptionsResourceInfo();
  @Override
  public ResourceProperties getResourceProperties() {
    return properties;
  }

  @Override
  public void setResourceProperties(ResourceProperties resourceProperties) {
    properties = (AWSEC2DHCPOptionsProperties) resourceProperties;
  }

  @Override
  public ResourceInfo getResourceInfo() {
    return info;
  }

  @Override
  public void setResourceInfo(ResourceInfo resourceInfo) {
    info = (AWSEC2DHCPOptionsResourceInfo) resourceInfo;
  }

  @Override
  public int getNumCreateSteps() {
    return 2;
  }

  @Override
  public void create(int stepNum) throws Exception {
    ServiceConfiguration configuration = Topology.lookup(Compute.class);
    switch (stepNum) {
      case 0: // create dhcp options
        // property validation
        validateProperties();
        CreateDhcpOptionsType createDhcpOptionsType = new CreateDhcpOptionsType();
        createDhcpOptionsType.setEffectiveUserId(info.getEffectiveUserId());
        DhcpConfigurationItemSetType dhcpConfigurationSet = new DhcpConfigurationItemSetType();
        dhcpConfigurationSet.setItem(Lists.<DhcpConfigurationItemType>newArrayList());
        ArrayList<DhcpConfigurationItemType> item = Lists.newArrayList();
        if (!Strings.isNullOrEmpty(properties.getDomainName())) {
          dhcpConfigurationSet.getItem().add(createDhcpConfigurationItemType("domain-name", createValueSet(properties.getDomainName())));
        }
        if (properties.getDomainNameServers() != null && !properties.getDomainNameServers().isEmpty()) {
          dhcpConfigurationSet.getItem().add(createDhcpConfigurationItemType("domain-name-servers", createValueSet(properties.getDomainNameServers())));
        }
        if (properties.getNtpServers() != null && !properties.getNtpServers().isEmpty()) {
          dhcpConfigurationSet.getItem().add(createDhcpConfigurationItemType("ntp-servers", createValueSet(properties.getNtpServers())));
        }
        if (properties.getNetbiosNameServers() != null && !properties.getNetbiosNameServers().isEmpty()) {
          dhcpConfigurationSet.getItem().add(createDhcpConfigurationItemType("netbios-name-servers", createValueSet(properties.getNetbiosNameServers())));
        }
        if (properties.getNetbiosNodeType() != null) {
          dhcpConfigurationSet.getItem().add(createDhcpConfigurationItemType("netbios-node-type", createValueSet(String.valueOf(properties.getNetbiosNodeType()))));
        }
        createDhcpOptionsType.setDhcpConfigurationSet(dhcpConfigurationSet);
        CreateDhcpOptionsResponseType createDhcpOptionsResponseType = AsyncRequests.<CreateDhcpOptionsType, CreateDhcpOptionsResponseType> sendSync(configuration, createDhcpOptionsType);
        info.setPhysicalResourceId(createDhcpOptionsResponseType.getDhcpOptions().getDhcpOptionsId());
        info.setReferenceValueJson(JsonHelper.getStringFromJsonNode(new TextNode(info.getPhysicalResourceId())));
        break;
      case 1: // tag dhcp options
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

  private DhcpConfigurationItemType createDhcpConfigurationItemType(String key, DhcpValueSetType valueSet) {
    DhcpConfigurationItemType dhcpConfigurationItemType = new DhcpConfigurationItemType();
    dhcpConfigurationItemType.setKey(key);
    dhcpConfigurationItemType.setValueSet(valueSet);
    return dhcpConfigurationItemType;
  }

  private DhcpValueSetType createValueSet(String value) {
    return createValueSet(Lists.<String>newArrayList(value));
  }
  private DhcpValueSetType createValueSet(List<String> values) {
    DhcpValueSetType dhcpValueSetType = new DhcpValueSetType();
    dhcpValueSetType.setItem(Lists.<DhcpValueType>newArrayList());
    for (String value:values) {
      DhcpValueType dhcpValueType = new DhcpValueType();
      dhcpValueType.setValue(value);
      dhcpValueSetType.getItem().add(dhcpValueType);
    }
    return dhcpValueSetType;
  }

  private void validateProperties() throws ValidationErrorException {
    if ((properties.getDomainNameServers() == null || properties.getDomainNameServers().isEmpty()) &&
     (properties.getNetbiosNameServers() == null || properties.getNetbiosNameServers().isEmpty()) &&
     (properties.getNtpServers() == null || properties.getNtpServers().isEmpty())) {
      throw new ValidationErrorException("At least one of DomainNameServers, NetbiosNameServers, NtpServers must be specified.");
    }
    if ((properties.getNetbiosNameServers() != null && !properties.getNetbiosNameServers().isEmpty() && properties.getNetbiosNodeType() == null)) {
      throw new ValidationErrorException("If you specify NetbiosNameServers, then NetbiosNodeType is required.");
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
    // Check dhcp optionss (return if gone)
    DescribeDhcpOptionsType describeDhcpOptionsType = new DescribeDhcpOptionsType();
    describeDhcpOptionsType.setEffectiveUserId(info.getEffectiveUserId());
    setDhcpOptionsId(describeDhcpOptionsType, info.getPhysicalResourceId());
    DescribeDhcpOptionsResponseType describeDhcpOptionsResponseType = AsyncRequests.<DescribeDhcpOptionsType,DescribeDhcpOptionsResponseType> sendSync(configuration, describeDhcpOptionsType);
    if (describeDhcpOptionsResponseType.getDhcpOptionsSet() == null || describeDhcpOptionsResponseType.getDhcpOptionsSet().getItem() == null || describeDhcpOptionsResponseType.getDhcpOptionsSet().getItem().isEmpty()) {
      return; // already deleted
    }
    DeleteDhcpOptionsType deleteDhcpOptionsType = new DeleteDhcpOptionsType();
    deleteDhcpOptionsType.setEffectiveUserId(info.getEffectiveUserId());
    deleteDhcpOptionsType.setDhcpOptionsId(info.getPhysicalResourceId());
    AsyncRequests.<DeleteDhcpOptionsType,DeleteDhcpOptionsResponseType> sendSync(configuration, deleteDhcpOptionsType);
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
  public void rollbackCreate() throws Exception {
    delete();
  }

}


