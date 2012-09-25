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
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.eucalyptus.reporting.art.entity.*;
import com.eucalyptus.reporting.art.util.DurationCalculator;
import com.eucalyptus.reporting.art.util.StartEndTimes;
import com.eucalyptus.reporting.domain.*;
import com.eucalyptus.reporting.event_store.ReportingInstanceCreateEvent;
import com.eucalyptus.reporting.event_store.ReportingInstanceUsageEvent;
import com.google.common.base.Objects;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

public class InstanceArtGenerator extends AbstractArtGenerator
{
	private static final Logger log = Logger.getLogger( InstanceArtGenerator.class );

	/* Metric names */
	private static final String METRIC_NET_IN_BYTES   = "NetworkIn";
	private static final String METRIC_NET_OUT_BYTES  = "NetworkOut";
	private static final String METRIC_DISK_IN_BYTES  = "DiskReadBytes";
	private static final String METRIC_DISK_OUT_BYTES = "DiskWriteBytes";
	private static final String METRIC_DISK_READ_OPS  = "DiskReadOps";
	private static final String METRIC_DISK_WRITE_OPS = "DiskWriteOps";
	private static final String METRIC_CPU_USAGE_MS   = "CPUUtilization";
	private static final String METRIC_VOLUME_READ    = "VolumeTotalReadTime";
	private static final String METRIC_VOLUME_WRITE   = "VolumeTotalWriteTime";

	private static final String DIM_TOTAL     = "total";
	private static final String DIM_DEFAULT   = "default";

	@Override
	public ReportArtEntity generateReportArt(ReportArtEntity report)
	{
		log.debug("Generating report ART");

		/* Create super-tree of availZones, clusters, accounts, users, and instances;
		 * and create a Map of the instance usage nodes at the bottom.
		 */
		final Map<String,InstanceUsageArtEntity> usageEntities = Maps.newHashMap();
		final Iterator<ReportingInstanceCreateEvent> createEventIterator = getInstanceCreateEventIterator();
		while (createEventIterator.hasNext()) {
			final ReportingInstanceCreateEvent createEvent = createEventIterator.next();
			if ( createEvent.getTimestampMs() > report.getEndMs() ) {
				break; // end of relevant events
			}

			if (! report.getZones().containsKey(createEvent.getAvailabilityZone())) {
				report.getZones().put(createEvent.getAvailabilityZone(), new AvailabilityZoneArtEntity());
			}
			final AvailabilityZoneArtEntity zone = report.getZones().get( createEvent.getAvailabilityZone() );
			
			final ReportingUser reportingUser = getUserById( createEvent.getUserId() );
			if (reportingUser==null) {
				log.error("No user corresponding to event:" + createEvent.getUserId());
				continue;
			}
			final ReportingAccount reportingAccount = getAccountById(reportingUser.getAccountId());
			if (reportingAccount==null) {
				log.error("No account corresponding to user:" + reportingUser.getAccountId());
				continue;
			}
			if (! zone.getAccounts().containsKey(reportingAccount.getName())) {
				zone.getAccounts().put(reportingAccount.getName(), new AccountArtEntity());
			}
			final AccountArtEntity account = zone.getAccounts().get(reportingAccount.getName());
			if (! account.getUsers().containsKey(reportingUser.getName())) {
				account.getUsers().put(reportingUser.getName(), new UserArtEntity());
			}
			final UserArtEntity user = account.getUsers().get(reportingUser.getName());
			if (! user.getInstances().containsKey(createEvent.getUuid())) {
				user.getInstances().put(createEvent.getUuid(), new InstanceArtEntity(createEvent.getInstanceType(), createEvent.getInstanceId()));
			}
			final InstanceArtEntity instance = user.getInstances().get(createEvent.getUuid());
			instance.getUsage().addInstanceCnt(1);
			usageEntities.put(createEvent.getUuid(), instance.getUsage());
		}

		/* Scan through instance usage and update instance usage nodes. Also,
		 * find start and end times.
		 */
		final Map<UsageEventKey,ReportingInstanceUsageEvent> lastEvents = Maps.newHashMap();
		final Map<String,StartEndTimes> startEndTimes = Maps.newHashMap();
		final Iterator<ReportingInstanceUsageEvent> usageEventIterator = getInstanceUsageEventIterator();
		while (usageEventIterator.hasNext()) {
			final ReportingInstanceUsageEvent usageEvent = usageEventIterator.next();
			if ( usageEvent.getTimestampMs() > (report.getEndMs() + TimeUnit.HOURS.toMillis(12)) ) {
				break; // end of relevant events, we go beyond the end of the report period for interpolation
			}

			/* Update instance start and end times */
			if (! startEndTimes.containsKey(usageEvent.getUuid())) {
				startEndTimes.put(usageEvent.getUuid(),
					new StartEndTimes(usageEvent.getTimestampMs(), usageEvent.getTimestampMs()));
			} else {
				StartEndTimes seTime = startEndTimes.get(usageEvent.getUuid());
				seTime.setStartTime(Math.min(seTime.getStartTime(), usageEvent.getTimestampMs()));
				seTime.setEndTime(Math.max(seTime.getEndTime(), usageEvent.getTimestampMs()));
			}
			
			
			/* Grab last event for this metric/dim combo, and usage entity to update */
			final UsageEventKey key = new UsageEventKey(usageEvent.getUuid(), usageEvent.getMetric(),
					usageEvent.getDimension());
			if (! lastEvents.containsKey(key)) {
				lastEvents.put(key, usageEvent);
				continue;
			}
			final ReportingInstanceUsageEvent lastEvent = lastEvents.get(key);
			if (! usageEntities.containsKey(usageEvent.getUuid())) {
				log.error("usage event without corresponding instance:" + usageEvent.getUuid());
				continue;
			}
			final InstanceUsageArtEntity usage = usageEntities.get(usageEvent.getUuid());
			final String metric = usageEvent.getMetric();
			final String dim    = usageEvent.getDimension();
			if (usageEvent.getValue()==null || lastEvent.getValue()==null) {
				log.debug("Null metric values shouldn't occur");
				continue;
			}

			/* We sometimes miss events here. Last event is dropped. Last event is last event for this metric/dim combo?? */

			/* Update metrics in usage */
			if ( usageEvent.getValue() == null ) {
				continue;
			}
			/* Subtract last usage from this usage because all these statistics are CUMULATIVE.	 */
			final Double value = usageEvent.getValue() - Objects.firstNonNull( lastEvent.getValue(), 0d );
			final Double valueMB = value/1024/1024; //don't bitshift a double

			if (metric.equals(METRIC_NET_IN_BYTES) && dim.equals(DIM_TOTAL)) {
				usage.addNetTotalInMegs(interpolate(report.getBeginMs(), report.getEndMs(),
						lastEvent.getTimestampMs(), usageEvent.getTimestampMs(), valueMB).longValue());
			} else if (metric.equals(METRIC_NET_OUT_BYTES) && dim.equals(DIM_TOTAL)) {
				usage.addNetTotalOutMegs(interpolate(report.getBeginMs(), report.getEndMs(),
						lastEvent.getTimestampMs(), usageEvent.getTimestampMs(), valueMB).longValue());
			} else if (metric.equals(METRIC_DISK_IN_BYTES)) {
				usage.addDiskReadMegs( interpolate( report.getBeginMs(), report.getEndMs(),
						lastEvent.getTimestampMs(), usageEvent.getTimestampMs(), valueMB ).longValue() );
			} else if (metric.equals(METRIC_DISK_OUT_BYTES)) {
				usage.addDiskWriteMegs( interpolate( report.getBeginMs(), report.getEndMs(),
						lastEvent.getTimestampMs(), usageEvent.getTimestampMs(), valueMB ).longValue() );
			} else if (metric.equals(METRIC_DISK_READ_OPS)) {
				usage.addDiskReadOps( interpolate( report.getBeginMs(), report.getEndMs(),
						lastEvent.getTimestampMs(), usageEvent.getTimestampMs(), value ).longValue() );
			} else if (metric.equals(METRIC_DISK_WRITE_OPS)) {
				usage.addDiskWriteOps( interpolate( report.getBeginMs(), report.getEndMs(),
						lastEvent.getTimestampMs(), usageEvent.getTimestampMs(), value ).longValue() );
			} else if (metric.equals(METRIC_VOLUME_READ)) {
				usage.addDiskReadTime( interpolate( report.getBeginMs(), report.getEndMs(),
						lastEvent.getTimestampMs(), usageEvent.getTimestampMs(), value ).longValue() );
			} else if (metric.equals(METRIC_VOLUME_WRITE)) {
				usage.addDiskWriteTime( interpolate( report.getBeginMs(), report.getEndMs(),
						lastEvent.getTimestampMs(), usageEvent.getTimestampMs(), value ).longValue() );
			} else if (metric.equals(METRIC_CPU_USAGE_MS) && (dim.equals(DIM_DEFAULT))) {
				usage.addCpuUtilizationMs(interpolate(report.getBeginMs(), report.getEndMs(),
						lastEvent.getTimestampMs(), usageEvent.getTimestampMs(), value).longValue());
			} else {
				log.debug("Unrecognized metric for report:" + metric + "/" + dim);
			}

			lastEvents.put(key, usageEvent);
		} //while

		/* Remove any instances with no usage in report period
		 */
		final Set<String> zonesToRemove = Sets.newHashSet();
		for ( final Map.Entry<String,AvailabilityZoneArtEntity> zoneEntry : report.getZones().entrySet() ) {
			final Set<String> accountsToRemove = Sets.newHashSet();
			for ( final Map.Entry<String,AccountArtEntity> accountEntry : zoneEntry.getValue().getAccounts().entrySet() ) {
				final Set<String> usersToRemove = Sets.newHashSet();
				for ( final Map.Entry<String,UserArtEntity> userEntry : accountEntry.getValue().getUsers().entrySet() ) {
					final Set<String> instancesToRemove = Sets.newHashSet();
					for ( final Map.Entry<String,InstanceArtEntity> instanceEntry : userEntry.getValue().getInstances().entrySet() ) {
						if ( instanceEntry.getValue().getUsage().isEmpty() ) {
							instancesToRemove.add( instanceEntry.getKey() );
						}
					}
					removeAndTrackEmpty( userEntry.getValue().getInstances(), instancesToRemove, usersToRemove, userEntry.getKey() );
				}
				removeAndTrackEmpty( accountEntry.getValue().getUsers(), usersToRemove, accountsToRemove, accountEntry.getKey() );
			}
			removeAndTrackEmpty( zoneEntry.getValue().getAccounts(), accountsToRemove, zonesToRemove, zoneEntry.getKey() );
		}
		report.getZones().keySet().removeAll( zonesToRemove );

		/* Update durations of all instances
		 */
		for (String uuid: startEndTimes.keySet()) {
			final StartEndTimes seTime = startEndTimes.get(uuid);
			if (usageEntities.containsKey(uuid)) {
				long durationMs = DurationCalculator.boundDuration(report.getBeginMs(), report.getEndMs(),
						seTime.getStartTime(), seTime.getEndTime());
				usageEntities.get(uuid).setDurationMs(durationMs);
			} else {
				log.error("startEndTime without corresponding instance:" + uuid);
			}
		}

		/* Perform totals and summations
		 */
		for ( final AvailabilityZoneArtEntity zone : report.getZones().values() ) {
			final UsageTotalsArtEntity zoneUsage = zone.getUsageTotals();
			for ( final AccountArtEntity account : zone.getAccounts().values() ) {
				final UsageTotalsArtEntity accountUsage = account.getUsageTotals();
				for ( final UserArtEntity user : account.getUsers().values() ) {
					final UsageTotalsArtEntity userUsage = user.getUsageTotals();
					for ( final InstanceArtEntity instance : user.getInstances().values() ) {
						updateUsageTotals(userUsage, instance);
						updateUsageTotals(accountUsage, instance);
						updateUsageTotals(zoneUsage, instance);
					}
				}
			}
		}

		return report;
	}

	private void removeAndTrackEmpty(
			final Map<String, ?> instances,
			final Set<String> emptyInstanceUuids,
			final Set<String> usersToRemove,
			final String key) {
		instances.keySet().removeAll( emptyInstanceUuids );
		if ( instances.isEmpty() ) {
			usersToRemove.add( key );
		}
	}

	protected Iterator<ReportingInstanceCreateEvent> getInstanceCreateEventIterator() {
		return getEventIterator( ReportingInstanceCreateEvent.class, "scanInstanceCreateEvents" );
	}

	protected Iterator<ReportingInstanceUsageEvent> getInstanceUsageEventIterator() {
		return getEventIterator( ReportingInstanceUsageEvent.class, "scanInstanceUsageEvents" );
	}

	private static void updateUsageTotals(UsageTotalsArtEntity totals, InstanceArtEntity instance)
	{
		final InstanceUsageArtEntity totalEntity = totals.getInstanceTotals();
		final InstanceUsageArtEntity usage = instance.getUsage();
		
		/* Update metrics */
		addUsage( totalEntity, usage );
		
		/* Update total running time and type count for this instance type */
		final Map<String,InstanceUsageArtEntity> typeTotals = totals.getTypeTotals();
		if (!typeTotals.containsKey(instance.getInstanceType().toLowerCase())) {  //TODO:STEVE: why is this lower cased?
			typeTotals.put(instance.getInstanceType().toLowerCase(),
					new InstanceUsageArtEntity());
		}
		final InstanceUsageArtEntity typeTotal =
			typeTotals.get(instance.getInstanceType().toLowerCase());

		addUsage( typeTotal, usage );
	}

	private static void addUsage( final InstanceUsageArtEntity total, final InstanceUsageArtEntity usage ) {
		total.addDurationMs( usage.getDurationMs() );
		total.addCpuUtilizationMs( usage.getCpuUtilizationMs() );
		total.addDiskReadMegs( usage.getDiskReadMegs() );
		total.addDiskWriteMegs( usage.getDiskWriteMegs() );
		total.addDiskReadOps( usage.getDiskReadOps() );
		total.addDiskWriteOps( usage.getDiskWriteOps() );
		total.addDiskReadTime( usage.getDiskReadTime() );
		total.addDiskWriteTime( usage.getDiskWriteTime() );
		total.addNetTotalInMegs( usage.getNetTotalInMegs() );
		total.addNetTotalOutMegs( usage.getNetTotalOutMegs() );
		total.addInstanceCnt( 1 );
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
			//Report falls entirely within period
			factor = ((double)(repEnd-repBegin))/periodDuration;
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
	
}
