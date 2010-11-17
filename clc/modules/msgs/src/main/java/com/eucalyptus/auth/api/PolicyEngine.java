package com.eucalyptus.auth.api;

import com.eucalyptus.auth.AuthException;

public interface PolicyEngine {
  
  /**
   * Evaluate the resource access through the policy engine. Throw exception if the resource access is denied.
   * 
   * @param resourceType The type of the resource
   * @param resourceName The relative name/id of the resource
   * @throws AuthException if access is denied.
   */
  public <T> void evaluateAuthorization( Class<T> resourceType, String resourceName ) throws AuthException;
  
}
