package com.eucalyptus.util;

public class EucalyptusClusterException extends EucalyptusCloudException {

  public EucalyptusClusterException( ) {
    super( );
  }

  public EucalyptusClusterException( String message, Throwable ex ) {
    super( message, ex );
  }

  public EucalyptusClusterException( String message ) {
    super( message );
  }

  public EucalyptusClusterException( Throwable ex ) {
    super( ex );
  }

}
