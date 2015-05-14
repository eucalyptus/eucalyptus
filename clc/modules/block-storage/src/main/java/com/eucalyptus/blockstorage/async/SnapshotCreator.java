/*************************************************************************
 * Copyright 2009-2015 Eucalyptus Systems, Inc.
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
 *
 * This file may incorporate work covered under the following copyright
 * and permission notice:
 *
 *   Software License Agreement (BSD License)
 *
 *   Copyright (c) 2008, Regents of the University of California
 *   All rights reserved.
 *
 *   Redistribution and use of this software in source and binary forms,
 *   with or without modification, are permitted provided that the
 *   following conditions are met:
 *
 *     Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *     Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer
 *     in the documentation and/or other materials provided with the
 *     distribution.
 *
 *   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *   "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *   LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 *   FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 *   COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 *   INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 *   BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *   LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 *   CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 *   LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 *   ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 *   POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 *   THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 *   COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 *   AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 *   IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 *   SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 *   WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 *   REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 *   IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 *   NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/

package com.eucalyptus.blockstorage.async;

import java.util.NoSuchElementException;
import java.util.concurrent.Future;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.eucalyptus.blockstorage.LogicalStorageManager;
import com.eucalyptus.blockstorage.S3SnapshotTransfer;
import com.eucalyptus.blockstorage.SnapshotProgressCallback;
import com.eucalyptus.blockstorage.SnapshotTransfer;
import com.eucalyptus.blockstorage.StorageResource;
import com.eucalyptus.blockstorage.entities.SnapshotInfo;
import com.eucalyptus.blockstorage.entities.SnapshotTransferConfiguration;
import com.eucalyptus.blockstorage.entities.StorageInfo;
import com.eucalyptus.blockstorage.util.StorageProperties;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionException;
import com.eucalyptus.entities.TransactionResource;
import com.eucalyptus.util.EucalyptusCloudException;
import com.google.common.base.Function;

import edu.ucsb.eucalyptus.util.EucaSemaphore;
import edu.ucsb.eucalyptus.util.EucaSemaphoreDirectory;

public class SnapshotCreator implements Runnable {
  private static Logger LOG = Logger.getLogger(SnapshotCreator.class);

  private String volumeId;
  private String snapshotId;
  private String snapPointId;
  private LogicalStorageManager blockManager;

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
  }

  @Override
  public void run() {
    EucaSemaphore semaphore = EucaSemaphoreDirectory.getSolitarySemaphore(volumeId);
    try {
      Boolean shouldTransferSnapshots = true;
      StorageResource snapshotResource = null;
      SnapshotTransfer snapshotTransfer = null;
      String bucket = null;
      SnapshotProgressCallback progressCallback = null;

      // Check whether the snapshot needs to be uploaded
      shouldTransferSnapshots = StorageInfo.getStorageInfo().getShouldTransferSnapshots();

      if (shouldTransferSnapshots) {
        // Prepare for the snapshot upload (fetch credentials for snapshot upload to osg, create the bucket). Error out if this fails without
        // creating the snapshot on the blockstorage backend
        snapshotTransfer = new S3SnapshotTransfer(snapshotId, snapshotId);
        bucket = snapshotTransfer.prepareForUpload();

        if (snapshotTransfer == null || StringUtils.isBlank(bucket)) {
          throw new EucalyptusCloudException("Failed to initialize snapshot transfer mechanism for uploading " + snapshotId);
        }
      }

      // Acquire the semaphore here and release it here as well
      try {
        try {
          semaphore.acquire();
        } catch (InterruptedException ex) {
          throw new EucalyptusCloudException("Failed to create snapshot " + snapshotId + " as the semaphore could not be acquired");
        }

        // Check to ensure that a failed/cancellation has not be set
        if (!isSnapshotMarkedFailed(snapshotId)) {
          progressCallback = new SnapshotProgressCallback(snapshotId); // Setup the progress callback, that should start the progress
          snapshotResource = blockManager.createSnapshot(this.volumeId, this.snapshotId, this.snapPointId, shouldTransferSnapshots);
          progressCallback.updateBackendProgress(50); // to indicate that backend snapshot process is 50% done
        } else {
          throw new EucalyptusCloudException("Snapshot " + this.snapshotId + " marked as failed by another thread");
        }
      } finally {
        semaphore.release();
      }

      Future<String> uploadFuture = null;
      if (shouldTransferSnapshots) {
        if (snapshotResource == null) {
          throw new EucalyptusCloudException("Snapshot file unknown. Cannot transfer snapshot");
        }

        // Update snapshot location in database
        String snapshotLocation = SnapshotInfo.generateSnapshotLocationURI(SnapshotTransferConfiguration.OSG, bucket, snapshotId);
        Entities.asTransaction(SnapshotInfo.class, new Function<String, SnapshotInfo>() {

          @Override
          public SnapshotInfo apply(String arg0) {
            SnapshotInfo snapshotInfo = null;
            try {
              snapshotInfo = Entities.uniqueResult(new SnapshotInfo(snapshotId));
              snapshotInfo.setSnapshotLocation(arg0);
            } catch (TransactionException | NoSuchElementException e) {
              LOG.debug("Failed to update upload location for snapshot " + snapshotId + ". Skipping it and moving on", e);
            }
            return snapshotInfo;
          }
        }).apply(snapshotLocation);

        if (!isSnapshotMarkedFailed(snapshotId)) {
          try {
            uploadFuture = snapshotTransfer.upload(snapshotResource, progressCallback);
          } catch (Exception e) {
            throw new EucalyptusCloudException("Failed to upload snapshot " + snapshotId + " to objectstorage", e);
          }
        } else {
          throw new EucalyptusCloudException("Snapshot " + this.snapshotId + " marked as failed by another thread");
        }
      } else {
        // Snapshot does not have to be transferred
      }

      // finish the snapshot on backend - sever iscsi connection, disconnect and wait for it to complete
      try {
        LOG.debug("Finishing up " + snapshotId + " on block storage backend");
        blockManager.finishVolume(snapshotId);
        LOG.info("Finished creating " + snapshotId + " on block storage backend");
        progressCallback.updateBackendProgress(50); // to indicate that backend snapshot process is 100% done
      } catch (EucalyptusCloudException ex) {
        LOG.warn("Failed to complete snapshot " + snapshotId + " on backend", ex);
        throw ex;
      }

      // If uploading, wait for upload to complete
      if (uploadFuture != null) {
        LOG.debug("Waiting for upload of " + snapshotId + " to complete");
        if (uploadFuture.get() != null) {
          LOG.info("Uploaded " + snapshotId + " to object storage gateway, etag result - " + uploadFuture.get());
        } else {
          LOG.warn("Failed to upload " + snapshotId + " to object storage gateway failed. Check prior logs for exact errors");
          throw new EucalyptusCloudException("Failed to upload " + snapshotId + " to object storage gateway");
        }
      }

      // Mark snapshot as available
      markSnapshotAvailable();
    } catch (Exception ex) {
      LOG.error("Failed to create snapshot " + snapshotId, ex);

      try {
        markSnapshotFailed();
      } catch (TransactionException | NoSuchElementException e) {
        LOG.warn("Cannot update " + snapshotId + " status to failed on SC", e);
      }
    }
  }

  /*
   * Does a check of the snapshot's status as reflected in the DB.
   */
  private boolean isSnapshotMarkedFailed(String snapshotId) {
    try (TransactionResource tran = Entities.transactionFor(SnapshotInfo.class)) {
      tran.setRollbackOnly();
      SnapshotInfo snap = Entities.uniqueResult(new SnapshotInfo(snapshotId));
      if (snap != null && StorageProperties.Status.failed.toString().equals(snap.getStatus())) {
        return true;
      }
    } catch (Exception e) {
      LOG.error("Error determining status of snapshot " + snapshotId);
    }
    return false;
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
          snap.setSnapPointId(null);
          return snap;
        } catch (TransactionException | NoSuchElementException e) {
          LOG.error("Failed to retrieve snapshot entity from DB for " + arg0, e);
        }
        return null;
      }
    };

    Entities.asTransaction(SnapshotInfo.class, updateFunction).apply(snapshotId);
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
}
