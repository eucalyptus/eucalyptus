/*************************************************************************
 * Copyright 2009-2014 Eucalyptus Systems, Inc.
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
import com.eucalyptus.auth.Accounts;
import com.eucalyptus.cloudformation.CloudFormationException;
import com.eucalyptus.cloudformation.ValidationErrorException;
import com.eucalyptus.cloudformation.resources.EC2Helper;
import com.eucalyptus.cloudformation.resources.ResourceAction;
import com.eucalyptus.cloudformation.resources.ResourceInfo;
import com.eucalyptus.cloudformation.resources.ResourceProperties;
import com.eucalyptus.cloudformation.resources.standard.TagHelper;
import com.eucalyptus.cloudformation.resources.standard.info.AWSEC2InstanceResourceInfo;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.AWSEC2InstanceProperties;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.EC2BlockDeviceMapping;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.EC2MountPoint;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.EC2NetworkInterface;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.EC2NetworkInterfacePrivateIPSpecification;
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
import com.eucalyptus.compute.common.AttachVolumeResponseType;
import com.eucalyptus.compute.common.AttachVolumeType;
import com.eucalyptus.compute.common.AttachedVolume;
import com.eucalyptus.compute.common.BlockDeviceMappingItemType;
import com.eucalyptus.compute.common.Compute;
import com.eucalyptus.compute.common.CreateTagsResponseType;
import com.eucalyptus.compute.common.CreateTagsType;
import com.eucalyptus.compute.common.DescribeInstancesResponseType;
import com.eucalyptus.compute.common.DescribeInstancesType;
import com.eucalyptus.compute.common.DescribeSecurityGroupsResponseType;
import com.eucalyptus.compute.common.DescribeSecurityGroupsType;
import com.eucalyptus.compute.common.DescribeVolumesResponseType;
import com.eucalyptus.compute.common.DescribeVolumesType;
import com.eucalyptus.compute.common.EbsDeviceMapping;
import com.eucalyptus.compute.common.GroupItemType;
import com.eucalyptus.compute.common.InstanceNetworkInterfaceSetItemRequestType;
import com.eucalyptus.compute.common.InstanceNetworkInterfaceSetRequestType;
import com.eucalyptus.compute.common.PrivateIpAddressesSetItemRequestType;
import com.eucalyptus.compute.common.PrivateIpAddressesSetRequestType;
import com.eucalyptus.compute.common.RunInstancesResponseType;
import com.eucalyptus.compute.common.RunInstancesType;
import com.eucalyptus.compute.common.RunningInstancesItemType;
import com.eucalyptus.compute.common.SecurityGroupIdSetItemType;
import com.eucalyptus.compute.common.SecurityGroupIdSetType;
import com.eucalyptus.compute.common.SecurityGroupItemType;
import com.eucalyptus.compute.common.TerminateInstancesResponseType;
import com.eucalyptus.compute.common.TerminateInstancesType;
import com.eucalyptus.compute.common.Volume;
import com.eucalyptus.configurable.ConfigurableClass;
import com.eucalyptus.configurable.ConfigurableField;
import com.eucalyptus.util.async.AsyncRequests;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.netflix.glisten.WorkflowOperations;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import javax.annotation.Nullable;

/**
 * Created by ethomas on 2/3/14.
 */
@ConfigurableClass( root = "cloudformation", description = "Parameters controlling cloud formation")
public class AWSEC2InstanceResourceAction extends ResourceAction {

  @ConfigurableField(initial = "300", description = "The amount of time (in seconds) to wait for an instance to be running after creation)")
  public static volatile Integer INSTANCE_RUNNING_MAX_CREATE_RETRY_SECS = 300;

  @ConfigurableField(initial = "300", description = "The amount of time (in seconds) to wait for an instance to have volumes attached after creation)")
  public static volatile Integer INSTANCE_ATTACH_VOLUME_MAX_CREATE_RETRY_SECS = 300;

  @ConfigurableField(initial = "300", description = "The amount of time (in seconds) to wait for an instance to be terminated after deletion)")
  public static volatile Integer INSTANCE_TERMINATED_MAX_DELETE_RETRY_SECS = 300;


  private AWSEC2InstanceProperties properties = new AWSEC2InstanceProperties();
  private AWSEC2InstanceResourceInfo info = new AWSEC2InstanceResourceInfo();

  public AWSEC2InstanceResourceAction() {
    for (CreateSteps createStep : CreateSteps.values()) {
      createSteps.put(createStep.name(), createStep);
    }
    for (DeleteSteps deleteStep : DeleteSteps.values()) {
      deleteSteps.put(deleteStep.name(), deleteStep);
    }

  }

  private enum CreateSteps implements Step {
    RUN_INSTANCE {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        synchronized (AWSEC2InstanceResourceAction.class) { // seems to have some issues with multiple running events...
          AWSEC2InstanceResourceAction action = (AWSEC2InstanceResourceAction) resourceAction;
          ServiceConfiguration configuration = Topology.lookup(Compute.class);
          RunInstancesType runInstancesType = MessageHelper.createMessage(RunInstancesType.class, action.info.getEffectiveUserId());
          runInstancesType.setImageId(action.properties.getImageId());
          if (action.properties.getAvailabilityZone() != null && !action.properties.getAvailabilityZone().isEmpty()) {
            runInstancesType.setAvailabilityZone(action.properties.getAvailabilityZone());
          }
          if (action.properties.getBlockDeviceMappings() != null && !action.properties.getBlockDeviceMappings().isEmpty()) {
            runInstancesType.setBlockDeviceMapping(action.convertBlockDeviceMappings(action.properties.getBlockDeviceMappings()));
          }
          if (action.properties.getBlockDeviceMappings() != null) {
            runInstancesType.setDisableTerminate(action.properties.getDisableApiTermination());
          }
          if (action.properties.getEbsOptimized() != null) {
            runInstancesType.setEbsOptimized(action.properties.getEbsOptimized());
          }
          if (action.properties.getIamInstanceProfile() != null && !action.properties.getIamInstanceProfile().isEmpty()) {
            runInstancesType.setIamInstanceProfileName(action.properties.getIamInstanceProfile()); // TODO: DOCS claim this is for profile name, is ARN supported?
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
          // Skipping mapping resourceaction.properties.getSourceDestCheck() for now
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
            describeVolumesType.setVolumeSet(volumeIds);
            DescribeVolumesResponseType describeVolumesResponseType = AsyncRequests.<DescribeVolumesType, DescribeVolumesResponseType>sendSync(configuration, describeVolumesType);
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
          return action;
        }
      }
    },
    WAIT_UNTIL_RUNNING {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        synchronized (AWSEC2InstanceResourceAction.class) { // seems to have some issues with multiple running events...
          AWSEC2InstanceResourceAction action = (AWSEC2InstanceResourceAction) resourceAction;
          ServiceConfiguration configuration = Topology.lookup(Compute.class);
          DescribeInstancesType describeInstancesType = MessageHelper.createMessage(DescribeInstancesType.class, action.info.getEffectiveUserId());
          describeInstancesType.setInstancesSet(Lists.newArrayList(action.info.getPhysicalResourceId()));
          DescribeInstancesResponseType describeInstancesResponseType = AsyncRequests.<DescribeInstancesType, DescribeInstancesResponseType>sendSync(configuration, describeInstancesType);
          if (describeInstancesResponseType.getReservationSet().size() == 0) {
            throw new ValidationFailedException("Instance " + action.info.getAccountId() + " does not yet exist");
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
          throw new ValidationFailedException(("Instance " + action.info.getPhysicalResourceId() + " is not yet running, currently " + runningInstancesItemType.getStateName()));
        }
      }

      @Override
      public Integer getTimeout() {
        return INSTANCE_RUNNING_MAX_CREATE_RETRY_SECS;
      }
    },
    CREATE_TAGS {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        synchronized (AWSEC2InstanceResourceAction.class) { // seems to have some issues with multiple running events...
          AWSEC2InstanceResourceAction action = (AWSEC2InstanceResourceAction) resourceAction;
          ServiceConfiguration configuration = Topology.lookup(Compute.class);
          // Create 'system' tags as admin user
          String effectiveAdminUserId = Accounts.lookupPrincipalByAccountNumber( Accounts.lookupPrincipalByUserId(action.info.getEffectiveUserId()).getAccountNumber( ) ).getUserId();
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
      }
    },
    ATTACH_VOLUMES {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        synchronized (AWSEC2InstanceResourceAction.class) { // seems to have some issues with multiple running events...
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
      }
    },
    WAIT_UNTIL_VOLUMES_ATTACHED {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        synchronized (AWSEC2InstanceResourceAction.class) { // seems to have some issues with multiple running events...
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
            describeVolumesType.setVolumeSet(Lists.newArrayList(volumeIds));
            DescribeVolumesResponseType describeVolumesResponseType = AsyncRequests.<DescribeVolumesType, DescribeVolumesResponseType>sendSync(configuration, describeVolumesType);
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
                throw new ValidationFailedException("One or more volumes is not yet attached to the instance");
              }
            }
          }
          return action;
        }
      }

      @Override
      public Integer getTimeout() {
        return INSTANCE_ATTACH_VOLUME_MAX_CREATE_RETRY_SECS;
      }
    };

    @Nullable
    @Override
    public Integer getTimeout() {
      return null;
    }
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

  private enum DeleteSteps implements Step {
    TERMINATE_INSTANCE {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        synchronized (AWSEC2InstanceResourceAction.class) { // seems to have some issues with multiple running events...
          AWSEC2InstanceResourceAction action = (AWSEC2InstanceResourceAction) resourceAction;
          ServiceConfiguration configuration = Topology.lookup(Compute.class);
          // See if instance was ever populated
          if (action.info.getPhysicalResourceId() == null) return action;
          // First see if instance exists or has been terminated
          DescribeInstancesType describeInstancesType = MessageHelper.createMessage(DescribeInstancesType.class, action.info.getEffectiveUserId());
          describeInstancesType.setInstancesSet(Lists.newArrayList(action.info.getPhysicalResourceId()));
          DescribeInstancesResponseType describeInstancesResponseType = AsyncRequests.<DescribeInstancesType, DescribeInstancesResponseType>sendSync(configuration, describeInstancesType);
          if (describeInstancesResponseType.getReservationSet().size() == 0) return action; // already terminated
          if ("terminated".equals(
            describeInstancesResponseType.getReservationSet().get(0).getInstancesSet().get(0).getStateName()))
            return action;

          // Send terminate message (do not need to detatch volumes thankfully
          TerminateInstancesType terminateInstancesType = MessageHelper.createMessage(TerminateInstancesType.class, action.info.getEffectiveUserId());
          terminateInstancesType.setInstancesSet(Lists.newArrayList(action.info.getPhysicalResourceId()));
          AsyncRequests.<TerminateInstancesType, TerminateInstancesResponseType>sendSync(configuration, terminateInstancesType);
          return action;
        }
      }
    },
    VERIFY_TERMINATED {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        synchronized (AWSEC2InstanceResourceAction.class) { // seems to have some issues with multiple running events...
          AWSEC2InstanceResourceAction action = (AWSEC2InstanceResourceAction) resourceAction;
          ServiceConfiguration configuration = Topology.lookup(Compute.class);
          // See if instance was ever populated
          if (action.info.getPhysicalResourceId() == null) return action;
          DescribeInstancesType describeInstancesType = MessageHelper.createMessage(DescribeInstancesType.class, action.info.getEffectiveUserId());
          describeInstancesType.setInstancesSet(Lists.newArrayList(action.info.getPhysicalResourceId()));
          DescribeInstancesResponseType describeInstancesResponseType = AsyncRequests.<DescribeInstancesType, DescribeInstancesResponseType>sendSync(configuration, describeInstancesType);
          if (describeInstancesResponseType.getReservationSet().size() == 0) return action; // already terminated
          if ("terminated".equals(
            describeInstancesResponseType.getReservationSet().get(0).getInstancesSet().get(0).getStateName()))
            return action;
          throw new ValidationFailedException(("Instance " + action.info.getPhysicalResourceId() + " is not yet terminated, currently " + describeInstancesResponseType.getReservationSet().get(0).getInstancesSet().get(0).getStateName()));
        }
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

  private ArrayList<GroupItemType> convertSecurityGroups(List<String> securityGroups, ServiceConfiguration configuration, String effectiveUserId) throws Exception {
    if (securityGroups == null) return null;
    // Is there a better way?
    DescribeSecurityGroupsType describeSecurityGroupsType = MessageHelper.createMessage(DescribeSecurityGroupsType.class, effectiveUserId);
    DescribeSecurityGroupsResponseType describeSecurityGroupsResponseType =
      AsyncRequests.<DescribeSecurityGroupsType, DescribeSecurityGroupsResponseType>sendSync(configuration, describeSecurityGroupsType);
    ArrayList<SecurityGroupItemType> securityGroupItemTypeArrayList = describeSecurityGroupsResponseType.getSecurityGroupInfo();
    Map<String, String> nameToIdMap = Maps.newHashMap();
    if (securityGroupItemTypeArrayList != null) {
      for (SecurityGroupItemType securityGroupItemType : securityGroupItemTypeArrayList) {
        nameToIdMap.put(securityGroupItemType.getGroupName(), securityGroupItemType.getGroupId());
      }
    }
    ArrayList<GroupItemType> groupItemTypes = Lists.newArrayList();
    for (String securityGroup : securityGroups) {
      GroupItemType groupItemType = new GroupItemType();
      groupItemType.setGroupName(securityGroup);
      String groupId = nameToIdMap.get(securityGroup);
      if (groupId == null) throw new NoSuchElementException("No such security group with name " + securityGroup);
      groupItemType.setGroupId(groupId);
      groupItemTypes.add(groupItemType);
    }
    return groupItemTypes;
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

  @Override
  public void refreshAttributes() throws Exception {
    // This assumes everything is set, propertywise and attributewise
    ServiceConfiguration configuration = Topology.lookup(Compute.class);
    DescribeInstancesType describeInstancesType = MessageHelper.createMessage(DescribeInstancesType.class, info.getEffectiveUserId());
    describeInstancesType.setInstancesSet(Lists.newArrayList(info.getPhysicalResourceId()));
    DescribeInstancesResponseType describeInstancesResponseType = AsyncRequests.<DescribeInstancesType, DescribeInstancesResponseType>sendSync(configuration, describeInstancesType);
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
}




