package edu.ucsb.eucalyptus.cloud.ws;

import org.apache.log4j.Logger;

import com.eucalyptus.util.EntityWrapper;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.StorageProperties;

import edu.ucsb.eucalyptus.cloud.entities.StorageStatsInfo;
import edu.ucsb.eucalyptus.msgs.StorageUsageStatsRecord;

public class BlockStorageStatistics {
	private Logger LOG = Logger.getLogger( BlockStorageStatistics.class );
	private Long totalSpaceUsed;
	private Integer numberOfVolumes;

	public BlockStorageStatistics() {
		totalSpaceUsed = 0L;
		numberOfVolumes = 0;
		getStateInfo();
		dump();
	}

	public void updateSpaceUsed(Long bytes) {
		totalSpaceUsed += bytes;
		updateStateInfo();
		dump();
	}

	public void incrementVolumeCount() {
		numberOfVolumes++;
		updateStateInfo();
		dump();
	}

	public void decrementVolumeCount() {
		numberOfVolumes--;
		updateStateInfo();
		dump();
	}

	public void dump() {
		LOG.info(StorageUsageStatsRecord.create(numberOfVolumes, totalSpaceUsed));
	}

	private void getStateInfo() {
		EntityWrapper<StorageStatsInfo> db = new EntityWrapper<StorageStatsInfo>();
		try {
			StorageStatsInfo walrusStats = db.getUnique(new StorageStatsInfo(StorageProperties.NAME));
			numberOfVolumes = walrusStats.getNumberOfVolumes();
			totalSpaceUsed = walrusStats.getTotalSpaceUsed();
		} catch(EucalyptusCloudException ex) {
			StorageStatsInfo storageStats = new StorageStatsInfo(StorageProperties.NAME,
					numberOfVolumes,
					totalSpaceUsed);
			db.add(storageStats);
		}
		db.commit();
	}
	
	private void updateStateInfo() {
		EntityWrapper<StorageStatsInfo> db = new EntityWrapper<StorageStatsInfo>();
		try {
			StorageStatsInfo walrusStats = db.getUnique(new StorageStatsInfo(StorageProperties.NAME));
			walrusStats.setNumberOfVolumes(numberOfVolumes);
			walrusStats.setTotalSpaceUsed(totalSpaceUsed);			
		} catch(EucalyptusCloudException ex) {
			StorageStatsInfo walrusStats = new StorageStatsInfo(StorageProperties.NAME,
					numberOfVolumes,
					totalSpaceUsed);
			db.add(walrusStats);
		}
		db.commit();
	}
}
