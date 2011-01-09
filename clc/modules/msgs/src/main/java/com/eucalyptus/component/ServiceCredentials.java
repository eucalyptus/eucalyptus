package com.eucalyptus.component;

import java.security.KeyPair;
import java.security.cert.X509Certificate;

/**
 *
 */
public class ServiceCredentials implements ComponentInformation {
  private final Service parent;
  private KeyPair         keys;
  private X509Certificate certificate;
  
  public ServiceCredentials( Service parent ) {
    this.parent = parent;
  }
  
  @Override
  public String getName( ) {
    return this.parent.getName( );
  }
  
  public KeyPair getKeys( ) {
    return this.keys;
  }
  
  public void setKeys( KeyPair keys ) {
    this.keys = keys;
  }
  
  public X509Certificate getCertificate( ) {
    return this.certificate;
  }
  
  public void setCertificate( X509Certificate certificate ) {
    this.certificate = certificate;
  }
  
  protected final Service getParent( ) {
    return this.parent;
  }

  @Override
  public String toString( ) {
    if( this.certificate != null ) {
      return String.format( "ServiceCredentials name=%s cert-sn=%s cert-dn=%s", this.getName( ), this.getCertificate( ).getSerialNumber( ), this.getCertificate( ).getSubjectDN( ) );
    } else {
      return String.format( "Credentials name=%s cert-sn=%s cert-dn=%s", this.getName( ), "none", "none" );
    }
  }
  
  
  
}
