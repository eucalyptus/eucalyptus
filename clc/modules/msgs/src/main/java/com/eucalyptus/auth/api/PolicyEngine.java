package com.eucalyptus.auth.api;

import java.util.Map;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.Contract;
import com.eucalyptus.auth.principal.Account;
import com.eucalyptus.auth.principal.User;

public interface PolicyEngine {
  
  /**
   * Evaluate authorizations for a request to access a resource.
   * 
   * @param resourceType
   * @param resourceName
   * @param resourceAccount
   * @param action
   * @param requestUser
   * @return
   * @throws AuthException
   */
  public Map<String, Contract> evaluateAuthorization( String resourceType, String resourceName, Account resourceAccount, String action, User requestUser ) throws AuthException;
  
  /**
   * Evaluate quota for a request to allocate a resource.
   * 
   * @param resourceType
   * @param resourceName
   * @param action
   * @param requestUser
   * @param quantity
   * @throws AuthException
   */
  public void evaluateQuota( String resourceType, String resourceName, String action, User requestUser, Long quantity) throws AuthException;

}
