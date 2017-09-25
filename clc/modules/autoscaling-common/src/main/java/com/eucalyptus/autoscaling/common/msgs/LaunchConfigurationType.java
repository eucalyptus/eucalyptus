/*************************************************************************
 * (c) Copyright 2017 Hewlett Packard Enterprise Development Company LP
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
 ************************************************************************/
package com.eucalyptus.autoscaling.common.msgs;

import java.util.Date;
import edu.ucsb.eucalyptus.msgs.EucalyptusData;

public class LaunchConfigurationType extends EucalyptusData {

  private String launchConfigurationName;
  private String launchConfigurationARN;
  private String imageId;
  private String keyName;
  private SecurityGroups securityGroups;
  private String userData;
  private String instanceType;
  private String kernelId;
  private String ramdiskId;
  private BlockDeviceMappings blockDeviceMappings;
  private InstanceMonitoring instanceMonitoring;
  private String spotPrice;
  private String iamInstanceProfile;
  private Date createdTime;
  private Boolean ebsOptimized;
  private Boolean associatePublicIpAddress;

  public String getLaunchConfigurationName( ) {
    return launchConfigurationName;
  }

  public void setLaunchConfigurationName( String launchConfigurationName ) {
    this.launchConfigurationName = launchConfigurationName;
  }

  public String getLaunchConfigurationARN( ) {
    return launchConfigurationARN;
  }

  public void setLaunchConfigurationARN( String launchConfigurationARN ) {
    this.launchConfigurationARN = launchConfigurationARN;
  }

  public String getImageId( ) {
    return imageId;
  }

  public void setImageId( String imageId ) {
    this.imageId = imageId;
  }

  public String getKeyName( ) {
    return keyName;
  }

  public void setKeyName( String keyName ) {
    this.keyName = keyName;
  }

  public SecurityGroups getSecurityGroups( ) {
    return securityGroups;
  }

  public void setSecurityGroups( SecurityGroups securityGroups ) {
    this.securityGroups = securityGroups;
  }

  public String getUserData( ) {
    return userData;
  }

  public void setUserData( String userData ) {
    this.userData = userData;
  }

  public String getInstanceType( ) {
    return instanceType;
  }

  public void setInstanceType( String instanceType ) {
    this.instanceType = instanceType;
  }

  public String getKernelId( ) {
    return kernelId;
  }

  public void setKernelId( String kernelId ) {
    this.kernelId = kernelId;
  }

  public String getRamdiskId( ) {
    return ramdiskId;
  }

  public void setRamdiskId( String ramdiskId ) {
    this.ramdiskId = ramdiskId;
  }

  public BlockDeviceMappings getBlockDeviceMappings( ) {
    return blockDeviceMappings;
  }

  public void setBlockDeviceMappings( BlockDeviceMappings blockDeviceMappings ) {
    this.blockDeviceMappings = blockDeviceMappings;
  }

  public InstanceMonitoring getInstanceMonitoring( ) {
    return instanceMonitoring;
  }

  public void setInstanceMonitoring( InstanceMonitoring instanceMonitoring ) {
    this.instanceMonitoring = instanceMonitoring;
  }

  public String getSpotPrice( ) {
    return spotPrice;
  }

  public void setSpotPrice( String spotPrice ) {
    this.spotPrice = spotPrice;
  }

  public String getIamInstanceProfile( ) {
    return iamInstanceProfile;
  }

  public void setIamInstanceProfile( String iamInstanceProfile ) {
    this.iamInstanceProfile = iamInstanceProfile;
  }

  public Date getCreatedTime( ) {
    return createdTime;
  }

  public void setCreatedTime( Date createdTime ) {
    this.createdTime = createdTime;
  }

  public Boolean getEbsOptimized( ) {
    return ebsOptimized;
  }

  public void setEbsOptimized( Boolean ebsOptimized ) {
    this.ebsOptimized = ebsOptimized;
  }

  public Boolean getAssociatePublicIpAddress( ) {
    return associatePublicIpAddress;
  }

  public void setAssociatePublicIpAddress( Boolean associatePublicIpAddress ) {
    this.associatePublicIpAddress = associatePublicIpAddress;
  }
}
