package com.eucalyptus.reporting.art.generator;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.log4j.Logger;

import com.eucalyptus.entities.EntityWrapper;
import com.eucalyptus.reporting.art.entity.AccountArtEntity;
import com.eucalyptus.reporting.art.entity.AvailabilityZoneArtEntity;
import com.eucalyptus.reporting.art.entity.ClusterArtEntity;
import com.eucalyptus.reporting.art.entity.InstanceArtEntity;
import com.eucalyptus.reporting.art.entity.InstanceUsageArtEntity;
import com.eucalyptus.reporting.art.entity.UsageTotalsArtEntity;
import com.eucalyptus.reporting.art.entity.VolumeArtEntity;
import com.eucalyptus.reporting.art.entity.ReportArtEntity;
import com.eucalyptus.reporting.art.entity.UserArtEntity;
import com.eucalyptus.reporting.art.entity.VolumeUsageArtEntity;
import com.eucalyptus.reporting.domain.ReportingAccount;
import com.eucalyptus.reporting.domain.ReportingAccountDao;
import com.eucalyptus.reporting.domain.ReportingUser;
import com.eucalyptus.reporting.domain.ReportingUserDao;
import com.eucalyptus.reporting.event_store.*;

public class VolumeArtGenerator
	implements ArtGenerator
{
    private static Logger log = Logger.getLogger( VolumeArtGenerator.class );

    public VolumeArtGenerator()
	{
		
	}
	
	public ReportArtEntity generateReportArt(ReportArtEntity report)
	{
		log.debug("GENERATING REPORT ART");
		EntityWrapper wrapper = EntityWrapper.get( ReportingVolumeCreateEvent.class );

		/* Create super-tree of availZones, accounts, users, and volumes;
		 * and create a Map of the volume nodes at the bottom.
		 */
		Iterator iter = wrapper.scanWithNativeQuery( "scanVolumeCreateEvents" );
		Map<String,VolumeArtEntity> volumeEntities = new HashMap<String,VolumeArtEntity>();
		Map<String,StartEndTimes> volStartEndTimes = new HashMap<String,StartEndTimes>();
		while (iter.hasNext()) {
			ReportingVolumeCreateEvent createEvent = (ReportingVolumeCreateEvent) iter.next();
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
			VolumeArtEntity volume = new VolumeArtEntity(createEvent.getVolumeId());
			user.getVolumes().put(createEvent.getUuid(), volume);
			volume.getUsage().setSizeGB( createEvent.getSizeGB() );
			long startTime = Math.max(report.getBeginMs(), createEvent.getTimestampMs());
			volStartEndTimes.put(createEvent.getUuid(), new StartEndTimes( startTime, report.getEndMs() ));
			volumeEntities.put(createEvent.getUuid(), volume);
		}
		
		iter = wrapper.scanWithNativeQuery( "scanVolumeDeleteEvents" );
		while (iter.hasNext()) {
			ReportingVolumeDeleteEvent deleteEvent = (ReportingVolumeDeleteEvent) iter.next();
			VolumeArtEntity volume = volumeEntities.get(deleteEvent.getUuid());
			long endTime = Math.min(deleteEvent.getTimestampMs(), report.getEndMs());
			if (! volStartEndTimes.containsKey(deleteEvent.getUuid())) {
				log.error("Volume delete event without corresponding start event:" + deleteEvent.getUuid());
				continue;
			}
			StartEndTimes startEndTimes = volStartEndTimes.get(deleteEvent.getUuid());
			startEndTimes.setEndTimeMs(endTime);
		}

		for (String uuid: volumeEntities.keySet()) {
			VolumeArtEntity volume = volumeEntities.get(uuid);
			StartEndTimes startEndTimes = volStartEndTimes.get(uuid);
			if (uuid == null) {
				log.error("volume without corresponding start end times:" + uuid);
				continue;
			}
			volume.getUsage().setDurationMs(startEndTimes.getDurationMs());
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
		iter = wrapper.scanWithNativeQuery( "scanVolumeAttachEvents" );
		Map<String,Map<String,StartEndTimes>> attachStartEndTimes =
			new HashMap<String,Map<String,StartEndTimes>>(); //volUuid -> instanceUuid -> startEndTimes
		while (iter.hasNext()) {
			ReportingVolumeAttachEvent attachEvent = (ReportingVolumeAttachEvent) iter.next();
			long startTime = Math.max(report.getBeginMs(), attachEvent.getTimestampMs());
			if (! attachStartEndTimes.containsKey(attachEvent.getVolumeUuid())) {
				attachStartEndTimes.put(attachEvent.getVolumeUuid(), new HashMap<String,StartEndTimes>());
			}
			Map<String,StartEndTimes> innerMap = attachStartEndTimes.get(attachEvent.getVolumeUuid());
			innerMap.put(attachEvent.getInstanceUuid(), new StartEndTimes( startTime, report.getEndMs() ));
		}

		/* Find attachment end times
		 */
		iter = wrapper.scanWithNativeQuery( "scanVolumeDetachEvents" );
		while (iter.hasNext()) {
			ReportingVolumeDetachEvent detachEvent = (ReportingVolumeDetachEvent) iter.next();
			long endTime = Math.max(report.getBeginMs(), detachEvent.getTimestampMs());
			if (! attachStartEndTimes.containsKey(detachEvent.getVolumeUuid())) {
				log.error("Detach volume without corresponding attach:" + detachEvent.getVolumeUuid());
				continue;
			}
			Map<String,StartEndTimes> innerMap = attachStartEndTimes.get(detachEvent.getVolumeUuid());
			if (!innerMap.containsKey(detachEvent.getInstanceUuid())) {
				log.error("Detach instance without corresponding attach:" + detachEvent.getInstanceUuid());
				continue;				
			}
			innerMap.get(detachEvent.getInstanceUuid()).setEndTimeMs(endTime);
		}

		for (String volUuid: volumeEntities.keySet()) {
			VolumeArtEntity volume = volumeEntities.get(volUuid);
			if (! attachStartEndTimes.containsKey(volUuid)) {
				log.error("Volume without corresponding start/end times:" + volUuid);
				continue;								
			}
			Map<String,StartEndTimes> innerMap = attachStartEndTimes.get(volUuid);
			for (String instanceUuid: innerMap.keySet()) {
				VolumeUsageArtEntity attachUsage = new VolumeUsageArtEntity();
				attachUsage.setSizeGB(volume.getUsage().getSizeGB());
				attachUsage.setDurationMs(innerMap.get(instanceUuid).getDurationMs());
				volume.getInstanceAttachments().put(instanceUuid, attachUsage);
			}
		}

		
		/* Perform totals and summations
		 */
		for (String zoneName : report.getZones().keySet()) {
			AvailabilityZoneArtEntity zone = report.getZones().get(zoneName);
			for (String accountName : zone.getAccounts().keySet()) {
				AccountArtEntity account = zone.getAccounts().get(accountName);
				for (String userName : account.getUsers().keySet()) {
					UserArtEntity user = account.getUsers().get(userName);
					for (String volumeUuid : user.getVolumes().keySet()) {
						VolumeArtEntity volume = user.getVolumes().get(volumeUuid);
						updateUsageTotals(user.getUsageTotals().getVolumeTotals(), volume.getUsage());
						updateUsageTotals(account.getUsageTotals().getVolumeTotals(), volume.getUsage());
						updateUsageTotals(zone.getUsageTotals().getVolumeTotals(), volume.getUsage());
					}
				}
			}
		}


		return report;
	}
	
	private static void updateUsageTotals(VolumeUsageArtEntity totalEntity,
			VolumeUsageArtEntity newEntity)
	{
		
		totalEntity.setDurationMs(totalEntity.getDurationMs()+newEntity.getDurationMs());
		totalEntity.setVolumeCnt(totalEntity.getVolumeCnt()+1);
		totalEntity.setSizeGB(plus(totalEntity.getSizeGB(), newEntity.getSizeGB()));

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
