package com.eucalyptus.auth.ldap.authentication;

import com.eucalyptus.auth.LdapException;
import com.eucalyptus.auth.ldap.LdapIntegrationConfiguration;
import com.eucalyptus.auth.ldap.LicParser;

public class LdapAuthenticatorFactory {

  public static LdapAuthenticator getLdapAuthenticator( LdapIntegrationConfiguration lic ) throws LdapException {
    if ( lic == null ) {
      throw new LdapException( "Can not find LDAP authenticator for empty configuration" );
    }
    if ( LicParser.LDAP_AUTH_METHOD_SASL_GSSAPI.equals( lic.getAuthMethod( ) ) ) {
      return new GssapiKrb5Authenticator( );
    } else {
      return new DefaultAuthenticator( );
    }
  }
  
}
