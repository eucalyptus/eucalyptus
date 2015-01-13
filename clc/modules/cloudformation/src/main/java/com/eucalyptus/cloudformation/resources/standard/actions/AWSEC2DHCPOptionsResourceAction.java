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


import com.amazonaws.services.simpleworkflow.flow.core.Promise;
import com.eucalyptus.auth.Accounts;
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
import com.eucalyptus.cloudformation.workflow.StackActivity;
import com.eucalyptus.cloudformation.workflow.steps.CreateMultiStepPromise;
import com.eucalyptus.cloudformation.workflow.steps.DeleteMultiStepPromise;
import com.eucalyptus.cloudformation.workflow.steps.Step;
import com.eucalyptus.cloudformation.workflow.steps.StepTransform;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.Topology;
import com.eucalyptus.compute.common.Compute;
import com.eucalyptus.compute.common.CreateDhcpOptionsResponseType;
import com.eucalyptus.compute.common.CreateDhcpOptionsType;
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
import com.google.common.collect.Lists;
import com.netflix.glisten.WorkflowOperations;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;

/**
 * Created by ethomas on 2/3/14.
 */
public class AWSEC2DHCPOptionsResourceAction extends ResourceAction {

  private AWSEC2DHCPOptionsProperties properties = new AWSEC2DHCPOptionsProperties();
  private AWSEC2DHCPOptionsResourceInfo info = new AWSEC2DHCPOptionsResourceInfo();

  public AWSEC2DHCPOptionsResourceAction() {
    for (CreateSteps createStep: CreateSteps.values()) {
      createSteps.put(createStep.name(), createStep);
    }
    for (DeleteSteps deleteStep: DeleteSteps.values()) {
      deleteSteps.put(deleteStep.name(), deleteStep);
    }

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
        String effectiveAdminUserId = Accounts.lookupUserById(action.info.getEffectiveUserId()).getAccount().lookupAdmin().getUserId();
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
        if (action.info.getPhysicalResourceId() == null) return action;
        // Check dhcp options (return if gone)
        DescribeDhcpOptionsType describeDhcpOptionsType = MessageHelper.createMessage(DescribeDhcpOptionsType.class, action.info.getEffectiveUserId());
        action.setDhcpOptionsId(describeDhcpOptionsType, action.info.getPhysicalResourceId());
        DescribeDhcpOptionsResponseType describeDhcpOptionsResponseType = AsyncRequests.<DescribeDhcpOptionsType,DescribeDhcpOptionsResponseType> sendSync(configuration, describeDhcpOptionsType);
        if (describeDhcpOptionsResponseType.getDhcpOptionsSet() == null || describeDhcpOptionsResponseType.getDhcpOptionsSet().getItem() == null || describeDhcpOptionsResponseType.getDhcpOptionsSet().getItem().isEmpty()) {
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
  public Promise<String> getCreatePromise(WorkflowOperations<StackActivity> workflowOperations, String resourceId, String stackId, String accountId, String effectiveUserId) {
    List<String> stepIds = Lists.transform(Lists.newArrayList(CreateSteps.values()), StepTransform.INSTANCE);
    return new CreateMultiStepPromise(workflowOperations, stepIds, this).getCreatePromise(resourceId, stackId, accountId, effectiveUserId);
  }

  @Override
  public Promise<String> getDeletePromise(WorkflowOperations<StackActivity> workflowOperations, String resourceId, String stackId, String accountId, String effectiveUserId) {
    List<String> stepIds = Lists.transform(Lists.newArrayList(DeleteSteps.values()), StepTransform.INSTANCE);
    return new DeleteMultiStepPromise(workflowOperations, stepIds, this).getDeletePromise(resourceId, stackId, accountId, effectiveUserId);
  }


}


