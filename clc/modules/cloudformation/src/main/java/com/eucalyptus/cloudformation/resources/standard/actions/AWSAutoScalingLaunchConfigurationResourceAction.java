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
import com.eucalyptus.cloudformation.workflow.StackActivityClient;
import com.eucalyptus.cloudformation.workflow.steps.CreateMultiStepPromise;
import com.eucalyptus.cloudformation.workflow.steps.DeleteMultiStepPromise;
import com.eucalyptus.cloudformation.workflow.steps.Step;
import com.eucalyptus.cloudformation.workflow.steps.StepTransform;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.Topology;
import com.eucalyptus.util.async.AsyncRequests;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.collect.Lists;
import com.netflix.glisten.WorkflowOperations;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;

/**
 * Created by ethomas on 2/3/14.
 */
public class AWSAutoScalingLaunchConfigurationResourceAction extends ResourceAction {

  private AWSAutoScalingLaunchConfigurationProperties properties = new AWSAutoScalingLaunchConfigurationProperties();
  private AWSAutoScalingLaunchConfigurationResourceInfo info = new AWSAutoScalingLaunchConfigurationResourceInfo();

  public AWSAutoScalingLaunchConfigurationResourceAction() {
    for (CreateSteps createStep: CreateSteps.values()) {
      createSteps.put(createStep.name(), createStep);
    }
    for (DeleteSteps deleteStep: DeleteSteps.values()) {
      deleteSteps.put(deleteStep.name(), deleteStep);
    }

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
    DELETE_LAUNCH_CONFIG {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        AWSAutoScalingLaunchConfigurationResourceAction action = (AWSAutoScalingLaunchConfigurationResourceAction) resourceAction;
        ServiceConfiguration configuration = Topology.lookup(AutoScaling.class);
        if (action.info.getPhysicalResourceId() == null) return action;
        DeleteLaunchConfigurationType deleteLaunchConfigurationType = MessageHelper.createMessage(DeleteLaunchConfigurationType.class, action.info.getEffectiveUserId());
        deleteLaunchConfigurationType.setLaunchConfigurationName(action.info.getPhysicalResourceId());
        AsyncRequests.<DeleteLaunchConfigurationType,DeleteLaunchConfigurationResponseType> sendSync(configuration, deleteLaunchConfigurationType);
        return action;
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

  @Override
  public Promise<String> getCreatePromise(WorkflowOperations<StackActivityClient> workflowOperations, String resourceId, String stackId, String accountId, String effectiveUserId) {
    List<String> stepIds = Lists.transform(Lists.newArrayList(CreateSteps.values()), StepTransform.INSTANCE);
    return new CreateMultiStepPromise(workflowOperations, stepIds, this).getCreatePromise(resourceId, stackId, accountId, effectiveUserId);
  }

  @Override
  public Promise<String> getDeletePromise(WorkflowOperations<StackActivityClient> workflowOperations, String resourceId, String stackId, String accountId, String effectiveUserId) {
    List<String> stepIds = Lists.transform(Lists.newArrayList(DeleteSteps.values()), StepTransform.INSTANCE);
    return new DeleteMultiStepPromise(workflowOperations, stepIds, this).getDeletePromise(resourceId, stackId, accountId, effectiveUserId);
  }

}


