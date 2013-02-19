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
package com.eucalyptus.autoscaling

import com.eucalyptus.auth.Accounts
import com.eucalyptus.auth.api.AccountProvider
import com.eucalyptus.auth.principal.AccessKey
import com.eucalyptus.auth.principal.Account
import com.eucalyptus.auth.principal.Certificate
import com.eucalyptus.auth.principal.Group
import com.eucalyptus.auth.principal.Principals
import com.eucalyptus.auth.principal.User
import com.eucalyptus.autoscaling.activities.ActivityManager
import com.eucalyptus.autoscaling.activities.ActivityStatusCode
import com.eucalyptus.autoscaling.activities.ScalingActivities
import com.eucalyptus.autoscaling.activities.ScalingActivity
import com.eucalyptus.autoscaling.common.Activity
import com.eucalyptus.autoscaling.common.AutoScalingGroupNames
import com.eucalyptus.autoscaling.common.AutoScalingGroupType
import com.eucalyptus.autoscaling.common.AutoScalingInstanceDetails
import com.eucalyptus.autoscaling.common.AvailabilityZones
import com.eucalyptus.autoscaling.common.BlockDeviceMappingType
import com.eucalyptus.autoscaling.common.BlockDeviceMappings
import com.eucalyptus.autoscaling.common.CreateAutoScalingGroupType
import com.eucalyptus.autoscaling.common.CreateLaunchConfigurationType
import com.eucalyptus.autoscaling.common.DeleteAutoScalingGroupType
import com.eucalyptus.autoscaling.common.DeleteLaunchConfigurationType
import com.eucalyptus.autoscaling.common.DeletePolicyType
import com.eucalyptus.autoscaling.common.DescribeAutoScalingGroupsResponseType
import com.eucalyptus.autoscaling.common.DescribeAutoScalingGroupsType
import com.eucalyptus.autoscaling.common.DescribeAutoScalingInstancesResponseType
import com.eucalyptus.autoscaling.common.DescribeAutoScalingInstancesType
import com.eucalyptus.autoscaling.common.DescribeLaunchConfigurationsResponseType
import com.eucalyptus.autoscaling.common.DescribeLaunchConfigurationsType
import com.eucalyptus.autoscaling.common.DescribePoliciesResponseType
import com.eucalyptus.autoscaling.common.DescribePoliciesType
import com.eucalyptus.autoscaling.common.ExecutePolicyType
import com.eucalyptus.autoscaling.common.InstanceMonitoring
import com.eucalyptus.autoscaling.common.LaunchConfigurationNames
import com.eucalyptus.autoscaling.common.LaunchConfigurationType
import com.eucalyptus.autoscaling.common.LoadBalancerNames
import com.eucalyptus.autoscaling.common.PolicyNames
import com.eucalyptus.autoscaling.common.PutScalingPolicyType
import com.eucalyptus.autoscaling.common.ScalingPolicyType
import com.eucalyptus.autoscaling.common.SecurityGroups
import com.eucalyptus.autoscaling.common.TerminateInstanceInAutoScalingGroupResponseType
import com.eucalyptus.autoscaling.common.TerminateInstanceInAutoScalingGroupType
import com.eucalyptus.autoscaling.common.TerminationPolicies
import com.eucalyptus.autoscaling.configurations.LaunchConfiguration
import com.eucalyptus.autoscaling.configurations.LaunchConfigurations
import com.eucalyptus.autoscaling.groups.AutoScalingGroup
import com.eucalyptus.autoscaling.groups.AutoScalingGroups
import com.eucalyptus.autoscaling.groups.HealthCheckType
import com.eucalyptus.autoscaling.instances.AutoScalingInstance
import com.eucalyptus.autoscaling.instances.AutoScalingInstances
import com.eucalyptus.autoscaling.instances.HealthStatus
import com.eucalyptus.autoscaling.instances.LifecycleState
import com.eucalyptus.autoscaling.metadata.AutoScalingMetadataNotFoundException
import com.eucalyptus.autoscaling.policies.ScalingPolicies
import com.eucalyptus.autoscaling.policies.ScalingPolicy
import com.eucalyptus.context.Context
import com.eucalyptus.context.Contexts
import com.eucalyptus.crypto.util.Timestamps
import com.eucalyptus.util.Callback
import com.eucalyptus.util.OwnerFullName
import com.eucalyptus.util.TypeMappers
import com.google.common.base.Predicate
import com.google.common.collect.Lists
import edu.ucsb.eucalyptus.cloud.NotImplementedException
import edu.ucsb.eucalyptus.msgs.BaseMessage
import static org.junit.Assert.*
import org.junit.BeforeClass
import org.junit.Test

import java.security.cert.X509Certificate

/**
 * 
 */
@SuppressWarnings("GroovyAccessibility")
class AutoScalingServiceTest {
  
  @BeforeClass
  static void before() {
    TypeMappers.TypeMapperDiscovery discovery = new TypeMappers.TypeMapperDiscovery()
    discovery.processClass( AutoScalingGroups.AutoScalingGroupTransform.class )
    discovery.processClass( AutoScalingInstances.AutoScalingInstanceSummaryTransform.class )
    discovery.processClass( AutoScalingInstances.AutoScalingInstanceTransform.class )
    discovery.processClass( LaunchConfigurations.BlockDeviceTransform.class )
    discovery.processClass( LaunchConfigurations.LaunchConfigurationTransform.class )
    discovery.processClass( ScalingActivities.ScalingActivityTransform.class )
    discovery.processClass( ScalingPolicies.ScalingPolicyTransform.class )
  }
  
  @Test
  void testLaunchConfigurations() {
    Accounts.setAccountProvider( accountProvider() )
    AutoScalingService service = service()
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
    Accounts.setAccountProvider( accountProvider() )
    AutoScalingService service = service()
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
      placementGroup: "placementgroup",
      terminationPolicies: new TerminationPolicies( member: [ "NewestInstance", "Default" ] ),
      vpcZoneIdentifier: "vpc-1",      
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
    Accounts.setAccountProvider( accountProvider() )
    AutoScalingService service = service()
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
    Accounts.setAccountProvider( accountProvider() )
    AutoScalingService service = service( launchConfigurationStore(), autoScalingGroupStore(), autoScalingInstanceStore( [
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
    Accounts.setAccountProvider( accountProvider() )
    AutoScalingGroup group;
    AutoScalingService service = service( launchConfigurationStore(), autoScalingGroupStore( [
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
      terminateInstanceInAutoScalingGroupResponseType.terminateInstanceInAutoScalingGroupResult.activity;

    assertNotNull( "Activity", activity )
    assertEquals( "Activity identifier", 'b7717740-7246-11e2-bcfd-0800200c9a66', activity.activityId )
    assertEquals( "Activity group name", "Group1", activity.autoScalingGroupName )
    assertEquals( "Activity cause", "Some cause", activity.cause )
    assertEquals( "Activity start time", date( "2013-02-01T12:34:56.789Z" ), activity.startTime )
    assertEquals( "Activity status code", "InProgress", activity.statusCode )
    assertEquals( "Activity description", "Description", activity.description )
    assertEquals( "Activity details", "Details", activity.details )
    assertEquals( "Activity progress", 13, activity.progress )
  }

  AutoScalingService service( 
      launchConfigurationStore = launchConfigurationStore(),
      autoScalingGroupStore = autoScalingGroupStore(),
      autoScalingInstanceStore = autoScalingInstanceStore(),
      scalingPolicyStore = scalingPolicyStore(),
      activityManager = activityManager()
  ) {
    new AutoScalingService( 
        launchConfigurationStore,         
        autoScalingGroupStore, 
        autoScalingInstanceStore,
        scalingPolicyStore,
        activityManager )
  }
  
  AccountProvider accountProvider() {
    new AccountProvider() {
      @Override
      Account lookupAccountByName(final String accountName) {
        throw new NotImplementedException()
      }

      @Override
      Account lookupAccountById(final String accountId) {
        Principals.systemAccount( )
      }

      @Override
      Account addAccount(final String accountName) {
        throw new NotImplementedException()
      }

      @Override
      void deleteAccount(final String accountName, final boolean forceDeleteSystem, final boolean recursive) {
        throw new NotImplementedException()
      }

      @Override
      List<Account> listAllAccounts() {
        throw new NotImplementedException()
      }

      @Override
      Set<String> resolveAccountNumbersForName(final String accountNAmeLike) {
        [] as Set
      }

      @Override
      List<User> listAllUsers() {
        throw new NotImplementedException()
      }

      @Override
      boolean shareSameAccount(final String userId1, final String userId2) {
        throw new NotImplementedException()
      }

      @Override
      User lookupUserById(final String userId) {
        throw new NotImplementedException()
      }

      @Override
      User lookupUserByAccessKeyId(final String keyId) {
        throw new NotImplementedException()
      }

      @Override
      User lookupUserByCertificate(final X509Certificate cert) {
        throw new NotImplementedException()
      }

      @Override
      User lookupUserByConfirmationCode(final String code) {
        throw new NotImplementedException()
      }

      @Override
      Group lookupGroupById(final String groupId) {
        throw new NotImplementedException()
      }

      @Override
      Certificate lookupCertificate(final X509Certificate cert) {
        throw new NotImplementedException()
      }

      @Override
      AccessKey lookupAccessKeyById(final String keyId) {
        throw new NotImplementedException()
      }

      @Override
      User lookupUserByName(final String userName) {
        throw new NotImplementedException()
      }
    }
  }
  
  LaunchConfigurations launchConfigurationStore() {
    List<LaunchConfiguration> configurations = []
    
    new LaunchConfigurations() {
      @Override
      List<LaunchConfiguration> list(OwnerFullName ownerFullName) {
        configurations.findAll { configuration -> configuration.ownerAccountNumber.equals( ownerFullName.accountNumber ) } 
      }

      @Override
      List<LaunchConfiguration> list(OwnerFullName ownerFullName, Predicate<? super LaunchConfiguration> filter) {
        configurations.findAll { configuration -> filter.apply( configuration ) } as List
      }

      @Override
      LaunchConfiguration lookup(OwnerFullName ownerFullName, String launchConfigurationName) {
        configurations.find { LaunchConfiguration configuration ->
          configuration.getClass().getMethod("getDisplayName").invoke( configuration ).equals( launchConfigurationName ) &&  // work around some groovy metaclass issue
              configuration.getClass().getMethod("getOwnerAccountNumber").invoke( configuration ).equals( ownerFullName.accountNumber ) 
        }
      }

      @Override
      boolean delete(LaunchConfiguration launchConfiguration) {
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
      List<AutoScalingGroup> list(OwnerFullName ownerFullName) {
        groups.findAll { group -> group.getClass().getMethod("getOwnerAccountNumber").invoke( group ).equals( ownerFullName.accountNumber ) }
      }

      @Override
      List<AutoScalingGroup> list(OwnerFullName ownerFullName, Predicate<? super AutoScalingGroup> filter) {
        list( ownerFullName ).findAll { group -> filter.apply( group ) } as List
      }

      @Override
      List<AutoScalingGroup> listRequiringScaling() {
        []
      }

      @Override
      List<AutoScalingGroup> listRequiringInstanceReplacement() {
        []
      }

      @Override
      List<AutoScalingGroup> listRequiringMonitoring(long interval) {
        []
      }

      @Override
      AutoScalingGroup lookup(OwnerFullName ownerFullName, String autoScalingGroupName) {
        AutoScalingGroup group = groups.find { AutoScalingGroup group ->
          group.getClass().getMethod("getArn").invoke( group ).equals( autoScalingGroupName ) ||
          ( group.getClass().getMethod("getDisplayName").invoke( group ).equals( autoScalingGroupName ) && // work around some groovy metaclass issue
            group.getClass().getMethod("getOwnerAccountNumber").invoke( group ).equals( ownerFullName.accountNumber ) )
        }
        if ( group == null ) {
          throw new AutoScalingMetadataNotFoundException("Group not found: " + autoScalingGroupName)
        }        
        group
      }

      @Override
      AutoScalingGroup update(OwnerFullName ownerFullName, 
                              String autoScalingGroupName, 
                              Callback<AutoScalingGroup> groupUpdateCallback) {
        AutoScalingGroup group = lookup( ownerFullName, autoScalingGroupName )
        groupUpdateCallback.fire( group )
        group 
      }

      @Override
      boolean delete(AutoScalingGroup autoScalingGroup) {
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
      List<AutoScalingInstance> list(OwnerFullName ownerFullName) {
        ownerFullName == null ?
          instances :
          instances.findAll { instance -> instance.getClass().getMethod("getOwnerAccountNumber").invoke( instance ).equals( ownerFullName.accountNumber ) }
      }

      @Override
      List<AutoScalingInstance> list(OwnerFullName ownerFullName, Predicate<? super AutoScalingInstance> filter) {
        list( ownerFullName ).findAll { instance -> filter.apply( instance ) } as List
      }

      @Override
      List<AutoScalingInstance> listByGroup(OwnerFullName ownerFullName, String groupName) {
        list( ownerFullName, { AutoScalingInstance instance -> groupName.equals(instance.autoScalingGroupName) } as Predicate )
      }

      @Override
      List<AutoScalingInstance> listByGroup(AutoScalingGroup group) {
        listByGroup( 
            (OwnerFullName)group.getClass().getMethod("getOwner").invoke( group ), 
            (String)group.getClass().getMethod("getAutoScalingGroupName").invoke( group ) )
      }

      @Override
      List<AutoScalingInstance> listUnhealthyByGroup( AutoScalingGroup group ) {
        []
      }

      @Override
      AutoScalingInstance lookup(OwnerFullName ownerFullName, String instanceId) {
        list( ownerFullName ).find { instance -> 
          instance.getClass().getMethod("getInstanceId").invoke( instance ).equals( instanceId ) 
        }
      }

      @Override
      AutoScalingInstance update(OwnerFullName ownerFullName, 
                                 String instanceId, 
                                 Callback<AutoScalingInstance> instanceUpdateCallback) {
        AutoScalingInstance instance = lookup( ownerFullName, instanceId )
        instanceUpdateCallback.fire( instance )
        instance
      }

      @Override
      void markMissingInstancesUnhealthy(AutoScalingGroup group, 
                                         Collection<String> instanceIds) {
      }

      @Override
      boolean delete(AutoScalingInstance autoScalingInstance) {
        instances.remove( 0 ) != null
      }

      @Override
      boolean deleteByGroup(AutoScalingGroup group) {
        instances.removeAll( instances.findAll { instance -> group.autoScalingGroupName.equals( instance.autoScalingGroupName ) } )
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
      List<ScalingPolicy> list(final OwnerFullName ownerFullName) {
        policies.findAll { policy -> policy.ownerAccountNumber.equals( ownerFullName.accountNumber ) }
      }

      @Override
      List<ScalingPolicy> list(final OwnerFullName ownerFullName, final Predicate<? super ScalingPolicy> filter) {
        policies.findAll { policy -> filter.apply( policy ) } as List
      }

      @Override
      ScalingPolicy lookup(final OwnerFullName ownerFullName, final String autoScalingGroupName, final String policyName) {
        ScalingPolicy policy = policies.find { ScalingPolicy policy ->
          policy.getClass().getMethod("getDisplayName").invoke( policy ).equals( policyName ) && // work around some groovy metaclass issue
          policy.getClass().getMethod("getOwnerAccountNumber").invoke( policy ).equals( ownerFullName.accountNumber )
        }
        if ( policy == null ) {
          throw new AutoScalingMetadataNotFoundException("Policy not found: " + policyName)  
        }
        policy
      }

      @Override
      ScalingPolicy update(final OwnerFullName ownerFullName, final String autoScalingGroupName, final String policyName, final Callback<ScalingPolicy> policyUpdateCallback) {
        ScalingPolicy policy = lookup( ownerFullName, autoScalingGroupName, policyName )
        policyUpdateCallback.fire( policy )
        policy
      }

      @Override
      boolean delete(final ScalingPolicy scalingPolicy) {
        policies.remove( 0 ) != null
      }

      @Override
      ScalingPolicy save(final ScalingPolicy scalingPolicy) {
        scalingPolicy.setId( "1" )
        policies.add( scalingPolicy )
        scalingPolicy
      }
    }
  }
  
  ActivityManager activityManager() {
    new ActivityManager() {
      @Override
      void doScaling() {
      }

      @Override
      List<ScalingActivity> terminateInstances( AutoScalingGroup group, 
                                                Collection<AutoScalingInstance> instances ) {
        [ new ScalingActivity(
          naturalId: 'b7717740-7246-11e2-bcfd-0800200c9a66',
          group: group,
          autoScalingGroupName: group.getClass().getMethod("getAutoScalingGroupName").invoke( group ),
          activityStatusCode: ActivityStatusCode.InProgress,
          cause: "Some cause",
          creationTimestamp: date( "2013-02-01T12:34:56.789Z" ),
          description: "Description",
          details:  "Details",
          progress: 13
        ) ];
      }

      @Override
      List<String> validateReferences( OwnerFullName owner,
                                       Collection<String> availabilityZones) {
        []
      }

      @Override
      List<String> validateReferences( OwnerFullName owner,
                                       Iterable<String> imageIds,
                                       String keyName,
                                       Iterable<String> securityGroups) {
        []
      }
    }
  }
  
  Date date( String timestamp ) {
    Timestamps.parseIso8601Timestamp( timestamp )        
  }
}
