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
public enum LifecycleState implements Predicate<AutoScalingInstance> {

  /**
   * Instance is launching
   */
  Pending,

  /**
   * Not currently used
   */
  Quarantined,

  /**
   * Instance is running
   */
  InService,

  /**
   * Instance is terminating
   */
  Terminating,

  /**
   * Not currently used
   */
  Terminated;

  /**
   * State matching predicate.
   *
   * @param instance The instance to match
   * @return True if the instance is in this state
   */
  @Override
  public boolean apply( @Nullable final AutoScalingInstance instance ) {
    return instance != null && this == instance.getLifecycleState();
  }

  /**
   * Get a Predicate for view matching.
   */
  public Predicate<AutoScalingInstanceCoreView> forView() {
    return new Predicate<AutoScalingInstanceCoreView>() {
      @Override
      public boolean apply( @Nullable final AutoScalingInstanceCoreView instance ) {
        return instance != null && LifecycleState.this == instance.getLifecycleState();
      }
    };
  }

  /**
   * A predicate to transition from one state to another.
   *
   * <p>The state of a given instance is transitioned if the state of the
   * instance matches this lifecycle state. The predicate returns true if the
   * state was changed.</p>
   *
   * @param to The state to transition to
   * @return The transition function
   */
  public Predicate<AutoScalingInstance> transitionTo( final LifecycleState to ) {
    return new LifecycleStateTransition( this, to );
  }

  private static final class LifecycleStateTransition implements Predicate<AutoScalingInstance> {
    private final LifecycleState from;
    private final LifecycleState to;

    private LifecycleStateTransition( final LifecycleState from,
                                      final LifecycleState to ) {
      this.from = from;
      this.to = to;
    }

    @Override
    public boolean apply( final AutoScalingInstance instance ) {
      boolean transitioned = false;
      if ( from.apply( instance ) ) {
        instance.setLifecycleState( to );
        transitioned = true;
      }
      return transitioned;
    }
  }
}
