/*************************************************************************
 * Copyright 2009-2016 Ent. Services Development Corporation LP
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
import com.eucalyptus.cloudformation.resources.EC2Helper;
import com.eucalyptus.cloudformation.resources.ResourceAction;
import com.eucalyptus.cloudformation.resources.ResourceInfo;
import com.eucalyptus.cloudformation.resources.ResourceProperties;
import com.eucalyptus.cloudformation.resources.standard.TagHelper;
import com.eucalyptus.cloudformation.resources.standard.info.AWSEC2NatGatewayResourceInfo;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.AWSEC2NatGatewayProperties;
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
import com.eucalyptus.compute.common.CloudFilters;
import com.eucalyptus.compute.common.Compute;
import com.eucalyptus.compute.common.CreateNatGatewayResponseType;
import com.eucalyptus.compute.common.CreateNatGatewayType;
import com.eucalyptus.compute.common.CreateTagsResponseType;
import com.eucalyptus.compute.common.CreateTagsType;
import com.eucalyptus.compute.common.DeleteNatGatewayType;
import com.eucalyptus.compute.common.DeleteTagsResponseType;
import com.eucalyptus.compute.common.DeleteTagsType;
import com.eucalyptus.compute.common.DescribeNatGatewaysResponseType;
import com.eucalyptus.compute.common.DescribeNatGatewaysType;
import com.eucalyptus.compute.common.DescribeTagsResponseType;
import com.eucalyptus.compute.common.DescribeTagsType;
import com.eucalyptus.compute.common.TagInfo;
import com.eucalyptus.configurable.ConfigurableClass;
import com.eucalyptus.configurable.ConfigurableField;
import com.eucalyptus.util.async.AsyncExceptions;
import com.eucalyptus.util.async.AsyncRequests;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.apache.log4j.Logger;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static com.eucalyptus.util.async.AsyncExceptions.asWebServiceErrorMessage;


/**
 * Created by ethomas on 2/3/14.
 */
@ConfigurableClass( root = "cloudformation", description = "Parameters controlling cloud formation")
public class AWSEC2NatGatewayResourceAction extends StepBasedResourceAction {
  private static final Logger LOG = Logger.getLogger(AWSEC2NatGatewayResourceAction.class);
  private AWSEC2NatGatewayProperties properties = new AWSEC2NatGatewayProperties();
  private AWSEC2NatGatewayResourceInfo info = new AWSEC2NatGatewayResourceInfo();

  @ConfigurableField(initial = "300", description = "The amount of time (in seconds) to wait for a nat gateway to be available after create)")
  public static volatile Integer NAT_GATEWAY_AVAILABLE_MAX_CREATE_RETRY_SECS = 300;

  public AWSEC2NatGatewayResourceAction() {
    // all updates are replacement
    super(fromEnum(CreateSteps.class), fromEnum(DeleteSteps.class), fromUpdateEnum(UpdateNoInterruptionSteps.class), null);
  }
  @Override
  public UpdateType getUpdateType(ResourceAction resourceAction, boolean stackTagsChanged) {
    UpdateType updateType = info.supportsTags() && stackTagsChanged ? UpdateType.NO_INTERRUPTION : UpdateType.NONE;
    AWSEC2NatGatewayResourceAction otherAction = (AWSEC2NatGatewayResourceAction) resourceAction;
    if (!Objects.equals(properties.getAllocationId(), otherAction.properties.getAllocationId())) {
      updateType = UpdateType.max(updateType, UpdateType.NEEDS_REPLACEMENT);
    }
    if (!Objects.equals(properties.getSubnetId(), otherAction.properties.getSubnetId())) {
      updateType = UpdateType.max(updateType, UpdateType.NEEDS_REPLACEMENT);
    }
    if (!Objects.equals(properties.getTags(), otherAction.properties.getTags())) {
      updateType = UpdateType.max(updateType, UpdateType.NO_INTERRUPTION);
    }
    return updateType;
  }

  private enum CreateSteps implements Step {
    CREATE_NAT_GATEWAY {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        AWSEC2NatGatewayResourceAction action = (AWSEC2NatGatewayResourceAction) resourceAction;
        ServiceConfiguration configuration = Topology.lookup(Compute.class);
        CreateNatGatewayType createNatGatewayType = MessageHelper.createMessage(CreateNatGatewayType.class, action.info.getEffectiveUserId());
        createNatGatewayType.setAllocationId(action.properties.getAllocationId());
        createNatGatewayType.setSubnetId(action.properties.getSubnetId());
        CreateNatGatewayResponseType createNatGatewayResponseType = AsyncRequests.sendSync(configuration, createNatGatewayType);
        action.info.setPhysicalResourceId(createNatGatewayResponseType.getNatGateway().getNatGatewayId());
        action.info.setCreatedEnoughToDelete(true);
        action.info.setReferenceValueJson(JsonHelper.getStringFromJsonNode(new TextNode(action.info.getPhysicalResourceId())));
       return action;
      }
    },
    CREATE_TAGS {
      @Override
      public ResourceAction perform(final ResourceAction resourceAction) throws Exception {
        AWSEC2NatGatewayResourceAction action = (AWSEC2NatGatewayResourceAction) resourceAction;
        ServiceConfiguration configuration = Topology.lookup( Compute.class );
        // Create 'system' tags as admin user
        String effectiveAdminUserId = action.info.getAccountId( );
        CreateTagsType createSystemTagsType = MessageHelper.createPrivilegedMessage( CreateTagsType.class, effectiveAdminUserId );
        createSystemTagsType.setResourcesSet( Lists.newArrayList( action.info.getPhysicalResourceId( ) ) );
        createSystemTagsType.setTagSet( EC2Helper.createTagSet( TagHelper.getEC2SystemTags( action.info, action.getStackEntity( ) ) ) );
        AsyncRequests.<CreateTagsType, CreateTagsResponseType>sendSync( configuration, createSystemTagsType );
        // Create non-system tags as regular user
        List<EC2Tag> tags = TagHelper.getEC2StackTags( action.getStackEntity( ) );
        if ( action.properties.getTags( ) != null && !action.properties.getTags( ).isEmpty( ) ) {
          TagHelper.checkReservedEC2TemplateTags( action.properties.getTags( ) );
          tags.addAll( action.properties.getTags( ) );
        }
        if ( !tags.isEmpty( ) ) {
          CreateTagsType createTagsType = MessageHelper.createMessage( CreateTagsType.class, action.info.getEffectiveUserId( ) );
          createTagsType.setResourcesSet( Lists.newArrayList( action.info.getPhysicalResourceId( ) ) );
          createTagsType.setTagSet( EC2Helper.createTagSet( tags ) );
          AsyncRequests.<CreateTagsType, CreateTagsResponseType>sendSync( configuration, createTagsType );
        }
        return action;
      }
    },
    VERIFY_AVAILABLE {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        AWSEC2NatGatewayResourceAction action = (AWSEC2NatGatewayResourceAction) resourceAction;
        ServiceConfiguration configuration = Topology.lookup(Compute.class);
        DescribeNatGatewaysType describeNatGatewaysType = MessageHelper.createMessage(DescribeNatGatewaysType.class, action.info.getEffectiveUserId());
        describeNatGatewaysType.getFilterSet( ).add( CloudFilters.filter( "nat-gateway-id", action.info.getPhysicalResourceId( ) ) );
        DescribeNatGatewaysResponseType describeNatGatewaysResponseType;
        try {
          describeNatGatewaysResponseType = AsyncRequests.sendSync( configuration, describeNatGatewaysType);
        } catch ( final Exception e ) {
          throw new ValidationErrorException("Error describing nat gateway " + action.info.getPhysicalResourceId() + ":" + asWebServiceErrorMessage( e, e.getMessage() ) );
        }
        if (describeNatGatewaysResponseType.getNatGatewaySet() == null || describeNatGatewaysResponseType.getNatGatewaySet().getItem() == null ||
          describeNatGatewaysResponseType.getNatGatewaySet().getItem().size() == 0) {
          throw new RetryAfterConditionCheckFailedException("Nat gateway " + action.info.getPhysicalResourceId() + " not yet available");
        }
        if (!"available".equals(describeNatGatewaysResponseType.getNatGatewaySet().getItem().get(0).getState())) {
          throw new RetryAfterConditionCheckFailedException("Nat gateway " + action.info.getPhysicalResourceId() + " not yet available");
        }
        return action;
      }

      @Override
      public Integer getTimeout() {
        return NAT_GATEWAY_AVAILABLE_MAX_CREATE_RETRY_SECS;
      }
    }
  }

  private enum DeleteSteps implements Step {
    DELETE_NAT_GATEWAY {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        AWSEC2NatGatewayResourceAction action = (AWSEC2NatGatewayResourceAction) resourceAction;
        ServiceConfiguration configuration = Topology.lookup(Compute.class);
        if (!Boolean.TRUE.equals(action.info.getCreatedEnoughToDelete())) return action;
        DeleteNatGatewayType deleteNatGatewayType = MessageHelper.createMessage(DeleteNatGatewayType.class, action.info.getEffectiveUserId());
        deleteNatGatewayType.setNatGatewayId(action.info.getPhysicalResourceId());
        try {
          AsyncRequests.sendSync( configuration, deleteNatGatewayType );
        } catch ( final Exception e ) {
          if ( !AsyncExceptions.isWebServiceErrorCode( e, "NatGatewayNotFound" ) ) {
            throw e;
          }
        }
        return action;
      }
    }
  }

  private enum UpdateNoInterruptionSteps implements UpdateStep {
    UPDATE_TAGS {
      @Override
      public ResourceAction perform(ResourceAction oldResourceAction, ResourceAction newResourceAction) throws Exception {
        AWSEC2NatGatewayResourceAction oldAction = (AWSEC2NatGatewayResourceAction) oldResourceAction;
        AWSEC2NatGatewayResourceAction newAction = (AWSEC2NatGatewayResourceAction) newResourceAction;
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
    properties = (AWSEC2NatGatewayProperties) resourceProperties;
  }

  @Override
  public ResourceInfo getResourceInfo() {
    return info;
  }

  @Override
  public void setResourceInfo(ResourceInfo resourceInfo) {
    info = (AWSEC2NatGatewayResourceInfo) resourceInfo;
  }

}


