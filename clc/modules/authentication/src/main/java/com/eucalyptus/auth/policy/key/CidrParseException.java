package com.eucalyptus.auth.policy.key;

public class CidrParseException extends Exception {

  private static final long serialVersionUID = 1L;

  public CidrParseException( ) {
    super( );
  }
  
  public CidrParseException( String message ) {
    super( message );
  }
  
  public CidrParseException( Throwable cause ) {
    super( cause );
  }
  
  public CidrParseException( String message, Throwable cause ) {
    super( message, cause );
  }
  
}
