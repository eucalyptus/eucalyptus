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
import com.eucalyptus.cloudformation.resources.ResourceAction;
import com.eucalyptus.cloudformation.resources.ResourceInfo;
import com.eucalyptus.cloudformation.resources.ResourceProperties;
import com.eucalyptus.cloudformation.resources.standard.info.AWSEC2SubnetRouteTableAssociationResourceInfo;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.AWSEC2SubnetRouteTableAssociationProperties;
import com.eucalyptus.cloudformation.template.JsonHelper;
import com.eucalyptus.cloudformation.util.MessageHelper;
import com.eucalyptus.cloudformation.workflow.steps.Step;
import com.eucalyptus.cloudformation.workflow.steps.StepBasedResourceAction;
import com.eucalyptus.cloudformation.workflow.steps.UpdateStep;
import com.eucalyptus.cloudformation.workflow.updateinfo.UpdateType;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.Topology;
import com.eucalyptus.compute.common.AssociateRouteTableResponseType;
import com.eucalyptus.compute.common.AssociateRouteTableType;
import com.eucalyptus.compute.common.CloudFilters;
import com.eucalyptus.compute.common.Compute;
import com.eucalyptus.compute.common.DescribeRouteTablesResponseType;
import com.eucalyptus.compute.common.DescribeRouteTablesType;
import com.eucalyptus.compute.common.DescribeSubnetsResponseType;
import com.eucalyptus.compute.common.DescribeSubnetsType;
import com.eucalyptus.compute.common.DisassociateRouteTableResponseType;
import com.eucalyptus.compute.common.DisassociateRouteTableType;
import com.eucalyptus.compute.common.Filter;
import com.eucalyptus.compute.common.ReplaceRouteTableAssociationResponseType;
import com.eucalyptus.compute.common.ReplaceRouteTableAssociationType;
import com.eucalyptus.util.async.AsyncRequests;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.collect.Lists;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Objects;

/**
 * Created by ethomas on 2/3/14.
 */
public class AWSEC2SubnetRouteTableAssociationResourceAction extends StepBasedResourceAction {

  private AWSEC2SubnetRouteTableAssociationProperties properties = new AWSEC2SubnetRouteTableAssociationProperties();
  private AWSEC2SubnetRouteTableAssociationResourceInfo info = new AWSEC2SubnetRouteTableAssociationResourceInfo();

  public AWSEC2SubnetRouteTableAssociationResourceAction() {
    super(fromEnum(CreateSteps.class), fromEnum(DeleteSteps.class), fromUpdateEnum(UpdateNoInterruptionSteps.class), null);
  }

  @Override
  public UpdateType getUpdateType(ResourceAction resourceAction, boolean stackTagsChanged) {
    UpdateType updateType = info.supportsTags() && stackTagsChanged ? UpdateType.NO_INTERRUPTION : UpdateType.NONE;
    AWSEC2SubnetRouteTableAssociationResourceAction otherAction = (AWSEC2SubnetRouteTableAssociationResourceAction) resourceAction;
    if (!Objects.equals(properties.getRouteTableId(), otherAction.properties.getRouteTableId())) {
      updateType = UpdateType.max(updateType, UpdateType.NO_INTERRUPTION);
    }
    if (!Objects.equals(properties.getSubnetId(), otherAction.properties.getSubnetId())) {
      updateType = UpdateType.max(updateType, UpdateType.NEEDS_REPLACEMENT);
    }
    return updateType;
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
        action.info.setCreatedEnoughToDelete(true);
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
        AWSEC2SubnetRouteTableAssociationResourceAction action = (AWSEC2SubnetRouteTableAssociationResourceAction) resourceAction;
        ServiceConfiguration configuration = Topology.lookup(Compute.class);
        if (!Boolean.TRUE.equals(action.info.getCreatedEnoughToDelete())) return action;

        if (!action.associationIdExistsForDelete(configuration)) return action;
        if (!action.routeTableExistsForDelete(configuration)) return action;
        if (!action.subnetExistsForDelete(configuration)) return action;
        action.disassociateRouteTable(configuration, action.info.getPhysicalResourceId());
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
    UPDATE_ASSOCIATION {
      @Override
      public ResourceAction perform(ResourceAction oldResourceAction, ResourceAction newResourceAction) throws Exception {
        AWSEC2SubnetRouteTableAssociationResourceAction oldAction = (AWSEC2SubnetRouteTableAssociationResourceAction) oldResourceAction;
        AWSEC2SubnetRouteTableAssociationResourceAction newAction = (AWSEC2SubnetRouteTableAssociationResourceAction) newResourceAction;
        ServiceConfiguration configuration = Topology.lookup(Compute.class);
        String newAssociationId = newAction.replaceAssociation(configuration, oldAction.info.getPhysicalResourceId(), newAction.properties.getRouteTableId());
        newAction.info.setPhysicalResourceId(newAssociationId);
        newAction.info.setCreatedEnoughToDelete(true);
        newAction.info.setReferenceValueJson(JsonHelper.getStringFromJsonNode(new TextNode(newAction.info.getPhysicalResourceId())));
        return newAction;
      }
    },
    ;

    @Nullable
    @Override
    public Integer getTimeout() {
      return null;
    }
  }

  private String replaceAssociation(ServiceConfiguration configuration, String oldAssociationId, String routeTableId) throws Exception {
    ReplaceRouteTableAssociationType replaceRouteTableAssociationType = MessageHelper.createMessage(ReplaceRouteTableAssociationType.class, info.getEffectiveUserId());
    replaceRouteTableAssociationType.setRouteTableId(routeTableId);
    replaceRouteTableAssociationType.setAssociationId(oldAssociationId);
    ReplaceRouteTableAssociationResponseType replaceRouteTableAssociationResponseType = AsyncRequests.<ReplaceRouteTableAssociationType, ReplaceRouteTableAssociationResponseType> sendSync(configuration, replaceRouteTableAssociationType);
    return replaceRouteTableAssociationResponseType.getNewAssociationId();
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
    describeSubnetsType.getFilterSet( ).add( CloudFilters.filter( "subnet-id", properties.getSubnetId( ) ) );
    DescribeSubnetsResponseType describeSubnetsResponseType = AsyncRequests.sendSync( configuration, describeSubnetsType );
    if (describeSubnetsResponseType.getSubnetSet() == null || describeSubnetsResponseType.getSubnetSet().getItem() == null ||
      describeSubnetsResponseType.getSubnetSet().getItem().isEmpty()) {
      throw new ValidationErrorException("No such subnet with id '" + properties.getSubnetId());
    }
  }

  private void checkRouteTableExists(ServiceConfiguration configuration) throws Exception {
    DescribeRouteTablesType describeRouteTablesType = MessageHelper.createMessage(DescribeRouteTablesType.class, info.getEffectiveUserId());
    describeRouteTablesType.getFilterSet( ).add( CloudFilters.filter( "route-table-id", properties.getRouteTableId( ) ) );
    DescribeRouteTablesResponseType describeRouteTablesResponseType = AsyncRequests.sendSync( configuration, describeRouteTablesType );
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
    describeSubnetsType.getFilterSet( ).add( CloudFilters.filter( "subnet-id", properties.getSubnetId( ) ) );
    DescribeSubnetsResponseType describeSubnetsResponseType = AsyncRequests.sendSync( configuration, describeSubnetsType );
    if (describeSubnetsResponseType.getSubnetSet() == null || describeSubnetsResponseType.getSubnetSet().getItem() == null ||
      describeSubnetsResponseType.getSubnetSet().getItem().isEmpty()) {
      return false;
    }
    return true;
  }

  private boolean routeTableExistsForDelete(ServiceConfiguration configuration) throws Exception {
    DescribeRouteTablesType describeRouteTablesType = MessageHelper.createMessage(DescribeRouteTablesType.class, info.getEffectiveUserId());
    describeRouteTablesType.getFilterSet( ).add( CloudFilters.filter( "route-table-id", properties.getRouteTableId( ) ) );
    DescribeRouteTablesResponseType describeRouteTablesResponseType = AsyncRequests.sendSync( configuration, describeRouteTablesType );
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
    DescribeRouteTablesResponseType describeRouteTablesResponseType = AsyncRequests.<DescribeRouteTablesType, DescribeRouteTablesResponseType> sendSync( configuration, describeRouteTablesType );
    if (describeRouteTablesResponseType.getRouteTableSet() == null || describeRouteTablesResponseType.getRouteTableSet().getItem() == null ||
      describeRouteTablesResponseType.getRouteTableSet().getItem().isEmpty()) {
      return false;
    }
    return true;
  }


}


