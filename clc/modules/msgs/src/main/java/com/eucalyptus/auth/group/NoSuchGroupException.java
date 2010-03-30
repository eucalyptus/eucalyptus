package com.eucalyptus.auth.group;

public class NoSuchGroupException extends Exception {

  public NoSuchGroupException( ) {
    super( );
  }

  public NoSuchGroupException( String message, Throwable cause ) {
    super( message, cause );
  }

  public NoSuchGroupException( String message ) {
    super( message );
  }

  public NoSuchGroupException( Throwable cause ) {
    super( cause );
  }

}
