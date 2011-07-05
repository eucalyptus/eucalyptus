package com.eucalyptus.auth.ldap.authentication;

import com.eucalyptus.auth.ldap.LdapIntegrationConfiguration;
import com.eucalyptus.auth.ldap.LicParser;

public class AuthenticationTest {

  public static void main( String[] args ) throws Exception {
    LdapIntegrationConfiguration lic = new LdapIntegrationConfiguration( );
    lic.setServerUrl( "" );
    lic.setAuthMethod( LicParser.LDAP_AUTH_METHOD_SASL_GSSAPI );
    lic.setAuthPrincipal( "cn=admin,dc=test-eucalyptus,dc=com" );
    lic.setAuthCredentials( "" );
    lic.setUseSsl( true );
    lic.setIgnoreSslCertValidation( true );
    lic.setKrb5Conf( "" );
    
    LdapAuthenticator auth = LdapAuthenticatorFactory.getLdapAuthenticator( lic );
    auth.authenticate( lic, "peter", "" );
    
    System.out.println( "Success" );
  }
  
}
