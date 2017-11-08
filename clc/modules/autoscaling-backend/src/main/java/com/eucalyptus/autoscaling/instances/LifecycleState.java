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
