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
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.LdapContext;
import org.apache.log4j.Logger;
import com.eucalyptus.auth.LdapException;
import com.eucalyptus.auth.euare.ldap.authentication.AuthenticationUtil;
import com.eucalyptus.auth.euare.ldap.authentication.LdapAuthenticatorFactory;

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
    searchControls.setCountLimit( 0 );
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
