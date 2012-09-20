package com.eucalyptus.reporting.art.generator;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.log4j.Logger;

import com.eucalyptus.entities.EntityWrapper;
import com.eucalyptus.reporting.art.entity.AccountArtEntity;
import com.eucalyptus.reporting.art.entity.AvailabilityZoneArtEntity;
import com.eucalyptus.reporting.art.entity.InstanceArtEntity;
import com.eucalyptus.reporting.art.entity.VolumeArtEntity;
import com.eucalyptus.reporting.art.entity.ReportArtEntity;
import com.eucalyptus.reporting.art.entity.UserArtEntity;
import com.eucalyptus.reporting.art.entity.VolumeUsageArtEntity;
import com.eucalyptus.reporting.art.util.AttachDurationCalculator;
import com.eucalyptus.reporting.art.util.DurationCalculator;
import com.eucalyptus.reporting.art.util.StartEndTimes;
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
		 * and create a Map of the volume nodes at the bottom with start times.
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
			volume.getUsage().setSizeGB( createEvent.getSizeGB() );
			long startTime = Math.max(report.getBeginMs(), createEvent.getTimestampMs());
			if (startTime <= report.getEndMs()) {
				user.getVolumes().put(createEvent.getUuid(), volume);
				volStartEndTimes.put(createEvent.getUuid(), new StartEndTimes( startTime, report.getEndMs() ));
				volumeEntities.put(createEvent.getUuid(), volume);
				volume.getUsage().setVolumeCnt(1);
			}
		}
		
		/* Find end times for the volumes
		 */
		iter = wrapper.scanWithNativeQuery( "scanVolumeDeleteEvents" );
		while (iter.hasNext()) {
			ReportingVolumeDeleteEvent deleteEvent = (ReportingVolumeDeleteEvent) iter.next();
			long endTime = Math.min(deleteEvent.getTimestampMs(), report.getEndMs());
			if (endTime >= report.getBeginMs() && volStartEndTimes.containsKey(deleteEvent.getUuid())) {
				StartEndTimes startEndTimes = volStartEndTimes.get(deleteEvent.getUuid());
				startEndTimes.setEndTime(endTime);
				volumeEntities.remove(deleteEvent.getUuid());
				volStartEndTimes.remove(deleteEvent.getUuid());
			}
		}

		/* Set the duration of each volume
		 */
		for (String uuid: volumeEntities.keySet()) {
			VolumeArtEntity volume = volumeEntities.get(uuid);
			StartEndTimes startEndTimes = volStartEndTimes.get(uuid);
			if (uuid == null) {
				log.error("volume without corresponding start end times:" + uuid);
				continue;
			}
			long duration = DurationCalculator.boundDuration(report.getBeginMs(), report.getEndMs(),
					startEndTimes.getStartTime(), startEndTimes.getEndTime());
			volume.getUsage().setGBSecs(duration*volume.getUsage().getSizeGB());
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
		iter = wrapper.scanWithNativeQuery( "scanVolumeAttachEvents" );
		while (iter.hasNext()) {
			ReportingVolumeAttachEvent attachEvent = (ReportingVolumeAttachEvent) iter.next();
			durationCalc.attach(attachEvent.getInstanceUuid(), attachEvent.getVolumeUuid(),
					attachEvent.getTimestampMs());
		}

		/* Find attachment end times and set durations
		 */
		iter = wrapper.scanWithNativeQuery( "scanVolumeDetachEvents" );
		while (iter.hasNext()) {
			ReportingVolumeDetachEvent detachEvent = (ReportingVolumeDetachEvent) iter.next();
			long duration = durationCalc.detach(detachEvent.getInstanceUuid(),
					detachEvent.getVolumeUuid(), detachEvent.getTimestampMs());
			if (duration==0) continue;
			if (! volumeEntities.containsKey(detachEvent.getVolumeUuid())) continue;
			VolumeArtEntity volume = volumeEntities.get(detachEvent.getVolumeUuid());
			long gbsecs = duration * volume.getUsage().getSizeGB();
			/* If a volume is repeatedly attached to and detached from an instance,
			 * add up the total attachment time.
			 */
			if (volume.getInstanceAttachments().containsKey(detachEvent.getInstanceUuid())) {
				gbsecs += volume.getInstanceAttachments().get(detachEvent.getInstanceUuid()).getGBSecs();
			}
			VolumeUsageArtEntity usage = new VolumeUsageArtEntity();
			usage.setGBSecs(gbsecs);
			usage.setSizeGB(volume.getUsage().getSizeGB());
			usage.setVolumeCnt(1);
			volume.getInstanceAttachments().put(detachEvent.getInstanceUuid(), usage);
		}

		
		/* Perform totals and summations for user, account, and zone
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
		
		totalEntity.setGBSecs(totalEntity.getGBSecs()+newEntity.getGBSecs());
		totalEntity.setVolumeCnt(totalEntity.getVolumeCnt()+1);
		totalEntity.setSizeGB(plus(totalEntity.getSizeGB(), newEntity.getSizeGB()));

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
	

}
