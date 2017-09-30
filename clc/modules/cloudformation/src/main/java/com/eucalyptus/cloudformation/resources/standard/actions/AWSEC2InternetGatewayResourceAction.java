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


import com.eucalyptus.cloudformation.resources.EC2Helper;
import com.eucalyptus.cloudformation.resources.ResourceAction;
import com.eucalyptus.cloudformation.resources.ResourceInfo;
import com.eucalyptus.cloudformation.resources.ResourceProperties;
import com.eucalyptus.cloudformation.resources.standard.TagHelper;
import com.eucalyptus.cloudformation.resources.standard.info.AWSEC2InternetGatewayResourceInfo;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.AWSEC2InternetGatewayProperties;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.EC2Tag;
import com.eucalyptus.cloudformation.template.JsonHelper;
import com.eucalyptus.cloudformation.util.MessageHelper;
import com.eucalyptus.cloudformation.workflow.steps.Step;
import com.eucalyptus.cloudformation.workflow.steps.StepBasedResourceAction;
import com.eucalyptus.cloudformation.workflow.steps.UpdateStep;
import com.eucalyptus.cloudformation.workflow.updateinfo.UpdateType;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.Topology;
import com.eucalyptus.compute.common.CloudFilters;
import com.eucalyptus.compute.common.Compute;
import com.eucalyptus.compute.common.CreateInternetGatewayResponseType;
import com.eucalyptus.compute.common.CreateInternetGatewayType;
import com.eucalyptus.compute.common.CreateTagsResponseType;
import com.eucalyptus.compute.common.CreateTagsType;
import com.eucalyptus.compute.common.DeleteInternetGatewayResponseType;
import com.eucalyptus.compute.common.DeleteInternetGatewayType;
import com.eucalyptus.compute.common.DeleteTagsResponseType;
import com.eucalyptus.compute.common.DeleteTagsType;
import com.eucalyptus.compute.common.DescribeInternetGatewaysResponseType;
import com.eucalyptus.compute.common.DescribeInternetGatewaysType;
import com.eucalyptus.compute.common.DescribeTagsResponseType;
import com.eucalyptus.compute.common.DescribeTagsType;
import com.eucalyptus.compute.common.TagInfo;
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
public class AWSEC2InternetGatewayResourceAction extends StepBasedResourceAction {

  private AWSEC2InternetGatewayProperties properties = new AWSEC2InternetGatewayProperties();
  private AWSEC2InternetGatewayResourceInfo info = new AWSEC2InternetGatewayResourceInfo();

  public AWSEC2InternetGatewayResourceAction() {
    super(fromEnum(CreateSteps.class), fromEnum(DeleteSteps.class), fromUpdateEnum(UpdateNoInterruptionSteps.class), null);
  }
  @Override
  public UpdateType getUpdateType(ResourceAction resourceAction, boolean stackTagsChanged) {
    UpdateType updateType = info.supportsTags() && stackTagsChanged ? UpdateType.NO_INTERRUPTION : UpdateType.NONE;
    AWSEC2InternetGatewayResourceAction otherAction = (AWSEC2InternetGatewayResourceAction) resourceAction;
    if (!Objects.equals(properties.getTags(), otherAction.properties.getTags())) {
      updateType = UpdateType.max(updateType, UpdateType.NO_INTERRUPTION);
    }
    return updateType;
  }

  private enum CreateSteps implements Step {
    CREATE_GATEWAY {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        AWSEC2InternetGatewayResourceAction action = (AWSEC2InternetGatewayResourceAction) resourceAction;
        ServiceConfiguration configuration = Topology.lookup(Compute.class);
        CreateInternetGatewayType createInternetGatewayType = MessageHelper.createMessage(CreateInternetGatewayType.class, action.info.getEffectiveUserId());
        CreateInternetGatewayResponseType createInternetGatewayResponseType = AsyncRequests.<CreateInternetGatewayType,CreateInternetGatewayResponseType> sendSync(configuration, createInternetGatewayType);
        action.info.setPhysicalResourceId(createInternetGatewayResponseType.getInternetGateway().getInternetGatewayId());
        action.info.setCreatedEnoughToDelete(true);
        action.info.setReferenceValueJson(JsonHelper.getStringFromJsonNode(new TextNode(action.info.getPhysicalResourceId())));
        return action;
      }
    },
    CREATE_TAGS {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        AWSEC2InternetGatewayResourceAction action = (AWSEC2InternetGatewayResourceAction) resourceAction;
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
    };

    @Override
    public Integer getTimeout( ) {
      return null;
    }
  }

  private enum DeleteSteps implements Step {
    DELETE_GATEWAY {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        AWSEC2InternetGatewayResourceAction action = (AWSEC2InternetGatewayResourceAction) resourceAction;
        ServiceConfiguration configuration = Topology.lookup(Compute.class);
        if (!Boolean.TRUE.equals(action.info.getCreatedEnoughToDelete())) return action;
        // Check gateway (return if gone)
        DescribeInternetGatewaysType describeInternetGatewaysType = MessageHelper.createMessage(DescribeInternetGatewaysType.class, action.info.getEffectiveUserId());
        describeInternetGatewaysType.getFilterSet( ).add( CloudFilters.filter( "internet-gateway-id", action.info.getPhysicalResourceId() ) );
        DescribeInternetGatewaysResponseType describeInternetGatewaysResponseType = AsyncRequests.sendSync(configuration, describeInternetGatewaysType);
        if (describeInternetGatewaysResponseType.getInternetGatewaySet() == null ||
            describeInternetGatewaysResponseType.getInternetGatewaySet().getItem() == null ||
            describeInternetGatewaysResponseType.getInternetGatewaySet().getItem().isEmpty()) {
          return action; // already deleted
        }
        DeleteInternetGatewayType deleteInternetGatewayType = MessageHelper.createMessage(DeleteInternetGatewayType.class, action.info.getEffectiveUserId());
        deleteInternetGatewayType.setInternetGatewayId(action.info.getPhysicalResourceId());
        AsyncRequests.<DeleteInternetGatewayType,DeleteInternetGatewayResponseType> sendSync(configuration, deleteInternetGatewayType);
        return action;
      }
    };

    @Override
    public Integer getTimeout( ) {
      return null;
    }
  }

  private enum UpdateNoInterruptionSteps implements UpdateStep {
    UPDATE_TAGS {
      @Override
      public ResourceAction perform(ResourceAction oldResourceAction, ResourceAction newResourceAction) throws Exception {
        AWSEC2InternetGatewayResourceAction oldAction = (AWSEC2InternetGatewayResourceAction) oldResourceAction;
        AWSEC2InternetGatewayResourceAction newAction = (AWSEC2InternetGatewayResourceAction) newResourceAction;
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
    properties = (AWSEC2InternetGatewayProperties) resourceProperties;
  }

  @Override
  public ResourceInfo getResourceInfo() {
    return info;
  }

  @Override
  public void setResourceInfo(ResourceInfo resourceInfo) {
    info = (AWSEC2InternetGatewayResourceInfo) resourceInfo;
  }



}



