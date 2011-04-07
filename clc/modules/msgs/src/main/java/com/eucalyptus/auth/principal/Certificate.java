package com.eucalyptus.auth.principal;

import java.io.Serializable;
import java.security.cert.X509Certificate;
import java.util.Date;
import com.eucalyptus.auth.AuthException;

public interface  Certificate extends /*HasId,*/ Serializable {
  public String getCertificateId( );
  public Boolean isActive( );
  public void setActive( Boolean active ) throws AuthException;
  
  public Boolean isRevoked( );
  public void setRevoked( Boolean revoked ) throws AuthException;
  
  public String getPem( );
  public X509Certificate getX509Certificate( );
  public void setX509Certificate( X509Certificate x509 ) throws AuthException;

  public Date getCreateDate( );
  public void setCreateDate( Date createDate ) throws AuthException;
  
  public User getUser( ) throws AuthException;
  
}
