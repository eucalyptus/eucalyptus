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


import static com.eucalyptus.util.async.AsyncExceptions.asWebServiceErrorMessage;
import static com.eucalyptus.util.async.AsyncExceptions.isWebServiceErrorCode;
import com.amazonaws.services.simpleworkflow.flow.core.Promise;
import com.eucalyptus.cloudformation.ValidationErrorException;
import com.eucalyptus.cloudformation.resources.ResourceAction;
import com.eucalyptus.cloudformation.resources.ResourceInfo;
import com.eucalyptus.cloudformation.resources.ResourceProperties;
import com.eucalyptus.cloudformation.resources.standard.info.AWSEC2VolumeAttachmentResourceInfo;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.AWSEC2VolumeAttachmentProperties;
import com.eucalyptus.cloudformation.template.JsonHelper;
import com.eucalyptus.cloudformation.util.MessageHelper;
import com.eucalyptus.cloudformation.workflow.StackActivity;
import com.eucalyptus.cloudformation.workflow.ValidationFailedException;
import com.eucalyptus.cloudformation.workflow.steps.CreateMultiStepPromise;
import com.eucalyptus.cloudformation.workflow.steps.DeleteMultiStepPromise;
import com.eucalyptus.cloudformation.workflow.steps.Step;
import com.eucalyptus.cloudformation.workflow.steps.StepTransform;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.Topology;
import com.eucalyptus.compute.common.AttachVolumeResponseType;
import com.eucalyptus.compute.common.AttachVolumeType;
import com.eucalyptus.compute.common.AttachedVolume;
import com.eucalyptus.compute.common.Compute;
import com.eucalyptus.compute.common.DescribeInstancesResponseType;
import com.eucalyptus.compute.common.DescribeInstancesType;
import com.eucalyptus.compute.common.DescribeVolumesResponseType;
import com.eucalyptus.compute.common.DescribeVolumesType;
import com.eucalyptus.compute.common.DetachVolumeResponseType;
import com.eucalyptus.compute.common.DetachVolumeType;
import com.eucalyptus.configurable.ConfigurableClass;
import com.eucalyptus.configurable.ConfigurableField;
import com.eucalyptus.util.async.AsyncExceptions;
import com.eucalyptus.util.async.AsyncRequests;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.collect.Lists;
import com.netflix.glisten.WorkflowOperations;

import java.util.List;
import javax.annotation.Nullable;

/**
 * Created by ethomas on 2/3/14.
 */
@ConfigurableClass( root = "cloudformation", description = "Parameters controlling cloud formation")
public class AWSEC2VolumeAttachmentResourceAction extends ResourceAction {

  private AWSEC2VolumeAttachmentProperties properties = new AWSEC2VolumeAttachmentProperties();
  private AWSEC2VolumeAttachmentResourceInfo info = new AWSEC2VolumeAttachmentResourceInfo();

  @ConfigurableField(initial = "300", description = "The amount of time (in seconds) to wait for a volume to be attached during create)")
  public static volatile Integer VOLUME_ATTACHMENT_MAX_CREATE_RETRY_SECS = 300;

  @ConfigurableField(initial = "300", description = "The amount of time (in seconds) to wait for a volume to detach during delete)")
  public static volatile Integer VOLUME_DETACHMENT_MAX_DELETE_RETRY_SECS = 300;


  public AWSEC2VolumeAttachmentResourceAction() {
    for (CreateSteps createStep : CreateSteps.values()) {
      createSteps.put(createStep.name(), createStep);
    }
    for (DeleteSteps deleteStep : DeleteSteps.values()) {
      deleteSteps.put(deleteStep.name(), deleteStep);
    }

  }

  private enum CreateSteps implements Step {
    ATTACH_VOLUME {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        AWSEC2VolumeAttachmentResourceAction action = (AWSEC2VolumeAttachmentResourceAction) resourceAction;
        ServiceConfiguration configuration = Topology.lookup(Compute.class);
        DescribeInstancesType describeInstancesType = MessageHelper.createMessage(DescribeInstancesType.class, action.info.getEffectiveUserId());
        describeInstancesType.setInstancesSet(Lists.newArrayList(action.properties.getInstanceId()));
        DescribeInstancesResponseType describeInstancesResponseType;
        try {
          describeInstancesResponseType = AsyncRequests.sendSync( configuration, describeInstancesType );
        } catch ( Exception e ) {
          if ( AsyncExceptions.isWebServiceErrorCode( e, "InvalidInstanceID.NotFound" ) ) {
            throw new ValidationErrorException("No such instance " + action.properties.getInstanceId());
          } else {
            throw e;
          }
        }
        if (describeInstancesResponseType.getReservationSet() == null || describeInstancesResponseType.getReservationSet().isEmpty()) {
          throw new ValidationErrorException("No such instance " + action.properties.getInstanceId());
        }
        DescribeVolumesType describeVolumesType = MessageHelper.createMessage(DescribeVolumesType.class, action.info.getEffectiveUserId());
        describeVolumesType.setVolumeSet(Lists.newArrayList(action.properties.getVolumeId()));
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
        describeVolumesType.setVolumeSet( Lists.newArrayList( action.properties.getVolumeId() ) );
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

      private ValidationFailedException throwNotAttachedMessage(String volumeId, String instanceId) throws ValidationFailedException {
        throw new ValidationFailedException("Volume " + volumeId + " not yet attached to instance " + instanceId);
      }
    },
    POPULATE_FIELDS {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        AWSEC2VolumeAttachmentResourceAction action = (AWSEC2VolumeAttachmentResourceAction) resourceAction;
        ServiceConfiguration configuration = Topology.lookup(Compute.class);
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
        describeVolumesType.setVolumeSet(Lists.newArrayList(action.properties.getVolumeId()));
        DescribeVolumesResponseType describeVolumesResponseType;
        try {
          describeVolumesResponseType = AsyncRequests.sendSync( configuration, describeVolumesType );
        } catch ( Exception e ) {
          if ( isWebServiceErrorCode( e, "InvalidVolume.NotFound" ) ) {
            return action;
          }
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
        throw new ValidationFailedException("Volume " + action.properties.getVolumeId() + " is not yet detached from instance " + action.properties.getInstanceId());
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
      if (action.info.getPhysicalResourceId() == null) return true;
      DescribeInstancesType describeInstancesType = MessageHelper.createMessage(DescribeInstancesType.class, action.info.getEffectiveUserId());
      describeInstancesType.setInstancesSet(Lists.newArrayList(action.properties.getInstanceId()));
      DescribeInstancesResponseType describeInstancesResponseType;
      try {
        describeInstancesResponseType = AsyncRequests.sendSync( configuration, describeInstancesType );
      } catch ( Exception e ) {
        if ( AsyncExceptions.isWebServiceErrorCode( e, "InvalidInstanceID.NotFound" ) ) {
          return true; // can't be attached to a nonexistent instance;
        } else {
          throw e;
        }
      }
      if (describeInstancesResponseType.getReservationSet() == null || describeInstancesResponseType.getReservationSet().isEmpty()) {
        return true; // can't be attached to a nonexistent instance;
      }
      DescribeVolumesType describeVolumesType = MessageHelper.createMessage(DescribeVolumesType.class, action.info.getEffectiveUserId());
      describeVolumesType.setVolumeSet(Lists.newArrayList(action.properties.getVolumeId()));
      DescribeVolumesResponseType describeVolumesResponseType;
      try {
        describeVolumesResponseType = AsyncRequests.sendSync( configuration, describeVolumesType );
      } catch ( Exception e ) {
        if ( isWebServiceErrorCode( e, "InvalidVolume.NotFound" ) ) {
          return true; // volume can't be attached if it doesn't exist
        }
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

  @Override
  public Promise<String> getCreatePromise(WorkflowOperations<StackActivity> workflowOperations, String resourceId, String stackId, String accountId, String effectiveUserId) {
    List<String> stepIds = Lists.transform(Lists.newArrayList(CreateSteps.values()), StepTransform.INSTANCE);
    return new CreateMultiStepPromise(workflowOperations, stepIds, this).getCreatePromise(resourceId, stackId, accountId, effectiveUserId);
  }

  @Override
  public Promise<String> getDeletePromise(WorkflowOperations<StackActivity> workflowOperations, String resourceId, String stackId, String accountId, String effectiveUserId) {
    List<String> stepIds = Lists.transform(Lists.newArrayList(DeleteSteps.values()), StepTransform.INSTANCE);
    return new DeleteMultiStepPromise(workflowOperations, stepIds, this).getDeletePromise(resourceId, stackId, accountId, effectiveUserId);
  }

}


