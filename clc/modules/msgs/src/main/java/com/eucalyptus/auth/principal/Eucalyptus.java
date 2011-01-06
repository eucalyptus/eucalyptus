package com.eucalyptus.auth.principal;

import java.math.BigInteger;
import java.net.URL;
import java.security.cert.X509Certificate;
import java.util.List;
import com.eucalyptus.component.auth.SystemCredentialProvider;


public class Eucalyptus implements ComponentPrincipal {

  @Override
  public String getName( ) {
    return this.getClass( ).getSimpleName( ).toLowerCase( );
  }

  @Override
  public X509Certificate getX509Certificate( ) {
    return SystemCredentialProvider.getCredentialProvider( null );
  }

  /**
   * TODO: DOCUMENT
   * @see com.eucalyptus.auth.principal.credential.X509Principal#getAllX509Certificates()
   * @return
   */
  @Override
  public List<X509Certificate> getAllX509Certificates( ) {
    return null;
  }

  /**
   * TODO: DOCUMENT
   * @see com.eucalyptus.auth.principal.credential.X509Principal#setX509Certificate(java.security.cert.X509Certificate)
   * @param cert
   */
  @Override
  public void setX509Certificate( X509Certificate cert ) {}

  /**
   * TODO: DOCUMENT
   * @see com.eucalyptus.auth.principal.credential.X509Principal#revokeX509Certificate()
   */
  @Override
  public void revokeX509Certificate( ) {}

  /**
   * TODO: DOCUMENT
   * @see com.eucalyptus.auth.principal.credential.CredentialPrincipal#getNumber()
   * @return
   */
  @Override
  public BigInteger getNumber( ) {
    return null;
  }

  /**
   * TODO: DOCUMENT
   * @see com.eucalyptus.auth.principal.credential.HmacPrincipal#revokeSecretKey()
   */
  @Override
  public void revokeSecretKey( ) {}

  /**
   * TODO: DOCUMENT
   * @see com.eucalyptus.auth.principal.credential.HmacPrincipal#getQueryId()
   * @return
   */
  @Override
  public String getQueryId( ) {
    return null;
  }

  /**
   * TODO: DOCUMENT
   * @see com.eucalyptus.auth.principal.credential.HmacPrincipal#getSecretKey()
   * @return
   */
  @Override
  public String getSecretKey( ) {
    return null;
  }

  /**
   * TODO: DOCUMENT
   * @see com.eucalyptus.auth.principal.credential.HmacPrincipal#setQueryId(java.lang.String)
   * @param queryId
   */
  @Override
  public void setQueryId( String queryId ) {}

  /**
   * TODO: DOCUMENT
   * @see com.eucalyptus.auth.principal.credential.HmacPrincipal#setSecretKey(java.lang.String)
   * @param secretKey
   */
  @Override
  public void setSecretKey( String secretKey ) {}

  /**
   * TODO: DOCUMENT
   * @see com.eucalyptus.auth.principal.ComponentPrincipal#getAddress()
   * @return
   */
  @Override
  public URL getAddress( ) {
    return null;
  }

}
