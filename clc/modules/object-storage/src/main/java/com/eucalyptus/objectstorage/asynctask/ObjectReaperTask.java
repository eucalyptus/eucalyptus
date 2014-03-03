/*
 * Copyright 2009-2014 Eucalyptus Systems, Inc.
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
 */

package com.eucalyptus.objectstorage.asynctask;

import java.util.List;

import com.eucalyptus.entities.TransactionException;
import com.eucalyptus.entities.Transactions;
import com.eucalyptus.objectstorage.MpuPartMetadataManagers;
import com.eucalyptus.objectstorage.entities.PartEntity;
import com.eucalyptus.objectstorage.ObjectMetadataManagers;
import com.eucalyptus.objectstorage.ObjectState;
import com.eucalyptus.objectstorage.providers.ObjectStorageProviders;
import com.eucalyptus.objectstorage.OsgObjectFactory;
import org.apache.log4j.Logger;

import com.eucalyptus.objectstorage.entities.ObjectEntity;
import com.eucalyptus.util.EucalyptusCloudException;

/**
 * Scans metadata for "deleted" objects and removes them from the backend.
 * Many of these may be running concurrently.
 *
 */
public class ObjectReaperTask implements Runnable {
	private static final Logger LOG = Logger.getLogger(ObjectReaperTask.class);

    private boolean interrupted = false;

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
		} catch(EucalyptusCloudException ex) {
			//Failed. Keep record so we can retry later
			LOG.trace("Reaping failed due to error for object: " + obj.getBucket().getBucketUuid() + "/" + obj.getObjectUuid() + " Will retry", ex);
		}
	}
	
	//Does a single scan of the DB and reclaims objects it finds in the 'deleting' state
	@Override
	public void run() {
		long startTime = System.currentTimeMillis();
		try {
			LOG.debug("Initiating object-storage object reaper task");
			cleanDeleting();
            cleanFailed();
            cleanParts();
		} catch(final Throwable f) {
			LOG.error("Error during object reaper execution. Will retry later", f);
		} finally {
			try {
				long endTime = System.currentTimeMillis();
				LOG.debug("Object reaper execution task took " + Long.toString(endTime - startTime) + "ms to complete");
			} catch( final Throwable f) {
				//Do nothing, but don't allow exceptions out
			}
		}
	}

    private void cleanDeleting() {
        try {
            List<ObjectEntity> entitiesToClean = ObjectMetadataManagers.getInstance().lookupObjectsInState(null, null, null, ObjectState.deleting);
            LOG.trace("Reaping " + entitiesToClean.size() + " objects from backend");
            for(ObjectEntity obj : entitiesToClean) {
                try {
                    reapObject(obj);
                } catch(final Throwable f) {
                    LOG.error("Error during object reaper cleanup for object: " +
                            " uuid= " + obj.getObjectUuid(), f);
                }
                if (interrupted) {
                    break;
                }
            }
        } catch(Exception e) {
            LOG.warn("Error encountered during reaping of deleting-state object. Will retry on next cycle", e);
        }
    }

    private void cleanFailed() {
        try {
            List<ObjectEntity> entitiesToClean = ObjectMetadataManagers.getInstance().lookupFailedObjects();
            LOG.trace("Reaping " + entitiesToClean.size() + " objects with expired creation time from backend");
            for(ObjectEntity obj : entitiesToClean) {
                try {
                    reapObject(obj);
                } catch(final Throwable f) {
                    LOG.error("Error during object reaper cleanup for object: " +
                            " uuid= " + obj.getObjectUuid(), f);
                }
                if (interrupted) {
                    break;
                }
            }
        } catch(Exception e) {
            LOG.warn("Error encountered during reaping of deleting-state object. Will retry on next cycle", e);
        }
    }

    private void cleanParts() {
        //For multipart upload. These are parts that are duplicates or are not the latest according to timestamp and have been marked for deletion.
        try {
            List<PartEntity> partsToClean = MpuPartMetadataManagers.getInstance().lookupPartsInState(null, null, null, ObjectState.deleting);
            LOG.trace("Reaping " + partsToClean.size() + " parts from backend");
            for(PartEntity part : partsToClean) {
                try {
                    reapPart(part);
                } catch(final Throwable f) {
                    LOG.error("Error during part reaper cleanup for part: " +
                            part.getBucket().getBucketName() + " uploadId: " + part.getUploadId() + " partNumber: " + part.getPartNumber() +
                            " uuid= " + part.getPartUuid(), f);
                }
                if (interrupted) {
                    break;
                }
            }
        } catch(Exception e) {
            LOG.warn("Error cleaning parts. Will retry later.", e);
        }
    }

    public void reapPart(final PartEntity part) throws Exception {
        //we don't care about the backend here, because the backend will handle GC'ing parts
        //on its own.
        try {
            Transactions.delete(part);
        } catch (TransactionException e) {
            LOG.error("Unable to drop part: " + part.getBucket().getBucketName() + " uploadId: " + part.getUploadId() + " partNumber: " + part.getPartNumber() + " uuid: " + part.getPartUuid());
        }
    }
}
