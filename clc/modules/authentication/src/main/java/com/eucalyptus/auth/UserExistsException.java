package com.eucalyptus.auth;

public class UserExistsException extends Exception {

  public UserExistsException( ) {
    super( );
  }

  public UserExistsException( String arg0, Throwable arg1 ) {
    super( arg0, arg1 );
  }

  public UserExistsException( String arg0 ) {
    super( arg0 );
  }

  public UserExistsException( Throwable arg0 ) {
    super( arg0 );
  }

}
