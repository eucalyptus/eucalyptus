package com.eucalyptus.reporting.modules.storage;

import java.util.Map;

import org.apache.log4j.Logger;

import com.eucalyptus.entities.EntityWrapper;
import com.eucalyptus.event.EventListener;
import com.eucalyptus.reporting.event.Event;
import com.eucalyptus.reporting.event.StorageEvent;
import com.eucalyptus.reporting.user.ReportingAccountDao;
import com.eucalyptus.reporting.user.ReportingUserDao;

public class StorageEventListener
	implements EventListener<Event>
{
	private static Logger LOG = Logger.getLogger( StorageEventListener.class );
	
	private static long WRITE_INTERVAL_MS = 10800000l; //write every 3 hrs

	private Map<StorageSummaryKey, StorageUsageData> usageDataMap;
	private long lastAllSnapshotMs = 0l;

	public StorageEventListener()
	{
	}
	
	@Override
	public void fireEvent(Event event)
	{
		if (event instanceof StorageEvent) {
			StorageEvent storageEvent = (StorageEvent) event;

			
			/* Retain records of all account and user id's and names encountered
			 * even if they're subsequently deleted.
			 */
			ReportingAccountDao.getInstance().addUpdateAccount(
					storageEvent.getAccountId(), storageEvent.getAccountName());
			ReportingUserDao.getInstance().addUpdateUser(storageEvent.getOwnerId(),
					storageEvent.getOwnerName());


			long timeMillis = getCurrentTimeMillis();

			final StorageUsageLog usageLog = StorageUsageLog.getStorageUsageLog();

			EntityWrapper<StorageUsageSnapshot> entityWrapper =
				EntityWrapper.get( StorageUsageSnapshot.class );
			try {

				LOG.debug("Receive event:" + storageEvent.toString());

				/* Load usageDataMap if starting up
				 */
				if (usageDataMap == null) {

					this.usageDataMap = usageLog.findLatestUsageData();
					LOG.info("Loaded usageDataMap");

				}

				
				/* Update usageDataMap
				 */
				StorageSummaryKey key = new StorageSummaryKey(
						storageEvent.getOwnerId(),
						storageEvent.getAccountId(),
						storageEvent.getClusterName(),
						storageEvent.getAvailabilityZone());
				StorageUsageData usageData;
				if (usageDataMap.containsKey(key)) {
					usageData = usageDataMap.get(key);
				} else {
					usageData = new StorageUsageData();
					usageDataMap.put(key, usageData);
				}
				long addAmountMegs = (storageEvent.isCreateOrDelete())
						? storageEvent.getSizeMegs()
						: -storageEvent.getSizeMegs();
				long addNum = (storageEvent.isCreateOrDelete()) ? 1 : -1;
				switch(storageEvent.getEventType()) {
					case EbsSnapshot:
						Long newSnapshotsNum =
							addLong(usageData.getSnapshotsNum(), addNum);
						usageData.setSnapshotsNum(newSnapshotsNum);
						Long newSnapshotsMegs =
							addLong(usageData.getSnapshotsMegs(), addAmountMegs);
						usageData.setSnapshotsMegs(newSnapshotsMegs);
						break;
					case EbsVolume:
						Long newVolumesNum =
							addLong(usageData.getVolumesNum(), addNum);
						usageData.setVolumesNum(newVolumesNum);
						Long newVolumesMegs =
							addLong(usageData.getVolumesMegs(), addAmountMegs);
						usageData.setVolumesMegs(newVolumesMegs);						
						break;

				}

				/* Write data to DB
				 */
				if ((timeMillis - lastAllSnapshotMs) > WRITE_INTERVAL_MS) {
					/* Write all snapshots
					 */
					LOG.info("Starting allSnapshot...");
					for (StorageSummaryKey summaryKey: usageDataMap.keySet()) {
						StorageSnapshotKey snapshotKey = new StorageSnapshotKey(
								summaryKey.getOwnerId(), summaryKey.getAccountId(),
								summaryKey.getClusterName(),
								summaryKey.getAvailabilityZone(), timeMillis);
						StorageUsageSnapshot sus =
							new StorageUsageSnapshot(snapshotKey, usageDataMap.get(key));
						sus.setAllSnapshot(true);
						LOG.info("Storing as part of allSnapshot:" + sus);
						entityWrapper.add(sus);
						lastAllSnapshotMs = timeMillis;
					}
					LOG.info("Ending allSnapshot...");
				} else {
					/* Write this snapshot
					 */
					StorageSnapshotKey snapshotKey = new StorageSnapshotKey(
							key.getOwnerId(), key.getAccountId(), key.getClusterName(),
							key.getAvailabilityZone(), timeMillis);
					StorageUsageSnapshot sus =
						new StorageUsageSnapshot(snapshotKey, usageDataMap.get(key));
					LOG.info("Storing:" + sus);
					entityWrapper.add(sus);
				}

				entityWrapper.commit();
			} catch (Exception ex) {
				entityWrapper.rollback();
				LOG.error(ex);		
			}
		}
	}
	
	private static final Long addLong(Long a, long b)
	{
		return new Long(a.longValue() + b);
	}

	protected long getCurrentTimeMillis()
	{
		return (this.testCurrentTimeMillis == null)
				? System.currentTimeMillis()
				: this.testCurrentTimeMillis.longValue();
	}

	private Long testCurrentTimeMillis = null;

	/**
	 * Used only for testing. Sets the poller into test mode and overrides the
	 * actual timestamp with a false one.
	 */
	protected void setCurrentTimeMillis(long testCurrentTimeMillis)
	{
		this.testCurrentTimeMillis = new Long(testCurrentTimeMillis);
	}

}
