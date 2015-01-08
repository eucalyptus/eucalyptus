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


import com.eucalyptus.cloudformation.ValidationErrorException;
import com.eucalyptus.cloudformation.resources.ResourceAction;
import com.eucalyptus.cloudformation.resources.ResourceInfo;
import com.eucalyptus.cloudformation.resources.ResourceProperties;
import com.eucalyptus.cloudformation.resources.standard.info.AWSEC2InstanceResourceInfo;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.AWSEC2InstanceProperties;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.EC2BlockDeviceMapping;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.EC2MountPoint;
import com.eucalyptus.cloudformation.template.JsonHelper;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.Topology;
import com.eucalyptus.compute.common.AttachVolumeResponseType;
import com.eucalyptus.compute.common.AttachVolumeType;
import com.eucalyptus.compute.common.AttachedVolume;
import com.eucalyptus.compute.common.BlockDeviceMappingItemType;
import com.eucalyptus.compute.common.Compute;
import com.eucalyptus.compute.common.DescribeInstancesResponseType;
import com.eucalyptus.compute.common.DescribeInstancesType;
import com.eucalyptus.compute.common.DescribeSecurityGroupsResponseType;
import com.eucalyptus.compute.common.DescribeSecurityGroupsType;
import com.eucalyptus.compute.common.DescribeVolumesResponseType;
import com.eucalyptus.compute.common.DescribeVolumesType;
import com.eucalyptus.compute.common.EbsDeviceMapping;
import com.eucalyptus.compute.common.GroupItemType;
import com.eucalyptus.compute.common.RunInstancesResponseType;
import com.eucalyptus.compute.common.RunInstancesType;
import com.eucalyptus.compute.common.RunningInstancesItemType;
import com.eucalyptus.compute.common.SecurityGroupItemType;
import com.eucalyptus.compute.common.TerminateInstancesType;
import com.eucalyptus.compute.common.Volume;
import com.eucalyptus.util.async.AsyncRequests;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import edu.ucsb.eucalyptus.msgs.TerminateInstancesResponseType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * Created by ethomas on 2/3/14.
 */
public class AWSEC2InstanceResourceAction extends ResourceAction {

  private AWSEC2InstanceProperties properties = new AWSEC2InstanceProperties();
  private AWSEC2InstanceResourceInfo info = new AWSEC2InstanceResourceInfo();
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

  private ArrayList<GroupItemType> convertSecurityGroups(List<String> securityGroups, ServiceConfiguration configuration) throws Exception {
    if (securityGroups == null) return null;
    // Is there a better way?
    DescribeSecurityGroupsType describeSecurityGroupsType = new DescribeSecurityGroupsType();
    describeSecurityGroupsType.setEffectiveUserId(info.getEffectiveUserId());
    DescribeSecurityGroupsResponseType describeSecurityGroupsResponseType =
      AsyncRequests.<DescribeSecurityGroupsType,DescribeSecurityGroupsResponseType> sendSync(configuration, describeSecurityGroupsType);
    ArrayList<SecurityGroupItemType> securityGroupItemTypeArrayList = describeSecurityGroupsResponseType.getSecurityGroupInfo();
    Map<String, String> nameToIdMap = Maps.newHashMap();
    if (securityGroupItemTypeArrayList != null) {
      for (SecurityGroupItemType securityGroupItemType: securityGroupItemTypeArrayList) {
        nameToIdMap.put(securityGroupItemType.getGroupName(), securityGroupItemType.getGroupId());
      }
    }
    ArrayList<GroupItemType> groupItemTypes = Lists.newArrayList();
    for (String securityGroup: securityGroups) {
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
    for (EC2BlockDeviceMapping ec2BlockDeviceMapping: blockDeviceMappings) {
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
  public int getNumCreateSteps() {
    return 3;
  }

  @Override
  public void create(int stepNum) throws Exception {
    ServiceConfiguration configuration = Topology.lookup(Compute.class);
    switch (stepNum) {
      case 0: // run instance
        RunInstancesType runInstancesType = new RunInstancesType();
        runInstancesType.setImageId(properties.getImageId());
        if (properties.getAvailabilityZone() != null && !properties.getAvailabilityZone().isEmpty()) {
          runInstancesType.setAvailabilityZone(properties.getAvailabilityZone());
        }
        if (properties.getBlockDeviceMappings() != null && !properties.getBlockDeviceMappings().isEmpty()) {
          runInstancesType.setBlockDeviceMapping(convertBlockDeviceMappings(properties.getBlockDeviceMappings()));
        }
        if (properties.getBlockDeviceMappings() != null) {
          runInstancesType.setDisableTerminate(properties.getDisableApiTermination());
        }
        if (properties.getEbsOptimized() != null) {
          runInstancesType.setEbsOptimized(properties.getEbsOptimized());
        }
        if (properties.getIamInstanceProfile() != null && !properties.getIamInstanceProfile().isEmpty()) {
          runInstancesType.setIamInstanceProfileName(properties.getIamInstanceProfile()); // TODO: DOCS claim this is for profile name, is ARN supported?
        }
        if (properties.getInstanceType() != null && !properties.getInstanceType().isEmpty()) {
          runInstancesType.setInstanceType(properties.getInstanceType());
        }
        if (properties.getKernelId() != null && !properties.getKernelId().isEmpty()) {
          runInstancesType.setKernelId(properties.getKernelId());
        }
        if (properties.getKeyName() != null && !properties.getKeyName().isEmpty()) {
          runInstancesType.setKeyName(properties.getKeyName());
        }
        if (properties.getMonitoring() != null) {
          runInstancesType.setMonitoring(properties.getMonitoring());
        }
        // Skipping mapping resourceProperties.getNetworkInterfaces() for now
        if (properties.getPlacementGroupName() != null && !properties.getPlacementGroupName().isEmpty()) {
          runInstancesType.setPlacementGroup(properties.getPlacementGroupName());
        }
        if (properties.getPrivateIpAddress() != null && !properties.getPrivateIpAddress().isEmpty()) {
          runInstancesType.setPrivateIpAddress(properties.getPrivateIpAddress());
        }
        if (properties.getRamdiskId() != null && !properties.getRamdiskId().isEmpty()) {
          runInstancesType.setRamdiskId(properties.getRamdiskId());
        }
        // Skipping mapping resourceProperties.getSecurityGroupIds() for now
        if (properties.getSecurityGroups() != null && !properties.getSecurityGroups().isEmpty()) {
          runInstancesType.setSecurityGroups(convertSecurityGroups(properties.getSecurityGroups(), configuration));
        }
        // Skipping mapping resourceProperties.getSourceDestCheck() for now
        if (properties.getSubnetId() != null && !properties.getSubnetId().isEmpty()) {
          runInstancesType.setSubnetId(properties.getSubnetId());
        }
        // Skipping mapping resourceProperties.getTags() for now
        // Skipping mapping resourceProperties.getTenancy() for now
        if (properties.getUserData() != null && !properties.getUserData().isEmpty()) {
          runInstancesType.setUserData(properties.getUserData());
        }

        // make sure all volumes exist and are available
        if (properties.getVolumes() != null && !properties.getVolumes().isEmpty()) {
          DescribeVolumesType describeVolumesType = new DescribeVolumesType();
          ArrayList<String> volumeIds = Lists.newArrayList();
          for (EC2MountPoint ec2MountPoint: properties.getVolumes()) {
            volumeIds.add(ec2MountPoint.getVolumeId());
          }
          describeVolumesType.setVolumeSet(volumeIds);
          describeVolumesType.setEffectiveUserId(info.getEffectiveUserId());
          DescribeVolumesResponseType describeVolumesResponseType = AsyncRequests.<DescribeVolumesType,DescribeVolumesResponseType> sendSync(configuration, describeVolumesType);
          Map<String, String> volumeStatusMap = Maps.newHashMap();
          for (Volume volume: describeVolumesResponseType.getVolumeSet()) {
            volumeStatusMap.put(volume.getVolumeId(), volume.getStatus());
          }
          for (String volumeId: volumeIds) {
            if (!volumeStatusMap.containsKey(volumeId)) {
              throw new ValidationErrorException("No such volume " + volumeId);
            } else if (!"available".equals(volumeStatusMap.get(volumeId))) {
              throw new ValidationErrorException("Volume " + volumeId + " not available");
            }
          }
        }

        runInstancesType.setMinCount(1);
        runInstancesType.setMaxCount(1);
        runInstancesType.setEffectiveUserId(info.getEffectiveUserId());
        RunInstancesResponseType runInstancesResponseType = AsyncRequests.<RunInstancesType,RunInstancesResponseType> sendSync(configuration, runInstancesType);
        info.setPhysicalResourceId(runInstancesResponseType.getRsvInfo().getInstancesSet().get(0).getInstanceId());
        break;
      case 1: // wait until running
        boolean running = false;
        for (int i=0;i<60;i++) { // sleeping for 5 seconds 60 times... (5 minutes)
          Thread.sleep(5000L);
          DescribeInstancesType describeInstancesType = new DescribeInstancesType();
          describeInstancesType.setInstancesSet(Lists.newArrayList(info.getPhysicalResourceId()));
          describeInstancesType.setEffectiveUserId(info.getEffectiveUserId());
          DescribeInstancesResponseType describeInstancesResponseType = AsyncRequests.<DescribeInstancesType,DescribeInstancesResponseType> sendSync(configuration, describeInstancesType);
          if (describeInstancesResponseType.getReservationSet().size()==0) continue;
          RunningInstancesItemType runningInstancesItemType = describeInstancesResponseType.getReservationSet().get(0).getInstancesSet().get(0);
          if ("running".equals(runningInstancesItemType.getStateName())) {
            info.setPrivateIp(JsonHelper.getStringFromJsonNode(new TextNode(runningInstancesItemType.getPrivateIpAddress())));
            info.setPublicIp(JsonHelper.getStringFromJsonNode(new TextNode(runningInstancesItemType.getIpAddress())));
            info.setAvailabilityZone(JsonHelper.getStringFromJsonNode(new TextNode(runningInstancesItemType.getPlacement())));
            info.setPrivateDnsName(JsonHelper.getStringFromJsonNode(new TextNode(runningInstancesItemType.getPrivateDnsName())));
            info.setPublicDnsName(JsonHelper.getStringFromJsonNode(new TextNode(runningInstancesItemType.getDnsName())));
            info.setReferenceValueJson(JsonHelper.getStringFromJsonNode(new TextNode(info.getPhysicalResourceId())));
            running = true;
            break;
          }
        }
        if (!running) {
          throw new Exception("Timeout");
        }
        break;
      case 2: // attach volumes
        if (properties.getVolumes() != null && !properties.getVolumes().isEmpty()) {
          ArrayList<String> volumeIds = Lists.newArrayList();
          Map<String, String> deviceMap = Maps.newHashMap();
          for (EC2MountPoint ec2MountPoint: properties.getVolumes()) {
            volumeIds.add(ec2MountPoint.getVolumeId());
            deviceMap.put(ec2MountPoint.getVolumeId(), ec2MountPoint.getDevice());
            AttachVolumeType attachVolumeType = new AttachVolumeType();
            attachVolumeType.setEffectiveUserId(info.getEffectiveUserId());
            attachVolumeType.setInstanceId(info.getPhysicalResourceId());
            attachVolumeType.setVolumeId(ec2MountPoint.getVolumeId());
            attachVolumeType.setDevice(ec2MountPoint.getDevice());
            AsyncRequests.<AttachVolumeType, AttachVolumeResponseType> sendSync(configuration, attachVolumeType);
          }

          boolean allAttached = false;
          for (int i=0;i<60;i++) { // sleeping for 5 seconds 60 times... (5 minutes)
            Thread.sleep(5000L);
            DescribeVolumesType describeVolumesType2 = new DescribeVolumesType();
            describeVolumesType2.setVolumeSet(Lists.newArrayList(volumeIds));
            describeVolumesType2.setEffectiveUserId(info.getEffectiveUserId());
            DescribeVolumesResponseType describeVolumesResponseType2 = AsyncRequests.<DescribeVolumesType,DescribeVolumesResponseType> sendSync(configuration, describeVolumesType2);
            Map<String, String> volumeStatusMap = Maps.newHashMap();
            for (Volume volume: describeVolumesResponseType2.getVolumeSet()) {
              for (AttachedVolume attachedVolume: volume.getAttachmentSet()) {
                if (attachedVolume.getInstanceId().equals(info.getPhysicalResourceId()) && attachedVolume.getDevice().equals(deviceMap.get(volume.getVolumeId()))) {
                  volumeStatusMap.put(volume.getVolumeId(), attachedVolume.getStatus());
                }
              }
            }
            boolean anyNonAttached = false;
            for (String volumeId: volumeIds) {
              if (!"attached".equals(volumeStatusMap.get(volumeId))) {
                anyNonAttached = true;
                break;
              }
            }
            if (!anyNonAttached) {
              allAttached = true;
              break;
            }
          }
          if (!allAttached) throw new Exception("Timeout");
        }
        break;
      default:
        throw new IllegalStateException("Invalid step " + stepNum);
    }
  }

  @Override
  public void update(int stepNum) throws Exception {
    throw new UnsupportedOperationException();
  }

  public void rollbackUpdate() throws Exception {
    // can't update so rollbackUpdate should be a NOOP
  }

  @Override
  public void delete() throws Exception {
    if (info.getPhysicalResourceId() == null) return;
    ServiceConfiguration configuration = Topology.lookup(Compute.class);
    // First see if instance exists or has been terminated
    DescribeInstancesType describeInstancesType = new DescribeInstancesType();
    describeInstancesType.setInstancesSet(Lists.newArrayList(info.getPhysicalResourceId()));
    describeInstancesType.setEffectiveUserId(info.getEffectiveUserId());
    DescribeInstancesResponseType describeInstancesResponseType = AsyncRequests.<DescribeInstancesType,DescribeInstancesResponseType> sendSync(configuration, describeInstancesType);
    if (describeInstancesResponseType.getReservationSet().size() == 0) return; // already terminated
    if ("terminated".equals(
      describeInstancesResponseType.getReservationSet().get(0).getInstancesSet().get(0).getStateName())) return;

    // Volumes automatically detach
    TerminateInstancesType terminateInstancesType = new TerminateInstancesType();
    terminateInstancesType.setInstancesSet(Lists.newArrayList(info.getPhysicalResourceId()));
    terminateInstancesType.setEffectiveUserId(info.getEffectiveUserId());
    AsyncRequests.<TerminateInstancesType,TerminateInstancesResponseType> sendSync(configuration, terminateInstancesType);
    boolean terminated = false;
    for (int i=0;i<60;i++) { // sleeping for 5 seconds 60 times... (5 minutes)
      Thread.sleep(5000L);
      DescribeInstancesType describeInstancesType2 = new DescribeInstancesType();
      describeInstancesType2.setInstancesSet(Lists.newArrayList(info.getPhysicalResourceId()));
      describeInstancesType2.setEffectiveUserId(info.getEffectiveUserId());
      DescribeInstancesResponseType describeInstancesResponseType2 = AsyncRequests.<DescribeInstancesType,DescribeInstancesResponseType> sendSync(configuration, describeInstancesType2);
      if (describeInstancesResponseType2.getReservationSet().size() == 0) {
        terminated = true;
        break;
      }
      if ("terminated".equals(describeInstancesResponseType2.getReservationSet().get(0).getInstancesSet().get(0).getStateName())) {
        terminated = true;
        break;
      }
    }
    if (!terminated) throw new Exception("Timeout");
  }

  @Override
  public void rollbackCreate() throws Exception {
    delete();
  }

}



