package com.eucalyptus.reporting.storage;

import java.util.*;

import org.apache.log4j.Logger;

import com.eucalyptus.entities.EntityWrapper;
import com.eucalyptus.event.EventListener;
import com.eucalyptus.reporting.Period;
import com.eucalyptus.reporting.event.Event;
import com.eucalyptus.reporting.event.StorageEvent;

public class StorageEventListener
	implements EventListener<Event>
{
	private static Logger LOG = Logger.getLogger( StorageEventListener.class );
	
	private static long WRITE_INTERVAL_MS = 10800000l; //write every 3 hrs

	private Map<UsageDataKey, StorageUsageData> usageDataMap;
	private long lastAllSnapshotMs = 0l;

	public StorageEventListener()
	{
	}
	
	@Override
	public void fireEvent(Event event)
	{
		if (event instanceof StorageEvent) {
			StorageEvent storageEvent = (StorageEvent) event;
			long timeMillis = getCurrentTimeMillis();

			final StorageUsageLog usageLog = StorageUsageLog.getStorageUsageLog();

			EntityWrapper<StorageUsageSnapshot> entityWrapper =
				EntityWrapper.get( StorageUsageSnapshot.class );
			try {

				LOG.debug("Receive event:" + storageEvent.toString());

				/* Load usageDataMap if starting up
				 */
				if (usageDataMap == null) {
					this.usageDataMap = new HashMap<UsageDataKey, StorageUsageData>();
					// TODO: optimize so we don't have to scan entire log on
					// startup
					Iterator<StorageUsageSnapshot> iter = usageLog
							.scanLog(new Period(0l, Long.MAX_VALUE));
					while (iter.hasNext()) {
						StorageUsageSnapshot snapshot = iter.next();
						UsageDataKey key = new UsageDataKey(
								snapshot.getSnapshotKey());
						usageDataMap.put(key, snapshot.getUsageData());
						if (snapshot.getAllSnapshot()) {
							lastAllSnapshotMs = timeMillis;							
						}
						System.out.println("Loaded key:" + key);
					}
					LOG.info("Loaded usageDataMap, last allSnapshot:" + lastAllSnapshotMs);
				}

				
				/* Update usageDataMap
				 */
				UsageDataKey key = new UsageDataKey(storageEvent.getOwnerId(),
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
					for (UsageDataKey udk: usageDataMap.keySet()) {
						SnapshotKey snapshotKey = udk.newSnapshotKey(timeMillis);
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
					SnapshotKey snapshotKey = key.newSnapshotKey(timeMillis);
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

	private class UsageDataKey
	{
		private final String ownerId;
		private final String accountId;
		private final String clusterName;
		private final String availabilityZone;

		public UsageDataKey(String ownerId, String accountId, String clusterName,
				String availabilityZone)
		{
			this.ownerId = ownerId;
			this.accountId = accountId;
			this.clusterName = clusterName;
			this.availabilityZone = availabilityZone;
		}
		
		public UsageDataKey(SnapshotKey key)
		{
			this.ownerId = key.getOwnerId();
			this.accountId = key.getAccountId();
			this.clusterName = key.getClusterName();
			this.availabilityZone = key.getAvailabilityZone();
		}

		public String getOwnerId()
		{
			return ownerId;
		}

		public String getAccountId()
		{
			return accountId;
		}

		public String getClusterName()
		{
			return clusterName;
		}

		public String getAvailabilityZone()
		{
			return availabilityZone;
		}
		
		public SnapshotKey newSnapshotKey(long timestampMs)
		{
			return new SnapshotKey(ownerId, accountId, clusterName, availabilityZone, timestampMs);
		}

		@Override
		public int hashCode()
		{
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			result = prime * result
					+ ((accountId == null) ? 0 : accountId.hashCode());
			result = prime
					* result
					+ ((availabilityZone == null) ? 0 : availabilityZone
							.hashCode());
			result = prime * result
					+ ((clusterName == null) ? 0 : clusterName.hashCode());
			result = prime * result
					+ ((ownerId == null) ? 0 : ownerId.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj)
		{
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			UsageDataKey other = (UsageDataKey) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			if (accountId == null) {
				if (other.accountId != null)
					return false;
			} else if (!accountId.equals(other.accountId))
				return false;
			if (availabilityZone == null) {
				if (other.availabilityZone != null)
					return false;
			} else if (!availabilityZone.equals(other.availabilityZone))
				return false;
			if (clusterName == null) {
				if (other.clusterName != null)
					return false;
			} else if (!clusterName.equals(other.clusterName))
				return false;
			if (ownerId == null) {
				if (other.ownerId != null)
					return false;
			} else if (!ownerId.equals(other.ownerId))
				return false;
			return true;
		}

		private StorageEventListener getOuterType()
		{
			return StorageEventListener.this;
		}


	}
}
