/*************************************************************************
 * Copyright 2009-2013 Ent. Services Development Corporation LP
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


import com.eucalyptus.autoscaling.common.AutoScaling;
import com.eucalyptus.autoscaling.common.msgs.BlockDeviceMappingType;
import com.eucalyptus.autoscaling.common.msgs.BlockDeviceMappings;
import com.eucalyptus.autoscaling.common.msgs.CreateLaunchConfigurationResponseType;
import com.eucalyptus.autoscaling.common.msgs.CreateLaunchConfigurationType;
import com.eucalyptus.autoscaling.common.msgs.DeleteLaunchConfigurationResponseType;
import com.eucalyptus.autoscaling.common.msgs.DeleteLaunchConfigurationType;
import com.eucalyptus.autoscaling.common.msgs.InstanceMonitoring;
import com.eucalyptus.autoscaling.common.msgs.SecurityGroups;
import com.eucalyptus.cloudformation.ValidationErrorException;
import com.eucalyptus.cloudformation.resources.ResourceAction;
import com.eucalyptus.cloudformation.resources.ResourceInfo;
import com.eucalyptus.cloudformation.resources.ResourceProperties;
import com.eucalyptus.cloudformation.resources.standard.info.AWSAutoScalingLaunchConfigurationResourceInfo;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.AWSAutoScalingLaunchConfigurationProperties;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.AutoScalingBlockDeviceMapping;
import com.eucalyptus.cloudformation.template.JsonHelper;
import com.eucalyptus.cloudformation.util.MessageHelper;
import com.eucalyptus.cloudformation.workflow.steps.Step;
import com.eucalyptus.cloudformation.workflow.steps.StepBasedResourceAction;
import com.eucalyptus.cloudformation.workflow.updateinfo.UpdateType;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.Topology;
import com.eucalyptus.util.async.AsyncRequests;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.collect.Lists;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Created by ethomas on 2/3/14.
 */
public class AWSAutoScalingLaunchConfigurationResourceAction extends StepBasedResourceAction {

  private AWSAutoScalingLaunchConfigurationProperties properties = new AWSAutoScalingLaunchConfigurationProperties();
  private AWSAutoScalingLaunchConfigurationResourceInfo info = new AWSAutoScalingLaunchConfigurationResourceInfo();

  public AWSAutoScalingLaunchConfigurationResourceAction() {
    // all updates are of type replacement
    super(fromEnum(CreateSteps.class), fromEnum(DeleteSteps.class), null, null);
  }

  @Override
  public UpdateType getUpdateType(ResourceAction resourceAction, boolean stackTagsChanged) {
    UpdateType updateType = info.supportsTags() && stackTagsChanged ? UpdateType.NO_INTERRUPTION : UpdateType.NONE;
    AWSAutoScalingLaunchConfigurationResourceAction otherAction = (AWSAutoScalingLaunchConfigurationResourceAction) resourceAction;
    if (!Objects.equals(properties.getAssociatePublicIpAddress(), otherAction.properties.getAssociatePublicIpAddress())) {
      updateType = UpdateType.max(updateType, UpdateType.NEEDS_REPLACEMENT);
    }
    if (!Objects.equals(properties.getBlockDeviceMappings(), otherAction.properties.getBlockDeviceMappings())) {
      updateType = UpdateType.max(updateType, UpdateType.NEEDS_REPLACEMENT);
    }
    if (!Objects.equals(properties.getEbsOptimized(), otherAction.properties.getEbsOptimized())) {
      updateType = UpdateType.max(updateType, UpdateType.NEEDS_REPLACEMENT);
    }
    if (!Objects.equals(properties.getIamInstanceProfile(), otherAction.properties.getIamInstanceProfile())) {
      updateType = UpdateType.max(updateType, UpdateType.NEEDS_REPLACEMENT);
    }
    if (!Objects.equals(properties.getImageId(), otherAction.properties.getImageId())) {
      updateType = UpdateType.max(updateType, UpdateType.NEEDS_REPLACEMENT);
    }
    if (!Objects.equals(properties.getInstanceId(), otherAction.properties.getInstanceId())) {
      updateType = UpdateType.max(updateType, UpdateType.NEEDS_REPLACEMENT);
    }
    if (!Objects.equals(properties.getInstanceMonitoring(), otherAction.properties.getInstanceMonitoring())) {
      updateType = UpdateType.max(updateType, UpdateType.NEEDS_REPLACEMENT);
    }
    if (!Objects.equals(properties.getInstanceType(), otherAction.properties.getInstanceType())) {
      updateType = UpdateType.max(updateType, UpdateType.NEEDS_REPLACEMENT);
    }
    if (!Objects.equals(properties.getKernelId(), otherAction.properties.getKernelId())) {
      updateType = UpdateType.max(updateType, UpdateType.NEEDS_REPLACEMENT);
    }
    if (!Objects.equals(properties.getKeyName(), otherAction.properties.getKeyName())) {
      updateType = UpdateType.max(updateType, UpdateType.NEEDS_REPLACEMENT);
    }
    if (!Objects.equals(properties.getRamDiskId(), otherAction.properties.getRamDiskId())) {
      updateType = UpdateType.max(updateType, UpdateType.NEEDS_REPLACEMENT);
    }
    if (!Objects.equals(properties.getSecurityGroups(), otherAction.properties.getSecurityGroups())) {
      updateType = UpdateType.max(updateType, UpdateType.NEEDS_REPLACEMENT);
    }
    if (!Objects.equals(properties.getSpotPrice(), otherAction.properties.getSpotPrice())) {
      updateType = UpdateType.max(updateType, UpdateType.NEEDS_REPLACEMENT);
    }
    if (!Objects.equals(properties.getUserData(), otherAction.properties.getUserData())) {
      updateType = UpdateType.max(updateType, UpdateType.NEEDS_REPLACEMENT);
    }
    return updateType;
  }

  private enum CreateSteps implements Step {
    CREATE_LAUNCH_CONFIG {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        AWSAutoScalingLaunchConfigurationResourceAction action = (AWSAutoScalingLaunchConfigurationResourceAction) resourceAction;
        ServiceConfiguration configuration = Topology.lookup(AutoScaling.class);
        CreateLaunchConfigurationType createLaunchConfigurationType = MessageHelper.createMessage(CreateLaunchConfigurationType.class, action.info.getEffectiveUserId());
        if (action.properties.getInstanceId() != null) {
          throw new ValidationErrorException("InstanceId not supported");
        }
        if (action.properties.getBlockDeviceMappings() != null) {
          createLaunchConfigurationType.setBlockDeviceMappings(action.convertBlockDeviceMappings(action.properties.getBlockDeviceMappings()));
        }
        if (action.properties.getAssociatePublicIpAddress() != null) {
          createLaunchConfigurationType.setAssociatePublicIpAddress(action.properties.getAssociatePublicIpAddress());
        }
        createLaunchConfigurationType.setEbsOptimized(action.properties.getEbsOptimized() != null ? action.properties.getEbsOptimized() : Boolean.FALSE);
        createLaunchConfigurationType.setIamInstanceProfile(action.properties.getIamInstanceProfile());
        createLaunchConfigurationType.setImageId(action.properties.getImageId());
        InstanceMonitoring instanceMonitoring = new InstanceMonitoring();
        instanceMonitoring.setEnabled(action.properties.getInstanceMonitoring() != null ? action.properties.getInstanceMonitoring() : Boolean.TRUE);
        createLaunchConfigurationType.setInstanceMonitoring(instanceMonitoring);
        createLaunchConfigurationType.setInstanceType(action.properties.getInstanceType());
        createLaunchConfigurationType.setKernelId(action.properties.getKernelId());
        createLaunchConfigurationType.setKeyName(action.properties.getKeyName());
        createLaunchConfigurationType.setRamdiskId(action.properties.getRamDiskId());
        if (action.properties.getSecurityGroups() != null) {
          createLaunchConfigurationType.setSecurityGroups(new SecurityGroups(action.properties.getSecurityGroups()));
        }
        createLaunchConfigurationType.setSpotPrice(action.properties.getSpotPrice());
        createLaunchConfigurationType.setUserData(action.properties.getUserData());
        String launchConfigurationName = action.getDefaultPhysicalResourceId();
        createLaunchConfigurationType.setLaunchConfigurationName(launchConfigurationName);
        AsyncRequests.<CreateLaunchConfigurationType,CreateLaunchConfigurationResponseType> sendSync(configuration, createLaunchConfigurationType);
        action.info.setPhysicalResourceId(launchConfigurationName);
        action.info.setCreatedEnoughToDelete(true);
        action.info.setReferenceValueJson(JsonHelper.getStringFromJsonNode(new TextNode(action.info.getPhysicalResourceId())));
        return action;
      }
    }
  }

  private enum DeleteSteps implements Step {
    DELETE_LAUNCH_CONFIG {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        AWSAutoScalingLaunchConfigurationResourceAction action = (AWSAutoScalingLaunchConfigurationResourceAction) resourceAction;
        ServiceConfiguration configuration = Topology.lookup(AutoScaling.class);
        if (!Boolean.TRUE.equals(action.info.getCreatedEnoughToDelete())) return action;
        DeleteLaunchConfigurationType deleteLaunchConfigurationType = MessageHelper.createMessage(DeleteLaunchConfigurationType.class, action.info.getEffectiveUserId());
        deleteLaunchConfigurationType.setLaunchConfigurationName(action.info.getPhysicalResourceId());
        AsyncRequests.<DeleteLaunchConfigurationType,DeleteLaunchConfigurationResponseType> sendSync(configuration, deleteLaunchConfigurationType);
        return action;
      }
    };
  }


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



}


