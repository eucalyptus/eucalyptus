package com.eucalyptus.cloud.util;

public class VerificationException extends RuntimeException {
  
  public VerificationException( ) {}
  
  public VerificationException( String message ) {
    super( message );
  }
  
  public VerificationException( Throwable cause ) {
    super( cause );
  }
  
  public VerificationException( String message, Throwable cause ) {
    super( message, cause );
  }
  
}
