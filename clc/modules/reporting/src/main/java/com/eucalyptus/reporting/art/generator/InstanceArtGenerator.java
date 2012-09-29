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
import com.eucalyptus.reporting.domain.*;
import com.eucalyptus.reporting.event_store.ReportingInstanceCreateEvent;
import com.eucalyptus.reporting.event_store.ReportingInstanceUsageEvent;
import com.google.common.base.Predicate;

public class InstanceArtGenerator
	extends AbstractArtGenerator
{
    private static Logger log = Logger.getLogger( InstanceArtGenerator.class );

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
    
    private static final long USAGE_SEARCH_PERIOD = TimeUnit.DAYS.toMillis( 12 );

    
    
    public InstanceArtGenerator()
	{
		
	}
	
	public ReportArtEntity generateReportArt(final ReportArtEntity report)
	{
		log.debug("GENERATING REPORT ART");

		/* Create super-tree of availZones, clusters, accounts, users, and instances;
		 * and create a Map of the instance usage nodes at the bottom.
		 */
		final Map<String,InstanceUsageArtEntity> usageEntities = new HashMap<String,InstanceUsageArtEntity>();
		final Map<String, Long> instanceStartTimes = new HashMap<String, Long>();

        foreachInstanceCreateEvent( report.getEndMs(), new Predicate<ReportingInstanceCreateEvent>() {
            @Override
            public boolean apply( final ReportingInstanceCreateEvent createEvent ) {

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
            	instance.getUsage().addInstanceCnt(1);
            	usageEntities.put(createEvent.getUuid(), instance.getUsage());
            	instanceStartTimes.put(createEvent.getUuid(), createEvent.getTimestampMs());
            	return true;            
            }
        } );


		/* Gather values for the last event before report beginning, the first event after
		 * report beginning, the last event before report end, and the first event after report
		 * end, for every instance/metric/dimension combo, from the usage log. This is
		 * necessary to calculate fractional usage for report boundaries that fall between
		 * usage events.
		 */
		final Map<InstanceMetricDimensionKey, TimestampValueAccumulator> accumulators =
			new HashMap<InstanceMetricDimensionKey, TimestampValueAccumulator>();
        foreachInstanceUsageEvent( report.getBeginMs()-USAGE_SEARCH_PERIOD,
        		report.getEndMs()+USAGE_SEARCH_PERIOD,
        		new Predicate<ReportingInstanceUsageEvent>() {
            @Override
            public boolean apply( final ReportingInstanceUsageEvent usageEvent ) {
            	InstanceMetricDimensionKey key =
            		new InstanceMetricDimensionKey(usageEvent.getUuid(), usageEvent.getMetric(),
            				usageEvent.getDimension());
            	if (!accumulators.containsKey(key)) {
            		accumulators.put(key, new TimestampValueAccumulator(report.getBeginMs(), report.getEndMs()));
            	}
            	if (usageEvent.getValue()!=null) {
            		TimestampValue tv = new TimestampValue(usageEvent.getTimestampMs(), usageEvent.getValue());
            		accumulators.get(key).addTimestampValue(tv);
            	}
            	/* Add zeroeth event value if we need one */
            	if (instanceStartTimes.containsKey(usageEvent.getUuid())
            			&& (accumulators.get(key).lastBeforeBeginning==null
            					|| accumulators.get(key).firstAfterBeginning==null)) {
            		accumulators.get(key).addTimestampValue(new TimestampValue(instanceStartTimes.get(usageEvent.getUuid()), 0d));
            	}
            	return true;
            }
        } );

		
		/* Fill in ART values and durations for all the instance/metric/dimension
		 * combo data gathered above.
		 */
		for (InstanceMetricDimensionKey key: accumulators.keySet()) {
			TimestampValueAccumulator acc = accumulators.get(key);
			
			/* No usage within report boundaries */
			if (acc.firstAfterBeginning==null || acc.lastBeforeEnd==null) continue;
			double val = 0;
			/* Add all usage which occurs entirely within report boundaries */
			val += (acc.lastBeforeEnd.val - acc.firstAfterBeginning.val);
			/* Add partial usage for periods which cross report beginning or end */
			if (acc.lastBeforeBeginning!=null) {
				long durationMs = acc.firstAfterBeginning.timeMs - acc.lastBeforeBeginning.timeMs;
				/* factor is fraction of usage which comes after report beginning */
				double factor = ((double)acc.firstAfterBeginning.timeMs-report.getBeginMs())/durationMs;
				val += (acc.firstAfterBeginning.val-acc.lastBeforeBeginning.val)*factor;
			}
			if (acc.firstAfterEnd!=null) {
				long durationMs = acc.firstAfterEnd.timeMs - acc.lastBeforeEnd.timeMs;
				/* factor is fraction of usage which comes before report end */
				double factor = ((double)report.getEndMs()-acc.lastBeforeEnd.timeMs)/durationMs;
				val += (acc.firstAfterEnd.val-acc.lastBeforeEnd.val) * factor;
			}

			/* Update usage in ART */
			InstanceUsageArtEntity usageEntity = usageEntities.get(key.instanceUuid);
			addMetricValueToUsageEntity(usageEntity, key.metric, key.dimension, val);

			/* Update instance duration in ART */
			long startMs = acc.lastBeforeBeginning!=null ? report.getBeginMs() : acc.firstAfterBeginning.timeMs;
			long endMs = acc.firstAfterEnd!=null ? report.getEndMs() : acc.lastBeforeEnd.timeMs;
			usageEntity.setDurationMs(Math.max(usageEntity.getDurationMs(), endMs-startMs));
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


	private static void updateUsageTotals(UsageTotalsArtEntity totals, InstanceArtEntity instance)
	{
		InstanceUsageArtEntity totalEntity = totals.getInstanceTotals();
		InstanceUsageArtEntity usage = instance.getUsage();
		
		/* Update metrics */
        addUsage( totalEntity, usage );
		
		/* Update total running time and type count for this instance type */
		Map<String,InstanceUsageArtEntity> typeTotals = totals.getTypeTotals();
		if (!typeTotals.containsKey(instance.getInstanceType().toLowerCase())) {
			typeTotals.put(instance.getInstanceType().toLowerCase(),
					new InstanceUsageArtEntity());
		}
		InstanceUsageArtEntity typeTotal =
			typeTotals.get(instance.getInstanceType().toLowerCase());
		
        addUsage( typeTotal, usage );
		
	}

    private static void addUsage( final InstanceUsageArtEntity total, final InstanceUsageArtEntity usage )
    {
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

    private static void addMetricValueToUsageEntity(InstanceUsageArtEntity usage, String metric, String dim, double val)
    {
    	long value = new Double(val).longValue();
    	long valueMB = value<<20;
        if (metric.equals(METRIC_NET_IN_BYTES) && dim.equals(DIM_TOTAL)) {
            usage.addNetTotalInMegs(valueMB);
        } else if (metric.equals(METRIC_NET_OUT_BYTES) && dim.equals(DIM_TOTAL)) {
            usage.addNetTotalOutMegs(valueMB);
        } else if (metric.equals(METRIC_DISK_IN_BYTES)) {
            usage.addDiskReadMegs(valueMB);
        } else if (metric.equals(METRIC_DISK_OUT_BYTES)) {
            usage.addDiskWriteMegs(valueMB);
        } else if (metric.equals(METRIC_DISK_READ_OPS)) {
            usage.addDiskReadOps(value);
        } else if (metric.equals(METRIC_DISK_WRITE_OPS)) {
            usage.addDiskWriteOps(value);
        } else if (metric.equals(METRIC_VOLUME_READ)) {
            usage.addDiskReadTime(value);
        } else if (metric.equals(METRIC_VOLUME_WRITE)) {
            usage.addDiskWriteTime(value);
        } else if (metric.equals(METRIC_CPU_USAGE_MS) && (dim.equals(DIM_DEFAULT))) {
            usage.addCpuUtilizationMs(value);
        } else {
            log.debug("Unrecognized metric for report:" + metric + "/" + dim);
        }
    }

	private static class InstanceMetricDimensionKey
	{
		private final String instanceUuid;
		private final String metric;
		private final String dimension;
		
		public InstanceMetricDimensionKey(String instanceUuid, String metric, String dimension)
		{
			this.instanceUuid = instanceUuid;
			this.metric = metric;
			this.dimension = dimension;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((dimension == null) ? 0 : dimension.hashCode());
			result = prime * result	+ ((instanceUuid == null) ? 0 : instanceUuid.hashCode());
			result = prime * result	+ ((metric == null) ? 0 : metric.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) return true;
			if (obj == null) return false;
			if (getClass() != obj.getClass()) return false;
			InstanceMetricDimensionKey other = (InstanceMetricDimensionKey) obj;
			if (dimension == null) {
				if (other.dimension != null) return false;
			} else if (!dimension.equals(other.dimension)) return false;
			if (instanceUuid == null) {
				if (other.instanceUuid != null)	return false;
			} else if (!instanceUuid.equals(other.instanceUuid)) return false;
			if (metric == null) {
				if (other.metric != null) return false;
			} else if (!metric.equals(other.metric)) return false;
			return true;
		}
		
	}
	
	/**
	 * A value of a metric at a given time 
	 */
	private static class TimestampValue
	{
		private final long timeMs;
		private final double val;
		
		TimestampValue(long ms, double val)
		{
			this.timeMs = ms;
			this.val = val;
		}
	}
	
	/**
	 * TimestampValueAccumulator retains the last value before report beginning, the first
	 * value after report beginning, the last value before report end, and the first value
	 * after report end. It discards all other values which are not needed. 
	 */
	private static class TimestampValueAccumulator
	{
		private final long reportBeginMs;
		private final long reportEndMs;
		
		private TimestampValue lastBeforeBeginning = null;
		private TimestampValue firstAfterBeginning = null;
		private TimestampValue lastBeforeEnd = null;
		private TimestampValue firstAfterEnd = null;
		
		TimestampValueAccumulator(long reportBeginMs, long reportEndMs)
		{
			this.reportBeginMs = reportBeginMs;
			this.reportEndMs = reportEndMs;
		}
		
		public void addTimestampValue(TimestampValue tv)
		{			
			if (tv.timeMs < reportBeginMs) {
				if (lastBeforeBeginning == null || tv.timeMs > lastBeforeBeginning.timeMs) {
					lastBeforeBeginning = tv;
				}
			} else if (tv.timeMs >= reportBeginMs && tv.timeMs <= reportEndMs) {
				if (firstAfterBeginning==null || tv.timeMs < firstAfterBeginning.timeMs) {
					firstAfterBeginning = tv;
				}
				if (lastBeforeEnd==null || tv.timeMs > lastBeforeEnd.timeMs) {
					lastBeforeEnd = tv;
				}
			} else {
				if (firstAfterEnd==null || tv.timeMs < firstAfterEnd.timeMs) {
					firstAfterEnd = tv;
				}
			}			
		}

	}

    protected void foreachInstanceUsageEvent(long startInclusive, long endExclusive,
            Predicate<? super ReportingInstanceUsageEvent> callback)
    {
        foreach( ReportingInstanceUsageEvent.class, between( startInclusive, endExclusive ), callback );
    }
    
    protected void foreachInstanceCreateEvent(long endExclusive,
    		Predicate<? super ReportingInstanceCreateEvent> callback )
    {
        foreach( ReportingInstanceCreateEvent.class, before( endExclusive ), callback );
    }
	
}
