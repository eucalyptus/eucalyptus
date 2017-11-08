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
 *************************************************************************
 * PopulateSnapPoints.groovy
 * 
 * Runs during each Storage Controller (SC) intialization, only for 
 * Eucalyptus v4.4.x. To be removed for v5.0. 
 * 
 * Can also be run manually on a running SC (eucalyptus-cloud process).
 * Usage: (no parameters)
 *   euctl euca=@PopulateSnapPoints.groovy
 * 
 * 1. Look for existing snapshots in this availability zone (cluster) in
 *      the Eucalyptus database that have no Snapshot Point ID. Snapshots 
 *      created in v4.3.x or earlier will have 'null' in this field.
 * 2. Look for any of these snapshots' parent volumes in each Ceph pool 
 *      defined for this zone.
 * 3. For each parent volume found in a pool, populate the Snapshot Point ID
 *      using the pool ID, parent volume ID, and snapshot ID.
 * 4. Commit the updated snapshot rows to the database.
 * 5. Display any remaining snapshots needing Snapshot Point ID whose parent
 *      volume were not found in any Ceph pool and thus not updated.
 *
 ************************************************************************/

package com.eucalyptus.blockstorage

import java.util.List
import java.util.NoSuchElementException

import org.apache.log4j.Logger

import com.ceph.rbd.Rbd
import com.ceph.rbd.RbdImage
import com.eucalyptus.blockstorage.ceph.CephRbdAdapter
import com.eucalyptus.blockstorage.ceph.CephRbdConnectionManager
import com.eucalyptus.blockstorage.ceph.CephRbdFormatTwoAdapter
import com.eucalyptus.blockstorage.ceph.CephRbdProvider
import com.eucalyptus.blockstorage.ceph.entities.CephRbdInfo
import com.eucalyptus.blockstorage.ceph.exceptions.EucalyptusCephException
import com.eucalyptus.blockstorage.config.StorageControllerBuilder
import com.eucalyptus.blockstorage.entities.SnapshotInfo
import com.eucalyptus.blockstorage.entities.SnapshotInfo_
import com.eucalyptus.blockstorage.entities.VolumeInfo
import com.eucalyptus.blockstorage.util.StorageProperties
import com.eucalyptus.entities.Entities
import com.eucalyptus.entities.EntityRestriction
import com.eucalyptus.entities.TransactionException
import com.eucalyptus.entities.Transactions
import com.eucalyptus.entities.TransactionResource

import com.google.common.base.Function

class LocalLogger {

  private static Logger LOG = null
  private static String LINE_PREFIX = null
  private static StringBuffer output = null

  static void initLogger(Class name, String prefix) {
    LOG = Logger.getLogger(name)
    LINE_PREFIX = prefix + ": "
    output = new StringBuffer(1000)
  }

  private static void addOutput(Object message) {
    output.append(LINE_PREFIX + message.toString() + '\n')
  }

  static String getOutput() {
    return output.toString()
  }

  static void trace(Object message) {
    LOG.trace(LINE_PREFIX + message)
  }

  static void debug(Object message) {
    LOG.debug(LINE_PREFIX + message)
  }

  static void info(Object message) {
    LOG.debug(LINE_PREFIX + message)
    addOutput(message)
  }

  static void warn(Object message) {
    LOG.warn(LINE_PREFIX + message)
    addOutput("WARNING: " + message)
  }

  static void warn(Object message, Throwable t) {
    LOG.warn(LINE_PREFIX + message, t)
    addOutput("WARNING: " + message + "\n    " + t.getMessage())
  }

  static void error(Object message) {
    LOG.error(LINE_PREFIX + message)
    addOutput("ERROR: " + message)
  }

  static void error(Object message, Throwable t) {
    LOG.error(LINE_PREFIX + message, t)
    addOutput("ERROR: " + message + "\n    " + t.getMessage())
  }

}  // end class LocalLogger

class SnapPointsUpdater {

  private static Function<Object, List<SnapshotInfo>> getSnapshotsToUpdateList = new Function<Object, List<SnapshotInfo>>() {
    @Override
    public List<SnapshotInfo> apply(Object unused) {
      LocalLogger.debug("Querying zone " + StorageProperties.NAME)
      List<SnapshotInfo> snapshotsToUpdate = Entities.criteriaQuery(SnapshotInfo.class).where(
          Entities.restriction(SnapshotInfo.class).all(
          Entities.restriction(SnapshotInfo.class).isNull(SnapshotInfo_.snapPointId).build(),
          Entities.restriction(SnapshotInfo.class).equal(SnapshotInfo_.scName, StorageProperties.NAME).build()
          )
          ).list();
      LocalLogger.debug("Returned list size is " + snapshotsToUpdate.size())
      return snapshotsToUpdate
    }
  }

  private static Function<List<SnapshotInfo>, Boolean> storeUpdatedSnapshots = new Function<List<SnapshotInfo>, Boolean>() {
    @Override
    public Boolean apply(List<SnapshotInfo> snapshotsToUpdate) {
      boolean result = true
      for (SnapshotInfo snapshot : snapshotsToUpdate) {
        if (snapshot.getSnapPointId() != null) {
          SnapshotInfo snapshotFromDb = null
          try {
            snapshotFromDb = Entities.criteriaQuery(Entities.restriction(SnapshotInfo.class).equal(
                SnapshotInfo_.snapshotId, snapshot.getSnapshotId())).uniqueResult()
            snapshotFromDb.setSnapPointId(snapshot.getSnapPointId())
          } catch (Exception e) {
            LocalLogger.error("Caught Exception looking up snapshot " + snapshot.getSnapshotId() +
                " from Eucalyptus database", e)
            result = false
          }
        }
      }
      return new Boolean(result)
    }
  }

  static String updateSnapPoints() {

    LocalLogger.initLogger(SnapPointsUpdater.class, "PopulateSnapPoints")

    LocalLogger.info("Starting")

    List<SnapshotInfo> snapshotsToUpdate = null
    try {
      snapshotsToUpdate =
          Entities.asTransaction(SnapshotInfo.class, getSnapshotsToUpdateList).apply(null)
    } catch (Exception e) {
      LocalLogger.error("Caught Exception getting snapshot list", e)
    }

    if (snapshotsToUpdate != null && !snapshotsToUpdate.isEmpty()) {

      boolean anySnapshotsUpdated = false

      CephRbdInfo cephInfo = CephRbdInfo.getStorageInfo()
      String[] volumePoolsArray = cephInfo.getAllVolumePools()
      List volumePools = Arrays.asList(volumePoolsArray)

      for (String pool : volumePools) {
        CephRbdConnectionManager rbdConnection = null
        try {
          LocalLogger.trace("Connecting to Ceph pool " + pool)
          rbdConnection = CephRbdConnectionManager.getConnection(cephInfo, pool)
          LocalLogger.trace("Connected to Ceph pool " + pool)
        } catch (Exception e) {
          LocalLogger.error("Caught Exception connecting to Ceph pool " + pool, e)
          continue
        }
        Rbd rbd = rbdConnection.getRbd()
        String[] imageIdsArray = rbd.list(100000) //arbitrary 100KB initial buffer size
        List<String> imageIds = Arrays.asList(imageIdsArray)

        for (SnapshotInfo snapshot : snapshotsToUpdate) {
          // Even though we queried for non-null snap points, we might have filled it in
          // when scanning a previous pool
          if (snapshot.getSnapPointId() == null) {
            String volumeId = snapshot.getVolumeId()
            String snapshotId = snapshot.getSnapshotId()
            LocalLogger.trace("Looking for volume " + volumeId + " for snapshot " + snapshotId + " in pool " + pool)
            String fullVolumeId = null
            if (imageIds.contains(volumeId)) {
              fullVolumeId = volumeId
            } else if (imageIds.contains(cephInfo.getDeletedImagePrefix()+volumeId)) {
              fullVolumeId = cephInfo.getDeletedImagePrefix() + volumeId
            }
            if (fullVolumeId != null) {
              String snapPointId = pool + CephRbdInfo.POOL_IMAGE_DELIMITER + fullVolumeId + \
                CephRbdInfo.IMAGE_SNAPSHOT_DELIMITER + CephRbdInfo.SNAPSHOT_FOR_PREFIX + snapshotId
              LocalLogger.info("Found volume " + fullVolumeId + " for snapshot " + snapshotId + " in pool " + pool
                  + ". Storing snapshot point " + snapPointId)
              snapshot.setSnapPointId(snapPointId)
              anySnapshotsUpdated = true
            } else {
              LocalLogger.trace("Couldn't find volume " + volumeId + " for snapshot " + snapshotId + " in pool " + pool)
            }
          }
        }  // end for all snapshots to update
        if (rbdConnection != null) {
          rbdConnection.close()
        }
      }  // end for each pool

      if (anySnapshotsUpdated) {
        try {
          LocalLogger.trace("Updating snapshot info in database")
          Boolean result = Entities.asTransaction(SnapshotInfo.class, storeUpdatedSnapshots).apply(snapshotsToUpdate)
          if (result == null || !result.booleanValue()) {
            LocalLogger.error("Failure trying to store newly updated snapshot points in " + \
              "Eucalyptus database. Snapshots might not be updated.")
          }
        } catch (Exception e) {
          LocalLogger.error("Caught Exception trying to store newly updated snapshot points in " + \
        "Eucalyptus database. Snapshots might not be updated", e)
        }
      }

      for (SnapshotInfo snapshot : snapshotsToUpdate) {
        if (snapshot.getSnapPointId() == null) {
          LocalLogger.warn("Snapshot " + snapshot.getSnapshotId() + " not found in any pool, snapshot point not updated.\n" +
            "If this snapshot originated in another zone, then this is normal.")
        }
      }

    } else {
      LocalLogger.info("No existing snapshots required updating")
    }

    LocalLogger.info("Finished, exiting")

    return LocalLogger.getOutput()

  }  // end updateSnapPoints

}  // end class SnapPointsUpdater

// This line allows this file to be run as a groovy script, see file comment block.
return SnapPointsUpdater.updateSnapPoints();
