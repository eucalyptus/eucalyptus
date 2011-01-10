package com.eucalyptus.auth.principal;

import java.io.Serializable;
import java.security.Principal;

/**
 * The interface for the user account.
 * 
 * @author wenye
 *
 */
public interface Account extends BasePrincipal, Serializable {
  
  public static final String SYSTEM_ACCOUNT = "eucasystem";

  public String getAccountId( );
  
}
