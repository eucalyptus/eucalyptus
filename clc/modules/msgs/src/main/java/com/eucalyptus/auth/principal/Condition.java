package com.eucalyptus.auth.principal;

import java.io.Serializable;
import java.util.Set;

public interface Condition extends Serializable {

  public String getType( );
  
  public String getKey( );
  
  public Set<String> getValues( );
  
}
