package com.eucalyptus.auth;

/**
 * Exception related to policy.
 * 
 * @author wenye
 *
 */
public class PolicyException extends Exception {

  private static final long serialVersionUID = 1L;

  public static final String EMPTY_POLICY = "Empty input policy text";
  public static final String SIZE_TOO_LARGE = "Policy size is too large";
  public static final String SYNTAX_ERROR = "Policy has syntax error";
  
  public PolicyException( ) {
    super( );
  }
  
  public PolicyException( String message, Throwable cause ) {
    super( message, cause );
  }
  
  public PolicyException( String message ) {
    super( message );
  }
  
  public PolicyException( Throwable cause ) {
    super( cause );
  }
  
}
