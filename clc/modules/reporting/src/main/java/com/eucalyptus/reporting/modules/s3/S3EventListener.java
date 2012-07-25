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

package com.eucalyptus.reporting.modules.s3;

import java.util.Map;

import org.apache.log4j.Logger;

import com.eucalyptus.entities.EntityWrapper;
import com.eucalyptus.event.EventListener;
import com.eucalyptus.reporting.event.Event;
import com.eucalyptus.reporting.event.S3Event;
import com.eucalyptus.reporting.user.ReportingAccountDao;
import com.eucalyptus.reporting.user.ReportingUserDao;

public class S3EventListener
	implements EventListener<Event>
{
	private static Logger LOG = Logger.getLogger( S3EventListener.class );
	
	private static long WRITE_INTERVAL_MS = 10800000l; //write every 3 hrs

	private Map<S3SummaryKey, S3UsageData> usageDataMap;
	private long lastAllSnapshotMs = 0l;

	public S3EventListener()
	{
	}
	
	@Override
	public void fireEvent(Event event)
	{
		if (event instanceof S3Event) {
			S3Event s3Event = (S3Event) event;

			/* Retain records of all account and user id's and names encountered
			 * even if they're subsequently deleted.
			 */
			ReportingAccountDao.getInstance().addUpdateAccount(
					s3Event.getAccountId(), s3Event.getAccountName());
			ReportingUserDao.getInstance().addUpdateUser(s3Event.getOwnerId(),
					s3Event.getOwnerName());

			long timeMillis = getCurrentTimeMillis();

			final S3UsageLog usageLog = S3UsageLog.getS3UsageLog();

			EntityWrapper<S3UsageSnapshot> entityWrapper =
				EntityWrapper.get( S3UsageSnapshot.class );
			try {

				/* Load usageDataMap if starting up
				 */
				if (usageDataMap == null) {

					this.usageDataMap = usageLog.findLatestUsageData();
					LOG.info("Loaded usageDataMap");

				}

				
				/* Update usageDataMap
				 */
				S3SummaryKey key = new S3SummaryKey(s3Event.getOwnerId(),
						s3Event.getAccountId());
				S3UsageData usageData;
				if (usageDataMap.containsKey(key)) {
					usageData = usageDataMap.get(key);
				} else {
					usageData = new S3UsageData();
					usageDataMap.put(key, usageData);
				}
				long addNum = (s3Event.isCreateOrDelete()) ? 1 : -1;

				if (s3Event.isObjectOrBucket()) {
					
					long addAmountMegs = (s3Event.isCreateOrDelete())
						? s3Event.getSizeMegs()
						: -s3Event.getSizeMegs();
					LOG.debug("Receive event:" + s3Event.toString() + " usageData:" + usageData + " addNum:" + addNum + " addAmountMegs:" + addAmountMegs);

					Long newObjectsNum =
						addLong(usageData.getObjectsNum(), addNum);
					if (newObjectsNum!=null && newObjectsNum.longValue()<0) {
						throw new IllegalStateException("Objects num cannot be negative");
					}
					usageData.setObjectsNum(newObjectsNum);
					Long newObjectsMegs =
						addLong(usageData.getObjectsMegs(), addAmountMegs);
					if (newObjectsMegs!=null && newObjectsMegs.longValue()<0) {
						throw new IllegalStateException("Objects megs cannot be negative");
					}
					usageData.setObjectsMegs(newObjectsMegs);
					
				} else {
					
					Long newBucketsNum =
						addLong(usageData.getBucketsNum(), addNum);
					if (newBucketsNum!=null && newBucketsNum.longValue()<0) {
						throw new IllegalStateException("Buckets num cannot be negative");
					}
					usageData.setBucketsNum(newBucketsNum);
					LOG.debug("Receive event:" + s3Event.toString() + " usageData:" + usageData + " addNum:" + addNum);

					
				}

				/* Write data to DB
				 */
				if ((timeMillis - lastAllSnapshotMs) > WRITE_INTERVAL_MS) {
					/* Write all snapshots
					 */
					LOG.info("Starting allSnapshot...");
					for (S3SummaryKey summaryKey: usageDataMap.keySet()) {
						S3SnapshotKey snapshotKey = new S3SnapshotKey(
								summaryKey.getOwnerId(), summaryKey.getAccountId(),
								timeMillis);
						S3UsageSnapshot sus =
							new S3UsageSnapshot(snapshotKey, usageDataMap.get(key));
						sus.setAllSnapshot(true);
						LOG.info("Storing part of allSnapshot:" + sus);
						entityWrapper.add(sus);
						lastAllSnapshotMs = timeMillis;
					}
					LOG.info("Ending allSnapshot...");
				} else {
					/* Write this snapshot
					 */
					S3SnapshotKey snapshotKey = new S3SnapshotKey(
							key.getOwnerId(), key.getAccountId(),
							timeMillis);
					S3UsageSnapshot sus =
						new S3UsageSnapshot(snapshotKey, usageDataMap.get(key));
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

	/**
	 * Overridable for the purpose of testing. Testing generates fake events
	 * with fake times.
	 */
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
