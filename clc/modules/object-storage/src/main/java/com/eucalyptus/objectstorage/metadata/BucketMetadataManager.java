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

package com.eucalyptus.objectstorage.metadata;

import java.util.List;

import javax.annotation.Nonnull;
import com.eucalyptus.objectstorage.BucketState;
import com.eucalyptus.objectstorage.entities.Bucket;
import com.eucalyptus.objectstorage.exceptions.IllegalResourceStateException;
import com.eucalyptus.objectstorage.exceptions.InvalidMetadataException;
import com.eucalyptus.objectstorage.exceptions.MetadataOperationFailureException;
import com.eucalyptus.objectstorage.exceptions.NoSuchEntityException;
import com.eucalyptus.objectstorage.util.ObjectStorageProperties.VersioningStatus;
import com.eucalyptus.storage.msgs.s3.AccessControlPolicy;
import io.vavr.control.Option;

/**
 * Interface to operate on buckets This interface is an action mechanism, not a policy checker. Validation on input, beyond what is required for the
 * operation to succeed, is outside the scope for this. e.g. BucketMetadataManager will not enforce S3 naming conventions.
 * 
 * This is the interface to the metadata update mechanisms.
 * 
 */
public interface BucketMetadataManager {

  void start() throws Exception;

  void stop() throws Exception;

  /**
   * Convenience function for initialization of the entity and transitioning it to the 'creating' state.
   * 
   * @param bucketName
   * @param acp
   * @param iamUserId
   * @param location
   * @return
   * @throws Exception
   */
  Bucket persistBucketInCreatingState(String bucketName, AccessControlPolicy acp, String iamUserId, String location) throws Exception;

  /**
   * Transitions the bucket entity to the requested state subject to the state-machine transition of buckets. The bucket entity is re-loaded from the
   * persistence system based on bucketUuid and only the state and update timestamps are modified. The updated entity is returned to the caller.
   *
   * If the bucket (as queried from persistence store) is not in a compatible state then an IllegalResourceStateException is thrown. If something else
   * prevents the update from taking place (db not available,no record found, etc) then a MetadataOperationFailureException is thrown.
   *
   * @param bucket
   * @param destState
   * @return
   * @throws Exception
   */
  Bucket transitionBucketToState(final Bucket bucket, final BucketState destState) throws IllegalResourceStateException,
      MetadataOperationFailureException;

  /**
   * Complete the deletion operation, the end result is a bucket in the 'deleted' state
   * 
   * @param bucket The bucket uuid to update
   */
  void deleteBucketMetadata(final Bucket bucket) throws Exception;

  /**
   * Returns a bucket's metadata object in any state
   * 
   * @param bucketName
   * @return
   */
  Bucket lookupBucket(String bucketName) throws Exception;

  /**
   * Returns a bucket's metadata object in any state
   * 
   * @param bucketUuid
   * @return
   */
  Bucket lookupBucketByUuid(String bucketUuid) throws Exception;

  /**
   * Lookup an extant bucket. This is the method to be used to lookup for subsequent modification or to verify existence of a bucket.
   * 
   * @param bucketName
   * @return
   * @throws Exception
   */
  Bucket lookupExtantBucket(String bucketName) throws NoSuchEntityException, MetadataOperationFailureException;

  /**
   * Returns a list of buckets in the 'deleting' state. This is intended for GC usage
   * 
   * @return
   * @throws Exception
   */
  List<Bucket> getBucketsForDeletion() throws Exception;

  /**
   * Returns list of buckets owned by id. Buckets are detached from any persistence session.
   * 
   * @param ownerCanonicalId
   * @return
   */
  List<Bucket> lookupBucketsByOwner(String ownerCanonicalId) throws Exception;

  /**
   * Returns list of buckets in the desired state.
   * 
   * @return
   */
  List<Bucket> lookupBucketsByState(BucketState state) throws Exception;

  /**
   * Returns list of buckets owned by user's iam id, in the given account. Buckets are detached from any persistence session.
   * 
   * @return
   */
  List<Bucket> lookupBucketsByUser(String userIamId) throws Exception;

  /**
   * Returns count of buckets owned by user's iam id, in the given account. Buckets are detached from any persistence session.
   * 
   * @return
   */
  long countBucketsByUser(String userIamId) throws Exception;

  /**
   * Returns count of buckets owned by account id, in the given account. Buckets are detached from any persistence session.
   * 
   * @return
   */
  long countBucketsByAccount(String canonicalId) throws Exception;

  /**
   * Update the ACP
   * 
   * @param bucketEntity
   * @param acp
   * @return
   * @throws Exception
   */
  Bucket setAcp(Bucket bucketEntity, AccessControlPolicy acp) throws Exception;

  /**
   * Update the logging status
   * 
   * @param bucketEntity
   * @param loggingEnabled
   * @param destBucket
   * @param destPrefix
   * @return
   * @throws Exception
   */
  Bucket setLoggingStatus(Bucket bucketEntity, Boolean loggingEnabled, String destBucket, String destPrefix) throws Exception;

  /**
   * Update versioning status
   * 
   * @param bucketEntity
   * @param newState
   * @return
   * @throws Exception
   */
  Bucket setVersioning(Bucket bucketEntity, VersioningStatus newState) throws IllegalResourceStateException,
      MetadataOperationFailureException, NoSuchEntityException;


  /**
   * Update the iam policy for the bucket, pass Option#none() to delete.
   */
  Bucket setPolicy(
      @Nonnull Bucket bucketEntity,
      @Nonnull Option<String> policy
  ) throws NoSuchEntityException, InvalidMetadataException;

  /**
   * Returns the approximate total size of all objects in all buckets in the system. This is not guaranteed to be consistent. An approximation at
   * best.
   * 
   * @return
   */
  long totalSizeOfAllBuckets();
}
