package com.eucalyptus.auth.ldap;

public class LdapException extends Exception {
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
