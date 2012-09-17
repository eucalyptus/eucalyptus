package com.eucalyptus.reporting.art.generator;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.log4j.Logger;

import com.eucalyptus.entities.EntityWrapper;
import com.eucalyptus.reporting.art.entity.AccountArtEntity;
import com.eucalyptus.reporting.art.entity.AvailabilityZoneArtEntity;
import com.eucalyptus.reporting.art.entity.BucketArtEntity;
import com.eucalyptus.reporting.art.entity.ReportArtEntity;
import com.eucalyptus.reporting.art.entity.S3ObjectUsageArtEntity;
import com.eucalyptus.reporting.art.entity.UserArtEntity;
import com.eucalyptus.reporting.domain.ReportingAccount;
import com.eucalyptus.reporting.domain.ReportingAccountDao;
import com.eucalyptus.reporting.domain.ReportingUser;
import com.eucalyptus.reporting.domain.ReportingUserDao;
import com.eucalyptus.reporting.event_store.ReportingS3BucketCreateEvent;
import com.eucalyptus.reporting.event_store.ReportingS3ObjectCreateEvent;
import com.eucalyptus.reporting.event_store.ReportingS3ObjectDeleteEvent;

public class S3ArtGenerator
	implements ArtGenerator
{
    private static Logger log = Logger.getLogger( S3ArtGenerator.class );

    public S3ArtGenerator()
	{
		
	}
	
	public ReportArtEntity generateReportArt(ReportArtEntity report)
	{
		log.debug("GENERATING REPORT ART");
		EntityWrapper wrapper = EntityWrapper.get( ReportingS3BucketCreateEvent.class );

		/* Create super-tree of availZones, accounts, users, and volumes;
		 * and create a Map of the volume nodes at the bottom with start times.
		 */
		Iterator iter = wrapper.scanWithNativeQuery( "scanS3BucketCreateEvents" );
		Map<String,BucketArtEntity> bucketEntities = new HashMap<String,BucketArtEntity>();
		while (iter.hasNext()) {
			ReportingS3BucketCreateEvent createEvent = (ReportingS3BucketCreateEvent) iter.next();
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
			if (! user.getBuckets().containsKey(createEvent.getS3BucketName())) {
				user.getBuckets().put(createEvent.getS3BucketName(), new BucketArtEntity());
			}
			BucketArtEntity bucket = user.getBuckets().get(createEvent.getS3BucketName());
			bucketEntities.put(createEvent.getS3BucketName(), bucket);
		}
		
		/* Scan s3 objects and add to buckets
		 * size, start times ("buck/obj" keyed), add to buckets
		 */
		iter = wrapper.scanWithNativeQuery( "scanS3ObjectCreateEvents" );
		Map<String, Map<String, Long>> objectStartTimes = new HashMap<String, Map<String, Long>>();
		while (iter.hasNext()) {
			ReportingS3ObjectCreateEvent createEvent = (ReportingS3ObjectCreateEvent) iter.next();
			if (createEvent.getTimestampMs() < report.getEndMs()) {
				S3ObjectUsageArtEntity usage = new S3ObjectUsageArtEntity();
				usage.setSizeGB(createEvent.getSizeGB());
				usage.setObjectsNum(1);
				/* By default this object ends at the end of the report, but this is overridden
				 *   down below if it ends earlier.
				 */
				long durationSecs = (report.getEndMs()-createEvent.getTimestampMs())/1000;
				usage.setGBsecs(durationSecs*createEvent.getSizeGB());
				bucketEntities.get(createEvent.getS3BucketName()).getObjects().put(createEvent.getS3ObjectName(), usage);
				if (! objectStartTimes.containsKey(createEvent.getS3BucketName())) {
					objectStartTimes.put(createEvent.getS3BucketName(), new HashMap<String, Long>());
				}
				Map<String, Long> innerMap = objectStartTimes.get(createEvent.getS3BucketName());
				innerMap.put(createEvent.getS3ObjectName(), createEvent.getTimestampMs());
			}
		}

		/* Find end times for the objects, fill in values
		 */
		iter = wrapper.scanWithNativeQuery( "scanS3ObjectDeleteEvents" );
		while (iter.hasNext()) {
			ReportingS3ObjectDeleteEvent deleteEvent = (ReportingS3ObjectDeleteEvent) iter.next();
			String bukName = deleteEvent.getS3BucketName();
			String objName = deleteEvent.getS3ObjectName();
			
			/* Determine duration for this object */
			long endTime = Math.min(deleteEvent.getTimestampMs(), report.getEndMs());
			if (! objectStartTimes.containsKey(bukName)) continue;
			Map<String, Long> innerMap = objectStartTimes.get(bukName);
			if (! innerMap.containsKey(objName)) continue;
			long durationSecs = (endTime-innerMap.get(objName).longValue())/1000;
			if (! bucketEntities.containsKey(bukName)) continue;
			BucketArtEntity bucket = bucketEntities.get(bukName);
			/* Delete objects which end before report begins */
			if (deleteEvent.getTimestampMs() < report.getBeginMs()) {
				bucket.getObjects().remove(deleteEvent.getS3ObjectName());
			} else {
				if (!bucket.getObjects().containsKey(objName)) continue;
				S3ObjectUsageArtEntity usage = bucket.getObjects().get(objName);
				usage.setGBsecs(usage.getSizeGB()*durationSecs);
			}
		}		
		
		/* Perform totals and summations for user, account, zone, and bucket
		 */
		for (String zoneName : report.getZones().keySet()) {
			AvailabilityZoneArtEntity zone = report.getZones().get(zoneName);
			for (String accountName : zone.getAccounts().keySet()) {
				AccountArtEntity account = zone.getAccounts().get(accountName);
				for (String userName : account.getUsers().keySet()) {
					UserArtEntity user = account.getUsers().get(userName);
					for (String bucketName : user.getBuckets().keySet()) {
						BucketArtEntity bucket = user.getBuckets().get(bucketName);
						for (String objectName: bucket.getObjects().keySet()) {
							S3ObjectUsageArtEntity usage = bucket.getObjects().get(objectName);
							updateUsageTotals(bucket.getTotalUsage(), usage);
							updateUsageTotals(user.getUsageTotals().getS3ObjectTotals(), usage);
							updateUsageTotals(account.getUsageTotals().getS3ObjectTotals(), usage);
							updateUsageTotals(zone.getUsageTotals().getS3ObjectTotals(), usage);							
						}
					}
				}
			}
		}


		return report;
	}
	
	private static void updateUsageTotals(S3ObjectUsageArtEntity totalEntity,
			S3ObjectUsageArtEntity newEntity)
	{

		totalEntity.setObjectsNum(totalEntity.getObjectsNum() + newEntity.getObjectsNum());
		totalEntity.setGBsecs(totalEntity.getGBsecs() + newEntity.getGBsecs());

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
