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
}
