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
*    Software License Agreement (BSD License)
* 
*    Copyright (c) 2008, Regents of the University of California
*    All rights reserved.
* 
*    Redistribution and use of this software in source and binary forms, with
*    or without modification, are permitted provided that the following
*    conditions are met:
* 
*      Redistributions of source code must retain the above copyright notice,
*      this list of conditions and the following disclaimer.
* 
*      Redistributions in binary form must reproduce the above copyright
*      notice, this list of conditions and the following disclaimer in the
*      documentation and/or other materials provided with the distribution.
* 
*    THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
*    IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
*    TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
*    PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
*    OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
*    EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
*    PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
*    PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
*    LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
*    NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
*    SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. USERS OF
*    THIS SOFTWARE ACKNOWLEDGE THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE
*    LICENSED MATERIAL, COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS
*    SOFTWARE, AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
*    IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA, SANTA
*    BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY, WHICH IN
*    THE REGENTS’ DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
*    OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
*    WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
*    ANY SUCH LICENSES OR RIGHTS.
*******************************************************************************/
package edu.ucsb.eucalyptus.cloud.ws;

import org.apache.log4j.Logger;

import com.eucalyptus.entities.EntityWrapper;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.WalrusProperties;

import edu.ucsb.eucalyptus.cloud.entities.WalrusStatsInfo;
import edu.ucsb.eucalyptus.msgs.WalrusUsageStatsRecord;

public class WalrusStatistics {
	private Logger LOG = Logger.getLogger( WalrusStatistics.class );
	private Long totalBytesIn;
	private Long totalBytesOut;
	private Long totalSpaceUsed;
	private Integer numberOfBuckets;

	public WalrusStatistics() {
		totalBytesIn = 0L;
		totalBytesOut = 0L;
		totalSpaceUsed = 0L;
		numberOfBuckets = 0;
		getStateInfo();
		dump();
	}

	public void updateBytesIn(Long bytes) {
		totalBytesIn += bytes;
		dump();
	}

	public void updateBytesOut(Long bytes) {
		totalBytesOut += bytes;
		dump();
	}

	public void updateSpaceUsed(Long bytes) {
		totalSpaceUsed += bytes;
		updateStateInfo();
		dump();
	}

	public void incrementBucketCount() {
		numberOfBuckets++;
		updateStateInfo();
		dump();
	}

	public void decrementBucketCount() {
		numberOfBuckets--;
		updateStateInfo();
		dump();
	}

	public void resetBytesIn() {
		totalBytesIn = 0L;
	}

	public void resetBytesOut() {
		totalBytesOut = 0L;
	}

	public void dump() {
		LOG.info(WalrusUsageStatsRecord.create(totalBytesIn, totalBytesOut, numberOfBuckets, totalSpaceUsed));
	}

	private void getStateInfo() {
		EntityWrapper<WalrusStatsInfo> db = new EntityWrapper<WalrusStatsInfo>("eucalyptus_walrus");
		try {
			WalrusStatsInfo walrusStats = db.getUnique(new WalrusStatsInfo(WalrusProperties.NAME));
			numberOfBuckets = walrusStats.getNumberOfBuckets();
			totalSpaceUsed = walrusStats.getTotalSpaceUsed();
		} catch(EucalyptusCloudException ex) {
			WalrusStatsInfo walrusStats = new WalrusStatsInfo(WalrusProperties.NAME,
					numberOfBuckets,
					totalSpaceUsed);
			db.add(walrusStats);
		}
		db.commit();
	}
	
	private void updateStateInfo() {
		EntityWrapper<WalrusStatsInfo> db = new EntityWrapper<WalrusStatsInfo>("eucalyptus_walrus");
		try {
			WalrusStatsInfo walrusStats = db.getUnique(new WalrusStatsInfo(WalrusProperties.NAME));
			walrusStats.setNumberOfBuckets(numberOfBuckets);
			walrusStats.setTotalSpaceUsed(totalSpaceUsed);			
		} catch(EucalyptusCloudException ex) {
			WalrusStatsInfo walrusStats = new WalrusStatsInfo(WalrusProperties.NAME,
					numberOfBuckets,
					totalSpaceUsed);
			db.add(walrusStats);
		}
		db.commit();
	}
}
