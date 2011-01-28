package com.eucalyptus.reporting.storage;

import java.util.*;

import org.hibernate.*;
import org.apache.log4j.*;

import com.eucalyptus.entities.EntityWrapper;
import com.eucalyptus.reporting.Period;
import com.eucalyptus.reporting.event.*;
import com.eucalyptus.reporting.queue.QueueReceiver;

public class StorageEventPoller
{
	private static Logger LOG = Logger.getLogger( StorageEventPoller.class );

	private final QueueReceiver receiver;
	private Map<UsageDataKey, StorageUsageData> usageDataMap;
	private Set<UsageDataKey> changedSnapshots;

	public StorageEventPoller(QueueReceiver receiver)
	{
		this.receiver = receiver;
		this.usageDataMap = null;
	}

	public void writeEvents()
	{
		EntityWrapper<StorageUsageSnapshot> entityWrapper =
			EntityWrapper.get( StorageUsageSnapshot.class );
		Session sess = null;
		try {

			sess = entityWrapper.getSession();
			final StorageUsageLog usageLog = StorageUsageLog.getStorageUsageLog();
			
			/* Load usageDataMap
			 */
			if (usageDataMap == null) {
				this.usageDataMap = new HashMap<UsageDataKey, StorageUsageData>();
				this.changedSnapshots = new HashSet<UsageDataKey>();
				//TODO: optimize so we don't have to scan entire log on startup
				Iterator<StorageUsageSnapshot> iter =
					usageLog.scanLog(new Period(0l, Long.MAX_VALUE));
				while (iter.hasNext()) {
					StorageUsageSnapshot snapshot = iter.next();
					UsageDataKey key = new UsageDataKey(snapshot.getSnapshotKey());
					usageDataMap.put(key, snapshot.getUsageData());
					System.out.println("Loaded key:" + key);
				}
				LOG.info("Loaded usageDataMap");
			}

			/* Add usage data from events and aggregate snapshots
			 */
			for (Event event = receiver.receiveEventNoWait();
					event != null;
					event = receiver.receiveEventNoWait())
			{
				StorageEvent storageEvent = (StorageEvent) event;
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
					case S3Object:
						Long newObjectsNum =
							addLong(usageData.getObjectsNum(), addNum);
						usageData.setObjectsNum(newObjectsNum);
						Long newObjectsMegs =
							addLong(usageData.getObjectsMegs(), addAmountMegs);
						usageData.setObjectsMegs(newObjectsMegs);
						break;
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
				changedSnapshots.add(key);
			}

			/* Store aggregated final snapshots
			 */
			final long timestampMs = getTimestampMs();
			for (UsageDataKey key : changedSnapshots) {
				SnapshotKey snapshotKey = key.newSnapshotKey(timestampMs);
				StorageUsageSnapshot sus =
					new StorageUsageSnapshot(snapshotKey, usageDataMap.get(key));
				System.out.println("Storing:" + sus);
				sess.save(sus);
			}
			changedSnapshots.clear();

			entityWrapper.commit();
		} catch (Exception ex) {
			entityWrapper.rollback();
			LOG.error(ex);
		}
	}
	
	private static final Long addLong(Long a, long b)
	{
		return new Long(a.longValue() + b);
	}

	protected long getTimestampMs()
	{
		return System.currentTimeMillis();
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

		private StorageEventPoller getOuterType()
		{
			return StorageEventPoller.this;
		}


	}

}
