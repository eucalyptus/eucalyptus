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
import com.eucalyptus.cloudformation.workflow.ResourceFailureException;
import com.eucalyptus.cloudformation.workflow.RetryAfterConditionCheckFailedException;
import com.eucalyptus.cloudformation.workflow.steps.Step;
import com.eucalyptus.cloudformation.workflow.steps.StepBasedResourceAction;
import com.eucalyptus.cloudformation.workflow.steps.UpdateStep;
import com.eucalyptus.cloudformation.workflow.updateinfo.UpdateType;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.Topology;
import com.eucalyptus.compute.common.CloudFilters;
import com.eucalyptus.compute.common.Compute;
import com.eucalyptus.compute.common.CreateSnapshotResponseType;
import com.eucalyptus.compute.common.CreateSnapshotType;
import com.eucalyptus.compute.common.CreateTagsResponseType;
import com.eucalyptus.compute.common.CreateTagsType;
import com.eucalyptus.compute.common.CreateVolumeResponseType;
import com.eucalyptus.compute.common.CreateVolumeType;
import com.eucalyptus.compute.common.DeleteTagsResponseType;
import com.eucalyptus.compute.common.DeleteTagsType;
import com.eucalyptus.compute.common.DeleteVolumeResponseType;
import com.eucalyptus.compute.common.DeleteVolumeType;
import com.eucalyptus.compute.common.DescribeSnapshotsResponseType;
import com.eucalyptus.compute.common.DescribeSnapshotsType;
import com.eucalyptus.compute.common.DescribeTagsResponseType;
import com.eucalyptus.compute.common.DescribeTagsType;
import com.eucalyptus.compute.common.DescribeVolumeAttributeResponseType;
import com.eucalyptus.compute.common.DescribeVolumeAttributeType;
import com.eucalyptus.compute.common.DescribeVolumesResponseType;
import com.eucalyptus.compute.common.DescribeVolumesType;
import com.eucalyptus.compute.common.ModifyVolumeAttributeType;
import com.eucalyptus.compute.common.TagInfo;
import com.eucalyptus.configurable.ConfigurableClass;
import com.eucalyptus.configurable.ConfigurableField;
import com.eucalyptus.util.async.AsyncRequests;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static com.eucalyptus.util.async.AsyncExceptions.asWebServiceErrorMessage;

/**
 * Created by ethomas on 2/3/14.
 */
@ConfigurableClass( root = "cloudformation", description = "Parameters controlling cloud formation")
public class AWSEC2VolumeResourceAction extends StepBasedResourceAction {
  private AWSEC2VolumeProperties properties = new AWSEC2VolumeProperties();
  private AWSEC2VolumeResourceInfo info = new AWSEC2VolumeResourceInfo();

  @ConfigurableField(initial = "300", description = "The amount of time (in seconds) to wait for a volume to be available after create)")
  public static volatile Integer VOLUME_AVAILABLE_MAX_CREATE_RETRY_SECS = 300;

  @ConfigurableField(initial = "300", description = "The amount of time (in seconds) to wait for a snapshot to be complete (if specified as the deletion policy) before a volume is deleted)")
  public static volatile Integer VOLUME_SNAPSHOT_COMPLETE_MAX_DELETE_RETRY_SECS = 300;



  @ConfigurableField(initial = "300", description = "The amount of time (in seconds) to wait for a volume to be deleted)")
  public static volatile Integer VOLUME_DELETED_MAX_DELETE_RETRY_SECS = 300;


  public AWSEC2VolumeResourceAction() {
    super(fromEnum(CreateSteps.class), fromEnum(DeleteSteps.class), fromUpdateEnum(UpdateNoInterruptionSteps.class), null);
  }

  @Override
  public UpdateType getUpdateType(ResourceAction resourceAction, boolean stackTagsChanged) {
    UpdateType updateType = info.supportsTags() && stackTagsChanged ? UpdateType.NO_INTERRUPTION : UpdateType.NONE;
    AWSEC2VolumeResourceAction otherAction = (AWSEC2VolumeResourceAction) resourceAction;
    if (!Objects.equals(properties.getAutoEnableIO(), otherAction.properties.getAutoEnableIO())) {
      updateType = UpdateType.max(updateType, UpdateType.NO_INTERRUPTION);
    }
    if (!Objects.equals(properties.getAvailabilityZone(), otherAction.properties.getAvailabilityZone())) {
      updateType = UpdateType.max(updateType, UpdateType.UNSUPPORTED);
    }
    if (!Objects.equals(properties.getEncrypted(), otherAction.properties.getEncrypted())) {
      updateType = UpdateType.max(updateType, UpdateType.UNSUPPORTED);
    }
    if (!Objects.equals(properties.getIops(), otherAction.properties.getIops())) {
      updateType = UpdateType.max(updateType, UpdateType.UNSUPPORTED);
    }
    if (!Objects.equals(properties.getKmsKeyId(), otherAction.properties.getKmsKeyId())) {
      updateType = UpdateType.max(updateType, UpdateType.UNSUPPORTED);
    }
    if (!Objects.equals(properties.getSize(), otherAction.properties.getSize())) {
      updateType = UpdateType.max(updateType, UpdateType.UNSUPPORTED);
    }
    if (!Objects.equals(properties.getSnapshotId(), otherAction.properties.getSnapshotId())) {
      updateType = UpdateType.max(updateType, UpdateType.UNSUPPORTED);
    }
    if (!Objects.equals(properties.getTags(), otherAction.properties.getTags())) {
      updateType = UpdateType.max(updateType, UpdateType.NO_INTERRUPTION); // docs are wrong here, experimentation shows no interruption at AWS
    }
    if (!Objects.equals(properties.getVolumeType(), otherAction.properties.getVolumeType())) {
      updateType = UpdateType.max(updateType, UpdateType.UNSUPPORTED);
    }
    return updateType;
  }

  private enum CreateSteps implements Step {
    CREATE_VOLUME {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        AWSEC2VolumeResourceAction action = (AWSEC2VolumeResourceAction) resourceAction;
        ServiceConfiguration configuration = Topology.lookup(Compute.class);
        CreateVolumeType createVolumeType = MessageHelper.createMessage(CreateVolumeType.class, action.info.getEffectiveUserId());
        createVolumeType.setAvailabilityZone(action.properties.getAvailabilityZone());
        if (action.properties.getEncrypted() != null) {
          createVolumeType.setEncrypted(action.properties.getEncrypted());
        }
        // KmsKeyId not supported
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
        action.info.setCreatedEnoughToDelete(true);
        action.info.setReferenceValueJson(JsonHelper.getStringFromJsonNode(new TextNode(action.info.getPhysicalResourceId())));
        return action;
      }
    },
    SET_ATTRIBUTES {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        AWSEC2VolumeResourceAction action = (AWSEC2VolumeResourceAction) resourceAction;
        ServiceConfiguration configuration = Topology.lookup(Compute.class);
        if (action.properties.getAutoEnableIO() != null) {
          ModifyVolumeAttributeType modifyVolumeAttributeTypeType = MessageHelper.createMessage(ModifyVolumeAttributeType.class, action.info.getEffectiveUserId());
          modifyVolumeAttributeTypeType.setVolumeId(action.info.getPhysicalResourceId());
          modifyVolumeAttributeTypeType.setAutoEnableIO(action.properties.getAutoEnableIO());
          AsyncRequests.sendSync(configuration, modifyVolumeAttributeTypeType);
        }
        return action;
      }
    },
    VERIFY_AVAILABLE {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        AWSEC2VolumeResourceAction action = (AWSEC2VolumeResourceAction) resourceAction;
        ServiceConfiguration configuration = Topology.lookup(Compute.class);
        DescribeVolumesType describeVolumesType = MessageHelper.createMessage(DescribeVolumesType.class, action.info.getEffectiveUserId());
        describeVolumesType.getFilterSet( ).add( CloudFilters.filter( "volume-id", action.info.getPhysicalResourceId( ) ) );
        DescribeVolumesResponseType describeVolumesResponseType;
        try {
          describeVolumesResponseType = AsyncRequests.sendSync( configuration, describeVolumesType);
        } catch ( final Exception e ) {
          throw new ValidationErrorException("Error describing volume " + action.info.getPhysicalResourceId() + ":" + asWebServiceErrorMessage( e, e.getMessage() ) );
        }
        if (describeVolumesResponseType.getVolumeSet().size()==0) {
          throw new RetryAfterConditionCheckFailedException("Volume " + action.info.getPhysicalResourceId() + " not yet available");
        }
        if (!"available".equals(describeVolumesResponseType.getVolumeSet().get(0).getStatus())) {
          throw new RetryAfterConditionCheckFailedException("Volume " + action.info.getPhysicalResourceId() + " not yet available");
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

    @Nullable
    @Override
    public Integer getTimeout() {
      return null;
    }
  }

  private enum DeleteSteps implements Step {
    CREATE_SNAPSHOT {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        AWSEC2VolumeResourceAction action = (AWSEC2VolumeResourceAction) resourceAction;
        ServiceConfiguration configuration = Topology.lookup(Compute.class);
        if (volumeDeleted(action, configuration)) return action;
        if (!("Snapshot".equals(action.info.getDeletionPolicy()))) return action;
        CreateSnapshotType createSnapshotType = MessageHelper.createMessage(CreateSnapshotType.class, action.info.getEffectiveUserId());
        createSnapshotType.setVolumeId(action.info.getPhysicalResourceId());
        CreateSnapshotResponseType createSnapshotResponseType = AsyncRequests.<CreateSnapshotType, CreateSnapshotResponseType>sendSync(configuration, createSnapshotType);
        if (createSnapshotResponseType.getSnapshot() == null || createSnapshotResponseType.getSnapshot().getSnapshotId() == null) {
          throw new ResourceFailureException("Unable to create snapshot on delete for volume " + action.info.getPhysicalResourceId());
        } else {
          action.info.setSnapshotIdForDelete(JsonHelper.getStringFromJsonNode(new TextNode(createSnapshotResponseType.getSnapshot().getSnapshotId())));
        }
        return action;
      }
    },

    VERIFY_SNAPSHOT_COMPLETE {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        AWSEC2VolumeResourceAction action = (AWSEC2VolumeResourceAction) resourceAction;
        ServiceConfiguration configuration = Topology.lookup(Compute.class);
        if (volumeDeleted(action, configuration)) return action;
        if (!("Snapshot".equals(action.info.getDeletionPolicy()))) return action;
        DescribeSnapshotsType describeSnapshotsType = MessageHelper.createMessage(DescribeSnapshotsType.class, action.info.getEffectiveUserId());
        String snapshotId = JsonHelper.getJsonNodeFromString(action.info.getSnapshotIdForDelete()).asText();
        describeSnapshotsType.getFilterSet( ).add( CloudFilters.filter( "snapshot-id", snapshotId ) );
        DescribeSnapshotsResponseType describeSnapshotsResponseType = AsyncRequests.sendSync(configuration, describeSnapshotsType);
        if (describeSnapshotsResponseType.getSnapshotSet() == null || describeSnapshotsResponseType.getSnapshotSet().isEmpty()) {
          throw new RetryAfterConditionCheckFailedException("Snapshot " + snapshotId + " not yet complete");
        }
        if ("error".equals(describeSnapshotsResponseType.getSnapshotSet().get(0).getStatus())) {
          throw new ResourceFailureException("Error creating snapshot " + snapshotId + ", while deleting volume " + action.info.getPhysicalResourceId());
        } else if (!"completed".equals(describeSnapshotsResponseType.getSnapshotSet().get(0).getStatus())) {
          throw new RetryAfterConditionCheckFailedException("Snapshot " + snapshotId + " not yet complete");
        }
        return action;
      }

      @Override
      public Integer getTimeout() {
        return VOLUME_SNAPSHOT_COMPLETE_MAX_DELETE_RETRY_SECS;
      }
    },

    CREATE_SNAPSHOT_TAGS {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        AWSEC2VolumeResourceAction action = (AWSEC2VolumeResourceAction) resourceAction;
        ServiceConfiguration configuration = Topology.lookup(Compute.class);
        if (volumeDeleted(action, configuration)) return action;
        if (!("Snapshot".equals(action.info.getDeletionPolicy()))) return action;
        String snapshotId = JsonHelper.getJsonNodeFromString(action.info.getSnapshotIdForDelete()).asText();
        // Create 'system' tags as admin user
        String effectiveAdminUserId = action.info.getAccountId( );
        CreateTagsType createSystemTagsType = MessageHelper.createPrivilegedMessage(CreateTagsType.class, effectiveAdminUserId);
        createSystemTagsType.setResourcesSet(Lists.newArrayList(snapshotId));
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
          createTagsType.setResourcesSet(Lists.newArrayList(snapshotId));
          createTagsType.setTagSet(EC2Helper.createTagSet(tags));
          AsyncRequests.<CreateTagsType, CreateTagsResponseType>sendSync(configuration, createTagsType);
        }
        return action;
      }
    },

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
        throw new RetryAfterConditionCheckFailedException("Volume " + action.info.getPhysicalResourceId() + " not yet deleted");
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
      if (!Boolean.TRUE.equals(action.info.getCreatedEnoughToDelete())) return true;
      DescribeVolumesType describeVolumesType = MessageHelper.createMessage(DescribeVolumesType.class, action.info.getEffectiveUserId());
      describeVolumesType.getFilterSet( ).add( CloudFilters.filter( "volume-id", action.info.getPhysicalResourceId( ) ) );
      DescribeVolumesResponseType describeVolumesResponseType;
      try {
        describeVolumesResponseType = AsyncRequests.sendSync(configuration, describeVolumesType);
      } catch ( final Exception e ) {
        throw new ValidationErrorException("Error describing volume " + action.info.getPhysicalResourceId() + ":" + asWebServiceErrorMessage( e, e.getMessage() ) );
      }
      if (describeVolumesResponseType.getVolumeSet().size() == 0) {
        return true; // already deleted
      }
      if ("deleted".equals(describeVolumesResponseType.getVolumeSet().get(0).getStatus())) {
        return true;
      }
      return false;
    }
  }

  private enum UpdateNoInterruptionSteps implements UpdateStep {
    UPDATE_ATTRIBUTES {
      @Override
      public ResourceAction perform(ResourceAction oldResourceAction, ResourceAction newResourceAction) throws Exception {
        AWSEC2VolumeResourceAction oldAction = (AWSEC2VolumeResourceAction) oldResourceAction;
        AWSEC2VolumeResourceAction newAction = (AWSEC2VolumeResourceAction) newResourceAction;
        ServiceConfiguration configuration = Topology.lookup(Compute.class);
        DescribeVolumeAttributeType describeVolumeAttributeType = MessageHelper.createMessage(DescribeVolumeAttributeType.class, newAction.info.getEffectiveUserId());
        describeVolumeAttributeType.setVolumeId(newAction.info.getPhysicalResourceId());
        describeVolumeAttributeType.setAttribute("autoEnableIO");
        DescribeVolumeAttributeResponseType describeVolumeAttributeResponseType = AsyncRequests.sendSync(configuration, describeVolumeAttributeType);
        Boolean originalValue = null;
        if (describeVolumeAttributeResponseType != null) {
          originalValue = describeVolumeAttributeResponseType.getAutoEnableIO();
        }
        if (!Objects.equals(originalValue, newAction.properties.getAutoEnableIO())) {
          ModifyVolumeAttributeType modifyVolumeAttributeTypeType = MessageHelper.createMessage(ModifyVolumeAttributeType.class, newAction.info.getEffectiveUserId());
          modifyVolumeAttributeTypeType.setVolumeId(newAction.info.getPhysicalResourceId());
          modifyVolumeAttributeTypeType.setAutoEnableIO(newAction.properties.getAutoEnableIO());
          AsyncRequests.sendSync(configuration, modifyVolumeAttributeTypeType);
        }
        return newAction;
      }
    },
    UPDATE_TAGS {
      @Override
      public ResourceAction perform(ResourceAction oldResourceAction, ResourceAction newResourceAction) throws Exception {
        AWSEC2VolumeResourceAction oldAction = (AWSEC2VolumeResourceAction) oldResourceAction;
        AWSEC2VolumeResourceAction newAction = (AWSEC2VolumeResourceAction) newResourceAction;
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



}


