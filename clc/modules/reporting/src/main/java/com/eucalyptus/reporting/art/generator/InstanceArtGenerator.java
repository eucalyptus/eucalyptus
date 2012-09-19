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
package com.eucalyptus.reporting.art.generator;

import java.util.*;

import org.apache.log4j.Logger;

import com.eucalyptus.entities.EntityWrapper;
import com.eucalyptus.reporting.art.entity.*;
import com.eucalyptus.reporting.domain.*;
import com.eucalyptus.reporting.event_store.ReportingInstanceCreateEvent;
import com.eucalyptus.reporting.event_store.ReportingInstanceUsageEvent;

public class InstanceArtGenerator
	implements ArtGenerator
{
    private static Logger log = Logger.getLogger( InstanceArtGenerator.class );

    /* Metric names */
    private static final String METRIC_NET_IN_BYTES   = "NetworkIn";
    private static final String METRIC_NET_OUT_BYTES  = "NetworkOut";
    private static final String METRIC_DISK_IN_BYTES  = "DiskReadBytes";
    private static final String METRIC_DISK_OUT_BYTES = "DiskWriteBytes";
    private static final String METRIC_CPU_USAGE_MS   = "CPUUtilizationMs";
    
    private static final String DIM_TOTAL     = "total";
    private static final String DIM_ROOT	  = "root";
    private static final String DIM_INTERNAL  = "internal";
    private static final String DIM_DEFAULT   = "default";
    private static final String DIM_EPHEMERAL = "ephemeral";
    
    
    public InstanceArtGenerator()
	{
		
	}
	
	public ReportArtEntity generateReportArt(ReportArtEntity report)
	{
		log.debug("GENERATING REPORT ART");
		EntityWrapper wrapper = EntityWrapper.get( ReportingInstanceCreateEvent.class );

		/* Create super-tree of availZones, clusters, accounts, users, and instances;
		 * and create a Map of the instance usage nodes at the bottom.
		 */
		Map<String,InstanceUsageArtEntity> usageEntities = new HashMap<String,InstanceUsageArtEntity>();
		Iterator iter = wrapper.scanWithNativeQuery( "scanInstanceCreateEvents" );
		while (iter.hasNext()) {
			ReportingInstanceCreateEvent createEvent = (ReportingInstanceCreateEvent) iter.next();

			if (! report.getZones().containsKey(createEvent.getAvailabilityZone())) {
				report.getZones().put(createEvent.getAvailabilityZone(), new AvailabilityZoneArtEntity());
			}
			AvailabilityZoneArtEntity zone = report.getZones().get(createEvent.getAvailabilityZone());
			
			ReportingUser reportingUser = ReportingUserDao.getInstance().getReportingUser(createEvent.getUserId());
			if (reportingUser==null) {
				log.error("No user corresponding to event:" + createEvent.getUserId());
			}
			ReportingAccount reportingAccount = ReportingAccountDao.getInstance().getReportingAccount(reportingUser.getAccountId());
			if (reportingAccount==null) {
				log.error("No account corresponding to user:" + reportingUser.getAccountId());
			}
			if (! zone.getAccounts().containsKey(reportingAccount.getName())) {
				zone.getAccounts().put(reportingAccount.getName(), new AccountArtEntity());
			}
			AccountArtEntity account = zone.getAccounts().get(reportingAccount.getName());
			if (! account.getUsers().containsKey(reportingUser.getName())) {
				account.getUsers().put(reportingUser.getName(), new UserArtEntity());
			}
			UserArtEntity user = account.getUsers().get(reportingUser.getName());
			if (! user.getInstances().containsKey(createEvent.getUuid())) {
				user.getInstances().put(createEvent.getUuid(), new InstanceArtEntity(createEvent.getInstanceType(), createEvent.getInstanceId()));
			}
			InstanceArtEntity instance = user.getInstances().get(createEvent.getUuid());
			usageEntities.put(createEvent.getUuid(), instance.getUsage());
		}

		
		
		/* Scan through instance usage and update instance usage nodes. Also,
		 * find start and end times.
		 */
		Map<UsageEventKey,ReportingInstanceUsageEvent> lastEvents =
			new HashMap<UsageEventKey,ReportingInstanceUsageEvent>();
		Map<String,StartEndTime> startEndTimes =
			new HashMap<String,StartEndTime>();
		iter = wrapper.scanWithNativeQuery( "scanInstanceUsageEvents" );
		while (iter.hasNext()) {

			/* Grab event, last event, and usage entity to update */
			ReportingInstanceUsageEvent usageEvent = (ReportingInstanceUsageEvent) iter.next();
			UsageEventKey key = new UsageEventKey(usageEvent.getUuid(), usageEvent.getMetric(),
					usageEvent.getDimension());
			if (! lastEvents.containsKey(key)) {
				lastEvents.put(key, usageEvent);
				continue;
			}
			ReportingInstanceUsageEvent lastEvent = lastEvents.get(key);
			if (! usageEntities.containsKey(usageEvent.getUuid())) {
				log.error("usage event without corresponding instance:" + usageEvent.getUuid());
				continue;				
			}
			InstanceUsageArtEntity usage = usageEntities.get(usageEvent.getUuid());
			String metric = usageEvent.getMetric();
			String dim    = usageEvent.getDimension();
			if (usageEvent.getValue()==null || lastEvent.getValue()==null) {
				log.debug("Null metric values shouldn't occur");
				continue;
			}

			/* Update instance start and end times */
			if (! startEndTimes.containsKey(usageEvent.getUuid())) {
				startEndTimes.put(usageEvent.getUuid(),
						new StartEndTime(Math.max(report.getBeginMs(), lastEvent.getTimestampMs()),
								Math.min(report.getEndMs(), usageEvent.getTimestampMs())));
			} else {
				StartEndTime seTime = startEndTimes.get(usageEvent.getUuid());
				seTime.setStartTimeMs(Math.max(report.getBeginMs(),
						Math.min(seTime.getStartTimeMs(), lastEvent.getTimestampMs())));
				seTime.setEndTimeMs(Math.min(report.getEndMs(),
						Math.max(seTime.getEndTimeMs(), usageEvent.getTimestampMs())));
			}

			/* Update metrics in usage */
			Double value = null;
			/* Subtract last usage from this usage because all these statistics are CUMULATIVE.	 */
			if (usageEvent.getValue()!=null && lastEvent.getValue()!=null) {
				value = usageEvent.getValue() - lastEvent.getValue();
			} else if (usageEvent.getValue()!=null) {
				value = usageEvent.getValue();
			}
			Double valueMB = (value==null) ? null : value/1024/1024; //don't bitshift a double

			if (metric.equals(METRIC_NET_IN_BYTES) && dim.equals(DIM_TOTAL)) {
				usage.addNetTotalInMegs(interpolate(report.getBeginMs(), report.getEndMs(),
						lastEvent.getTimestampMs(), usageEvent.getTimestampMs(), valueMB).longValue());
			} else if (metric.equals(METRIC_NET_OUT_BYTES) && dim.equals(DIM_TOTAL)) {
				usage.addNetTotalOutMegs(interpolate(report.getBeginMs(), report.getEndMs(),
						lastEvent.getTimestampMs(), usageEvent.getTimestampMs(), valueMB).longValue());
			} else if (metric.equals(METRIC_NET_IN_BYTES)  && dim.equals(DIM_INTERNAL)) {
				usage.addNetInternalInMegs(interpolate(report.getBeginMs(), report.getEndMs(),
						lastEvent.getTimestampMs(), usageEvent.getTimestampMs(), valueMB).longValue());
			} else if (metric.equals(METRIC_NET_OUT_BYTES) && dim.equals(DIM_INTERNAL)) {
				usage.addNetTotalInMegs(interpolate(report.getBeginMs(), report.getEndMs(),
						lastEvent.getTimestampMs(), usageEvent.getTimestampMs(), valueMB).longValue());
			} else if (metric.equals(METRIC_DISK_IN_BYTES) && (dim.equals(DIM_ROOT)||dim.startsWith(DIM_EPHEMERAL))) {
				usage.addDiskInMegs(interpolate(report.getBeginMs(), report.getEndMs(),
						lastEvent.getTimestampMs(), usageEvent.getTimestampMs(), valueMB).longValue());
			} else if (metric.equals(METRIC_DISK_OUT_BYTES) && (dim.equals(DIM_ROOT)||dim.startsWith(DIM_EPHEMERAL))) {
				usage.addDiskOutMegs(interpolate(report.getBeginMs(), report.getEndMs(),
						lastEvent.getTimestampMs(), usageEvent.getTimestampMs(), valueMB).longValue());
			} else if (metric.equals(METRIC_CPU_USAGE_MS) && (dim.equals(DIM_DEFAULT))) {
				usage.addCpuUtilizationMs(interpolate(report.getBeginMs(), report.getEndMs(),
						lastEvent.getTimestampMs(), usageEvent.getTimestampMs(), value).longValue());
			} else {
				log.debug("Unrecognized metric for report:" + metric + "/" + dim);
			}

			lastEvents.put(key, usageEvent);
  		} //while 

		
		/* Update durations of all instances
		 */
		for (String uuid: startEndTimes.keySet()) {
			StartEndTime seTime = startEndTimes.get(uuid);
			if (usageEntities.containsKey(uuid)) {
				long durationMs = seTime.getEndTimeMs() - seTime.getStartTimeMs();
				usageEntities.get(uuid).setDurationMs(durationMs);
			} else {
				log.error("startEndTime without corresponding instance:" + uuid);
			}
		}
		
		
		/* Perform totals and summations
		 */
		for (String zoneName : report.getZones().keySet()) {
			AvailabilityZoneArtEntity zone = report.getZones().get(zoneName);
			UsageTotalsArtEntity zoneUsage = zone.getUsageTotals();
			for (String accountName : zone.getAccounts().keySet()) {
				AccountArtEntity account = zone.getAccounts().get(accountName);
				UsageTotalsArtEntity accountUsage = account.getUsageTotals();
				for (String userName : account.getUsers().keySet()) {
					UserArtEntity user = account.getUsers().get(userName);
					UsageTotalsArtEntity userUsage = user.getUsageTotals();
					for (String instanceUuid : user.getInstances().keySet()) {
						InstanceArtEntity instance = user.getInstances().get(instanceUuid);
						updateUsageTotals(userUsage, instance);
						updateUsageTotals(accountUsage, instance);
						updateUsageTotals(zoneUsage, instance);
					}
				}
			}
		}

		return report;
	}


	private static void updateUsageTotals(UsageTotalsArtEntity totals, InstanceArtEntity instance)
	{
		InstanceUsageArtEntity totalEntity = totals.getInstanceTotals();
		InstanceUsageArtEntity usage = instance.getUsage();
		
		/* Update metrics */
		totalEntity.addDurationMs(usage.getDurationMs());
		totalEntity.addCpuUtilizationMs(usage.getCpuUtilizationMs());
		totalEntity.addDiskInMegs(totalEntity.getDiskInMegs());
		totalEntity.addDiskOutMegs(usage.getDiskOutMegs());
		totalEntity.addNetTotalInMegs(usage.getNetTotalInMegs());
		totalEntity.addNetTotalOutMegs(usage.getNetTotalOutMegs());
		totalEntity.addNetInternalInMegs(usage.getNetInternalInMegs());
		totalEntity.addNetInternalOutMegs(usage.getNetInternalOutMegs());
		
		/* Update total running time and type count for this instance type */
		Map<String,InstanceUsageArtEntity> typeTotals = totals.getTypeTotals();
		if (!typeTotals.containsKey(instance.getInstanceType().toLowerCase())) {
			typeTotals.put(instance.getInstanceType().toLowerCase(),
					new InstanceUsageArtEntity());
		}
		InstanceUsageArtEntity typeTotal =
			typeTotals.get(instance.getInstanceType().toLowerCase());
		
		typeTotal.addInstanceCnt(1);
		typeTotal.addDurationMs(totalEntity.getDurationMs());
		
	}

	/**
	 * Interpolate usage based upon the fraction of the usage in some period which falls within
	 * report boundaries. 
	 */
	private static Double interpolate(long repBegin, long repEnd, long perBegin, long perEnd, Double currValue)
	{
		if (currValue==null) return null;
		
		final double periodDuration = (perEnd-perBegin);
		double factor = 0d;
		if (perEnd <= repBegin || perBegin >= repEnd) {
			//Period falls completely outside of report, on either end
			factor = 0d;
		} else if (perBegin < repBegin && perEnd <= repEnd) {
			//Period begin comes before report begin but period end lies within it
			factor = ((double)perEnd-repBegin)/periodDuration;
		} else if (perBegin >= repBegin && perEnd >= repEnd) {
			//Period begin lies within report but period end comes after it
			 factor = ((double)repEnd-perBegin)/periodDuration;
		} else if (perBegin >= repBegin && perEnd <= repEnd) {
			//Period falls entirely within report
			factor = 1d;
		} else if (repBegin >= perBegin && repEnd <= perEnd) {
			//Report falls entirely within period (<15 second report?)
			factor = ((double)(repBegin-perBegin)+(perEnd-repEnd))/periodDuration;
		} else {
			throw new IllegalStateException("impossible boundary condition");
		}

		if (factor < 0 || factor > 1) throw new IllegalStateException("factor<0 || factor>1");
		
//		log.debug(String.format("remainingFactor, report:%d-%d (%d), period:%d-%d (%d), factor:%f",
//				repBegin, repEnd, repEnd-repBegin, perBegin, perEnd, perEnd-perBegin, factor));

		return currValue*factor;
	}

	private static class UsageEventKey
	{
		private final String uuid;
		private final String metric;
		private final String dimension;
		
		private UsageEventKey(String uuid, String metric, String dimension)
		{
			this.uuid = uuid;
			this.metric = metric;
			this.dimension = dimension;
		}

		public String getUuid()
		{
			return uuid;
		}
		
		public String getMetric()
		{
			return metric;
		}
		
		public String getDimension()
		{
			return dimension;
		}

		@Override
		public int hashCode()
		{
			final int prime = 31;
			int result = 1;
			result = prime * result
					+ ((dimension == null) ? 0 : dimension.hashCode());
			result = prime * result
					+ ((metric == null) ? 0 : metric.hashCode());
			result = prime * result + ((uuid == null) ? 0 : uuid.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj)
		{
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			UsageEventKey other = (UsageEventKey) obj;
			if (dimension == null) {
				if (other.dimension != null)
					return false;
			} else if (!dimension.equals(other.dimension))
				return false;
			if (metric == null) {
				if (other.metric != null)
					return false;
			} else if (!metric.equals(other.metric))
				return false;
			if (uuid == null) {
				if (other.uuid != null)
					return false;
			} else if (!uuid.equals(other.uuid))
				return false;
			return true;
		}
		
		
	}
	
	private static class StartEndTime
	{
		private long startTimeMs;
		private long endTimeMs;
		
		private StartEndTime(long startTimeMs, long endTimeMs)
		{
			this.startTimeMs = startTimeMs;
			this.endTimeMs = endTimeMs;
		}

		public long getStartTimeMs()
		{
			return startTimeMs;
		}

		public void setStartTimeMs(long startTimeMs)
		{
			this.startTimeMs = startTimeMs;
		}

		public long getEndTimeMs()
		{
			return endTimeMs;
		}

		public void setEndTimeMs(long endTimeMs)
		{
			this.endTimeMs = endTimeMs;
		}
	}
	
	/**
	 * Addition with the peculiar semantics for null we need here
	 */
	private static Long plus(Long added, Long defaultVal)
	{
		if (added==null) {
			return defaultVal;
		} else if (defaultVal==null) {
			return added;
		} else {
			return (added.longValue() + defaultVal.longValue());
		}
		
	}
	
	/**
	 * Subtraction with the peculiar semantics we need here: previous value of null means zero
	 *    whereas current value of null returns null.
	 */
	private static Long subtract(Long currCumul, Long prevCumul)
	{
		if (currCumul==null) {
			return null;
		} else if (prevCumul==null) {
			return currCumul;
		} else {
		    return new Long(currCumul.longValue()-prevCumul.longValue());	
		}
	}

	private static Long max(Long a, Long b)
	{
		if (a==null) {
			return b;
		} else if (b==null) {
			return a;
		} else {
			return new Long(Math.max(a.longValue(), b.longValue()));
		}
	}

}
