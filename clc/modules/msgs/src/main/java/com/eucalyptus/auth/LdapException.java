package com.eucalyptus.auth;

public class LdapException extends Exception {

  private static final long serialVersionUID = 1L;

  public LdapException( ) {
    super( );
  }

  public LdapException( String message, Throwable cause ) {
    super( message, cause );
  }

  public LdapException( String message ) {
    super( message );
  }

  public LdapException( Throwable cause ) {
    super( cause );
  }
  
}
