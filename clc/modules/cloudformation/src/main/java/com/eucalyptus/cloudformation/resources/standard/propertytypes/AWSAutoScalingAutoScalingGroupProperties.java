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

public class AWSAutoScalingAutoScalingGroupProperties implements ResourceProperties {

  @Property
  private ArrayList<String> availabilityZones = Lists.newArrayList( );

  @Property
  private Integer cooldown;

  @Property
  private Integer desiredCapacity;

  @Property
  private Integer healthCheckGracePeriod;

  @Property
  private String healthCheckType;

  @Property
  private String instanceId;

  @Property
  private String launchConfigurationName;

  @Property
  private ArrayList<String> loadBalancerNames = Lists.newArrayList( );

  @Required
  @Property
  private Integer maxSize;

  @Required
  @Property
  private Integer minSize;

  @Property
  private ArrayList<AutoScalingNotificationConfiguration> notificationConfigurations = Lists.newArrayList( );

  @Property
  private ArrayList<AutoScalingTag> tags = Lists.newArrayList( );

  @Property
  private ArrayList<String> terminationPolicies = Lists.newArrayList( );

  @Property( name = "VPCZoneIdentifier" )
  private ArrayList<String> vpcZoneIdentifier = Lists.newArrayList( );

  public ArrayList<String> getAvailabilityZones( ) {
    return availabilityZones;
  }

  public void setAvailabilityZones( ArrayList<String> availabilityZones ) {
    this.availabilityZones = availabilityZones;
  }

  public Integer getCooldown( ) {
    return cooldown;
  }

  public void setCooldown( Integer cooldown ) {
    this.cooldown = cooldown;
  }

  public Integer getDesiredCapacity( ) {
    return desiredCapacity;
  }

  public void setDesiredCapacity( Integer desiredCapacity ) {
    this.desiredCapacity = desiredCapacity;
  }

  public Integer getHealthCheckGracePeriod( ) {
    return healthCheckGracePeriod;
  }

  public void setHealthCheckGracePeriod( Integer healthCheckGracePeriod ) {
    this.healthCheckGracePeriod = healthCheckGracePeriod;
  }

  public String getHealthCheckType( ) {
    return healthCheckType;
  }

  public void setHealthCheckType( String healthCheckType ) {
    this.healthCheckType = healthCheckType;
  }

  public String getInstanceId( ) {
    return instanceId;
  }

  public void setInstanceId( String instanceId ) {
    this.instanceId = instanceId;
  }

  public String getLaunchConfigurationName( ) {
    return launchConfigurationName;
  }

  public void setLaunchConfigurationName( String launchConfigurationName ) {
    this.launchConfigurationName = launchConfigurationName;
  }

  public ArrayList<String> getLoadBalancerNames( ) {
    return loadBalancerNames;
  }

  public void setLoadBalancerNames( ArrayList<String> loadBalancerNames ) {
    this.loadBalancerNames = loadBalancerNames;
  }

  public Integer getMaxSize( ) {
    return maxSize;
  }

  public void setMaxSize( Integer maxSize ) {
    this.maxSize = maxSize;
  }

  public Integer getMinSize( ) {
    return minSize;
  }

  public void setMinSize( Integer minSize ) {
    this.minSize = minSize;
  }

  public ArrayList<AutoScalingNotificationConfiguration> getNotificationConfigurations( ) {
    return notificationConfigurations;
  }

  public void setNotificationConfigurations( ArrayList<AutoScalingNotificationConfiguration> notificationConfigurations ) {
    this.notificationConfigurations = notificationConfigurations;
  }

  public ArrayList<AutoScalingTag> getTags( ) {
    return tags;
  }

  public void setTags( ArrayList<AutoScalingTag> tags ) {
    this.tags = tags;
  }

  public ArrayList<String> getTerminationPolicies( ) {
    return terminationPolicies;
  }

  public void setTerminationPolicies( ArrayList<String> terminationPolicies ) {
    this.terminationPolicies = terminationPolicies;
  }

  public ArrayList<String> getVpcZoneIdentifier( ) {
    return vpcZoneIdentifier;
  }

  public void setVpcZoneIdentifier( ArrayList<String> vpcZoneIdentifier ) {
    this.vpcZoneIdentifier = vpcZoneIdentifier;
  }

  @Override
  public String toString( ) {
    return MoreObjects.toStringHelper( this )
        .add( "availabilityZones", availabilityZones )
        .add( "cooldown", cooldown )
        .add( "desiredCapacity", desiredCapacity )
        .add( "healthCheckGracePeriod", healthCheckGracePeriod )
        .add( "healthCheckType", healthCheckType )
        .add( "instanceId", instanceId )
        .add( "launchConfigurationName", launchConfigurationName )
        .add( "loadBalancerNames", loadBalancerNames )
        .add( "maxSize", maxSize )
        .add( "minSize", minSize )
        .add( "notificationConfigurations", notificationConfigurations )
        .add( "tags", tags )
        .add( "terminationPolicies", terminationPolicies )
        .add( "vpcZoneIdentifier", vpcZoneIdentifier )
        .toString( );
  }
}
