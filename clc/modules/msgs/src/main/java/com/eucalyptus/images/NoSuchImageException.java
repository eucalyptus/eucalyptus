package com.eucalyptus.images;

public class NoSuchImageException extends Exception {

  public NoSuchImageException( ) {
    super( );
  }

  public NoSuchImageException( String message, Throwable cause ) {
    super( message, cause );
  }

  public NoSuchImageException( String message ) {
    super( message );
  }

  public NoSuchImageException( Throwable cause ) {
    super( cause );
  }

}
