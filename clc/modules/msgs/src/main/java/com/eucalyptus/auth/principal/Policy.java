package com.eucalyptus.auth.principal;

import java.io.Serializable;
import com.eucalyptus.auth.AuthException;

public interface Policy extends /*HasId, */Serializable {
  public String getPolicyId( );
  public String getName( );
  
  public String getText( );
  
  public String getVersion( );
  
  public Group getGroup( ) throws AuthException;
  
}
