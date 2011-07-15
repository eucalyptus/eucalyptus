package com.eucalyptus.auth.ldap.authentication;

import com.eucalyptus.auth.LdapException;
import com.eucalyptus.auth.ldap.LdapIntegrationConfiguration;
import com.eucalyptus.auth.ldap.LicParser;

public class LdapAuthenticatorFactory {

  public static LdapAuthenticator getLdapAuthenticator( String authMethod ) throws LdapException {
    if ( authMethod == null ) {
      throw new LdapException( "Can not find LDAP authenticator for empty authentication method" );
    }
    if ( LicParser.LDAP_AUTH_METHOD_SASL_GSSAPI.equals( authMethod ) ) {
      return new GssapiKrb5Authenticator( );
    } else {
      return new DefaultAuthenticator( );
    }
  }
  
}
