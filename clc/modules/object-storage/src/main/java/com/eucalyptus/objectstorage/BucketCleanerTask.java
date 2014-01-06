package com.eucalyptus.objectstorage;

import java.util.List;
import java.util.Random;

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
public class BucketCleanerTask implements Runnable {
	private static final Logger LOG = Logger.getLogger(BucketCleanerTask.class);

	private long startTime;
	private static final long MAX_TASK_DURATION = 30 * 1000; //30 seconds
	
	public BucketCleanerTask() {}
		
	//Does a single scan of all objects in the bucket and does history cleanup on each
	@Override
	public void run() {
		startTime = System.currentTimeMillis();
		try {
			LOG.trace("Initiating bucket cleanup task");
			final List<Bucket> buckets = BucketManagers.getInstance().list(null, true, null);

			if(buckets == null || buckets.size() <= 0) {
				LOG.trace("No buckets found to clean. Cleanup task complete");
				return;
			}
			
			Random rand = new Random(System.currentTimeMillis());
			Bucket b = null;
			
			//Randomly iterate through
			int idx = 0;
			while(buckets.size() > 0 && !isTimedOut()) {
				idx = rand.nextInt(buckets.size());
				b = buckets.get(idx);
				cleanObjectHistoriesInBucket(b);
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
	
	private boolean isTimedOut() {
		return System.currentTimeMillis() - startTime >= MAX_TASK_DURATION;		
	}
	
	protected void cleanObjectHistoriesInBucket(Bucket b) {
		String nextKey = null;
		final int chunkSize = 1000;
		PaginatedResult<ObjectEntity> result = null;
		do {
			try {
				result = ObjectManagers.getInstance().listPaginated(b, chunkSize, null, null, nextKey);
			} catch(final Throwable f) {
				LOG.error("Could not get object listing for bucket " + b.getBucketName() + " with next marker: " + nextKey);
				nextKey = null;
				result = null;
				break;
			}
			
			for(ObjectEntity obj : result.getEntityList()) {
				try {
					ObjectManagers.getInstance().doFullRepair(b, obj.getObjectKey());
				} catch(final Throwable f) {
					LOG.error("Error doing async repair of object " + b.getBucketName() + "/" + obj.getObjectKey() + " Continuing to next object", f);					
				}
			}
			
			if(result.getIsTruncated()) {
				nextKey = ((ObjectEntity)result.getLastEntry()).getObjectKey();
			} else {
				nextKey = null;
			}
		} while(nextKey != null && !isTimedOut());
	}
}
