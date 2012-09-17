package com.eucalyptus.reporting.art.generator;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.log4j.Logger;

import com.eucalyptus.entities.EntityWrapper;
import com.eucalyptus.reporting.art.entity.AccountArtEntity;
import com.eucalyptus.reporting.art.entity.AvailabilityZoneArtEntity;
import com.eucalyptus.reporting.art.entity.ReportArtEntity;
import com.eucalyptus.reporting.art.entity.UserArtEntity;
import com.eucalyptus.reporting.art.entity.VolumeArtEntity;
import com.eucalyptus.reporting.art.entity.VolumeSnapshotUsageArtEntity;
import com.eucalyptus.reporting.domain.ReportingAccount;
import com.eucalyptus.reporting.domain.ReportingAccountDao;
import com.eucalyptus.reporting.domain.ReportingUser;
import com.eucalyptus.reporting.domain.ReportingUserDao;
import com.eucalyptus.reporting.event_store.ReportingVolumeCreateEvent;
import com.eucalyptus.reporting.event_store.ReportingVolumeSnapshotCreateEvent;

public class VolumeSnapshotArtGenerator
	implements ArtGenerator
{
    private static Logger log = Logger.getLogger( VolumeSnapshotArtGenerator.class );

    public VolumeSnapshotArtGenerator()
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
			volumeEntities.put(createEvent.getUuid(), volume);
		}
		

		iter = wrapper.scanWithNativeQuery( "scanVolumeSnapshotCreateEvents" );
		Map<String, VolumeSnapshotUsageArtEntity> snapshotEntities = new HashMap<String, VolumeSnapshotUsageArtEntity>();
		Map<String, Long> snapshotStartTimes = new HashMap<String, Long>();
		while (iter.hasNext()) {
			ReportingVolumeSnapshotCreateEvent createEvent = (ReportingVolumeSnapshotCreateEvent) iter.next();
			VolumeSnapshotUsageArtEntity usage = new VolumeSnapshotUsageArtEntity();
			usage.setSizeGB(createEvent.getSizeGB());
			usage.setSnapshotNum(1);
			/* Default sizeGB is remainder of report * GB. This will be overwritten later if there's
			 * a corresponding delete event before the report end, later.
			 */
			usage.setGBSecs(createEvent.getSizeGB() * (report.getEndMs() - createEvent.getTimestampMs()));
			if (createEvent.getTimestampMs() <= report.getEndMs()) {
				VolumeArtEntity volume = volumeEntities.get(createEvent.getVolumeUuid());
				volume.getSnapshotUsage().put(createEvent.getVolumeSnapshotId(), usage);
				snapshotEntities.put(createEvent.getUuid(), usage);
				snapshotStartTimes.put(createEvent.getUuid(), createEvent.getTimestampMs());
			}
		}
		
		iter = wrapper.scanWithNativeQuery( "scanVolumeSnapshotDeleteEvents" );
		while (iter.hasNext()) {
			ReportingVolumeSnapshotCreateEvent deleteEvent = (ReportingVolumeSnapshotCreateEvent) iter.next();
			if (snapshotEntities.containsKey(deleteEvent.getUuid())) {
				VolumeSnapshotUsageArtEntity snap = snapshotEntities.get(deleteEvent.getUuid());
				long startTimeMs = snapshotStartTimes.get(deleteEvent.getUuid()).longValue();
				snap.setSizeGB(snap.getSizeGB() * (deleteEvent.getTimestampMs() - startTimeMs));
			}
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
						for (String snapId: volume.getSnapshotUsage().keySet()) {
							VolumeSnapshotUsageArtEntity snap = volume.getSnapshotUsage().get(snapId);
							updateUsageTotals(volume.getSnapshotTotals(), snap);							
							updateUsageTotals(user.getUsageTotals().getSnapshotTotals(), snap);
							updateUsageTotals(account.getUsageTotals().getSnapshotTotals(), snap);
							updateUsageTotals(zone.getUsageTotals().getSnapshotTotals(), snap);
							
						}
					}
				}
			}
		}


		return report;
	}
	
	private static void updateUsageTotals(VolumeSnapshotUsageArtEntity totalEntity,
			VolumeSnapshotUsageArtEntity newEntity)
	{
		
		totalEntity.setSnapshotNum(newEntity.getSnapshotNum()+1);
		totalEntity.setGBSecs(totalEntity.getGBSecs()+newEntity.getGBSecs());
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
