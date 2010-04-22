package com.eucalyptus.auth.api;

import java.security.Key;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import javax.security.auth.x500.X500Principal;

public interface CertificateProvider extends BaseSecurityProvider {
  /**
   * TODO: DOCUMENT CryptoProvider.java
   * @return
   */
  public abstract KeyPair generateKeyPair( );
  public abstract String getFingerPrint( Key privKey );
  
  /**
   * Following operations produce a signed certificate. If the <tt>signer</tt> certificate is null, then the certificate generated is self signed.
   * 
   * @param keys
   * @param subjectDn
   * @param signer
   * @return
   */
  public abstract X509Certificate generateCertificate( KeyPair keys, X500Principal subjectDn, X500Principal signer, PrivateKey signingKey );
  public abstract X509Certificate generateCertificate( KeyPair keys, String userName );
  public abstract X509Certificate generateCertificate( KeyPair keys, X500Principal subjectDn );

  /**
   * Mechanically identical to the above, but signed by the root cert.
   */
  public abstract X509Certificate generateServiceCertificate( KeyPair keys, String userName );

}
