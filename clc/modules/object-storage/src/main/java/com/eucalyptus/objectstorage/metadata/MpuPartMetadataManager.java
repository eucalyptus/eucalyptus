/*
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
 */

package com.eucalyptus.objectstorage.metadata;

import com.eucalyptus.objectstorage.ObjectState;
import com.eucalyptus.objectstorage.PaginatedResult;
import com.eucalyptus.objectstorage.entities.Bucket;
import com.eucalyptus.objectstorage.entities.PartEntity;
import com.eucalyptus.objectstorage.exceptions.IllegalResourceStateException;
import com.eucalyptus.objectstorage.exceptions.MetadataOperationFailureException;
import com.eucalyptus.objectstorage.exceptions.s3.S3Exception;
import com.eucalyptus.storage.msgs.s3.Part;

import javax.annotation.Nonnull;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

/**
 * Created by zhill on 2/18/14.
 */
public interface MpuPartMetadataManager {
    void start() throws Exception;

    void stop() throws Exception;

    /**
     * Finalize creation of the part. Analogous to object creations.
     * Returned entity will be in state 'extant', have an etag, etc.
     * @param objectToUpdate
     * @param updateTimestamp
     * @param eTag
     * @return
     * @throws MetadataOperationFailureException
     */
    public PartEntity finalizeCreation(PartEntity objectToUpdate, Date updateTimestamp, String eTag) throws MetadataOperationFailureException;

    /**
     * Persist a new entity indicating a part upload operation is in progress.
     * Returns the entity persisted with state 'creating'
     * @param objectToCreate
     * @return
     * @throws Exception
     */
    public PartEntity initiatePartCreation(@Nonnull PartEntity objectToCreate) throws Exception;

    /**
     * Remove all non-latest parts for the given object key. Cleans-up the history in case
     * parts are overwritten.
     *
     * @param bucket
     * @param objectKey
     * @throws Exception
     */
    public void cleanupInvalidParts(Bucket bucket, String objectKey, String uploadId, int partNumber) throws Exception;

    /**
     * Returns parts that have expired in creating state.
     * @return
     * @throws MetadataOperationFailureException
     */
    public List<PartEntity> lookupFailedParts() throws MetadataOperationFailureException;

    public void delete(@Nonnull PartEntity objectToDelete) throws IllegalResourceStateException, MetadataOperationFailureException;

    public List<PartEntity> lookupPartsInState(Bucket searchBucket, String searchKey, String uploadId, ObjectState state) throws Exception;

    /**
     * Removes all parts for the given uploadId by deleteing the metadata records. Will update the bucket size to reflect removed 'extant' parts
     * @return
     * @throws Exception
     */
    public void removeParts(Bucket bucket, String uploadId) throws Exception;

    /**
     * Flushes all part records for the given bucket. Does not update bucket size. This is expected for use prior to a bucket deletion or where
     * size or state changes aren't important.
     * @param bucket
     * @throws Exception
     */
    public void flushAllParts(Bucket bucket) throws Exception;

    public PartEntity transitionPartToState(@Nonnull PartEntity entity, @Nonnull ObjectState destState) throws IllegalResourceStateException, MetadataOperationFailureException;

    public PartEntity updateCreationTimeout(PartEntity entity) throws Exception;

    public HashMap<Integer, PartEntity> getParts(Bucket bucket, String objectKey, String uploadId) throws Exception;

    public long processPartListAndGetSize(List<Part> partsInManifest, HashMap<Integer, PartEntity> availableParts) throws S3Exception;

    public PaginatedResult<PartEntity> listPartsForUpload(Bucket bucket,
                                                   String objectKey,
                                                   String uploadId,
                                                   Integer partNumberMarker,
                                                   Integer maxParts) throws Exception;


}
