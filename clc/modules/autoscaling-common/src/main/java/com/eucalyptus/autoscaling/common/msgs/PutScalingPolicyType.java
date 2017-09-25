/*************************************************************************
 * (c) Copyright 2017 Hewlett Packard Enterprise Development Company LP
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
 ************************************************************************/
package com.eucalyptus.autoscaling.common.msgs;

import javax.annotation.Nonnull;
import com.eucalyptus.autoscaling.common.AutoScalingMessageValidation;

public class PutScalingPolicyType extends AutoScalingMessage {

  @Nonnull
  @AutoScalingMessageValidation.FieldRegex( AutoScalingMessageValidation.FieldRegexValue.NAME_OR_ARN )
  private String autoScalingGroupName;
  @Nonnull
  @AutoScalingMessageValidation.FieldRegex( AutoScalingMessageValidation.FieldRegexValue.NAME )
  private String policyName;
  @Nonnull
  private Integer scalingAdjustment;
  @Nonnull
  @AutoScalingMessageValidation.FieldRegex( AutoScalingMessageValidation.FieldRegexValue.ADJUSTMENT )
  private String adjustmentType;
  @AutoScalingMessageValidation.FieldRange
  private Integer cooldown;
  private Integer minAdjustmentStep;

  public String getAutoScalingGroupName( ) {
    return autoScalingGroupName;
  }

  public void setAutoScalingGroupName( String autoScalingGroupName ) {
    this.autoScalingGroupName = autoScalingGroupName;
  }

  public String getPolicyName( ) {
    return policyName;
  }

  public void setPolicyName( String policyName ) {
    this.policyName = policyName;
  }

  public Integer getScalingAdjustment( ) {
    return scalingAdjustment;
  }

  public void setScalingAdjustment( Integer scalingAdjustment ) {
    this.scalingAdjustment = scalingAdjustment;
  }

  public String getAdjustmentType( ) {
    return adjustmentType;
  }

  public void setAdjustmentType( String adjustmentType ) {
    this.adjustmentType = adjustmentType;
  }

  public Integer getCooldown( ) {
    return cooldown;
  }

  public void setCooldown( Integer cooldown ) {
    this.cooldown = cooldown;
  }

  public Integer getMinAdjustmentStep( ) {
    return minAdjustmentStep;
  }

  public void setMinAdjustmentStep( Integer minAdjustmentStep ) {
    this.minAdjustmentStep = minAdjustmentStep;
  }
}
