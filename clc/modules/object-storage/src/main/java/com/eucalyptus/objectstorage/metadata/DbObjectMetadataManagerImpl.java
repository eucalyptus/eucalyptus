/*************************************************************************
 * Copyright 2009-2015 Ent. Services Development Corporation LP
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
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.persistence.EntityTransaction;

import org.apache.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.criterion.Example;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import com.eucalyptus.auth.principal.UserPrincipal;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionException;
import com.eucalyptus.entities.TransactionResource;
import com.eucalyptus.entities.Transactions;
import com.eucalyptus.objectstorage.ObjectMetadataManagers;
import com.eucalyptus.objectstorage.ObjectState;
import com.eucalyptus.objectstorage.PaginatedResult;
import com.eucalyptus.objectstorage.entities.Bucket;
import com.eucalyptus.objectstorage.entities.ObjectEntity;
import com.eucalyptus.objectstorage.exceptions.IllegalResourceStateException;
import com.eucalyptus.objectstorage.exceptions.MetadataOperationFailureException;
import com.eucalyptus.objectstorage.exceptions.ObjectStorageInternalException;
import com.eucalyptus.objectstorage.exceptions.s3.InternalErrorException;
import com.eucalyptus.objectstorage.exceptions.s3.NoSuchKeyException;
import com.eucalyptus.objectstorage.exceptions.s3.S3Exception;
import com.eucalyptus.objectstorage.util.ObjectStorageProperties;
import com.eucalyptus.storage.msgs.s3.AccessControlPolicy;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Strings;

/**
 * Database backed implementation of ObjectMetadataManager
 * 
 */
public class DbObjectMetadataManagerImpl implements ObjectMetadataManager {
  private static final Logger LOG = Logger.getLogger(DbObjectMetadataManagerImpl.class);

  private enum ObjectListingType{ All, Latest, RequiringCleanup }

  public void start() throws Exception {
    LOG.trace("Starting DbObjectMetadataManager");
  }

  public void stop() throws Exception {
    LOG.trace("Stopping DbObjectMetadataManager");
  }

  @Override
  public ObjectEntity initiateCreation(@Nonnull ObjectEntity objectToCreate) throws Exception {
    return this.transitionObjectToState(objectToCreate, ObjectState.creating);
  }

  @Override
  public ObjectEntity finalizeCreation(ObjectEntity objectToUpdate, Date updateTimestamp, String eTag) throws MetadataOperationFailureException {
    objectToUpdate.setObjectModifiedTimestamp(updateTimestamp);
    objectToUpdate.seteTag(eTag);
    objectToUpdate.markLatest();
    return this.transitionObjectToState(objectToUpdate, ObjectState.extant);
  }

  @Override
  public ObjectEntity finalizeMultipartInit(ObjectEntity objectToUpdate, Date updateTimestamp, String uploadId)
      throws MetadataOperationFailureException {
    objectToUpdate.setObjectModifiedTimestamp(updateTimestamp);
    objectToUpdate.setUploadId(uploadId);
    objectToUpdate.setIsLatest(false);
    return this.transitionObjectToState(objectToUpdate, ObjectState.mpu_pending);
  }

  @Override
  public List<ObjectEntity> lookupObjectsInState(@Nullable Bucket bucket, String objectKey, String versionId, ObjectState state) throws Exception {
    try (TransactionResource db = Entities.transactionFor(ObjectEntity.class)) {
      Criteria search =
          Entities.createCriteria(ObjectEntity.class).add(Example.create(new ObjectEntity(bucket, objectKey, versionId).withState(state)));
      search.addOrder(Order.desc("objectModifiedTimestamp"));
      if (bucket != null) {
        search = getSearchByBucket(search, bucket);
      }
      List<ObjectEntity> results = search.list();
      db.commit();
      return results;
    } catch (NoSuchElementException e) {
      // Nothing, return empty list
      return new ArrayList<>(0);
    } catch (Exception e) {
      LOG.error("Error fetching pending write records for object " + (bucket == null ? "" : bucket.getBucketName()) + "/" + objectKey + "?versionId="
          + versionId);
      throw e;
    }
  }

  @Override
  public List<ObjectEntity> lookupObjectsForReaping(Bucket bucket, String objectKeyPrefix, Date age) {
    List<ObjectEntity> results;
    try (TransactionResource tran = Entities.transactionFor(ObjectEntity.class)) {
      // setup example and criteria
      ObjectEntity example = new ObjectEntity().withState(ObjectState.extant).withBucket(bucket);
      Criteria search = Entities.createCriteria(ObjectEntity.class).add(Example.create(example));
      search.add(Restrictions.lt("creationTimestamp", age));
      if (objectKeyPrefix != null && !objectKeyPrefix.equals("")) {
        search.add(Restrictions.like("objectKey", objectKeyPrefix, MatchMode.START));
      }
      search = getSearchByBucket(search, bucket);
      results = search.list();
      tran.commit();
    } catch (Exception ex) {
      LOG.error("exception caught while retrieving objects prefix with " + objectKeyPrefix + " from bucket " + bucket.getBucketName()
          + ", error message - " + ex.getMessage());
      return Collections.EMPTY_LIST;
    }
    return results;
  }

  /**
   * Provides the search criteria to handle the FK relation from ObjectEntity->Bucket Returns a criteria for a search that matches the given bucket
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

  /**
   * A more limited version of read-repair, it just modifies the 'islatest' tag, but will not mark any for deletion
   */
  private static final Predicate<ObjectEntity> SET_LATEST_PREDICATE = new Predicate<ObjectEntity>() {
    public boolean apply(ObjectEntity example) {
      try {
        example.setIsLatest(true);
        example = example.withState(ObjectState.extant);
        Criteria search = Entities.createCriteria(ObjectEntity.class);
        search.add(Example.create(example)).addOrder(Order.desc("objectModifiedTimestamp"));
        search = getSearchByBucket(search, example.getBucket());
        List<ObjectEntity> results = search.list();

        if (results != null && results.size() > 1) {
          try {
            // Set all but the first element as not latest
            for (ObjectEntity obj : results.subList(1, results.size())) {
              obj.setIsLatest(false);
            }
          } catch (IndexOutOfBoundsException e) {
            // Either 0 or 1 result, nothing to do
          }
        }
      } catch (NoSuchElementException e) {
        // Nothing to do.
      } catch (Exception e) {
        LOG.error("Error consolidating Object records for " + example.getResourceFullName(), e);
        return false;
      }
      return true;

    }
  };

  @Override
  public void cleanupInvalidObjects(final Bucket bucket, final String objectKey) throws Exception {
    ObjectEntity searchExample = new ObjectEntity(bucket, objectKey, null);

    final Predicate<ObjectEntity> repairPredicate = new Predicate<ObjectEntity>() {
      public boolean apply(ObjectEntity example) {
        try {

          // Find object versions that need updated
          ObjectEntity searchExample =
              new ObjectEntity().withKey(example.getObjectKey()).withBucket(example.getBucket()).withState(ObjectState.extant);
          Criteria searchCriteria = Entities.createCriteria(ObjectEntity.class);
          searchCriteria.add(Example.create(searchExample));
          searchCriteria.add(Restrictions.or(Restrictions.eq("versionId", ObjectStorageProperties.NULL_VERSION_ID),
              Restrictions.eq("isLatest", Boolean.TRUE)));
          searchCriteria.addOrder(Order.desc("objectModifiedTimestamp"));
          searchCriteria = getSearchByBucket(searchCriteria, example.getBucket());
          List<ObjectEntity> results = searchCriteria.list();
          if (results.size() <= 1) {
            if (!results.isEmpty()) {
              results.get(0).setCleanupRequired(Boolean.FALSE);
            }
            return true;
          }

          ObjectEntity latest = results.get(0);
          latest.setIsLatest(Boolean.TRUE);
          latest.setCleanupRequired(Boolean.FALSE);
          // Set all but the first element as not latest
          for (ObjectEntity obj : results.subList(1, results.size())) {
            LOG.trace("Marking object " + obj.getObjectUuid() + " as no longer latest version for " + example.getObjectKey( ));
            obj.setIsLatest(Boolean.FALSE);
            if (latest.isNullVersioned() && obj.isNullVersioned()) {
              LOG.trace("Transitioning object version to deleting for " + example.getObjectKey( ) );
              transitionObjectToState(obj, ObjectState.deleting);
            }
          }
        } catch (NoSuchElementException e) {
          // Nothing to do.
        } catch (Exception e) {
          LOG.error("Error consolidationg Object records for " + example.getBucket().getBucketName() + "/" + example.getObjectKey());
          return false;
        }
        return true;
      }
    };

    try {
      LOG.trace("Starting cleanup for " + bucket.getBucketName() + "/" + objectKey);
      Entities.asTransaction(repairPredicate).apply(searchExample);
    } catch (final Throwable f) {
      LOG.error("Error in version/null repair", f);
    }
  }

  /**
   * Returns the ObjectEntities that are in 'creating' for too long and thus should be considered failed
   */
  @Override
  public List<ObjectEntity> lookupFailedObjects() throws MetadataOperationFailureException {
    // Return the latest version based on the created date.
    try (TransactionResource trans = Entities.transactionFor(ObjectEntity.class)) {
      ObjectEntity searchExample = new ObjectEntity().withState(ObjectState.creating);
      Criteria search = Entities.createCriteria(ObjectEntity.class);
      List<ObjectEntity> results =
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
  public ObjectEntity lookupObject(Bucket bucket, String objectKey, String versionId) throws NoSuchElementException,
      MetadataOperationFailureException {
    try {
      // Return the latest version based on the created date.
      try (TransactionResource trans = Entities.transactionFor(ObjectEntity.class)) {
        ObjectEntity searchExample = new ObjectEntity().withBucket(bucket).withKey(objectKey).withState(ObjectState.extant);
        if (Strings.isNullOrEmpty(versionId)) {
          searchExample.setIsLatest(true);
        } else {
          searchExample = searchExample.withVersionId(versionId);
        }

        Criteria search =
            Entities.createCriteria(ObjectEntity.class).add(Example.create(searchExample)).addOrder(Order.desc("objectModifiedTimestamp"))
                .setMaxResults(1);
        search = getSearchByBucket(search, bucket);
        List<ObjectEntity> results = search.list();

        if (results == null || results.size() < 1) {
          throw new NoSuchElementException();
        } else if (results.size() > 1) {
          // this.repairObjectLatest(bucket, objectKey);
          // Do async repair if necessary to remove old data if overwritten
          // fireRepairTask(bucket, objectKey);
        }

        trans.commit();
        return results.get(0);
      }
    } catch (NoSuchElementException ex) {
      throw ex;
    } catch (Exception e) {
      LOG.error("Error getting object entity for " + bucket.getBucketName() + "/" + objectKey + "?version=" + versionId, e);
      throw new MetadataOperationFailureException(e);
    }
  }

  @Override
  public ObjectEntity generateAndPersistDeleteMarker(@Nonnull ObjectEntity currentObject, @Nonnull AccessControlPolicy acp, @Nonnull UserPrincipal owningUser)
      throws MetadataOperationFailureException {
    final ObjectEntity deleteMarker = currentObject.generateNewDeleteMarkerFrom();

    try (TransactionResource trans = Entities.transactionFor(ObjectEntity.class)) {
      deleteMarker.setOwnerCanonicalId( owningUser.getCanonicalId( ) );
      deleteMarker.setOwnerDisplayName( owningUser.getAccountAlias( ) );
      deleteMarker.setOwnerIamUserDisplayName(owningUser.getName());
      deleteMarker.setOwnerIamUserId(owningUser.getUserId());
      deleteMarker.setAcl(acp);
      ObjectEntity persistedDeleteMarker = Entities.persist(deleteMarker);
      persistedDeleteMarker = ObjectMetadataManagers.getInstance().transitionObjectToState(persistedDeleteMarker, ObjectState.extant);
      trans.commit();
      return persistedDeleteMarker;
    } catch (Exception e) {
      LOG.warn("Failed to persist the delete marker " + deleteMarker.getObjectUuid());
      throw new MetadataOperationFailureException(e);
    }
  }

  @Override
  public void delete(final @Nonnull ObjectEntity objectToDelete) throws IllegalResourceStateException, MetadataOperationFailureException {
    try {
      // Delete markers can be just removed not state transitioned.
      if (objectToDelete.getIsDeleteMarker()) {
        Transactions.delete(objectToDelete);
        return;
      }

      boolean success = Entities.asTransaction(ObjectEntity.class, ObjectStateTransitions.TRANSITION_TO_DELETED).apply(objectToDelete);
      if (!success) {
        throw new MetadataOperationFailureException("Delete operation returned false");
      }
    } catch (MetadataOperationFailureException | IllegalResourceStateException e) {
      throw e;
    } catch (Exception e) {
      throw new MetadataOperationFailureException(e);
    }
  }

  @Override
  public void flushUploads(Bucket bucket) throws Exception {
    EntityTransaction db = Entities.get(ObjectEntity.class);
    try {
      Criteria search = Entities.createCriteria(ObjectEntity.class);
      ObjectEntity searchExample = new ObjectEntity().withBucket(bucket).withState(ObjectState.mpu_pending);
      search.add(Example.create(searchExample));
      search = getSearchByBucket(search, bucket);
      List<ObjectEntity> uploads = search.list();
      for (ObjectEntity e : uploads) {
        Entities.delete(e);
      }
      db.commit();
    } catch (Exception e) {
      throw new MetadataOperationFailureException(e);
    } finally {
      if (db != null && db.isActive()) {
        db.rollback();
      }
    }
  }

  @Override
  public ObjectEntity lookupUpload(Bucket bucket, String objectKey, String uploadId) throws NoSuchElementException, MetadataOperationFailureException {
    try {
      try (TransactionResource trans = Entities.transactionFor(ObjectEntity.class)) {
        ObjectEntity searchExample =
            new ObjectEntity().withBucket(bucket).withKey(objectKey).withUploadId(uploadId).withState(ObjectState.mpu_pending);
        Criteria searchUploadId = Entities.createCriteria(ObjectEntity.class).add(Example.create(searchExample));
        searchUploadId = getSearchByBucket(searchUploadId, bucket);
        List<ObjectEntity> results = searchUploadId.list();
        trans.commit();

        if (results == null || results.isEmpty()) {
          throw new NoSuchElementException();
        } else {
          return results.get(0);
        }
      }
    } catch (NoSuchElementException e) {
      throw e;
    } catch (Exception e) {
      LOG.error("Error getting object entity for " + bucket.getBucketName() + "/" + objectKey + "?uploadId=" + uploadId, e);
      throw new MetadataOperationFailureException(e);
    }
  }

  @Override
  public PaginatedResult<ObjectEntity> listUploads(Bucket bucket, int maxUploads, String prefix, String delimiter, String keyMarker,
      String uploadIdMarker) throws Exception {

    try (TransactionResource trans = Entities.transactionFor(ObjectEntity.class)) {
      PaginatedResult<ObjectEntity> result = new PaginatedResult<ObjectEntity>();
      HashSet<String> commonPrefixes = new HashSet<String>();

      // Include zero since 'istruncated' is still valid
      if (maxUploads >= 0) {
        final int queryStrideSize = maxUploads + 1;
        ObjectEntity searchObj = new ObjectEntity();
        searchObj.withBucket(bucket); // This doesn't actually filter, but do it anyway
        searchObj.withState(ObjectState.mpu_pending);

        Criteria objCriteria = Entities.createCriteria(ObjectEntity.class);
        objCriteria.setReadOnly(true);
        objCriteria.setFetchSize(queryStrideSize);
        objCriteria.add(Example.create(searchObj));
        objCriteria.addOrder(Order.asc("objectKey"));
        objCriteria.addOrder(Order.asc("uploadId"));
        objCriteria.setMaxResults(queryStrideSize);

        if (!Strings.isNullOrEmpty(keyMarker)) {
          if (!Strings.isNullOrEmpty(uploadIdMarker)) {
            // The result set should be exclusive of the pair that matches the key-marker upload-id-marker
            objCriteria.add(Restrictions.or(Restrictions.and(Restrictions.eq("objectKey", keyMarker), Restrictions.gt("uploadId", uploadIdMarker)),
                Restrictions.gt("objectKey", keyMarker)));
          } else {
            objCriteria.add(Restrictions.gt("objectKey", keyMarker));
            uploadIdMarker = "";
          }
        } else {
          keyMarker = "";
          uploadIdMarker = "";
        }

        if (!Strings.isNullOrEmpty(prefix)) {
          objCriteria.add(Restrictions.like("objectKey", prefix, MatchMode.START));
        } else {
          prefix = "";
        }

        // Be sure to add the bucket restriction last
        objCriteria = getSearchByBucket(objCriteria, bucket);

        // Ensure not null.
        if (Strings.isNullOrEmpty(delimiter)) {
          delimiter = "";
        }

        List<ObjectEntity> objectInfos = null;
        int resultKeyCount = 0;
        String[] parts = null;
        String prefixString = null;
        boolean useDelimiter = !Strings.isNullOrEmpty(delimiter);
        int pages = 0;

        // Iterate over result sets of size maxkeys + 1 since
        // commonPrefixes collapse the list, we may examine many more
        // records than maxkeys + 1
        do {
          parts = null;
          prefixString = null;

          // Skip ahead the next page of 'queryStrideSize' results.
          objCriteria.setFirstResult(pages++ * queryStrideSize);

          objectInfos = (List<ObjectEntity>) objCriteria.list();
          if (objectInfos == null) {
            // nothing to do.
            break;
          }

          for (ObjectEntity objectRecord : objectInfos) {
            if (useDelimiter) {
              // Check if it will get aggregated as a commonprefix
              // Split the substring with at least 2 matches as we need a result containing trailing strings. For instance
              // if "x" is delimiter and key string is also "x", then this key should be included in common prefixes.
              // "x".split("x") gives 0 strings which causes the subsequent logic to skip the key where as
              // "x".split("x", 2) gives 2 empty strings which is what the logic expects
              parts = objectRecord.getObjectKey().substring(prefix.length()).split(delimiter, 2);
              if (parts.length > 1) {
                prefixString = prefix + parts[0] + delimiter;
                if (!prefixString.equals(keyMarker) && !commonPrefixes.contains(prefixString)) {
                  if (resultKeyCount == maxUploads) {
                    // This is a new record, so we know
                    // we're truncating if this is true
                    result.setIsTruncated(true);
                    resultKeyCount++;
                    break;
                  } else {
                    // Add it to the common prefix set
                    commonPrefixes.add(prefixString);
                    result.setLastEntry(prefixString);
                    // count the unique commonprefix as a
                    // single return entry
                    resultKeyCount++;
                  }
                } else {
                  // Already have this prefix, so skip
                }
                continue;
              }
            }

            if (resultKeyCount == maxUploads) {
              // This is a new (non-commonprefix) record, so
              // we know we're truncating
              result.setIsTruncated(true);
              resultKeyCount++;
              break;
            }

            result.getEntityList().add(objectRecord);
            result.setLastEntry(objectRecord);
            resultKeyCount++;
          }

          if (resultKeyCount <= maxUploads && objectInfos.size() <= maxUploads) {
            break;
          }
        } while (resultKeyCount <= maxUploads);

        // Sort the prefixes from the hashtable and add to the reply
        if (commonPrefixes != null) {
          result.getCommonPrefixes().addAll(commonPrefixes);
          Collections.sort(result.getCommonPrefixes());
        }
      } else {
        throw new IllegalArgumentException("max uploads must be positive integer");
      }

      return result;
    } catch (Exception e) {
      LOG.error("Error generating paginated multipart upload list for bucket " + bucket.getBucketName(), e);
      throw e;
    }
  }

  @Override
  public ObjectEntity setAcp(ObjectEntity object, AccessControlPolicy acp) throws S3Exception, TransactionException {
    try (TransactionResource trans = Entities.transactionFor(ObjectEntity.class)) {
      // Do record swap if existing record is found.
      ObjectEntity extantEntity = Entities.merge(object);
      extantEntity.setAcl(acp);
      trans.commit();
      return extantEntity;
    } catch (Exception e) {
      LOG.error("Error setting ACP on backend for object: " + object.getResourceFullName());
      throw new InternalErrorException(object.getResourceFullName() + "?versionId=" + object.getVersionId());
    }
  }

  @Override
  public PaginatedResult<ObjectEntity> listPaginated(final Bucket bucket, int maxKeys, String prefix, String delimiter, String startKey)
      throws Exception {
    return doListVersionsPaginated(bucket, maxKeys, prefix, delimiter, startKey, null, ObjectListingType.Latest );

  }

  @Override
  public List<ObjectEntity> lookupObjectVersions(Bucket bucket, String objectKey, int numResults) throws Exception {
    ObjectEntity searchObj = new ObjectEntity().withBucket(bucket).withState(ObjectState.extant).withKey(objectKey);

    List<ObjectEntity> objectInfos = null;
    try (TransactionResource tran = Entities.transactionFor(ObjectEntity.class)) {
      Criteria objCriteria = Entities.createCriteria(ObjectEntity.class);
      objCriteria.setMaxResults(numResults);
      objCriteria.setReadOnly(true);
      objCriteria.add(Example.create(searchObj));
      objCriteria.addOrder(Order.desc("objectModifiedTimestamp"));

      objCriteria = getSearchByBucket(objCriteria, bucket);
      objectInfos = objCriteria.list();
      tran.commit();
    } catch (Exception ex) {
      LOG.warn("exception caught while retrieving all versions of object " + objectKey + " in bucket " + bucket.getBucketName());
      throw ex;
    }
    return objectInfos;
  }

  @Override
  public ObjectEntity makeLatest(ObjectEntity entity) throws Exception {
    ObjectEntity retrieved = null;
    try (TransactionResource tran = Entities.transactionFor(ObjectEntity.class)) {
      retrieved = lookupObject(entity.getBucket(), entity.getObjectKey(), entity.getVersionId());
      retrieved.markLatest();
      Entities.mergeDirect(retrieved);
      tran.commit();
    } catch (Exception ex) {
      LOG.warn("while attempting to set isLatest = true on the newest remaining object version, an exception was encountered: ", ex);
      throw ex;
    }
    return retrieved;
  }

  private ObjectEntity makeNotLatest(ObjectEntity entity) throws Exception {
    try (TransactionResource tran = Entities.transactionFor(ObjectEntity.class)) {
      ObjectEntity retrieved = Entities.merge(entity);
      retrieved.setIsLatest(Boolean.FALSE);
      tran.commit();
      return retrieved;
    } catch (Exception ex) {
      LOG.warn("while attempting to set isLatest = true on the newest remaining object version, an exception was encountered: ", ex);
      throw ex;
    }
  }

  @Override
  public PaginatedResult<ObjectEntity> listVersionsPaginated(
      final Bucket bucket,
      final int maxEntries,
      final String prefix,
      final String delimiter,
      final String fromKeyMarker,
      final String fromVersionId,
      final boolean latestOnly
  ) throws Exception {
    return doListVersionsPaginated(bucket, maxEntries, prefix, delimiter, fromKeyMarker, fromVersionId,
        latestOnly ? ObjectListingType.Latest : ObjectListingType.All);
  }

  @Override
  public PaginatedResult<ObjectEntity> listForCleanupPaginated(
      final Bucket bucket,
      final int maxKeys,
      final String prefix,
      final String delimiter,
      final String startKey)
      throws Exception {
    return doListVersionsPaginated(bucket, maxKeys, prefix, delimiter, startKey, null,
        ObjectListingType.RequiringCleanup);
  }

  protected PaginatedResult<ObjectEntity> doListVersionsPaginated(
      final Bucket bucket,
      final int maxEntries,
            String prefix,
            String delimiter,
      final String fromKeyMarker,
      final String fromVersionId,
      final ObjectListingType listingType
  ) throws Exception {
    try ( final TransactionResource db = Entities.readOnlyDistinctTransactionFor( ObjectEntity.class ) ) {
      PaginatedResult<ObjectEntity> result = new PaginatedResult<ObjectEntity>();
      HashSet<String> commonPrefixes = new HashSet<String>();

      // Include zero since 'istruncated' is still valid
      if (maxEntries >= 0) {
        ObjectEntity searchObj = new ObjectEntity().withBucket(bucket).withState(ObjectState.extant);

        // Return latest version, so exclude delete markers as well.
        // This makes listVersion act like listObjects
        if ( listingType != ObjectListingType.All ) {
          searchObj.setIsLatest(true);
          searchObj.setIsDeleteMarker(false);
          if ( listingType == ObjectListingType.RequiringCleanup ) {
            searchObj.setCleanupRequired(true);
          }
        }

        Criteria objCriteria = Entities.createCriteria(ObjectEntity.class);
        objCriteria.setReadOnly(true);
        objCriteria.setFetchSize(1_000);
        objCriteria.add(Example.create(searchObj));
        objCriteria.addOrder(Order.asc("objectKey"));
        objCriteria.addOrder(Order.desc("objectModifiedTimestamp"));

        if (!Strings.isNullOrEmpty(fromKeyMarker)) {
          if (!Strings.isNullOrEmpty(fromVersionId)) {
            // Look for the key that matches the key-marker and version-id-marker
            ObjectEntity searchObject = new ObjectEntity(bucket, fromKeyMarker, fromVersionId);
            ObjectEntity matchingObject = null;
            try {
              matchingObject = Entities.uniqueResult(searchObject);
              if (matchingObject == null || matchingObject.getObjectModifiedTimestamp() == null) {
                throw new NoSuchKeyException(bucket.getBucketName() + "/" + fromKeyMarker + "?versionId=" + fromVersionId);
              }
            } catch (Exception e) {
              LOG.warn("No matching object found for key-marker=" + fromKeyMarker + " and version-id-marker=" + fromVersionId);
              throw new NoSuchKeyException(bucket.getBucketName() + "/" + fromKeyMarker + "?versionId=" + fromVersionId);
            }

            // The result set should be exclusive of the key with the key-marker version-id-marker pair. Look for keys that chronologically
            // follow the version-id-marker for the given key-marker and also the keys that follow the key-marker.
            objCriteria.add(Restrictions.or(
                Restrictions.and(Restrictions.eq("objectKey", fromKeyMarker),
                    Restrictions.lt("objectModifiedTimestamp", matchingObject.getObjectModifiedTimestamp())),
                Restrictions.gt("objectKey", fromKeyMarker)));
          } else { // No version-id-marker, just set the criteria the key-marker
            objCriteria.add(Restrictions.gt("objectKey", fromKeyMarker));
          }
        } else {
          // No criteria to be set
        }

        if (!Strings.isNullOrEmpty(prefix)) {
          objCriteria.add(Restrictions.like("objectKey", prefix, MatchMode.START));
        } else {
          prefix = "";
        }

        objCriteria = getSearchByBucket(objCriteria, bucket);

        // Ensure not null.
        if (Strings.isNullOrEmpty(delimiter)) {
          delimiter = "";
        }

        int resultKeyCount = 0;
        String[] parts = null;
        String prefixString = null;
        boolean useDelimiter = !Strings.isNullOrEmpty(delimiter);

        // Iterate over results, since
        // commonPrefixes collapse the list, we may examine many more
        // records than maxkeys + 1
        final ScrollableResults objectResults = objCriteria.scroll( ScrollMode.FORWARD_ONLY );
        try {
          while ( objectResults.next( ) ) {
            final ObjectEntity objectRecord = (ObjectEntity) objectResults.get( 0 );
            if (useDelimiter) {
              // Check if it will get aggregated as a commonprefix
              // Split the substring with at least 2 matches as we need a result containing trailing strings. For instance
              // if "x" is delimiter and key string is also "x", then this key should be included in common prefixes.
              // "x".split("x") gives 0 strings which causes the subsequent logic to skip the key where as
              // "x".split("x", 2) gives 2 empty strings which is what the logic expects
              parts = objectRecord.getObjectKey().substring(prefix.length()).split(delimiter, 2);
              if (parts.length > 1) {
                prefixString = prefix + parts[0] + delimiter;
                if (!prefixString.equals(fromKeyMarker) && !commonPrefixes.contains(prefixString)) {
                  if (resultKeyCount == maxEntries) {
                    // This is a new record, so we know
                    // we're truncating if this is true
                    result.setIsTruncated(true);
                    break;
                  } else {
                    // Add it to the common prefix set
                    commonPrefixes.add(prefixString);
                    result.setLastEntry(prefixString);
                    // count the unique commonprefix as a
                    // single return entry
                    resultKeyCount++;
                  }
                } // else, already have this prefix, so skip
                Entities.evict( objectRecord );
                continue;
              }
            }

            if (resultKeyCount == maxEntries) {
              // This is a new (non-commonprefix) record, so
              // we know we're truncating
              result.setIsTruncated(true);
              break;
            }

            result.getEntityList().add(objectRecord);
            result.setLastEntry(objectRecord);
            resultKeyCount++;
          }
        } finally {
          objectResults.close( );
        }

        // Sort the prefixes from the hashtable and add to the reply
        if (commonPrefixes != null) {
          result.getCommonPrefixes().addAll(commonPrefixes);
          Collections.sort(result.getCommonPrefixes());
        }
      } else {
        throw new IllegalArgumentException("MaxKeys must be positive integer");
      }

      return result;
    } catch (Exception e) {
      LOG.error("Error generating paginated object list of bucket " + bucket.getBucketName(), e);
      throw e;
    }
  }

  @Override
  public long countValid(Bucket bucket) throws Exception {
    try (TransactionResource trans = Entities.transactionFor(ObjectEntity.class)) {
      /*
       * Criteria queryCriteria = Entities.createCriteria(ObjectEntity.class); queryCriteria.add(Restrictions.eq("state", ObjectState.extant))
       * .createCriteria("bucket").add(Restrictions.eq("naturalId", bucket.getNaturalId()))
       */
      Criteria queryCriteria =
          Entities.createCriteria(ObjectEntity.class).add(Restrictions.eq("state", ObjectState.extant)).setProjection(Projections.rowCount());
      queryCriteria = getSearchByBucket(queryCriteria, bucket);
      queryCriteria.setReadOnly(true);
      final Number count = (Number) queryCriteria.uniqueResult();
      trans.commit();
      return count.longValue();
    } catch (Throwable e) {
      LOG.error("Error getting object count for bucket " + bucket.getBucketName(), e);
      throw new Exception(e);
    }
  }

  @Override
  public long getTotalSize(Bucket bucket) throws Exception {
    try (TransactionResource trans = Entities.transactionFor(ObjectEntity.class)) {
      Criteria queryCriteria =
          Entities.createCriteria(ObjectEntity.class)
              .add(Restrictions.or(Restrictions.eq("state", ObjectState.creating), Restrictions.eq("state", ObjectState.extant)))
              .setProjection(Projections.sum("size"));
      if (bucket != null) {
        queryCriteria = getSearchByBucket(queryCriteria, bucket);
      }
      queryCriteria.setReadOnly(true);
      final Number count = (Number) queryCriteria.uniqueResult();
      return count == null ? 0 : count.longValue();
    } catch (Throwable e) {
      LOG.error("Error getting object total size for " + (bucket == null ? "all buckets" : "bucket " + bucket.getBucketName()), e);
      throw new Exception(e);
    }
  }

  @Override
  public ObjectEntity transitionObjectToState(@Nonnull final ObjectEntity entity, @Nonnull ObjectState destState)
      throws IllegalResourceStateException, MetadataOperationFailureException {
    Function<ObjectEntity, ObjectEntity> transitionFunction;

    switch (destState) {
      case creating:
        transitionFunction = ObjectStateTransitions.TRANSITION_TO_CREATING;
        break;
      case extant:
        transitionFunction = ObjectStateTransitions.TRANSITION_TO_EXTANT;
        break;
      case mpu_pending:
        transitionFunction = ObjectStateTransitions.TRANSITION_TO_MPU_PENDING;
        break;
      case deleting:
        transitionFunction = ObjectStateTransitions.TRANSITION_TO_DELETING;
        break;
      default:
        LOG.error("Unexpected destination state: " + destState);
        throw new IllegalArgumentException();
    }

    try {
      ObjectEntity result = Entities.asTransaction(ObjectEntity.class, transitionFunction).apply(entity);
      return result;
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
  public ObjectEntity updateCreationTimeout(ObjectEntity entity) throws Exception {
    try (TransactionResource trans = Entities.transactionFor(ObjectEntity.class)) {
      ObjectEntity mergedEntity = Entities.merge(entity);
      if (ObjectState.creating.equals(mergedEntity.getState())) {
        mergedEntity.updateCreationExpiration();
      }
      Entities.flush(mergedEntity); // Ensure it is pushed right away
      trans.commit();
      return mergedEntity;
    } catch (Exception e) {
      LOG.error("Error updating progress timeout for object " + entity.getObjectUuid());
      throw e;
    }
  }

}
