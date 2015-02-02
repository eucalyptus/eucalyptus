/*************************************************************************
 * Copyright 2013-2014 Eucalyptus Systems, Inc.
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
