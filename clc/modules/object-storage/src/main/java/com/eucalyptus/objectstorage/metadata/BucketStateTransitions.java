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
import com.eucalyptus.objectstorage.BucketState;
import com.eucalyptus.objectstorage.entities.Bucket;
import com.eucalyptus.objectstorage.exceptions.IllegalResourceStateException;
import com.eucalyptus.objectstorage.exceptions.MetadataOperationFailureException;
import com.eucalyptus.objectstorage.exceptions.NoSuchEntityException;
import com.google.common.base.Function;

import javax.annotation.Nullable;
import java.util.NoSuchElementException;

/**
 * Created by zhill on 2/14/14.
 */
public class BucketStateTransitions {
    /**
     * Inserts the new record into the db in 'creating' state. Will not be allowed
     * if any other records for the same bucket-name are in creating or extant state.
     */
    static final Function<Bucket, Bucket> TRANSITION_TO_CREATING = new Function<Bucket, Bucket> () {

        @Nullable
        @Override
        public Bucket apply(@Nullable Bucket initializedBucket) {
            if(initializedBucket == null) {
                throw new RuntimeException("Null bucket record cannot be updated");
            } else {
                if(initializedBucket.getState() == null || BucketState.creating.equals(initializedBucket.getState())) {
                    initializedBucket.setState(BucketState.creating);
                    return Entities.persist(initializedBucket);
                } else {
                    throw new IllegalResourceStateException(initializedBucket.getBucketName(), null, BucketState.creating.toString(), initializedBucket.getState().toString());
                }
            }
        }
    };
    /**
     * Function that does the actual update of state. Will only transition creating->extant
     */
    static final Function<Bucket, Bucket> TRANSITION_TO_EXTANT = new Function<Bucket, Bucket> () {

        @Nullable
        @Override
        public Bucket apply(@Nullable Bucket searchBucket) {
            if(searchBucket == null) {
                throw new RuntimeException("Null bucket record cannot be updated");
            } else {
                try {
                    Bucket foundBucket = Entities.uniqueResult(new Bucket().withUuid(searchBucket.getBucketUuid()));
                    if(!BucketState.deleting.equals(foundBucket.getState())) {
                        foundBucket.setState(BucketState.extant);
                        return foundBucket;
                    } else {
                        throw new IllegalResourceStateException("Cannot transition to extant from non-creating state", null, BucketState.creating.toString(), foundBucket.getState().toString());
                    }
                } catch(NoSuchElementException e) {
                    throw new NoSuchEntityException(searchBucket.getBucketUuid());
                } catch(TransactionException e) {
                    throw new MetadataOperationFailureException(e);
                }
            }
        }
    };
    /**
     * Function that does the actual update of state. Will transition from any state -> deleting
     */
    static final Function<Bucket, Bucket> TRANSITION_TO_DELETING = new Function<Bucket, Bucket> () {

        @Nullable
        @Override
        public Bucket apply(@Nullable Bucket searchBucket) {
            if(searchBucket == null) {
                throw new RuntimeException("Null bucket record cannot be updated");
            } else {
                try {
                    Bucket foundBucket = Entities.uniqueResult(new Bucket().withUuid(searchBucket.getBucketUuid()));
                    foundBucket.setState(BucketState.deleting);
                    foundBucket.setBucketName(null);
                    return foundBucket;
                } catch(NoSuchElementException e) {
                    throw new NoSuchEntityException(searchBucket.getBucketUuid());
                } catch(TransactionException e) {
                    throw new MetadataOperationFailureException(e);
                }
            }
        }

    };
}
