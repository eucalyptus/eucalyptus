package com.eucalyptus.component;

import java.security.KeyPair;
import java.security.cert.X509Certificate;

public class Credentials implements ComponentInformation {
  private final Component parent;
  private KeyPair         keys;
  private X509Certificate certificate;
  
  public Credentials( Component parent ) {
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
  
  /**
   * @return the parent
   */
  protected final Component getParent( ) {
    return this.parent;
  }
  
}
