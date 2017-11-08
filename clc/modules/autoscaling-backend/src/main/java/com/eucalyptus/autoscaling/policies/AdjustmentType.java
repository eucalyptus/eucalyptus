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
package com.eucalyptus.autoscaling.policies;

import static com.eucalyptus.autoscaling.common.AutoScalingMetadata.AdjustmentTypeMetadata;
import com.eucalyptus.auth.principal.Principals;
import com.eucalyptus.auth.principal.OwnerFullName;

/**
 *
 */
public enum AdjustmentType implements AdjustmentTypeMetadata {

  /**
   * Change capacity by the value of adjustmentStep
   */
  ChangeInCapacity {
    @Override
    public int adjustCapacity( final int currentCapacity, 
                               final int adjustmentStep, 
                               final int minAdjustment ) {
      return currentCapacity + adjustmentStep;
    }
  },

  /**
   * Change capacity to the value of adjustmentStep
   */
  ExactCapacity {
    @Override
    public int adjustCapacity( final int currentCapacity, 
                               final int adjustmentStep, 
                               final int minAdjustment ) {
      return adjustmentStep;
    }
  },

  /**
   * Change capacity by a percentage defined by adjustmentStep.
   * 
   * If the change is "less" than the minAdjustment then minAdjustment is used
   * instead.
   */
  PercentChangeInCapacity {
    @Override
    public int adjustCapacity( final int currentCapacity, 
                               final int adjustmentStep, 
                               final int minAdjustment ) {
      final double multiplier = Math.abs(adjustmentStep) / 100d;
      final int adjustment = Math.max( 
          (int) Math.floor( currentCapacity * multiplier ), 
          Math.max( Math.abs( minAdjustment ), 1 ) );
      return currentCapacity + (adjustment * (adjustmentStep < 0 ? -1 : 1));
    }
  };

  @Override
  public String getDisplayName() {
    return name();
  }

  @Override
  public OwnerFullName getOwner() {
    return Principals.systemFullName();
  }

  public final int adjustCapacity( final int currentCapacity,
                                   final int adjustmentStep,
                                   final int minAdjustment,
                                   final int minSize,
                                   final int maxSize ) {
    int adjusted = adjustCapacity( currentCapacity, adjustmentStep, minAdjustment );
    adjusted = Math.min( adjusted, maxSize );        
    adjusted = Math.max( adjusted, minSize );        
    return adjusted;
  }

  public abstract int adjustCapacity( final int currentCapacity,
                                      final int adjustmentStep,                                      
                                      final int minAdjustment );
  
}
