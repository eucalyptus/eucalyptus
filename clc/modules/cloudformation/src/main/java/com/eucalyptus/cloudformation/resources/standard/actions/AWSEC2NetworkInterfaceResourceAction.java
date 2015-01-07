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
import com.eucalyptus.cloudformation.resources.standard.info.AWSEC2NetworkInterfaceResourceInfo;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.AWSEC2NetworkInterfaceProperties;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.EC2Tag;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.PrivateIpAddressSpecification;
import com.eucalyptus.cloudformation.template.JsonHelper;
import com.eucalyptus.cloudformation.util.MessageHelper;
import com.eucalyptus.cloudformation.workflow.StackActivity;
import com.eucalyptus.cloudformation.workflow.ValidationFailedException;
import com.eucalyptus.cloudformation.workflow.steps.CreateMultiStepPromise;
import com.eucalyptus.cloudformation.workflow.steps.DeleteMultiStepPromise;
import com.eucalyptus.cloudformation.workflow.steps.Step;
import com.eucalyptus.cloudformation.workflow.steps.StepTransform;
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
import com.eucalyptus.configurable.ConfigurableField;
import com.eucalyptus.util.async.AsyncRequests;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
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
public class AWSEC2NetworkInterfaceResourceAction extends ResourceAction {

  private AWSEC2NetworkInterfaceProperties properties = new AWSEC2NetworkInterfaceProperties();
  private AWSEC2NetworkInterfaceResourceInfo info = new AWSEC2NetworkInterfaceResourceInfo();
  @ConfigurableField(initial = "300", description = "The amount of time (in seconds) to wait for a network interface to be available after create)")
  public static volatile Integer NETWORK_INTERFACE_AVAILABLE_MAX_CREATE_RETRY_SECS = 300;

  @ConfigurableField(initial = "300", description = "The amount of time (in seconds) to wait for a network interface to be deleted)")
  public static volatile Integer NETWORK_INTERFACE_DELETED_MAX_DELETE_RETRY_SECS = 300;

  public AWSEC2NetworkInterfaceResourceAction() {
    for (CreateSteps createStep: CreateSteps.values()) {
      createSteps.put(createStep.name(), createStep);
    }
    for (DeleteSteps deleteStep: DeleteSteps.values()) {
      deleteSteps.put(deleteStep.name(), deleteStep);
    }

  }
  private enum CreateSteps implements Step {
    CREATE_NETWORK_INTERFACE {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        AWSEC2NetworkInterfaceResourceAction action = (AWSEC2NetworkInterfaceResourceAction) resourceAction;
        ServiceConfiguration configuration = Topology.lookup(Compute.class);
        CreateNetworkInterfaceType createNetworkInterfaceType = MessageHelper.createMessage(CreateNetworkInterfaceType.class, action.info.getEffectiveUserId());
        createNetworkInterfaceType.setSubnetId(action.properties.getSubnetId());
        if (!Strings.isNullOrEmpty(action.properties.getDescription())) {
          createNetworkInterfaceType.setDescription(action.properties.getDescription());
        }
        if (!Strings.isNullOrEmpty(action.properties.getPrivateIpAddress())) {
          createNetworkInterfaceType.setPrivateIpAddress(action.properties.getPrivateIpAddress());
        }
        /// TODO: do something with SourceDestCheck
        if (action.properties.getPrivateIpAddresses() != null && !action.properties.getPrivateIpAddresses().isEmpty()) {
          createNetworkInterfaceType.setPrivateIpAddressesSet(action.convertPrivateIpAddresses(action.properties.getPrivateIpAddresses()));
        }
        // TODO: should we check each group ourself(?)
        if (action.properties.getGroupSet() != null && !action.properties.getGroupSet().isEmpty()) {
          createNetworkInterfaceType.setGroupSet(action.convertGroupSet(action.properties.getGroupSet()));
        }
        if (action.properties.getSecondaryPrivateIpAddressCount() != null) {
          action.properties.setSecondaryPrivateIpAddressCount(action.properties.getSecondaryPrivateIpAddressCount());
        }
        CreateNetworkInterfaceResponseType createNetworkInterfaceResponseType = AsyncRequests.<CreateNetworkInterfaceType, CreateNetworkInterfaceResponseType>sendSync(configuration, createNetworkInterfaceType);
        action.info.setPhysicalResourceId(createNetworkInterfaceResponseType.getNetworkInterface().getNetworkInterfaceId());
        action.info.setReferenceValueJson(JsonHelper.getStringFromJsonNode(new TextNode(action.info.getPhysicalResourceId())));
        return action;
      }
    },
    GET_PRIVATE_IP_ADDRESS {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        AWSEC2NetworkInterfaceResourceAction action = (AWSEC2NetworkInterfaceResourceAction) resourceAction;
        ServiceConfiguration configuration = Topology.lookup(Compute.class);
        // Note: this is done separately, because an exception is thrown if not exactly one item is primary and we won't persist the network interface id,
        // but it will have been created
        DescribeNetworkInterfacesType describeNetworkInterfacesType = MessageHelper.createMessage(DescribeNetworkInterfacesType.class, action.info.getEffectiveUserId());
        describeNetworkInterfacesType.setNetworkInterfaceIdSet(action.convertNetworkInterfaceIdSet(action.info.getPhysicalResourceId()));
        DescribeNetworkInterfacesResponseType describeNetworkInterfacesResponseType = AsyncRequests.<DescribeNetworkInterfacesType, DescribeNetworkInterfacesResponseType>sendSync(configuration, describeNetworkInterfacesType);
        if (describeNetworkInterfacesResponseType.getNetworkInterfaceSet() == null || describeNetworkInterfacesResponseType.getNetworkInterfaceSet().getItem() == null ||
          describeNetworkInterfacesResponseType.getNetworkInterfaceSet().getItem().size() != 1) {
          throw new ValidationErrorException("Network interface " + action.info.getPhysicalResourceId() + " either does not exist or is not unique");
        }
        // Get the private ip addresses
        String primaryIp = null;
        boolean foundPrimary = false;
        ArrayNode secondaryIpArrayNode = new ObjectMapper().createArrayNode();
        for (NetworkInterfacePrivateIpAddressesSetItemType networkInterfacePrivateIpAddressesSetItemType : describeNetworkInterfacesResponseType.getNetworkInterfaceSet().getItem().get(0).getPrivateIpAddressesSet().getItem()) {
          if (networkInterfacePrivateIpAddressesSetItemType.getPrimary()) {
            if (foundPrimary) {
              throw new ValidationErrorException("Network interface " + action.info.getPhysicalResourceId() + " has a non-unique primary private ip address");
            } else {
              primaryIp = networkInterfacePrivateIpAddressesSetItemType.getPrivateIpAddress();
              foundPrimary = true;
            }
          } else {
            secondaryIpArrayNode.add(networkInterfacePrivateIpAddressesSetItemType.getPrivateIpAddress());
          }
        }
        if (!foundPrimary) {
          throw new ValidationErrorException("Network interface " + action.info.getPhysicalResourceId() + " has no primary private ip address");
        }
        action.info.setPrimaryPrivateIpAddress(JsonHelper.getStringFromJsonNode(new TextNode(primaryIp)));
        action.info.setSecondaryPrivateIpAddresses(JsonHelper.getStringFromJsonNode(secondaryIpArrayNode));
        return action;
      }
    },
    VERIFY_AVAILABLE {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        AWSEC2NetworkInterfaceResourceAction action = (AWSEC2NetworkInterfaceResourceAction) resourceAction;
        ServiceConfiguration configuration = Topology.lookup(Compute.class);
        DescribeNetworkInterfacesType describeNetworkInterfacesType = MessageHelper.createMessage(DescribeNetworkInterfacesType.class, action.info.getEffectiveUserId());
        NetworkInterfaceIdSetType networkInterfaceIdSet = action.getNetworkInterfaceIdSetType(action.info.getPhysicalResourceId());
        describeNetworkInterfacesType.setNetworkInterfaceIdSet(networkInterfaceIdSet);
        DescribeNetworkInterfacesResponseType describeNetworkInterfacesResponseType = AsyncRequests.<DescribeNetworkInterfacesType,DescribeNetworkInterfacesResponseType> sendSync(configuration, describeNetworkInterfacesType);
        if (describeNetworkInterfacesResponseType.getNetworkInterfaceSet().getItem().size() ==0) {
          throw new ValidationFailedException("Network interface " + action.info.getPhysicalResourceId() + " not yet available");
        }
        if (!"available".equals(describeNetworkInterfacesResponseType.getNetworkInterfaceSet().getItem().get(0).getStatus())) {
          throw new ValidationFailedException("Volume " + action.info.getPhysicalResourceId() + " not yet available");
        }
        return action;
      }

      @Override
      public Integer getTimeout() {
        return NETWORK_INTERFACE_AVAILABLE_MAX_CREATE_RETRY_SECS;
      }

    },
    CREATE_TAGS {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        AWSEC2NetworkInterfaceResourceAction action = (AWSEC2NetworkInterfaceResourceAction) resourceAction;
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

  private NetworkInterfaceIdSetType getNetworkInterfaceIdSetType(String networkInterfaceId) {
    NetworkInterfaceIdSetType networkInterfaceIdSet = new NetworkInterfaceIdSetType();
    ArrayList<NetworkInterfaceIdSetItemType> item = Lists.newArrayList();
    NetworkInterfaceIdSetItemType networkInterfaceIdSetItemType = new NetworkInterfaceIdSetItemType();
    networkInterfaceIdSetItemType.setNetworkInterfaceId(networkInterfaceId);
    item.add(networkInterfaceIdSetItemType);
    networkInterfaceIdSet.setItem(item);
    return networkInterfaceIdSet;
  }

  private enum DeleteSteps implements Step {
    DELETE_NETWORK_INTERFACE {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        AWSEC2NetworkInterfaceResourceAction action = (AWSEC2NetworkInterfaceResourceAction) resourceAction;
        ServiceConfiguration configuration = Topology.lookup(Compute.class);
        if (action.info.getPhysicalResourceId() == null) return action;
        if (checkDeleted(action, configuration)) return action;
        DeleteNetworkInterfaceType deleteNetworkInterfaceType = MessageHelper.createMessage(DeleteNetworkInterfaceType.class, action.info.getEffectiveUserId());
        deleteNetworkInterfaceType.setNetworkInterfaceId(action.info.getPhysicalResourceId());
        AsyncRequests.<DeleteNetworkInterfaceType, DeleteNetworkInterfaceResponseType>sendSync(configuration, deleteNetworkInterfaceType);
        return action;
      }
    },
    VERIFY_DELETE {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        AWSEC2NetworkInterfaceResourceAction action = (AWSEC2NetworkInterfaceResourceAction) resourceAction;
        ServiceConfiguration configuration = Topology.lookup(Compute.class);
        if (action.info.getPhysicalResourceId() == null) return action;
        if (checkDeleted(action, configuration)) return action;
        throw new ValidationFailedException("Network interface " + action.info.getPhysicalResourceId() + " not yet deleted");
      }

      @Override
      public Integer getTimeout() {
        return NETWORK_INTERFACE_DELETED_MAX_DELETE_RETRY_SECS;
      }
    };

    @Nullable
    @Override
    public Integer getTimeout() {
      return null;
    }

    private static boolean checkDeleted(AWSEC2NetworkInterfaceResourceAction action, ServiceConfiguration configuration) throws Exception {
      // check if network interface still exists (return otherwise)
      DescribeNetworkInterfacesType describeNetworkInterfacesType = MessageHelper.createMessage(DescribeNetworkInterfacesType.class, action.info.getEffectiveUserId());
      describeNetworkInterfacesType.setNetworkInterfaceIdSet(action.convertNetworkInterfaceIdSet(action.info.getPhysicalResourceId()));
      DescribeNetworkInterfacesResponseType describeNetworkInterfacesResponseType = AsyncRequests.<DescribeNetworkInterfacesType, DescribeNetworkInterfacesResponseType>sendSync(configuration, describeNetworkInterfacesType);
      if (describeNetworkInterfacesResponseType.getNetworkInterfaceSet() == null || describeNetworkInterfacesResponseType.getNetworkInterfaceSet().getItem() == null ||
        describeNetworkInterfacesResponseType.getNetworkInterfaceSet().getItem().isEmpty()) {
        return true;
      }
      return false;
    }

  }

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


