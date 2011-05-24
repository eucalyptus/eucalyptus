package com.eucalyptus.auth.principal;

import java.io.Serializable;
import java.util.Date;
import com.eucalyptus.auth.AuthException;

public interface AccessKey extends /*HasId, */Serializable {

  public Boolean isActive( );
  public void setActive( Boolean active ) throws AuthException;
  
  public String getAccessKey( );
  public String getSecretKey( );
  
  /*
   * Returns the SECRET key -- also available via longer named {@link#getSecretKey()}
   * @deprecated {@link #getSecretKey()}
   * @see {@link com.eucalyptus.auth.Accounts}
   */
//  public String getKey( );
//  public void setKey( String key ) throws AuthException; TODO:YE:  AccessKey should be immutable.  Is there a reason to allow mutator access?
  
  public Date getCreateDate( );
  public void setCreateDate( Date createDate ) throws AuthException;
  
  public User getUser( ) throws AuthException;
  
}
