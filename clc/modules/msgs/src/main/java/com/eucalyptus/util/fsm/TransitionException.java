package com.eucalyptus.util.fsm;

public class TransitionException extends RuntimeException {
  
  public TransitionException( ) {}
  
  public TransitionException( String message ) {
    super( message );
  }
  
  public TransitionException( Throwable cause ) {
    super( cause );
  }
  
  public TransitionException( String message, Throwable cause ) {
    super( message, cause );
  }
  
}
