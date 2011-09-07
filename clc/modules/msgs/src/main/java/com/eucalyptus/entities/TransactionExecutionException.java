package com.eucalyptus.entities;


public class TransactionExecutionException extends TransactionException {

  public TransactionExecutionException( ) {
    super( );
  }

  public TransactionExecutionException( String message, Throwable ex ) {
    super( message, ex );
  }

  public TransactionExecutionException( String message ) {
    super( message );
  }

  public TransactionExecutionException( Throwable ex ) {
    super( ex );
  }

}
