package com.eucalyptus.auth.crypto;

import org.apache.log4j.Logger;
import com.eucalyptus.auth.CredentialProviders;

public class CryptoProviders {
  private static Logger         LOG      = Logger.getLogger( CryptoProviders.class );
  
  private static CryptoProvider provider;
  
  //TODO: MAKE THIS CONFIGURABLE, HIIIIIIIIIIIIIiiii
  public static void setProvider( CryptoProvider newProvider ) {
    if( provider == null ) { 
      synchronized ( CredentialProviders.class ) {
        LOG.info( "Setting the crypto provider to: " + newProvider.getClass( ) );
        provider = newProvider;
      }
    }
  }
  
  public static CryptoProvider getProvider( ) {
    com.eucalyptus.auth.crypto.DefaultCryptoProvider.class.toString( );
    return provider;
  }
  
  /**
   * @param userName
   * @return
   * @see com.eucalyptus.auth.crypto.CryptoProvider#generateCertificateCode(java.lang.String)
   */
  public static String generateCertificateCode( String userName ) {
    return CryptoProviders.getProvider( ).generateCertificateCode( userName );
  }
  
  /**
   * @param userName
   * @return
   * @see com.eucalyptus.auth.crypto.CryptoProvider#generateConfirmationCode(java.lang.String)
   */
  public static String generateConfirmationCode( String userName ) {
    return CryptoProviders.getProvider( ).generateConfirmationCode( userName );
  }
  
  /**
   * @param password
   * @return
   * @see com.eucalyptus.auth.crypto.CryptoProvider#generateHashedPassword(java.lang.String)
   */
  public static String generateHashedPassword( String password ) {
    return CryptoProviders.getProvider( ).generateHashedPassword( password );
  }
  
  /**
   * @param userName
   * @return
   * @see com.eucalyptus.auth.crypto.CryptoProvider#generateQueryId(java.lang.String)
   */
  public static String generateQueryId( String userName ) {
    return CryptoProviders.getProvider( ).generateQueryId( userName );
  }
  
  /**
   * @param userName
   * @return
   * @see com.eucalyptus.auth.crypto.CryptoProvider#generateSecretKey(java.lang.String)
   */
  public static String generateSecretKey( String userName ) {
    return CryptoProviders.getProvider( ).generateSecretKey( userName );
  }
  
  /**
   * @param userName
   * @return
   * @see com.eucalyptus.auth.crypto.CryptoProvider#generateSessionToken(java.lang.String)
   */
  public static String generateSessionToken( String userName ) {
    return CryptoProviders.getProvider( ).generateSessionToken( userName );
  }
  
  /**
   * @param input
   * @param hash
   * @param randomize
   * @return
   * @see com.eucalyptus.auth.crypto.CryptoProvider#getDigestBase64(java.lang.String, com.eucalyptus.auth.crypto.Digest, boolean)
   */
  public static String getDigestBase64( String input, Digest hash, boolean randomize ) {
    return CryptoProviders.getProvider( ).getDigestBase64( input, hash, randomize );
  }
  
}
