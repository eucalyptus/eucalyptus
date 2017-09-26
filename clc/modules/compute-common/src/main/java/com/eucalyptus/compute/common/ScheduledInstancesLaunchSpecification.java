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
