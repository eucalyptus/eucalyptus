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
@InheritConstructors class DescribePoliciesResponseType extends com.eucalyptus.autoscaling.common.msgs.DescribePoliciesResponseType implements AutoScalingBackendMessage { }
@InheritConstructors class DescribePoliciesType extends com.eucalyptus.autoscaling.common.msgs.DescribePoliciesType implements AutoScalingBackendMessage { }
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
