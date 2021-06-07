/*************************************************************************
 * Copyright 2009-2015 Ent. Services Development Corporation LP
 *
 * Redistribution and use of this software in source and binary forms,
 * with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *   Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 *   Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer
 *   in the documentation and/or other materials provided with the
 *   distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 ************************************************************************/

package com.eucalyptus.objectstorage.auth;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.AuthContextSupplier;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.Permissions;
import com.eucalyptus.auth.PolicyResourceContext;
import com.eucalyptus.auth.PolicyResourceContext.PolicyResourceInfo;
import com.eucalyptus.auth.principal.AccountFullName;
import com.eucalyptus.auth.principal.AccountIdentifiers;
import com.eucalyptus.auth.principal.PolicyScope;
import com.eucalyptus.auth.principal.PolicyVersion;
import com.eucalyptus.auth.principal.PolicyVersions;
import com.eucalyptus.auth.principal.Principals;
import com.eucalyptus.auth.principal.UserPrincipal;
import com.eucalyptus.component.annotation.ComponentNamed;
import com.eucalyptus.context.Context;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.context.NoSuchContextException;
import com.eucalyptus.objectstorage.ObjectState;
import com.eucalyptus.objectstorage.entities.Bucket;
import com.eucalyptus.objectstorage.entities.ObjectEntity;
import com.eucalyptus.objectstorage.entities.S3AccessControlledEntity;
import com.eucalyptus.objectstorage.msgs.CreateBucketType;
import com.eucalyptus.objectstorage.msgs.ObjectStorageRequestType;
import com.eucalyptus.objectstorage.policy.AdminOverrideAllowed;
import com.eucalyptus.objectstorage.policy.RequiresACLPermission;
import com.eucalyptus.objectstorage.policy.RequiresPermission;
import com.eucalyptus.objectstorage.policy.ResourceType;
import com.eucalyptus.objectstorage.policy.S3PolicySpec;
import com.eucalyptus.objectstorage.util.ObjectStorageProperties;
import com.eucalyptus.system.Ats;
import io.vavr.Tuple;
import io.vavr.Tuple3;
import io.vavr.Tuple4;
import io.vavr.control.Option;

@ComponentNamed
public class OsgAuthorizationHandler implements RequestAuthorizationHandler {
  private static final Logger LOG = Logger.getLogger(OsgAuthorizationHandler.class);

  /**
   * Evaluates the authorization for the operation requested, evaluates IAM, ACL, and bucket policy (bucket policy not yet supported).
   * 
   * @param request
   * @param bucketResourceEntity
   * @param objectResourceEntity
   * @param resourceAllocationSize the size for the quota check(s) if applicable
   * @return true if authorized
   */
  public <T extends ObjectStorageRequestType> boolean operationAllowed(@Nonnull T request,
      @Nullable final Bucket bucketResourceEntity, @Nullable final ObjectEntity objectResourceEntity,
      long resourceAllocationSize) throws IllegalArgumentException {
    /*
     * Process the operation's authz requirements based on the request type annotations
     */
    Ats requestAuthzProperties = Ats.from(request);
    ObjectStorageProperties.Permission[] requiredBucketACLPermissions = null;
    ObjectStorageProperties.Permission[] requiredObjectACLPermissions = null;
    boolean allowOwnerOnly = true;
    ObjectStorageProperties.Resource[] requiredOwnerOf = null;
    RequiresACLPermission requiredACLs = requestAuthzProperties.get(RequiresACLPermission.class);
    if (requiredACLs != null) {
      requiredBucketACLPermissions = requiredACLs.bucket();
      requiredObjectACLPermissions = requiredACLs.object();
      requiredOwnerOf = requiredACLs.ownerOf();
      allowOwnerOnly = requiredOwnerOf.length > 0;
    } else {
      // No ACL annotation is ok, maybe a admin only op
    }

    String[] requiredActions = null;
    RequiresPermission perms = requestAuthzProperties.get(RequiresPermission.class);
    if (perms != null) {
      // check for version specific IAM permissions and version Id in the request
      if (perms.version().length > 0 && StringUtils.isNotBlank(request.getVersionId())
          && !StringUtils.equalsIgnoreCase(request.getVersionId(), ObjectStorageProperties.NULL_VERSION_ID)) {
        requiredActions = perms.version(); // Use version specific IAM perms
      } else {
        requiredActions = perms.standard(); // Use default/standard IAM perms
      }
    }

    Boolean allowAdmin = (requestAuthzProperties.get(AdminOverrideAllowed.class) != null);

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

    // Get user from context
    final UserPrincipal requestUser;
    final String requestAccountNumber;
    final String requestCanonicalId;
    final AuthContextSupplier authContext;
    try {
      final Context ctx = Contexts.lookup(request.getCorrelationId());
      requestUser = ctx.getUser();
      requestAccountNumber = ctx.getAccountNumber();
      requestCanonicalId = requestUser.getCanonicalId();
      authContext = ctx.getAuthContext();
    } catch ( final NoSuchContextException e ) {
      LOG.error( "Context not found, cannot evaluate authorization " + request.getCorrelationId( ) );
      return false;
    }

    if (allowAdmin && requestUser.isSystemAdmin()) {
      // Admin override
      return true;
    }

    final Option<Tuple4<String,String,Integer,String>>  bucketPolicy;
    final String resourceOwnerAccountNumber;
    final String bucketOwnerAccountNumber;
    final PolicyResourceInfo<S3AccessControlledEntity> policyResourceInfo;
    if (resourceType == null) {
      LOG.error("No resource type found in request class annotations, cannot process.");
      return false;
    } else {
      try {
        // Ensure we have the proper resource entities present and get owner info
        switch (resourceType) {
          case S3PolicySpec.S3_RESOURCE_BUCKET:
            // Get the bucket owner. bucket and resource owner are same in this case
            if (bucketResourceEntity == null) {
              LOG.error("Could not check access for operation due to no bucket resource entity found");
              return false;
            } else {
              bucketOwnerAccountNumber =
                  resourceOwnerAccountNumber = lookupAccountIdByCanonicalId(bucketResourceEntity.getOwnerCanonicalId());
              policyResourceInfo = PolicyResourceContext.resourceInfo(resourceOwnerAccountNumber, bucketResourceEntity);
              bucketPolicy = Option.of( Tuple.of(
                  bucketOwnerAccountNumber,
                  bucketResourceEntity.getBucketName( ),
                  bucketResourceEntity.getVersion( ),
                  bucketResourceEntity.getPolicy( ) ) );
            }
            break;
          case S3PolicySpec.S3_RESOURCE_OBJECT:
            // get the bucket owner account number as the bucket and object owner may be different
            if (bucketResourceEntity == null) { // cannot be null as every object is associated with one bucket
              LOG.error("Could not check access for operation due to no bucket resource entity found");
              return false;
            } else {
              bucketOwnerAccountNumber = lookupAccountIdByCanonicalId(bucketResourceEntity.getOwnerCanonicalId());
              bucketPolicy = Option.of( Tuple.of(
                  bucketOwnerAccountNumber,
                  bucketResourceEntity.getBucketName( ),
                  bucketResourceEntity.getVersion( ),
                  bucketResourceEntity.getPolicy( ) ) );
            }
            // get the object owner.
            if (objectResourceEntity == null) {
              LOG.error("Could not check access for operation due to no object resource entity found");
              return false;
            } else {
              // on create treat the object owner as the bucket owner for authorization purposes
              resourceOwnerAccountNumber = ObjectState.creating == objectResourceEntity.getState( ) ?
                  bucketOwnerAccountNumber :
                  lookupAccountIdByCanonicalId( objectResourceEntity.getOwnerCanonicalId( ) );
              policyResourceInfo = PolicyResourceContext.resourceInfo(resourceOwnerAccountNumber, objectResourceEntity);
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
    final String resourceId;
    if ( S3PolicySpec.S3_RESOURCE_BUCKET.equals(resourceType)) {
      resourceId = request.getBucket();
    } else if ( S3PolicySpec.S3_RESOURCE_OBJECT.equals(resourceType)) {
      resourceId = request.getFullResource();
    } else {
      resourceId = null;
    }

    // Override for 'eucalyptus' account and workaround for EUCA-11346
    // Skip ACL checks for 'eucalyptus' account only. ACL checks must be performed for all other accounts including system accounts
    // IAM checks must be performed for all accounts
    if (allowAdmin && AccountIdentifiers.SYSTEM_ACCOUNT.equals(requestUser.getAccountAlias())) {
      return iamPermissionsAllow(true, authContext, requiredActions, policyResourceInfo, Option.none( ), resourceType, resourceId, resourceAllocationSize);
    }

    // Don't allow anonymous to create buckets. EUCA-12902
    if ( S3PolicySpec.S3_RESOURCE_BUCKET.equals(resourceType) &&
        Principals.nobodyAccount( ).getAccountNumber( ).equals(resourceOwnerAccountNumber) &&
        request instanceof CreateBucketType) {
      return false;
    }

    if (requiredBucketACLPermissions == null) {
      throw new IllegalArgumentException("No requires-permission actions found in request class annotations, cannot process.");
    }

    /*
     * Bucket or object owner only? It is expected that ownerOnly flag can be used solely or in combination with ACL checks. If owner checks are
     * required, evaluate them first before evaluating the ACLs
     */
    Boolean isRequestByOwner = false;
    if ( allowOwnerOnly ) { // owner checks are in effect
      if (requiredOwnerOf == null || requiredOwnerOf.length == 0) {
        LOG.error("Owner only flag does not include resource (bucket, object) that ownership checks should be applied to");
        return false;
      }

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
    final boolean requestAccountIsResourceAccount = // so request account iam policy is sufficient to grant access
        isRequestByOwner ||
            ( (S3PolicySpec.S3_RESOURCE_OBJECT.equals( resourceType ) || S3PolicySpec.S3_RESOURCE_BUCKET.equals( resourceType )) &&
                resourceOwnerAccountNumber.equals( requestAccountNumber ) );
    final boolean bucketAccountIsObjectAccount = // so bucket policy is sufficient to grant access
        S3PolicySpec.S3_RESOURCE_OBJECT.equals( resourceType ) &&
        resourceOwnerAccountNumber.equals( bucketOwnerAccountNumber );

    /* ACL Checks: Is the user's account allowed? */
    Boolean aclAllow = false;
    if ( requiredBucketACLPermissions.length > 0 || requiredObjectACLPermissions.length > 0 ) { // check ACLs if any

      // Check bucket ACLs, if any
      if ( requiredBucketACLPermissions.length > 0 ) {

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
      aclAllow = isRequestByOwner;
    }

    final Boolean iamAllow = iamPermissionsAllow(aclAllow, authContext, requiredActions, policyResourceInfo, bucketPolicy, resourceType, resourceId, resourceAllocationSize);

    // Must have both acl and iam allow (account & user)
    return (aclAllow || bucketAccountIsObjectAccount || requestAccountIsResourceAccount) && iamAllow;
  }

  private String lookupAccountIdByCanonicalId( final String canonicalId ) throws AuthException {
    if ( AccountIdentifiers.NOBODY_CANONICAL_ID.equals( canonicalId ) ) {
      return Principals.nobodyAccount( ).getAccountNumber( );
    } else {
      return Accounts.lookupAccountIdByCanonicalId( canonicalId );
    }
  }

  private static Boolean iamPermissionsAllow(
      final boolean aclsAllow,
      final AuthContextSupplier authContext,
      final String[] requiredActions,
      final PolicyResourceInfo<S3AccessControlledEntity> policyResourceInfo,
      final Option<Tuple4<String,String,Integer,String>> bucketPolicyOption,
      final String resourceType,
      final String resourceId,
      final long resourceAllocationSize
  ) {
    final PolicyVersion bucketPolicy =
        getBucketPolicy( bucketPolicyOption.map( t -> Tuple.of( t._2( ), t._3( ), t._4( ) ) ) );
    final AccountFullName bucketPolicyAccount = bucketPolicyOption.isDefined( ) ?
        AccountFullName.getInstance( bucketPolicyOption.get( )._1( ) ) :
        null;
    final AccountFullName resourceAccount =
        AccountFullName.getInstance( policyResourceInfo.getResourceAccountNumber( ) );

    /* IAM checks: Is the user allowed within the account? */
    // the Permissions.isAuthorized() handles the default deny for each action.
    boolean iamAllow = true; // Evaluate each iam action required, all must be allowed
    for (String action : requiredActions) {
      try ( final PolicyResourceContext context = PolicyResourceContext.of( policyResourceInfo, action ) ) {
        // Any deny overrides an allow
        // Note: explicitly set resourceOwnerAccount to null here, otherwise iam will reject even if the ACL checks
        // were valid, let ACLs handle cross-account access.
        iamAllow &=
            Permissions.isAuthorized( S3PolicySpec.VENDOR_S3, resourceType, resourceId, resourceAccount, bucketPolicy, bucketPolicyAccount, action, authContext, aclsAllow )
                && Permissions.canAllocate( S3PolicySpec.VENDOR_S3, resourceType, resourceId, action, authContext, resourceAllocationSize);
      }
    }
    return iamAllow;
  }

  private static PolicyVersion getBucketPolicy( final Option<Tuple3<String,Integer,String>> bucketPolicyOption ) {
    PolicyVersion policyVersion = null;
    if ( bucketPolicyOption.isDefined( ) ) {
      final String bucketName = bucketPolicyOption.get( )._1( );
      final String policy = bucketPolicyOption.get( )._3( );
      if ( policy != null ) {
        final String policyVersionId = "arn:aws:s3:::" + bucketName + "/policy/bucket," + bucketPolicyOption.get( )._2( );
        final String policyName = "Policy for bucket " + bucketName;
        final String policyHash = PolicyVersions.hash( policy );
        policyVersion = new PolicyVersion() {
          @Override public String getPolicyName() { return policyName; }
          @Override public PolicyScope getPolicyScope() { return PolicyScope.Resource; }
          @Override public String getPolicyVersionId() { return policyVersionId; }
          @Override public String getPolicyHash() { return policyHash; }
          @Override public String getPolicy() { return policy; }
        };
      }
    }
    return policyVersion;
  }
}
