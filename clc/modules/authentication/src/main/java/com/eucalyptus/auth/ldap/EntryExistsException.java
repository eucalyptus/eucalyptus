package com.eucalyptus.auth.ldap;

public class EntryExistsException extends Exception {
  public EntryExistsException( ) {
    super( );
  }

  public EntryExistsException( String message, Throwable cause ) {
    super( message, cause );
  }

  public EntryExistsException( String message ) {
    super( message );
  }

  public EntryExistsException( Throwable cause ) {
    super( cause );
  }
}
