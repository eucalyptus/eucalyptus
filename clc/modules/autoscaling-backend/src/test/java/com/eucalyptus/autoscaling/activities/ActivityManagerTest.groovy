/*************************************************************************
 * Copyright 2009-2013 Eucalyptus Systems, Inc.
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
 *
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 ************************************************************************/
package com.eucalyptus.autoscaling.activities

import com.eucalyptus.auth.Accounts
import com.eucalyptus.auth.AuthException
import com.eucalyptus.auth.api.AccountProvider
import com.eucalyptus.auth.principal.AccessKey
import com.eucalyptus.auth.principal.Account
import com.eucalyptus.auth.principal.Certificate
import com.eucalyptus.auth.principal.Group
import com.eucalyptus.auth.principal.Principals
import com.eucalyptus.auth.principal.Role
import com.eucalyptus.auth.principal.User
import com.eucalyptus.autoscaling.common.AutoScalingMetadata
import com.eucalyptus.autoscaling.configurations.LaunchConfiguration
import com.eucalyptus.autoscaling.configurations.LaunchConfigurations
import com.eucalyptus.autoscaling.groups.AutoScalingGroup
import com.eucalyptus.autoscaling.groups.AutoScalingGroups
import com.eucalyptus.autoscaling.groups.HealthCheckType
import com.eucalyptus.autoscaling.groups.ScalingProcessType
import com.eucalyptus.autoscaling.groups.TerminationPolicyType
import com.eucalyptus.autoscaling.instances.AutoScalingInstance
import com.eucalyptus.autoscaling.instances.AutoScalingInstances
import com.eucalyptus.autoscaling.instances.ConfigurationState
import com.eucalyptus.autoscaling.instances.HealthStatus
import com.eucalyptus.autoscaling.instances.LifecycleState
import com.eucalyptus.autoscaling.metadata.AutoScalingMetadataNotFoundException
import com.eucalyptus.autoscaling.tags.Tag
import com.eucalyptus.crypto.util.Timestamps
import com.eucalyptus.entities.AbstractOwnedPersistent;
import com.eucalyptus.loadbalancing.common.msgs.DeregisterInstancesFromLoadBalancerResponseType
import com.eucalyptus.loadbalancing.common.msgs.DeregisterInstancesFromLoadBalancerResult
import com.eucalyptus.loadbalancing.common.msgs.DeregisterInstancesFromLoadBalancerType
import com.eucalyptus.loadbalancing.common.msgs.DescribeInstanceHealthResponseType
import com.eucalyptus.loadbalancing.common.msgs.DescribeInstanceHealthResult
import com.eucalyptus.loadbalancing.common.msgs.DescribeInstanceHealthType
import com.eucalyptus.loadbalancing.common.msgs.InstanceState
import com.eucalyptus.loadbalancing.common.msgs.InstanceStates
import com.eucalyptus.loadbalancing.common.msgs.Instances
import com.eucalyptus.loadbalancing.common.msgs.RegisterInstancesWithLoadBalancerResponseType
import com.eucalyptus.loadbalancing.common.msgs.RegisterInstancesWithLoadBalancerResult
import com.eucalyptus.loadbalancing.common.msgs.RegisterInstancesWithLoadBalancerType
import com.eucalyptus.util.Callback
import com.eucalyptus.util.OwnerFullName
import com.eucalyptus.util.TypeMappers
import com.eucalyptus.ws.WebServicesException
import com.google.common.base.Function
import com.google.common.base.Functions
import com.google.common.base.Predicate
import com.google.common.base.Predicates
import com.google.common.base.Strings
import com.google.common.base.Supplier
import com.google.common.base.Suppliers
import com.google.common.collect.Sets

import edu.ucsb.eucalyptus.msgs.CreateTagsType
import edu.ucsb.eucalyptus.msgs.DescribeInstanceStatusResponseType
import edu.ucsb.eucalyptus.msgs.DescribeInstanceStatusType
import edu.ucsb.eucalyptus.msgs.DescribeTagsType
import edu.ucsb.eucalyptus.msgs.InstanceStatusItemType
import edu.ucsb.eucalyptus.msgs.InstanceStatusSetType
import edu.ucsb.eucalyptus.msgs.InstanceStatusType
import edu.ucsb.eucalyptus.msgs.ReservationInfoType
import edu.ucsb.eucalyptus.msgs.RunInstancesResponseType
import edu.ucsb.eucalyptus.msgs.RunInstancesType
import edu.ucsb.eucalyptus.msgs.RunningInstancesItemType
import edu.ucsb.eucalyptus.msgs.TerminateInstancesType
import static org.junit.Assert.*

import org.junit.BeforeClass
import org.junit.Test

import java.lang.reflect.Method
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit

import javax.annotation.Nonnull
import javax.annotation.Nullable

import edu.ucsb.eucalyptus.msgs.InstanceStateType

import com.eucalyptus.auth.principal.Policy

/**
 *
 */
@SuppressWarnings("GroovyAccessibility")
class ActivityManagerTest {

  @BeforeClass
  static void before() {
    TypeMappers.TypeMapperDiscovery discovery = new TypeMappers.TypeMapperDiscovery()
    discovery.processClass( AutoScalingGroups.AutoScalingGroupCoreViewTransform.class )
    discovery.processClass( AutoScalingGroups.AutoScalingGroupMinimumViewTransform.class )
    discovery.processClass( AutoScalingGroups.AutoScalingGroupMetricsViewTransform.class )
    discovery.processClass( AutoScalingGroups.AutoScalingGroupScalingViewTransform.class )
    discovery.processClass( AutoScalingInstances.AutoScalingInstanceCoreViewTransform.class )
    discovery.processClass( AutoScalingInstances.AutoScalingInstanceGroupViewTransform.class )
    discovery.processClass( LaunchConfigurations.LaunchConfigurationCoreViewTransform.class )
    discovery.processClass( LaunchConfigurations.LaunchConfigurationMinimumViewTransform.class )
    discovery.processClass( LaunchConfigurations.LaunchConfigurationToRunInstances.class )
    discovery.processClass( ScalingActivities.ScalingActivityTransform.class )
  }

  @Test
  void testLaunchInstances() {
    Accounts.setAccountProvider( accountProvider() )

    AutoScalingGroup group = new AutoScalingGroup(
        id: "1",
        naturalId: "1",
        availabilityZones: [ "Zone1" ],
        displayName: "Group1",
        launchConfiguration: new LaunchConfiguration(
            id: "1",
            naturalId: "1",
            ownerAccountNumber: "000000000000",
            displayName: "Config1",
            imageId: "emi-00000001",
            instanceType: "m1.small",
        ),
        scalingRequired: true,
        desiredCapacity: 2,
        capacity:  0,
        minSize: 1,
        maxSize: 2,
        ownerAccountNumber: "000000000000",
        version: 1,
    )
    List<AutoScalingInstance> instances = []
    List<ScalingActivity> scalingActivities = []
    ActivityManager manager = activityManager( group, scalingActivities, instances )

    assertEquals( "Group capacity", 0, group.capacity )
    assertEquals( "Instance count", 0, instances.size() )
    assertEquals( "Scaling activity count", 0, scalingActivities.size() )

    doScaling( scalingActivities, manager )

    assertEquals( "Group capacity", 2, group.capacity )
    assertFalse( "Group scaling required", group.scalingRequired )
    assertEquals( "Instance count", 2, instances.size() )
    assertEquals( "Instances 1 id", "i-00000001", instances.get(0).instanceId )
    assertEquals( "Instances 1 az", "Zone1", instances.get(0).availabilityZone )
    assertEquals( "Instances 2 id", "i-00000002", instances.get(1).instanceId )
    assertEquals( "Instances 2 az", "Zone1", instances.get(1).availabilityZone )
    assertEquals( "Scaling activity count", 2, scalingActivities.size() )
    assertEquals( "Scaling activity 1 status", ActivityStatusCode.Successful, scalingActivities.get(0).statusCode )
    assertNotNull( "Scaling activity 1 has end date", scalingActivities.get(0).endTime )
    assertEquals( "Scaling activity 2 status", ActivityStatusCode.Successful, scalingActivities.get(1).statusCode )
    assertNotNull( "Scaling activity 2 has end date", scalingActivities.get(1).endTime )
  }

  @Test
  void testRegisterInstances() {
    Accounts.setAccountProvider( accountProvider() )

    AutoScalingGroup group = new AutoScalingGroup(
        id: "1",
        naturalId: "1",
        availabilityZones: [ "Zone1" ],
        loadBalancerNames: [ "LoadBalancer1", "LoadBalancer2" ],
        displayName: "Group1",
        launchConfiguration: new LaunchConfiguration(
            id: "1",
            naturalId: "1",
            ownerAccountNumber: "000000000000",
            displayName: "Config1",
            imageId: "emi-00000001",
            instanceType: "m1.small",
        ),
        scalingRequired: false,
        desiredCapacity: 2,
        capacity:  2,
        minSize: 1,
        maxSize: 2,
        ownerAccountNumber: "000000000000",
        version: 1,
    )
    List<AutoScalingInstance> instances = [
        new AutoScalingInstance(
            id: "2",
            naturalId: "1",
            uniqueName: "1",
            displayName: "i-00000001",
            ownerAccountNumber: "000000000000",
            availabilityZone: "Zone1",
            healthStatus: HealthStatus.Healthy,
            autoScalingGroup: group,
            autoScalingGroupName: group.autoScalingGroupName,
            launchConfigurationName: "Config1",
            lifecycleState: LifecycleState.InService,
            configurationState: ConfigurationState.Instantiated,
            registrationAttempts: 0,
        ),
        new AutoScalingInstance(
            id: "2",
            naturalId: "2",
            uniqueName: "2",
            displayName: "i-00000002",
            ownerAccountNumber: "000000000000",
            availabilityZone: "Zone1",
            healthStatus: HealthStatus.Healthy,
            autoScalingGroup: group,
            autoScalingGroupName: group.autoScalingGroupName,
            launchConfigurationName: "Config1",
            lifecycleState: LifecycleState.InService,
            configurationState: ConfigurationState.Instantiated,
            registrationAttempts: 0,
        ),
    ]
    List<ScalingActivity> scalingActivities = []
    ActivityManager manager = activityManager( group, scalingActivities, instances )

    assertEquals( "Group capacity", 2, group.capacity )
    assertEquals( "Instance count", 2, instances.size() )
    assertEquals( "Scaling activity count", 0, scalingActivities.size() )

    manager.doScaling()

    assertEquals( "Group capacity", 2, group.capacity )
    assertFalse( "Group scaling required", group.scalingRequired )
    assertEquals( "Instance count", 2, instances.size() )
    assertEquals( "Instances 1 id", "i-00000001", instances.get(0).instanceId )
    assertEquals( "Instances 1 az", "Zone1", instances.get(0).availabilityZone )
    assertEquals( "Instances 1 config state", ConfigurationState.Registered, instances.get(0).configurationState )
    assertEquals( "Instances 2 id", "i-00000002", instances.get(1).instanceId )
    assertEquals( "Instances 2 az", "Zone1", instances.get(1).availabilityZone )
    assertEquals( "Instances 2 config state", ConfigurationState.Registered, instances.get(1).configurationState )
    assertEquals( "Scaling activity count", 2, scalingActivities.size() )
    assertEquals( "Scaling activity 1 status", ActivityStatusCode.Successful, scalingActivities.get(0).statusCode )
    assertNotNull( "Scaling activity 1 has end date", scalingActivities.get(0).endTime )
    assertEquals( "Scaling activity 2 status", ActivityStatusCode.Successful, scalingActivities.get(1).statusCode )
    assertNotNull( "Scaling activity 2 has end date", scalingActivities.get(1).endTime )
  }

  @Test
  void testHealthCheckSuccess() {
    Accounts.setAccountProvider( accountProvider() )

    AutoScalingGroup group = new AutoScalingGroup(
        id: "1",
        naturalId: "1",
        availabilityZones: [ "Zone1" ],
        loadBalancerNames: [ "LoadBalancer1", "LoadBalancer2" ],
        healthCheckType: HealthCheckType.ELB,
        displayName: "Group1",
        launchConfiguration: new LaunchConfiguration(
            id: "1",
            naturalId: "1",
            ownerAccountNumber: "000000000000",
            displayName: "Config1",
            imageId: "emi-00000001",
            instanceType: "m1.small",
        ),
        scalingRequired: false,
        desiredCapacity: 2,
        capacity:  2,
        minSize: 1,
        maxSize: 2,
        ownerAccountNumber: "000000000000",
        version: 1,
    )
    List<AutoScalingInstance> instances = [
        new AutoScalingInstance(
            id: "2",
            naturalId: "1",
            uniqueName: "1",
            displayName: "i-00000001",
            ownerAccountNumber: "000000000000",
            availabilityZone: "Zone1",
            healthStatus: HealthStatus.Healthy,
            autoScalingGroup: group,
            autoScalingGroupName: group.autoScalingGroupName,
            launchConfigurationName: "Config1",
            lifecycleState: LifecycleState.InService,
            configurationState: ConfigurationState.Registered,
            registrationAttempts: 0,
        ),
        new AutoScalingInstance(
            id: "2",
            naturalId: "2",
            uniqueName: "2",
            displayName: "i-00000002",
            ownerAccountNumber: "000000000000",
            availabilityZone: "Zone1",
            healthStatus: HealthStatus.Healthy,
            autoScalingGroup: group,
            autoScalingGroupName: group.autoScalingGroupName,
            launchConfigurationName: "Config1",
            lifecycleState: LifecycleState.InService,
            configurationState: ConfigurationState.Registered,
            registrationAttempts: 0,
        ),
    ]
    List<ScalingActivity> scalingActivities = []
    ActivityManager manager = activityManager( group, scalingActivities, instances, true )

    assertEquals( "Group capacity", 2, group.capacity )
    assertEquals( "Instance count", 2, instances.size() )
    assertEquals( "Scaling activity count", 0, scalingActivities.size() )

    manager.doScaling()

    assertEquals( "Group capacity", 2, group.capacity )
    assertFalse( "Group scaling required", group.scalingRequired )
    assertEquals( "Instance count", 2, instances.size() )
    assertEquals( "Instances 1 id", "i-00000001", instances.get(0).instanceId )
    assertEquals( "Instances 1 health status", HealthStatus.Healthy, instances.get(0).healthStatus )
    assertEquals( "Instances 2 id", "i-00000002", instances.get(1).instanceId )
    assertEquals( "Instances 2 health status", HealthStatus.Healthy, instances.get(1).healthStatus )
    assertEquals( "Scaling activity count", 0, scalingActivities.size() )
  }

  @Test
  void testEC2HealthCheckFailure() {
    Accounts.setAccountProvider( accountProvider() )

    AutoScalingGroup group = new AutoScalingGroup(
        id: "1",
        naturalId: "1",
        availabilityZones: [ "Zone1" ],
        loadBalancerNames: [ "LoadBalancer1", "LoadBalancer2" ],
        healthCheckType: HealthCheckType.EC2,
        displayName: "Group1",
        launchConfiguration: new LaunchConfiguration(
            id: "1",
            naturalId: "1",
            ownerAccountNumber: "000000000000",
            displayName: "Config1",
            imageId: "emi-00000001",
            instanceType: "m1.small",
        ),
        scalingRequired: false,
        desiredCapacity: 2,
        capacity:  2,
        minSize: 1,
        maxSize: 2,
        ownerAccountNumber: "000000000000",
        version: 1,
    )
    List<AutoScalingInstance> instances = [
        new AutoScalingInstance(
            id: "2",
            naturalId: "1",
            uniqueName: "1",
            displayName: "i-00000001",
            ownerAccountNumber: "000000000000",
            availabilityZone: "Zone1",
            healthStatus: HealthStatus.Healthy,
            autoScalingGroup: group,
            autoScalingGroupName: group.autoScalingGroupName,
            launchConfigurationName: "Config1",
            lifecycleState: LifecycleState.InService,
            configurationState: ConfigurationState.Registered,
            registrationAttempts: 0,
        ),
        new AutoScalingInstance(
            id: "2",
            naturalId: "2",
            uniqueName: "2",
            displayName: "i-00000002",
            ownerAccountNumber: "000000000000",
            availabilityZone: "Zone1",
            healthStatus: HealthStatus.Healthy,
            autoScalingGroup: group,
            autoScalingGroupName: group.autoScalingGroupName,
            launchConfigurationName: "Config1",
            lifecycleState: LifecycleState.InService,
            configurationState: ConfigurationState.Registered,
            registrationAttempts: 0,
        ),
    ]
    List<ScalingActivity> scalingActivities = []
    ActivityManager manager = activityManager( group, scalingActivities, instances, true, ["i-00000002"] )

    assertEquals( "Group capacity", 2, group.capacity )
    assertEquals( "Instance count", 2, instances.size() )
    assertEquals( "Scaling activity count", 0, scalingActivities.size() )

    manager.doScaling()

    assertEquals( "Group capacity", 2, group.capacity )
    assertFalse( "Group scaling required", group.scalingRequired )
    assertEquals( "Instance count", 2, instances.size() )
    assertEquals( "Instances 1 id", "i-00000001", instances.get(0).instanceId )
    assertEquals( "Instances 1 health status", HealthStatus.Healthy, instances.get(0).healthStatus )
    assertEquals( "Instances 2 id", "i-00000002", instances.get(1).instanceId )
    assertEquals( "Instances 2 health status", HealthStatus.Unhealthy, instances.get(1).healthStatus )
    assertEquals( "Scaling activity count", 0, scalingActivities.size() )
  }

  @Test
  void testELBHealthCheckFailure() {
    for ( HealthCheckType type : HealthCheckType.values() ) {
      Accounts.setAccountProvider( accountProvider() )

      AutoScalingGroup group = new AutoScalingGroup(
          id: "1",
          naturalId: "1",
          availabilityZones: [ "Zone1" ],
          loadBalancerNames: [ "LoadBalancer1", "LoadBalancer2" ],
          healthCheckType: type,
          displayName: "Group1",
          launchConfiguration: new LaunchConfiguration(
              id: "1",
              naturalId: "1",
              ownerAccountNumber: "000000000000",
              displayName: "Config1",
              imageId: "emi-00000001",
              instanceType: "m1.small",
          ),
          scalingRequired: false,
          desiredCapacity: 2,
          capacity:  2,
          minSize: 1,
          maxSize: 2,
          ownerAccountNumber: "000000000000",
          version: 1,
      )
      List<AutoScalingInstance> instances = [
          new AutoScalingInstance(
              id: "2",
              naturalId: "1",
              uniqueName: "1",
              displayName: "i-00000001",
              ownerAccountNumber: "000000000000",
              availabilityZone: "Zone1",
              healthStatus: HealthStatus.Healthy,
              autoScalingGroup: group,
              autoScalingGroupName: group.autoScalingGroupName,
              launchConfigurationName: "Config1",
              lifecycleState: LifecycleState.InService,
              configurationState: ConfigurationState.Registered,
              registrationAttempts: 0,
          ),
          new AutoScalingInstance(
              id: "2",
              naturalId: "2",
              uniqueName: "2",
              displayName: "i-00000002",
              ownerAccountNumber: "000000000000",
              availabilityZone: "Zone1",
              healthStatus: HealthStatus.Healthy,
              autoScalingGroup: group,
              autoScalingGroupName: group.autoScalingGroupName,
              launchConfigurationName: "Config1",
              lifecycleState: LifecycleState.InService,
              configurationState: ConfigurationState.Registered,
              registrationAttempts: 0,
          ),
      ]
      List<ScalingActivity> scalingActivities = []
      ActivityManager manager = activityManager( group, scalingActivities, instances, true, [], ["i-00000002"] )

      assertEquals( "Group capacity", 2, group.capacity )
      assertEquals( "Instance count", 2, instances.size() )
      assertEquals( "Scaling activity count", 0, scalingActivities.size() )

      manager.doScaling()

      assertEquals( "Group capacity", 2, group.capacity )
      assertFalse( "Group scaling required", group.scalingRequired )
      assertEquals( "Instance count", 2, instances.size() )
      assertEquals( "Instances 1 id", "i-00000001", instances.get(0).instanceId )
      assertEquals( "Instances 1 health status", HealthStatus.Healthy, instances.get(0).healthStatus )
      assertEquals( "Instances 2 id", "i-00000002", instances.get(1).instanceId )
      assertEquals( "Instances 2 health status", type==HealthCheckType.ELB ? HealthStatus.Unhealthy : HealthStatus.Healthy, instances.get(1).healthStatus )
      assertEquals( "Scaling activity count", 0, scalingActivities.size() )
    }
  }

  @Test
  void testTerminateInstances() {
    Accounts.setAccountProvider( accountProvider() )

    AutoScalingGroup group = new AutoScalingGroup(
        id: "1",
        naturalId: "1",
        availabilityZones: [ "Zone1" ],
        displayName: "Group1",
        launchConfiguration: new LaunchConfiguration(
            id: "1",
            naturalId: "1",
            ownerAccountNumber: "000000000000",
            displayName: "Config1",
            imageId: "emi-00000001",
            instanceType: "m1.small",
        ),
        scalingRequired: true,
        desiredCapacity: 0,
        capacity:  2,
        minSize: 0,
        maxSize: 2,
        ownerAccountNumber: "000000000000",
        version: 1,
    )
    List<AutoScalingInstance> instances = [
        new AutoScalingInstance(
            id: "1",
            naturalId: "1",
            uniqueName: "1",
            ownerAccountNumber: "000000000000",
            availabilityZone: "Zone1",
            healthStatus: HealthStatus.Healthy,
            autoScalingGroup: group,
            autoScalingGroupName: group.autoScalingGroupName,
            launchConfigurationName: "Config1",
            lifecycleState: LifecycleState.InService,
            configurationState: ConfigurationState.Registered,
        ),
        new AutoScalingInstance(
            id: "2",
            naturalId: "2",
            uniqueName: "2",
            ownerAccountNumber: "000000000000",
            availabilityZone: "Zone1",
            healthStatus: HealthStatus.Healthy,
            autoScalingGroup: group,
            autoScalingGroupName: group.autoScalingGroupName,
            launchConfigurationName: "Config1",
            lifecycleState: LifecycleState.InService,
            configurationState: ConfigurationState.Registered,
        ),
    ]
    List<ScalingActivity> scalingActivities = []
    ActivityManager manager = activityManager( group, scalingActivities, instances )

    assertEquals( "Group capacity", 2, group.capacity )
    assertEquals( "Instance count", 2, instances.size() )
    assertEquals( "Scaling activity count", 0, scalingActivities.size() )

    doScaling( scalingActivities, manager )

    assertEquals( "Group capacity", 0, group.capacity )
    assertFalse( "Group scaling required", group.scalingRequired )
    assertEquals( "Instance count", 0, instances.size() )
    assertEquals( "Scaling activity count", 2, scalingActivities.size() )
    assertEquals( "Scaling activity 1 status", ActivityStatusCode.Successful, scalingActivities.get(0).statusCode )
    assertNotNull( "Scaling activity 1 has end date", scalingActivities.get(0).endTime )
    assertEquals( "Scaling activity 2 status", ActivityStatusCode.Successful, scalingActivities.get(1).statusCode )
    assertNotNull( "Scaling activity 2 has end date", scalingActivities.get(1).endTime )
  }

  @Test
  void testTerminateRegisteredInstances() {
    Accounts.setAccountProvider( accountProvider() )

    AutoScalingGroup group = new AutoScalingGroup(
        id: "1",
        naturalId: "1",
        availabilityZones: [ "Zone1" ],
        loadBalancerNames: [ "LoadBalancer1", "LoadBalancer2" ],
        displayName: "Group1",
        launchConfiguration: new LaunchConfiguration(
            id: "1",
            naturalId: "1",
            ownerAccountNumber: "000000000000",
            displayName: "Config1",
            imageId: "emi-00000001",
            instanceType: "m1.small",
        ),
        scalingRequired: true,
        desiredCapacity: 0,
        capacity:  2,
        minSize: 0,
        maxSize: 2,
        ownerAccountNumber: "000000000000",
        version: 1,
    )
    List<AutoScalingInstance> instances = [
        new AutoScalingInstance(
            id: "1",
            naturalId: "1",
            uniqueName: "1",
            ownerAccountNumber: "000000000000",
            availabilityZone: "Zone1",
            healthStatus: HealthStatus.Healthy,
            autoScalingGroup: group,
            autoScalingGroupName: group.autoScalingGroupName,
            launchConfigurationName: "Config1",
            lifecycleState: LifecycleState.InService,
            configurationState: ConfigurationState.Registered,
        ),
        new AutoScalingInstance(
            id: "2",
            naturalId: "2",
            uniqueName: "2",
            ownerAccountNumber: "000000000000",
            availabilityZone: "Zone1",
            healthStatus: HealthStatus.Healthy,
            autoScalingGroup: group,
            autoScalingGroupName: group.autoScalingGroupName,
            launchConfigurationName: "Config1",
            lifecycleState: LifecycleState.InService,
            configurationState: ConfigurationState.Registered,
        ),
    ]
    List<ScalingActivity> scalingActivities = []
    ActivityManager manager = activityManager( group, scalingActivities, instances )

    assertEquals( "Group capacity", 2, group.capacity )
    assertEquals( "Instance count", 2, instances.size() )
    assertEquals( "Scaling activity count", 0, scalingActivities.size() )

    doScaling( scalingActivities, manager )

    assertEquals( "Group capacity", 0, group.capacity )
    assertFalse( "Group scaling required", group.scalingRequired )
    assertEquals( "Instance count", 0, instances.size() )
    assertEquals( "Scaling activity count", 4, scalingActivities.size() )
    assertEquals( "Scaling activity 1 status", ActivityStatusCode.Successful, scalingActivities.get(0).statusCode )
    assertNotNull( "Scaling activity 1 has end date", scalingActivities.get(0).endTime )
    assertEquals( "Scaling activity 2 status", ActivityStatusCode.Successful, scalingActivities.get(1).statusCode )
    assertNotNull( "Scaling activity 2 has end date", scalingActivities.get(1).endTime )
    assertEquals( "Scaling activity 3 status", ActivityStatusCode.Successful, scalingActivities.get(2).statusCode )
    assertNotNull( "Scaling activity 3 has end date", scalingActivities.get(2).endTime )
    assertEquals( "Scaling activity 4 status", ActivityStatusCode.Successful, scalingActivities.get(3).statusCode )
    assertNotNull( "Scaling activity 4 has end date", scalingActivities.get(3).endTime )
  }

  @Test
  void testLaunchInstancesMultipleAvailabilityZones() {
    Accounts.setAccountProvider( accountProvider() )

    AutoScalingGroup group = new AutoScalingGroup(
        id: "1",
        naturalId: "1",
        availabilityZones: [ "Zone1", "Zone2", "Zone3", "Zone4" ],
        displayName: "Group1",
        launchConfiguration: new LaunchConfiguration(
            id: "1",
            naturalId: "1",
            ownerAccountNumber: "000000000000",
            displayName: "Config1",
            imageId: "emi-00000001",
            instanceType: "m1.small",
        ),
        scalingRequired: true,
        desiredCapacity: 8,
        capacity:  0,
        minSize: 8,
        maxSize: 8,
        ownerAccountNumber: "000000000000",
        version: 1,
    )
    List<AutoScalingInstance> instances = []
    List<ScalingActivity> scalingActivities = []
    ActivityManager manager = activityManager( group, scalingActivities, instances )

    assertEquals( "Group capacity", 0, group.capacity )
    assertEquals( "Instance count", 0, instances.size() )
    assertEquals( "Scaling activity count", 0, scalingActivities.size() )

    doScaling( scalingActivities, manager )

    assertEquals( "Group capacity", 8, group.capacity )
    assertFalse( "Group scaling required", group.scalingRequired )
    assertEquals( "Instance count", 8, instances.size() )
    assertEquals( "Instances 1 id", "i-00000001", instances.get(0).instanceId )
    assertEquals( "Instances 1 az", "Zone1", instances.get(0).availabilityZone )
    assertEquals( "Instances 2 id", "i-00000002", instances.get(1).instanceId )
    assertEquals( "Instances 2 az", "Zone2", instances.get(1).availabilityZone )
    assertEquals( "Instances 3 id", "i-00000003", instances.get(2).instanceId )
    assertEquals( "Instances 3 az", "Zone3", instances.get(2).availabilityZone )
    assertEquals( "Instances 4 id", "i-00000004", instances.get(3).instanceId )
    assertEquals( "Instances 4 az", "Zone4", instances.get(3).availabilityZone )
    assertEquals( "Instances 5 id", "i-00000005", instances.get(4).instanceId )
    assertEquals( "Instances 5 az", "Zone1", instances.get(4).availabilityZone )
    assertEquals( "Instances 6 id", "i-00000006", instances.get(5).instanceId )
    assertEquals( "Instances 6 az", "Zone2", instances.get(5).availabilityZone )
    assertEquals( "Instances 7 id", "i-00000007", instances.get(6).instanceId )
    assertEquals( "Instances 7 az", "Zone3", instances.get(6).availabilityZone )
    assertEquals( "Instances 8 id", "i-00000008", instances.get(7).instanceId )
    assertEquals( "Instances 8 az", "Zone4", instances.get(7).availabilityZone )
    assertEquals( "Scaling activity count", 8, scalingActivities.size() )
    for ( int i=0; i<8; i++ ) {
      assertEquals( "Scaling activity "+(i+1)+" status", ActivityStatusCode.Successful, scalingActivities.get(i).statusCode )
      assertNotNull( "Scaling activity "+(i+1)+" has end date", scalingActivities.get(i).endTime )
    }
  }

  @Test
  void testLaunchInstancesMultipleAvailabilityZonesSkipsUnavailable() {
    Accounts.setAccountProvider( accountProvider() )

    AutoScalingGroup group = new AutoScalingGroup(
        id: "1",
        naturalId: "1",
        availabilityZones: [ "Zone1", "Zone2", "Zone3", "Zone4" ],
        displayName: "Group1",
        launchConfiguration: new LaunchConfiguration(
            id: "1",
            naturalId: "1",
            ownerAccountNumber: "000000000000",
            displayName: "Config1",
            imageId: "emi-00000001",
            instanceType: "m1.small",
        ),
        scalingRequired: true,
        desiredCapacity: 6,
        capacity:  0,
        minSize: 6,
        maxSize: 6,
        ownerAccountNumber: "000000000000",
        version: 1,
    )
    List<AutoScalingInstance> instances = []
    List<ScalingActivity> scalingActivities = []
    ActivityManager manager = activityManager( group, scalingActivities, instances, false, [], [], [ "Zone1" ] )

    assertEquals( "Group capacity", 0, group.capacity )
    assertEquals( "Instance count", 0, instances.size() )
    assertEquals( "Scaling activity count", 0, scalingActivities.size() )

    doScaling( scalingActivities, manager )

    assertEquals( "Group capacity", 6, group.capacity )
    assertFalse( "Group scaling required", group.scalingRequired )
    assertEquals( "Instance count", 6, instances.size() )
    assertEquals( "Instances 1 id", "i-00000001", instances.get(0).instanceId )
    assertEquals( "Instances 1 az", "Zone2", instances.get(0).availabilityZone )
    assertEquals( "Instances 2 id", "i-00000002", instances.get(1).instanceId )
    assertEquals( "Instances 2 az", "Zone3", instances.get(1).availabilityZone )
    assertEquals( "Instances 3 id", "i-00000003", instances.get(2).instanceId )
    assertEquals( "Instances 3 az", "Zone4", instances.get(2).availabilityZone )
    assertEquals( "Instances 4 id", "i-00000004", instances.get(3).instanceId )
    assertEquals( "Instances 4 az", "Zone2", instances.get(3).availabilityZone )
    assertEquals( "Instances 5 id", "i-00000005", instances.get(4).instanceId )
    assertEquals( "Instances 5 az", "Zone3", instances.get(4).availabilityZone )
    assertEquals( "Instances 6 id", "i-00000006", instances.get(5).instanceId )
    assertEquals( "Instances 6 az", "Zone4", instances.get(5).availabilityZone )
    assertEquals( "Scaling activity count", 6, scalingActivities.size() )
    for ( int i=0; i<6; i++ ) {
      assertEquals( "Scaling activity "+(i+1)+" status", ActivityStatusCode.Successful, scalingActivities.get(i).statusCode )
      assertNotNull( "Scaling activity "+(i+1)+" has end date", scalingActivities.get(i).endTime )
    }
  }

  @Test
  void testAvailabilityZoneFailover() {
    Accounts.setAccountProvider( accountProvider() )

    AutoScalingGroup group = new AutoScalingGroup(
        id: "1",
        naturalId: "1",
        availabilityZones: [ "Zone1", "Zone2" ],
        displayName: "Group1",
        launchConfiguration: new LaunchConfiguration(
            id: "1",
            naturalId: "1",
            ownerAccountNumber: "000000000000",
            displayName: "Config1",
            imageId: "emi-00000001",
            instanceType: "m1.small",
        ),
        scalingRequired: false,
        desiredCapacity: 4,
        capacity: 4,
        minSize: 0,
        maxSize: 4,
        ownerAccountNumber: "000000000000",
        terminationPolicies: [ TerminationPolicyType.OldestInstance ],
        version: 1,
    )
    List<AutoScalingInstance> instances = [
        instance( 101, group, "Zone1" ),
        instance( 102, group, "Zone1" ),
        instance( 103, group, "Zone2" ),
        instance( 104, group, "Zone2" ),
    ]
    List<ScalingActivity> scalingActivities = []
    List<String> failedZones = [ "Zone1" ]
    ActivityManager manager = activityManager( group, scalingActivities, instances, true, [], [], failedZones )

    assertEquals( "Group capacity", 4, group.capacity )
    assertEquals( "Instance count", 4, instances.size() )
    assertEquals( "Scaling activity count", 0, scalingActivities.size() )

    // Should fail over to Zone2
    doScaling( scalingActivities, manager )

    assertEquals( "Group capacity", 4, group.capacity )
    assertFalse( "Group scaling required", group.scalingRequired )
    assertEquals( "Instance count", 4, instances.size() )
    assertEquals( "Instances 1 id", "i-00000103", instances.get(0).instanceId )
    assertEquals( "Instances 1 az", "Zone2", instances.get(0).availabilityZone )
    assertEquals( "Instances 2 id", "i-00000104", instances.get(1).instanceId )
    assertEquals( "Instances 2 az", "Zone2", instances.get(1).availabilityZone )
    assertEquals( "Instances 3 id", "i-00000001", instances.get(2).instanceId )
    assertEquals( "Instances 3 az", "Zone2", instances.get(2).availabilityZone )
    assertEquals( "Instances 4 id", "i-00000002", instances.get(3).instanceId )
    assertEquals( "Instances 4 az", "Zone2", instances.get(3).availabilityZone )
    assertEquals( "Scaling activity count", 4, scalingActivities.size() )
    for ( int i=0; i<4; i++ ) {
      assertEquals( "Scaling activity "+(i+1)+" status", ActivityStatusCode.Successful, scalingActivities.get(i).statusCode )
      assertNotNull( "Scaling activity "+(i+1)+" has end date", scalingActivities.get(i).endTime )
    }

    failedZones.clear()
    failedZones.add( "Zone2" )

    // Should fail over to Zone1
    doScaling( scalingActivities, manager )

    assertEquals( "Group capacity", 4, group.capacity )
    assertFalse( "Group scaling required", group.scalingRequired )
    assertEquals( "Instances 1 id", "i-00000003", instances.get(0).instanceId )
    assertEquals( "Instances 1 az", "Zone1", instances.get(0).availabilityZone )
    assertEquals( "Instances 2 id", "i-00000004", instances.get(1).instanceId )
    assertEquals( "Instances 2 az", "Zone1", instances.get(1).availabilityZone )
    assertEquals( "Instances 3 id", "i-00000005", instances.get(2).instanceId )
    assertEquals( "Instances 3 az", "Zone1", instances.get(2).availabilityZone )
    assertEquals( "Instances 4 id", "i-00000006", instances.get(3).instanceId )
    assertEquals( "Instances 4 az", "Zone1", instances.get(3).availabilityZone )
    assertEquals( "Scaling activity count", 12, scalingActivities.size() )
    for ( int i=0; i<12; i++ ) {
      assertEquals( "Scaling activity "+(i+1)+" status", ActivityStatusCode.Successful, scalingActivities.get(i).statusCode )
      assertNotNull( "Scaling activity "+(i+1)+" has end date", scalingActivities.get(i).endTime )
    }

    failedZones.clear()

    // Should use both Zone1 and Zone2
    doScaling( scalingActivities, manager )

    assertEquals( "Group capacity", 4, group.capacity )
    assertFalse( "Group scaling required", group.scalingRequired )
    assertEquals( "Instances 1 id", "i-00000005", instances.get(0).instanceId )
    assertEquals( "Instances 1 az", "Zone1", instances.get(0).availabilityZone )
    assertEquals( "Instances 2 id", "i-00000006", instances.get(1).instanceId )
    assertEquals( "Instances 2 az", "Zone1", instances.get(1).availabilityZone )
    assertEquals( "Instances 3 id", "i-00000007", instances.get(2).instanceId )
    assertEquals( "Instances 3 az", "Zone2", instances.get(2).availabilityZone )
    assertEquals( "Instances 4 id", "i-00000008", instances.get(3).instanceId )
    assertEquals( "Instances 4 az", "Zone2", instances.get(3).availabilityZone )
    assertEquals( "Scaling activity count", 16, scalingActivities.size() )
    for ( int i=0; i<16; i++ ) {
      assertEquals( "Scaling activity "+(i+1)+" status", ActivityStatusCode.Successful, scalingActivities.get(i).statusCode )
      assertNotNull( "Scaling activity "+(i+1)+" has end date", scalingActivities.get(i).endTime )
    }
  }

  @Test
  void testTerminateInstancesMultipleAvailabilityZones() {
    Accounts.setAccountProvider( accountProvider() )

    AutoScalingGroup group = new AutoScalingGroup(
        id: "1",
        naturalId: "1",
        availabilityZones: [ "Zone1", "Zone2", "Zone3" ],
        displayName: "Group1",
        launchConfiguration: new LaunchConfiguration(
            id: "1",
            naturalId: "1",
            ownerAccountNumber: "000000000000",
            displayName: "Config1",
            imageId: "emi-00000001",
            instanceType: "m1.small",
        ),
        scalingRequired: true,
        desiredCapacity: 3,
        capacity:  5,
        minSize: 0,
        maxSize: 6,
        ownerAccountNumber: "000000000000",
        version: 1,
    )
    List<AutoScalingInstance> instances = [
        new AutoScalingInstance(
            id: "1",
            naturalId: "1",
            uniqueName: "1",
            displayName: "i-00000001",
            ownerAccountNumber: "000000000000",
            creationTimestamp: timestamp("2012-02-10T12:01:00.000Z"),
            availabilityZone: "Zone3",
            healthStatus: HealthStatus.Healthy,
            autoScalingGroup: group,
            autoScalingGroupName: group.autoScalingGroupName,
            launchConfigurationName: "Config1",
            lifecycleState: LifecycleState.InService,
            configurationState: ConfigurationState.Registered,
        ),
        new AutoScalingInstance(
            id: "2",
            naturalId: "2",
            uniqueName: "2",
            displayName: "i-00000002",
            ownerAccountNumber: "000000000000",
            creationTimestamp: timestamp("2012-02-10T12:02:00.000Z"),
            availabilityZone: "Zone1",
            healthStatus: HealthStatus.Healthy,
            autoScalingGroup: group,
            autoScalingGroupName: group.autoScalingGroupName,
            launchConfigurationName: "Config1",
            lifecycleState: LifecycleState.InService,
            configurationState: ConfigurationState.Registered,
        ),
        new AutoScalingInstance(
            id: "3",
            naturalId: "3",
            uniqueName: "3",
            displayName: "i-00000003",
            ownerAccountNumber: "000000000000",
            creationTimestamp: timestamp("2012-02-10T12:03:00.000Z"),
            availabilityZone: "Zone3",
            healthStatus: HealthStatus.Healthy,
            autoScalingGroup: group,
            autoScalingGroupName: group.autoScalingGroupName,
            launchConfigurationName: "Config1",
            lifecycleState: LifecycleState.InService,
            configurationState: ConfigurationState.Registered,
        ),
        new AutoScalingInstance(
            id: "4",
            naturalId: "4",
            uniqueName: "4",
            displayName: "i-00000004",
            ownerAccountNumber: "000000000000",
            creationTimestamp: timestamp("2012-02-10T12:04:00.000Z"),
            availabilityZone: "Zone2",
            healthStatus: HealthStatus.Healthy,
            autoScalingGroup: group,
            autoScalingGroupName: group.autoScalingGroupName,
            launchConfigurationName: "Config1",
            lifecycleState: LifecycleState.InService,
            configurationState: ConfigurationState.Registered,
        ),
        new AutoScalingInstance(
            id: "5",
            naturalId: "5",
            uniqueName: "5",
            displayName: "i-00000005",
            ownerAccountNumber: "000000000000",
            creationTimestamp: timestamp("2012-02-10T12:05:00.000Z"),
            availabilityZone: "Zone2",
            healthStatus: HealthStatus.Healthy,
            autoScalingGroup: group,
            autoScalingGroupName: group.autoScalingGroupName,
            launchConfigurationName: "Config1",
            lifecycleState: LifecycleState.InService,
            configurationState: ConfigurationState.Registered,
        ),
    ]
    List<ScalingActivity> scalingActivities = []
    ActivityManager manager = activityManager( group, scalingActivities, instances )

    assertEquals( "Group capacity", 5, group.capacity )
    assertEquals( "Instance count", 5, instances.size() )
    assertEquals( "Scaling activity count", 0, scalingActivities.size() )

    doScaling( scalingActivities, manager )

    assertEquals( "Group capacity", 3, group.capacity )
    assertFalse( "Group scaling required", group.scalingRequired )
    assertEquals( "Instance count", 3, instances.size() )
    assertEquals( "Instances 1 id", "i-00000001", instances.get(0).instanceId )
    assertEquals( "Instances 1 az", "Zone3", instances.get(0).availabilityZone )
    assertEquals( "Instances 2 id", "i-00000002", instances.get(1).instanceId )
    assertEquals( "Instances 2 az", "Zone1", instances.get(1).availabilityZone )
    assertEquals( "Instances 3 id", "i-00000004", instances.get(2).instanceId )
    assertEquals( "Instances 3 az", "Zone2", instances.get(2).availabilityZone )
    assertEquals( "Scaling activity count", 2, scalingActivities.size() )
    assertEquals( "Scaling activity 1 status", ActivityStatusCode.Successful, scalingActivities.get(0).statusCode )
    assertNotNull( "Scaling activity 1 has end date", scalingActivities.get(0).endTime )
    assertEquals( "Scaling activity 2 status", ActivityStatusCode.Successful, scalingActivities.get(1).statusCode )
    assertNotNull( "Scaling activity 2 has end date", scalingActivities.get(1).endTime )
  }

  @Test
  void testTerminateFromUnwantedAvailabilityZones() {
    Accounts.setAccountProvider( accountProvider() )

    AutoScalingGroup group = new AutoScalingGroup(
        id: "1",
        naturalId: "1",
        availabilityZones: [ "Zone1" ],
        displayName: "Group1",
        launchConfiguration: new LaunchConfiguration(
            id: "1",
            naturalId: "1",
            ownerAccountNumber: "000000000000",
            displayName: "Config1",
            imageId: "emi-00000001",
            instanceType: "m1.small",
        ),
        scalingRequired: true,
        desiredCapacity: 2,
        capacity:  3,
        minSize: 0,
        maxSize: 3,
        ownerAccountNumber: "000000000000",
        version: 1,
    )
    List<AutoScalingInstance> instances = [
        instance( 101, group, "Zone1", HealthStatus.Healthy, LifecycleState.InService, ConfigurationState.Registered ),
        instance( 102, group, "Zone2", HealthStatus.Healthy, LifecycleState.InService, ConfigurationState.Registered ),
        instance( 103, group, "Zone2", HealthStatus.Healthy, LifecycleState.InService, ConfigurationState.Registered ),
    ]
    List<ScalingActivity> scalingActivities = []
    ActivityManager manager = activityManager( group, scalingActivities, instances, true )

    assertEquals( "Group capacity", 3, group.capacity )
    assertEquals( "Instance count", 3, instances.size() )
    assertEquals( "Scaling activity count", 0, scalingActivities.size() )

    doScaling( scalingActivities, manager )

    assertEquals( "Group capacity", 2, group.capacity )
    assertFalse( "Group scaling required", group.scalingRequired )
    assertEquals( "Instance count", 2, instances.size() )
    assertEquals( "Instances 1 id", "i-00000101", instances.get(0).instanceId )
    assertEquals( "Instances 1 az", "Zone1", instances.get(0).availabilityZone )
    assertEquals( "Instances 2 id", "i-00000001", instances.get(1).instanceId )
    assertEquals( "Instances 2 az", "Zone1", instances.get(1).availabilityZone )
    assertEquals( "Scaling activity count", 3, scalingActivities.size() )
    for ( int i=0; i<3; i++ ) {
      assertEquals( "Scaling activity "+(i+1)+" status", ActivityStatusCode.Successful, scalingActivities.get(i).statusCode )
      assertNotNull( "Scaling activity "+(i+1)+" has end date", scalingActivities.get(i).endTime )
    }
  }

  @Test
  void testAdministrativeSuspension() {
    Accounts.setAccountProvider( accountProvider() )

    AutoScalingGroup group = new AutoScalingGroup(
        id: "1",
        naturalId: "1",
        availabilityZones: [ "Zone1" ],
        displayName: "Group1",
        launchConfiguration: new LaunchConfiguration(
            id: "1",
            naturalId: "1",
            ownerAccountNumber: "000000000000",
            displayName: "Config1",
            imageId: "emi-00000000",
            instanceType: "m1.small",
        ),
        scalingRequired: true,
        desiredCapacity: 1,
        capacity:  0,
        minSize: 0,
        maxSize: 3,
        creationTimestamp: timestamp( "2000-01-01T00:00:00.000" ),
        lastUpdateTimestamp: timestamp( "2000-01-01T00:00:00.000" ),
        ownerAccountNumber: "000000000000",
        version: 1,
    )
    List<AutoScalingInstance> instances = []
    List<ScalingActivity> scalingActivities = []
    ActivityManager manager = activityManager( group, scalingActivities, instances, false )

    assertEquals( "Group capacity", 0, group.capacity )
    assertEquals( "Instance count", 0, instances.size() )
    assertEquals( "Scaling activity count", 0, scalingActivities.size() )

    doScaling( scalingActivities, manager )

    assertEquals( "Group capacity", 0, group.capacity )
    assertTrue( "Group scaling required", group.scalingRequired )
    assertEquals( "Group suspended processes size", 1, group.suspendedProcesses.size() )
    assertEquals( "Group suspended processes", ScalingProcessType.Launch, group.suspendedProcesses.iterator().next().getScalingProcessType() )
    assertEquals( "Instance count", 0, instances.size() )
    assertEquals( "Scaling activity count", 15, scalingActivities.size() )
    for ( int i=0; i<15; i++ ) {
      assertEquals( "Scaling activity "+(i+1)+" status", ActivityStatusCode.Failed, scalingActivities.get(i).statusCode )
      assertNotNull( "Scaling activity "+(i+1)+" has end date", scalingActivities.get(i).endTime )
    }
  }

  Date timestamp( String text ) {
    Timestamps.parseIso8601Timestamp( text )
  }

  AutoScalingInstance instance( int id,
                                AutoScalingGroup group,
                                String availabilityZone,
                                HealthStatus healthStatus = HealthStatus.Healthy,
                                LifecycleState lifecycleState = LifecycleState.InService,
                                ConfigurationState configurationState = ConfigurationState.Registered ) {
    new AutoScalingInstance(
        id: String.valueOf( id ),
        version: 1,
        naturalId: String.valueOf( id ),
        uniqueName: String.valueOf( id ),
        displayName: "i-" + Strings.padStart( String.valueOf( id ), 8, '0' as char ),
        ownerAccountNumber: "000000000000",
        availabilityZone: availabilityZone,
        healthStatus: healthStatus,
        autoScalingGroup: group,
        autoScalingGroupName: group.autoScalingGroupName,
        launchConfigurationName: "Config1",
        lifecycleState: lifecycleState,
        configurationState: configurationState,
        creationTimestamp: new Date(),
        lastUpdateTimestamp: new Date()
    )
  }

  private void doScaling( List<ScalingActivity> scalingActivities,
                          ActivityManager manager) {
    int count = 0
    int activityCount = -1
    while ( activityCount != scalingActivities.size() && count < 100 ) {
      activityCount = scalingActivities.size()
      manager.doScaling()
      count++
    }
  }

  private ActivityManager activityManager( AutoScalingGroup group,
                                           List<ScalingActivity> scalingActivities,
                                           List<AutoScalingInstance> instances,
                                           boolean healthChecks = false,
                                           List<String> unhealthyInstanceIds = [],
                                           List<String> unhealthyElbInstanceIds = [],
                                           List<String> unavailableZones = [] ) {
    ActivityManager manager = new ActivityManager(
        autoScalingActivitiesStore(scalingActivities),
        autoScalingGroupStore([group],healthChecks),
        autoScalingInstanceStore(instances),
        zoneAvailabilityMarkers(),
        zoneMonitor(unavailableZones)
    ) {
      long timeOffset = 0
      int instanceCount = 0
      BackoffRunner runner = new BackoffRunner() {
        @Override
        protected long timestamp() {
          testTimestamp()
        }
      }

      @Override
      protected long timestamp() {
        testTimestamp()
      }

      long testTimestamp() {
        System.currentTimeMillis() + timeOffset
      }

      @Override
      void doScaling() {
        super.doScaling()
        timeOffset += TimeUnit.MINUTES.toMillis( 15 ) // ff time a bit
      }

      @Override
      void runTask(ActivityManager.ScalingProcessTask task) {
        runner.runTask( task )
      }

      @Override
      boolean taskInProgress(String groupArn) {
        false
      }

      @Override
      EucalyptusClient createEucalyptusClientForUser(String userId) {
        new TestClients.TestEucalyptusClient( userId, { request ->
          if (request instanceof RunInstancesType) {
            if ( "emi-00000000".equals( request.imageId ) )
                throw new WebServicesException( "Test error triggered by using emi-00000000" )
            new RunInstancesResponseType(
                  rsvInfo: new ReservationInfoType(
                      instancesSet: [
                          new RunningInstancesItemType(
                              instanceId: "i-0000000" + (++instanceCount),
                              placement: ((RunInstancesType) request).availabilityZone,
                          )
                      ]
                  )
              )
          } else if ( request instanceof DescribeInstanceStatusType ) {
            new DescribeInstanceStatusResponseType(
              instanceStatusSet: new InstanceStatusSetType(
                  item: request.instancesSet.collect { instanceId ->
                    new InstanceStatusItemType(
                        instanceId: instanceId,
                        instanceState: new InstanceStateType( code: 16, name: "running" ),
                        instanceStatus: new InstanceStatusType( status: unhealthyInstanceIds.contains( instanceId ) ? "impaired" : "ok" ),
                        systemStatus: new InstanceStatusType( status: "ok" ),
                    )
                  },
              )
            )
          } else if ( request instanceof CreateTagsType ||
              request instanceof TerminateInstancesType ||
              request instanceof DescribeTagsType ) {
                request.reply
          } else {
            throw new RuntimeException("Unknown request type: " + request.getClass())
          }
        } as TestClients.RequestHandler )
      }

      @Override
      ElbClient createElbClientForUser(final String userId) {
        new TestClients.TestElbClient( userId, { request ->
          if (request instanceof RegisterInstancesWithLoadBalancerType ) {
            new RegisterInstancesWithLoadBalancerResponseType(
                registerInstancesWithLoadBalancerResult: new RegisterInstancesWithLoadBalancerResult(
                    instances: new Instances(
                        member: request.instances.member
                    )
                )
            )
          } else if ( request instanceof DeregisterInstancesFromLoadBalancerType ) {
            new DeregisterInstancesFromLoadBalancerResponseType(
                deregisterInstancesFromLoadBalancerResult: new DeregisterInstancesFromLoadBalancerResult(
                    instances: new Instances(
                        member: request.instances.member
                    )
                )
            )
          } else if ( request instanceof DescribeInstanceHealthType ) {
            new DescribeInstanceHealthResponseType(
                describeInstanceHealthResult: new DescribeInstanceHealthResult(
                    instanceStates: new InstanceStates(
                        member: unhealthyElbInstanceIds.collect{ instanceId ->
                          new InstanceState(
                            instanceId: instanceId,
                            state: "OutOfService",
                          )
                        }
                    )
                )
            )
          } else {
            throw new RuntimeException("Unknown request type: " + request.getClass())
          }
        } as TestClients.RequestHandler )
      }

      @Override
      Supplier<String> userIdSupplier(String accountNumber) {
        Suppliers.ofInstance(accountNumber)
      }

      @Override
      List<Tag> getTags(AutoScalingMetadata.AutoScalingGroupMetadata autoScalingGroup) {
        []
      }
    }
    manager
  }

  AccountProvider accountProvider() {
    new AccountProvider() {
      @Override
      Account lookupAccountByName(final String accountName) {
        throw new UnsupportedOperationException()
      }

      @Override
      Account lookupAccountById(final String accountId) {
        Principals.systemAccount( )
      }

      @Override
      Account addAccount(final String accountName) {
        throw new UnsupportedOperationException()
      }

      @Override
      void deleteAccount(final String accountName, final boolean forceDeleteSystem, final boolean recursive) {
        throw new UnsupportedOperationException()
      }

      @Override
      int countAccounts() {
        1
      }

      @Override
      int countUsers() {
        1
      }

      @Override
      int countGroups() {
        0
      }

      @Override
      List<Account> listAllAccounts() {
        throw new UnsupportedOperationException()
      }

      @Override
      List<Account> listAccountsByStatus(final User.RegistrationStatus status) {
        throw new UnsupportedOperationException()
      }

      @Override
      Set<String> resolveAccountNumbersForName(final String accountNAmeLike) {
        [] as Set
      }

      @Override
      List<User> listAllUsers() {
        throw new UnsupportedOperationException()
      }

      @Override
      boolean shareSameAccount(final String userId1, final String userId2) {
        throw new UnsupportedOperationException()
      }

      @Override
      User lookupUserById(final String userId) {
        throw new UnsupportedOperationException()
      }

      @Override
      User lookupUserByAccessKeyId(final String keyId) {
        throw new UnsupportedOperationException()
      }

      @Override
      User lookupUserByCertificate(final X509Certificate cert) {
        throw new UnsupportedOperationException()
      }

      @Override
      User lookupUserByConfirmationCode(final String code) {
        throw new UnsupportedOperationException()
      }

      @Override
      Group lookupGroupById(final String groupId) {
        throw new UnsupportedOperationException()
      }

      @Override
      List<User> listUsersForAccounts(final Collection<String> accountIds, final boolean eager) {
        throw new UnsupportedOperationException()
      }

      @Override
      List<Group> listGroupsForAccounts(final Collection<String> accountIds) {
        throw new UnsupportedOperationException()
      }

      @Override
      Map<String, List<Policy>> listPoliciesForUsers(final Collection<String> userIds) {
        throw new UnsupportedOperationException()
      }

      @Override
      Map<String, List<Policy>> listPoliciesForGroups(final Collection<String> groupIds) {
        throw new UnsupportedOperationException()
      }

      @Override
      Map<String, List<Certificate>> listSigningCertificatesForUsers(final Collection<String> userIds) {
        throw new UnsupportedOperationException()
      }

      @Override
      Map<String, List<AccessKey>> listAccessKeysForUsers(final Collection<String> userIds) {
        throw new UnsupportedOperationException()
      }

      @Override
      Role lookupRoleById(final String roleId) {
        throw new UnsupportedOperationException()
      }

      @Override
      Certificate lookupCertificate(final X509Certificate cert) {
        throw new UnsupportedOperationException()
      }

      @Override
      AccessKey lookupAccessKeyById(final String keyId) {
        throw new UnsupportedOperationException()
      }

      @Override
      User lookupUserByName(final String userName) {
        throw new UnsupportedOperationException()
      }

      @Override
      Account lookupAccountByCanonicalId(String canonicalId) throws AuthException {
        throw new UnsupportedOperationException()
      }

      @Override
      User lookupUserByEmailAddress(String email) throws AuthException {
        throw new UnsupportedOperationException()
      }
    }
  }

  ScalingActivities autoScalingActivitiesStore( List<ScalingActivity> activities = [] ) {
    new ScalingActivities() {
      @Override
      <T> List<T> list(@Nullable OwnerFullName ownerFullName,
                       @Nonnull Predicate<? super ScalingActivity> filter,
                       @Nonnull Function<? super ScalingActivity, T> transform ) {
        activities
            .findAll { activity -> ( ownerFullName==null || activity.ownerAccountNumber.equals( ownerFullName.accountNumber ) ) && filter.apply( activity ) }
            .collect { activity -> transform.apply( activity ) }
      }

      @Override
      <T> List<T> list(@Nullable OwnerFullName ownerFullName,
                       @Nullable AutoScalingMetadata.AutoScalingGroupMetadata group,
                       @Nonnull  Collection<String> activityIds,
                       @Nonnull  Predicate<? super ScalingActivity> filter,
                       @Nonnull  Function<? super ScalingActivity, T> transform) {
        activities
            .findAll{ activity ->
              (activityIds.isEmpty() || activityIds.contains( activity.activityId ) ) &&
              (ownerFullName==null || activity.ownerAccountNumber.equals( ownerFullName.accountNumber ))
            }
            .collect { activity -> transform.apply( activity ) }
      }

      @Override
      <T> List<T> listByActivityStatusCode(@Nullable OwnerFullName ownerFullName,
                                           @Nonnull  Collection<ActivityStatusCode> statusCodes,
                                           @Nonnull  Function<? super ScalingActivity, T> transform) {
        []
      }

      @Override
      void update(OwnerFullName ownerFullName,
                  String activityId,
                  Callback<ScalingActivity> activityUpdateCallback) {
        ScalingActivity activity = activities.find{ ScalingActivity activity -> activityId.equals( activity.displayName ) }
        if ( !activity ) throw new AutoScalingMetadataNotFoundException("Activity not found: " + activityId)
        activityUpdateCallback.fire( activity )
      }

      @Override
      boolean delete(AutoScalingMetadata.ScalingActivityMetadata scalingActivity) {
        activities.remove(scalingActivity)
      }

      @Override
      int deleteByCreatedAge(@Nullable OwnerFullName ownerFullName,
                             long createdBefore) {
        0
      }

      @Override
      ScalingActivity save(ScalingActivity scalingActivity) {
        scalingActivity.setId( "1" )
        scalingActivity.setNaturalId( UUID.randomUUID( ).toString( ) )
        scalingActivity.setCreationTimestamp( new Date() )
        scalingActivity.setLastUpdateTimestamp( new Date() )
        activities.add( scalingActivity )
        scalingActivity
      }
    }
  }

  AutoScalingGroups autoScalingGroupStore( List<AutoScalingGroup> groups = [], boolean healthChecks = false ) {
    new AutoScalingGroups() {
      @Override
      <T> List<T> list(@Nullable OwnerFullName ownerFullName,
                       @Nonnull Predicate<? super AutoScalingGroup> filter,
                       @Nonnull Function<? super AutoScalingGroup, T> transform ) {
        groups
            .findAll { group -> ( ownerFullName==null || group.ownerAccountNumber.equals( ownerFullName.accountNumber ) ) && filter.apply( group ) }
            .collect { group -> transform.apply( group ) }
      }

      @Override
      <T> List<T> listRequiringScaling(@Nonnull Function<? super AutoScalingGroup, T> transform) {
        groups
            .findAll { group -> Boolean.TRUE.equals( group.scalingRequired ) }
            .collect { group -> transform.apply( group ) }
      }

      @Override
      <T> List<T> listRequiringInstanceReplacement(@Nonnull Function<? super AutoScalingGroup, T> transform) {
        []
      }

      @Override
      <T> List<T> listRequiringMonitoring(long interval,
                                          @Nonnull Function<? super AutoScalingGroup, T> transform) {
        healthChecks ?
          groups.collect { group -> transform.apply( group ) } :
          []
      }

      @Override
      <T> T lookup(OwnerFullName ownerFullName,
                   String autoScalingGroupName,
                   Function<? super AutoScalingGroup, T> transform) {
        AutoScalingGroup group = groups.find { AutoScalingGroup group ->
          group.arn.equals( autoScalingGroupName ) ||
              ( group.displayName.equals( autoScalingGroupName ) &&
                  group.ownerAccountNumber.equals( ownerFullName.accountNumber ) )
        }
        if ( group == null ) {
          throw new AutoScalingMetadataNotFoundException("Group not found: " + autoScalingGroupName)
        }
        transform.apply( group )
      }

      @Override
      void update(OwnerFullName ownerFullName,
                  String autoScalingGroupName,
                  Callback<AutoScalingGroup> groupUpdateCallback) {
        AutoScalingGroup group = lookup( ownerFullName, autoScalingGroupName, { it } as Function<AutoScalingGroup,AutoScalingGroup> )
        groupUpdateCallback.fire( group )
      }

      @Override
      void markScalingRequiredForZones(Set<String> availabilityZones) {
        groups.findAll { group ->
          !Sets.intersection( group.availabilityZones as Set<String>, availabilityZones ).isEmpty()
        }.each { group ->
          group.scalingRequired = true
        }
      }

      @Override
      boolean delete(AutoScalingMetadata.AutoScalingGroupMetadata autoScalingGroup) {
        groups.remove( autoScalingGroup )
      }

      @Override
      AutoScalingGroup save(AutoScalingGroup autoScalingGroup) {
        autoScalingGroup.setId( "1" )
        autoScalingGroup.setNaturalId( UUID.randomUUID( ).toString( ) )
        groups.add( autoScalingGroup )
        autoScalingGroup
      }
    }
  }

  AutoScalingInstances autoScalingInstanceStore( List<AutoScalingInstance> instances = [] ) {
    new AutoScalingInstances(){
      long timestamp = System.currentTimeMillis() - 1000

      @Override
      <T> List<T> list(@Nullable OwnerFullName ownerFullName,
                       @Nonnull Predicate<? super AutoScalingInstance> filter,
                       @Nonnull Function<? super AutoScalingInstance, T> transform ) {
        instances
            .findAll { instance -> ( ownerFullName==null || instance.ownerAccountNumber.equals( ownerFullName.accountNumber ) ) && filter.apply( instance ) }
            .collect { instance -> transform.apply( instance ) }
      }

      @Override
      <T> List<T> listByGroup(OwnerFullName ownerFullName,
                              String groupName,
                              Function<? super AutoScalingInstance, T> transform) {
        list( ownerFullName, { AutoScalingInstance instance ->
          groupName.equals( instance.autoScalingGroupName )
        } as Predicate, transform )
      }

      @Override
      <T> List<T> listByGroup(AutoScalingMetadata.AutoScalingGroupMetadata group,
                              Predicate<? super AutoScalingInstance> filter,
                              Function<? super AutoScalingInstance, T> transform) {
        group == null ?
          list( null, filter, transform ) :
          listByGroup(
            group.owner,
            group.displayName,
            Functions.identity() )
              .findAll{ AutoScalingInstance instance -> filter.apply( instance ) }
              .collect{ AutoScalingInstance instance -> transform.apply( instance ) }
      }

      @Override
      <T> List<T> listByState(LifecycleState lifecycleState,
                              ConfigurationState configurationState,
                              Function<? super AutoScalingInstance, T> transform) {
        instances
            .findAll { instance -> lifecycleState.apply( instance ) && configurationState.apply( instance ) }
            .collect { instance -> transform.apply( instance ) }
      }

      @Override
      <T> List<T> listUnhealthyByGroup(AutoScalingMetadata.AutoScalingGroupMetadata group,
                                       Function<? super AutoScalingInstance, T> transform) {
        []
      }

      @Override
      <T> T lookup(OwnerFullName ownerFullName,
                   String instanceId,
                   Function<? super AutoScalingInstance, T> transform) {
        transform.apply( list( ownerFullName, Predicates.alwaysTrue(), { it } as Function<AutoScalingInstance,AutoScalingInstance> ).find { instance ->
          instance.instanceId.equals( instanceId )
        } )
      }

      @Override
      void update(OwnerFullName ownerFullName,
                  String instanceId,
                  Callback<AutoScalingInstance> instanceUpdateCallback) {
        AutoScalingInstance instance = lookup( ownerFullName, instanceId, { it } as Function<AutoScalingInstance,AutoScalingInstance> )
        instanceUpdateCallback.fire( instance )
      }

      @Override
      void markMissingInstancesUnhealthy(AutoScalingMetadata.AutoScalingGroupMetadata group,
                                         Collection<String> instanceIds) {
        instances.each { instance ->
          if (group.displayName.equals( instance.autoScalingGroupName ) &&
             !instanceIds.contains(instance.instanceId  ) ) {
            instance.healthStatus = HealthStatus.Unhealthy
          } }
      }

      @Override
      void markExpiredPendingUnhealthy(AutoScalingMetadata.AutoScalingGroupMetadata group,
                                       Collection<String> instanceIds,
                                       long maxAge) {
      }

      @Override
      Set<String> verifyInstanceIds(String accountNumber,
                                    Collection<String> instanceIds) {
        [] as Set
      }

      @Override
      void transitionState(AutoScalingMetadata.AutoScalingGroupMetadata group,
                           LifecycleState from,
                           LifecycleState to,
                           Collection<String> instanceIds) {
        instances.each { instance ->
          instanceIds.contains( instance.instanceId ) &&
              from.transitionTo(to).apply( instance )  }
      }

      @Override
      void transitionConfigurationState(AutoScalingMetadata.AutoScalingGroupMetadata group,
                                        ConfigurationState from,
                                        ConfigurationState to,
                                        Collection<String> instanceIds) {
        instances.each { instance ->
          instanceIds.contains( instance.instanceId ) &&
              from.transitionTo(to).apply( instance )  }
      }

      @Override
      int registrationFailure(AutoScalingMetadata.AutoScalingGroupMetadata group,
                              Collection<String> instanceIds) {
        0
      }

      @Override
      boolean delete(AutoScalingMetadata.AutoScalingInstanceMetadata autoScalingInstance) {
        instances.remove( autoScalingInstance )
      }

      @Override
      boolean deleteByGroup(AutoScalingMetadata.AutoScalingGroupMetadata group) {
        instances.removeAll( instances.findAll { instance -> group.autoScalingGroupName.equals( instance.autoScalingGroupName ) } )
      }

      @Override
      AutoScalingInstance save(AutoScalingInstance autoScalingInstance) {
        AutoScalingGroup group = autoScalingInstance.autoScalingGroup
        autoScalingInstance.setId( "1" )
        autoScalingInstance.setVersion( 1 )
        autoScalingInstance.setNaturalId( UUID.randomUUID( ).toString( ) )
        autoScalingInstance.setCreationTimestamp( new Date(timestamp++) )
        autoScalingInstance.setLastUpdateTimestamp( new Date(timestamp) )
        Method method = AbstractOwnedPersistent.class.getDeclaredMethod( "setUniqueName", [ String.class ] as Class[] )
        method.setAccessible( true )
        method.invoke( autoScalingInstance, [ autoScalingInstance.instanceId ] as Object[] )
        autoScalingInstance.autoScalingGroupName = group.displayName
        instances.add( autoScalingInstance )
        autoScalingInstance
      }
    }
  }

  ZoneMonitor zoneMonitor( List<String> unavailableZones ) {
    new ZoneMonitor() {
      @Override
      Set<String> getUnavailableZones(long duration) {
        unavailableZones as Set<String>
      }
    }
  }

  ZoneUnavailabilityMarkers zoneAvailabilityMarkers( ) {
    new ZoneUnavailabilityMarkers() {
      private final Set<String> unavailableZones = Sets.newHashSet( )

      @Override
      void updateUnavailableZones(Set<String> unavailableZones,
                                  ZoneUnavailabilityMarkers.ZoneCallback callback) {
        final Set<String> changedZones = Sets.newHashSet( Sets.symmetricDifference( unavailableZones, this.unavailableZones ) )
        this.unavailableZones.clear()
        this.unavailableZones.addAll( unavailableZones )
        callback.notifyChangedZones( changedZones )
      }
    }
  }
}
