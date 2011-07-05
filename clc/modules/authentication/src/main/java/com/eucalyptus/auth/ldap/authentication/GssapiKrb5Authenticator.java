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
 * 
 * @author wenye
 *
 */
public class GssapiKrb5Authenticator extends AbstractBaseAuthenticator {
  
  public static final String KRB5_CONF_PROPERTY = "java.security.krb5.conf";
  public static final String KRB5_LOGIN_MODULE = "com.sun.security.auth.module.Krb5LoginModule";
  public static final String JAAS_CONF_OPTION_CLIENT = "client";
  public static final String KRB5_LOGIN_CONTEXT_NAME = GssapiKrb5Authenticator.class.getName( );
  
  private static final Logger LOG = Logger.getLogger( GssapiKrb5Authenticator.class );
    
  public GssapiKrb5Authenticator( ) {
  }
  
  @Override
  public LdapContext authenticate( final LdapIntegrationConfiguration lic, final String login, final String password ) throws LdapException {
    if ( Strings.isNullOrEmpty( login ) || Strings.isNullOrEmpty( password ) ) {
      throw new LdapException( "LDAP login failed: empty login name or password" );
    }
    
    System.setProperty( KRB5_CONF_PROPERTY, lic.getKrb5Conf( ) );
    
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
        env.put( Context.PROVIDER_URL, lic.getServerUrl( ) );
        env.put( Context.SECURITY_AUTHENTICATION, LicParser.LDAP_AUTH_METHOD_SASL_GSSAPI );
        if ( lic.isUseSsl( ) ) {
          env.put( Context.SECURITY_PROTOCOL, SSL_PROTOCOL );
          if ( lic.isIgnoreSslCertValidation( ) ) {
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
