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
package com.eucalyptus.cloudformation.resources.impl;

import com.eucalyptus.bootstrap.BootstrapArgs;
import com.eucalyptus.cloudformation.CloudFormationException;
import com.eucalyptus.cloudformation.resources.Resource;
import com.eucalyptus.cloudformation.resources.ResourcePropertyResolver;
import com.eucalyptus.cloudformation.resources.propertytypes.*;
import com.eucalyptus.component.ComponentId;
import com.eucalyptus.component.ComponentIds;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.ServiceConfigurations;
import com.eucalyptus.component.Topology;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.util.async.AsyncRequests;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.collect.Lists;
import edu.ucsb.eucalyptus.msgs.*;
import com.fasterxml.jackson.databind.JsonNode;
import edu.ucsb.eucalyptus.msgs.RunInstancesType;
import groovy.transform.ToString;

import java.io.File;
import java.util.ArrayList;
import java.util.List;


/**
 * Created by ethomas on 2/3/14.
 */
public class AWSEC2Instance extends Resource {

  private AWSEC2InstanceProperties resourceProperties = new AWSEC2InstanceProperties();
  private AWSEC2InstanceAttributes resourceAttributes = new AWSEC2InstanceAttributes();


  public AWSEC2Instance() {
    setType("AWS::EC2::Instance");
  }

  @Override
  public ResourceProperties getResourceProperties() {
    return resourceProperties;
  }

  @Override
  public ResourceAttributes getResourceAttributes() {
    return resourceAttributes;
  }

  @Override
  public void populateResourceProperties(JsonNode jsonNode) throws CloudFormationException {
    ResourcePropertyResolver.populateResourceProperties(resourceProperties, jsonNode);
  }

  @Override
  public void create() throws Exception {
    RunInstancesType runInstancesType = new RunInstancesType();
    runInstancesType.setImageId(resourceProperties.getImageId());
    if (resourceProperties.getAvailabilityZone() != null && !resourceProperties.getAvailabilityZone().isEmpty()) {
      runInstancesType.setAvailabilityZone(resourceProperties.getAvailabilityZone());
    }
    if (resourceProperties.getBlockDeviceMappings() != null && !resourceProperties.getBlockDeviceMappings().isEmpty()) {
      runInstancesType.setBlockDeviceMapping(convertBlockDeviceMappings(resourceProperties.getBlockDeviceMappings()));
    }
    if (resourceProperties.getBlockDeviceMappings() != null) {
      runInstancesType.setDisableTerminate(resourceProperties.getDisableApiTermination());
    }
    if (resourceProperties.getEbsOptimized() != null) {
      runInstancesType.setEbsOptimized(resourceProperties.getEbsOptimized());
    }
    if (resourceProperties.getIamInstanceProfile() != null && !resourceProperties.getIamInstanceProfile().isEmpty()) {
      runInstancesType.setIamInstanceProfileName(resourceProperties.getIamInstanceProfile()); // TODO: DOCS claim this is for profile name, is ARN supported?
    }
    if (resourceProperties.getInstanceType() != null && !resourceProperties.getInstanceType().isEmpty()) {
      runInstancesType.setInstanceType(resourceProperties.getInstanceType());
    }
    if (resourceProperties.getKernelId() != null && !resourceProperties.getKernelId().isEmpty()) {
      runInstancesType.setKernelId(resourceProperties.getKernelId());
    }
    if (resourceProperties.getKeyName() != null && !resourceProperties.getKeyName().isEmpty()) {
      runInstancesType.setKeyName(resourceProperties.getKeyName());
    }
    if (resourceProperties.getMonitoring() != null) {
      runInstancesType.setMonitoring(resourceProperties.getMonitoring());
    }
    // Skipping mapping resourceProperties.getNetworkInterfaces() for now
    if (resourceProperties.getPlacementGroupName() != null && !resourceProperties.getPlacementGroupName().isEmpty()) {
      runInstancesType.setPlacementGroup(resourceProperties.getPlacementGroupName());
    }
    if (resourceProperties.getPrivateIpAddress() != null && !resourceProperties.getPrivateIpAddress().isEmpty()) {
      runInstancesType.setPrivateIpAddress(resourceProperties.getPrivateIpAddress());
    }
    if (resourceProperties.getRamdiskId() != null && !resourceProperties.getRamdiskId().isEmpty()) {
      runInstancesType.setRamdiskId(resourceProperties.getRamdiskId());
    }
    // Skipping mapping resourceProperties.getSecurityGroupIds() for now
    if (resourceProperties.getSecurityGroups() != null && !resourceProperties.getSecurityGroups().isEmpty()) {
      runInstancesType.setSecurityGroups(convertSecurityGroups(resourceProperties.getSecurityGroups()));
    }
    // Skipping mapping resourceProperties.getSourceDestCheck() for now
    if (resourceProperties.getSubnetId() != null && !resourceProperties.getSubnetId().isEmpty()) {
      runInstancesType.setSubnetId(resourceProperties.getSubnetId());
    }
    // Skipping mapping resourceProperties.getTags() for now
    // Skipping mapping resourceProperties.getTenancy() for now
    if (resourceProperties.getUserData() != null && !resourceProperties.getUserData().isEmpty()) {
      runInstancesType.setUserData(resourceProperties.getUserData());
    }
    // Skipping mapping resourceProperties.getVolumes() for now

    runInstancesType.setMinCount(1);
    runInstancesType.setMaxCount(1);
    final ComponentId componentId = ComponentIds.lookup(Eucalyptus.class);
    ServiceConfiguration configuration;
    if ( componentId.isAlwaysLocal() ||
      ( BootstrapArgs.isCloudController() && componentId.isCloudLocal() && !componentId.isRegisterable() ) ) {
      configuration = ServiceConfigurations.createEphemeral(componentId);
    } else {
      configuration = Topology.lookup(Eucalyptus.class);
    }
    runInstancesType.setEffectiveUserId(getEffectiveUserId());
    RunInstancesResponseType runInstancesResponseType = AsyncRequests.<RunInstancesType,RunInstancesResponseType> sendSync(configuration, runInstancesType);
    setPhysicalResourceId(runInstancesResponseType.getRsvInfo().getInstancesSet().get(0).getInstanceId());
    for (int i=0;i<24;i++) { // sleeping for 5 seconds 24 times... (2 minutes)
      Thread.sleep(5000L);
      DescribeInstancesType describeInstancesType = new DescribeInstancesType();
      describeInstancesType.setInstancesSet(Lists.newArrayList(getPhysicalResourceId()));
      describeInstancesType.setEffectiveUserId(getEffectiveUserId());
      DescribeInstancesResponseType describeInstancesResponseType = AsyncRequests.<DescribeInstancesType,DescribeInstancesResponseType> sendSync(configuration, describeInstancesType);
      RunningInstancesItemType runningInstancesItemType = describeInstancesResponseType.getReservationSet().get(0).getInstancesSet().get(0);
      if ("running".equals(runningInstancesItemType.getStateName())) {
        resourceAttributes.setPrivateIp(new TextNode(runningInstancesItemType.getPrivateIpAddress()));
        resourceAttributes.setPublicIp(new TextNode(runningInstancesItemType.getIpAddress()));
        resourceAttributes.setAvailabilityZone(new TextNode(runningInstancesItemType.getPlacement()));
        resourceAttributes.setPrivateDnsName(new TextNode(runningInstancesItemType.getPrivateDnsName()));
        resourceAttributes.setPublicDnsName(new TextNode(runningInstancesItemType.getDnsName()));
        return;
      }
    }
    throw new Exception("Timeout");
  }

  private ArrayList<GroupItemType> convertSecurityGroups(List<String> securityGroups) {
    if (securityGroups == null) return null;
    ArrayList<GroupItemType> groupItemTypes = Lists.newArrayList();
    for (String securityGroup: securityGroups) {
      GroupItemType groupItemType = new GroupItemType();
      groupItemType.setGroupName(securityGroup);
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
  public void delete() throws Exception {
    if (getPhysicalResourceId() == null) return;
    TerminateInstancesType terminateInstancesType = new TerminateInstancesType();
    terminateInstancesType.setInstancesSet(Lists.newArrayList(getPhysicalResourceId()));
    final ComponentId componentId = ComponentIds.lookup(Eucalyptus.class);
    ServiceConfiguration configuration;
    if ( componentId.isAlwaysLocal() ||
      ( BootstrapArgs.isCloudController() && componentId.isCloudLocal() && !componentId.isRegisterable() ) ) {
      configuration = ServiceConfigurations.createEphemeral(componentId);
    } else {
      configuration = Topology.lookup(Eucalyptus.class);
    }
    terminateInstancesType.setEffectiveUserId(getEffectiveUserId());
    AsyncRequests.<TerminateInstancesType,TerminateInstancesResponseType> sendSync(configuration, terminateInstancesType);
    boolean terminated = false;
    for (int i=0;i<24;i++) { // sleeping for 5 seconds 24 times... (2 minutes)
      Thread.sleep(5000L);
      DescribeInstancesType describeInstancesType = new DescribeInstancesType();
      describeInstancesType.setInstancesSet(Lists.newArrayList(getPhysicalResourceId()));
      describeInstancesType.setEffectiveUserId(getEffectiveUserId());
      DescribeInstancesResponseType describeInstancesResponseType = AsyncRequests.<DescribeInstancesType,DescribeInstancesResponseType> sendSync(configuration, describeInstancesType);
      if ("terminated".equals(describeInstancesResponseType.getReservationSet().get(0).getInstancesSet().get(0).getStateName())) {
        terminated = true;
        break;
      }
    }
    if (!terminated) throw new Exception("Timeout");
    // terminate one more time, just to make sure it is gone
    terminateInstancesType = new TerminateInstancesType();
    terminateInstancesType.setInstancesSet(Lists.newArrayList(getPhysicalResourceId()));
    terminateInstancesType.setEffectiveUserId(getEffectiveUserId());
    AsyncRequests.<TerminateInstancesType,TerminateInstancesResponseType> sendSync(configuration, terminateInstancesType);
    terminated = false;
    for (int i=0;i<24;i++) { // sleeping for 5 seconds 24 times... (2 minutes)
      Thread.sleep(5000L);
      DescribeInstancesType describeInstancesType = new DescribeInstancesType();
      describeInstancesType.setInstancesSet(Lists.newArrayList(getPhysicalResourceId()));
      describeInstancesType.setEffectiveUserId(getEffectiveUserId());
      DescribeInstancesResponseType describeInstancesResponseType = AsyncRequests.<DescribeInstancesType,DescribeInstancesResponseType> sendSync(configuration, describeInstancesType);
      if (describeInstancesResponseType.getReservationSet().size() == 0) {
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

  @Override
  public JsonNode referenceValue() {
    return new TextNode(getPhysicalResourceId());
  }

}
