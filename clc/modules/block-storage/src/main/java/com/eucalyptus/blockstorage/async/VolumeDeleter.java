/*************************************************************************
 * Copyright 2008 Regents of the University of California
 * Copyright 2009-2015 Ent. Services Development Corporation LP
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

import java.util.Date;
import java.util.List;
import java.util.NoSuchElementException;

import org.apache.log4j.Logger;

import com.eucalyptus.blockstorage.LogicalStorageManager;
import com.eucalyptus.blockstorage.entities.VolumeInfo;
import com.eucalyptus.blockstorage.entities.VolumeToken;
import com.eucalyptus.blockstorage.util.StorageProperties;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionException;
import com.eucalyptus.entities.TransactionResource;
import com.eucalyptus.entities.Transactions;
import com.eucalyptus.storage.common.CheckerTask;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.metrics.MonitoredAction;
import com.eucalyptus.util.metrics.ThruputMetrics;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;

import com.eucalyptus.blockstorage.util.EucaSemaphoreDirectory;

/**
 * Checker task for removing volumes marked in deleting status
 * 
 * @author Swathi Gangisetty
 *
 */
public class VolumeDeleter extends CheckerTask {
  private static Logger LOG = Logger.getLogger(VolumeDeleter.class);

  private LogicalStorageManager blockManager;

  public VolumeDeleter(LogicalStorageManager blockManager) {
    this.name = VolumeDeleter.class.getSimpleName();
    this.runInterval = 30; // runs every 30 seconds, TODO make this configurable?
    this.blockManager = blockManager;
  }

  @Override
  public void run() {
    try {
      // Look for volumes marked for deletion and delete them
      VolumeInfo searchVolume = new VolumeInfo();
      searchVolume.setStatus(StorageProperties.Status.deleting.toString());
      List<VolumeInfo> volumesToBeDeleted = null;
      try {
        volumesToBeDeleted = Transactions.findAll(searchVolume);
      } catch (Exception e) {
        LOG.error("Failed to lookup volumes marked for deletion", e);
        return;
      }

      if (volumesToBeDeleted != null && !volumesToBeDeleted.isEmpty()) {
        for (VolumeInfo vol : volumesToBeDeleted) {
          // Do separate transaction for each volume so we don't
          // keep the transaction open for a long time
          try (TransactionResource tran = Entities.transactionFor(VolumeInfo.class)) {
            vol = Entities.uniqueResult(vol);
            final String volumeId = vol.getVolumeId();
            LOG.debug("Volume: " + volumeId + " marked for deletion. Checking export status");
            if (Iterables.any(vol.getAttachmentTokens(), new Predicate<VolumeToken>() {
              @Override
              public boolean apply(VolumeToken token) {
                // Return true if attachment is valid or export exists.
                try {
                  return token.hasActiveExports();
                } catch (EucalyptusCloudException e) {
                  LOG.warn("Failure checking for active exports for volume " + volumeId);
                  return false;
                }
              }
            })) {
              // Exports exists... try un exporting the volume before deleting.
              LOG.info("Volume: " + volumeId + " found to be exported. Detaching volume from all hosts");
              try {
                Entities.asTransaction(VolumeInfo.class, invalidateAndDetachAll(blockManager)).apply(volumeId);
              } catch (Exception e) {
                LOG.error("Failed to fully detach volume " + volumeId, e);
              }
            }

            LOG.info("Volume: " + volumeId + " was marked for deletion. Cleaning up...");
            try {
              blockManager.deleteVolume(volumeId);
            } catch (EucalyptusCloudException e) {
              LOG.debug("Failed to delete " + volumeId, e);
              LOG.warn("Unable to delete " + volumeId + ". Will retry later");
              continue;
            }
            vol.setStatus(StorageProperties.Status.deleted.toString());
            vol.setDeletionTime(new Date());
            EucaSemaphoreDirectory.removeSemaphore(volumeId); // who put it there ?
            tran.commit();
          } catch (Exception e) {
            LOG.error("Error deleting volume " + vol.getVolumeId() + ": " + e.getMessage());
            LOG.debug("Exception during deleting volume " + vol.getVolumeId() + ".", e);
          } finally {
            ThruputMetrics.endOperation(MonitoredAction.DELETE_VOLUME, vol.getVolumeId(), System.currentTimeMillis());
          }
        }
      } else {
        LOG.trace("No volumes marked for deletion");
      }
    } catch (Exception e) { // could catch InterruptedException
      LOG.warn("Unable to remove volumes marked for deletion", e);
      return;
    }
  }

  private static Function<String, VolumeInfo> invalidateAndDetachAll(final LogicalStorageManager blockManager) {

    final Predicate<VolumeToken> invalidateExports = new Predicate<VolumeToken>() {
      @Override
      public boolean apply(VolumeToken volToken) {
        VolumeToken tokenEntity = Entities.merge(volToken);
        try {
          tokenEntity.invalidateAllExportsAndToken();
          return true;
        } catch (Exception e) {
          LOG.error("Failed invalidating exports for token " + tokenEntity.getToken());
          return false;
        }
      }
    };

    // Could save cycles by statically setting all of these functions that don't require closures so they are not
    // constructed for each request.
    return new Function<String, VolumeInfo>() {
      @Override
      public VolumeInfo apply(String volumeId) {
        try {
          VolumeInfo volumeEntity = Entities.uniqueResult(new VolumeInfo(volumeId));
          try {
            LOG.debug("Invalidating all tokens and all exports for " + volumeId);
            // Invalidate all tokens and exports and forcibly detach.
            if (!Iterables.all(volumeEntity.getAttachmentTokens(), invalidateExports)) {
              // At least one failed.
              LOG.error("Failed to invalidate all tokens and exports");
            }
          } catch (Exception e) {
            LOG.error("Error invalidating tokens", e);
          }

          try {
            LOG.debug("Unexporting volume " + volumeId + " from all hosts");
            blockManager.unexportVolumeFromAll(volumeId);
          } catch (EucalyptusCloudException ex) {
            LOG.error("Detaching volume " + volumeId + " from all hosts failed", ex);
          }
        } catch (NoSuchElementException e) {
          LOG.error("Cannot force detach of volume " + volumeId + " because it is not found in database");
          return null;
        } catch (TransactionException e) {
          LOG.error("Failed to lookup volume " + volumeId);
        }

        return null;
      }
    };
  }
}
