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
