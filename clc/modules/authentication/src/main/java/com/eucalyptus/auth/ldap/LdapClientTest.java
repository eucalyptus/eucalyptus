/*************************************************************************
 * Copyright 2009-2012 Eucalyptus Systems, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 ************************************************************************/

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
    lic.setUseSsl( false );
    lic.setIgnoreSslCertValidation( true );
    lic.setKrb5Conf( "/home/wenye/workspace/jndi-gssapi/src/krb5.conf" );
    lic.setGroupBaseDn( "ou=Groups,dc=test-eucalyptus,dc=com" );
    lic.setUserBaseDn( "ou=People,dc=test-eucalyptus,dc=com" );
    
    LdapClient client = LdapClient.authenticateClient( lic );
    NamingEnumeration<SearchResult> results = client.search( "dc=test-eucalyptus,dc=com", "objectClass=inetOrgPerson", new String[]{ "displayName" } );
    while ( results.hasMore( ) ) {
      SearchResult r = results.next( );
      System.out.println( r.getName( ) + " " + r.getNameInNamespace( ) + " -- " + r.getAttributes( ) );
    }
  }
  
}
