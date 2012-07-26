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
 *
 * This file may incorporate work covered under the following copyright
 * and permission notice:
 *
 *   Software License Agreement (BSD License)
 *
 *   Copyright (c) 2008, Regents of the University of California
 *   All rights reserved.
 *
 *   Redistribution and use of this software in source and binary forms,
 *   with or without modification, are permitted provided that the
 *   following conditions are met:
 *
 *     Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *     Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer
 *     in the documentation and/or other materials provided with the
 *     distribution.
 *
 *   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *   "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *   LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 *   FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 *   COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 *   INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 *   BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *   LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 *   CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 *   LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 *   ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 *   POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 *   THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 *   COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 *   AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 *   IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 *   SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 *   WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 *   REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 *   IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 *   NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/

package com.eucalyptus.auth.ldap.authentication;

import java.io.IOException;
import java.security.PrivilegedAction;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;
import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.Configuration;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import org.apache.log4j.Logger;
import com.eucalyptus.auth.LdapException;
import com.eucalyptus.auth.ldap.LdapIntegrationConfiguration;
import com.eucalyptus.auth.ldap.LicParser;
import com.google.common.base.Strings;

/**
 * Authenticator for GSSAPI with Kerberos V5.
 */
public class GssapiKrb5Authenticator implements LdapAuthenticator {
  
  public static final String KRB5_CONF_PROPERTY = "java.security.krb5.conf";
  public static final String KRB5_LOGIN_MODULE = "com.sun.security.auth.module.Krb5LoginModule";
  public static final String JAAS_CONF_OPTION_CLIENT = "client";
  public static final String KRB5_LOGIN_CONTEXT_NAME = GssapiKrb5Authenticator.class.getName( );
  
  private static final Logger LOG = Logger.getLogger( GssapiKrb5Authenticator.class );
    
  public GssapiKrb5Authenticator( ) {
  }
  
  /**
   * See {@link com.eucalyptus.auth.ldap.authentication.LdapAuthenticator}
   * <p>
   *  extraArgs[0] is the path of krb5.conf
   * </p>
   */
  @Override
  public LdapContext authenticate( final String serverUrl, String method, final boolean useSsl, final boolean ignoreSslCert, final String login, final String password, Object... extraArgs ) throws LdapException {
    if ( Strings.isNullOrEmpty( login ) || Strings.isNullOrEmpty( password ) ) {
      throw new LdapException( "LDAP login failed: empty login name or password" );
    }
    if ( extraArgs.length < 1 || !( extraArgs[0] instanceof String ) || Strings.isNullOrEmpty( ( String )extraArgs[0] ) ) {
      throw new LdapException( "GSSAPI w/ Kerberos V5 requires krb5.conf argument" );
    }
    
    System.setProperty( KRB5_CONF_PROPERTY, ( String )extraArgs[0] );
    
    final Map<String, String> options = new HashMap<String, String>( );
    options.put( JAAS_CONF_OPTION_CLIENT, "TRUE" );
    final Configuration configuration = new Configuration( ) {
      @Override
      public AppConfigurationEntry[] getAppConfigurationEntry( String name ) {
        return new AppConfigurationEntry[] { new AppConfigurationEntry( KRB5_LOGIN_MODULE,
                                                                        AppConfigurationEntry.LoginModuleControlFlag.REQUIRED,
                                                                        options )
        };
      }
    };
    final CallbackHandler callbackHandler = new CallbackHandler( ) {
      @Override
      public void handle( Callback[] callbacks ) throws IOException, UnsupportedCallbackException {
        for ( int i = 0; i < callbacks.length; i++ ) {
          if ( callbacks[i] instanceof NameCallback ) {
            NameCallback cb = ( NameCallback )callbacks[i];
            cb.setName( login );
          } else if ( callbacks[i] instanceof PasswordCallback ) {
            PasswordCallback cb = ( PasswordCallback )callbacks[i];
            char[] pwBytes = new char[password.length( )];
            password.getChars( 0, pwBytes.length, pwBytes, 0 );
            cb.setPassword( pwBytes );
          }
        }
      }
    };
    // 1. Log in (to Kerberos)
    LoginContext loginContext = null;
    try {
      loginContext = new LoginContext( KRB5_LOGIN_CONTEXT_NAME, null, callbackHandler, configuration );
      loginContext.login();
    } catch ( LoginException e ) {
      LOG.error( e, e );
      throw new LdapException( "Failed to login to Kerberos", e );
    }
    // 2. Perform JNDI work as logged in subject
    LdapContext ldapContext = Subject.<LdapContext>doAs( loginContext.getSubject(), new PrivilegedAction<LdapContext>( ) {
      @Override
      public LdapContext run( ) {
        Properties env = new Properties( );
        env.put( Context.INITIAL_CONTEXT_FACTORY, LDAP_CONTEXT_FACTORY );
        env.put( Context.REFERRAL, "follow" );
        env.put( Context.PROVIDER_URL, serverUrl );
        env.put( Context.SECURITY_AUTHENTICATION, LicParser.LDAP_AUTH_METHOD_SASL_GSSAPI );
        if ( useSsl ) {
          env.put( Context.SECURITY_PROTOCOL, SSL_PROTOCOL );
          if ( ignoreSslCert ) {
            env.put( SOCKET_FACTORY, EasySSLSocketFactory.class.getCanonicalName( ) );
          }
        }
        try {
          return new InitialLdapContext( env, null );
        } catch ( NamingException e ) {
          LOG.error( e, e );
        }
        return null;
      }
    } );
    if ( ldapContext == null ) {
      throw new LdapException( "LDAP login failed, possibly wrong credential" );
    }
    return ldapContext;
  }
  
}
