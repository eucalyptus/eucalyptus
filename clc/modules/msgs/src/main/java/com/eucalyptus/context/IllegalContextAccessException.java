package com.eucalyptus.context;

public class IllegalContextAccessException extends RuntimeException {

  public IllegalContextAccessException( ) {
    super( );
  }

  public IllegalContextAccessException( String message, Throwable cause ) {
    super( message, cause );
  }

  public IllegalContextAccessException( String message ) {
    super( message );
  }

  public IllegalContextAccessException( Throwable cause ) {
    super( cause );
  }

}
