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
import com.google.common.collect.Maps;

public class InstanceArtGenerator
	extends AbstractArtGenerator
{
    private static Logger log = Logger.getLogger( InstanceArtGenerator.class );

    /* Metric names */
    public static final String METRIC_NET_IN_BYTES   = "NetworkIn";
    public static final String METRIC_NET_OUT_BYTES  = "NetworkOut";
    public static final String METRIC_DISK_IN_BYTES  = "DiskReadBytes";
    public static final String METRIC_DISK_OUT_BYTES = "DiskWriteBytes";
    public static final String METRIC_DISK_READ_OPS  = "DiskReadOps";
    public static final String METRIC_DISK_WRITE_OPS = "DiskWriteOps";
    public static final String METRIC_CPU_USAGE_MS   = "CPUUtilization";
    public static final String METRIC_VOLUME_READ    = "VolumeTotalReadTime";
    public static final String METRIC_VOLUME_WRITE   = "VolumeTotalWriteTime";

    public static final String DIM_TOTAL     = "total";
    public static final String DIM_DEFAULT   = "default";
    
    private static final long USAGE_SEARCH_PERIOD = TimeUnit.DAYS.toMillis( 12 );

    
    
    public InstanceArtGenerator()
	{
		
	}
	
	@Override
  public ReportArtEntity generateReportArt(final ReportArtEntity report)
	{
		log.debug("GENERATING REPORT ART");

    final Map<String, ReportingUser> users = Maps.newHashMap();
    final Map<String, String> accounts = Maps.newHashMap();

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

            	final ReportingUser reportingUser = getUserById( users, createEvent.getUserId() );
            	if (reportingUser==null) {
            		log.error("No user corresponding to event:" + createEvent.getUserId());
                return true;
            	}
            	final String accountName = getAccountNameById( accounts, reportingUser.getAccountId() );
            	if (accountName==null) {
            		log.error("No account corresponding to user:" + reportingUser.getAccountId());
                return true;
              }
            	if (! zone.getAccounts().containsKey(accountName)) {
            		zone.getAccounts().put(accountName, new AccountArtEntity());
            	}
            	AccountArtEntity account = zone.getAccounts().get(accountName);
            	if (! account.getUsers().containsKey(reportingUser.getName())) {
            		account.getUsers().put(reportingUser.getName(), new UserArtEntity());
            	}
            	UserArtEntity user = account.getUsers().get(reportingUser.getName());
            	if (! user.getInstances().containsKey(createEvent.getUuid())) {
            		user.getInstances().put(createEvent.getUuid(), new InstanceArtEntity(createEvent.getInstanceType(),
            				createEvent.getInstanceId()));
            	}
            	InstanceArtEntity instance = user.getInstances().get(createEvent.getUuid());
            	instance.getUsage().addInstanceCnt(1);
            	usageEntities.put(createEvent.getUuid(), instance.getUsage());
            	instanceStartTimes.put(createEvent.getUuid(), createEvent.getTimestampMs());
            	return true;            
            }
        } );


        /* Scan through events in order, and update the total usage in the instance usage art entity, for each
         * uuid/metric/dimension combo. Metric values are cumulative, so we must subtract each from the last.
         * For this reason, we must retain previous values of each uuid/metric/dim combo. We must also retain
         * the earliest and latest times for each combo, to update the duration.
         */
		final Map<InstanceMetricDimensionKey, MetricPrevData> prevDataMap =
			new HashMap<InstanceMetricDimensionKey, MetricPrevData>();
        foreachInstanceUsageEvent( report.getBeginMs()-USAGE_SEARCH_PERIOD,
        		report.getEndMs()+USAGE_SEARCH_PERIOD,
        		new Predicate<ReportingInstanceUsageEvent>() {
            @Override
            public boolean apply( final ReportingInstanceUsageEvent event ) {

            	final InstanceMetricDimensionKey key =
            		new InstanceMetricDimensionKey(event.getUuid(), event.getMetric(),
            				event.getDimension());
            	final long eventMs = event.getTimestampMs();
        		if (event.getValue()==null) return true;
        		final InstanceUsageArtEntity usageEntity = usageEntities.get(event.getUuid());
        		if (usageEntity==null) return true;

        		if (!prevDataMap.containsKey(key)) {
        			/* No prior value. Use usage from instance creation to present */
            		if (instanceStartTimes.containsKey(event.getUuid())) {
            			//Equivalent to inserting a zero-usage event at instance creation time
            			Double fractionalVal = fractionalUsage(report.getBeginMs(), report.getEndMs(),
            					instanceStartTimes.get(event.getUuid()), eventMs, event.getValue());
        				addMetricValueToUsageEntity(usageEntity, event.getMetric(), event.getDimension(),
        						fractionalVal);
        				log.debug(String.format("new metric time:%d-%d report:%d-%d uuid:%s metric:%s dim:%s val:%f fraction:%f",
        						instanceStartTimes.get(event.getUuid()), eventMs, report.getBeginMs(), report.getEndMs(),
        						event.getUuid(), event.getMetric(),	event.getDimension(), event.getValue(), fractionalVal));
            		}
        			prevDataMap.put(key, new MetricPrevData(eventMs, eventMs, event.getValue()));
        		} else {
        			/* Previous value exists */
                	final MetricPrevData prevData = prevDataMap.get(key);
        		
            		/* We have a period (firstMs to now); update the instance duration if necessary */
        			usageEntity.setDurationMs(Math.max(usageEntity.getDurationMs(),
							overlap(report.getBeginMs(), report.getEndMs(), prevData.firstMs, eventMs)));        			

        			if (event.getValue() < prevData.lastVal) {
        				/* SENSOR RESET; we lost data; just take whatever amount greater than 0 */
        				Double fractionalVal = fractionalUsage(report.getBeginMs(), report.getEndMs(),
        						prevData.lastMs, eventMs, event.getValue());
        				addMetricValueToUsageEntity(usageEntity, event.getMetric(), event.getDimension(),
        						fractionalVal);
        				log.debug(String.format("reset time:%d-%d report:%d-%d uuid:%s metric:%s dim:%s val:%f fraction:%f",
        						prevData.lastMs, eventMs, report.getBeginMs(), report.getEndMs(), event.getUuid(), event.getMetric(),
        						event.getDimension(), event.getValue(), fractionalVal));
       			} else {
        				/* Increase total by val minus lastVal */
        				Double fractionalVal = fractionalUsage(report.getBeginMs(), report.getEndMs(),
        						prevData.lastMs, eventMs, event.getValue()-prevData.lastVal);
        				addMetricValueToUsageEntity(usageEntity, event.getMetric(), event.getDimension(),
        						fractionalVal);
        				log.debug(String.format("event time:%d-%d report:%d-%d uuid:%s metric:%s dim:%s val:%f lastVal:%f fraction:%f",
        						prevData.lastMs, eventMs, report.getBeginMs(), report.getEndMs(), event.getUuid(), event.getMetric(),
        						event.getDimension(), event.getValue(), prevData.lastVal, fractionalVal));
        			}
        			prevDataMap.put(key, new MetricPrevData(prevData.firstMs, eventMs, event.getValue()));
        		}
            	return true;
            }
        } );

		
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
    	long valueMB = value>>20;
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
	 * Immutable record of prior data for a uuid/metric/dim combo. This is replaced rather than updated.
	 */
	private static class MetricPrevData
	{
		private final long   firstMs;
		private final double lastVal;
		private final long   lastMs;
		
		private MetricPrevData(long firstMs, long lastMs, double lastVal)
		{
			this.firstMs = firstMs;
			this.lastMs  = lastMs;
			this.lastVal = lastVal;
		}
	}

	/**
	 * Find the overlapping portion of two time periods
	 */
	private static long overlap(long repBegin, long repEnd, long perBegin, long perEnd)
	{
		if (perEnd <= repBegin || perBegin >= repEnd) {
			return 0l;
		} else {
			return Math.min(repEnd, perEnd) - Math.max(repBegin, perBegin);
		}
	}
	
	/**
	 * Return the fraction of usage which occurs within both the report boundaries and the period boundaries.
	 */
    private static Double fractionalUsage(long repBegin, long repEnd, long perBegin, long perEnd, Double usage)
    {   
    	if (usage==null) return null;
        double duration = (double) (perEnd-perBegin);
        double overlapping = (double)overlap(repBegin, repEnd, perBegin, perEnd);
        return usage*(overlapping/duration);
    }
	
    protected void foreachInstanceUsageEvent(long startInclusive, long endExclusive,
            Predicate<? super ReportingInstanceUsageEvent> callback)
    {
        foreach( ReportingInstanceUsageEvent.class, between( startInclusive, endExclusive ), true, callback );
    }
    
    protected void foreachInstanceCreateEvent(long endExclusive,
    		Predicate<? super ReportingInstanceCreateEvent> callback )
    {
        foreach( ReportingInstanceCreateEvent.class, before( endExclusive ), true, callback );
    }
	
}
