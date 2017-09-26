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


import com.eucalyptus.auth.Accounts;
import com.eucalyptus.cloudformation.ValidationErrorException;
import com.eucalyptus.cloudformation.entity.SignalEntity;
import com.eucalyptus.cloudformation.entity.SignalEntityManager;
import com.eucalyptus.cloudformation.entity.StackEventEntityManager;
import com.eucalyptus.cloudformation.resources.EC2Helper;
import com.eucalyptus.cloudformation.resources.ResourceAction;
import com.eucalyptus.cloudformation.resources.ResourceInfo;
import com.eucalyptus.cloudformation.resources.ResourceProperties;
import com.eucalyptus.cloudformation.resources.standard.TagHelper;
import com.eucalyptus.cloudformation.resources.standard.info.AWSEC2InstanceResourceInfo;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.AWSEC2InstanceProperties;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.EC2BlockDeviceMapping;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.EC2EBSBlockDevice;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.EC2MountPoint;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.EC2NetworkInterface;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.EC2NetworkInterfacePrivateIPSpecification;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.EC2Tag;
import com.eucalyptus.cloudformation.template.CreationPolicy;
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
import com.eucalyptus.compute.common.AttachVolumeResponseType;
import com.eucalyptus.compute.common.AttachVolumeType;
import com.eucalyptus.compute.common.AttachedVolume;
import com.eucalyptus.compute.common.AttributeBooleanFlatValueType;
import com.eucalyptus.compute.common.AttributeBooleanValueType;
import com.eucalyptus.compute.common.AttributeValueType;
import com.eucalyptus.compute.common.BlockDeviceMappingItemType;
import com.eucalyptus.compute.common.Compute;
import com.eucalyptus.compute.common.CreateTagsResponseType;
import com.eucalyptus.compute.common.CreateTagsType;
import com.eucalyptus.compute.common.DeleteTagsResponseType;
import com.eucalyptus.compute.common.DeleteTagsType;
import com.eucalyptus.compute.common.DescribeInstanceAttributeResponseType;
import com.eucalyptus.compute.common.DescribeInstanceAttributeType;
import com.eucalyptus.compute.common.DescribeInstancesResponseType;
import com.eucalyptus.compute.common.DescribeInstancesType;
import com.eucalyptus.compute.common.DescribeSecurityGroupsResponseType;
import com.eucalyptus.compute.common.DescribeSecurityGroupsType;
import com.eucalyptus.compute.common.DescribeTagsResponseType;
import com.eucalyptus.compute.common.DescribeTagsType;
import com.eucalyptus.compute.common.DescribeVolumesResponseType;
import com.eucalyptus.compute.common.DescribeVolumesType;
import com.eucalyptus.compute.common.DetachVolumeType;
import com.eucalyptus.compute.common.EbsDeviceMapping;
import com.eucalyptus.compute.common.Filter;
import com.eucalyptus.compute.common.GroupIdSetType;
import com.eucalyptus.compute.common.GroupItemType;
import com.eucalyptus.compute.common.InstanceBlockDeviceMapping;
import com.eucalyptus.compute.common.InstanceBlockDeviceMappingItemType;
import com.eucalyptus.compute.common.InstanceBlockDeviceMappingSetType;
import com.eucalyptus.compute.common.InstanceEbsBlockDeviceType;
import com.eucalyptus.compute.common.InstanceNetworkInterfaceSetItemRequestType;
import com.eucalyptus.compute.common.InstanceNetworkInterfaceSetRequestType;
import com.eucalyptus.compute.common.ModifyInstanceAttributeType;
import com.eucalyptus.compute.common.MonitorInstancesType;
import com.eucalyptus.compute.common.PrivateIpAddressesSetItemRequestType;
import com.eucalyptus.compute.common.PrivateIpAddressesSetRequestType;
import com.eucalyptus.compute.common.ResetInstanceAttributeType;
import com.eucalyptus.compute.common.RunInstancesResponseType;
import com.eucalyptus.compute.common.RunInstancesType;
import com.eucalyptus.compute.common.RunningInstancesItemType;
import com.eucalyptus.compute.common.SecurityGroupIdSetItemType;
import com.eucalyptus.compute.common.SecurityGroupIdSetType;
import com.eucalyptus.compute.common.StartInstancesType;
import com.eucalyptus.compute.common.StopInstancesType;
import com.eucalyptus.compute.common.TagInfo;
import com.eucalyptus.compute.common.TerminateInstancesResponseType;
import com.eucalyptus.compute.common.TerminateInstancesType;
import com.eucalyptus.compute.common.UnmonitorInstancesType;
import com.eucalyptus.compute.common.Volume;
import com.eucalyptus.configurable.ConfigurableClass;
import com.eucalyptus.configurable.ConfigurableField;
import com.eucalyptus.util.async.AsyncRequests;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static com.eucalyptus.util.async.AsyncExceptions.asWebServiceErrorMessage;

/**
 * Created by ethomas on 2/3/14.
 */
@ConfigurableClass( root = "cloudformation", description = "Parameters controlling cloud formation")
public class AWSEC2InstanceResourceAction extends StepBasedResourceAction {

  @ConfigurableField(initial = "300", description = "The amount of time (in seconds) to wait for an instance to be running after creation)")
  public static volatile Integer INSTANCE_RUNNING_MAX_CREATE_RETRY_SECS = 300;

  @ConfigurableField(initial = "300", description = "The amount of time (in seconds) to wait for an instance to have volumes attached after creation)")
  public static volatile Integer INSTANCE_ATTACH_VOLUME_MAX_CREATE_RETRY_SECS = 300;

  @ConfigurableField(initial = "300", description = "The amount of time (in seconds) to wait for an instance to have new volumes attached after update)")
  public static volatile Integer INSTANCE_ATTACH_VOLUME_MAX_UPDATE_RETRY_SECS = 300;

  @ConfigurableField(initial = "300", description = "The amount of time (in seconds) to wait for an instance to detach old volumes during update")
  public static volatile Integer INSTANCE_DETACH_VOLUME_MAX_UPDATE_RETRY_SECS = 300;

  @ConfigurableField(initial = "300", description = "The amount of time (in seconds) to wait for an instance to be terminated after deletion)")
  public static volatile Integer INSTANCE_TERMINATED_MAX_DELETE_RETRY_SECS = 300;

  @ConfigurableField(initial = "300", description = "The amount of time (in seconds) to wait for an instance to be stopped after update)")
  public static volatile Integer INSTANCE_STOPPED_MAX_UPDATE_RETRY_SECS = 300;

  @ConfigurableField(initial = "300", description = "The amount of time (in seconds) to wait for an instance to be running after update)")
  public static volatile Integer INSTANCE_RUNNING_MAX_UPDATE_RETRY_SECS = 300;

  private AWSEC2InstanceProperties properties = new AWSEC2InstanceProperties();
  private AWSEC2InstanceResourceInfo info = new AWSEC2InstanceResourceInfo();

  public AWSEC2InstanceResourceAction() {
    super(fromEnum(CreateSteps.class), fromEnum(DeleteSteps.class), fromUpdateEnum(UpdateNoInterruptionSteps.class), fromUpdateEnum(UpdateSomeInterruptionSteps.class));
  }

  @Override
  public UpdateType getUpdateType(ResourceAction resourceAction, boolean stackTagsChanged) throws Exception {
    AWSEC2InstanceResourceAction otherAction = (AWSEC2InstanceResourceAction) resourceAction;
    if (info.getPhysicalResourceId() == null) {
      throw new ValidationErrorException("Can not call update on this instance.  It was never created");
    }
    ServiceConfiguration configuration = Topology.lookup(Compute.class);
    DescribeInstancesType describeInstancesType = MessageHelper.createMessage(DescribeInstancesType.class, info.getEffectiveUserId());
    describeInstancesType.getFilterSet( ).add( Filter.filter("instance-id", info.getPhysicalResourceId()) );
    DescribeInstancesResponseType describeInstancesResponseType = AsyncRequests.sendSync( configuration, describeInstancesType );
    if (describeInstancesResponseType.getReservationSet().size() == 0) {
      throw new ValidationErrorException("Instance " + info.getPhysicalResourceId( ) + " does not exist");
    }
    RunningInstancesItemType runningInstancesItemType = describeInstancesResponseType.getReservationSet().get(0).getInstancesSet().get(0);
    boolean isVpc = runningInstancesItemType.getVpcId() != null;
    boolean isEbs = "ebs".equals(runningInstancesItemType.getRootDeviceType());
    UpdateType updateType = info.supportsTags() && stackTagsChanged ? UpdateType.NO_INTERRUPTION : UpdateType.NONE;
    if (!Objects.equals(properties.getAdditionalInfo(), otherAction.properties.getAdditionalInfo())) {
      updateType = updateType.max(updateType, isEbs ? UpdateType.SOME_INTERRUPTION : UpdateType.NEEDS_REPLACEMENT);
    }
    if (!Objects.equals(properties.getAvailabilityZone(), otherAction.properties.getAvailabilityZone())) {
      updateType = updateType.max(updateType, UpdateType.NEEDS_REPLACEMENT);
    }
   if (!Objects.equals(properties.getBlockDeviceMappings(), otherAction.properties.getBlockDeviceMappings())) {
      if (onlyChangedDeleteOnTerminate(properties.getBlockDeviceMappings(), otherAction.properties.getBlockDeviceMappings())) {
        updateType = updateType.max(updateType, UpdateType.NO_INTERRUPTION);
      } else {
        updateType = updateType.max(updateType, UpdateType.NEEDS_REPLACEMENT);
      }
    }
    if (!Objects.equals(properties.getDisableApiTermination(), otherAction.properties.getDisableApiTermination())) {
      updateType = updateType.max(updateType, UpdateType.NO_INTERRUPTION);
    }
    if (!Objects.equals(properties.getEbsOptimized(), otherAction.properties.getEbsOptimized())) {
      updateType = updateType.max(updateType, isEbs ? UpdateType.SOME_INTERRUPTION : UpdateType.NEEDS_REPLACEMENT);
    }
    if (!Objects.equals(properties.getIamInstanceProfile(), otherAction.properties.getIamInstanceProfile())) {
      updateType = updateType.max(updateType, UpdateType.NEEDS_REPLACEMENT);
    }
    if (!Objects.equals(properties.getImageId(), otherAction.properties.getImageId())) {
      updateType = updateType.max(updateType, UpdateType.NEEDS_REPLACEMENT);
    }
    if (!Objects.equals(properties.getInstanceInitiatedShutdownBehavior(), otherAction.properties.getInstanceInitiatedShutdownBehavior())) {
      updateType = updateType.max(updateType, UpdateType.NO_INTERRUPTION);
    }
    if (!Objects.equals(properties.getInstanceType(), otherAction.properties.getInstanceType())) {
      updateType = updateType.max(updateType, isEbs ? UpdateType.SOME_INTERRUPTION : UpdateType.NEEDS_REPLACEMENT);
    }
    if (!Objects.equals(properties.getKernelId(), otherAction.properties.getKernelId())) {
      updateType = updateType.max(updateType, isEbs ? UpdateType.SOME_INTERRUPTION : UpdateType.NEEDS_REPLACEMENT);
    }
    if (!Objects.equals(properties.getKeyName(), otherAction.properties.getKeyName())) {
      updateType = updateType.max(updateType, UpdateType.NEEDS_REPLACEMENT);
    }
    if (!Objects.equals(properties.getMonitoring(), otherAction.properties.getMonitoring())) {
      updateType = updateType.max(updateType, UpdateType.NO_INTERRUPTION);
    }
    if (!Objects.equals(properties.getNetworkInterfaces(), otherAction.properties.getNetworkInterfaces())) {
      updateType = updateType.max(updateType, UpdateType.NEEDS_REPLACEMENT);
    }
    if (!Objects.equals(properties.getPlacementGroupName(), otherAction.properties.getPlacementGroupName())) {
      updateType = updateType.max(updateType, UpdateType.NEEDS_REPLACEMENT);
    }
    if (!Objects.equals(properties.getPrivateIpAddress(), otherAction.properties.getPrivateIpAddress())) {
      updateType = updateType.max(updateType, UpdateType.NEEDS_REPLACEMENT);
    }
    if (!Objects.equals(properties.getRamdiskId(), otherAction.properties.getRamdiskId())) {
      updateType = updateType.max(updateType, isEbs ? UpdateType.SOME_INTERRUPTION : UpdateType.NEEDS_REPLACEMENT);
    }
    if (!Objects.equals(properties.getSecurityGroupIds(), otherAction.properties.getSecurityGroupIds())) {
      updateType = updateType.max(updateType, isVpc ? UpdateType.NO_INTERRUPTION : UpdateType.NEEDS_REPLACEMENT);
    }
    if (!Objects.equals(properties.getSecurityGroups(), otherAction.properties.getSecurityGroups())) {
      updateType = updateType.max(updateType, UpdateType.NEEDS_REPLACEMENT);
    }
    if (!Objects.equals(properties.getSourceDestCheck(), otherAction.properties.getSourceDestCheck())) {
      updateType = updateType.max(updateType, UpdateType.NO_INTERRUPTION);
    }
    if (!Objects.equals(properties.getSubnetId(), otherAction.properties.getSubnetId())) {
      updateType = updateType.max(updateType, UpdateType.NEEDS_REPLACEMENT);
    }
    if (!Objects.equals(properties.getTags(), otherAction.properties.getTags())) {
      updateType = updateType.max(updateType, UpdateType.NO_INTERRUPTION);
    }
    if (!Objects.equals(properties.getTenancy(), otherAction.properties.getTenancy())) {
      updateType = updateType.max(updateType, UpdateType.NEEDS_REPLACEMENT);
    }
    if (!Objects.equals(properties.getUserData(), otherAction.properties.getUserData())) {
      updateType = updateType.max(updateType, isEbs ? UpdateType.SOME_INTERRUPTION : UpdateType.NEEDS_REPLACEMENT);
    }
    if (!Objects.equals(properties.getVolumes(), otherAction.properties.getVolumes())) {
      updateType = updateType.max(updateType, UpdateType.NO_INTERRUPTION);
    }
    return updateType;
  }

  private enum CreateSteps implements Step {
    RUN_INSTANCE {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        AWSEC2InstanceResourceAction action = (AWSEC2InstanceResourceAction) resourceAction;
        ServiceConfiguration configuration = Topology.lookup(Compute.class);
        RunInstancesType runInstancesType = MessageHelper.createMessage(RunInstancesType.class, action.info.getEffectiveUserId());
        runInstancesType.setImageId(action.properties.getImageId());
        if (action.properties.getAdditionalInfo() != null && !action.properties.getAdditionalInfo().isEmpty()) {
          runInstancesType.setAdditionalInfo(action.properties.getAdditionalInfo());
        }
        if (action.properties.getAvailabilityZone() != null && !action.properties.getAvailabilityZone().isEmpty()) {
          runInstancesType.setAvailabilityZone(action.properties.getAvailabilityZone());
        }
        if (action.properties.getBlockDeviceMappings() != null && !action.properties.getBlockDeviceMappings().isEmpty()) {
          runInstancesType.setBlockDeviceMapping(action.convertBlockDeviceMappings(action.properties.getBlockDeviceMappings()));
        }
        if (action.properties.getDisableApiTermination() != null) {
          runInstancesType.setDisableTerminate(action.properties.getDisableApiTermination());
        }
        if (action.properties.getEbsOptimized() != null) {
          runInstancesType.setEbsOptimized(action.properties.getEbsOptimized());
        }
        if (action.properties.getIamInstanceProfile() != null && !action.properties.getIamInstanceProfile().isEmpty()) {
          runInstancesType.setIamInstanceProfileName(action.properties.getIamInstanceProfile());
        }
        if (action.properties.getInstanceType() != null && !action.properties.getInstanceType().isEmpty()) {
          runInstancesType.setInstanceType(action.properties.getInstanceType());
        }
        if (action.properties.getKernelId() != null && !action.properties.getKernelId().isEmpty()) {
          runInstancesType.setKernelId(action.properties.getKernelId());
        }
        if (action.properties.getKeyName() != null && !action.properties.getKeyName().isEmpty()) {
          runInstancesType.setKeyName(action.properties.getKeyName());
        }
        if (action.properties.getMonitoring() != null) {
          runInstancesType.setMonitoring(action.properties.getMonitoring());
        }
        if (action.properties.getNetworkInterfaces() != null && !action.properties.getNetworkInterfaces().isEmpty()) {
          runInstancesType.setNetworkInterfaceSet(action.convertNetworkInterfaceSet(action.properties.getNetworkInterfaces()));
        }
        if (action.properties.getSecurityGroupIds() != null && !action.properties.getSecurityGroupIds().isEmpty() &&
          action.properties.getNetworkInterfaces() != null && !action.properties.getNetworkInterfaces().isEmpty()) {
          throw new ValidationErrorException("SecurityGroupIds and NetworkInterfaces can not both be set on an AWS::EC2::Instance");
        }
        if (action.properties.getSecurityGroups() != null && !action.properties.getSecurityGroups().isEmpty() &&
          action.properties.getNetworkInterfaces() != null && !action.properties.getNetworkInterfaces().isEmpty()) {
          throw new ValidationErrorException("SecurityGroups and NetworkInterfaces can not both be set on an AWS::EC2::Instance");
        }
        if (action.properties.getPlacementGroupName() != null && !action.properties.getPlacementGroupName().isEmpty()) {
          runInstancesType.setPlacementGroup(action.properties.getPlacementGroupName());
        }
        if (action.properties.getPrivateIpAddress() != null && !action.properties.getPrivateIpAddress().isEmpty()) {
          runInstancesType.setPrivateIpAddress(action.properties.getPrivateIpAddress());
        }
        if (action.properties.getRamdiskId() != null && !action.properties.getRamdiskId().isEmpty()) {
          runInstancesType.setRamdiskId(action.properties.getRamdiskId());
        }
        if (action.properties.getSecurityGroupIds() != null && !action.properties.getSecurityGroupIds().isEmpty() &&
          action.properties.getSecurityGroups() != null && !action.properties.getSecurityGroups().isEmpty()) {
          throw new ValidationErrorException("SecurityGroupIds and SecurityGroups can not both be set on an AWS::EC2::Instance");
        }
        if (action.properties.getSecurityGroupIds() != null && !action.properties.getSecurityGroupIds().isEmpty()) {
          runInstancesType.setGroupIdSet(Lists.newArrayList(action.properties.getSecurityGroupIds()));
        }
        if (action.properties.getSecurityGroups() != null && !action.properties.getSecurityGroups().isEmpty()) {
          runInstancesType.setGroupSet(Lists.newArrayList(action.properties.getSecurityGroups()));
        }
        if (action.properties.getSubnetId() != null && !action.properties.getSubnetId().isEmpty()) {
          runInstancesType.setSubnetId(action.properties.getSubnetId());
        }
        if (action.properties.getTenancy() != null && !action.properties.getTenancy().isEmpty()) {
          if (!"default".equals(action.properties.getTenancy()) && !"dedicated".equals(action.properties.getTenancy())) {
            throw new ValidationErrorException("Tenancy must be 'default' or 'dedicated'");
          }
          runInstancesType.setPlacementTenancy(action.properties.getTenancy());
        }
        if (action.properties.getUserData() != null && !action.properties.getUserData().isEmpty()) {
          runInstancesType.setUserData(action.properties.getUserData());
        }

        // make sure all volumes exist and are available
        if (action.properties.getVolumes() != null && !action.properties.getVolumes().isEmpty()) {
          DescribeVolumesType describeVolumesType = MessageHelper.createMessage(DescribeVolumesType.class, action.info.getEffectiveUserId());
          ArrayList<String> volumeIds = Lists.newArrayList();
          for (EC2MountPoint ec2MountPoint : action.properties.getVolumes()) {
            volumeIds.add(ec2MountPoint.getVolumeId());
          }
          describeVolumesType.getFilterSet( ).add( Filter.filter( "volume-id", volumeIds ) );
          DescribeVolumesResponseType describeVolumesResponseType;
          try {
            describeVolumesResponseType = AsyncRequests.sendSync( configuration, describeVolumesType );
          } catch ( final Exception e ) {
            throw new ValidationErrorException("Error checking volumes " + asWebServiceErrorMessage( e, e.getMessage( ) ) );
          }
          Map<String, String> volumeStatusMap = Maps.newHashMap();
          for (Volume volume : describeVolumesResponseType.getVolumeSet()) {
            volumeStatusMap.put(volume.getVolumeId(), volume.getStatus());
          }
          for (String volumeId : volumeIds) {
            if (!volumeStatusMap.containsKey(volumeId)) {
              throw new ValidationErrorException("No such volume " + volumeId);
            } else if (!"available".equals(volumeStatusMap.get(volumeId))) {
              throw new ValidationErrorException("Volume " + volumeId + " not available");
            }
          }
        }

        runInstancesType.setMinCount(1);
        runInstancesType.setMaxCount(1);
        RunInstancesResponseType runInstancesResponseType = AsyncRequests.<RunInstancesType, RunInstancesResponseType>sendSync(configuration, runInstancesType);
        action.info.setPhysicalResourceId(runInstancesResponseType.getRsvInfo().getInstancesSet().get(0).getInstanceId());
        action.info.setCreatedEnoughToDelete(true);
        action.info.setEucaCreateStartTime(JsonHelper.getStringFromJsonNode(new TextNode("" + System.currentTimeMillis())));
        return action;
      }
    },
    SET_ATTRIBUTES {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        AWSEC2InstanceResourceAction action = (AWSEC2InstanceResourceAction) resourceAction;
        ServiceConfiguration configuration = Topology.lookup(Compute.class);
        if (action.properties.getInstanceInitiatedShutdownBehavior() != null || action.properties.getSourceDestCheck() != null) {
          ModifyInstanceAttributeType modifyInstanceAttributeType = MessageHelper.createMessage(ModifyInstanceAttributeType.class, action.info.getEffectiveUserId());
          modifyInstanceAttributeType.setInstanceId(action.info.getPhysicalResourceId());
          if (action.properties.getInstanceInitiatedShutdownBehavior() != null) {
            modifyInstanceAttributeType.setInstanceInitiatedShutdownBehavior(convertToAttributeValueType(action.properties.getInstanceInitiatedShutdownBehavior()));
          }
          if (action.properties.getSourceDestCheck() != null) {
            modifyInstanceAttributeType.setSourceDestCheck(convertToAttributeBooleanValueType(action.properties.getSourceDestCheck()));
          }
          AsyncRequests.sendSync(configuration, modifyInstanceAttributeType);
        }
        return action;
      }

      @Override
      public Integer getTimeout() {
        return null;
      }
    },
    WAIT_UNTIL_RUNNING {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        AWSEC2InstanceResourceAction action = (AWSEC2InstanceResourceAction) resourceAction;
        ServiceConfiguration configuration = Topology.lookup(Compute.class);
        DescribeInstancesType describeInstancesType = MessageHelper.createMessage(DescribeInstancesType.class, action.info.getEffectiveUserId());
        describeInstancesType.getFilterSet( ).add( Filter.filter( "instance-id", action.info.getPhysicalResourceId( ) ) );
        DescribeInstancesResponseType describeInstancesResponseType = AsyncRequests.sendSync( configuration, describeInstancesType );
        if (describeInstancesResponseType.getReservationSet().size() == 0) {
          throw new RetryAfterConditionCheckFailedException("Instance " + action.info.getPhysicalResourceId( ) + " does not yet exist");
        }
        RunningInstancesItemType runningInstancesItemType = describeInstancesResponseType.getReservationSet().get(0).getInstancesSet().get(0);
        if ("running".equals(runningInstancesItemType.getStateName())) {
          action.info.setPrivateIp(JsonHelper.getStringFromJsonNode(new TextNode(runningInstancesItemType.getPrivateIpAddress())));
          action.info.setPublicIp(JsonHelper.getStringFromJsonNode(new TextNode(runningInstancesItemType.getIpAddress())));
          action.info.setAvailabilityZone(JsonHelper.getStringFromJsonNode(new TextNode(runningInstancesItemType.getPlacement())));
          action.info.setPrivateDnsName(JsonHelper.getStringFromJsonNode(new TextNode(runningInstancesItemType.getPrivateDnsName())));
          action.info.setPublicDnsName(JsonHelper.getStringFromJsonNode(new TextNode(runningInstancesItemType.getDnsName())));
          action.info.setReferenceValueJson(JsonHelper.getStringFromJsonNode(new TextNode(action.info.getPhysicalResourceId())));
          return action;
        }
        throw new RetryAfterConditionCheckFailedException(("Instance " + action.info.getPhysicalResourceId() + " is not yet running, currently " + runningInstancesItemType.getStateName()));
      }

      @Override
      public Integer getTimeout() {
        return INSTANCE_RUNNING_MAX_CREATE_RETRY_SECS;
      }
    },
    CREATE_TAGS {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        AWSEC2InstanceResourceAction action = (AWSEC2InstanceResourceAction) resourceAction;
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
    },
    ATTACH_VOLUMES {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        AWSEC2InstanceResourceAction action = (AWSEC2InstanceResourceAction) resourceAction;
        ServiceConfiguration configuration = Topology.lookup(Compute.class);
        if (action.properties.getVolumes() != null && !action.properties.getVolumes().isEmpty()) {
          ArrayList<String> volumeIds = Lists.newArrayList();
          Map<String, String> deviceMap = Maps.newHashMap();
          for (EC2MountPoint ec2MountPoint : action.properties.getVolumes()) {
            volumeIds.add(ec2MountPoint.getVolumeId());
            deviceMap.put(ec2MountPoint.getVolumeId(), ec2MountPoint.getDevice());
            AttachVolumeType attachVolumeType = MessageHelper.createMessage(AttachVolumeType.class, action.info.getEffectiveUserId());
            attachVolumeType.setInstanceId(action.info.getPhysicalResourceId());
            attachVolumeType.setVolumeId(ec2MountPoint.getVolumeId());
            attachVolumeType.setDevice(ec2MountPoint.getDevice());
            AsyncRequests.<AttachVolumeType, AttachVolumeResponseType>sendSync(configuration, attachVolumeType);
          }
        }
        return action;
      }
    },
    WAIT_UNTIL_VOLUMES_ATTACHED {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        AWSEC2InstanceResourceAction action = (AWSEC2InstanceResourceAction) resourceAction;
        ServiceConfiguration configuration = Topology.lookup(Compute.class);
        if (action.properties.getVolumes() != null && !action.properties.getVolumes().isEmpty()) {
          ArrayList<String> volumeIds = Lists.newArrayList();
          Map<String, String> deviceMap = Maps.newHashMap();
          for (EC2MountPoint ec2MountPoint : action.properties.getVolumes()) {
            volumeIds.add(ec2MountPoint.getVolumeId());
            deviceMap.put(ec2MountPoint.getVolumeId(), ec2MountPoint.getDevice());
          }
          DescribeVolumesType describeVolumesType = MessageHelper.createMessage(DescribeVolumesType.class, action.info.getEffectiveUserId());
          describeVolumesType.getFilterSet( ).add( Filter.filter( "volume-id", volumeIds ) );
          DescribeVolumesResponseType describeVolumesResponseType;
          try {
            describeVolumesResponseType = AsyncRequests.sendSync( configuration, describeVolumesType );
          } catch ( Exception e ) {
            throw new RetryAfterConditionCheckFailedException("Error describing volumes: " + asWebServiceErrorMessage( e, e.getMessage( ) ) );
          }
          Map<String, String> volumeStatusMap = Maps.newHashMap();
          for (Volume volume : describeVolumesResponseType.getVolumeSet()) {
            for (AttachedVolume attachedVolume : volume.getAttachmentSet()) {
              if (attachedVolume.getInstanceId().equals(action.info.getPhysicalResourceId()) && attachedVolume.getDevice().equals(deviceMap.get(volume.getVolumeId()))) {
                volumeStatusMap.put(volume.getVolumeId(), attachedVolume.getStatus());
              }
            }
          }
          for (String volumeId : volumeIds) {
            if (!"attached".equals(volumeStatusMap.get(volumeId))) {
              throw new RetryAfterConditionCheckFailedException("One or more volumes is not yet attached to the instance");
            }
          }
        }
        return action;
      }

      @Override
      public Integer getTimeout() {
        return INSTANCE_ATTACH_VOLUME_MAX_CREATE_RETRY_SECS;
      }
    },
    CHECK_SIGNALS {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        AWSEC2InstanceResourceAction action = (AWSEC2InstanceResourceAction) resourceAction;
        CreationPolicy creationPolicy = CreationPolicy.parse(action.info.getCreationPolicyJson());
        if (creationPolicy != null && creationPolicy.getResourceSignal() != null) {
          if (creationPolicy.getResourceSignal().getCount() != 1) {
            throw new ValidationErrorException("ResourceSignal CreationPolicy property Count cannot be greater than 1 for EC2 instance resources");
          }
          // check for signals
          Collection<SignalEntity> signals = SignalEntityManager.getSignals(action.getStackEntity().getStackId(), action.info.getAccountId(), action.info.getLogicalResourceId(),
            action.getStackEntity().getStackVersion());
          int numSuccessSignals = 0;
          if (signals != null) {
            for (SignalEntity signal : signals) {
              // For some reason AWS completely ignores signals that do not have the unique id of the instance id
              if (!Objects.equals(signal.getUniqueId(), action.info.getPhysicalResourceId())) continue;
              if (signal.getStatus() == SignalEntity.Status.FAILURE) {
                throw new ResourceFailureException("Received FAILURE signal with UniqueId " + signal.getUniqueId());
              }
              if (!signal.getProcessed()) {
                StackEventEntityManager.addSignalStackEvent(signal);
                signal.setProcessed(true);
                SignalEntityManager.updateSignal(signal);
              }
              numSuccessSignals++;
            }
          }
          if (numSuccessSignals < creationPolicy.getResourceSignal().getCount()) {
            long durationMs = System.currentTimeMillis() - Long.valueOf(JsonHelper.getJsonNodeFromString(action.info.getEucaCreateStartTime()).asText());
            if (TimeUnit.MILLISECONDS.toSeconds(durationMs) > creationPolicy.getResourceSignal().getTimeout()) {
              throw new ResourceFailureException("Failed to receive " + creationPolicy.getResourceSignal().getCount() + " resource signal(s) within the specified duration");
            }
            throw new RetryAfterConditionCheckFailedException("Not enough success signals yet");
          }
        }
        return action;
      }
      @Nullable
      @Override
      public Integer getTimeout() {
        return (int) TimeUnit.HOURS.toSeconds(12);
      }
    };

    @Nullable
    @Override
    public Integer getTimeout() {
      return null;
    }
  }


  private enum DeleteSteps implements Step {
    TERMINATE_INSTANCE {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        AWSEC2InstanceResourceAction action = (AWSEC2InstanceResourceAction) resourceAction;
        ServiceConfiguration configuration = Topology.lookup(Compute.class);
        // See if instance was ever populated
        if (!Boolean.TRUE.equals(action.info.getCreatedEnoughToDelete())) return action;
        // First see if instance exists or has been terminated
        DescribeInstancesType describeInstancesType = MessageHelper.createMessage(DescribeInstancesType.class, action.info.getEffectiveUserId());
        describeInstancesType.getFilterSet().add(Filter.filter("instance-id", action.info.getPhysicalResourceId()));
        DescribeInstancesResponseType describeInstancesResponseType = AsyncRequests.sendSync(configuration, describeInstancesType);
        if (describeInstancesResponseType.getReservationSet().size() == 0) return action; // already terminated
        if ("terminated".equals(describeInstancesResponseType.getReservationSet().get(0).getInstancesSet().get(0).getStateName())) {
          return action;
        }
         // Send terminate message (do not need to detatch volumes thankfully
        TerminateInstancesType terminateInstancesType = MessageHelper.createMessage(TerminateInstancesType.class, action.info.getEffectiveUserId());
        terminateInstancesType.setInstancesSet(Lists.newArrayList(action.info.getPhysicalResourceId()));
        AsyncRequests.<TerminateInstancesType, TerminateInstancesResponseType>sendSync(configuration, terminateInstancesType);
        return action;
      }
    },
    VERIFY_TERMINATED {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        AWSEC2InstanceResourceAction action = (AWSEC2InstanceResourceAction) resourceAction;
        ServiceConfiguration configuration = Topology.lookup(Compute.class);
        // See if instance was ever populated
        if (!Boolean.TRUE.equals(action.info.getCreatedEnoughToDelete())) return action;
        DescribeInstancesType describeInstancesType = MessageHelper.createMessage(DescribeInstancesType.class, action.info.getEffectiveUserId());
        describeInstancesType.getFilterSet( ).add( Filter.filter( "instance-id", action.info.getPhysicalResourceId( ) ) );
        DescribeInstancesResponseType describeInstancesResponseType = AsyncRequests.sendSync( configuration, describeInstancesType );
        if (describeInstancesResponseType.getReservationSet().size() == 0) return action; // already terminated
        if ("terminated".equals(describeInstancesResponseType.getReservationSet().get(0).getInstancesSet().get(0).getStateName())) {
          return action;
        }
        throw new RetryAfterConditionCheckFailedException(("Instance " + action.info.getPhysicalResourceId() + " is not yet terminated, currently " + describeInstancesResponseType.getReservationSet().get(0).getInstancesSet().get(0).getStateName()));
      }

      @Override
      public Integer getTimeout() {
        return INSTANCE_TERMINATED_MAX_DELETE_RETRY_SECS;
      }
    };

    @Nullable
    @Override
    public Integer getTimeout() {
      return null;
    }
  }


  private enum UpdateNoInterruptionSteps implements UpdateStep {
    UPDATE_ATTRIBUTES {
      @Override
      public ResourceAction perform(ResourceAction oldResourceAction, ResourceAction newResourceAction) throws Exception {
        AWSEC2InstanceResourceAction oldAction = (AWSEC2InstanceResourceAction) oldResourceAction;
        AWSEC2InstanceResourceAction newAction = (AWSEC2InstanceResourceAction) newResourceAction;
        ServiceConfiguration configuration = Topology.lookup(Compute.class);

        // a couple of checks before we change anything
        if (newAction.properties.getSecurityGroups() != null && !newAction.properties.getSecurityGroups().isEmpty() &&
          newAction.properties.getNetworkInterfaces() != null && !newAction.properties.getNetworkInterfaces().isEmpty()) {
          throw new ValidationErrorException("SecurityGroups and NetworkInterfaces can not both be set on an AWS::EC2::Instance");
        }
        if (newAction.properties.getSecurityGroupIds() != null && !newAction.properties.getSecurityGroupIds().isEmpty() &&
          newAction.properties.getNetworkInterfaces() != null && !newAction.properties.getNetworkInterfaces().isEmpty()) {
          throw new ValidationErrorException("SecurityGroupIds and NetworkInterfaces can not both be set on an AWS::EC2::Instance");
        }
        if (newAction.properties.getSecurityGroupIds() != null && !newAction.properties.getSecurityGroupIds().isEmpty() &&
          newAction.properties.getSecurityGroups() != null && !newAction.properties.getSecurityGroups().isEmpty()) {
          throw new ValidationErrorException("SecurityGroupIds and SecurityGroups can not both be set on an AWS::EC2::Instance");
        }
        // first lookup existing attributes
        DescribeInstancesType describeInstancesType = MessageHelper.createMessage(DescribeInstancesType.class, newAction.info.getEffectiveUserId());
        describeInstancesType.getFilterSet().add(Filter.filter("instance-id", newAction.info.getPhysicalResourceId()));
        DescribeInstancesResponseType describeInstancesResponseType = AsyncRequests.sendSync(configuration, describeInstancesType);
        if (describeInstancesResponseType.getReservationSet().size() == 0) {
          throw new ValidationErrorException("Instance " + newAction.info.getPhysicalResourceId() + " does not exist");
        }
        RunningInstancesItemType runningInstancesItemType = describeInstancesResponseType.getReservationSet().get(0).getInstancesSet().get(0);
        if (!blockDeviceMappingsDeleteOnTerminateEquals(runningInstancesItemType.getBlockDevices(), newAction.properties.getBlockDeviceMappings())) {
          if (newAction.properties.getBlockDeviceMappings() != null && !newAction.properties.getBlockDeviceMappings().isEmpty()) {
            ModifyInstanceAttributeType modifyInstanceAttributeType = MessageHelper.createMessage(ModifyInstanceAttributeType.class, newAction.info.getEffectiveUserId());
            modifyInstanceAttributeType.setInstanceId(newAction.info.getPhysicalResourceId());
            modifyInstanceAttributeType.setBlockDeviceMappingSet(convertToBlockMappingWithDeleteOnTerminateValues(newAction.properties.getBlockDeviceMappings()));
            AsyncRequests.sendSync(configuration, modifyInstanceAttributeType);
          } else {
            // If here, oldAction and newAction block device mapping must only differ by a value in the
            // 'DeleteOnTermination' in one or more of the EBS attached block devices.
            // This means that BlockDeviceMapping can not be null or empty in the old template and
            // not-null and not empty in the new template.  Experimentation shows a 'null' value
            // in block device mapping on an update will not change the delete-on-terminate value of the
            // root volume.
            // thus we just check the old volume is null or empty, in which case we do nothing,
            // otherwise something funny is going on.
            if (oldAction.properties.getBlockDeviceMappings() != null && !oldAction.properties.getBlockDeviceMappings().isEmpty()) {
              throw new ValidationErrorException("Unable to update block device mappings.  Null value somehow equated with non-null value");
            }
          }
        }
        if (!Objects.equals(runningInstancesItemType.getDisableApiTermination(), newAction.properties.getDisableApiTermination())) {
          if (newAction.properties.getDisableApiTermination() != null) {
            ModifyInstanceAttributeType modifyInstanceAttributeType = MessageHelper.createMessage(ModifyInstanceAttributeType.class, newAction.info.getEffectiveUserId());
            modifyInstanceAttributeType.setInstanceId(newAction.info.getPhysicalResourceId());
            modifyInstanceAttributeType.setDisableApiTermination(convertToAttributeBooleanValueType(newAction.properties.getDisableApiTermination()));
            AsyncRequests.sendSync(configuration, modifyInstanceAttributeType);
          } else {
            resetInstanceAttribute(configuration, newAction, "disableApiTermination");
          }
        }
        if (!Objects.equals(runningInstancesItemType.getInstanceInitiatedShutdownBehavior(), newAction.properties.getInstanceInitiatedShutdownBehavior())) {
          if (newAction.properties.getInstanceInitiatedShutdownBehavior() != null) {
            ModifyInstanceAttributeType modifyInstanceAttributeType = MessageHelper.createMessage(ModifyInstanceAttributeType.class, newAction.info.getEffectiveUserId());
            modifyInstanceAttributeType.setInstanceId(newAction.info.getPhysicalResourceId());
            modifyInstanceAttributeType.setInstanceInitiatedShutdownBehavior(convertToAttributeValueType(newAction.properties.getInstanceInitiatedShutdownBehavior()));
            AsyncRequests.sendSync(configuration, modifyInstanceAttributeType);
          } else {
            // AWS does not support resetInstanceAttribute( on "instanceInitiatedShutdownBehavior"
            ModifyInstanceAttributeType modifyInstanceAttributeType = MessageHelper.createMessage(ModifyInstanceAttributeType.class, newAction.info.getEffectiveUserId());
            modifyInstanceAttributeType.setInstanceId(newAction.info.getPhysicalResourceId());
            modifyInstanceAttributeType.setInstanceInitiatedShutdownBehavior(convertToAttributeValueType("stop"));
            AsyncRequests.sendSync(configuration, modifyInstanceAttributeType);
          }
        }
        // check security group ids on vpc, but only if the network interfaces property is not set.  If anything has changed in network interfaces,
        // we run with replacement anyway
        if (newAction.properties.getNetworkInterfaces() == null || newAction.properties.getNetworkInterfaces().isEmpty()) {
          // Only update the 'groupSet' (i.e. security group ids) if: vpc is enabled -- otherwise needs replacement anyway
          if (runningInstancesItemType.getVpcId() != null) {
            List<String> newGroups = defaultSecurityGroupInVpcIfNullOrEmpty(configuration, runningInstancesItemType.getVpcId(), newAction.info.getEffectiveUserId(), newAction.properties.getSecurityGroupIds());
            if (!groupIdsEquals(runningInstancesItemType.getGroupSet(), newGroups)) {
              ModifyInstanceAttributeType modifyInstanceAttributeType = MessageHelper.createMessage(ModifyInstanceAttributeType.class, newAction.info.getEffectiveUserId());
              modifyInstanceAttributeType.setInstanceId(newAction.info.getPhysicalResourceId());
              modifyInstanceAttributeType.setGroupIdSet(convertToGroupIdSet(newGroups));
              AsyncRequests.sendSync(configuration, modifyInstanceAttributeType);
            }
          }
        }
        if (!Objects.equals(runningInstancesItemType.getMonitoring(), BoolToString(newAction.properties.getMonitoring()))) {
          if (!Boolean.TRUE.equals(newAction.properties.getMonitoring())) {
            UnmonitorInstancesType unmonitorInstancesType = MessageHelper.createMessage(UnmonitorInstancesType.class, newAction.info.getEffectiveUserId());
            unmonitorInstancesType.setInstancesSet(Lists.newArrayList(newAction.info.getPhysicalResourceId()));
            AsyncRequests.sendSync(configuration, unmonitorInstancesType);
          } else {
            MonitorInstancesType monitorInstancesType = MessageHelper.createMessage(MonitorInstancesType.class, newAction.info.getEffectiveUserId());
            monitorInstancesType.setInstancesSet(Lists.newArrayList(newAction.info.getPhysicalResourceId()));
            AsyncRequests.sendSync(configuration, monitorInstancesType);
          }
        }
        // EUCA-12124: regardless ofvalue runningInstancesItemType.getSourceDestCheck() is null so we get the value another way
        DescribeInstanceAttributeType describeInstanceAttributeType = MessageHelper.createMessage(DescribeInstanceAttributeType.class, newAction.info.getEffectiveUserId());
        describeInstanceAttributeType.setInstanceId(newAction.info.getPhysicalResourceId());
        describeInstanceAttributeType.setAttribute("sourceDestCheck");
        DescribeInstanceAttributeResponseType describeInstanceAttributeResponseType = AsyncRequests.sendSync(configuration, describeInstanceAttributeType);
        if (!Objects.equals(describeInstanceAttributeResponseType.getSourceDestCheck(), newAction.properties.getSourceDestCheck())) {
          if (newAction.properties.getSourceDestCheck() != null) {
            ModifyInstanceAttributeType modifyInstanceAttributeType = MessageHelper.createMessage(ModifyInstanceAttributeType.class, newAction.info.getEffectiveUserId());
            modifyInstanceAttributeType.setInstanceId(newAction.info.getPhysicalResourceId());
            modifyInstanceAttributeType.setSourceDestCheck(convertToAttributeBooleanValueType(newAction.properties.getSourceDestCheck()));
            AsyncRequests.sendSync(configuration, modifyInstanceAttributeType);
          } else {
            resetInstanceAttribute(configuration, newAction, "sourceDestCheck");
          }
        }
        return newAction;
      }

      private String BoolToString(Boolean b) {
        return b == null ? null : String.valueOf(b.booleanValue());
      }
    },
    UPDATE_TAGS {
      @Override
      public ResourceAction perform(ResourceAction oldResourceAction, ResourceAction newResourceAction) throws Exception {
        AWSEC2InstanceResourceAction oldAction = (AWSEC2InstanceResourceAction) oldResourceAction;
        AWSEC2InstanceResourceAction newAction = (AWSEC2InstanceResourceAction) newResourceAction;
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
    },

    DETACH_OLD_VOLUMES {
      @Override
      public ResourceAction perform(ResourceAction oldResourceAction, ResourceAction newResourceAction) throws Exception {
        AWSEC2InstanceResourceAction oldAction = (AWSEC2InstanceResourceAction) oldResourceAction;
        AWSEC2InstanceResourceAction newAction = (AWSEC2InstanceResourceAction) newResourceAction;
        ServiceConfiguration configuration = Topology.lookup(Compute.class);
        Map<String, String> oldVolumeDeviceMap = getVolumeDeviceMap(oldAction);
        Map<String, String> newVolumeDeviceMap = getVolumeDeviceMap(newAction);
        Map<String, String> detachingVolumeDeviceMap = getDetachingVolumeDeviceMap(oldVolumeDeviceMap, newVolumeDeviceMap);
        DescribeVolumesType describeVolumesType = MessageHelper.createMessage(DescribeVolumesType.class, newAction.info.getEffectiveUserId());
        describeVolumesType.getFilterSet( ).add( Filter.filter( "volume-id", detachingVolumeDeviceMap.keySet() ) );
        DescribeVolumesResponseType describeVolumesResponseType = AsyncRequests.sendSync( configuration, describeVolumesType );
        if (describeVolumesResponseType != null && describeVolumesResponseType.getVolumeSet() != null) {
          for (Volume volume : describeVolumesResponseType.getVolumeSet()) {
            if (!detachingVolumeDeviceMap.containsKey(volume.getVolumeId()))
              continue; // shouldn't happen but unrelated volume.  I don't care
            if (volume.getAttachmentSet() != null) {
              for (AttachedVolume attachedVolume : volume.getAttachmentSet()) {
                // only detach volumes that match the old input record.  Other things that might match (new device or
                // instance id) means someone changed something outside cloudformation.  We will not second guess that.
                if (detachingVolumeDeviceMap.containsKey(attachedVolume.getVolumeId()) &&
                  newAction.info.getPhysicalResourceId().equals(attachedVolume.getInstanceId()) &&
                  detachingVolumeDeviceMap.get(attachedVolume.getVolumeId()).equals(attachedVolume.getDevice())) {
                  DetachVolumeType detachVolumeType = MessageHelper.createMessage(DetachVolumeType.class, newAction.info.getEffectiveUserId());
                  detachVolumeType.setVolumeId(attachedVolume.getVolumeId());
                  detachVolumeType.setInstanceId(newAction.info.getPhysicalResourceId());
                  AsyncRequests.sendSync(configuration, detachVolumeType);
                }
              }
            }
          }
        }
        return newAction;
      }
    },
    WAIT_UNTIL_OLD_VOLUMES_NOT_ATTACHED {
      @Override
      public ResourceAction perform(ResourceAction oldResourceAction, ResourceAction newResourceAction) throws Exception {
        AWSEC2InstanceResourceAction oldAction = (AWSEC2InstanceResourceAction) oldResourceAction;
        AWSEC2InstanceResourceAction newAction = (AWSEC2InstanceResourceAction) newResourceAction;
        ServiceConfiguration configuration = Topology.lookup(Compute.class);
        Map<String, String> oldVolumeDeviceMap = getVolumeDeviceMap(oldAction);
        Map<String, String> newVolumeDeviceMap = getVolumeDeviceMap(newAction);
        Map<String, String> detachingVolumeDeviceMap = getDetachingVolumeDeviceMap(oldVolumeDeviceMap, newVolumeDeviceMap);
        // a bit of a strange situation here.  In the previous step we only detached old volumes that were in a sense
        // 'correct'.  (i.e. existed and had the old instance id and device associated with them.  As such our test here
        // is not so much that the volumes are 'available' as some of them may be attached to other instances or at
        // other device ports.  We just make sure we don't have any devices that still have an attachment set that contains
        // the 'correct' information
        boolean stillAttached = false;
        DescribeVolumesType describeVolumesType = MessageHelper.createMessage(DescribeVolumesType.class, newAction.info.getEffectiveUserId());
        if (!detachingVolumeDeviceMap.isEmpty()) {
          describeVolumesType.getFilterSet().add(Filter.filter("volume-id", detachingVolumeDeviceMap.keySet()));
          DescribeVolumesResponseType describeVolumesResponseType = AsyncRequests.sendSync(configuration, describeVolumesType);
          if (describeVolumesResponseType != null && describeVolumesResponseType.getVolumeSet() != null) {
            for (Volume volume : describeVolumesResponseType.getVolumeSet()) {
              if (!detachingVolumeDeviceMap.containsKey(volume.getVolumeId()))
                continue;
              if (volume.getAttachmentSet() != null) {
                for (AttachedVolume attachedVolume : volume.getAttachmentSet()) {
                  if (detachingVolumeDeviceMap.containsKey(attachedVolume.getVolumeId()) &&
                    newAction.info.getPhysicalResourceId().equals(attachedVolume.getInstanceId()) &&
                    detachingVolumeDeviceMap.get(attachedVolume.getVolumeId()).equals(attachedVolume.getDevice())) {
                    // might still be lingering in the detached state
                    if (!"detached".equals(attachedVolume.getStatus())) {
                      stillAttached = true;
                      break;
                    }
                  }
                }
              }
            }
          }
        }
        if (stillAttached) {
          throw new RetryAfterConditionCheckFailedException("One or more volumes is not yet detached from the instance");
        }
        return newAction;
      }

      @Override
      public Integer getTimeout() {
        return INSTANCE_DETACH_VOLUME_MAX_UPDATE_RETRY_SECS;
      }
    },
    ATTACH_NEW_VOLUMES {
      public ResourceAction perform(ResourceAction oldResourceAction, ResourceAction newResourceAction) throws Exception {
        AWSEC2InstanceResourceAction oldAction = (AWSEC2InstanceResourceAction) oldResourceAction;
        AWSEC2InstanceResourceAction newAction = (AWSEC2InstanceResourceAction) newResourceAction;
        ServiceConfiguration configuration = Topology.lookup(Compute.class);
        Map<String, String> oldVolumeDeviceMap = getVolumeDeviceMap(oldAction);
        Map<String, String> newVolumeDeviceMap = getVolumeDeviceMap(newAction);
        Map<String, String> attachingVolumeDeviceMap = getAttachingVolumeDeviceMap(oldVolumeDeviceMap, newVolumeDeviceMap);
        for (String volumeId: attachingVolumeDeviceMap.keySet()) {
          AttachVolumeType attachVolumeType = MessageHelper.createMessage(AttachVolumeType.class, newAction.info.getEffectiveUserId());
          attachVolumeType.setVolumeId(volumeId);
          attachVolumeType.setInstanceId(newAction.info.getPhysicalResourceId());
          attachVolumeType.setDevice(attachingVolumeDeviceMap.get(volumeId));
          AsyncRequests.<AttachVolumeType, AttachVolumeResponseType>sendSync(configuration, attachVolumeType);
        }
        return newAction;
      }
    },
    WAIT_UNTIL_NEW_VOLUMES_ATTACHED {
      @Override
      public ResourceAction perform(ResourceAction oldResourceAction, ResourceAction newResourceAction) throws Exception {
        AWSEC2InstanceResourceAction oldAction = (AWSEC2InstanceResourceAction) oldResourceAction;
        AWSEC2InstanceResourceAction newAction = (AWSEC2InstanceResourceAction) newResourceAction;
        ServiceConfiguration configuration = Topology.lookup(Compute.class);
        Map<String, String> oldVolumeDeviceMap = getVolumeDeviceMap(oldAction);
        Map<String, String> newVolumeDeviceMap = getVolumeDeviceMap(newAction);
        Map<String, String> attachingVolumeDeviceMap = getAttachingVolumeDeviceMap(oldVolumeDeviceMap, newVolumeDeviceMap);
        if (!attachingVolumeDeviceMap.isEmpty()) {
          Map<String, String> volumeStatusMap = Maps.newHashMap();
          Collection<String> volumeIds = attachingVolumeDeviceMap.keySet();
          DescribeVolumesType describeVolumesType = MessageHelper.createMessage(DescribeVolumesType.class, newAction.info.getEffectiveUserId());
          describeVolumesType.getFilterSet( ).add( Filter.filter( "volume-id", volumeIds ) );
          DescribeVolumesResponseType describeVolumesResponseType = AsyncRequests.sendSync( configuration, describeVolumesType );
          for (Volume volume : describeVolumesResponseType.getVolumeSet()) {
            for (AttachedVolume attachedVolume : volume.getAttachmentSet()) {
              if (attachedVolume.getInstanceId().equals(newAction.info.getPhysicalResourceId()) && attachedVolume.getDevice().equals(attachingVolumeDeviceMap.get(volume.getVolumeId()))) {
                volumeStatusMap.put(volume.getVolumeId(), attachedVolume.getStatus());
              }
            }
          }
          for (String volumeId : volumeIds) {
            if (!"attached".equals(volumeStatusMap.get(volumeId))) {
              throw new RetryAfterConditionCheckFailedException("One or more volumes is not yet attached to the instance");
            }
          }
        }
        return newAction;
      }

      @Override
      public Integer getTimeout() {
        return INSTANCE_ATTACH_VOLUME_MAX_UPDATE_RETRY_SECS;
      }
    };

    @Nullable
    @Override
    public Integer getTimeout() {
      return null;
    }
  }

  private enum UpdateSomeInterruptionSteps implements UpdateStep {
    UPDATE_NO_INTERRUPTION_ATTRIBUTES {
      @Override
      public ResourceAction perform(ResourceAction oldResourceAction, ResourceAction newResourceAction) throws Exception {
        return UpdateNoInterruptionSteps.UPDATE_ATTRIBUTES.perform(oldResourceAction, newResourceAction);
      }
      @Nullable
      @Override
      public Integer getTimeout() {
        return UpdateNoInterruptionSteps.UPDATE_ATTRIBUTES.getTimeout();
      }
    },
    UPDATE_TAGS {
      @Override
      public ResourceAction perform(ResourceAction oldResourceAction, ResourceAction newResourceAction) throws Exception {
        return UpdateNoInterruptionSteps.UPDATE_TAGS.perform(oldResourceAction, newResourceAction);
      }
      @Nullable
      @Override
      public Integer getTimeout() {
        return UpdateNoInterruptionSteps.UPDATE_TAGS.getTimeout();
      }
    },

    DETACH_OLD_VOLUMES {
      @Override
      public ResourceAction perform(ResourceAction oldResourceAction, ResourceAction newResourceAction) throws Exception {
        return UpdateNoInterruptionSteps.DETACH_OLD_VOLUMES.perform(oldResourceAction, newResourceAction);
      }
      @Nullable
      @Override
      public Integer getTimeout() {
        return UpdateNoInterruptionSteps.DETACH_OLD_VOLUMES.getTimeout();
      }
    },
    WAIT_UNTIL_OLD_VOLUMES_NOT_ATTACHED {
      @Override
      public ResourceAction perform(ResourceAction oldResourceAction, ResourceAction newResourceAction) throws Exception {
        return UpdateNoInterruptionSteps.WAIT_UNTIL_OLD_VOLUMES_NOT_ATTACHED.perform(oldResourceAction, newResourceAction);
      }
      @Nullable
      @Override
      public Integer getTimeout() {
        return UpdateNoInterruptionSteps.WAIT_UNTIL_OLD_VOLUMES_NOT_ATTACHED.getTimeout();
      }
    },
    ATTACH_NEW_VOLUMES {
      @Override
      public ResourceAction perform(ResourceAction oldResourceAction, ResourceAction newResourceAction) throws Exception {
        return UpdateNoInterruptionSteps.ATTACH_NEW_VOLUMES.perform(oldResourceAction, newResourceAction);
      }
      @Nullable
      @Override
      public Integer getTimeout() {
        return UpdateNoInterruptionSteps.ATTACH_NEW_VOLUMES.getTimeout();
      }
    },
    WAIT_UNTIL_NEW_VOLUMES_ATTACHED {
      @Override
      public ResourceAction perform(ResourceAction oldResourceAction, ResourceAction newResourceAction) throws Exception {
        return UpdateNoInterruptionSteps.WAIT_UNTIL_NEW_VOLUMES_ATTACHED.perform(oldResourceAction, newResourceAction);
      }
      @Nullable
      @Override
      public Integer getTimeout() {
        return UpdateNoInterruptionSteps.WAIT_UNTIL_NEW_VOLUMES_ATTACHED.getTimeout();
      }
    },
    STOP_INSTANCE {
      @Override
      public ResourceAction perform(ResourceAction oldResourceAction, ResourceAction newResourceAction) throws Exception {
        AWSEC2InstanceResourceAction oldAction = (AWSEC2InstanceResourceAction) oldResourceAction;
        AWSEC2InstanceResourceAction newAction = (AWSEC2InstanceResourceAction) newResourceAction;
        ServiceConfiguration configuration = Topology.lookup(Compute.class);
        StopInstancesType stopInstancesType = MessageHelper.createMessage(StopInstancesType.class, newAction.info.getEffectiveUserId());
        stopInstancesType.setInstancesSet(Lists.newArrayList(newAction.info.getPhysicalResourceId()));
        AsyncRequests.sendSync(configuration, stopInstancesType);
        return newAction;
      }
    },
    VERIFY_STOPPED {
      @Override
      public ResourceAction perform(ResourceAction oldResourceAction, ResourceAction newResourceAction) throws Exception {
        AWSEC2InstanceResourceAction oldAction = (AWSEC2InstanceResourceAction) oldResourceAction;
        AWSEC2InstanceResourceAction newAction = (AWSEC2InstanceResourceAction) newResourceAction;
        ServiceConfiguration configuration = Topology.lookup(Compute.class);
        DescribeInstancesType describeInstancesType = MessageHelper.createMessage(DescribeInstancesType.class, newAction.info.getEffectiveUserId());
        describeInstancesType.getFilterSet().add(Filter.filter("instance-id", newAction.info.getPhysicalResourceId()));
        DescribeInstancesResponseType describeInstancesResponseType = AsyncRequests.sendSync(configuration, describeInstancesType);
        if (describeInstancesResponseType.getReservationSet().size() == 0) throw new ValidationErrorException("Instance " + newAction.info.getPhysicalResourceId() + " not found.");
        if ("terminated".equals(describeInstancesResponseType.getReservationSet().get(0).getInstancesSet().get(0).getStateName())) {
          throw new ValidationErrorException("Instance " + newAction.info.getPhysicalResourceId() + " terminated.");
        }
        if ("stopped".equals(describeInstancesResponseType.getReservationSet().get(0).getInstancesSet().get(0).getStateName())) {
          return newAction;
        }
        throw new RetryAfterConditionCheckFailedException(("Instance " + newAction.info.getPhysicalResourceId() + " is not yet stopped, currently " + describeInstancesResponseType.getReservationSet().get(0).getInstancesSet().get(0).getStateName()));
      }
      public Integer getTimeout() {
        return INSTANCE_STOPPED_MAX_UPDATE_RETRY_SECS;
      }
    },
    UPDATE_SOME_INTERRUPTION_ATTRIBUTES {
      @Override
      public ResourceAction perform(ResourceAction oldResourceAction, ResourceAction newResourceAction) throws Exception {
        AWSEC2InstanceResourceAction oldAction = (AWSEC2InstanceResourceAction) oldResourceAction;
        AWSEC2InstanceResourceAction newAction = (AWSEC2InstanceResourceAction) newResourceAction;
        ServiceConfiguration configuration = Topology.lookup(Compute.class);
        // first lookup existing attributes
        DescribeInstancesType describeInstancesType = MessageHelper.createMessage(DescribeInstancesType.class, newAction.info.getEffectiveUserId());
        describeInstancesType.getFilterSet().add(Filter.filter("instance-id", newAction.info.getPhysicalResourceId()));
        DescribeInstancesResponseType describeInstancesResponseType = AsyncRequests.sendSync(configuration, describeInstancesType);
        if (describeInstancesResponseType.getReservationSet().size() == 0) {
          throw new ValidationErrorException("Instance " + newAction.info.getPhysicalResourceId() + " does not exist");
        }
        RunningInstancesItemType runningInstancesItemType = describeInstancesResponseType.getReservationSet().get(0).getInstancesSet().get(0);


        DescribeInstanceAttributeType describeInstanceAttributeType1 = MessageHelper.createMessage(DescribeInstanceAttributeType.class, newAction.info.getEffectiveUserId());
        describeInstanceAttributeType1.setInstanceId(newAction.info.getPhysicalResourceId());
        describeInstanceAttributeType1.setAttribute("ebsOptimized");
        DescribeInstanceAttributeResponseType describeInstanceAttributeResponseType1 = AsyncRequests.sendSync(configuration, describeInstanceAttributeType1);
        if (!Objects.equals(describeInstanceAttributeResponseType1.getEbsOptimized(), newAction.properties.getEbsOptimized())) {
          if (newAction.properties.getEbsOptimized() != null) {
            ModifyInstanceAttributeType modifyInstanceAttributeType = MessageHelper.createMessage(ModifyInstanceAttributeType.class, newAction.info.getEffectiveUserId());
            modifyInstanceAttributeType.setInstanceId(newAction.info.getPhysicalResourceId());
            modifyInstanceAttributeType.setEbsOptimized(convertToAttributeBooleanFlatValueType(newAction.properties.getEbsOptimized()));
            AsyncRequests.sendSync(configuration, modifyInstanceAttributeType);
          } else {
            ModifyInstanceAttributeType modifyInstanceAttributeType = MessageHelper.createMessage(ModifyInstanceAttributeType.class, newAction.info.getEffectiveUserId());
            modifyInstanceAttributeType.setInstanceId(newAction.info.getPhysicalResourceId());
            modifyInstanceAttributeType.setEbsOptimized(convertToAttributeBooleanFlatValueType(false));
            AsyncRequests.sendSync(configuration, modifyInstanceAttributeType);
          }
        }

        if (!Objects.equals(runningInstancesItemType.getInstanceType(), newAction.properties.getInstanceType())) {
          if (newAction.properties.getInstanceType() != null) {
            ModifyInstanceAttributeType modifyInstanceAttributeType = MessageHelper.createMessage(ModifyInstanceAttributeType.class, newAction.info.getEffectiveUserId());
            modifyInstanceAttributeType.setInstanceId(newAction.info.getPhysicalResourceId());
            modifyInstanceAttributeType.setInstanceType(convertToAttributeValueType(newAction.properties.getInstanceType()));
            AsyncRequests.sendSync(configuration, modifyInstanceAttributeType);
          } else {
            ModifyInstanceAttributeType modifyInstanceAttributeType = MessageHelper.createMessage(ModifyInstanceAttributeType.class, newAction.info.getEffectiveUserId());
            modifyInstanceAttributeType.setInstanceId(newAction.info.getPhysicalResourceId());
            modifyInstanceAttributeType.setInstanceType(convertToAttributeValueType("m1.small"));
            AsyncRequests.sendSync(configuration, modifyInstanceAttributeType);
          }
        }
        if (!Objects.equals(runningInstancesItemType.getKernel(), newAction.properties.getKernelId())) {
          if (newAction.properties.getKernelId() != null) {
            ModifyInstanceAttributeType modifyInstanceAttributeType = MessageHelper.createMessage(ModifyInstanceAttributeType.class, newAction.info.getEffectiveUserId());
            modifyInstanceAttributeType.setInstanceId(newAction.info.getPhysicalResourceId());
            modifyInstanceAttributeType.setKernel(convertToAttributeValueType(newAction.properties.getKernelId()));
            AsyncRequests.sendSync(configuration, modifyInstanceAttributeType);
          } else {
            resetInstanceAttribute(configuration, newAction, "kernel");
          }
        }
        if (!Objects.equals(runningInstancesItemType.getRamdisk(), newAction.properties.getRamdiskId())) {
          if (newAction.properties.getRamdiskId() != null) {
            ModifyInstanceAttributeType modifyInstanceAttributeType = MessageHelper.createMessage(ModifyInstanceAttributeType.class, newAction.info.getEffectiveUserId());
            modifyInstanceAttributeType.setInstanceId(newAction.info.getPhysicalResourceId());
            modifyInstanceAttributeType.setRamdisk(convertToAttributeValueType(newAction.properties.getRamdiskId()));
            AsyncRequests.sendSync(configuration, modifyInstanceAttributeType);
          } else {
            resetInstanceAttribute(configuration, newAction, "ramdisk");
          }
        }
        DescribeInstanceAttributeType describeInstanceAttributeType2 = MessageHelper.createMessage(DescribeInstanceAttributeType.class, newAction.info.getEffectiveUserId());
        describeInstanceAttributeType2.setInstanceId(newAction.info.getPhysicalResourceId());
        describeInstanceAttributeType2.setAttribute("userData");
        DescribeInstanceAttributeResponseType describeInstanceAttributeResponseType2 = AsyncRequests.sendSync(configuration, describeInstanceAttributeType2);
        // Null user data comes back as an empty string in describeInstanceAttributes.
        if (!Objects.equals(Strings.nullToEmpty(describeInstanceAttributeResponseType2.getUserData()), Strings.nullToEmpty(newAction.properties.getUserData()))) {
          if (newAction.properties.getUserData() != null) {
            ModifyInstanceAttributeType modifyInstanceAttributeType = MessageHelper.createMessage(ModifyInstanceAttributeType.class, newAction.info.getEffectiveUserId());
            modifyInstanceAttributeType.setInstanceId(newAction.info.getPhysicalResourceId());
            modifyInstanceAttributeType.setUserData(convertToAttributeValueType(newAction.properties.getUserData()));
            AsyncRequests.sendSync(configuration, modifyInstanceAttributeType);
          } else {
            ModifyInstanceAttributeType modifyInstanceAttributeType = MessageHelper.createMessage(ModifyInstanceAttributeType.class, newAction.info.getEffectiveUserId());
            modifyInstanceAttributeType.setInstanceId(newAction.info.getPhysicalResourceId());
            modifyInstanceAttributeType.setUserData(convertToAttributeValueType(""));
            AsyncRequests.sendSync(configuration, modifyInstanceAttributeType);
          }
        }
        return newAction;
      }
    },
    START_INSTANCE {
      @Override
      public ResourceAction perform(ResourceAction oldResourceAction, ResourceAction newResourceAction) throws Exception {
        AWSEC2InstanceResourceAction oldAction = (AWSEC2InstanceResourceAction) oldResourceAction;
        AWSEC2InstanceResourceAction newAction = (AWSEC2InstanceResourceAction) newResourceAction;
        ServiceConfiguration configuration = Topology.lookup(Compute.class);
        StartInstancesType startInstancesType = MessageHelper.createMessage(StartInstancesType.class, newAction.info.getEffectiveUserId());
        startInstancesType.setInstancesSet(Lists.newArrayList(newAction.info.getPhysicalResourceId()));
        startInstancesType.setAdditionalInfo(newAction.properties.getAdditionalInfo());
        AsyncRequests.sendSync(configuration, startInstancesType);
        return newAction;
      }
    },
    WAIT_UNTIL_RUNNING {
      @Override
      public ResourceAction perform(ResourceAction oldResourceAction, ResourceAction newResourceAction) throws Exception {
        return CreateSteps.WAIT_UNTIL_RUNNING.perform(newResourceAction);
      }

      @Override
      public Integer getTimeout() {
        return INSTANCE_RUNNING_MAX_UPDATE_RETRY_SECS;
      }
    };

    @Nullable
    @Override
    public Integer getTimeout() {
      return null;
    }
  };

  private static AttributeBooleanFlatValueType convertToAttributeBooleanFlatValueType(Boolean value) {
    AttributeBooleanFlatValueType attributeBooleanFlatValueType = new AttributeBooleanFlatValueType();
    attributeBooleanFlatValueType.setValue(value);
    return attributeBooleanFlatValueType;
  }

  private static void resetInstanceAttribute(ServiceConfiguration configuration, AWSEC2InstanceResourceAction action, String attribute) throws Exception {
    ResetInstanceAttributeType resetInstanceAttributeType = MessageHelper.createMessage(ResetInstanceAttributeType.class, action.info.getEffectiveUserId());
    resetInstanceAttributeType.setInstanceId(action.info.getPhysicalResourceId());
    resetInstanceAttributeType.setAttribute(attribute);
    AsyncRequests.sendSync(configuration, resetInstanceAttributeType);
  }

  private static Map<String, String> getDetachingVolumeDeviceMap(Map<String, String> oldVolumeDeviceMap, Map<String, String> newVolumeDeviceMap) {
    Map<String, String> detachingVolumeDeviceMap = Maps.newHashMap();
    for (String volumeId: oldVolumeDeviceMap.keySet()) {
      if (!newVolumeDeviceMap.containsKey(volumeId) || !Objects.equals(newVolumeDeviceMap.get(volumeId), oldVolumeDeviceMap.get(volumeId))) {
        detachingVolumeDeviceMap.put(volumeId, oldVolumeDeviceMap.get(volumeId));
      }
    }
    return detachingVolumeDeviceMap;
  }

  private static Map<String,String> getAttachingVolumeDeviceMap(Map<String, String> oldVolumeDeviceMap, Map<String, String> newVolumeDeviceMap) {
    Map<String, String> attachingVolumeDeviceMap = Maps.newHashMap();
    for (String volumeId: newVolumeDeviceMap.keySet()) {
      if (!oldVolumeDeviceMap.containsKey(volumeId) || !Objects.equals(newVolumeDeviceMap.get(volumeId), oldVolumeDeviceMap.get(volumeId))) {
        attachingVolumeDeviceMap.put(volumeId, newVolumeDeviceMap.get(volumeId));
      }
    }
    return attachingVolumeDeviceMap;
  }

  private static Map<String,String> getVolumeDeviceMap(AWSEC2InstanceResourceAction oldAction) {
    Map<String, String> volumeDeviceMap = Maps.newHashMap();
    if (oldAction.properties.getVolumes() != null) {
      for (EC2MountPoint volume : oldAction.properties.getVolumes()) {
        volumeDeviceMap.put(volume.getVolumeId(), volume.getDevice());
      }
    }
    return volumeDeviceMap;
  }

  private static boolean groupIdsEquals(ArrayList<GroupItemType> groupSet, List<String> securityGroupIds) {
    Set<String> set1 = Sets.newHashSet();
    if (groupSet != null) {
      for (GroupItemType groupItemType: groupSet) {
        set1.add(groupItemType.getGroupId());
      }
    }
    Set<String> set2 = Sets.newHashSet();
    if (securityGroupIds != null) {
      set2.addAll(securityGroupIds);
    }
    return Objects.equals(set1, set2);
  }

  private static GroupIdSetType convertToGroupIdSet(List<String> securityGroupIds) {
    GroupIdSetType groupIdSetType = new GroupIdSetType();
    for (String securityGroupId: securityGroupIds) {
      SecurityGroupIdSetItemType securityGroupIdSetItemType = new SecurityGroupIdSetItemType();
      securityGroupIdSetItemType.setGroupId(securityGroupId);
      groupIdSetType.getItem().add(securityGroupIdSetItemType);
    }
    return groupIdSetType;
  }

  private static InstanceBlockDeviceMappingSetType convertToBlockMappingWithDeleteOnTerminateValues(List<EC2BlockDeviceMapping> blockDeviceMappings) {
    InstanceBlockDeviceMappingSetType instanceBlockDeviceMappingSetType = new InstanceBlockDeviceMappingSetType();
    for (EC2BlockDeviceMapping blockDeviceMapping: blockDeviceMappings) {
      if (blockDeviceMapping.getEbs() == null) continue;
      InstanceBlockDeviceMappingItemType instanceBlockDeviceMappingItemType = new InstanceBlockDeviceMappingItemType();
      instanceBlockDeviceMappingItemType.setDeviceName(blockDeviceMapping.getDeviceName());
      InstanceEbsBlockDeviceType ebs = new InstanceEbsBlockDeviceType();
      ebs.setDeleteOnTermination(blockDeviceMapping.getEbs().getDeleteOnTermination() == null ? Boolean.TRUE : blockDeviceMapping.getEbs().getDeleteOnTermination());
      instanceBlockDeviceMappingItemType.setEbs(ebs);
      instanceBlockDeviceMappingSetType.getItem().add(instanceBlockDeviceMappingItemType);
    }
    return instanceBlockDeviceMappingSetType;
  }


  private ArrayList<BlockDeviceMappingItemType> convertBlockDeviceMappings(List<EC2BlockDeviceMapping> blockDeviceMappings) {
    if (blockDeviceMappings == null) return null;
    ArrayList<BlockDeviceMappingItemType> blockDeviceMappingItemTypes = Lists.newArrayList();
    for (EC2BlockDeviceMapping ec2BlockDeviceMapping : blockDeviceMappings) {
      BlockDeviceMappingItemType itemType = new BlockDeviceMappingItemType();
      itemType.setDeviceName(ec2BlockDeviceMapping.getDeviceName());
      itemType.setNoDevice(ec2BlockDeviceMapping.getNoDevice() != null ? Boolean.TRUE : null);
      itemType.setVirtualName(ec2BlockDeviceMapping.getVirtualName());
      if (ec2BlockDeviceMapping.getEbs() != null) {
        EbsDeviceMapping ebs = new EbsDeviceMapping();
        ebs.setDeleteOnTermination(ec2BlockDeviceMapping.getEbs().getDeleteOnTermination() == null ? Boolean.TRUE : ec2BlockDeviceMapping.getEbs().getDeleteOnTermination());
        ebs.setIops(ec2BlockDeviceMapping.getEbs().getIops());
        ebs.setSnapshotId(ec2BlockDeviceMapping.getEbs().getSnapshotId());
        ebs.setVolumeSize(ec2BlockDeviceMapping.getEbs().getVolumeSize() == null ? null : Integer.valueOf(ec2BlockDeviceMapping.getEbs().getVolumeSize()));
        ebs.setVolumeType(ec2BlockDeviceMapping.getEbs().getVolumeType() != null ? ec2BlockDeviceMapping.getEbs().getVolumeType() : "standard");
        itemType.setEbs(ebs);
      }
      blockDeviceMappingItemTypes.add(itemType);
    }
    return blockDeviceMappingItemTypes;
  }

  private static boolean blockDeviceMappingsDeleteOnTerminateEquals(ArrayList<InstanceBlockDeviceMapping> blockDevices, List<EC2BlockDeviceMapping> blockDeviceMappings) {
    Map<String, Boolean> map1 = Maps.newHashMap();
    if (blockDevices != null) {
      for (InstanceBlockDeviceMapping blockDevice: blockDevices) {
        if (blockDevice.getEbs() != null) {
          map1.put(blockDevice.getDeviceName(), blockDevice.getEbs().getDeleteOnTermination() == null ? Boolean.TRUE : blockDevice.getEbs().getDeleteOnTermination());
        }
      }
    }

    Map<String, Boolean> map2 = Maps.newHashMap();
    if (blockDeviceMappings != null) {
      for (EC2BlockDeviceMapping blockDeviceMapping: blockDeviceMappings) {
        if (blockDeviceMapping.getEbs() != null) {
          map2.put(blockDeviceMapping.getDeviceName(), blockDeviceMapping.getEbs().getDeleteOnTermination() == null ? Boolean.TRUE : blockDeviceMapping.getEbs().getDeleteOnTermination());
        }
      }
    }

    return Objects.equals(map1, map2);
  }

  @Override
  public void refreshAttributes() throws Exception {
    // This assumes everything is set, propertywise and attributewise
    ServiceConfiguration configuration = Topology.lookup(Compute.class);
    DescribeInstancesType describeInstancesType = MessageHelper.createMessage(DescribeInstancesType.class, info.getEffectiveUserId());
    describeInstancesType.getFilterSet( ).add( Filter.filter( "instance-id", info.getPhysicalResourceId( ) ) );
    DescribeInstancesResponseType describeInstancesResponseType = AsyncRequests.sendSync( configuration, describeInstancesType );
    if (describeInstancesResponseType.getReservationSet().size() == 0) {
      return;
    }
    RunningInstancesItemType runningInstancesItemType = describeInstancesResponseType.getReservationSet().get(0).getInstancesSet().get(0);
    info.setPrivateIp(JsonHelper.getStringFromJsonNode(new TextNode(runningInstancesItemType.getPrivateIpAddress())));
    info.setPublicIp(JsonHelper.getStringFromJsonNode(new TextNode(runningInstancesItemType.getIpAddress())));
    info.setAvailabilityZone(JsonHelper.getStringFromJsonNode(new TextNode(runningInstancesItemType.getPlacement())));
    info.setPrivateDnsName(JsonHelper.getStringFromJsonNode(new TextNode(runningInstancesItemType.getPrivateDnsName())));
    info.setPublicDnsName(JsonHelper.getStringFromJsonNode(new TextNode(runningInstancesItemType.getDnsName())));
    info.setReferenceValueJson(JsonHelper.getStringFromJsonNode(new TextNode(info.getPhysicalResourceId())));
  }

  private static AttributeValueType convertToAttributeValueType(String value) {
    AttributeValueType attributeValueType = new AttributeValueType();
    attributeValueType.setValue(value);
    return attributeValueType;
  }

  private static boolean onlyChangedDeleteOnTerminate(List<EC2BlockDeviceMapping> blockDeviceMappings1, List<EC2BlockDeviceMapping> blockDeviceMappings2) {
    if (blockDeviceMappings1 == null || blockDeviceMappings2 == null) return false;
    Map<String, EC2BlockDeviceMapping> device1Map = Maps.newHashMap();
    for (EC2BlockDeviceMapping ec2BlockDeviceMapping1: blockDeviceMappings1) {
      device1Map.put(ec2BlockDeviceMapping1.getDeviceName(), ec2BlockDeviceMapping1);
    }
    for (EC2BlockDeviceMapping ec2BlockDeviceMapping2: blockDeviceMappings2) {
      if (!device1Map.containsKey(ec2BlockDeviceMapping2.getDeviceName())) {
        return false;
      } else {
        // only delete on terminate can be different
        EC2BlockDeviceMapping ec2BlockDeviceMapping1 = device1Map.get(ec2BlockDeviceMapping2.getDeviceName());
        // already checked device names
        if (!Objects.equals(ec2BlockDeviceMapping1.getNoDevice(), ec2BlockDeviceMapping2.getNoDevice())) {
          return false;
        }
        if (!Objects.equals(ec2BlockDeviceMapping1.getVirtualName(), ec2BlockDeviceMapping2.getVirtualName())) {
          return false;
        }
        EC2EBSBlockDevice ebs1 = ec2BlockDeviceMapping1.getEbs();
        EC2EBSBlockDevice ebs2 = ec2BlockDeviceMapping2.getEbs();

        if (ebs1 != null && ebs2 == null) {
          return false;
        }
        if (ebs1 == null && ebs2 != null) {
          return false;
        }
        if (ebs1 == null && ebs2 == null) {
          continue;
        }
        if (!Objects.equals(ebs1.getIops(), ebs2.getIops())) {
          return false;
        }
        if (!Objects.equals(ebs1.getSnapshotId(), ebs2.getSnapshotId())) {
          return false;
        }
        if (!Objects.equals(ebs1.getVolumeSize(), ebs2.getVolumeSize())) {
          return false;
        }
        if (!Objects.equals(ebs1.getVolumeType(), ebs2.getVolumeType())) {
          return false;
        }
      }
      device1Map.remove(ec2BlockDeviceMapping2.getDeviceName());
    }
    if (!device1Map.isEmpty()) {
      return false;
    }
    return true;
  }


  private static AttributeBooleanValueType convertToAttributeBooleanValueType(Boolean value) {
    AttributeBooleanValueType attributeBooleanValueType = new AttributeBooleanValueType();
    attributeBooleanValueType.setValue(value);
    return attributeBooleanValueType;
  }


  private InstanceNetworkInterfaceSetRequestType convertNetworkInterfaceSet(List<EC2NetworkInterface> networkInterfaces) {
    InstanceNetworkInterfaceSetRequestType instanceNetworkInterfaceSetRequestType = new InstanceNetworkInterfaceSetRequestType();
    ArrayList<InstanceNetworkInterfaceSetItemRequestType> item = Lists.newArrayList();
    for (EC2NetworkInterface networkInterface: networkInterfaces) {
      InstanceNetworkInterfaceSetItemRequestType instanceNetworkInterfaceSetItemRequestType = new InstanceNetworkInterfaceSetItemRequestType();
      if (networkInterface.getNetworkInterfaceId() != null) {
        instanceNetworkInterfaceSetItemRequestType.setNetworkInterfaceId(networkInterface.getNetworkInterfaceId());
      }
      if (networkInterface.getDeviceIndex() != null) {
        instanceNetworkInterfaceSetItemRequestType.setDeviceIndex(networkInterface.getDeviceIndex());
      }
      if (networkInterface.getDeleteOnTermination() != null) {
        instanceNetworkInterfaceSetItemRequestType.setDeleteOnTermination(networkInterface.getDeleteOnTermination());
      }
      if (networkInterface.getAssociatePublicIpAddress() != null) {
        instanceNetworkInterfaceSetItemRequestType.setAssociatePublicIpAddress(networkInterface.getAssociatePublicIpAddress());
      }
      if (networkInterface.getDescription() != null) {
        instanceNetworkInterfaceSetItemRequestType.setDescription(networkInterface.getDescription());
      }
      if (networkInterface.getGroupSet() != null) {
        instanceNetworkInterfaceSetItemRequestType.setGroupSet(convertGroupSet(networkInterface.getGroupSet()));
      }
      if (networkInterface.getPrivateIpAddress() != null) {
        instanceNetworkInterfaceSetItemRequestType.setPrivateIpAddress(networkInterface.getPrivateIpAddress());
      }
      if (networkInterface.getPrivateIpAddresses() != null) {
        instanceNetworkInterfaceSetItemRequestType.setPrivateIpAddressesSet(convertPrivateIpAddressSet(networkInterface.getPrivateIpAddresses()));
      }
      if (networkInterface.getSecondaryPrivateIpAddressCount() != null) {
        instanceNetworkInterfaceSetItemRequestType.setSecondaryPrivateIpAddressCount(networkInterface.getSecondaryPrivateIpAddressCount());
      }
      if (networkInterface.getSubnetId() != null) {
        instanceNetworkInterfaceSetItemRequestType.setSubnetId(networkInterface.getSubnetId());
      }
      item.add(instanceNetworkInterfaceSetItemRequestType);
    }
    instanceNetworkInterfaceSetRequestType.setItem(item);
    return instanceNetworkInterfaceSetRequestType;
  }

  private PrivateIpAddressesSetRequestType convertPrivateIpAddressSet(List<EC2NetworkInterfacePrivateIPSpecification> privateIpAddresses) {
    if (privateIpAddresses == null) return null;
    PrivateIpAddressesSetRequestType privateIpAddressesSetRequestType = new PrivateIpAddressesSetRequestType();
    ArrayList<PrivateIpAddressesSetItemRequestType> item = Lists.newArrayList();
    for (EC2NetworkInterfacePrivateIPSpecification ec2NetworkInterfacePrivateIPSpecification: privateIpAddresses) {
      PrivateIpAddressesSetItemRequestType privateIpAddressesSetItemRequestType = new PrivateIpAddressesSetItemRequestType();
      if (ec2NetworkInterfacePrivateIPSpecification.getPrimary() != null) {
        privateIpAddressesSetItemRequestType.setPrimary(ec2NetworkInterfacePrivateIPSpecification.getPrimary());
      }
      if (ec2NetworkInterfacePrivateIPSpecification.getPrivateIpAddress() != null){
        privateIpAddressesSetItemRequestType.setPrivateIpAddress(ec2NetworkInterfacePrivateIPSpecification.getPrivateIpAddress());
      }
      item.add(privateIpAddressesSetItemRequestType);
    }
    privateIpAddressesSetRequestType.setItem(item);
    return privateIpAddressesSetRequestType;
  }

  private SecurityGroupIdSetType convertGroupSet(List<String> groupSet) {
    if (groupSet == null) return null;
    SecurityGroupIdSetType securityGroupIdSetType = new SecurityGroupIdSetType();
    ArrayList<SecurityGroupIdSetItemType> item = Lists.newArrayList();
    for (String groupId: groupSet) {
      SecurityGroupIdSetItemType securityGroupIdSetItemType = new SecurityGroupIdSetItemType();
      securityGroupIdSetItemType.setGroupId(groupId);
      item.add(securityGroupIdSetItemType);
    }
    securityGroupIdSetType.setItem(item);
    return securityGroupIdSetType;
  }

  private static List<String> defaultSecurityGroupInVpcIfNullOrEmpty(ServiceConfiguration configuration, String vpcId, String effectiveUserId, List<String> groupIds) throws Exception {
    if (groupIds != null && !groupIds.isEmpty()) return groupIds;
    DescribeSecurityGroupsType describeSecurityGroupsType = MessageHelper.createMessage(DescribeSecurityGroupsType.class, effectiveUserId);
    describeSecurityGroupsType.getFilterSet().add(Filter.filter("vpc-id", vpcId));
    describeSecurityGroupsType.getFilterSet().add(Filter.filter("group-name", "default"));
    DescribeSecurityGroupsResponseType describeSecurityGroupsResponseType = AsyncRequests.sendSync(configuration, describeSecurityGroupsType);
    if (describeSecurityGroupsResponseType == null || describeSecurityGroupsResponseType.getSecurityGroupInfo() == null ||
            describeSecurityGroupsResponseType.getSecurityGroupInfo().size() != 1) {
      throw new ValidationErrorException("Could not find unique default security group for vpc " + vpcId);
    }
    return Lists.newArrayList(describeSecurityGroupsResponseType.getSecurityGroupInfo().get(0).getGroupId());
  }
  @Override
  public ResourceProperties getResourceProperties() {
    return properties;
  }

  @Override
  public void setResourceProperties(ResourceProperties resourceProperties) {
    properties = (AWSEC2InstanceProperties) resourceProperties;
  }

  @Override
  public ResourceInfo getResourceInfo() {
    return info;
  }

  @Override
  public void setResourceInfo(ResourceInfo resourceInfo) {
    info = (AWSEC2InstanceResourceInfo) resourceInfo;
  }





}




