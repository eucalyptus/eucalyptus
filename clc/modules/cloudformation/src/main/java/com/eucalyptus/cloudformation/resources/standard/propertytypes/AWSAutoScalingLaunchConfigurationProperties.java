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
