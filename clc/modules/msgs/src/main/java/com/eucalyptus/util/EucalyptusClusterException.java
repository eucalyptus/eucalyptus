package com.eucalyptus.util;

public class EucalyptusClusterException extends EucalyptusCloudException {

  private EucalyptusClusterException( ) {
    super( );
  }

  public EucalyptusClusterException( String message, Throwable ex ) {
    super( message, ex );
  }

  private EucalyptusClusterException( String message ) {
    super( message );
  }

  private EucalyptusClusterException( Throwable ex ) {
    super( ex );
  }

}
