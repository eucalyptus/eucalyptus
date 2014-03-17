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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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
import com.eucalyptus.entities.Transactions;
import com.eucalyptus.objectstorage.ObjectState;
import com.eucalyptus.objectstorage.PaginatedResult;
import com.eucalyptus.objectstorage.exceptions.IllegalResourceStateException;
import com.eucalyptus.objectstorage.exceptions.MetadataOperationFailureException;
import com.eucalyptus.objectstorage.exceptions.ObjectStorageInternalException;
import com.eucalyptus.objectstorage.exceptions.s3.NoSuchUploadException;
import com.google.common.base.Function;
import com.google.common.collect.Iterators;
import org.apache.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.criterion.Example;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import com.eucalyptus.auth.principal.User;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionException;
import com.eucalyptus.objectstorage.entities.Bucket;
import com.eucalyptus.objectstorage.entities.ObjectEntity;
import com.eucalyptus.objectstorage.exceptions.s3.InternalErrorException;
import com.eucalyptus.objectstorage.exceptions.s3.S3Exception;
import com.eucalyptus.objectstorage.util.ObjectStorageProperties;
import com.eucalyptus.storage.msgs.s3.AccessControlPolicy;
import com.google.common.base.Predicate;
import com.google.common.base.Strings;

/**
 * Database backed implementation of ObjectMetadataManager
 * 
 */
public class DbObjectMetadataManagerImpl implements ObjectMetadataManager {
	private static final Logger LOG = Logger.getLogger(DbObjectMetadataManagerImpl.class);

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
            objectToUpdate.setIsLatest(true);
            return this.transitionObjectToState(objectToUpdate, ObjectState.extant);
    }

    @Override
    public ObjectEntity finalizeMultipartInit(ObjectEntity objectToUpdate, Date updateTimestamp, String uploadId) throws MetadataOperationFailureException {
        objectToUpdate.setObjectModifiedTimestamp(updateTimestamp);
        objectToUpdate.setUploadId(uploadId);
        objectToUpdate.setIsLatest(false);
        return this.transitionObjectToState(objectToUpdate, ObjectState.mpu_pending);
    }

	@Override
	public List<ObjectEntity> lookupObjectsInState(Bucket bucket, String objectKey, String versionId, ObjectState state) throws Exception {
        try (TransactionResource db = Entities.transactionFor(ObjectEntity.class)) {
            Criteria search = Entities.createCriteria(ObjectEntity.class).add(Example.create(new ObjectEntity(bucket, objectKey, versionId).withState(state)));
            search.addOrder(Order.desc("objectModifiedTimestamp"));
            if(bucket != null) {
                search = getSearchByBucket(search, bucket);
            }
            List<ObjectEntity> results = search.list();
            db.commit();
            return results;
        } catch (NoSuchElementException e) {
            // Nothing, return empty list
            return new ArrayList<>(0);
        } catch (Exception e) {
            LOG.error("Error fetching pending write records for object " + bucket.getBucketName() + "/" + objectKey + "?versionId="
                    + versionId);
            throw e;
        }
	}

    /**
     * Provides the search criteria to handle the FK relation from ObjectEntity->Bucket
     * Returns a criteria for a search that matches the given bucket
     * @param baseCriteria
     * @param bucket
     * @return
     */
    protected static Criteria getSearchByBucket(@Nonnull Criteria baseCriteria, @Nullable Bucket bucket) {
        if(bucket != null) {
            return baseCriteria.createCriteria("bucket").add(Restrictions.eq("naturalId", bucket.getNaturalId()));
        } else {
            return baseCriteria;
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

    private static final Comparator timestampComparator = new Comparator<ObjectEntity>() {

        @Override
        public int compare(ObjectEntity objectEntity, ObjectEntity objectEntity2) {
            return objectEntity2.getObjectModifiedTimestamp().compareTo(objectEntity.getObjectModifiedTimestamp());
        }
    };

	@Override
	public void cleanupInvalidObjects(final Bucket bucket, final String objectKey) throws Exception {
		ObjectEntity searchExample = new ObjectEntity(bucket, objectKey, null);
		
		final Predicate<ObjectEntity> repairPredicate = new Predicate<ObjectEntity>() {
			public boolean apply(ObjectEntity example) {
                try {

                    //Find not-latest null-versioned objects and mark them for deletion.
                    ObjectEntity searchExample = new ObjectEntity().withKey(example.getObjectKey()).withBucket(example.getBucket()).withState(ObjectState.extant).withVersionId(ObjectStorageProperties.NULL_VERSION_ID);
                    Criteria searchCriteria = Entities.createCriteria(ObjectEntity.class);
                    searchCriteria.add(Example.create(searchExample)).addOrder(Order.desc("objectModifiedTimestamp"));
                    searchCriteria = getSearchByBucket(searchCriteria, example.getBucket());
                    List<ObjectEntity> results = searchCriteria.list();
                    if(results.size() <= 1) {
                        //nothing to do
                        return true;
                    }

                    results.get(0).setIsLatest(true);
                    // Set all but the first element as not latest
                    for (ObjectEntity obj : results.subList(1, results.size())) {
                        LOG.trace("Marking object " + obj.getObjectUuid() + " as no longer latest version");
                        obj.setIsLatest(false);
                        obj = transitionObjectToState(obj, ObjectState.deleting);
                    }
                } catch (NoSuchElementException e) {
                    // Nothing to do.
                } catch (Exception e) {
                    LOG.error("Error consolidationg Object records for " + example.getBucket().getBucketName() + "/"
                            + example.getObjectKey());
                    return false;
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

    @Override
    public void cleanupAllNullVersionedObjectRecords(final Bucket bucket, final String objectKey) throws Exception {
        ObjectEntity searchExample = new ObjectEntity(bucket, objectKey, null);

        final Predicate<ObjectEntity> repairPredicate = new Predicate<ObjectEntity>() {
            public boolean apply(ObjectEntity example) {
                try {
                    //Find all null-versioned objects and mark them for deletion.
                    ObjectEntity searchExample = new ObjectEntity().withKey(example.getObjectKey()).withBucket(example.getBucket()).withState(ObjectState.extant).withVersionId(ObjectStorageProperties.NULL_VERSION_ID);
                    Criteria searchCriteria = Entities.createCriteria(ObjectEntity.class);
                    searchCriteria.add(Example.create(searchExample));
                    searchCriteria = getSearchByBucket(searchCriteria, bucket);

                    // Set all but the first element as not latest
                    for (ObjectEntity obj : (List<ObjectEntity>)searchCriteria.list()) {
                        LOG.trace("Marking object " + obj.getObjectUuid() + " as no longer latest version");
                        obj.setIsLatest(false);
                        obj = transitionObjectToState(obj, ObjectState.deleting);
                    }
                } catch (NoSuchElementException e) {
                    // Nothing to do.
                } catch (Exception e) {
                    LOG.error("Error consolidationg Object records for " + example.getBucket().getBucketName() + "/"
                            + example.getObjectKey());
                    return false;
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
     * Returns the ObjectEntities that are in 'creating' for too long and
     * thus should be considered failed
     */
    @Override
    public List<ObjectEntity> lookupFailedObjects() throws MetadataOperationFailureException {
        // Return the latest version based on the created date.
        try (TransactionResource trans = Entities.transactionFor(ObjectEntity.class)){
            ObjectEntity searchExample = new ObjectEntity().withState(ObjectState.creating);
            Criteria search = Entities.createCriteria(ObjectEntity.class);
            List<ObjectEntity> results = search.add(Example.create(searchExample))
                    .add(Restrictions.lt("creationExpiration", System.currentTimeMillis())).list();
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
	public ObjectEntity lookupObject(Bucket bucket, String objectKey, String versionId) throws NoSuchElementException, MetadataOperationFailureException {
		try {
			// Return the latest version based on the created date.
			try(TransactionResource trans = Entities.transactionFor(ObjectEntity.class)) {
				ObjectEntity searchExample = new ObjectEntity().withBucket(bucket).withKey(objectKey).withState(ObjectState.extant);
				if (Strings.isNullOrEmpty(versionId)) {
					searchExample.setIsLatest(true);
				} else {
                    searchExample = searchExample.withVersionId(versionId);
                }

				Criteria search = Entities.createCriteria(ObjectEntity.class).add(Example.create(searchExample))
                        .addOrder(Order.desc("objectModifiedTimestamp")).setMaxResults(1);
                search = getSearchByBucket(search, bucket);
				List<ObjectEntity> results = search.list();

                if (results == null || results.size() < 1) {
                    throw new NoSuchElementException();
                } else if (results.size() > 1) {
                    //this.repairObjectLatest(bucket, objectKey);
                    //Do async repair if necessary to remove old data if overwritten
                    //fireRepairTask(bucket, objectKey);
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
    public ObjectEntity generateAndPersistDeleteMarker(@Nonnull ObjectEntity currentObject,
                                                       @Nonnull AccessControlPolicy acp,
                                                       @Nonnull User owningUser) throws MetadataOperationFailureException {
        final ObjectEntity deleteMarker = currentObject.generateNewDeleteMarkerFrom();

        try (TransactionResource trans = Entities.transactionFor(ObjectEntity.class)){
            deleteMarker.setAcl(acp);
            deleteMarker.setOwnerCanonicalId(owningUser.getAccount().getCanonicalId());
            deleteMarker.setOwnerDisplayName(owningUser.getAccount().getName());
            deleteMarker.setOwnerIamUserDisplayName(owningUser.getName());
            deleteMarker.setOwnerIamUserId(owningUser.getUserId());
            ObjectEntity persistedDeleteMarker = Entities.persist(deleteMarker);
            trans.commit();
            return persistedDeleteMarker;
        } catch(Exception e) {
            LOG.warn("Failed to persist the delete marker " + deleteMarker.getObjectUuid());
            throw new MetadataOperationFailureException(e);
        }
    }

	@Override
	public void delete(final @Nonnull ObjectEntity objectToDelete) throws IllegalResourceStateException, MetadataOperationFailureException {
        try {
            //Delete markers can be just removed not state transitioned.
            if(objectToDelete.getIsDeleteMarker()) {
                Transactions.delete(objectToDelete);
                return;
            }

            boolean success = Entities.asTransaction(ObjectEntity.class, ObjectStateTransitions.TRANSITION_TO_DELETED).apply(objectToDelete);
            if(!success) {
                throw new MetadataOperationFailureException("Delete operation returned false");
            }
        } catch(MetadataOperationFailureException | IllegalResourceStateException e) {
            throw e;
        } catch(Exception e) {
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
            for(ObjectEntity e : uploads) {
                Entities.delete(e);
            }
            db.commit();
        } catch(Exception e) {
            throw new MetadataOperationFailureException(e);
        } finally {
            if (db != null && db.isActive()) {
                db.rollback();
            }
        }
    }


    @Override
    public ObjectEntity lookupUpload(Bucket bucket, String uploadId) throws Exception {
        EntityTransaction db = Entities.get(ObjectEntity.class);
        try {
            Criteria search = Entities.createCriteria(ObjectEntity.class);
            ObjectEntity searchExample = new ObjectEntity().withBucket(bucket).withState(ObjectState.mpu_pending);
            searchExample.setUploadId(uploadId);
            searchExample.setPartNumber(null);
            search.add(Example.create(searchExample));
            search = getSearchByBucket(search, bucket);
            List<ObjectEntity> results = search.list();
            db.commit();
            if (results.size() > 0) {
                return results.get(0);
            } else {
                throw new NoSuchUploadException(uploadId);
            }
        } finally {
            if (db != null && db.isActive()) {
                db.rollback();
            }
        }
    }

    @Override
    public PaginatedResult<ObjectEntity> listUploads(Bucket bucket, int maxUploads, String prefix, String delimiter, String keyMarker, String uploadIdMarker) throws Exception {

        try(TransactionResource trans = Entities.transactionFor(ObjectEntity.class)) {
            PaginatedResult<ObjectEntity> result = new PaginatedResult<ObjectEntity>();
            HashSet<String> commonPrefixes = new HashSet<String>();

            // Include zero since 'istruncated' is still valid
            if (maxUploads >= 0) {
                final int queryStrideSize = maxUploads + 1;
                ObjectEntity searchObj = new ObjectEntity();
                searchObj.withBucket(bucket); //This doesn't actually filter, but do it anyway
                searchObj.withState(ObjectState.mpu_pending);

                Criteria objCriteria = Entities.createCriteria(ObjectEntity.class);
                objCriteria.setReadOnly(true);
                objCriteria.setFetchSize(queryStrideSize);
                objCriteria.add(Example.create(searchObj));
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

                //Be sure to add the bucket restriction last
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
		try (TransactionResource trans = Entities.transactionFor(ObjectEntity.class)){
				// Do record swap if existing record is found.
				ObjectEntity extantEntity = Entities.merge(object);
				extantEntity.setAcl(acp);
				trans.commit();
				return extantEntity;
        } catch (Exception e) {
            LOG.error("Error setting ACP on backend for object: " + object.getResourceFullName());
			throw new InternalErrorException(object.getResourceFullName() + "?versionId="
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
				ObjectEntity searchObj = new ObjectEntity().withBucket(bucket).withState(ObjectState.extant);

				// Return latest version, so exclude delete markers as well.
				// This makes listVersion act like listObjects
				if (latestOnly) {
					searchObj.setIsLatest(true);
                    searchObj.setIsDeleteMarker(false);
				}

				Criteria objCriteria = Entities.createCriteria(ObjectEntity.class);
				objCriteria.setReadOnly(true);
				objCriteria.setFetchSize(queryStrideSize);
				objCriteria.add(Example.create(searchObj));
				objCriteria.addOrder(Order.asc("objectKey"));
				objCriteria.addOrder(Order.desc("objectModifiedTimestamp"));
				objCriteria.setMaxResults(queryStrideSize);

				if (!Strings.isNullOrEmpty(fromKeyMarker)) {
					objCriteria.add(Restrictions.gt("objectKey", fromKeyMarker));
				}

				if (!Strings.isNullOrEmpty(fromVersionId)) {
					objCriteria.add(Restrictions.gt("versionId", fromVersionId));
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

						if (resultKeyCount == maxEntries) {
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
		try(TransactionResource trans = Entities.transactionFor(ObjectEntity.class)) {
			/*Criteria queryCriteria = Entities.createCriteria(ObjectEntity.class);
            queryCriteria.add(Restrictions.eq("state", ObjectState.extant))
                    .createCriteria("bucket").add(Restrictions.eq("naturalId", bucket.getNaturalId()))*/
            Criteria queryCriteria = Entities.createCriteria(ObjectEntity.class).add(Restrictions.eq("state", ObjectState.extant))
                    .setProjection(Projections.rowCount());
            queryCriteria = getSearchByBucket(queryCriteria, bucket);
            final Number count = (Number) queryCriteria.uniqueResult();
            trans.commit();
            return count.longValue();
		} catch (Throwable e) {
			LOG.error("Error getting object count for bucket " + bucket.getBucketName(), e);
			throw new Exception(e);
		}
	}

    @Override
    public ObjectEntity transitionObjectToState(@Nonnull final ObjectEntity entity, @Nonnull ObjectState destState) throws IllegalResourceStateException, MetadataOperationFailureException {
        Function<ObjectEntity, ObjectEntity> transitionFunction;

        switch(destState) {
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
            return Entities.asTransaction(ObjectEntity.class, transitionFunction).apply(entity);
        } catch(ObjectStorageInternalException e) {
            throw e;
        } catch(Exception e) {
            throw new MetadataOperationFailureException(e);
        }
    }

    /**
     * Update the progress timeout field in the object entity. Will set it
     * to current-time + ObjectStorageProperties.PROGRESS_TIMEOUT_SEC
     * @param entity
     * @throws Exception
     */
    public ObjectEntity updateCreationTimeout(ObjectEntity entity) throws Exception {
        try (TransactionResource trans = Entities.transactionFor(ObjectEntity.class)) {
            ObjectEntity mergedEntity = Entities.merge(entity);
            if(ObjectState.creating.equals(mergedEntity.getState())) {
                mergedEntity.updateCreationExpiration();
            }
            Entities.flush(mergedEntity); //Ensure it is pushed right away
            trans.commit();
            return mergedEntity;
        } catch(Exception e) {
            LOG.error("Error updating progress timeout for object " + entity.getObjectUuid());
            throw e;
        }
    }

}
