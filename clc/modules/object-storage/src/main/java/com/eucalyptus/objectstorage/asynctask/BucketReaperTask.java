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

package com.eucalyptus.objectstorage.asynctask;

import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.eucalyptus.objectstorage.BucketMetadataManagers;
import com.eucalyptus.objectstorage.BucketState;
import com.eucalyptus.objectstorage.ObjectMetadataManagers;
import com.eucalyptus.objectstorage.OsgBucketFactory;
import com.eucalyptus.objectstorage.PaginatedResult;
import com.eucalyptus.objectstorage.entities.Bucket;
import com.eucalyptus.objectstorage.entities.ObjectEntity;
import com.eucalyptus.objectstorage.entities.ObjectStorageGlobalConfiguration;
import com.eucalyptus.objectstorage.providers.ObjectStorageProviders;
import com.eucalyptus.storage.config.ConfigurationCache;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;

/**
 * Scans metadata for each objects in a bucket and cleans history for each.
 */
public class BucketReaperTask {
  private static final Logger LOG = Logger.getLogger(BucketReaperTask.class);

  private static final long TASK_INTERVAL = 30 * 1000; // 30 seconds
  private static final long TIMEOUT_LOG_INTERVAL = TimeUnit.HOURS.toMillis( 1 );
  private static final AtomicLong TIMEOUT_LOG_TOKEN = new AtomicLong( );
  private static final Random rand = new Random(System.currentTimeMillis());

  private volatile long startTime;
  private volatile long timeoutTime;
  private volatile boolean interrupted = false;

  public BucketReaperTask() {}

  // Does a single scan of all objects in the bucket and does history cleanup on each
  public void run( final long nextRun ) {
    startTime = System.currentTimeMillis( );
    timeoutTime = nextRun - TASK_INTERVAL;
    Supplier<Level> timeoutLevelSupplier = Suppliers.memoize(() -> logTimeout( ) ? Level.WARN : Level.TRACE);

    int bucketsResolved = 0;
    int bucketsCleaned = 0;
    boolean cleaningStarted = false;

    try {
      LOG.trace("Initiating bucket cleanup task");
      final List<Bucket> buckets = BucketMetadataManagers.getInstance().lookupBucketsByState(null);

      if (buckets == null || buckets.size() <= 0) {
        LOG.trace("No buckets found to clean. Cleanup task complete");
        return;
      }

      // Resolve all bucket states (fast) before cleaning histories (slow, could time out)
      for (Bucket bucket : buckets) {
        if (!isTimedOut() && !interrupted) {
          resolveBucketState(bucket);
          bucketsResolved++;
        } else {
          LOG.warn("Timed out while cleaning up bucket states after processing " + bucketsResolved + " buckets.");
          break;
        }
      }
      LOG.trace("Finished resolving " + bucketsResolved + " bucket states.");
      // Randomly iterate through the buckets so they all have equal chance of running before a timeout
      Bucket b;
      int idx;
      cleaningStarted = true;
      while (buckets.size() > 0 && !isTimedOut() && !interrupted) {
        idx = rand.nextInt(buckets.size());
        b = buckets.get(idx);
        cleanObjectHistoriesInBucket(b,timeoutLevelSupplier);
        buckets.remove(idx);
        bucketsCleaned++;
      }
    } catch (final Throwable f) {
      LOG.error("Error during bucket cleanup execution. Will retry later", f);
    } finally {
      try {
        long endTime = System.currentTimeMillis();
        LOG.trace("Bucket cleanup execution task took " + Long.toString(endTime - startTime) + "ms to complete");
        if (isTimedOut() || interrupted) {
          if (!cleaningStarted) {
            LOG.log(timeoutLevelSupplier.get(),
                "Timed out while cleaning up bucket states after processing " + bucketsResolved + " buckets. " +
                    "No object records were cleaned up.");
          } else {
            LOG.log(timeoutLevelSupplier.get(),
                "Timed out while cleaning up object records after processing " + bucketsCleaned + " buckets.");
          }
        }
      } catch (final Throwable f) {
        // Do nothing, but don't allow exceptions out
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
   * Fixes the state of the bucket. If in 'deleting' state, will issue deletion to backend. And remove the bucket. If in 'creating' state that is
   * expired (by timestamp), will issue delete to backend and update state
   * 
   * @param bucket
   */
  private void resolveBucketState(Bucket bucket) {
    LOG.trace("Resolving bucket state for bucket uuid " + bucket.getBucketUuid() + ", name " + bucket.getBucketName());
    if (BucketState.deleting.equals(bucket.getState())
        || !bucket.stateStillValid(ConfigurationCache.getConfiguration(ObjectStorageGlobalConfiguration.class)
            .getBucket_creation_wait_interval_seconds())) {
      // Clean-up a bucket marked for deletion. This usually indicates a failed delete operation previously
      LOG.trace("Deleting backend bucket for bucket uuid " + bucket.getBucketUuid() + " during bucket cleanup");
      try {
        OsgBucketFactory.getFactory().deleteBucket(ObjectStorageProviders.getInstance(), bucket, null, null);
      } catch (Exception e) {
        LOG.error("Error cleaning deletion marked bucketuuid " + bucket.getBucketUuid(), e);
      }
    }
  }

  private boolean isTimedOut() {
    return System.currentTimeMillis() >= timeoutTime;
  }

  protected void cleanObjectHistoriesInBucket( final Bucket b, final Supplier<Level> timeoutLevelSupplier ) {
    String nextKey = null;
    final int chunkSize = 1000;
    int objectsProcessed = 0;
    PaginatedResult<ObjectEntity> result = null;
    LOG.trace("Cleaning object histories for bucket uuid " + b.getBucketUuid() + ", name " + b.getBucketName());
    do {
      try {
        result = ObjectMetadataManagers.getInstance().listForCleanupPaginated(b, chunkSize, null, null, nextKey);
      } catch (final Throwable f) {
        LOG.error("Could not get object listing for bucket " + b.getBucketName() + " with next marker: " + nextKey);
        break;
      }

      INNER: for (ObjectEntity obj : result.getEntityList()) {
        try {
          ObjectMetadataManagers.getInstance().cleanupInvalidObjects(b, obj.getObjectKey());
          objectsProcessed++;
          if ((objectsProcessed % 1000) == 0) {
            LOG.trace("Processed " + objectsProcessed + " objects for bucket uuid " + b.getBucketUuid() +
                    ", name " + b.getBucketName());
          }
        } catch (final Throwable f) {
          LOG.error("Error doing async repair of object " + b.getBucketName() + "/" + obj.getObjectKey() + " Continuing to next object", f);
        }
        if (interrupted) {
          break INNER;
        }
      }

      if (!interrupted && result.getIsTruncated()) {
        nextKey = ((ObjectEntity) result.getLastEntry()).getObjectKey();
      } else {
        nextKey = null;
      }
    } while (nextKey != null && !isTimedOut());
    if (interrupted || isTimedOut()) {
      LOG.log( timeoutLevelSupplier.get( ),
          "Timed out while cleaning up object records in bucket uuid " +
              b.getBucketUuid() + ", name " + b.getBucketName() +
              " after processing " + objectsProcessed + " objects.");
    } else {
      LOG.trace("Finished cleaning " + objectsProcessed + " object histories for bucket uuid " +
          b.getBucketUuid() + ", name " + b.getBucketName());
    }
  }

  private boolean logTimeout( ) {
    final long lastToken = TIMEOUT_LOG_TOKEN.get( );
    final long currentToken = System.currentTimeMillis( ) / TIMEOUT_LOG_INTERVAL;
    return lastToken!=currentToken && TIMEOUT_LOG_TOKEN.compareAndSet( lastToken, currentToken );
  }
}
