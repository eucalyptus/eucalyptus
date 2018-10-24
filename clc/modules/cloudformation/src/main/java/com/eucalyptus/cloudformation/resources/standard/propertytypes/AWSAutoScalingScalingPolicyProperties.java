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
package com.eucalyptus.cloudformation.resources.standard.propertytypes;

import com.eucalyptus.cloudformation.resources.ResourceProperties;
import com.eucalyptus.cloudformation.resources.annotations.Property;
import com.eucalyptus.cloudformation.resources.annotations.Required;
import com.google.common.base.MoreObjects;

public class AWSAutoScalingScalingPolicyProperties implements ResourceProperties {

  @Required
  @Property
  private String adjustmentType;

  @Required
  @Property
  private String autoScalingGroupName;

  @Property
  private Integer cooldown;

  @Required
  @Property
  private Integer scalingAdjustment;

  public String getAdjustmentType( ) {
    return adjustmentType;
  }

  public void setAdjustmentType( String adjustmentType ) {
    this.adjustmentType = adjustmentType;
  }

  public String getAutoScalingGroupName( ) {
    return autoScalingGroupName;
  }

  public void setAutoScalingGroupName( String autoScalingGroupName ) {
    this.autoScalingGroupName = autoScalingGroupName;
  }

  public Integer getCooldown( ) {
    return cooldown;
  }

  public void setCooldown( Integer cooldown ) {
    this.cooldown = cooldown;
  }

  public Integer getScalingAdjustment( ) {
    return scalingAdjustment;
  }

  public void setScalingAdjustment( Integer scalingAdjustment ) {
    this.scalingAdjustment = scalingAdjustment;
  }

  @Override
  public String toString( ) {
    return MoreObjects.toStringHelper( this )
        .add( "adjustmentType", adjustmentType )
        .add( "autoScalingGroupName", autoScalingGroupName )
        .add( "cooldown", cooldown )
        .add( "scalingAdjustment", scalingAdjustment )
        .toString( );
  }
}
