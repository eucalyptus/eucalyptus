/*************************************************************************
 * Copyright 2009-2014 Eucalyptus Systems, Inc.
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
package com.eucalyptus.autoscaling.service.ws

import com.eucalyptus.autoscaling.common.msgs.AutoScalingGroupNames
import com.eucalyptus.autoscaling.common.msgs.AvailabilityZones
import com.eucalyptus.autoscaling.common.msgs.BlockDeviceMappingType
import com.eucalyptus.autoscaling.common.msgs.BlockDeviceMappings
import com.eucalyptus.autoscaling.common.msgs.CreateAutoScalingGroupType
import com.eucalyptus.autoscaling.common.msgs.CreateLaunchConfigurationType
import com.eucalyptus.autoscaling.common.msgs.DeleteAutoScalingGroupType
import com.eucalyptus.autoscaling.common.msgs.DeleteLaunchConfigurationType
import com.eucalyptus.autoscaling.common.msgs.DeletePolicyType
import com.eucalyptus.autoscaling.common.msgs.DescribeAdjustmentTypesType
import com.eucalyptus.autoscaling.common.msgs.DescribeAutoScalingGroupsType
import com.eucalyptus.autoscaling.common.msgs.DescribeLaunchConfigurationsType
import com.eucalyptus.autoscaling.common.msgs.DescribePoliciesType
import com.eucalyptus.autoscaling.common.msgs.DescribeTagsType
import com.eucalyptus.autoscaling.common.msgs.DescribeTerminationPolicyTypesType
import com.eucalyptus.autoscaling.common.msgs.Ebs
import com.eucalyptus.autoscaling.common.msgs.ExecutePolicyType
import com.eucalyptus.autoscaling.common.msgs.Filter
import com.eucalyptus.autoscaling.common.msgs.Filters
import com.eucalyptus.autoscaling.common.msgs.InstanceMonitoring
import com.eucalyptus.autoscaling.common.msgs.LaunchConfigurationNames
import com.eucalyptus.autoscaling.common.msgs.LoadBalancerNames
import com.eucalyptus.autoscaling.common.msgs.PolicyNames
import com.eucalyptus.autoscaling.common.msgs.PutScalingPolicyType
import com.eucalyptus.autoscaling.common.msgs.SecurityGroups
import com.eucalyptus.autoscaling.common.msgs.SetDesiredCapacityType
import com.eucalyptus.autoscaling.common.msgs.TerminationPolicies
import com.eucalyptus.autoscaling.common.msgs.UpdateAutoScalingGroupType
import com.eucalyptus.autoscaling.common.msgs.Values
import com.eucalyptus.ws.protocol.QueryBindingTestSupport
import edu.ucsb.eucalyptus.msgs.BaseMessage
import org.junit.Test

/**
 *
 */
class AutoScalingBindingTest extends QueryBindingTestSupport {

  @Test
  void testValidBinding() {
    URL resource = AutoScalingBindingTest.class.getResource( '/autoscaling-binding.xml' )
    assertValidBindingXml( resource )
  }

  @Test
  void testValidQueryBinding() {
    URL resource = AutoScalingBindingTest.class.getResource( '/autoscaling-binding.xml' )
    assertValidQueryBinding( resource )
  }

  @Test
  void testMessageQueryBindings() {
    URL resource = AutoScalingBindingTest.class.getResource( '/autoscaling-binding.xml' )

    AutoScalingQueryBinding asb = new AutoScalingQueryBinding() {
      @Override
      protected com.eucalyptus.binding.Binding getBindingWithElementClass( final String operationName ) {
        createTestBindingFromXml( resource, operationName )
      }

      @Override
      protected void validateBinding( final com.eucalyptus.binding.Binding currentBinding,
                                      final String operationName,
                                      final Map<String, String> params,
                                      final BaseMessage eucaMsg) {
        // Validation requires compiled bindings
      }
    }

    // CreateAutoScalingGroup
    bindAndAssertObject( asb, CreateAutoScalingGroupType.class, "CreateAutoScalingGroup", new CreateAutoScalingGroupType(
        autoScalingGroupName: 'Name',
        launchConfigurationName: 'LaunchName',
        minSize: 1,
        maxSize: 10,
        desiredCapacity: 8,
        defaultCooldown: 73,
        availabilityZones: new AvailabilityZones( member: [ 'Zone1', 'Zone2' ] ),
        loadBalancerNames: new LoadBalancerNames( member: [ 'Balancer1', 'Balancer1', 'Balancer2' ] ),
        healthCheckType: 'EC2',
        healthCheckGracePeriod: 3000,
        terminationPolicies: new TerminationPolicies( member: [ 'Default' ] )
    ), 14 )

    // CreateLaunchConfiguration
    bindAndAssertObject( asb, CreateLaunchConfigurationType.class, "CreateLaunchConfiguration", new CreateLaunchConfigurationType(
        launchConfigurationName: 'LaunchName',
        imageId: 'emi-01234567',
        keyName: 'KeyName',
        securityGroups: new SecurityGroups( member: [ 'Group1', 'Group2' ] ),
        userData: 'UserData',
        instanceType: 'm1.small',
        kernelId: 'eki-76543210',
        ramdiskId: 'eri-76543210',
        blockDeviceMappings: new BlockDeviceMappings( member: [
            new BlockDeviceMappingType( deviceName: '/dev/sdf', virtualName: 'ephemeral0' ),
            new BlockDeviceMappingType( deviceName: '/dev/sdh', ebs: new Ebs( volumeSize: 12, snapshotId: 'snap-00000001' ) ) ] ),
        instanceMonitoring: new InstanceMonitoring( enabled: true )
    ), 15 )

    // DeleteAutoScalingGroup
    bindAndAssertObject( asb, DeleteAutoScalingGroupType.class, "DeleteAutoScalingGroup", new DeleteAutoScalingGroupType(
        autoScalingGroupName: 'Name',
        forceDelete: true
    ), 2 )

    // DeleteLaunchConfiguration
    bindAndAssertObject( asb, DeleteLaunchConfigurationType.class, "DeleteLaunchConfiguration", new DeleteLaunchConfigurationType(
        launchConfigurationName: 'LaunchName',
    ), 1 )

    // DeletePolicy
    bindAndAssertObject( asb, DeletePolicyType.class, "DeletePolicy", new DeletePolicyType(
        autoScalingGroupName: 'Name',
        policyName: 'PolicyName',
    ), 2 )

    // DescribeAdjustmentTypes
    bindAndAssertObject( asb, DescribeAdjustmentTypesType.class, "DescribeAdjustmentTypes", new DescribeAdjustmentTypesType(
    ), 0 )

    // DescribeAutoScalingGroups
    bindAndAssertObject( asb, DescribeAutoScalingGroupsType.class, "DescribeAutoScalingGroups", new DescribeAutoScalingGroupsType(
        autoScalingGroupNames: new AutoScalingGroupNames( member: [ 'Name1', 'Name5', 'Name2' ] ),
        nextToken: "NextToken",
        maxRecords: 123
    ), 5 )

    // DescribeLaunchConfigurations
    bindAndAssertObject( asb, DescribeLaunchConfigurationsType.class, "DescribeLaunchConfigurations", new DescribeLaunchConfigurationsType(
        launchConfigurationNames: new LaunchConfigurationNames( member: [ 'Name1', 'Name5', 'Name2' ] ),
        nextToken: "NextToken",
        maxRecords: 123
    ), 5 )

    // DescribePolicies
    bindAndAssertObject( asb, DescribePoliciesType.class, "DescribePolicies", new DescribePoliciesType(
        autoScalingGroupName: 'GroupName',
        policyNames: new PolicyNames( member: [ 'Name1', 'Name5', 'Name2' ] ),
        nextToken: "NextToken",
        maxRecords: 123
    ), 6 )

    // DescribeTerminationPolicyTypes
    bindAndAssertObject( asb, DescribeTerminationPolicyTypesType.class, "DescribeTerminationPolicyTypes", new DescribeTerminationPolicyTypesType(
    ), 0 )

    // ExecutePolicy
    bindAndAssertObject( asb, ExecutePolicyType.class, "ExecutePolicy", new ExecutePolicyType(
        autoScalingGroupName: 'GroupName',
        policyName: 'PolicyName',
        honorCooldown: true
    ), 3 )

    // PutScalingPolicy
    bindAndAssertObject( asb, PutScalingPolicyType.class, "PutScalingPolicy", new PutScalingPolicyType(
        autoScalingGroupName: 'GroupName',
        policyName: 'PolicyName',
        scalingAdjustment: 2,
        adjustmentType: 'ChangeInCapacity',
        cooldown: 345,
        minAdjustmentStep: 4
    ), 6 )

    // SetDesiredCapacity
    bindAndAssertObject( asb, SetDesiredCapacityType.class, "SetDesiredCapacity", new SetDesiredCapacityType(
        autoScalingGroupName: 'GroupName',
        desiredCapacity: 3,
        honorCooldown: true
    ), 3 )

    // UpdateAutoScalingGroup
    bindAndAssertObject( asb, UpdateAutoScalingGroupType.class, "UpdateAutoScalingGroup", new UpdateAutoScalingGroupType(
        autoScalingGroupName: 'GroupName',
        launchConfigurationName: 'LaunchName',
        minSize: 2,
        maxSize: 7,
        desiredCapacity: 5,
        defaultCooldown: 123,
        availabilityZones: new AvailabilityZones( member: [ 'C', 'B', '1' ] ),
        healthCheckType: 'ELB',
        healthCheckGracePeriod: 4534,
        terminationPolicies: new TerminationPolicies( member: [ 'Default' ] )
    ), 12 )

    // DescribeTags
    bindAndAssertObject( asb, DescribeTagsType.class, "DescribeTags", new DescribeTagsType(
        filters: new Filters(
            member: [
                new Filter(
                    name: "key",
                    values: new Values(
                        member: [
                            "TestKey"
                        ]
                    )
                )
            ],
        )
    ), 2 )
  }
}
