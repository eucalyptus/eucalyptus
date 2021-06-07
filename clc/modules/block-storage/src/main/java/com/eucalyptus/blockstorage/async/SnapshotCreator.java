/*************************************************************************
 * Copyright 2008 Regents of the University of California
 * Copyright 2017 Ent. Services Development Corporation LP
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
 * POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 * THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 * COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 * AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 * IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 * SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 * WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 * REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 * IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 * NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/

package com.eucalyptus.blockstorage.async;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.Future;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.criterion.Example;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;

import com.eucalyptus.blockstorage.LogicalStorageManager;
import com.eucalyptus.blockstorage.S3SnapshotTransfer;
import com.eucalyptus.blockstorage.SnapshotProgressCallback;
import com.eucalyptus.blockstorage.SnapshotTransfer;
import com.eucalyptus.blockstorage.StorageResource;
import com.eucalyptus.blockstorage.StorageResourceWithCallback;
import com.eucalyptus.blockstorage.entities.SnapshotInfo;
import com.eucalyptus.blockstorage.entities.SnapshotTransferConfiguration;
import com.eucalyptus.blockstorage.entities.StorageInfo;
import com.eucalyptus.blockstorage.util.BlockStorageUtilSvc;
import com.eucalyptus.blockstorage.util.BlockStorageUtilSvcImpl;
import com.eucalyptus.blockstorage.util.StorageProperties;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionException;
import com.eucalyptus.entities.TransactionResource;
import com.eucalyptus.entities.Transactions;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.metrics.MonitoredAction;
import com.eucalyptus.util.metrics.ThruputMetrics;
import com.google.common.base.Function;

import com.eucalyptus.blockstorage.util.EucaSemaphore;
import com.eucalyptus.blockstorage.util.EucaSemaphoreDirectory;

public class SnapshotCreator implements Runnable {
  private static Logger LOG = Logger.getLogger(SnapshotCreator.class);

  private String volumeId;
  private String snapshotId;
  private String snapPointId;
  private LogicalStorageManager blockManager;
  private SnapshotTransfer snapshotTransfer;
  private SnapshotProgressCallback progressCallback;
  private BlockStorageUtilSvc blockStorageUtilSvc;

  /**
   * Initializes the Snapshotter task. snapPointId should be null if no snap point has been created yet.
   * 
   * @param volumeId
   * @param snapshotId
   * @param snapPointId
   * @param blockManager TODO
   */
  public SnapshotCreator(String volumeId, String snapshotId, String snapPointId, LogicalStorageManager blockManager) {
    this.volumeId = volumeId;
    this.snapshotId = snapshotId;
    this.snapPointId = snapPointId;
    this.blockManager = blockManager;
    this.blockStorageUtilSvc = new BlockStorageUtilSvcImpl();
  }

  /**
   * Strictly for use by unit tests only, SnapshotTransfer and ProgressCallback are mocked and instantiated which should never be the case for actual
   * use
   * 
   * @param volumeId
   * @param snapshotId
   * @param snapPointId
   * @param mockBlockManager
   * @param mockSnapshotTransfer
   * @param mockProgressCallback
   */
  protected SnapshotCreator(String volumeId, String snapshotId, String snapPointId, LogicalStorageManager mockBlockManager,
      SnapshotTransfer mockSnapshotTransfer, SnapshotProgressCallback mockProgressCallback) {
    this(volumeId, snapshotId, snapPointId, mockBlockManager);
    this.snapshotTransfer = mockSnapshotTransfer;
    this.progressCallback = mockProgressCallback;
  }

  @Override
  public void run() {
    LOG.trace("Starting SnapshotCreator task");
    EucaSemaphore semaphore = EucaSemaphoreDirectory.getSolitarySemaphore(this.volumeId);
    try {
      Boolean shouldTransferSnapshots = true;
      // SnapshotTransfer snapshotTransfer = null;
      String bucket = null;
      // SnapshotProgressCallback progressCallback = null;

      // Check whether the snapshot needs to be uploaded
      shouldTransferSnapshots = StorageInfo.getStorageInfo().getShouldTransferSnapshots();

      if (shouldTransferSnapshots) {
        // Prepare for the snapshot upload (fetch credentials for snapshot upload to osg, create the bucket). Error out if this fails without
        // creating the snapshot on the blockstorage backend
        if (null == snapshotTransfer) {
          snapshotTransfer = new S3SnapshotTransfer(this.snapshotId, this.snapshotId);
        }
        bucket = snapshotTransfer.prepareForUpload();

        if (snapshotTransfer == null || StringUtils.isBlank(bucket)) {
          throw new EucalyptusCloudException("Failed to initialize snapshot transfer mechanism for uploading " + this.snapshotId);
        }
      }

      // Acquire the semaphore here and release it here as well
      try {
        try {
          semaphore.acquire();
        } catch (InterruptedException ex) {
          throw new EucalyptusCloudException("Failed to create snapshot " + this.snapshotId + " as the semaphore could not be acquired");
        }

        // Check to ensure that a failed/cancellation has not be set
        if (!isSnapshotMarkedFailed(this.snapshotId)) {
          if (null == progressCallback) {
            progressCallback = new SnapshotProgressCallback(this.snapshotId); // Setup the progress callback, that should start the progress
          }
          blockManager.createSnapshot(this.volumeId, this.snapshotId, this.snapPointId);
          progressCallback.updateBackendProgress(50); // to indicate that backend snapshot process is 50% done
        } else {
          throw new EucalyptusCloudException("Snapshot " + this.snapshotId + " marked as failed by another thread");
        }
      } finally {
        semaphore.release();
      }

      SnapshotInfo prevSnap = null;
      SnapshotInfo currSnap = null;

      Future<String> uploadFuture = null;
      if (!isSnapshotMarkedFailed(this.snapshotId)) {

        if (shouldTransferSnapshots) {
          // TODO move this check down further

          // generate snapshot location
          String snapshotLocation = SnapshotInfo.generateSnapshotLocationURI(SnapshotTransferConfiguration.OSG, bucket, this.snapshotId);

          // gather what needs to be uploaded
          try {
            // Check if backend supports snap deltas
            Integer maxDeltaLimit = StorageInfo.getStorageInfo().getMaxSnapshotDeltas();
            maxDeltaLimit = maxDeltaLimit != null ? maxDeltaLimit : 0;

            if (maxDeltaLimit > 0 && blockManager.supportsIncrementalSnapshots()) { // backend supports delta, evaluate if a delta can be uploaded
              LOG.debug("EBS backend supports incremental snapshots");

              int attempts = 0;
              do {
                attempts++;
                prevSnap = fetchPreviousSnapshot(maxDeltaLimit);

                if (prevSnap != null) {
                  // Acquire a semaphore to previous snapshot before updating the metadata for current snapshot
                  EucaSemaphore prevSnapSemaphore = EucaSemaphoreDirectory.getSolitarySemaphore(prevSnap.getSnapshotId());
                  try {
                    try {
                      prevSnapSemaphore.acquire();
                    } catch (InterruptedException ex) {
                      LOG.warn("Cannot update metadata for snapshot " + this.snapshotId + " due to an error acquiring semaphore for a previous snapshot "
                          + prevSnap.getSnapshotId() + ". May retry again later");
                      continue;
                    }
                    currSnap = updateSnapshotInfo(prevSnap.getSnapshotId(), snapshotLocation);
                  } finally {
                    prevSnapSemaphore.release();
                  }
                } else {
                  currSnap = updateSnapshotInfo(snapshotLocation);
                }
              } while (currSnap == null && attempts < 10);

            } else { // backend does not support deltas, upload entire snapshot
              LOG.debug("Either EBS backend does not support incremental snapshots or the feature is disabled");
              currSnap = updateSnapshotInfo(snapshotLocation);
            }

            if (currSnap == null) {
              LOG.warn("Failed to update metadata for snapshot " + this.snapshotId);
              throw new EucalyptusCloudException("Failed to update metadata for snapshot " + this.snapshotId);
            }

          } catch (EucalyptusCloudException e) {
            throw e;
          } catch (Exception e) {
            LOG.warn("Unable to evaluate snapshot location and upload specifics, failing snapshot " + this.snapshotId, e);
            throw new EucalyptusCloudException("Unable to evaluate snapshot location and upload specifics, failing snapshot " + this.snapshotId, e);
          }

          StorageResourceWithCallback srwc = null;
          StorageResource snapshotResource = null;

          if (prevSnap != null) {
            LOG.info("Generate delta between penultimate snapshot " + prevSnap.getSnapshotId() + " and latest snapshot " + this.snapshotId);
            srwc =
                blockManager.prepIncrementalSnapshotForUpload(this.volumeId, this.snapshotId, this.snapPointId, prevSnap.getSnapshotId(), prevSnap.getSnapPointId());
            snapshotResource = srwc.getSr();
          } else {
            LOG.info("Upload entire content of snapshot " + this.snapshotId);
            snapshotResource = blockManager.prepSnapshotForUpload(this.volumeId, this.snapshotId, this.snapPointId);
          }

          if (snapshotResource == null) {
            LOG.warn("Unable to upload snapshot " + this.snapshotId + " due to invalid source");
            throw new EucalyptusCloudException("Unable to upload snapshot " + this.snapshotId + " due to invalid source");
          }

          try {
            uploadFuture = snapshotTransfer.upload(snapshotResource, progressCallback);
          } catch (Exception e) {
            throw new EucalyptusCloudException("Failed to upload snapshot " + this.snapshotId + " to objectstorage", e);
          } finally {
            if (srwc != null && srwc.getCallback() != null) {
              // Call the callback even if the upload fails, to clean up temp snapshot artifacts
              blockManager.executeCallback(srwc.getCallback(), srwc.getSr());
            }
          }
        } else {
          // Snapshot does not have to be transferred
          LOG.debug("Snapshot uploads are disabled, skipping upload step for " + this.snapshotId);
        }
      } else {
        LOG.warn("Snapshot " + this.snapshotId + " marked as failed, aborting upload process");
        throw new EucalyptusCloudException("Snapshot " + this.snapshotId + " marked as failed, aborting upload process");
      }

      // finish the snapshot on backend - sever iscsi connection, disconnect and wait for it to complete
      try {
        LOG.debug("Finishing up " + this.snapshotId + " on block storage backend");
        blockManager.finishVolume(this.snapshotId);
        LOG.info("Finished creating " + this.snapshotId + " on block storage backend");
        progressCallback.updateBackendProgress(50); // to indicate that backend snapshot process is 50% done
      } catch (EucalyptusCloudException ex) {
        LOG.warn("Failed to complete snapshot " + this.snapshotId + " on backend", ex);
        throw ex;
      }

      // If uploading, wait for upload to complete
      if (uploadFuture != null) {
        LOG.debug("Waiting for upload of " + this.snapshotId + " to complete");
        if (uploadFuture.get() != null) {
          LOG.info("Uploaded " + this.snapshotId + " to object storage gateway, etag result - " + uploadFuture.get());
        } else {
          LOG.warn("Failed to upload " + this.snapshotId + " to object storage gateway failed. Check prior logs for exact errors");
          throw new EucalyptusCloudException("Failed to upload " + this.snapshotId + " to object storage gateway");
        }
      }

      // Now that the snapshot is complete, if it's a delta, wait until the
      // snapshot transfer timeout for the parent to go into a final state 
      // (good or bad). If good, declare this snapshot available.
      // If bad, of if it never competes, declare this snapshot failed. 
      // (Artifacts will be cleaned up by periodic cleanup tasks.)
      
      if (prevSnap != null) {
        final int MILLIS_PER_HOUR = 60 * 60 * 1000;
        final int timeoutMillis = StorageInfo.getStorageInfo().getSnapshotTransferTimeoutInHours()
            * MILLIS_PER_HOUR;
        final int pollPeriodMillis = 10000; // 10 sec
        int waitTimeSoFar = 0;
        
        while (waitTimeSoFar < timeoutMillis) {
          try {
            if (isSnapshotMarkedFinalized(prevSnap.getSnapshotId())) {
              break;
            }
          } catch (NoSuchElementException nsee) {
            LOG.error("Previous snapshot " + prevSnap.getSnapshotId() + 
                " no longer recorded in the database. " +
                "Setting this snapshot delta " + this.snapshotId + 
                " to 'failed' state because it requires an intact previous snapshot.");
            throw nsee; // will be caught later in this method
          } catch (TransactionException te) {
            LOG.error("General database error. " +
                "Setting this snapshot delta " + this.snapshotId + 
                " to 'failed' state because it requires an intact previous snapshot.");
            throw te; // will be caught later in this method
          }
          try {
            Thread.sleep(pollPeriodMillis);
          } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while waiting for previous snapshot to complete", e);
          }
          waitTimeSoFar += pollPeriodMillis;
        }
        if (isSnapshotMarkedFinalized(prevSnap.getSnapshotId())) {
          if (isSnapshotMarkedAvailable(prevSnap.getSnapshotId())) {
            markSnapshotAvailable();
          } else {
            markSnapshotFailed();
            LOG.error("Previous snapshot " + prevSnap.getSnapshotId() + " finalized into the " +
                prevSnap.getStatus() + " state. This snapshot delta " + this.snapshotId + 
                " set to 'failed' state because it requires an intact previous snapshot.");
          }
        } else {
          markSnapshotFailed();
          LOG.error("Previous snapshot " + prevSnap.getSnapshotId() + " never finalized after " + 
              "waiting " + (timeoutMillis / MILLIS_PER_HOUR) + " hours. " + 
              "This snapshot delta " + this.snapshotId + " set to 'failed' state because " +
              "it requires an intact previous snapshot.");
        }
      } else {
        // No previous snapshot, therefore we're done
        markSnapshotAvailable();
        LOG.debug("Snapshot " + this.snapshotId + " set to 'available' state");
      }
    } catch (Exception ex) {
      LOG.error("Failed to create snapshot " + this.snapshotId, ex);

      try {
        markSnapshotFailed();
        LOG.debug("Snapshot " + this.snapshotId + " set to 'failed' state");
      } catch (TransactionException | NoSuchElementException e) {
        LOG.warn("Cannot update snapshot " + this.snapshotId + " status to 'failed' in DB", e);
      }
    }
    LOG.trace("Finished SnapshotCreator task");
  }

  /* @return the given snapshot's info from the DB, or
   *         null if DB entry not found
   */
  private SnapshotInfo getSnapshotInfo(String snapshotId) throws TransactionException, NoSuchElementException {
    try (TransactionResource tran = Entities.transactionFor(SnapshotInfo.class)) {
      tran.setRollbackOnly();
      return Entities.uniqueResult(new SnapshotInfo(snapshotId));
    } catch (TransactionException | NoSuchElementException dbe) {
      // Database exception, toss it upstairs
      LOG.error("Database error checking for status of snapshot " + snapshotId + ": " + dbe);
      throw dbe;
    }
  }
  
  /*
   * Does a check of the snapshot's status as reflected in the DB.
   * @return true if snapshot is in the 'failed' state,
   *         false otherwise
   */
  private boolean isSnapshotMarkedFailed(String snapshotId) throws TransactionException, NoSuchElementException {
    SnapshotInfo snapshotInfo = getSnapshotInfo(snapshotId);
    if (snapshotInfo != null) {
      return StorageProperties.Status.failed.toString().equals(snapshotInfo.getStatus());
    } else {
      return false;
    }
  }

  /*
   * Does a check of the snapshot's status as reflected in the DB.
   * @return false if snapshot is not (yet) recorded in database
   *         or it's in the 'creating' or 'pending' state,
   *         true otherwise
   */
  private boolean isSnapshotMarkedFinalized(String snapshotId) throws TransactionException, NoSuchElementException {
    SnapshotInfo snapshotInfo = getSnapshotInfo(snapshotId);
    if (snapshotInfo != null) {
      return !(StorageProperties.Status.creating.toString().equals(snapshotInfo.getStatus()) ||
          StorageProperties.Status.pending.toString().equals(snapshotInfo.getStatus()));
    } else {
      return false;
    }
  }

  /*
   * Does a check of the snapshot's status as reflected in the DB.
   * @return true if snapshot is in the 'available' state,
   *         false otherwise
   */
  private boolean isSnapshotMarkedAvailable(String snapshotId) throws TransactionException, NoSuchElementException {
    SnapshotInfo snapshotInfo = getSnapshotInfo(snapshotId);
    if (snapshotInfo != null) {
      return StorageProperties.Status.available.toString().equals(snapshotInfo.getStatus());
    } else {
      return false;
    }
  }

  private void markSnapshotAvailable() throws TransactionException, NoSuchElementException {
    Function<String, SnapshotInfo> updateFunction = new Function<String, SnapshotInfo>() {
      @Override
      public SnapshotInfo apply(String arg0) {
        SnapshotInfo snap;
        try {
          snap = Entities.uniqueResult(new SnapshotInfo(arg0));
          snap.setStatus(StorageProperties.Status.available.toString());
          snap.setProgress("100");
          return snap;
        } catch (TransactionException | NoSuchElementException e) {
          LOG.error("Failed to retrieve snapshot entity from DB for " + arg0, e);
        }
        return null;
      }
    };

    Entities.asTransaction(SnapshotInfo.class, updateFunction).apply(snapshotId);
    ThruputMetrics.endOperation(MonitoredAction.CREATE_SNAPSHOT, snapshotId, System.currentTimeMillis());
  }

  private void markSnapshotFailed() throws TransactionException, NoSuchElementException {
    Function<String, SnapshotInfo> updateFunction = new Function<String, SnapshotInfo>() {

      @Override
      public SnapshotInfo apply(String arg0) {
        SnapshotInfo snap;
        try {
          snap = Entities.uniqueResult(new SnapshotInfo(arg0));
          snap.setStatus(StorageProperties.Status.failed.toString());
          snap.setProgress("0");
          return snap;
        } catch (TransactionException | NoSuchElementException e) {
          LOG.error("Failed to retrieve snapshot entity from DB for " + arg0, e);
        }
        return null;
      }
    };

    Entities.asTransaction(SnapshotInfo.class, updateFunction).apply(snapshotId);
  }

  private SnapshotInfo fetchPreviousSnapshot(int maxDeltas) throws Exception {

    SnapshotInfo prevSnapToAssign = null;
    SnapshotInfo currSnap = Transactions.find(new SnapshotInfo(snapshotId));

    try (TransactionResource tr = Entities.transactionFor(SnapshotInfo.class)) {
      
      // Find the most recent snapshot that is not in one of the states that
      // is ineligible to use for creating a snap delta.  
      SnapshotInfo prevEligibleSnapSearch = new SnapshotInfo();
      prevEligibleSnapSearch.setVolumeId(currSnap.getVolumeId());
      Criteria search = Entities.createCriteria(SnapshotInfo.class);
      search.add(Example.create(prevEligibleSnapSearch).enableLike(MatchMode.EXACT));
      search.add(Restrictions.and(StorageProperties.SNAPSHOT_DELTA_GENERATION_CRITERION, Restrictions.lt("startTime", currSnap.getStartTime())));
      search.addOrder(Order.desc("startTime"));
      search.setReadOnly(true);
      search.setMaxResults(1);  // only return the latest one

      List<SnapshotInfo> prevEligibleSnapList = (List<SnapshotInfo>) search.list();

      boolean committed = false;
      
      if (prevEligibleSnapList != null && prevEligibleSnapList.size() > 0 && 
          (prevSnapToAssign = prevEligibleSnapList.get(0)) != null) {
        // Found an eligible previous snapshot to use as a parent for this 
        // snapshot, if we make it a delta.
        if (prevSnapToAssign.getSnapshotLocation() != null && prevSnapToAssign.getIsOrigin() != null) { 
          LOG.info(this.volumeId + " has been snapshotted and uploaded before. Most recent such snapshot is " + prevSnapToAssign.getSnapshotId());

          
          // Get all the restorable snapshots for this volume, earlier than the current snapshot
          SnapshotInfo prevRestorableSnapsSearch = new SnapshotInfo();
          prevRestorableSnapsSearch.setVolumeId(currSnap.getVolumeId());
          search = Entities.createCriteria(SnapshotInfo.class);
          search.add(Example.create(prevRestorableSnapsSearch).enableLike(MatchMode.EXACT));
          search.add(Restrictions.and(StorageProperties.SNAPSHOT_DELTA_RESTORATION_CRITERION, Restrictions.lt("startTime", currSnap.getStartTime())));
          search.addOrder(Order.desc("startTime"));
          search.setReadOnly(true);
          List<SnapshotInfo> prevRestorableSnapsList = (List<SnapshotInfo>) search.list();
          tr.commit();
          committed = true;

          // Get the snap chain ending with the previous snapshot (not the current)
          List<SnapshotInfo> snapChain = blockStorageUtilSvc.getSnapshotChain(prevRestorableSnapsList, prevSnapToAssign.getSnapshotId());
          int numDeltas = 0;
          if (snapChain == null || snapChain.size() == 0) {
            // This should never happen. The chain should always include the 
            // parent (previous) snapshot we already found. But create it as a 
            // full snapshot instead of failing, to account for the unknown case
            // that might not prevent an OK full snapshot.
            LOG.error("Did not find the current snapshot's previous snapshot " + 
                prevSnapToAssign.getSnapshotId() + " in the restorable snapshots list. " +
                "The current snapshot " + currSnap.getSnapshotId() + 
                " will be created as a full snapshot.");
          } else if (snapChain.get(0).getPreviousSnapshotId() != null) {
            // This should never happen. The first snapshot in the chain
            // should always be a full snapshot. But create it as a 
            // full snapshot instead of failing, to account for the unknown case
            // that might not prevent an OK full snapshot.
            LOG.error("First snapshot " + snapChain.get(0).getSnapshotId() +
                " in the chain of " + snapChain.size() + " snapshots is not a full snapshot. The current snapshot " +
                currSnap.getSnapshotId() + " will be created as a full snapshot.");
          } else {
            numDeltas = snapChain.size() - 1;
            LOG.info(this.volumeId + " has " + numDeltas + " delta(s) since the last full checkpoint. Max limit is " + maxDeltas);
            if (numDeltas < maxDeltas) {
              return prevSnapToAssign;
            }
          }
        } else {
          LOG.info(this.volumeId + " has not been snapshotted and/or uploaded after the support for incremental snapshots was added");
        }
      } else {
        LOG.info(this.volumeId + " has no prior active snapshots in the system");
      }
      if (!committed) {
        tr.commit();
      }
    } catch (Exception e) {
      LOG.warn("Failed to look up previous snapshots for " + this.volumeId, e); // return null on exception, forces entire snapshot to get uploaded
    }
    return null;
  }

  private SnapshotInfo updateSnapshotInfo(final String prevSnapId, final String snapshotLocation) throws Exception {
    return Entities.asTransaction(SnapshotInfo.class, new Function<String, SnapshotInfo>() {

      @Override
      public SnapshotInfo apply(String arg0) {
        SnapshotInfo currSnap = null;
        SnapshotInfo prevSnap = null;
        try {
          prevSnap = Entities.uniqueResult(new SnapshotInfo(prevSnapId));
          if (!StorageProperties.DELTA_GENERATION_STATE_EXCLUSION.contains(prevSnap.getStatus())) {
            currSnap = Entities.uniqueResult(new SnapshotInfo(snapshotId));
            currSnap.setSnapshotLocation(snapshotLocation);
            currSnap.setPreviousSnapshotId(prevSnapId);
            return currSnap;
          } else {
            LOG.warn("Snapshot " + prevSnapId + " has already been marked as " + prevSnap.getStatus()
                + ". It cannot be used as the source for incremental snapshot upload of " + snapshotId);
          }
        } catch (TransactionException | NoSuchElementException e) {
          LOG.debug("Failed to update snapshot upload location and previous snapshot ID for snapshot " + snapshotId, e);
        }
        return null;
      }
    }).apply(prevSnapId);
  }

  private SnapshotInfo updateSnapshotInfo(final String snapshotLocation) throws Exception {
    return Entities.asTransaction(SnapshotInfo.class, new Function<String, SnapshotInfo>() {

      @Override
      public SnapshotInfo apply(String arg0) {
        SnapshotInfo currSnap = null;
        try {
          currSnap = Entities.uniqueResult(new SnapshotInfo(snapshotId));
          currSnap.setSnapshotLocation(snapshotLocation);
          return currSnap;
        } catch (TransactionException | NoSuchElementException e) {
          LOG.warn("Failed to update snapshot upload location for snapshot " + snapshotId, e);
        }
        return null;
      }
    }).apply(this.snapshotId);
  }
}
