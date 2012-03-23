package com.eucalyptus.crypto;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.apache.log4j.Logger;

/**
 * Facade for helpers and a set of generator methods providing non-trivial random tokens.
 * 
 * @author decker
 */
public class Crypto {
  private static Logger LOG = Logger.getLogger( Crypto.class );
  private static BaseSecurityProvider DUMMY = new BaseSecurityProvider( ) {};
  private static ConcurrentMap<Class, BaseSecurityProvider> providers = new ConcurrentHashMap<Class, BaseSecurityProvider>( );
  static {
    BaseSecurityProvider provider;
    try {
      Class provClass = ClassLoader.getSystemClassLoader().loadClass( "com.eucalyptus.auth.crypto.DefaultCryptoProvider" );
      provider = ( BaseSecurityProvider ) provClass.newInstance( );
    } catch ( Exception t ) {
      LOG.debug( t, t );
      provider = DUMMY;
    }
    providers.put( CertificateProvider.class, provider );
    providers.put( HmacProvider.class, provider );
    providers.put( CryptoProvider.class, provider );
  }

  /**
   * @param password
   * @return
   * @see com.eucalyptus.crypto.CryptoProvider#generateHashedPassword(java.lang.String)
   */
  public static String generateHashedPassword( final String password ) {
    return Crypto.getCryptoProvider( ).generateHashedPassword( password );
  }
  
  /**
   * @see com.eucalyptus.crypto.CryptoProvider#generateSessionToken()
   */
  public static String generateSessionToken() {
    return Crypto.getCryptoProvider( ).generateSessionToken();
  }

  /**
   * @see com.eucalyptus.crypto.CryptoProvider#generateQueryId()
   */
  public static String generateQueryId() {
    return Crypto.getCryptoProvider().generateQueryId();
  }

  /**
   * @see com.eucalyptus.crypto.CryptoProvider#generateSecretKey()
   */
  public static String generateSecretKey() {
    return Crypto.getCryptoProvider().generateSecretKey();
  }

  /**
   * @see com.eucalyptus.crypto.CryptoProvider#getDigestBase64(java.lang.String, com.eucalyptus.crypto.Digest)
   */
  public static String getDigestBase64( final String input, final Digest hash ) {
    return Crypto.getCryptoProvider( ).getDigestBase64( input, hash );
  }
  
  /**
   * @see com.eucalyptus.crypto.CryptoProvider#generateId(String, String)
   */
  public static String generateId( final String seed, final String prefix ) {
    return Crypto.getCryptoProvider( ).generateId( seed, prefix );
  }

  public static CertificateProvider getCertificateProvider( ) {
    return (CertificateProvider) providers.get( CertificateProvider.class );
  }

  public static HmacProvider getHmacProvider( ) {
    return (HmacProvider) providers.get( HmacProvider.class );
  }

  public static CryptoProvider getCryptoProvider( ) {
    return (CryptoProvider) providers.get( CryptoProvider.class );
  }
  
}
