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
import com.eucalyptus.objectstorage.ObjectState;
import com.eucalyptus.objectstorage.entities.Bucket;
import com.eucalyptus.objectstorage.entities.PartEntity;
import com.eucalyptus.objectstorage.exceptions.IllegalResourceStateException;
import com.eucalyptus.objectstorage.exceptions.MetadataOperationFailureException;
import com.eucalyptus.objectstorage.exceptions.NoSuchEntityException;
import com.eucalyptus.objectstorage.exceptions.ObjectStorageInternalException;
import com.google.common.base.Function;

/**
 * Created by zhill on 2/14/14.
 */
public class MpuPartStateTransitions {
  private static final Logger LOG = Logger.getLogger(MpuPartStateTransitions.class);

  /**
   * Inserts the new record into the db in 'creating' state.
   */
  static final Function<PartEntity, PartEntity> TRANSITION_TO_CREATING = new Function<PartEntity, PartEntity>() {

    @Nullable
    @Override
    public PartEntity apply(@Nullable PartEntity initializedObject) {
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
   * Function that does the actual update of state. Will only transition creating->extant
   */
  static final Function<PartEntity, PartEntity> TRANSITION_TO_EXTANT = new Function<PartEntity, PartEntity>() {

    @Nullable
    @Override
    public PartEntity apply(@Nullable PartEntity entity) {
      if (entity == null) {
        throw new RuntimeException("Null bucket record cannot be updated");
      } else {
        try {
          PartEntity updatingEntity = Entities.uniqueResult(new PartEntity().withUuid(entity.getPartUuid()));
          if (!ObjectState.deleting.equals(entity.getState())) {
            // Set the new part state
            updatingEntity.setState(ObjectState.extant);
            updatingEntity.setCreationExpiration(null);
            updatingEntity.setObjectModifiedTimestamp(entity.getObjectModifiedTimestamp());
            updatingEntity.setIsLatest(true);
            updatingEntity.seteTag(entity.geteTag());
            updatingEntity.setSize(entity.getSize());

            // Remove old versions and update bucket size within this transaction.
            MpuPartMetadataManagers.getInstance().cleanupInvalidParts(entity.getBucket(), entity.getObjectKey(), entity.getUploadId(),
                entity.getPartNumber());
          } else {
            throw new IllegalResourceStateException("Cannot transition to extant from non-creating state", null, ObjectState.creating.toString(),
                entity.getState().toString());
          }
          return updatingEntity;
        } catch (NoSuchElementException e) {
          throw new NoSuchEntityException(entity.getPartUuid());
        } catch (ObjectStorageInternalException e) {
          throw e;
        } catch (Exception e) {
          LOG.warn("Cannot update state of object " + entity.getPartUuid(), e);
          throw new MetadataOperationFailureException(e);
        }
      }
    }
  };

  /**
   * Function that does the actual update of state. Will transition from any state -> deleting
   */
  static final Function<PartEntity, PartEntity> TRANSITION_TO_DELETING = new Function<PartEntity, PartEntity>() {

    @Nullable
    @Override
    public PartEntity apply(@Nullable PartEntity objectToUpdate) {
      if (objectToUpdate == null) {
        throw new RuntimeException("Null bucket record cannot be updated");
      } else {
        try {
          PartEntity entity;
          // Shortcut to avoid a lookup if we're already loaded in a transaction
          // This is most common case for deletion operations
          if (!Entities.isPersistent(objectToUpdate)) {
            entity = Entities.uniqueResult(new PartEntity().withUuid(objectToUpdate.getPartUuid()));
          } else {
            entity = objectToUpdate;
          }

          entity.setState(ObjectState.deleting);
          return entity;
        } catch (NoSuchElementException e) {
          throw new NoSuchEntityException(objectToUpdate.getPartUuid());
        } catch (Exception e) {
          throw new MetadataOperationFailureException(e);
        }
      }
    }
  };

}
