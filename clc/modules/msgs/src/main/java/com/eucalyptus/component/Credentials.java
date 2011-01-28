package com.eucalyptus.component;

import java.security.KeyPair;
import java.security.cert.X509Certificate;
import com.eucalyptus.component.auth.SystemCredentialProvider;

/**
 *
 */
public class Credentials implements ComponentInformation {
  private final Service parent;
  
  public Credentials( Service parent ) {
    this.parent = parent;
  }
  
  @Override
  public String getName( ) {
    return this.parent.getName( );
  }
  
  public KeyPair getKeys( ) {
    return SystemCredentialProvider.getCredentialProvider( this.parent.getParent( ).getIdentity( ) ).getKeyPair( );
  }
  
  public X509Certificate getCertificate( ) {
    return SystemCredentialProvider.getCredentialProvider( this.parent.getParent( ).getIdentity( ) ).getCertificate( );
  }
  
  protected final Service getParent( ) {
    return this.parent;
  }

  @Override
  public String toString( ) {
    if( this.getCertificate( ) != null ) {
      return String.format( "ServiceCredentials name=%s cert-sn=%s cert-dn=%s", this.getName( ), this.getCertificate( ).getSerialNumber( ), this.getCertificate( ).getSubjectDN( ) );
    } else {
      return String.format( "Credentials name=%s cert-sn=%s cert-dn=%s", this.getName( ), "none", "none" );
    }
  }
  
  
  
}
