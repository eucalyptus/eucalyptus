/*************************************************************************
 * Copyright 2009-2013 Eucalyptus Systems, Inc.
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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.eucalyptus.objectstorage.entities.S3AccessControlledEntity;
import com.eucalyptus.objectstorage.msgs.ObjectStorageRequestType;

/**
 * Does ACL + IAM + bucketpolicy (when implemented) checks to authorize a request based on the request
 * type annotations: 
 * {@link com.eucalyptus.objectstorage.policy.AdminOverrideAllowed}, 
 * {@link com.eucalyptus.objectstorage.policy.RequiresACLPermission}, 
 * {@link com.eucalyptus.objectstorage.policy.RequiresPermission}, and
 * {@link ResourceType}
 *
 */
public interface RequestAuthorizationHandler {
	/**
	 * Evaluates the authorization for the operation requested, evaluates IAM, ACL, and bucket policy (bucket policy not yet supported).
	 * @param request
	 * @param optionalResourceId optional (can be null) explicit resourceId to check. If null, the request is used to get the resource.
	 * @param optionalOwnerId optional (can be null) owner Id for the resource being evaluated.
	 * @param optionalResourceAcl option acl for the requested resource
	 * @param resourceAllocationSize the size for the quota check(s) if applicable
	 * @return
	 */
	public abstract <T extends ObjectStorageRequestType> boolean operationAllowed(@Nonnull T request, @Nullable final S3AccessControlledEntity bucketResourceEntity, @Nullable final S3AccessControlledEntity objectResourceEntity, long resourceAllocationSize) throws IllegalArgumentException;

}
