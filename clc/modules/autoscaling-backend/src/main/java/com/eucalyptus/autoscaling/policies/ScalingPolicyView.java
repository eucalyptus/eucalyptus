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
package com.eucalyptus.autoscaling.policies;

import com.eucalyptus.autoscaling.common.AutoScalingMetadata;
import com.eucalyptus.autoscaling.groups.AutoScalingGroup;
import com.eucalyptus.util.OwnerFullName;

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
