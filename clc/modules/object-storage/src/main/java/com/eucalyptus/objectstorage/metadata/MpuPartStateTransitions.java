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
import com.eucalyptus.objectstorage.ObjectState;
import com.eucalyptus.objectstorage.entities.Bucket;
import com.eucalyptus.objectstorage.entities.PartEntity;
import com.eucalyptus.objectstorage.exceptions.IllegalResourceStateException;
import com.eucalyptus.objectstorage.exceptions.MetadataOperationFailureException;
import com.eucalyptus.objectstorage.exceptions.NoSuchEntityException;
import com.eucalyptus.objectstorage.exceptions.ObjectStorageInternalException;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import org.apache.log4j.Logger;

import javax.annotation.Nullable;
import java.util.NoSuchElementException;

/**
 * Created by zhill on 2/14/14.
 */
public class MpuPartStateTransitions {
    private static final Logger LOG = Logger.getLogger(MpuPartStateTransitions.class);

    /**
     * Inserts the new record into the db in 'creating' state.
     */
    static final Function<PartEntity, PartEntity> TRANSITION_TO_CREATING = new Function<PartEntity, PartEntity> () {

        @Nullable
        @Override
        public PartEntity apply(@Nullable PartEntity initializedObject) {
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
     * Function that does the actual update of state. Will only transition creating->extant
     */
    static final Function<PartEntity, PartEntity> TRANSITION_TO_EXTANT = new Function<PartEntity, PartEntity> () {

        @Nullable
        @Override
        public PartEntity apply(@Nullable PartEntity entity) {
            if(entity == null) {
                throw new RuntimeException("Null bucket record cannot be updated");
            } else {
                try {
                    PartEntity updatingEntity = Entities.uniqueResult(new PartEntity().withUuid(entity.getPartUuid()));
                    if(!ObjectState.deleting.equals(entity.getState())) {
                        //Set the new part state
                        updatingEntity.setState(ObjectState.extant);
                        updatingEntity.setCreationExpiration(null);
                        updatingEntity.setObjectModifiedTimestamp(entity.getObjectModifiedTimestamp());
                        updatingEntity.setIsLatest(true);
                        updatingEntity.seteTag(entity.geteTag());
                        updatingEntity.setSize(entity.getSize());

                        //Remove old versions and update bucket size within this transaction.
                        MpuPartMetadataManagers.getInstance().cleanupInvalidParts(entity.getBucket(), entity.getObjectKey(), entity.getUploadId(), entity.getPartNumber());

                        //Update the bucket size, guard to ensure duplicate state transitions, while idempotent, don't double sum
                        if(ObjectState.creating.equals(updatingEntity.getLastState())) {
                            BucketMetadataManagers.getInstance().updateBucketSize(updatingEntity.getBucket(), entity.getSize());
                        }
                    } else {
                        throw new IllegalResourceStateException("Cannot transition to extant from non-creating state", null, ObjectState.creating.toString(), entity.getState().toString());
                    }
                    return updatingEntity;
                } catch(NoSuchElementException e) {
                    throw new NoSuchEntityException(entity.getPartUuid());
                } catch(ObjectStorageInternalException e) {
                    throw e;
                } catch(Exception e) {
                    LOG.warn("Cannot update state of object " + entity.getPartUuid(), e);
                    throw new MetadataOperationFailureException(e);
                }
            }



            /*
            if(entity == null) {
                throw new RuntimeException("Null bucket record cannot be updated");
            } else {
                try {
                    PartEntity updatingEntity = Entities.uniqueResult(new PartEntity().withUuid(entity.getPartUuid()));
                    if(!ObjectState.deleting.equals(entity.getState())) {
                        updatingEntity.setState(ObjectState.extant);
                        updatingEntity.setCreationExpiration(null);
                        updatingEntity.setObjectModifiedTimestamp(entity.getObjectModifiedTimestamp());
                        updatingEntity.setIsLatest(true);
                        updatingEntity.seteTag(entity.geteTag());
                        updatingEntity.setSize(entity.getSize());
                        BucketMetadataManagers.getInstance().updateBucketSize(updatingEntity.getBucket(), entity.getSize());
                    } else {
                        throw new IllegalResourceStateException("Cannot transition to extant from non-creating state", null, ObjectState.creating.toString(), entity.getState().toString());
                    }
                    return updatingEntity;
                } catch(NoSuchElementException e) {
                    throw new NoSuchEntityException(entity.getPartUuid());
                } catch(ObjectStorageInternalException e) {
                    throw e;
                } catch(Exception e) {
                    LOG.warn("Cannot update state of object " + entity.getPartUuid(), e);
                    throw new MetadataOperationFailureException(e);
                }
            }
            */
        }
    };
  
    /**
     * Function that does the actual update of state. Will transition from any state -> deleting
     */
    static final Function<PartEntity, PartEntity> TRANSITION_TO_DELETING = new Function<PartEntity, PartEntity> () {

        @Nullable
        @Override
        public PartEntity apply(@Nullable PartEntity objectToUpdate) {
            if(objectToUpdate == null) {
                throw new RuntimeException("Null bucket record cannot be updated");
            } else {
                try {
                    PartEntity entity;
                    //Shortcut to avoid a lookup if we're already loaded in a transaction
                    //This is most common case for deletion operations
                    if(!Entities.isPersistent(objectToUpdate)) {
                        entity = Entities.uniqueResult(new PartEntity().withUuid(objectToUpdate.getPartUuid()));
                    } else {
                        entity = objectToUpdate;
                    }

                    entity.setState(ObjectState.deleting);
                    //Only decrement bucket size if object was extant
                    if(ObjectState.extant.equals(entity.getLastState())) {
                        BucketMetadataManagers.getInstance().updateBucketSize(entity.getBucket(), -entity.getSize());
                    }
                    return entity;
                } catch(NoSuchElementException e) {
                    throw new NoSuchEntityException(objectToUpdate.getPartUuid());
                } catch(Exception e) {
                    throw new MetadataOperationFailureException(e);
                }
            }
        }
    };

}
