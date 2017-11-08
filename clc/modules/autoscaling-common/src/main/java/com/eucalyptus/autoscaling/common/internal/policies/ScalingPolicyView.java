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
package com.eucalyptus.autoscaling.common.internal.policies;

import com.eucalyptus.autoscaling.common.AutoScalingMetadata;
import com.eucalyptus.autoscaling.common.internal.groups.AutoScalingGroup;
import com.eucalyptus.auth.principal.OwnerFullName;

/**
 * Immutable view of a scaling policy
 */
public class ScalingPolicyView implements AutoScalingMetadata.ScalingPolicyMetadata {

  private final ScalingPolicy scalingPolicy;

  public ScalingPolicyView( final ScalingPolicy scalingPolicy ) {
    this.scalingPolicy = scalingPolicy;
  }

  @Override
  public String getDisplayName() {
    return scalingPolicy.getPolicyName();
  }

  public String getPolicyName() {
    return scalingPolicy.getPolicyName();
  }

  @Override
  public String getArn() {
    return scalingPolicy.getArn();
  }

  public String getAutoScalingGroupName() {
    return scalingPolicy.getAutoScalingGroupName();
  }

  public AdjustmentType getAdjustmentType() {
    return scalingPolicy.getAdjustmentType();
  }

  public Integer getCooldown() {
    return scalingPolicy.getCooldown();
  }

  public AutoScalingGroup getGroup() {
    return scalingPolicy.getGroup();
  }

  public Integer getMinAdjustmentStep() {
    return scalingPolicy.getMinAdjustmentStep();
  }

  public Integer getScalingAdjustment() {
    return scalingPolicy.getScalingAdjustment();
  }

  public String getOwnerAccountNumber() {
    return scalingPolicy.getOwnerAccountNumber();
  }

  @Override
  public OwnerFullName getOwner() {
    return scalingPolicy.getOwner();
  }

}
