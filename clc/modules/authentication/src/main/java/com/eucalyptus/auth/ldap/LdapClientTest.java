package com.eucalyptus.auth.ldap;

import java.util.List;
import javax.naming.NamingEnumeration;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.SearchResult;

public class LdapClientTest {

  public static void main( String[] args ) throws Exception {
    LdapIntegrationConfiguration lic = new LdapIntegrationConfiguration( );
    lic.setServerUrl( "ldap://localhost" );
    lic.setAuthMethod( "simple" );
    lic.setAuthPrincipal( "cn=EucalyptusManager,dc=eucalyptus,dc=com" );
    lic.setAuthCredentials( "zoomzoom" );
    lic.setGroupBaseDn( "ou=Groups,dc=eucalyptus,dc=com" );
    lic.setUserBaseDn( "ou=People,dc=eucalyptus,dc=com" );
    
    LdapClient client = new LdapClient( lic );
    NamingEnumeration<SearchResult> results = client.search( lic.getUserBaseDn( ), new BasicAttributes( ), new String[]{ "displayName" } );
    while ( results.hasMore( ) ) {
      SearchResult result = results.next( );
      System.out.println( result.getName( ) + ", " + result.getClassName( ) + ", " + result.getNameInNamespace( ) + ", " + result.getAttributes( ) );
      System.out.println( result.getAttributes( ).get( "uid" ).get( ) );
    }
    results = client.search( lic.getGroupBaseDn( ), new BasicAttributes( ), new String[]{ "cn", "member" } );
    while ( results.hasMore( ) ) {
      System.out.println( results.next( ) );
    }
  }
  
}
