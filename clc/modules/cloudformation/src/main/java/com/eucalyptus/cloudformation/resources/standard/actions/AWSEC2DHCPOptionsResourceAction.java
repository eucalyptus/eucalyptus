/*************************************************************************
 * Copyright 2009-2015 Ent. Services Development Corporation LP
 *
 * Redistribution and use of this software in source and binary forms,
 * with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *   Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 *   Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer
 *   in the documentation and/or other materials provided with the
 *   distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
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
import com.eucalyptus.cloudformation.resources.standard.propertytypes.EC2Tag;
import com.eucalyptus.cloudformation.template.JsonHelper;
import com.eucalyptus.cloudformation.util.MessageHelper;
import com.eucalyptus.cloudformation.workflow.steps.Step;
import com.eucalyptus.cloudformation.workflow.steps.StepBasedResourceAction;
import com.eucalyptus.cloudformation.workflow.steps.UpdateStep;
import com.eucalyptus.cloudformation.workflow.updateinfo.UpdateType;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.Topology;
import com.eucalyptus.compute.common.CloudFilters;
import com.eucalyptus.compute.common.Compute;
import com.eucalyptus.compute.common.CreateDhcpOptionsResponseType;
import com.eucalyptus.compute.common.CreateDhcpOptionsType;
import com.eucalyptus.compute.common.CreateTagsResponseType;
import com.eucalyptus.compute.common.CreateTagsType;
import com.eucalyptus.compute.common.DeleteDhcpOptionsResponseType;
import com.eucalyptus.compute.common.DeleteDhcpOptionsType;
import com.eucalyptus.compute.common.DeleteTagsResponseType;
import com.eucalyptus.compute.common.DeleteTagsType;
import com.eucalyptus.compute.common.DescribeDhcpOptionsResponseType;
import com.eucalyptus.compute.common.DescribeDhcpOptionsType;
import com.eucalyptus.compute.common.DescribeTagsResponseType;
import com.eucalyptus.compute.common.DescribeTagsType;
import com.eucalyptus.compute.common.DhcpConfigurationItemSetType;
import com.eucalyptus.compute.common.DhcpConfigurationItemType;
import com.eucalyptus.compute.common.DhcpValueSetType;
import com.eucalyptus.compute.common.DhcpValueType;
import com.eucalyptus.compute.common.TagInfo;
import com.eucalyptus.util.async.AsyncRequests;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Created by ethomas on 2/3/14.
 */
public class AWSEC2DHCPOptionsResourceAction extends StepBasedResourceAction {

  private AWSEC2DHCPOptionsProperties properties = new AWSEC2DHCPOptionsProperties();
  private AWSEC2DHCPOptionsResourceInfo info = new AWSEC2DHCPOptionsResourceInfo();

  public AWSEC2DHCPOptionsResourceAction() {
    super(fromEnum(CreateSteps.class), fromEnum(DeleteSteps.class), fromUpdateEnum(UpdateNoInterruptionSteps.class), null);
  }

  @Override
  public UpdateType getUpdateType(ResourceAction resourceAction, boolean stackTagsChanged) {
    UpdateType updateType = info.supportsTags() && stackTagsChanged ? UpdateType.NO_INTERRUPTION : UpdateType.NONE;
    AWSEC2DHCPOptionsResourceAction otherAction = (AWSEC2DHCPOptionsResourceAction) resourceAction;
    if (!Objects.equals(properties.getDomainName(), otherAction.properties.getDomainName())) {
      updateType = UpdateType.max(updateType, UpdateType.NEEDS_REPLACEMENT);
    }
    if (!Objects.equals(properties.getDomainNameServers(), otherAction.properties.getDomainNameServers())) {
      updateType = UpdateType.max(updateType, UpdateType.NEEDS_REPLACEMENT);
    }
    if (!Objects.equals(properties.getNetbiosNameServers(), otherAction.properties.getNetbiosNameServers())) {
      updateType = UpdateType.max(updateType, UpdateType.NEEDS_REPLACEMENT);
    }
    if (!Objects.equals(properties.getNetbiosNodeType(), otherAction.properties.getNetbiosNodeType())) {
      updateType = UpdateType.max(updateType, UpdateType.NEEDS_REPLACEMENT);
    }
    if (!Objects.equals(properties.getNtpServers(), otherAction.properties.getNtpServers())) {
      updateType = UpdateType.max(updateType, UpdateType.NEEDS_REPLACEMENT);
    }
    if (!Objects.equals(properties.getTags(), otherAction.properties.getTags())) {
      updateType = UpdateType.max(updateType, UpdateType.NO_INTERRUPTION);
    }
    return updateType;
  }

  private enum CreateSteps implements Step {
    CREATE_DHCP_OPTIONS {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        AWSEC2DHCPOptionsResourceAction action = (AWSEC2DHCPOptionsResourceAction) resourceAction;
        ServiceConfiguration configuration = Topology.lookup(Compute.class);
        action.validateProperties();
        CreateDhcpOptionsType createDhcpOptionsType = MessageHelper.createMessage(CreateDhcpOptionsType.class, action.info.getEffectiveUserId());
        DhcpConfigurationItemSetType dhcpConfigurationSet = new DhcpConfigurationItemSetType();
        dhcpConfigurationSet.setItem(Lists.<DhcpConfigurationItemType>newArrayList());
        ArrayList<DhcpConfigurationItemType> item = Lists.newArrayList();
        if (!Strings.isNullOrEmpty(action.properties.getDomainName())) {
          dhcpConfigurationSet.getItem().add(action.createDhcpConfigurationItemType("domain-name", action.createValueSet(action.properties.getDomainName())));
        }
        if (action.properties.getDomainNameServers() != null && !action.properties.getDomainNameServers().isEmpty()) {
          dhcpConfigurationSet.getItem().add(action.createDhcpConfigurationItemType("domain-name-servers", action.createValueSet(action.properties.getDomainNameServers())));
        }
        if (action.properties.getNtpServers() != null && !action.properties.getNtpServers().isEmpty()) {
          dhcpConfigurationSet.getItem().add(action.createDhcpConfigurationItemType("ntp-servers", action.createValueSet(action.properties.getNtpServers())));
        }
        if (action.properties.getNetbiosNameServers() != null && !action.properties.getNetbiosNameServers().isEmpty()) {
          dhcpConfigurationSet.getItem().add(action.createDhcpConfigurationItemType("netbios-name-servers", action.createValueSet(action.properties.getNetbiosNameServers())));
        }
        if (action.properties.getNetbiosNodeType() != null) {
          dhcpConfigurationSet.getItem().add(action.createDhcpConfigurationItemType("netbios-node-type", action.createValueSet(String.valueOf(action.properties.getNetbiosNodeType()))));
        }
        createDhcpOptionsType.setDhcpConfigurationSet(dhcpConfigurationSet);
        CreateDhcpOptionsResponseType createDhcpOptionsResponseType = AsyncRequests.<CreateDhcpOptionsType, CreateDhcpOptionsResponseType> sendSync(configuration, createDhcpOptionsType);
        action.info.setPhysicalResourceId(createDhcpOptionsResponseType.getDhcpOptions().getDhcpOptionsId());
        action.info.setCreatedEnoughToDelete(true);
        action.info.setReferenceValueJson(JsonHelper.getStringFromJsonNode(new TextNode(action.info.getPhysicalResourceId())));
        return action;
      }
    },
    CREATE_TAGS {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        AWSEC2DHCPOptionsResourceAction action = (AWSEC2DHCPOptionsResourceAction) resourceAction;
        ServiceConfiguration configuration = Topology.lookup(Compute.class);
        // Create 'system' tags as admin user
        String effectiveAdminUserId = action.info.getAccountId( );
        CreateTagsType createSystemTagsType = MessageHelper.createPrivilegedMessage(CreateTagsType.class, effectiveAdminUserId);
        createSystemTagsType.setResourcesSet(Lists.newArrayList(action.info.getPhysicalResourceId()));
        createSystemTagsType.setTagSet(EC2Helper.createTagSet(TagHelper.getEC2SystemTags(action.info, action.getStackEntity())));
        AsyncRequests.<CreateTagsType, CreateTagsResponseType>sendSync(configuration, createSystemTagsType);
        // Create non-system tags as regular user
        List<EC2Tag> tags = TagHelper.getEC2StackTags(action.getStackEntity());
        if (action.properties.getTags() != null && !action.properties.getTags().isEmpty()) {
          TagHelper.checkReservedEC2TemplateTags(action.properties.getTags());
          tags.addAll(action.properties.getTags());
        }
        if (!tags.isEmpty()) {
          CreateTagsType createTagsType = MessageHelper.createMessage(CreateTagsType.class, action.info.getEffectiveUserId());
          createTagsType.setResourcesSet(Lists.newArrayList(action.info.getPhysicalResourceId()));
          createTagsType.setTagSet(EC2Helper.createTagSet(tags));
          AsyncRequests.<CreateTagsType, CreateTagsResponseType>sendSync(configuration, createTagsType);
        }
        return action;
      }
    };

    @Nullable
    @Override
    public Integer getTimeout() {
      return null;
    }

  }

  private enum DeleteSteps implements Step {
    DELETE_DHCP_OPTIONS {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        AWSEC2DHCPOptionsResourceAction action = (AWSEC2DHCPOptionsResourceAction) resourceAction;
        ServiceConfiguration configuration = Topology.lookup(Compute.class);
        if (!Boolean.TRUE.equals(action.info.getCreatedEnoughToDelete())) return action;
        // Check dhcp options (return if gone)
        DescribeDhcpOptionsType describeDhcpOptionsType = MessageHelper.createMessage(DescribeDhcpOptionsType.class, action.info.getEffectiveUserId());
        describeDhcpOptionsType.getFilterSet( ).add( CloudFilters.filter("dhcp-options-id", action.info.getPhysicalResourceId()));
        DescribeDhcpOptionsResponseType describeDhcpOptionsResponseType = AsyncRequests.sendSync(configuration, describeDhcpOptionsType);
        if (describeDhcpOptionsResponseType.getDhcpOptionsSet() == null ||
            describeDhcpOptionsResponseType.getDhcpOptionsSet().getItem() == null ||
            describeDhcpOptionsResponseType.getDhcpOptionsSet().getItem().isEmpty()) {
          return action; // already deleted
        }
        DeleteDhcpOptionsType deleteDhcpOptionsType = MessageHelper.createMessage(DeleteDhcpOptionsType.class, action.info.getEffectiveUserId());
        deleteDhcpOptionsType.setDhcpOptionsId(action.info.getPhysicalResourceId());
        AsyncRequests.<DeleteDhcpOptionsType,DeleteDhcpOptionsResponseType> sendSync(configuration, deleteDhcpOptionsType);
        return action;
      }
    };

    @Nullable
    @Override
    public Integer getTimeout() {
      return null;
    }
  }

  private enum UpdateNoInterruptionSteps implements UpdateStep {
    UPDATE_TAGS {
      @Override
      public ResourceAction perform(ResourceAction oldResourceAction, ResourceAction newResourceAction) throws Exception {
        AWSEC2DHCPOptionsResourceAction oldAction = (AWSEC2DHCPOptionsResourceAction) oldResourceAction;
        AWSEC2DHCPOptionsResourceAction newAction = (AWSEC2DHCPOptionsResourceAction) newResourceAction;
        ServiceConfiguration configuration = Topology.lookup(Compute.class);
        DescribeTagsType describeTagsType = MessageHelper.createMessage(DescribeTagsType.class, newAction.info.getEffectiveUserId());
        describeTagsType.setFilterSet(Lists.newArrayList( CloudFilters.filter("resource-id", newAction.info.getPhysicalResourceId())));
        DescribeTagsResponseType describeTagsResponseType = AsyncRequests.sendSync(configuration, describeTagsType);
        Set<EC2Tag> existingTags = Sets.newLinkedHashSet();
        if (describeTagsResponseType != null && describeTagsResponseType.getTagSet() != null) {
          for (TagInfo tagInfo: describeTagsResponseType.getTagSet()) {
            EC2Tag tag = new EC2Tag();
            tag.setKey(tagInfo.getKey());
            tag.setValue(tagInfo.getValue());
            existingTags.add(tag);
          }
        }
        Set<EC2Tag> newTags = Sets.newLinkedHashSet();
        if (newAction.properties.getTags() != null) {
          newTags.addAll(newAction.properties.getTags());
        }
        List<EC2Tag> newStackTags = TagHelper.getEC2StackTags(newAction.getStackEntity());
        if (newStackTags != null) {
          newTags.addAll(newStackTags);
        }
        TagHelper.checkReservedEC2TemplateTags(newTags);
        // add only 'new' tags
        Set<EC2Tag> onlyNewTags = Sets.difference(newTags, existingTags);
        if (!onlyNewTags.isEmpty()) {
          CreateTagsType createTagsType = MessageHelper.createMessage(CreateTagsType.class, newAction.info.getEffectiveUserId());
          createTagsType.setResourcesSet(Lists.newArrayList(newAction.info.getPhysicalResourceId()));
          createTagsType.setTagSet(EC2Helper.createTagSet(onlyNewTags));
          AsyncRequests.<CreateTagsType, CreateTagsResponseType>sendSync(configuration, createTagsType);
        }
        //  Get old tags...
        Set<EC2Tag> oldTags = Sets.newLinkedHashSet();
        if (oldAction.properties.getTags() != null) {
          oldTags.addAll(oldAction.properties.getTags());
        }
        List<EC2Tag> oldStackTags = TagHelper.getEC2StackTags(oldAction.getStackEntity());
        if (oldStackTags != null) {
          oldTags.addAll(oldStackTags);
        }

        // remove only the old tags that are not new and that exist
        Set<EC2Tag> tagsToRemove = Sets.intersection(oldTags, Sets.difference(existingTags, newTags));
        if (!tagsToRemove.isEmpty()) {
          DeleteTagsType deleteTagsType = MessageHelper.createMessage(DeleteTagsType.class, newAction.info.getEffectiveUserId());
          deleteTagsType.setResourcesSet(Lists.newArrayList(newAction.info.getPhysicalResourceId()));
          deleteTagsType.setTagSet(EC2Helper.deleteTagSet(tagsToRemove));
          AsyncRequests.<DeleteTagsType, DeleteTagsResponseType>sendSync(configuration, deleteTagsType);
        }
        return newAction;
      }
    };

    @Nullable
    @Override
    public Integer getTimeout() {
      return null;
    }
  }


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




}


