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


import com.eucalyptus.auth.Accounts;
import com.eucalyptus.cloudformation.ValidationErrorException;
import com.eucalyptus.cloudformation.resources.EC2Helper;
import com.eucalyptus.cloudformation.resources.ResourceAction;
import com.eucalyptus.cloudformation.resources.ResourceInfo;
import com.eucalyptus.cloudformation.resources.ResourceProperties;
import com.eucalyptus.cloudformation.resources.standard.TagHelper;
import com.eucalyptus.cloudformation.resources.standard.info.AWSEC2VPCResourceInfo;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.AWSEC2VPCProperties;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.EC2Tag;
import com.eucalyptus.cloudformation.template.JsonHelper;
import com.eucalyptus.cloudformation.util.MessageHelper;
import com.eucalyptus.cloudformation.workflow.steps.Step;
import com.eucalyptus.cloudformation.workflow.steps.StepBasedResourceAction;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.Topology;
import com.eucalyptus.compute.common.AttributeBooleanValueType;
import com.eucalyptus.compute.common.Compute;
import com.eucalyptus.compute.common.CreateTagsResponseType;
import com.eucalyptus.compute.common.CreateTagsType;
import com.eucalyptus.compute.common.CreateVpcResponseType;
import com.eucalyptus.compute.common.CreateVpcType;
import com.eucalyptus.compute.common.DeleteVpcResponseType;
import com.eucalyptus.compute.common.DeleteVpcType;
import com.eucalyptus.compute.common.DescribeNetworkAclsResponseType;
import com.eucalyptus.compute.common.DescribeNetworkAclsType;
import com.eucalyptus.compute.common.DescribeSecurityGroupsResponseType;
import com.eucalyptus.compute.common.DescribeSecurityGroupsType;
import com.eucalyptus.compute.common.DescribeVpcsResponseType;
import com.eucalyptus.compute.common.DescribeVpcsType;
import com.eucalyptus.compute.common.Filter;
import com.eucalyptus.compute.common.ModifyVpcAttributeResponseType;
import com.eucalyptus.compute.common.ModifyVpcAttributeType;
import com.eucalyptus.util.async.AsyncRequests;
import com.eucalyptus.util.async.CheckedListenableFuture;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.collect.Lists;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Created by ethomas on 2/3/14.
 */
public class AWSEC2VPCResourceAction extends StepBasedResourceAction {

  private AWSEC2VPCProperties properties = new AWSEC2VPCProperties();
  private AWSEC2VPCResourceInfo info = new AWSEC2VPCResourceInfo();

  public AWSEC2VPCResourceAction() {
    super(fromEnum(CreateSteps.class), fromEnum(DeleteSteps.class), null, null);
  }

  private enum CreateSteps implements Step {
    CREATE_VPC {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        AWSEC2VPCResourceAction action = (AWSEC2VPCResourceAction) resourceAction;
        ServiceConfiguration configuration = Topology.lookup(Compute.class);
        CreateVpcType createVpcType = MessageHelper.createMessage(CreateVpcType.class, action.info.getEffectiveUserId());
        createVpcType.setCidrBlock(action.properties.getCidrBlock());
        if (action.properties.getInstanceTenancy() == null) {
          createVpcType.setInstanceTenancy("default");
        } else if (!"default".equals(action.properties.getInstanceTenancy()) && !"dedicated".equals(action.properties.getInstanceTenancy())) {
          throw new ValidationErrorException("InstanceTenancy must be 'dedicated' or 'default");
        } else {
          createVpcType.setInstanceTenancy(action.properties.getInstanceTenancy());
        }
        CreateVpcResponseType createVpcResponseType = AsyncRequests.<CreateVpcType,CreateVpcResponseType> sendSync(configuration, createVpcType);
        action.info.setPhysicalResourceId(createVpcResponseType.getVpc().getVpcId());
        action.info.setCidrBlock(JsonHelper.getStringFromJsonNode(new TextNode( createVpcResponseType.getVpc( ).getCidrBlock( ))));
        action.info.setReferenceValueJson(JsonHelper.getStringFromJsonNode(new TextNode(action.info.getPhysicalResourceId())));
        return action;
      }
    },
    SET_DNS_ENTRIES {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        AWSEC2VPCResourceAction action = (AWSEC2VPCResourceAction) resourceAction;
        ServiceConfiguration configuration = Topology.lookup(Compute.class);
        ModifyVpcAttributeType modifyVpcAttributeType = MessageHelper.createMessage(ModifyVpcAttributeType.class, action.info.getEffectiveUserId());
        boolean enableDnsSupport = true; // default value
        boolean enableDnsHostnames = false; // default value
        if (action.properties.getEnableDnsSupport() != null) {
          enableDnsSupport = action.properties.getEnableDnsSupport();
        }
        if (action.properties.getEnableDnsHostnames() != null) {
          enableDnsHostnames = action.properties.getEnableDnsHostnames();
        }
        modifyVpcAttributeType.setVpcId(action.info.getPhysicalResourceId());
        modifyVpcAttributeType.setEnableDnsSupport(action.createAttributeBooleanValueType(enableDnsSupport));
        modifyVpcAttributeType.setEnableDnsHostnames(action.createAttributeBooleanValueType(enableDnsHostnames));
        // TODO: does the below return any errors?
        AsyncRequests.<ModifyVpcAttributeType,ModifyVpcAttributeResponseType> sendSync(configuration, modifyVpcAttributeType);
        return action;
      }
    },
    CREATE_TAGS {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        AWSEC2VPCResourceAction action = (AWSEC2VPCResourceAction) resourceAction;
        ServiceConfiguration configuration = Topology.lookup(Compute.class);
        // Create 'system' tags as admin user
        String effectiveAdminUserId = Accounts.lookupPrincipalByAccountNumber( Accounts.lookupPrincipalByUserId(action.info.getEffectiveUserId()).getAccountNumber( ) ).getUserId();
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
    },
    DESCRIBE_VPC_RESOURCES_TO_GET_ATTRIBUTES {
      @Override
      public ResourceAction perform( final ResourceAction resourceAction ) throws Exception {
        AWSEC2VPCResourceAction action = (AWSEC2VPCResourceAction) resourceAction;
        ServiceConfiguration configuration = Topology.lookup(Compute.class);
        // Describe groups to find the default
        final DescribeSecurityGroupsType groupsRequest = MessageHelper.createMessage(DescribeSecurityGroupsType.class, action.info.getEffectiveUserId());
        groupsRequest.setSecurityGroupSet( Lists.newArrayList( "default" ) );
        groupsRequest.setFilterSet( Lists.newArrayList( Filter.filter( "vpc-id", action.info.getPhysicalResourceId( ) ) ) );
        final CheckedListenableFuture<DescribeSecurityGroupsResponseType> groupsFuture =
            AsyncRequests.dispatch( configuration, groupsRequest );
        // Describe network acls to find the default
        final DescribeNetworkAclsType networkAclsRequest = MessageHelper.createMessage(DescribeNetworkAclsType.class, action.info.getEffectiveUserId());
        networkAclsRequest.setFilterSet( Lists.newArrayList(
            Filter.filter( "vpc-id", action.info.getPhysicalResourceId( ) ),
            Filter.filter( "default", "true" )
        ) );
        final CheckedListenableFuture<DescribeNetworkAclsResponseType> networkAclsFuture =
            AsyncRequests.dispatch( configuration, networkAclsRequest );
        // Record attribute values
        if ( !groupsFuture.get( ).getSecurityGroupInfo( ).isEmpty( ) ) {
          action.info.setDefaultSecurityGroup( JsonHelper.getStringFromJsonNode( new TextNode( groupsFuture.get( ).getSecurityGroupInfo( ).get( 0 ).getGroupId( ) ) ) );
        }
        if ( !networkAclsFuture.get( ).getNetworkAclSet( ).getItem( ).isEmpty( ) ) {
          action.info.setDefaultNetworkAcl( JsonHelper.getStringFromJsonNode( new TextNode( networkAclsFuture.get( ).getNetworkAclSet( ).getItem( ).get( 0 ).getNetworkAclId( ) ) ) );
        }
        return action;
      }
    },
    ;

    @Nullable
    @Override
    public Integer getTimeout() {
      return null;
    }
  }

  private enum DeleteSteps implements Step {
    DELETE_VPC {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        AWSEC2VPCResourceAction action = (AWSEC2VPCResourceAction) resourceAction;
        ServiceConfiguration configuration = Topology.lookup(Compute.class);
        if (action.info.getPhysicalResourceId() == null) return action;

        DescribeVpcsType describeVpcsType = MessageHelper.createMessage(DescribeVpcsType.class, action.info.getEffectiveUserId());
        describeVpcsType.getFilterSet( ).add( Filter.filter( "vpc-id", action.info.getPhysicalResourceId( ) ) );
        DescribeVpcsResponseType describeVpcsResponseType = AsyncRequests.sendSync( configuration, describeVpcsType );
        if (describeVpcsResponseType.getVpcSet() == null ||
            describeVpcsResponseType.getVpcSet().getItem() == null ||
            describeVpcsResponseType.getVpcSet().getItem().isEmpty()) {
          return action; // already deleted
        }
        DeleteVpcType deleteVpcType = MessageHelper.createMessage(DeleteVpcType.class, action.info.getEffectiveUserId());
        deleteVpcType.setVpcId(action.info.getPhysicalResourceId());
        AsyncRequests.<DeleteVpcType,DeleteVpcResponseType> sendSync(configuration, deleteVpcType);
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
    properties = (AWSEC2VPCProperties) resourceProperties;
  }

  @Override
  public ResourceInfo getResourceInfo() {
    return info;
  }

  @Override
  public void setResourceInfo(ResourceInfo resourceInfo) {
    info = (AWSEC2VPCResourceInfo) resourceInfo;
  }

  private AttributeBooleanValueType createAttributeBooleanValueType(boolean value) {
    AttributeBooleanValueType attributeBooleanValueType = new AttributeBooleanValueType();
    attributeBooleanValueType.setValue(value);
    return attributeBooleanValueType;
  }



}


