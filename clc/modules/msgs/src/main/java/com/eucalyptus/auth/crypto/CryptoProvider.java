package com.eucalyptus.auth.crypto;

public interface CryptoProvider {
  
  public abstract String generateHashedPassword( String password );
  
  public abstract String generateQueryId( String userName );
  
  public abstract String generateSecretKey( String userName );
  
  public abstract String generateCertificateCode( String userName );
  
  public abstract String generateConfirmationCode( String userName );
  
  public abstract String generateSessionToken( String userName );
  
  public abstract String getDigestBase64( String input, Digest hash, boolean randomize );
  
}