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
import com.eucalyptus.cloudformation.entity.StackResourceEntity;
import com.eucalyptus.cloudformation.entity.StackResourceEntityManager;
import com.eucalyptus.cloudformation.resources.EC2Helper;
import com.eucalyptus.cloudformation.resources.ResourceAction;
import com.eucalyptus.cloudformation.resources.ResourceInfo;
import com.eucalyptus.cloudformation.resources.ResourceProperties;
import com.eucalyptus.cloudformation.resources.standard.info.AWSEC2EIPAssociationResourceInfo;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.AWSEC2EIPAssociationProperties;
import com.eucalyptus.cloudformation.template.JsonHelper;
import com.eucalyptus.cloudformation.util.MessageHelper;
import com.eucalyptus.cloudformation.workflow.steps.Step;
import com.eucalyptus.cloudformation.workflow.steps.StepBasedResourceAction;
import com.eucalyptus.cloudformation.workflow.steps.UpdateStep;
import com.eucalyptus.cloudformation.workflow.updateinfo.UpdateType;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.Topology;
import com.eucalyptus.compute.common.AssociateAddressResponseType;
import com.eucalyptus.compute.common.AssociateAddressType;
import com.eucalyptus.compute.common.CloudFilters;
import com.eucalyptus.compute.common.Compute;
import com.eucalyptus.compute.common.DescribeAddressesResponseType;
import com.eucalyptus.compute.common.DescribeAddressesType;
import com.eucalyptus.compute.common.DescribeInstancesResponseType;
import com.eucalyptus.compute.common.DescribeInstancesType;
import com.eucalyptus.compute.common.DescribeNetworkInterfacesResponseType;
import com.eucalyptus.compute.common.DescribeNetworkInterfacesType;
import com.eucalyptus.compute.common.DisassociateAddressResponseType;
import com.eucalyptus.compute.common.DisassociateAddressType;
import com.eucalyptus.compute.common.NetworkInterfaceType;
import com.eucalyptus.util.async.AsyncRequests;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.collect.Lists;
import org.apache.log4j.Logger;

import javax.annotation.Nullable;
import java.util.Objects;

/**
 * Created by ethomas on 2/3/14.
 */
public class AWSEC2EIPAssociationResourceAction extends StepBasedResourceAction {
  private static final Logger LOG = Logger.getLogger(AWSEC2EIPAssociationResourceAction.class);
  private AWSEC2EIPAssociationProperties properties = new AWSEC2EIPAssociationProperties();
  private AWSEC2EIPAssociationResourceInfo info = new AWSEC2EIPAssociationResourceInfo();

  public AWSEC2EIPAssociationResourceAction() {
    super(fromEnum(CreateSteps.class), fromEnum(DeleteSteps.class), fromUpdateEnum(UpdateNoInterruptionSteps.class), null);
  }

  @Override
  public boolean mustCheckUpdateTypeEvenIfNoPropertiesChanged() {
    return true;
  }

  @Override
  public UpdateType getUpdateType(ResourceAction resourceAction, boolean stackTagsChanged) {
    UpdateType updateType = info.supportsTags() && stackTagsChanged ? UpdateType.NO_INTERRUPTION : UpdateType.NONE;
    AWSEC2EIPAssociationResourceAction otherAction = (AWSEC2EIPAssociationResourceAction) resourceAction;
    if (!Objects.equals(properties.getAllocationId(), otherAction.properties.getAllocationId())) {
      // Update requires: Replacement if you also change the InstanceId or NetworkInterfaceId property. If not, update requires No interruption.
      if (!Objects.equals(properties.getInstanceId(), otherAction.properties.getInstanceId()) ||
        !Objects.equals(properties.getNetworkInterfaceId(), otherAction.properties.getNetworkInterfaceId())) {
        updateType = UpdateType.max(updateType, UpdateType.NEEDS_REPLACEMENT);
      } else {
        updateType = UpdateType.max(updateType, UpdateType.NO_INTERRUPTION);
      }
    }
    if (!Objects.equals(properties.getEip(), otherAction.properties.getEip())) {
      // Update requires: Replacement if you also change the InstanceId or NetworkInterfaceId property. If not, update requires No interruption.
      if (!Objects.equals(properties.getInstanceId(), otherAction.properties.getInstanceId()) ||
        !Objects.equals(properties.getNetworkInterfaceId(), otherAction.properties.getNetworkInterfaceId())) {
        updateType = UpdateType.max(updateType, UpdateType.NEEDS_REPLACEMENT);
      } else {
        updateType = UpdateType.max(updateType, UpdateType.NO_INTERRUPTION);
      }
    }
    // Might be a little redundancy here, but trying to adhere to the instructions exactly rather than optimize
    if (!Objects.equals(properties.getInstanceId(), otherAction.properties.getInstanceId())) {
      // Update requires: Replacement if you also change the AllocationId or EIP property. If not, update requires No interruption.
      if (!Objects.equals(properties.getAllocationId(), otherAction.properties.getAllocationId()) ||
        !Objects.equals(properties.getEip(), otherAction.properties.getEip())) {
        updateType = UpdateType.max(updateType, UpdateType.NEEDS_REPLACEMENT);
      } else {
        updateType = UpdateType.max(updateType, UpdateType.NO_INTERRUPTION);
      }
    }
    if (!Objects.equals(properties.getNetworkInterfaceId(), otherAction.properties.getNetworkInterfaceId())) {
      // Update requires: Replacement if you also change the AllocationId or EIP property. If not, update requires No interruption.
      if (!Objects.equals(properties.getAllocationId(), otherAction.properties.getAllocationId()) ||
        !Objects.equals(properties.getEip(), otherAction.properties.getEip())) {
        updateType = UpdateType.max(updateType, UpdateType.NEEDS_REPLACEMENT);
      } else {
        updateType = UpdateType.max(updateType, UpdateType.NO_INTERRUPTION);
      }
    }
    if (!Objects.equals(properties.getPrivateIpAddress(), otherAction.properties.getPrivateIpAddress())) {
      updateType = UpdateType.max(updateType, UpdateType.NO_INTERRUPTION);
    }
    // finally update if the 'instance' we are associated was either NO_INTERRUPTION or SOME_INTERRUPTION updated
    if (Objects.equals(properties.getInstanceId(), otherAction.properties.getInstanceId()) ) {
      if (wasInstanceUpdated(otherAction)) {
        updateType = UpdateType.max(updateType, UpdateType.NO_INTERRUPTION);
      }
    }
    return updateType;
  }

  private boolean wasInstanceUpdated(AWSEC2EIPAssociationResourceAction action) {
    if (action.properties.getInstanceId() == null) return false;
    StackResourceEntity stackResourceEntity = StackResourceEntityManager.getStackResourceByPhysicalResourceId(action.stackEntity.getStackId(), action.stackEntity.getAccountId(), action.properties.getInstanceId(), action.stackEntity.getStackVersion());
    return (stackResourceEntity != null &&
      (stackResourceEntity.getUpdateType().equals(UpdateType.NO_INTERRUPTION.toString()) || stackResourceEntity.getUpdateType().equals(UpdateType.SOME_INTERRUPTION.toString())));
  }

  private enum CreateSteps implements Step {
    CREATE_EIP_ASSOCIATION {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        AWSEC2EIPAssociationResourceAction action = (AWSEC2EIPAssociationResourceAction) resourceAction;
        ServiceConfiguration configuration = Topology.lookup(Compute.class);
        AssociateAddressType associateAddressType = MessageHelper.createMessage(AssociateAddressType.class, action.info.getEffectiveUserId());
        associateAddressType.setAllowReassociation(true); // to allow for no-interruption update
        if (action.properties.getInstanceId() == null && action.properties.getNetworkInterfaceId() == null) {
          throw new ValidationErrorException("Either instance ID or network interface id must be specified");
        }
        if (action.properties.getInstanceId() != null) {
          DescribeInstancesType describeInstancesType = MessageHelper.createMessage(DescribeInstancesType.class, action.info.getEffectiveUserId());
          describeInstancesType.getFilterSet( ).add( CloudFilters.filter( "instance-id", action.properties.getInstanceId() ) );
          DescribeInstancesResponseType describeInstancesResponseType = AsyncRequests.sendSync( configuration, describeInstancesType );
          if (describeInstancesResponseType.getReservationSet() == null || describeInstancesResponseType.getReservationSet().isEmpty()) {
            throw new ValidationErrorException("No such instance " + action.properties.getInstanceId());
          }
          associateAddressType.setInstanceId(action.properties.getInstanceId());
        }
        if (action.properties.getEip() != null) {
          DescribeAddressesType describeAddressesType = MessageHelper.createMessage(DescribeAddressesType.class, action.info.getEffectiveUserId());
          describeAddressesType.setPublicIpsSet(Lists.newArrayList(action.properties.getEip()));
          DescribeAddressesResponseType describeAddressesResponseType = AsyncRequests.<DescribeAddressesType, DescribeAddressesResponseType> sendSync(configuration, describeAddressesType);
          if (describeAddressesResponseType.getAddressesSet() == null || describeAddressesResponseType.getAddressesSet().isEmpty()) {
            throw new ValidationErrorException("No such EIP " + action.properties.getEip());
          }
          associateAddressType.setPublicIp(action.properties.getEip());
        }
        if (action.properties.getAllocationId() != null) {
          DescribeAddressesType describeAddressesType = MessageHelper.createMessage(DescribeAddressesType.class, action.info.getEffectiveUserId());
          describeAddressesType.setAllocationIds(Lists.newArrayList(action.properties.getAllocationId()));
          DescribeAddressesResponseType describeAddressesResponseType = AsyncRequests.<DescribeAddressesType, DescribeAddressesResponseType> sendSync(configuration, describeAddressesType);
          if (describeAddressesResponseType.getAddressesSet() == null || describeAddressesResponseType.getAddressesSet().isEmpty()) {
            throw new ValidationErrorException("No such allocation-id " + action.properties.getAllocationId());
          }
          associateAddressType.setAllocationId(action.properties.getAllocationId());
        }
        if (action.properties.getNetworkInterfaceId() != null) {
          associateAddressType.setNetworkInterfaceId(action.properties.getNetworkInterfaceId());
        }
        if (action.properties.getPrivateIpAddress() != null) {
          associateAddressType.setPrivateIpAddress(action.properties.getPrivateIpAddress());
        }
        AssociateAddressResponseType associateAddressResponseType = AsyncRequests.<AssociateAddressType, AssociateAddressResponseType> sendSync(configuration, associateAddressType);
        if (action.properties.getAllocationId() != null) {
          action.info.setPhysicalResourceId(associateAddressResponseType.getAssociationId());
        } else {
          action.info.setPhysicalResourceId(action.getDefaultPhysicalResourceId());
        }
        action.info.setCreatedEnoughToDelete(true);
        action.info.setReferenceValueJson(JsonHelper.getStringFromJsonNode(new TextNode(action.info.getPhysicalResourceId())));

        // Update the instance info
        if (action.properties.getInstanceId() != null) {
          EC2Helper.refreshInstanceAttributes(action.getStackEntity(), action.properties.getInstanceId(), action.info.getEffectiveUserId(), action.getStackEntity().getStackVersion());
        }

        // Update the instance info (if network id exists)
        if (action.properties.getNetworkInterfaceId() != null) {
          DescribeNetworkInterfacesType describeNetworkInterfacesType = MessageHelper.createMessage(DescribeNetworkInterfacesType.class, action.info.getEffectiveUserId());
          describeNetworkInterfacesType.getFilterSet( ).add( CloudFilters.filter( "network-interface-id", action.properties.getNetworkInterfaceId() ) );
          DescribeNetworkInterfacesResponseType describeNetworkInterfacesResponseType = AsyncRequests.sendSync( configuration, describeNetworkInterfacesType);
          if (describeNetworkInterfacesResponseType.getNetworkInterfaceSet() != null &&
            describeNetworkInterfacesResponseType.getNetworkInterfaceSet().getItem() != null &&
            !describeNetworkInterfacesResponseType.getNetworkInterfaceSet().getItem().isEmpty()) {
            for (NetworkInterfaceType networkInterfaceType: describeNetworkInterfacesResponseType.getNetworkInterfaceSet().getItem()) {
              if (networkInterfaceType != null && networkInterfaceType.getAttachment() != null &&
                networkInterfaceType.getAttachment().getDeviceIndex() == 0 &&
                networkInterfaceType.getAttachment().getInstanceId() != null) {
                EC2Helper.refreshInstanceAttributes(action.getStackEntity(), networkInterfaceType.getAttachment().getInstanceId(), action.info.getEffectiveUserId(), action.getStackEntity().getStackVersion());
              }
            }
          }
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
    DELETE_EIP_ASSOCIATION {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        AWSEC2EIPAssociationResourceAction action = (AWSEC2EIPAssociationResourceAction) resourceAction;
        ServiceConfiguration configuration = Topology.lookup(Compute.class);
        if (!Boolean.TRUE.equals(action.info.getCreatedEnoughToDelete())) return action;
        if (action.properties.getAllocationId() != null) {
          DescribeAddressesType describeAddressesType = MessageHelper.createMessage(DescribeAddressesType.class, action.info.getEffectiveUserId());
          describeAddressesType.setAllocationIds(Lists.newArrayList(action.properties.getAllocationId()));
          DescribeAddressesResponseType describeAddressesResponseType = AsyncRequests.<DescribeAddressesType, DescribeAddressesResponseType> sendSync(configuration, describeAddressesType);
          if (describeAddressesResponseType.getAddressesSet() != null && !describeAddressesResponseType.getAddressesSet().isEmpty()) {
            DisassociateAddressType disassociateAddressType = MessageHelper.createMessage(DisassociateAddressType.class, action.info.getEffectiveUserId());
            disassociateAddressType.setAssociationId(action.info.getPhysicalResourceId());
            AsyncRequests.<DisassociateAddressType, DisassociateAddressResponseType> sendSync(configuration, disassociateAddressType);
          }
        }
        if (action.properties.getEip() != null) {
          DescribeAddressesType describeAddressesType = MessageHelper.createMessage(DescribeAddressesType.class, action.info.getEffectiveUserId());
          describeAddressesType.setPublicIpsSet(Lists.newArrayList(action.properties.getEip()));
          DescribeAddressesResponseType describeAddressesResponseType = AsyncRequests.<DescribeAddressesType, DescribeAddressesResponseType> sendSync(configuration, describeAddressesType);
          if (describeAddressesResponseType.getAddressesSet() != null && !describeAddressesResponseType.getAddressesSet().isEmpty()) {
            DisassociateAddressType disassociateAddressType = MessageHelper.createMessage(DisassociateAddressType.class, action.info.getEffectiveUserId());
            disassociateAddressType.setPublicIp(action.properties.getEip());
            AsyncRequests.<DisassociateAddressType, DisassociateAddressResponseType> sendSync(configuration, disassociateAddressType);
          }
        }

        // Update the instance info
        if (action.properties.getInstanceId() != null) {
          EC2Helper.refreshInstanceAttributes(action.getStackEntity(), action.properties.getInstanceId(), action.info.getEffectiveUserId(), action.getStackEntity().getStackVersion());
        }

        // Update the instance info (if network id exists)
        if (action.properties.getNetworkInterfaceId() != null) {
          DescribeNetworkInterfacesType describeNetworkInterfacesType = MessageHelper.createMessage(DescribeNetworkInterfacesType.class, action.info.getEffectiveUserId());
          describeNetworkInterfacesType.getFilterSet( ).add( CloudFilters.filter( "network-interface-id", action.properties.getNetworkInterfaceId() ) );
          DescribeNetworkInterfacesResponseType describeNetworkInterfacesResponseType = AsyncRequests.sendSync(configuration, describeNetworkInterfacesType);
          if (describeNetworkInterfacesResponseType.getNetworkInterfaceSet() != null &&
            describeNetworkInterfacesResponseType.getNetworkInterfaceSet().getItem() != null &&
            !describeNetworkInterfacesResponseType.getNetworkInterfaceSet().getItem().isEmpty()) {
            for (NetworkInterfaceType networkInterfaceType: describeNetworkInterfacesResponseType.getNetworkInterfaceSet().getItem()) {
              if (networkInterfaceType != null && networkInterfaceType.getAttachment() != null &&
                networkInterfaceType.getAttachment().getDeviceIndex() == 0 &&
                networkInterfaceType.getAttachment().getInstanceId() != null) {
                EC2Helper.refreshInstanceAttributes(action.getStackEntity(), networkInterfaceType.getAttachment().getInstanceId(), action.info.getEffectiveUserId(), action.getStackEntity().getStackVersion());
              }
            }
          }
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

  private enum UpdateNoInterruptionSteps implements UpdateStep {
    CREATE_EIP_ASSOCIATION {
      @Override
      public ResourceAction perform(ResourceAction oldResourceAction, ResourceAction newResourceAction) throws Exception {
        AWSEC2EIPAssociationResourceAction oldAction = (AWSEC2EIPAssociationResourceAction) oldResourceAction;
        AWSEC2EIPAssociationResourceAction newAction = (AWSEC2EIPAssociationResourceAction) newResourceAction;
        ServiceConfiguration configuration = Topology.lookup(Compute.class);
        AssociateAddressType associateAddressType = MessageHelper.createMessage(AssociateAddressType.class, newAction.info.getEffectiveUserId());
        associateAddressType.setAllowReassociation(true); // to allow for no-interruption update
        if (newAction.properties.getInstanceId() == null && newAction.properties.getNetworkInterfaceId() == null) {
          throw new ValidationErrorException("Either instance ID or network interface id must be specified");
        }
        if (newAction.properties.getInstanceId() != null) {
          DescribeInstancesType describeInstancesType = MessageHelper.createMessage(DescribeInstancesType.class, newAction.info.getEffectiveUserId());
          describeInstancesType.getFilterSet( ).add( CloudFilters.filter( "instance-id", newAction.properties.getInstanceId() ) );
          DescribeInstancesResponseType describeInstancesResponseType = AsyncRequests.sendSync( configuration, describeInstancesType );
          if (describeInstancesResponseType.getReservationSet() == null || describeInstancesResponseType.getReservationSet().isEmpty()) {
            throw new ValidationErrorException("No such instance " + newAction.properties.getInstanceId());
          }
          associateAddressType.setInstanceId(newAction.properties.getInstanceId());
        }
        if (newAction.properties.getEip() != null) {
          DescribeAddressesType describeAddressesType = MessageHelper.createMessage(DescribeAddressesType.class, newAction.info.getEffectiveUserId());
          describeAddressesType.setPublicIpsSet(Lists.newArrayList(newAction.properties.getEip()));
          DescribeAddressesResponseType describeAddressesResponseType = AsyncRequests.<DescribeAddressesType, DescribeAddressesResponseType> sendSync(configuration, describeAddressesType);
          if (describeAddressesResponseType.getAddressesSet() == null || describeAddressesResponseType.getAddressesSet().isEmpty()) {
            throw new ValidationErrorException("No such EIP " + newAction.properties.getEip());
          }
          associateAddressType.setPublicIp(newAction.properties.getEip());
        }
        if (newAction.properties.getAllocationId() != null) {
          DescribeAddressesType describeAddressesType = MessageHelper.createMessage(DescribeAddressesType.class, newAction.info.getEffectiveUserId());
          describeAddressesType.setAllocationIds(Lists.newArrayList(newAction.properties.getAllocationId()));
          DescribeAddressesResponseType describeAddressesResponseType = AsyncRequests.<DescribeAddressesType, DescribeAddressesResponseType> sendSync(configuration, describeAddressesType);
          if (describeAddressesResponseType.getAddressesSet() == null || describeAddressesResponseType.getAddressesSet().isEmpty()) {
            throw new ValidationErrorException("No such allocation-id " + newAction.properties.getAllocationId());
          }
          associateAddressType.setAllocationId(newAction.properties.getAllocationId());
        }
        if (newAction.properties.getNetworkInterfaceId() != null) {
          associateAddressType.setNetworkInterfaceId(newAction.properties.getNetworkInterfaceId());
        }
        if (newAction.properties.getPrivateIpAddress() != null) {
          associateAddressType.setPrivateIpAddress(newAction.properties.getPrivateIpAddress());
        }
        AssociateAddressResponseType associateAddressResponseType = AsyncRequests.<AssociateAddressType, AssociateAddressResponseType> sendSync(configuration, associateAddressType);
        if (newAction.properties.getAllocationId() != null) {
          newAction.info.setPhysicalResourceId(associateAddressResponseType.getAssociationId());
        } else {
          newAction.info.setPhysicalResourceId(newAction.getDefaultPhysicalResourceId());
        }
        newAction.info.setCreatedEnoughToDelete(true);
        newAction.info.setReferenceValueJson(JsonHelper.getStringFromJsonNode(new TextNode(newAction.info.getPhysicalResourceId())));

        // Update the instance info
        if (newAction.properties.getInstanceId() != null) {
          EC2Helper.refreshInstanceAttributes(newAction.getStackEntity(), newAction.properties.getInstanceId(), newAction.info.getEffectiveUserId(), newAction.getStackEntity().getStackVersion());
        }

        // Update the instance info (if network id exists)
        if (newAction.properties.getNetworkInterfaceId() != null) {
          DescribeNetworkInterfacesType describeNetworkInterfacesType = MessageHelper.createMessage(DescribeNetworkInterfacesType.class, newAction.info.getEffectiveUserId());
          describeNetworkInterfacesType.getFilterSet( ).add( CloudFilters.filter( "network-interface-id", newAction.properties.getNetworkInterfaceId() ) );
          DescribeNetworkInterfacesResponseType describeNetworkInterfacesResponseType = AsyncRequests.sendSync( configuration, describeNetworkInterfacesType);
          if (describeNetworkInterfacesResponseType.getNetworkInterfaceSet() != null &&
            describeNetworkInterfacesResponseType.getNetworkInterfaceSet().getItem() != null &&
            !describeNetworkInterfacesResponseType.getNetworkInterfaceSet().getItem().isEmpty()) {
            for (NetworkInterfaceType networkInterfaceType: describeNetworkInterfacesResponseType.getNetworkInterfaceSet().getItem()) {
              if (networkInterfaceType != null && networkInterfaceType.getAttachment() != null &&
                networkInterfaceType.getAttachment().getDeviceIndex() == 0 &&
                networkInterfaceType.getAttachment().getInstanceId() != null) {
                EC2Helper.refreshInstanceAttributes(newAction.getStackEntity(), networkInterfaceType.getAttachment().getInstanceId(), newAction.info.getEffectiveUserId(), newAction.getStackEntity().getStackVersion());
              }
            }
          }
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
    properties = (AWSEC2EIPAssociationProperties) resourceProperties;
  }

  @Override
  public ResourceInfo getResourceInfo() {
    return info;
  }

  @Override
  public void setResourceInfo(ResourceInfo resourceInfo) {
    info = (AWSEC2EIPAssociationResourceInfo) resourceInfo;
  }




}


