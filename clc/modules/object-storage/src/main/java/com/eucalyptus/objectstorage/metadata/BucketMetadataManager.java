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

package com.eucalyptus.objectstorage.metadata;

import java.util.List;

import com.eucalyptus.entities.TransactionException;
import com.eucalyptus.objectstorage.BucketState;
import com.eucalyptus.objectstorage.entities.Bucket;
import com.eucalyptus.objectstorage.exceptions.IllegalResourceStateException;
import com.eucalyptus.objectstorage.exceptions.MetadataOperationFailureException;
import com.eucalyptus.objectstorage.exceptions.NoSuchEntityException;
import com.eucalyptus.objectstorage.exceptions.s3.S3Exception;
import com.eucalyptus.objectstorage.util.ObjectStorageProperties.VersioningStatus;
import com.eucalyptus.storage.msgs.s3.AccessControlPolicy;

/**
 * Interface to operate on buckets
 * This interface is an action mechanism, not a policy checker. Validation on input, beyond what is required for the operation
 * to succeed, is outside the scope for this. e.g. BucketMetadataManager will not enforce S3 naming conventions.
 * 
 * This is the interface to the metadata update mechanisms.
 * 
 */
public interface BucketMetadataManager {

    public void start() throws Exception;

    public void stop() throws Exception;

    /**
     * Convenience function for initialization of the entity and transitioning it to the 'creating' state.
     * @param bucketName
     * @param acp
     * @param iamUserId
     * @param location
     * @return
     * @throws Exception
     */
    public Bucket persistBucketInCreatingState(String bucketName,
                                               AccessControlPolicy acp,
                                               String iamUserId,
                                               String location) throws Exception;

        /**
         * Transitions the bucket entity to the requested state subject to the state-machine transition of buckets.
         * The bucket entity is re-loaded from the persistence system based on bucketUuid and only the state
         * and update timestamps are modified. The updated entity is returned to the caller.
         *
         * If the bucket (as queried from persistence store) is not in a compatible state then an IllegalResourceStateException is thrown.
         * If something else prevents the update from taking place (db not available,no record found, etc) then a MetadataOperationFailureException is thrown.
         *
         * @param bucket
         * @param destState
         * @return
         * @throws Exception
         */
    public Bucket transitionBucketToState(final Bucket bucket, final BucketState destState) throws IllegalResourceStateException, MetadataOperationFailureException;

	/**
	 * Complete the deletion operation, the end result is a bucket in the 'deleted' state 
	 * @param bucket The bucket uuid to update
	 */
	public void deleteBucketMetadata(final Bucket bucket) throws Exception;
	
	/**
	 * Returns a bucket's metadata object in any state
	 * @param bucketName
	 * @return
	 */
	public Bucket lookupBucket(String bucketName) throws Exception;

    /**
     * Returns a bucket's metadata object in any state
     * @param bucketUuid
     * @return
     */
    public Bucket lookupBucketByUuid(String bucketUuid) throws Exception;

    /**
     * Lookup an extant bucket. This is the method to be used to lookup for subsequent
     * modification or to verify existence of a bucket.
     * @param bucketName
     * @return
     * @throws Exception
     */
    public Bucket lookupExtantBucket(String bucketName) throws NoSuchEntityException, MetadataOperationFailureException;

    /**
     * Returns a list of buckets in the 'deleting' state. This is intended for GC usage
     * @return
     * @throws Exception
     */
    public List<Bucket> getBucketsForDeletion() throws Exception;

    /**
	 * Returns list of buckets owned by id. Buckets are detached from any persistence session.
	 * @param ownerCanonicalId
	 * @return
	 */
	public List<Bucket> lookupBucketsByOwner(String ownerCanonicalId) throws Exception;

    /**
     * Returns list of buckets in the desired state.
     * @return
     */
    public List<Bucket> lookupBucketsByState(BucketState state) throws Exception;

    /**
	 * Returns list of buckets owned by user's iam id, in the given account. Buckets are detached from any persistence session.
	 * @return
	 */
	public List<Bucket> lookupBucketsByUser(String userIamId) throws Exception;
	
	/**
	 * Change bucket size estimate. sizeToChange can be any value. Negatives decrement, positives
	 * increment the size
	 * @param bucket
	 * @param sizeToChange long indicating the change to make, not the new value
     * @return Updated bucket entity
	 * @throws TransactionException
	 */
    public Bucket updateBucketSize(final Bucket bucket, final long sizeToChange) throws TransactionException;
	
	/**
	 * Returns count of buckets owned by user's iam id, in the given account. Buckets are detached from any persistence session.
	 * @return
	 */
	public long countBucketsByUser(String userIamId) throws Exception;
	/**
	 * Returns count of buckets owned by account id, in the given account. Buckets are detached from any persistence session.
	 * @return
	 */	
	public long countBucketsByAccount(String canonicalId) throws Exception;
	
	/**
	 * Update the ACP
	 * @param bucketEntity
	 * @param acl
	 * @return
	 * @throws Exception
	 */
	public Bucket setAcp(Bucket bucketEntity, String acl)  throws Exception;

    /**
     * Update the ACP
     * @param bucketEntity
     * @param acp
     * @return
     * @throws Exception
     */
    public Bucket setAcp(Bucket bucketEntity, AccessControlPolicy acp)  throws Exception;

    /**
	 * Update the logging status
	 * @param bucketEntity
	 * @param loggingEnabled
	 * @param destBucket
	 * @param destPrefix
	 * @return
	 * @throws Exception
	 */
	public Bucket setLoggingStatus(Bucket bucketEntity, Boolean loggingEnabled, String destBucket, String destPrefix) throws Exception;
	
	/**
	 * Update versioning status
	 * @param bucketEntity
	 * @param newState
	 * @return
	 * @throws Exception
	 */
	public Bucket setVersioning(Bucket bucketEntity, VersioningStatus newState) throws IllegalResourceStateException, MetadataOperationFailureException, NoSuchEntityException;

	/**
	 * Returns the approximate total size of all objects in all buckets
	 * in the system. This is not guaranteed to be consistent. An approximation
	 * at best.
	 * @return
	 */
	public long totalSizeOfAllBuckets();
}
