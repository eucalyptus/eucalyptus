package com.eucalyptus.util.fsm;

public class TransitionListenerException extends RuntimeException {

  public TransitionListenerException( ) {
    super( );
  }

  public TransitionListenerException( String message, Throwable cause ) {
    super( message, cause );
  }

  public TransitionListenerException( String message ) {
    super( message );
  }

  public TransitionListenerException( Throwable cause ) {
    super( cause );
  }

}
