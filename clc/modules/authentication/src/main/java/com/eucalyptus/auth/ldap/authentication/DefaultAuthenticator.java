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

package com.eucalyptus.auth.ldap.authentication;

import java.util.Properties;
import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;
import org.apache.log4j.Logger;
import com.eucalyptus.auth.LdapException;
import com.eucalyptus.auth.ldap.LdapIntegrationConfiguration;
import com.google.common.base.Strings;

/**
 * The default authenticator.
 * 
 * Supports:
 *   simple
 *   SASL/DIGEST-MD5
 *
 * SSL is also supported.
 */
public class DefaultAuthenticator implements LdapAuthenticator {
  
  private static Logger LOG = Logger.getLogger( DefaultAuthenticator.class ); 
  
  public DefaultAuthenticator( ) {
  }
  
  @Override
  public LdapContext authenticate( String serverUrl, String method, boolean useSsl, boolean ignoreSslCert, String login, String password, Object... extraArgs ) throws LdapException {
    if ( Strings.isNullOrEmpty( login ) || Strings.isNullOrEmpty( password ) ) {
      throw new LdapException( "LDAP login failed: empty login name or password" );
    }    
    Properties env = new Properties( );
    env.put( Context.INITIAL_CONTEXT_FACTORY, LDAP_CONTEXT_FACTORY );
    env.put( Context.PROVIDER_URL, serverUrl );
    env.put( Context.SECURITY_AUTHENTICATION, method );
    env.put( Context.SECURITY_PRINCIPAL, login );
    env.put( Context.SECURITY_CREDENTIALS, password );
    if ( useSsl ) {
      env.put( Context.SECURITY_PROTOCOL, SSL_PROTOCOL );
      if ( ignoreSslCert ) {
        env.put( SOCKET_FACTORY, EasySSLSocketFactory.class.getCanonicalName( ) );
      }
    }
    LdapContext ldapContext = null;
    try {
      ldapContext = new InitialLdapContext( env, null );
    } catch ( NamingException e ) {
      LOG.error( e, e );
      throw new LdapException( "LDAP login failure", e );
    }
    return ldapContext;
  }
  
}
