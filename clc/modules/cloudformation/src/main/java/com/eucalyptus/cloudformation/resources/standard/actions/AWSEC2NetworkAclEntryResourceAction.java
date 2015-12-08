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


import com.eucalyptus.cloudformation.ValidationErrorException;
import com.eucalyptus.cloudformation.resources.ResourceAction;
import com.eucalyptus.cloudformation.resources.ResourceInfo;
import com.eucalyptus.cloudformation.resources.ResourceProperties;
import com.eucalyptus.cloudformation.resources.standard.info.AWSEC2NetworkAclEntryResourceInfo;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.AWSEC2NetworkAclEntryProperties;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.EC2ICMP;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.EC2PortRange;
import com.eucalyptus.cloudformation.template.JsonHelper;
import com.eucalyptus.cloudformation.util.MessageHelper;
import com.eucalyptus.cloudformation.workflow.steps.Step;
import com.eucalyptus.cloudformation.workflow.steps.StepBasedResourceAction;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.Topology;
import com.eucalyptus.compute.common.Compute;
import com.eucalyptus.compute.common.CreateNetworkAclEntryResponseType;
import com.eucalyptus.compute.common.CreateNetworkAclEntryType;
import com.eucalyptus.compute.common.DeleteNetworkAclEntryResponseType;
import com.eucalyptus.compute.common.DeleteNetworkAclEntryType;
import com.eucalyptus.compute.common.DescribeNetworkAclsResponseType;
import com.eucalyptus.compute.common.DescribeNetworkAclsType;
import com.eucalyptus.compute.common.Filter;
import com.eucalyptus.compute.common.IcmpTypeCodeType;
import com.eucalyptus.compute.common.NetworkAclEntryType;
import com.eucalyptus.compute.common.NetworkAclType;
import com.eucalyptus.compute.common.PortRangeType;
import com.eucalyptus.util.async.AsyncRequests;
import com.fasterxml.jackson.databind.node.TextNode;

import javax.annotation.Nullable;

/**
 * Created by ethomas on 2/3/14.
 */
public class AWSEC2NetworkAclEntryResourceAction extends StepBasedResourceAction {

  private AWSEC2NetworkAclEntryProperties properties = new AWSEC2NetworkAclEntryProperties();
  private AWSEC2NetworkAclEntryResourceInfo info = new AWSEC2NetworkAclEntryResourceInfo();

  public AWSEC2NetworkAclEntryResourceAction() {
    super(fromEnum(CreateSteps.class), fromEnum(DeleteSteps.class));
  }

  private enum CreateSteps implements Step {
    CREATE_NETWORK_ACL_ENTRY {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        AWSEC2NetworkAclEntryResourceAction action = (AWSEC2NetworkAclEntryResourceAction) resourceAction;
        ServiceConfiguration configuration = Topology.lookup(Compute.class);
        // See if network acl is there
        if (action.properties.getNetworkAclId().isEmpty()) {
          throw new ValidationErrorException("NetworkAclId is a required field");
        }
        DescribeNetworkAclsType describeNetworkAclsType = MessageHelper.createMessage(DescribeNetworkAclsType.class, action.info.getEffectiveUserId());
        describeNetworkAclsType.getFilterSet( ).add( Filter.filter( "network-acl-id", action.properties.getNetworkAclId() ) );
        DescribeNetworkAclsResponseType describeNetworkAclsResponseType = AsyncRequests.sendSync(configuration, describeNetworkAclsType);
        if (describeNetworkAclsResponseType.getNetworkAclSet() == null || describeNetworkAclsResponseType.getNetworkAclSet().getItem() == null ||
          describeNetworkAclsResponseType.getNetworkAclSet().getItem().isEmpty()) {
          throw new ValidationErrorException("No such network acl with id '" + action.properties.getNetworkAclId());
        }
        CreateNetworkAclEntryType createNetworkAclEntryType = MessageHelper.createMessage(CreateNetworkAclEntryType.class, action.info.getEffectiveUserId());
        createNetworkAclEntryType.setCidrBlock(action.properties.getCidrBlock());
        if (action.properties.getEgress() != null){
          createNetworkAclEntryType.setEgress(action.properties.getEgress());
        }
        createNetworkAclEntryType.setIcmpTypeCode(action.convertIcmpTypeCode(action.properties.getIcmp()));
        createNetworkAclEntryType.setNetworkAclId(action.properties.getNetworkAclId());
        createNetworkAclEntryType.setPortRange(action.convertPortRange(action.properties.getPortRange()));
        createNetworkAclEntryType.setProtocol(action.properties.getProtocol() == null ? null : String.valueOf(action.properties.getProtocol()));
        createNetworkAclEntryType.setRuleAction(action.properties.getRuleAction());
        createNetworkAclEntryType.setRuleNumber(action.properties.getRuleNumber());
        CreateNetworkAclEntryResponseType CreateNetworkAclEntryResponseType = AsyncRequests.<CreateNetworkAclEntryType, CreateNetworkAclEntryResponseType>sendSync(configuration, createNetworkAclEntryType);
        action.info.setPhysicalResourceId(action.getDefaultPhysicalResourceId());
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
    DELETE_NETWORK_ACL_ENTRY {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        AWSEC2NetworkAclEntryResourceAction action = (AWSEC2NetworkAclEntryResourceAction) resourceAction;
        ServiceConfiguration configuration = Topology.lookup(Compute.class);
        if (action.info.getPhysicalResourceId() == null) return action;

        // See if network ACL is there
        DescribeNetworkAclsType describeNetworkAclsType = MessageHelper.createMessage(DescribeNetworkAclsType.class, action.info.getEffectiveUserId());
        describeNetworkAclsType.getFilterSet( ).add( Filter.filter( "network-acl-id", action.properties.getNetworkAclId() ) );
        DescribeNetworkAclsResponseType describeNetworkAclsResponseType = AsyncRequests.sendSync( configuration, describeNetworkAclsType);
        if (describeNetworkAclsResponseType.getNetworkAclSet() == null || describeNetworkAclsResponseType.getNetworkAclSet().getItem() == null ||
          describeNetworkAclsResponseType.getNetworkAclSet().getItem().isEmpty()) {
          return action; // no network acl
        }
        // now see if the entry is in the rule
        boolean foundEntry = false;
        for (NetworkAclType networkAclType: describeNetworkAclsResponseType.getNetworkAclSet().getItem()) {
          if (networkAclType.getEntrySet() == null || networkAclType.getEntrySet().getItem() == null) continue;
          for (NetworkAclEntryType networkAclEntryType : networkAclType.getEntrySet().getItem()) {
            if (action.properties.getRuleNumber().equals(networkAclEntryType.getRuleNumber()) && action.properties.getEgress().equals(networkAclEntryType.getEgress())) {
              foundEntry = true;
              break;
            }
          }
        }
        if (!foundEntry) return action; // no rule to remove
        DeleteNetworkAclEntryType deleteNetworkAclEntryType = MessageHelper.createMessage(DeleteNetworkAclEntryType.class, action.info.getEffectiveUserId());
        deleteNetworkAclEntryType.setNetworkAclId(action.properties.getNetworkAclId());
        deleteNetworkAclEntryType.setEgress(action.properties.getEgress());
        deleteNetworkAclEntryType.setRuleNumber(action.properties.getRuleNumber());
        DeleteNetworkAclEntryResponseType deleteNetworkAclEntryResponseType = AsyncRequests.<DeleteNetworkAclEntryType, DeleteNetworkAclEntryResponseType> sendSync(configuration, deleteNetworkAclEntryType);
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
    properties = (AWSEC2NetworkAclEntryProperties) resourceProperties;
  }

  @Override
  public ResourceInfo getResourceInfo() {
    return info;
  }

  @Override
  public void setResourceInfo(ResourceInfo resourceInfo) {
    info = (AWSEC2NetworkAclEntryResourceInfo) resourceInfo;
  }

  private PortRangeType convertPortRange(EC2PortRange portRange) {
    if (portRange == null) return null;
    PortRangeType portRangeType = new PortRangeType();
    portRangeType.setFrom(portRange.getFrom());
    portRangeType.setTo(portRange.getTo());
    return portRangeType;
  }

  private IcmpTypeCodeType convertIcmpTypeCode(EC2ICMP icmp) {
    if (icmp == null) return null;
    IcmpTypeCodeType icmpTypeCodeType = new IcmpTypeCodeType();
    icmpTypeCodeType.setCode(icmp.getCode());
    icmpTypeCodeType.setType(icmp.getType());
    return icmpTypeCodeType;
  }



}


