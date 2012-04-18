package com.eucalyptus.auth.api;

import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import com.eucalyptus.ws.WebServicesException;

public class BaseSecurityException extends WebServicesException {

  public BaseSecurityException( ) {
    super( );
  }

  public BaseSecurityException( HttpResponseStatus status ) {
    super( status );
  }

  public BaseSecurityException( String message, HttpResponseStatus status ) {
    super( message, status );
  }

  public BaseSecurityException( String message, Throwable cause, HttpResponseStatus status ) {
    super( message, cause, status );
  }

  public BaseSecurityException( String message, Throwable cause ) {
    super( message, cause );
  }

  public BaseSecurityException( String message ) {
    super( message );
  }

  public BaseSecurityException( Throwable cause, HttpResponseStatus status ) {
    super( cause, status );
  }

  public BaseSecurityException( Throwable cause ) {
    super( cause );
  }

}
