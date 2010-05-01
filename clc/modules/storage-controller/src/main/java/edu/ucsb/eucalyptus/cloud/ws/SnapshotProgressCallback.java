package edu.ucsb.eucalyptus.cloud.ws;

import org.apache.log4j.Logger;

import com.eucalyptus.entities.EntityWrapper;
import com.eucalyptus.util.StorageProperties;

import edu.ucsb.eucalyptus.cloud.entities.SnapshotInfo;

public class SnapshotProgressCallback implements CallBack {
	private String snapshotId;
	private int progressTick;
	private int updateThreshold;
	private static Logger LOG = Logger.getLogger(SnapshotProgressCallback.class);

	public SnapshotProgressCallback(String snapshotId, long size, int chunkSize) {
		this.snapshotId = snapshotId;
		progressTick = 3; //minimum percent update
		updateThreshold = (int)(((size * progressTick) / 100) / chunkSize);
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

	public int getUpdateThreshold() {
		return updateThreshold;
	}
}