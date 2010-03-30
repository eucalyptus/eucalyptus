package com.eucalyptus.auth.crypto;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import javax.security.auth.x500.X500Principal;

public interface CryptoProvider {
  
  public abstract X509Certificate generateCertificate( KeyPair keys, String userName );
  
  public abstract X509Certificate generateCertificate( KeyPair keys, X500Principal subjectDn );
  
  /**
   * Produces a signed certificate. If the <tt>signer</tt> certificate is null, then the certificate generated is self signed.
   * 
   * @param keys
   * @param subjectDn
   * @param signer
   * @return
   */
  public abstract X509Certificate generateCertificate( KeyPair keys, X500Principal subjectDn, X500Principal signer, PrivateKey signingKey );
  
  public abstract String generateCertificateCode( String userName );
  
  public abstract String generateConfirmationCode( String userName );
  
  public abstract String generateHashedPassword( String password );
  
  public abstract KeyPair generateKeyPair( );
  
  public abstract String generateQueryId( String userName );
  
  public abstract String generateSecretKey( String userName );
  
  public abstract X509Certificate generateServiceCertificate( KeyPair keys, String userName );
  
  public abstract String generateSessionToken( String userName );
  
  public abstract String getDigestBase64( String input, Digest hash, boolean randomize );
  
}