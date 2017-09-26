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
