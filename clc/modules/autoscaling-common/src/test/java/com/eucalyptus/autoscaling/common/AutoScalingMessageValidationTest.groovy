/*************************************************************************
 * Copyright 2009-2013 Ent. Services Development Corporation LP
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
package com.eucalyptus.autoscaling.common

import com.eucalyptus.autoscaling.common.msgs.AvailabilityZones
import com.eucalyptus.autoscaling.common.msgs.CreateAutoScalingGroupType
import com.eucalyptus.autoscaling.common.msgs.CreateLaunchConfigurationType
import com.eucalyptus.autoscaling.common.msgs.CreateOrUpdateTagsType
import com.eucalyptus.autoscaling.common.msgs.LoadBalancerNames
import com.eucalyptus.autoscaling.common.msgs.SecurityGroups
import com.eucalyptus.autoscaling.common.msgs.TagType
import com.eucalyptus.autoscaling.common.msgs.Tags
import com.eucalyptus.autoscaling.common.msgs.TerminationPolicies

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
        availabilityZones: new AvailabilityZones( member: [ 'PARTI00' ] as ArrayList<String> ),
        loadBalancerNames: new LoadBalancerNames( member: [ 'Balancer1', 'Balancer1', 'Balancer2' ] as ArrayList<String> ),
        healthCheckType: 'EC2',
        healthCheckGracePeriod: 3000,
        terminationPolicies: new TerminationPolicies( member: [ 'Default' ] as ArrayList<String> )
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
        availabilityZones: new AvailabilityZones( member: [ ] as ArrayList<String> ),
        loadBalancerNames: new LoadBalancerNames( member: [ 'Balancer1', 'Balancer1', 'Balancer2' ] as ArrayList<String> ),
        healthCheckType: 'EC2',
        healthCheckGracePeriod: 3000,
        terminationPolicies: new TerminationPolicies( member: [ 'Default!!' ] as ArrayList<String> )
    )

    Map<String,String> result = createGroup.validate()
    assertEquals( "Name error", "'Nam:e' for parameter AutoScalingGroupName is invalid", result["AutoScalingGroupName"] )
    assertEquals( "MinSize error", "-1 for parameter MinSize is invalid", result["MinSize"] )
    assertEquals( "Zone error", "One of AvailabilityZones or VPCZoneIdentifier is required", result["AvailabilityZones.member.1"] )
    assertEquals( "Termination policy error", "'Default!!' for parameter TerminationPolicies.member.1 is invalid", result["TerminationPolicies.member.1"] )
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
            ] as ArrayList<TagType>
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
            member: [ "MyGroup", "sg-00000001" ] as ArrayList<String>
        ),
        launchConfigurationName: "MyLaunchConfiguration"
    ).with { CreateLaunchConfigurationType createLaunchConfiguration ->
      assertEquals( "Create launch config invalid groups validation result", ["SecurityGroups.member": "Must use either use group-id or group-name for all the security groups, not both at the same time"], createLaunchConfiguration.validate() );
    }

    new CreateLaunchConfigurationType(
        imageId: "emi-00000000",
        instanceType: "m1.small",
        launchConfigurationName: "MyLaunchConfiguration"
    ).with { CreateLaunchConfigurationType createLaunchConfiguration ->
      assertEquals( "Create launch config no groups validation result", [:], createLaunchConfiguration.validate() );
    }

    new CreateLaunchConfigurationType(
        imageId: "emi-00000000",
        instanceType: "m1.small",
        securityGroups: new SecurityGroups(
            member: [ "MyGroup1", "MyGroup2", "MyGroup3", "MyGroup4" ] as ArrayList<String>
        ),
        launchConfigurationName: "MyLaunchConfiguration"
    ).with { CreateLaunchConfigurationType createLaunchConfiguration ->
      assertEquals( "Create launch config groups by name validation result", [:], createLaunchConfiguration.validate() );
    }

    new CreateLaunchConfigurationType(
        imageId: "emi-00000000",
        instanceType: "m1.small",
        securityGroups: new SecurityGroups(
            member: [ "sg-00000001", "sg-00000002" ] as ArrayList<String>
        ),
        launchConfigurationName: "MyLaunchConfiguration"
    ).with { CreateLaunchConfigurationType createLaunchConfiguration ->
      assertEquals( "Create launch config groups by id validation result", [:], createLaunchConfiguration.validate() );
    }
  }
}
