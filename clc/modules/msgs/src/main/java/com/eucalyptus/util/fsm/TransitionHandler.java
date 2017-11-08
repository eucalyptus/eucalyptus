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

import com.eucalyptus.util.HasName;

public interface TransitionHandler<P extends HasName<P>, S extends Automata.State, T extends Automata.Transition> extends TransitionRule<S, T>, TransitionAction<P> {
  
  /**
   * @see com.eucalyptus.util.fsm.TransitionRule#getName()
   */
  public abstract T getName( );
  
  /**
   * @return the action
   */
  public abstract TransitionAction<P> getAction( );
  
  /**
   * Add a transition listener. The stages of the transition will execute for
   * each listener in the order it was added:
   * <ol>
   * <li>{@link TransitionListener#before()}</li>
   * <li>{@link TransitionListener#leave()}</li>
   * <li>{@link TransitionListener#enter()}</li>
   * <li>{@link TransitionListener#after()}</li>
   * </ol>
   * 
   * @param transitionListener
   */
  public abstract TransitionHandler<P, S, T> addListener( TransitionListener<P> listener );
  
  /**
   * @return the rule
   */
  public abstract TransitionRule<S, T> getRule( );
  
  public abstract String toString( );
  
  /**
   * @see java.lang.Object#hashCode()
   * @return
   */
  public abstract int hashCode( );
  
  /**
   * @see java.lang.Object#equals(java.lang.Object)
   * @param obj
   * @return
   */
  public abstract boolean equals( Object obj );
  
}
