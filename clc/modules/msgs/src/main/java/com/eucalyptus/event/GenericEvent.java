package com.eucalyptus.event;

public class GenericEvent<T> extends Event {
  private T message;
  
  public GenericEvent( ) {
    super( );
  }


  public GenericEvent( T message ) {
    super( );
    this.message = message;
  }
  
  public GenericEvent<T> setMessage( T message ) {
    this.message = message;
    return this;
  }

  public T getMessage( ) {
    return message;
  }

  public boolean matches( T myMessage ) {
    return this.message.equals( message );
  }
  
}
