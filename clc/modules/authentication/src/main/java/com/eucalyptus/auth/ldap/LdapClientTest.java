package com.eucalyptus.auth.ldap;

import javax.naming.NamingEnumeration;
import javax.naming.directory.SearchResult;

public class LdapClientTest {

  public static void main( String[] args ) throws Exception {
    LdapIntegrationConfiguration lic = new LdapIntegrationConfiguration( );
    lic.setServerUrl( "" );
    lic.setAuthMethod( "GSSAPI" );
    //lic.setAuthPrincipal( "cn=admin,dc=test-eucalyptus,dc=com" );
    lic.setAuthPrincipal( "peter" );
    lic.setAuthCredentials( "" );
    lic.setUseSsl( true );
    lic.setIgnoreSslCertValidation( true );
    lic.setKrb5Conf( "" );
    lic.setGroupBaseDn( "ou=Groups,dc=test-eucalyptus,dc=com" );
    lic.setUserBaseDn( "ou=People,dc=test-eucalyptus,dc=com" );
    
    LdapClient client = new LdapClient( lic );
    NamingEnumeration<SearchResult> results = client.search( "dc=test-eucalyptus,dc=com", "objectClass=inetOrgPerson", new String[]{ "displayName" } );
    while ( results.hasMore( ) ) {
      SearchResult r = results.next( );
      System.out.println( r.getNameInNamespace( ) + " -- " + r.getAttributes( ) );
    }
  }
  
}
