package com.eucalyptus.auth.api;

import java.util.Map;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.Contract;

public interface PolicyEngine {
  
  /**
   * Evaluate authorizations for resource access. Throw exception if the resource access is denied.
   * 
   * @param <T> the resource type.
   * @param resourceClass The type class of the resource
   * @param resourceName The relative name/id of the resource
   * @param resourceAccountId The account ID of the resource
   * @throws AuthException if access is denied.
   */
  public <T> void evaluateAuthorization( Class<T> resourceClass, String resourceName, String resourceAccountId ) throws AuthException;
  
  /**
   * Evaluate quota for resource allocation. Throw exception if the resource allocation is denied.
   * 
   * @param <T> the resource type.
   * @param resourceClass The type class of the resource
   * @throws AuthException if allocation is denied.
   */
  public <T> void evaluateQuota( Class<T> resourceClass, String resourceName ) throws AuthException;
  
}
