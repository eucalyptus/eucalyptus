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

package com.eucalyptus.reporting.modules.instance;

import java.util.*;

import org.apache.log4j.Logger;

import com.eucalyptus.configurable.ConfigurableClass;
import com.eucalyptus.entities.EntityWrapper;
import com.eucalyptus.reporting.Period;

/**
 * <p>InstanceUsageLog is the main API for accessing usage information which
 * has been stored in the usage log.
 * 
 * <p>The usage data in logs is <i>sampled</i>, meaning data is collected
 * every <i>n</i> seconds and written. As a result, some small error will
 * be introduced if the boundaries of desired periods (ie months) do not
 * exactly correspond to the boundaries of the samples. In that case, the
 * reporting mechanism will be unable to determine how much of the usage in
 * a sample belongs to which of the two periods whose boundaries it crosses,
 * so it will assign usage to one period based on a rule.
 * 
 * <p>Very recent information (within the prior five minutes, for example)
 * may not have been acquired yet, in which case, an empty period or a
 * period with incomplete information may be returned.
 */
@ConfigurableClass(root="instanceLog", alias="basic", description="Configuration for instance usage sampling and logging", singleton=true)
public class InstanceUsageLog
{
	private static Logger log = Logger.getLogger( InstanceUsageLog.class );

	private static InstanceUsageLog singletonInstance = null;

	private InstanceUsageLog()
	{
	}

	public static synchronized InstanceUsageLog getInstanceUsageLog()
	{
		if (singletonInstance==null) {
			singletonInstance = new InstanceUsageLog();
		}
		return singletonInstance;
	}


	/**
	 * Permanently purges data older than a certain timestamp from the log. 
	 */
	public void purgeLog(long earlierThanMs)
	{
		log.info(String.format("purge earlierThan:%d ", earlierThanMs));

		EntityWrapper<InstanceAttributes> entityWrapper = EntityWrapper.get(InstanceAttributes.class);
		try {
			
			/* Delete older instance snapshots
			 */
			entityWrapper.createSQLQuery("DELETE FROM instance_usage_snapshot WHERE timestamp_ms < ?")
				.setLong(0, new Long(earlierThanMs))
				.executeUpdate();

			/* Delete all reporting instances which no longer have even a
			 * a single corresponding instance usage snapshot, using
			 * MySQL's fancy multi-table delete with left outer join syntax.
			 */
			entityWrapper.createSQLQuery(
					"DELETE reporting_instance" 
					+ " FROM reporting_instance"
					+ " LEFT OUTER JOIN instance_usage_snapshot"
					+ " ON reporting_instance.uuid = instance_usage_snapshot.uuid"
					+ " WHERE instance_usage_snapshot.uuid IS NULL")
				.executeUpdate();
			entityWrapper.commit();
		} catch (Exception ex) {
			log.error(ex);
			entityWrapper.rollback();
			throw new RuntimeException(ex);
		}
	}
	
	
	/**
	 * <p>Find the latest snapshot before timestampMs.
	 */
	long findLatestAllSnapshotBefore(long timestampMs)
	{
		long foundTimestampMs = 0l;

		EntityWrapper<InstanceUsageSnapshot> entityWrapper = null;
		try {

			/* Iteratively query before startingMs, moving backward in
			 * exponentially growing intervals, starting at 3 hrs before
			 */
	        for (double minsBefore=180; /* 3 hrs */
	        	 System.currentTimeMillis()-(long)(minsBefore*60*1000) > 0;
	        	 minsBefore=Math.pow(minsBefore, 1.1))
	        {
	            long queryStartMs = System.currentTimeMillis()-(long)(minsBefore*60*1000);

	        	entityWrapper = EntityWrapper.get(InstanceUsageSnapshot.class);
				
				log.info("Searching for latest timestamp before beginning:" + queryStartMs);
				@SuppressWarnings("rawtypes")
				List iuses =
					entityWrapper.createQuery(
						"from InstanceUsageSnapshot as ius"
						+ " WHERE ius.timestampMs > ?"
						+ " AND ius.timestampMs < ?")
						.setLong(0, new Long(queryStartMs))
						.setLong(1, new Long(timestampMs))
						.list();
				for (Object obj: iuses) {
					InstanceUsageSnapshot snapshot = (InstanceUsageSnapshot) obj;
					foundTimestampMs = snapshot.getTimestampMs();
				}
				entityWrapper.commit();
				if (foundTimestampMs != 0l) break;
			}
			log.info("Found latest timestamp before beginning:"
					+ foundTimestampMs);			
		} catch (Exception ex) {
			log.error(ex);
			if (entityWrapper != null) entityWrapper.rollback();
			throw new RuntimeException(ex);
		}
		
		return foundTimestampMs;	
	}

	
	
	/**
	 * <p>Gather a Map of all Instance resource usage for a period.
	 */
    public Map<InstanceSummaryKey, InstanceUsageSummary> getUsageSummaryMap(
    		Period period, String accountId)
    {
    	log.info("GetUsageSummaryMap period:" + period);

		final Map<InstanceSummaryKey, InstanceUsageSummary> usageMap =
    		new HashMap<InstanceSummaryKey, InstanceUsageSummary>();

		EntityWrapper<InstanceUsageSnapshot> entityWrapper =
			EntityWrapper.get(InstanceUsageSnapshot.class);
		try {

			/* Start query from last snapshot before report beginning, and
			 * iterate through the data until after the end. We'll truncate and
			 * extrapolate.
			 */
			long latestSnapshotBeforeMs =
				findLatestAllSnapshotBefore(period.getBeginningMs());
			long afterEnd = period.getEndingMs() 
					+ ((period.getBeginningMs()-latestSnapshotBeforeMs)*2);
			log.debug("latestSnapshotBeforeMs:" + latestSnapshotBeforeMs + " afterEndMs:" + afterEnd);

			
			@SuppressWarnings("rawtypes")
			List list = null;
			
			if (accountId == null) {
				list = entityWrapper.createQuery(
					"from InstanceAttributes as ia, InstanceUsageSnapshot as ius"
					+ " where ia.uuid = ius.uuid"
					+ " and ius.timestampMs > ?"
					+ " and ius.timestampMs < ?"
					+ " order by ius.timestampMs")
					.setLong(0, latestSnapshotBeforeMs)
					.setLong(1, afterEnd)
					.list();
			} else {
				list = entityWrapper.createQuery(
						"from InstanceAttributes as ia, InstanceUsageSnapshot as ius"
						+ " where ia.uuid = ius.uuid"
						+ " and ia.accountId = ?"
						+ " and ius.timestampMs > ?"
						+ " and ius.timestampMs < ?"
						+ " order by ius.timestampMs")
						.setString(0, accountId)
						.setLong(1, latestSnapshotBeforeMs)
						.setLong(2, afterEnd)
						.list();		
			}
			

			
			/* Accumulate data over timeline, by instance, keyed by instance uuid.
			 * Accumulated data consists of the instance running time, network
			 * io megs, and disk io megs for each instance.
			 */
			Map<String,InstanceDataAccumulator> dataAccumulatorMap =
				new HashMap<String,InstanceDataAccumulator>();
			
			for (Object obj: list) {

				Object[] row = (Object[]) obj;
				InstanceAttributes insAttrs = (InstanceAttributes) row[0];
				InstanceUsageSnapshot snapshot = (InstanceUsageSnapshot) row[1];

				log.debug("Found row attrs:" + insAttrs + " snapshot:" + snapshot);
				
				String uuid = insAttrs.getUuid();
				if ( !dataAccumulatorMap.containsKey( uuid ) ) {
					InstanceDataAccumulator accumulator =
						new InstanceDataAccumulator( insAttrs, snapshot, period );
					dataAccumulatorMap.put( uuid, accumulator );
				} else {
					InstanceDataAccumulator accumulator =
						dataAccumulatorMap.get( uuid );
					accumulator.update( snapshot );
				}

			}

			
			/* Summarize usage for each (zone,cluster,acct,user) key, by
			 * summing all usage for all instances for each key. Populate
			 * the usageMap, which is what we return.
			 */
			for (String uuid: dataAccumulatorMap.keySet()) {
				log.debug("Instance uuid:" + uuid);
				InstanceDataAccumulator accumulator =
					dataAccumulatorMap.get(uuid);
				InstanceSummaryKey key =
					new InstanceSummaryKey(accumulator.getInstanceAttributes());
				if (! usageMap.containsKey(key)) {
					usageMap.put(key, new InstanceUsageSummary());
				}
				InstanceUsageSummary ius = usageMap.get(key);
				ius.addDiskIoMegs(accumulator.getDiskIoMegs());
				ius.addNetworkIoMegs(accumulator.getNetIoMegs());
				ius.sumFromDurationSecsAndType(accumulator.getDurationSecs(),
						accumulator.getInstanceAttributes().getInstanceType());
				log.debug("Instance summary:" + ius);
			}

			entityWrapper.commit();
		} catch (Exception ex) {
			log.error(ex);
			entityWrapper.rollback();
			throw new RuntimeException(ex);
		}

		
		if (log.isDebugEnabled()) {
			log.debug("Printing usageMap");
			for (InstanceSummaryKey key: usageMap.keySet()) {
				log.debug("key:" + key + " summary:" + usageMap.get(key));
			}
		}

        return usageMap;
    }


    /**
     * Accumulate data for a <i>single instance</i>.
     * InstanceDataAccumulator will accumulate a series of
     * InstanceUsageSnapshot if you call the <code>update</code> method with
     * each snapshot, after which it can return the total duration of the
     * instance, disk io usage, and net io usage for the instance.
     */
    private class InstanceDataAccumulator
    {
    	private final InstanceAttributes insAttrs;
    	private InstanceUsageSnapshot lastSnapshot;
    	private InstanceUsageSnapshot firstSnapshot;
    	private InstanceUsageSnapshot priorSnapshot;
    	private Period period;
    	private boolean accumulate = false;
    	private boolean doneAccumulating = false;
    	
    	public InstanceDataAccumulator(InstanceAttributes insAttrs,
    			InstanceUsageSnapshot snapshot, Period period)
		{
			super();
			this.insAttrs = insAttrs;
			this.lastSnapshot = snapshot;
			this.priorSnapshot = snapshot;
			this.firstSnapshot = snapshot;
			this.period = period;
		}
    	
    	public void update(InstanceUsageSnapshot snapshot)
    	{
    		log.debug("Update time:" + snapshot.getTimestampMs() + " insId:" + insAttrs.getInstanceId());
    		if (!accumulate
    			&& snapshot.getTimestampMs() <= period.getEndingMs()
    			&& (priorSnapshot.getTimestampMs() >= period.getBeginningMs()
    				|| snapshot.getTimestampMs() >= period.getBeginningMs()))
    		{
    			accumulate = true;
	    		log.debug("Start accumulating:" + priorSnapshot.getTimestampMs());
    		}
    		if (accumulate && !doneAccumulating) {
    	    	accumulateDiskIoMegs(snapshot);
    	    	accumulateNetIoMegs(snapshot);
    	    	if (priorSnapshot.getTimestampMs() > period.getEndingMs()) {
    	    		log.debug("Done accumulating:" + priorSnapshot.getTimestampMs());
    	    		doneAccumulating = true;
    	    	}
    		}
    		this.lastSnapshot = snapshot;
    		this.priorSnapshot = snapshot;    				
    	}

    	public InstanceAttributes getInstanceAttributes()
    	{
    		return this.insAttrs;
    	}
    	
    	public long getDurationSecs()
    	{
    		/* If report period does not overlap usage at all, then duration is 0 */
    		if (period.getBeginningMs() >= lastSnapshot.getTimestampMs()
    				|| period.getEndingMs() <= firstSnapshot.getTimestampMs()) {
    			log.debug("duration=0, period does not overlap at all, report:" + period.getBeginningMs()
    					+ "-" + period.getEndingMs() + ", snaps:" + firstSnapshot.getTimestampMs() + "-"
    					+ lastSnapshot.getTimestampMs());
    			return 0l;
    		} else {
    			///This is all wrong; it finds the beginning regardless...
    			long truncatedBeginMs = Math.max(period.getBeginningMs(), firstSnapshot.getTimestampMs());
    			long truncatedEndMs   = Math.min(period.getEndingMs(), lastSnapshot.getTimestampMs());
    			log.debug("truncated:" + truncatedBeginMs + "-" + truncatedEndMs);
        		return ( truncatedEndMs-truncatedBeginMs ) / 1000;
    		}
    	}

    	
    	/**
    	 * Calculate what fraction of the current reporting period lies within the
    	 * begin and end of the generated report period.
    	 * 
    	 * @returns a double from 0 to 1
    	 */
    	private double calcWithinFactor(long timestampMs)
    	{
    		final double periodDuration = (double)(timestampMs-priorSnapshot.getTimestampMs());
    		double result = 0d;
    		if (periodDuration==0) return 0d;
    		
    		final long perBegin = priorSnapshot.getTimestampMs();
    		final long perEnd = timestampMs;
    		final long repBegin = period.getBeginningMs();
    		final long repEnd = period.getEndingMs();
    		
    		if (perEnd <= repBegin || perBegin >= repEnd) {
    			//Period falls completely outside of report, on either end
    			result = 0d;
    		} else if (perBegin < repBegin && perEnd <= repEnd) {
    			//Period begin comes before report begin but period end lies within it
    			result = ((double)perEnd-repBegin)/periodDuration;
    		} else if (perBegin >= repBegin && perEnd >= repEnd) {
    			//Period begin lies within report but period end comes after it
    			 result = ((double)repEnd-perBegin)/periodDuration;
    		} else if (perBegin >= repBegin && perEnd <= repEnd) {
    			//Period falls entirely within report
    			result = 1d;
    		} else if (repBegin >= perBegin && repEnd <= perEnd) {
    			//Report falls entirely within period (<15 second report?)
    			result = ((double)(repBegin-perBegin)+(perEnd-repEnd))/periodDuration;
    		} else {
    			throw new IllegalStateException("impossible boundary condition");
    		}

    		if (result < 0 || result > 1) throw new IllegalStateException("result<0 || result>1");
    		
    		log.debug(String.format("remainingFactor, report:%d-%d (%d), period:%d-%d (%d), factor:%f",
    				repBegin, repEnd, repEnd-repBegin, perBegin, perEnd, perEnd-perBegin, result));
    		
    		return result;
    	}

    	
    	private long accumulatedDiskIoMegs = 0l;
    	private long lastCumulativeDiskIoMegs = 0l;
    	
    	public void accumulateDiskIoMegs(InstanceUsageSnapshot snapshot)
    	{
    		double remainingFactor = calcWithinFactor(snapshot.getTimestampMs());
       		this.accumulatedDiskIoMegs += (long)(remainingFactor*(snapshot.getCumulativeDiskIoMegs()-lastCumulativeDiskIoMegs));
    		this.lastCumulativeDiskIoMegs = snapshot.getCumulativeDiskIoMegs();
    	}
    	
    	public long getDiskIoMegs()
    	{
    		return this.accumulatedDiskIoMegs;
    	}
    	

    	private long accumulatedNetIoMegs = 0l;
    	private long lastCumulativeNetIoMegs = 0l;

    	public long getNetIoMegs()
    	{
    		return this.accumulatedNetIoMegs;
    	}

    	public void accumulateNetIoMegs(InstanceUsageSnapshot snapshot)
    	{
    		double remainingFactor = calcWithinFactor(snapshot.getTimestampMs());
        	this.accumulatedNetIoMegs += (long)(remainingFactor*(snapshot.getCumulativeNetworkIoMegs()-lastCumulativeNetIoMegs));    			
    		this.lastCumulativeNetIoMegs = snapshot.getCumulativeNetworkIoMegs();
    	}
    	
    }

}
