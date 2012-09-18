package com.eucalyptus.reporting.art.generator;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.log4j.Logger;

import com.eucalyptus.entities.EntityWrapper;
import com.eucalyptus.reporting.art.entity.AccountArtEntity;
import com.eucalyptus.reporting.art.entity.AvailabilityZoneArtEntity;
import com.eucalyptus.reporting.art.entity.ElasticIpArtEntity;
import com.eucalyptus.reporting.art.entity.ElasticIpUsageArtEntity;
import com.eucalyptus.reporting.art.entity.InstanceArtEntity;
import com.eucalyptus.reporting.art.entity.ReportArtEntity;
import com.eucalyptus.reporting.art.entity.UserArtEntity;
import com.eucalyptus.reporting.art.util.AttachDurationCalculator;
import com.eucalyptus.reporting.domain.ReportingAccount;
import com.eucalyptus.reporting.domain.ReportingAccountDao;
import com.eucalyptus.reporting.domain.ReportingUser;
import com.eucalyptus.reporting.domain.ReportingUserDao;
import com.eucalyptus.reporting.event_store.ReportingElasticIpAttachEvent;
import com.eucalyptus.reporting.event_store.ReportingElasticIpCreateEvent;
import com.eucalyptus.reporting.event_store.ReportingElasticIpDeleteEvent;
import com.eucalyptus.reporting.event_store.ReportingElasticIpDetachEvent;
import com.eucalyptus.reporting.event_store.ReportingInstanceCreateEvent;

public class ElasticIpArtGenerator
	implements ArtGenerator
{
    private static Logger log = Logger.getLogger( ElasticIpArtGenerator.class );

    public ElasticIpArtGenerator()
	{
		
	}
	
	public ReportArtEntity generateReportArt(ReportArtEntity report)
	{
		log.debug("GENERATING REPORT ART");
		EntityWrapper wrapper = EntityWrapper.get( ReportingElasticIpCreateEvent.class );

		/* Create super-tree of availZones, clusters, accounts, users, and instances;
		 * and create a Map of the instance nodes at the bottom.
		 */
		Map<String,ElasticIpArtEntity> elasticIpEntities = new HashMap<String,ElasticIpArtEntity>();
		Map<String,StartEndTimes> ipStartEndTimes = new HashMap<String,StartEndTimes>();
		Iterator iter = wrapper.scanWithNativeQuery( "scanElasticIpCreateEvents" );
		while (iter.hasNext()) {
			ReportingElasticIpCreateEvent createEvent = (ReportingElasticIpCreateEvent) iter.next();
			ReportingUser reportingUser = ReportingUserDao.getInstance().getReportingUser(createEvent.getUserId());
			if (reportingUser==null) {
				log.error("No user corresponding to event:" + createEvent.getUserId());
			}
			ReportingAccount reportingAccount = ReportingAccountDao.getInstance().getReportingAccount(reportingUser.getAccountId());
			if (reportingAccount==null) {
				log.error("No account corresponding to user:" + reportingUser.getAccountId());
			}
			AccountArtEntity account = new AccountArtEntity();
			if (! report.getAccounts().containsKey(reportingAccount.getName())) {
				report.getAccounts().put(reportingAccount.getName(), account);
			}
			UserArtEntity user = new UserArtEntity();
			if (!account.getUsers().containsKey(reportingUser.getName())) {
				account.getUsers().put(reportingUser.getName(), user);
			}
			ElasticIpArtEntity elasticIp = new ElasticIpArtEntity();
			if (!user.getElasticIps().containsKey(createEvent.getIp())) {
				user.getElasticIps().put(createEvent.getIp(), elasticIp);
			}
			elasticIpEntities.put(createEvent.getUuid(), elasticIp);
			ipStartEndTimes.put(createEvent.getUuid(), new StartEndTimes( createEvent.getTimestampMs(), report.getEndMs() ));

		}
		
		/* Find end times for the elastic ips
		 */
		iter = wrapper.scanWithNativeQuery( "scanElasticIpDeleteEvents" );
		while (iter.hasNext()) {
			ReportingElasticIpDeleteEvent deleteEvent = (ReportingElasticIpDeleteEvent) iter.next();
			long endTime = Math.min(deleteEvent.getTimestampMs(), report.getEndMs());
			if (endTime >= report.getBeginMs() && ipStartEndTimes.containsKey(deleteEvent.getUuid())) {
				StartEndTimes startEndTimes = ipStartEndTimes.get(deleteEvent.getUuid());
				startEndTimes.setEndTimeMs(endTime);
			} else {
				elasticIpEntities.remove(deleteEvent.getUuid());
				ipStartEndTimes.remove(deleteEvent.getUuid());
			}
		}

		/* Set the duration of each elastic ip
		 */
		for (String uuid: elasticIpEntities.keySet()) {
			ElasticIpArtEntity elasticIp = elasticIpEntities.get(uuid);
			StartEndTimes startEndTimes = ipStartEndTimes.get(uuid);
			if (uuid == null) {
				log.error("elasticIp without corresponding start end times:" + uuid);
				continue;
			}
			elasticIp.getUsage().setDurationMs(startEndTimes.getEndTimeMs() - startEndTimes.getStartTimeMs());
			elasticIp.getUsage().setIpNum(1);
		}
		
		/* Scan instance entities so we can get the instance id from the uuid
		 */
		Map<String,InstanceArtEntity> instanceEntities = new HashMap<String,InstanceArtEntity>();
		iter = wrapper.scanWithNativeQuery( "scanInstanceCreateEvents" );
		while (iter.hasNext()) {
			ReportingInstanceCreateEvent createEvent = (ReportingInstanceCreateEvent) iter.next();
			InstanceArtEntity instance = new InstanceArtEntity(createEvent.getInstanceType(), createEvent.getInstanceId());
			instanceEntities.put(createEvent.getUuid(), instance);
		}
		
		/* Find attachment start times
		 */
		AttachDurationCalculator<String,String> durationCalc = new AttachDurationCalculator<String,String>(report.getBeginMs(), report.getEndMs());
		iter = wrapper.scanWithNativeQuery( "scanElasticIpAttachEvents" );
		while (iter.hasNext()) {
			ReportingElasticIpAttachEvent attachEvent = (ReportingElasticIpAttachEvent) iter.next();
			durationCalc.attach(attachEvent.getInstanceUuid(), attachEvent.getIpUuid(),
					attachEvent.getTimestampMs());
		}

		/* Find attachment end times and set durations
		 */
		iter = wrapper.scanWithNativeQuery( "scanElasticIpDetachEvents" );
		while (iter.hasNext()) {
			ReportingElasticIpDetachEvent detachEvent = (ReportingElasticIpDetachEvent) iter.next();
			long duration = durationCalc.detach(detachEvent.getInstanceUuid(),
					detachEvent.getIpUuid(), detachEvent.getTimestampMs());
			if (duration==0) continue;
			if (! elasticIpEntities.containsKey(detachEvent.getIpUuid())) continue;
			ElasticIpArtEntity elasticIp = elasticIpEntities.get(detachEvent.getIpUuid());
			String instanceId = instanceEntities.get(detachEvent.getInstanceUuid()).getInstanceId();
			/* If an ip is repeatedly attached to and detached from an instance,
			 * add up the total attachment time.
			 */
			if (elasticIp.getInstanceAttachments().containsKey(instanceId)) {
				duration += elasticIp.getInstanceAttachments().get(instanceId).getDurationMs();
			}
			ElasticIpUsageArtEntity usage = new ElasticIpUsageArtEntity();
			usage.setDurationMs(duration);
			elasticIp.getInstanceAttachments().put(instanceId, usage);
		}

		
		/* Perform totals and summations for user, account, and zone
		 */
		for (String zoneName : report.getZones().keySet()) {
			AvailabilityZoneArtEntity zone = report.getZones().get(zoneName);
			for (String accountName : zone.getAccounts().keySet()) {
				AccountArtEntity account = zone.getAccounts().get(accountName);
				for (String userName : account.getUsers().keySet()) {
					UserArtEntity user = account.getUsers().get(userName);
					for (String ipUuid : user.getElasticIps().keySet()) {
						ElasticIpArtEntity ip = user.getElasticIps().get(ipUuid);
						updateUsageTotals(user.getUsageTotals().getElasticIpTotals(), ip.getUsage());
						updateUsageTotals(account.getUsageTotals().getElasticIpTotals(), ip.getUsage());
						updateUsageTotals(zone.getUsageTotals().getElasticIpTotals(), ip.getUsage());
					}
				}
			}
		}


		return report;
	}
	
	private static void updateUsageTotals(ElasticIpUsageArtEntity totalEntity,
			ElasticIpUsageArtEntity newEntity)
	{

		totalEntity.setIpNum(totalEntity.getIpNum()+newEntity.getIpNum());
		totalEntity.setDurationMs(totalEntity.getDurationMs()+newEntity.getDurationMs());

	}

	private static class StartEndTimes
	{
		private long startTimeMs;
		private long endTimeMs;

		private StartEndTimes(long startTimeMs, long endTimeMs )
		{
			this.startTimeMs = startTimeMs;
			this.endTimeMs = endTimeMs;
		}
		
		public long getStartTimeMs()
		{
			return startTimeMs;
		}
		
		public long getEndTimeMs()
		{
			return endTimeMs;
		}
		
		public void setEndTimeMs(long endTimeMs)
		{
			this.endTimeMs = endTimeMs;
		}
		
		public long getDurationMs()
		{
			return endTimeMs - startTimeMs;
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


}
