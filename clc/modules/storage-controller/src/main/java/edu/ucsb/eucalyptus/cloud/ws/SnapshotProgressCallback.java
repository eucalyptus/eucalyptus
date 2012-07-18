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
 ************************************************************************/

package edu.ucsb.eucalyptus.cloud.ws;

import org.apache.log4j.Logger;

import com.eucalyptus.entities.EntityWrapper;
import com.eucalyptus.util.StorageProperties;

import edu.ucsb.eucalyptus.cloud.entities.SnapshotInfo;

public class SnapshotProgressCallback implements CallBack {
	private String snapshotId;
	private int progressTick;
	private long updateThreshold;
	private static Logger LOG = Logger.getLogger(SnapshotProgressCallback.class);

	public SnapshotProgressCallback(String snapshotId, long size, int chunkSize) {
		this.snapshotId = snapshotId;
		progressTick = 3; //minimum percent update
		updateThreshold = ((size * progressTick) / 100) / chunkSize;
	}

	public void run() {
		EntityWrapper<SnapshotInfo> db = StorageProperties.getEntityWrapper();
		SnapshotInfo snapshotInfo = new SnapshotInfo(snapshotId);
		try {
			SnapshotInfo foundSnapshotInfo = db.getUnique(snapshotInfo);
			if(foundSnapshotInfo.getProgress() == null)
				foundSnapshotInfo.setProgress("0");
			Integer progress = Integer.parseInt(foundSnapshotInfo.getProgress());
			progress += progressTick;
			foundSnapshotInfo.setProgress(String.valueOf(progress));
		} catch (Exception ex) {
			db.rollback();
			failed();
			LOG.error(ex);
		}
		db.commit();
	}

	public void finish() {
		EntityWrapper<SnapshotInfo> db = StorageProperties.getEntityWrapper();
		SnapshotInfo snapshotInfo = new SnapshotInfo(snapshotId);
		try {
			SnapshotInfo foundSnapshotInfo = db.getUnique(snapshotInfo);
			foundSnapshotInfo.setProgress(String.valueOf(100));
			foundSnapshotInfo.setStatus(StorageProperties.Status.available.toString());
			foundSnapshotInfo.setShouldTransfer(false);
		} catch (Exception ex) {
			db.rollback();
			LOG.warn(ex);
		}
		db.commit();
	}

	public void failed() {
		EntityWrapper<SnapshotInfo> db = StorageProperties.getEntityWrapper();
		SnapshotInfo snapshotInfo = new SnapshotInfo(snapshotId);
		try {
			SnapshotInfo foundSnapshotInfo = db.getUnique(snapshotInfo);
			foundSnapshotInfo.setProgress(String.valueOf(0));
			foundSnapshotInfo.setStatus(StorageProperties.Status.failed.toString());
		} catch (Exception ex) {
			db.rollback();
			LOG.warn(ex);
		}
		db.commit();

	}

	public long getUpdateThreshold() {
		return updateThreshold;
	}
}
