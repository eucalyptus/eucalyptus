package com.eucalyptus.auth.principal;

import java.io.Serializable;

public interface Policy extends Serializable {
  
  public String getName( );

  public String getPolicyId( );
  
  public String getPolicyText( );
  
  public String getPolicyVersion( );
}
