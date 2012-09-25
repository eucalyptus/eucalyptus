package com.eucalyptus.reporting.art.generator;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.eucalyptus.reporting.art.entity.AccountArtEntity;
import com.eucalyptus.reporting.art.entity.ElasticIpArtEntity;
import com.eucalyptus.reporting.art.entity.ElasticIpUsageArtEntity;
import com.eucalyptus.reporting.art.entity.InstanceArtEntity;
import com.eucalyptus.reporting.art.entity.ReportArtEntity;
import com.eucalyptus.reporting.art.entity.UserArtEntity;
import com.eucalyptus.reporting.domain.ReportingAccount;
import com.eucalyptus.reporting.domain.ReportingUser;
import com.eucalyptus.reporting.event_store.ReportingElasticIpAttachEvent;
import com.eucalyptus.reporting.event_store.ReportingElasticIpCreateEvent;
import com.eucalyptus.reporting.event_store.ReportingElasticIpDeleteEvent;
import com.eucalyptus.reporting.event_store.ReportingElasticIpDetachEvent;
import com.eucalyptus.reporting.event_store.ReportingEventSupport;
import com.eucalyptus.reporting.event_store.ReportingInstanceCreateEvent;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class ElasticIpArtGenerator extends AbstractArtGenerator
{
	private static Logger log = Logger.getLogger( ElasticIpArtGenerator.class );

	@Override
	public ReportArtEntity generateReportArt( final ReportArtEntity report )
	{
		log.debug("GENERATING REPORT ART");

		// Find end times for the elastic ips (key is uuid)
		final Map<String,List<Long>> ipToDeleteTimesMap = buildTimestampMap( report, getElasticIpDeleteEventIterator() );

		/* Create super-tree of availZones, clusters, accounts, users, and instances;
				 * and create a Map of the instance nodes at the bottom.
				 */
		final Map<String,List<ElasticIpAllocation>> ipUuidToAllocationListMap = Maps.newHashMap();
		final Iterator<ReportingElasticIpCreateEvent> createEventIterator = getElasticIpCreateEventIterator();
		while ( createEventIterator.hasNext() ) {
			final ReportingElasticIpCreateEvent createEvent = createEventIterator.next();
			final Long deleteTime = findTimeAfter( ipToDeleteTimesMap, createEvent.getUuid(), createEvent.getTimestampMs() );
			if ( deleteTime < report.getBeginMs() ) {
				continue; // usage not relevant for this report
			}
			if ( createEvent.getTimestampMs() > report.getEndMs() ) {
				break; // end of relevant events for this report
			}
			final ReportingUser reportingUser = getUserById( createEvent.getUserId() );
			if (reportingUser==null) {
				log.error("No user corresponding to event:" + createEvent.getUserId() + " " + createEvent.getNaturalId());
				continue;
			}
			final ReportingAccount reportingAccount = getAccountById( reportingUser.getAccountId() );
			if (reportingAccount==null) {
				log.error("No account corresponding to user:" + reportingUser.getAccountId()+ " " + createEvent.getNaturalId());
				continue;
			}
			List<ElasticIpAllocation> allocations = ipUuidToAllocationListMap.get( createEvent.getUuid() );
			if ( allocations == null ) {
				allocations = Lists.newArrayList();
				ipUuidToAllocationListMap.put( createEvent.getUuid(), allocations );
			}
			allocations.add( new ElasticIpAllocation( reportingAccount.getName(), reportingUser.getName(), createEvent.getIp(), createEvent.getTimestampMs(), deleteTime ) );
			final AccountArtEntity account;
			if (!report.getAccounts().containsKey(reportingAccount.getName())) {
				account = new AccountArtEntity();
				report.getAccounts().put(reportingAccount.getName(), account);
			} else {
				account = report.getAccounts().get(reportingAccount.getName());
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
		}

		
		/* Scan instance entities so we can get the instance id from the uuid
		 */
		final Map<String,InstanceArtEntity> instanceEntities = Maps.newHashMap();
		final Iterator<ReportingInstanceCreateEvent> instanceIterator = getInstanceCreateEventIterator();
		while ( instanceIterator.hasNext() ) {
			final ReportingInstanceCreateEvent createEvent = instanceIterator.next();
			if ( createEvent.getTimestampMs() > report.getEndMs() ) {
				break; // end of relevant events for this report
			}
			final InstanceArtEntity instance = new InstanceArtEntity(createEvent.getInstanceType(), createEvent.getInstanceId());
			instanceEntities.put(createEvent.getUuid(), instance);
		}

		// Find end times for the elastic ips (key is uuid)
		final Map<String,List<Long>> ipToDetachTimesMap = buildTimestampMap( report, getElasticIpDetachEventIterator() );

		/* Find attachment start times
		 */
		final Iterator<ReportingElasticIpAttachEvent> attachEventIterator = getElasticIpAttachEventIterator();
		while (attachEventIterator.hasNext()) {
			final ReportingElasticIpAttachEvent attachEvent = attachEventIterator.next();
			// tolerate missing detach events by accounting for delete events also
			final Long deleteTime = findTimeAfter( ipToDeleteTimesMap, attachEvent.getIpUuid(), attachEvent.getTimestampMs() );
			final Long detachTime = Math.min( deleteTime, findTimeAfter( ipToDetachTimesMap, attachEvent.getIpUuid(), attachEvent.getTimestampMs() ));
			if ( detachTime < report.getBeginMs() ) {
				continue; // usage not relevant for this report
			}
			if ( attachEvent.getTimestampMs() > report.getEndMs() ) {
				break; // end of relevant events for this report
			}
			final Long attachmentDuration = calculateDuration( report, attachEvent.getTimestampMs(), detachTime );
			final ElasticIpArtEntity entity = findEntityForTimestamp( report, ipUuidToAllocationListMap, attachEvent.getIpUuid(), attachEvent.getTimestampMs() );
			if ( entity == null ) {
				log.error("Unable to find elastic ip owner for attachment, instance uuid: " + attachEvent.getInstanceUuid() );
				continue;
			}
			final InstanceArtEntity instance = instanceEntities.get( attachEvent.getInstanceUuid() );
			if ( instance == null ) {
				log.error("Unable to find instance for attachment, instance uuid: " + attachEvent.getInstanceUuid() );
				continue;
			}
			ElasticIpUsageArtEntity usage = entity.getInstanceAttachments().get( instance.getInstanceId() );
			if ( usage == null ) {
				usage = new ElasticIpUsageArtEntity();
				usage.setIpNum( 1 );
				entity.getInstanceAttachments().put(  instance.getInstanceId(), usage );
			}
			usage.setDurationMs( usage.getDurationMs() + attachmentDuration );
		}

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

	private Map<String,List<Long>> buildTimestampMap( final ReportArtEntity report, final Iterator<? extends ReportingEventSupport> eventIterator ) {
		final Map<String,List<Long>> ipToTimesMap = Maps.newHashMap();
		while ( eventIterator.hasNext() ) {
			final ReportingEventSupport event = eventIterator.next();
			if ( event.getTimestampMs() <= report.getEndMs() ) {
				List<Long> endTimes = ipToTimesMap.get( getIpUuid( event ) );
				if ( endTimes == null ) {
					endTimes = Lists.newArrayList( event.getTimestampMs() );
					ipToTimesMap.put( getIpUuid( event ), endTimes );
				} else if ( endTimes.size() == 1 && event.getTimestampMs() < report.getBeginMs() ) {
					endTimes.set( 0, event.getTimestampMs() );
				} else {
					endTimes.add( event.getTimestampMs() );
				}
			} else {
				break; // end of relevant data
			}
		}
		return ipToTimesMap;
	}

	private String getIpUuid( final ReportingEventSupport event ) {
		if ( event instanceof ReportingElasticIpDeleteEvent ) {
			return ((ReportingElasticIpDeleteEvent) event).getUuid();
		} else if ( event instanceof  ReportingElasticIpDetachEvent ) {
			return ((ReportingElasticIpDetachEvent) event).getIpUuid();
		}
		throw new IllegalStateException("Unsupported event type: " + event.getClass());
	}

	private Long findTimeAfter( final Map<String, List<Long>> ipToEndTimesMap, final String uuid, final Long startTime ) {
		Long timeAfter = Long.MAX_VALUE;

		final List<Long> endTimesForIp = ipToEndTimesMap.get( uuid );
		if ( endTimesForIp != null ) {
			for ( final Long endTime : endTimesForIp ) {
				if ( endTime > startTime ) {
					timeAfter = endTime;
					break;
				}
			}
		}

		return timeAfter;
	}

	protected Iterator<ReportingElasticIpCreateEvent> getElasticIpCreateEventIterator() {
		return getEventIterator( ReportingElasticIpCreateEvent.class, "scanElasticIpCreateEvents" );
	}

	protected Iterator<ReportingElasticIpDeleteEvent> getElasticIpDeleteEventIterator() {
		return getEventIterator( ReportingElasticIpDeleteEvent.class, "scanElasticIpDeleteEvents" );
	}

	protected Iterator<ReportingElasticIpAttachEvent> getElasticIpAttachEventIterator() {
		return getEventIterator( ReportingElasticIpAttachEvent.class, "scanElasticIpAttachEvents" );
	}

	protected Iterator<ReportingElasticIpDetachEvent> getElasticIpDetachEventIterator() {
		return getEventIterator( ReportingElasticIpDetachEvent.class, "scanElasticIpDetachEvents" );
	}

	protected Iterator<ReportingInstanceCreateEvent> getInstanceCreateEventIterator() {
		return getEventIterator( ReportingInstanceCreateEvent.class, "scanInstanceCreateEvents" );
	}

	private static void updateUsageTotals( ElasticIpUsageArtEntity totalEntity, ElasticIpUsageArtEntity newEntity ) {
		totalEntity.setIpNum(totalEntity.getIpNum()+newEntity.getIpNum());
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
