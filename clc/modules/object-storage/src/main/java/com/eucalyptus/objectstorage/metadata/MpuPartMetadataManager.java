/*************************************************************************
 * Copyright 2009-2014 Ent. Services Development Corporation LP
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

import java.util.Date;
import java.util.HashMap;
import java.util.List;

import javax.annotation.Nonnull;

import com.eucalyptus.objectstorage.ObjectState;
import com.eucalyptus.objectstorage.PaginatedResult;
import com.eucalyptus.objectstorage.entities.Bucket;
import com.eucalyptus.objectstorage.entities.PartEntity;
import com.eucalyptus.objectstorage.exceptions.IllegalResourceStateException;
import com.eucalyptus.objectstorage.exceptions.MetadataOperationFailureException;
import com.eucalyptus.objectstorage.exceptions.s3.S3Exception;
import com.eucalyptus.storage.msgs.s3.Part;

/**
 * Created by zhill on 2/18/14.
 */
public interface MpuPartMetadataManager {
  void start() throws Exception;

  void stop() throws Exception;

  /**
   * Finalize creation of the part. Analogous to object creations. Returned entity will be in state 'extant', have an etag, etc.
   * 
   * @param objectToUpdate
   * @param updateTimestamp
   * @param eTag
   * @return
   * @throws MetadataOperationFailureException
   */
  public PartEntity finalizeCreation(PartEntity objectToUpdate, Date updateTimestamp, String eTag) throws MetadataOperationFailureException;

  /**
   * Persist a new entity indicating a part upload operation is in progress. Returns the entity persisted with state 'creating'
   * 
   * @param objectToCreate
   * @return
   * @throws Exception
   */
  public PartEntity initiatePartCreation(@Nonnull PartEntity objectToCreate) throws Exception;

  /**
   * Remove all non-latest parts for the given object key. Cleans-up the history in case parts are overwritten.
   *
   * @param bucket
   * @param objectKey
   * @throws Exception
   */
  public void cleanupInvalidParts(Bucket bucket, String objectKey, String uploadId, int partNumber) throws Exception;

  /**
   * Returns parts that have expired in creating state.
   * 
   * @return
   * @throws MetadataOperationFailureException
   */
  public List<PartEntity> lookupFailedParts() throws MetadataOperationFailureException;

  public void delete(@Nonnull PartEntity objectToDelete) throws IllegalResourceStateException, MetadataOperationFailureException;

  public List<PartEntity> lookupPartsInState(Bucket searchBucket, String searchKey, String uploadId, ObjectState state) throws Exception;

  /**
   * Removes all parts for the given uploadId by deleteing the metadata records. Will update the bucket size to reflect removed 'extant' parts
   * 
   * @return
   * @throws Exception
   */
  public void removeParts(Bucket bucket, String uploadId) throws Exception;

  /**
   * Flushes all part records for the given bucket. Does not update bucket size. This is expected for use prior to a bucket deletion or where size or
   * state changes aren't important.
   * 
   * @param bucket
   * @throws Exception
   */
  public void flushAllParts(Bucket bucket) throws Exception;

  public PartEntity transitionPartToState(@Nonnull PartEntity entity, @Nonnull ObjectState destState) throws IllegalResourceStateException,
      MetadataOperationFailureException;

  public PartEntity updateCreationTimeout(PartEntity entity) throws Exception;

  public HashMap<Integer, PartEntity> getParts(Bucket bucket, String objectKey, String uploadId) throws Exception;

  public long processPartListAndGetSize(List<Part> partsInManifest, HashMap<Integer, PartEntity> availableParts) throws S3Exception;

  public PaginatedResult<PartEntity> listPartsForUpload(Bucket bucket, String objectKey, String uploadId, int partNumberMarker, int maxParts)
      throws Exception;

  /**
   * Returns the conservative sum size of all parts in the given bucket. Includes any in-progress uploads (objects in 'creating' or 'extant' state)
   * 
   * @param bucket
   * @return
   * @throws Exception
   */
  public long getTotalSize(Bucket bucket) throws Exception;

}
