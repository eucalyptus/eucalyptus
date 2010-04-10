package com.eucalyptus.auth.api;

import org.jboss.netty.handler.codec.http.HttpResponseStatus;

public class NoSuchCertificateException extends BaseSecurityException {

  public NoSuchCertificateException( ) {
    super( );
  }

  public NoSuchCertificateException( HttpResponseStatus status ) {
    super( status );
  }

  public NoSuchCertificateException( String message, HttpResponseStatus status ) {
    super( message, status );
  }

  public NoSuchCertificateException( String message, Throwable cause, HttpResponseStatus status ) {
    super( message, cause, status );
  }

  public NoSuchCertificateException( String message, Throwable cause ) {
    super( message, cause );
  }

  public NoSuchCertificateException( String message ) {
    super( message );
  }

  public NoSuchCertificateException( Throwable cause, HttpResponseStatus status ) {
    super( cause, status );
  }

  public NoSuchCertificateException( Throwable cause ) {
    super( cause );
  }

}
