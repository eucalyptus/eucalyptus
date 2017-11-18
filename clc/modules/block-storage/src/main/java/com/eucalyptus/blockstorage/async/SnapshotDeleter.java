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

import java.util.concurrent.ConcurrentHashMap;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.criterion.Example;
import org.hibernate.criterion.MatchMode;

import com.eucalyptus.blockstorage.LogicalStorageManager;
import com.eucalyptus.blockstorage.S3SnapshotTransfer;
import com.eucalyptus.blockstorage.entities.SnapshotInfo;
import com.eucalyptus.blockstorage.entities.SnapshotInfo_;
import com.eucalyptus.blockstorage.util.StorageProperties;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionResource;
import com.eucalyptus.entities.Transactions;
import com.eucalyptus.storage.common.CheckerTask;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.metrics.MonitoredAction;
import com.eucalyptus.util.metrics.ThruputMetrics;

import com.eucalyptus.blockstorage.util.EucaSemaphore;
import com.eucalyptus.blockstorage.util.EucaSemaphoreDirectory;

/**
 * Checker task for removing snapshots marked in deleting status
 * 
 * @author Swathi Gangisetty
 *
 */
public class SnapshotDeleter extends CheckerTask {

  private static Logger LOG = Logger.getLogger(SnapshotDeleter.class);

  private static ConcurrentHashMap<String, Date> retryMap = new ConcurrentHashMap<String, Date>();

  // Retry for two days max
  // TODO make this configurable?
  private static final long RETRY_MAX_MILLIS = (2/*days*/ * 24 * 60 * 60 * 1000)/*millisecs*/;
  private static final long RETRY_MAX_MINUTES = RETRY_MAX_MILLIS / 1000 / 60;
  private static final long RETRY_MAX_HOURS = RETRY_MAX_MINUTES / 60;

  private LogicalStorageManager blockManager;
  private S3SnapshotTransfer snapshotTransfer;

  public SnapshotDeleter(LogicalStorageManager blockManager) {
    this.name = SnapshotDeleter.class.getSimpleName();
    this.runInterval = 30; // runs every 30 seconds, TODO make this configurable?
    this.blockManager = blockManager;
  }

  public SnapshotDeleter(LogicalStorageManager blockManager, S3SnapshotTransfer mock) {
    this(blockManager);
    this.snapshotTransfer = mock;
  }

  @Override
  public void run() {
    // Clean up on EBS backend
    deleteFromEBS();
    // Clean up on OSG
    deleteFromOSG();
  }

  private void deleteFromEBS() {
    try {
      SnapshotInfo searchSnap = new SnapshotInfo();
      searchSnap.setStatus(StorageProperties.Status.deleting.toString());
      List<SnapshotInfo> snapshotsToBeDeleted = null;
      try {
        snapshotsToBeDeleted = Transactions.findAll(searchSnap);
      } catch (Exception e) {
        LOG.warn("Failed to lookup snapshots marked for deletion", e);
        return;
      }
      if (snapshotsToBeDeleted != null && !snapshotsToBeDeleted.isEmpty()) {
        LOG.trace("Deleting snapshots from EBS");
        for (SnapshotInfo snap : snapshotsToBeDeleted) {
          try {
            String snapshotId = snap.getSnapshotId();
            LOG.debug("Snapshot " + snapshotId + " was marked for deletion from EBS backend. Evaluating prerequisites for cleanup...");

            if (snap.getIsOrigin() != null && snap.getIsOrigin()) { // check if snapshot originates in this az
              // acquire semaphore before deleting to avoid concurrent interaction with delta creation process
              LOG.debug("Snapshot " + snapshotId + " originates from this az, acquire semaphore before deletion");
              EucaSemaphore snapSemaphore = EucaSemaphoreDirectory.getSolitarySemaphore(snapshotId);
              try {
                try {
                  snapSemaphore.acquire();
                } catch (InterruptedException ex) {
                  LOG.warn("Cannot process deletion of " + snapshotId + " due to an error acquiring semaphore. Will retry again later");
                  continue;
                }
                deleteSnapFromEBS(snap);
              } finally {
                snapSemaphore.release();
                EucaSemaphoreDirectory.removeSemaphore(snapshotId);
              }
            } else { // either pre 4.4 snapshot or snapshot does not originate in this az
              // no need to acquire semaphore, delete straight away
              deleteSnapFromEBS(snap);
            }
          } catch (Exception e) {
            LOG.warn("Failed to process deletion for " + snap.getSnapshotId() + " on EBS backend", e);
            continue;
          } finally {
            ThruputMetrics.endOperation(MonitoredAction.DELETE_SNAPSHOT, snap.getSnapshotId(), System.currentTimeMillis());
          }
        }
      } else {
        LOG.trace("No snapshots marked for deletion");
      }
    } catch (Exception e) { // could catch InterruptedException
      LOG.warn("Unable to remove snapshots marked for deletion from EBS backend", e);
      return;
    }
  }

  private void deleteFromOSG() {
    try {
      // Get the snapshots that are deleted from EBS but not yet deleted from OSG, 
      // in reverse time order so we never try to delete a parent before a child
      List<SnapshotInfo> snapshotsToBeDeleted = null;
      try (TransactionResource tr = Entities.transactionFor(SnapshotInfo.class)) {
        snapshotsToBeDeleted = Entities.criteriaQuery(
            Entities.restriction(SnapshotInfo.class)
            .equal(SnapshotInfo_.status, StorageProperties.Status.deletedfromebs.toString()).build())
            .orderByDesc(SnapshotInfo_.startTime)
            .list();
        tr.commit();
      } catch (Exception e) {
        LOG.warn("Failed database lookup of snapshots marked for deletion from OSG", e);
        return;
      }

      if (snapshotsToBeDeleted != null && !snapshotsToBeDeleted.isEmpty()) {
        LOG.trace("Deleting snapshots from OSG");
        for (SnapshotInfo snap : snapshotsToBeDeleted) {
          try {
            String snapshotId = snap.getSnapshotId();

            LOG.debug("Snapshot " + snapshotId + " was marked for deletion from OSG. Evaluating prerequisites for cleanup...");
            if (snap.getIsOrigin() == null) { // old snapshot prior to 4.4
              LOG.debug("Snapshot " + snapshotId + " may have been created prior to incremental snapshot support");
              deleteSnapFromOSG(snap); // delete snapshot
            } else if (snap.getIsOrigin()) { // snapshot originated in the same az
              LOG.debug("Snapshot " + snapshotId + " originates from this az, verifying if it's needed to restore other snapshots");
              List<SnapshotInfo> nextSnaps = null;
              try (TransactionResource tr = Entities.transactionFor(SnapshotInfo.class)) {

                SnapshotInfo nextSnapSearch = new SnapshotInfo();
                nextSnapSearch.setScName(snap.getScName());
                nextSnapSearch.setVolumeId(snap.getVolumeId());
                nextSnapSearch.setIsOrigin(Boolean.TRUE);
                nextSnapSearch.setPreviousSnapshotId(snap.getSnapshotId());
                Criteria search = Entities.createCriteria(SnapshotInfo.class);
                search.add(Example.create(nextSnapSearch).enableLike(MatchMode.EXACT));
                search.add(StorageProperties.SNAPSHOT_DELTA_RESTORATION_CRITERION);
                search.setReadOnly(true);

                nextSnaps = (List<SnapshotInfo>) search.list();
                tr.commit();
              } catch (Exception e) {
                LOG.warn("Failed to lookup snapshots that may depend on " + snapshotId + " for reconstruction", e);
                return;
              }

              if (nextSnaps != null && !nextSnaps.isEmpty()) {
                // Found deltas that might depend on this snapshot for reconstruction, don't delete.
                // Normally there will be only 1 next snap, optimize for that case.
                String nextSnapIds = nextSnaps.get(0).getSnapshotId();
                if (nextSnaps.size() > 1) {
                  for (int nextSnapIdNum = 1; nextSnapIdNum < nextSnaps.size(); nextSnapIdNum++) {
                    nextSnapIds = nextSnapIds + ", " + nextSnaps.get(nextSnapIdNum).getSnapshotId();
                  }
                }
                LOG.debug("Snapshot " + snapshotId + " is required for restoring other snapshots in the system." +
                    " Cannot delete from OSG. Direct children of this snapshot: " + nextSnapIds);
              } else {
                LOG.debug("Snapshot " + snapshotId + " is not required for restoring other snapshots in the system");
                deleteSnapFromOSG(snap); // delete snapshot
              }
            } else { // snapshot originated in a different az
              // skip evaluation and just mark the snapshot deleted, let the source az deal with the osg remnants TODO fix this later
              LOG.debug("Snapshot " + snapshotId + " orignated from a different az, let the source az deal with deletion from OSG");
              markSnapDeleted(snapshotId);
            }
          } catch (Exception e) {
            LOG.warn("Failed to process deletion for " + snap.getSnapshotId() + " on ObjectStorageGateway", e);
            continue;
          }
        }
      } else {
        LOG.trace("No snapshots marked for deletion from OSG");
      }
    } catch (Exception e) { // could catch InterruptedException
      LOG.warn("Unable to remove snapshots marked for deletion from OSG", e);
      return;
    }
  }

  private void deleteSnapFromEBS(SnapshotInfo snap) {
    String snapshotId = snap.getSnapshotId();
    LOG.debug("Deleting snapshot " + snapshotId + " from EBS backend...");

    try {
      blockManager.deleteSnapshot(snapshotId, snap.getSnapPointId());
    } catch (EucalyptusCloudException e) {
      LOG.warn("Unable to delete " + snapshotId + " from EBS backend. Will retry later", e);
      return;
    }

    if (StringUtils.isNotBlank(snap.getSnapshotLocation())) {
      // snapshot removal from s3 needs evaluation
      markSnapDeletedFromEBS(snapshotId);
      LOG.debug("Snapshot " + snapshotId + " set to 'deletedfromebs' state from EBS cleanup");
    } else {
      // no evidence of snapshot upload to OSG, mark the snapshot as deleted
      markSnapDeleted(snapshotId);
      LOG.debug("Snapshot " + snapshotId + " set to 'deleted' state from EBS cleanup");
    }
  }

  private void deleteSnapFromOSG(SnapshotInfo snap) {
    if (StringUtils.isNotBlank(snap.getSnapshotLocation())) {
      LOG.debug("Deleting snapshot " + snap.getSnapshotId() + " from ObjectStorageGateway");
      try {
        String[] names = SnapshotInfo.getSnapshotBucketKeyNames(snap.getSnapshotLocation());
        if (snapshotTransfer == null) {
          snapshotTransfer = new S3SnapshotTransfer();
        }
        snapshotTransfer.setSnapshotId(snap.getSnapshotId());
        snapshotTransfer.setBucketName(names[0]);
        snapshotTransfer.setKeyName(names[1]);
        snapshotTransfer.delete();

        markSnapDeleted(snap.getSnapshotId());
      } catch (Exception e) {
        LOG.info("Failed to delete snapshot " + snap.getSnapshotId() + " from ObjectStorageGateway.", e);
        retryOrDelete(snap.getSnapshotId(), /*fromEBS*/ false, "from OSG");
        return;
      }
    } else {
      LOG.debug("Snapshot location missing for " + snap.getSnapshotId() + 
          ". It may be created later. Skipping deletion from ObjectStorageGateway for now.");
      retryOrDelete(snap.getSnapshotId(), /*fromEBS*/ false, "from OSG");
    }
  }

  private void markSnapDeleted(String snapshotId) {
    try (TransactionResource tran = Entities.transactionFor(SnapshotInfo.class)) {
      SnapshotInfo foundSnapshotInfo = Entities.uniqueResult(new SnapshotInfo(snapshotId));
      foundSnapshotInfo.setStatus(StorageProperties.Status.deleted.toString());
      foundSnapshotInfo.setDeletionTime(new Date());
      tran.commit();
      // Only remove it from the retry list (OK if it's not there) if we
      // successfully set the snap to deleted. Otherwise it may be put back
      // on the list on the next run and start a retry period again.
      retryMap.remove(snapshotId);
      LOG.debug("Snapshot " + snapshotId + " set to 'deleted' state");
    } catch (Exception e) {
      LOG.warn("Failed to update status for " + snapshotId + " to deleted", e);
    }
  }

  private void markSnapDeletedFromEBS(String snapshotId) {
    try (TransactionResource tran = Entities.transactionFor(SnapshotInfo.class)) {
      SnapshotInfo foundSnapshotInfo = Entities.uniqueResult(new SnapshotInfo(snapshotId));
      foundSnapshotInfo.setStatus(StorageProperties.Status.deletedfromebs.toString());
      tran.commit();
    } catch (Exception e) {
      LOG.warn("Failed to update status for " + snapshotId + " to deletedfromebs", e);
    }
  }
  
  private boolean retryOrDelete(String snapshotId, boolean fromEBS, String fromEBSorOSG) {
    Date retryStartDate = null;
    Date now = new Date();
    boolean timedOut = false;
    if ((retryStartDate = retryMap.get(snapshotId)) == null) {
      LOG.warn("Deleting snapshot " + snapshotId + " " + fromEBSorOSG + " failed. Will retry until " + 
          new Date(now.getTime() + RETRY_MAX_MILLIS));
      retryMap.put(snapshotId, now);
    } else {
      long retryStartMillis = retryStartDate.getTime();
      if (now.getTime() - retryStartMillis > RETRY_MAX_MILLIS) {
        timedOut = true;
        // Compiler warns of "dead code" here due to constants.
        // Kept here so we can change RETRY_MAX_MILLIS for test purposes.
        String retryMaxHumanReadable = (RETRY_MAX_HOURS < 1 ? 
            (RETRY_MAX_MINUTES + " minutes") : (RETRY_MAX_HOURS + " hours"));
        LOG.error("Deleting snapshot " + snapshotId + " " + fromEBSorOSG + " failed on its final attempt, after " + 
            retryMaxHumanReadable + 
            ". Giving up trying to delete it " + fromEBSorOSG);
        try {
          if (fromEBS) {
            markSnapDeletedFromEBS(snapshotId);
          } else {
            markSnapDeleted(snapshotId);
          }
        } catch (Exception e2) {
          LOG.info("Snapshot " + snapshotId + " could not be deleted " + fromEBSorOSG + 
              ". It may have already been deleted in another thread.");
        }
      } else {
        LOG.warn("Deleting snapshot " + snapshotId + " " + fromEBSorOSG + " failed again. Will retry until " + 
            new Date(retryStartMillis + RETRY_MAX_MILLIS));
      }
    }
    return timedOut;
  }
  
}
