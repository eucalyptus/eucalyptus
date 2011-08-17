package com.eucalyptus.entities;

import java.util.concurrent.ExecutionException;

public abstract class TransactionException extends ExecutionException {

  protected TransactionException( ) {
    super( );
  }

  protected TransactionException( String message, Throwable ex ) {
    super( message, ex );
  }

  protected TransactionException( String message ) {
    super( message );
  }

  protected TransactionException( Throwable ex ) {
    super( ex );
  }

}
