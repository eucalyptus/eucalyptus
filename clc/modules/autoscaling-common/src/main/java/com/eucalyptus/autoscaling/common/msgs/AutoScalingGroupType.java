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

import java.util.Date;
import com.eucalyptus.util.CompatFunction;
import edu.ucsb.eucalyptus.msgs.EucalyptusData;

public class AutoScalingGroupType extends EucalyptusData {

  private String autoScalingGroupName;
  private String autoScalingGroupARN;
  private String launchConfigurationName;
  private Integer minSize;
  private Integer maxSize;
  private Integer desiredCapacity;
  private Integer defaultCooldown;
  private AvailabilityZones availabilityZones;
  private LoadBalancerNames loadBalancerNames;
  private String healthCheckType;
  private Integer healthCheckGracePeriod;
  private Instances instances;
  private Date createdTime;
  private SuspendedProcesses suspendedProcesses;
  private Boolean newInstancesProtectedFromScaleIn;
  private String placementGroup;
  private String vpcZoneIdentifier;
  private EnabledMetrics enabledMetrics;
  private String status;
  private TagDescriptionList tags;
  private TerminationPolicies terminationPolicies;

  public static CompatFunction<AutoScalingGroupType, String> groupName() {
    return AutoScalingGroupType::getAutoScalingGroupName;
  }

  public String getAutoScalingGroupName( ) {
    return autoScalingGroupName;
  }

  public void setAutoScalingGroupName( String autoScalingGroupName ) {
    this.autoScalingGroupName = autoScalingGroupName;
  }

  public String getAutoScalingGroupARN( ) {
    return autoScalingGroupARN;
  }

  public void setAutoScalingGroupARN( String autoScalingGroupARN ) {
    this.autoScalingGroupARN = autoScalingGroupARN;
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

  public LoadBalancerNames getLoadBalancerNames( ) {
    return loadBalancerNames;
  }

  public void setLoadBalancerNames( LoadBalancerNames loadBalancerNames ) {
    this.loadBalancerNames = loadBalancerNames;
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

  public Instances getInstances( ) {
    return instances;
  }

  public void setInstances( Instances instances ) {
    this.instances = instances;
  }

  public Date getCreatedTime( ) {
    return createdTime;
  }

  public void setCreatedTime( Date createdTime ) {
    this.createdTime = createdTime;
  }

  public SuspendedProcesses getSuspendedProcesses( ) {
    return suspendedProcesses;
  }

  public void setSuspendedProcesses( SuspendedProcesses suspendedProcesses ) {
    this.suspendedProcesses = suspendedProcesses;
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

  public EnabledMetrics getEnabledMetrics( ) {
    return enabledMetrics;
  }

  public void setEnabledMetrics( EnabledMetrics enabledMetrics ) {
    this.enabledMetrics = enabledMetrics;
  }

  public String getStatus( ) {
    return status;
  }

  public void setStatus( String status ) {
    this.status = status;
  }

  public TagDescriptionList getTags( ) {
    return tags;
  }

  public void setTags( TagDescriptionList tags ) {
    this.tags = tags;
  }

  public TerminationPolicies getTerminationPolicies( ) {
    return terminationPolicies;
  }

  public void setTerminationPolicies( TerminationPolicies terminationPolicies ) {
    this.terminationPolicies = terminationPolicies;
  }
}
