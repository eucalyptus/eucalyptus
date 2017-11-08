/*************************************************************************
 * Copyright 2009-2013 Ent. Services Development Corporation LP
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

import com.eucalyptus.objectstorage.entities.S3AccessControlledEntity;
import com.eucalyptus.objectstorage.msgs.ObjectStorageRequestType;

/**
 * Does ACL + IAM + bucketpolicy (when implemented) checks to authorize a request based on the request type annotations:
 * {@link com.eucalyptus.objectstorage.policy.AdminOverrideAllowed}, {@link com.eucalyptus.objectstorage.policy.RequiresACLPermission},
 * {@link com.eucalyptus.objectstorage.policy.RequiresPermission}, and {@link com.eucalyptus.objectstorage.policy.ResourceType}
 *
 */
public interface RequestAuthorizationHandler {
  /**
   * Evaluates the authorization for the operation requested, evaluates IAM, ACL, and bucket policy (bucket policy not yet supported).
   * 
   * @param request
   * @param optionalResourceId optional (can be null) explicit resourceId to check. If null, the request is used to get the resource.
   * @param optionalOwnerId optional (can be null) owner Id for the resource being evaluated.
   * @param optionalResourceAcl option acl for the requested resource
   * @param resourceAllocationSize the size for the quota check(s) if applicable
   * @return
   */
  public abstract <T extends ObjectStorageRequestType> boolean operationAllowed(@Nonnull T request,
      @Nullable final S3AccessControlledEntity bucketResourceEntity, @Nullable final S3AccessControlledEntity objectResourceEntity,
      long resourceAllocationSize) throws IllegalArgumentException;

}
