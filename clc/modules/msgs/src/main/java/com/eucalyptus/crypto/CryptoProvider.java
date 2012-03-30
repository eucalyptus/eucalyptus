package com.eucalyptus.crypto;



public interface CryptoProvider extends BaseSecurityProvider {
  
  /**
   * TODO: DOCUMENT CryptoProvider.java
   */
  String generateId( String seed, String prefix );
  String generateQueryId();
  String generateSecretKey();
  String generateHashedPassword( String password );
  String generateSessionToken();
  
  /**
   * TODO: DOCUMENT CryptoProvider.java
   */
  String getDigestBase64( String input, Digest hash );
  String getFingerPrint( byte[] data );

  String generateLinuxSaltedPassword( String password );
  boolean verifyLinuxSaltedPassword( String clear, String hashed );
}