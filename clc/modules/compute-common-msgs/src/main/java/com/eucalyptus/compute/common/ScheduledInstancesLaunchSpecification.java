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
import edu.ucsb.eucalyptus.msgs.EucalyptusData;

public class ScheduledInstancesLaunchSpecification extends EucalyptusData {

  private ArrayList<ScheduledInstancesBlockDeviceMapping> blockDeviceMappings;
  private Boolean ebsOptimized;
  private ScheduledInstancesIamInstanceProfile iamInstanceProfile;
  private String imageId;
  private String instanceType;
  private String kernelId;
  private String keyName;
  private ScheduledInstancesMonitoring monitoring;
  @HttpEmbedded( multiple = true )
  private ArrayList<ScheduledInstancesNetworkInterface> networkInterfaces;
  private ScheduledInstancesPlacement placement;
  private String ramdiskId;
  private ArrayList<String> securityGroupId;
  private String subnetId;
  private String userData;

  public ArrayList<ScheduledInstancesBlockDeviceMapping> getBlockDeviceMappings( ) {
    return blockDeviceMappings;
  }

  public void setBlockDeviceMappings( ArrayList<ScheduledInstancesBlockDeviceMapping> blockDeviceMappings ) {
    this.blockDeviceMappings = blockDeviceMappings;
  }

  public Boolean getEbsOptimized( ) {
    return ebsOptimized;
  }

  public void setEbsOptimized( Boolean ebsOptimized ) {
    this.ebsOptimized = ebsOptimized;
  }

  public ScheduledInstancesIamInstanceProfile getIamInstanceProfile( ) {
    return iamInstanceProfile;
  }

  public void setIamInstanceProfile( ScheduledInstancesIamInstanceProfile iamInstanceProfile ) {
    this.iamInstanceProfile = iamInstanceProfile;
  }

  public String getImageId( ) {
    return imageId;
  }

  public void setImageId( String imageId ) {
    this.imageId = imageId;
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

  public String getKeyName( ) {
    return keyName;
  }

  public void setKeyName( String keyName ) {
    this.keyName = keyName;
  }

  public ScheduledInstancesMonitoring getMonitoring( ) {
    return monitoring;
  }

  public void setMonitoring( ScheduledInstancesMonitoring monitoring ) {
    this.monitoring = monitoring;
  }

  public ArrayList<ScheduledInstancesNetworkInterface> getNetworkInterfaces( ) {
    return networkInterfaces;
  }

  public void setNetworkInterfaces( ArrayList<ScheduledInstancesNetworkInterface> networkInterfaces ) {
    this.networkInterfaces = networkInterfaces;
  }

  public ScheduledInstancesPlacement getPlacement( ) {
    return placement;
  }

  public void setPlacement( ScheduledInstancesPlacement placement ) {
    this.placement = placement;
  }

  public String getRamdiskId( ) {
    return ramdiskId;
  }

  public void setRamdiskId( String ramdiskId ) {
    this.ramdiskId = ramdiskId;
  }

  public ArrayList<String> getSecurityGroupId( ) {
    return securityGroupId;
  }

  public void setSecurityGroupId( ArrayList<String> securityGroupId ) {
    this.securityGroupId = securityGroupId;
  }

  public String getSubnetId( ) {
    return subnetId;
  }

  public void setSubnetId( String subnetId ) {
    this.subnetId = subnetId;
  }

  public String getUserData( ) {
    return userData;
  }

  public void setUserData( String userData ) {
    this.userData = userData;
  }
}
