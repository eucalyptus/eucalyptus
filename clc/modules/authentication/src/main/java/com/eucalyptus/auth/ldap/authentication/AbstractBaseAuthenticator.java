package com.eucalyptus.auth.ldap.authentication;

import javax.naming.ldap.LdapContext;
import com.eucalyptus.auth.LdapException;
import com.eucalyptus.auth.ldap.LdapIntegrationConfiguration;

public abstract class AbstractBaseAuthenticator implements LdapAuthenticator {
  
  @Override
  public LdapContext authenticate( LdapIntegrationConfiguration lic ) throws LdapException {
    return authenticate( lic, lic.getAuthPrincipal( ), AuthenticationUtil.decryptPassword( lic.getAuthCredentials( ) ) );
  }
  
}
