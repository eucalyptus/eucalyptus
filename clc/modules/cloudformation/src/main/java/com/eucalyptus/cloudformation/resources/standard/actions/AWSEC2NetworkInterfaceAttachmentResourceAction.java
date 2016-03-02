/*************************************************************************
 * Copyright 2009-2016 Eucalyptus Systems, Inc.
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
import com.eucalyptus.cloudformation.resources.standard.info.AWSEC2NetworkInterfaceAttachmentResourceInfo;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.AWSEC2NetworkInterfaceAttachmentProperties;
import com.eucalyptus.cloudformation.template.JsonHelper;
import com.eucalyptus.cloudformation.util.MessageHelper;
import com.eucalyptus.cloudformation.workflow.steps.Step;
import com.eucalyptus.cloudformation.workflow.steps.StepBasedResourceAction;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.Topology;
import com.eucalyptus.compute.common.AttachNetworkInterfaceResponseType;
import com.eucalyptus.compute.common.AttachNetworkInterfaceType;
import com.eucalyptus.compute.common.Compute;
import com.eucalyptus.compute.common.DetachNetworkInterfaceResponseType;
import com.eucalyptus.compute.common.DetachNetworkInterfaceType;
import com.eucalyptus.compute.common.ModifyNetworkInterfaceAttachmentType;
import com.eucalyptus.compute.common.ModifyNetworkInterfaceAttributeResponseType;
import com.eucalyptus.compute.common.ModifyNetworkInterfaceAttributeType;
import com.eucalyptus.util.async.AsyncExceptions;
import com.eucalyptus.util.async.AsyncExceptions.AsyncWebServiceError;
import com.eucalyptus.util.async.AsyncRequests;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.base.Optional;
import com.google.common.base.Strings;

import javax.annotation.Nullable;

/**
* Created by ethomas on 2/3/14.
*/
public class AWSEC2NetworkInterfaceAttachmentResourceAction extends StepBasedResourceAction {

  private AWSEC2NetworkInterfaceAttachmentProperties properties = new AWSEC2NetworkInterfaceAttachmentProperties( );
  private AWSEC2NetworkInterfaceAttachmentResourceInfo info = new AWSEC2NetworkInterfaceAttachmentResourceInfo( );

//  @ConfigurableField(initial = "300", description = "The amount of time (in seconds) to wait for a network interface to be attached during create)")
//  public static volatile Integer NETWORK_INTERFACE_ATTACHMENT_MAX_CREATE_RETRY_SECS = 300;
//
//  @ConfigurableField(initial = "300", description = "The amount of time (in seconds) to wait for a network interface to detach during delete)")
//  public static volatile Integer NETWORK_INTERFACE_DETACHMENT_MAX_DELETE_RETRY_SECS = 300;

  public AWSEC2NetworkInterfaceAttachmentResourceAction() {
    super( fromEnum(CreateSteps.class), fromEnum(DeleteSteps.class), null, null );
  }

  private enum CreateSteps implements Step {
    CREATE_NETWORK_INTERFACE_ATTACHMENT {
      @Override
      public ResourceAction perform( final ResourceAction resourceAction ) throws Exception {
        final AWSEC2NetworkInterfaceAttachmentResourceAction action =
            (AWSEC2NetworkInterfaceAttachmentResourceAction) resourceAction;
        final ServiceConfiguration configuration = Topology.lookup( Compute.class );
        final AttachNetworkInterfaceType attachNetworkInterfaceType =
            MessageHelper.createMessage (AttachNetworkInterfaceType.class, action.info.getEffectiveUserId( ) );
        attachNetworkInterfaceType.setDeviceIndex( action.properties.getDeviceIndex( ) );
        attachNetworkInterfaceType.setInstanceId( action.properties.getInstanceId( ) );
        attachNetworkInterfaceType.setNetworkInterfaceId( action.properties.getNetworkInterfaceId( ) );

        try {
          final AttachNetworkInterfaceResponseType attachNetworkInterfaceResponseType =
              AsyncRequests.sendSync( configuration, attachNetworkInterfaceType );
          final String attachmentId = attachNetworkInterfaceResponseType.getAttachmentId( );
          action.info.setPhysicalResourceId( attachmentId );
          action.info.setCreatedEnoughToDelete(true);
          action.info.setReferenceValueJson( JsonHelper.getStringFromJsonNode( new TextNode( attachmentId ) ) );
        } catch ( final Exception e ) {
          final Optional<AsyncWebServiceError> error = AsyncExceptions.asWebServiceError( e );
          if ( error.isPresent( ) ) switch ( Strings.nullToEmpty( error.get( ).getCode( ) ) ) {
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
    },
//    WAIT_UNTIL_ATTACHED {
//      @Override
//      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
//        AWSEC2NetworkInterfaceAttachmentResourceAction action = (AWSEC2NetworkInterfaceAttachmentResourceAction) resourceAction;
//        ServiceConfiguration configuration = Topology.lookup(Compute.class);
//        DescribeNetworkInterfacesType describeNetworkInterfacesType = MessageHelper.createMessage(DescribeNetworkInterfacesType.class, action.info.getEffectiveUserId());
//        describeNetworkInterfacesType.setNetworkInterfaceIdSet(action.convertNetworkInterfaceIdSet(action.properties.getNetworkInterfaceId()));
//        DescribeNetworkInterfacesResponseType describeNetworkInterfacesResponseType = AsyncRequests.<DescribeNetworkInterfacesType, DescribeNetworkInterfacesResponseType>sendSync(configuration, describeNetworkInterfacesType);
//        if (describeNetworkInterfacesResponseType.getNetworkInterfaceSet() == null || describeNetworkInterfacesResponseType.getNetworkInterfaceSet().getItem() == null ||
//          describeNetworkInterfacesResponseType.getNetworkInterfaceSet().getItem().isEmpty()) {
//          throwNotAttachedMessage(action.properties.getNetworkInterfaceId(), action.properties.getInstanceId());
//        }
//        if (describeNetworkInterfacesResponseType.getNetworkInterfaceSet().getItem().get(0).getAttachment() == null ||
//          !"attached".equals(describeNetworkInterfacesResponseType.getNetworkInterfaceSet().getItem().get(0).getAttachment().getStatus())) {
//          throwNotAttachedMessage(action.properties.getNetworkInterfaceId(), action.properties.getInstanceId());
//        }
//        return action;
//      }
//
//      @Override
//      public Integer getTimeout() {
//        return NETWORK_INTERFACE_ATTACHMENT_MAX_CREATE_RETRY_SECS;
//      }
//
//      public void throwNotAttachedMessage(String networkInterfaceId, String instanceId) throws RetryAfterConditionCheckFailedException {
//        throw new RetryAfterConditionCheckFailedException("Network interface  " + networkInterfaceId + " not yet attached to instance " + instanceId);
//      }
//    },
    SET_ATTRIBUTES {
      @Override
      public ResourceAction perform( final ResourceAction resourceAction ) throws Exception {
        final AWSEC2NetworkInterfaceAttachmentResourceAction action =
            (AWSEC2NetworkInterfaceAttachmentResourceAction) resourceAction;
        if ( action.properties.getDeleteOnTermination( ) != null ) {
          final ServiceConfiguration configuration = Topology.lookup( Compute.class );
          final ModifyNetworkInterfaceAttachmentType attachment = new ModifyNetworkInterfaceAttachmentType( );
          attachment.setAttachmentId( action.info.getPhysicalResourceId( ) );
          attachment.setDeleteOnTermination( action.properties.getDeleteOnTermination( ) );
          final ModifyNetworkInterfaceAttributeType modifyNetworkInterfaceAttributeType =
              MessageHelper.createMessage( ModifyNetworkInterfaceAttributeType.class, action.info.getEffectiveUserId( ) );
          modifyNetworkInterfaceAttributeType.setNetworkInterfaceId( action.properties.getNetworkInterfaceId( ) );
          modifyNetworkInterfaceAttributeType.setAttachment( attachment );
          AsyncRequests.<ModifyNetworkInterfaceAttributeType, ModifyNetworkInterfaceAttributeResponseType>sendSync(
              configuration,
              modifyNetworkInterfaceAttributeType );
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
    DELETE_NETWORK_INTERFACE_ATTACHMENT {
      @Override
      public ResourceAction perform( final ResourceAction resourceAction ) throws Exception {
        final AWSEC2NetworkInterfaceAttachmentResourceAction action =
            (AWSEC2NetworkInterfaceAttachmentResourceAction) resourceAction;
        if ( action.info.getPhysicalResourceId( ) != null ) {
          final ServiceConfiguration configuration = Topology.lookup( Compute.class );
          final DetachNetworkInterfaceType detachNetworkInterfaceType =
              MessageHelper.createMessage( DetachNetworkInterfaceType.class, action.info.getEffectiveUserId( ) );
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
        }
        return action;
      }

    },
//    WAIT_UNTIL_DETACHED {
//      @Override
//      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
//        AWSEC2NetworkInterfaceAttachmentResourceAction action = (AWSEC2NetworkInterfaceAttachmentResourceAction) resourceAction;
//        ServiceConfiguration configuration = Topology.lookup(Compute.class);
//        if (notCreatedOrNoInstanceOrNoNetworkInterface(action, configuration)) return action;
//        boolean detached = false;
//        DescribeNetworkInterfacesType describeNetworkInterfacesType = MessageHelper.createMessage(DescribeNetworkInterfacesType.class, action.info.getEffectiveUserId());
//        describeNetworkInterfacesType.setNetworkInterfaceIdSet(action.convertNetworkInterfaceIdSet(action.properties.getNetworkInterfaceId()));
//        DescribeNetworkInterfacesResponseType describeNetworkInterfacesResponseType = AsyncRequests.<DescribeNetworkInterfacesType, DescribeNetworkInterfacesResponseType>sendSync(configuration, describeNetworkInterfacesType);
//        if (describeNetworkInterfacesResponseType.getNetworkInterfaceSet() == null || describeNetworkInterfacesResponseType.getNetworkInterfaceSet().getItem() == null ||
//          describeNetworkInterfacesResponseType.getNetworkInterfaceSet().getItem().isEmpty()) {
//          return action;
//        }
//        if (describeNetworkInterfacesResponseType.getNetworkInterfaceSet().getItem().get(0).getAttachment() == null ||
//          !describeNetworkInterfacesResponseType.getNetworkInterfaceSet().getItem().get(0).getAttachment().getAttachmentId().equals(action.info.getPhysicalResourceId())) {
//          return action; // must be attached to something else
//        }
//        if ("detached".equals(describeNetworkInterfacesResponseType.getNetworkInterfaceSet().getItem().get(0).getAttachment().getStatus())) {
//          return action; // detached
//        }
//        throw new RetryAfterConditionCheckFailedException("Network interface " + action.properties.getNetworkInterfaceId() + " is not yet detached from instance " + action.properties.getInstanceId());
//      }
//
//      @Override
//      public Integer getTimeout() {
//        return NETWORK_INTERFACE_DETACHMENT_MAX_DELETE_RETRY_SECS;
//      }
//    },
    ;
//    private static boolean notCreatedOrNoInstanceOrNoNetworkInterface(AWSEC2NetworkInterfaceAttachmentResourceAction action, ServiceConfiguration configuration) throws Exception {
//      if (action.info.getCreatedEnoughToDelete() != Boolean.TRUE) return true;
//      DescribeInstancesType describeInstancesType = MessageHelper.createMessage(DescribeInstancesType.class, action.info.getEffectiveUserId());
//      describeInstancesType.setInstancesSet(Lists.newArrayList(action.properties.getInstanceId()));
//      DescribeInstancesResponseType describeInstancesResponseType = AsyncRequests.<DescribeInstancesType,DescribeInstancesResponseType> sendSync(configuration, describeInstancesType);
//      if (describeInstancesResponseType.getReservationSet() == null || describeInstancesResponseType.getReservationSet().isEmpty()) {
//        return true; // can't be attached to a nonexistent instance;
//      }
//      DescribeNetworkInterfacesType describeNetworkInterfacesType = MessageHelper.createMessage(DescribeNetworkInterfacesType.class, action.info.getEffectiveUserId());
//      describeNetworkInterfacesType.setNetworkInterfaceIdSet(action.convertNetworkInterfaceIdSet(action.properties.getNetworkInterfaceId()));
//      DescribeNetworkInterfacesResponseType describeNetworkInterfacesResponseType = AsyncRequests.<DescribeNetworkInterfacesType, DescribeNetworkInterfacesResponseType>sendSync(configuration, describeNetworkInterfacesType);
//      if (describeNetworkInterfacesResponseType.getNetworkInterfaceSet() == null || describeNetworkInterfacesResponseType.getNetworkInterfaceSet().getItem() == null ||
//        describeNetworkInterfacesResponseType.getNetworkInterfaceSet().getItem().isEmpty()) {
//        return true; // network interface can't be attached if it doesnt exist
//      }
//      return false;
//    };

    @Nullable
    @Override
    public Integer getTimeout( ) {
      return null;
    }
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

//  private NetworkInterfaceIdSetType convertNetworkInterfaceIdSet(String networkInterfaceId) {
//    NetworkInterfaceIdSetType networkInterfaceIdSetType = new NetworkInterfaceIdSetType();
//    ArrayList<NetworkInterfaceIdSetItemType> item = Lists.newArrayList();
//    NetworkInterfaceIdSetItemType networkInterfaceIdSetItemType = new NetworkInterfaceIdSetItemType();
//    networkInterfaceIdSetItemType.setNetworkInterfaceId(networkInterfaceId);
//    item.add(networkInterfaceIdSetItemType);
//    networkInterfaceIdSetType.setItem(item);
//    return networkInterfaceIdSetType;
//  }

}


