package com.eucalyptus.auth.util;

import java.io.UnsupportedEncodingException;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import com.eucalyptus.crypto.util.B64;
import com.eucalyptus.crypto.util.PEMFiles;

public class X509CertHelper {
  
  public static String fromCertificate( X509Certificate x509 ) {
    return B64.url.encString( PEMFiles.getBytes( x509 ) );
  }
  
  public static X509Certificate toCertificate( String x509PemString ) {
    return PEMFiles.getCert( B64.url.dec( x509PemString ) );
  }
  
  public static String certificateToPem( X509Certificate x509 ) {
    try {
      return new String( PEMFiles.getBytes( x509 ), "UTF-8" );
    } catch ( UnsupportedEncodingException e ) {
      return new String( PEMFiles.getBytes( x509 ) );
    }
  }
  
  public static X509Certificate pemToCertificate( String pem ) {
    try {
      return PEMFiles.getCert( pem.getBytes( "UTF-8" ) );
    } catch ( UnsupportedEncodingException e ) {
      return PEMFiles.getCert( pem.getBytes( ) );
    }
  }
  
  public static String privateKeyToPem( PrivateKey pk ) {
    try {
      return new String( PEMFiles.getBytes( pk ), "UTF-8" );
    } catch ( UnsupportedEncodingException e ) {
      return new String( PEMFiles.getBytes( pk ) );
    }
  }
  
}
