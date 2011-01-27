package com.eucalyptus.auth.principal;

import java.io.Serializable;
import java.util.Set;
import com.eucalyptus.auth.AuthException;

public interface Condition extends Serializable {

  public String getType( );
  
  public String getKey( );
  
  public Set<String> getValues( ) throws AuthException;
  
}
