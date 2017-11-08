/*************************************************************************
 * Copyright 2013-2014 Ent. Services Development Corporation LP
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

package com.eucalyptus.objectstorage;

import com.eucalyptus.auth.principal.User;
import com.eucalyptus.objectstorage.entities.Bucket;
import com.eucalyptus.objectstorage.exceptions.s3.S3Exception;
import com.eucalyptus.objectstorage.providers.ObjectStorageProviderClient;

/**
 * A factory for creating and deleting buckets. Performs both metadata and backend resource operations.
 * 
 * NOTE: this performs no access control operations or checks. The caller must ensure proper permissions prior to invoking this factory
 * 
 */
public interface BucketFactory {
  /**
   * Create the named bucket in metadata and on the backend. Will either successfully create the bucket and return a record in the 'extant' state, or
   * throw an exception that indicates an error. In the error case, metadata may be left behind, but will be in a 'deleting' state to be cleaned up
   * later.
   *
   * If the bucket already exists (by logical name, not uuid) then an exception is thrown and the caller can deal with the conflict explicitly.
   * 
   * @param backendProvider The provider client to access backend resources
   * @param bucketToCreate the initialized bucket to persist
   * @param correlationId The request correlationId for logging and tracing (optional)
   * @return the Bucket object representing the successfully created bucket
   */
  public Bucket createBucket(ObjectStorageProviderClient backendProvider, Bucket bucketToCreate, String correlationId, User requestUser)
      throws S3Exception;

  /**
   * Delete the named bucket in metadata and on the backend. Upon return, the bucket and it's metadata will be confirmed removed from the system. An
   * exception will occur and may result in a bucket in the 'deleting' state depending on the error.
   * 
   * This operation is idempotent and is retry-able. Bucket may be in any state prior to invoking this.
   *
   * @param bucket the bucket entity to work off
   * @param correlationId optional id for tracing requests in the logs, not used for any logic
   */
  public void deleteBucket(ObjectStorageProviderClient backendProvider, Bucket bucket, String correlationId, User requestUser) throws S3Exception;

}
