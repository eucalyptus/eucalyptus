package com.eucalyptus.context;

public class ServiceInitializationException extends RuntimeException {

  public ServiceInitializationException( ) {
    super( );
  }

  public ServiceInitializationException( String message, Throwable cause ) {
    super( message, cause );
  }

  public ServiceInitializationException( String message ) {
    super( message );
  }

  public ServiceInitializationException( Throwable cause ) {
    super( cause );
  }

}
