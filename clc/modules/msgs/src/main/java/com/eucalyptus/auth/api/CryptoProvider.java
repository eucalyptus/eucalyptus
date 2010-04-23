package com.eucalyptus.auth.api;

import com.eucalyptus.auth.crypto.Digest;


public interface CryptoProvider extends BaseSecurityProvider {
  
  /**
   * TODO: DOCUMENT CryptoProvider.java
   * @param userName
   * @return
   */
  public abstract String generateId( String userId, String prefix );
  public abstract String generateCertificateCode( String userName );
  public abstract String generateConfirmationCode( String userName );
  public abstract String generateHashedPassword( String password );
  public abstract String generateSessionToken( String userName );
  
  /**
   * TODO: DOCUMENT CryptoProvider.java
   * @param input
   * @param hash
   * @param randomize
   * @return
   */
  public abstract String getDigestBase64( final String input, final Digest hash, final boolean randomize );
  

}