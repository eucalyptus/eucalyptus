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

import static org.junit.Assert.*
import org.junit.Test
import com.eucalyptus.autoscaling.configurations.LaunchConfigurations
import com.eucalyptus.autoscaling.configurations.LaunchConfiguration
import com.eucalyptus.util.OwnerFullName
import com.google.common.base.Predicate
import com.eucalyptus.context.Context
import edu.ucsb.eucalyptus.msgs.BaseMessage
import com.eucalyptus.auth.Accounts
import com.eucalyptus.auth.api.AccountProvider
import com.eucalyptus.auth.principal.Account
import com.eucalyptus.auth.principal.User
import java.security.cert.X509Certificate
import com.eucalyptus.auth.principal.Group
import com.eucalyptus.auth.principal.Certificate
import com.eucalyptus.auth.principal.AccessKey
import com.eucalyptus.auth.principal.Principals
import edu.ucsb.eucalyptus.cloud.NotImplementedException
import com.google.common.collect.Lists
import com.eucalyptus.context.Contexts
import com.eucalyptus.util.TypeMappers
import com.eucalyptus.autoscaling.groups.AutoScalingGroups
import com.eucalyptus.autoscaling.groups.AutoScalingGroup
import com.eucalyptus.util.Callback

/**
 * 
 */
class AutoScalingServiceTest {
  
  @Test
  void testLaunchConfigurations() {
    Accounts.setAccountProvider( accountProvider() )
    TypeMappers.TypeMapperDiscovery discovery = new TypeMappers.TypeMapperDiscovery()
    discovery.processClass( LaunchConfigurations.BlockDeviceTransform.class )
    discovery.processClass( LaunchConfigurations.LaunchConfigurationTransform.class )
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
    TypeMappers.TypeMapperDiscovery discovery = new TypeMappers.TypeMapperDiscovery()
    discovery.processClass( AutoScalingGroups.AutoScalingGroupTransform.class )
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
    assertEquals( "Availability zones", [ "zone-1" ] as Set, group.availabilityZones.member as Set )
    assertNotNull( "Load balancer names", group.loadBalancerNames )
    assertEquals( "Load balancer names", [ "balancer-1", "balancer-2" ] as Set, group.loadBalancerNames.member as Set )
    assertNotNull( "Termination policies", group.terminationPolicies )
    assertEquals( "Termination policies", [ "NewestInstance", "Default" ] as Set, group.terminationPolicies.member as Set )

    service.deleteAutoScalingGroup( new DeleteAutoScalingGroupType( autoScalingGroupName: "Test" ) )

    assertEquals( "Groups count", 0,
        service.describeAutoScalingGroups( new DescribeAutoScalingGroupsType() )
            .describeAutoScalingGroupsResult.autoScalingGroups.member.size() )

    service.deleteLaunchConfiguration( new DeleteLaunchConfigurationType( launchConfigurationName: "TestLaunch" ) )

    assertEquals( "Configuration count", 0,
        service.describeLaunchConfigurations( new DescribeLaunchConfigurationsType() )
            .describeLaunchConfigurationsResult.launchConfigurations.member.size() )
  }  
  
  AutoScalingService service() {
    new AutoScalingService( launchConfigurationStore(), autoScalingGroupStore() )
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
  
  AutoScalingGroups autoScalingGroupStore() {
    List<AutoScalingGroup> groups = []

    new AutoScalingGroups() {
      @Override
      List<AutoScalingGroup> list(OwnerFullName ownerFullName) {
        groups.findAll { group -> group.ownerAccountNumber.equals( ownerFullName.accountNumber ) }
      }

      @Override
      List<AutoScalingGroup> list(OwnerFullName ownerFullName, Predicate<? super AutoScalingGroup> filter) {
        groups.findAll { group -> filter.apply( group ) } as List
      }

      @Override
      AutoScalingGroup lookup(OwnerFullName ownerFullName, String autoScalingGroupName) {
        groups.find { AutoScalingGroup group ->
          group.getClass().getMethod("getDisplayName").invoke( group ).equals( autoScalingGroupName ) && // work around some groovy metaclass issue
              group.getClass().getMethod("getOwnerAccountNumber").invoke( group ).equals( ownerFullName.accountNumber )
        }
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
}
