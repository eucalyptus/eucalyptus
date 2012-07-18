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

import javax.naming.ldap.LdapContext;
import com.eucalyptus.auth.LdapException;
import com.eucalyptus.auth.ldap.LdapIntegrationConfiguration;

public interface LdapAuthenticator {

  public static final String LDAP_CONTEXT_FACTORY = "com.sun.jndi.ldap.LdapCtxFactory";
  public static final String SSL_PROTOCOL = "ssl";
  public static final String SOCKET_FACTORY = "java.naming.ldap.factory.socket";
  

  /**
   * Authenticate LDAP service.
   * 
   * @param serverUrl The LDAP service URL
   * @param method The authentication method
   * @param useSsl Whether to use SSL
   * @param ignoreSslCert Whether to ignore SSL certificate validation
   * @param login The login name
   * @param password The password
   * @param extraArgs Extra arguments.
   * @return An authenticated LDAP context.
   * @throws LdapException If there is any error.
   */
  public LdapContext authenticate( String serverUrl, String method, boolean useSsl, boolean ignoreSslCert, String login, String password, Object... extraArgs ) throws LdapException;
  
}
