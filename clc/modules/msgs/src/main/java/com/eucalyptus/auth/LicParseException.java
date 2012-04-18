package com.eucalyptus.auth;

public class LicParseException extends Exception {

  private static final long serialVersionUID = 1L;
  
  public static final String EMPTY_LIC = "Empty configuration";
  public static final String SYNTAX_ERROR = "Syntax error in configuration";

  public LicParseException( ) {
    super( );
  }
  
  public LicParseException( String message, Throwable cause ) {
    super( message, cause );
  }
  
  public LicParseException( String message ) {
    super( message );
  }
  
  public LicParseException( Throwable cause ) {
    super( cause );
  }
  
}
