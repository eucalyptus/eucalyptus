package com.eucalyptus.webui.shared.checker;

public class InvalidValueException extends Exception {

  private static final long serialVersionUID = 1L;

  public InvalidValueException( ) {
    super( );
  }
  
  public InvalidValueException( String message ) {
    super( message );
  }
  
  public InvalidValueException( Throwable cause ) {
    super( cause );
  }
  
  public InvalidValueException( String message, Throwable cause ) {
    super( message, cause );
  }
  
}
