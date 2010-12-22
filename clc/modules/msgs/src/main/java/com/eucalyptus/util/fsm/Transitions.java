package com.eucalyptus.util.fsm;

import org.apache.log4j.Logger;
import com.eucalyptus.util.HasName;

public class Transitions {
  private static Logger                        LOG  = Logger.getLogger( Transitions.class );
                                                    
  public static <P extends HasName<P>, S extends Enum<S>, T extends Enum<T>> Transition<P, S, T> create( T name, S fromState, S toState, S errorState, TransitionAction<P> action, TransitionListener<P>... listeners ) {
    TransitionRule<S, T> rule = new BasicTransitionRule<S, T>( name, fromState, toState );
    return new Transition<P, S, T>( rule, action, listeners );
  }
  
  static class BasicTransitionRule<S extends Enum<S>, T extends Enum<T>> implements TransitionRule<S, T> {
    private T             name;
    private S             fromState;
    private S             toState;
    private S             errorState;
    private final Boolean fromStateMark;
    private final Boolean toStateMark;
    private final Boolean errorStateMark;
    
    protected BasicTransitionRule( T name, S fromState, S toState ) {
      this.name = name;
      this.fromState = fromState;
      this.toState = toState;
      this.errorState = fromState;
      this.fromStateMark = false;
      this.toStateMark = false;
      this.errorStateMark = false;
    }

    protected BasicTransitionRule( T name, S fromState, S toState, S errorState ) {
      this.name = name;
      this.fromState = fromState;
      this.toState = toState;
      this.errorState = errorState;
      this.fromStateMark = false;
      this.toStateMark = false;
      this.errorStateMark = false;
    }
    
    protected BasicTransitionRule( T name, S fromState, Boolean fromStateMark, S toState, Boolean toStateMark, S errorState, Boolean errorStateMark ) {
      this.name = name;
      this.fromState = fromState;
      this.toState = toState;
      this.errorState = errorState;
      this.fromStateMark = fromStateMark;
      this.toStateMark = toStateMark;
      this.errorStateMark = errorStateMark;
    }
    
    @Override
    public final T getName( ) {
      return this.name;
    }
    
    @Override
    public final S getFromState( ) {
      return this.fromState;
    }
    
    @Override
    public final S getToState( ) {
      return this.toState;
    }
    
    @Override
    public final S getErrorState( ) {
      return this.errorState;
    }
    
    @Override
    public final Boolean getFromStateMark( ) {
      return this.fromStateMark;
    }
    
    @Override
    public final Boolean getToStateMark( ) {
      return this.toStateMark;
    }
    
    @Override
    public final Boolean getErrorStateMark( ) {
      return this.errorStateMark;
    }
    
    @Override
    public int compareTo( TransitionRule<S, T> that ) {
      return this.getName( ).compareTo( that.getName( ) );
    }

  }
  
}
