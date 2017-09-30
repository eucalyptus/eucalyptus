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
import com.eucalyptus.cloudformation.resources.standard.info.AWSEC2VolumeAttachmentResourceInfo;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.AWSEC2VolumeAttachmentProperties;
import com.eucalyptus.cloudformation.template.JsonHelper;
import com.eucalyptus.cloudformation.util.MessageHelper;
import com.eucalyptus.cloudformation.workflow.RetryAfterConditionCheckFailedException;
import com.eucalyptus.cloudformation.workflow.steps.Step;
import com.eucalyptus.cloudformation.workflow.steps.StepBasedResourceAction;
import com.eucalyptus.cloudformation.workflow.updateinfo.UpdateType;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.Topology;
import com.eucalyptus.compute.common.AttachVolumeResponseType;
import com.eucalyptus.compute.common.AttachVolumeType;
import com.eucalyptus.compute.common.AttachedVolume;
import com.eucalyptus.compute.common.CloudFilters;
import com.eucalyptus.compute.common.Compute;
import com.eucalyptus.compute.common.DescribeInstancesResponseType;
import com.eucalyptus.compute.common.DescribeInstancesType;
import com.eucalyptus.compute.common.DescribeVolumesResponseType;
import com.eucalyptus.compute.common.DescribeVolumesType;
import com.eucalyptus.compute.common.DetachVolumeResponseType;
import com.eucalyptus.compute.common.DetachVolumeType;
import com.eucalyptus.configurable.ConfigurableClass;
import com.eucalyptus.configurable.ConfigurableField;
import com.eucalyptus.util.async.AsyncRequests;
import com.fasterxml.jackson.databind.node.TextNode;

import javax.annotation.Nullable;
import java.util.Objects;

import static com.eucalyptus.util.async.AsyncExceptions.asWebServiceErrorMessage;

/**
 * Created by ethomas on 2/3/14.
 */
@ConfigurableClass( root = "cloudformation", description = "Parameters controlling cloud formation")
public class AWSEC2VolumeAttachmentResourceAction extends StepBasedResourceAction {

  private AWSEC2VolumeAttachmentProperties properties = new AWSEC2VolumeAttachmentProperties();
  private AWSEC2VolumeAttachmentResourceInfo info = new AWSEC2VolumeAttachmentResourceInfo();

  @ConfigurableField(initial = "300", description = "The amount of time (in seconds) to wait for a volume to be attached during create)")
  public static volatile Integer VOLUME_ATTACHMENT_MAX_CREATE_RETRY_SECS = 300;

  @ConfigurableField(initial = "300", description = "The amount of time (in seconds) to wait for a volume to detach during delete)")
  public static volatile Integer VOLUME_DETACHMENT_MAX_DELETE_RETRY_SECS = 300;


  public AWSEC2VolumeAttachmentResourceAction() {
    // update not supported
    super(fromEnum(CreateSteps.class), fromEnum(DeleteSteps.class), null, null);
  }

  @Override
  public UpdateType getUpdateType(ResourceAction resourceAction, boolean stackTagsChanged) {
    UpdateType updateType = info.supportsTags() && stackTagsChanged ? UpdateType.NO_INTERRUPTION : UpdateType.NONE;
    AWSEC2VolumeAttachmentResourceAction otherAction = (AWSEC2VolumeAttachmentResourceAction) resourceAction;
    if (!Objects.equals(properties.getDevice(), otherAction.properties.getDevice())) {
      updateType = UpdateType.max(updateType, UpdateType.UNSUPPORTED);
    }

    if (!Objects.equals(properties.getInstanceId(), otherAction.properties.getInstanceId())) {
      updateType = UpdateType.max(updateType, UpdateType.UNSUPPORTED);
    }

    if (!Objects.equals(properties.getVolumeId(), otherAction.properties.getVolumeId())) {
      updateType = UpdateType.max(updateType, UpdateType.UNSUPPORTED);
    }
    return updateType;
  }


  private enum CreateSteps implements Step {
    ATTACH_VOLUME {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        AWSEC2VolumeAttachmentResourceAction action = (AWSEC2VolumeAttachmentResourceAction) resourceAction;
        ServiceConfiguration configuration = Topology.lookup(Compute.class);
        DescribeInstancesType describeInstancesType = MessageHelper.createMessage(DescribeInstancesType.class, action.info.getEffectiveUserId());
        describeInstancesType.getFilterSet( ).add( CloudFilters.filter( "instance-id", action.properties.getInstanceId( ) ) );
        DescribeInstancesResponseType describeInstancesResponseType = AsyncRequests.sendSync( configuration, describeInstancesType );
        if (describeInstancesResponseType.getReservationSet() == null || describeInstancesResponseType.getReservationSet().isEmpty()) {
          throw new ValidationErrorException("No such instance " + action.properties.getInstanceId());
        }
        DescribeVolumesType describeVolumesType = MessageHelper.createMessage(DescribeVolumesType.class, action.info.getEffectiveUserId());
        describeVolumesType.getFilterSet( ).add( CloudFilters.filter( "volume-id", action.properties.getVolumeId( ) ) );
        DescribeVolumesResponseType describeVolumesResponseType;
        try {
          describeVolumesResponseType = AsyncRequests.sendSync( configuration, describeVolumesType );
        } catch ( Exception e ) {
          throw new ValidationErrorException("Error describing volume " + action.properties.getVolumeId() + ":" + asWebServiceErrorMessage( e, e.getMessage() ) );
        }
        if (describeVolumesResponseType.getVolumeSet().size()==0) throw new ValidationErrorException("No such volume " + action.properties.getVolumeId());
        if (!"available".equals(describeVolumesResponseType.getVolumeSet().get(0).getStatus())) {
          throw new ValidationErrorException("Volume " + action.properties.getVolumeId() + " not available");
        }
        AttachVolumeType attachVolumeType = MessageHelper.createMessage(AttachVolumeType.class, action.info.getEffectiveUserId());
        attachVolumeType.setInstanceId(action.properties.getInstanceId());
        attachVolumeType.setVolumeId(action.properties.getVolumeId());
        attachVolumeType.setDevice(action.properties.getDevice());
        AsyncRequests.<AttachVolumeType, AttachVolumeResponseType> sendSync(configuration, attachVolumeType);
        return action;
      }
    },
    WAIT_UNTIL_ATTACHED {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        AWSEC2VolumeAttachmentResourceAction action = (AWSEC2VolumeAttachmentResourceAction) resourceAction;
        ServiceConfiguration configuration = Topology.lookup(Compute.class);
        boolean attached = false;
        DescribeVolumesType describeVolumesType = MessageHelper.createMessage(DescribeVolumesType.class, action.info.getEffectiveUserId());
        describeVolumesType.getFilterSet( ).add( CloudFilters.filter( "volume-id", action.properties.getVolumeId( ) ) );
        DescribeVolumesResponseType describeVolumesResponseType;
        try {
          describeVolumesResponseType = AsyncRequests.sendSync( configuration, describeVolumesType );
        } catch ( Exception e ) {
          throw new ValidationErrorException("Error describing volume " + action.properties.getVolumeId() + ":" + asWebServiceErrorMessage( e, e.getMessage() ) );
        }
        if (describeVolumesResponseType.getVolumeSet().size() == 0) {
          throwNotAttachedMessage(action.properties.getVolumeId(), action.properties.getInstanceId());
        }
        if (describeVolumesResponseType.getVolumeSet().get(0).getAttachmentSet() == null || describeVolumesResponseType.getVolumeSet().get(0).getAttachmentSet().isEmpty()) {
          throwNotAttachedMessage(action.properties.getVolumeId(), action.properties.getInstanceId());
        }
        for (AttachedVolume attachedVolume: describeVolumesResponseType.getVolumeSet().get(0).getAttachmentSet()) {
          if (attachedVolume.getInstanceId().equals(action.properties.getInstanceId()) &&
            attachedVolume.getDevice().equals(action.properties.getDevice()) && attachedVolume.getStatus().equals("attached")) {
            attached = true;
            break;
          }
        }
        if (!attached) {
          throwNotAttachedMessage(action.properties.getVolumeId(), action.properties.getInstanceId());
        }
        return action;
      }

      @Override
      public Integer getTimeout() {
        return VOLUME_ATTACHMENT_MAX_CREATE_RETRY_SECS;
      }

      private RetryAfterConditionCheckFailedException throwNotAttachedMessage(String volumeId, String instanceId) throws RetryAfterConditionCheckFailedException {
        throw new RetryAfterConditionCheckFailedException("Volume " + volumeId + " not yet attached to instance " + instanceId);
      }
    },
    POPULATE_FIELDS {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        AWSEC2VolumeAttachmentResourceAction action = (AWSEC2VolumeAttachmentResourceAction) resourceAction;
        ServiceConfiguration configuration = Topology.lookup(Compute.class);
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
    DETACH_VOLUME {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        AWSEC2VolumeAttachmentResourceAction action = (AWSEC2VolumeAttachmentResourceAction) resourceAction;
        ServiceConfiguration configuration = Topology.lookup(Compute.class);
        if (notCreatedOrNoInstanceOrNoVolume(action, configuration)) return action;
        DetachVolumeType detachVolumeType = MessageHelper.createMessage(DetachVolumeType.class, action.info.getEffectiveUserId());
        detachVolumeType.setInstanceId(action.properties.getInstanceId());
        detachVolumeType.setVolumeId(action.properties.getVolumeId());
        detachVolumeType.setDevice(action.properties.getDevice());
        AsyncRequests.<DetachVolumeType, DetachVolumeResponseType> sendSync(configuration, detachVolumeType);
        return action;
      }
    },
    WAIT_UNTIL_DETACHED {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        AWSEC2VolumeAttachmentResourceAction action = (AWSEC2VolumeAttachmentResourceAction) resourceAction;
        ServiceConfiguration configuration = Topology.lookup(Compute.class);
        if (notCreatedOrNoInstanceOrNoVolume(action, configuration)) return action;
        boolean detached = false;
        DescribeVolumesType describeVolumesType = MessageHelper.createMessage(DescribeVolumesType.class, action.info.getEffectiveUserId());
        describeVolumesType.getFilterSet( ).add( CloudFilters.filter( "volume-id", action.properties.getVolumeId( ) ) );
        DescribeVolumesResponseType describeVolumesResponseType;
        try {
          describeVolumesResponseType = AsyncRequests.sendSync( configuration, describeVolumesType );
        } catch ( Exception e ) {
          throw new ValidationErrorException("Error describing volume " + action.properties.getVolumeId() + ":" + asWebServiceErrorMessage( e, e.getMessage() ) );
        }
        if (describeVolumesResponseType.getVolumeSet().size() == 0) {
          return action; // volume is gone
        }
        if (describeVolumesResponseType.getVolumeSet().get(0).getAttachmentSet() == null || describeVolumesResponseType.getVolumeSet().get(0).getAttachmentSet().isEmpty()) {
          return action; // volume not attached to anything
        }
        for (AttachedVolume attachedVolume: describeVolumesResponseType.getVolumeSet().get(0).getAttachmentSet()) {
          if (attachedVolume.getInstanceId().equals(action.properties.getInstanceId())
            && attachedVolume.getDevice().equals(action.properties.getDevice()) && attachedVolume.getStatus().equals("detached")) {
            detached = true;
            break;
          }
        }
        if (detached == true) return action;
        throw new RetryAfterConditionCheckFailedException("Volume " + action.properties.getVolumeId() + " is not yet detached from instance " + action.properties.getInstanceId());
      }

      @Override
      public Integer getTimeout() {
        return VOLUME_DETACHMENT_MAX_DELETE_RETRY_SECS;
      }
    };

    @Nullable
    @Override
    public Integer getTimeout() {
      return null;
    }

    private static boolean notCreatedOrNoInstanceOrNoVolume(AWSEC2VolumeAttachmentResourceAction action, ServiceConfiguration configuration) throws Exception {
      if (!Boolean.TRUE.equals(action.info.getCreatedEnoughToDelete())) return true;
      DescribeInstancesType describeInstancesType = MessageHelper.createMessage(DescribeInstancesType.class, action.info.getEffectiveUserId());
      describeInstancesType.getFilterSet( ).add( CloudFilters.filter( "instance-id", action.properties.getInstanceId( ) ) );
      DescribeInstancesResponseType describeInstancesResponseType = AsyncRequests.sendSync( configuration, describeInstancesType );
      if (describeInstancesResponseType.getReservationSet() == null || describeInstancesResponseType.getReservationSet().isEmpty()) {
        return true; // can't be attached to a nonexistent instance;
      }
      DescribeVolumesType describeVolumesType = MessageHelper.createMessage(DescribeVolumesType.class, action.info.getEffectiveUserId());
      describeVolumesType.getFilterSet( ).add( CloudFilters.filter( "volume-id", action.properties.getVolumeId( ) ) );
      DescribeVolumesResponseType describeVolumesResponseType;
      try {
        describeVolumesResponseType = AsyncRequests.sendSync( configuration, describeVolumesType );
      } catch ( Exception e ) {
        throw new ValidationErrorException("Error describing volume " + action.properties.getVolumeId() + ":" + asWebServiceErrorMessage( e, e.getMessage() ) );
      }
      if (describeVolumesResponseType.getVolumeSet().size()==0) {
        return true; // volume can't be attached if it doesn't exist
      }
      return false;
    }
  }


  @Override
  public ResourceProperties getResourceProperties() {
    return properties;
  }

  @Override
  public void setResourceProperties(ResourceProperties resourceProperties) {
    properties = (AWSEC2VolumeAttachmentProperties) resourceProperties;
  }

  @Override
  public ResourceInfo getResourceInfo() {
    return info;
  }

  @Override
  public void setResourceInfo(ResourceInfo resourceInfo) {
    info = (AWSEC2VolumeAttachmentResourceInfo) resourceInfo;
  }



}


