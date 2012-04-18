package com.eucalyptus.auth;

/**
 * Exception related to policy.
 * 
 * @author wenye
 *
 */
public class PolicyParseException extends Exception {

  private static final long serialVersionUID = 1L;

  public static final String EMPTY_POLICY = "Empty input policy text";
  public static final String SIZE_TOO_LARGE = "Policy size is too large";
  public static final String SYNTAX_ERROR = "Policy has syntax error";
  
  public PolicyParseException( ) {
    super( );
  }
  
  public PolicyParseException( String message, Throwable cause ) {
    super( message, cause );
  }
  
  public PolicyParseException( String message ) {
    super( message );
  }
  
  public PolicyParseException( Throwable cause ) {
    super( cause );
  }
  
}
