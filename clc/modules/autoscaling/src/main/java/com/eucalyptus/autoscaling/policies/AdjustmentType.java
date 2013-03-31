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

import static com.eucalyptus.autoscaling.common.AutoScalingMetadata.AdjustmentTypeMetadata;
import com.eucalyptus.auth.principal.Principals;
import com.eucalyptus.util.OwnerFullName;

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
