package com.eucalyptus.util;

public class TransactionException extends EucalyptusCloudException {

  public TransactionException( ) {
    super( );
  }

  public TransactionException( String message, Throwable ex ) {
    super( message, ex );
  }

  public TransactionException( String message ) {
    super( message );
  }

  public TransactionException( Throwable ex ) {
    super( ex );
  }

}
