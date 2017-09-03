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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.persistence.EntityTransaction;

import org.apache.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.criterion.Example;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionResource;
import com.eucalyptus.entities.Transactions;
import com.eucalyptus.objectstorage.MpuPartMetadataManagers;
import com.eucalyptus.objectstorage.ObjectState;
import com.eucalyptus.objectstorage.PaginatedResult;
import com.eucalyptus.objectstorage.entities.Bucket;
import com.eucalyptus.objectstorage.entities.PartEntity;
import com.eucalyptus.objectstorage.exceptions.IllegalResourceStateException;
import com.eucalyptus.objectstorage.exceptions.MetadataOperationFailureException;
import com.eucalyptus.objectstorage.exceptions.ObjectStorageInternalException;
import com.eucalyptus.objectstorage.exceptions.s3.EntityTooSmallException;
import com.eucalyptus.objectstorage.exceptions.s3.InvalidArgumentException;
import com.eucalyptus.objectstorage.exceptions.s3.InvalidPartException;
import com.eucalyptus.objectstorage.exceptions.s3.InvalidPartOrderException;
import com.eucalyptus.objectstorage.exceptions.s3.S3Exception;
import com.eucalyptus.objectstorage.util.ObjectStorageProperties;
import com.eucalyptus.storage.msgs.s3.Part;
import com.google.common.base.Function;
import com.google.common.base.Objects;
import com.google.common.base.Predicate;

/**
 * Database backed implementation of ObjectMetadataManager
 * 
 */
public class DbMpuPartMetadataManagerImpl implements MpuPartMetadataManager {
  private static final Logger LOG = Logger.getLogger(DbMpuPartMetadataManagerImpl.class);

  @Override
  public void start() throws Exception {
    // Do nothing
  }

  @Override
  public void stop() throws Exception {}

  @Override
  public PartEntity initiatePartCreation(@Nonnull PartEntity objectToCreate) throws Exception {
    return this.transitionPartToState(objectToCreate, ObjectState.creating);
  }

  @Override
  public PartEntity finalizeCreation(PartEntity objectToUpdate, Date updateTimestamp, String eTag) throws MetadataOperationFailureException {
    objectToUpdate.setObjectModifiedTimestamp(updateTimestamp);
    objectToUpdate.seteTag(eTag);
    objectToUpdate.setIsLatest(true);
    return this.transitionPartToState(objectToUpdate, ObjectState.extant);
  }

  /**
   * Provides the search criteria to handle the FK relation from PartEntity->Bucket Returns a criteria for a search that matches the given bucket
   * 
   * @param baseCriteria
   * @param bucket
   * @return
   */
  protected static Criteria getSearchByBucket(@Nonnull Criteria baseCriteria, @Nullable Bucket bucket) {
    if (bucket != null) {
      return baseCriteria.createCriteria("bucket").add(Restrictions.eq("naturalId", bucket.getNaturalId()));
    } else {
      return baseCriteria;
    }
  }

  @Override
  public void cleanupInvalidParts(final Bucket bucket, final String objectKey, final String uploadId, final int partNumber) throws Exception {
    final PartEntity searchExample = new PartEntity(bucket, objectKey, uploadId).withPartNumber(partNumber);

    final Predicate<PartEntity> repairPredicate = new Predicate<PartEntity>() {
      public boolean apply(PartEntity example) {
        try {

          // Find not-latest null-versioned objects and mark them for deletion.
          PartEntity searchExample =
              new PartEntity().withKey(example.getObjectKey()).withBucket(example.getBucket()).withState(ObjectState.extant)
                  .withUploadId(example.getUploadId()).withPartNumber(example.getPartNumber());
          Criteria searchCriteria = Entities.createCriteria(PartEntity.class);
          searchCriteria.add(Example.create(searchExample)).addOrder(Order.desc("objectModifiedTimestamp"));
          searchCriteria = getSearchByBucket(searchCriteria, example.getBucket());
          List<PartEntity> results = searchCriteria.list();
          if (results.size() <= 1) {
            // nothing to do
            return true;
          }

          results.get(0).setIsLatest(true);
          // Set all but the first element as not latest
          for (PartEntity obj : results.subList(1, results.size())) {
            LOG.trace("Marking mpu part " + obj.getPartUuid() + " as no longer latest version and for cleanup");
            obj.setIsLatest(false);
            obj = transitionPartToState(obj, ObjectState.deleting);
          }
        } catch (NoSuchElementException e) {
          // Nothing to do.
        } catch (Exception e) {
          LOG.error("Error consolidating PartEntity records for " + example.getBucket().getBucketName() + "/" + example.getObjectKey()
              + " uploadId = " + example.getUploadId() + " partNumber = " + example.getPartNumber());
          return false;
        }
        return true;
      }
    };

    try {
      Entities.asTransaction(repairPredicate).apply(searchExample);
    } catch (final Throwable f) {
      LOG.error("Error in version/null repair", f);
    }
  }

  /**
   * Returns the ObjectEntities that are in 'creating' for too long and thus should be considered failed
   */
  @Override
  public List<PartEntity> lookupFailedParts() throws MetadataOperationFailureException {
    // Return the latest version based on the created date.
    try (TransactionResource trans = Entities.transactionFor(PartEntity.class)) {
      PartEntity searchExample = new PartEntity().withState(ObjectState.creating);
      Criteria search = Entities.createCriteria(PartEntity.class);
      List<PartEntity> results =
          search.add(Example.create(searchExample)).add(Restrictions.lt("creationExpiration", System.currentTimeMillis())).list();
      trans.commit();
      return results;
    } catch (NoSuchElementException e) {
      // Swallow this exception
      return new ArrayList(0);
    } catch (Exception e) {
      LOG.warn("Error fetching failed or deleted object records");
      throw new MetadataOperationFailureException(e);
    }
  }

  @Override
  public void delete(final @Nonnull PartEntity objectToDelete) throws IllegalResourceStateException, MetadataOperationFailureException {
    try {
      Transactions.delete(objectToDelete);
    } catch (MetadataOperationFailureException | IllegalResourceStateException e) {
      throw e;
    } catch (Exception e) {
      throw new MetadataOperationFailureException(e);
    }
  }

  @Override
  public List<PartEntity> lookupPartsInState(Bucket searchBucket, String searchKey, String uploadId, ObjectState state) throws Exception {
    EntityTransaction db = Entities.get(PartEntity.class);
    try {
      Criteria search = Entities.createCriteria(PartEntity.class);
      PartEntity searchExample = new PartEntity().withBucket(searchBucket).withKey(searchKey).withUploadId(uploadId).withState(state);
      search.add(Example.create(searchExample));
      if (searchBucket != null) {
        search = getSearchByBucket(search, searchBucket);
      }
      List<PartEntity> results = search.list();
      db.commit();
      return results;
    } finally {
      if (db != null && db.isActive()) {
        db.rollback();
      }
    }
  }

  @Override
  public void removeParts(final Bucket bucket, final String uploadId) throws Exception {
    Predicate<String> removePredicate = new Predicate<String>() {
      public boolean apply(String uploadId) {
        try (TransactionResource db = Entities.transactionFor(PartEntity.class)) {
          // Calculate the sum size of the parts to update the bucket size.
          PartEntity searchExample = new PartEntity().withUploadId(uploadId).withState(ObjectState.extant);
          long size =
              Objects.firstNonNull(
                  (Number) Entities.createCriteria(PartEntity.class).add(Example.create(searchExample)).setProjection(Projections.sum("size"))
                      .setReadOnly(true).uniqueResult(), 0).longValue();

          // Remove all part records with this upload id
          Entities.deleteAllMatching(PartEntity.class, "where part_number IS NOT NULL and upload_id=:uploadId",
              Collections.singletonMap("uploadId", uploadId));
          db.commit();
        } catch (Exception e) {
          LOG.trace("Error finalizing part-removal transaction. Will retry.", e);
          throw new RuntimeException(e);
        }

        return true;
      }
    };

    Entities.asTransaction(PartEntity.class, removePredicate).apply(uploadId);
  }

  @Override
  public void flushAllParts(Bucket bucket) throws Exception {
    try (TransactionResource db = Entities.transactionFor(PartEntity.class)) {
      Criteria search = Entities.createCriteria(PartEntity.class);
      PartEntity searchExample = new PartEntity().withBucket(bucket);
      search.add(Example.create(searchExample));
      search = getSearchByBucket(search, bucket);
      List<PartEntity> uploads = search.list();
      for (PartEntity e : uploads) {
        Entities.delete(e);
      }
      db.commit();
    }
  }

  @Override
  public PartEntity transitionPartToState(@Nonnull final PartEntity entity, @Nonnull ObjectState destState) throws IllegalResourceStateException,
      MetadataOperationFailureException {
    Function<PartEntity, PartEntity> transitionFunction;

    switch (destState) {
      case creating:
        transitionFunction = MpuPartStateTransitions.TRANSITION_TO_CREATING;
        break;
      case extant:
        transitionFunction = MpuPartStateTransitions.TRANSITION_TO_EXTANT;
        break;
      case deleting:
        transitionFunction = MpuPartStateTransitions.TRANSITION_TO_DELETING;
        break;
      default:
        LOG.error("Unexpected destination state: " + destState);
        throw new IllegalArgumentException();
    }

    try {
      return Entities.asTransaction(PartEntity.class, transitionFunction).apply(entity);
    } catch (ObjectStorageInternalException e) {
      throw e;
    } catch (Exception e) {
      throw new MetadataOperationFailureException(e);
    }
  }

  /**
   * Update the progress timeout field in the object entity. Will set it to current-time + ObjectStorageProperties.PROGRESS_TIMEOUT_SEC
   * 
   * @param entity
   * @throws Exception
   */
  @Override
  public PartEntity updateCreationTimeout(PartEntity entity) throws Exception {
    try (TransactionResource trans = Entities.transactionFor(PartEntity.class)) {
      PartEntity mergedEntity = Entities.merge(entity);
      if (ObjectState.creating.equals(mergedEntity.getState())) {
        mergedEntity.updateCreationExpiration();
      }
      Entities.flush(mergedEntity); // Ensure it is pushed right away
      trans.commit();
      return mergedEntity;
    } catch (Exception e) {
      LOG.error("Error updating progress timeout for object " + entity.getPartUuid());
      throw e;
    }
  }

  @Override
  public HashMap<Integer, PartEntity> getParts(Bucket bucket, String objectKey, String uploadId) throws Exception {
    HashMap<Integer, PartEntity> parts = new HashMap<>();
    try (TransactionResource trans = Entities.transactionFor(PartEntity.class)) {
      Criteria search = Entities.createCriteria(PartEntity.class);
      PartEntity searchExample = new PartEntity(bucket, objectKey, uploadId).withState(ObjectState.extant);
      search.add(Example.create(searchExample));
      search = getSearchByBucket(search, bucket);
      List<PartEntity> results = search.list();
      trans.commit();
      for (PartEntity result : results) {
        parts.put(result.getPartNumber(), result);
      }
      return parts;
    } catch (Exception e) {
      LOG.warn("Error looking up parts for MPU id : " + uploadId);
      throw e;
    }
  }

  @Override
  public long processPartListAndGetSize(List<Part> partsInManifest, HashMap<Integer, PartEntity> availableParts) throws S3Exception {
    int lastPartNumber = 0;
    long objectSize = 0;
    int numPartsProcessed = 0;
    for (Part partInManifest : partsInManifest) {
      Integer partNumber = partInManifest.getPartNumber();
      if (partNumber < ObjectStorageProperties.MIN_PART_NUMBER || partNumber > ObjectStorageProperties.MAX_PART_NUMBER) {
        throw new InvalidArgumentException("PartNumber", "Part number must be an integer between " + ObjectStorageProperties.MIN_PART_NUMBER
            + " and " + ObjectStorageProperties.MAX_PART_NUMBER + ", inclusive");
      }
      if (partNumber <= lastPartNumber) {
        throw new InvalidPartOrderException("partNumber: " + partNumber);
      }
      PartEntity actualPart = availableParts.get(partNumber);
      if (actualPart == null) {
        throw new InvalidPartException("partNumber: " + partNumber);
      }
      final long actualPartSize = actualPart.getSize();
      if ((++numPartsProcessed) < partsInManifest.size() && actualPartSize < ObjectStorageProperties.MPU_PART_MIN_SIZE) {
        throw new EntityTooSmallException("uploadId: " + actualPart.getUploadId() + " partNumber: " + partNumber);
      }
      objectSize += actualPartSize;
      lastPartNumber = partNumber;
    }
    return objectSize;
  }

  @Override
  public long getTotalSize(Bucket bucket) throws Exception {
    try (TransactionResource trans = Entities.transactionFor(PartEntity.class)) {
      Criteria queryCriteria =
          Entities.createCriteria(PartEntity.class)
              .add(Restrictions.or(Restrictions.eq("state", ObjectState.creating), Restrictions.eq("state", ObjectState.extant)))
              .setProjection(Projections.sum("size"));
      if (bucket != null) {
        queryCriteria = getSearchByBucket(queryCriteria, bucket);
      }
      queryCriteria.setReadOnly(true);
      final Number count = (Number) queryCriteria.uniqueResult();
      return count == null ? 0 : count.longValue();
    } catch (Throwable e) {
      LOG.error("Error getting part total size for bucket " + bucket.getBucketName(), e);
      throw new Exception(e);
    }
  }

  @Override
  public PaginatedResult<PartEntity> listPartsForUpload(final Bucket bucket, final String objectKey, final String uploadId,
      final int partNumberMarker, final int maxParts) throws Exception {

    EntityTransaction db = Entities.get(PartEntity.class);
    try {
      PaginatedResult<PartEntity> result = new PaginatedResult<PartEntity>();
      HashSet<String> commonPrefixes = new HashSet<String>();

      // Include zero since 'istruncated' is still valid
      if (maxParts >= 0) {
        final int queryStrideSize = maxParts + 1;
        PartEntity searchPart = new PartEntity(bucket, objectKey, uploadId).withState(ObjectState.extant);

        Criteria objCriteria = Entities.createCriteria(PartEntity.class);
        objCriteria.setReadOnly(true);
        objCriteria.setFetchSize(queryStrideSize);
        objCriteria.add(Example.create(searchPart));
        objCriteria.addOrder(Order.asc("partNumber"));
        objCriteria.setMaxResults(queryStrideSize);

        if (partNumberMarker > 0) {
          objCriteria.add(Restrictions.gt("partNumber", partNumberMarker));
        }

        objCriteria = getSearchByBucket(objCriteria, bucket);

        List<PartEntity> partInfos = null;
        int resultKeyCount = 0;
        String[] parts = null;
        int pages = 0;

        // Iterate over result sets of size maxkeys + 1 since
        // commonPrefixes collapse the list, we may examine many more
        // records than maxkeys + 1
        do {
          parts = null;

          // Skip ahead the next page of 'queryStrideSize' results.
          objCriteria.setFirstResult(pages++ * queryStrideSize);

          partInfos = (List<PartEntity>) objCriteria.list();
          if (partInfos == null) {
            // nothing to do.
            break;
          }

          for (PartEntity partRecord : partInfos) {

            if (resultKeyCount == maxParts) {
              result.setIsTruncated(true);
              resultKeyCount++;
              break;
            }

            result.getEntityList().add(partRecord);
            result.setLastEntry(partRecord);
            resultKeyCount++;
          }

          if (resultKeyCount <= maxParts && partInfos.size() <= maxParts) {
            break;
          }
        } while (resultKeyCount <= maxParts);
      } else {
        throw new IllegalArgumentException("MaxKeys must be positive integer");
      }

      return result;
    } catch (Exception e) {
      LOG.error("Error generating paginated parts list for upload ID " + uploadId, e);
      throw e;
    } finally {
      db.rollback();
    }
  }
}
