package com.eucalyptus.auth.util;

import java.security.KeyPair;
import java.security.cert.X509Certificate;
import com.eucalyptus.auth.crypto.Certs;

public class X509CertHelper {
  
  public static String fromCertificate( X509Certificate x509 ) {
    return B64.url.encString( PEMFiles.getBytes( x509 ) );
  }
  
  public static X509Certificate toCertificate( String x509PemString ) {
    return PEMFiles.getCert( B64.url.dec( x509PemString ) );
  }
  
  public static X509Certificate createCertificate( String userName ) throws Exception {
    KeyPair keyPair = Certs.generateKeyPair( );
    X509Certificate x509 = Certs.generateCertificate( keyPair, userName );
    x509.checkValidity( );
    return x509;
  }
}
