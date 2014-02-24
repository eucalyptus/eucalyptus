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
import com.eucalyptus.entities.TransactionResource;
import com.eucalyptus.objectstorage.ObjectState;
import com.eucalyptus.objectstorage.PaginatedResult;
import com.eucalyptus.objectstorage.entities.Bucket;
import com.eucalyptus.objectstorage.entities.PartEntity;
import com.eucalyptus.objectstorage.exceptions.IllegalResourceStateException;
import com.eucalyptus.objectstorage.exceptions.MetadataOperationFailureException;
import com.eucalyptus.objectstorage.exceptions.ObjectStorageInternalException;
import com.eucalyptus.objectstorage.exceptions.s3.EntityTooSmallException;
import com.eucalyptus.objectstorage.exceptions.s3.InvalidPartException;
import com.eucalyptus.objectstorage.exceptions.s3.InvalidPartOrderException;
import com.eucalyptus.objectstorage.exceptions.s3.S3Exception;
import com.eucalyptus.objectstorage.util.ObjectStorageProperties;
import com.eucalyptus.storage.msgs.s3.Part;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import org.apache.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.criterion.Example;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.persistence.EntityTransaction;
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

/**
 * Database backed implementation of ObjectMetadataManager
 * 
 */
public class DbMpuPartMetadataManagerImpl implements MpuPartMetadataManager {
	private static final Logger LOG = Logger.getLogger(DbMpuPartMetadataManagerImpl.class);
	private static final ExecutorService HISTORY_REPAIR_EXECUTOR = Executors.newCachedThreadPool();

	@Override
    public void start() throws Exception {
		// Do nothing
	}

	@Override
    public void stop() throws Exception {
		try {
			List<Runnable> pendingTasks = HISTORY_REPAIR_EXECUTOR.shutdownNow();
			LOG.info("Stopping ObjectMetadataManager... Found " + pendingTasks.size() + " pending tasks at time of shutdown");
		} catch (final Throwable f) {
			LOG.error("Error stopping ObjectMetadataManager", f);
		}
	}

    @Override
    public PartEntity initiatePartCreation(@Nonnull PartEntity objectToCreate) throws Exception {
        return this.transitionPartToState(objectToCreate, ObjectState.creating);
    }

    @Override
    public PartEntity finalizeCreation(PartEntity objectToUpdate, Date updateTimestamp, String eTag) throws MetadataOperationFailureException {
        objectToUpdate.setObjectModifiedTimestamp(updateTimestamp);
        objectToUpdate.seteTag(eTag);
        objectToUpdate.setIsLatest(true);
        return this.transitionPartToState(objectToUpdate, ObjectState.extant);
    }

    /**
     * Provides the search criteria to handle the FK relation from PartEntity->Bucket
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
	private static final Predicate<PartEntity> SET_LATEST_PREDICATE = new Predicate<PartEntity>() {
		public boolean apply(PartEntity example) {
			try {
				example.setIsLatest(true);
                example = example.withState(ObjectState.extant);
				Criteria search = Entities.createCriteria(PartEntity.class);
				search.add(Example.create(example)).addOrder(Order.desc("objectModifiedTimestamp"));
                search = getSearchByBucket(search, example.getBucket());
                List<PartEntity> results = search.list();

				if (results != null && results.size() > 1) {
					try {
						// Set all but the first element as not latest
						for (PartEntity obj : results.subList(1, results.size())) {
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

    private static final Comparator timestampComparator = new Comparator<PartEntity>() {

        @Override
        public int compare(PartEntity objectEntity, PartEntity objectEntity2) {
            return objectEntity2.getObjectModifiedTimestamp().compareTo(objectEntity.getObjectModifiedTimestamp());
        }
    };

	@Override
	public void cleanupInvalidParts(final Bucket bucket, final String objectKey) throws Exception {
		PartEntity searchExample = new PartEntity(bucket, objectKey, null);
		
		final Predicate<PartEntity> repairPredicate = new Predicate<PartEntity>() {
			public boolean apply(PartEntity example) {
                try {
                    //Ensure only the most recent is 'latest'
                    SET_LATEST_PREDICATE.apply(example);

                    //Find not-latest null-versioned objects and mark them for deletion.
                    PartEntity searchExample = new PartEntity().withKey(objectKey).withBucket(bucket).withState(ObjectState.extant);
                    searchExample.setIsLatest(false);
                    List<PartEntity> results = Entities.query(searchExample);
                    if (results != null && results.size() > 0) {
                        // Set all but the first element as not latest
                        for (PartEntity obj : results) {
                            LOG.trace("Marking MPU part " + obj.getPartUuid() + " as no longer latest version");
                            obj = transitionPartToState(obj, ObjectState.deleting);
                        }
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
    public List<PartEntity> lookupFailedParts() throws MetadataOperationFailureException {
        // Return the latest version based on the created date.
        try (TransactionResource trans = Entities.transactionFor(PartEntity.class)){
            PartEntity searchExample = new PartEntity().withState(ObjectState.creating);
            Criteria search = Entities.createCriteria(PartEntity.class);
            List<PartEntity> results = search.add(Example.create(searchExample))
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
	public void delete(final @Nonnull PartEntity objectToDelete) throws IllegalResourceStateException, MetadataOperationFailureException {
        try {
            boolean success = Entities.asTransaction(PartEntity.class, MpuPartStateTransitions.TRANSITION_TO_DELETED).apply(objectToDelete);
            if(!success) {
                throw new MetadataOperationFailureException("Delete operation returned false");
            }
        } catch(MetadataOperationFailureException | IllegalResourceStateException e) {
            throw e;
        } catch(Exception e) {
            throw new MetadataOperationFailureException(e);
        }
	}

	protected void fireRepairTask(final Bucket bucket, final String objectKey) {
		try {
			HISTORY_REPAIR_EXECUTOR.submit(new Runnable() {
				public void run() {
					try {
						cleanupInvalidParts(bucket, objectKey);
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
    public List<PartEntity> lookupPartsInState(Bucket searchBucket, String searchKey, String uploadId, ObjectState state) throws Exception {
        EntityTransaction db = Entities.get(PartEntity.class);
        try {
            Criteria search = Entities.createCriteria(PartEntity.class);
            PartEntity searchExample = new PartEntity().withBucket(searchBucket).withKey(searchKey).withUploadId(uploadId).withState(state);
            search.add(Example.create(searchExample));
            if(searchBucket != null ) {
                search = getSearchByBucket(search, searchBucket);
            }
            List<PartEntity> results = search.list();
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
    public PartEntity transitionPartToState(@Nonnull final PartEntity entity, @Nonnull ObjectState destState) throws IllegalResourceStateException, MetadataOperationFailureException {
        Function<PartEntity, PartEntity> transitionFunction;

        switch(destState) {
            case creating:
                transitionFunction = MpuPartStateTransitions.TRANSITION_TO_CREATING;
                break;
            case extant:
                transitionFunction = MpuPartStateTransitions.TRANSITION_TO_EXTANT;
                break;
            case deleting:
                transitionFunction = MpuPartStateTransitions.TRANSITION_TO_DELETING;
                break;
            default:
                LOG.error("Unexpected destination state: " + destState);
                throw new IllegalArgumentException();
        }

        try {
            return Entities.asTransaction(PartEntity.class, transitionFunction).apply(entity);
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
    @Override
    public PartEntity updateCreationTimeout(PartEntity entity) throws Exception {
        try (TransactionResource trans = Entities.transactionFor(PartEntity.class)) {
            PartEntity mergedEntity = Entities.merge(entity);
            if(ObjectState.creating.equals(mergedEntity.getState())) {
                mergedEntity.updateCreationExpiration();
            }
            Entities.flush(mergedEntity); //Ensure it is pushed right away
            trans.commit();
            return mergedEntity;
        } catch(Exception e) {
            LOG.error("Error updating progress timeout for object " + entity.getPartUuid());
            throw e;
        }
    }

    @Override
    public HashMap<Integer, PartEntity> getParts(Bucket bucket, String objectKey, String uploadId) throws Exception {
        HashMap<Integer, PartEntity> parts = new HashMap<>();
        try(TransactionResource trans = Entities.transactionFor(PartEntity.class)){
            Criteria search = Entities.createCriteria(PartEntity.class);
            PartEntity searchExample = new PartEntity(bucket, objectKey, uploadId).withState(ObjectState.extant);
            search.add(Example.create(searchExample));
            search = getSearchByBucket(search, bucket);
            List<PartEntity> results = search.list();
            trans.commit();
            for (PartEntity result : results) {
                parts.put(result.getPartNumber(), result);
            }
            return parts;
        } catch(Exception e) {
            LOG.warn("Error looking up parts for MPU id : " + uploadId);
            throw e;
        }
    }

    protected void firePartConsolidation(final Bucket bucket, final String uploadId, final String objectKey, final Integer partNumber) {
        try {
            HISTORY_REPAIR_EXECUTOR.submit(new Runnable() {
                public void run() {
                    try {
                        doConsolidateParts(bucket, objectKey, uploadId, partNumber);
                    } catch (final Throwable f) {
                        LOG.error("Error during object history consolidation for " + bucket + " uploadId:" + uploadId + " partNumber:" + partNumber, f);
                    }
                }
            });
        } catch (final Throwable f) {
            LOG.warn("Error setting part history for " + bucket+ " uploadId:" + uploadId + " partNumber:" + partNumber, f);
        }
    }

    private void doConsolidateParts(Bucket bucket, String objectKey, String uploadId, Integer partNumber) {
        PartEntity searchExample = new PartEntity(bucket, objectKey, uploadId).withPartNumber(partNumber);
        searchExample = searchExample.withState(ObjectState.extant);

        final Predicate<PartEntity> repairPredicate = new Predicate<PartEntity>() {
            public boolean apply(PartEntity example) {
                //Remove all but latest entry
                try {
                    Criteria search = Entities.createCriteria(PartEntity.class);
                    List<PartEntity> results = search.add(Example.create(example))
                            .addOrder(Order.desc("objectModifiedTimestamp")).list();

                    if (results != null && results.size() > 0) {
                        try {
                            for (PartEntity partEntity : results.subList(1, results.size())) {
                                LOG.trace("Marking part " + partEntity.getBucket().getBucketName()
                                        + " uploadId: " + partEntity.getUploadId()
                                        + " partNumber: " + partEntity.getPartNumber()
                                        + " for deletion because it is not latest.");
                                partEntity.setState(ObjectState.deleting);
                            }
                        } catch (IndexOutOfBoundsException e) {
                            // Either 0 or 1 result, nothing to do
                        }
                    }
                } catch (NoSuchElementException e) {
                    // Nothing to do.
                } catch (Exception e) {
                    LOG.error("Error consolidationg Part records for " + example.getBucket().getBucketName() + " uploadId: "
                            + example.getUploadId() + " partNumber: " + example.getPartNumber());
                    return false;
                }
                return true;
            }
        };
        try {
            Entities.asTransaction(repairPredicate).apply(searchExample);
        } catch (final Throwable f) {
            LOG.error("Error in part repair", f);
        }
    }

    @Override
    public long processPartListAndGetSize(List<Part> partsInManifest, HashMap<Integer, PartEntity> availableParts) throws S3Exception {
        int lastPartNumber = 0;
        long objectSize = 0;
        int numPartsProcessed = 0;
        for (Part partInManifest : partsInManifest) {
            Integer partNumber = partInManifest.getPartNumber();
            if (partNumber <= lastPartNumber) {
                throw new InvalidPartOrderException("partNumber: " + partNumber);
            }
            PartEntity actualPart = availableParts.get(partNumber);
            if (actualPart == null) {
                throw new InvalidPartException("partNumber: " + partNumber);
            }
            final long actualPartSize = actualPart.getSize();
            if ((++numPartsProcessed) < partsInManifest.size() && actualPartSize < ObjectStorageProperties.MPU_PART_MIN_SIZE) {
            	throw new EntityTooSmallException("uploadId: " + actualPart.getUploadId() + " partNumber: " + partNumber);
            }
            objectSize += actualPartSize;
            lastPartNumber = partNumber;
        }
        return objectSize;
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
                PartEntity searchPart = new PartEntity(bucket, objectKey, uploadId).withState(ObjectState.extant);

                Criteria objCriteria = Entities.createCriteria(PartEntity.class);
                objCriteria.setReadOnly(true);
                objCriteria.setFetchSize(queryStrideSize);
                objCriteria.add(Example.create(searchPart));
                objCriteria.addOrder(Order.asc("partNumber"));
                objCriteria.addOrder(Order.desc("objectModifiedTimestamp"));
                objCriteria.setMaxResults(queryStrideSize);

                if (partNumberMarker!= null) {
                    objCriteria.add(Restrictions.gt("partNumber", partNumberMarker));
                }
                objCriteria = getSearchByBucket(objCriteria, bucket);

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

                        result.getEntityList().add(partRecord);
                        result.setLastEntry(partRecord);
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
}
