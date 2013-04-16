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
package com.eucalyptus.autoscaling.instances;

import javax.annotation.Nullable;
import com.google.common.base.Predicate;

/**
 *
 */
public enum HealthStatus implements Predicate<AutoScalingInstance> {

  /**
   * Instance is healthy
   */
  Healthy,

  /**
   * Instance is unhealthy
   */
  Unhealthy,
  ;

  /**
   * State matching predicate.
   *
   * @param instance The instance to match
   * @return True if the instance is in this state
   */
  @Override
  public boolean apply( @Nullable final AutoScalingInstance instance ) {
    return instance != null && this == instance.getHealthStatus();
  }

  /**
   * Get a Predicate for view matching.
   */
  public Predicate<AutoScalingInstanceCoreView> forView() {
    return new Predicate<AutoScalingInstanceCoreView>() {
      @Override
      public boolean apply( @Nullable final AutoScalingInstanceCoreView instance ) {
        return instance != null && HealthStatus.this == instance.getHealthStatus();
      }
    };
  }
}
