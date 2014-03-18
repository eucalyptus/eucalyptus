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

package com.eucalyptus.objectstorage.asynctask;

import java.util.List;
import java.util.Random;

import com.eucalyptus.objectstorage.*;
import com.eucalyptus.objectstorage.BucketMetadataManagers;
import com.eucalyptus.objectstorage.ObjectMetadataManagers;
import com.eucalyptus.objectstorage.providers.ObjectStorageProviders;
import com.eucalyptus.objectstorage.util.ObjectStorageProperties;
import org.apache.log4j.Logger;

import com.eucalyptus.objectstorage.entities.Bucket;
import com.eucalyptus.objectstorage.entities.ObjectEntity;

/**
 * Scans metadata for each objects in a bucket and cleans history for each
 * Many of these may be running concurrently. Has a self-imposed timeout of 30 seconds.
 * 
 * This should be more than sufficient given that it only manipulates metadata and never
 * interacts with the backend.
 *
 */
public class BucketReaperTask implements Runnable {
	private static final Logger LOG = Logger.getLogger(BucketReaperTask.class);

	private long startTime;
	private static final long MAX_TASK_DURATION = 30 * 1000; //30 seconds
    private static final Random rand = new Random(System.currentTimeMillis());
    private boolean interrupted = false;

	public BucketReaperTask() {}
		
	//Does a single scan of all objects in the bucket and does history cleanup on each
	@Override
	public void run() {
		startTime = System.currentTimeMillis();
		try {
			LOG.trace("Initiating bucket cleanup task");
			final List<Bucket> buckets = BucketMetadataManagers.getInstance().lookupBucketsByState(null);

			if(buckets == null || buckets.size() <= 0) {
				LOG.trace("No buckets found to clean. Cleanup task complete");
				return;
			}

			Bucket b;
			//Randomly iterate through
			int idx;
			while(buckets.size() > 0 && !isTimedOut() && !interrupted) {
				idx = rand.nextInt(buckets.size());
				b = buckets.get(idx);
				cleanObjectHistoriesInBucket(b);
                resolveBucketState(b);
				buckets.remove(idx);				
			}
			
		} catch(final Throwable f) {
			LOG.error("Error during bucket cleanup execution. Will retry later", f);			
		} finally {
			try {
				long endTime = System.currentTimeMillis();
				LOG.trace("Bucket cleanup execution task took " + Long.toString(endTime - startTime) + "ms to complete");
			} catch( final Throwable f) {
				//Do nothing, but don't allow exceptions out
			}
		}
	}

    public void interrupt() {
        this.interrupted = true;
    }

    public void resume() {
        this.interrupted = false;
    }

    /**
     * Fixes the state of the bucket. If in 'deleting' state, will issue deletion to backend. And remove the bucket.
     * If in 'creating' state that is expired (by timestamp), will issue delete to backend and update state
     * @param bucket
     */
    private void resolveBucketState(Bucket bucket) {
        LOG.trace("Resolving bucket state for bucket uuid " + bucket.getBucketUuid());
        if(BucketState.deleting.equals(bucket.getState()) || !bucket.stateStillValid()) {
            //Clean-up a bucket marked for deletion. This usually indicates a failed delete operation previously
            LOG.trace("Deleting backend bucket for bucket uuid " + bucket.getBucketUuid() + " during bucket cleanup");
            try {
                OsgBucketFactory.getFactory().deleteBucket(ObjectStorageProviders.getInstance(), bucket, null, null);
            } catch(Exception e) {
                LOG.error("Error cleaning deletion marked bucketuuid " + bucket.getBucketUuid(), e);
            }
        }
    }
	
	private boolean isTimedOut() {
		return System.currentTimeMillis() - startTime >= MAX_TASK_DURATION;		
	}
	
	protected void cleanObjectHistoriesInBucket(Bucket b) {
		String nextKey = null;
		final int chunkSize = 1000;
		PaginatedResult<ObjectEntity> result = null;
        LOG.trace("Cleaning object histories for bucket uuid " + b.getBucketUuid());
		do {
			try {
				result = ObjectMetadataManagers.getInstance().listPaginated(b, chunkSize, null, null, nextKey);
			} catch(final Throwable f) {
				LOG.error("Could not get object listing for bucket " + b.getBucketName() + " with next marker: " + nextKey);
				nextKey = null;
				result = null;
				break;
			}
			
			INNER: for(ObjectEntity obj : result.getEntityList()) {
				try {
					ObjectMetadataManagers.getInstance().cleanupInvalidObjects(b, obj.getObjectKey());
				} catch(final Throwable f) {
					LOG.error("Error doing async repair of object " + b.getBucketName() + "/" + obj.getObjectKey() + " Continuing to next object", f);					
				}
                if (interrupted) {
                    break INNER;
                }
			}
			
			if(!interrupted && result.getIsTruncated()) {
				nextKey = ((ObjectEntity)result.getLastEntry()).getObjectKey();
			} else {
				nextKey = null;
			}
		} while(nextKey != null && !isTimedOut());
	}
}
