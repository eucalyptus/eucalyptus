package com.eucalyptus.auth.api;

import java.util.Map;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.Contract;
import com.eucalyptus.auth.principal.User;
import edu.ucsb.eucalyptus.msgs.BaseMessage;

public interface PolicyEngine {
  
  /**
   * Evaluate authorizations for resource access. Throw exception if the resource access is denied.
   * 
   * @param <T> the resource type.
   * @param resourceClass The type class of the resource
   * @param resourceName The relative name/id of the resource
   * @param resourceAccountId The account ID of the resource
   * @param request The request that is associated with the access
   * @param requestUser The user that 
   * @return the map of contracts if access is granted
   * @throws AuthException if access is denied.
   */
  public <T> Map<String, Contract> evaluateAuthorization( Class<T> resourceClass, String resourceName, String resourceAccountId, BaseMessage request, User requestUser ) throws AuthException;
  
  /**
   * Evaluate quota for resource allocation. Throw exception if the resource allocation is denied.
   * 
   * @param <T> the resource type.
   * @param resourceClass The type class of the resource
   * @param resourceName The name of the resource to allocate
   * @param request The request that is associated with the access
   * @param requestUser The user that 
   * @param quantity The quantity of the resource to allocate
   * @throws AuthException if allocation is denied.
   */
  public <T> void evaluateQuota( Class<T> resourceClass, String resourceName, Integer quantity, BaseMessage request, User requestUser ) throws AuthException;
  
}
