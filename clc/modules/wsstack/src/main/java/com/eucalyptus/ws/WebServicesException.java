package com.eucalyptus.ws;

public class WebServicesException extends Exception {

  // TODO: fix it...
  private static final long serialVersionUID = 1L;

  public WebServicesException( ) {
  }

  public WebServicesException( String message ) {
    super( message );
  }

  public WebServicesException( Throwable cause ) {
    super( cause );
  }

  public WebServicesException( String message, Throwable cause ) {
    super( message, cause );
  }

}
