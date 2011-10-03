package com.eucalyptus.util;

public class EucalyptusClusterException extends RuntimeException {

  private static final long serialVersionUID = 1L;

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
