/*************************************************************************
 * Copyright 2009-2014 Ent. Services Development Corporation LP
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

package com.eucalyptus.objectstorage.asynctask;

import java.util.List;

import org.apache.log4j.Logger;

import com.eucalyptus.entities.TransactionException;
import com.eucalyptus.entities.Transactions;
import com.eucalyptus.objectstorage.MpuPartMetadataManagers;
import com.eucalyptus.objectstorage.ObjectMetadataManagers;
import com.eucalyptus.objectstorage.ObjectState;
import com.eucalyptus.objectstorage.OsgObjectFactory;
import com.eucalyptus.objectstorage.entities.ObjectEntity;
import com.eucalyptus.objectstorage.entities.PartEntity;
import com.eucalyptus.objectstorage.providers.ObjectStorageProviders;
import com.eucalyptus.util.EucalyptusCloudException;

/**
 * Scans metadata for "deleted" objects and removes them from the backend. Many of these may be running concurrently.
 *
 */
public class ObjectReaperTask implements Runnable {
  private static final Logger LOG = Logger.getLogger(ObjectReaperTask.class);

  private volatile boolean interrupted = false;

  public ObjectReaperTask() {}

  public void interrupt() {
    this.interrupted = true;
  }

  public void resume() {
    this.interrupted = false;
  }

  public void reapObject(final ObjectEntity obj) throws Exception {
    LOG.trace("Reaping object " + obj.getObjectUuid());
    try {
      OsgObjectFactory.getFactory().actuallyDeleteObject(ObjectStorageProviders.getInstance(), obj, null);
    } catch (EucalyptusCloudException ex) {
      // Failed. Keep record so we can retry later
      LOG.trace("Reaping failed due to error for object: " + obj.getBucket().getBucketUuid() + "/" + obj.getObjectUuid() + " Will retry", ex);
    }
  }

  // Does a single scan of the DB and reclaims objects it finds in the 'deleting' state
  @Override
  public void run() {
    long startTime = System.currentTimeMillis();
    try {
      LOG.debug("Initiating object-storage object reaper task");
      cleanDeleting();
      cleanFailed();
      cleanParts();
    } catch (final Throwable f) {
      LOG.error("Error during object reaper execution. Will retry later", f);
    } finally {
      try {
        long endTime = System.currentTimeMillis();
        LOG.debug("Object reaper execution task took " + Long.toString(endTime - startTime) + "ms to complete");
      } catch (final Throwable f) {
        // Do nothing, but don't allow exceptions out
      }
    }
  }

  private void cleanDeleting() {
    try {
      List<ObjectEntity> entitiesToClean = ObjectMetadataManagers.getInstance().lookupObjectsInState(null, null, null, ObjectState.deleting);
      LOG.trace("Reaping " + entitiesToClean.size() + " objects from backend");
      for (ObjectEntity obj : entitiesToClean) {
        try {
          reapObject(obj);
        } catch (final Throwable f) {
          LOG.error("Error during object reaper cleanup for object: " + " uuid= " + obj.getObjectUuid(), f);
        }
        if (interrupted) {
          break;
        }
      }
    } catch (Exception e) {
      LOG.warn("Error encountered during reaping of deleting-state object. Will retry on next cycle", e);
    }
  }

  private void cleanFailed() {
    try {
      List<ObjectEntity> entitiesToClean = ObjectMetadataManagers.getInstance().lookupFailedObjects();
      LOG.trace("Reaping " + entitiesToClean.size() + " objects with expired creation time from backend");
      for (ObjectEntity obj : entitiesToClean) {
        try {
          reapObject(obj);
        } catch (final Throwable f) {
          LOG.error("Error during object reaper cleanup for object: " + " uuid= " + obj.getObjectUuid(), f);
        }
        if (interrupted) {
          break;
        }
      }
    } catch (Exception e) {
      LOG.warn("Error encountered during reaping of deleting-state object. Will retry on next cycle", e);
    }
  }

  private void cleanParts() {
    // For multipart upload. These are parts that are duplicates or are not the latest according to timestamp and have been marked for deletion.
    try {
      List<PartEntity> partsToClean = MpuPartMetadataManagers.getInstance().lookupPartsInState(null, null, null, ObjectState.deleting);
      LOG.trace("Reaping " + partsToClean.size() + " parts from backend");
      for (PartEntity part : partsToClean) {
        try {
          reapPart(part);
        } catch (final Throwable f) {
          LOG.error("Error during part reaper cleanup for part: " + part.getBucket().getBucketName() + " uploadId: " + part.getUploadId()
              + " partNumber: " + part.getPartNumber() + " uuid= " + part.getPartUuid(), f);
        }
        if (interrupted) {
          break;
        }
      }
    } catch (Exception e) {
      LOG.warn("Error cleaning parts. Will retry later.", e);
    }
  }

  public void reapPart(final PartEntity part) throws Exception {
    // we don't care about the backend here, because the backend will handle GC'ing parts
    // on its own.
    try {
      Transactions.delete(part);
    } catch (TransactionException e) {
      LOG.error("Unable to drop part: " + part.getBucket().getBucketName() + " uploadId: " + part.getUploadId() + " partNumber: "
          + part.getPartNumber() + " uuid: " + part.getPartUuid());
    }
  }
}
