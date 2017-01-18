/*************************************************************************
 * Copyright 2009-2012 Eucalyptus Systems, Inc.
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

package com.eucalyptus.blockstorage;

import java.util.NoSuchElementException;
import java.util.concurrent.Callable;

import org.apache.log4j.Logger;

import com.eucalyptus.blockstorage.entities.SnapshotInfo;
import com.eucalyptus.blockstorage.util.StorageProperties;
import com.eucalyptus.component.Components;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionException;
import com.eucalyptus.system.Threads;
import com.google.common.base.Function;

/**
 * Callback that updates snapshot upload info in the db after at least N percent upload change. Currently set at 3%
 * 
 * All updates are asynchronous so actual db update may be delayed. Uses a single queue to ensure that updates are in-order.
 * 
 * The finish and fail operations are no-ops.
 * 
 */
public class SnapshotProgressCallback {
  private static Logger LOG = Logger.getLogger(SnapshotProgressCallback.class);
  private static final int PROGRESS_TICK = 3; // Percent between updates

  private String snapshotId;
  private long uploadSize;
  private long bytesTransferred;
  private int lastProgress;
  private int uploadProgress;
  private int backendProgress;
  private ServiceConfiguration scConfig;

  public SnapshotProgressCallback(String snapshotId) {
    this.snapshotId = snapshotId;
    this.uploadSize = 0L;
    this.bytesTransferred = 0L;
    this.scConfig = Components.lookup(Storage.class).getLocalServiceConfiguration();
    this.backendProgress = 1;// to indicate that some amount of snapshot in in progress
    this.uploadProgress = 0;
    this.lastProgress = this.backendProgress + this.uploadProgress;
    Threads.enqueue(scConfig, SnapshotProgressCallback.class, 1, new ProgressSetter(this.snapshotId, this.lastProgress));
  }

  // strictly to be used by mock only
  protected SnapshotProgressCallback() {}

  // Set the size before calling update()
  public void setUploadSize(long uploadSize) {
    this.uploadSize = uploadSize;
  }

  public synchronized void updateUploadProgress(final long bytesTransferred) {
    if (this.uploadSize > 0) {
      this.bytesTransferred += bytesTransferred;
      int progress = (int) ((this.bytesTransferred * 50) / uploadSize); // halving the progress as upload is one half of snapshot creation process
      if (progress >= 50 || (progress - this.uploadProgress < PROGRESS_TICK)) {
        // Don't update. Either not enough change or snapshot transfer is complete
        return;
      } else {
        this.uploadProgress = progress;
        this.lastProgress = this.backendProgress + this.uploadProgress;
        Threads.enqueue(scConfig, SnapshotProgressCallback.class, 1, new ProgressSetter(this.snapshotId, this.lastProgress));
      }
    }
  }

  public synchronized void updateBackendProgress(final int percentComplete) {
    int progress = (int) percentComplete / 2; // halving the progress as backend stuff is one half of snapshot creation process
    if (progress > 50 || (progress - this.backendProgress < PROGRESS_TICK)) {
      // Don't update. Either not enough change or snapshot is 100% complete
      return;
    } else {
      this.backendProgress = progress;
      this.lastProgress = this.backendProgress + this.uploadProgress;
      Threads.enqueue(scConfig, SnapshotProgressCallback.class, 1, new ProgressSetter(this.snapshotId, this.lastProgress));
    }
  }

  class ProgressSetter implements Callable<Boolean> {
    private String snapshot;
    private int progress;

    public ProgressSetter(String snapshotId, int progress) {
      this.snapshot = snapshotId;
      this.progress = progress;
    }

    @Override
    public Boolean call() throws Exception {
      if (this.progress < 100) { // set the progress only if its under 100
        try {
          Entities.asTransaction(SnapshotInfo.class, new Function<String, SnapshotInfo>() {

            @Override
            public SnapshotInfo apply(String arg0) {
              SnapshotInfo snap = null;
              try {
                snap = Entities.uniqueResult(new SnapshotInfo(snapshot));
                if (StorageProperties.Status.pending.toString().equals(snap.getStatus())
                    || StorageProperties.Status.creating.toString().equals(snap.getStatus())) {
                  if (snap.getProgress() != null) {
                    try {
                      if (progress > Integer.valueOf(snap.getProgress())) { // set progress only if its greater than current value
                        snap.setProgress(String.valueOf(progress));
                      } else {
                        // don't set progress if its lower than last progress
                      }
                    } catch (NumberFormatException e) { // error parsing progress?
                      LOG.debug("Encountered issue while parsing snapshot progress: " + snap.getProgress());
                      snap.setProgress(String.valueOf(progress));
                    }
                  } else { // probably setting progress for the first time, go ahead and set it
                    snap.setProgress(String.valueOf(progress));
                  }
                } else if (!StorageProperties.Status.failed.toString().equals(snap.getStatus())) {
                  // probably setting progress for the first time, go ahead and set it
                  snap.setProgress(String.valueOf(progress));
                } else {
                  // don't set the progress if the snapshot has been marked failed
                }
              } catch (TransactionException | NoSuchElementException e) {
                LOG.debug("Could not find the SC database entity for " + snapshot + ". Skipping progress update");
              }
              return snap;
            }
          }).apply(this.snapshot);
        } catch (Exception e) {
          LOG.debug("Could not update snapshot progress in DB for " + snapshot + " to " + lastProgress + "% due to " + e.getMessage());
        }
      } else {
        // not setting progress since its greater than 100 and it does not make sense
      }
      return true;
    }
  }
}
