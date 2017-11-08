/*************************************************************************
 * Copyright 2009-2015 Ent. Services Development Corporation LP
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
package com.eucalyptus.autoscaling.common;

import com.eucalyptus.auth.policy.annotation.PolicyResourceType;
import com.eucalyptus.auth.policy.annotation.PolicyVendor;
import com.eucalyptus.auth.type.RestrictedType;
import com.eucalyptus.autoscaling.common.policy.AutoScalingPolicySpec;

/**
 *
 */
@PolicyVendor( AutoScalingPolicySpec.VENDOR_AUTOSCALING )
public interface AutoScalingMetadata extends RestrictedType {

  interface AutoScalingMetadataWithResourceName extends AutoScalingMetadata {
    String getArn();
  } 
  
  @PolicyResourceType( "launchconfiguration" )
  interface LaunchConfigurationMetadata extends AutoScalingMetadataWithResourceName {}

  @PolicyResourceType( "autoscalinggroup" )
  interface AutoScalingGroupMetadata extends AutoScalingMetadataWithResourceName {}

  @PolicyResourceType( "terminationpolicytype" )
  interface TerminationPolicyTypeMetadata extends AutoScalingMetadata {}

  @PolicyResourceType( "adjustmenttype" )
  interface AdjustmentTypeMetadata extends AutoScalingMetadata {}

  @PolicyResourceType( "scalingpolicy" )
  interface ScalingPolicyMetadata extends AutoScalingMetadataWithResourceName {}

  @PolicyResourceType( "autoscalinginstance" )
  interface AutoScalingInstanceMetadata extends AutoScalingMetadata {}

  @PolicyResourceType( "tag" )
  interface AutoScalingTagMetadata extends AutoScalingMetadata {}

  @PolicyResourceType( "scalingprocesstype" )
  interface ScalingProcessTypeMetadata extends AutoScalingMetadata {}

  @PolicyResourceType( "scalingactivity" )
  interface ScalingActivityMetadata extends AutoScalingMetadata {}

  @PolicyResourceType( "metriccollectiontype" )
  interface MetricCollectionTypeMetadata extends AutoScalingMetadata {}

}
