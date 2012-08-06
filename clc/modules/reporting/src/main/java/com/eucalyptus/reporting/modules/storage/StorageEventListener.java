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
 *
 * This file may incorporate work covered under the following copyright
 * and permission notice:
 *
 *   Software License Agreement (BSD License)
 *
 *   Copyright (c) 2008, Regents of the University of California
 *   All rights reserved.
 *
 *   Redistribution and use of this software in source and binary forms,
 *   with or without modification, are permitted provided that the
 *   following conditions are met:
 *
 *     Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *     Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer
 *     in the documentation and/or other materials provided with the
 *     distribution.
 *
 *   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *   "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *   LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 *   FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 *   COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 *   INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 *   BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *   LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 *   CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 *   LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 *   ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 *   POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 *   THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 *   COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 *   AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 *   IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 *   SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 *   WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 *   REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 *   IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 *   NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/

package com.eucalyptus.reporting.modules.storage;

import java.util.Map;

import org.apache.log4j.Logger;

import com.eucalyptus.entities.EntityWrapper;
import com.eucalyptus.event.EventListener;
import com.eucalyptus.reporting.event.Event;
import com.eucalyptus.reporting.event.StorageEvent;
import com.eucalyptus.reporting.domain.ReportingAccountCrud;
import com.eucalyptus.reporting.domain.ReportingUserCrud;

public class StorageEventListener
	implements EventListener<Event>
{
	private static Logger LOG = Logger.getLogger( StorageEventListener.class );
	
	private static long WRITE_INTERVAL_MS = 10800000l; //write every 3 hrs

	private Map<StorageSummaryKey, StorageUsageData> usageDataMap;
	private long lastAllSnapshotMs = 0l;

	public StorageEventListener()
	{
	}
	
	@Override
	public void fireEvent(Event event)
	{
		if (event instanceof StorageEvent) {
			StorageEvent storageEvent = (StorageEvent) event;

			
			/* Retain records of all account and user id's and names encountered
			 * even if they're subsequently deleted.
			 */
			ReportingAccountCrud.getInstance().createOrUpdateAccount(
					storageEvent.getAccountId(), storageEvent.getAccountName());
			ReportingUserCrud.getInstance().createOrUpdateUser(storageEvent.getOwnerId(), storageEvent.getAccountId(),
					storageEvent.getOwnerName());


			long timeMillis = getCurrentTimeMillis();

			final StorageUsageLog usageLog = StorageUsageLog.getStorageUsageLog();

			EntityWrapper<StorageUsageSnapshot> entityWrapper =
				EntityWrapper.get( StorageUsageSnapshot.class );
			try {

				/* Load usageDataMap if starting up
				 */
				if (usageDataMap == null) {

					this.usageDataMap = usageLog.findLatestUsageData();
					LOG.info("Loaded usageDataMap");

				}

				
				/* Update usageDataMap
				 */
				StorageSummaryKey key = new StorageSummaryKey(
						storageEvent.getOwnerId(),
						storageEvent.getAccountId(),
						storageEvent.getClusterName(),
						storageEvent.getAvailabilityZone());
				StorageUsageData usageData;
				if (usageDataMap.containsKey(key)) {
					usageData = usageDataMap.get(key);
				} else {
					usageData = new StorageUsageData();
					usageDataMap.put(key, usageData);
				}
				long addAmountMegs = (storageEvent.isCreateOrDelete())
						? storageEvent.getSizeMegs()
						: -storageEvent.getSizeMegs();
				long addNum = (storageEvent.isCreateOrDelete()) ? 1 : -1;
				LOG.debug("Receive event:" + storageEvent.toString() + " usageData:" + usageData + " addAmountMegs:" + addAmountMegs + " addNum:" + addNum);

				switch(storageEvent.getEventType()) {
					case EbsSnapshot:
						Long newSnapshotsNum =
							addLong(usageData.getSnapshotsNum(), addNum);
						if (newSnapshotsNum!=null && newSnapshotsNum.longValue()<0) {
							throw new IllegalStateException("Snapshots num cannot be negative");
						}
						usageData.setSnapshotsNum(newSnapshotsNum);
						Long newSnapshotsMegs =
							addLong(usageData.getSnapshotsMegs(), addAmountMegs);
						if (newSnapshotsMegs!=null && newSnapshotsMegs.longValue()<0) {
							throw new IllegalStateException("Snapshots megs cannot be negative");
						}
						usageData.setSnapshotsMegs(newSnapshotsMegs);
						break;
					case EbsVolume:
						Long newVolumesNum =
							addLong(usageData.getVolumesNum(), addNum);
						if (newVolumesNum!=null && newVolumesNum.longValue()<0) {
							throw new IllegalStateException("Volumes num cannot be negative");
						}
						usageData.setVolumesNum(newVolumesNum);
						Long newVolumesMegs =
							addLong(usageData.getVolumesMegs(), addAmountMegs);
						if (newVolumesMegs!=null && newVolumesMegs.longValue()<0) {
							throw new IllegalStateException("Volumes megs cannot be negative");
						}
						usageData.setVolumesMegs(newVolumesMegs);						
						break;

				}

				/* Write data to DB
				 */
				if ((timeMillis - lastAllSnapshotMs) > WRITE_INTERVAL_MS) {
					/* Write all snapshots
					 */
					LOG.info("Starting allSnapshot...");
					for (StorageSummaryKey summaryKey: usageDataMap.keySet()) {
						StorageSnapshotKey snapshotKey = new StorageSnapshotKey(
								summaryKey.getOwnerId(), summaryKey.getAccountId(),
								summaryKey.getClusterName(),
								summaryKey.getAvailabilityZone(), timeMillis);
						StorageUsageSnapshot sus =
							new StorageUsageSnapshot(snapshotKey, usageDataMap.get(key));
						sus.setAllSnapshot(true);
						LOG.info("Storing as part of allSnapshot:" + sus);
						entityWrapper.add(sus);
						lastAllSnapshotMs = timeMillis;
					}
					LOG.info("Ending allSnapshot...");
				} else {
					/* Write this snapshot
					 */
					StorageSnapshotKey snapshotKey = new StorageSnapshotKey(
							key.getOwnerId(), key.getAccountId(), key.getClusterName(),
							key.getAvailabilityZone(), timeMillis);
					StorageUsageSnapshot sus =
						new StorageUsageSnapshot(snapshotKey, usageDataMap.get(key));
					LOG.info("Storing:" + sus);
					entityWrapper.add(sus);
				}

				entityWrapper.commit();
			} catch (Exception ex) {
				entityWrapper.rollback();
				LOG.error(ex);		
			}
		}
	}
	
	private static final Long addLong(Long a, long b)
	{
		return new Long(a.longValue() + b);
	}

	protected long getCurrentTimeMillis()
	{
		return (this.testCurrentTimeMillis == null)
				? System.currentTimeMillis()
				: this.testCurrentTimeMillis.longValue();
	}

	private Long testCurrentTimeMillis = null;

	/**
	 * Used only for testing. Sets the poller into test mode and overrides the
	 * actual timestamp with a false one.
	 */
	protected void setCurrentTimeMillis(long testCurrentTimeMillis)
	{
		this.testCurrentTimeMillis = new Long(testCurrentTimeMillis);
	}

}
