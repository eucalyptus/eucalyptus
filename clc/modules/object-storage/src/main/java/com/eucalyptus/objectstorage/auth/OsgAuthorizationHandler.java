/*************************************************************************
 * Copyright 2009-2014 Eucalyptus Systems, Inc.
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

import com.eucalyptus.auth.AuthContextSupplier;
import java.util.Collections;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.eucalyptus.auth.tokens.SecurityTokenManager;
import org.apache.log4j.Logger;

import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.Permissions;
import com.eucalyptus.auth.policy.PolicySpec;
import com.eucalyptus.auth.principal.Account;
import com.eucalyptus.auth.principal.Principals;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.context.Context;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.context.NoSuchContextException;
import com.eucalyptus.objectstorage.entities.S3AccessControlledEntity;
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
	 * @return
	 */
	protected static boolean isUserAnonymous(User usr) {
		return Principals.nobodyUser().equals(usr);
	}

	/**
	 * Evaluates the authorization for the operation requested, evaluates IAM, ACL, and bucket policy (bucket policy not yet supported).
	 * @param request
	 * @param bucketResourceEntity
	 * @param objectResourceEntity
	 * @param resourceAllocationSize the size for the quota check(s) if applicable
	 * @return
	 */
	public <T extends ObjectStorageRequestType> boolean operationAllowed(@Nonnull T request, @Nullable final S3AccessControlledEntity bucketResourceEntity, @Nullable final S3AccessControlledEntity objectResourceEntity, long resourceAllocationSize) throws IllegalArgumentException {
		/*
		 * Process the operation's authz requirements based on the request type annotations
		 */
		Ats requestAuthzProperties = Ats.from(request);
		ObjectStorageProperties.Permission[] requiredBucketACLPermissions = null;
		ObjectStorageProperties.Permission[] requiredObjectACLPermissions = null;
		Boolean allowOwnerOnly = null;
		RequiresACLPermission requiredACLs = requestAuthzProperties.get(RequiresACLPermission.class);
		if(requiredACLs != null) {
			requiredBucketACLPermissions = requiredACLs.bucket();
			requiredObjectACLPermissions = requiredACLs.object();
			allowOwnerOnly = requiredACLs.ownerOnly();
		} else {
			//No ACL annotation is ok, maybe a admin only op
		}
		
		String[] requiredActions = null;
		RequiresPermission perms = requestAuthzProperties.get(RequiresPermission.class);
		if(perms != null) {
			requiredActions = perms.value(); 
		}

		Boolean allowAdmin = (requestAuthzProperties.get(AdminOverrideAllowed.class) != null);
		Boolean allowOnlyAdmin = (requestAuthzProperties.get(AdminOverrideAllowed.class) != null) && requestAuthzProperties.get(AdminOverrideAllowed.class).adminOnly();
		
		//Must have at least one of: admin-only, owner-only, ACL, or IAM.
		if(requiredBucketACLPermissions == null && 
				requiredObjectACLPermissions == null &&
				requiredActions == null &&
				!allowAdmin) {
			//Insufficient permission set on the message type.
			LOG.error("Insufficient permission annotations on type: " + request.getClass().getName() + " cannot evaluate authorization");
			return false;
		}
		
		String resourceType = null;
		if(requestAuthzProperties.get(ResourceType.class) != null) {
			resourceType = requestAuthzProperties.get(ResourceType.class).value();
		}
		
		//Use these variables to isolate where all the AuthExceptions can happen on account/user lookups
		User requestUser = null;
		String securityToken = null;
		Account requestAccount = null;
		AuthContextSupplier authContext = null;
		try {
			//Use context if available as it saves a DB lookup
			try {
				Context ctx = Contexts.lookup(request.getCorrelationId());
				requestUser = ctx.getUser();
				securityToken = ctx.getSecurityToken();
				requestAccount = requestUser.getAccount();
				authContext = ctx.getAuthContext();
			} catch(NoSuchContextException e) {
				requestUser = null;
				securityToken = null;
				requestAccount = null;
				authContext = null;
			}
						
			//This is not an expected path, but if no context found use the request credentials itself
			if(requestUser == null && !Strings.isNullOrEmpty(request.getEffectiveUserId())) {
				requestUser = Accounts.lookupUserById(request.getEffectiveUserId());
				requestAccount = requestUser.getAccount();
			}
			
			if(requestUser == null) {
				if(!Strings.isNullOrEmpty(request.getAccessKeyID())) {
					if(securityToken != null) {
						requestUser = SecurityTokenManager.lookupUser(request.getAccessKeyID(), securityToken);
					}
					else {
						requestUser = Accounts.lookupUserByAccessKeyId(request.getAccessKeyID());
					}
					requestAccount = requestUser.getAccount();
				} else {
					//Set to anonymous user since all else failed
					requestUser = Principals.nobodyUser();
					requestAccount = requestUser.getAccount();
				}
			}
		} catch (AuthException e) {
			LOG.error("Failed to get user for request, cannot verify authorization: " + e.getMessage(), e);				
			return false;
		}
		
		
		if(allowAdmin && requestUser.isSystemAdmin()) {
			//Admin override
			return true;
		}

		if ( authContext == null ) {
			authContext = Permissions.createAuthContextSupplier( requestUser, Collections.<String,String>emptyMap( ) );
		}

		Account resourceOwnerAccount = null;
		if(resourceType == null) {
			LOG.error("No resource type found in request class annotations, cannot process.");
			return false;
		} else {
			try {
				//Ensure we have the proper resource entities present and get owner info						
				if(PolicySpec.S3_RESOURCE_BUCKET.equals(resourceType)) {
					//Get the bucket owner.
					if(bucketResourceEntity == null) {
						LOG.error("Could not check access for operation due to no bucket resource entity found");
						return false;
					} else {
						resourceOwnerAccount = Accounts.lookupAccountByCanonicalId(bucketResourceEntity.getOwnerCanonicalId());
					}
				} else if(PolicySpec.S3_RESOURCE_OBJECT.equals(resourceType)) {
					if(objectResourceEntity == null) {
						LOG.error("Could not check access for operation due to no object resource entity found");
						return false;
					} else {
						resourceOwnerAccount = Accounts.lookupAccountByCanonicalId(objectResourceEntity.getOwnerCanonicalId());
					}
				}
			} catch(AuthException e) {
				LOG.error("Exception caught looking up resource owner. Disallowing operation.",e);
				return false;
			}
		}
		
		//Get the resourceId based on IAM resource type
		String resourceId = null;
		if(resourceId == null ) {
			if(PolicySpec.S3_RESOURCE_BUCKET.equals(resourceType)) {
				resourceId = request.getBucket();
			} else if(PolicySpec.S3_RESOURCE_OBJECT.equals(resourceType)) {
				resourceId = request.getFullResource();
			}
		}

		if(allowAdmin &&
				requestUser.isSystemUser( ) &&
				iamPermissionsAllow( authContext, requiredActions, resourceType, resourceId, resourceAllocationSize ) ) {
			//Admin override
			return true;
		}
		
		if(requiredBucketACLPermissions == null && requiredObjectACLPermissions == null) {
			throw new IllegalArgumentException("No requires-permission actions found in request class annotations, cannot process.");
		}

		/* ACL Checks: Is the user's account allowed? */
		Boolean aclAllow = false;		
		if(requiredBucketACLPermissions != null && requiredBucketACLPermissions.length > 0) {
			//Check bucket ACLs
			
			if(bucketResourceEntity == null) {
				//There are bucket ACL requirements but no bucket entity to check. fail.
				//Don't bother with other checks, this is an invalid state
				LOG.error("Null bucket resource, cannot evaluate bucket ACL");
				return false;
			}
			
			//Evaluate the bucket ACL, any matching grant gives permission
			for(ObjectStorageProperties.Permission permission : requiredBucketACLPermissions) {
				aclAllow = aclAllow || bucketResourceEntity.can(permission, requestAccount.getCanonicalId());
			}
		}
		
		//Check object ACLs, if any
		if(requiredObjectACLPermissions != null && requiredObjectACLPermissions.length > 0) {
			if(objectResourceEntity == null) {
				//There are object ACL requirements but no object entity to check. fail.
				//Don't bother with other checks, this is an invalid state				
				LOG.error("Null bucket resource, cannot evaluate bucket ACL");
				return false;
			}
			for(ObjectStorageProperties.Permission permission : requiredObjectACLPermissions) {
				aclAllow = aclAllow || objectResourceEntity.can(permission, requestAccount.getCanonicalId());
			}
		}
		
		/* Resource owner only? if so, override any previous acl decisions
		 * It is not expected that owneronly is set as well as other ACL permissions,
		 * Regular owner permissions (READ, WRITE, READ_ACP, WRITE_ACP) are handled by the regular acl checks.
		 * OwnerOnly should be only used for operations not covered by the other Permissions (e.g. logging, or versioning)
		 */
		aclAllow = (allowOwnerOnly ? resourceOwnerAccount.getAccountNumber().equals(requestAccount.getAccountNumber()) : aclAllow);
		if(isUserAnonymous(requestUser)) {
			//Skip the IAM checks for anonymous access since they will always fail and aren't valid for anonymous users.
			return aclAllow;
		} else {
			Boolean iamAllow = iamPermissionsAllow( authContext, requiredActions, resourceType, resourceId, resourceAllocationSize );
			//Must have both acl and iam allow (account & user)
			return aclAllow && iamAllow;
		}
	}

	private static Boolean iamPermissionsAllow(
				final AuthContextSupplier authContext,
				final String[] requiredActions,
				final String resourceType,
				final String resourceId,
				final long resourceAllocationSize
	) {
		/* IAM checks: Is the user allowed within the account? */
		// the Permissions.isAuthorized() handles the default deny for each action.
		boolean iamAllow = true;  //Evaluate each iam action required, all must be allowed
		for(String action : requiredActions ) {
			//Any deny overrides an allow
			//Note: explicitly set resourceOwnerAccount to null here, otherwise iam will reject even if the ACL checks
			// were valid, let ACLs handle cross-account access.
			iamAllow &=
					Permissions.isAuthorized( PolicySpec.VENDOR_S3, resourceType, resourceId, null, action, authContext ) &&
					Permissions.canAllocate(PolicySpec.VENDOR_S3, resourceType, resourceId, action, authContext, resourceAllocationSize);
		}
		return iamAllow;
	}

}
