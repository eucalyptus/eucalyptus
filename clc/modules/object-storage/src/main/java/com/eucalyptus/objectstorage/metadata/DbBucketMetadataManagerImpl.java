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

package com.eucalyptus.objectstorage.metadata;

import java.util.HashMap;
import java.util.List;
import java.util.NoSuchElementException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.persistence.EntityTransaction;

import com.eucalyptus.objectstorage.BucketState;
import com.eucalyptus.objectstorage.exceptions.MetadataOperationFailureException;
import com.eucalyptus.objectstorage.exceptions.NoSuchEntityException;
import org.apache.log4j.Logger;
import org.hibernate.Criteria;

import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionException;
import com.eucalyptus.entities.TransactionResource;
import com.eucalyptus.entities.Transactions;
import com.eucalyptus.objectstorage.entities.Bucket;
import com.eucalyptus.objectstorage.exceptions.IllegalResourceStateException;
import com.eucalyptus.objectstorage.exceptions.s3.NoSuchBucketException;
import com.eucalyptus.objectstorage.exceptions.s3.S3Exception;
import com.eucalyptus.objectstorage.util.ObjectStorageProperties.VersioningStatus;
import com.eucalyptus.storage.msgs.s3.AccessControlPolicy;
import com.google.common.base.Function;
import com.google.common.base.Objects;
import org.hibernate.PersistentObjectException;
import org.hibernate.criterion.Example;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.exception.ConstraintViolationException;

/**
 * Three types of failures on a metadata operation:
 * IllegalResourceState - entity is not in a state where the update is a valid change (e.g. deleting->creating)
 * EntityNotFound - the entity is no longer found on the backend (e.g. deleted while other update pending)
 * MetadataOperationFailureException - the operation could not complete, db failure, etc
 *
 */

public class DbBucketMetadataManagerImpl implements BucketMetadataManager {
	private static final Logger LOG = Logger.getLogger(DbBucketMetadataManagerImpl.class);

    public void start() throws Exception {}
	public void stop() throws Exception {}


    @Override
    public Bucket persistBucketInCreatingState(@Nonnull String bucketName,
                                               @Nonnull AccessControlPolicy acp,
                                               @Nullable String iamUserId,
                                               @Nullable String location) throws IllegalResourceStateException, MetadataOperationFailureException, NoSuchEntityException {
        Bucket initialized;
        try {
            initialized = Bucket.getInitializedBucket(bucketName, iamUserId, acp, location);
        } catch(Exception e) {
            throw new MetadataOperationFailureException(e);
        }

        return transitionBucketToState(initialized, BucketState.creating);
    }

    @Override
    public Bucket lookupExtantBucket(@Nonnull String bucketName) throws NoSuchEntityException, MetadataOperationFailureException {
        try {
            Bucket searchExample = new Bucket(bucketName).withState(BucketState.extant);
            return Transactions.find(searchExample);
        } catch (NoSuchElementException e) {
            throw new NoSuchEntityException(bucketName);
        } catch (Exception e) {
            LOG.warn("Error querying bucket existence in db", e);
            throw new MetadataOperationFailureException(e);
        }
    }
		
	@Override
	public Bucket lookupBucket(@Nonnull String bucketName) throws NoSuchEntityException, MetadataOperationFailureException {
		try {
			Bucket searchExample = new Bucket(bucketName);
			return Transactions.find(searchExample);
		} catch (NoSuchElementException e) {
			throw new NoSuchEntityException(bucketName);
		} catch (Exception e) {
			LOG.warn("Error querying bucket existence in db",e);
			throw new MetadataOperationFailureException(e);
		}		
	}

    @Override
    public Bucket lookupBucketByUuid(@Nonnull String bucketUuid) throws NoSuchEntityException, MetadataOperationFailureException {
        try {
            Bucket searchExample = new Bucket().withUuid(bucketUuid);
            return Transactions.find(searchExample);
        } catch (NoSuchElementException e) {
            throw new NoSuchEntityException(bucketUuid);
        } catch (Exception e) {
            LOG.warn("Error querying bucket existence in db", e);
            throw new MetadataOperationFailureException(e);
        }
    }

    @Override
    public List<Bucket> getBucketsForDeletion() throws Exception {
        try {
            //Rely on uniqueness contraint that only one record can be in
            // either creating or extant state with that bucket name
            Bucket searchBucket = new Bucket().withState(BucketState.deleting);
            return Transactions.findAll(searchBucket);
        } catch (NoSuchElementException e) {
            throw e;
        } catch (Exception e) {
            LOG.error("Error querying bucket existence in db",e);
            throw e;
        }
    }

    @Override
    public Bucket transitionBucketToState(@Nonnull final Bucket bucket, @Nonnull BucketState destState) throws NoSuchEntityException, IllegalResourceStateException, MetadataOperationFailureException {
        Function<Bucket, Bucket> transitionFunction = null;

        switch(destState) {
        case creating:
            transitionFunction = BucketStateTransitions.TRANSITION_TO_CREATING;
            break;
        case extant:
            transitionFunction = BucketStateTransitions.TRANSITION_TO_EXTANT;
            break;
        case deleting:
            transitionFunction = BucketStateTransitions.TRANSITION_TO_DELETING;
            break;
        default:
            LOG.error("Unexpected destination state: " + destState);
            throw new IllegalArgumentException();
        }

        try {
            return Entities.asTransaction(Bucket.class, transitionFunction).apply(bucket);
        } catch(IllegalResourceStateException e) {
            throw e;
        } catch(ConstraintViolationException e) {
            IllegalResourceStateException ex = new IllegalResourceStateException();
            ex.initCause(e);
            throw ex;
        } catch(PersistentObjectException e) {
            //Object passed for merge is not found on the db.
            throw new NoSuchEntityException("Bucket entity not found for merge", e);
        } catch(Exception e) {
            throw new MetadataOperationFailureException(e);
        }
    }
	
	@Override
	public void deleteBucketMetadata(@Nonnull final Bucket bucket) throws Exception {
		try(TransactionResource trans = Entities.transactionFor(Bucket.class)) {
			Bucket bucketToDelete = Entities.uniqueResult(bucket);
			if(BucketState.deleting.equals(bucketToDelete.getState())) {
                //Remove the record.
				Entities.delete(bucketToDelete);
			} else {
				throw new IllegalResourceStateException("Bucket not in deleting state, no valid transition to deleted", null, BucketState.deleting.toString(), bucketToDelete.getState().toString());
			}
			trans.commit();
		} catch(NoSuchElementException e) {
			//Ok, continue.
			LOG.trace("Bucket deletion finalization for (bucket uuid) " + bucket.getBucketUuid() + " failed to find entity record. Returning normally");
		}	
	}

	@Override
	public List<Bucket> lookupBucketsByOwner(String ownerCanonicalId) throws MetadataOperationFailureException {
		Bucket searchBucket = new Bucket().withState(BucketState.extant);
		searchBucket.setOwnerCanonicalId(ownerCanonicalId);
		List<Bucket> buckets = null;
		try (TransactionResource trans = Entities.transactionFor(Bucket.class)){
            Criteria searchCriteria = Entities.createCriteria(Bucket.class);
            Example example = Example.create(searchBucket);
            searchCriteria.add(example);
            searchCriteria.addOrder(Order.asc("bucketName"));
            searchCriteria.setReadOnly(true);
            buckets = searchCriteria.list();
            trans.commit();
            return buckets;
		} catch (Exception e) {
			LOG.error("Error listing buckets for user " + ownerCanonicalId + " due to DB transaction error", e);
			throw new MetadataOperationFailureException(e);
		}
	}

    @Override
    public List<Bucket> lookupBucketsByState(BucketState state) throws TransactionException {
        Bucket searchBucket = new Bucket().withState(state);
        List<Bucket> buckets = null;
        try (TransactionResource trans = Entities.transactionFor(Bucket.class)){
            Criteria searchCriteria = Entities.createCriteria(Bucket.class);
            Example example = Example.create(searchBucket);
            searchCriteria.add(example);
            searchCriteria.addOrder(Order.asc("bucketName"));
            searchCriteria.setReadOnly(true);
            buckets = searchCriteria.list();
            trans.commit();
            return buckets;
        } catch (Exception e) {
            LOG.error("Error listing buckets in the state: " + state + " due to DB transaction error", e);
            throw e;
        }
    }
	
	@Override
	public List<Bucket> lookupBucketsByUser(String userIamId) throws MetadataOperationFailureException {
		Bucket searchBucket = new Bucket().withState(BucketState.extant);
		searchBucket.setOwnerIamUserId(userIamId);
		List<Bucket> buckets = null;
		try {
			buckets = Transactions.findAll(searchBucket);
			return buckets;
		} catch (TransactionException e) {
			LOG.error("Error listing buckets for user " + userIamId + " due to DB transaction error", e);
			throw new MetadataOperationFailureException(e);
		}
	}
	
	@Override
	public long countBucketsByUser(String userIamId) throws MetadataOperationFailureException {
		Bucket searchBucket = new Bucket();
		searchBucket.setOwnerIamUserId(userIamId);
        try (TransactionResource db = Entities.transactionFor(Bucket.class)) {
            return Entities.count(searchBucket, Restrictions.ne("state", BucketState.deleting), new HashMap<String,String>());
        } catch (Exception e) {
            LOG.warn("Error counting buckets for user " + userIamId + " due to DB transaction error", e);
            throw new MetadataOperationFailureException(e);
        }
	}

	@Override
	public long countBucketsByAccount(String canonicalId) throws MetadataOperationFailureException {
		Bucket searchBucket = new Bucket();
		searchBucket.setOwnerCanonicalId(canonicalId);
		try (TransactionResource db = Entities.transactionFor(Bucket.class)){
			return Entities.count(searchBucket, Restrictions.ne("state", BucketState.deleting), new HashMap<String,String>());
        } catch (Exception e) {
			LOG.warn("Error counting buckets for account canonicalId " + canonicalId + " due to DB transaction error", e);
			throw new MetadataOperationFailureException(e);
		}
	}
	
	@Override
	public Bucket setAcp(@Nonnull Bucket bucketEntity, @Nonnull String acl)  throws MetadataOperationFailureException, NoSuchEntityException {
		try (TransactionResource trans = Entities.transactionFor(Bucket.class)) {
            Bucket bucket = Entities.merge(bucketEntity);
			bucket.setAcl(acl);
			trans.commit();
			return bucket;
		} catch(NoSuchElementException e) {
			throw new NoSuchEntityException(bucketEntity.getBucketName());
		} catch(Exception e) {
			LOG.error("Error updating acl for bucket " + bucketEntity.getBucketName(), e);
			throw new MetadataOperationFailureException(e);
		}
	}

    @Override
    public Bucket setAcp(@Nonnull Bucket bucketEntity, @Nonnull AccessControlPolicy acp)throws MetadataOperationFailureException, NoSuchEntityException {
        try (TransactionResource trans = Entities.transactionFor(Bucket.class)) {
            Bucket bucket = Entities.merge(bucketEntity);
            bucket.setAcl(acp);
            trans.commit();
            return bucket;
        } catch(NoSuchElementException e) {
            throw new NoSuchEntityException(bucketEntity.getBucketName());
        } catch(Exception e) {
            LOG.error("Error updating acl for bucket " + bucketEntity.getBucketName(), e);
            throw new MetadataOperationFailureException(e);
        }
    }
	
	@Override
	public Bucket setLoggingStatus(@Nonnull Bucket bucketEntity,
			@Nonnull Boolean loggingEnabled, 
			@Nullable String destBucket, 
			@Nullable String destPrefix) throws TransactionException, S3Exception {
		EntityTransaction db = Entities.get(Bucket.class);
		try {
			Bucket bucket = Entities.uniqueResult(bucketEntity);
			bucket.setLoggingEnabled(loggingEnabled);
			bucket.setTargetBucket(destBucket);
			bucket.setTargetPrefix(destPrefix);
			db.commit();
			return bucket;
		} catch(NoSuchElementException e) {
			throw new NoSuchBucketException(bucketEntity.getBucketName());
		} catch(TransactionException e) {
			LOG.error("Transaction error updating acl for bucket " + bucketEntity.getBucketName(),e);
			throw e;
		} finally {
			if(db != null && db.isActive()) {
				db.rollback();
			}
		}
	}
	
	@Override
	public Bucket setVersioning(@Nonnull Bucket bucketEntity, 
			@Nonnull VersioningStatus newState) throws IllegalResourceStateException, MetadataOperationFailureException, NoSuchEntityException {

		try (TransactionResource trans = Entities.transactionFor(Bucket.class)) {
			Bucket bucket = Entities.uniqueResult(new Bucket().withUuid(bucketEntity.getBucketUuid()));

            if(VersioningStatus.Disabled.equals(newState)) {
                //The user cannot ever set 'Disabled'.
                throw new IllegalResourceStateException("Invalid versioning state transition");
            }
            bucket.setVersioning(newState);
			trans.commit();
			return bucket;
		} catch(NoSuchElementException e) {
			throw new NoSuchEntityException(bucketEntity.getBucketName());
		} catch(TransactionException e) {
			LOG.error("Transaction error updating versioning state for bucket " + bucketEntity.getBucketName(),e);
			throw new MetadataOperationFailureException(e);
		}
	}
	
	@Override
	public long totalSizeOfAllBuckets() throws MetadataOperationFailureException {
		long size = -1;
	    try (TransactionResource db = Entities.transactionFor(Bucket.class)){
	    	size = Objects.firstNonNull( (Number) Entities.createCriteria( Bucket.class )
                    .setProjection(Projections.sum("bucketSize"))
	    			.setReadOnly( true )
	    			.uniqueResult(), 0 ).longValue();
	    	db.commit();
	    } catch (Exception e) {
	    	LOG.warn("Error getting buckets cumulative size",e);
            throw new MetadataOperationFailureException(e);
	    }
	    return size;
	}

	@Override
	public Bucket updateBucketSize(final Bucket bucket, final long sizeToChange) throws TransactionException {
		Function<Bucket, Bucket> incrementSize = new Function<Bucket, Bucket>() {
			@Override
			public Bucket apply(Bucket updateBucket) {
                try {
                    Bucket b = Entities.uniqueResult(new Bucket().withUuid(updateBucket.getBucketUuid()));
                    b.setBucketSize(b.getBucketSize() + sizeToChange);
                    return b;
                } catch(TransactionException e) {
                    throw new RuntimeException(e);
                }
			}
			
		};

		return Entities.asTransaction(incrementSize).apply(bucket);
	}
}
