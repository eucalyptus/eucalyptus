/*************************************************************************
 * Copyright 2009-2013 Eucalyptus Systems, Inc.
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


import com.amazonaws.services.simpleworkflow.flow.core.Promise;
import com.eucalyptus.cloudformation.resources.EC2Helper;
import com.eucalyptus.cloudformation.resources.ResourceAction;
import com.eucalyptus.cloudformation.resources.ResourceInfo;
import com.eucalyptus.cloudformation.resources.ResourceProperties;
import com.eucalyptus.cloudformation.resources.standard.TagHelper;
import com.eucalyptus.cloudformation.resources.standard.info.AWSEC2VolumeResourceInfo;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.AWSEC2VolumeProperties;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.EC2Tag;
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
import com.eucalyptus.compute.common.Compute;
import com.eucalyptus.compute.common.CreateTagsResponseType;
import com.eucalyptus.compute.common.CreateTagsType;
import com.eucalyptus.compute.common.CreateVolumeResponseType;
import com.eucalyptus.compute.common.CreateVolumeType;
import com.eucalyptus.compute.common.DeleteVolumeResponseType;
import com.eucalyptus.compute.common.DeleteVolumeType;
import com.eucalyptus.compute.common.DescribeVolumesResponseType;
import com.eucalyptus.compute.common.DescribeVolumesType;
import com.eucalyptus.configurable.ConfigurableClass;
import com.eucalyptus.configurable.ConfigurableField;
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
public class AWSEC2VolumeResourceAction extends ResourceAction {

  private AWSEC2VolumeProperties properties = new AWSEC2VolumeProperties();
  private AWSEC2VolumeResourceInfo info = new AWSEC2VolumeResourceInfo();

  @ConfigurableField(initial = "300", description = "The amount of time (in seconds) to wait for a volume to be available after create)")
  public static volatile Integer VOLUME_AVAILABLE_MAX_CREATE_RETRY_SECS = 300;

  @ConfigurableField(initial = "300", description = "The amount of time (in seconds) to wait for a volume to be deleted)")
  public static volatile Integer VOLUME_DELETED_MAX_DELETE_RETRY_SECS = 300;


  public AWSEC2VolumeResourceAction() {
    for (CreateSteps createStep : CreateSteps.values()) {
      createSteps.put(createStep.name(), createStep);
    }
    for (DeleteSteps deleteStep : DeleteSteps.values()) {
      deleteSteps.put(deleteStep.name(), deleteStep);
    }

  }

  private enum CreateSteps implements Step {
    CREATE_VOLUME {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        AWSEC2VolumeResourceAction action = (AWSEC2VolumeResourceAction) resourceAction;
        ServiceConfiguration configuration = Topology.lookup(Compute.class);
        CreateVolumeType createVolumeType = MessageHelper.createMessage(CreateVolumeType.class, action.info.getEffectiveUserId());
        createVolumeType.setAvailabilityZone(action.properties.getAvailabilityZone());
        if (action.properties.getIops() != null) {
          createVolumeType.setIops(action.properties.getIops());
        }
        if (action.properties.getSize() != null) {
          createVolumeType.setSize(action.properties.getSize());
        }
        if (action.properties.getSnapshotId() != null) {
          createVolumeType.setSnapshotId(action.properties.getSnapshotId());
        }
        if (action.properties.getVolumeType() != null) {
          createVolumeType.setVolumeType(action.properties.getVolumeType());
        } else {
          createVolumeType.setVolumeType("standard");
        }

        CreateVolumeResponseType createVolumeResponseType = AsyncRequests.<CreateVolumeType,CreateVolumeResponseType> sendSync(configuration, createVolumeType);
        action.info.setPhysicalResourceId(createVolumeResponseType.getVolume().getVolumeId());
        action.info.setReferenceValueJson(JsonHelper.getStringFromJsonNode(new TextNode(action.info.getPhysicalResourceId())));
        return action;
      }
    },
    VERIFY_AVAILABLE {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        AWSEC2VolumeResourceAction action = (AWSEC2VolumeResourceAction) resourceAction;
        ServiceConfiguration configuration = Topology.lookup(Compute.class);
        DescribeVolumesType describeVolumesType = MessageHelper.createMessage(DescribeVolumesType.class, action.info.getEffectiveUserId());
        describeVolumesType.setVolumeSet(Lists.newArrayList(action.info.getPhysicalResourceId()));
        DescribeVolumesResponseType describeVolumesResponseType = AsyncRequests.<DescribeVolumesType,DescribeVolumesResponseType> sendSync(configuration, describeVolumesType);
        if (describeVolumesResponseType.getVolumeSet().size()==0) {
          throw new ValidationFailedException("Volume " + action.info.getPhysicalResourceId() + " not yet available");
        }
        if (!"available".equals(describeVolumesResponseType.getVolumeSet().get(0).getStatus())) {
          throw new ValidationFailedException("Volume " + action.info.getPhysicalResourceId() + " not yet available");
        }
        return action;
      }

      @Override
      public Integer getTimeout() {
        return VOLUME_AVAILABLE_MAX_CREATE_RETRY_SECS;
      }

    },
    CREATE_TAGS {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        AWSEC2VolumeResourceAction action = (AWSEC2VolumeResourceAction) resourceAction;
        ServiceConfiguration configuration = Topology.lookup(Compute.class);
        List<EC2Tag> tags = TagHelper.getEC2StackTags(action.info, action.getStackEntity());
        if (action.properties.getTags() != null && !action.properties.getTags().isEmpty()) {
          TagHelper.checkReservedEC2TemplateTags(action.properties.getTags());
          tags.addAll(action.properties.getTags());
        }
        // due to stack aws: tags
        CreateTagsType createTagsType = MessageHelper.createPrivilegedMessage(CreateTagsType.class, action.info.getEffectiveUserId());
        createTagsType.setResourcesSet(Lists.newArrayList(action.info.getPhysicalResourceId()));
        createTagsType.setTagSet(EC2Helper.createTagSet(tags));
        AsyncRequests.<CreateTagsType, CreateTagsResponseType>sendSync(configuration, createTagsType);
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
    DELETE_VOLUME {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        AWSEC2VolumeResourceAction action = (AWSEC2VolumeResourceAction) resourceAction;
        ServiceConfiguration configuration = Topology.lookup(Compute.class);
        if (volumeDeleted(action, configuration)) return action;
        DeleteVolumeType deleteVolumeType = MessageHelper.createMessage(DeleteVolumeType.class, action.info.getEffectiveUserId());
        deleteVolumeType.setVolumeId(action.info.getPhysicalResourceId());
        AsyncRequests.<DeleteVolumeType, DeleteVolumeResponseType>sendSync(configuration, deleteVolumeType);
        return action;
      }
    },
    VERIFY_DELETE {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        AWSEC2VolumeResourceAction action = (AWSEC2VolumeResourceAction) resourceAction;
        ServiceConfiguration configuration = Topology.lookup(Compute.class);
        if (volumeDeleted(action, configuration)) return action;
        throw new ValidationFailedException("Volume " + action.info.getPhysicalResourceId() + " not yet deleted");
      }

      @Override
      public Integer getTimeout() {
        return VOLUME_DELETED_MAX_DELETE_RETRY_SECS;
      }
    };

    @Nullable
    @Override
    public Integer getTimeout() {
      return null;
    }

    private static boolean volumeDeleted(AWSEC2VolumeResourceAction action, ServiceConfiguration configuration) throws Exception {
      if (action.info.getPhysicalResourceId() == null) return true;
      DescribeVolumesType describeVolumesType = MessageHelper.createMessage(DescribeVolumesType.class, action.info.getEffectiveUserId());
      describeVolumesType.setVolumeSet(Lists.newArrayList(action.info.getPhysicalResourceId()));
      DescribeVolumesResponseType describeVolumesResponseType = AsyncRequests.<DescribeVolumesType,DescribeVolumesResponseType> sendSync(configuration, describeVolumesType);
      if (describeVolumesResponseType.getVolumeSet().size() == 0) {
        return true; // already deleted
      }
      if ("deleted".equals(describeVolumesResponseType.getVolumeSet().get(0).getStatus())) {
        return true;
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
    properties = (AWSEC2VolumeProperties) resourceProperties;
  }

  @Override
  public ResourceInfo getResourceInfo() {
    return info;
  }

  @Override
  public void setResourceInfo(ResourceInfo resourceInfo) {
    info = (AWSEC2VolumeResourceInfo) resourceInfo;
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


