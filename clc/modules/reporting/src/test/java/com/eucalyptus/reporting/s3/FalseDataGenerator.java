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

package com.eucalyptus.reporting.s3;

import java.util.Iterator;
import java.util.Map;

import com.eucalyptus.entities.EntityWrapper;
import com.eucalyptus.reporting.Period;
import com.eucalyptus.reporting.event.S3Event;
import com.eucalyptus.reporting.modules.s3.*;
import com.eucalyptus.reporting.queue.*;
import com.eucalyptus.reporting.queue.QueueFactory.QueueIdentifier;
import com.eucalyptus.util.ExposedCommand;

public class FalseDataGenerator
{
	private static final int  NUM_USERS    = 32;
	private static final int  NUM_ACCOUNTS = 16;
	private static final int  SNAPSHOTS_PER_USER = 256;
	private static final long START_TIME = 1104566400000l; //Jan 1, 2005 12:00AM
	private static final int  TIME_USAGE_APART = 1000000; //ms
	private static final long MAX_MS = ((SNAPSHOTS_PER_USER+1) * TIME_USAGE_APART) + START_TIME;
	private static final long ERROR_MARGIN_MS = 60*60*1000;

	@ExposedCommand
	public static void generateFalseData()
	{
		System.out.println(" ----> GENERATING FALSE DATA");

		QueueSender queueSender = QueueFactory.getInstance().getSender(QueueIdentifier.S3);

		TestEventListener listener = new TestEventListener();
		listener.setCurrentTimeMillis(START_TIME);
		QueueReceiver queueReceiver = QueueFactory.getInstance().getReceiver(QueueIdentifier.S3);
		queueReceiver.removeAllListeners(); //Remove non-test listeners set up by bootstrapper
		queueReceiver.addEventListener(listener);
	
		
		for (int i = 0; i < SNAPSHOTS_PER_USER; i++) {
			
			long timestampMs = (i * TIME_USAGE_APART) + START_TIME;
			listener.setCurrentTimeMillis(timestampMs);

			for (int j = 0; j < NUM_USERS; j++) {
				String userId = String.format("fakeUserId-%d", j);
				String userName = String.format("fakeUserName:%d", j);
				String accountId = String.format("fakeAccountId-%d",
						(j % NUM_ACCOUNTS));
				String accountName = String.format("fakeAccountName:%d", (j % NUM_ACCOUNTS));

				long sizeMegs = 1024 + i;
				S3Event event = new S3Event(true, sizeMegs, userId, userName,
						accountId, accountName);
				queueSender.send(event);
				if (i % 10 == 0) {
					event = new S3Event(true, userId, userName, accountId,
							accountName);
					queueSender.send(event);
				}
				System.out.printf("Sending event for %d,%d\n", i, j);

			}
		}

	}


	@ExposedCommand
	public static void printUsageSummaryMap(String beginMsStr, String endMsStr)
	{
		
		long beginMs = Long.parseLong(beginMsStr);
		long endMs = Long.parseLong(endMsStr);
		Period period = new Period(beginMs, endMs);
		System.out.println(" ----> PRINT USAGE SUMMARY MAP:" + period);

		Map<S3SummaryKey, S3UsageSummary> usageSummaryMap =
			S3UsageLog.getS3UsageLog().getUsageSummaryMap(period, "admin");
		for (S3SummaryKey key: usageSummaryMap.keySet()) {
			System.out.println("key:" + key + " summary:"
					+ usageSummaryMap.get(key));
		}
	}
	
	private static boolean isWithinError(long val, long correctVal, long error)
	{
		return ((correctVal - error) < val) && (val < (correctVal + error));
	}
	

	@ExposedCommand
	public static void removeFalseData()
	{
		System.out.println(" ----> REMOVING FALSE DATA");

		S3UsageLog.getS3UsageLog().purgeLog(MAX_MS);
	}

	@ExposedCommand
	public static void removeAllData()
	{
		System.out.println(" ----> REMOVING ALL DATA");

		S3UsageLog.getS3UsageLog().purgeLog(Long.MAX_VALUE);		
	}

	/**
	 * <p>containsRecentRows checks if there are recent rows in 
	 * S3UsageSnapshot. This is used for testing: we delete
	 * all data, then set up volumes, then determine if rows made
	 * it to the DB.
	 * 
	 * @param hasRows Indicates whether there should be any
	 * rows in S3UsageSnapshot. If true, and there are no rows,
	 * and Exception is thrown; if false, and there are rows, an
	 * Exception is thrown. If true, rows are checked to verify they
	 * are relatively recent (within 1 hr). 
	 */
	@ExposedCommand
	public static void containsRecentRows(String hasRows)
	{
		boolean containsRows =
			(hasRows!=null && hasRows.equalsIgnoreCase("true"));
		System.out.println(" ----> CONTAINS RECENT ROWS:" + containsRows);
		
		EntityWrapper<S3UsageSnapshot> entityWrapper =
			EntityWrapper.get(S3UsageSnapshot.class);
		
		try {
			int rowCnt = 0;
			@SuppressWarnings("rawtypes")
			Iterator iter =
				entityWrapper.createQuery(
					"from S3UsageSnapshot as sus")
					.iterate();
			while (iter.hasNext()) {
				if (!containsRows) {
					throw new RuntimeException("Found >0 rows where 0 expected");
				}
				rowCnt++;
				S3UsageSnapshot snapshot = (S3UsageSnapshot) iter.next();
				long foundTimestampMs = snapshot.getSnapshotKey().getTimestampMs();
				long nowMs = System.currentTimeMillis();
				if (!isWithinError(nowMs, foundTimestampMs, ERROR_MARGIN_MS)) {
					throw new RuntimeException(String.format(
							"Row outside error margin, expected:%d found:%d",
							nowMs, foundTimestampMs));
				}
			}
			if (rowCnt==0 && containsRows) {
				throw new RuntimeException("Found 0 rows where >0 expected");
			}
			entityWrapper.commit();
		} catch (Exception ex) {
			entityWrapper.rollback();
			throw new RuntimeException(ex);
		}

	}


	
	public static void printFalseData()
	{
		System.out.println(" ----> PRINTING FALSE DATA");

//		S3UsageLog usageLog = S3UsageLog.getS3UsageLog();
//		Iterator<S3UsageSnapshot> iter = usageLog.scanLog(new Period(0, MAX_MS));
//		while (iter.hasNext()) {
//			S3UsageSnapshot snapshot = iter.next();
//			System.out.println(snapshot);
//		}
	}

	/**
	 * TestEventListener provides fake times which you can modify.
	 * 
	 * @author twerges
	 */
	private static class TestEventListener
		extends S3EventListener
	{
		private long fakeCurrentTimeMillis = 0l;

		protected void setCurrentTimeMillis(long currentTimeMillis)
		{
			this.fakeCurrentTimeMillis = currentTimeMillis;
		}
		
		protected long getCurrentTimeMillis()
		{
			return fakeCurrentTimeMillis;
		}
	}

}
