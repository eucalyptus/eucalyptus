/*************************************************************************
 * (c) Copyright 2016 Hewlett Packard Enterprise Development Company LP
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
