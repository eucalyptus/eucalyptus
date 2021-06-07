/*************************************************************************
 * Copyright 2009-2016 Ent. Services Development Corporation LP
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
package com.eucalyptus.autoscaling

import com.eucalyptus.auth.Accounts
import com.eucalyptus.auth.api.PrincipalProvider
import com.eucalyptus.auth.principal.AccountFullName
import com.eucalyptus.auth.principal.TestProvider
import com.eucalyptus.autoscaling.common.internal.activities.ActivityCause
import com.eucalyptus.autoscaling.activities.ActivityManager
import com.eucalyptus.autoscaling.common.internal.activities.ActivityStatusCode
import com.eucalyptus.autoscaling.activities.CloudWatchClient
import com.eucalyptus.autoscaling.common.internal.activities.ScalingActivities
import com.eucalyptus.autoscaling.common.internal.activities.ScalingActivity
import com.eucalyptus.autoscaling.activities.TestClients
import com.eucalyptus.autoscaling.backend.AutoScalingBackendService
import com.eucalyptus.autoscaling.common.AutoScalingMetadata
import com.eucalyptus.autoscaling.common.backend.msgs.CreateAutoScalingGroupType
import com.eucalyptus.autoscaling.common.backend.msgs.CreateLaunchConfigurationType
import com.eucalyptus.autoscaling.common.backend.msgs.DeleteAutoScalingGroupType
import com.eucalyptus.autoscaling.common.backend.msgs.DeleteLaunchConfigurationType
import com.eucalyptus.autoscaling.common.backend.msgs.DeletePolicyType
import com.eucalyptus.autoscaling.common.backend.msgs.DescribePoliciesResponseType
import com.eucalyptus.autoscaling.common.backend.msgs.DescribePoliciesType
import com.eucalyptus.autoscaling.common.backend.msgs.ExecutePolicyType
import com.eucalyptus.autoscaling.common.backend.msgs.PutScalingPolicyType
import com.eucalyptus.autoscaling.common.backend.msgs.TerminateInstanceInAutoScalingGroupResponseType
import com.eucalyptus.autoscaling.common.backend.msgs.TerminateInstanceInAutoScalingGroupType
import com.eucalyptus.autoscaling.common.msgs.Activity
import com.eucalyptus.autoscaling.common.msgs.AutoScalingGroupNames
import com.eucalyptus.autoscaling.common.msgs.AutoScalingGroupType
import com.eucalyptus.autoscaling.common.msgs.AutoScalingInstanceDetails
import com.eucalyptus.autoscaling.common.msgs.AvailabilityZones
import com.eucalyptus.autoscaling.common.msgs.BlockDeviceMappingType
import com.eucalyptus.autoscaling.common.msgs.BlockDeviceMappings
import com.eucalyptus.autoscaling.common.msgs.DescribeAutoScalingGroupsResponseType
import com.eucalyptus.autoscaling.common.msgs.DescribeAutoScalingGroupsType
import com.eucalyptus.autoscaling.common.msgs.DescribeAutoScalingInstancesResponseType
import com.eucalyptus.autoscaling.common.msgs.DescribeAutoScalingInstancesType
import com.eucalyptus.autoscaling.common.msgs.DescribeLaunchConfigurationsResponseType
import com.eucalyptus.autoscaling.common.msgs.DescribeLaunchConfigurationsType
import com.eucalyptus.autoscaling.common.msgs.InstanceMonitoring
import com.eucalyptus.autoscaling.common.msgs.LaunchConfigurationNames
import com.eucalyptus.autoscaling.common.msgs.LaunchConfigurationType
import com.eucalyptus.autoscaling.common.msgs.LoadBalancerNames
import com.eucalyptus.autoscaling.common.msgs.PolicyNames
import com.eucalyptus.autoscaling.common.msgs.ScalingPolicyType
import com.eucalyptus.autoscaling.common.msgs.SecurityGroups
import com.eucalyptus.autoscaling.common.msgs.TerminationPolicies
import com.eucalyptus.autoscaling.common.internal.configurations.LaunchConfiguration
import com.eucalyptus.autoscaling.common.internal.configurations.LaunchConfigurations
import com.eucalyptus.autoscaling.common.internal.groups.AutoScalingGroup
import com.eucalyptus.autoscaling.common.internal.groups.AutoScalingGroupCoreView
import com.eucalyptus.autoscaling.common.internal.groups.AutoScalingGroups
import com.eucalyptus.autoscaling.common.internal.groups.HealthCheckType
import com.eucalyptus.autoscaling.common.internal.instances.AutoScalingInstance
import com.eucalyptus.autoscaling.common.internal.instances.AutoScalingInstances
import com.eucalyptus.autoscaling.common.internal.instances.ConfigurationState
import com.eucalyptus.autoscaling.common.internal.instances.HealthStatus
import com.eucalyptus.autoscaling.common.internal.instances.LifecycleState
import com.eucalyptus.autoscaling.common.internal.metadata.AutoScalingMetadataException
import com.eucalyptus.autoscaling.common.internal.metadata.AutoScalingMetadataNotFoundException
import com.eucalyptus.autoscaling.common.internal.policies.ScalingPolicies
import com.eucalyptus.autoscaling.common.internal.policies.ScalingPolicy
import com.eucalyptus.autoscaling.common.internal.tags.AutoScalingGroupTag
import com.eucalyptus.autoscaling.common.internal.tags.Tag
import com.eucalyptus.autoscaling.common.internal.tags.TagSupportDiscovery
import com.eucalyptus.autoscaling.common.internal.tags.Tags
import com.eucalyptus.autoscaling.service.AutoScalingService
import com.eucalyptus.cloudwatch.common.msgs.DescribeAlarmsResponseType
import com.eucalyptus.cloudwatch.common.msgs.DescribeAlarmsType
import com.eucalyptus.context.Context
import com.eucalyptus.context.Contexts
import com.eucalyptus.crypto.util.Timestamps
import com.eucalyptus.util.Callback
import com.eucalyptus.util.Classes
import com.eucalyptus.util.Consumer
import com.eucalyptus.auth.principal.OwnerFullName
import com.eucalyptus.util.TypeMappers
import com.google.common.base.Function
import com.google.common.base.Functions
import com.google.common.base.Predicate
import com.google.common.base.Predicates
import com.google.common.base.Suppliers
import com.google.common.collect.ImmutableMap
import com.google.common.collect.Lists
import edu.ucsb.eucalyptus.msgs.BaseMessage
import static org.junit.Assert.*
import org.junit.BeforeClass
import org.junit.Test

import javax.annotation.Nonnull
import javax.annotation.Nullable
import com.eucalyptus.auth.Permissions
import com.eucalyptus.auth.policy.PolicyEngineImpl

/**
 *
 */
@SuppressWarnings("GroovyAccessibility")
class AutoScalingServiceTest {

  @BeforeClass
  static void before() {
    TypeMappers.TypeMapperDiscovery discovery = new TypeMappers.TypeMapperDiscovery()
    def registerTypeMapper = { Class<?> transformClass ->
      Classes.genericsToClasses( transformClass ).with {
        Class<?> from = get( 0 )
        Class<?> to = get( 1 )
        try {
          TypeMappers.lookup( from, to )
        } catch ( IllegalArgumentException ) {  // not registered
          discovery.processClass( transformClass )
        }
      }
    }
    [
        AutoScalingGroups.AutoScalingGroupTransform,
        AutoScalingGroups.AutoScalingGroupCoreViewTransform,
        AutoScalingGroups.AutoScalingGroupMinimumViewTransform,
        AutoScalingGroups.AutoScalingGroupMetricsViewTransform,
        AutoScalingGroups.AutoScalingGroupScalingViewTransform,
        AutoScalingInstances.AutoScalingInstanceSummaryTransform,
        AutoScalingInstances.AutoScalingInstanceTransform,
        AutoScalingInstances.AutoScalingInstanceCoreViewTransform,
        AutoScalingInstances.AutoScalingInstanceGroupViewTransform,
        LaunchConfigurations.BlockDeviceTransform,
        LaunchConfigurations.LaunchConfigurationTransform,
        LaunchConfigurations.LaunchConfigurationCoreViewTransform,
        LaunchConfigurations.LaunchConfigurationMinimumViewTransform,
        ScalingActivities.ScalingActivityTransform,
        ScalingPolicies.ScalingPolicyTransform,
        ScalingPolicies.ScalingPolicyViewTransform,
        Tags.TagToTagDescription
    ].each( registerTypeMapper )
    TagSupportDiscovery tagDiscovery = new TagSupportDiscovery()
    tagDiscovery.processClass( TestAutoScalingGroupTagSupport.class )
    Permissions.setPolicyEngine( new PolicyEngineImpl( Suppliers.ofInstance( Boolean.FALSE ), Suppliers.<String>ofInstance( null ) ) )
  }

  @Test
  void testLaunchConfigurations() {
    Accounts.setIdentityProvider( identityProvider() )
    AutoScalingConvergedService service = service()
    Contexts.threadLocal(  new Context( "", new BaseMessage() ) )

    service.createLaunchConfiguration( new CreateLaunchConfigurationType(
        launchConfigurationName: "Test",
        imageId: "emi-00000001",
        kernelId: "eki-00000001",
        ramdiskId: "eri-00000001",
        instanceType: "m1.small",
        keyName: "keyname",
        userData: "data",
        instanceMonitoring: new InstanceMonitoring( Boolean.TRUE ),
        blockDeviceMappings: new BlockDeviceMappings( [ new BlockDeviceMappingType( deviceName: "/dev/sdf", virtualName: "ephemeral1" ) ] ),
        securityGroups: new SecurityGroups( member: Lists.newArrayList( "test" ) )
    ) )

    DescribeLaunchConfigurationsResponseType emptyDescribeResponse =
      service.describeLaunchConfigurations( new DescribeLaunchConfigurationsType( launchConfigurationNames: new LaunchConfigurationNames( member: [ "BADNAME" ] ) ) )

    List<LaunchConfigurationType> emptyConfigurations =
      emptyDescribeResponse.describeLaunchConfigurationsResult.launchConfigurations.member

    assertEquals( "Configuration count", 0, emptyConfigurations.size() )

    DescribeLaunchConfigurationsResponseType describeLaunchConfigurationsResponseType =
      service.describeLaunchConfigurations( new DescribeLaunchConfigurationsType() )

    List<LaunchConfigurationType> configurations =
      describeLaunchConfigurationsResponseType.describeLaunchConfigurationsResult.launchConfigurations.member

    assertEquals( "Configuration count", 1, configurations.size() )
    LaunchConfigurationType config = configurations.get( 0 )
    assertEquals( "Launch configuration name", "Test", config.launchConfigurationName )
    assertEquals( "Image ID", "emi-00000001", config.imageId )
    assertEquals( "Kernel ID", "eki-00000001", config.kernelId )
    assertEquals( "Ramdisk ID", "eri-00000001", config.ramdiskId )
    assertEquals( "Instance type", "m1.small", config.instanceType )
    assertEquals( "Key name", "keyname", config.keyName )
    assertEquals( "User data", "data", config.userData )
    assertNotNull( "Security groups", config.securityGroups )
    assertEquals( "Security groups", Lists.newArrayList( "test" ), config.securityGroups.member )
    assertNotNull( "Block device mappings", config.blockDeviceMappings )
    assertNotNull( "Block device mappings members", config.blockDeviceMappings.member )
    assertEquals( "Block device mappings size", 1, config.blockDeviceMappings.member.size() )
    assertEquals( "Block device mappings 0 device", "/dev/sdf", config.blockDeviceMappings.member.get(0).deviceName )
    assertEquals( "Block device mappings 0 virtual", "ephemeral1", config.blockDeviceMappings.member.get(0).virtualName )

    service.deleteLaunchConfiguration( new DeleteLaunchConfigurationType( launchConfigurationName: "Test" ) )

    assertEquals( "Configuration count", 0,
        service.describeLaunchConfigurations( new DescribeLaunchConfigurationsType() )
        .describeLaunchConfigurationsResult.launchConfigurations.member.size() )
  }

  @Test
  void testAutoScalingGroups() {
    Accounts.setIdentityProvider( identityProvider() )
    AutoScalingConvergedService service = service()
    Contexts.threadLocal(  new Context( "", new BaseMessage() ) )

    service.createLaunchConfiguration( new CreateLaunchConfigurationType(
        launchConfigurationName: "TestLaunch",
        imageId: "emi-00000001",
        instanceType: "m1.small"
    ) )

    service.createAutoScalingGroup( new CreateAutoScalingGroupType(
      autoScalingGroupName: "Test",
      defaultCooldown: 5,
      desiredCapacity: 3,
      minSize: 1,
      maxSize: 10,
      availabilityZones: new AvailabilityZones( member: [ "zone-1" ] ),
      launchConfigurationName: "TestLaunch",
      healthCheckGracePeriod: 4,
      healthCheckType: "EC2",
      loadBalancerNames: new LoadBalancerNames( member: [ "balancer-1", "balancer-2" ] ),
      terminationPolicies: new TerminationPolicies( member: [ "NewestInstance", "Default" ] ),
    ) )

    DescribeAutoScalingGroupsResponseType emptyDescribeResponse =
      service.describeAutoScalingGroups( new DescribeAutoScalingGroupsType( autoScalingGroupNames: new AutoScalingGroupNames( member: [ "BADNAME" ] ) ) )

    List<AutoScalingGroupType> emptyGroups =
      emptyDescribeResponse.describeAutoScalingGroupsResult.autoScalingGroups.member

    assertEquals( "Configuration count", 0, emptyGroups.size() )

    DescribeAutoScalingGroupsResponseType describeAutoScalingGroupsResponseType =
      service.describeAutoScalingGroups( new DescribeAutoScalingGroupsType() )

    List<AutoScalingGroupType> groups =
      describeAutoScalingGroupsResponseType.describeAutoScalingGroupsResult.autoScalingGroups.member

    assertEquals( "Group count", 1, groups.size() )
    AutoScalingGroupType group = groups.get( 0 )
    assertEquals( "Auto scaling group name", "Test", group.autoScalingGroupName )
    assertEquals( "Launch configuration name", "TestLaunch", group.launchConfigurationName )
    assertEquals( "Max size", 10,  group.getMaxSize() )
    assertEquals( "Min size", 1,  group.getMinSize() )
    assertEquals( "Default cooldown", 5,  group.getDefaultCooldown() )
    assertEquals( "Desired capacity", 3,  group.getDesiredCapacity() )
    assertEquals( "Health check grace period", 4,  group.getHealthCheckGracePeriod() )
    assertEquals( "Health check type", "EC2", group.getHealthCheckType() )
    assertNotNull( "Availability zones", group.availabilityZones )
    assertEquals( "Availability zones", [ "zone-1" ], group.availabilityZones.member)
    assertNotNull( "Load balancer names", group.loadBalancerNames )
    assertEquals( "Load balancer names", [ "balancer-1", "balancer-2" ], group.loadBalancerNames.member )
    assertNotNull( "Termination policies", group.terminationPolicies )
    assertEquals( "Termination policies", [ "NewestInstance", "Default" ], group.terminationPolicies.member )

    service.deleteAutoScalingGroup( new DeleteAutoScalingGroupType( autoScalingGroupName: "Test" ) )

    assertEquals( "Groups count", 0,
        service.describeAutoScalingGroups( new DescribeAutoScalingGroupsType() )
            .describeAutoScalingGroupsResult.autoScalingGroups.member.size() )

    service.deleteLaunchConfiguration( new DeleteLaunchConfigurationType( launchConfigurationName: "TestLaunch" ) )

    assertEquals( "Configuration count", 0,
        service.describeLaunchConfigurations( new DescribeLaunchConfigurationsType() )
            .describeLaunchConfigurationsResult.launchConfigurations.member.size() )
  }

  @Test
  void testScalingPolicies() {
    Accounts.setIdentityProvider( identityProvider() )
    AutoScalingConvergedService service = service()
    Contexts.threadLocal(  new Context( "", new BaseMessage() ) )

    service.createLaunchConfiguration( new CreateLaunchConfigurationType(
        launchConfigurationName: "TestLaunch",
        imageId: "emi-00000001",
        instanceType: "m1.small"
    ) )

    service.createAutoScalingGroup( new CreateAutoScalingGroupType(
        autoScalingGroupName: "TestGroup",
        desiredCapacity: 3,
        minSize: 1,
        maxSize: 10,
        availabilityZones: new AvailabilityZones( member: [ "zone-1" ] ),
        launchConfigurationName: "TestLaunch"
    ) )

    service.putScalingPolicy( new PutScalingPolicyType(
        autoScalingGroupName: "TestGroup",
        policyName: "Test",
        adjustmentType: "ChangeInCapacity",
        scalingAdjustment: 2
    ) )

    DescribePoliciesResponseType emptyDescribeResponse =
      service.describePolicies( new DescribePoliciesType( policyNames: new PolicyNames( member: [ "BADNAME" ] ) ) )

    List<ScalingPolicyType> emptyPolicies =
      emptyDescribeResponse.describePoliciesResult.scalingPolicies.member

    assertEquals( "Configuration count", 0, emptyPolicies.size() )

    DescribePoliciesResponseType describePoliciesResponseType =
      service.describePolicies( new DescribePoliciesType() )

    List<ScalingPolicyType> policies =
      describePoliciesResponseType.describePoliciesResult.scalingPolicies.member

    assertEquals( "Group count", 1, policies.size() )
    ScalingPolicyType policy = policies.get( 0 )
    assertEquals( "Policy name", "Test", policy.policyName )
    assertEquals( "Auto scaling group name", "TestGroup", policy.autoScalingGroupName )
    assertEquals( "Adjustment type", "ChangeInCapacity",  policy.adjustmentType )
    assertEquals( "Scaling adjustment", 2,  policy.scalingAdjustment )

    service.executePolicy( new ExecutePolicyType( autoScalingGroupName: "TestGroup", policyName: "Test" ) )

    DescribeAutoScalingGroupsResponseType describeAutoScalingGroupsResponseType =
      service.describeAutoScalingGroups( new DescribeAutoScalingGroupsType() )

    List<AutoScalingGroupType> groups =
      describeAutoScalingGroupsResponseType.describeAutoScalingGroupsResult.autoScalingGroups.member

    assertEquals( "Group count", 1, groups.size() )
    AutoScalingGroupType group = groups.get( 0 )
    assertEquals( "Group desired capacity after scaling", 5, group.desiredCapacity )

    service.deletePolicy( new DeletePolicyType( autoScalingGroupName: "TestGroup", policyName: "Test" ) )

    assertEquals( "Policies count", 0,
        service.describePolicies( new DescribePoliciesType() )
            .describePoliciesResult.scalingPolicies.member.size() )

    service.deleteAutoScalingGroup( new DeleteAutoScalingGroupType( autoScalingGroupName: "TestGroup" ) )

    assertEquals( "Groups count", 0,
        service.describeAutoScalingGroups( new DescribeAutoScalingGroupsType() )
            .describeAutoScalingGroupsResult.autoScalingGroups.member.size() )

    service.deleteLaunchConfiguration( new DeleteLaunchConfigurationType( launchConfigurationName: "TestLaunch" ) )

    assertEquals( "Configuration count", 0,
        service.describeLaunchConfigurations( new DescribeLaunchConfigurationsType() )
            .describeLaunchConfigurationsResult.launchConfigurations.member.size() )
  }

  @SuppressWarnings("GroovyAssignabilityCheck")
  @Test
  void testDescribeInstances() {
    Accounts.setIdentityProvider( identityProvider() )
    AutoScalingConvergedService service = service( launchConfigurationStore(), autoScalingGroupStore(), autoScalingInstanceStore( [
      new AutoScalingInstance(
          ownerAccountNumber: '000000000000',
          displayName: 'i-00000001',
          availabilityZone: 'Zone1',
          autoScalingGroupName: 'Group1',
          launchConfigurationName: 'Config1',
          healthStatus: HealthStatus.Healthy,
          lifecycleState: LifecycleState.InService )
    ] ), scalingPolicyStore() )
    Contexts.threadLocal(  new Context( "", new BaseMessage() ) )

    DescribeAutoScalingInstancesResponseType describeAutoScalingInstancesResponseType =
      service.describeAutoScalingInstances( new DescribeAutoScalingInstancesType() )

    List<AutoScalingInstanceDetails> instanceDetails =
      describeAutoScalingInstancesResponseType.describeAutoScalingInstancesResult.autoScalingInstances.member

    assertEquals( "Group count", 1, instanceDetails.size() )
    AutoScalingInstanceDetails details = instanceDetails.get( 0 )
    assertEquals( "Instance identifier", "i-00000001", details.instanceId )
    assertEquals( "Auto scaling group name", "Group1", details.autoScalingGroupName )
    assertEquals( "Launch configuration name", "Config1",  details.launchConfigurationName )
    assertEquals( "Availability zone", "Zone1",  details.availabilityZone )
    assertEquals( "Health status", "Healthy",  details.healthStatus )
    assertEquals( "Lifecycle state", "InService",  details.lifecycleState )
  }

  @SuppressWarnings("GroovyAssignabilityCheck")
  @Test
  void testTerminateInstances() {
    Accounts.setIdentityProvider( identityProvider() )
    AutoScalingGroup group
    AutoScalingConvergedService service = service( launchConfigurationStore(), autoScalingGroupStore( [
        group = new AutoScalingGroup(
            naturalId: '88777c80-7248-11e2-bcfd-0800200c9a66',
            ownerAccountNumber: '000000000000',
            displayName: 'Group1',
            availabilityZones: [ 'Zone1' ],
            maxSize: 1,
            minSize: 1,
            desiredCapacity: 1,
            capacity: 1,
            scalingRequired: false,
            defaultCooldown: 300,
            healthCheckType: HealthCheckType.EC2,
            launchConfiguration: new LaunchConfiguration(),
        )
    ] ), autoScalingInstanceStore( [
        new AutoScalingInstance(
            ownerAccountNumber: '000000000000',
            displayName: 'i-00000001',
            availabilityZone: 'Zone1',
            autoScalingGroup: group,
            autoScalingGroupName: 'Group1',
            launchConfigurationName: 'Config1',
            healthStatus: HealthStatus.Healthy,
            lifecycleState: LifecycleState.InService,
        )
    ] ), scalingPolicyStore() )
    Contexts.threadLocal(  new Context( "", new BaseMessage() ) )

    TerminateInstanceInAutoScalingGroupResponseType terminateInstanceInAutoScalingGroupResponseType =
      service.terminateInstanceInAutoScalingGroup( new TerminateInstanceInAutoScalingGroupType(
        instanceId: 'i-00000001',
        shouldDecrementDesiredCapacity: true,
      ) )

    Activity activity =
      terminateInstanceInAutoScalingGroupResponseType.terminateInstanceInAutoScalingGroupResult.activity

    assertNotNull( "Activity", activity )
    assertEquals( "Activity identifier", 'b7717740-7246-11e2-bcfd-0800200c9a66', activity.activityId )
    assertEquals( "Activity group name", "Group1", activity.autoScalingGroupName )
    assertEquals( "Activity cause", "At 2013-02-01T12:34:56Z Some cause.", activity.cause )
    assertEquals( "Activity start time", date( "2013-02-01T12:34:56.789Z" ), activity.startTime )
    assertEquals( "Activity status code", "InProgress", activity.statusCode )
    assertEquals( "Activity description", "Description", activity.description )
    assertEquals( "Activity details", "Details", activity.details )
    assertEquals( "Activity progress", 13, activity.progress )
  }

  class AutoScalingConvergedService {
    @Delegate AutoScalingBackendService backend
    @Delegate AutoScalingService userFacing
  }

  AutoScalingConvergedService service(
      launchConfigurationStore = launchConfigurationStore(),
      autoScalingGroupStore = autoScalingGroupStore(),
      autoScalingInstanceStore = autoScalingInstanceStore(),
      scalingPolicyStore = scalingPolicyStore(),
      activityManager = activityManager(),
      scalingActivities = autoScalingActivitiesStore()
  ) {
    new AutoScalingConvergedService(
        backend: new AutoScalingBackendService(
            launchConfigurationStore,
            autoScalingGroupStore,
            autoScalingInstanceStore,
            scalingPolicyStore,
            activityManager ),
        userFacing: new AutoScalingService(
            launchConfigurationStore,
            autoScalingGroupStore,
            autoScalingInstanceStore,
            scalingPolicyStore,
            scalingActivities
        )
    )
  }

  PrincipalProvider identityProvider() {
    new TestProvider( )
  }

  LaunchConfigurations launchConfigurationStore() {
    List<LaunchConfiguration> configurations = []

    new LaunchConfigurations() {
      @Override
      <T> List<T> list(@Nullable OwnerFullName ownerFullName,
                       @Nonnull Predicate<? super LaunchConfiguration> filter,
                       @Nonnull Function<? super LaunchConfiguration, T> transform ) {
        configurations
            .findAll { configuration -> ( ownerFullName==null || configuration.ownerAccountNumber.equals( ownerFullName.accountNumber ) ) && filter.apply( configuration ) }
            .collect { configuration -> transform.apply( configuration ) }
      }

      @Override
      <T> T lookup(OwnerFullName ownerFullName,
                   String launchConfigurationName,
                   @Nonnull Function<? super LaunchConfiguration, T> transform) throws AutoScalingMetadataException {
        LaunchConfiguration configuration = configurations.find { LaunchConfiguration configuration ->
          configuration.displayName.equals( launchConfigurationName ) &&
              configuration.ownerAccountNumber.equals( ownerFullName.accountNumber )
        }
        if ( configuration == null ) throw new AutoScalingMetadataException("Not found");
        transform.apply( configuration )
      }

      @Override
      boolean delete(AutoScalingMetadata.LaunchConfigurationMetadata launchConfiguration) {
        configurations.remove( 0 ) != null
      }

      @Override
      LaunchConfiguration save(LaunchConfiguration launchConfiguration) {
        launchConfiguration.setId( "1" )
        configurations.add( launchConfiguration )
        launchConfiguration
      }
    }
  }

  AutoScalingGroups autoScalingGroupStore( List<AutoScalingGroup> groups = [] ) {
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
        []
      }

      @Override
      <T> List<T> listRequiringInstanceReplacement(@Nonnull Function<? super AutoScalingGroup, T> transform) {
        []
      }

      @Override
      <T> List<T> listRequiringMonitoring(Set<AutoScalingGroups.MonitoringSelector> selectors,
                                          @Nonnull Function<? super AutoScalingGroup, T> transform) {
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
      void markScalingRequiredForZones(Set<String> availabilityZones) { }

      @Override
      boolean delete(AutoScalingMetadata.AutoScalingGroupMetadata autoScalingGroup) {
        groups.remove( 0 ) != null
      }

      @Override
      AutoScalingGroup save(AutoScalingGroup autoScalingGroup) {
        autoScalingGroup.setId( "1" )
        groups.add( autoScalingGroup )
        autoScalingGroup
      }
    }
  }

  AutoScalingInstances autoScalingInstanceStore( List<AutoScalingInstance> instances = [] ) {
    new AutoScalingInstances(){
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
              transform )
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
      }

      @Override
      void markExpiredPendingUnhealthy(AutoScalingMetadata.AutoScalingGroupMetadata group,
                                       Collection<String> instanceIds,
                                       long maxAge) {
      }

      @Override
      Set<String> verifyInstanceIds(String accountNumber,
                                    Collection<String> instanceIds) {
        return [] as Set
      }

      @Override
      void transitionState(AutoScalingMetadata.AutoScalingGroupMetadata group,
                           LifecycleState from,
                           LifecycleState to,
                           Collection<String> instanceIds) {
      }

      @Override
      void transitionConfigurationState(AutoScalingMetadata.AutoScalingGroupMetadata group,
                                        ConfigurationState from,
                                        ConfigurationState to,
                                        Collection<String> instanceIds) {
      }

      @Override
      int registrationFailure(AutoScalingMetadata.AutoScalingGroupMetadata group,
                              Collection<String> instanceIds) {
        0
      }

      @Override
      boolean delete(OwnerFullName ownerFullName,
                     String instanceId) {
        instances.remove( lookup( ownerFullName, instanceId, Functions.identity( ) ) )
      }

      @Override
      boolean deleteByGroup(AutoScalingMetadata.AutoScalingGroupMetadata group) {
        instances.removeAll( instances.findAll { instance -> group.displayName.equals( instance.autoScalingGroupName ) } )
      }

      @Override
      AutoScalingInstance save(AutoScalingInstance autoScalingInstance) {
        autoScalingInstance.setId( "1" )
        instances.add( autoScalingInstance )
        autoScalingInstance
      }
    }
  }

  ScalingPolicies scalingPolicyStore( List<ScalingPolicy> policies = [] ) {
    new ScalingPolicies() {
      @Override
      <T> List<T> list(@Nullable OwnerFullName ownerFullName,
                       @Nonnull Predicate<? super ScalingPolicy> filter,
                       @Nonnull Function<? super ScalingPolicy, T> transform ) {
        policies
            .findAll { policy -> ( ownerFullName==null || policy.ownerAccountNumber.equals( ownerFullName.accountNumber ) ) && filter.apply( policy ) }
            .collect { policy -> transform.apply( policy ) }
      }

      @Override
      <T> T lookup(OwnerFullName ownerFullName,
                   String autoScalingGroupName,
                   String policyName,
                   @Nonnull Function<? super ScalingPolicy, T> transform) {
        ScalingPolicy policy = policies.find { ScalingPolicy policy ->
          policy.displayName.equals( policyName ) &&
          policy.ownerAccountNumber.equals( ownerFullName.accountNumber )
        }
        if ( policy == null ) {
          throw new AutoScalingMetadataNotFoundException("Policy not found: " + policyName)
        }
        transform.apply( policy )
      }

      @Override
      ScalingPolicy update(final OwnerFullName ownerFullName, final String autoScalingGroupName, final String policyName, final Callback<ScalingPolicy> policyUpdateCallback) {
        ScalingPolicy policy = lookup( ownerFullName, autoScalingGroupName, policyName, { it } as Function<ScalingPolicy,ScalingPolicy> )
        policyUpdateCallback.fire( policy )
        policy
      }

      @Override
      boolean delete(final AutoScalingMetadata.ScalingPolicyMetadata scalingPolicy) {
        policies.remove( 0 ) != null
      }

      @Override
      ScalingPolicy save(final ScalingPolicy scalingPolicy) {
        scalingPolicy.setId( "1" )
        scalingPolicy.autoScalingGroupName = scalingPolicy.group.autoScalingGroupName
        policies.add( scalingPolicy )
        scalingPolicy
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
        ScalingActivity activity = lookup( ownerFullName, activityId )
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

  ActivityManager activityManager() {
    new ActivityManager() {
      @Override
      void doScaling() {
      }

      @Override
      CloudWatchClient createCloudWatchClientForUser( final AccountFullName accountFullName ) {
        new TestClients.TesCloudWatchClient( accountFullName, { request ->
          if (request instanceof DescribeAlarmsType ) {
            new DescribeAlarmsResponseType( )
          }
        } as TestClients.RequestHandler )
      }

      @Override
      List<ScalingActivity> terminateInstances( AutoScalingGroupCoreView group,
                                                List<String> instanceIds ) {
        [ new ScalingActivity(
          naturalId: 'b7717740-7246-11e2-bcfd-0800200c9a66',
          autoScalingGroupName: group.autoScalingGroupName,
          statusCode: ActivityStatusCode.InProgress,
          causes: [ new ActivityCause( date( "2013-02-01T12:34:56.789Z"), "Some cause") ],
          creationTimestamp: date( "2013-02-01T12:34:56.789Z" ),
          description: "Description",
          details:  "Details",
          progress: 13
        ) ]
      }

      @Override
      List<String> validateReferences( OwnerFullName owner,
                                       Consumer<? super Map<String,String>> availabilityZoneToSubnetMapConsumer,
                                       Iterable<String> availabilityZones,
                                       Iterable<String> loadBalancerNames,
                                       Iterable<String> targetGroupArns,
                                       Iterable<String> subnetIds ) {
        if ( subnetIds?.iterator( )?.hasNext( ) ) {
          availabilityZoneToSubnetMapConsumer.accept( ImmutableMap.of( subnetIds.iterator().next(), 'zone-1' ) )
        }
        []
      }

      @Override
      List<String> validateReferences( OwnerFullName owner,
                                       Iterable<String> imageIds,
                                       String instanceType,
                                       String keyName,
                                       Iterable<String> securityGroups,
                                       String iamInstanceProfile ) {
        []
      }
    }
  }

  Date date( String timestamp ) {
    Timestamps.parseIso8601Timestamp( timestamp )
  }

  public static class TestAutoScalingGroupTagSupport extends AutoScalingGroupTag.AutoScalingGroupTagSupport {
    @Override
    Map<String, List<Tag>> getResourceTagMap( OwnerFullName owner,
                                              Iterable<String> identifiers,
                                              Predicate<? super Tag> tagPredicate) {
      Map<String,List<Tag>> map = [:]
      identifiers.each{ String id -> map.put( id, [] ) }
      map
    }
  }
}
