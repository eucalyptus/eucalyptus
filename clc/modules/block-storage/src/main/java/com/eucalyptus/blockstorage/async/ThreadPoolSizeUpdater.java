/*************************************************************************
 * Copyright 2016 Ent. Services Development Corporation LP
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

package com.eucalyptus.blockstorage.async;

import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.eucalyptus.blockstorage.entities.StorageInfo;
import com.eucalyptus.blockstorage.threadpool.SnapshotThreadPool;
import com.eucalyptus.blockstorage.threadpool.SnapshotTransferThreadPool;
import com.eucalyptus.blockstorage.threadpool.VolumeThreadPool;
import com.eucalyptus.storage.common.CheckerTask;

/**
 * Periodic task for comparing sizes of VolumeThreadPool, SnapshotThreadPool and SnapshotTransferThreadPool with configuration and updating the thread
 * pools accordingly
 * 
 * @author Swathi Gangisetty
 *
 */
public class ThreadPoolSizeUpdater extends CheckerTask {

  private static Logger LOG = Logger.getLogger(ThreadPoolSizeUpdater.class);

  public ThreadPoolSizeUpdater() {
    this.name = ThreadPoolSizeUpdater.class.getSimpleName();
    this.runInterval = 30; // runs every 30 seconds, make this configurable?
    this.runIntervalUnit = TimeUnit.SECONDS;
    this.isFixedDelay = Boolean.TRUE;
  }

  @Override
  public void run() {
    try {
      // check volume, snapshot and snapshot transfer thread pool size and update if necessary
      StorageInfo info = StorageInfo.getStorageInfo();

      Integer currentSize = VolumeThreadPool.getPoolSize();
      if (currentSize != null && info.getMaxConcurrentVolumes() != null && currentSize != info.getMaxConcurrentVolumes()) {
        LOG.debug("Updating VolumeThreadPool to " + info.getMaxConcurrentVolumes());
        VolumeThreadPool.updatePoolSize(info.getMaxConcurrentVolumes());
      }

      currentSize = SnapshotThreadPool.getPoolSize();
      if (currentSize != null && info.getMaxConcurrentSnapshots() != null && currentSize != info.getMaxConcurrentSnapshots()) {
        LOG.debug("Updating SnapshotThreadPool to " + info.getMaxConcurrentSnapshots());
        SnapshotThreadPool.updatePoolSize(info.getMaxConcurrentSnapshots());
      }

      currentSize = SnapshotTransferThreadPool.getPoolSize();
      if (currentSize != null && info.getMaxConcurrentSnapshotTransfers() != null && currentSize != info.getMaxConcurrentSnapshotTransfers()) {
        LOG.debug("Updating SnapshotTransferThreadPool to " + info.getMaxConcurrentSnapshotTransfers());
        SnapshotTransferThreadPool.updatePoolSize(info.getMaxConcurrentSnapshotTransfers());
      }
    } catch (Throwable t) {
      LOG.debug("Unable to check or update thread pool size", t);
    }
  }
}
