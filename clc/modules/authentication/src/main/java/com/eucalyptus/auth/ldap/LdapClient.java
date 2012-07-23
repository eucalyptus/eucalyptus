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
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.LdapContext;
import org.apache.log4j.Logger;
import com.eucalyptus.auth.LdapException;
import com.eucalyptus.auth.ldap.authentication.AuthenticationUtil;
import com.eucalyptus.auth.ldap.authentication.LdapAuthenticatorFactory;

public class LdapClient {
  
  public static final int TIMEOUT_IN_MILLIS = 60000; // a minute
  
  public static boolean DEBUG = false;
  
  private static Logger LOG = Logger.getLogger( LdapClient.class );

  private LdapContext context;
  
  private LdapClient( ) {
    // constructor not allowed
  }
  
  private LdapClient( LdapContext context ) {
    this.context = context;
  }
  
  public static LdapClient authenticateClient( LdapIntegrationConfiguration lic ) throws LdapException {
    LdapContext context = LdapAuthenticatorFactory.getLdapAuthenticator( lic.getAuthMethod( ) ).authenticate( lic.getServerUrl( ),
                                                                                                              lic.getAuthMethod( ),
                                                                                                              lic.isUseSsl( ),
                                                                                                              lic.isIgnoreSslCertValidation( ),
                                                                                                              lic.getAuthPrincipal( ),
                                                                                                              AuthenticationUtil.decryptPassword( lic.getAuthCredentials( ) ),
                                                                                                              lic.getKrb5Conf( ) );
    return new LdapClient( context );
  }
  
  public static LdapClient authenticateUser( LdapIntegrationConfiguration lic, String login, String password ) throws LdapException {
    LdapContext context = LdapAuthenticatorFactory.getLdapAuthenticator( lic.getAuthMethod( ) ).authenticate( lic.getServerUrl( ),
                                                                                                              lic.getRealUserAuthMethod( ),
                                                                                                              lic.isUseSsl( ),
                                                                                                              lic.isIgnoreSslCertValidation( ),
                                                                                                              login,
                                                                                                              password,
                                                                                                              lic.getKrb5Conf( ) );
    return new LdapClient( context );
  }

  public void close( ) {
    if ( this.context != null ) {
      try {
        this.context.close( );
      } catch ( NamingException e ) {
        LOG.error( e, e );
      }
    }
  }
  
  public LdapContext getContext( ) {
    return this.context;
  }
  
  public synchronized NamingEnumeration<SearchResult> search( String baseDn, String filter, String[] attrs ) throws LdapException {
    if ( DEBUG ) { LOG.debug( "<search> " + baseDn + ": filter = " + filter ); }
    SearchControls searchControls = new SearchControls( );
    if ( attrs != null ) {
      searchControls.setReturningAttributes( attrs );
    }
    searchControls.setDerefLinkFlag( true );
    searchControls.setSearchScope( SearchControls.SUBTREE_SCOPE );
    searchControls.setTimeLimit( TIMEOUT_IN_MILLIS );
    try {
      return context.search( baseDn, filter, searchControls );
    } catch ( NamingException e ) {
      LOG.error( e, e );
      throw new LdapException( "Searching " + baseDn + "failed using " + filter, e );
    }
  }
  
  public synchronized NamingEnumeration<SearchResult> search( String baseDn, Attributes matchingAttrs, String[] attrs ) throws LdapException {
    if ( DEBUG ) { LOG.debug( "<search> " + baseDn + ": " + matchingAttrs ); }
    try {
      return context.search( baseDn, matchingAttrs, attrs );
    } catch ( NamingException e ) {
      LOG.error( e, e );
      throw new LdapException( "Searching " + baseDn + "failed using " + matchingAttrs, e );
    }
  }
  
}
