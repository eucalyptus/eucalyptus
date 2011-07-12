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
 *   
 * @author wenye
 *
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
