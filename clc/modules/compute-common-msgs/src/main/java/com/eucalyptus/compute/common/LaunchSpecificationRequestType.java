/*************************************************************************
 * Copyright 2009-2014 Ent. Services Development Corporation LP
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
package com.eucalyptus.compute.common;

import java.util.ArrayList;
import com.eucalyptus.binding.HttpEmbedded;
import com.eucalyptus.binding.HttpParameterMapping;
import edu.ucsb.eucalyptus.msgs.EucalyptusData;

public class LaunchSpecificationRequestType extends EucalyptusData {

  private String imageId;
  private String keyName;
  private ArrayList<String> securityGroupId;
  private ArrayList<String> securityGroup;
  private String userData;
  private String instanceType;
  @HttpParameterMapping( parameter = "Placement.AvailabilityZone" )
  private String availabilityZone;
  @HttpParameterMapping( parameter = "Placement.GroupName" )
  private String groupName;
  private String kernelId;
  private String ramdiskId;
  @HttpEmbedded( multiple = true )
  private ArrayList<BlockDeviceMappingItemType> blockDeviceMapping;
  @HttpParameterMapping( parameter = "Monitoring.Enabled" )
  private Boolean monitoringEnabled;
  private String subnetId;
  @HttpEmbedded( multiple = true )
  private ArrayList<InstanceNetworkInterfaceSetItemRequestType> networkInterface;
  @HttpParameterMapping( parameter = "IamInstanceProfile.Arn" )
  private String instanceProfileArn;
  @HttpParameterMapping( parameter = "IamInstanceProfile.Name" )
  private String instanceProfileName;
  private Boolean ebsOptimized;

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

  public ArrayList<String> getSecurityGroupId( ) {
    return securityGroupId;
  }

  public void setSecurityGroupId( ArrayList<String> securityGroupId ) {
    this.securityGroupId = securityGroupId;
  }

  public ArrayList<String> getSecurityGroup( ) {
    return securityGroup;
  }

  public void setSecurityGroup( ArrayList<String> securityGroup ) {
    this.securityGroup = securityGroup;
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

  public String getAvailabilityZone( ) {
    return availabilityZone;
  }

  public void setAvailabilityZone( String availabilityZone ) {
    this.availabilityZone = availabilityZone;
  }

  public String getGroupName( ) {
    return groupName;
  }

  public void setGroupName( String groupName ) {
    this.groupName = groupName;
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

  public ArrayList<BlockDeviceMappingItemType> getBlockDeviceMapping( ) {
    return blockDeviceMapping;
  }

  public void setBlockDeviceMapping( ArrayList<BlockDeviceMappingItemType> blockDeviceMapping ) {
    this.blockDeviceMapping = blockDeviceMapping;
  }

  public Boolean getMonitoringEnabled( ) {
    return monitoringEnabled;
  }

  public void setMonitoringEnabled( Boolean monitoringEnabled ) {
    this.monitoringEnabled = monitoringEnabled;
  }

  public String getSubnetId( ) {
    return subnetId;
  }

  public void setSubnetId( String subnetId ) {
    this.subnetId = subnetId;
  }

  public ArrayList<InstanceNetworkInterfaceSetItemRequestType> getNetworkInterface( ) {
    return networkInterface;
  }

  public void setNetworkInterface( ArrayList<InstanceNetworkInterfaceSetItemRequestType> networkInterface ) {
    this.networkInterface = networkInterface;
  }

  public String getInstanceProfileArn( ) {
    return instanceProfileArn;
  }

  public void setInstanceProfileArn( String instanceProfileArn ) {
    this.instanceProfileArn = instanceProfileArn;
  }

  public String getInstanceProfileName( ) {
    return instanceProfileName;
  }

  public void setInstanceProfileName( String instanceProfileName ) {
    this.instanceProfileName = instanceProfileName;
  }

  public Boolean getEbsOptimized( ) {
    return ebsOptimized;
  }

  public void setEbsOptimized( Boolean ebsOptimized ) {
    this.ebsOptimized = ebsOptimized;
  }
}
