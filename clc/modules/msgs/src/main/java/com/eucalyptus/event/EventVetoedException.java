package com.eucalyptus.event;


public class EventVetoedException extends Exception {
  public EventVetoedException( ) {
    super( );
  }

  public EventVetoedException( String arg0, Throwable arg1 ) {
    super( arg0, arg1 );
  }

  public EventVetoedException( String arg0 ) {
    super( arg0 );
  }

  public EventVetoedException( Throwable arg0 ) {
    super( arg0 );
  }

}
