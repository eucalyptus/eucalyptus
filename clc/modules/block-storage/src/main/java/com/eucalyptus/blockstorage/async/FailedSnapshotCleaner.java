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

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.NoSuchElementException;

import org.apache.log4j.Logger;

import com.eucalyptus.blockstorage.LogicalStorageManager;
import com.eucalyptus.blockstorage.S3SnapshotTransfer;
import com.eucalyptus.blockstorage.entities.SnapshotInfo;
import com.eucalyptus.blockstorage.util.BlockStorageUtil;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionException;
import com.eucalyptus.entities.TransactionResource;
import com.eucalyptus.storage.common.CheckerTask;
import com.google.common.base.Strings;

/**
 * Checker task for cleaning snapshots marked in failed status
 * 
 * @author Swathi Gangisetty
 *
 */
public class FailedSnapshotCleaner extends CheckerTask {

  private static Logger LOG = Logger.getLogger(FailedSnapshotCleaner.class);

  private LogicalStorageManager blockManager;
  private S3SnapshotTransfer snapshotTransfer;

  public FailedSnapshotCleaner(LogicalStorageManager blockManager) {
    this.name = FailedSnapshotCleaner.class.getSimpleName();
    this.runInterval = 2 * 60; // runs every 2 minutes, make this configurable?
    this.blockManager = blockManager;
  }

  public FailedSnapshotCleaner(LogicalStorageManager blockManager, S3SnapshotTransfer mock) {
    this(blockManager);
    this.snapshotTransfer = mock;
  }

  @Override
  public void run() {
    try {
      List<SnapshotInfo> snapshotInfos = null;
      try (TransactionResource tr = Entities.transactionFor(SnapshotInfo.class)) {
        snapshotInfos = Entities.query(new SnapshotInfo(), Boolean.TRUE, BlockStorageUtil.getFailedCriterion(), Collections.EMPTY_MAP);
        tr.commit();
      } catch (Exception e) {
        LOG.warn("Unable to query database for failed snapshots", e);
        return;
      }

      if (snapshotInfos != null && !snapshotInfos.isEmpty()) {
        for (SnapshotInfo snapshotInfo : snapshotInfos) {
          cleanSnapshot(snapshotInfo);
        }
      } else {
        LOG.trace("No failed snapshots to clean");
      }
    } catch (Exception e) { // could catch InterruptedException
      LOG.warn("Unable to clean failed snapshots", e);
      return;
    }
  }

  private void cleanSnapshot(SnapshotInfo snapInfo) {
    String snapshotId = snapInfo.getSnapshotId();
    LOG.info("Cleaning up failed snapshot " + snapshotId);

    // Disconnect snapshot from SC
    try {
      LOG.info("Disconnecting snapshot " + snapshotId + " from the Storage Controller");
      blockManager.finishVolume(snapshotId);
    } catch (Exception e) {
      LOG.debug("Attempt to disconnect snapshot " + snapshotId + " from Storage Controller failed because: " + e.getMessage());
    }

    // Delete snapshot from backend
    try {
      LOG.info("Cleaning snapshot " + snapshotId + " on storage backend");
      blockManager.cleanSnapshot(snapshotId, snapInfo.getSnapPointId());
    } catch (Exception e) {
      LOG.debug("Attempt to clean snapshot " + snapshotId + " on storage backend failed because: " + e.getMessage());
    }

    // Delete snapshot from OSG
    try {
      if (!Strings.isNullOrEmpty(snapInfo.getSnapshotLocation())) {
        LOG.info("Deleting snapshot " + snapshotId + " from objectsotrage");
        if (snapshotTransfer == null) {
          snapshotTransfer = new S3SnapshotTransfer();
        }
        String[] names = SnapshotInfo.getSnapshotBucketKeyNames(snapInfo.getSnapshotLocation());
        snapshotTransfer.setSnapshotId(snapshotId);
        snapshotTransfer.setBucketName(names[0]);
        snapshotTransfer.setKeyName(names[1]);
        snapshotTransfer.cancelUpload();
      }
    } catch (Exception e) {
      LOG.debug("Attempt to delete uploaded snapshot " + snapshotId + " from objectstorage failed because: " + e.getMessage());
    }

    // Update deleted time
    try (TransactionResource tr = Entities.transactionFor(SnapshotInfo.class)) {
      try {
        SnapshotInfo lookup = Entities.uniqueResult(snapInfo);
        if (lookup.getDeletionTime() == null) {
          lookup.setDeletionTime(new Date());
        }
        tr.commit();
      } catch (TransactionException | NoSuchElementException e) {
        LOG.warn("Failed to update deletion time for " + snapshotId, e);
      }
    }
  }
}
