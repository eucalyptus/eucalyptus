package com.eucalyptus.util;

public class EucalyptusCloudException extends Exception {

  public EucalyptusCloudException( ) {
    super( "Internal Error." );
  }

  public EucalyptusCloudException( String message ) {
    super( message );
  }

  public EucalyptusCloudException( Throwable ex ) {
    super( "Internal Error.", ex );
  }

  public EucalyptusCloudException( String message, Throwable ex ) {
    super( message, ex );
  }
}
