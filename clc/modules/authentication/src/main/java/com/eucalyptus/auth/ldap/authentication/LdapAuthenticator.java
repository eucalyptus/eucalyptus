package com.eucalyptus.auth.ldap.authentication;

import javax.naming.ldap.LdapContext;
import com.eucalyptus.auth.LdapException;
import com.eucalyptus.auth.ldap.LdapIntegrationConfiguration;

public interface LdapAuthenticator {

  public static final String LDAP_CONTEXT_FACTORY = "com.sun.jndi.ldap.LdapCtxFactory";
  public static final String SSL_PROTOCOL = "ssl";
  public static final String SOCKET_FACTORY = "java.naming.ldap.factory.socket";

  /**
   * Authenticate the user for LDAP sync.
   * 
   * @param lic
   * @return
   * @throws LdapException
   */
  public LdapContext authenticate( LdapIntegrationConfiguration lic ) throws LdapException;
  
  /**
   * Authenticate an arbitrary user based on LDAP sync config.
   * 
   * @param lic
   * @param login
   * @param password
   * @return
   * @throws LdapException
   */
  public LdapContext authenticate( LdapIntegrationConfiguration lic, String login, String password ) throws LdapException;
  
}
