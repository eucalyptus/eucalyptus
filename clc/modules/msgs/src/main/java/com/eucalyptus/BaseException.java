package com.eucalyptus;

public abstract class BaseException extends Exception {

  public BaseException( ) {
    super( );
  }

  public BaseException( String message, Throwable cause ) {
    super( message, cause );
  }

  public BaseException( String message ) {
    super( message );
  }

  public BaseException( Throwable cause ) {
    super( cause );
  }

}
