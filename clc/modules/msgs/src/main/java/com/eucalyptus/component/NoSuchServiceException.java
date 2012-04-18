package com.eucalyptus.component;

import com.eucalyptus.util.EucalyptusCloudException;

public class NoSuchServiceException extends NoSuchComponentException {

  public NoSuchServiceException( ) {
    super( );
  }

  public NoSuchServiceException( String message, Throwable ex ) {
    super( message, ex );
  }

  public NoSuchServiceException( String message ) {
    super( message );
  }

  public NoSuchServiceException( Throwable ex ) {
    super( ex );
  }

}
