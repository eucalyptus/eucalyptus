package com.eucalyptus.auth.ldap;

public class EntryNotFoundException extends Exception {
  public EntryNotFoundException( ) {
    super( );
  }

  public EntryNotFoundException( String message, Throwable cause ) {
    super( message, cause );
  }

  public EntryNotFoundException( String message ) {
    super( message );
  }

  public EntryNotFoundException( Throwable cause ) {
    super( cause );
  }
}
