package com.eucalyptus.util;

public class TransactionFireException extends TransactionException {

  public TransactionFireException( ) {
    super( );
  }

  public TransactionFireException( String message, Throwable ex ) {
    super( message, ex );
  }

  public TransactionFireException( String message ) {
    super( message );
  }

  public TransactionFireException( Throwable ex ) {
    super( ex );
  }

}
