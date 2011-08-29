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
   * @param userName
   * @return
   * @see com.eucalyptus.crypto.CryptoProvider#generateSessionToken(java.lang.String)
   */
  public static String generateSessionToken( final String userName ) {
    return Crypto.getCryptoProvider( ).generateSessionToken( userName );
  }
  
  /**
   * @param input
   * @param hash
   * @param randomize
   * @return
   * @see com.eucalyptus.crypto.CryptoProvider#getDigestBase64(java.lang.String, com.eucalyptus.crypto.Digest, boolean)
   */
  public static String getDigestBase64( final String input, final Digest hash, final boolean randomize ) {
    return Crypto.getCryptoProvider( ).getDigestBase64( input, hash, randomize );
  }
  
  /**
   * @param userId
   * @param prefix
   * @return
   * @see com.eucalyptus.crypto.CryptoProvider#generateId(String, String)
   */
  public static String generateId( final String userId, final String prefix ) {
    return Crypto.getCryptoProvider( ).generateId( userId, prefix );
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
