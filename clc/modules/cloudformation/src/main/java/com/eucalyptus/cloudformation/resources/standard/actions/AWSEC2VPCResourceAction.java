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


import com.eucalyptus.autoscaling.common.msgs.DeleteTagsResponseType;
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
import com.eucalyptus.cloudformation.workflow.steps.UpdateStep;
import com.eucalyptus.cloudformation.workflow.updateinfo.UpdateType;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.Topology;
import com.eucalyptus.compute.common.AttributeBooleanValueType;
import com.eucalyptus.compute.common.CloudFilters;
import com.eucalyptus.compute.common.Compute;
import com.eucalyptus.compute.common.CreateTagsResponseType;
import com.eucalyptus.compute.common.CreateTagsType;
import com.eucalyptus.compute.common.CreateVpcResponseType;
import com.eucalyptus.compute.common.CreateVpcType;
import com.eucalyptus.compute.common.DeleteTagsType;
import com.eucalyptus.compute.common.DeleteVpcResponseType;
import com.eucalyptus.compute.common.DeleteVpcType;
import com.eucalyptus.compute.common.DescribeNetworkAclsResponseType;
import com.eucalyptus.compute.common.DescribeNetworkAclsType;
import com.eucalyptus.compute.common.DescribeSecurityGroupsResponseType;
import com.eucalyptus.compute.common.DescribeSecurityGroupsType;
import com.eucalyptus.compute.common.DescribeTagsResponseType;
import com.eucalyptus.compute.common.DescribeTagsType;
import com.eucalyptus.compute.common.DescribeVpcsResponseType;
import com.eucalyptus.compute.common.DescribeVpcsType;
import com.eucalyptus.compute.common.ModifyVpcAttributeResponseType;
import com.eucalyptus.compute.common.ModifyVpcAttributeType;
import com.eucalyptus.compute.common.TagInfo;
import com.eucalyptus.util.async.AsyncRequests;
import com.eucalyptus.util.async.CheckedListenableFuture;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Created by ethomas on 2/3/14.
 */
public class AWSEC2VPCResourceAction extends StepBasedResourceAction {

  private AWSEC2VPCProperties properties = new AWSEC2VPCProperties();
  private AWSEC2VPCResourceInfo info = new AWSEC2VPCResourceInfo();

  public AWSEC2VPCResourceAction() {
    super(fromEnum(CreateSteps.class), fromEnum(DeleteSteps.class), fromUpdateEnum(UpdateNoInterruptionSteps.class), null);
  }

  @Override
  public UpdateType getUpdateType(ResourceAction resourceAction, boolean stackTagsChanged) {
    UpdateType updateType = info.supportsTags() && stackTagsChanged ? UpdateType.NO_INTERRUPTION : UpdateType.NONE;
    AWSEC2VPCResourceAction otherAction = (AWSEC2VPCResourceAction) resourceAction;
    if (!Objects.equals(properties.getCidrBlock(), otherAction.properties.getCidrBlock())) {
      updateType = UpdateType.max(updateType, UpdateType.NEEDS_REPLACEMENT);
    }
    if (!Objects.equals(properties.getEnableDnsHostnames(), otherAction.properties.getEnableDnsHostnames())) {
      updateType = UpdateType.max(updateType, UpdateType.NO_INTERRUPTION);
    }
    if (!Objects.equals(properties.getEnableDnsSupport(), otherAction.properties.getEnableDnsSupport())) {
      updateType = UpdateType.max(updateType, UpdateType.NO_INTERRUPTION);
    }
    if (!Objects.equals(properties.getInstanceTenancy(), otherAction.properties.getInstanceTenancy())) {
      updateType = UpdateType.max(updateType, UpdateType.NEEDS_REPLACEMENT);
    }
    if (!Objects.equals(properties.getTags(), otherAction.properties.getTags())) {
      updateType = UpdateType.max(updateType, UpdateType.NO_INTERRUPTION);
    }
    return updateType;
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
        action.info.setCreatedEnoughToDelete(true);
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
        String effectiveAdminUserId = action.info.getAccountId( );
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
        groupsRequest.setFilterSet( Lists.newArrayList( CloudFilters.filter( "vpc-id", action.info.getPhysicalResourceId( ) ) ) );
        final CheckedListenableFuture<DescribeSecurityGroupsResponseType> groupsFuture =
            AsyncRequests.dispatch( configuration, groupsRequest );
        // Describe network acls to find the default
        final DescribeNetworkAclsType networkAclsRequest = MessageHelper.createMessage(DescribeNetworkAclsType.class, action.info.getEffectiveUserId());
        networkAclsRequest.setFilterSet( Lists.newArrayList(
            CloudFilters.filter( "vpc-id", action.info.getPhysicalResourceId( ) ),
            CloudFilters.filter( "default", "true" )
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
    };

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
        if (!Boolean.TRUE.equals(action.info.getCreatedEnoughToDelete())) return action;

        DescribeVpcsType describeVpcsType = MessageHelper.createMessage(DescribeVpcsType.class, action.info.getEffectiveUserId());
        describeVpcsType.getFilterSet( ).add( CloudFilters.filter( "vpc-id", action.info.getPhysicalResourceId( ) ) );
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

  private enum UpdateNoInterruptionSteps implements UpdateStep {
    UPDATE_DNS_ENTRIES {
      @Override
      public ResourceAction perform(ResourceAction oldResourceAction, ResourceAction newResourceAction) throws Exception {
        AWSEC2VPCResourceAction oldAction = (AWSEC2VPCResourceAction) oldResourceAction;
        AWSEC2VPCResourceAction newAction = (AWSEC2VPCResourceAction) newResourceAction;
        ServiceConfiguration configuration = Topology.lookup(Compute.class);
        ModifyVpcAttributeType modifyVpcAttributeType = MessageHelper.createMessage(ModifyVpcAttributeType.class, newAction.info.getEffectiveUserId());
        boolean enableDnsSupport = true; // default value
        boolean enableDnsHostnames = false; // default value
        if (newAction.properties.getEnableDnsSupport() != null) {
          enableDnsSupport = newAction.properties.getEnableDnsSupport();
        }
        if (newAction.properties.getEnableDnsHostnames() != null) {
          enableDnsHostnames = newAction.properties.getEnableDnsHostnames();
        }
        modifyVpcAttributeType.setVpcId(newAction.info.getPhysicalResourceId());
        modifyVpcAttributeType.setEnableDnsSupport(newAction.createAttributeBooleanValueType(enableDnsSupport));
        modifyVpcAttributeType.setEnableDnsHostnames(newAction.createAttributeBooleanValueType(enableDnsHostnames));
        // TODO: does the below return any errors?
        AsyncRequests.<ModifyVpcAttributeType,ModifyVpcAttributeResponseType> sendSync(configuration, modifyVpcAttributeType);
        return newAction;
      }
    },
    UPDATE_TAGS {
      @Override
      public ResourceAction perform(ResourceAction oldResourceAction, ResourceAction newResourceAction) throws Exception {
        AWSEC2VPCResourceAction oldAction = (AWSEC2VPCResourceAction) oldResourceAction;
        AWSEC2VPCResourceAction newAction = (AWSEC2VPCResourceAction) newResourceAction;
        ServiceConfiguration configuration = Topology.lookup(Compute.class);
        DescribeTagsType describeTagsType = MessageHelper.createMessage(DescribeTagsType.class, newAction.info.getEffectiveUserId());
        describeTagsType.setFilterSet(Lists.newArrayList( CloudFilters.filter("resource-id", newAction.info.getPhysicalResourceId())));
        DescribeTagsResponseType describeTagsResponseType = AsyncRequests.sendSync(configuration, describeTagsType);
        Set<EC2Tag> existingTags = Sets.newLinkedHashSet();
        if (describeTagsResponseType != null && describeTagsResponseType.getTagSet() != null) {
          for (TagInfo tagInfo: describeTagsResponseType.getTagSet()) {
            EC2Tag tag = new EC2Tag();
            tag.setKey(tagInfo.getKey());
            tag.setValue(tagInfo.getValue());
            existingTags.add(tag);
          }
        }
        Set<EC2Tag> newTags = Sets.newLinkedHashSet();
        if (newAction.properties.getTags() != null) {
          newTags.addAll(newAction.properties.getTags());
        }
        List<EC2Tag> newStackTags = TagHelper.getEC2StackTags(newAction.getStackEntity());
        if (newStackTags != null) {
          newTags.addAll(newStackTags);
        }
        TagHelper.checkReservedEC2TemplateTags(newTags);
        // add only 'new' tags
        Set<EC2Tag> onlyNewTags = Sets.difference(newTags, existingTags);
        if (!onlyNewTags.isEmpty()) {
          CreateTagsType createTagsType = MessageHelper.createMessage(CreateTagsType.class, newAction.info.getEffectiveUserId());
          createTagsType.setResourcesSet(Lists.newArrayList(newAction.info.getPhysicalResourceId()));
          createTagsType.setTagSet(EC2Helper.createTagSet(onlyNewTags));
          AsyncRequests.<CreateTagsType, CreateTagsResponseType>sendSync(configuration, createTagsType);
        }
        //  Get old tags...
        Set<EC2Tag> oldTags = Sets.newLinkedHashSet();
        if (oldAction.properties.getTags() != null) {
          oldTags.addAll(oldAction.properties.getTags());
        }
        List<EC2Tag> oldStackTags = TagHelper.getEC2StackTags(oldAction.getStackEntity());
        if (oldStackTags != null) {
          oldTags.addAll(oldStackTags);
        }

        // remove only the old tags that are not new and that exist
        Set<EC2Tag> tagsToRemove = Sets.intersection(oldTags, Sets.difference(existingTags, newTags));
        if (!tagsToRemove.isEmpty()) {
          DeleteTagsType deleteTagsType = MessageHelper.createMessage(DeleteTagsType.class, newAction.info.getEffectiveUserId());
          deleteTagsType.setResourcesSet(Lists.newArrayList(newAction.info.getPhysicalResourceId()));
          deleteTagsType.setTagSet(EC2Helper.deleteTagSet(tagsToRemove));
          AsyncRequests.<DeleteTagsType, DeleteTagsResponseType>sendSync(configuration, deleteTagsType);
        }
        return newAction;
      }
    },
    DESCRIBE_VPC_RESOURCES_TO_UPDATE_ATTRIBUTES {
      @Override
      public ResourceAction perform(ResourceAction oldResourceAction, ResourceAction newResourceAction) throws Exception {
        AWSEC2VPCResourceAction oldAction = (AWSEC2VPCResourceAction) oldResourceAction;
        AWSEC2VPCResourceAction newAction = (AWSEC2VPCResourceAction) newResourceAction;
        ServiceConfiguration configuration = Topology.lookup(Compute.class);
        // Describe groups to find the default
        final DescribeSecurityGroupsType groupsRequest = MessageHelper.createMessage(DescribeSecurityGroupsType.class, newAction.info.getEffectiveUserId());
        groupsRequest.setSecurityGroupSet(Lists.newArrayList("default"));
        groupsRequest.setFilterSet(Lists.newArrayList( CloudFilters.filter("vpc-id", newAction.info.getPhysicalResourceId())));
        final CheckedListenableFuture<DescribeSecurityGroupsResponseType> groupsFuture =
          AsyncRequests.dispatch(configuration, groupsRequest);
        // Describe network acls to find the default
        final DescribeNetworkAclsType networkAclsRequest = MessageHelper.createMessage(DescribeNetworkAclsType.class, newAction.info.getEffectiveUserId());
        networkAclsRequest.setFilterSet(Lists.newArrayList(
          CloudFilters.filter("vpc-id", newAction.info.getPhysicalResourceId()),
          CloudFilters.filter("default", "true")
        ));
        final CheckedListenableFuture<DescribeNetworkAclsResponseType> networkAclsFuture =
          AsyncRequests.dispatch(configuration, networkAclsRequest);
        // Record attribute values
        if (!groupsFuture.get().getSecurityGroupInfo().isEmpty()) {
          newAction.info.setDefaultSecurityGroup(JsonHelper.getStringFromJsonNode(new TextNode(groupsFuture.get().getSecurityGroupInfo().get(0).getGroupId())));
        }
        if (!networkAclsFuture.get().getNetworkAclSet().getItem().isEmpty()) {
          newAction.info.setDefaultNetworkAcl(JsonHelper.getStringFromJsonNode(new TextNode(networkAclsFuture.get().getNetworkAclSet().getItem().get(0).getNetworkAclId())));
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


