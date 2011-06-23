package com.eucalyptus.auth.ldap;

import java.util.List;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.LdapContext;
import javax.naming.ldap.LdapName;

public class LdapClientTest {

  public static void main( String[] args ) throws Exception {
    LdapIntegrationConfiguration lic = new LdapIntegrationConfiguration( );
    lic.setServerUrl( "ldap://eucahost-4-21.eucalyptus" );
    lic.setAuthMethod( "simple" );
    lic.setAuthPrincipal( "cn=admin,dc=test-eucalyptus,dc=com" );
    lic.setAuthCredentials( "pwd*4admin" );
    lic.setUseSsl( true );
    lic.setGroupBaseDn( "ou=Groups,dc=test-eucalyptus,dc=com" );
    lic.setUserBaseDn( "ou=People,dc=test-eucalyptus,dc=com" );
    
    LdapClient client = new LdapClient( lic );
    NamingEnumeration<SearchResult> results = client.search( "dc=test-eucalyptus,dc=com", "objectClass=inetOrgPerson", new String[]{ "displayName" } );
    while ( results.hasMore( ) ) {
      SearchResult r = results.next( );
      System.out.println( r.getNameInNamespace( ) + " -- " + r.getAttributes( ) );
    }
    
    /*
    System.out.println( "" );
    
    LdapContext context = client.getContext( );
    try {
      Attributes attrs = context.getAttributes( new LdapName( "uid=stevejobs,ou=People,dc=eucalyptus,dc=com" ), new String[]{ "displayName" } );
      System.out.println( attrs );
      System.out.println( attrs.get( "displayName" ).get( ) );
    } catch ( NamingException e ) {
      e.printStackTrace( );
    }
    */
  }
  
}
