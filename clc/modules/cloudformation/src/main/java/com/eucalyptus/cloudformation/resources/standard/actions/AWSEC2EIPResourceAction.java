/*************************************************************************
 * Copyright 2009-2015 Eucalyptus Systems, Inc.
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
import com.eucalyptus.cloudformation.resources.EC2Helper;
import com.eucalyptus.cloudformation.resources.ResourceAction;
import com.eucalyptus.cloudformation.resources.ResourceInfo;
import com.eucalyptus.cloudformation.resources.ResourceProperties;
import com.eucalyptus.cloudformation.resources.standard.info.AWSEC2EIPResourceInfo;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.AWSEC2EIPProperties;
import com.eucalyptus.cloudformation.template.JsonHelper;
import com.eucalyptus.cloudformation.util.MessageHelper;
import com.eucalyptus.cloudformation.workflow.StackActivityClient;
import com.eucalyptus.cloudformation.workflow.steps.CreateMultiStepPromise;
import com.eucalyptus.cloudformation.workflow.steps.DeleteMultiStepPromise;
import com.eucalyptus.cloudformation.workflow.steps.Step;
import com.eucalyptus.cloudformation.workflow.steps.StepTransform;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.Topology;
import com.eucalyptus.compute.common.AllocateAddressResponseType;
import com.eucalyptus.compute.common.AllocateAddressType;
import com.eucalyptus.compute.common.AssociateAddressResponseType;
import com.eucalyptus.compute.common.AssociateAddressType;
import com.eucalyptus.compute.common.Compute;
import com.eucalyptus.compute.common.DescribeAddressesResponseType;
import com.eucalyptus.compute.common.DescribeAddressesType;
import com.eucalyptus.compute.common.DescribeInstancesResponseType;
import com.eucalyptus.compute.common.DescribeInstancesType;
import com.eucalyptus.compute.common.Filter;
import com.eucalyptus.compute.common.ReleaseAddressResponseType;
import com.eucalyptus.compute.common.ReleaseAddressType;
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
public class AWSEC2EIPResourceAction extends ResourceAction {
  private static final Logger LOG = Logger.getLogger(AWSEC2EIPResourceAction.class);
  private AWSEC2EIPProperties properties = new AWSEC2EIPProperties();
  private AWSEC2EIPResourceInfo info = new AWSEC2EIPResourceInfo();

  public AWSEC2EIPResourceAction() {
    for (CreateSteps createStep: CreateSteps.values()) {
      createSteps.put(createStep.name(), createStep);
    }
    for (DeleteSteps deleteStep: DeleteSteps.values()) {
      deleteSteps.put(deleteStep.name(), deleteStep);
    }

  }
  private enum CreateSteps implements Step {
    CREATE_EIP__ADDRESS {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        AWSEC2EIPResourceAction action = (AWSEC2EIPResourceAction) resourceAction;
        ServiceConfiguration configuration = Topology.lookup(Compute.class);
        AllocateAddressType allocateAddressType = MessageHelper.createMessage(AllocateAddressType.class, action.info.getEffectiveUserId());
        if (action.properties.getDomain() != null && !"vpc".equals(action.properties.getDomain())) {
          throw new ValidationErrorException("vpc is the only supported value for Domain");
        }
        if (action.properties.getDomain() != null) {
          allocateAddressType.setDomain(action.properties.getDomain());
        }
        AllocateAddressResponseType allocateAddressResponseType = AsyncRequests.<AllocateAddressType, AllocateAddressResponseType> sendSync(configuration, allocateAddressType);
        String publicIp = allocateAddressResponseType.getPublicIp();
        action.info.setPhysicalResourceId(publicIp);
        action.info.setReferenceValueJson(JsonHelper.getStringFromJsonNode(new TextNode(action.info.getPhysicalResourceId())));
        if (action.properties.getDomain() != null) {
          action.info.setAllocationId(JsonHelper.getStringFromJsonNode(new TextNode(allocateAddressResponseType.getAllocationId())));
        }
        return action;
      }
    },
    ATTACH_TO_INSTANCE {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        AWSEC2EIPResourceAction action = (AWSEC2EIPResourceAction) resourceAction;
        ServiceConfiguration configuration = Topology.lookup(Compute.class);
        if (action.properties.getInstanceId() != null) {
          DescribeInstancesType describeInstancesType = MessageHelper.createMessage(DescribeInstancesType.class, action.info.getEffectiveUserId());
          describeInstancesType.getFilterSet( ).add( Filter.filter( "instance-id", action.properties.getInstanceId( ) ) );
          DescribeInstancesResponseType describeInstancesResponseType = AsyncRequests.sendSync( configuration, describeInstancesType );
          if (describeInstancesResponseType.getReservationSet() == null || describeInstancesResponseType.getReservationSet().isEmpty()) {
            throw new ValidationErrorException("No such instance " + action.properties.getInstanceId());
          }
          AssociateAddressType associateAddressType = MessageHelper.createMessage(AssociateAddressType.class, action.info.getEffectiveUserId());
          if (action.properties.getDomain() != null) {
            associateAddressType.setAllocationId(JsonHelper.getJsonNodeFromString(action.info.getAllocationId()).asText());
          } else {
            associateAddressType.setPublicIp(action.info.getPhysicalResourceId());
          }
          associateAddressType.setInstanceId(action.properties.getInstanceId());
          AsyncRequests.<AssociateAddressType, AssociateAddressResponseType> sendSync(configuration, associateAddressType);

          // Update the instance info
          if (action.properties.getInstanceId() != null) {
            EC2Helper.refreshInstanceAttributes(action.getStackEntity(), action.properties.getInstanceId(), action.info.getEffectiveUserId());
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
    DELETE_EIP_ADDRESS {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        AWSEC2EIPResourceAction action = (AWSEC2EIPResourceAction) resourceAction;
        ServiceConfiguration configuration = Topology.lookup(Compute.class);
        if (action.info.getPhysicalResourceId() == null) return action;
        DescribeAddressesType describeAddressesType = MessageHelper.createMessage(DescribeAddressesType.class, action.info.getEffectiveUserId());
        if (action.properties.getDomain() != null) {
          describeAddressesType.setAllocationIds(Lists.newArrayList(JsonHelper.getJsonNodeFromString(action.info.getAllocationId()).asText()));
        } else {
          describeAddressesType.setPublicIpsSet(Lists.newArrayList(action.info.getPhysicalResourceId()));
        }
        DescribeAddressesResponseType describeAddressesResponseType = AsyncRequests.<DescribeAddressesType, DescribeAddressesResponseType> sendSync(configuration, describeAddressesType);
        if (describeAddressesResponseType.getAddressesSet() != null && !describeAddressesResponseType.getAddressesSet().isEmpty()) {
          ReleaseAddressType releaseAddressType = MessageHelper.createMessage(ReleaseAddressType.class, action.info.getEffectiveUserId());
          if (action.properties.getDomain() != null) {
            releaseAddressType.setAllocationId(JsonHelper.getJsonNodeFromString(action.info.getAllocationId()).asText());
          } else {
            releaseAddressType.setPublicIp(action.info.getPhysicalResourceId());
          }
          AsyncRequests.<ReleaseAddressType, ReleaseAddressResponseType> sendSync(configuration, releaseAddressType);
        }

        // Update the instance info
        if (action.properties.getInstanceId() != null) {
          EC2Helper.refreshInstanceAttributes(action.getStackEntity(), action.properties.getInstanceId(), action.info.getEffectiveUserId());
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
    properties = (AWSEC2EIPProperties) resourceProperties;
  }

  @Override
  public ResourceInfo getResourceInfo() {
    return info;
  }

  @Override
  public void setResourceInfo(ResourceInfo resourceInfo) {
    info = (AWSEC2EIPResourceInfo) resourceInfo;
  }

  @Override
  public Promise<String> getCreatePromise(WorkflowOperations<StackActivityClient> workflowOperations, String resourceId, String stackId, String accountId, String effectiveUserId) {
    List<String> stepIds = Lists.transform(Lists.newArrayList(CreateSteps.values()), StepTransform.INSTANCE);
    return new CreateMultiStepPromise(workflowOperations, stepIds, this).getCreatePromise(resourceId, stackId, accountId, effectiveUserId);
  }

  @Override
  public Promise<String> getDeletePromise(WorkflowOperations<StackActivityClient> workflowOperations, String resourceId, String stackId, String accountId, String effectiveUserId) {
    List<String> stepIds = Lists.transform(Lists.newArrayList(DeleteSteps.values()), StepTransform.INSTANCE);
    return new DeleteMultiStepPromise(workflowOperations, stepIds, this).getDeletePromise(resourceId, stackId, accountId, effectiveUserId);
  }
}


