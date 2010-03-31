package com.eucalyptus.auth.crypto;

import com.eucalyptus.auth.Authentication;

/**
 * Facade for helpers and a set of generator methods providing non-trivial random tokens.
 * 
 * @author decker
 */
public class Crypto {
  
  /**
   * @param userName
   * @return
   * @see com.eucalyptus.auth.api.CryptoProvider#generateCertificateCode(java.lang.String)
   */
  public static String generateCertificateCode( final String userName ) {
    return Authentication.getCryptoProvider( ).generateCertificateCode( userName );
  }
  
  /**
   * @param userName
   * @return
   * @see com.eucalyptus.auth.api.CryptoProvider#generateConfirmationCode(java.lang.String)
   */
  public static String generateConfirmationCode( final String userName ) {
    return Authentication.getCryptoProvider( ).generateConfirmationCode( userName );
  }
  
  /**
   * @param password
   * @return
   * @see com.eucalyptus.auth.api.CryptoProvider#generateHashedPassword(java.lang.String)
   */
  public static String generateHashedPassword( final String password ) {
    return Authentication.getCryptoProvider( ).generateHashedPassword( password );
  }
  
  /**
   * @param userName
   * @return
   * @see com.eucalyptus.auth.api.CryptoProvider#generateSessionToken(java.lang.String)
   */
  public static String generateSessionToken( final String userName ) {
    return Authentication.getCryptoProvider( ).generateSessionToken( userName );
  }
  
  /**
   * @param input
   * @param hash
   * @param randomize
   * @return
   * @see com.eucalyptus.auth.api.CryptoProvider#getDigestBase64(java.lang.String, com.eucalyptus.auth.crypto.Digest, boolean)
   */
  public static String getDigestBase64( final String input, final Digest hash, final boolean randomize ) {
    return Authentication.getCryptoProvider( ).getDigestBase64( input, hash, randomize );
  }
  
  public static String generateId( final String userId, final String prefix ) {
    return Authentication.getCryptoProvider( ).generateId( userId, prefix );
  }
  
}
