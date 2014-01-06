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

import java.util.Date;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.persistence.EntityTransaction;

import org.apache.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.criterion.Example;
import org.hibernate.criterion.Projections;

import com.eucalyptus.auth.principal.User;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.EntityWrapper;
import com.eucalyptus.entities.TransactionException;
import com.eucalyptus.entities.Transactions;
import com.eucalyptus.objectstorage.entities.Bucket;
import com.eucalyptus.objectstorage.entities.ObjectEntity;
import com.eucalyptus.objectstorage.exceptions.s3.BucketAlreadyExistsException;
import com.eucalyptus.objectstorage.exceptions.s3.BucketAlreadyOwnedByYouException;
import com.eucalyptus.objectstorage.exceptions.s3.InternalErrorException;
import com.eucalyptus.objectstorage.exceptions.s3.NoSuchBucketException;
import com.eucalyptus.objectstorage.exceptions.s3.S3Exception;
import com.eucalyptus.objectstorage.util.ObjectStorageProperties;
import com.eucalyptus.objectstorage.util.ObjectStorageProperties.VersioningStatus;
import com.google.common.base.Function;
import com.google.common.base.Objects;
import com.google.common.base.Predicate;

public class DbBucketManagerImpl implements BucketManager {
	private static final Logger LOG = Logger.getLogger(DbBucketManagerImpl.class);
	
    	public void start() throws Exception {}
    	public void stop() throws Exception {}
    	
    	/**
	 * Check that the bucket is a valid DNS name (or optionally can look like an IP)
	 */
    	@Override
	public boolean checkBucketName(String bucketName) throws Exception {		
		if(!bucketName.matches("^[A-Za-z0-9][A-Za-z0-9._-]+"))
			return false;
		if(bucketName.length() < 3 || bucketName.length() > 255)
			return false;
		String[] addrParts = bucketName.split("\\.");
		boolean ipFormat = true;
		if(addrParts.length == 4) {
			for(String addrPart : addrParts) {
				try {
					Integer.parseInt(addrPart);
				} catch(NumberFormatException ex) {
					ipFormat = false;
					break;
				}
			}
		} else {
			ipFormat = false;
		}		
		if(ipFormat)
			return false;
		return true;
	}
	
	/**
	 * Does the bucket contain snapshots...
	 * @param bucketName
	 * @return
	 * @throws Exception
	 */
	private boolean bucketHasSnapshots(String bucketName) throws Exception {
		EntityWrapper<ObjectEntity> dbSnap = null;

		try {
			dbSnap = EntityWrapper.get(ObjectEntity.class);
			ObjectEntity objInfo = new ObjectEntity();
			objInfo.setBucketName(bucketName);
			objInfo.setIsSnapshot(true);

			Criteria snapCount = dbSnap.createCriteria(ObjectEntity.class).add(Example.create(objInfo)).setProjection(Projections.rowCount());
			snapCount.setReadOnly(true);
			Long rowCount = (Long)snapCount.uniqueResult();
			dbSnap.rollback();
			if (rowCount != null && rowCount.longValue() > 0) {
				return true;
			}
			return false;
		} catch(Exception e) {
			if(dbSnap != null) {
				dbSnap.rollback();
			}
			throw e;
		}
	}
	
	@Override
	public Bucket get(@Nonnull String bucketName,
			@Nonnull boolean includeHidden,
			@Nullable CallableWithRollback<?,?> resourceModifier) throws Exception {
		try {
			Bucket searchExample = new Bucket(bucketName);
			if(!includeHidden) {				
				searchExample.setHidden(false);
			}
			return Transactions.find(searchExample);
		} catch (NoSuchElementException e) {
			throw e;
		} catch (Exception e) {
			LOG.error("Error querying bucket existence in db",e);
			throw e;
		}		
	}

	@Override
	public boolean exists(@Nonnull String bucketName,
			@Nullable CallableWithRollback<?,?> resourceModifier) throws Exception {
		try {
			return (Transactions.find(new Bucket(bucketName)) != null);
		} catch (NoSuchElementException e) {
			return false;		
		} catch (Exception e) {
			LOG.error("Error querying bucket existence in db",e);
			throw e;
		}
	}

	@Override
	public <T,R> T create(@Nonnull String bucketName, 
			@Nonnull User owner,
			@Nonnull String acl, 
			@Nonnull String location,
			@Nullable CallableWithRollback<T,R> resourceModifier) throws Exception, TransactionException {

		Bucket newBucket = new Bucket(bucketName);
		try {
			Bucket foundBucket = Transactions.find(newBucket);
			if(foundBucket != null) {
				if(foundBucket.getOwnerCanonicalId().equals(owner.getAccount().getCanonicalId())) {
					throw new BucketAlreadyOwnedByYouException(bucketName);
				} else {
					throw new BucketAlreadyExistsException(bucketName);
				}
			}
		} catch(NoSuchElementException e) {
			//Expected result, continue
		} catch(S3Exception e) {
			throw e;
		} catch(Exception e) {
			//Lookup failed.
			LOG.error("Lookup for bucket " + bucketName + " failed during creation checks. Cannot proceed.",e);
			throw new InternalErrorException(bucketName);
		}
		
		newBucket.setOwnerCanonicalId(owner.getAccount().getCanonicalId());
		newBucket.setOwnerDisplayName(owner.getAccount().getName());
		newBucket.setOwnerIamUserId(owner.getUserId());
		newBucket.setBucketSize(0L);
		newBucket.setHidden(false);
		newBucket.setAcl(acl);
		newBucket.setLocation(location);
		newBucket.setLoggingEnabled(false);		
		newBucket.setVersioning(ObjectStorageProperties.VersioningStatus.Disabled.toString());
		newBucket.setCreationDate(new Date());
		
		T result = null;
		try {
			if(resourceModifier != null) {
				result = resourceModifier.call();
			}
		} catch(Exception e) {
			LOG.error("Error creating bucket in backend",e);
			throw new InternalErrorException(bucketName);
		}
		
		try {
			Transactions.saveDirect(newBucket);			
		} catch(TransactionException ex) {
			//Rollback the bucket creation.
			LOG.error("Error persisting bucket record for bucket " + bucketName, ex);
			
			//Do backend cleanup here.
			if(resourceModifier != null) {
				try {
					R rollbackResult = resourceModifier.rollback(result);
				} catch(Exception e) {
					LOG.error("Backend rollback of operation failed",e);			
				}
			}
			throw ex;
		}
		
		return result;
	}
	
	@Override
	public <T> void delete(Bucket bucketEntity, 
			final CallableWithRollback<T,?> resourceModifier) throws Exception {
	
		//TODO: look at resolving state conflict where backend succeeds but db update fails
		Predicate<Bucket> deleteSync = new Predicate<Bucket>() {
			public boolean apply(Bucket bucket) {				
				if(resourceModifier != null) {					
					try {
						resourceModifier.call();
					} catch(final Throwable f) {
						LOG.error("Error in backend call for delete bucket: " + bucket.getBucketName(), f);
						return false;
					}
				}
				
				try {
					Transactions.delete(bucket);
				} catch(TransactionException e) {
					LOG.error("Error deleting bucket in DB",e);
					return false;
				} catch(NoSuchElementException e) {
					//Ok, continue.
				}
				return true;
			}
		};
		
		Entities.asTransaction(deleteSync).apply(bucketEntity);		
	}

	@Override
	public List<Bucket> list(String ownerCanonicalId, 
			boolean includeHidden, 
			CallableWithRollback<?,?> resourceModifier) throws TransactionException {
		Bucket searchBucket = new Bucket();
		searchBucket.setOwnerCanonicalId(ownerCanonicalId);
		if(includeHidden) {
			searchBucket.setHidden(null);
		} else {
			searchBucket.setHidden(false);
		}
		List<Bucket> buckets = null;
		try {
			buckets = Transactions.findAll(searchBucket);
			return buckets;
		} catch (TransactionException e) {
			LOG.error("Error listing buckets for user " + ownerCanonicalId + " due to DB transaction error", e);
			throw e;
		}
	}
	
	@Override
	public List<Bucket> listByUser(String userIamId, 
			boolean includeHidden, 
			CallableWithRollback<?,?> resourceModifier) throws TransactionException {
		Bucket searchBucket = new Bucket();
		searchBucket.setHidden(includeHidden);
		searchBucket.setOwnerIamUserId(userIamId);
		List<Bucket> buckets = null;
		try {
			buckets = Transactions.findAll(searchBucket);
			return buckets;
		} catch (TransactionException e) {
			LOG.error("Error listing buckets for user " + userIamId + " due to DB transaction error", e);
			throw e;
		}
	}
	
	@Override
	public long countByUser(String userIamId, 
			boolean includeHidden, 
			CallableWithRollback<?,?> resourceModifier) throws ExecutionException {
		Bucket searchBucket = new Bucket();
		searchBucket.setHidden(includeHidden);
		searchBucket.setOwnerIamUserId(userIamId);
		EntityTransaction db = Entities.get(Bucket.class);
		try {
			return Entities.count(searchBucket);
		} catch (Exception e) {
			LOG.error("Error counting buckets for user " + userIamId + " due to DB transaction error", e);
			throw new ExecutionException(e);
		} finally {
			db.rollback();
		}
	}

	@Override
	public long countByAccount(String canonicalId, 
			boolean includeHidden, 
			CallableWithRollback<?,?> resourceModifier) throws ExecutionException {
		Bucket searchBucket = new Bucket();
		searchBucket.setHidden(includeHidden);
		searchBucket.setOwnerCanonicalId(canonicalId);
		EntityTransaction db = Entities.get(Bucket.class);
		try {
			return Entities.count(searchBucket);
		} catch (Exception e) {
			LOG.error("Error counting buckets for account canonicalId " + canonicalId + " due to DB transaction error", e);
			throw new ExecutionException(e);
		} finally {
			db.rollback();
		}
	}
	
	@Override
	public <T> T setAcp(@Nonnull Bucket bucketEntity, 
			@Nonnull String acl, 
			@Nullable CallableWithRollback<T, ?> resourceModifier)  throws TransactionException, S3Exception {
		EntityTransaction db = Entities.get(Bucket.class);
		T result = null;
		try {
			Bucket bucket = Entities.uniqueResult(bucketEntity);
			bucket.setAcl(acl);
			
			if(resourceModifier != null) {				
				try {
					result = resourceModifier.call();
				} catch(Exception ex) {
					LOG.error("Resource modifier call (backend) for setAcl failed. Rolling back", ex);
					try {
						resourceModifier.rollback(result);
					} catch(Exception ex2) {
						LOG.error("Resource rollback failed on setAcl rollback.", ex2);
					}
				}				
			} else {
				result = null;
			}
			
			db.commit();
			return result;
		} catch(NoSuchElementException e) {
			throw new NoSuchBucketException(bucketEntity.getBucketName());
		} catch(TransactionException e) {
			LOG.error("Transaction error updating acl for bucket " + bucketEntity.getBucketName(),e);
			try {
				resourceModifier.rollback(result);
			} catch(Exception e2) {
				LOG.error("Rollback after transaction exception failed for resource modififer",e2);				
			}
			throw e;
		} finally {
			if(db != null && db.isActive()) {
				db.rollback();
			}
		}
	}
	
	@Override
	public <T> T setLoggingStatus(@Nonnull Bucket bucketEntity, 
			@Nonnull Boolean loggingEnabled, 
			@Nullable String destBucket, 
			@Nullable String destPrefix, 
			@Nullable CallableWithRollback<T, ?> resourceModifier) throws TransactionException, S3Exception {
		EntityTransaction db = Entities.get(Bucket.class);
		T result = null;
		try {
			Bucket bucket = Entities.uniqueResult(bucketEntity);
			bucket.setLoggingEnabled(loggingEnabled);
			bucket.setTargetBucket(destBucket);
			bucket.setTargetPrefix(destPrefix);
			
			if(resourceModifier != null) {				
				try {
					result = resourceModifier.call();
				} catch(Exception ex) {
					LOG.error("Resource modifier call (backend) for setLogging failed. Rolling back", ex);
					try {
						resourceModifier.rollback(result);
					} catch(Exception ex2) {
						LOG.error("Resource rollback failed on setLogging rollback.", ex2);
					}
				}				
			} else {
				result = null;
			}
			
			db.commit();
			return result;
		} catch(NoSuchElementException e) {
			throw new NoSuchBucketException(bucketEntity.getBucketName());
		} catch(TransactionException e) {
			LOG.error("Transaction error updating acl for bucket " + bucketEntity.getBucketName(),e);
			try {
				resourceModifier.rollback(result);
			} catch(Exception e2) {
				LOG.error("Rollback after transaction exception failed for resource modififer",e2);				
			}
			throw e;
		} finally {
			if(db != null && db.isActive()) {
				db.rollback();
			}
		}
	}
	
	@Override
	public <T> T setVersioning(@Nonnull Bucket bucketEntity, 
			@Nonnull VersioningStatus newState, 
			@Nullable CallableWithRollback<T, ?> resourceModifier) throws TransactionException, S3Exception {
		EntityTransaction db = Entities.get(Bucket.class);
		T result = null;
		try {
			Bucket bucket = Entities.uniqueResult(bucketEntity);
			bucket.setVersioning(newState.toString());
			
			if(resourceModifier != null) {
				try {
					result = resourceModifier.call();
				} catch(Exception ex) {
					LOG.error("Resource modifier call (backend) for setVersioning failed. Rolling back", ex);
					try {
						resourceModifier.rollback(result);
					} catch(Exception ex2) {
						LOG.error("Resource rollback failed on setVersioning rollback.", ex2);
					}
				}				
			} else {
				result = null;
			}
			
			db.commit();
			return result;
		} catch(NoSuchElementException e) {
			throw new NoSuchBucketException(bucketEntity.getBucketName());
		} catch(TransactionException e) {
			LOG.error("Transaction error updating versioning state for bucket " + bucketEntity.getBucketName(),e);
			try {
				resourceModifier.rollback(result);
			} catch(Exception e2) {
				LOG.error("Rollback after transaction exception failed for resource modififer",e2);				
			}
			throw e;
		} finally {
			if(db != null && db.isActive()) {
				db.rollback();
			}
		}
	}

	@Override
	public String getVersionId(Bucket bucketEntity) throws TransactionException,
			S3Exception {
		Bucket b = Transactions.find(bucketEntity);
		if(b.isVersioningEnabled()) {
			return UUID.randomUUID().toString().replaceAll("-", "");
		} else {
			return ObjectEntity.NULL_VERSION_STRING;
		}
	}
	
	@Override
	public long totalSizeOfAllBuckets() {
		long size = -1;
		final EntityTransaction db = Entities.get( Bucket.class );
	    try {
	    	size = Objects.firstNonNull( (Number) Entities.createCriteria( Bucket.class )
	    			.setProjection( Projections.sum( "bucketSize" ) )
	    			.setReadOnly( true )
	    			.uniqueResult(), 0 ).longValue();
	    	db.commit();
	    } catch (Exception e) {
	    	db.rollback();
	    }
	    return size;
	}

	@Override
	public void updateBucketSize(String bucketName, final long sizeToChange) throws TransactionException {
		Function<String, Bucket> incrementSize = new Function<String, Bucket>() {
			@Override
			public Bucket apply(String bucketName) {
				Bucket b;
				try {
					b = Entities.uniqueResult(new Bucket(bucketName));
					b.setBucketSize(b.getBucketSize() + sizeToChange);
					Entities.mergeDirect(b);
					return b;
				} catch (TransactionException | NoSuchElementException e) {
					LOG.error("Error updating bucket " + bucketName + " size by " + sizeToChange,e);
				}
				return null;
			}
			
		};
		
		Entities.asTransaction(incrementSize).apply(bucketName);
	}

	@Override
	public boolean isEmpty(Bucket bucket) throws Exception {
		return (ObjectManagers.getInstance().countValid(bucket) == 0);
	}
}
