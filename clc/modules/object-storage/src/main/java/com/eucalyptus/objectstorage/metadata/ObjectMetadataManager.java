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

import java.util.Date;
import java.util.List;
import java.util.NoSuchElementException;

import com.eucalyptus.auth.principal.User;
import com.eucalyptus.entities.TransactionException;
import com.eucalyptus.objectstorage.ObjectState;
import com.eucalyptus.objectstorage.PaginatedResult;
import com.eucalyptus.objectstorage.entities.Bucket;
import com.eucalyptus.objectstorage.entities.ObjectEntity;
import com.eucalyptus.objectstorage.entities.PartEntity;
import com.eucalyptus.objectstorage.exceptions.IllegalResourceStateException;
import com.eucalyptus.objectstorage.exceptions.MetadataOperationFailureException;
import com.eucalyptus.objectstorage.exceptions.s3.S3Exception;
import com.eucalyptus.storage.msgs.s3.AccessControlPolicy;

/**
 * Interface for interacting with object metadata (not content directly)
 * @author zhill
 *
 */
public interface ObjectMetadataManager {

	public void start() throws Exception;
	public void stop() throws Exception;

	/**
	 * Get the entity record, not the content
	 * @param bucket
	 * @param objectKey
	 * @param versionId
	 * @return
	 * @throws TransactionException
	 */
	public ObjectEntity lookupObject(Bucket bucket, String objectKey, String versionId) throws NoSuchElementException;


    /**
     * Get the entity record for a multipart-upload
     * @param bucket
     * @param uploadId
     * @return
     * @throws TransactionException
     */
    public ObjectEntity lookupUpload(Bucket bucket, String uploadId) throws Exception;


    public List<ObjectEntity> lookupObjectsInState(Bucket bucket, String objectKey, String versionId, ObjectState state) throws Exception;

    /**
     * Given an initialized object, set the state and persist it
     * @param objectToCreate
     * @return
     * @throws Exception
     */
    public ObjectEntity initiateCreation(ObjectEntity objectToCreate) throws Exception;

    /**
     *
     * @param objectToUpdate
     * @param updateTimestamp
     * @param eTag
     * @return
     * @throws com.eucalyptus.objectstorage.exceptions.MetadataOperationFailureException
     */
    public ObjectEntity finalizeCreation(ObjectEntity objectToUpdate, Date updateTimestamp, String eTag) throws MetadataOperationFailureException;

    /**
     * Commit the state of the initialized MPU record to persist the uploadId and timestamp. The returned entity will be in
     * state 'mpu-pending'
     * @param objectToUpdate
     * @param updateTimestamp
     * @param uploadId
     * @return
     * @throws MetadataOperationFailureException
     */
    public ObjectEntity finalizeMultipartInit(ObjectEntity objectToUpdate, Date updateTimestamp, String uploadId) throws MetadataOperationFailureException;

    /**
     * Return paginated list of object entities indicating uploads in progress given a bucket
     * This method and {@link #listVersionsPaginated(com.eucalyptus.objectstorage.entities.Bucket, int, String, String, String, String, boolean)}
     * are similar with a few differences: we are listing "incomplete" objects with multipart uploads in progress
     * and there is not concept of a delete marker
     * @param bucket
     * @param maxUploads
     * @param prefix
     * @param delimiter
     * @param keyMarker
     * @param uploadIdMarker
     * @return
     * @throws Exception
     */
    public PaginatedResult<ObjectEntity> listUploads(final Bucket bucket,
                                                   int maxUploads,
                                                   String prefix,
                                                   String delimiter,
                                                   String keyMarker,
                                                   String uploadIdMarker) throws Exception;

    /**
     * List the objects in the given bucket
     * @param bucket
     * @param maxRecordCount
     * @param prefix
     * @param delimiter
     * @param startKey
     * @return
     * @throws TransactionException
     */
	public PaginatedResult<ObjectEntity> listPaginated(Bucket bucket, int maxRecordCount, String prefix, String delimiter, String startKey) throws Exception;

	/**
	 * List the object versions in the given bucket
	 * @param bucket
	 * @param maxKeys
	 * @param prefix
	 * @param delimiter
	 * @param startKey
	 * @param startVersionId
	 * @param includeDeleteMarkers
	 * @return
	 * @throws TransactionException
	 */
	public PaginatedResult<ObjectEntity> listVersionsPaginated(Bucket bucket, int maxKeys, String prefix, String delimiter, String startKey, String startVersionId, boolean includeDeleteMarkers) throws Exception;
	
	/**
	 * Delete the object entity
	 * @param objectToDelete
	 * @throws S3Exception
	 * @throws TransactionException
	 */
    public void delete(ObjectEntity objectToDelete) throws IllegalResourceStateException, MetadataOperationFailureException;

    /**
     * For the given object entity, generate a new delete marker for it and persist that marker
     * @param currentObject
     * @param acp
     * @param owningUser
     * @return
     * @throws MetadataOperationFailureException
     */
    public ObjectEntity generateAndPersistDeleteMarker(ObjectEntity currentObject,
                                                       AccessControlPolicy acp,
                                                       User owningUser) throws MetadataOperationFailureException;

        /**
         * Change the state of the object, per the state machine rules
         * @param entity
         * @param destState
         * @return
         * @throws IllegalResourceStateException
         * @throws com.eucalyptus.objectstorage.exceptions.MetadataOperationFailureException
         */
    public ObjectEntity transitionObjectToState(ObjectEntity entity, ObjectState destState) throws IllegalResourceStateException, MetadataOperationFailureException;


    /**
     * Sets the access control policy on the object.
     * @param object
     * @param acp
     * @return
     * @throws Exception
     */
	public ObjectEntity setAcp(ObjectEntity object, AccessControlPolicy acp) throws Exception;

    /**
     * Flush all pending uploads in the given bucket. Specifically, all objectentities in 'mpu-pending' state
     * for the give bucket.
     * @param bucket
     * @throws Exception
     */
    public void flushUploads(Bucket bucket) throws Exception;

    /**
     * Similar to cleanupInvalidObjects but does not preserve the latest. Will set all 'null' versioned
     * object records to 'deleting' state. This is intended for synchronous invocation during object creation to
     * invalidate all extant 'null' versions of the object to make quota-enforcement feasible.
     * @param bucket
     * @param objectKey
     * @throws Exception
     */
    public void cleanupAllNullVersionedObjectRecords(Bucket bucket, String objectKey) throws Exception;

    /**
     * Returns objects stuck in 'creating' state that are determined to be failed.
     * Failure detection is based on timestamp comparision is limited by the
     * {@link com.eucalyptus.objectstorage.entities.ObjectStorageGlobalConfiguration.failed_put_timeout_hrs}.
     * @return
     * @throws Exception
     */
    public List<ObjectEntity> lookupFailedObjects() throws Exception;
	
	/**
	 * Fix an object history if needed. Scans the sorted object records and marks
	 * latest as well as marking contiguous null-versioned records for deletion to
	 * remove contiguous nulls in the version history
	 * 
	 * @param bucket
	 * @param objectKey
	 * @throws Exception
	 */
	public void cleanupInvalidObjects(Bucket bucket, String objectKey) throws Exception;

	/**
	 * Returns a count of "valid" objects in the bucket. Valid means visible to user, not-deleting, and not-pending/failed.
	 * @param bucket
	 * @return
	 */
	public long countValid(Bucket bucket) throws Exception ;

    /**
     * Increment the timeout, use this call to indicate to other OSGs that the upload/creation operation is ongoing and to
     * avoid the record being GC'd the cleanup threads.
     *
     * @param entity
     * @return
     * @throws Exception
     */
    public ObjectEntity updateCreationTimeout(ObjectEntity entity) throws Exception ;

}
