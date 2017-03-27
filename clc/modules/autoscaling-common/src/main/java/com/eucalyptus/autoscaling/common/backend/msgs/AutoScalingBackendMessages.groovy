/*************************************************************************
 * Copyright 2009-2014 Eucalyptus Systems, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY without even the implied warranty of
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
@GroovyAddClassUUID
package com.eucalyptus.autoscaling.common.backend.msgs

import com.eucalyptus.autoscaling.common.AutoScalingBackend
import edu.ucsb.eucalyptus.msgs.BaseMessageMarker
import com.eucalyptus.component.annotation.ComponentMessage
import groovy.transform.InheritConstructors

import edu.ucsb.eucalyptus.msgs.GroovyAddClassUUID


@ComponentMessage(AutoScalingBackend)
interface AutoScalingBackendMessage extends BaseMessageMarker { }

@InheritConstructors class CreateAutoScalingGroupResponseType extends com.eucalyptus.autoscaling.common.msgs.CreateAutoScalingGroupResponseType implements AutoScalingBackendMessage { }
@InheritConstructors class CreateAutoScalingGroupType extends com.eucalyptus.autoscaling.common.msgs.CreateAutoScalingGroupType implements AutoScalingBackendMessage { }
@InheritConstructors class CreateLaunchConfigurationResponseType extends com.eucalyptus.autoscaling.common.msgs.CreateLaunchConfigurationResponseType implements AutoScalingBackendMessage { }
@InheritConstructors class CreateLaunchConfigurationType extends com.eucalyptus.autoscaling.common.msgs.CreateLaunchConfigurationType implements AutoScalingBackendMessage { }
@InheritConstructors class CreateOrUpdateTagsResponseType extends com.eucalyptus.autoscaling.common.msgs.CreateOrUpdateTagsResponseType implements AutoScalingBackendMessage { }
@InheritConstructors class CreateOrUpdateTagsType extends com.eucalyptus.autoscaling.common.msgs.CreateOrUpdateTagsType implements AutoScalingBackendMessage { }
@InheritConstructors class DeleteAutoScalingGroupResponseType extends com.eucalyptus.autoscaling.common.msgs.DeleteAutoScalingGroupResponseType implements AutoScalingBackendMessage { }
@InheritConstructors class DeleteAutoScalingGroupType extends com.eucalyptus.autoscaling.common.msgs.DeleteAutoScalingGroupType implements AutoScalingBackendMessage { }
@InheritConstructors class DeleteLaunchConfigurationResponseType extends com.eucalyptus.autoscaling.common.msgs.DeleteLaunchConfigurationResponseType implements AutoScalingBackendMessage { }
@InheritConstructors class DeleteLaunchConfigurationType extends com.eucalyptus.autoscaling.common.msgs.DeleteLaunchConfigurationType implements AutoScalingBackendMessage { }
@InheritConstructors class DeletePolicyResponseType extends com.eucalyptus.autoscaling.common.msgs.DeletePolicyResponseType implements AutoScalingBackendMessage { }
@InheritConstructors class DeletePolicyType extends com.eucalyptus.autoscaling.common.msgs.DeletePolicyType implements AutoScalingBackendMessage { }
@InheritConstructors class DeleteTagsResponseType extends com.eucalyptus.autoscaling.common.msgs.DeleteTagsResponseType implements AutoScalingBackendMessage { }
@InheritConstructors class DeleteTagsType extends com.eucalyptus.autoscaling.common.msgs.DeleteTagsType implements AutoScalingBackendMessage { }
@InheritConstructors class DescribeAdjustmentTypesResponseType extends com.eucalyptus.autoscaling.common.msgs.DescribeAdjustmentTypesResponseType implements AutoScalingBackendMessage { }
@InheritConstructors class DescribeAdjustmentTypesType extends com.eucalyptus.autoscaling.common.msgs.DescribeAdjustmentTypesType implements AutoScalingBackendMessage { }
@InheritConstructors class DescribeAutoScalingGroupsResponseType extends com.eucalyptus.autoscaling.common.msgs.DescribeAutoScalingGroupsResponseType implements AutoScalingBackendMessage { }
@InheritConstructors class DescribeAutoScalingGroupsType extends com.eucalyptus.autoscaling.common.msgs.DescribeAutoScalingGroupsType implements AutoScalingBackendMessage { }
@InheritConstructors class DescribeAutoScalingInstancesResponseType extends com.eucalyptus.autoscaling.common.msgs.DescribeAutoScalingInstancesResponseType implements AutoScalingBackendMessage { }
@InheritConstructors class DescribeAutoScalingInstancesType extends com.eucalyptus.autoscaling.common.msgs.DescribeAutoScalingInstancesType implements AutoScalingBackendMessage { }
@InheritConstructors class DescribeLaunchConfigurationsResponseType extends com.eucalyptus.autoscaling.common.msgs.DescribeLaunchConfigurationsResponseType implements AutoScalingBackendMessage { }
@InheritConstructors class DescribeLaunchConfigurationsType extends com.eucalyptus.autoscaling.common.msgs.DescribeLaunchConfigurationsType implements AutoScalingBackendMessage { }
@InheritConstructors class DescribeMetricCollectionTypesResponseType extends com.eucalyptus.autoscaling.common.msgs.DescribeMetricCollectionTypesResponseType implements AutoScalingBackendMessage { }
@InheritConstructors class DescribeMetricCollectionTypesType extends com.eucalyptus.autoscaling.common.msgs.DescribeMetricCollectionTypesType implements AutoScalingBackendMessage { }
@InheritConstructors class DescribePoliciesResponseType extends com.eucalyptus.autoscaling.common.msgs.DescribePoliciesResponseType implements AutoScalingBackendMessage { }
@InheritConstructors class DescribePoliciesType extends com.eucalyptus.autoscaling.common.msgs.DescribePoliciesType implements AutoScalingBackendMessage { }
@InheritConstructors class DescribeScalingActivitiesResponseType extends com.eucalyptus.autoscaling.common.msgs.DescribeScalingActivitiesResponseType implements AutoScalingBackendMessage { }
@InheritConstructors class DescribeScalingActivitiesType extends com.eucalyptus.autoscaling.common.msgs.DescribeScalingActivitiesType implements AutoScalingBackendMessage { }
@InheritConstructors class DescribeScalingProcessTypesResponseType extends com.eucalyptus.autoscaling.common.msgs.DescribeScalingProcessTypesResponseType implements AutoScalingBackendMessage { }
@InheritConstructors class DescribeScalingProcessTypesType extends com.eucalyptus.autoscaling.common.msgs.DescribeScalingProcessTypesType implements AutoScalingBackendMessage { }
@InheritConstructors class DescribeTagsResponseType extends com.eucalyptus.autoscaling.common.msgs.DescribeTagsResponseType implements AutoScalingBackendMessage { }
@InheritConstructors class DescribeTagsType extends com.eucalyptus.autoscaling.common.msgs.DescribeTagsType implements AutoScalingBackendMessage { }
@InheritConstructors class DescribeTerminationPolicyTypesResponseType extends com.eucalyptus.autoscaling.common.msgs.DescribeTerminationPolicyTypesResponseType implements AutoScalingBackendMessage { }
@InheritConstructors class DescribeTerminationPolicyTypesType extends com.eucalyptus.autoscaling.common.msgs.DescribeTerminationPolicyTypesType implements AutoScalingBackendMessage { }
@InheritConstructors class DisableMetricsCollectionResponseType extends com.eucalyptus.autoscaling.common.msgs.DisableMetricsCollectionResponseType implements AutoScalingBackendMessage { }
@InheritConstructors class DisableMetricsCollectionType extends com.eucalyptus.autoscaling.common.msgs.DisableMetricsCollectionType implements AutoScalingBackendMessage { }
@InheritConstructors class EnableMetricsCollectionResponseType extends com.eucalyptus.autoscaling.common.msgs.EnableMetricsCollectionResponseType implements AutoScalingBackendMessage { }
@InheritConstructors class EnableMetricsCollectionType extends com.eucalyptus.autoscaling.common.msgs.EnableMetricsCollectionType implements AutoScalingBackendMessage { }
@InheritConstructors class ExecutePolicyResponseType extends com.eucalyptus.autoscaling.common.msgs.ExecutePolicyResponseType implements AutoScalingBackendMessage { }
@InheritConstructors class ExecutePolicyType extends com.eucalyptus.autoscaling.common.msgs.ExecutePolicyType implements AutoScalingBackendMessage { }
@InheritConstructors class PutScalingPolicyResponseType extends com.eucalyptus.autoscaling.common.msgs.PutScalingPolicyResponseType implements AutoScalingBackendMessage { }
@InheritConstructors class PutScalingPolicyType extends com.eucalyptus.autoscaling.common.msgs.PutScalingPolicyType implements AutoScalingBackendMessage { }
@InheritConstructors class ResumeProcessesResponseType extends com.eucalyptus.autoscaling.common.msgs.ResumeProcessesResponseType implements AutoScalingBackendMessage { }
@InheritConstructors class ResumeProcessesType extends com.eucalyptus.autoscaling.common.msgs.ResumeProcessesType implements AutoScalingBackendMessage { }
@InheritConstructors class SetDesiredCapacityResponseType extends com.eucalyptus.autoscaling.common.msgs.SetDesiredCapacityResponseType implements AutoScalingBackendMessage { }
@InheritConstructors class SetDesiredCapacityType extends com.eucalyptus.autoscaling.common.msgs.SetDesiredCapacityType implements AutoScalingBackendMessage { }
@InheritConstructors class SetInstanceHealthResponseType extends com.eucalyptus.autoscaling.common.msgs.SetInstanceHealthResponseType implements AutoScalingBackendMessage { }
@InheritConstructors class SetInstanceHealthType extends com.eucalyptus.autoscaling.common.msgs.SetInstanceHealthType implements AutoScalingBackendMessage { }
@InheritConstructors class SetInstanceProtectionResponseType extends com.eucalyptus.autoscaling.common.msgs.SetInstanceProtectionResponseType implements AutoScalingBackendMessage { }
@InheritConstructors class SetInstanceProtectionType extends com.eucalyptus.autoscaling.common.msgs.SetInstanceProtectionType implements AutoScalingBackendMessage { }
@InheritConstructors class SuspendProcessesResponseType extends com.eucalyptus.autoscaling.common.msgs.SuspendProcessesResponseType implements AutoScalingBackendMessage { }
@InheritConstructors class SuspendProcessesType extends com.eucalyptus.autoscaling.common.msgs.SuspendProcessesType implements AutoScalingBackendMessage { }
@InheritConstructors class TerminateInstanceInAutoScalingGroupResponseType extends com.eucalyptus.autoscaling.common.msgs.TerminateInstanceInAutoScalingGroupResponseType implements AutoScalingBackendMessage { }
@InheritConstructors class TerminateInstanceInAutoScalingGroupType extends com.eucalyptus.autoscaling.common.msgs.TerminateInstanceInAutoScalingGroupType implements AutoScalingBackendMessage { }
@InheritConstructors class UpdateAutoScalingGroupResponseType extends com.eucalyptus.autoscaling.common.msgs.UpdateAutoScalingGroupResponseType implements AutoScalingBackendMessage { }
@InheritConstructors class UpdateAutoScalingGroupType extends com.eucalyptus.autoscaling.common.msgs.UpdateAutoScalingGroupType implements AutoScalingBackendMessage { }
