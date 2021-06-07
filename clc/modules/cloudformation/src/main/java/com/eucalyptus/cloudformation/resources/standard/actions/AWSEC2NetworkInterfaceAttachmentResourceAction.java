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
import com.eucalyptus.cloudformation.resources.ResourceAction;
import com.eucalyptus.cloudformation.resources.ResourceInfo;
import com.eucalyptus.cloudformation.resources.ResourceProperties;
import com.eucalyptus.cloudformation.resources.standard.info.AWSEC2NetworkInterfaceAttachmentResourceInfo;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.AWSEC2NetworkInterfaceAttachmentProperties;
import com.eucalyptus.cloudformation.template.JsonHelper;
import com.eucalyptus.cloudformation.util.MessageHelper;
import com.eucalyptus.cloudformation.workflow.RetryAfterConditionCheckFailedException;
import com.eucalyptus.cloudformation.workflow.steps.Step;
import com.eucalyptus.cloudformation.workflow.steps.StepBasedResourceAction;
import com.eucalyptus.cloudformation.workflow.steps.UpdateStep;
import com.eucalyptus.cloudformation.workflow.updateinfo.UpdateType;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.Topology;
import com.eucalyptus.compute.common.AttachNetworkInterfaceResponseType;
import com.eucalyptus.compute.common.AttachNetworkInterfaceType;
import com.eucalyptus.compute.common.Compute;
import com.eucalyptus.compute.common.DescribeInstancesResponseType;
import com.eucalyptus.compute.common.DescribeInstancesType;
import com.eucalyptus.compute.common.DescribeNetworkInterfacesResponseType;
import com.eucalyptus.compute.common.DescribeNetworkInterfacesType;
import com.eucalyptus.compute.common.DetachNetworkInterfaceResponseType;
import com.eucalyptus.compute.common.DetachNetworkInterfaceType;
import com.eucalyptus.compute.common.ModifyNetworkInterfaceAttachmentType;
import com.eucalyptus.compute.common.ModifyNetworkInterfaceAttributeResponseType;
import com.eucalyptus.compute.common.ModifyNetworkInterfaceAttributeType;
import com.eucalyptus.compute.common.NetworkInterfaceIdSetItemType;
import com.eucalyptus.compute.common.NetworkInterfaceIdSetType;
import com.eucalyptus.configurable.ConfigurableClass;
import com.eucalyptus.configurable.ConfigurableField;
import com.eucalyptus.util.async.AsyncExceptions;
import com.eucalyptus.util.async.AsyncExceptions.AsyncWebServiceError;
import com.eucalyptus.util.async.AsyncRequests;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Objects;

/**
* Created by ethomas on 2/3/14.
*/
@ConfigurableClass( root = "cloudformation", description = "Parameters controlling cloud formation")
public class AWSEC2NetworkInterfaceAttachmentResourceAction extends StepBasedResourceAction {
  private AWSEC2NetworkInterfaceAttachmentProperties properties = new AWSEC2NetworkInterfaceAttachmentProperties( );
  private AWSEC2NetworkInterfaceAttachmentResourceInfo info = new AWSEC2NetworkInterfaceAttachmentResourceInfo( );

  @ConfigurableField(initial = "300", description = "The amount of time (in seconds) to wait for a network interface to be attached during create or update)")
  public static volatile Integer NETWORK_INTERFACE_ATTACHMENT_MAX_CREATE_OR_UPDATE_RETRY_SECS = 300;

  @ConfigurableField(initial = "300", description = "The amount of time (in seconds) to wait for a network interface to detach during delete or update)")
  public static volatile Integer NETWORK_INTERFACE_DETACHMENT_MAX_DELETE_OR_UPDATE_RETRY_SECS = 300;

  public AWSEC2NetworkInterfaceAttachmentResourceAction() {
    super(fromEnum(CreateSteps.class), fromEnum(DeleteSteps.class), fromUpdateEnum(UpdateNoInterruptionSteps.class), null);
  }
  @Override
  public UpdateType getUpdateType(ResourceAction resourceAction, boolean stackTagsChanged) throws ValidationErrorException {
    UpdateType updateType = info.supportsTags() && stackTagsChanged ? UpdateType.NO_INTERRUPTION : UpdateType.NONE;
    AWSEC2NetworkInterfaceAttachmentResourceAction otherAction = (AWSEC2NetworkInterfaceAttachmentResourceAction) resourceAction;
    if (!Objects.equals(properties.getDeleteOnTermination(), otherAction.properties.getDeleteOnTermination())) {
      updateType = UpdateType.max(updateType, UpdateType.NO_INTERRUPTION);
    }
    if (!Objects.equals(properties.getDeviceIndex(), otherAction.properties.getDeviceIndex())) {
      updateType = UpdateType.max(updateType, UpdateType.NO_INTERRUPTION);
    }
    if (!Objects.equals(properties.getInstanceId(), otherAction.properties.getInstanceId())) {
      updateType = UpdateType.max(updateType, UpdateType.NO_INTERRUPTION);
    }
    if (!Objects.equals(properties.getNetworkInterfaceId(), otherAction.properties.getNetworkInterfaceId())) {
      updateType = UpdateType.max(updateType, UpdateType.NO_INTERRUPTION);
    }
    return updateType;
  }

  private enum CreateSteps implements Step {
    CREATE_NETWORK_INTERFACE_ATTACHMENT {
      @Override
      public ResourceAction perform( final ResourceAction resourceAction ) throws Exception {
        final AWSEC2NetworkInterfaceAttachmentResourceAction action =
            (AWSEC2NetworkInterfaceAttachmentResourceAction) resourceAction;
        final ServiceConfiguration configuration = Topology.lookup( Compute.class );
        return createNetworkInterfaceAttachment(action, configuration);
      }
    },
    SET_ATTRIBUTES {
      @Override
      public ResourceAction perform( final ResourceAction resourceAction ) throws Exception {
        final AWSEC2NetworkInterfaceAttachmentResourceAction action =
          (AWSEC2NetworkInterfaceAttachmentResourceAction) resourceAction;
        final ServiceConfiguration configuration = Topology.lookup( Compute.class );
        return setAttributes(action, configuration);
      }
    },
    WAIT_UNTIL_ATTACHED {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        AWSEC2NetworkInterfaceAttachmentResourceAction action = (AWSEC2NetworkInterfaceAttachmentResourceAction) resourceAction;
        ServiceConfiguration configuration = Topology.lookup(Compute.class);
        return waitUntilAttached(action, configuration);
      }

      @Override
      public Integer getTimeout() {
        return NETWORK_INTERFACE_ATTACHMENT_MAX_CREATE_OR_UPDATE_RETRY_SECS;
      }

    }
  }

  private static void throwNotAttachedMessage(String networkInterfaceId, String instanceId) throws RetryAfterConditionCheckFailedException {
    throw new RetryAfterConditionCheckFailedException("Network interface  " + networkInterfaceId + " not yet attached to instance " + instanceId);
  }
  private static ResourceAction waitUntilAttached(AWSEC2NetworkInterfaceAttachmentResourceAction action, ServiceConfiguration configuration) throws Exception {
    DescribeNetworkInterfacesType describeNetworkInterfacesType = MessageHelper.createMessage(DescribeNetworkInterfacesType.class, action.info.getEffectiveUserId());
    describeNetworkInterfacesType.setNetworkInterfaceIdSet(action.convertNetworkInterfaceIdSet(action.properties.getNetworkInterfaceId()));
    DescribeNetworkInterfacesResponseType describeNetworkInterfacesResponseType = AsyncRequests.<DescribeNetworkInterfacesType, DescribeNetworkInterfacesResponseType>sendSync(configuration, describeNetworkInterfacesType);
    if (describeNetworkInterfacesResponseType.getNetworkInterfaceSet() == null || describeNetworkInterfacesResponseType.getNetworkInterfaceSet().getItem() == null ||
      describeNetworkInterfacesResponseType.getNetworkInterfaceSet().getItem().isEmpty()) {
      throwNotAttachedMessage(action.properties.getNetworkInterfaceId(), action.properties.getInstanceId());
    }
    if (describeNetworkInterfacesResponseType.getNetworkInterfaceSet().getItem().get(0).getAttachment() == null ||
      !"attached".equals(describeNetworkInterfacesResponseType.getNetworkInterfaceSet().getItem().get(0).getAttachment().getStatus())) {
      throwNotAttachedMessage(action.properties.getNetworkInterfaceId(), action.properties.getInstanceId());
    }
    return action;
  }

  private static ResourceAction setAttributes(AWSEC2NetworkInterfaceAttachmentResourceAction action, ServiceConfiguration configuration) throws Exception {
    final ModifyNetworkInterfaceAttachmentType attachment = new ModifyNetworkInterfaceAttachmentType( );
    attachment.setAttachmentId( action.info.getPhysicalResourceId( ) );
    attachment.setDeleteOnTermination( action.properties.getDeleteOnTermination( ) != null
      ? action.properties.getDeleteOnTermination() : Boolean.TRUE);
    final ModifyNetworkInterfaceAttributeType modifyNetworkInterfaceAttributeType =
      MessageHelper.createMessage(ModifyNetworkInterfaceAttributeType.class, action.info.getEffectiveUserId());
    modifyNetworkInterfaceAttributeType.setNetworkInterfaceId( action.properties.getNetworkInterfaceId( ) );
    modifyNetworkInterfaceAttributeType.setAttachment( attachment );
    AsyncRequests.<ModifyNetworkInterfaceAttributeType, ModifyNetworkInterfaceAttributeResponseType>sendSync(
      configuration,
      modifyNetworkInterfaceAttributeType);
    return action;
  }

  private static ResourceAction createNetworkInterfaceAttachment(AWSEC2NetworkInterfaceAttachmentResourceAction action, ServiceConfiguration configuration) throws Exception {
    final AttachNetworkInterfaceType attachNetworkInterfaceType =
        MessageHelper.createMessage(AttachNetworkInterfaceType.class, action.info.getEffectiveUserId());
    attachNetworkInterfaceType.setDeviceIndex( action.properties.getDeviceIndex( ) );
    attachNetworkInterfaceType.setInstanceId( action.properties.getInstanceId( ) );
    attachNetworkInterfaceType.setNetworkInterfaceId( action.properties.getNetworkInterfaceId( ) );

    try {
      final AttachNetworkInterfaceResponseType attachNetworkInterfaceResponseType =
          AsyncRequests.sendSync(configuration, attachNetworkInterfaceType);
      final String attachmentId = attachNetworkInterfaceResponseType.getAttachmentId( );
      action.info.setPhysicalResourceId( attachmentId );
      action.info.setCreatedEnoughToDelete(true);
      action.info.setReferenceValueJson( JsonHelper.getStringFromJsonNode(new TextNode(attachmentId)) );
    } catch ( final Exception e ) {
      final Optional<AsyncWebServiceError> error = AsyncExceptions.asWebServiceError( e );
      if ( error.isPresent( ) ) switch ( Strings.nullToEmpty(error.get().getCode()) ) {
        case "InvalidInstanceID.NotFound":
          throw new ValidationErrorException( "No such instance " + action.properties.getInstanceId( ) );
        case "InvalidNetworkInterfaceID.NotFound":
          throw new ValidationErrorException( "No such network interface " + action.properties.getNetworkInterfaceId( ) );
        case "InvalidParameterValue":
          throw new ValidationErrorException( "Error attaching network interface - " + e.getMessage( ) );
      }
      throw e;
    }
    return action;
  }

  private enum DeleteSteps implements Step {
    DELETE_NETWORK_INTERFACE_ATTACHMENT {
      @Override
      public ResourceAction perform( final ResourceAction resourceAction ) throws Exception {
        final AWSEC2NetworkInterfaceAttachmentResourceAction action =
            (AWSEC2NetworkInterfaceAttachmentResourceAction) resourceAction;
        ServiceConfiguration configuration = Topology.lookup(Compute.class);
        if ( !Boolean.TRUE.equals(action.info.getCreatedEnoughToDelete()) ) return action;
        if (notCreatedOrNoInstanceOrNoNetworkInterface(action, configuration)) return action;
        return deleteNetworkInterfaceAttachment(action, configuration);
      }

    },
    WAIT_UNTIL_DETACHED {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        AWSEC2NetworkInterfaceAttachmentResourceAction action = (AWSEC2NetworkInterfaceAttachmentResourceAction) resourceAction;
        ServiceConfiguration configuration = Topology.lookup(Compute.class);
        if ( !Boolean.TRUE.equals(action.info.getCreatedEnoughToDelete()) ) return action;
        if (notCreatedOrNoInstanceOrNoNetworkInterface(action, configuration)) return action;
        return waitUntilDetached(action, configuration);
      }

      @Override
      public Integer getTimeout() {
        return NETWORK_INTERFACE_DETACHMENT_MAX_DELETE_OR_UPDATE_RETRY_SECS;
      }
    }
  }

  private enum UpdateNoInterruptionSteps implements UpdateStep {
    DELETE_NETWORK_INTERFACE_ATTACHMENT {
      @Override
      public ResourceAction perform(ResourceAction oldResourceAction, ResourceAction newResourceAction) throws Exception {
        AWSEC2NetworkInterfaceAttachmentResourceAction oldAction = (AWSEC2NetworkInterfaceAttachmentResourceAction) oldResourceAction;
        AWSEC2NetworkInterfaceAttachmentResourceAction newAction = (AWSEC2NetworkInterfaceAttachmentResourceAction) newResourceAction;
        if (deviceInstanceOrNetworkInterfaceIsDifferent(oldAction, newAction)) {
          ServiceConfiguration configuration = Topology.lookup(Compute.class);
          deleteNetworkInterfaceAttachment(oldAction, configuration);
        }
        return newAction;
      }
    },
    WAIT_UNTIL_DETACHED {
      @Override
      public ResourceAction perform(ResourceAction oldResourceAction, ResourceAction newResourceAction) throws Exception {
        AWSEC2NetworkInterfaceAttachmentResourceAction oldAction = (AWSEC2NetworkInterfaceAttachmentResourceAction) oldResourceAction;
        AWSEC2NetworkInterfaceAttachmentResourceAction newAction = (AWSEC2NetworkInterfaceAttachmentResourceAction) newResourceAction;
        if (deviceInstanceOrNetworkInterfaceIsDifferent(oldAction, newAction)) {
          ServiceConfiguration configuration = Topology.lookup(Compute.class);
          waitUntilDetached(oldAction, configuration);
        }
        return newAction;

      }

      @Override
      public Integer getTimeout() {
        return NETWORK_INTERFACE_DETACHMENT_MAX_DELETE_OR_UPDATE_RETRY_SECS;
      }
    },
    CREATE_NETWORK_INTERFACE_ATTACHMENT {
      @Override
      public ResourceAction perform(ResourceAction oldResourceAction, ResourceAction newResourceAction) throws Exception {
        AWSEC2NetworkInterfaceAttachmentResourceAction oldAction = (AWSEC2NetworkInterfaceAttachmentResourceAction) oldResourceAction;
        AWSEC2NetworkInterfaceAttachmentResourceAction newAction = (AWSEC2NetworkInterfaceAttachmentResourceAction) newResourceAction;
        if (deviceInstanceOrNetworkInterfaceIsDifferent(oldAction, newAction)) {
          final ServiceConfiguration configuration = Topology.lookup(Compute.class);
          return createNetworkInterfaceAttachment(newAction, configuration);
        }
        return newAction;
      }
    },
    WAIT_UNTIL_ATTACHED {
      @Override
      public ResourceAction perform(ResourceAction oldResourceAction, ResourceAction newResourceAction) throws Exception {
        AWSEC2NetworkInterfaceAttachmentResourceAction oldAction = (AWSEC2NetworkInterfaceAttachmentResourceAction) oldResourceAction;
        AWSEC2NetworkInterfaceAttachmentResourceAction newAction = (AWSEC2NetworkInterfaceAttachmentResourceAction) newResourceAction;
        if (deviceInstanceOrNetworkInterfaceIsDifferent(oldAction, newAction)) {
          ServiceConfiguration configuration = Topology.lookup(Compute.class);
          return waitUntilAttached(newAction, configuration);
        }
        return newAction;
      }
      @Override
      public Integer getTimeout() {
        return NETWORK_INTERFACE_ATTACHMENT_MAX_CREATE_OR_UPDATE_RETRY_SECS;
      }

    },
    SET_ATTRIBUTES {
      @Override
      public ResourceAction perform(ResourceAction oldResourceAction, ResourceAction newResourceAction) throws Exception {
        AWSEC2NetworkInterfaceAttachmentResourceAction oldAction = (AWSEC2NetworkInterfaceAttachmentResourceAction) oldResourceAction;
        AWSEC2NetworkInterfaceAttachmentResourceAction newAction = (AWSEC2NetworkInterfaceAttachmentResourceAction) newResourceAction;
        final ServiceConfiguration configuration = Topology.lookup( Compute.class );
        return setAttributes(newAction, configuration);
      }
    };

    private static boolean deviceInstanceOrNetworkInterfaceIsDifferent(AWSEC2NetworkInterfaceAttachmentResourceAction oldAction, AWSEC2NetworkInterfaceAttachmentResourceAction newAction) {
      return !(
        Objects.equals(oldAction.properties.getDeviceIndex(), newAction.properties.getDeviceIndex()) &&
          Objects.equals(oldAction.properties.getInstanceId(), newAction.properties.getInstanceId()) &&
          Objects.equals(oldAction.properties.getNetworkInterfaceId(), newAction.properties.getNetworkInterfaceId())
      );
    }
  }

  private static ResourceAction waitUntilDetached(AWSEC2NetworkInterfaceAttachmentResourceAction action, ServiceConfiguration configuration) throws Exception {
    DescribeNetworkInterfacesType describeNetworkInterfacesType = MessageHelper.createMessage(DescribeNetworkInterfacesType.class, action.info.getEffectiveUserId());
    describeNetworkInterfacesType.setNetworkInterfaceIdSet(action.convertNetworkInterfaceIdSet(action.properties.getNetworkInterfaceId()));
    DescribeNetworkInterfacesResponseType describeNetworkInterfacesResponseType = AsyncRequests.<DescribeNetworkInterfacesType, DescribeNetworkInterfacesResponseType>sendSync(configuration, describeNetworkInterfacesType);
    if (describeNetworkInterfacesResponseType.getNetworkInterfaceSet() == null || describeNetworkInterfacesResponseType.getNetworkInterfaceSet().getItem() == null ||
      describeNetworkInterfacesResponseType.getNetworkInterfaceSet().getItem().isEmpty()) {
      return action;
    }
    if (describeNetworkInterfacesResponseType.getNetworkInterfaceSet().getItem().get(0).getAttachment() == null ||
      !describeNetworkInterfacesResponseType.getNetworkInterfaceSet().getItem().get(0).getAttachment().getAttachmentId().equals(action.info.getPhysicalResourceId())) {
      return action; // must be attached to something else
    }
    if ("detached".equals(describeNetworkInterfacesResponseType.getNetworkInterfaceSet().getItem().get(0).getAttachment().getStatus())) {
      return action; // detached
    }
    throw new RetryAfterConditionCheckFailedException("Network interface " + action.properties.getNetworkInterfaceId() + " is not yet detached from instance " + action.properties.getInstanceId());
  }

  private static ResourceAction deleteNetworkInterfaceAttachment(AWSEC2NetworkInterfaceAttachmentResourceAction action, ServiceConfiguration configuration) throws Exception {
    final DetachNetworkInterfaceType detachNetworkInterfaceType =
        MessageHelper.createMessage(DetachNetworkInterfaceType.class, action.info.getEffectiveUserId());
    detachNetworkInterfaceType.setAttachmentId( action.info.getPhysicalResourceId( ) );
    try {
      AsyncRequests.<DetachNetworkInterfaceType, DetachNetworkInterfaceResponseType>sendSync(
        configuration,
        detachNetworkInterfaceType
      );
    } catch ( final Exception e ) {
      final Optional<AsyncWebServiceError> error = AsyncExceptions.asWebServiceError( e );
      if ( !error.isPresent( ) || !"InvalidAttachmentID.NotFound".equals( error.get( ).getCode( ) ) ) {
        throw e;
      }
    }
    return action;
  }


  @Override
  public ResourceProperties getResourceProperties( ) {
    return properties;
  }

  @Override
  public void setResourceProperties( final ResourceProperties resourceProperties ) {
    properties = (AWSEC2NetworkInterfaceAttachmentProperties) resourceProperties;
  }

  @Override
  public ResourceInfo getResourceInfo( ) {
    return info;
  }

  @Override
  public void setResourceInfo( final ResourceInfo resourceInfo ) {
    info = (AWSEC2NetworkInterfaceAttachmentResourceInfo) resourceInfo;
  }

  private static boolean notCreatedOrNoInstanceOrNoNetworkInterface(AWSEC2NetworkInterfaceAttachmentResourceAction action, ServiceConfiguration configuration) throws Exception {
    if (!Boolean.TRUE.equals(action.info.getCreatedEnoughToDelete())) return true;
    try {
      final DescribeInstancesType describeInstancesType = MessageHelper.createMessage(DescribeInstancesType.class, action.info.getEffectiveUserId());
      describeInstancesType.setInstancesSet(Lists.newArrayList(action.properties.getInstanceId()));
      DescribeInstancesResponseType describeInstancesResponseType = AsyncRequests.<DescribeInstancesType,DescribeInstancesResponseType> sendSync(configuration, describeInstancesType);
      if (describeInstancesResponseType.getReservationSet() == null || describeInstancesResponseType.getReservationSet().isEmpty()) {
        return true; // can't be attached to a nonexistent instance;
      }
    } catch ( final Exception e ) {
      final Optional<AsyncWebServiceError> error = AsyncExceptions.asWebServiceError( e );
      if ( error.isPresent( ) ) switch ( Strings.nullToEmpty(error.get().getCode()) ) {
        case "InvalidInstanceID.NotFound":
          return true; // can't be attached to a nonexistent instance;
      }
      throw e;
    }

    try {
      final DescribeNetworkInterfacesType describeNetworkInterfacesType = MessageHelper.createMessage(DescribeNetworkInterfacesType.class, action.info.getEffectiveUserId());
      describeNetworkInterfacesType.setNetworkInterfaceIdSet(action.convertNetworkInterfaceIdSet(action.properties.getNetworkInterfaceId()));
      DescribeNetworkInterfacesResponseType describeNetworkInterfacesResponseType = AsyncRequests.<DescribeNetworkInterfacesType, DescribeNetworkInterfacesResponseType>sendSync(configuration, describeNetworkInterfacesType);
      if (describeNetworkInterfacesResponseType.getNetworkInterfaceSet() == null || describeNetworkInterfacesResponseType.getNetworkInterfaceSet().getItem() == null ||
              describeNetworkInterfacesResponseType.getNetworkInterfaceSet().getItem().isEmpty()) {
        return true; // network interface can't be attached if it doesnt exist
      }
    } catch ( final Exception e ) {
      final Optional<AsyncWebServiceError> error = AsyncExceptions.asWebServiceError( e );
      if ( error.isPresent( ) ) switch ( Strings.nullToEmpty(error.get().getCode()) ) {
        case "InvalidNetworkInterfaceID.NotFound":
          return true; // network interface can't be attached if it doesnt exist
      }
      throw e;
    }

    return false;
  }

  private NetworkInterfaceIdSetType convertNetworkInterfaceIdSet(String networkInterfaceId) {
    NetworkInterfaceIdSetType networkInterfaceIdSetType = new NetworkInterfaceIdSetType();
    ArrayList<NetworkInterfaceIdSetItemType> item = Lists.newArrayList();
    NetworkInterfaceIdSetItemType networkInterfaceIdSetItemType = new NetworkInterfaceIdSetItemType();
    networkInterfaceIdSetItemType.setNetworkInterfaceId(networkInterfaceId);
    item.add(networkInterfaceIdSetItemType);
    networkInterfaceIdSetType.setItem(item);
    return networkInterfaceIdSetType;
  }

}


