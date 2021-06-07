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
import com.eucalyptus.cloudformation.entity.StackResourceEntity;
import com.eucalyptus.cloudformation.entity.StackResourceEntityManager;
import com.eucalyptus.cloudformation.resources.EC2Helper;
import com.eucalyptus.cloudformation.resources.ResourceAction;
import com.eucalyptus.cloudformation.resources.ResourceInfo;
import com.eucalyptus.cloudformation.resources.ResourceProperties;
import com.eucalyptus.cloudformation.resources.standard.TagHelper;
import com.eucalyptus.cloudformation.resources.standard.info.AWSEC2EIPResourceInfo;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.AWSEC2EIPProperties;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.EC2Tag;
import com.eucalyptus.cloudformation.template.JsonHelper;
import com.eucalyptus.cloudformation.util.MessageHelper;
import com.eucalyptus.cloudformation.workflow.steps.Step;
import com.eucalyptus.cloudformation.workflow.steps.StepBasedResourceAction;
import com.eucalyptus.cloudformation.workflow.steps.UpdateStep;
import com.eucalyptus.cloudformation.workflow.updateinfo.UpdateType;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.Topology;
import com.eucalyptus.compute.common.AddressInfoType;
import com.eucalyptus.compute.common.AllocateAddressResponseType;
import com.eucalyptus.compute.common.AllocateAddressType;
import com.eucalyptus.compute.common.AssociateAddressResponseType;
import com.eucalyptus.compute.common.AssociateAddressType;
import com.eucalyptus.compute.common.CloudFilters;
import com.eucalyptus.compute.common.Compute;
import com.eucalyptus.compute.common.ComputeApi;
import com.eucalyptus.compute.common.CreateTagsType;
import com.eucalyptus.compute.common.DeleteTagsType;
import com.eucalyptus.compute.common.DescribeAddressesResponseType;
import com.eucalyptus.compute.common.DescribeAddressesType;
import com.eucalyptus.compute.common.DescribeInstancesResponseType;
import com.eucalyptus.compute.common.DescribeInstancesType;
import com.eucalyptus.compute.common.DescribeTagsResponseType;
import com.eucalyptus.compute.common.DescribeTagsType;
import com.eucalyptus.compute.common.DisassociateAddressResponseType;
import com.eucalyptus.compute.common.DisassociateAddressType;
import com.eucalyptus.compute.common.ReleaseAddressResponseType;
import com.eucalyptus.compute.common.ReleaseAddressType;
import com.eucalyptus.compute.common.TagInfo;
import com.eucalyptus.util.async.AsyncProxy;
import com.eucalyptus.util.async.AsyncRequests;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;


/**
 * Created by ethomas on 2/3/14.
 */
public class AWSEC2EIPResourceAction extends StepBasedResourceAction {
  private AWSEC2EIPProperties properties = new AWSEC2EIPProperties();
  private AWSEC2EIPResourceInfo info = new AWSEC2EIPResourceInfo();

  public AWSEC2EIPResourceAction() {
    super(fromEnum(CreateSteps.class), fromEnum(DeleteSteps.class), fromUpdateEnum(UpdateNoInterruptionSteps.class), null);
  }

  @Override
  public boolean mustCheckUpdateTypeEvenIfNoPropertiesChanged() {
    return true;
  }

  @Override
  public UpdateType getUpdateType(ResourceAction resourceAction, boolean stackTagsChanged) {
    UpdateType updateType = info.supportsTags() && stackTagsChanged ? UpdateType.NO_INTERRUPTION : UpdateType.NONE;
    AWSEC2EIPResourceAction otherAction = (AWSEC2EIPResourceAction) resourceAction;
    if (!Objects.equals(properties.getDomain(), otherAction.properties.getDomain())) {
      updateType = UpdateType.max(updateType, UpdateType.NEEDS_REPLACEMENT);
    }
    if (!Objects.equals(properties.getInstanceId(), otherAction.properties.getInstanceId())) {
      updateType = UpdateType.max(updateType, UpdateType.NO_INTERRUPTION);
    }
    // finally update if the 'instance' we are associated was either NO_INTERRUPTION or SOME_INTERRUPTION updated
    if (Objects.equals(properties.getInstanceId(), otherAction.properties.getInstanceId()) ) {
      if (wasInstanceUpdated(otherAction)) {
        updateType = UpdateType.max(updateType, UpdateType.NO_INTERRUPTION);
      }
    }
    return updateType;
  }

  private boolean wasInstanceUpdated(AWSEC2EIPResourceAction action) {
    if (action.properties.getInstanceId() == null) {
      return false;
    }
    StackResourceEntity stackResourceEntity = StackResourceEntityManager.getStackResourceByPhysicalResourceId(action.stackEntity.getStackId(), action.stackEntity.getAccountId(), action.properties.getInstanceId(), action.stackEntity.getStackVersion());
    return  (stackResourceEntity != null && stackResourceEntity.getUpdateType() != null &&
      (stackResourceEntity.getUpdateType().equals(UpdateType.NO_INTERRUPTION.toString()) || stackResourceEntity.getUpdateType().equals(UpdateType.SOME_INTERRUPTION.toString())));
  }

  private enum CreateSteps implements Step {
    CREATE_EIP__ADDRESS {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        AWSEC2EIPResourceAction action = (AWSEC2EIPResourceAction) resourceAction;
        ServiceConfiguration configuration = Topology.lookup(Compute.class);
        AllocateAddressType allocateAddressType = MessageHelper.createMessage(AllocateAddressType.class, action.info.getEffectiveUserId());
        if (action.properties.getDomain() != null && !"vpc".equals(action.properties.getDomain())) {
          throw new ValidationErrorException("vpc is the only supported value for Domain");
        }
        if (action.properties.getDomain() != null) {
          allocateAddressType.setDomain(action.properties.getDomain());
        }
        AllocateAddressResponseType allocateAddressResponseType = AsyncRequests.sendSync(configuration, allocateAddressType);
        String publicIp = allocateAddressResponseType.getPublicIp();
        action.info.setPhysicalResourceId(publicIp);
        action.info.setCreatedEnoughToDelete(true);
        action.info.setReferenceValueJson(JsonHelper.getStringFromJsonNode(new TextNode(action.info.getPhysicalResourceId())));
        if (action.properties.getDomain() != null) {
          action.info.setAllocationId(JsonHelper.getStringFromJsonNode(new TextNode(allocateAddressResponseType.getAllocationId())));
        }
        return action;
      }
    },
    CREATE_TAGS {
      @Override
      public ResourceAction perform(final ResourceAction resourceAction) throws Exception {
        final AWSEC2EIPResourceAction action = (AWSEC2EIPResourceAction) resourceAction;
        if ( action.info.getAllocationId( ) != null ) {
          final String allocationId = JsonHelper.getJsonNodeFromString(action.info.getAllocationId()).asText();
          // Create 'system' tags as admin user
          final String effectiveAdminUserId = action.info.getAccountId( );
          final CreateTagsType createSystemTagsType = MessageHelper.createPrivilegedMessage( CreateTagsType.class, effectiveAdminUserId );
          createSystemTagsType.setResourcesSet( Lists.newArrayList( allocationId ) );
          createSystemTagsType.setTagSet( EC2Helper.createTagSet( TagHelper.getEC2SystemTags( action.info, action.getStackEntity( ) ) ) );
          AsyncProxy.client( ComputeApi.class, Function.identity( ) ).createTags(
              createSystemTagsType
          );
          // Create non-system tags as regular user
          final List<EC2Tag> tags = TagHelper.getEC2StackTags( action.getStackEntity( ) );
          if ( action.properties.getTags( ) != null && !action.properties.getTags( ).isEmpty( ) ) {
            TagHelper.checkReservedEC2TemplateTags( action.properties.getTags( ) );
            tags.addAll( action.properties.getTags( ) );
          }
          if ( !tags.isEmpty( ) ) {
            final CreateTagsType createTagsType = MessageHelper.createMessage( CreateTagsType.class, action.info.getEffectiveUserId( ) );
            createTagsType.setResourcesSet( Lists.newArrayList( allocationId ) );
            createTagsType.setTagSet( EC2Helper.createTagSet( tags ) );
            AsyncProxy.client( ComputeApi.class, Function.identity( ) ).createTags(
                createTagsType
            );
          }
        }
        return action;
      }
    },
    ATTACH_TO_INSTANCE {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        AWSEC2EIPResourceAction action = (AWSEC2EIPResourceAction) resourceAction;
        ServiceConfiguration configuration = Topology.lookup(Compute.class);
        if (action.properties.getInstanceId() != null) {
          DescribeInstancesType describeInstancesType = MessageHelper.createMessage(DescribeInstancesType.class, action.info.getEffectiveUserId());
          describeInstancesType.getFilterSet( ).add( CloudFilters.filter( "instance-id", action.properties.getInstanceId( ) ) );
          DescribeInstancesResponseType describeInstancesResponseType = AsyncRequests.sendSync( configuration, describeInstancesType );
          if (describeInstancesResponseType.getReservationSet() == null || describeInstancesResponseType.getReservationSet().isEmpty()) {
            throw new ValidationErrorException("No such instance " + action.properties.getInstanceId());
          }
          AssociateAddressType associateAddressType = MessageHelper.createMessage(AssociateAddressType.class, action.info.getEffectiveUserId());
          if (action.properties.getDomain() != null) {
            associateAddressType.setAllocationId(JsonHelper.getJsonNodeFromString(action.info.getAllocationId()).asText());
          } else {
            associateAddressType.setPublicIp(action.info.getPhysicalResourceId());
          }
          associateAddressType.setInstanceId(action.properties.getInstanceId());
          AsyncRequests.<AssociateAddressType, AssociateAddressResponseType> sendSync(configuration, associateAddressType);

          // Update the instance info
          if (action.properties.getInstanceId() != null) {
            EC2Helper.refreshInstanceAttributes(action.getStackEntity(), action.properties.getInstanceId(), action.info.getEffectiveUserId(), action.getStackEntity().getStackVersion());
          }

        }
        return action;
      }
    }
  }

  private enum DeleteSteps implements Step {
    DETACH_FROM_INSTANCE {
      @Override
      public ResourceAction perform( final ResourceAction resourceAction ) throws Exception {
        final AWSEC2EIPResourceAction action = (AWSEC2EIPResourceAction) resourceAction;
        if (!Boolean.TRUE.equals(action.info.getCreatedEnoughToDelete())) return action;
        if ( action.properties.getInstanceId( ) != null ) {
          final ServiceConfiguration configuration = Topology.lookup( Compute.class );
          final List<AddressInfoType> addresses = describeAddresses( action, configuration ).getAddressesSet( );
          if ( addresses != null && !addresses.isEmpty( ) &&
              ( addresses.get( 0 ).getInstanceId( ) != null || addresses.get( 0 ).getNetworkInterfaceId( ) != null ) ) {
            final DisassociateAddressType disassociateAddressType =
                MessageHelper.createMessage( DisassociateAddressType.class, action.info.getEffectiveUserId( ) );
            if ( action.properties.getDomain( ) != null ) {
              disassociateAddressType.setAssociationId( addresses.get( 0 ).getAssociationId( ) );
            } else {
              disassociateAddressType.setPublicIp( action.info.getPhysicalResourceId( ) );
            }
            AsyncRequests.<DisassociateAddressType, DisassociateAddressResponseType> sendSync(
                configuration,
                disassociateAddressType
            );
          }

          // Update the instance info
          EC2Helper.refreshInstanceAttributes(
              action.getStackEntity( ),
              action.properties.getInstanceId( ),
              action.info.getEffectiveUserId( ),
              action.getStackEntity().getStackVersion()
          );
        }
        return action;
      }
    },
    DELETE_EIP_ADDRESS {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        AWSEC2EIPResourceAction action = (AWSEC2EIPResourceAction) resourceAction;
        ServiceConfiguration configuration = Topology.lookup(Compute.class);
        if (!Boolean.TRUE.equals(action.info.getCreatedEnoughToDelete())) return action;
        final DescribeAddressesResponseType describeAddressesResponseType = describeAddresses( action, configuration );
        if (describeAddressesResponseType.getAddressesSet() != null && !describeAddressesResponseType.getAddressesSet().isEmpty()) {
          ReleaseAddressType releaseAddressType = MessageHelper.createMessage(ReleaseAddressType.class, action.info.getEffectiveUserId());
          if (action.properties.getDomain() != null) {
            releaseAddressType.setAllocationId(JsonHelper.getJsonNodeFromString(action.info.getAllocationId()).asText());
          } else {
            releaseAddressType.setPublicIp(action.info.getPhysicalResourceId());
          }
          AsyncRequests.<ReleaseAddressType, ReleaseAddressResponseType> sendSync(configuration, releaseAddressType);
        }
        return action;
      }
    }
  }

  @Override
  public ResourceProperties getResourceProperties() {
    return properties;
  }

  @Override
  public void setResourceProperties(ResourceProperties resourceProperties) {
    properties = (AWSEC2EIPProperties) resourceProperties;
  }

  @Override
  public ResourceInfo getResourceInfo() {
    return info;
  }

  @Override
  public void setResourceInfo(ResourceInfo resourceInfo) {
    info = (AWSEC2EIPResourceInfo) resourceInfo;
  }

  private enum UpdateNoInterruptionSteps implements UpdateStep {
    UPDATE_INSTANCE_ATTACHMENT {
      @Override
      public ResourceAction perform(ResourceAction oldResourceAction, ResourceAction newResourceAction) throws Exception {
        AWSEC2EIPResourceAction oldAction = (AWSEC2EIPResourceAction) oldResourceAction;
        AWSEC2EIPResourceAction newAction = (AWSEC2EIPResourceAction) newResourceAction;
        ServiceConfiguration configuration = Topology.lookup(Compute.class);

        // In theory newAction and oldAction should have same items for allocationId, domain, and ip address (otherwise we would be in replacement)
        // As such we will use the "new action" to see if there is an instance association.
        String oldInstanceId = null;
        final List<AddressInfoType> addresses = describeAddresses(newAction, configuration).getAddressesSet();
        if (addresses != null && !addresses.isEmpty() &&
          (addresses.get(0).getInstanceId() != null)) {
          oldInstanceId = addresses.get(0).getInstanceId();
        }


        if (!Objects.equals(oldInstanceId, newAction.properties.getInstanceId())) {
          if (newAction.properties.getInstanceId() != null) {
            DescribeInstancesType describeInstancesType = MessageHelper.createMessage(DescribeInstancesType.class, newAction.info.getEffectiveUserId());
            describeInstancesType.getFilterSet().add( CloudFilters.filter("instance-id", newAction.properties.getInstanceId()));
            DescribeInstancesResponseType describeInstancesResponseType = AsyncRequests.sendSync(configuration, describeInstancesType);
            if (describeInstancesResponseType.getReservationSet() == null || describeInstancesResponseType.getReservationSet().isEmpty()) {
              throw new ValidationErrorException("No such instance " + newAction.properties.getInstanceId());
            }
          }
          if (oldInstanceId != null) {
            // disassociate from old instance

            final DisassociateAddressType disassociateAddressType =
              MessageHelper.createMessage(DisassociateAddressType.class, newAction.info.getEffectiveUserId());
            if (newAction.properties.getDomain() != null) {
              disassociateAddressType.setAssociationId(addresses.get(0).getAssociationId());
            } else {
              disassociateAddressType.setPublicIp(newAction.info.getPhysicalResourceId());
            }
            AsyncRequests.<DisassociateAddressType, DisassociateAddressResponseType>sendSync(
              configuration,
              disassociateAddressType
            );

            // refresh old instance attributes... (TODO: see if this 'takes') (not sure if the stack version is correct
            EC2Helper.refreshInstanceAttributes(newAction.getStackEntity(), oldInstanceId,
              newAction.info.getEffectiveUserId(), newAction.getStackEntity().getStackVersion());
          }
          if (newAction.properties.getInstanceId() != null) {
            AssociateAddressType associateAddressType = MessageHelper.createMessage(AssociateAddressType.class, newAction.info.getEffectiveUserId());
            if (newAction.properties.getDomain() != null) {
              associateAddressType.setAllocationId(JsonHelper.getJsonNodeFromString(newAction.info.getAllocationId()).asText());
            } else {
              associateAddressType.setPublicIp(newAction.info.getPhysicalResourceId());
            }
            associateAddressType.setInstanceId(newAction.properties.getInstanceId());
            AsyncRequests.<AssociateAddressType, AssociateAddressResponseType> sendSync(configuration, associateAddressType);

            // Update the instance info
            if (newAction.properties.getInstanceId() != null) {
              EC2Helper.refreshInstanceAttributes(newAction.getStackEntity(), newAction.properties.getInstanceId(), newAction.info.getEffectiveUserId(), newAction.getStackEntity().getStackVersion());
            }
          }
        }
        return newAction;
      }
    },
    UPDATE_TAGS {
      @Override
      public ResourceAction perform(ResourceAction oldResourceAction, ResourceAction newResourceAction) throws Exception {
        final AWSEC2EIPResourceAction oldAction = (AWSEC2EIPResourceAction) oldResourceAction;
        final AWSEC2EIPResourceAction newAction = (AWSEC2EIPResourceAction) newResourceAction;
        if ( oldAction.info.getAllocationId( ) != null &&
            newAction.info.getAllocationId( ) != null ) {
          final ComputeApi compute = AsyncProxy.client( ComputeApi.class, Function.identity( ) );
          final String allocationId = JsonHelper.getJsonNodeFromString(newAction.info.getAllocationId()).asText();
          final DescribeTagsType describeTagsType = MessageHelper.createMessage(DescribeTagsType.class, newAction.info.getEffectiveUserId());
          describeTagsType.setFilterSet(Lists.newArrayList(CloudFilters.filter("resource-id", allocationId)));
          final DescribeTagsResponseType describeTagsResponseType = compute.describeTags(describeTagsType);
          final Set<EC2Tag> existingTags = Sets.newLinkedHashSet();
          if (describeTagsResponseType != null && describeTagsResponseType.getTagSet() != null) {
            for (final TagInfo tagInfo: describeTagsResponseType.getTagSet()) {
              existingTags.add( new EC2Tag( tagInfo.getKey( ), tagInfo.getValue() )  );
            }
          }
          final Set<EC2Tag> newTags = Sets.newLinkedHashSet();
          if (newAction.properties.getTags() != null) {
            newTags.addAll(newAction.properties.getTags());
          }
          final List<EC2Tag> newStackTags = TagHelper.getEC2StackTags(newAction.getStackEntity());
          if (newStackTags != null) {
            newTags.addAll(newStackTags);
          }
          TagHelper.checkReservedEC2TemplateTags(newTags);
          // add only 'new' tags
          final Set<EC2Tag> onlyNewTags = Sets.difference(newTags, existingTags);
          if (!onlyNewTags.isEmpty()) {
            final CreateTagsType createTagsType = MessageHelper.createMessage(CreateTagsType.class, newAction.info.getEffectiveUserId());
            createTagsType.setResourcesSet(Lists.newArrayList(allocationId));
            createTagsType.setTagSet(EC2Helper.createTagSet(onlyNewTags));
            compute.createTags(createTagsType);
          }
          //  Get old tags...
          final Set<EC2Tag> oldTags = Sets.newLinkedHashSet();
          if (oldAction.properties.getTags() != null) {
            oldTags.addAll(oldAction.properties.getTags());
          }
          final List<EC2Tag> oldStackTags = TagHelper.getEC2StackTags(oldAction.getStackEntity());
          if (oldStackTags != null) {
            oldTags.addAll(oldStackTags);
          }

          // remove only the old tags that are not new and that exist
          final Set<EC2Tag> tagsToRemove = Sets.intersection(oldTags, Sets.difference(existingTags, newTags));
          if (!tagsToRemove.isEmpty()) {
            final DeleteTagsType deleteTagsType = MessageHelper.createMessage(DeleteTagsType.class, newAction.info.getEffectiveUserId());
            deleteTagsType.setResourcesSet(Lists.newArrayList(allocationId));
            deleteTagsType.setTagSet(EC2Helper.deleteTagSet(tagsToRemove));
            compute.deleteTags(deleteTagsType);
          }
        }
        return newAction;
      }
    }
  }

  private static DescribeAddressesResponseType describeAddresses(
    final AWSEC2EIPResourceAction action,
    final ServiceConfiguration configuration
  ) throws Exception {
    final DescribeAddressesType describeAddressesType =
      MessageHelper.createMessage(DescribeAddressesType.class, action.info.getEffectiveUserId());
    if (action.properties.getDomain() != null) {
      describeAddressesType.setAllocationIds(
        Lists.newArrayList( JsonHelper.getJsonNodeFromString( action.info.getAllocationId( ) ).asText( ) )
      );
    } else {
      describeAddressesType.setPublicIpsSet( Lists.newArrayList( action.info.getPhysicalResourceId( ) ) );
    }
    return AsyncRequests.sendSync( configuration, describeAddressesType );
  }

}


