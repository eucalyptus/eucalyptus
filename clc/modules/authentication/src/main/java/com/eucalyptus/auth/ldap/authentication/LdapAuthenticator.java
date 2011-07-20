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
