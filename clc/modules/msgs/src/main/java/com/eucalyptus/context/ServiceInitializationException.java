package com.eucalyptus.context;

import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import com.eucalyptus.ws.WebServicesException;

public class ServiceInitializationException extends WebServicesException {

  public ServiceInitializationException( String message, Throwable cause ) {
    super( message, cause, HttpResponseStatus.PRECONDITION_FAILED );
  }

  public ServiceInitializationException( String message ) {
    super( message, HttpResponseStatus.PRECONDITION_FAILED );
  }

}
