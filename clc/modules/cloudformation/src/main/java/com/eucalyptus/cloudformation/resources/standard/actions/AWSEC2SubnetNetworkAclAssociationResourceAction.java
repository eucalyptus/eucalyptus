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
import com.eucalyptus.cloudformation.resources.standard.info.AWSEC2SubnetNetworkAclAssociationResourceInfo;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.AWSEC2SubnetNetworkAclAssociationProperties;
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
import com.eucalyptus.compute.common.DescribeNetworkAclsResponseType;
import com.eucalyptus.compute.common.DescribeNetworkAclsType;
import com.eucalyptus.compute.common.DescribeSubnetsResponseType;
import com.eucalyptus.compute.common.DescribeSubnetsType;
import com.eucalyptus.compute.common.Filter;
import com.eucalyptus.compute.common.NetworkAclAssociationType;
import com.eucalyptus.compute.common.NetworkAclIdSetItemType;
import com.eucalyptus.compute.common.NetworkAclIdSetType;
import com.eucalyptus.compute.common.NetworkAclType;
import com.eucalyptus.compute.common.ReplaceNetworkAclAssociationResponseType;
import com.eucalyptus.compute.common.ReplaceNetworkAclAssociationType;
import com.eucalyptus.compute.common.SubnetIdSetItemType;
import com.eucalyptus.compute.common.SubnetIdSetType;
import com.eucalyptus.util.async.AsyncRequests;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.collect.Lists;
import com.netflix.glisten.WorkflowOperations;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;

/**
 * Created by ethomas on 2/3/14.
 */
public class AWSEC2SubnetNetworkAclAssociationResourceAction extends ResourceAction {

  private AWSEC2SubnetNetworkAclAssociationProperties properties = new AWSEC2SubnetNetworkAclAssociationProperties();
  private AWSEC2SubnetNetworkAclAssociationResourceInfo info = new AWSEC2SubnetNetworkAclAssociationResourceInfo();

  public AWSEC2SubnetNetworkAclAssociationResourceAction() {
    for (CreateSteps createStep: CreateSteps.values()) {
      createSteps.put(createStep.name(), createStep);
    }
    for (DeleteSteps deleteStep: DeleteSteps.values()) {
      deleteSteps.put(deleteStep.name(), deleteStep);
    }

  }
  private enum CreateSteps implements Step {
    CREATE_ASSOCIATION {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        AWSEC2SubnetNetworkAclAssociationResourceAction action = (AWSEC2SubnetNetworkAclAssociationResourceAction) resourceAction;
        ServiceConfiguration configuration = Topology.lookup(Compute.class);
        // See if network acl is there
        action.checkNetworkAclExists(configuration);
        // See if subnet is there
        action.checkSubnetExists(configuration);
        // Find the current network acl so we can find the association id
        String associationId = action.getAssociationId(configuration);
        if (associationId == null) {
          throw new ValidationErrorException("Unable to find current network acl association id for subnet with id " + action.properties.getSubnetId());
        }
        String newAssociationId = action.replaceAssociation(configuration, associationId, action.properties.getNetworkAclId());
        action.info.setPhysicalResourceId(newAssociationId);
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
    DELETE_ASSOCIATION {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        AWSEC2SubnetNetworkAclAssociationResourceAction action = (AWSEC2SubnetNetworkAclAssociationResourceAction) resourceAction;
        ServiceConfiguration configuration = Topology.lookup(Compute.class);
        if (action.info.getPhysicalResourceId() == null) return action;

        // First see if association id is there...
        if (!action.associationIdExistsForDelete(configuration)) return action;
        if (!action.networkAclExistsForDelete(configuration)) return action;
        String vpcId = action.checkSubnetIdAndGetVpcIdForDelete(configuration);
        if (vpcId == null) return action;
        String defaultNetworkAclId = action.findDefaultNetworkAclId(configuration, vpcId);
        if (defaultNetworkAclId == null) {
          throw new ValidationErrorException("Unable to find the default network acl id for vpc " + vpcId);
        }
        action.replaceAssociation(configuration, action.info.getPhysicalResourceId(), defaultNetworkAclId);
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
    properties = (AWSEC2SubnetNetworkAclAssociationProperties) resourceProperties;
  }

  @Override
  public ResourceInfo getResourceInfo() {
    return info;
  }

  @Override
  public void setResourceInfo(ResourceInfo resourceInfo) {
    info = (AWSEC2SubnetNetworkAclAssociationResourceInfo) resourceInfo;
  }

  private String replaceAssociation(ServiceConfiguration configuration, String associationId, String networkAclId) throws Exception {
    ReplaceNetworkAclAssociationType replaceNetworkAclAssociationType = MessageHelper.createMessage(ReplaceNetworkAclAssociationType.class, info.getEffectiveUserId());
    replaceNetworkAclAssociationType.setAssociationId(associationId);
    replaceNetworkAclAssociationType.setNetworkAclId(networkAclId);
    ReplaceNetworkAclAssociationResponseType replaceNetworkAclAssociationResponseType = AsyncRequests.<ReplaceNetworkAclAssociationType, ReplaceNetworkAclAssociationResponseType> sendSync(configuration, replaceNetworkAclAssociationType);
    return replaceNetworkAclAssociationResponseType.getNewAssociationId();
  }

  private void checkSubnetExists(ServiceConfiguration configuration) throws Exception {
    DescribeSubnetsType describeSubnetsType = MessageHelper.createMessage(DescribeSubnetsType.class, info.getEffectiveUserId());
    SubnetIdSetType SubnetIdSet = new SubnetIdSetType();
    SubnetIdSetItemType SubnetIdSetItem = new SubnetIdSetItemType();
    SubnetIdSetItem.setSubnetId(properties.getSubnetId());
    SubnetIdSet.setItem(Lists.newArrayList(SubnetIdSetItem));
    describeSubnetsType.setSubnetSet(SubnetIdSet);
    DescribeSubnetsResponseType describeSubnetsResponseType = AsyncRequests.<DescribeSubnetsType, DescribeSubnetsResponseType> sendSync(configuration, describeSubnetsType);
    if (describeSubnetsResponseType.getSubnetSet() == null || describeSubnetsResponseType.getSubnetSet().getItem() == null ||
      describeSubnetsResponseType.getSubnetSet().getItem().isEmpty()) {
      throw new ValidationErrorException("No such subnet with id '" + properties.getSubnetId());
    }
  }

  private void checkNetworkAclExists(ServiceConfiguration configuration) throws Exception {
    DescribeNetworkAclsType describeNetworkAclsType = MessageHelper.createMessage(DescribeNetworkAclsType.class, info.getEffectiveUserId());
    NetworkAclIdSetType networkAclIdSet = new NetworkAclIdSetType();
    NetworkAclIdSetItemType networkAclIdSetItem = new NetworkAclIdSetItemType();
    networkAclIdSetItem.setNetworkAclId(properties.getNetworkAclId());
    networkAclIdSet.setItem(Lists.newArrayList(networkAclIdSetItem));
    describeNetworkAclsType.setNetworkAclIdSet(networkAclIdSet);
    DescribeNetworkAclsResponseType describeNetworkAclsResponseType = AsyncRequests.<DescribeNetworkAclsType, DescribeNetworkAclsResponseType> sendSync(configuration, describeNetworkAclsType);
    if (describeNetworkAclsResponseType.getNetworkAclSet() == null || describeNetworkAclsResponseType.getNetworkAclSet().getItem() == null ||
      describeNetworkAclsResponseType.getNetworkAclSet().getItem().isEmpty()) {
      throw new ValidationErrorException("No such network acl with id '" + properties.getNetworkAclId());
    }
  }

  private String getAssociationId(ServiceConfiguration configuration) throws Exception {
    String associationId = null;
    DescribeNetworkAclsType describeNetworkAclsType = MessageHelper.createMessage(DescribeNetworkAclsType.class, info.getEffectiveUserId());
    ArrayList<Filter> filterSet = Lists.newArrayList();;
    Filter filter = new Filter();
    filter.setName("association.subnet-id");
    filter.setValueSet(Lists.<String>newArrayList(properties.getSubnetId()));
    filterSet.add(filter);
    describeNetworkAclsType.setFilterSet(filterSet);
    DescribeNetworkAclsResponseType describeNetworkAclsResponseType = AsyncRequests.<DescribeNetworkAclsType, DescribeNetworkAclsResponseType> sendSync(configuration, describeNetworkAclsType);
    if (describeNetworkAclsResponseType.getNetworkAclSet() == null || describeNetworkAclsResponseType.getNetworkAclSet().getItem() == null ||
      describeNetworkAclsResponseType.getNetworkAclSet().getItem().isEmpty()) {
      throw new ValidationErrorException("Can not find existing network association for subnet " + properties.getSubnetId());
    }
    if (describeNetworkAclsResponseType.getNetworkAclSet().getItem().size() > 1) {
      throw new ValidationErrorException("More than one existing network association exists for subnet " + properties.getSubnetId());
    }
    if (describeNetworkAclsResponseType.getNetworkAclSet().getItem().get(0).getAssociationSet() != null &&
      describeNetworkAclsResponseType.getNetworkAclSet().getItem().get(0).getAssociationSet().getItem() != null) {
      for (NetworkAclAssociationType networkAclAssociationType: describeNetworkAclsResponseType.getNetworkAclSet().getItem().get(0).getAssociationSet().getItem()) {
        if (properties.getSubnetId().equals(networkAclAssociationType.getSubnetId())) {
          associationId = networkAclAssociationType.getNetworkAclAssociationId();
          break;
        }
      }
    }
    return associationId;
  }

  private String findDefaultNetworkAclId(ServiceConfiguration configuration, String vpcId) throws Exception{
    DescribeNetworkAclsType describeNetworkAclsType = MessageHelper.createMessage(DescribeNetworkAclsType.class, info.getEffectiveUserId());
    ArrayList<Filter> filterSet = Lists.newArrayList();;
    Filter filter = new Filter();
    filter.setName("vpc-id");
    filter.setValueSet(Lists.<String>newArrayList(vpcId));
    filterSet.add(filter);
    describeNetworkAclsType.setFilterSet(filterSet);
    DescribeNetworkAclsResponseType describeNetworkAclsResponseType = AsyncRequests.<DescribeNetworkAclsType, DescribeNetworkAclsResponseType> sendSync(configuration, describeNetworkAclsType);
    if (describeNetworkAclsResponseType.getNetworkAclSet() == null || describeNetworkAclsResponseType.getNetworkAclSet().getItem() == null ||
      describeNetworkAclsResponseType.getNetworkAclSet().getItem().isEmpty()) {
      return null;
    }
    for (NetworkAclType networkAclType: describeNetworkAclsResponseType.getNetworkAclSet().getItem()) {
      if (vpcId.equals(networkAclType.getVpcId()) && networkAclType.get_default()) {
        return networkAclType.getNetworkAclId();
      }
    }
    return null;
  }

  private String checkSubnetIdAndGetVpcIdForDelete(ServiceConfiguration configuration) throws Exception {
    DescribeSubnetsType describeSubnetsType = MessageHelper.createMessage(DescribeSubnetsType.class, info.getEffectiveUserId());
    SubnetIdSetType SubnetIdSet = new SubnetIdSetType();
    SubnetIdSetItemType SubnetIdSetItem = new SubnetIdSetItemType();
    SubnetIdSetItem.setSubnetId(properties.getSubnetId());
    SubnetIdSet.setItem(Lists.newArrayList(SubnetIdSetItem));
    describeSubnetsType.setSubnetSet(SubnetIdSet);
    DescribeSubnetsResponseType describeSubnetsResponseType = AsyncRequests.<DescribeSubnetsType, DescribeSubnetsResponseType> sendSync(configuration, describeSubnetsType);
    if (describeSubnetsResponseType.getSubnetSet() == null || describeSubnetsResponseType.getSubnetSet().getItem() == null ||
      describeSubnetsResponseType.getSubnetSet().getItem().isEmpty()) {
      return null;
    }
    return describeSubnetsResponseType.getSubnetSet().getItem().get(0).getVpcId();
  }

  private boolean networkAclExistsForDelete(ServiceConfiguration configuration) throws Exception {
    DescribeNetworkAclsType describeNetworkAclsType = MessageHelper.createMessage(DescribeNetworkAclsType.class, info.getEffectiveUserId());
    NetworkAclIdSetType networkAclIdSet = new NetworkAclIdSetType();
    NetworkAclIdSetItemType networkAclIdSetItem = new NetworkAclIdSetItemType();
    networkAclIdSetItem.setNetworkAclId(properties.getNetworkAclId());
    networkAclIdSet.setItem(Lists.newArrayList(networkAclIdSetItem));
    describeNetworkAclsType.setNetworkAclIdSet(networkAclIdSet);
    DescribeNetworkAclsResponseType describeNetworkAclsResponseType = AsyncRequests.<DescribeNetworkAclsType, DescribeNetworkAclsResponseType> sendSync(configuration, describeNetworkAclsType);
    if (describeNetworkAclsResponseType.getNetworkAclSet() == null || describeNetworkAclsResponseType.getNetworkAclSet().getItem() == null ||
      describeNetworkAclsResponseType.getNetworkAclSet().getItem().isEmpty()) {
      return false;
    }
    return true;
  }

  private boolean associationIdExistsForDelete(ServiceConfiguration configuration) throws Exception {
    DescribeNetworkAclsType describeNetworkAclsType = MessageHelper.createMessage(DescribeNetworkAclsType.class, info.getEffectiveUserId());
    ArrayList<Filter> filterSet = Lists.newArrayList();;
    Filter filter = new Filter();
    filter.setName("association.association-id");
    filter.setValueSet(Lists.<String>newArrayList(info.getPhysicalResourceId()));
    filterSet.add(filter);
    describeNetworkAclsType.setFilterSet(filterSet);
    DescribeNetworkAclsResponseType describeNetworkAclsResponseType = AsyncRequests.<DescribeNetworkAclsType, DescribeNetworkAclsResponseType> sendSync(configuration, describeNetworkAclsType);
    if (describeNetworkAclsResponseType.getNetworkAclSet() == null || describeNetworkAclsResponseType.getNetworkAclSet().getItem() == null ||
      describeNetworkAclsResponseType.getNetworkAclSet().getItem().isEmpty()) {
      return false;
    }
    return true;
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


