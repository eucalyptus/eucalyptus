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

import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionException;
import com.eucalyptus.objectstorage.BucketMetadataManagers;
import com.eucalyptus.objectstorage.BucketState;
import com.eucalyptus.objectstorage.MpuPartMetadataManagers;
import com.eucalyptus.objectstorage.ObjectMetadataManagers;
import com.eucalyptus.objectstorage.ObjectState;
import com.eucalyptus.objectstorage.entities.Bucket;
import com.eucalyptus.objectstorage.entities.ObjectEntity;
import com.eucalyptus.objectstorage.exceptions.IllegalResourceStateException;
import com.eucalyptus.objectstorage.exceptions.MetadataOperationFailureException;
import com.eucalyptus.objectstorage.exceptions.NoSuchEntityException;
import com.eucalyptus.objectstorage.exceptions.ObjectStorageInternalException;
import com.google.common.base.Function;
import com.google.common.base.Predicate;

import javax.annotation.Nullable;
import java.util.List;
import java.util.NoSuchElementException;
import org.apache.log4j.Logger;

/**
 * Created by zhill on 2/14/14.
 */
public class ObjectStateTransitions {
    private static final Logger LOG = Logger.getLogger(ObjectStateTransitions.class);

    /**
     * Inserts the new record into the db in 'creating' state.
     */
    static final Function<ObjectEntity, ObjectEntity> TRANSITION_TO_CREATING = new Function<ObjectEntity, ObjectEntity> () {

        @Nullable
        @Override
        public ObjectEntity apply(@Nullable ObjectEntity initializedObject) {
            if(initializedObject == null) {
                throw new RuntimeException("Null bucket record cannot be updated");
            } else {
                Bucket extantBucket;
                try {
                    extantBucket = Entities.uniqueResult(new Bucket().withUuid(initializedObject.getBucket().getBucketUuid()));
                } catch(NoSuchElementException e) {
                    throw new NoSuchEntityException(e);
                } catch(TransactionException e) {
                    throw new MetadataOperationFailureException(e);
                }

                if(extantBucket == null) {
                    throw new NoSuchEntityException(initializedObject.getBucket().getBucketUuid());
                }
                if(initializedObject.getState() != null && !ObjectState.creating.equals(initializedObject.getState())) {
                    throw new IllegalResourceStateException(initializedObject.getResourceFullName(), null, ObjectState.creating.toString(), initializedObject.getState().toString());
                } else {
                    if(extantBucket == null || !BucketState.extant.equals(extantBucket.getState())) {
                        //invalid bucket, cannot create
                        throw new IllegalResourceStateException(extantBucket.getBucketUuid(), null, BucketState.extant.toString(), extantBucket.getState().toString());
                    }
                    initializedObject.setBucket(extantBucket);
                    initializedObject.setState(ObjectState.creating);
                    initializedObject.updateCreationExpiration();
                    return Entities.persist(initializedObject);
                }

            }
        }
    };
    /**
     * Function that does the actual update of state. Will only transition creating->extant or mpu_pending->extant
     */
    static final Function<ObjectEntity, ObjectEntity> TRANSITION_TO_EXTANT = new Function<ObjectEntity, ObjectEntity> () {

        @Nullable
        @Override
        public ObjectEntity apply(@Nullable ObjectEntity entity) {
            if(entity == null) {
                throw new RuntimeException("Null bucket record cannot be updated");
            } else {
                try {
                    ObjectEntity updatingEntity = Entities.uniqueResult(new ObjectEntity().withUuid(entity.getObjectUuid()));
                    if(!ObjectState.deleting.equals(entity.getState())) {
                        //Remove old versions and update bucket size within this transaction.
                        //If quota enforcement wasn't needed this would be unnecessary.
                        //NOTE: this must be done before the new object is updated or it will clean it as well due to Hibernate context
                        // and session. First remove nulls, then set state to extant.
                        /*ObjectMetadataManagers.getInstance().cleanupAllNullVersionedObjectRecords(entity.getBucket(), entity.getObjectKey());

                        if(ObjectState.mpu_pending.equals(entity.getState())) {
                            MpuPartMetadataManagers.getInstance().removeParts(entity.getBucket(), entity.getUploadId());
                        }*/

                        //Set the new object state
                        updatingEntity.setState(ObjectState.extant);
                        updatingEntity.setCreationExpiration(null);
                        updatingEntity.setObjectModifiedTimestamp(entity.getObjectModifiedTimestamp());
                        updatingEntity.setIsLatest(true);
                        updatingEntity.seteTag(entity.geteTag());
                        updatingEntity.setSize(entity.getSize());

                        ObjectMetadataManagers.getInstance().cleanupInvalidObjects(updatingEntity.getBucket(), updatingEntity.getObjectKey());

                        if(ObjectState.mpu_pending.equals(updatingEntity.getLastState())) {
                            MpuPartMetadataManagers.getInstance().removeParts(updatingEntity.getBucket(), updatingEntity.getUploadId());
                        }

                        //Update the bucket size.
                        if(ObjectState.creating.equals(updatingEntity.getLastState()) || ObjectState.mpu_pending.equals(updatingEntity.getLastState())) {
                            BucketMetadataManagers.getInstance().updateBucketSize(updatingEntity.getBucket(), entity.getSize());
                        }

                    } else {
                        throw new IllegalResourceStateException("Cannot transition to extant from non-creating state", null, ObjectState.creating.toString(), entity.getState().toString());
                    }
                    return updatingEntity;
                } catch(NoSuchElementException e) {
                    throw new NoSuchEntityException(entity.getObjectUuid());
                } catch(ObjectStorageInternalException e) {
                    throw e;
                } catch(Exception e) {
                    LOG.warn("Cannot update state of object " + entity.getObjectUuid(), e);
                    throw new MetadataOperationFailureException(e);
                }
            }
        }
    };

    /**
     * Function that does the actual update of state. Will only transition creating->mpu_pending
     */
    static final Function<ObjectEntity, ObjectEntity> TRANSITION_TO_MPU_PENDING = new Function<ObjectEntity, ObjectEntity> () {

        @Nullable
        @Override
        public ObjectEntity apply(@Nullable ObjectEntity entity) {
            if(entity == null) {
                throw new RuntimeException("Null bucket record cannot be updated");
            } else {
                try {
                    ObjectEntity updatingEntity = Entities.uniqueResult(new ObjectEntity().withUuid(entity.getObjectUuid()));
                    if(ObjectState.creating.equals(updatingEntity.getState())) {
                        updatingEntity.setState(ObjectState.mpu_pending);
                        updatingEntity.setCreationExpiration(null);
                        updatingEntity.setObjectModifiedTimestamp(entity.getObjectModifiedTimestamp());
                        updatingEntity.setUploadId(entity.getUploadId());
                    } else {
                        throw new IllegalResourceStateException("Cannot transition to mpu-pending from non-creating state", null, ObjectState.creating.toString(), entity.getState().toString());
                    }
                    return updatingEntity;
                } catch(ObjectStorageInternalException e) {
                    throw e;
                } catch(Exception e) {
                    LOG.warn("Cannot update state of object " + entity.getObjectUuid(), e);
                    throw new MetadataOperationFailureException(e);
                }
            }
        }
    };
    /**
     * Function that does the actual update of state. Will transition from any state -> deleting
     */
    static final Function<ObjectEntity, ObjectEntity> TRANSITION_TO_DELETING = new Function<ObjectEntity, ObjectEntity> () {

        @Nullable
        @Override
        public ObjectEntity apply(@Nullable ObjectEntity objectToUpdate) {
            if(objectToUpdate == null) {
                throw new RuntimeException("Null bucket record cannot be updated");
            } else {
                try {
                    ObjectEntity entity;
                    //Shortcut to avoid a lookup if we're already loaded in a transaction
                    //This is most common case for deletion operations
                    if(!Entities.isPersistent(objectToUpdate)) {
                        entity = Entities.uniqueResult(new ObjectEntity().withUuid(objectToUpdate.getObjectUuid()));
                    } else {
                        entity = objectToUpdate;
                    }
                    entity.setState(ObjectState.deleting);

                    if(ObjectState.extant.equals(entity.getLastState())) {
                        BucketMetadataManagers.getInstance().updateBucketSize(entity.getBucket(), -entity.getSize());
                    }
                    return entity;
                } catch(NoSuchElementException e) {
                    throw new NoSuchEntityException(objectToUpdate.getObjectUuid());
                } catch(Exception e) {
                    throw new MetadataOperationFailureException(e);
                }
            }
        }
    };
    /**
     * Does the actual deletion of the entity
     */
    static final Predicate<ObjectEntity> TRANSITION_TO_DELETED = new Predicate<ObjectEntity> () {

        @Nullable
        @Override
        public boolean apply(@Nullable ObjectEntity objectToUpdate) {
            if(objectToUpdate == null) {
                throw new RuntimeException("Null bucket record cannot be updated");
            } else {
                try {
                    ObjectEntity entity;
                    //Shortcut to avoid a lookup if we're already loaded in a transaction
                    //This is most common case for deletion operations
                    if(!Entities.isPersistent(objectToUpdate)) {
                        entity = Entities.uniqueResult(new ObjectEntity().withUuid(objectToUpdate.getObjectUuid()));
                    } else {
                        entity = objectToUpdate;
                    }
                    if(!ObjectState.deleting.equals(objectToUpdate.getState())) {
                        throw new IllegalResourceStateException("Entity not in deleting state. Only valid transition to deleted is from deleting.", null, ObjectState.deleting.toString(), objectToUpdate.getState().toString());
                    }
                    Entities.delete(entity);
                    return true;
                } catch(ObjectStorageInternalException e) {
                    throw e;
                } catch(NoSuchElementException e) {
                    throw new NoSuchEntityException(objectToUpdate.getObjectUuid());
                } catch(Exception e) {
                    throw new MetadataOperationFailureException(e);
                }
            }
        }
    };
}
