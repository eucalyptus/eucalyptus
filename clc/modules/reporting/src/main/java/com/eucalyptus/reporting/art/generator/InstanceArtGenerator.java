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
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;

import com.eucalyptus.reporting.art.entity.*;
import com.eucalyptus.reporting.domain.*;
import com.eucalyptus.reporting.event_store.ReportingInstanceCreateEvent;
import com.eucalyptus.reporting.event_store.ReportingInstanceUsageEvent;
import com.google.common.base.Objects;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class InstanceArtGenerator extends AbstractArtGenerator
{
	private static final Logger log = Logger.getLogger( InstanceArtGenerator.class );

	private static final long USAGE_SEARCH_PERIOD = TimeUnit.DAYS.toMillis( 5 );

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
	public ReportArtEntity generateReportArt( final ReportArtEntity report )
	{
		log.debug("Generating report ART");

		// locate relevant usage events
		final Map<String, UsageCollator> usageCollators = findUsageEventsForReport( report );

		/* Create super-tree of availZones, clusters, accounts, users, and instances;
		 * and create a Map of the instance usage nodes at the bottom.
		 */
		final Map<String,InstanceUsageArtEntity> usageEntities = Maps.newHashMap();
		foreachInstanceCreateEventInReverse( report.getEndMs(), new Predicate<ReportingInstanceCreateEvent>() {
			@Override
			public boolean apply( final ReportingInstanceCreateEvent createEvent ) {
				if (!usageCollators.keySet().contains(createEvent.getUuid())) {
					return true; // ignore, no usage in report period
				}

				if (! report.getZones().containsKey(createEvent.getAvailabilityZone())) {
					report.getZones().put(createEvent.getAvailabilityZone(), new AvailabilityZoneArtEntity());
				}
				final AvailabilityZoneArtEntity zone = report.getZones().get( createEvent.getAvailabilityZone() );

				final ReportingUser reportingUser = getUserById( createEvent.getUserId() );
				if (reportingUser==null) {
					log.error("No user corresponding to event:" + createEvent.getUserId());
					usageEntities.remove( createEvent.getUuid() );
					return true;
				}
				final ReportingAccount reportingAccount = getAccountById(reportingUser.getAccountId());
				if (reportingAccount==null) {
					log.error("No account corresponding to user:" + reportingUser.getAccountId());
					usageEntities.remove( createEvent.getUuid() );
					return true;
				}
				usageCollators.get(createEvent.getUuid()).created( report.getBeginMs(), createEvent.getTimestampMs() );
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
				return !usageEntities.keySet().containsAll( usageCollators.keySet() );
			}
		} );

		/* Scan through instance usage and update instance usage nodes. Also,
		 * find start and end times.
		 */
		for( final Map.Entry<String,UsageCollator> usageCollatorEntry : usageCollators.entrySet() ) {
			final String instanceUuid = usageCollatorEntry.getKey();
			final UsageCollator usageCollator = usageCollatorEntry.getValue();
			final InstanceUsageArtEntity usage = usageEntities.get( instanceUuid );

			/* Update duration
			 */
			usage.setDurationMs( usageCollator.getDuration( report.getBeginMs(), report.getEndMs() ) );

			for ( final Map.Entry<UsageMetricDimensionKey,UsageMetricDimension> mericsEntry : usageCollator.usage.entrySet() ) {
				final UsageMetricDimensionKey key = mericsEntry.getKey();
				final UsageMetricDimension usageMetricDimension = mericsEntry.getValue();
				final String metric = key.metric;
				final String dim    = key.dimension;

				ReportingInstanceUsageEvent lastEvent = null;
				for ( final ReportingInstanceUsageEvent usageEvent : usageMetricDimension ) {
					if ( lastEvent == null ) {
						lastEvent = usageEvent; // we will have added an initial "zero" event if appropriate
						continue;
					}

					if ( lastEvent.getSequenceNum() >= usageEvent.getSequenceNum() ) {
						// usage reset, treat last usage as zero
						lastEvent = usageEvent.zero( lastEvent.getTimestampMs() );
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

					lastEvent = usageEvent;
				}
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

	private Map<String, UsageCollator> findUsageEventsForReport( final ReportArtEntity report ) {
		final Map<String,UsageCollator> usageCollators = Maps.newHashMap();
		foreachInstanceUsageEvent( report.getBeginMs(), report.getEndMs(), true, new Predicate<ReportingInstanceUsageEvent>() {
			@Override
			public boolean apply( final ReportingInstanceUsageEvent usageEvent ) {
				UsageCollator collator = usageCollators.get( usageEvent.getUuid() );
				if ( collator == null ) {
					collator = new UsageCollator();
					usageCollators.put( usageEvent.getUuid(), collator );
				}
				collator.addUsage( usageEvent );
				return true;
			}
		} );
		for ( final UsageCollator collator :  usageCollators.values() ) {
			collator.regularUsageCompleted();
		}
		// locate final relevant usage event for each instance
		foreachInstanceUsageEvent( report.getEndMs(), report.getEndMs() + USAGE_SEARCH_PERIOD, true, new Predicate<ReportingInstanceUsageEvent>() {
			@Override
			public boolean apply( final ReportingInstanceUsageEvent usageEvent ) {
				UsageCollator collator = usageCollators.get( usageEvent.getUuid() );
				if ( collator == null ) {
					collator = new UsageCollator();
					usageCollators.put( usageEvent.getUuid(), collator );
				}
				if ( !collator.hasPostUsage( usageEvent ) ) {
					collator.addPostUsage( usageEvent );
				}
				return true;
			}
		} );
		// locate final relevant usage event for each instance
		foreachInstanceUsageEvent( report.getBeginMs() - USAGE_SEARCH_PERIOD, report.getBeginMs(), false, new Predicate<ReportingInstanceUsageEvent>() {
			@Override
			public boolean apply( final ReportingInstanceUsageEvent usageEvent ) {
				UsageCollator collator = usageCollators.get( usageEvent.getUuid() );
				if ( collator != null && !collator.hasPreUsage( report.getBeginMs(), usageEvent ) ) {
					collator.addPreUsage( usageEvent );
				}
				return !Iterables.all( usageCollators.values(), new Predicate<UsageCollator>() {
					@Override
					public boolean apply( final UsageCollator usageCollator ) {
						return usageCollator.hasPreUsage( report.getBeginMs() );
					}
				} ); // stop search if pre usage found for all
			}
		} );
		return usageCollators;
	}

	private Criterion between( final Long beginInclusive, final Long endExclusive ) {
		return Restrictions.conjunction()
				.add( Restrictions.ge( TIMESTAMP_MS, beginInclusive ) )
				.add( before( endExclusive ) );
	}

	private Criterion before( final Long endExclusive ) {
		return Restrictions.lt( TIMESTAMP_MS, endExclusive );
	}

	protected void foreachInstanceCreateEventInReverse(
			final long endExclusive,
			final Predicate<? super ReportingInstanceCreateEvent> callback ) {
		foreach( ReportingInstanceCreateEvent.class,
				before( endExclusive ),
				false,
				validateCreate( callback ) );
	}

	protected void foreachInstanceUsageEvent(
			final long startInclusive,
			final long endExclusive,
			final boolean forward,
			final Predicate<? super ReportingInstanceUsageEvent> callback ) {
		foreach( ReportingInstanceUsageEvent.class,
				between( startInclusive, endExclusive ),
				forward,
				validateUsage( callback ) );
	}

	private Predicate<ReportingInstanceUsageEvent> validateUsage(
			final Predicate<? super ReportingInstanceUsageEvent> callback ) {
		return new Predicate<ReportingInstanceUsageEvent>(){
			@Override
			public boolean apply( final ReportingInstanceUsageEvent event ) {
				if ( event == null ||
						 event.getDimension() == null ||
						 event.getMetric() == null ||
						 event.getSequenceNum() == null ||
						 event.getUuid() == null ||
						 event.getValue() == null ) {
					log.debug("Ignoring invalid usage event: " + event);
					return true;
				}
				return callback.apply( event );
			}
		};
	}

	private Predicate<ReportingInstanceCreateEvent> validateCreate(
			final Predicate<? super ReportingInstanceCreateEvent> callback ) {
		return new Predicate<ReportingInstanceCreateEvent>(){
			@Override
			public boolean apply( final ReportingInstanceCreateEvent event ) {
				if ( event == null ||
						event.getAvailabilityZone() == null ||
						event.getInstanceId() == null ||
						event.getInstanceType() == null ||
						event.getUserId() == null ||
						event.getUuid() == null ) {
					log.debug("Ignoring invalid create event: " + event);
					return true;
				}
				return callback.apply( event );
			}
		};
	}

	private static void updateUsageTotals(UsageTotalsArtEntity totals, InstanceArtEntity instance)
	{
		final InstanceUsageArtEntity totalEntity = totals.getInstanceTotals();
		final InstanceUsageArtEntity usage = instance.getUsage();
		
		/* Update metrics */
		addUsage( totalEntity, usage );
		
		/* Update total running time and type count for this instance type */
		final Map<String,InstanceUsageArtEntity> typeTotals = totals.getTypeTotals();
		if (!typeTotals.containsKey(instance.getInstanceType())) {
			typeTotals.put(instance.getInstanceType(), new InstanceUsageArtEntity());
		}
		final InstanceUsageArtEntity typeTotal =
			typeTotals.get(instance.getInstanceType());

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
		double factor;
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

	private static final class UsageMetricDimensionKey {
		private final String metric;
		private final String dimension;

		private UsageMetricDimensionKey(
				final String metric,
				final String dimension ) {
			this.metric = metric;
			this.dimension = dimension;
		}

		private UsageMetricDimensionKey(
				final ReportingInstanceUsageEvent usage ) {
			this( usage.getMetric(), usage.getDimension() );
		}


		@SuppressWarnings( "RedundantIfStatement" )
		@Override
		public boolean equals( final Object o ) {
			if ( this == o ) return true;
			if ( o == null || getClass() != o.getClass() ) return false;

			final UsageMetricDimensionKey that = (UsageMetricDimensionKey) o;

			if ( !dimension.equals( that.dimension ) ) return false;
			if ( !metric.equals( that.metric ) ) return false;

			return true;
		}

		@Override
		public int hashCode() {
			int result = metric.hashCode();
			result = 31 * result + dimension.hashCode();
			return result;
		}
	}

	private static final class UsageMetricDimension implements Iterable<ReportingInstanceUsageEvent>{
		private ReportingInstanceUsageEvent preReportUsage;
		private ReportingInstanceUsageEvent firstReportUsage;
		private ReportingInstanceUsageEvent lastReportUsage;
		private ReportingInstanceUsageEvent postReportUsage;

		private List<ReportingInstanceUsageEvent> sequenceResets = Lists.newArrayList(); // pairs of pre-post reset usage

		private ReportingInstanceUsageEvent lastUsageEvent; // only used during collection

		// called in ascending timestamp order to add usage withing report period
		public void addUsage( final ReportingInstanceUsageEvent usageEvent ) {
			if( firstReportUsage == null ) {
				firstReportUsage = usageEvent;
			}

			if( lastUsageEvent != null && lastUsageEvent.getSequenceNum() > usageEvent.getSequenceNum() ) {
				// sequence reset, need to keep both these events
				sequenceResets.add( lastUsageEvent );
				sequenceResets.add( usageEvent );
			}

			lastUsageEvent = usageEvent;
		}

		public void regularUsageCompleted() {
			if ( firstReportUsage != lastUsageEvent ) lastReportUsage = lastUsageEvent;
			lastUsageEvent = null;
		}


		public void created( final Long beginMs, final Long timestampMs ) {
			// Add a ZERO usage event at create time
			if ( timestampMs > beginMs ) {
				if ( lastReportUsage == null ) {
					lastReportUsage = firstReportUsage;
				}
				firstReportUsage = Iterables.getFirst( this, null )
						.zero( Math.min( firstReportUsage.getTimestampMs(), timestampMs ) );
			} else if ( preReportUsage == null ) {
				preReportUsage = Iterables.getFirst( this, null ).zero( timestampMs );
			}
		}

		public boolean hasPostUsage() {
			return postReportUsage != null;
		}

		public void addPostUsage( final ReportingInstanceUsageEvent usageEvent ) {
			postReportUsage = usageEvent;
		}

		public boolean hasPreUsage( final long startTime ) {
			return preReportUsage != null
					|| ( firstReportUsage!=null &&
					firstReportUsage.getTimestampMs()!=null &&
					firstReportUsage.getTimestampMs()==startTime );
		}

		public void addPreUsage( final ReportingInstanceUsageEvent usageEvent ) {
			preReportUsage = usageEvent;
		}

		@Override
		public Iterator<ReportingInstanceUsageEvent> iterator() {
			final List<ReportingInstanceUsageEvent> events = Lists.newArrayList();

			if ( preReportUsage != null ) events.add( preReportUsage );
			if ( firstReportUsage != null ) events.add( firstReportUsage );
			events.addAll( sequenceResets );
			if ( lastReportUsage != null ) events.add( lastReportUsage );
			if ( postReportUsage != null ) events.add( postReportUsage );

			return events.iterator();
		}
	}

	private static final class UsageCollator {
		private final Map<UsageMetricDimensionKey,UsageMetricDimension> usage = Maps.newHashMap();

		private UsageMetricDimensionKey key( final ReportingInstanceUsageEvent usageEvent ) {
			return new UsageMetricDimensionKey( usageEvent );
		}

		private UsageMetricDimension metricsFor( final UsageMetricDimensionKey key ) {
			UsageMetricDimension usageMetricDimension = usage.get( key );
			if ( usageMetricDimension == null ) {
				usageMetricDimension = new UsageMetricDimension();
				usage.put( key, usageMetricDimension );
			}
			return usageMetricDimension;
		}

		// called in ascending timestamp order to add usage withing report period
		public void addUsage( final ReportingInstanceUsageEvent usageEvent ) {
			metricsFor( key(usageEvent) ).addUsage( usageEvent );
		}

		public void regularUsageCompleted() {
			for( final UsageMetricDimension usageMetricDimension : usage.values() ) {
				usageMetricDimension.regularUsageCompleted();
			}
		}

		public boolean hasPostUsage( final ReportingInstanceUsageEvent usageEvent ) {
			UsageMetricDimension usageMetricDimension = usage.get( key(usageEvent) );
			return usageMetricDimension != null && usageMetricDimension.hasPostUsage();
		}

		public void addPostUsage( final ReportingInstanceUsageEvent usageEvent ) {
			metricsFor( key(usageEvent) ).addPostUsage( usageEvent );
		}

		public boolean hasPreUsage( final long startTime, final ReportingInstanceUsageEvent usageEvent  ) {
			UsageMetricDimension usageMetricDimension = usage.get( key(usageEvent) );
			return usageMetricDimension != null && usageMetricDimension.hasPreUsage( startTime );
		}

		public boolean hasPreUsage( final long startTime ) {
			return !usage.isEmpty() && Iterables.all( usage.values(), preUsagePredicate( startTime ) );
		}

		public void addPreUsage( final ReportingInstanceUsageEvent usageEvent ) {
			metricsFor( key(usageEvent) ).addPreUsage( usageEvent );
		}

		private Predicate<UsageMetricDimension> preUsagePredicate( final long startTime ) {
			return new Predicate<UsageMetricDimension>() {
				@Override
				public boolean apply( final UsageMetricDimension usageMetricDimension ) {
					return usageMetricDimension.hasPreUsage( startTime );
				}
			};
		}

		private Predicate<UsageMetricDimension> postUsagePredicate() {
			return new Predicate<UsageMetricDimension>() {
				@Override
				public boolean apply( final UsageMetricDimension usageMetricDimension ) {
					return usageMetricDimension.hasPostUsage();
				}
			};
		}

		public long getDuration( final long beginMs, final long endMs ) {
			long startTime = Iterables.any( usage.values(), preUsagePredicate( beginMs ) ) ?
					beginMs :
					findFirstUsageTime();
			long endTime = Iterables.any( usage.values(), postUsagePredicate() ) ?
					endMs :
					findLastUsageTime();

			return endTime - startTime;
		}

		private long findLastUsageTime() {
			long time = 0;
			for( final UsageMetricDimension usageMetricDimension : usage.values() ) {
				time = Math.max( time, usageMetricDimension.lastReportUsage.getTimestampMs() );
			}
			return time;
		}

		private long findFirstUsageTime() {
			long time = Long.MAX_VALUE;
			for( final UsageMetricDimension usageMetricDimension : usage.values() ) {
				time = Math.min( time, usageMetricDimension.firstReportUsage.getTimestampMs() );
			}
			return time == Long.MAX_VALUE ? 0 : time;
		}

		public void created( final Long beginMs, final Long timestampMs ) {
			for( final UsageMetricDimension usageMetricDimension : usage.values() ) {
				usageMetricDimension.created( beginMs, timestampMs );
			}
		}
	}
}
