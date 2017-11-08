/*************************************************************************
 * Copyright 2008 Regents of the University of California
 * Copyright 2009-2012 Ent. Services Development Corporation LP
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
 * POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 * THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 * COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 * AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 * IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 * SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 * WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 * REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 * IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 * NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/

package com.eucalyptus.util.fsm;

import com.eucalyptus.util.Callback;
import com.eucalyptus.util.HasName;
import com.eucalyptus.util.Callback.Completion;

/**
 * <p>
 * A callback interface which is invoked according to the progress of a transitions between two
 * states: {@fromState} is the state before the transition and {@toState} is the state
 * after the transition. The primary purpose of implementing classes is to enforce pre-conditions
 * and apply side effects corresponding with the change of state. The expectation is that
 * transitions are applied atomically in the sense that only one can be in progress at a time.
 * </p>
 * <p>
 * The methods are invoked in the following order; their purpose is summarized here with details in
 * the documentation for each method:
 * <ol>
 * <li>{@link #before()}: prior to any change of state -- check preconditions.</li>
 * <li>{@link #leave()}: on leaving {@code fromState}.</li>
 * <li>{@link #enter()}: on entering {@code toState}.</li>
 * <li>
 * <li>{@link #after(toState)}: after the transition has been completed.</li>
 * </ol>
 * </p>
 * 
 * @author decker
 * 
 * @param <S> enum type of the states in the state machine.
 */
public interface TransitionAction<P extends HasName<P>> {
  /**
   * Invoked before leaving the {@code fromState}. At this time the transition
   * has not yet begun and can be aborted. A return of false or a caught
   * exception will stop application of the transition.
   * 
   * Implementors should ensure to avoid side-effects.
   * 
   * @return false iff the transition should not be executed.
   */
  public abstract boolean before( P parent );
  
  /**
   * Applies changes corresponding with leaving {@code fromState} and <strong>signals completion
   * using the supplied {@link Callback.Completion}</strong>. This method is invoked
   * when the transition begins and after the state has been changed to {@code toState} but before
   * any side effects have been applied.
   * 
   * Implementors can assume that no other transitions will execute until this
   * transition has completed.
   */
  public abstract void leave( P parent, Completion transitionCallback );
  
  /**
   * Applies changes corresponding with having entered {@code toState} and is
   * invoked when the transition completes leaving the state as {@code toState}.
   * 
   * Implementors can assume that no other transitions will execute until this
   * transition has completed.
   */
  public abstract void enter( P parent );
  
  /**
   * Invoked after the transition completes but before any other transition is
   * evaluated against the underlying state machine.
   * 
   * Implementors should ensure to avoid side-effects.
   */
  public abstract void after( P parent );
  
}
