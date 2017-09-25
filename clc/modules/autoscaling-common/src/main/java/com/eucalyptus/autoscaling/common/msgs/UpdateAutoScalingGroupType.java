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

import java.util.Collection;
import java.util.Map;
import javax.annotation.Nonnull;
import com.eucalyptus.autoscaling.common.AutoScalingMessageValidation;
import com.eucalyptus.binding.HttpParameterMapping;

public class UpdateAutoScalingGroupType extends AutoScalingMessage {

  @Nonnull
  @AutoScalingMessageValidation.FieldRegex( AutoScalingMessageValidation.FieldRegexValue.NAME_OR_ARN )
  private String autoScalingGroupName;
  @AutoScalingMessageValidation.FieldRegex( AutoScalingMessageValidation.FieldRegexValue.NAME_OR_ARN )
  private String launchConfigurationName;
  @AutoScalingMessageValidation.FieldRange
  private Integer minSize;
  @AutoScalingMessageValidation.FieldRange
  private Integer maxSize;
  @AutoScalingMessageValidation.FieldRange
  private Integer desiredCapacity;
  @AutoScalingMessageValidation.FieldRange
  private Integer defaultCooldown;
  private AvailabilityZones availabilityZones;
  @AutoScalingMessageValidation.FieldRegex( AutoScalingMessageValidation.FieldRegexValue.HEALTH_CHECK )
  private String healthCheckType;
  @AutoScalingMessageValidation.FieldRange
  private Integer healthCheckGracePeriod;
  private Boolean newInstancesProtectedFromScaleIn;
  @AutoScalingMessageValidation.FieldRegex( AutoScalingMessageValidation.FieldRegexValue.EC2_NAME )
  private String placementGroup;
  @AutoScalingMessageValidation.FieldRegex( AutoScalingMessageValidation.FieldRegexValue.EC2_NAME )
  @HttpParameterMapping( parameter = "VPCZoneIdentifier" )
  private String vpcZoneIdentifier;
  private TerminationPolicies terminationPolicies;

  public Collection<String> availabilityZones( ) {
    final AvailabilityZones zones = availabilityZones;
    return ( zones == null ? null : zones.getMember( ) );
  }

  public Collection<String> terminationPolicies( ) {
    final TerminationPolicies policies = terminationPolicies;
    return ( policies == null ? null : policies.getMember( ) );
  }

  @Override
  public Map<String, String> validate( ) {
    Map<String, String> errors = super.validate( );
    if ( minSize != null && maxSize != null && minSize > maxSize ) {
      errors.put( "MinSize", "MinSize must not be greater than MaxSize" );
    }

    if ( minSize != null && desiredCapacity != null && desiredCapacity < minSize ) {
      errors.put( "DesiredCapacity", "DesiredCapacity must not be less than MinSize" );
    }

    if ( maxSize != null && desiredCapacity != null && desiredCapacity > maxSize ) {
      errors.put( "DesiredCapacity", "DesiredCapacity must not be greater than MaxSize" );
    }

    return errors;
  }

  public String getAutoScalingGroupName( ) {
    return autoScalingGroupName;
  }

  public void setAutoScalingGroupName( String autoScalingGroupName ) {
    this.autoScalingGroupName = autoScalingGroupName;
  }

  public String getLaunchConfigurationName( ) {
    return launchConfigurationName;
  }

  public void setLaunchConfigurationName( String launchConfigurationName ) {
    this.launchConfigurationName = launchConfigurationName;
  }

  public Integer getMinSize( ) {
    return minSize;
  }

  public void setMinSize( Integer minSize ) {
    this.minSize = minSize;
  }

  public Integer getMaxSize( ) {
    return maxSize;
  }

  public void setMaxSize( Integer maxSize ) {
    this.maxSize = maxSize;
  }

  public Integer getDesiredCapacity( ) {
    return desiredCapacity;
  }

  public void setDesiredCapacity( Integer desiredCapacity ) {
    this.desiredCapacity = desiredCapacity;
  }

  public Integer getDefaultCooldown( ) {
    return defaultCooldown;
  }

  public void setDefaultCooldown( Integer defaultCooldown ) {
    this.defaultCooldown = defaultCooldown;
  }

  public AvailabilityZones getAvailabilityZones( ) {
    return availabilityZones;
  }

  public void setAvailabilityZones( AvailabilityZones availabilityZones ) {
    this.availabilityZones = availabilityZones;
  }

  public String getHealthCheckType( ) {
    return healthCheckType;
  }

  public void setHealthCheckType( String healthCheckType ) {
    this.healthCheckType = healthCheckType;
  }

  public Integer getHealthCheckGracePeriod( ) {
    return healthCheckGracePeriod;
  }

  public void setHealthCheckGracePeriod( Integer healthCheckGracePeriod ) {
    this.healthCheckGracePeriod = healthCheckGracePeriod;
  }

  public Boolean getNewInstancesProtectedFromScaleIn( ) {
    return newInstancesProtectedFromScaleIn;
  }

  public void setNewInstancesProtectedFromScaleIn( Boolean newInstancesProtectedFromScaleIn ) {
    this.newInstancesProtectedFromScaleIn = newInstancesProtectedFromScaleIn;
  }

  public String getPlacementGroup( ) {
    return placementGroup;
  }

  public void setPlacementGroup( String placementGroup ) {
    this.placementGroup = placementGroup;
  }

  public String getVpcZoneIdentifier( ) {
    return vpcZoneIdentifier;
  }

  public void setVpcZoneIdentifier( String vpcZoneIdentifier ) {
    this.vpcZoneIdentifier = vpcZoneIdentifier;
  }

  public TerminationPolicies getTerminationPolicies( ) {
    return terminationPolicies;
  }

  public void setTerminationPolicies( TerminationPolicies terminationPolicies ) {
    this.terminationPolicies = terminationPolicies;
  }
}
