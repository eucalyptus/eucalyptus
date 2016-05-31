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
import com.eucalyptus.cloudformation.resources.EC2Helper;
import com.eucalyptus.cloudformation.resources.ResourceAction;
import com.eucalyptus.cloudformation.resources.ResourceInfo;
import com.eucalyptus.cloudformation.resources.ResourceProperties;
import com.eucalyptus.cloudformation.resources.standard.TagHelper;
import com.eucalyptus.cloudformation.resources.standard.info.AWSEC2SubnetResourceInfo;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.AWSEC2SubnetProperties;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.EC2Tag;
import com.eucalyptus.cloudformation.template.JsonHelper;
import com.eucalyptus.cloudformation.util.MessageHelper;
import com.eucalyptus.cloudformation.workflow.RetryAfterConditionCheckFailedException;
import com.eucalyptus.cloudformation.workflow.steps.Step;
import com.eucalyptus.cloudformation.workflow.steps.StepBasedResourceAction;
import com.eucalyptus.cloudformation.workflow.steps.UpdateStep;
import com.eucalyptus.cloudformation.workflow.updateinfo.UpdateType;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.Topology;
import com.eucalyptus.compute.common.AttributeBooleanValueType;
import com.eucalyptus.compute.common.Compute;
import com.eucalyptus.compute.common.CreateSubnetResponseType;
import com.eucalyptus.compute.common.CreateSubnetType;
import com.eucalyptus.compute.common.CreateTagsResponseType;
import com.eucalyptus.compute.common.CreateTagsType;
import com.eucalyptus.compute.common.DeleteSubnetResponseType;
import com.eucalyptus.compute.common.DeleteSubnetType;
import com.eucalyptus.compute.common.DeleteTagsResponseType;
import com.eucalyptus.compute.common.DeleteTagsType;
import com.eucalyptus.compute.common.DescribeSubnetsResponseType;
import com.eucalyptus.compute.common.DescribeSubnetsType;
import com.eucalyptus.compute.common.DescribeTagsResponseType;
import com.eucalyptus.compute.common.DescribeTagsType;
import com.eucalyptus.compute.common.DescribeVpcsResponseType;
import com.eucalyptus.compute.common.DescribeVpcsType;
import com.eucalyptus.compute.common.Filter;
import com.eucalyptus.compute.common.ModifySubnetAttributeResponseType;
import com.eucalyptus.compute.common.ModifySubnetAttributeType;
import com.eucalyptus.compute.common.TagInfo;
import com.eucalyptus.configurable.ConfigurableClass;
import com.eucalyptus.configurable.ConfigurableField;
import com.eucalyptus.util.async.AsyncExceptions;
import com.eucalyptus.util.async.AsyncRequests;
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
@ConfigurableClass( root = "cloudformation", description = "Parameters controlling cloud formation")
public class AWSEC2SubnetResourceAction extends StepBasedResourceAction {
  @ConfigurableField(initial = "300", description = "The amount of time (in seconds) to retry subnet deletes")
  public static volatile Integer SUBNET_MAX_DELETE_RETRY_SECS = 300;

  private AWSEC2SubnetProperties properties = new AWSEC2SubnetProperties();
  private AWSEC2SubnetResourceInfo info = new AWSEC2SubnetResourceInfo();

  public AWSEC2SubnetResourceAction() {
    super(fromEnum(CreateSteps.class), fromEnum(DeleteSteps.class), fromUpdateEnum(UpdateNoInterruptionSteps.class), null);
  }

  @Override
  public UpdateType getUpdateType(ResourceAction resourceAction, boolean stackTagsChanged) {
    UpdateType updateType = info.supportsTags() && stackTagsChanged ? UpdateType.NO_INTERRUPTION : UpdateType.NONE;
    AWSEC2SubnetResourceAction otherAction = (AWSEC2SubnetResourceAction) resourceAction;
    if (!Objects.equals(properties.getAvailabilityZone(), otherAction.properties.getAvailabilityZone())) {
      updateType = UpdateType.max(updateType, UpdateType.NEEDS_REPLACEMENT);
    }
    if (!Objects.equals(properties.getCidrBlock(), otherAction.properties.getCidrBlock())) {
      updateType = UpdateType.max(updateType, UpdateType.NEEDS_REPLACEMENT);
    }
    if (!Objects.equals(properties.getMapPublicIpOnLaunch(), otherAction.properties.getMapPublicIpOnLaunch())) {
      updateType = UpdateType.max(updateType, UpdateType.NO_INTERRUPTION);
    }
    if (!Objects.equals(properties.getTags(), otherAction.properties.getTags())) {
      updateType = UpdateType.max(updateType, UpdateType.NO_INTERRUPTION);
    }
    if (!Objects.equals(properties.getVpcId(), otherAction.properties.getVpcId())) {
      updateType = UpdateType.max(updateType, UpdateType.NEEDS_REPLACEMENT);
    }
    return updateType;
  }

  private enum CreateSteps implements Step {
    CREATE_SUBNET {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        AWSEC2SubnetResourceAction action = (AWSEC2SubnetResourceAction) resourceAction;
        ServiceConfiguration configuration = Topology.lookup(Compute.class);
        CreateSubnetType createSubnetType = MessageHelper.createMessage(CreateSubnetType.class, action.info.getEffectiveUserId());
        createSubnetType.setVpcId(action.properties.getVpcId());
        if (action.properties.getAvailabilityZone() != null) {
          createSubnetType.setAvailabilityZone(action.properties.getAvailabilityZone());
        }
        createSubnetType.setCidrBlock(action.properties.getCidrBlock());
        CreateSubnetResponseType createSubnetResponseType = AsyncRequests.<CreateSubnetType,CreateSubnetResponseType> sendSync(configuration, createSubnetType);
        action.info.setPhysicalResourceId(createSubnetResponseType.getSubnet().getSubnetId());
        action.info.setCreatedEnoughToDelete(true);
        action.info.setReferenceValueJson(JsonHelper.getStringFromJsonNode(new TextNode(action.info.getPhysicalResourceId())));
        action.info.setAvailabilityZone(JsonHelper.getStringFromJsonNode(new TextNode(createSubnetResponseType.getSubnet().getAvailabilityZone())));
        return action;
      }
    },
    MODIFY_SUBNET_ATTRIBUTES {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        final AWSEC2SubnetResourceAction action = (AWSEC2SubnetResourceAction) resourceAction;
        if ( action.properties.getMapPublicIpOnLaunch( ) != null ) {
          final ServiceConfiguration configuration = Topology.lookup(Compute.class);
          final ModifySubnetAttributeType modifySubnet =
              MessageHelper.createMessage(ModifySubnetAttributeType.class, action.info.getEffectiveUserId());
          final AttributeBooleanValueType value = new AttributeBooleanValueType( );
          value.setValue( action.properties.getMapPublicIpOnLaunch( ) );
          modifySubnet.setMapPublicIpOnLaunch( value );
          modifySubnet.setSubnetId( action.info.getPhysicalResourceId( ) );
          AsyncRequests.<ModifySubnetAttributeType,ModifySubnetAttributeResponseType>sendSync( configuration, modifySubnet );
        }
        return action;
      }
    },
    CREATE_TAGS {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        AWSEC2SubnetResourceAction action = (AWSEC2SubnetResourceAction) resourceAction;
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
    };

    @Nullable
    @Override
    public Integer getTimeout() {
      return null;
    }
  }

  private enum DeleteSteps implements Step {
    DELETE_SUBNET {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        AWSEC2SubnetResourceAction action = (AWSEC2SubnetResourceAction) resourceAction;
        ServiceConfiguration configuration = Topology.lookup(Compute.class);
        if (!Boolean.TRUE.equals(action.info.getCreatedEnoughToDelete())) return action;

        // Check vpc (return if gone)
        DescribeVpcsType describeVpcsType = MessageHelper.createMessage(DescribeVpcsType.class, action.info.getEffectiveUserId());
        describeVpcsType.getFilterSet( ).add( Filter.filter( "vpc-id", action.properties.getVpcId( ) ) );
        DescribeVpcsResponseType describeVpcsResponseType = AsyncRequests.sendSync(configuration, describeVpcsType);
        if (describeVpcsResponseType.getVpcSet() == null ||
            describeVpcsResponseType.getVpcSet().getItem() == null ||
            describeVpcsResponseType.getVpcSet().getItem().isEmpty()) {
          return action; // already deleted
        }
        // check subnet (return if gone)
        DescribeSubnetsType describeSubnetsType = MessageHelper.createMessage(DescribeSubnetsType.class, action.info.getEffectiveUserId());
        describeSubnetsType.getFilterSet( ).add( Filter.filter( "subnet-id", action.info.getPhysicalResourceId( ) ) );
        DescribeSubnetsResponseType describeSubnetsResponseType = AsyncRequests.sendSync( configuration, describeSubnetsType);
        if (describeSubnetsResponseType.getSubnetSet() == null ||
            describeSubnetsResponseType.getSubnetSet().getItem() == null ||
            describeSubnetsResponseType.getSubnetSet().getItem().isEmpty()) {
          return action; // already deleted
        }
        DeleteSubnetType deleteSubnetType = MessageHelper.createMessage(DeleteSubnetType.class, action.info.getEffectiveUserId());
        deleteSubnetType.setSubnetId(action.info.getPhysicalResourceId());
        try {
          AsyncRequests.<DeleteSubnetType,DeleteSubnetResponseType> sendSync(configuration, deleteSubnetType);
        } catch (Exception ex) {
          throw new RetryAfterConditionCheckFailedException(AsyncExceptions.asWebServiceErrorMessage(ex,"Error deleting subnet"));
        }
        return action;
      }
    };

    @Nullable
    @Override
    public Integer getTimeout( ) {
      return SUBNET_MAX_DELETE_RETRY_SECS;
    }
  }

  private enum UpdateNoInterruptionSteps implements UpdateStep {
    UPDATE_ATTRIBUTES {
      @Override
      public ResourceAction perform(ResourceAction oldResourceAction, ResourceAction newResourceAction) throws Exception {
        AWSEC2SubnetResourceAction oldAction = (AWSEC2SubnetResourceAction) oldResourceAction;
        AWSEC2SubnetResourceAction newAction = (AWSEC2SubnetResourceAction) newResourceAction;
        ServiceConfiguration configuration = Topology.lookup(Compute.class);
        final ModifySubnetAttributeType modifySubnet =
        MessageHelper.createMessage(ModifySubnetAttributeType.class, newAction.info.getEffectiveUserId());
        final AttributeBooleanValueType value = new AttributeBooleanValueType( );
        value.setValue( Boolean.TRUE.equals(newAction.properties.getMapPublicIpOnLaunch( )));
        modifySubnet.setMapPublicIpOnLaunch( value );
        modifySubnet.setSubnetId( newAction.info.getPhysicalResourceId( ) );
        AsyncRequests.<ModifySubnetAttributeType,ModifySubnetAttributeResponseType>sendSync( configuration, modifySubnet );
        return newAction;
      }
    },
    UPDATE_TAGS {
      @Override
      public ResourceAction perform(ResourceAction oldResourceAction, ResourceAction newResourceAction) throws Exception {
        AWSEC2SubnetResourceAction oldAction = (AWSEC2SubnetResourceAction) oldResourceAction;
        AWSEC2SubnetResourceAction newAction = (AWSEC2SubnetResourceAction) newResourceAction;
        ServiceConfiguration configuration = Topology.lookup(Compute.class);
        DescribeTagsType describeTagsType = MessageHelper.createMessage(DescribeTagsType.class, newAction.info.getEffectiveUserId());
        describeTagsType.setFilterSet(Lists.newArrayList(Filter.filter("resource-id", newAction.info.getPhysicalResourceId())));
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
    properties = (AWSEC2SubnetProperties) resourceProperties;
  }

  @Override
  public ResourceInfo getResourceInfo() {
    return info;
  }

  @Override
  public void setResourceInfo(ResourceInfo resourceInfo) {
    info = (AWSEC2SubnetResourceInfo) resourceInfo;
  }



}


