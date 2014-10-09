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
import com.eucalyptus.cloudformation.resources.standard.info.AWSEC2SubnetRouteTableAssociationResourceInfo;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.AWSEC2SubnetRouteTableAssociationProperties;
import com.eucalyptus.cloudformation.template.JsonHelper;
import com.eucalyptus.cloudformation.util.MessageHelper;
import com.eucalyptus.cloudformation.workflow.StackActivity;
import com.eucalyptus.cloudformation.workflow.steps.MultiStepWithRetryCreatePromise;
import com.eucalyptus.cloudformation.workflow.steps.MultiStepWithRetryDeletePromise;
import com.eucalyptus.cloudformation.workflow.steps.Step;
import com.eucalyptus.cloudformation.workflow.steps.StepTransform;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.Topology;
import com.eucalyptus.compute.common.AssociateRouteTableResponseType;
import com.eucalyptus.compute.common.AssociateRouteTableType;
import com.eucalyptus.compute.common.Compute;
import com.eucalyptus.compute.common.DescribeRouteTablesResponseType;
import com.eucalyptus.compute.common.DescribeRouteTablesType;
import com.eucalyptus.compute.common.DescribeSubnetsResponseType;
import com.eucalyptus.compute.common.DescribeSubnetsType;
import com.eucalyptus.compute.common.DisassociateRouteTableResponseType;
import com.eucalyptus.compute.common.DisassociateRouteTableType;
import com.eucalyptus.compute.common.Filter;
import com.eucalyptus.compute.common.RouteTableIdSetItemType;
import com.eucalyptus.compute.common.RouteTableIdSetType;
import com.eucalyptus.compute.common.SubnetIdSetItemType;
import com.eucalyptus.compute.common.SubnetIdSetType;
import com.eucalyptus.util.async.AsyncRequests;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.collect.Lists;
import com.netflix.glisten.WorkflowOperations;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ethomas on 2/3/14.
 */
public class AWSEC2SubnetRouteTableAssociationResourceAction extends ResourceAction {

  private AWSEC2SubnetRouteTableAssociationProperties properties = new AWSEC2SubnetRouteTableAssociationProperties();
  private AWSEC2SubnetRouteTableAssociationResourceInfo info = new AWSEC2SubnetRouteTableAssociationResourceInfo();

  public AWSEC2SubnetRouteTableAssociationResourceAction() {
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
        AWSEC2SubnetRouteTableAssociationResourceAction action = (AWSEC2SubnetRouteTableAssociationResourceAction) resourceAction;
        ServiceConfiguration configuration = Topology.lookup(Compute.class);
        // See if route table is there
        action.checkRouteTableExists(configuration);
        // See if subnet is there
        action.checkSubnetExists(configuration);
        String associationId = action.associateRouteTable(configuration, action.properties.getSubnetId(), action.properties.getRouteTableId());
        action.info.setPhysicalResourceId(associationId);
        action.info.setReferenceValueJson(JsonHelper.getStringFromJsonNode(new TextNode(action.info.getPhysicalResourceId())));
        return action;
      }

      @Override
      public RetryPolicy getRetryPolicy() {
        return null;
      }
    }
  }

  private enum DeleteSteps implements Step {
    DELETE_ASSOCIATION {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        AWSEC2SubnetRouteTableAssociationResourceAction action = (AWSEC2SubnetRouteTableAssociationResourceAction) resourceAction;
        ServiceConfiguration configuration = Topology.lookup(Compute.class);
        if (action.info.getPhysicalResourceId() == null) return action;

        if (!action.associationIdExistsForDelete(configuration)) return action;
        if (!action.routeTableExistsForDelete(configuration)) return action;
        if (!action.subnetExistsForDelete(configuration)) return action;
        action.disassociateRouteTable(configuration, action.info.getPhysicalResourceId());
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
    properties = (AWSEC2SubnetRouteTableAssociationProperties) resourceProperties;
  }

  @Override
  public ResourceInfo getResourceInfo() {
    return info;
  }

  @Override
  public void setResourceInfo(ResourceInfo resourceInfo) {
    info = (AWSEC2SubnetRouteTableAssociationResourceInfo) resourceInfo;
  }

  private String associateRouteTable(ServiceConfiguration configuration, String subnetId, String routeTableId) throws Exception {
    AssociateRouteTableType associateRouteTableType = MessageHelper.createMessage(AssociateRouteTableType.class, info.getEffectiveUserId());
    associateRouteTableType.setRouteTableId(routeTableId);
    associateRouteTableType.setSubnetId(subnetId);
    AssociateRouteTableResponseType associateRouteTableResponseType = AsyncRequests.<AssociateRouteTableType, AssociateRouteTableResponseType> sendSync(configuration, associateRouteTableType);
    return associateRouteTableResponseType.getAssociationId();
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

  private void checkRouteTableExists(ServiceConfiguration configuration) throws Exception {
    DescribeRouteTablesType describeRouteTablesType = MessageHelper.createMessage(DescribeRouteTablesType.class, info.getEffectiveUserId());
    RouteTableIdSetType routeTableIdSet = new RouteTableIdSetType();
    RouteTableIdSetItemType routeTableIdSetItem = new RouteTableIdSetItemType();
    routeTableIdSetItem.setRouteTableId(properties.getRouteTableId());
    routeTableIdSet.setItem(Lists.newArrayList(routeTableIdSetItem));
    describeRouteTablesType.setRouteTableIdSet(routeTableIdSet);
    DescribeRouteTablesResponseType describeRouteTablesResponseType = AsyncRequests.<DescribeRouteTablesType, DescribeRouteTablesResponseType> sendSync(configuration, describeRouteTablesType);
    if (describeRouteTablesResponseType.getRouteTableSet() == null || describeRouteTablesResponseType.getRouteTableSet().getItem() == null ||
      describeRouteTablesResponseType.getRouteTableSet().getItem().isEmpty()) {
      throw new ValidationErrorException("No such route table with id '" + properties.getRouteTableId());
    }
  }

  private void disassociateRouteTable(ServiceConfiguration configuration, String associationId) throws Exception {
    DisassociateRouteTableType disassociateRouteTableType = MessageHelper.createMessage(DisassociateRouteTableType.class, info.getEffectiveUserId());
    disassociateRouteTableType.setAssociationId(associationId);
    AsyncRequests.<DisassociateRouteTableType, DisassociateRouteTableResponseType> sendSync(configuration, disassociateRouteTableType);
  }

  private boolean subnetExistsForDelete(ServiceConfiguration configuration) throws Exception {
    DescribeSubnetsType describeSubnetsType = MessageHelper.createMessage(DescribeSubnetsType.class, info.getEffectiveUserId());
    SubnetIdSetType SubnetIdSet = new SubnetIdSetType();
    SubnetIdSetItemType SubnetIdSetItem = new SubnetIdSetItemType();
    SubnetIdSetItem.setSubnetId(properties.getSubnetId());
    SubnetIdSet.setItem(Lists.newArrayList(SubnetIdSetItem));
    describeSubnetsType.setSubnetSet(SubnetIdSet);
    DescribeSubnetsResponseType describeSubnetsResponseType = AsyncRequests.<DescribeSubnetsType, DescribeSubnetsResponseType> sendSync(configuration, describeSubnetsType);
    if (describeSubnetsResponseType.getSubnetSet() == null || describeSubnetsResponseType.getSubnetSet().getItem() == null ||
      describeSubnetsResponseType.getSubnetSet().getItem().isEmpty()) {
      return false;
    }
    return true;
  }

  private boolean routeTableExistsForDelete(ServiceConfiguration configuration) throws Exception {
    DescribeRouteTablesType describeRouteTablesType = MessageHelper.createMessage(DescribeRouteTablesType.class, info.getEffectiveUserId());
    RouteTableIdSetType routeTableIdSet = new RouteTableIdSetType();
    RouteTableIdSetItemType routeTableIdSetItem = new RouteTableIdSetItemType();
    routeTableIdSetItem.setRouteTableId(properties.getRouteTableId());
    routeTableIdSet.setItem(Lists.newArrayList(routeTableIdSetItem));
    describeRouteTablesType.setRouteTableIdSet(routeTableIdSet);
    DescribeRouteTablesResponseType describeRouteTablesResponseType = AsyncRequests.<DescribeRouteTablesType, DescribeRouteTablesResponseType> sendSync(configuration, describeRouteTablesType);
    if (describeRouteTablesResponseType.getRouteTableSet() == null || describeRouteTablesResponseType.getRouteTableSet().getItem() == null ||
      describeRouteTablesResponseType.getRouteTableSet().getItem().isEmpty()) {
      return false;
    }
    return true;
  }

  private boolean associationIdExistsForDelete(ServiceConfiguration configuration) throws Exception {
    DescribeRouteTablesType describeRouteTablesType = MessageHelper.createMessage(DescribeRouteTablesType.class, info.getEffectiveUserId());
    ArrayList<Filter> filterSet = Lists.newArrayList();;
    Filter filter = new Filter();
    filter.setName("association.route-table-association-id");
    filter.setValueSet(Lists.<String>newArrayList(info.getPhysicalResourceId()));
    filterSet.add(filter);
    describeRouteTablesType.setFilterSet(filterSet);
    DescribeRouteTablesResponseType describeRouteTablesResponseType = AsyncRequests.<DescribeRouteTablesType, DescribeRouteTablesResponseType> sendSync(configuration, describeRouteTablesType);
    if (describeRouteTablesResponseType.getRouteTableSet() == null || describeRouteTablesResponseType.getRouteTableSet().getItem() == null ||
      describeRouteTablesResponseType.getRouteTableSet().getItem().isEmpty()) {
      return false;
    }
    return true;
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


