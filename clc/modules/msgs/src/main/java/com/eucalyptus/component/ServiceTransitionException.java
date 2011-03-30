package com.eucalyptus.component;

public class ServiceTransitionException extends RuntimeException {
  
  public ServiceTransitionException( ) {}
  
  public ServiceTransitionException( String message ) {
    super( message );
  }
  
  public ServiceTransitionException( Throwable cause ) {
    super( cause );
  }
  
  public ServiceTransitionException( String message, Throwable cause ) {
    super( message, cause );
  }
  
}
