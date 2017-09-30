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
import com.eucalyptus.cloudformation.resources.standard.info.AWSEC2NetworkInterfaceResourceInfo;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.AWSEC2NetworkInterfaceProperties;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.EC2Tag;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.PrivateIpAddressSpecification;
import com.eucalyptus.cloudformation.template.JsonHelper;
import com.eucalyptus.cloudformation.util.MessageHelper;
import com.eucalyptus.cloudformation.workflow.RetryAfterConditionCheckFailedException;
import com.eucalyptus.cloudformation.workflow.steps.Step;
import com.eucalyptus.cloudformation.workflow.steps.StepBasedResourceAction;
import com.eucalyptus.cloudformation.workflow.steps.UpdateStep;
import com.eucalyptus.cloudformation.workflow.updateinfo.UpdateType;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.Topology;
import com.eucalyptus.compute.common.AssignPrivateIpAddressesSetItemRequestType;
import com.eucalyptus.compute.common.AssignPrivateIpAddressesSetRequestType;
import com.eucalyptus.compute.common.AssignPrivateIpAddressesType;
import com.eucalyptus.compute.common.AttributeBooleanValueType;
import com.eucalyptus.compute.common.CloudFilters;
import com.eucalyptus.compute.common.Compute;
import com.eucalyptus.compute.common.CreateNetworkInterfaceResponseType;
import com.eucalyptus.compute.common.CreateNetworkInterfaceType;
import com.eucalyptus.compute.common.CreateTagsResponseType;
import com.eucalyptus.compute.common.CreateTagsType;
import com.eucalyptus.compute.common.DeleteNetworkInterfaceResponseType;
import com.eucalyptus.compute.common.DeleteNetworkInterfaceType;
import com.eucalyptus.compute.common.DeleteTagsResponseType;
import com.eucalyptus.compute.common.DeleteTagsType;
import com.eucalyptus.compute.common.DescribeNetworkInterfacesResponseType;
import com.eucalyptus.compute.common.DescribeNetworkInterfacesType;
import com.eucalyptus.compute.common.DescribeSecurityGroupsResponseType;
import com.eucalyptus.compute.common.DescribeSecurityGroupsType;
import com.eucalyptus.compute.common.DescribeSubnetsResponseType;
import com.eucalyptus.compute.common.DescribeSubnetsType;
import com.eucalyptus.compute.common.DescribeTagsResponseType;
import com.eucalyptus.compute.common.DescribeTagsType;
import com.eucalyptus.compute.common.ModifyNetworkInterfaceAttributeResponseType;
import com.eucalyptus.compute.common.ModifyNetworkInterfaceAttributeType;
import com.eucalyptus.compute.common.NetworkInterfacePrivateIpAddressesSetItemType;
import com.eucalyptus.compute.common.NullableAttributeValueType;
import com.eucalyptus.compute.common.PrivateIpAddressesSetItemRequestType;
import com.eucalyptus.compute.common.PrivateIpAddressesSetRequestType;
import com.eucalyptus.compute.common.SecurityGroupIdSetItemType;
import com.eucalyptus.compute.common.SecurityGroupIdSetType;
import com.eucalyptus.compute.common.SubnetIdSetItemType;
import com.eucalyptus.compute.common.SubnetIdSetType;
import com.eucalyptus.compute.common.TagInfo;
import com.eucalyptus.compute.common.UnassignPrivateIpAddressesType;
import com.eucalyptus.configurable.ConfigurableField;
import com.eucalyptus.util.async.AsyncRequests;
import com.fasterxml.jackson.databind.node.ArrayNode;
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
public class AWSEC2NetworkInterfaceResourceAction extends StepBasedResourceAction {

  private AWSEC2NetworkInterfaceProperties properties = new AWSEC2NetworkInterfaceProperties();
  private AWSEC2NetworkInterfaceResourceInfo info = new AWSEC2NetworkInterfaceResourceInfo();
  @ConfigurableField(initial = "300", description = "The amount of time (in seconds) to wait for a network interface to be available after create)")
  public static volatile Integer NETWORK_INTERFACE_AVAILABLE_MAX_CREATE_RETRY_SECS = 300;

  @ConfigurableField(initial = "300", description = "The amount of time (in seconds) to wait for a network interface to be deleted)")
  public static volatile Integer NETWORK_INTERFACE_DELETED_MAX_DELETE_RETRY_SECS = 300;

  public AWSEC2NetworkInterfaceResourceAction() {
    super(fromEnum(CreateSteps.class), fromEnum(DeleteSteps.class), fromUpdateEnum(UpdateNoInterruptionSteps.class), null);
  }
  @Override
  public UpdateType getUpdateType(ResourceAction resourceAction, boolean stackTagsChanged) throws ValidationErrorException {
    UpdateType updateType = info.supportsTags() && stackTagsChanged ? UpdateType.NO_INTERRUPTION : UpdateType.NONE;
    AWSEC2NetworkInterfaceResourceAction otherAction = (AWSEC2NetworkInterfaceResourceAction) resourceAction;
    if (!Objects.equals(properties.getDescription(), otherAction.properties.getDescription())) {
      updateType = UpdateType.max(updateType, UpdateType.NO_INTERRUPTION);
    }
    if (!Objects.equals(properties.getGroupSet(), otherAction.properties.getGroupSet())) {
      updateType = UpdateType.max(updateType, UpdateType.NO_INTERRUPTION);
    }
    if (!Objects.equals(properties.getPrivateIpAddress(), otherAction.properties.getPrivateIpAddress())) {
      updateType = UpdateType.max(updateType, UpdateType.NEEDS_REPLACEMENT);
    }
    if (!Objects.equals(properties.getPrivateIpAddresses(), otherAction.properties.getPrivateIpAddresses())) {
      if (primaryAddressChanged(properties.getPrivateIpAddresses(), otherAction.properties.getPrivateIpAddresses())) {
        updateType = UpdateType.max(updateType, UpdateType.NEEDS_REPLACEMENT);
      } else {
        updateType = UpdateType.max(updateType, UpdateType.NO_INTERRUPTION);
      }
    }
    if (!Objects.equals(properties.getSecondaryPrivateIpAddressCount(), otherAction.properties.getSecondaryPrivateIpAddressCount())) {
      updateType = UpdateType.max(updateType, UpdateType.NO_INTERRUPTION);
    }
    if (!Objects.equals(properties.getSourceDestCheck(), otherAction.properties.getSourceDestCheck())) {
      updateType = UpdateType.max(updateType, UpdateType.NO_INTERRUPTION);
    }
    if (!Objects.equals(properties.getSubnetId(), otherAction.properties.getSubnetId())) {
      updateType = UpdateType.max(updateType, UpdateType.NEEDS_REPLACEMENT);
    }
    if (!Objects.equals(properties.getTags(), otherAction.properties.getTags())) {
      updateType = UpdateType.max(updateType, UpdateType.NO_INTERRUPTION);
    }
    return updateType;
  }

  private boolean primaryAddressChanged(List<PrivateIpAddressSpecification> privateIpAddresses1, List<PrivateIpAddressSpecification> privateIpAddresses2) throws ValidationErrorException {
    String primaryAddress1 = getPrimaryAddressStr(privateIpAddresses1);
    String primaryAddress2 = getPrimaryAddressStr(privateIpAddresses2);
    return !Objects.equals(primaryAddress1, primaryAddress2);
  }

  private String getPrimaryAddressStr(List<PrivateIpAddressSpecification> privateIpAddresses) throws ValidationErrorException {
    String primary = null;
    if (privateIpAddresses != null) {
      for (PrivateIpAddressSpecification spec: privateIpAddresses) {
        if (spec != null && Boolean.TRUE.equals(spec.getPrimary())) {
          if (primary != null) throw new ValidationErrorException("More than one primary private ip address was passed in");
          primary = spec.getPrivateIpAddress();
        }
      }
    }
    return primary;
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
        if (action.properties.getPrivateIpAddresses() != null && !action.properties.getPrivateIpAddresses().isEmpty()) {
          if (action.properties.getSecondaryPrivateIpAddressCount() != null) {
            throw new ValidationErrorException("SecondaryPrivateIpAddressCount can only be assigned when no secondary private IP address is specified in PrivateIpAddresses");
          }
          createNetworkInterfaceType.setPrivateIpAddressesSet(action.convertPrivateIpAddresses(action.properties.getPrivateIpAddresses()));
        }
        if (action.properties.getGroupSet() != null && !action.properties.getGroupSet().isEmpty()) {
          createNetworkInterfaceType.setGroupSet(action.convertGroupSet(action.properties.getGroupSet()));
        }
        if (action.properties.getSecondaryPrivateIpAddressCount() != null) {
          action.properties.setSecondaryPrivateIpAddressCount(action.properties.getSecondaryPrivateIpAddressCount());
        }
        CreateNetworkInterfaceResponseType createNetworkInterfaceResponseType = AsyncRequests.<CreateNetworkInterfaceType, CreateNetworkInterfaceResponseType>sendSync(configuration, createNetworkInterfaceType);
        action.info.setPhysicalResourceId(createNetworkInterfaceResponseType.getNetworkInterface().getNetworkInterfaceId());
        action.info.setCreatedEnoughToDelete(true);
        action.info.setReferenceValueJson(JsonHelper.getStringFromJsonNode(new TextNode(action.info.getPhysicalResourceId())));
        return action;
      }
    },
    SET_SOURCE_DEST_CHECK {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        AWSEC2NetworkInterfaceResourceAction action = (AWSEC2NetworkInterfaceResourceAction) resourceAction;
        ServiceConfiguration configuration = Topology.lookup(Compute.class);
        if (action.properties.getSourceDestCheck() != null) {
          ModifyNetworkInterfaceAttributeType modifyNetworkInterfaceAttributeType = MessageHelper.createMessage(ModifyNetworkInterfaceAttributeType.class, action.info.getEffectiveUserId());
          modifyNetworkInterfaceAttributeType.setNetworkInterfaceId(action.info.getPhysicalResourceId());
          modifyNetworkInterfaceAttributeType.setSourceDestCheck(action.convertAttributeBooleanValueType(action.properties.getSourceDestCheck()));
          ModifyNetworkInterfaceAttributeResponseType modifyNetworkInterfaceAttributeResponseType = AsyncRequests.sendSync(configuration, modifyNetworkInterfaceAttributeType);
        }
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
        describeNetworkInterfacesType.getFilterSet( ).add( CloudFilters.filter( "network-interface-id", action.info.getPhysicalResourceId( ) ) );
        DescribeNetworkInterfacesResponseType describeNetworkInterfacesResponseType = AsyncRequests.sendSync(configuration, describeNetworkInterfacesType);
        if (describeNetworkInterfacesResponseType.getNetworkInterfaceSet() == null ||
            describeNetworkInterfacesResponseType.getNetworkInterfaceSet().getItem() == null ||
            describeNetworkInterfacesResponseType.getNetworkInterfaceSet().getItem().size() != 1) {
          throw new ValidationErrorException("Network interface " + action.info.getPhysicalResourceId() + " either does not exist or is not unique");
        }
        // Get the private ip addresses
        String primaryIp = null;
        boolean foundPrimary = false;
        ArrayNode secondaryIpArrayNode = JsonHelper.createArrayNode();
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
        describeNetworkInterfacesType.getFilterSet( ).add( CloudFilters.filter( "network-interface-id", action.info.getPhysicalResourceId( ) ) );
        DescribeNetworkInterfacesResponseType describeNetworkInterfacesResponseType = AsyncRequests.sendSync( configuration, describeNetworkInterfacesType );
        if (describeNetworkInterfacesResponseType.getNetworkInterfaceSet().getItem().size() ==0) {
          throw new RetryAfterConditionCheckFailedException("Network interface " + action.info.getPhysicalResourceId() + " not yet available");
        }
        if (!"available".equals(describeNetworkInterfacesResponseType.getNetworkInterfaceSet().getItem().get(0).getStatus())) {
          throw new RetryAfterConditionCheckFailedException("Volume " + action.info.getPhysicalResourceId() + " not yet available");
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
    DELETE_NETWORK_INTERFACE {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        AWSEC2NetworkInterfaceResourceAction action = (AWSEC2NetworkInterfaceResourceAction) resourceAction;
        ServiceConfiguration configuration = Topology.lookup(Compute.class);
        if (!Boolean.TRUE.equals(action.info.getCreatedEnoughToDelete())) return action;
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
        if (!Boolean.TRUE.equals(action.info.getCreatedEnoughToDelete())) return action;
        if (checkDeleted(action, configuration)) return action;
        throw new RetryAfterConditionCheckFailedException("Network interface " + action.info.getPhysicalResourceId() + " not yet deleted");
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
      describeNetworkInterfacesType.getFilterSet( ).add( CloudFilters.filter( "network-interface-id", action.info.getPhysicalResourceId( ) ) );
      DescribeNetworkInterfacesResponseType describeNetworkInterfacesResponseType = AsyncRequests.sendSync(configuration, describeNetworkInterfacesType);
      if (describeNetworkInterfacesResponseType.getNetworkInterfaceSet() == null ||
          describeNetworkInterfacesResponseType.getNetworkInterfaceSet().getItem() == null ||
          describeNetworkInterfacesResponseType.getNetworkInterfaceSet().getItem().isEmpty()) {
        return true;
      }
      return false;
    }

  }

  private enum UpdateNoInterruptionSteps implements UpdateStep {
    UPDATE_NETWORK_INTERFACE_ATTRIBUTES {
      public ResourceAction perform(ResourceAction oldResourceAction, ResourceAction newResourceAction) throws Exception {
        AWSEC2NetworkInterfaceResourceAction oldAction = (AWSEC2NetworkInterfaceResourceAction) oldResourceAction;
        AWSEC2NetworkInterfaceResourceAction newAction = (AWSEC2NetworkInterfaceResourceAction) newResourceAction;
        ServiceConfiguration configuration = Topology.lookup(Compute.class);
        if (!Objects.equals(oldAction.properties.getDescription(), newAction.properties.getDescription()) ||
          !Objects.equals(oldAction.properties.getGroupSet(), newAction.properties.getGroupSet()) ||
          !Objects.equals(oldAction.properties.getSourceDestCheck(), newAction.properties.getSourceDestCheck())) {
          ModifyNetworkInterfaceAttributeType modifyNetworkInterfaceAttributeType = MessageHelper.createMessage(ModifyNetworkInterfaceAttributeType.class, newAction.info.getEffectiveUserId());
          modifyNetworkInterfaceAttributeType.setNetworkInterfaceId(newAction.info.getPhysicalResourceId());
          if (!Objects.equals(oldAction.properties.getDescription(), newAction.properties.getDescription())) {
            modifyNetworkInterfaceAttributeType.setDescription(newAction.convertNullableAttributeValueType(newAction.properties.getDescription() != null ? newAction.properties.getDescription() : ""));
          }
          if (!Objects.equals(oldAction.properties.getGroupSet(), newAction.properties.getGroupSet())) {
            modifyNetworkInterfaceAttributeType.setGroupSet(newAction.convertGroupSet(newAction.properties.getGroupSet() != null && !newAction.properties.getGroupSet().isEmpty() ?
              newAction.properties.getGroupSet() : Lists.newArrayList(newAction.getDefaultGroupId(configuration) )));
          }
          if (!Objects.equals(oldAction.properties.getSourceDestCheck(), newAction.properties.getSourceDestCheck())) {
            modifyNetworkInterfaceAttributeType.setSourceDestCheck(newAction.convertAttributeBooleanValueType(newAction.properties.getSourceDestCheck() != null ? newAction.properties.getSourceDestCheck() : Boolean.TRUE));
          }
          ModifyNetworkInterfaceAttributeResponseType modifyNetworkInterfaceAttributeResponseType = AsyncRequests.sendSync(configuration, modifyNetworkInterfaceAttributeType);
        }
        return newAction;
      }
    },
    UPDATE_SECONDARY_IP_ADDRESSES {
      @Override
      public ResourceAction perform(ResourceAction oldResourceAction, ResourceAction newResourceAction) throws Exception {
        AWSEC2NetworkInterfaceResourceAction oldAction = (AWSEC2NetworkInterfaceResourceAction) oldResourceAction;
        AWSEC2NetworkInterfaceResourceAction newAction = (AWSEC2NetworkInterfaceResourceAction) newResourceAction;
        ServiceConfiguration configuration = Topology.lookup(Compute.class);
        if (newAction.properties.getPrivateIpAddresses() != null && !newAction.properties.getPrivateIpAddresses().isEmpty()
          && newAction.properties.getSecondaryPrivateIpAddressCount() != null) {
          throw new ValidationErrorException("SecondaryPrivateIpAddressCount can only be assigned when no secondary private IP address is specified in PrivateIpAddresses");
        }

        if (!Objects.equals(oldAction.properties.getPrivateIpAddresses(), newAction.properties.getPrivateIpAddresses()) ||
          !Objects.equals(oldAction.properties.getSecondaryPrivateIpAddressCount(), newAction.properties.getSecondaryPrivateIpAddressCount())) {

          // Based on tests at aws, if the previous stack has a secondary private ip address count, all old values should be removed.  (Even ones possibly added externally)
          // Otherwise, just the literal secondary values should be removed.
          // So describe addresses and remove
          // 1) All secondary addresses if the oldAction had a secondary address count
          // 2) all the secondary addresses that are in the old list, that still are attached, and not in the new list (if there is a new list) otherwise
          Set<String> oldSecondaryAddresses = oldAction.convertToSetOfAddressStrings(oldAction.properties.getPrivateIpAddresses());
          Set<String> newSecondaryAddresses = newAction.convertToSetOfAddressStrings(newAction.properties.getPrivateIpAddresses());

          Set<String> existingSecondaryAddresses = Sets.newLinkedHashSet();
          DescribeNetworkInterfacesType describeNetworkInterfacesType = MessageHelper.createMessage(DescribeNetworkInterfacesType.class, newAction.info.getEffectiveUserId());
          describeNetworkInterfacesType.getFilterSet( ).add( CloudFilters.filter( "network-interface-id", newAction.info.getPhysicalResourceId( ) ) );
          DescribeNetworkInterfacesResponseType describeNetworkInterfacesResponseType = AsyncRequests.sendSync(configuration, describeNetworkInterfacesType);
          if (describeNetworkInterfacesResponseType.getNetworkInterfaceSet() == null ||
          describeNetworkInterfacesResponseType.getNetworkInterfaceSet().getItem() == null ||
            describeNetworkInterfacesResponseType.getNetworkInterfaceSet().getItem().size() != 1) {
            throw new ValidationErrorException("Network interface " + newAction.info.getPhysicalResourceId() + " either does not exist or is not unique");
          }
          for (NetworkInterfacePrivateIpAddressesSetItemType networkInterfacePrivateIpAddressesSetItemType : describeNetworkInterfacesResponseType.getNetworkInterfaceSet().getItem().get(0).getPrivateIpAddressesSet().getItem()) {
            if (!Boolean.TRUE.equals(networkInterfacePrivateIpAddressesSetItemType.getPrimary())) {
              existingSecondaryAddresses.add(networkInterfacePrivateIpAddressesSetItemType.getPrivateIpAddress());
            }
          }
          Set<String> addressesToRemove = Sets.newLinkedHashSet();
          if (oldAction.properties.getSecondaryPrivateIpAddressCount() != null && oldAction.properties.getSecondaryPrivateIpAddressCount() > 0) {
            addressesToRemove.addAll(existingSecondaryAddresses);
          } else {
            addressesToRemove.addAll(Sets.difference(Sets.intersection(existingSecondaryAddresses, oldSecondaryAddresses), newSecondaryAddresses));
          }
          if (!addressesToRemove.isEmpty()) {
            UnassignPrivateIpAddressesType unassignPrivateIpAddressesType = MessageHelper.createMessage(UnassignPrivateIpAddressesType.class, newAction.info.getEffectiveUserId());
            unassignPrivateIpAddressesType.setPrivateIpAddressesSet(newAction.convertToPrivateIpAddressSet(addressesToRemove));
            unassignPrivateIpAddressesType.setNetworkInterfaceId(newAction.info.getPhysicalResourceId());
            AsyncRequests.sendSync(configuration, unassignPrivateIpAddressesType);
          }

          // once here, if we have any addresses or count, add them as appropriate
          if (!newSecondaryAddresses.isEmpty() ||
            (newAction.properties.getSecondaryPrivateIpAddressCount() != null && newAction.properties.getSecondaryPrivateIpAddressCount() > 0)) {
            AssignPrivateIpAddressesType assignPrivateIpAddressesType = MessageHelper.createMessage(AssignPrivateIpAddressesType.class, newAction.info.getEffectiveUserId());
            assignPrivateIpAddressesType.setNetworkInterfaceId(newAction.info.getPhysicalResourceId());
            if (newAction.properties.getSecondaryPrivateIpAddressCount() != null && newAction.properties.getSecondaryPrivateIpAddressCount() > 0) {
              assignPrivateIpAddressesType.setSecondaryPrivateIpAddressCount(newAction.properties.getSecondaryPrivateIpAddressCount());
            } else {
              assignPrivateIpAddressesType.setPrivateIpAddressesSet(newAction.convertToPrivateIpAddressSet(Sets.difference(newSecondaryAddresses, Sets.intersection(oldSecondaryAddresses, existingSecondaryAddresses))));
            }
            AsyncRequests.sendSync(configuration, assignPrivateIpAddressesType);
          }
        }

        return newAction;
      }
    },
    UPDATE_IP_ADDRESS_ATTRIBUTES {
      @Override
      public ResourceAction perform(ResourceAction oldResourceAction, ResourceAction newResourceAction) throws Exception {
        AWSEC2NetworkInterfaceResourceAction oldAction = (AWSEC2NetworkInterfaceResourceAction) oldResourceAction;
        AWSEC2NetworkInterfaceResourceAction newAction = (AWSEC2NetworkInterfaceResourceAction) newResourceAction;
        ServiceConfiguration configuration = Topology.lookup(Compute.class);
        // Note: this is done separately, because an exception is thrown if not exactly one item is primary and we won't persist the network interface id,
        // but it will have been created
        DescribeNetworkInterfacesType describeNetworkInterfacesType = MessageHelper.createMessage(DescribeNetworkInterfacesType.class, newAction.info.getEffectiveUserId());
        describeNetworkInterfacesType.getFilterSet( ).add( CloudFilters.filter("network-interface-id", newAction.info.getPhysicalResourceId()) );
        DescribeNetworkInterfacesResponseType describeNetworkInterfacesResponseType = AsyncRequests.sendSync(configuration, describeNetworkInterfacesType);
        if (describeNetworkInterfacesResponseType.getNetworkInterfaceSet() == null ||
          describeNetworkInterfacesResponseType.getNetworkInterfaceSet().getItem() == null ||
          describeNetworkInterfacesResponseType.getNetworkInterfaceSet().getItem().size() != 1) {
          throw new ValidationErrorException("Network interface " + newAction.info.getPhysicalResourceId() + " either does not exist or is not unique");
        }
        // Get the private ip addresses
        String primaryIp = null;
        boolean foundPrimary = false;
        ArrayNode secondaryIpArrayNode = JsonHelper.createArrayNode();
        for (NetworkInterfacePrivateIpAddressesSetItemType networkInterfacePrivateIpAddressesSetItemType : describeNetworkInterfacesResponseType.getNetworkInterfaceSet().getItem().get(0).getPrivateIpAddressesSet().getItem()) {
          if (networkInterfacePrivateIpAddressesSetItemType.getPrimary()) {
            if (foundPrimary) {
              throw new ValidationErrorException("Network interface " + newAction.info.getPhysicalResourceId() + " has a non-unique primary private ip address");
            } else {
              primaryIp = networkInterfacePrivateIpAddressesSetItemType.getPrivateIpAddress();
              foundPrimary = true;
            }
          } else {
            secondaryIpArrayNode.add(networkInterfacePrivateIpAddressesSetItemType.getPrivateIpAddress());
          }
        }
        if (!foundPrimary) {
          throw new ValidationErrorException("Network interface " + newAction.info.getPhysicalResourceId() + " has no primary private ip address");
        }
        newAction.info.setPrimaryPrivateIpAddress(JsonHelper.getStringFromJsonNode(new TextNode(primaryIp)));
        newAction.info.setSecondaryPrivateIpAddresses(JsonHelper.getStringFromJsonNode(secondaryIpArrayNode));
        return newAction;
      }
    },

    UPDATE_TAGS {
      @Override
      public ResourceAction perform(ResourceAction oldResourceAction, ResourceAction newResourceAction) throws Exception {
        AWSEC2NetworkInterfaceResourceAction oldAction = (AWSEC2NetworkInterfaceResourceAction) oldResourceAction;
        AWSEC2NetworkInterfaceResourceAction newAction = (AWSEC2NetworkInterfaceResourceAction) newResourceAction;
        ServiceConfiguration configuration = Topology.lookup(Compute.class);
        DescribeTagsType describeTagsType = MessageHelper.createMessage(DescribeTagsType.class, newAction.info.getEffectiveUserId());
        describeTagsType.setFilterSet(Lists.newArrayList( CloudFilters.filter("resource-id", newAction.info.getPhysicalResourceId())));
        DescribeTagsResponseType describeTagsResponseType = AsyncRequests.sendSync(configuration, describeTagsType);
        Set<EC2Tag> existingTags = Sets.newLinkedHashSet();
        if (describeTagsResponseType != null && describeTagsResponseType.getTagSet() != null) {
          for (TagInfo tagInfo : describeTagsResponseType.getTagSet()) {
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

  private String getDefaultGroupId(ServiceConfiguration configuration) throws Exception {
    DescribeSubnetsType describeSubnetsType = MessageHelper.createMessage(DescribeSubnetsType.class, info.getEffectiveUserId());
    describeSubnetsType.setSubnetSet(makeSubnetSet(properties.getSubnetId()));
    DescribeSubnetsResponseType describeSubnetsResponseType = AsyncRequests.sendSync(configuration, describeSubnetsType);
    if (describeSubnetsResponseType == null || describeSubnetsResponseType.getSubnetSet() == null || describeSubnetsResponseType.getSubnetSet().getItem() == null ||
      describeSubnetsResponseType.getSubnetSet().getItem().size() != 1) {
      throw new ValidationErrorException("Subnet id " + properties.getSubnetId() + " matches either zero or more than one subnet");
    }
    String vpcId = describeSubnetsResponseType.getSubnetSet().getItem().get(0).getVpcId();
    DescribeSecurityGroupsType describeSecurityGroupsType = MessageHelper.createMessage(DescribeSecurityGroupsType.class, info.getEffectiveUserId());
    describeSecurityGroupsType.getFilterSet().add( CloudFilters.filter("vpc-id", vpcId));
    describeSecurityGroupsType.getFilterSet().add( CloudFilters.filter("group-name", "default"));
    DescribeSecurityGroupsResponseType describeSecurityGroupsResponseType = AsyncRequests.sendSync(configuration, describeSecurityGroupsType);
    if (describeSecurityGroupsResponseType == null || describeSecurityGroupsResponseType.getSecurityGroupInfo() == null ||
      describeSecurityGroupsResponseType.getSecurityGroupInfo().size() != 1) {
      throw new ValidationErrorException("Could not find unique default security group for vpc " + vpcId);
    }
    return describeSecurityGroupsResponseType.getSecurityGroupInfo().get(0).getGroupId();
  }

  private SubnetIdSetType makeSubnetSet(String subnetId) {
    SubnetIdSetType subnetIdSetType = new SubnetIdSetType();
    ArrayList<SubnetIdSetItemType> item = Lists.newArrayList();
    SubnetIdSetItemType subnetIdSetItemType = new SubnetIdSetItemType();
    subnetIdSetItemType.setSubnetId(subnetId);
    item.add(subnetIdSetItemType);
    subnetIdSetType.setItem(item);
    return subnetIdSetType;
  }

  private NullableAttributeValueType convertNullableAttributeValueType(String value) {
    NullableAttributeValueType nullableAttributeValueType = new NullableAttributeValueType();
    nullableAttributeValueType.setValue(value);
    return nullableAttributeValueType;
  }

  private AssignPrivateIpAddressesSetRequestType convertToPrivateIpAddressSet(Set<String> ipAddresses) {
    AssignPrivateIpAddressesSetRequestType assignPrivateIpAddressesSetRequestType = new AssignPrivateIpAddressesSetRequestType();
    ArrayList<AssignPrivateIpAddressesSetItemRequestType> item = Lists.newArrayList();
    if (ipAddresses != null) {
      for (String ipAddress: ipAddresses) {
        AssignPrivateIpAddressesSetItemRequestType assignPrivateIpAddressesSetItemRequestType = new AssignPrivateIpAddressesSetItemRequestType();
        assignPrivateIpAddressesSetItemRequestType.setPrivateIpAddress(ipAddress);
        item.add(assignPrivateIpAddressesSetItemRequestType);
      }
    }
    assignPrivateIpAddressesSetRequestType.setItem(item);
    return assignPrivateIpAddressesSetRequestType;
  }

  private Set<String> convertToSetOfAddressStrings(List<PrivateIpAddressSpecification> privateIpAddresses) {
    Set<String> setOfAddressStrings = Sets.newLinkedHashSet();
    if (privateIpAddresses != null) {
      for (PrivateIpAddressSpecification privateIpAddressSpecification: privateIpAddresses) {
        if (privateIpAddressSpecification != null && !Boolean.TRUE.equals(privateIpAddressSpecification.getPrimary()) && privateIpAddressSpecification.getPrivateIpAddress() != null) {
          setOfAddressStrings.add(privateIpAddressSpecification.getPrivateIpAddress());
        }
      }
    }
    return setOfAddressStrings;
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

  private AttributeBooleanValueType convertAttributeBooleanValueType(Boolean bool) {
    AttributeBooleanValueType attributeBooleanValueType = new AttributeBooleanValueType();
    attributeBooleanValueType.setValue(bool);
    return attributeBooleanValueType;
  }


}


