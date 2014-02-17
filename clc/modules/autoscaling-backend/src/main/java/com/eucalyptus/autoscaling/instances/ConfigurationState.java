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
public enum ConfigurationState implements Predicate<AutoScalingInstance> {

  /**
   * No related external configuration
   */
  Nihil,

  /**
   * Related EC2 instance exists
   */
  Instantiated,

  /**
   * Registered with ELB(s)
   */
  Registered,
  ;

  /**
   * State matching predicate.
   *
   * @param instance The instance to match
   * @return True if the instance is in this state
   */
  @Override
  public boolean apply( @Nullable final AutoScalingInstance instance ) {
    return instance != null && this == instance.getConfigurationState();
  }

  /**
   * Get a Predicate for view matching.
   */
  public Predicate<AutoScalingInstanceCoreView> forView() {
    return new Predicate<AutoScalingInstanceCoreView>() {
      @Override
      public boolean apply( @Nullable final AutoScalingInstanceCoreView instance ) {
        return instance != null && ConfigurationState.this == instance.getConfigurationState();
      }
    };
  }

  /**
   * A predicate to transition from one state to another.
   *
   * <p>The state of a given instance is transitioned if the state of the
   * instance matches this configuration state. The predicate returns true if
   * the state was changed.</p>
   *
   * @param to The state to transition to
   * @return The transition function
   */
  public Predicate<AutoScalingInstance> transitionTo( final ConfigurationState to ) {
    return new ConfigurationStateTransition( this, to );
  }

  private static final class ConfigurationStateTransition implements Predicate<AutoScalingInstance> {
    private final ConfigurationState from;
    private final ConfigurationState to;

    private ConfigurationStateTransition( final ConfigurationState from,
                                          final ConfigurationState to ) {
      this.from = from;
      this.to = to;
    }

    @Override
    public boolean apply( final AutoScalingInstance instance ) {
      boolean transitioned = false;
      if ( from.apply( instance ) ) {
        instance.setConfigurationState( to );
        instance.setRegistrationAttempts( 0 );
        transitioned = true;
      }
      return transitioned;
    }
  }
}
