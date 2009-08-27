/*******************************************************************************
*Copyright (c) 2009  Eucalyptus Systems, Inc.
* 
*  This program is free software: you can redistribute it and/or modify
*  it under the terms of the GNU General Public License as published by
*  the Free Software Foundation, only version 3 of the License.
* 
* 
*  This file is distributed in the hope that it will be useful, but WITHOUT
*  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
*  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
*  for more details.
* 
*  You should have received a copy of the GNU General Public License along
*  with this program.  If not, see <http://www.gnu.org/licenses/>.
* 
*  Please contact Eucalyptus Systems, Inc., 130 Castilian
*  Dr., Goleta, CA 93101 USA or visit <http://www.eucalyptus.com/licenses/>
*  if you need additional information or have any questions.
* 
*  This file may incorporate work covered under the following copyright and
*  permission notice:
* 
*    SOFTWARE, AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
*    IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA, SANTA
*    BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY, WHICH IN
*    THE REGENTS’ DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
*    OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
*    WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
*    ANY SUCH LICENSES OR RIGHTS.
 ******************************************************************************/
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
