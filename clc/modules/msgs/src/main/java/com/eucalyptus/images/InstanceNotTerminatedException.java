package com.eucalyptus.images;


public class InstanceNotTerminatedException extends Exception {

  public InstanceNotTerminatedException( ) {
    super( );
  }

  public InstanceNotTerminatedException( String message, Throwable cause ) {
    super( message, cause );
  }

  public InstanceNotTerminatedException( String message ) {
    super( message );
  }

  public InstanceNotTerminatedException( Throwable cause ) {
    super( cause );
  }
}
