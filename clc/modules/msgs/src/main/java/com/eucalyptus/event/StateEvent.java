package com.eucalyptus.event;

public class StateEvent<T,E extends Enum<E>> extends GenericEvent<T> {
  E state;  
  public StateEvent( E state ) {
    super( );
    this.state = state;
  }
  public StateEvent( E state, T message ) {
    super( message );
    this.state = state;
  }
  public E getState( ) {
    return state;
  }
  public StateEvent<T,E> setState( E state ) {
    this.state = state;
    return this;
  }
}
