package com.eucalyptus.event;

public abstract class PassiveEventListener<T> implements EventListener {

  @Override
  public final void advertiseEvent( Event event ) {}

  @Override
  public final void fireEvent( Event event ) {
    if( event instanceof GenericEvent ) {
      try {
        T msg = ( T ) ( ( GenericEvent ) event ).getMessage( );
        this.firingEvent( msg );
      } catch ( ClassCastException e ) {
      }
    }
  }
  
  public abstract void firingEvent( T t );
  
  
}
