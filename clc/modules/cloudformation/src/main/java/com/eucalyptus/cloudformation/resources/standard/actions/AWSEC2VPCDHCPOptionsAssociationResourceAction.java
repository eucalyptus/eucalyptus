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
import com.eucalyptus.cloudformation.resources.standard.info.AWSEC2VPCDHCPOptionsAssociationResourceInfo;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.AWSEC2VPCDHCPOptionsAssociationProperties;
import com.eucalyptus.cloudformation.template.JsonHelper;
import com.eucalyptus.cloudformation.util.MessageHelper;
import com.eucalyptus.cloudformation.workflow.steps.Step;
import com.eucalyptus.cloudformation.workflow.steps.StepBasedResourceAction;
import com.eucalyptus.cloudformation.workflow.steps.UpdateStep;
import com.eucalyptus.cloudformation.workflow.updateinfo.UpdateType;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.Topology;
import com.eucalyptus.compute.common.AssociateDhcpOptionsResponseType;
import com.eucalyptus.compute.common.AssociateDhcpOptionsType;
import com.eucalyptus.compute.common.CloudFilters;
import com.eucalyptus.compute.common.Compute;
import com.eucalyptus.compute.common.DescribeDhcpOptionsResponseType;
import com.eucalyptus.compute.common.DescribeDhcpOptionsType;
import com.eucalyptus.compute.common.DescribeVpcsResponseType;
import com.eucalyptus.compute.common.DescribeVpcsType;
import com.eucalyptus.util.async.AsyncRequests;
import com.fasterxml.jackson.databind.node.TextNode;

import javax.annotation.Nullable;
import java.util.Objects;

/**
 * Created by ethomas on 2/3/14.
 */
public class AWSEC2VPCDHCPOptionsAssociationResourceAction extends StepBasedResourceAction {

  private AWSEC2VPCDHCPOptionsAssociationProperties properties = new AWSEC2VPCDHCPOptionsAssociationProperties();
  private AWSEC2VPCDHCPOptionsAssociationResourceInfo info = new AWSEC2VPCDHCPOptionsAssociationResourceInfo();

  public AWSEC2VPCDHCPOptionsAssociationResourceAction() {
    super(fromEnum(CreateSteps.class), fromEnum(DeleteSteps.class), fromUpdateEnum(UpdateNoInterruptionSteps.class), null);
  }

  @Override
  public UpdateType getUpdateType(ResourceAction resourceAction, boolean stackTagsChanged) {
    UpdateType updateType = info.supportsTags() && stackTagsChanged ? UpdateType.NO_INTERRUPTION : UpdateType.NONE;
    AWSEC2VPCDHCPOptionsAssociationResourceAction otherAction = (AWSEC2VPCDHCPOptionsAssociationResourceAction) resourceAction;
    if (!Objects.equals(properties.getDhcpOptionsId(), otherAction.properties.getDhcpOptionsId())) {
      updateType = UpdateType.max(updateType, UpdateType.NO_INTERRUPTION);
    }
    if (!Objects.equals(properties.getVpcId(), otherAction.properties.getVpcId())) {
      updateType = UpdateType.max(updateType, UpdateType.NEEDS_REPLACEMENT);
    }
    return updateType;
  }

  private enum CreateSteps implements Step {
    CREATE_ASSOCIATION {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        AWSEC2VPCDHCPOptionsAssociationResourceAction action = (AWSEC2VPCDHCPOptionsAssociationResourceAction) resourceAction;
        ServiceConfiguration configuration = Topology.lookup(Compute.class);
        // Check dhcp options (if not "default")
        if (!"default".equals(action.properties.getDhcpOptionsId())) {
          DescribeDhcpOptionsType describeDhcpOptionsType = MessageHelper.createMessage(DescribeDhcpOptionsType.class, action.info.getEffectiveUserId());
          describeDhcpOptionsType.getFilterSet( ).add( CloudFilters.filter( "dhcp-options-id", action.properties.getDhcpOptionsId( ) ) );
          DescribeDhcpOptionsResponseType describeDhcpOptionsResponseType = AsyncRequests.sendSync( configuration, describeDhcpOptionsType );
          if (describeDhcpOptionsResponseType.getDhcpOptionsSet() == null ||
              describeDhcpOptionsResponseType.getDhcpOptionsSet().getItem() == null ||
              describeDhcpOptionsResponseType.getDhcpOptionsSet().getItem().isEmpty()) {
            throw new ValidationErrorException("No such dhcp options: " + action.properties.getDhcpOptionsId());
          }
        }
        // check vpc
        DescribeVpcsType describeVpcsType = MessageHelper.createMessage(DescribeVpcsType.class, action.info.getEffectiveUserId());
        describeVpcsType.getFilterSet( ).add( CloudFilters.filter( "vpc-id", action.properties.getVpcId( ) ) );
        DescribeVpcsResponseType describeVpcsResponseType = AsyncRequests.sendSync( configuration, describeVpcsType );
        if (describeVpcsResponseType.getVpcSet() == null ||
            describeVpcsResponseType.getVpcSet().getItem() == null ||
            describeVpcsResponseType.getVpcSet().getItem().isEmpty()) {
          throw new ValidationErrorException("No such vpc: " + action.properties.getVpcId());
        }
        AssociateDhcpOptionsType associateDhcpOptionsType = MessageHelper.createMessage(AssociateDhcpOptionsType.class, action.info.getEffectiveUserId());
        associateDhcpOptionsType.setDhcpOptionsId(action.properties.getDhcpOptionsId());
        associateDhcpOptionsType.setVpcId(action.properties.getVpcId());
        AsyncRequests.<AssociateDhcpOptionsType,AssociateDhcpOptionsResponseType> sendSync(configuration, associateDhcpOptionsType);
        action.info.setPhysicalResourceId(action.getDefaultPhysicalResourceId());
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
        AWSEC2VPCDHCPOptionsAssociationResourceAction action = (AWSEC2VPCDHCPOptionsAssociationResourceAction) resourceAction;
        ServiceConfiguration configuration = Topology.lookup(Compute.class);
        if (!Boolean.TRUE.equals(action.info.getCreatedEnoughToDelete())) return action;

        // Check dhcp options (if not "default")
        if (!"default".equals(action.properties.getDhcpOptionsId())) {
          DescribeDhcpOptionsType describeDhcpOptionsType = MessageHelper.createMessage(DescribeDhcpOptionsType.class, action.info.getEffectiveUserId());
          describeDhcpOptionsType.getFilterSet( ).add( CloudFilters.filter( "dhcp-options-id", action.properties.getDhcpOptionsId() ) );
          DescribeDhcpOptionsResponseType describeDhcpOptionsResponseType = AsyncRequests.sendSync( configuration, describeDhcpOptionsType);
          if (describeDhcpOptionsResponseType.getDhcpOptionsSet() == null ||
              describeDhcpOptionsResponseType.getDhcpOptionsSet().getItem() == null ||
              describeDhcpOptionsResponseType.getDhcpOptionsSet().getItem().isEmpty()) {
            return action;
          }
        }
        // check vpc
        DescribeVpcsType describeVpcsType = MessageHelper.createMessage(DescribeVpcsType.class, action.info.getEffectiveUserId());
        describeVpcsType.getFilterSet( ).add( CloudFilters.filter( "vpc-id", action.properties.getVpcId( ) ) );
        DescribeVpcsResponseType describeVpcsResponseType = AsyncRequests.sendSync(configuration, describeVpcsType);
        if (describeVpcsResponseType.getVpcSet() == null ||
            describeVpcsResponseType.getVpcSet().getItem() == null ||
            describeVpcsResponseType.getVpcSet().getItem().isEmpty()) {
          return action;
        }
        AssociateDhcpOptionsType associateDhcpOptionsType = MessageHelper.createMessage(AssociateDhcpOptionsType.class, action.info.getEffectiveUserId());
        associateDhcpOptionsType.setDhcpOptionsId("default");
        associateDhcpOptionsType.setVpcId(action.properties.getVpcId());
        AsyncRequests.<AssociateDhcpOptionsType,AssociateDhcpOptionsResponseType> sendSync(configuration, associateDhcpOptionsType);
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
        AWSEC2VPCDHCPOptionsAssociationResourceAction oldAction = (AWSEC2VPCDHCPOptionsAssociationResourceAction) oldResourceAction;
        AWSEC2VPCDHCPOptionsAssociationResourceAction newAction = (AWSEC2VPCDHCPOptionsAssociationResourceAction) newResourceAction;
        ServiceConfiguration configuration = Topology.lookup(Compute.class);
        // Check dhcp options (if not "default")
        if (!"default".equals(newAction.properties.getDhcpOptionsId())) {
          DescribeDhcpOptionsType describeDhcpOptionsType = MessageHelper.createMessage(DescribeDhcpOptionsType.class, newAction.info.getEffectiveUserId());
          describeDhcpOptionsType.getFilterSet( ).add( CloudFilters.filter( "dhcp-options-id", newAction.properties.getDhcpOptionsId( ) ) );
          DescribeDhcpOptionsResponseType describeDhcpOptionsResponseType = AsyncRequests.sendSync( configuration, describeDhcpOptionsType );
          if (describeDhcpOptionsResponseType.getDhcpOptionsSet() == null ||
            describeDhcpOptionsResponseType.getDhcpOptionsSet().getItem() == null ||
            describeDhcpOptionsResponseType.getDhcpOptionsSet().getItem().isEmpty()) {
            throw new ValidationErrorException("No such dhcp options: " + newAction.properties.getDhcpOptionsId());
          }
        }
        // check vpc
        DescribeVpcsType describeVpcsType = MessageHelper.createMessage(DescribeVpcsType.class, newAction.info.getEffectiveUserId());
        describeVpcsType.getFilterSet( ).add( CloudFilters.filter( "vpc-id", newAction.properties.getVpcId( ) ) );
        DescribeVpcsResponseType describeVpcsResponseType = AsyncRequests.sendSync( configuration, describeVpcsType );
        if (describeVpcsResponseType.getVpcSet() == null ||
          describeVpcsResponseType.getVpcSet().getItem() == null ||
          describeVpcsResponseType.getVpcSet().getItem().isEmpty()) {
          throw new ValidationErrorException("No such vpc: " + newAction.properties.getVpcId());
        }
        AssociateDhcpOptionsType associateDhcpOptionsType = MessageHelper.createMessage(AssociateDhcpOptionsType.class, newAction.info.getEffectiveUserId());
        associateDhcpOptionsType.setDhcpOptionsId(newAction.properties.getDhcpOptionsId());
        associateDhcpOptionsType.setVpcId(newAction.properties.getVpcId());
        AsyncRequests.<AssociateDhcpOptionsType,AssociateDhcpOptionsResponseType> sendSync(configuration, associateDhcpOptionsType);
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
    properties = (AWSEC2VPCDHCPOptionsAssociationProperties) resourceProperties;
  }

  @Override
  public ResourceInfo getResourceInfo() {
    return info;
  }

  @Override
  public void setResourceInfo(ResourceInfo resourceInfo) {
    info = (AWSEC2VPCDHCPOptionsAssociationResourceInfo) resourceInfo;
  }



}


