package com.eucalyptus.webui.client.service;

import java.io.Serializable;

public class EucalyptusServiceException extends Exception implements Serializable {

  private static final long serialVersionUID = 1L;

  public static final String INVALID_SESSION = "Invalid session";
  
  public EucalyptusServiceException( ) {
    super( );
  }
  
  public EucalyptusServiceException( String message ) {
    super( message );
  }
  
  public EucalyptusServiceException( Throwable cause ) {
    super( cause );
  }
  
  public EucalyptusServiceException( String message, Throwable cause ) {
    super( message, cause );
  }
  
}
