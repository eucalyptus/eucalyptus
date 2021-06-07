/**
 * Copyright 2020 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.cloudformation.resources.standard.actions;

import java.util.Objects;
import java.util.function.Function;
import com.eucalyptus.cloudformation.resources.ResourceAction;
import com.eucalyptus.cloudformation.resources.ResourceInfo;
import com.eucalyptus.cloudformation.resources.ResourceProperties;
import com.eucalyptus.cloudformation.resources.standard.info.AWSEC2LaunchTemplateResourceInfo;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.AWSEC2LaunchTemplateProperties;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.EC2BlockDeviceMapping;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.EC2NetworkInterfaceSpecification;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.EC2Tag;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.EC2TagSpecification;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.LaunchTemplateData;
import com.eucalyptus.cloudformation.template.JsonHelper;
import com.eucalyptus.cloudformation.util.MessageHelper;
import com.eucalyptus.cloudformation.workflow.steps.Step;
import com.eucalyptus.cloudformation.workflow.steps.StepBasedResourceAction;
import com.eucalyptus.cloudformation.workflow.updateinfo.UpdateType;
import com.eucalyptus.compute.common.ComputeApi;
import com.eucalyptus.compute.common.CreateLaunchTemplateResponseType;
import com.eucalyptus.compute.common.CreateLaunchTemplateType;
import com.eucalyptus.compute.common.DeleteLaunchTemplateType;
import com.eucalyptus.compute.common.LaunchTemplateBlockDeviceMappingRequest;
import com.eucalyptus.compute.common.LaunchTemplateBlockDeviceMappingRequestList;
import com.eucalyptus.compute.common.LaunchTemplateEbsBlockDeviceRequest;
import com.eucalyptus.compute.common.LaunchTemplateIamInstanceProfileSpecificationRequest;
import com.eucalyptus.compute.common.LaunchTemplateInstanceNetworkInterfaceSpecificationRequest;
import com.eucalyptus.compute.common.LaunchTemplateInstanceNetworkInterfaceSpecificationRequestList;
import com.eucalyptus.compute.common.LaunchTemplatePlacementRequest;
import com.eucalyptus.compute.common.LaunchTemplateTagSpecificationRequest;
import com.eucalyptus.compute.common.LaunchTemplateTagSpecificationRequestList;
import com.eucalyptus.compute.common.LaunchTemplatesMonitoringRequest;
import com.eucalyptus.compute.common.RequestLaunchTemplateData;
import com.eucalyptus.compute.common.ResourceTag;
import com.eucalyptus.compute.common.SecurityGroupIdStringList;
import com.eucalyptus.compute.common.SecurityGroupStringList;
import com.eucalyptus.util.async.AsyncExceptions;
import com.eucalyptus.util.async.AsyncProxy;
import com.fasterxml.jackson.databind.node.TextNode;

/**
 *
 */
public class AWSEC2LaunchTemplateResourceAction extends StepBasedResourceAction {
  private AWSEC2LaunchTemplateProperties properties = new AWSEC2LaunchTemplateProperties();
  private AWSEC2LaunchTemplateResourceInfo info = new AWSEC2LaunchTemplateResourceInfo();

  public AWSEC2LaunchTemplateResourceAction() {
    super(fromEnum(CreateSteps.class),
        fromEnum(DeleteSteps.class),
        null,
        null);
  }

  @Override
  public ResourceProperties getResourceProperties() {
    return properties;
  }

  @Override
  public void setResourceProperties(final ResourceProperties resourceProperties) {
    properties = (AWSEC2LaunchTemplateProperties) resourceProperties;
  }

  @Override
  public ResourceInfo getResourceInfo() {
    return info;
  }

  @Override
  public void setResourceInfo(final ResourceInfo resourceInfo) {
    info = (AWSEC2LaunchTemplateResourceInfo) resourceInfo;
  }

  @Override
  public UpdateType getUpdateType(final ResourceAction resourceAction, final boolean stackTagsChanged) {
    UpdateType updateType = info.supportsTags() && stackTagsChanged ? UpdateType.NO_INTERRUPTION : UpdateType.NONE;
    final AWSEC2LaunchTemplateResourceAction otherAction = (AWSEC2LaunchTemplateResourceAction) resourceAction;
    if (!Objects.equals(properties.getLaunchTemplateData(), otherAction.properties.getLaunchTemplateData())) {
      updateType = UpdateType.max(updateType, UpdateType.NO_INTERRUPTION);
    }
    return updateType;
  }

  private static RequestLaunchTemplateData toRequestLaunchTemplateData(final LaunchTemplateData launchTemplateData) {
    final RequestLaunchTemplateData data = new RequestLaunchTemplateData();
    data.setKernelId(launchTemplateData.getKernelId());
    data.setRamDiskId(launchTemplateData.getRamdiskId());
    data.setImageId(launchTemplateData.getImageId());
    data.setInstanceType(launchTemplateData.getInstanceType());
    data.setInstanceInitiatedShutdownBehavior(launchTemplateData.getInstanceInitiatedShutdownBehavior());
    data.setEbsOptimized(launchTemplateData.getEbsOptimized());
    data.setDisableApiTermination(launchTemplateData.getDisableApiTermination());
    data.setKeyName(launchTemplateData.getKeyName());
    if (!launchTemplateData.getBlockDeviceMappings().isEmpty()){
      final LaunchTemplateBlockDeviceMappingRequestList blockDeviceMappings = new LaunchTemplateBlockDeviceMappingRequestList();
      for (final EC2BlockDeviceMapping ec2BlockDeviceMapping : launchTemplateData.getBlockDeviceMappings()) {
        final LaunchTemplateBlockDeviceMappingRequest blockDeviceMapping =  new LaunchTemplateBlockDeviceMappingRequest();
        blockDeviceMapping.setDeviceName(ec2BlockDeviceMapping.getDeviceName());
        if (ec2BlockDeviceMapping.getEbs()!=null) {
          final LaunchTemplateEbsBlockDeviceRequest ebs = new LaunchTemplateEbsBlockDeviceRequest();
          ebs.setDeleteOnTermination(ec2BlockDeviceMapping.getEbs().getDeleteOnTermination());
          ebs.setIops(ec2BlockDeviceMapping.getEbs().getIops());
          ebs.setSnapshotId(ec2BlockDeviceMapping.getEbs().getSnapshotId());
          if (ec2BlockDeviceMapping.getEbs().getVolumeSize()!=null) {
            ebs.setVolumeSize(Integer.parseInt(ec2BlockDeviceMapping.getEbs().getVolumeSize()));
          }
          ebs.setVolumeType(ec2BlockDeviceMapping.getEbs().getVolumeType());
          blockDeviceMapping.setEbs(ebs);
        }
        if (ec2BlockDeviceMapping.getNoDevice()!=null) {
          blockDeviceMapping.setNoDevice("");
        }
        blockDeviceMapping.setVirtualName(ec2BlockDeviceMapping.getVirtualName());
        blockDeviceMappings.getMember().add(blockDeviceMapping);
      }
      data.setBlockDeviceMappings(blockDeviceMappings);
    }
    if (!launchTemplateData.getNetworkInterfaces().isEmpty()) {
      final LaunchTemplateInstanceNetworkInterfaceSpecificationRequestList networkInterfaces =
          new  LaunchTemplateInstanceNetworkInterfaceSpecificationRequestList();
      for (final EC2NetworkInterfaceSpecification ec2NetworkInterface : launchTemplateData.getNetworkInterfaces()) {
        final LaunchTemplateInstanceNetworkInterfaceSpecificationRequest networkInterfaceSpecification =
            new LaunchTemplateInstanceNetworkInterfaceSpecificationRequest();
        networkInterfaceSpecification.setAssociatePublicIpAddress(ec2NetworkInterface.getAssociatePublicIpAddress());
        networkInterfaceSpecification.setDeleteOnTermination(ec2NetworkInterface.getDeleteOnTermination());
        networkInterfaceSpecification.setDescription(ec2NetworkInterface.getDescription());
        networkInterfaceSpecification.setDeviceIndex(ec2NetworkInterface.getDeviceIndex());
        if (!ec2NetworkInterface.getGroups().isEmpty()) {
          final SecurityGroupIdStringList ids = new SecurityGroupIdStringList();
          ids.getMember().addAll(ec2NetworkInterface.getGroups());
          networkInterfaceSpecification.setGroups(ids);
        }
        networkInterfaceSpecification.setNetworkInterfaceId(ec2NetworkInterface.getNetworkInterfaceId());
        networkInterfaceSpecification.setPrivateIpAddress(ec2NetworkInterface.getPrivateIpAddress());
        networkInterfaceSpecification.setSubnetId(ec2NetworkInterface.getSubnetId());
        networkInterfaces.getMember().add(networkInterfaceSpecification);
      }
      data.setNetworkInterfaces(networkInterfaces);
    }
    if (launchTemplateData.getPlacement() != null &&
        launchTemplateData.getPlacement().getAvailabilityZone() != null) {
      final LaunchTemplatePlacementRequest launchTemplatePlacementRequest = new LaunchTemplatePlacementRequest();
      launchTemplatePlacementRequest.setAvailabilityZone(launchTemplateData.getPlacement().getAvailabilityZone());
      data.setPlacement(launchTemplatePlacementRequest);
    }
    if (launchTemplateData.getIamInstanceProfile() != null) {
      final LaunchTemplateIamInstanceProfileSpecificationRequest launchTemplateIamInstanceProfileSpecificationRequest =
          new LaunchTemplateIamInstanceProfileSpecificationRequest();
      launchTemplateIamInstanceProfileSpecificationRequest.setArn(launchTemplateData.getIamInstanceProfile().getArn());
      launchTemplateIamInstanceProfileSpecificationRequest.setName(launchTemplateData.getIamInstanceProfile().getName());
      data.setIamInstanceProfile(launchTemplateIamInstanceProfileSpecificationRequest);
    }
    if (launchTemplateData.getMonitoring() != null && launchTemplateData.getMonitoring().getEnabled() != null) {
      final LaunchTemplatesMonitoringRequest monitoring = new LaunchTemplatesMonitoringRequest();
      monitoring.setEnabled(launchTemplateData.getMonitoring().getEnabled());
      data.setMonitoring(monitoring);
    }
    if (!launchTemplateData.getSecurityGroupIds().isEmpty()) {
      final SecurityGroupIdStringList ids = new SecurityGroupIdStringList();
      ids.setMember(launchTemplateData.getSecurityGroupIds());
      data.setSecurityGroupIds(ids);
    }
    if (!launchTemplateData.getSecurityGroups().isEmpty()) {
      final SecurityGroupStringList groups = new SecurityGroupStringList();
      groups.setMember(launchTemplateData.getSecurityGroups());
      data.setSecurityGroups(groups);
    }
    data.setUserData(launchTemplateData.getUserData());
    if (launchTemplateData.getTagSpecifications()!=null && !launchTemplateData.getTagSpecifications().isEmpty()) {
      final LaunchTemplateTagSpecificationRequestList tagSpecifications = new LaunchTemplateTagSpecificationRequestList();
      for (final EC2TagSpecification ec2TagSpecification : launchTemplateData.getTagSpecifications()) {
        final LaunchTemplateTagSpecificationRequest tagSpec = new LaunchTemplateTagSpecificationRequest();
        tagSpec.setResourceType(ec2TagSpecification.getResourceType());
        for (final EC2Tag ec2Tag : ec2TagSpecification.getTags()) {
          final ResourceTag resourceTag = new ResourceTag();
          resourceTag.setKey(ec2Tag.getKey());
          resourceTag.setValue(ec2Tag.getValue());
          tagSpec.getTagSet().add(resourceTag);
        }
        tagSpecifications.getMember().add(tagSpec);
      }
      data.setTagSpecifications(tagSpecifications);
    }
    return data;
  }

  private enum CreateSteps implements Step {
    CREATE_LAUNCH_TEMPLATE {
      @Override
      public ResourceAction perform(final ResourceAction resourceAction) throws Exception {
        final AWSEC2LaunchTemplateResourceAction action = (AWSEC2LaunchTemplateResourceAction) resourceAction;
        final CreateLaunchTemplateType createLaunchTemplate =
            MessageHelper.createMessage(CreateLaunchTemplateType.class, action.info.getEffectiveUserId());
        createLaunchTemplate.setLaunchTemplateName(action.properties.getName());
        if (createLaunchTemplate.getLaunchTemplateName() == null){
          createLaunchTemplate.setLaunchTemplateName(action.getDefaultPhysicalResourceId());
        }
        createLaunchTemplate.setLaunchTemplateData(toRequestLaunchTemplateData(action.properties.getLaunchTemplateData()));
        final CreateLaunchTemplateResponseType createLaunchTemplateResponse =
            AsyncProxy.client(ComputeApi.class, Function.identity()).createLaunchTemplate(createLaunchTemplate);
        action.info.setPhysicalResourceId(createLaunchTemplateResponse.getLaunchTemplate().getLaunchTemplateId());
        action.info.setCreatedEnoughToDelete(true);
        action.info.setReferenceValueJson(JsonHelper.getStringFromJsonNode(new TextNode(action.info.getPhysicalResourceId())));
        return action;
      }
    }
  }

  private enum DeleteSteps implements Step {
    DELETE_LAUNCH_TEMPLATE {
      @Override
      public ResourceAction perform(final ResourceAction resourceAction) throws Exception {
        final AWSEC2LaunchTemplateResourceAction action = (AWSEC2LaunchTemplateResourceAction) resourceAction;
        if (!Boolean.TRUE.equals(action.info.getCreatedEnoughToDelete())) return action;
        final DeleteLaunchTemplateType deleteLaunchTemplate =
            MessageHelper.createMessage(DeleteLaunchTemplateType.class, action.info.getEffectiveUserId());
        deleteLaunchTemplate.setLaunchTemplateId(action.info.getPhysicalResourceId());
        try {
          AsyncProxy.client(ComputeApi.class, Function.identity()).deleteLaunchTemplate(deleteLaunchTemplate);
        } catch ( final RuntimeException e ) {
          if (!AsyncExceptions.isWebServiceErrorCode(e,"InvalidLaunchTemplateId.NotFound")) {
            throw e;
          }
        }
        return action;
      }
    }
  }
}
