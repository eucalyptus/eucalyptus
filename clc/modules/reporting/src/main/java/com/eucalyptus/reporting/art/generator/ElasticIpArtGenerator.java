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
 ************************************************************************/

package com.eucalyptus.reporting.art.generator;

import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.hibernate.criterion.Restrictions;

import com.eucalyptus.reporting.art.entity.AccountArtEntity;
import com.eucalyptus.reporting.art.entity.ElasticIpArtEntity;
import com.eucalyptus.reporting.art.entity.ElasticIpUsageArtEntity;
import com.eucalyptus.reporting.art.entity.InstanceArtEntity;
import com.eucalyptus.reporting.art.entity.ReportArtEntity;
import com.eucalyptus.reporting.art.entity.UserArtEntity;
import com.eucalyptus.reporting.domain.ReportingUser;
import com.eucalyptus.reporting.event_store.ReportingElasticIpAttachEvent;
import com.eucalyptus.reporting.event_store.ReportingElasticIpCreateEvent;
import com.eucalyptus.reporting.event_store.ReportingElasticIpDeleteEvent;
import com.eucalyptus.reporting.event_store.ReportingElasticIpDetachEvent;
import com.eucalyptus.reporting.event_store.ReportingEventSupport;
import com.eucalyptus.reporting.event_store.ReportingInstanceCreateEvent;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class ElasticIpArtGenerator extends AbstractArtGenerator
{
	private static Logger log = Logger.getLogger( ElasticIpArtGenerator.class );

	@Override
	public ReportArtEntity generateReportArt( final ReportArtEntity report )
	{
		log.debug("Generating report ART");

		// Find end times for the elastic ips (key is uuid)
		final Map<String,List<Long>> ipToDeleteTimesMap = Maps.newHashMap();
		foreachElasticIpDeleteEvent( buildTimestampMap( report, ipToDeleteTimesMap, ipUuid() ) );

		// cache for user/account info
		final Map<String,ReportingUser> reportingUsersById = Maps.newHashMap();
		final Map<String,String> accountNamesById = Maps.newHashMap();

		/* Create super-tree of availZones, clusters, accounts, users, and instances;
		 * and create a Map of the instance nodes at the bottom.
		 */
		final Map<String,List<ElasticIpAllocation>> ipToAllocationListMap = Maps.newHashMap();
		foreachElasticIpCreateEvent( new Predicate<ReportingElasticIpCreateEvent>() {
			@Override
			public boolean apply( final ReportingElasticIpCreateEvent createEvent ) {
				final Long deleteTime = findTimeAfter( ipToDeleteTimesMap, createEvent.getIp(), createEvent.getTimestampMs() );
				if ( deleteTime < report.getBeginMs() ) {
					return true; // usage not relevant for this report
				}
				if ( createEvent.getTimestampMs() > report.getEndMs() ) {
					return false; // end of relevant events for this report
				}
				final ReportingUser reportingUser = getUserById( reportingUsersById, createEvent.getUserId() );
				if (reportingUser==null) {
					log.error("No user corresponding to event:" + createEvent.getUserId() + " " + createEvent.getIp());
					return true;
				}
				final String accountName = getAccountNameById( accountNamesById, reportingUser.getAccountId() );
				if (accountName==null) {
					log.error("No account corresponding to user:" + reportingUser.getAccountId()+ " " + createEvent.getIp());
					return true;
				}
				List<ElasticIpAllocation> allocations = ipToAllocationListMap.get( createEvent.getIp() );
				if ( allocations == null ) {
					allocations = Lists.newArrayList();
					ipToAllocationListMap.put( createEvent.getIp(), allocations );
				}
				allocations.add( new ElasticIpAllocation( accountName, reportingUser.getName(), createEvent.getIp(), createEvent.getTimestampMs(), deleteTime ) );
				final AccountArtEntity account;
				if (!report.getAccounts().containsKey(accountName)) {
					account = new AccountArtEntity();
					report.getAccounts().put(accountName, account);
				} else {
					account = report.getAccounts().get(accountName);
				}
				final UserArtEntity user;
				if (!account.getUsers().containsKey(reportingUser.getName())) {
					user = new UserArtEntity();
					account.getUsers().put(reportingUser.getName(), user);
				} else {
					user = account.getUsers().get(reportingUser.getName());
				}
				final ElasticIpArtEntity elasticIp;
				if (!user.getElasticIps().containsKey(createEvent.getIp())) {
					elasticIp = new ElasticIpArtEntity();
					elasticIp.getUsage().setIpNum(1);
					user.getElasticIps().put(createEvent.getIp(), elasticIp);
				} else {
					elasticIp = user.getElasticIps().get(createEvent.getIp());
				}
				elasticIp.getUsage().setDurationMs( elasticIp.getUsage().getDurationMs() + calculateDuration( report, createEvent.getTimestampMs(), deleteTime ) );
				return true;
			}
		} );


		/* Scan instance entities so we can get the instance id from the uuid
				 */
		final Map<String,InstanceArtEntity> instanceEntities = Maps.newHashMap();
		foreachInstanceCreateEvent( new Predicate<ReportingInstanceCreateEvent>() {
			@Override
			public boolean apply( final ReportingInstanceCreateEvent createEvent ) {
				if ( createEvent.getTimestampMs() > report.getEndMs() ) {
					return false; // end of relevant events for this report
				}
				final InstanceArtEntity instance = new InstanceArtEntity(createEvent.getInstanceType(), createEvent.getInstanceId());
				instanceEntities.put(createEvent.getUuid(), instance);
				return true;
			}
		} );

		// Find end times for the elastic ips (key is uuid)
		final Map<String,List<Long>> ipToDetachTimesMap = Maps.newHashMap();
		foreachElasticIpDetachEvent( buildTimestampMap( report, ipToDetachTimesMap, ipUuid() ) );

		/* Find attachment start times
				 */
		foreachElasticIpAttachEvent( new Predicate<ReportingElasticIpAttachEvent>() {
			@Override
			public boolean apply( final ReportingElasticIpAttachEvent attachEvent ) {
				// tolerate missing detach events by accounting for delete events also
				final Long deleteTime = findTimeAfter( ipToDeleteTimesMap, attachEvent.getIp(), attachEvent.getTimestampMs() );
				final Long detachTime = Math.min( deleteTime, findTimeAfter( ipToDetachTimesMap, attachEvent.getIp(), attachEvent.getTimestampMs() ));
				if ( detachTime < report.getBeginMs() ) {
					return true; // usage not relevant for this report
				}
				if ( attachEvent.getTimestampMs() > report.getEndMs() ) {
					return false; // end of relevant events for this report
				}
				final Long attachmentDuration = calculateDuration( report, attachEvent.getTimestampMs(), detachTime );
				final ElasticIpArtEntity entity = findEntityForTimestamp( report, ipToAllocationListMap, attachEvent.getIp(), attachEvent.getTimestampMs() );
				if ( entity == null ) {
					log.error("Unable to find elastic ip owner for attachment, instance uuid: " + attachEvent.getInstanceUuid() );
					return true;
				}
				final InstanceArtEntity instance = instanceEntities.get( attachEvent.getInstanceUuid() );
				if ( instance == null ) {
					log.error("Unable to find instance for attachment, instance uuid: " + attachEvent.getInstanceUuid() );
					return true;
				}
				ElasticIpUsageArtEntity usage = entity.getInstanceAttachments().get( instance.getInstanceId() );
				if ( usage == null ) {
					usage = new ElasticIpUsageArtEntity();
					usage.setIpNum( 1 );
					entity.getInstanceAttachments().put(  instance.getInstanceId(), usage );
				}
				usage.setDurationMs( usage.getDurationMs() + attachmentDuration );
				return true;
			}
		} );

		/* Perform totals and summations for user, account, and global
		 */
		for( final AccountArtEntity account : report.getAccounts().values() ) {
			for( final UserArtEntity user  : account.getUsers().values() ) {
				for( final ElasticIpArtEntity ip : user.getElasticIps().values() ) {
					updateUsageTotals(user.getUsageTotals().getElasticIpTotals(), ip.getUsage());
					updateUsageTotals(account.getUsageTotals().getElasticIpTotals(), ip.getUsage());
					updateUsageTotals(report.getUsageTotals().getElasticIpTotals(), ip.getUsage());
				}
			}
		}

		return report;
	}

	private ElasticIpArtEntity findEntityForTimestamp( final ReportArtEntity report, final Map<String,List<ElasticIpAllocation>> ipUuidToAllocationListMap, final String uuid, final Long timestampMs ) {
		ElasticIpArtEntity entity = null;
		final List<ElasticIpAllocation> allocations = ipUuidToAllocationListMap.get( uuid );
		if ( allocations!=null ) {
			for( final ElasticIpAllocation allocation : allocations ) {
				if ( allocation.startTime < timestampMs && allocation.endTime >= timestampMs ) {
					final AccountArtEntity accountArtEntity = report.getAccounts().get( allocation.accountName );
					if ( accountArtEntity != null ) {
						final UserArtEntity userArtEntity = accountArtEntity.getUsers().get( allocation.userName );
						if ( userArtEntity != null ) {
							entity = userArtEntity.getElasticIps().get( allocation.ip );
						}
					}
					break;
				}
			}
		}
		return entity;
	}

	private Long calculateDuration( final ReportArtEntity report, final Long start, final Long end ) {
		return Math.min( report.getEndMs(), end ) - Math.max( report.getBeginMs(), start );
	}

	private Function<ReportingEventSupport,String> ipUuid() {
		return new Function<ReportingEventSupport, String>() {
			@Override
			public String apply( final ReportingEventSupport reportingEventSupport ) {
				return getIpUuid( reportingEventSupport );
			}
		};
	}

	private String getIpUuid( final ReportingEventSupport event ) {
		if ( event instanceof ReportingElasticIpDeleteEvent ) {
			return ((ReportingElasticIpDeleteEvent) event).getIp();
		} else if ( event instanceof  ReportingElasticIpDetachEvent ) {
			return ((ReportingElasticIpDetachEvent) event).getIp();
		}
		throw new IllegalStateException("Unsupported event type: " + event.getClass());
	}

	protected void foreachElasticIpCreateEvent( final Predicate<? super ReportingElasticIpCreateEvent> callback ) {
		foreach( ReportingElasticIpCreateEvent.class, Restrictions.conjunction(), true, callback );
	}

	protected void foreachElasticIpDeleteEvent( final Predicate<? super ReportingElasticIpDeleteEvent> callback ) {
		foreach( ReportingElasticIpDeleteEvent.class, Restrictions.conjunction(), true, callback );
	}

	protected void foreachElasticIpAttachEvent( final Predicate<? super ReportingElasticIpAttachEvent> callback ) {
		foreach( ReportingElasticIpAttachEvent.class, Restrictions.conjunction(), true, callback );
	}

	protected void foreachElasticIpDetachEvent( final Predicate<? super ReportingElasticIpDetachEvent> callback ) {
		foreach( ReportingElasticIpDetachEvent.class, Restrictions.conjunction(), true, callback );
	}

	protected void foreachInstanceCreateEvent( final Predicate<? super ReportingInstanceCreateEvent> callback ) {
		foreach( ReportingInstanceCreateEvent.class, Restrictions.conjunction(), true, callback );
	}

	private static void updateUsageTotals( ElasticIpUsageArtEntity totalEntity, ElasticIpUsageArtEntity newEntity ) {
		totalEntity.setIpNum( totalEntity.getIpNum() + newEntity.getIpNum() );
		totalEntity.setDurationMs(totalEntity.getDurationMs()+newEntity.getDurationMs());
	}

	private static class ElasticIpAllocation {
		private final String accountName;
		private final String userName;
		private final String ip;
		private final Long startTime;
		private final Long endTime;

		private ElasticIpAllocation( final String accountName, final String userName, final String ip, final Long startTime, final Long endTime ) {
			this.accountName = accountName;
			this.userName = userName;
			this.ip = ip;
			this.startTime = startTime;
			this.endTime = endTime;
		}
	}
}
