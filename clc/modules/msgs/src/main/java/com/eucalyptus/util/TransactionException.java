package com.eucalyptus.util;

import java.util.concurrent.ExecutionException;

public class TransactionException extends ExecutionException {

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
