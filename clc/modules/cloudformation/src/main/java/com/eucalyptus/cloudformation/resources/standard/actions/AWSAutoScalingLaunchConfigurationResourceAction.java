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


import com.eucalyptus.autoscaling.common.AutoScaling;
import com.eucalyptus.autoscaling.common.msgs.BlockDeviceMappingType;
import com.eucalyptus.autoscaling.common.msgs.BlockDeviceMappings;
import com.eucalyptus.autoscaling.common.msgs.DeleteLaunchConfigurationResponseType;
import com.eucalyptus.autoscaling.common.msgs.DeleteLaunchConfigurationType;
import com.eucalyptus.autoscaling.common.msgs.CreateLaunchConfigurationResponseType;
import com.eucalyptus.autoscaling.common.msgs.CreateLaunchConfigurationType;
import com.eucalyptus.autoscaling.common.msgs.Ebs;
import com.eucalyptus.autoscaling.common.msgs.InstanceMonitoring;
import com.eucalyptus.autoscaling.common.msgs.SecurityGroups;
import com.eucalyptus.cloudformation.ValidationErrorException;
import com.eucalyptus.cloudformation.resources.ResourceAction;
import com.eucalyptus.cloudformation.resources.ResourceInfo;
import com.eucalyptus.cloudformation.resources.ResourceProperties;
import com.eucalyptus.cloudformation.resources.standard.info.AWSAutoScalingLaunchConfigurationResourceInfo;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.AWSAutoScalingLaunchConfigurationProperties;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.AutoScalingBlockDeviceMapping;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.AutoScalingEBSBlockDevice;
import com.eucalyptus.cloudformation.template.JsonHelper;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.Topology;
import com.eucalyptus.util.async.AsyncRequests;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.collect.Lists;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ethomas on 2/3/14.
 */
public class AWSAutoScalingLaunchConfigurationResourceAction extends ResourceAction {

  private AWSAutoScalingLaunchConfigurationProperties properties = new AWSAutoScalingLaunchConfigurationProperties();
  private AWSAutoScalingLaunchConfigurationResourceInfo info = new AWSAutoScalingLaunchConfigurationResourceInfo();
  @Override
  public ResourceProperties getResourceProperties() {
    return properties;
  }

  @Override
  public void setResourceProperties(ResourceProperties resourceProperties) {
    properties = (AWSAutoScalingLaunchConfigurationProperties) resourceProperties;
  }

  @Override
  public ResourceInfo getResourceInfo() {
    return info;
  }

  @Override
  public void setResourceInfo(ResourceInfo resourceInfo) {
    info = (AWSAutoScalingLaunchConfigurationResourceInfo) resourceInfo;
  }

  @Override
  public void create(int stepNum) throws Exception {
    ServiceConfiguration configuration = Topology.lookup(AutoScaling.class);
    switch (stepNum) {
      case 0:
        CreateLaunchConfigurationType createLaunchConfigurationType = new CreateLaunchConfigurationType();
        if (properties.getInstanceId() != null) {
          throw new ValidationErrorException("InstanceId not supported");
        }
        if (properties.getBlockDeviceMappings() != null) {
          createLaunchConfigurationType.setBlockDeviceMappings(convertBlockDeviceMappings(properties.getBlockDeviceMappings()));
        }
        // Ignore AssociatePublicIpAddress for now (VPC)
        createLaunchConfigurationType.setEbsOptimized(properties.getEbsOptimized() != null ? properties.getEbsOptimized() : Boolean.FALSE);
        createLaunchConfigurationType.setIamInstanceProfile(properties.getIamInstanceProfile());
        createLaunchConfigurationType.setImageId(properties.getImageId());
        InstanceMonitoring instanceMonitoring = new InstanceMonitoring();
        instanceMonitoring.setEnabled(properties.getInstanceMonitoring() != null ? properties.getInstanceMonitoring() : Boolean.TRUE);
        createLaunchConfigurationType.setInstanceMonitoring(instanceMonitoring);
        createLaunchConfigurationType.setInstanceType(properties.getInstanceType());
        createLaunchConfigurationType.setKernelId(properties.getKernelId());
        createLaunchConfigurationType.setKeyName(properties.getKeyName());
        createLaunchConfigurationType.setRamdiskId(properties.getRamDiskId());
        if (properties.getSecurityGroups() != null) {
          createLaunchConfigurationType.setSecurityGroups(new SecurityGroups(properties.getSecurityGroups()));
        }
        createLaunchConfigurationType.setSpotPrice(properties.getSpotPrice());
        createLaunchConfigurationType.setUserData(properties.getUserData());
        String launchConfigurationName = getDefaultPhysicalResourceId();
        createLaunchConfigurationType.setLaunchConfigurationName(launchConfigurationName);
        createLaunchConfigurationType.setEffectiveUserId(info.getEffectiveUserId());
        AsyncRequests.<CreateLaunchConfigurationType,CreateLaunchConfigurationResponseType> sendSync(configuration, createLaunchConfigurationType);
        info.setPhysicalResourceId(launchConfigurationName);
        info.setReferenceValueJson(JsonHelper.getStringFromJsonNode(new TextNode(info.getPhysicalResourceId())));
        break;
      default:
        throw new IllegalStateException("Invalid step " + stepNum);
    }
  }

  private BlockDeviceMappings convertBlockDeviceMappings(List<AutoScalingBlockDeviceMapping> autoScalingBlockDeviceMappings) {
    ArrayList<BlockDeviceMappingType> blockDeviceMappingsList = Lists.newArrayList();
    for (AutoScalingBlockDeviceMapping autoScalingBlockDeviceMapping: autoScalingBlockDeviceMappings) {
      blockDeviceMappingsList.add(new BlockDeviceMappingType(autoScalingBlockDeviceMapping.getDeviceName(),
        autoScalingBlockDeviceMapping.getVirtualName(),
        autoScalingBlockDeviceMapping.getEbs() != null ? autoScalingBlockDeviceMapping.getEbs().getSnapshotId() : null,
        autoScalingBlockDeviceMapping.getEbs() != null ? autoScalingBlockDeviceMapping.getEbs().getVolumeSize() : null)
      );
    }
    return new BlockDeviceMappings(blockDeviceMappingsList);
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
    ServiceConfiguration configuration = Topology.lookup(AutoScaling.class);
    DeleteLaunchConfigurationType deleteLaunchConfigurationType = new DeleteLaunchConfigurationType();
    deleteLaunchConfigurationType.setEffectiveUserId(info.getEffectiveUserId());
    deleteLaunchConfigurationType.setLaunchConfigurationName(info.getPhysicalResourceId());
    AsyncRequests.<DeleteLaunchConfigurationType,DeleteLaunchConfigurationResponseType> sendSync(configuration, deleteLaunchConfigurationType);
  }

  @Override
  public void rollbackCreate() throws Exception {
    delete();
  }
}


