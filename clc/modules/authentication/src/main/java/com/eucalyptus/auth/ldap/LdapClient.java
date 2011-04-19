package com.eucalyptus.auth.ldap;

import java.security.GeneralSecurityException;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;
import org.apache.log4j.Logger;
import com.eucalyptus.auth.LdapException;
import com.eucalyptus.auth.crypto.StringCrypto;
import com.google.common.collect.Lists;

public class LdapClient {
  
  public static final String CRYPTO_FORMAT = "RSA/ECB/PKCS1Padding";
  public static final String CRYPTO_PROVIDER = "BC";
  
  public static final Pattern ENCRYPTED_PATTERN = Pattern.compile( "\\{(.+)\\}(.+)" );
  
  public static final String LDAP_CONTEXT_FACTORY = "com.sun.jndi.ldap.LdapCtxFactory";
  
  public static final int TIMEOUT_IN_MILLIS = 60000; // a minute
  
  public static boolean DEBUG = false;
  
  private static Logger LOG = Logger.getLogger(  LdapClient.class );
  
  private StringCrypto sc = new StringCrypto( CRYPTO_FORMAT, CRYPTO_PROVIDER );
  private Properties env;
  private LdapContext context;
  
  public LdapClient( LdapIntegrationConfiguration lic ) throws LdapException {
    this.env = new Properties( );
    prepareLdapContextEnv( lic );
    try {
      this.context = new InitialLdapContext( env, null );
    } catch ( NamingException e ) {
      LOG.error( e, e );
      throw new LdapException( "Connection failure", e );
    }
  }
  
  private String getPassword( String licCred ) throws LdapException {
    try {
      Matcher matcher = ENCRYPTED_PATTERN.matcher( licCred );
      if ( matcher.matches( ) ) {
        return sc.decryptOpenssl( matcher.group( 1 )/*format*/, matcher.group( 2 )/*passwordEncoded*/ );
      } else {
        // Not encrypted
        return licCred;
      }
    } catch ( GeneralSecurityException e ) {
      LOG.error( e, e );
      throw new LdapException( "Decryption failure", e );
    }
  }
  
  private void prepareLdapContextEnv( LdapIntegrationConfiguration lic ) throws LdapException {
    env.put( Context.INITIAL_CONTEXT_FACTORY, LDAP_CONTEXT_FACTORY );
    env.put( Context.PROVIDER_URL, lic.getServerUrl( ) );
    env.put( Context.SECURITY_AUTHENTICATION, lic.getAuthMethod( ) );
    if ( !LicParser.LDAP_AUTH_METHOD_SASL_GSSAPI.equals( lic.getAuthMethod( ) ) ) {
      env.put( Context.SECURITY_PRINCIPAL, lic.getAuthPrincipal( ) );
      env.put( Context.SECURITY_CREDENTIALS, getPassword( lic.getAuthCredentials( ) ) );
    }
    if ( lic.isUseSsl( ) ) {
      env.put( Context.SECURITY_PROTOCOL, "ssl" );
    }
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
