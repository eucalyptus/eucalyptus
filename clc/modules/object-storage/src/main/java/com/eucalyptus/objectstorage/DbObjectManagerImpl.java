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

package com.eucalyptus.objectstorage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.persistence.EntityTransaction;

import com.eucalyptus.entities.TransactionResource;
import com.eucalyptus.objectstorage.entities.PartEntity;
import com.eucalyptus.objectstorage.msgs.*;
import org.apache.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Example;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;

import com.eucalyptus.auth.principal.User;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionException;
import com.eucalyptus.entities.Transactions;
import com.eucalyptus.objectstorage.entities.Bucket;
import com.eucalyptus.objectstorage.entities.ObjectEntity;
import com.eucalyptus.objectstorage.exceptions.s3.InternalErrorException;
import com.eucalyptus.objectstorage.exceptions.s3.S3Exception;
import com.eucalyptus.objectstorage.util.OSGUtil;
import com.eucalyptus.objectstorage.util.ObjectStorageProperties;
import com.eucalyptus.records.Logs;
import com.eucalyptus.storage.msgs.s3.AccessControlPolicy;
import com.eucalyptus.util.EucalyptusCloudException;
import com.google.common.base.Predicate;
import com.google.common.base.Strings;
import com.google.gwt.thirdparty.guava.common.collect.Maps;

/**
 * Database backed implementation of ObjectManager
 */
public class DbObjectManagerImpl implements ObjectManager {
    private static final Logger LOG = Logger.getLogger(DbObjectManagerImpl.class);
    private static final ExecutorService HISTORY_REPAIR_EXECUTOR = Executors.newCachedThreadPool();

    public void start() throws Exception {
        // Do nothing
    }

    public void stop() throws Exception {
        try {
            List<Runnable> pendingTasks = HISTORY_REPAIR_EXECUTOR.shutdownNow();
            LOG.info("Stopping ObjectManager... Found " + pendingTasks.size() + " pending tasks at time of shutdown");
        } catch (final Throwable f) {
            LOG.error("Error stopping ObjectManager", f);
        }
    }

    @Override
    public <T, F> boolean exists(Bucket bucket, String objectKey, String versionId,
                                 CallableWithRollback<T, F> resourceModifier) throws Exception {
        try {
            return get(bucket, objectKey, versionId) != null;
        } catch (NoSuchElementException e) {
            return false;
        } catch (Exception e) {
            LOG.error("Error determining existence of " + bucket.getBucketName() + "/" + objectKey + " , version=" + versionId);
            throw e;
        }
    }

    @Override
    public long countRawEntities(Bucket bucket) throws Exception {
        /*
		 * Returns all entries, pending delete or not.
		 */
        EntityTransaction db = Entities.get(ObjectEntity.class);
        ObjectEntity exampleObject = new ObjectEntity(bucket.getBucketName(), null, null);

        try {
            return Entities.count(exampleObject);
        } catch (Throwable e) {
            LOG.error("Error getting object count for bucket " + bucket.getBucketName(), e);
            throw new Exception(e);
        } finally {
            db.rollback();
        }
    }

    /**
     * Returns the list of entities currently pending writes. List returned is
     * in no particular order. Caller must order if required.
     *
     * @param bucketName
     * @param objectKey
     * @param versionId
     * @return
     * @throws TransactionException
     */
    public List<ObjectEntity> getPendingWrites(Bucket bucket, String objectKey, String versionId) throws Exception {
        try {
            // Return the latest version based on the created date.
            EntityTransaction db = Entities.get(ObjectEntity.class);
            try {
                ObjectEntity searchExample = new ObjectEntity(bucket.getBucketName(), objectKey, versionId);
                Criteria search = Entities.createCriteria(ObjectEntity.class);
                List results = search.add(Example.create(searchExample))
                        .add(Restrictions.isNull("objectModifiedTimestamp")).list();
                db.commit();
                return (List<ObjectEntity>) results;
            } finally {
                if (db != null && db.isActive()) {
                    db.rollback();
                }
            }
        } catch (NoSuchElementException e) {
            // Nothing, return empty list
            return new ArrayList<ObjectEntity>(0);
        } catch (Exception e) {
            LOG.error("Error fetching pending write records for object " + bucket.getBucketName() + "/" + objectKey + "?versionId="
                    + versionId);
            throw e;
        }
    }

    /**
     * A more limited version of read-repair, it just modifies the 'islatest'
     * tag, but will not mark any for deletion
     */
    private static final Predicate<ObjectEntity> SET_LATEST_PREDICATE = new Predicate<ObjectEntity>() {
        public boolean apply(ObjectEntity example) {
            try {
                example.setIsLatest(true);
                Criteria search = Entities.createCriteria(ObjectEntity.class);
                List<ObjectEntity> results = search.add(Example.create(example))
                        .add(ObjectEntity.QueryHelpers.getNotDeletingRestriction())
                        .add(ObjectEntity.QueryHelpers.getNotPendingRestriction())
                        .addOrder(Order.desc("objectModifiedTimestamp")).list();

                if (results != null && results.size() > 1) {
                    try {
                        // Set all but the first element as not latest
                        for (ObjectEntity obj : results.subList(1, results.size())) {
                            obj.makeNotLatest();
                        }
                    } catch (IndexOutOfBoundsException e) {
                        // Either 0 or 1 result, nothing to do
                    }
                }
            } catch (NoSuchElementException e) {
                // Nothing to do.
            } catch (Exception e) {
                LOG.error("Error consolidationg Object records for " + example.getBucketName() + "/"
                        + example.getObjectKey());
                return false;
            }
            return true;
        }
    };

    /**
     * This is the proper function to use for doing read-repair operations
     */

    /**
     * Finds all object records and keeps latest, marks rest for deletion if
     * enabledVersioning == false, or just removes isLatest if enabledVersioning
     * == true
     *
     * @param bucketName
     * @param objectKey
     * @return
     * @throws Exception
     */
    public void repairObjectLatest(String bucketName, String objectKey) throws Exception {
        ObjectEntity searchExample = new ObjectEntity(bucketName, objectKey, null);
        try {
            Entities.asTransaction(SET_LATEST_PREDICATE).apply(searchExample);
        } catch (final Throwable f) {
            LOG.error("Error in version/null repair", f);
        }
    }

    /**
     * Scans the object for any contiguous "null" versioned records and removes
     * all but most recent. Only modifies contiguous, non-deleted records where
     * the versionId="null" (as a string).
     *
     * @param bucketName
     * @param objectKey
     * @throws Exception
     */
    public void doFullRepair(final Bucket bucket, final String objectKey) throws Exception {
        ObjectEntity searchExample = new ObjectEntity(bucket.getBucketName(), objectKey, null);

        final Predicate<ObjectEntity> repairPredicate = new Predicate<ObjectEntity>() {
            public boolean apply(ObjectEntity example) {

                if (bucket.isVersioningDisabled()) {
                    //Remove all but latest entry
                    try {
                        Criteria search = Entities.createCriteria(ObjectEntity.class);
                        List<ObjectEntity> results = search.add(Example.create(example))
                                .add(ObjectEntity.QueryHelpers.getNotDeletingRestriction())
                                .add(ObjectEntity.QueryHelpers.getNotPendingRestriction())
                                .add(ObjectEntity.QueryHelpers.getIsNotPartRestriction())
                                .addOrder(Order.desc("objectModifiedTimestamp")).list();

                        if (results != null && results.size() > 0) {
                            try {
                                if (results.get(0).getIsLatest()) {
                                    //Ensure set, but never transition from not-latest to latest. This just
                                    // makes sure all fields in the entity are set properly
                                    results.get(0).makeLatest();
                                }

                                // Set all but the first element as not latest
                                for (ObjectEntity obj : results.subList(1, results.size())) {
                                    LOG.trace("Marking object " + obj.getBucketName() + "/" + obj.getObjectUuid() + " for deletion because it is not latest.");
                                    obj.makeNotLatest();
                                    obj.markForDeletion();
                                }
                            } catch (IndexOutOfBoundsException e) {
                                // Either 0 or 1 result, nothing to do
                            }
                        }
                    } catch (NoSuchElementException e) {
                        // Nothing to do.
                    } catch (Exception e) {
                        LOG.error("Error consolidationg Object records for " + example.getBucketName() + "/"
                                + example.getObjectKey());
                        return false;
                    }
                } else {
                    //Versioning to consider
                    try {
                        Criteria search = Entities.createCriteria(ObjectEntity.class);
                        List<ObjectEntity> results = search.add(Example.create(example))
                                .add(ObjectEntity.QueryHelpers.getNotDeletingRestriction())
                                .add(ObjectEntity.QueryHelpers.getNotPendingRestriction())
                                .add(ObjectEntity.QueryHelpers.getIsNotPartRestriction())
                                .addOrder(Order.desc("objectModifiedTimestamp")).list();

                        ObjectEntity lastViewed = null;
                        if (results != null && results.size() > 0) {
                            try {
                                results.get(0).makeLatest();

                                // Set all but the first element as not latest
                                for (ObjectEntity obj : results.subList(1, results.size())) {
                                    LOG.trace("Marking object " + obj.getBucketName() + "/" + obj.getObjectUuid() + " as no longer latest version");
                                    obj.makeNotLatest();

                                    if (obj.isNullVersioned()) {
                                        if (lastViewed != null && lastViewed.isNullVersioned()) {
                                            LOG.trace("Marking object " + obj.getBucketName() + "/" + obj.getObjectUuid() + " for deletion because it is not latest.");
                                            obj.markForDeletion();
                                        }
                                        lastViewed = obj;
                                    } else {
                                        lastViewed = null;
                                    }
                                }
                            } catch (IndexOutOfBoundsException e) {
                                // Either 0 or 1 result, nothing to do
                            }
                        }
                    } catch (NoSuchElementException e) {
                        // Nothing to do.
                    } catch (Exception e) {
                        LOG.error("Error consolidationg Object records for " + example.getBucketName() + "/"
                                + example.getObjectKey());
                        return false;
                    }
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
     * Returns the ObjectEntities that have failed or are marked for deletion
     */
    @Override
    public List<ObjectEntity> getFailedOrDeleted() throws Exception {
        try {
            // Return the latest version based on the created date.
            EntityTransaction db = Entities.get(ObjectEntity.class);
            try {
                ObjectEntity searchExample = new ObjectEntity();
                Criteria search = Entities.createCriteria(ObjectEntity.class);
                List results = search.add(Example.create(searchExample))
                        .add(Restrictions.or(ObjectEntity.QueryHelpers.getDeletedRestriction(),
                                ObjectEntity.QueryHelpers.getFailedRestriction())).list();
                db.commit();
                return (List<ObjectEntity>) results;
            } finally {
                if (db != null && db.isActive()) {
                    db.rollback();
                }
            }
        } catch (NoSuchElementException e) {
            // Swallow this exception
        } catch (Exception e) {
            LOG.error("Error fetching failed or deleted object records");
            throw e;
        }

        return new ArrayList<ObjectEntity>(0);
    }

    @Override
    public ObjectEntity get(Bucket bucket, String objectKey, String versionId) throws Exception {
        try {
            // Return the latest version based on the created date.
            EntityTransaction db = Entities.get(ObjectEntity.class);
            try {
                ObjectEntity searchExample = new ObjectEntity(bucket.getBucketName(), objectKey, versionId);
                if (versionId == null) {
                    searchExample.setIsLatest(true);
                }

                Criteria search = Entities.createCriteria(ObjectEntity.class);
                List<ObjectEntity> results = search.add(Example.create(searchExample))
                        .addOrder(Order.desc("objectModifiedTimestamp"))
                        .add(ObjectEntity.QueryHelpers.getNotPendingRestriction())
                        .add(ObjectEntity.QueryHelpers.getNotDeletingRestriction()).list();

                if (results == null || results.size() < 1) {
                    throw new NoSuchElementException();
                } else if (results.size() > 1) {
                    this.repairObjectLatest(bucket.getBucketName(), objectKey);

                    //Do async repair if necessary to remove old data if overwritten
                    fireRepairTask(bucket, objectKey);
                }

                db.commit();
                return results.get(0);

            } finally {
                if (db != null && db.isActive()) {
                    db.rollback();
                }
            }
        } catch (NoSuchElementException ex) {
            throw ex;
        } catch (Exception e) {
            LOG.error("Error getting object entity for " + bucket.getBucketName() + "/" + objectKey + "?version=" + versionId, e);
            throw e;
        }
    }

    @Override
    public void delete(@Nonnull Bucket bucket, @Nonnull ObjectEntity objectToDelete, @Nonnull final User requestUser) throws Exception {
        if (bucket.isVersioningDisabled()) {
            //Do a synchronous delete of all records and objects for this key (using uuid)

            if (!ObjectStorageProperties.NULL_VERSION_ID.equals(objectToDelete.getVersionId()) && objectToDelete.getVersionId() != null) {
                throw new IllegalArgumentException("Cannot delete specific versionId on non-versioned bucket");
            }

            final ObjectStorageProviderClient osp;
            try {
                osp = ObjectStorageProviders.getInstance();
            } catch (NoSuchElementException e) {
                LOG.error("No provider client configured. Cannot execute delete operation", e);
                throw e;
            }

            //Get the latest entry to get its size for decrementing the bucket size later.
            final Long objectSize = objectToDelete.getSize();

            ObjectEntity example = new ObjectEntity(bucket.getBucketName(), objectToDelete.getObjectKey(), ObjectStorageProperties.NULL_VERSION_ID);
            Predicate<ObjectEntity> markObjectNulls = new Predicate<ObjectEntity>() {

                @Override
                public boolean apply(ObjectEntity objectExample) {
                    List<ObjectEntity> objectRecords = null;
                    try {
                        objectRecords = Entities.query(objectExample);

                        if (objectRecords != null) {
                            for (ObjectEntity object : objectRecords) {
                                try {
                                    object.markForDeletion();
                                } catch (Exception e) {
                                    LOG.error("Error calling backend in object delete: " + object.toString(), e);
                                }
                            }
                        }
                    } catch (NoSuchElementException e) {
                        //Nothing to do. fall through
                    } catch (final Throwable f) {
                        //Fail, safe because we haven't modified anything
                        return false;
                    }
                    return true;
                }
            };

            //Do the update to mark for deletion (in case of failure ensure gc
            if (!Entities.asTransaction(markObjectNulls).apply(example)) {
                throw new EucalyptusCloudException("Failed to mark records for deletion");
            }

            final DeleteObjectType deleteReq = new DeleteObjectType();
            deleteReq.setBucket(bucket.getBucketName());
            deleteReq.setUser(requestUser);
            try {
                deleteReq.setAccessKeyID(requestUser.getKeys().get(0).getAccessKey());
            } catch (final Throwable f) {
                LOG.error("Error getting access key for user: " + requestUser.getUserId());
                throw new Exception("Request user has no active access key to use for backend request");
            }
            Predicate<ObjectEntity> deleteObjectExact = new Predicate<ObjectEntity>() {

                @Override
                public boolean apply(ObjectEntity object) {
                    try {
                        deleteReq.setKey(object.getObjectUuid());
                        Logs.extreme().debug("Removing backend object for s3 object " + deleteReq.getBucket() + "/" + deleteReq.getKey());
                        DeleteObjectResponseType response = osp.deleteObject(deleteReq);

                        if (HttpResponseStatus.NO_CONTENT.equals(response.getStatus()) ||
                                HttpResponseStatus.NOT_FOUND.equals(response.getStatus()) ||
                                HttpResponseStatus.OK.equals(response.getStatus())) {
                            Logs.extreme().debug("Removing entity for s3 object " + object.getBucketName() + "/" + object.getObjectUuid());
                            return true;
                        } else {
                            LOG.error("Error removing backend object for s3 object " + object.getBucketName() + "/" + object.getObjectUuid() +
                                    " got response " + response.getStatus().toString() + " - " + response.getStatusMessage());
                        }
                    } catch (Exception e) {
                        LOG.error("Error calling backend in object delete: " + object.toString(), e);
                    }
                    return false;
                }
            };

            //Do the update, delete each record
            try {
                if (!Transactions.deleteAll(example, deleteObjectExact)) {
                    LOG.warn("Some objects not cleaned during delete operation, will remove asyncrounously later");
                }
            } catch (final Throwable f) {
                LOG.error("Error doing backend object deletion transaction", f);
                //ok, this will be finished asynchronously
            }

            try {
                // Update bucket size
                BucketManagers.getInstance().updateBucketSize(bucket.getBucketName(), -(objectSize.longValue()));
            } catch (NoSuchElementException e) {
                // Ok, not found. Can't update what isn't there. May have been
                // updated concurrently.
            } catch (Exception e) {
                LOG.warn("Error updating bucket size for removal of object:" + bucket.getBucketName() + "/" + objectToDelete.getObjectKey());
            }
        } else {
            throw new Exception("Versioning found not-disabled, versioned buckets not supported yet");
			
			/*
			 * Will place a delete-marker at the top of the stack (e.g. isLatest = true)
			 *  
			 */
        }
    }


    @Override
    public <T extends ObjectStorageDataResponseType, F> T create(final Bucket bucket, final ObjectEntity object,
                                                                 CallableWithRollback<T, F> resourceModifier) throws S3Exception, TransactionException {
        T result = null;
        try {
            ObjectEntity savedEntity = null;
            // Persist the new record in the 'pending' state.
            try {
                savedEntity = Transactions.saveDirect(object);
            } catch (TransactionException e) {
                // Fail. Could not persist.
                LOG.error("Transaction error creating initial object metadata for " + object.getResourceFullName(), e);
            } catch (Exception e) {
                // Fail. Unknown.
                LOG.error("Error creating initial object metadata for " + object.getResourceFullName(), e);
            }

            // Send the data through to the backend
            if (resourceModifier != null) {
                // This could be a long-lived operation...minutes
                result = resourceModifier.call();

                // Update the record and cleanup
                Date updatedDate = null;
                if (result != null) {
                    if (result.getLastModified() != null) {
                        updatedDate = result.getLastModified();
                    } else {
                        updatedDate = new Date();
                    }

                    //Use the same versionId since that is generated here
                    savedEntity.finalizeCreation(object.getVersionId(), updatedDate, result.getEtag());
                } else {
                    throw new Exception("Backend returned null result");
                }
            } else {
                // No Callable, so no result, just save the entity as given.
                savedEntity.finalizeCreation(null, new Date(), "");
            }

            // Update metadata post-call
            EntityTransaction db = Entities.get(ObjectEntity.class);
            try {
                Entities.mergeDirect(savedEntity);

                // Update bucket size
                try {
                    BucketManagers.getInstance().updateBucketSize(bucket.getBucketName(), savedEntity.getSize());
                } catch (final Throwable f) {
                    LOG.warn("Error updating bucket " + bucket.getBucketName() + " total object size. Not failing object put of .",
                            f);
                }

                db.commit();
            } catch (Exception e) {
                LOG.error("Error saving metadata object:" + bucket.getBucketName() + "/" + object.getObjectKey() + " version "
                        + object.getVersionId());
                throw e;
            } finally {
                if (db != null && db.isActive()) {
                    db.rollback();
                }
            }

            fireRepairTask(bucket, savedEntity.getObjectKey());

            return result;
        } catch (S3Exception e) {
            LOG.error("Error creating object: " + bucket.getBucketName() + "/" + object.getObjectKey());
            try {
                // Call the rollback. It is up to the provider to ensure the
                // rollback is correct for that backend
                if (resourceModifier != null) {
                    resourceModifier.rollback(result);
                }
            } catch (Exception ex) {
                LOG.error("Error rolling back object create", ex);
            }
            throw e;
        } catch (Exception e) {
            LOG.error("Error creating object: " + bucket.getBucketName() + "/" + object.getObjectKey());

            try {
                if (resourceModifier != null) {
                    resourceModifier.rollback(result);
                }
            } catch (Exception ex) {
                LOG.error("Error rolling back object create", ex);
            }
            throw new InternalErrorException(object.getBucketName() + "/" + object.getObjectKey());
        }
    }

    @Override
    public ObjectEntity createPending(Bucket bucket, ObjectEntity object) throws Exception {
        try {
            ObjectEntity savedEntity = null;
            // Persist the new record in the 'pending' state.
            try {
                savedEntity = Transactions.saveDirect(object);
                return savedEntity;
            } catch (TransactionException e) {
                // Fail. Could not persist.
                LOG.error("Transaction error creating initial object metadata for " + object.getResourceFullName(), e);
                throw e;
            } catch (Exception e) {
                // Fail. Unknown.
                LOG.error("Error creating initial object metadata for " + object.getResourceFullName(), e);
                throw e;
            }
        } catch (Exception e) {
            throw new InternalErrorException(object.getBucketName() + "/" + object.getObjectKey());
        }
    }

    @Override
    public <T extends ObjectStorageDataResponseType, F> T createPart(Bucket bucket, PartEntity part, CallableWithRollback<T, F> resourceModifier) throws Exception {
        T result = null;
        try {
            PartEntity savedEntity = null;
            // Persist the new record in the 'pending' state.
            try {
                savedEntity = Transactions.saveDirect(part);
            } catch (TransactionException e) {
                // Fail. Could not persist.
                LOG.error("Transaction error creating initial object metadata for " + part.getResourceFullName(), e);
            } catch (Exception e) {
                // Fail. Unknown.
                LOG.error("Error creating initial object metadata for " + part.getResourceFullName(), e);
            }

            // Send the data through to the backend
            if (resourceModifier != null) {
                // This could be a long-lived operation...minutes
                result = resourceModifier.call();

                // Update the record and cleanup
                Date updatedDate = null;
                if (result != null) {
                    if (result.getLastModified() != null) {
                        updatedDate = result.getLastModified();
                    } else {
                        updatedDate = new Date();
                    }

                    //Use the same versionId since that is generated here
                    savedEntity.finalizeCreation(updatedDate, result.getEtag());
                } else {
                    throw new Exception("Backend returned null result");
                }
            } else {
                // No Callable, so no result, just save the entity as given.
                savedEntity.finalizeCreation(new Date(), "");
            }

           // fireRepairTask(bucket, savedEntity.getObjectKey());

            return result;
        } catch (S3Exception e) {
            LOG.error("Error creating part: " + bucket.getBucketName() + "/" + part.getObjectKey());
            try {
                // Call the rollback. It is up to the provider to ensure the
                // rollback is correct for that backend
                if (resourceModifier != null) {
                    resourceModifier.rollback(result);
                }
            } catch (Exception ex) {
                LOG.error("Error rolling back part create", ex);
            }
            throw e;
        } catch (Exception e) {
            LOG.error("Error creating part: " + bucket.getBucketName() + "/" + part.getObjectKey());

            try {
                if (resourceModifier != null) {
                    resourceModifier.rollback(result);
                }
            } catch (Exception ex) {
                LOG.error("Error rolling back part create", ex);
            }
            throw new InternalErrorException(part.getBucketName() + "/" + part.getObjectKey());
        }
    }

    @Override
    public void updateObject(Bucket bucket, ObjectEntity object) throws Exception {
        // Update metadata post-call
        EntityTransaction db = Entities.get(ObjectEntity.class);
        try {
            Entities.mergeDirect(object);
            // Update bucket size
            try {
                BucketManagers.getInstance().updateBucketSize(bucket.getBucketName(), object.getSize());
            } catch (final Throwable f) {
                LOG.warn("Error updating bucket " + bucket.getBucketName() + " total object size. Not failing object put of .",
                        f);
            }
            db.commit();
        } catch (Exception e) {
            LOG.error("Error saving metadata object:" + bucket.getBucketName() + "/" + object.getObjectKey() + " version "
                    + object.getVersionId());
            throw e;
        } finally {
            if (db != null && db.isActive()) {
                db.rollback();
            }
        }
    }

    protected void fireRepairTask(final Bucket bucket, final String objectKey) {
        try {
            HISTORY_REPAIR_EXECUTOR.submit(new Runnable() {
                public void run() {
                    try {
                        doFullRepair(bucket, objectKey);
                    } catch (final Throwable f) {
                        LOG.error("Error during object history consolidation for " + bucket + "/" + objectKey, f);
                    }
                }
            });
        } catch (final Throwable f) {
            LOG.warn("Error setting object history for " + bucket + "/" + objectKey + ".", f);
        }
    }

    @Override
    public <T extends SetRESTObjectAccessControlPolicyResponseType, F> T setAcp(ObjectEntity object,
                                                                                AccessControlPolicy acp, CallableWithRollback<T, F> resourceModifier) throws S3Exception,
            TransactionException {
        T result = null;
        try {
            EntityTransaction db = Entities.get(ObjectEntity.class);
            try {
                if (resourceModifier != null) {
                    result = resourceModifier.call();
                }

                // Do record swap if existing record is found.
                ObjectEntity extantEntity = null;
                extantEntity = Entities.merge(object);
                extantEntity.setAcl(acp);
                db.commit();
                return result;
            } catch (Exception e) {
                LOG.error("Error updating ACP on object " + object.getBucketName() + "/" + object.getObjectKey()
                        + "?versionId=" + object.getVersionId());
                throw e;
            } finally {
                if (db != null && db.isActive()) {
                    db.rollback();
                }
            }
        } catch (S3Exception e) {
            LOG.error("Error setting ACP on backend for object: " + object.getBucketName() + "/"
                    + object.getObjectKey());
            try {
                // Call the rollback. It is up to the provider to ensure the
                // rollback is correct for that backend
                if (resourceModifier != null) {
                    resourceModifier.rollback(result);
                }
            } catch (Exception ex) {
                LOG.error("Error rolling back object ACP put", ex);
            }
            throw e;
        } catch (Exception e) {
            LOG.error("Error setting ACP on backend for object: " + object.getBucketName() + "/"
                    + object.getObjectKey());

            try {
                if (resourceModifier != null) {
                    resourceModifier.rollback(result);
                }
            } catch (Exception ex) {
                LOG.error("Error rolling back object ACP put", ex);
            }
            throw new InternalErrorException(object.getBucketName() + "/" + object.getObjectKey() + "?versionId="
                    + object.getVersionId());
        }
    }

    @Override
    public PaginatedResult<ObjectEntity> listPaginated(final Bucket bucket, int maxKeys, String prefix, String delimiter,
                                                       String startKey) throws Exception {
        return listVersionsPaginated(bucket, maxKeys, prefix, delimiter, startKey, null, true);

    }

    @Override
    public PaginatedResult<ObjectEntity> listVersionsPaginated(final Bucket bucket, int maxEntries, String prefix,
                                                               String delimiter, String fromKeyMarker, String fromVersionId, boolean latestOnly) throws Exception {

        EntityTransaction db = Entities.get(ObjectEntity.class);
        try {
            PaginatedResult<ObjectEntity> result = new PaginatedResult<ObjectEntity>();
            HashSet<String> commonPrefixes = new HashSet<String>();

            // Include zero since 'istruncated' is still valid
            if (maxEntries >= 0) {
                final int queryStrideSize = maxEntries + 1;
                ObjectEntity searchObj = new ObjectEntity();
                searchObj.setBucketName(bucket.getBucketName());

                // Return latest version, so exclude delete markers as well.
                // This makes listVersion act like listObjects
                if (latestOnly) {
                    searchObj.setDeleted(false);
                    searchObj.setIsLatest(true);
                }

                Criteria objCriteria = Entities.createCriteria(ObjectEntity.class);
                objCriteria.setReadOnly(true);
                objCriteria.setFetchSize(queryStrideSize);
                objCriteria.add(Example.create(searchObj));
                objCriteria.add(ObjectEntity.QueryHelpers.getNotPendingRestriction());
                objCriteria.add(ObjectEntity.QueryHelpers.getNotDeletingRestriction());
                objCriteria.addOrder(Order.asc("objectKey"));
                objCriteria.addOrder(Order.desc("objectModifiedTimestamp"));
                objCriteria.setMaxResults(queryStrideSize);

                if (!Strings.isNullOrEmpty(fromKeyMarker)) {
                    objCriteria.add(Restrictions.gt("objectKey", fromKeyMarker));
                } else {
                    fromKeyMarker = "";
                }

                if (!Strings.isNullOrEmpty(fromVersionId)) {
                    objCriteria.add(Restrictions.gt("versionId", fromVersionId));
                } else {
                    fromVersionId = "";
                }

                if (!Strings.isNullOrEmpty(prefix)) {
                    objCriteria.add(Restrictions.like("objectKey", prefix, MatchMode.START));
                } else {
                    prefix = "";
                }

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
                            parts = objectRecord.getObjectKey().substring(prefix.length()).split(delimiter);
                            if (parts.length > 1) {
                                prefixString = prefix + parts[0] + delimiter;
                                if (!commonPrefixes.contains(prefixString)) {
                                    if (resultKeyCount == maxEntries) {
                                        // This is a new record, so we know
                                        // we're truncating if this is true
                                        result.setIsTruncated(true);
                                        resultKeyCount++;
                                        break;
                                    } else {
                                        // Add it to the common prefix set
                                        commonPrefixes.add(prefixString);
                                        result.lastEntry = prefixString;
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

                        if (resultKeyCount == maxEntries) {
                            // This is a new (non-commonprefix) record, so
                            // we know we're truncating
                            result.setIsTruncated(true);
                            resultKeyCount++;
                            break;
                        }

                        result.entityList.add(objectRecord);
                        result.lastEntry = objectRecord;
                        resultKeyCount++;
                    }

                    if (resultKeyCount <= maxEntries && objectInfos.size() <= maxEntries) {
                        break;
                    }
                } while (resultKeyCount <= maxEntries);

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
        } finally {
            db.rollback();
        }
    }

    @Override
    public long countValid(Bucket bucket) throws Exception {
		/*
		 * Returns all entries, pending delete or not.
		 */
        EntityTransaction db = Entities.get(ObjectEntity.class);
        ObjectEntity exampleObject = new ObjectEntity(bucket.getBucketName(), null, null);
        Criterion crit = Restrictions.and(ObjectEntity.QueryHelpers.getNotDeletingRestriction(), ObjectEntity.QueryHelpers.getNotPendingRestriction());
        try {
            return Entities.count(exampleObject, crit, new HashMap<String, String>());
        } catch (Throwable e) {
            LOG.error("Error getting object count for bucket " + bucket.getBucketName(), e);
            throw new Exception(e);
        } finally {
            db.rollback();
        }
    }

    @Override
    public long getUploadSize(Bucket bucket, String objectKey, String uploadId) throws Exception {
            /*
             * Returns size for all valid parts with a specified uploadId
             */
        long size = 0;
        EntityTransaction db = Entities.get(PartEntity.class);
        try {
            Criteria search = Entities.createCriteria(PartEntity.class);
            PartEntity searchExample = new PartEntity(bucket.getBucketName(), objectKey, uploadId, null);
	        searchExample.setUuid(null);
            List<PartEntity> results = search.add(Example.create(searchExample))
                    .add(PartEntity.QueryHelpers.getNotPendingRestriction()).list();
            db.commit();
            for (PartEntity part : results) {
                size += part.getSize();
            }
        } finally {
            if (db != null && db.isActive()) {
                db.rollback();
            }
        }
        return size;
    }

    @Override
    public ObjectEntity getObject(Bucket bucket, String uploadId) throws Exception {
        EntityTransaction db = Entities.get(ObjectEntity.class);
        try {
            Criteria search = Entities.createCriteria(ObjectEntity.class);
            ObjectEntity searchExample = new ObjectEntity(bucket.getBucketName(), null, null);
            searchExample.setUploadId(uploadId);
            searchExample.setPartNumber(null);
            List<ObjectEntity> results = search.add(Example.create(searchExample))
                    .add(ObjectEntity.QueryHelpers.getNotDeletingRestriction())
                    .add(ObjectEntity.QueryHelpers.getIsPendingRestriction()).list();

            db.commit();
            if (results.size() > 0) {
                return results.get(0);
            } else {
                throw new InternalErrorException("Unable to get pending object entity for: " + bucket.getBucketName() + " " + uploadId);
            }
        } finally {
            if (db != null && db.isActive()) {
                db.rollback();
            }
        }
    }

    @Override
    public ObjectEntity getObjects(Bucket bucket) throws Exception {
        EntityTransaction db = Entities.get(ObjectEntity.class);
        try {
            Criteria search = Entities.createCriteria(ObjectEntity.class);
            ObjectEntity searchExample = new ObjectEntity(bucket.getBucketName(), null, null);
            searchExample.setPartNumber(null);
            List<ObjectEntity> results = search.add(Example.create(searchExample))
                    .add(ObjectEntity.QueryHelpers.getNotDeletingRestriction())
                    .add(ObjectEntity.QueryHelpers.getIsPendingRestriction())
                    .add(ObjectEntity.QueryHelpers.getIsMultipartRestriction()).list();
            db.commit();
            if (results.size() > 0) {
                return results.get(0);
            } else {
                throw new InternalErrorException("Unable to get pending object entities for: " + bucket.getBucketName());
            }
        } finally {
            if (db != null && db.isActive()) {
                db.rollback();
            }
        }
    }

    @Override
    public List<PartEntity> getParts(Bucket bucket, String objectKey, String uploadId) throws Exception {
        EntityTransaction db = Entities.get(PartEntity.class);
        try {
            Criteria search = Entities.createCriteria(PartEntity.class);
            PartEntity searchExample = new PartEntity(bucket.getBucketName(), objectKey, uploadId, null);
            searchExample.setUuid(null);
            List<PartEntity> results = search.add(Example.create(searchExample))
                    .add(ObjectEntity.QueryHelpers.getNotPendingRestriction()).list();
            db.commit();
            return results;
        } finally {
            if (db != null && db.isActive()) {
                db.rollback();
            }
        }
    }

    @Override
    public void removeParts(Bucket bucket, String uploadId) throws Exception {
        try ( TransactionResource db =
                      Entities.transactionFor( PartEntity.class ) ) {
            Entities.deleteAllMatching( PartEntity.class,
                            "where part_number IS NOT NULL and upload_id=:uploadId",
                            Collections.singletonMap( "uploadId", uploadId ));
            db.commit( );
        }
    }

    @Override
    public <T extends ObjectStorageDataResponseType, F> T merge(final Bucket bucket, final ObjectEntity savedEntity,
                                                                 CallableWithRollback<T, F> resourceModifier) throws S3Exception, TransactionException {
        T result = null;
        try {
            // Send the data through to the backend
            if (resourceModifier != null) {
                // This could be a long-lived operation...minutes
                result = resourceModifier.call();

                // Update the record and cleanup
                Date updatedDate = null;
                if (result != null) {
                    if (result.getLastModified() != null) {
                        updatedDate = result.getLastModified();
                    } else {
                        updatedDate = new Date();
                    }

                    //Use the same versionId since that is generated here
                    savedEntity.finalizeCreation(savedEntity.getVersionId(), updatedDate, result.getEtag());
                } else {
                    throw new Exception("Backend returned null result");
                }
            } else {
                // No Callable, so no result, just save the entity as given.
                savedEntity.finalizeCreation(null, new Date(), "");
            }

            // Update metadata post-call
            EntityTransaction db = Entities.get(ObjectEntity.class);
            try {
                Entities.mergeDirect(savedEntity);

                // Update bucket size
                try {
                    BucketManagers.getInstance().updateBucketSize(bucket.getBucketName(), savedEntity.getSize());
                } catch (final Throwable f) {
                    LOG.warn("Error updating bucket " + bucket.getBucketName() + " total object size. Not failing object put of .",
                            f);
                }

                db.commit();
            } catch (Exception e) {
                LOG.error("Error saving metadata object:" + bucket.getBucketName() + "/" + savedEntity.getObjectKey() + " version "
                        + savedEntity.getVersionId());
                throw e;
            } finally {
                if (db != null && db.isActive()) {
                    db.rollback();
                }
            }

            fireRepairTask(bucket, savedEntity.getObjectKey());

            return result;
        } catch (S3Exception e) {
            LOG.error("Error merging object: " + bucket.getBucketName() + "/" + savedEntity.getObjectKey());
            try {
                // Call the rollback. It is up to the provider to ensure the
                // rollback is correct for that backend
                if (resourceModifier != null) {
                    resourceModifier.rollback(result);
                }
            } catch (Exception ex) {
                LOG.error("Error rolling back object create", ex);
            }
            throw e;
        } catch (Exception e) {
            LOG.error("Error creating object: " + bucket.getBucketName() + "/" + savedEntity.getObjectKey());

            try {
                if (resourceModifier != null) {
                    resourceModifier.rollback(result);
                }
            } catch (Exception ex) {
                LOG.error("Error rolling back object create", ex);
            }
            throw new InternalErrorException(savedEntity.getBucketName() + "/" + savedEntity.getObjectKey());
        }
    }

    @Override
    public PaginatedResult<PartEntity> listPartsForUpload(final Bucket bucket,
                                                   final String objectKey,
                                                   final String uploadId,
                                                   final Integer partNumberMarker,
                                                   final Integer maxParts) throws Exception {

        EntityTransaction db = Entities.get(PartEntity.class);
        try {
            PaginatedResult<PartEntity> result = new PaginatedResult<PartEntity>();
            HashSet<String> commonPrefixes = new HashSet<String>();

            // Include zero since 'istruncated' is still valid
            if (maxParts >= 0) {
                final int queryStrideSize = maxParts + 1;
                PartEntity searchPart = new PartEntity();
                searchPart.setBucketName(bucket.getBucketName());
                searchPart.setObjectKey(objectKey);
                searchPart.setUploadId(uploadId);

                Criteria objCriteria = Entities.createCriteria(PartEntity.class);
                objCriteria.setReadOnly(true);
                //objCriteria.setFetchSize(queryStrideSize);
                objCriteria.add(Example.create(searchPart));
                //objCriteria.add(PartEntity.QueryHelpers.getNotPendingRestriction());
                objCriteria.addOrder(Order.asc("partNumber"));
                objCriteria.addOrder(Order.desc("objectModifiedTimestamp"));
                //objCriteria.setMaxResults(queryStrideSize);

                if (partNumberMarker!= null) {
                    objCriteria.add(Restrictions.gt("partNumber", partNumberMarker));
                }

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

                        result.entityList.add(partRecord);
                        result.lastEntry = partRecord;
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

    @Override
    public PaginatedResult<ObjectEntity> listParts(Bucket bucket, int maxUploads, String prefix, String delimiter, String keyMarker, String uploadIdMarker) throws Exception {
        EntityTransaction db = Entities.get(ObjectEntity.class);
        try {
            PaginatedResult<ObjectEntity> result = new PaginatedResult<ObjectEntity>();
            HashSet<String> commonPrefixes = new HashSet<String>();

            // Include zero since 'istruncated' is still valid
            if (maxUploads >= 0) {
                final int queryStrideSize = maxUploads + 1;
                ObjectEntity searchObj = new ObjectEntity();
                searchObj.setBucketName(bucket.getBucketName());

                Criteria objCriteria = Entities.createCriteria(ObjectEntity.class);
                objCriteria.setReadOnly(true);
                objCriteria.setFetchSize(queryStrideSize);
                objCriteria.add(Example.create(searchObj));
                objCriteria.add(ObjectEntity.QueryHelpers.getIsPendingRestriction());
                objCriteria.add(ObjectEntity.QueryHelpers.getNotDeletingRestriction());
                objCriteria.add(ObjectEntity.QueryHelpers.getIsMultipartRestriction());
                objCriteria.addOrder(Order.asc("objectKey"));
                objCriteria.setMaxResults(queryStrideSize);

                if (!Strings.isNullOrEmpty(keyMarker)) {
                    objCriteria.add(Restrictions.gt("objectKey", keyMarker));
                } else {
                    keyMarker = "";
                }

                if (!Strings.isNullOrEmpty(uploadIdMarker)) {
                    objCriteria.add(Restrictions.gt("uploadId", uploadIdMarker));
                } else {
                    uploadIdMarker = "";
                }

                if (!Strings.isNullOrEmpty(prefix)) {
                    objCriteria.add(Restrictions.like("objectKey", prefix, MatchMode.START));
                } else {
                    prefix = "";
                }

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
                            parts = objectRecord.getObjectKey().substring(prefix.length()).split(delimiter);
                            if (parts.length > 1) {
                                prefixString = prefix + parts[0] + delimiter;
                                if (!commonPrefixes.contains(prefixString)) {
                                    if (resultKeyCount == maxUploads) {
                                        // This is a new record, so we know
                                        // we're truncating if this is true
                                        result.setIsTruncated(true);
                                        resultKeyCount++;
                                        break;
                                    } else {
                                        // Add it to the common prefix set
                                        commonPrefixes.add(prefixString);
                                        result.lastEntry = prefixString;
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

                        result.entityList.add(objectRecord);
                        result.lastEntry = objectRecord;
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
        } finally {
            db.rollback();
        }
    }
}
