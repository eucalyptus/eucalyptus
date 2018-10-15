/*
 * Copyright 2018 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.compute.common;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class ResponseLaunchTemplateData extends EucalyptusData {

  private LaunchTemplateBlockDeviceMappingList blockDeviceMappings;
  private LaunchTemplateCpuOptions cpuOptions;
  private CreditSpecification creditSpecification;
  private Boolean disableApiTermination;
  private Boolean ebsOptimized;
  private ElasticGpuSpecificationResponseList elasticGpuSpecifications;
  private LaunchTemplateIamInstanceProfileSpecification iamInstanceProfile;
  private String imageId;
  private String instanceInitiatedShutdownBehavior;
  private LaunchTemplateInstanceMarketOptions instanceMarketOptions;
  private String instanceType;
  private String kernelId;
  private String keyName;
  private LaunchTemplatesMonitoring monitoring;
  private LaunchTemplateInstanceNetworkInterfaceSpecificationList networkInterfaces;
  private LaunchTemplatePlacement placement;
  private String ramDiskId;
  private ValueStringList securityGroupIds;
  private ValueStringList securityGroups;
  private LaunchTemplateTagSpecificationList tagSpecifications;
  private String userData;

  public LaunchTemplateBlockDeviceMappingList getBlockDeviceMappings( ) {
    return blockDeviceMappings;
  }

  public void setBlockDeviceMappings( final LaunchTemplateBlockDeviceMappingList blockDeviceMappings ) {
    this.blockDeviceMappings = blockDeviceMappings;
  }

  public LaunchTemplateCpuOptions getCpuOptions( ) {
    return cpuOptions;
  }

  public void setCpuOptions( final LaunchTemplateCpuOptions cpuOptions ) {
    this.cpuOptions = cpuOptions;
  }

  public CreditSpecification getCreditSpecification( ) {
    return creditSpecification;
  }

  public void setCreditSpecification( final CreditSpecification creditSpecification ) {
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

  public ElasticGpuSpecificationResponseList getElasticGpuSpecifications( ) {
    return elasticGpuSpecifications;
  }

  public void setElasticGpuSpecifications( final ElasticGpuSpecificationResponseList elasticGpuSpecifications ) {
    this.elasticGpuSpecifications = elasticGpuSpecifications;
  }

  public LaunchTemplateIamInstanceProfileSpecification getIamInstanceProfile( ) {
    return iamInstanceProfile;
  }

  public void setIamInstanceProfile( final LaunchTemplateIamInstanceProfileSpecification iamInstanceProfile ) {
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

  public LaunchTemplateInstanceMarketOptions getInstanceMarketOptions( ) {
    return instanceMarketOptions;
  }

  public void setInstanceMarketOptions( final LaunchTemplateInstanceMarketOptions instanceMarketOptions ) {
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

  public LaunchTemplatesMonitoring getMonitoring( ) {
    return monitoring;
  }

  public void setMonitoring( final LaunchTemplatesMonitoring monitoring ) {
    this.monitoring = monitoring;
  }

  public LaunchTemplateInstanceNetworkInterfaceSpecificationList getNetworkInterfaces( ) {
    return networkInterfaces;
  }

  public void setNetworkInterfaces( final LaunchTemplateInstanceNetworkInterfaceSpecificationList networkInterfaces ) {
    this.networkInterfaces = networkInterfaces;
  }

  public LaunchTemplatePlacement getPlacement( ) {
    return placement;
  }

  public void setPlacement( final LaunchTemplatePlacement placement ) {
    this.placement = placement;
  }

  public String getRamDiskId( ) {
    return ramDiskId;
  }

  public void setRamDiskId( final String ramDiskId ) {
    this.ramDiskId = ramDiskId;
  }

  public ValueStringList getSecurityGroupIds( ) {
    return securityGroupIds;
  }

  public void setSecurityGroupIds( final ValueStringList securityGroupIds ) {
    this.securityGroupIds = securityGroupIds;
  }

  public ValueStringList getSecurityGroups( ) {
    return securityGroups;
  }

  public void setSecurityGroups( final ValueStringList securityGroups ) {
    this.securityGroups = securityGroups;
  }

  public LaunchTemplateTagSpecificationList getTagSpecifications( ) {
    return tagSpecifications;
  }

  public void setTagSpecifications( final LaunchTemplateTagSpecificationList tagSpecifications ) {
    this.tagSpecifications = tagSpecifications;
  }

  public String getUserData( ) {
    return userData;
  }

  public void setUserData( final String userData ) {
    this.userData = userData;
  }

}
