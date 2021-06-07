/*
 * Copyright 2018 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.compute.common;

import com.eucalyptus.binding.HttpEmbedded;
import com.eucalyptus.binding.HttpParameterMapping;
import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class RequestLaunchTemplateData extends EucalyptusData {

  @HttpEmbedded
  private LaunchTemplateBlockDeviceMappingRequestList blockDeviceMappings;
  private LaunchTemplateCpuOptionsRequest cpuOptions;
  private CreditSpecificationRequest creditSpecification;
  private Boolean disableApiTermination;
  private Boolean ebsOptimized;
  @HttpEmbedded
  private ElasticGpuSpecificationList elasticGpuSpecifications;
  private LaunchTemplateIamInstanceProfileSpecificationRequest iamInstanceProfile;
  private String imageId;
  private String instanceInitiatedShutdownBehavior;
  private LaunchTemplateInstanceMarketOptionsRequest instanceMarketOptions;
  private String instanceType;
  private String kernelId;
  private String keyName;
  private LaunchTemplatesMonitoringRequest monitoring;
  @HttpEmbedded
  private LaunchTemplateInstanceNetworkInterfaceSpecificationRequestList networkInterfaces;
  private LaunchTemplatePlacementRequest placement;
  private String ramDiskId;
  @HttpParameterMapping( parameter = "SecurityGroupId" )
  private SecurityGroupIdStringList securityGroupIds;
  @HttpParameterMapping( parameter = "SecurityGroup" )
  private SecurityGroupStringList securityGroups;
  @HttpEmbedded
  private LaunchTemplateTagSpecificationRequestList tagSpecifications;
  private String userData;

  public LaunchTemplateBlockDeviceMappingRequestList getBlockDeviceMappings( ) {
    return blockDeviceMappings;
  }

  public void setBlockDeviceMappings( final LaunchTemplateBlockDeviceMappingRequestList blockDeviceMappings ) {
    this.blockDeviceMappings = blockDeviceMappings;
  }

  public LaunchTemplateCpuOptionsRequest getCpuOptions( ) {
    return cpuOptions;
  }

  public void setCpuOptions( final LaunchTemplateCpuOptionsRequest cpuOptions ) {
    this.cpuOptions = cpuOptions;
  }

  public CreditSpecificationRequest getCreditSpecification( ) {
    return creditSpecification;
  }

  public void setCreditSpecification( final CreditSpecificationRequest creditSpecification ) {
    this.creditSpecification = creditSpecification;
  }

  public Boolean getDisableApiTermination( ) {
    return disableApiTermination;
  }

  public void setDisableApiTermination( final Boolean disableApiTermination ) {
    this.disableApiTermination = disableApiTermination;
  }

  public Boolean getEbsOptimized( ) {
    return ebsOptimized;
  }

  public void setEbsOptimized( final Boolean ebsOptimized ) {
    this.ebsOptimized = ebsOptimized;
  }

  public ElasticGpuSpecificationList getElasticGpuSpecifications( ) {
    return elasticGpuSpecifications;
  }

  public void setElasticGpuSpecifications( final ElasticGpuSpecificationList elasticGpuSpecifications ) {
    this.elasticGpuSpecifications = elasticGpuSpecifications;
  }

  public LaunchTemplateIamInstanceProfileSpecificationRequest getIamInstanceProfile( ) {
    return iamInstanceProfile;
  }

  public void setIamInstanceProfile( final LaunchTemplateIamInstanceProfileSpecificationRequest iamInstanceProfile ) {
    this.iamInstanceProfile = iamInstanceProfile;
  }

  public String getImageId( ) {
    return imageId;
  }

  public void setImageId( final String imageId ) {
    this.imageId = imageId;
  }

  public String getInstanceInitiatedShutdownBehavior( ) {
    return instanceInitiatedShutdownBehavior;
  }

  public void setInstanceInitiatedShutdownBehavior( final String instanceInitiatedShutdownBehavior ) {
    this.instanceInitiatedShutdownBehavior = instanceInitiatedShutdownBehavior;
  }

  public LaunchTemplateInstanceMarketOptionsRequest getInstanceMarketOptions( ) {
    return instanceMarketOptions;
  }

  public void setInstanceMarketOptions( final LaunchTemplateInstanceMarketOptionsRequest instanceMarketOptions ) {
    this.instanceMarketOptions = instanceMarketOptions;
  }

  public String getInstanceType( ) {
    return instanceType;
  }

  public void setInstanceType( final String instanceType ) {
    this.instanceType = instanceType;
  }

  public String getKernelId( ) {
    return kernelId;
  }

  public void setKernelId( final String kernelId ) {
    this.kernelId = kernelId;
  }

  public String getKeyName( ) {
    return keyName;
  }

  public void setKeyName( final String keyName ) {
    this.keyName = keyName;
  }

  public LaunchTemplatesMonitoringRequest getMonitoring( ) {
    return monitoring;
  }

  public void setMonitoring( final LaunchTemplatesMonitoringRequest monitoring ) {
    this.monitoring = monitoring;
  }

  public LaunchTemplateInstanceNetworkInterfaceSpecificationRequestList getNetworkInterfaces( ) {
    return networkInterfaces;
  }

  public void setNetworkInterfaces( final LaunchTemplateInstanceNetworkInterfaceSpecificationRequestList networkInterfaces ) {
    this.networkInterfaces = networkInterfaces;
  }

  public LaunchTemplatePlacementRequest getPlacement( ) {
    return placement;
  }

  public void setPlacement( final LaunchTemplatePlacementRequest placement ) {
    this.placement = placement;
  }

  public String getRamDiskId( ) {
    return ramDiskId;
  }

  public void setRamDiskId( final String ramDiskId ) {
    this.ramDiskId = ramDiskId;
  }

  public SecurityGroupIdStringList getSecurityGroupIds( ) {
    return securityGroupIds;
  }

  public void setSecurityGroupIds( final SecurityGroupIdStringList securityGroupIds ) {
    this.securityGroupIds = securityGroupIds;
  }

  public SecurityGroupStringList getSecurityGroups( ) {
    return securityGroups;
  }

  public void setSecurityGroups( final SecurityGroupStringList securityGroups ) {
    this.securityGroups = securityGroups;
  }

  public LaunchTemplateTagSpecificationRequestList getTagSpecifications( ) {
    return tagSpecifications;
  }

  public void setTagSpecifications( final LaunchTemplateTagSpecificationRequestList tagSpecifications ) {
    this.tagSpecifications = tagSpecifications;
  }

  public String getUserData( ) {
    return userData;
  }

  public void setUserData( final String userData ) {
    this.userData = userData;
  }

}
