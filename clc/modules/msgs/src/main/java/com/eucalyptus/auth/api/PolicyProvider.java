package com.eucalyptus.auth.api;

import java.security.cert.X509Certificate;
import java.util.List;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.PolicyParseException;
import com.eucalyptus.auth.principal.Authorization;

/**
 * APIs related to policy.
 * 
 * @author wenye
 *
 */
public interface PolicyProvider {

  /**
   * Attach a new policy (in JSON text) to a group.
   * 
   * @param policy The policy in JSON.
   * @param groupName The name of the group to attach.
   * @param accountName The name of the account of the group.
   * @return The new policy ID
   * @throws AuthException for auth data errors
   * @throws PolicyParseException for policy parsing errors
   */
  public String attachGroupPolicy( String policy, String groupName, String accountName ) throws AuthException, PolicyParseException;
  
  /**
   * Attach a new policy (in JSON text) to a user.
   * 
   * @param policy The policy in JSON.
   * @param userName The name of the user to attach.
   * @param accountName The name of the account of the user.
   * @return The new policy ID
   * @throws AuthException for auth data errors
   * @throws PolicyParseException for policy parsing errors
   */
  public String attachUserPolicy( String policy, String userName, String accountName ) throws AuthException, PolicyParseException;
  
 
  /**
   * Remove a policy from a group.
   * 
   * @param policyId The ID of the policy to remove
   * @param groupName The name of the group to remove from
   * @param accountName The group's account name
   * @throws AuthException for any error
   */
  public void removeGroupPolicy( String policyId, String groupName, String accountName ) throws AuthException;
  
  /**
   * Lookup a user's authorization list.
   * 
   * @param resourceType Search by resource type
   * @param userId Search by user ID
   * @return The list of authorizations that can be potentially applied to the user
   * @throws AuthException for any error
   */
  public List<? extends Authorization> lookupAuthorizations( String resourceType, String userId ) throws AuthException;
  
  /**
   * Lookup account global authorizations.
   * 
   * @param resourceType The type of resource
   * @param accountId The account ID
   * @return The list of authorizations that are account global (i.e. attached to admin's user group)
   * @throws AuthException for any error
   */
  public List<? extends Authorization> lookupAccountGlobalAuthorizations( String resourceType, String accountId ) throws AuthException;
  
  /**
   * Lookup a user's quota.
   * 
   * @param resourceType Search by resource type
   * @param userId Search by user ID
   * @return The list of quotas that can be potentially applied to the user or the groups of the user.
   * @throws AuthException for any error
   */
  public List<? extends Authorization> lookupQuotas( String resourceType, String userId ) throws AuthException;
  
  /**
   * Lookup account global quotas.
   * 
   * @param resourceType The type of the resource.
   * @param accountId The account ID
   * @return The list of quotas that are account global (i.e. attached to admin's user group)
   * @throws AuthException for any error
   */
  public List<? extends Authorization> lookupAccountGlobalQuotas( String resourceType, String accountId ) throws AuthException;
  
  /**
   * Check if a certificate is active (not revoked and active).
   * 
   * @param cert The certificate to check.
   * @return The revoke status of the cert.
   * @throws AuthException
   */
  public boolean isCertificateActive( X509Certificate cert ) throws AuthException;
  
}
