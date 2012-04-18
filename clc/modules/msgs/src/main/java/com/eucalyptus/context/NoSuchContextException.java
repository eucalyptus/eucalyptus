package com.eucalyptus.context;

import com.eucalyptus.BaseException;

public class NoSuchContextException extends BaseException {

  public NoSuchContextException( ) {
    super( );
  }

  public NoSuchContextException( String message, Throwable cause ) {
    super( message, cause );
  }

  public NoSuchContextException( String message ) {
    super( message );
  }

  public NoSuchContextException( Throwable cause ) {
    super( cause );
  }

}
