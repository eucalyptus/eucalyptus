/*************************************************************************
 * Copyright 2009-2012 Eucalyptus Systems, Inc.
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

package com.eucalyptus.util.fsm;

import com.eucalyptus.util.Callback;
import com.eucalyptus.util.HasName;

/**
 * Simple top/bottom half transition. Delegates
 * {@link TransitionListener#leave()} to {@link #top()} and
 * {@link TransitionListener#enter()} to {@link #bottom()}.
 *
 * @param <T>
 * @param <S>
 */
public abstract class SplitTransition<P extends HasName<P>, S extends Enum<S>> extends AbstractTransitionAction<P> {
  
  @Override
  public final boolean before( P parent ) {
    return true;
  }
  
  /**
   * @see com.eucalyptus.util.fsm.TransitionListener#leave()
   */
  @Override
  public abstract void leave( P parent, Callback.Completion transitionCallback );
  
  /**
   * @see com.eucalyptus.util.fsm.TransitionListener#enter()
   */
  @Override
  public abstract void enter( P parent );
  
  @Override
  public final void after( P parent ) {}
  
}
