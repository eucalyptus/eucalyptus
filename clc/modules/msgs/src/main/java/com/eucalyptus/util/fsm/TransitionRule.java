package com.eucalyptus.util.fsm;

import com.eucalyptus.util.Mappable;

public interface TransitionRule<S extends Automata.State, T extends Automata.Transition> extends Mappable<TransitionRule<S, T>, T> {
  
  /**
   * @return the fromState
   */
  public S getFromState( );
  
  /**
   * @return the toState
   */
  public S getToState( );
  
  /**
   * @return the errorState
   */
  public S getErrorState( );
  
  /**
   * @return the fromStateMark
   */
  public Boolean getFromStateMark( );
  
  /**
   * @return the toStateMark
   */
  public Boolean getToStateMark( );
  
  /**
   * @return the errorStateMark
   */
  public Boolean getErrorStateMark( );
}
