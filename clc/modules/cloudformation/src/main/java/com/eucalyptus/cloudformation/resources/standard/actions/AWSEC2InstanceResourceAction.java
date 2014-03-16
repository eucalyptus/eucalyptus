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


import com.eucalyptus.bootstrap.BootstrapArgs;
import com.eucalyptus.cloudformation.resources.ResourceAction;
import com.eucalyptus.cloudformation.resources.ResourceInfo;
import com.eucalyptus.cloudformation.resources.ResourceProperties;
import com.eucalyptus.cloudformation.resources.standard.info.AWSEC2InstanceResourceInfo;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.AWSEC2InstanceProperties;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.EC2BlockDeviceMapping;
import com.eucalyptus.cloudformation.template.JsonHelper;
import com.eucalyptus.component.ComponentId;
import com.eucalyptus.component.ComponentIds;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.ServiceConfigurations;
import com.eucalyptus.component.Topology;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.util.async.AsyncRequests;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import edu.ucsb.eucalyptus.msgs.BlockDeviceMappingItemType;
import edu.ucsb.eucalyptus.msgs.DescribeInstancesResponseType;
import edu.ucsb.eucalyptus.msgs.DescribeInstancesType;
import edu.ucsb.eucalyptus.msgs.DescribeSecurityGroupsResponseType;
import edu.ucsb.eucalyptus.msgs.DescribeSecurityGroupsType;
import edu.ucsb.eucalyptus.msgs.EbsDeviceMapping;
import edu.ucsb.eucalyptus.msgs.GroupItemType;
import edu.ucsb.eucalyptus.msgs.RunInstancesResponseType;
import edu.ucsb.eucalyptus.msgs.RunInstancesType;
import edu.ucsb.eucalyptus.msgs.RunningInstancesItemType;
import edu.ucsb.eucalyptus.msgs.SecurityGroupItemType;
import edu.ucsb.eucalyptus.msgs.TerminateInstancesResponseType;
import edu.ucsb.eucalyptus.msgs.TerminateInstancesType;

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
      itemType.setNoDevice(ec2BlockDeviceMapping.getNoDevice() != null);
      itemType.setVirtualName(ec2BlockDeviceMapping.getVirtualName());
      if (ec2BlockDeviceMapping.getEbs() != null) {
        EbsDeviceMapping ebs = new EbsDeviceMapping();
        ebs.setDeleteOnTermination(ec2BlockDeviceMapping.getEbs().getDeleteOnTermination());
        ebs.setIops(ec2BlockDeviceMapping.getEbs().getIops());
        ebs.setSnapshotId(ec2BlockDeviceMapping.getEbs().getSnapshotId());
        ebs.setVolumeSize(Integer.valueOf(ec2BlockDeviceMapping.getEbs().getVolumeSize()));
        ebs.setVolumeType(ec2BlockDeviceMapping.getEbs().getVolumeType());
        itemType.setEbs(ebs);
      }
      blockDeviceMappingItemTypes.add(itemType);
    }
    return blockDeviceMappingItemTypes;
  }

  @Override
  public void create() throws Exception {
    ServiceConfiguration configuration = Topology.lookup(Eucalyptus.class);
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
    // Skipping mapping resourceProperties.getVolumes() for now

    runInstancesType.setMinCount(1);
    runInstancesType.setMaxCount(1);
    runInstancesType.setEffectiveUserId(info.getEffectiveUserId());
    RunInstancesResponseType runInstancesResponseType = AsyncRequests.<RunInstancesType,RunInstancesResponseType> sendSync(configuration, runInstancesType);
    info.setPhysicalResourceId(runInstancesResponseType.getRsvInfo().getInstancesSet().get(0).getInstanceId());
    for (int i=0;i<24;i++) { // sleeping for 5 seconds 24 times... (2 minutes)
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
        return;
      }
    }
    throw new Exception("Timeout");
  }


  @Override
  public void delete() throws Exception {
    if (info.getPhysicalResourceId() == null) return;
    ServiceConfiguration configuration = Topology.lookup(Eucalyptus.class);
    // First see if instance exists or has been terminated
    DescribeInstancesType describeInstancesType = new DescribeInstancesType();
    describeInstancesType.setInstancesSet(Lists.newArrayList(info.getPhysicalResourceId()));
    describeInstancesType.setEffectiveUserId(info.getEffectiveUserId());
    DescribeInstancesResponseType describeInstancesResponseType = AsyncRequests.<DescribeInstancesType,DescribeInstancesResponseType> sendSync(configuration, describeInstancesType);
    if (describeInstancesResponseType.getReservationSet().size() == 0) return; // already terminated
    if ("terminated".equals(
      describeInstancesResponseType.getReservationSet().get(0).getInstancesSet().get(0).getStateName())) return;

    TerminateInstancesType terminateInstancesType = new TerminateInstancesType();
    terminateInstancesType.setInstancesSet(Lists.newArrayList(info.getPhysicalResourceId()));
    terminateInstancesType.setEffectiveUserId(info.getEffectiveUserId());
    AsyncRequests.<TerminateInstancesType,TerminateInstancesResponseType> sendSync(configuration, terminateInstancesType);
    boolean terminated = false;
    for (int i=0;i<24;i++) { // sleeping for 5 seconds 24 times... (2 minutes)
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
  public void rollback() throws Exception {
    delete();
  }

}



