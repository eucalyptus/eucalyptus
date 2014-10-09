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
import com.amazonaws.services.simpleworkflow.flow.interceptors.RetryPolicy;
import com.eucalyptus.cloudformation.ValidationErrorException;
import com.eucalyptus.cloudformation.resources.ResourceAction;
import com.eucalyptus.cloudformation.resources.ResourceInfo;
import com.eucalyptus.cloudformation.resources.ResourceProperties;
import com.eucalyptus.cloudformation.resources.standard.info.AWSEC2NetworkInterfaceAttachmentResourceInfo;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.AWSEC2NetworkInterfaceAttachmentProperties;
import com.eucalyptus.cloudformation.template.JsonHelper;
import com.eucalyptus.cloudformation.util.MessageHelper;
import com.eucalyptus.cloudformation.workflow.StackActivity;
import com.eucalyptus.cloudformation.workflow.steps.MultiStepWithRetryCreatePromise;
import com.eucalyptus.cloudformation.workflow.steps.MultiStepWithRetryDeletePromise;
import com.eucalyptus.cloudformation.workflow.steps.Step;
import com.eucalyptus.cloudformation.workflow.steps.StepTransform;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.Topology;
import com.eucalyptus.compute.common.AttachNetworkInterfaceResponseType;
import com.eucalyptus.compute.common.AttachNetworkInterfaceType;
import com.eucalyptus.compute.common.Compute;
import com.eucalyptus.compute.common.DescribeInstancesResponseType;
import com.eucalyptus.compute.common.DescribeInstancesType;
import com.eucalyptus.compute.common.DescribeNetworkInterfacesResponseType;
import com.eucalyptus.compute.common.DescribeNetworkInterfacesType;
import com.eucalyptus.compute.common.DetachNetworkInterfaceResponseType;
import com.eucalyptus.compute.common.DetachNetworkInterfaceType;
import com.eucalyptus.compute.common.NetworkInterfaceIdSetItemType;
import com.eucalyptus.compute.common.NetworkInterfaceIdSetType;
import com.eucalyptus.compute.common.NetworkInterfaceType;
import com.eucalyptus.util.async.AsyncRequests;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.collect.Lists;
import com.netflix.glisten.WorkflowOperations;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ethomas on 2/3/14.
 */
public class AWSEC2NetworkInterfaceAttachmentResourceAction extends ResourceAction {

  private AWSEC2NetworkInterfaceAttachmentProperties properties = new AWSEC2NetworkInterfaceAttachmentProperties();
  private AWSEC2NetworkInterfaceAttachmentResourceInfo info = new AWSEC2NetworkInterfaceAttachmentResourceInfo();

  public AWSEC2NetworkInterfaceAttachmentResourceAction() {
    for (CreateSteps createStep: CreateSteps.values()) {
      createSteps.put(createStep.name(), createStep);
    }
    for (DeleteSteps deleteStep: DeleteSteps.values()) {
      deleteSteps.put(deleteStep.name(), deleteStep);
    }

  }
  private enum CreateSteps implements Step {
    CREATE_NETWORK_INTERFACE_ATTACHMENT {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        AWSEC2NetworkInterfaceAttachmentResourceAction action = (AWSEC2NetworkInterfaceAttachmentResourceAction) resourceAction;
        ServiceConfiguration configuration = Topology.lookup(Compute.class);
        AttachNetworkInterfaceType attachNetworkInterfaceType = MessageHelper.createMessage(AttachNetworkInterfaceType.class, action.info.getEffectiveUserId());
        if (action.properties.getInstanceId() != null) {
          DescribeInstancesType describeInstancesType = MessageHelper.createMessage(DescribeInstancesType.class, action.info.getEffectiveUserId());
          describeInstancesType.setInstancesSet(Lists.newArrayList(action.properties.getInstanceId()));
          DescribeInstancesResponseType describeInstancesResponseType = AsyncRequests.<DescribeInstancesType,DescribeInstancesResponseType> sendSync(configuration, describeInstancesType);
          if (describeInstancesResponseType.getReservationSet() == null || describeInstancesResponseType.getReservationSet().isEmpty()) {
            throw new ValidationErrorException("No such instance " + action.properties.getInstanceId());
          }
          attachNetworkInterfaceType.setInstanceId(action.properties.getInstanceId());
        }
        if (action.properties.getNetworkInterfaceId() != null) {
          DescribeNetworkInterfacesType describeNetworkInterfacesType = MessageHelper.createMessage(DescribeNetworkInterfacesType.class, action.info.getEffectiveUserId());
          describeNetworkInterfacesType.setNetworkInterfaceIdSet(action.convertNetworkInterfaceIdSet(action.properties.getNetworkInterfaceId()));
          DescribeNetworkInterfacesResponseType describeNetworkInterfacesResponseType = AsyncRequests.<DescribeNetworkInterfacesType, DescribeNetworkInterfacesResponseType>sendSync(configuration, describeNetworkInterfacesType);
          if (describeNetworkInterfacesResponseType.getNetworkInterfaceSet() == null || describeNetworkInterfacesResponseType.getNetworkInterfaceSet().getItem() == null ||
            describeNetworkInterfacesResponseType.getNetworkInterfaceSet().getItem().isEmpty()) {
            throw new ValidationErrorException("No such network interface " + action.properties.getNetworkInterfaceId());
          }
          attachNetworkInterfaceType.setNetworkInterfaceId(action.properties.getNetworkInterfaceId());
        }
        attachNetworkInterfaceType.setDeviceIndex(action.properties.getDeviceIndex());

        // TODO: figure out to do with delete on terminate...
        AttachNetworkInterfaceResponseType attachNetworkInterfaceResponseType = AsyncRequests.<AttachNetworkInterfaceType, AttachNetworkInterfaceResponseType> sendSync(configuration, attachNetworkInterfaceType);
        action.info.setPhysicalResourceId(attachNetworkInterfaceResponseType.getAttachmentId());
        action.info.setReferenceValueJson(JsonHelper.getStringFromJsonNode(new TextNode(action.info.getPhysicalResourceId())));
        // TODO: wait until attached

        return action;
      }

      @Override
      public RetryPolicy getRetryPolicy() {
        return null;
      }
    }
  }

  private enum DeleteSteps implements Step {
    DELETE_NETWORK_INTERFACE_ATTACHMENT {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        AWSEC2NetworkInterfaceAttachmentResourceAction action = (AWSEC2NetworkInterfaceAttachmentResourceAction) resourceAction;
        ServiceConfiguration configuration = Topology.lookup(Compute.class);
        if (action.info.getPhysicalResourceId() == null) return action;

        // check if network interface still exists (return otherwise)
        DescribeNetworkInterfacesType describeNetworkInterfacesType = MessageHelper.createMessage(DescribeNetworkInterfacesType.class, action.info.getEffectiveUserId());
        describeNetworkInterfacesType.setNetworkInterfaceIdSet(action.convertNetworkInterfaceIdSet(action.properties.getNetworkInterfaceId()));
        DescribeNetworkInterfacesResponseType describeNetworkInterfacesResponseType = AsyncRequests.<DescribeNetworkInterfacesType, DescribeNetworkInterfacesResponseType>sendSync(configuration, describeNetworkInterfacesType);
        if (describeNetworkInterfacesResponseType.getNetworkInterfaceSet() == null || describeNetworkInterfacesResponseType.getNetworkInterfaceSet().getItem() == null ||
          describeNetworkInterfacesResponseType.getNetworkInterfaceSet().getItem().isEmpty()) {
          return action;
        }
        // TODO: looks like this object has state, deal with attaching, etc.
        // see if my attachment still exists
        boolean foundAttachment = false;
        for (NetworkInterfaceType networkInterfaceType : describeNetworkInterfacesResponseType.getNetworkInterfaceSet().getItem()) {
          if (networkInterfaceType.getAttachment() != null && networkInterfaceType.getAttachment().getAttachmentId() != null
            && networkInterfaceType.getAttachment().getAttachmentId().equals(action.info.getPhysicalResourceId())) {
            foundAttachment = true;
            break;
          }
        }
        if (!foundAttachment) return action;
        DetachNetworkInterfaceType detachNetworkInterfaceType = MessageHelper.createMessage(DetachNetworkInterfaceType.class, action.info.getEffectiveUserId());
        detachNetworkInterfaceType.setAttachmentId(action.info.getPhysicalResourceId());
        AsyncRequests.<DetachNetworkInterfaceType, DetachNetworkInterfaceResponseType> sendSync(configuration, detachNetworkInterfaceType);
        // TODO: wait until detached
        return action;
      }

      @Override
      public RetryPolicy getRetryPolicy() {
        return null;
      }
    }
  }


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
  public Promise<String> getCreatePromise(WorkflowOperations<StackActivity> workflowOperations, String resourceId, String stackId, String accountId, String effectiveUserId) {
    List<String> stepIds = Lists.transform(Lists.newArrayList(CreateSteps.values()), StepTransform.INSTANCE);
    return new MultiStepWithRetryCreatePromise(workflowOperations, stepIds, this).getCreatePromise(resourceId, stackId, accountId, effectiveUserId);
  }

  @Override
  public Promise<String> getDeletePromise(WorkflowOperations<StackActivity> workflowOperations, String resourceId, String stackId, String accountId, String effectiveUserId) {
    List<String> stepIds = Lists.transform(Lists.newArrayList(DeleteSteps.values()), StepTransform.INSTANCE);
    return new MultiStepWithRetryDeletePromise(workflowOperations, stepIds, this).getDeletePromise(resourceId, stackId, accountId, effectiveUserId);
  }

}


