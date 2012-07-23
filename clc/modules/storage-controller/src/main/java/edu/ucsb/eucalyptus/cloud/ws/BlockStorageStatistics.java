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

	public Long getTotalSpaceUsed() {
		return totalSpaceUsed;
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

	public void incrementVolumeCount(long bytes) {
		totalSpaceUsed += bytes;
		numberOfVolumes++;
		updateStateInfo();
		dump();
	}

	public void decrementVolumeCount(long bytes) {
		totalSpaceUsed += bytes;
		numberOfVolumes--;
		updateStateInfo();
		dump();
	}

	public void dump() {
		LOG.info(StorageUsageStatsRecord.create(numberOfVolumes, totalSpaceUsed));
	}

	private void getStateInfo() {
		EntityWrapper<StorageStatsInfo> db = StorageProperties.getEntityWrapper();
		try {
			StorageStatsInfo storageStats = db.getUnique(new StorageStatsInfo(StorageProperties.NAME));
			numberOfVolumes = storageStats.getNumberOfVolumes();
			totalSpaceUsed = storageStats.getTotalSpaceUsed();
		} catch(EucalyptusCloudException ex) {
			StorageStatsInfo storageStats = new StorageStatsInfo(StorageProperties.NAME,
					numberOfVolumes,
					totalSpaceUsed);
			db.add(storageStats);
		}
		db.commit();
	}

	private void updateStateInfo() {
		EntityWrapper<StorageStatsInfo> db = StorageProperties.getEntityWrapper();
		try {
			StorageStatsInfo storageStats = db.getUnique(new StorageStatsInfo(StorageProperties.NAME));
			storageStats.setNumberOfVolumes(numberOfVolumes);
			storageStats.setTotalSpaceUsed(totalSpaceUsed);			
		} catch(EucalyptusCloudException ex) {
			StorageStatsInfo walrusStats = new StorageStatsInfo(StorageProperties.NAME,
					numberOfVolumes,
					totalSpaceUsed);
			db.add(walrusStats);
		}
		db.commit();
	}
}
