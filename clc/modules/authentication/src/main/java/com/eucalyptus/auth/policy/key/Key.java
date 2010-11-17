package com.eucalyptus.auth.policy.key;

import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.principal.Authorization;

public interface Key {

  public String value( Authorization auth ) throws AuthException;
  
}
