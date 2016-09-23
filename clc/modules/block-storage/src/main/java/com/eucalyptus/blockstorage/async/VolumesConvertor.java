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

import java.util.List;

import org.apache.log4j.Logger;

import com.eucalyptus.blockstorage.LogicalStorageManager;
import com.eucalyptus.blockstorage.entities.SnapshotInfo;
import com.eucalyptus.blockstorage.entities.VolumeInfo;
import com.eucalyptus.blockstorage.util.StorageProperties;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionResource;
import com.eucalyptus.system.Threads;
import com.eucalyptus.util.EucalyptusCloudException;
import com.google.common.collect.Lists;

public class VolumesConvertor extends Thread {
  private static Logger LOG = Logger.getLogger(VolumesConvertor.class);
  private static final Object LOCK = new Object();

  private LogicalStorageManager fromBlockManager;
  private LogicalStorageManager toBlockManager;

  public VolumesConvertor(LogicalStorageManager fromBlockManager, LogicalStorageManager toBlockManager) {
    super( Threads.threadUniqueName( "storage-volumes-convertor" ) );
    this.fromBlockManager = fromBlockManager;
    this.toBlockManager = toBlockManager;
  }

  @Override
  public void run() {
    // This is a heavy weight operation. It must execute atomically.
    // All other volume operations are forbidden when a conversion is in progress.
    synchronized (LOCK) {
      StorageProperties.enableStorage = StorageProperties.enableSnapshots = false;
      List<VolumeInfo> volumes = Lists.newArrayList();
      List<SnapshotInfo> snapshots = Lists.newArrayList();
      try (TransactionResource tran = Entities.transactionFor(VolumeInfo.class)) {
        VolumeInfo volumeInfo = new VolumeInfo();
        volumeInfo.setStatus(StorageProperties.Status.available.toString());
        List<VolumeInfo> volumeInfos = Entities.query(volumeInfo);
        volumes.addAll(volumeInfos);

        SnapshotInfo snapInfo = new SnapshotInfo();
        snapInfo.setStatus(StorageProperties.Status.available.toString());
        List<SnapshotInfo> snapshotInfos = Entities.query(snapInfo);
        snapshots.addAll(snapshotInfos);

        tran.commit();
      }
      for (VolumeInfo volume : volumes) {
        String volumeId = volume.getVolumeId();
        try {
          LOG.info("Converting volume: " + volumeId + " please wait...");
          String volumePath = fromBlockManager.getVolumePath(volumeId);
          toBlockManager.importVolume(volumeId, volumePath, volume.getSize());
          fromBlockManager.finishVolume(volumeId);
          LOG.info("Done converting volume: " + volumeId);
        } catch (Exception ex) {
          LOG.error(ex);
          try {
            toBlockManager.deleteVolume(volumeId);
          } catch (EucalyptusCloudException e1) {
            LOG.error(e1);
          }
          // this one failed, continue processing the rest
        }
      }

      for (SnapshotInfo snap : snapshots) {
        String snapshotId = snap.getSnapshotId();
        try {
          LOG.info("Converting snapshot: " + snapshotId + " please wait...");
          String snapPath = fromBlockManager.getSnapshotPath(snapshotId);
          int size = fromBlockManager.getSnapshotSize(snapshotId);
          toBlockManager.importSnapshot(snapshotId, snap.getVolumeId(), snapPath, size);
          fromBlockManager.finishVolume(snapshotId);
          LOG.info("Done converting snapshot: " + snapshotId);
        } catch (Exception ex) {
          LOG.error(ex);
          try {
            toBlockManager.deleteSnapshot(snapshotId);
          } catch (EucalyptusCloudException e1) {
            LOG.error(e1);
          }
          // this one failed, continue processing the rest
        }
      }
      LOG.info("Conversion complete");
      StorageProperties.enableStorage = StorageProperties.enableSnapshots = true;
    }
  }
}
