package com.eucalyptus.crypto;

import java.security.Key;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import javax.security.auth.x500.X500Principal;

/**
 * Facade for generator methods providing asymmetric keys and certificates.
 * 
 * @author decker
 */
public class Certs {
  
  /**
   * TODO: DOCUMENT Certs.java
   * 
   * @param keys
   * @param subjectDn
   * @param signer
   * @param signingKey
   * @return
   */
  public static X509Certificate generateCertificate( final KeyPair keys, final X500Principal subjectDn, final X500Principal signer, final PrivateKey signingKey ) {
    return Crypto.getCertificateProvider( ).generateCertificate( keys, subjectDn, signer, signingKey );
  }
  
  /**
   * TODO: DOCUMENT Certs.java
   * 
   * @param keys
   * @param subjectDn
   * @return
   */
  public static X509Certificate generateCertificate( final KeyPair keys, final X500Principal subjectDn ) {
    return Crypto.getCertificateProvider( ).generateCertificate( keys, subjectDn );
  }
  
  /**
   * TODO: DOCUMENT Certs.java
   * 
   * @param keys
   * @param userName
   * @return
   */
  public static X509Certificate generateCertificate( final KeyPair keys, final String userName ) {
    return Crypto.getCertificateProvider( ).generateCertificate( keys, userName );
  }
  
  /**
   * TODO: DOCUMENT Certs.java
   * 
   * @return
   */
  public static KeyPair generateKeyPair( ) {
    return Crypto.getCertificateProvider( ).generateKeyPair( );
  }
  
  /**
   * TODO: DOCUMENT Certs.java
   * 
   * @param keys
   * @param userName
   * @return
   */
  public static X509Certificate generateServiceCertificate( final KeyPair keys, final String userName ) {
    return Crypto.getCertificateProvider( ).generateServiceCertificate( keys, userName );
  }
  
  public static String getFingerPrint( Key privKey ) {
    return Crypto.getCertificateProvider( ).getFingerPrint( privKey );
  }
  
}
