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

import java.io.File;
import java.util.Date;
import java.util.List;
import java.util.Random;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.criterion.Example;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;

import com.eucalyptus.blockstorage.FileResource;
import com.eucalyptus.blockstorage.LogicalStorageManager;
import com.eucalyptus.blockstorage.S3SnapshotTransfer;
import com.eucalyptus.blockstorage.SnapshotTransfer;
import com.eucalyptus.blockstorage.StorageResource;
import com.eucalyptus.blockstorage.entities.SnapshotInfo;
import com.eucalyptus.blockstorage.entities.VolumeInfo;
import com.eucalyptus.blockstorage.util.StorageProperties;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionResource;
import com.eucalyptus.entities.Transactions;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.metrics.MonitoredAction;
import com.eucalyptus.util.metrics.ThruputMetrics;

import edu.ucsb.eucalyptus.util.EucaSemaphore;
import edu.ucsb.eucalyptus.util.EucaSemaphoreDirectory;
import edu.ucsb.eucalyptus.util.SystemUtil;
import edu.ucsb.eucalyptus.util.SystemUtil.CommandOutput;

public class VolumeCreator implements Runnable {
  private static Logger LOG = Logger.getLogger(VolumeCreator.class);
  private static Random randomGenerator = new Random();

  private String volumeId;
  private String snapshotId;
  private String parentVolumeId;
  private int size;
  private LogicalStorageManager blockManager;

  public VolumeCreator(String volumeId, String snapshotSetName, String snapshotId, String parentVolumeId, int size, LogicalStorageManager blockManager) {
    this.volumeId = volumeId;
    this.snapshotId = snapshotId;
    this.parentVolumeId = parentVolumeId;
    this.size = size;
    this.blockManager = blockManager;
  }

  @Override
  public void run() {
    boolean success = true;
    if (snapshotId != null) {
      try {
        SnapshotInfo snapshotInfo = new SnapshotInfo(snapshotId);
        List<SnapshotInfo> foundSnapshotInfos = Transactions.findAll(snapshotInfo);

        if (foundSnapshotInfos.size() == 0) {// SC *may not* have a database record for the snapshot and or the actual snapshot

          EucaSemaphore semaphore = EucaSemaphoreDirectory.getSolitarySemaphore(snapshotId);
          try {
            semaphore.acquire(); // Get the semaphore to avoid concurrent access by multiple threads
            foundSnapshotInfos = Transactions.findAll(snapshotInfo); // Check if another thread setup the snapshot

            if (foundSnapshotInfos.size() == 0) { // SC does not have a database record for the snapshot
              SnapshotInfo firstSnap = null;

              // Search for the snapshots on other clusters in the ascending order of creation time stamp and get the first one
              snapshotInfo.setScName(null);
              try (TransactionResource tr = Entities.transactionFor(SnapshotInfo.class)) {
                Criteria snapCriteria =
                    Entities.createCriteria(SnapshotInfo.class).setReadOnly(true).setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY)
                        .setCacheable(true).add(Example.create(snapshotInfo).enableLike(MatchMode.EXACT)).addOrder(Order.asc("creationTimestamp"));
                foundSnapshotInfos = (List<SnapshotInfo>) snapCriteria.list();
                tr.commit();
              }

              if (foundSnapshotInfos != null && foundSnapshotInfos.size() > 0) {
                firstSnap = foundSnapshotInfos.get(0);
              } else {
                throw new EucalyptusCloudException("No record of snapshot " + snapshotId + " on any SC");
              }

              // If size was not found in database, bail out. Can't create a snapshot without the size
              if (firstSnap.getSizeGb() == null || firstSnap.getSizeGb() <= 0) {
                throw new EucalyptusCloudException("Snapshot size for " + snapshotId
                    + " is unknown. Cannot prep snapshot holder on the storage backend");
              }

              // Check for the snpahsot on the storage backend. Clusters/zones/partitions may be connected to the same storage backend in
              // which case snapshot does not have to be downloaded from ObjectStorage.
              if (!blockManager.getFromBackend(snapshotId, firstSnap.getSizeGb())) { // Storage backend does not contain
                                                                                     // snapshot. Download snapshot
                // from OSG
                LOG.debug(snapshotId + " not found on storage backend. Will attempt to download from objectstorage gateway");

                String bucket = null;
                String key = null;

                if (StringUtils.isBlank(firstSnap.getSnapshotLocation())) {
                  throw new EucalyptusCloudException("Snapshot location (bucket, key) for " + snapshotId
                      + " is unknown. Cannot download snapshot from objectstorage.");
                }
                String[] names = SnapshotInfo.getSnapshotBucketKeyNames(firstSnap.getSnapshotLocation());
                bucket = names[0];
                key = names[1];
                if (StringUtils.isBlank(bucket) || StringUtils.isBlank(key)) {
                  throw new EucalyptusCloudException("Failed to parse bucket and key information for downloading " + snapshotId
                      + ". Cannot download snapshot from objectstorage.");
                }

                // Try to fetch the snapshot size before preparing the snapshot holder on the backend. If size is unavailable, the snapshot
                // must be downloaded, unzipped and measured before creating the snapshot holder on the backend. Some SANs (Equallogic) add
                // arbitrary amount of writable space to the lun and hence the exact size of the snapshot is required for preparing the
                // holder on the backend
                SnapshotTransfer snapshotTransfer = new S3SnapshotTransfer(snapshotId, bucket, key);
                Long actualSizeInBytes = null;
                try {
                  actualSizeInBytes = snapshotTransfer.getSizeInBytes();
                } catch (Exception e) {
                  LOG.debug("Snapshot size not found", e);
                }

                if (actualSizeInBytes == null) { // Download the snapshot from OSG and find out the size

                  String tmpSnapshotFileName = null;
                  try {
                    tmpSnapshotFileName = downloadSnapshotToTempFile(snapshotTransfer);

                    File snapFile = new File(tmpSnapshotFileName);
                    if (!snapFile.exists()) {
                      throw new EucalyptusCloudException("Unable to find snapshot " + snapshotId + "on SC");
                    }

                    // TODO add snapshot size to osg object metadata

                    long actualSnapSizeInMB = (long) Math.ceil((double) snapFile.length() / StorageProperties.MB);

                    try {
                      // Allocates the necessary resources on the backend
                      StorageResource storageResource = blockManager.prepareSnapshot(snapshotId, firstSnap.getSizeGb(), actualSnapSizeInMB);

                      if (storageResource != null) {
                        // Check if the destination is a block device
                        if (storageResource.getPath().startsWith("/dev/")) {
                          CommandOutput output =
                              SystemUtil.runWithRawOutput(new String[] {StorageProperties.EUCA_ROOT_WRAPPER, "dd", "if=" + tmpSnapshotFileName,
                                  "of=" + storageResource.getPath(), "bs=" + StorageProperties.blockSize});
                          LOG.debug("Output of dd command: " + output.error);
                          if (output.returnValue != 0) {
                            throw new EucalyptusCloudException("Failed to copy the snapshot to the right location due to: " + output.error);
                          }
                          cleanupFile(tmpSnapshotFileName);
                        } else {
                          // Rename file
                          if (!snapFile.renameTo(new File(storageResource.getPath()))) {
                            throw new EucalyptusCloudException("Failed to rename the snapshot");
                          }
                        }

                        // Finish the snapshot
                        blockManager.finishVolume(snapshotId);
                      } else {
                        LOG.warn("Block Manager replied that " + snapshotId
                            + " not on backend, but snapshot preparation indicated that the snapshot is already present");
                      }
                    } catch (Exception ex) {
                      LOG.error("Failed to prepare the snapshot " + snapshotId + " on storage backend. Cleaning up the snapshot on backend", ex);
                      cleanFailedSnapshot(snapshotId);
                      throw ex;
                    }
                  } catch (Exception ex) {
                    LOG.error("Failed to prepare the snapshot " + snapshotId + " on the storage backend. Cleaning up the snapshot on SC", ex);
                    cleanupFile(tmpSnapshotFileName);
                    throw ex;
                  }
                } else { // Prepare the snapshot holder and download the snapshot directly to it

                  long actualSnapSizeInMB = (long) Math.ceil((double) actualSizeInBytes / StorageProperties.MB);

                  try {
                    // Allocates the necessary resources on the backend
                    StorageResource storageResource = blockManager.prepareSnapshot(snapshotId, firstSnap.getSizeGb(), actualSnapSizeInMB);

                    if (storageResource != null) {
                      // Download the snapshot to the destination
                      snapshotTransfer.download(storageResource);

                      // Finish the snapshot
                      blockManager.finishVolume(snapshotId);
                    } else {
                      LOG.warn("Block Manager replied that " + snapshotId
                          + " not on backend, but snapshot preparation indicated that the snapshot is already present");
                    }
                  } catch (Exception ex) {
                    LOG.error("Failed to prepare the snapshot " + snapshotId + " on storage backend. Cleaning up the snapshot on backend", ex);
                    cleanFailedSnapshot(snapshotId);
                    throw ex;
                  }
                }

              } else { // Storage backend contains snapshot
                // Just create a record of it for this partition in the DB and get going!
                LOG.debug(snapshotId + " found on storage backend");
              }

              // Create a snapshot record for this SC
              try (TransactionResource tr = Entities.transactionFor(SnapshotInfo.class)) {
                snapshotInfo = copySnapshotInfo(firstSnap);
                snapshotInfo.setProgress("100");
                snapshotInfo.setStartTime(new Date());
                snapshotInfo.setStatus(StorageProperties.Status.available.toString());
                Entities.persist(snapshotInfo);
                tr.commit();
              }
            } else { // SC has a database record for the snapshot
              // This condition is hit when concurrent threads compete to create a volume from a snapshot that did not exist on this SC. One
              // of the concurrent threads may have finished the snapshot prep there by making it available to all other threads
              SnapshotInfo foundSnapshotInfo = foundSnapshotInfos.get(0);
              if (!StorageProperties.Status.available.toString().equals(foundSnapshotInfo.getStatus())) {
                success = false;
                LOG.warn("snapshot " + foundSnapshotInfo.getSnapshotId() + " not available.");
              } else {
                // Do NOT create the volume here as this is synchronized block. Snapshot prepping has to be synchronized, volume
                // creation can be done in parallel
              }
            }
          } catch (InterruptedException ex) {
            throw new EucalyptusCloudException("semaphore could not be acquired");
          } finally {
            try {
              semaphore.release();
            } finally {
              EucaSemaphoreDirectory.removeSemaphore(snapshotId);
            }
          }

          // Create the volume from the snapshot, this can happen in parallel.
          if (success) {
            size = blockManager.createVolume(volumeId, snapshotId, size);
          }
        } else { // SC has a database record for the snapshot
          // Repeated logic, fix it!
          SnapshotInfo foundSnapshotInfo = foundSnapshotInfos.get(0);
          if (!StorageProperties.Status.available.toString().equals(foundSnapshotInfo.getStatus())) {
            success = false;
            LOG.warn("snapshot " + foundSnapshotInfo.getSnapshotId() + " not available.");
          } else {
            size = blockManager.createVolume(volumeId, snapshotId, size);
          }
        }
      } catch (Exception ex) {
        success = false;
        LOG.error("Failed to create volume " + volumeId, ex);
      }
    } else { // Not a snapshot-based volume create.
      try {
        if (parentVolumeId != null) {
          // Clone the parent volume.
          blockManager.cloneVolume(volumeId, parentVolumeId);
        } else {
          // Create a regular empty volume
          blockManager.createVolume(volumeId, size);
        }
      } catch (Exception ex) {
        success = false;
        LOG.error("Failed to create volume " + volumeId, ex);
      }
    }

    // Update database record for the volume.
    VolumeInfo volumeInfo = new VolumeInfo(volumeId);
    try (TransactionResource tr = Entities.transactionFor(VolumeInfo.class)) {
      VolumeInfo foundVolumeInfo = Entities.uniqueResult(volumeInfo);
      if (foundVolumeInfo != null) {
        if (success) {
          foundVolumeInfo.setStatus(StorageProperties.Status.available.toString());
          ThruputMetrics.endOperation(snapshotId != null ? MonitoredAction.CREATE_VOLUME_FROM_SNAPSHOT : MonitoredAction.CREATE_VOLUME,
              volumeId, System.currentTimeMillis());
        } else {
          foundVolumeInfo.setStatus(StorageProperties.Status.failed.toString());
        }
        if (snapshotId != null) {
          foundVolumeInfo.setSize(size);
        }
      } else {
        LOG.error("VolumeInfo entity for volume id " + volumeId + " was not found in the database");
      }
      tr.commit();
    } catch (Exception e) {
      LOG.error("Failed to update VolumeInfo entity for volume id " + volumeId + " in the database", e);
    }
  }

  // DO NOT throw any exceptions from cleaning routines. Log the errors and move on
  private void cleanFailedSnapshot(String snapshotId) {
    if (snapshotId == null)
      return;
    LOG.debug("Disconnecting and cleaning local snapshot after failed snapshot transfer: " + snapshotId);
    try {
      blockManager.finishVolume(snapshotId);
    } catch (Exception e) {
      LOG.error("Error finishing failed snapshot " + snapshotId, e);
    } finally {
      try {
        blockManager.cleanSnapshot(snapshotId);
      } catch (Exception e) {
        LOG.error("Error deleting failed snapshot " + snapshotId, e);
      }
    }
  }

  private SnapshotInfo copySnapshotInfo(SnapshotInfo source) {
    SnapshotInfo copy = new SnapshotInfo(source.getSnapshotId());
    copy.setSizeGb(source.getSizeGb());
    copy.setSnapshotLocation(source.getSnapshotLocation());
    copy.setUserName(source.getUserName());
    copy.setVolumeId(source.getVolumeId());
    return copy;
  }

  private String downloadSnapshotToTempFile(SnapshotTransfer snapshotTransfer) throws EucalyptusCloudException {

    String tmpUncompressedFileName = null;
    File tmpUncompressedFile = null;
    int retry = 0;
    int maxRetry = 5;

    do {
      tmpUncompressedFileName =
          StorageProperties.storageRootDirectory + File.separator + snapshotId + "-" + String.valueOf(randomGenerator.nextInt());
      tmpUncompressedFile = new File(tmpUncompressedFileName);
    } while (tmpUncompressedFile.exists() && retry++ < maxRetry);

    // This should be *very* rare
    if (retry >= maxRetry) {
      // Nothing to clean up at this point
      throw new EucalyptusCloudException("Could not get a temporary file for snapshot " + snapshotId + " download after " + maxRetry + " attempts");
    }

    // Download the snapshot from OSG
    try {
      snapshotTransfer.download(new FileResource(snapshotId, tmpUncompressedFileName));
    } catch (Exception ex) {
      // Cleanup
      cleanupFile(tmpUncompressedFile);
      throw new EucalyptusCloudException("Failed to download snapshot " + snapshotId + " from objectstorage", ex);
    }

    return tmpUncompressedFileName;
  }

  private void cleanupFile(String fileName) {
    try {
      cleanupFile(new File(fileName));
    } catch (Exception e) {
      LOG.error("Failed to delete file", e);
    }
  }

  private void cleanupFile(File file) {
    if (file != null && file.exists()) {
      try {
        file.delete();
      } catch (Exception e) {
        LOG.error("Failed to delete file", e);
      }
    }
  }
}
