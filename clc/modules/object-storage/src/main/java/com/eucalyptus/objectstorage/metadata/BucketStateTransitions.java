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

import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionException;
import com.eucalyptus.objectstorage.BucketState;
import com.eucalyptus.objectstorage.ObjectMetadataManagers;
import com.eucalyptus.objectstorage.entities.Bucket;
import com.eucalyptus.objectstorage.exceptions.IllegalResourceStateException;
import com.eucalyptus.objectstorage.exceptions.MetadataOperationFailureException;
import com.eucalyptus.objectstorage.exceptions.NoSuchEntityException;
import com.google.common.base.Function;

/**
 * Created by zhill on 2/14/14.
 */
public class BucketStateTransitions {
  /**
   * Inserts the new record into the db in 'creating' state. Will not be allowed if any other records for the same bucket-name are in creating or
   * extant state.
   */
  static final Function<Bucket, Bucket> TRANSITION_TO_CREATING = new Function<Bucket, Bucket>() {

    @Nullable
    @Override
    public Bucket apply(@Nullable Bucket initializedBucket) {
      if (initializedBucket == null) {
        throw new RuntimeException("Null bucket record cannot be updated");
      } else {
        if (initializedBucket.getState() == null || BucketState.creating.equals(initializedBucket.getState())) {
          initializedBucket.setState(BucketState.creating);
          return Entities.persist(initializedBucket);
        } else {
          throw new IllegalResourceStateException(initializedBucket.getBucketName(), null, BucketState.creating.toString(), initializedBucket
              .getState().toString());
        }
      }
    }
  };
  /**
   * Function that does the actual update of state. Will only transition creating->extant
   */
  static final Function<Bucket, Bucket> TRANSITION_TO_EXTANT = new Function<Bucket, Bucket>() {

    @Nullable
    @Override
    public Bucket apply(@Nullable Bucket searchBucket) {
      if (searchBucket == null) {
        throw new RuntimeException("Null bucket record cannot be updated");
      } else {
        try {
          Bucket foundBucket = Entities.uniqueResult(new Bucket().withUuid(searchBucket.getBucketUuid()));
          if (!BucketState.deleting.equals(foundBucket.getState())) {
            foundBucket.setState(BucketState.extant);
            return foundBucket;
          } else {
            throw new IllegalResourceStateException("Cannot transition to extant from non-creating state", null, BucketState.creating.toString(),
                foundBucket.getState().toString());
          }
        } catch (NoSuchElementException e) {
          throw new NoSuchEntityException(searchBucket.getBucketUuid());
        } catch (TransactionException e) {
          throw new MetadataOperationFailureException(e);
        }
      }
    }
  };
  /**
   * Function that does the actual update of state. Will transition from any state -> deleting
   */
  static final Function<Bucket, Bucket> TRANSITION_TO_DELETING = new Function<Bucket, Bucket>() {

    @Nullable
    @Override
    public Bucket apply(@Nullable Bucket searchBucket) {
      if (searchBucket == null) {
        throw new RuntimeException("Null bucket record cannot be updated");
      } else {
        try {
          Bucket foundBucket = Entities.uniqueResult(new Bucket().withUuid(searchBucket.getBucketUuid()));
          // Must check for emptiness within this transaction
          if (ObjectMetadataManagers.getInstance().countValid(foundBucket) > 0) {
            throw new Exception("Bucket not empty");
          }
          foundBucket.setState(BucketState.deleting);
          foundBucket.setBucketName(null);
          return foundBucket;
        } catch (NoSuchElementException e) {
          throw new NoSuchEntityException(searchBucket.getBucketUuid());
        } catch (Exception e) {
          throw new MetadataOperationFailureException(e);
        }
      }
    }

  };
}
