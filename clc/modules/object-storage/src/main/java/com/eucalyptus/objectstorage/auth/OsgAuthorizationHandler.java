/*************************************************************************
 * Copyright 2009-2015 Eucalyptus Systems, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 ************************************************************************/

package com.eucalyptus.objectstorage.auth;

import java.util.Collections;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.log4j.Logger;

import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.AuthContextSupplier;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.Permissions;
import com.eucalyptus.auth.PolicyResourceContext;
import com.eucalyptus.auth.PolicyResourceContext.PolicyResourceInfo;
import com.eucalyptus.auth.policy.PolicySpec;
import com.eucalyptus.auth.principal.AccountIdentifiers;
import com.eucalyptus.auth.principal.Principals;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.auth.principal.UserPrincipal;
import com.eucalyptus.context.Context;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.context.NoSuchContextException;
import com.eucalyptus.objectstorage.entities.S3AccessControlledEntity;
import com.eucalyptus.objectstorage.msgs.CreateBucketType;
import com.eucalyptus.objectstorage.msgs.ObjectStorageRequestType;
import com.eucalyptus.objectstorage.policy.AdminOverrideAllowed;
import com.eucalyptus.objectstorage.policy.RequiresACLPermission;
import com.eucalyptus.objectstorage.policy.RequiresPermission;
import com.eucalyptus.objectstorage.policy.ResourceType;
import com.eucalyptus.objectstorage.util.ObjectStorageProperties;
import com.eucalyptus.system.Ats;
import com.google.common.base.Strings;

public class OsgAuthorizationHandler implements RequestAuthorizationHandler {
  private static final Logger LOG = Logger.getLogger(OsgAuthorizationHandler.class);
  private static final RequestAuthorizationHandler authzHandler = new OsgAuthorizationHandler();
  private static RequestAuthorizationHandler mocked = null;

  public static RequestAuthorizationHandler getInstance() {
    if (mocked != null) {
      return mocked;
    }
    return authzHandler;
  }

  public static void setInstance(RequestAuthorizationHandler mock) {
    mocked = mock;
  }

  /**
   * Does the current request have an authenticated user? Or is it anonymous?
   * 
   * @return
   */
  protected static boolean isUserAnonymous(User usr) {
    return Principals.nobodyUser().equals(usr);
  }

  /**
   * Evaluates the authorization for the operation requested, evaluates IAM, ACL, and bucket policy (bucket policy not yet supported).
   * 
   * @param request
   * @param bucketResourceEntity
   * @param objectResourceEntity
   * @param resourceAllocationSize the size for the quota check(s) if applicable
   * @return
   */
  public <T extends ObjectStorageRequestType> boolean operationAllowed(@Nonnull T request,
      @Nullable final S3AccessControlledEntity bucketResourceEntity, @Nullable final S3AccessControlledEntity objectResourceEntity,
      long resourceAllocationSize) throws IllegalArgumentException {
    /*
     * Process the operation's authz requirements based on the request type annotations
     */
    Ats requestAuthzProperties = Ats.from(request);
    ObjectStorageProperties.Permission[] requiredBucketACLPermissions = null;
    ObjectStorageProperties.Permission[] requiredObjectACLPermissions = null;
    Boolean allowOwnerOnly = null;
    ObjectStorageProperties.Resource[] requiredOwnerOf = null;
    RequiresACLPermission requiredACLs = requestAuthzProperties.get(RequiresACLPermission.class);
    if (requiredACLs != null) {
      requiredBucketACLPermissions = requiredACLs.bucket();
      requiredObjectACLPermissions = requiredACLs.object();
      allowOwnerOnly = requiredACLs.ownerOnly();
      requiredOwnerOf = requiredACLs.ownerOf();
    } else {
      // No ACL annotation is ok, maybe a admin only op
    }

    String[] requiredActions = null;
    RequiresPermission perms = requestAuthzProperties.get(RequiresPermission.class);
    if (perms != null) {
      requiredActions = perms.value();
    }

    Boolean allowAdmin = (requestAuthzProperties.get(AdminOverrideAllowed.class) != null);
    Boolean allowOnlyAdmin =
        (requestAuthzProperties.get(AdminOverrideAllowed.class) != null) && requestAuthzProperties.get(AdminOverrideAllowed.class).adminOnly();

    // Must have at least one of: admin-only, owner-only, ACL, or IAM.
    if (requiredBucketACLPermissions == null && requiredObjectACLPermissions == null && requiredActions == null && !allowAdmin) {
      // Insufficient permission set on the message type.
      LOG.error("Insufficient permission annotations on type: " + request.getClass().getName() + " cannot evaluate authorization");
      return false;
    }

    String resourceType = null;
    if (requestAuthzProperties.get(ResourceType.class) != null) {
      resourceType = requestAuthzProperties.get(ResourceType.class).value();
    }

    // Use these variables to isolate where all the AuthExceptions can happen on account/user lookups
    UserPrincipal requestUser = null;
    String requestAccountNumber = null;
    String requestCanonicalId = null;
    AuthContextSupplier authContext = null;
    try {
      // Use context if available as it saves a DB lookup
      try {
        Context ctx = Contexts.lookup(request.getCorrelationId());
        requestUser = ctx.getUser();
        requestAccountNumber = ctx.getAccountNumber();
        authContext = ctx.getAuthContext();
      } catch (NoSuchContextException e) {
        requestUser = null;
        requestAccountNumber = null;
        authContext = null;
      }

      // This is not an expected path, but if no context found use the request credentials itself
      if (requestUser == null && !Strings.isNullOrEmpty(request.getEffectiveUserId())) {
        requestUser = Accounts.lookupPrincipalByUserId(request.getEffectiveUserId());
        requestAccountNumber = requestUser.getAccountNumber();
      }

      if (requestUser == null) {
        // Set to anonymous user since all else failed
        requestUser = Principals.nobodyUser();
        requestAccountNumber = requestUser.getAccountNumber();
      }

      requestCanonicalId = requestUser.getCanonicalId();
    } catch (AuthException e) {
      LOG.error("Failed to get user for request, cannot verify authorization: " + e.getMessage(), e);
      return false;
    }

    if (allowAdmin && requestUser.isSystemAdmin()) {
      // Admin override
      return true;
    }

    if (authContext == null) {
      authContext = Permissions.createAuthContextSupplier(requestUser, Collections.<String, String>emptyMap());
    }

    final String resourceOwnerAccountNumber;
    final String bucketOwnerAccountNumber;
    final PolicyResourceInfo policyResourceInfo;
    if (resourceType == null) {
      LOG.error("No resource type found in request class annotations, cannot process.");
      return false;
    } else {
      try {
        // Ensure we have the proper resource entities present and get owner info
        switch (resourceType) {
          case PolicySpec.S3_RESOURCE_BUCKET:
            // Get the bucket owner. bucket and resource owner are same in this case
            if (bucketResourceEntity == null) {
              LOG.error("Could not check access for operation due to no bucket resource entity found");
              return false;
            } else {
              bucketOwnerAccountNumber =
                  resourceOwnerAccountNumber = lookupAccountIdByCanonicalId(bucketResourceEntity.getOwnerCanonicalId());
              policyResourceInfo = PolicyResourceContext.resourceInfo(resourceOwnerAccountNumber, bucketResourceEntity);
            }
            break;
          case PolicySpec.S3_RESOURCE_OBJECT:
            // get the object owner.
            if (objectResourceEntity == null) {
              LOG.error("Could not check access for operation due to no object resource entity found");
              return false;
            } else {
              resourceOwnerAccountNumber = lookupAccountIdByCanonicalId(objectResourceEntity.getOwnerCanonicalId());
              policyResourceInfo = PolicyResourceContext.resourceInfo(resourceOwnerAccountNumber, objectResourceEntity);
            }
            // get the bucket owner account number as the bucket and object owner may be different
            if (bucketResourceEntity == null) { // cannot be null as every object is associated with one bucket
              LOG.error("Could not check access for operation due to no bucket resource entity found");
              return false;
            } else {
              bucketOwnerAccountNumber = lookupAccountIdByCanonicalId(bucketResourceEntity.getOwnerCanonicalId());
            }
            break;
          default:
            LOG.error("Unknown resource type looking up resource owner. Disallowing operation.");
            return false;
        }
      } catch (AuthException e) {
        LOG.error("Exception caught looking up resource owner. Disallowing operation.", e);
        return false;
      }
    }

    // Get the resourceId based on IAM resource type
    String resourceId = null;
    if (resourceId == null) {
      if (PolicySpec.S3_RESOURCE_BUCKET.equals(resourceType)) {
        resourceId = request.getBucket();
      } else if (PolicySpec.S3_RESOURCE_OBJECT.equals(resourceType)) {
        resourceId = request.getFullResource();
      }
    }

    // Override for 'eucalyptus' account and workaroud for EUCA-11346
    // Skip ACL checks for 'eucalyptus' account only. ACL checks must be performed for all other accounts including system accounts
    // IAM checks must be performed for all accounts
    if (allowAdmin && requestUser.getAccountAlias() != null && AccountIdentifiers.SYSTEM_ACCOUNT.equals(requestUser.getAccountAlias())) {
      return iamPermissionsAllow(authContext, requiredActions, policyResourceInfo, resourceType, resourceId, resourceAllocationSize);
    }

    // Don't allow anonymous to create buckets. EUCA-12902
    if (PolicySpec.S3_RESOURCE_BUCKET.equals(resourceType) && 
        Principals.nobodyAccount( ).getAccountNumber( ).equals(resourceOwnerAccountNumber) &&
        request instanceof CreateBucketType) {
      return false;
    }

    if (requiredBucketACLPermissions == null && requiredObjectACLPermissions == null) {
      throw new IllegalArgumentException("No requires-permission actions found in request class annotations, cannot process.");
    }

    /*
     * Bucket or object owner only? It is expected that ownerOnly flag can be used solely or in combination with ACL checks. If owner checks are
     * required, evaluate them first before evaluating the ACLs
     */
    if (allowOwnerOnly != null && allowOwnerOnly) { // owner checks are in effect
      if (requiredOwnerOf == null || requiredOwnerOf.length == 0) {
        LOG.error("Owner only flag does not include resource (bucket, object) that ownership checks should be applied to");
        return false;
      }

      Boolean isRequestByOwner = false;
      for (ObjectStorageProperties.Resource resource : requiredOwnerOf) {
        if (ObjectStorageProperties.Resource.bucket.equals(resource)) {
          isRequestByOwner = isRequestByOwner || bucketOwnerAccountNumber.equals(requestAccountNumber);
        } else {
          isRequestByOwner = isRequestByOwner || resourceOwnerAccountNumber.equals(requestAccountNumber);
        }
      }

      if (!isRequestByOwner) {
        LOG.debug("Request is rejected by ACL checks due to account ownership requirements");
        return false;
      }
    } else {
      // owner check does not apply
    }

    /* ACL Checks: Is the user's account allowed? */
    Boolean aclAllow = false;
    if ((requiredBucketACLPermissions != null && requiredBucketACLPermissions.length > 0)
        || (requiredObjectACLPermissions != null && requiredObjectACLPermissions.length > 0)) { // check ACLs if any

      // Check bucket ACLs, if any
      if (requiredBucketACLPermissions != null && requiredBucketACLPermissions.length > 0) {

        // Evaluate the bucket ACL, any matching grant gives permission
        for (ObjectStorageProperties.Permission permission : requiredBucketACLPermissions) {
          aclAllow = aclAllow || bucketResourceEntity.can(permission, requestCanonicalId);
        }
      }

      // Check object ACLs, if any
      if (requiredObjectACLPermissions != null && requiredObjectACLPermissions.length > 0) {
        if (objectResourceEntity == null) {
          // There are object ACL requirements but no object entity to check. fail.
          // Don't bother with other checks, this is an invalid state
          LOG.error("Null bucket resource, cannot evaluate bucket ACL");
          return false;
        }
        for (ObjectStorageProperties.Permission permission : requiredObjectACLPermissions) {
          aclAllow = aclAllow || objectResourceEntity.can(permission, requestCanonicalId);
        }
      }
    } else { // No ACLs, ownership would have been used to determine privilege
      aclAllow = true;
    }

    if (isUserAnonymous(requestUser)) {
      // Skip the IAM checks for anonymous access since they will always fail and aren't valid for anonymous users.
      return aclAllow;
    } else {
      Boolean iamAllow = iamPermissionsAllow(authContext, requiredActions, policyResourceInfo, resourceType, resourceId, resourceAllocationSize);
      // Must have both acl and iam allow (account & user)
      return aclAllow && iamAllow;
    }
  }

  private String lookupAccountIdByCanonicalId( final String canonicalId ) throws AuthException {
    if ( AccountIdentifiers.NOBODY_CANONICAL_ID.equals( canonicalId ) ) {
      return Principals.nobodyAccount( ).getAccountNumber( );
    } else {
      return Accounts.lookupAccountIdByCanonicalId( canonicalId );
    }
  }

  private static Boolean iamPermissionsAllow(final AuthContextSupplier authContext, final String[] requiredActions,
      final PolicyResourceInfo policyResourceInfo, final String resourceType, final String resourceId, final long resourceAllocationSize) {
    /* IAM checks: Is the user allowed within the account? */
    // the Permissions.isAuthorized() handles the default deny for each action.
    boolean iamAllow = true; // Evaluate each iam action required, all must be allowed
    for (String action : requiredActions) {
      try (final PolicyResourceContext context = PolicyResourceContext.of(policyResourceInfo, action)) {
        // Any deny overrides an allow
        // Note: explicitly set resourceOwnerAccount to null here, otherwise iam will reject even if the ACL checks
        // were valid, let ACLs handle cross-account access.
        iamAllow &=
            Permissions.isAuthorized(PolicySpec.VENDOR_S3, resourceType, resourceId, null, action, authContext)
                && Permissions.canAllocate(PolicySpec.VENDOR_S3, resourceType, resourceId, action, authContext, resourceAllocationSize);
      }
    }
    return iamAllow;
  }

}
