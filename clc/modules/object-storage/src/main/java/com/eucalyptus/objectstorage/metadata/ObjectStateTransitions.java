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

import java.util.NoSuchElementException;

import javax.annotation.Nullable;

import org.apache.log4j.Logger;

import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionException;
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

/**
 * Created by zhill on 2/14/14.
 */
public class ObjectStateTransitions {
  private static final Logger LOG = Logger.getLogger(ObjectStateTransitions.class);

  /**
   * Inserts the new record into the db in 'creating' state.
   */
  static final Function<ObjectEntity, ObjectEntity> TRANSITION_TO_CREATING = new Function<ObjectEntity, ObjectEntity>() {

    @Nullable
    @Override
    public ObjectEntity apply(@Nullable ObjectEntity initializedObject) {
      if (initializedObject == null) {
        throw new RuntimeException("Null bucket record cannot be updated");
      } else {
        Bucket extantBucket;
        try {
          extantBucket = Entities.uniqueResult(new Bucket().withUuid(initializedObject.getBucket().getBucketUuid()));
        } catch (NoSuchElementException e) {
          throw new NoSuchEntityException(e);
        } catch (TransactionException e) {
          throw new MetadataOperationFailureException(e);
        }

        if (extantBucket == null) {
          throw new NoSuchEntityException(initializedObject.getBucket().getBucketUuid());
        }
        if (initializedObject.getState() != null && !ObjectState.creating.equals(initializedObject.getState())) {
          throw new IllegalResourceStateException(initializedObject.getResourceFullName(), null, ObjectState.creating.toString(), initializedObject
              .getState().toString());
        } else {
          if (extantBucket == null || !BucketState.extant.equals(extantBucket.getState())) {
            // invalid bucket, cannot create
            throw new IllegalResourceStateException(extantBucket.getBucketUuid(), null, BucketState.extant.toString(), extantBucket.getState()
                .toString());
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
  static final Function<ObjectEntity, ObjectEntity> TRANSITION_TO_EXTANT = new Function<ObjectEntity, ObjectEntity>() {

    @Nullable
    @Override
    public ObjectEntity apply(@Nullable ObjectEntity entity) {
      if (entity == null) {
        throw new RuntimeException("Null bucket record cannot be updated");
      } else {
        try {
          ObjectEntity updatingEntity = Entities.uniqueResult(new ObjectEntity().withUuid(entity.getObjectUuid()));
          if (!ObjectState.deleting.equals(entity.getState())) {
            // Remove old versions and update bucket size within this transaction.
            // If quota enforcement wasn't needed this would be unnecessary.
            // NOTE: this must be done before the new object is updated or it will clean it as well due to Hibernate context
            // and session. First remove nulls, then set state to extant.
            /*
             * ObjectMetadataManagers.getInstance().cleanupAllNullVersionedObjectRecords(entity.getBucket(), entity.getObjectKey());
             * 
             * if(ObjectState.mpu_pending.equals(entity.getState())) { MpuPartMetadataManagers.getInstance().removeParts(entity.getBucket(),
             * entity.getUploadId()); }
             */

            // Set the new object state
            updatingEntity.setState(ObjectState.extant);
            updatingEntity.setCreationExpiration(null);
            updatingEntity.setObjectModifiedTimestamp(entity.getObjectModifiedTimestamp());
            updatingEntity.markLatest();
            updatingEntity.seteTag(entity.geteTag());
            updatingEntity.setSize(entity.getSize());
            updatingEntity.setStoredHeaders(entity.getStoredHeaders());

            if (ObjectState.mpu_pending.equals(updatingEntity.getLastState())) {
              // Remove the parts, this will remove the sizes for the parts.
              MpuPartMetadataManagers.getInstance().removeParts(updatingEntity.getBucket(), updatingEntity.getUploadId());
            }

            ObjectMetadataManagers.getInstance().cleanupInvalidObjects(updatingEntity.getBucket(), updatingEntity.getObjectKey());

          } else {
            throw new IllegalResourceStateException("Cannot transition to extant from non-creating state", null, ObjectState.creating.toString(),
                entity.getState().toString());
          }
          return updatingEntity;
        } catch (NoSuchElementException e) {
          throw new NoSuchEntityException(entity.getObjectUuid());
        } catch (ObjectStorageInternalException e) {
          throw e;
        } catch (Exception e) {
          LOG.warn("Cannot update state of object " + entity.getObjectUuid(), e);
          throw new MetadataOperationFailureException(e);
        }
      }
    }
  };

  /**
   * Function that does the actual update of state. Will only transition creating->mpu_pending
   */
  static final Function<ObjectEntity, ObjectEntity> TRANSITION_TO_MPU_PENDING = new Function<ObjectEntity, ObjectEntity>() {

    @Nullable
    @Override
    public ObjectEntity apply(@Nullable ObjectEntity entity) {
      if (entity == null) {
        throw new RuntimeException("Null bucket record cannot be updated");
      } else {
        try {
          ObjectEntity updatingEntity = Entities.uniqueResult(new ObjectEntity().withUuid(entity.getObjectUuid()));
          if (ObjectState.creating.equals(updatingEntity.getState())) {
            updatingEntity.setState(ObjectState.mpu_pending);
            updatingEntity.setCreationExpiration(null);
            updatingEntity.setObjectModifiedTimestamp(entity.getObjectModifiedTimestamp());
            updatingEntity.setUploadId(entity.getUploadId());
          } else {
            throw new IllegalResourceStateException("Cannot transition to mpu-pending from non-creating state", null,
                ObjectState.creating.toString(), entity.getState().toString());
          }
          return updatingEntity;
        } catch (ObjectStorageInternalException e) {
          throw e;
        } catch (Exception e) {
          LOG.warn("Cannot update state of object " + entity.getObjectUuid(), e);
          throw new MetadataOperationFailureException(e);
        }
      }
    }
  };
  /**
   * Function that does the actual update of state. Will transition from any state -> deleting
   */
  static final Function<ObjectEntity, ObjectEntity> TRANSITION_TO_DELETING = new Function<ObjectEntity, ObjectEntity>() {

    @Nullable
    @Override
    public ObjectEntity apply(@Nullable ObjectEntity objectToUpdate) {
      if (objectToUpdate == null) {
        throw new RuntimeException("Null bucket record cannot be updated");
      } else {
        try {
          ObjectEntity entity;
          // Shortcut to avoid a lookup if we're already loaded in a transaction
          // This is most common case for deletion operations
          if (!Entities.isPersistent(objectToUpdate)) {
            entity = Entities.uniqueResult(new ObjectEntity().withUuid(objectToUpdate.getObjectUuid()));
          } else {
            entity = objectToUpdate;
          }
          entity.setState(ObjectState.deleting);
          entity.setIsLatest(Boolean.FALSE);

          return entity;
        } catch (NoSuchElementException e) {
          throw new NoSuchEntityException(objectToUpdate.getObjectUuid());
        } catch (Exception e) {
          throw new MetadataOperationFailureException(e);
        }
      }
    }
  };
  /**
   * Does the actual deletion of the entity
   */
  static final Predicate<ObjectEntity> TRANSITION_TO_DELETED = new Predicate<ObjectEntity>() {

    @Nullable
    @Override
    public boolean apply(@Nullable ObjectEntity objectToUpdate) {
      if (objectToUpdate == null) {
        throw new RuntimeException("Null bucket record cannot be updated");
      } else {
        try {
          ObjectEntity entity;
          // Shortcut to avoid a lookup if we're already loaded in a transaction
          // This is most common case for deletion operations
          if (!Entities.isPersistent(objectToUpdate)) {
            entity = Entities.uniqueResult(new ObjectEntity().withUuid(objectToUpdate.getObjectUuid()));
          } else {
            entity = objectToUpdate;
          }
          if (!ObjectState.deleting.equals(objectToUpdate.getState())) {
            throw new IllegalResourceStateException("Entity not in deleting state. Only valid transition to deleted is from deleting.", null,
                ObjectState.deleting.toString(), objectToUpdate.getState().toString());
          }
          Entities.delete(entity);
          return true;
        } catch (ObjectStorageInternalException e) {
          throw e;
        } catch (NoSuchElementException e) {
          throw new NoSuchEntityException(objectToUpdate.getObjectUuid());
        } catch (Exception e) {
          throw new MetadataOperationFailureException(e);
        }
      }
    }
  };
}
