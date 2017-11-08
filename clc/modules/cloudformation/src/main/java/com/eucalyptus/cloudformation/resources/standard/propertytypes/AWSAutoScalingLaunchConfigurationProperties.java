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
package com.eucalyptus.cloudformation.resources.standard.propertytypes;

import java.util.ArrayList;
import com.eucalyptus.cloudformation.resources.ResourceProperties;
import com.eucalyptus.cloudformation.resources.annotations.Property;
import com.eucalyptus.cloudformation.resources.annotations.Required;
import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;

public class AWSAutoScalingLaunchConfigurationProperties implements ResourceProperties {

  @Property
  private Boolean associatePublicIpAddress;

  @Property
  private ArrayList<AutoScalingBlockDeviceMapping> blockDeviceMappings = Lists.newArrayList( );

  @Property
  private Boolean ebsOptimized;

  @Property
  private String iamInstanceProfile;

  @Required
  @Property
  private String imageId;

  @Property
  private String instanceId;

  @Property
  private Boolean instanceMonitoring;

  @Required
  @Property
  private String instanceType;

  @Property
  private String kernelId;

  @Property
  private String keyName;

  @Property
  private String ramDiskId;

  @Property
  private ArrayList<String> securityGroups = Lists.newArrayList( );

  @Property
  private String spotPrice;

  @Property
  private String userData;

  public Boolean getAssociatePublicIpAddress( ) {
    return associatePublicIpAddress;
  }

  public void setAssociatePublicIpAddress( Boolean associatePublicIpAddress ) {
    this.associatePublicIpAddress = associatePublicIpAddress;
  }

  public ArrayList<AutoScalingBlockDeviceMapping> getBlockDeviceMappings( ) {
    return blockDeviceMappings;
  }

  public void setBlockDeviceMappings( ArrayList<AutoScalingBlockDeviceMapping> blockDeviceMappings ) {
    this.blockDeviceMappings = blockDeviceMappings;
  }

  public Boolean getEbsOptimized( ) {
    return ebsOptimized;
  }

  public void setEbsOptimized( Boolean ebsOptimized ) {
    this.ebsOptimized = ebsOptimized;
  }

  public String getIamInstanceProfile( ) {
    return iamInstanceProfile;
  }

  public void setIamInstanceProfile( String iamInstanceProfile ) {
    this.iamInstanceProfile = iamInstanceProfile;
  }

  public String getImageId( ) {
    return imageId;
  }

  public void setImageId( String imageId ) {
    this.imageId = imageId;
  }

  public String getInstanceId( ) {
    return instanceId;
  }

  public void setInstanceId( String instanceId ) {
    this.instanceId = instanceId;
  }

  public Boolean getInstanceMonitoring( ) {
    return instanceMonitoring;
  }

  public void setInstanceMonitoring( Boolean instanceMonitoring ) {
    this.instanceMonitoring = instanceMonitoring;
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

  public String getRamDiskId( ) {
    return ramDiskId;
  }

  public void setRamDiskId( String ramDiskId ) {
    this.ramDiskId = ramDiskId;
  }

  public ArrayList<String> getSecurityGroups( ) {
    return securityGroups;
  }

  public void setSecurityGroups( ArrayList<String> securityGroups ) {
    this.securityGroups = securityGroups;
  }

  public String getSpotPrice( ) {
    return spotPrice;
  }

  public void setSpotPrice( String spotPrice ) {
    this.spotPrice = spotPrice;
  }

  public String getUserData( ) {
    return userData;
  }

  public void setUserData( String userData ) {
    this.userData = userData;
  }

  @Override
  public String toString( ) {
    return MoreObjects.toStringHelper( this )
        .add( "associatePublicIpAddress", associatePublicIpAddress )
        .add( "blockDeviceMappings", blockDeviceMappings )
        .add( "ebsOptimized", ebsOptimized )
        .add( "iamInstanceProfile", iamInstanceProfile )
        .add( "imageId", imageId )
        .add( "instanceId", instanceId )
        .add( "instanceMonitoring", instanceMonitoring )
        .add( "instanceType", instanceType )
        .add( "kernelId", kernelId )
        .add( "keyName", keyName )
        .add( "ramDiskId", ramDiskId )
        .add( "securityGroups", securityGroups )
        .add( "spotPrice", spotPrice )
        .add( "userData", userData )
        .toString( );
  }
}
