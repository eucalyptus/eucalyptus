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

import java.util.Map;
import javax.annotation.Nonnull;
import com.eucalyptus.autoscaling.common.AutoScalingMessageValidation;
import javaslang.collection.Stream;

public class CreateLaunchConfigurationType extends AutoScalingMessage {

  private Boolean associatePublicIpAddress;
  @Nonnull
  @AutoScalingMessageValidation.FieldRegex( AutoScalingMessageValidation.FieldRegexValue.NAME )
  private String launchConfigurationName;
  @Nonnull
  @AutoScalingMessageValidation.FieldRegex( AutoScalingMessageValidation.FieldRegexValue.EC2_MACHINE_IMAGE )
  private String imageId;
  @AutoScalingMessageValidation.FieldRegex( AutoScalingMessageValidation.FieldRegexValue.EC2_NAME )
  private String keyName;
  private SecurityGroups securityGroups;
  @AutoScalingMessageValidation.FieldRegex( AutoScalingMessageValidation.FieldRegexValue.EC2_USERDATA )
  private String userData;
  @Nonnull
  @AutoScalingMessageValidation.FieldRegex( AutoScalingMessageValidation.FieldRegexValue.EC2_NAME )
  private String instanceType;
  @AutoScalingMessageValidation.FieldRegex( AutoScalingMessageValidation.FieldRegexValue.EC2_KERNEL_IMAGE )
  private String kernelId;
  @AutoScalingMessageValidation.FieldRegex( AutoScalingMessageValidation.FieldRegexValue.EC2_RAMDISK_IMAGE )
  private String ramdiskId;
  private BlockDeviceMappings blockDeviceMappings;
  private InstanceMonitoring instanceMonitoring;
  @AutoScalingMessageValidation.FieldRegex( AutoScalingMessageValidation.FieldRegexValue.EC2_PLACEMENT_TENANCY )
  private String placementTenancy;
  @AutoScalingMessageValidation.FieldRegex( AutoScalingMessageValidation.FieldRegexValue.EC2_SPOT_PRICE )
  private String spotPrice;
  @AutoScalingMessageValidation.FieldRegex( AutoScalingMessageValidation.FieldRegexValue.IAM_NAME_OR_ARN )
  private String iamInstanceProfile;
  private Boolean ebsOptimized;

  @Override
  public Map<String, String> validate( ) {
    Map<String, String> errors = super.validate( );
    // Validate security group identifiers or names used consistently
    if ( securityGroups != null && securityGroups.getMember( ) != null ) {
      int idCount = Stream.ofAll( securityGroups.getMember( ) )
          .filter( group -> group.matches( "sg-[0-9A-Fa-f]{8}" ) )
          .size( );
      if ( idCount != 0 && idCount != securityGroups.getMember( ).size( ) ) {
        errors.put( "SecurityGroups.member", "Must use either use group-id or group-name for all the security groups, not both at the same time" );
      }

    }

    return errors;
  }

  public Boolean getAssociatePublicIpAddress( ) {
    return associatePublicIpAddress;
  }

  public void setAssociatePublicIpAddress( Boolean associatePublicIpAddress ) {
    this.associatePublicIpAddress = associatePublicIpAddress;
  }

  public String getLaunchConfigurationName( ) {
    return launchConfigurationName;
  }

  public void setLaunchConfigurationName( String launchConfigurationName ) {
    this.launchConfigurationName = launchConfigurationName;
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

  public String getPlacementTenancy( ) {
    return placementTenancy;
  }

  public void setPlacementTenancy( String placementTenancy ) {
    this.placementTenancy = placementTenancy;
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

  public Boolean getEbsOptimized( ) {
    return ebsOptimized;
  }

  public void setEbsOptimized( Boolean ebsOptimized ) {
    this.ebsOptimized = ebsOptimized;
  }
}
