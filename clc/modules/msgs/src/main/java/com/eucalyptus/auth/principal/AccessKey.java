package com.eucalyptus.auth.principal;

import java.io.Serializable;
import java.util.Date;
import com.eucalyptus.auth.AuthException;

public interface AccessKey extends HasId, Serializable {

  public Boolean isActive( );
  public void setActive( Boolean active ) throws AuthException;
  
  public String getKey( );
  public void setKey( String key ) throws AuthException;
  
  public Date getCreateDate( );
  public void setCreateDate( Date createDate ) throws AuthException;
  
  public User getUser( ) throws AuthException;
  
}
