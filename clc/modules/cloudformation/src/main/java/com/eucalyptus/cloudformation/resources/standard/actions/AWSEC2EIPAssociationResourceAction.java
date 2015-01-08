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
import com.eucalyptus.cloudformation.ValidationErrorException;
import com.eucalyptus.cloudformation.resources.ResourceAction;
import com.eucalyptus.cloudformation.resources.ResourceInfo;
import com.eucalyptus.cloudformation.resources.ResourceProperties;
import com.eucalyptus.cloudformation.resources.standard.info.AWSEC2EIPAssociationResourceInfo;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.AWSEC2EIPAssociationProperties;
import com.eucalyptus.cloudformation.template.JsonHelper;
import com.eucalyptus.cloudformation.util.MessageHelper;
import com.eucalyptus.cloudformation.workflow.StackActivity;
import com.eucalyptus.cloudformation.workflow.steps.CreateMultiStepPromise;
import com.eucalyptus.cloudformation.workflow.steps.DeleteMultiStepPromise;
import com.eucalyptus.cloudformation.workflow.steps.Step;
import com.eucalyptus.cloudformation.workflow.steps.StepTransform;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.Topology;
import com.eucalyptus.compute.common.AssociateAddressResponseType;
import com.eucalyptus.compute.common.AssociateAddressType;
import com.eucalyptus.compute.common.Compute;
import com.eucalyptus.compute.common.DescribeAddressesResponseType;
import com.eucalyptus.compute.common.DescribeAddressesType;
import com.eucalyptus.compute.common.DescribeInstancesResponseType;
import com.eucalyptus.compute.common.DescribeInstancesType;
import com.eucalyptus.compute.common.DisassociateAddressResponseType;
import com.eucalyptus.compute.common.DisassociateAddressType;
import com.eucalyptus.util.async.AsyncRequests;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.collect.Lists;
import com.netflix.glisten.WorkflowOperations;
import org.apache.log4j.Logger;

import java.util.List;
import javax.annotation.Nullable;

/**
 * Created by ethomas on 2/3/14.
 */
public class AWSEC2EIPAssociationResourceAction extends ResourceAction {
  private static final Logger LOG = Logger.getLogger(AWSEC2EIPAssociationResourceAction.class);
  private AWSEC2EIPAssociationProperties properties = new AWSEC2EIPAssociationProperties();
  private AWSEC2EIPAssociationResourceInfo info = new AWSEC2EIPAssociationResourceInfo();

  public AWSEC2EIPAssociationResourceAction() {
    for (CreateSteps createStep: CreateSteps.values()) {
      createSteps.put(createStep.name(), createStep);
    }
    for (DeleteSteps deleteStep: DeleteSteps.values()) {
      deleteSteps.put(deleteStep.name(), deleteStep);
    }

  }
  private enum CreateSteps implements Step {
    CREATE_EIP_ASSOCIATION {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        AWSEC2EIPAssociationResourceAction action = (AWSEC2EIPAssociationResourceAction) resourceAction;
        ServiceConfiguration configuration = Topology.lookup(Compute.class);
        AssociateAddressType associateAddressType = MessageHelper.createMessage(AssociateAddressType.class, action.info.getEffectiveUserId());
        if (action.properties.getInstanceId() != null) {
          DescribeInstancesType describeInstancesType = MessageHelper.createMessage(DescribeInstancesType.class, action.info.getEffectiveUserId());
          describeInstancesType.setInstancesSet(Lists.newArrayList(action.properties.getInstanceId()));
          DescribeInstancesResponseType describeInstancesResponseType = AsyncRequests.<DescribeInstancesType,DescribeInstancesResponseType> sendSync(configuration, describeInstancesType);
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
          associateAddressType.setNetworkInterfaceId(action.properties.getNetworkInterfaceId());
        }
        AssociateAddressResponseType associateAddressResponseType = AsyncRequests.<AssociateAddressType, AssociateAddressResponseType> sendSync(configuration, associateAddressType);
        if (action.properties.getAllocationId() != null) {
          action.info.setPhysicalResourceId(associateAddressResponseType.getAssociationId());
        } else {
          action.info.setPhysicalResourceId(action.getDefaultPhysicalResourceId());
        }
        action.info.setReferenceValueJson(JsonHelper.getStringFromJsonNode(new TextNode(action.info.getPhysicalResourceId())));
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
        if (action.info.getPhysicalResourceId() == null) return action;
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


