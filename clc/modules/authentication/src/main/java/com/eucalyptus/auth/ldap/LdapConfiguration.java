package com.eucalyptus.auth.ldap;

/**
 * LDAP configurations for Eucalyptus.
 * TODO (wenye): Maybe they should be put in a configuration file.
 * @author wenye
 *
 */
public interface LdapConfiguration {
  public static final boolean ENABLE_LDAP = true;
  
  public static final String LDAP_CONTEXT_FACTORY = "com.sun.jndi.ldap.LdapCtxFactory";
  public static final String LDAP_LOCAL_SERVER_URL = "ldap://localhost:8778";
  public static final String LDAP_SECURITY_AUTHENTICATION = "simple";
  public static final String LDAP_SECURITY_PRINCIPAL = "cn=EucalyptusManager,dc=eucalyptus,dc=com";
  
  public static final String ROOT_DN = "dc=eucalyptus,dc=com";
  public static final String USER_BASE_DN = "ou=people," + ROOT_DN;
  public static final String GROUP_BASE_DN = "ou=groups," + ROOT_DN;
}
