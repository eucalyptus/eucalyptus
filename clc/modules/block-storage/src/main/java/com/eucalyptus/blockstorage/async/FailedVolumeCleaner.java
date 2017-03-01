/*************************************************************************
 * (c) Copyright 2017 Hewlett Packard Enterprise Development Company LP
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
import com.eucalyptus.blockstorage.entities.VolumeInfo;
import com.eucalyptus.blockstorage.util.BlockStorageUtil;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionException;
import com.eucalyptus.entities.TransactionResource;
import com.eucalyptus.storage.common.CheckerTask;

/**
 * Checker task for cleaning volumes marked in failed status
 * 
 * @author Swathi Gangisetty
 *
 */
public class FailedVolumeCleaner extends CheckerTask {
  private static Logger LOG = Logger.getLogger(FailedVolumeCleaner.class);

  private LogicalStorageManager blockManager;

  public FailedVolumeCleaner(LogicalStorageManager blockManager) {
    this.name = FailedVolumeCleaner.class.getSimpleName();
    this.runInterval = 2 * 60; // runs every 2 minutes, make this configurable?
    this.blockManager = blockManager;
  }

  @Override
  public void run() {
    try {
      List<VolumeInfo> volumeInfos = null;
      try (TransactionResource tr = Entities.transactionFor(VolumeInfo.class)) {
        volumeInfos = Entities.query(new VolumeInfo(), Boolean.TRUE, BlockStorageUtil.getFailedCriterion(), Collections.EMPTY_MAP);
        tr.commit();
      } catch (Exception e) {
        LOG.warn("Failed to query database for failed volumes", e);
        return;
      }

      if (volumeInfos != null && !volumeInfos.isEmpty()) {
        for (VolumeInfo volumeInfo : volumeInfos) {
          cleanVolume(volumeInfo);
        }
      } else {
        LOG.trace("No failed volumes to clean");
      }
    } catch (Exception e) { // could catch InterruptedException
      LOG.warn("Unable to clean failed volumes", e);
      return;
    }
  }

  private void cleanVolume(VolumeInfo volumeInfo) {
    String volumeId = volumeInfo.getVolumeId();
    LOG.info("Cleaning up failed volume " + volumeInfo.getVolumeId());

    try {
      blockManager.cleanVolume(volumeId);
    } catch (Exception e) {
      LOG.debug("Attempt to clean volume " + volumeId + " on storage backend failed because: " + e.getMessage());
    }

    // Update deleted time
    try (TransactionResource tr = Entities.transactionFor(VolumeInfo.class)) {
      try {
        VolumeInfo lookup = Entities.uniqueResult(volumeInfo);
        if (lookup.getDeletionTime() == null) {
          lookup.setDeletionTime(new Date());
        }
        tr.commit();
      } catch (TransactionException | NoSuchElementException e) {
        LOG.debug("Failed to update deletion time for " + volumeId, e);
      }
    }
  }
}
