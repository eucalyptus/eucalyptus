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
package com.eucalyptus.autoscaling.common

import static org.junit.Assert.*
import org.junit.Test

/**
 *
 */
class AutoScalingMessageValidationTest {

  @Test
  void testValid() {
    CreateAutoScalingGroupType createGroup = new CreateAutoScalingGroupType(
        autoScalingGroupName: 'Name',
        launchConfigurationName: 'arn:aws:autoscaling::244931368026:launchConfiguration:b436181e-1314-4dca-94a1-74a65e813681:launchConfigurationName/Config1:',
        minSize: 1,
        maxSize: 10,
        desiredCapacity: 8,
        defaultCooldown: 73,
        availabilityZones: new AvailabilityZones( member: [ 'PARTI00' ] ),
        loadBalancerNames: new LoadBalancerNames( member: [ 'Balancer1', 'Balancer1', 'Balancer2' ] ),
        healthCheckType: 'EC2',
        healthCheckGracePeriod: 3000,
        terminationPolicies: new TerminationPolicies( member: [ 'Default' ] )
    )

    assertEquals( "Errors", [:], createGroup.validate() )
  }

  @Test
  void testBasicValidation() {
    CreateAutoScalingGroupType createGroup = new CreateAutoScalingGroupType(
        autoScalingGroupName: 'Nam:e',
        launchConfigurationName: 'arn:aws:autoscaling::244931368026:launchConfiguration:b436181e-1314-4dca-94a1-74a65e813681:launchConfigurationName/Config1:',
        minSize: -1,
        maxSize: 10,
        desiredCapacity: 8,
        defaultCooldown: 73,
        availabilityZones: new AvailabilityZones( member: [ ] ),
        loadBalancerNames: new LoadBalancerNames( member: [ 'Balancer1', 'Balancer1', 'Balancer2' ] ),
        healthCheckType: 'EC2',
        healthCheckGracePeriod: 3000,
        terminationPolicies: new TerminationPolicies( member: [ 'Default!!' ] )
    )

    Map<String,String> result = createGroup.validate()
    assertEquals( "Name error", "Nam:e for parameter AutoScalingGroupName is invalid", result["AutoScalingGroupName"] )
    assertEquals( "MinSize error", "-1 for parameter MinSize is invalid", result["MinSize"] )
    assertEquals( "Zone error", "AvailabilityZones.member.1 is required", result["AvailabilityZones.member.1"] )
    assertEquals( "Termination policy error", "Default!! for parameter TerminationPolicies.member.1 is invalid", result["TerminationPolicies.member.1"] )
    assertEquals( "Error count", 4, result.size() )
  }

  @Test
  void testEmbeddedValidation() {
    CreateOrUpdateTagsType createTags = new CreateOrUpdateTagsType(
        tags: new Tags(
            member: [
                new TagType(
                    value: "MyValue"
                )
            ]
        )
    )

    assertEquals( "Embedded validation result", ["Tags.member.1.Key": "Tags.member.1.Key is required"], createTags.validate() );
  }

  @Test
  void testCreateLaunchConfigurationValidation() {
    new CreateLaunchConfigurationType(
        imageId: "emi-00000000",
        instanceType: "m1.small",
        securityGroups: new SecurityGroups(
            member: [ "MyGroup", "sg-00000001" ]
        ),
        launchConfigurationName: "MyLaunchConfiguration"
    ).with { createLaunchConfiguration ->
      assertEquals( "Create launch config invalid groups validation result", ["SecurityGroups.member": "Must use either use group-id or group-name for all the security groups, not both at the same time"], createLaunchConfiguration.validate() );
    }

    new CreateLaunchConfigurationType(
        imageId: "emi-00000000",
        instanceType: "m1.small",
        launchConfigurationName: "MyLaunchConfiguration"
    ).with { createLaunchConfiguration ->
      assertEquals( "Create launch config no groups validation result", [:], createLaunchConfiguration.validate() );
    }

    new CreateLaunchConfigurationType(
        imageId: "emi-00000000",
        instanceType: "m1.small",
        securityGroups: new SecurityGroups(
            member: [ "MyGroup1", "MyGroup2", "MyGroup3", "MyGroup4" ]
        ),
        launchConfigurationName: "MyLaunchConfiguration"
    ).with { createLaunchConfiguration ->
      assertEquals( "Create launch config groups by name validation result", [:], createLaunchConfiguration.validate() );
    }

    new CreateLaunchConfigurationType(
        imageId: "emi-00000000",
        instanceType: "m1.small",
        securityGroups: new SecurityGroups(
            member: [ "sg-00000001", "sg-00000002" ]
        ),
        launchConfigurationName: "MyLaunchConfiguration"
    ).with { createLaunchConfiguration ->
      assertEquals( "Create launch config groups by id validation result", [:], createLaunchConfiguration.validate() );
    }
  }
}
