/*************************************************************************
 * Copyright 2008 Regents of the University of California
 * Copyright 2009-2012 Ent. Services Development Corporation LP
 *
 * Redistribution and use of this software in source and binary forms,
 * with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *   Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 *   Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer
 *   in the documentation and/or other materials provided with the
 *   distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 * THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 * COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 * AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 * IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 * SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 * WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 * REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 * IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 * NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/

package com.eucalyptus.auth.euare.ldap;

import javax.naming.NamingEnumeration;
import javax.naming.directory.SearchResult;
import org.junit.Ignore;

@Ignore("Manual development test")
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
