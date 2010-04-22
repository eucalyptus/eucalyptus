package com.eucalyptus.ws;

import org.jboss.netty.handler.codec.http.HttpResponseStatus;

public class ServiceNotReadyException extends WebServicesException {

  public ServiceNotReadyException( ) {
    super( HttpResponseStatus.SERVICE_UNAVAILABLE );
  }

  public ServiceNotReadyException( String message, Throwable cause ) {
    super( message, cause, HttpResponseStatus.SERVICE_UNAVAILABLE );
  }

  public ServiceNotReadyException( String message ) {
    super( message, HttpResponseStatus.SERVICE_UNAVAILABLE );
  }

  public ServiceNotReadyException( Throwable cause ) {
    super( cause, HttpResponseStatus.SERVICE_UNAVAILABLE );
  }
  
  

}
