package com.eucalyptus.event;

import com.google.common.collect.Lists;


public class EventFailedException extends Exception {
  public EventFailedException( ) {
    super( );
  }

  public EventFailedException( String arg0, Throwable arg1 ) {
    super( arg0, arg1 );
  }

  public EventFailedException( String arg0 ) {
    super( arg0 );
  }

  public EventFailedException( Throwable arg0 ) {
    super( arg0 );
  }

}
