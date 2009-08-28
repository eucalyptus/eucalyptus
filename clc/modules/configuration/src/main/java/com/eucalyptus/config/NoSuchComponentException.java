package com.eucalyptus.config;

import com.eucalyptus.util.EucalyptusCloudException;

public class NoSuchComponentException extends EucalyptusCloudException {

  public NoSuchComponentException( ) {
    super( );
  }

  public NoSuchComponentException( String message, Throwable ex ) {
    super( message, ex );
  }

  public NoSuchComponentException( String message ) {
    super( message );
  }

  public NoSuchComponentException( Throwable ex ) {
    super( ex );
  }

}
