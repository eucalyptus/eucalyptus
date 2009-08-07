package com.eucalyptus.ws.util;

import org.apache.ws.security.WSSecurityException;
import org.apache.ws.security.components.crypto.Crypto;

import java.io.InputStream;
import java.math.BigInteger;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

public class CredentialProxy implements Crypto {

  private final X509Certificate cert;
  private final PrivateKey pk;
  
  public CredentialProxy( X509Certificate cert, PrivateKey pk ) {
    super( );
    this.cert = cert;
    this.pk = pk;
  }

  @Override
  public X509Certificate[] getCertificates( final String arg0 ) throws WSSecurityException {
    return new X509Certificate[] { this.cert };
  }

  @Override
  public PrivateKey getPrivateKey( final String alias, final String password ) throws Exception {
    return this.pk;
  }

  @Override public X509Certificate loadCertificate( final InputStream inputStream ) throws WSSecurityException { return null; }
  @Override public X509Certificate[] getX509Certificates( final byte[] bytes, final boolean b ) throws WSSecurityException { return new X509Certificate[0]; }
  @Override public byte[] getCertificateData( final boolean b, final X509Certificate[] x509Certificates ) throws WSSecurityException { return new byte[0]; }
  @Override public String getAliasForX509Cert( final Certificate certificate ) throws WSSecurityException { return null; }
  @Override public String getAliasForX509Cert( final String s ) throws WSSecurityException { return null; }
  @Override public String getAliasForX509Cert( final String s, final BigInteger bigInteger ) throws WSSecurityException { return null; }
  @Override public String getAliasForX509Cert( final byte[] bytes ) throws WSSecurityException { return null; }
  @Override public String getDefaultX509Alias( ) { return null; }
  @Override public byte[] getSKIBytesFromCert( final X509Certificate x509Certificate ) throws WSSecurityException { return new byte[0]; }
  @Override public String getAliasForX509CertThumb( final byte[] bytes ) throws WSSecurityException { return null; }
  @Override public KeyStore getKeyStore( ) { return null; }
  @Override public CertificateFactory getCertificateFactory( ) throws WSSecurityException { return null; }
  @Override public boolean validateCertPath( final X509Certificate[] x509Certificates ) throws WSSecurityException { return false; }
  @Override public String[] getAliasesForDN( final String s ) throws WSSecurityException { return new String[0]; }
}
